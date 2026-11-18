package com.parcelo.web;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.parcelo.common.geo.GeoPoint;
import com.parcelo.common.web.BusinessRuleException;
import com.parcelo.fleet.Driver;
import com.parcelo.fleet.DriverRepository;
import com.parcelo.shipment.Shipment;
import com.parcelo.shipment.ShipmentRepository;
import com.parcelo.tracking.LocationIngestionService;
import com.parcelo.trip.Trip;
import com.parcelo.trip.TripRepository;
import com.parcelo.trip.TripService;
import com.parcelo.trip.TripStatus;
import com.parcelo.trip.TripStop;
import com.parcelo.trip.TripStopRepository;

/**
 * The driver's phone view. Every action checks that the trip belongs to the signed-in driver;
 * the URL alone never grants access.
 */
@Controller
public class DriverAppController {

    private final DriverRepository drivers;
    private final TripRepository trips;
    private final TripService tripService;
    private final ShipmentRepository shipments;
    private final TripStopRepository stops;
    private final LocationIngestionService ingestion;

    public DriverAppController(DriverRepository drivers, TripRepository trips, TripService tripService,
                               ShipmentRepository shipments, TripStopRepository stops, LocationIngestionService ingestion) {
        this.drivers = drivers;
        this.trips = trips;
        this.tripService = tripService;
        this.shipments = shipments;
        this.stops = stops;
        this.ingestion = ingestion;
    }

    @GetMapping("/driver")
    @Transactional(readOnly = true)
    public String home(Authentication auth, Model model) {
        Driver driver = driverFor(auth);
        model.addAttribute("driver", driver);
        trips.findFirstByDriverIdAndStatusInOrderByPlannedStartAsc(driver.getId(), List.of(TripStatus.IN_PROGRESS, TripStatus.PLANNED))
            .ifPresent(trip -> {
                trip.getStops().size();
                Map<Long, List<Shipment>> byStop = shipments.findByTripIdOrderByIdAsc(trip.getId()).stream()
                    .filter(s -> s.getStop() != null).collect(Collectors.groupingBy(s -> s.getStop().getId()));
                model.addAttribute("trip", trip);
                model.addAttribute("byStop", byStop);
                model.addAttribute("next", trip.nextOpenStop().orElse(null));
            });
        return "driver";
    }

    @PostMapping("/driver/trips/{id}/stops/{stopId}/arrive")
    public String arrive(@PathVariable long id, @PathVariable long stopId, Authentication auth) {
        own(id, auth);
        tripService.arrive(id, stopId);
        return "redirect:/driver";
    }

    @PostMapping("/driver/trips/{id}/stops/{stopId}/complete")
    public String complete(@PathVariable long id, @PathVariable long stopId, @RequestParam String recipient,
                           @RequestParam(required = false) String note, Authentication auth, RedirectAttributes ra) {
        Trip trip = own(id, auth);
        TripStop stop = stops.findByIdAndTripId(stopId, id)
            .orElseThrow(() -> new BusinessRuleException("That stop is not on your trip"));
        if (stop.getKind() == TripStop.Kind.TRANSFER) {
            tripService.completeTransfer(id, stopId);
        } else {
            tripService.completeStop(id, stopId, recipient, note, trip.getVehicle().lastPosition());
        }
        ra.addFlashAttribute("ok", "Delivery recorded — thank you.");
        return "redirect:/driver";
    }

    @PostMapping("/driver/trips/{id}/stops/{stopId}/fail")
    public String fail(@PathVariable long id, @PathVariable long stopId, @RequestParam String reason,
                       Authentication auth, RedirectAttributes ra) {
        own(id, auth);
        tripService.failStop(id, stopId, reason);
        ra.addFlashAttribute("ok", "Recorded. Dispatch has been told.");
        return "redirect:/driver";
    }

    public record LocationFix(double lat, double lng, Double speedKph) {
    }

    /** Phone GPS feed: the driver's position while a trip is in progress. */
    @PostMapping("/api/driver/location")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> location(@RequestBody LocationFix fix, Authentication auth) {
        Driver driver = driverFor(auth);
        Trip trip = trips.findFirstByDriverIdAndStatusInOrderByPlannedStartAsc(driver.getId(), List.of(TripStatus.IN_PROGRESS))
            .orElseThrow(() -> new BusinessRuleException("You have no trip in progress"));
        if (Math.abs(fix.lat()) > 90 || Math.abs(fix.lng()) > 180) {
            throw new BusinessRuleException("Invalid coordinates");
        }
        ingestion.ingest(trip.getVehicle().getId(), new GeoPoint(fix.lat(), fix.lng()), fix.speedKph(), null);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private Driver driverFor(Authentication auth) {
        return drivers.findByUserUsername(auth.getName())
            .orElseThrow(() -> new AccessDeniedException("This account is not linked to a driver"));
    }

    private Trip own(long tripId, Authentication auth) {
        Driver driver = driverFor(auth);
        Trip trip = tripService.get(tripId);
        if (!trip.getDriver().getId().equals(driver.getId())) {
            throw new AccessDeniedException("Trip " + tripId + " is not assigned to you");
        }
        return trip;
    }
}
