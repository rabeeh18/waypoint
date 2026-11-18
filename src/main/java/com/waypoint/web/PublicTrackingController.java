package com.waypoint.web;

import java.time.Instant;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.waypoint.shipment.Shipment;
import com.waypoint.shipment.ShipmentRepository;
import com.waypoint.shipment.ShipmentStatus;

/** Anonymous parcel lookup: type a tracking number, see where the parcel is. */
@Controller
public class PublicTrackingController {

    private final ShipmentRepository shipments;
    private final TransactionTemplate readOnly;

    public PublicTrackingController(ShipmentRepository shipments, PlatformTransactionManager txManager) {
        this.shipments = shipments;
        this.readOnly = new TransactionTemplate(txManager);
        this.readOnly.setReadOnly(true);
    }

    @GetMapping("/track")
    public String track(@RequestParam(name = "n", required = false) String trackingNo, Model model) {
        String n = trackingNo == null ? "" : trackingNo.trim().toUpperCase();
        model.addAttribute("n", n);
        if (!n.isEmpty()) {
            readOnly.executeWithoutResult(tx -> shipments.findWithWarehouseByTrackingNo(n).ifPresentOrElse(s -> {
                model.addAttribute("s", s);
                model.addAttribute("from", s.getOriginWarehouse().getCity());
                model.addAttribute("status", message(s));
                Instant eta = s.getStatus() == ShipmentStatus.OUT_FOR_DELIVERY && s.getStop() != null ? s.getStop().getEta() : null;
                model.addAttribute("eta", eta);
            }, () -> model.addAttribute("problem", "We could not find a parcel with that number.")));
        }
        return "track";
    }

    private static String message(Shipment s) {
        return switch (s.getStatus()) {
            case CREATED, PLANNED -> "Waiting to leave the depot";
            case OUT_FOR_DELIVERY -> "On its way to you";
            case DELIVERED -> "Delivered";
            case FAILED -> "Delivery attempt failed";
            case CANCELLED -> "Cancelled";
        };
    }
}
