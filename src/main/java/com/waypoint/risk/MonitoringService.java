package com.waypoint.risk;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.waypoint.common.geo.GridReference;
import com.waypoint.common.time.OperationalClock;
import com.waypoint.common.time.StartupGate;
import com.waypoint.settings.SettingsService;
import com.waypoint.shipment.Shipment;
import com.waypoint.shipment.ShipmentRepository;
import com.waypoint.shipment.ShipmentStatus;
import com.waypoint.tracking.FleetBroadcaster;
import com.waypoint.tracking.LiveFleet;
import com.waypoint.tracking.LiveTrip;
import com.waypoint.trip.RiskBand;
import com.waypoint.trip.Trip;
import com.waypoint.trip.TripRepository;
import com.waypoint.trip.TripStatus;
import com.waypoint.trip.TripStop;

/**
 * The continuous monitoring loop. Every few seconds, for each in-progress trip: recompute ETAs,
 * score risk, record SLA breaches, open or clear incidents, then push the picture to dispatchers.
 */
@Service
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);
    private static final Duration REDETECT_EVERY = Duration.ofSeconds(60);

    private final LiveFleet live;
    private final TripRepository trips;
    private final ShipmentRepository shipments;
    private final RiskEvaluationRepository evaluations;
    private final SlaBreachRepository breaches;
    private final EtaCalculator etaCalculator;
    private final RiskEngine riskEngine;
    private final SettingsService settings;
    private final FleetBroadcaster broadcaster;
    private final OperationalClock clock;
    private final StartupGate gate;
    private final TransactionTemplate tx;

    private final Map<Long, TripSummary> latest = new ConcurrentHashMap<>();
    /** Last time each (trip, incident type) condition was reported, so we do not rewrite every cycle. */
    private final Map<String, Instant> activeConditions = new ConcurrentHashMap<>();
    private final Map<String, RiskBand> conditionSeverity = new ConcurrentHashMap<>();

    /** Demo reset: forget the latest trip summaries and the conditions being tracked. */
    public void forgetAll() {
        latest.clear();
        activeConditions.clear();
        conditionSeverity.clear();
    }

    public MonitoringService(LiveFleet live, TripRepository trips, ShipmentRepository shipments,
                             RiskEvaluationRepository evaluations, SlaBreachRepository breaches,
                             EtaCalculator etaCalculator, RiskEngine riskEngine,
                             SettingsService settings, FleetBroadcaster broadcaster,
                             OperationalClock clock, StartupGate gate, PlatformTransactionManager txManager) {
        this.live = live;
        this.trips = trips;
        this.shipments = shipments;
        this.evaluations = evaluations;
        this.breaches = breaches;
        this.etaCalculator = etaCalculator;
        this.riskEngine = riskEngine;
        this.settings = settings;
        this.broadcaster = broadcaster;
        this.clock = clock;
        this.gate = gate;
        this.tx = new TransactionTemplate(txManager);
    }

    private record Outcome(TripSummary summary, RiskAssessment assessment, String tripCode) {
    }

    @Scheduled(fixedDelay = 4000, initialDelay = 4000)
    public void cycle() {
        if (!gate.isOpen()) {
            return;
        }
        Instant now = clock.now();
        List<Long> seen = new ArrayList<>();
        for (LiveTrip lt : live.all()) {
            try {
                Outcome outcome = tx.execute(status -> evaluate(lt, now));
                if (outcome != null) {
                    latest.put(lt.tripId, outcome.summary());
                    seen.add(lt.tripId);
                }
            } catch (RuntimeException e) {
                log.warn("Monitoring failed for {}: {}", lt.tripCode, e.toString());
            }
        }
        latest.keySet().removeIf(id -> !seen.contains(id));
        broadcaster.publishTrips(Map.of("t", now.toEpochMilli(), "trips", summaries()));
    }

    private Outcome evaluate(LiveTrip lt, Instant now) {
        Trip trip = trips.findById(lt.tripId).orElse(null);
        if (trip == null || trip.getStatus() != TripStatus.IN_PROGRESS) {
            return null;
        }
        List<Shipment> open = shipments.findByTripIdInAndStatusIn(List.of(trip.getId()), List.of(ShipmentStatus.OUT_FOR_DELIVERY));
        EtaCalculator.TripEta eta = etaCalculator.compute(lt, trip.getDelayAdjustMin(), now);

        Integer deviation = null;
        TripStop nextStop = null;
        for (TripStop stop : trip.getStops()) {
            if (stop.getStatus().isDone() || !eta.byStop().containsKey(stop.getId())) {
                continue;
            }
            Instant stopEta = eta.byStop().get(stop.getId());
            if (nextStop == null) {
                nextStop = stop;
            }
            if (stopEta == null) {
                continue;
            }
            if (stop.getEta() == null || Math.abs(Duration.between(stop.getEta(), stopEta).toSeconds()) > 30) {
                stop.setEta(stopEta);
            }
            if (stop.getPlannedArrival() != null) {
                int d = (int) Duration.between(stop.getPlannedArrival(), stopEta).toMinutes();
                deviation = deviation == null ? d : Math.max(deviation, d);
            }
        }

        for (Shipment s : open) {
            if (s.getDeadline().isBefore(now) && !breaches.existsByShipmentIdAndKind(s.getId(), SlaBreachEvent.Kind.DEADLINE_PASSED)) {
                breaches.save(new SlaBreachEvent(s.getId(), trip.getId(), s.getDeadline(), now,
                    (int) Duration.between(s.getDeadline(), now).toMinutes(), SlaBreachEvent.Kind.DEADLINE_PASSED));
                s.flagDelayed();
            }
        }

        RiskAssessment assessment = riskEngine.assess(lt, eta, open, now);
        boolean bandChanged = assessment.band() != trip.getRiskBand();
        if (bandChanged || assessment.score() != trip.getRiskScore()) {
            if (bandChanged || Math.abs(assessment.score() - trip.getRiskScore()) >= 5) {
                evaluations.save(new RiskEvaluation(trip.getId(), now, assessment.score(), assessment.band(), assessment.explanation()));
            }
            trip.applyRisk(assessment.score(), assessment.band(), assessment.explanation(), now);
        }

        String condition;
        double offRoute;
        Double lat = null;
        Double lng = null;
        synchronized (lt) {
            offRoute = lt.offRouteMeters();
            if (lt.position() != null) {
                lat = lt.position().lat();
                lng = lt.position().lng();
            }
            condition = condition(lt, assessment);
        }
        Instant nextEta = nextStop == null ? null : eta.byStop().get(nextStop.getId());
        int done = (int) trip.completedStopCount();
        TripSummary summary = new TripSummary(trip.getId(), trip.getCode(), trip.getStatus().name(),
            lt.vehicleId, lt.registration, lt.driverName, trip.getWarehouse().getName(),
            trip.getWarehouse().getCity() + " → " + lastStopName(trip),
            done, trip.getStops().size(), trip.progressPercent(),
            nextStop == null ? null : nextStop.getName(), nextEta, deviation,
            assessment.worstLateMinutes() > 0 ? assessment.worstLateMinutes() : null,
            assessment.shipmentsAtRisk() + assessment.shipmentsLate(),
            assessment.score(), assessment.band().name(),
            assessment.contributions().stream().limit(4).toList(),
            condition, lat == null ? "—" : GridReference.of(lat, lng), lat, lng,
            Math.round(eta.pace() * 100) / 100.0, "NORMAL");
        return new Outcome(summary, assessment, trip.getCode());
    }

    private static String lastStopName(Trip trip) {
        List<TripStop> stops = trip.getStops();
        return stops.isEmpty() ? "—" : stops.get(stops.size() - 1).getName().replaceAll(" — .*", "");
    }

    private static String condition(LiveTrip lt, RiskAssessment a) {
        if (lt.brokenDown()) {
            return "Breakdown";
        }
        if (a.has("LOCATION_STALE")) {
            return "GPS lost";
        }
        if (a.has("ROUTE_DEVIATION")) {
            return "Off route";
        }
        if (a.has("UNEXPECTED_STOP")) {
            return "Stuck";
        }
        if (a.has("LONG_STOP")) {
            return "Stuck";
        }
        if (lt.currentStopAt() != null) {
            return "At stop";
        }
        return a.worstLateMinutes() > 0 ? "Running late" : "Driving";
    }

    // ------------------------------------------------------------------ reads for screens

    public List<TripSummary> summaries() {
        return latest.values().stream()
            .sorted(Comparator.comparingInt(TripSummary::riskScore).reversed().thenComparing(TripSummary::code))
            .toList();
    }

    public TripSummary summary(long tripId) {
        return latest.get(tripId);
    }

    public int shipmentsAtRisk() {
        return latest.values().stream().mapToInt(TripSummary::shipmentsAtRisk).sum();
    }
}
