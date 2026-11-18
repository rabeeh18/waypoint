package com.Parcelo.demo;

import java.util.List;

/**
 * Synthetic India reference data for the demo. Town coordinates and PIN codes are approximate
 * public facts; every business, person, vehicle and shipment is invented.
 */
final class DemoData {

    private DemoData() {
    }

    record WarehouseSeed(String code, String name, String city, String address, double lat, double lng, String regPrefix) {
    }

    record TownSeed(String warehouseCode, String town, String pin, double lat, double lng) {
    }

    record VehicleType(String name, int capacityKg) {
    }

    static final List<WarehouseSeed> WAREHOUSES = List.of(
        new WarehouseSeed("DEL", "Delhi DC", "Delhi", "Okhla Industrial Area Phase III, New Delhi", 28.5272, 77.2740, "DL 01"),
        new WarehouseSeed("BOM", "Mumbai DC", "Mumbai", "Kalher, Bhiwandi, Mumbai Metropolitan Region", 19.2813, 73.0483, "MH 04"),
        new WarehouseSeed("BLR", "Bengaluru Hub", "Bengaluru", "Peenya Industrial Area, Bengaluru", 13.0285, 77.5197, "KA 03"),
        new WarehouseSeed("HYD", "Hyderabad Depot", "Hyderabad", "Medchal Road Industrial Area, Hyderabad", 17.6296, 78.4813, "TS 08"),
        new WarehouseSeed("MAA", "Chennai Depot", "Chennai", "Ambattur Industrial Estate, Chennai", 13.0983, 80.1620, "TN 09"),
        new WarehouseSeed("CCU", "Kolkata Hub", "Kolkata", "Dankuni Industrial Park, Hooghly", 22.6760, 88.2970, "WB 02"));

    static final List<TownSeed> TOWNS = List.of(
        new TownSeed("DEL", "Gurugram", "122001", 28.4595, 77.0266),
        new TownSeed("DEL", "Noida", "201301", 28.5355, 77.3910),
        new TownSeed("DEL", "Ghaziabad", "201001", 28.6692, 77.4538),
        new TownSeed("DEL", "Faridabad", "121001", 28.4089, 77.3178),
        new TownSeed("DEL", "Sonipat", "131001", 28.9931, 77.0151),
        new TownSeed("DEL", "Bahadurgarh", "124507", 28.6920, 76.9350),
        new TownSeed("DEL", "Greater Noida", "201310", 28.4744, 77.5040),
        new TownSeed("DEL", "Rohtak", "124001", 28.8955, 76.6066),
        new TownSeed("BOM", "Thane", "400601", 19.2183, 72.9781),
        new TownSeed("BOM", "Navi Mumbai", "400703", 19.0771, 72.9987),
        new TownSeed("BOM", "Kalyan", "421301", 19.2437, 73.1355),
        new TownSeed("BOM", "Vasai", "401201", 19.3919, 72.8397),
        new TownSeed("BOM", "Panvel", "410206", 18.9894, 73.1175),
        new TownSeed("BOM", "Ulhasnagar", "421001", 19.2215, 73.1645),
        new TownSeed("BOM", "Mira Road", "401107", 19.2813, 72.8688),
        new TownSeed("BLR", "Tumakuru", "572101", 13.3379, 77.1173),
        new TownSeed("BLR", "Hoskote", "562114", 13.0707, 77.7982),
        new TownSeed("BLR", "Nelamangala", "562123", 13.0990, 77.3940),
        new TownSeed("BLR", "Ramanagara", "562159", 12.7217, 77.2810),
        new TownSeed("BLR", "Devanahalli", "562110", 13.2480, 77.7130),
        new TownSeed("BLR", "Anekal", "562106", 12.7110, 77.6950),
        new TownSeed("HYD", "Secunderabad", "500003", 17.4399, 78.4983),
        new TownSeed("HYD", "Sangareddy", "502001", 17.6190, 78.0840),
        new TownSeed("HYD", "Shamshabad", "501218", 17.2610, 78.3820),
        new TownSeed("HYD", "Ibrahimpatnam", "501506", 17.1180, 78.6540),
        new TownSeed("HYD", "Bhongir", "508116", 17.5140, 78.8890),
        new TownSeed("MAA", "Sriperumbudur", "602105", 12.9670, 79.9420),
        new TownSeed("MAA", "Tiruvallur", "602001", 13.1430, 79.9080),
        new TownSeed("MAA", "Poonamallee", "600056", 13.0470, 80.0970),
        new TownSeed("MAA", "Chengalpattu", "603001", 12.6819, 79.9888),
        new TownSeed("MAA", "Gummidipoondi", "601201", 13.4070, 80.1090),
        new TownSeed("MAA", "Kancheepuram", "631501", 12.8342, 79.7036),
        new TownSeed("CCU", "Howrah", "711101", 22.5958, 88.2636),
        new TownSeed("CCU", "Barasat", "700124", 22.7230, 88.4800),
        new TownSeed("CCU", "Barrackpore", "700120", 22.7660, 88.3770),
        new TownSeed("CCU", "Serampore", "712201", 22.7500, 88.3400),
        new TownSeed("CCU", "Dum Dum", "700028", 22.6220, 88.4210),
        new TownSeed("CCU", "Baruipur", "700144", 22.3590, 88.4330));

    static final List<VehicleType> VEHICLE_TYPES = List.of(
        new VehicleType("Tata 407 light truck", 2500),
        new VehicleType("Ashok Leyland Dost", 1500),
        new VehicleType("Eicher Pro 2049", 3500),
        new VehicleType("Mahindra Bolero Pik-Up", 1200));

    static final List<String> DRIVER_NAMES = List.of(
        "Ramesh Kumar",
        "Priya Nair",
        "Arjun Reddy",
        "Sanjay Patil",
        "Vijay Iyer",
        "Suresh Naidu",
        "Anil Deshmukh",
        "Meena Pillai",
        "Rahul Bose",
        "Kiran Rao",
        "Deepak Joshi",
        "Farhan Sheikh",
        "Vikas Gupta",
        "Mohan Das",
        "Suman Banerjee",
        "Imran Khan",
        "Ravi Teja",
        "Sandeep Mehta",
        "Mohit Jain",
        "Rakesh Verma",
        "Bikram Chatterjee",
        "Kavita Menon",
        "Dinesh Shetty",
        "Gaurav Tiwari");

    static final List<String> BUSINESS_PREFIXES = List.of(
        "Sharma",
        "Iyer",
        "Reddy",
        "Mehta",
        "Banerjee",
        "Nair",
        "Patel",
        "Khan",
        "Gupta",
        "Desai",
        "Das",
        "Rao",
        "Joshi",
        "Menon",
        "Shah",
        "Verma",
        "Naidu",
        "Bose");

    static final List<String> BUSINESS_KINDS = List.of(
        "Traders",
        "General Store",
        "Hardware",
        "Medicals",
        "Electricals",
        "Cloth House",
        "Auto Parts",
        "Sweets",
        "Dairy",
        "Pharma Distributors",
        "Steel Works",
        "Agro Supplies",
        "Mobile Point",
        "Furnishers");

    static final List<String> STREETS = List.of(
        "Main Road",
        "MG Road",
        "Station Road",
        "Market Street",
        "Bus Stand Road",
        "Temple Road",
        "Mall Road",
        "Court Road",
        "Old Town",
        "Industrial Estate",
        "Gandhi Road",
        "Nehru Nagar");

    static final List<String> FAILURE_REASONS = List.of(
        "Recipient not available", "Shop closed", "Address not found", "Refused by recipient",
        "Payment not ready", "Access road blocked");
}
