package com.parcelo.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.parcelo.audit.AuditRepository;
import com.parcelo.common.time.OperationalClock;
import com.parcelo.fleet.Driver;
import com.parcelo.fleet.DriverRepository;
import com.parcelo.fleet.DriverStatus;
import com.parcelo.fleet.FleetService;
import com.parcelo.fleet.MaintenanceRecord;
import com.parcelo.fleet.MaintenanceRepository;
import com.parcelo.fleet.Vehicle;
import com.parcelo.fleet.VehicleRepository;
import com.parcelo.fleet.WarehouseRepository;
import com.parcelo.trip.TripRepository;
import com.parcelo.trip.TripStatus;

@Controller
public class FleetController {

    private final FleetService fleet;
    private final VehicleRepository vehicles;
    private final DriverRepository drivers;
    private final WarehouseRepository warehouses;
    private final MaintenanceRepository maintenance;
    private final TripRepository trips;
    private final AuditRepository audit;
    private final OperationalClock clock;
    private final Fmt fmt;

    public FleetController(FleetService fleet, VehicleRepository vehicles, DriverRepository drivers,
                           WarehouseRepository warehouses, MaintenanceRepository maintenance, TripRepository trips,
                           AuditRepository audit, OperationalClock clock, Fmt fmt) {
        this.fleet = fleet;
        this.vehicles = vehicles;
        this.drivers = drivers;
        this.warehouses = warehouses;
        this.maintenance = maintenance;
        this.trips = trips;
        this.audit = audit;
        this.clock = clock;
        this.fmt = fmt;
    }

    // ------------------------------------------------------------------ vehicles

    @GetMapping("/fleet/vehicles")
    @Transactional(readOnly = true)
    public String vehicles(Model model) {
        List<Vehicle> all = vehicles.findAllByOrderByRegistrationAsc();
        model.addAttribute("vehicles", all);
        model.addAttribute("warehouses", warehouses.findByActiveTrueOrderByNameAsc());
        model.addAttribute("counts", all.stream().collect(Collectors.groupingBy(v -> v.getStatus().name(), Collectors.counting())));
        model.addAttribute("tab", "vehicles");
        return "fleet/vehicles";
    }

    @PostMapping("/fleet/vehicles")
    public String createVehicle(@RequestParam String registration, @RequestParam String type, @RequestParam int capacityKg,
                                @RequestParam long warehouseId, RedirectAttributes ra) {
        Vehicle v = fleet.createVehicle(registration, type, capacityKg, warehouseId);
        ra.addFlashAttribute("ok", v.getRegistration() + " registered.");
        return "redirect:/fleet/vehicles/" + v.getId();
    }

    @GetMapping("/fleet/vehicles/{id}")
    @Transactional(readOnly = true)
    public String vehicle(@PathVariable long id, Model model) {
        Vehicle v = fleet.vehicle(id);
        v.getHomeWarehouse().getName();
        Instant now = clock.now();
        List<MaintenanceRecord> records = maintenance.findByVehicleIdOrderByStartedAtDesc(id);
        long downtimeMin = records.stream().filter(r -> r.getStartedAt().isAfter(now.minusSeconds(30L * 86400)))
            .mapToLong(r -> r.durationUntil(now).toMinutes()).sum();
        var recent = trips.search(null, null, id, null, now.minusSeconds(60L * 86400), now.plusSeconds(86400), PageRequest.of(0, 15));
        recent.forEach(t -> t.getStops().size());
        model.addAttribute("v", v);
        model.addAttribute("records", records);
        model.addAttribute("downtime30", fmt.duration(java.time.Duration.ofMinutes(downtimeMin)));
        model.addAttribute("trips", recent.getContent());
        model.addAttribute("liveTrip", trips.findFirstByVehicleIdAndStatusIn(id, List.of(TripStatus.PLANNED, TripStatus.IN_PROGRESS)).orElse(null));
        model.addAttribute("warehouses", warehouses.findByActiveTrueOrderByNameAsc());
        model.addAttribute("changes", audit.findTop20ByEntityTypeAndEntityIdOrderByOccurredAtDesc("VEHICLE", String.valueOf(id)));
        model.addAttribute("tab", "vehicles");
        return "fleet/vehicle";
    }

