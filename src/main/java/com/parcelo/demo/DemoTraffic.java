package com.parcelo.demo;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.parcelo.common.time.StartupGate;
import com.parcelo.tracking.LiveFleet;
import com.parcelo.tracking.LiveTrip;
import com.parcelo.tracking.LocationIngestionService;

/**
 * Demo only: sends a position for every live trip every couple of seconds, moving the truck
 * along its route, so the maps have something to show. Real vehicles use the driver app instead.
 */
@Component
@Profile("demo")
public class DemoTraffic {

    /** Demo trucks drive at 40 km/h, shown 15 times faster so movement is visible on screen. */
    private static final double METERS_PER_TICK = 40 / 3.6 * 15 * 2;

    private final LiveFleet live;
    private final LocationIngestionService ingestion;
    private final StartupGate gate;

    public DemoTraffic(LiveFleet live, LocationIngestionService ingestion, StartupGate gate) {
        this.live = live;
        this.ingestion = ingestion;
        this.gate = gate;
    }

    @Scheduled(fixedDelay = 2000, initialDelay = 10_000)
    public void drive() {
        if (!gate.isOpen()) {
            return;
        }
        for (LiveTrip trip : live.all()) {
            double at = Math.min(trip.route.length(), trip.progress() + METERS_PER_TICK);
            ingestion.ingest(trip.vehicleId, trip.route.pointAt(at), 40.0, null);
        }
    }
}
