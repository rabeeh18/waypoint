package com.waypoint.web;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.waypoint.common.geo.GridReference;
import com.waypoint.common.time.OperationalClock;
import com.waypoint.config.AppMode;
import com.waypoint.common.web.NotFoundException;
import com.waypoint.fleet.Vehicle;
import com.waypoint.fleet.VehicleRepository;
import com.waypoint.fleet.Warehouse;
import com.waypoint.fleet.WarehouseRepository;
import com.waypoint.risk.MonitoringService;
import com.waypoint.shipment.Shipment;
import com.waypoint.shipment.ShipmentRepository;
import com.waypoint.tracking.FleetBroadcaster;
import com.waypoint.tracking.VehicleLocationRepository;
import com.waypoint.trip.RouteVersionRepository;
import com.waypoint.trip.Trip;
import com.waypoint.trip.TripService;
import com.waypoint.trip.TripStop;

/** The dashboard page and the JSON it loads; live changes then arrive over STOMP. */
@Controller
public class DashboardController {

    private final MonitoringService monitoring;
    private final KpiService kpis;
    private final WarehouseRepository warehouses;
    private final VehicleRepository vehicles;
    private final TripService trips;
    private final RouteVersionRepository routes;
    private final ShipmentRepository shipments;
    private final VehicleLocationRepository locations;
    private final FleetBroadcaster broadcaster;
    private final OperationalClock clock;
    private final AppMode mode;

    public DashboardController(MonitoringService monitoring, KpiService kpis, WarehouseRepository warehouses,
                           VehicleRepository vehicles,
                           TripService trips, RouteVersionRepository routes, ShipmentRepository shipments,
                           VehicleLocationRepository locations, FleetBroadcaster broadcaster,
                           OperationalClock clock, AppMode mode) {
        this.monitoring = monitoring;
        this.kpis = kpis;
        this.warehouses = warehouses;
        this.vehicles = vehicles;
        this.trips = trips;
        this.routes = routes;
        this.shipments = shipments;
        this.locations = locations;
        this.broadcaster = broadcaster;
        this.clock = clock;
        this.mode = mode;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("kpi", kpis.compute());
        return "dashboard";
    }

    @GetMapping("/api/dashboard/snapshot")
    @ResponseBody
    @Transactional(readOnly = true)
    public Map<String, Object> snapshot() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("t", clock.now().toEpochMilli());
        out.put("kpis", kpis.compute());
        out.put("trips", monitoring.summaries());
        out.put("warehouses", warehouses.findByActiveTrueOrderByNameAsc().stream().map(DashboardController::warehouseJson).toList());
        out.put("vehicles", vehicles.findAllByOrderByRegistrationAsc().stream().map(DashboardController::vehicleJson).toList());
        out.put("positions", broadcaster.positionsFrame());
        return out;
    }

    @GetMapping("/api/dashboard/kpis")
    @ResponseBody
    public Map<String, Object> kpis() {
        return kpis.compute();
    }

    /** Everything the side panel and map need to show one trip. */
    @GetMapping("/api/dashboard/trips/{id}")
    @ResponseBody
    @Transactional(readOnly = true)
    public Map<String, Object> trip(@PathVariable long id) {
        Trip trip = trips.get(id);
        Map<Long, List<Shipment>> byStop = shipments.findByTripIdOrderByIdAsc(id).stream()
            .filter(s -> s.getStop() != null)
            .collect(Collectors.groupingBy(s -> s.getStop().getId()));
        List<Map<String, Object>> stops = new ArrayList<>();
        for (TripStop s : trip.getStops()) {
            List<Shipment> list = byStop.getOrDefault(s.getId(), List.of());
            Instant deadline = list.stream().map(Shipment::getDeadline).min(Instant::compareTo).orElse(null);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("seq", s.getSeq());
            m.put("kind", s.getKind().name());
            m.put("name", s.getName());
            m.put("status", s.getStatus().name());
            m.put("lat", s.getLat());
            m.put("lng", s.getLng());
            m.put("plannedArrival", epoch(s.getPlannedArrival()));
            m.put("eta", epoch(s.getEta()));
            m.put("actualArrival", epoch(s.getActualArrival()));
            m.put("actualDeparture", epoch(s.getActualDeparture()));
            m.put("deadline", epoch(deadline));
            m.put("shipments", list.size());
            m.put("grid", GridReference.of(s.getLat(), s.getLng()));
            stops.add(m);
        }
        List<Map<String, Object>> routeVersions = routes.findByTripIdOrderByVersionNoAsc(id).stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("version", r.getVersionNo());
            m.put("active", r.isActive());
            m.put("geometry", r.getGeometry());
            m.put("estimated", r.isEstimated());
            m.put("reason", r.getReason());
            m.put("distanceM", r.getDistanceM());
            return m;
        }).toList();
        List<double[]> trail = locations.findByTripIdOrderByRecordedAtAsc(id).stream()
            .map(l -> new double[] {round(l.getLat()), round(l.getLng())}).toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", trip.getId());
        out.put("code", trip.getCode());
        out.put("status", trip.getStatus().name());
        out.put("vehicle", vehicleJson(trip.getVehicle()));
        out.put("driver", Map.of("id", trip.getDriver().getId(), "name", trip.getDriver().getFullName(), "phone", trip.getDriver().getPhone()));
        out.put("warehouse", warehouseJson(trip.getWarehouse()));
        out.put("plannedStart", epoch(trip.getPlannedStart()));
        out.put("actualStart", epoch(trip.getActualStart()));
        out.put("weightKg", trip.getTotalWeightKg());
        out.put("delayAdjustMin", trip.getDelayAdjustMin());
        out.put("stops", stops);
        out.put("routes", routeVersions);
        out.put("trail", trail);
        out.put("summary", monitoring.summary(id));
        return out;
    }

    // ------------------------------------------------------------------ json helpers

    static Map<String, Object> warehouseJson(Warehouse w) {
        return Map.of("id", w.getId(), "code", w.getCode(), "name", w.getName(), "city", w.getCity(),
            "lat", w.getLat(), "lng", w.getLng(), "grid", GridReference.of(w.getLat(), w.getLng()));
    }

    static Map<String, Object> vehicleJson(Vehicle v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", v.getId());
        m.put("registration", v.getRegistration());
        m.put("type", v.getType());
        m.put("capacityKg", v.getCapacityKg());
        m.put("status", v.getStatus().name());
        m.put("statusLabel", v.getStatus().label());
        m.put("lat", v.lastPosition().lat());
        m.put("lng", v.lastPosition().lng());
        m.put("warehouse", v.getHomeWarehouse().getCode());
        return m;
    }

    static Long epoch(Instant i) {
        return i == null ? null : i.toEpochMilli();
    }

    private static double round(double v) {
        return Math.round(v * 1e5) / 1e5;
    }
}