    @PostMapping("/fleet/vehicles/{id}")
    public String updateVehicle(@PathVariable long id, @RequestParam String type, @RequestParam int capacityKg,
                                @RequestParam long warehouseId,
                                RedirectAttributes ra) {
        fleet.updateVehicle(id, type, capacityKg, warehouseId);
        ra.addFlashAttribute("ok", "Vehicle updated.");
        return "redirect:/fleet/vehicles/" + id;
    }

    @PostMapping("/fleet/vehicles/{id}/workshop")
    public String workshop(@PathVariable long id, @RequestParam MaintenanceRecord.Kind kind, @RequestParam String reason,
                           RedirectAttributes ra) {
        fleet.sendToWorkshop(id, kind, reason);
        ra.addFlashAttribute("ok", "Vehicle marked unavailable.");
        return "redirect:/fleet/vehicles/" + id;
    }

    @PostMapping("/fleet/vehicles/{id}/return")
    public String returnToService(@PathVariable long id, RedirectAttributes ra) {
        fleet.returnToService(id);
        ra.addFlashAttribute("ok", "Vehicle is back in service and can be assigned.");
        return "redirect:/fleet/vehicles/" + id;
    }

    // ------------------------------------------------------------------ drivers

    @GetMapping("/fleet/drivers")
    @Transactional(readOnly = true)
    public String drivers(Model model) {
        List<Driver> all = drivers.findAllByOrderByFullNameAsc();
        LocalDate today = fmt.today();
        model.addAttribute("drivers", all);
        model.addAttribute("today", today);
        model.addAttribute("soon", today.plusDays(30));
        model.addAttribute("warehouses", warehouses.findByActiveTrueOrderByNameAsc());
        model.addAttribute("tab", "drivers");
        return "fleet/drivers";
    }

    @PostMapping("/fleet/drivers")
    public String createDriver(@RequestParam String fullName, @RequestParam String phone, @RequestParam String licenseNumber,
                               @RequestParam LocalDate licenseExpiry, @RequestParam long warehouseId, RedirectAttributes ra) {
        Driver d = fleet.createDriver(fullName, phone, licenseNumber, licenseExpiry, warehouseId);
        ra.addFlashAttribute("ok", d.getFullName() + " added.");
        return "redirect:/fleet/drivers/" + d.getId();
    }

    @GetMapping("/fleet/drivers/{id}")
    @Transactional(readOnly = true)
    public String driver(@PathVariable long id, Model model) {
        Driver d = fleet.driver(id);
        d.getHomeWarehouse().getName();
        Instant now = clock.now();
        var recent = trips.search(null, null, null, id, now.minusSeconds(60L * 86400), now.plusSeconds(86400), PageRequest.of(0, 15));
        recent.forEach(t -> t.getStops().size());
        model.addAttribute("d", d);
        model.addAttribute("trips", recent.getContent());
        model.addAttribute("statuses", DriverStatus.values());
        model.addAttribute("warehouses", warehouses.findByActiveTrueOrderByNameAsc());
        model.addAttribute("today", fmt.today());
        model.addAttribute("changes", audit.findTop20ByEntityTypeAndEntityIdOrderByOccurredAtDesc("DRIVER", String.valueOf(id)));
        model.addAttribute("tab", "drivers");
        return "fleet/driver";
    }

    @PostMapping("/fleet/drivers/{id}")
    public String updateDriver(@PathVariable long id, @RequestParam String fullName, @RequestParam String phone,
                               @RequestParam String licenseNumber, @RequestParam LocalDate licenseExpiry,
                               @RequestParam long warehouseId, @RequestParam DriverStatus status, RedirectAttributes ra) {
        fleet.updateDriver(id, fullName, phone, licenseNumber, licenseExpiry, warehouseId, status);
        ra.addFlashAttribute("ok", "Driver updated.");
        return "redirect:/fleet/drivers/" + id;
    }
}
