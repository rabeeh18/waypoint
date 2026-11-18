-- Parcelo schema.


CREATE TABLE audit_log (
    id bigint NOT NULL,
    actor character varying(60) NOT NULL,
    action character varying(60) NOT NULL,
    entity_type character varying(30) NOT NULL,
    entity_id character varying(40),
    summary character varying(300) NOT NULL,
    before_json text,
    after_json text,
    occurred_at timestamp with time zone NOT NULL
);

CREATE SEQUENCE audit_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE audit_log_id_seq OWNED BY audit_log.id;

CREATE TABLE delivery_proofs (
    id bigint NOT NULL,
    stop_id bigint NOT NULL,
    recipient_name character varying(120) NOT NULL,
    note character varying(500),
    lat double precision,
    lng double precision,
    captured_by character varying(60),
    captured_at timestamp with time zone NOT NULL
);

CREATE SEQUENCE delivery_proofs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE delivery_proofs_id_seq OWNED BY delivery_proofs.id;

CREATE TABLE drivers (
    id bigint NOT NULL,
    user_id bigint,
    full_name character varying(120) NOT NULL,
    phone character varying(20) NOT NULL,
    license_number character varying(30) NOT NULL,
    license_expiry date NOT NULL,
    home_warehouse_id bigint NOT NULL,
    status character varying(20) DEFAULT 'AVAILABLE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT drivers_status_check CHECK (((status)::text = ANY ((ARRAY['AVAILABLE'::character varying, 'ON_TRIP'::character varying, 'OFF_DUTY'::character varying, 'UNAVAILABLE'::character varying])::text[])))
);

CREATE SEQUENCE drivers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE drivers_id_seq OWNED BY drivers.id;


CREATE TABLE maintenance_records (
    id bigint NOT NULL,
    vehicle_id bigint NOT NULL,
    kind character varying(20) NOT NULL,
    reason character varying(255) NOT NULL,
    started_at timestamp with time zone NOT NULL,
    ended_at timestamp with time zone,
    created_by character varying(60),
    CONSTRAINT maintenance_records_check CHECK (((ended_at IS NULL) OR (ended_at >= started_at))),
    CONSTRAINT maintenance_records_kind_check CHECK (((kind)::text = ANY ((ARRAY['BREAKDOWN'::character varying, 'SCHEDULED'::character varying, 'INSPECTION'::character varying])::text[])))
);

CREATE SEQUENCE maintenance_records_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE maintenance_records_id_seq OWNED BY maintenance_records.id;

CREATE TABLE notifications (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    severity character varying(10) NOT NULL,
    title character varying(200) NOT NULL,
    body character varying(500),
    link character varying(200),
    created_at timestamp with time zone NOT NULL,
    read_at timestamp with time zone
);

CREATE SEQUENCE notifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE notifications_id_seq OWNED BY notifications.id;

CREATE TABLE risk_evaluations (
    id bigint NOT NULL,
    trip_id bigint NOT NULL,
    evaluated_at timestamp with time zone NOT NULL,
    score integer NOT NULL,
    band character varying(10) NOT NULL,
    reasons text NOT NULL
);

CREATE SEQUENCE risk_evaluations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE risk_evaluations_id_seq OWNED BY risk_evaluations.id;

CREATE TABLE risk_rules (
    code character varying(30) NOT NULL,
    label character varying(120) NOT NULL,
    description character varying(400) NOT NULL,
    points integer NOT NULL,
    threshold double precision,
    unit character varying(20),
    enabled boolean DEFAULT true NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by character varying(60),
    CONSTRAINT risk_rules_points_check CHECK ((points >= 0))
);

CREATE TABLE roles (
    id smallint NOT NULL,
    name character varying(30) NOT NULL
);

CREATE SEQUENCE roles_id_seq
    AS smallint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE roles_id_seq OWNED BY roles.id;

CREATE TABLE route_versions (
    id bigint NOT NULL,
    trip_id bigint NOT NULL,
    version_no integer NOT NULL,
    active boolean DEFAULT true NOT NULL,
    geometry text NOT NULL,
    distance_m integer NOT NULL,
    duration_s integer NOT NULL,
    leg_distances_m text NOT NULL,
    leg_durations_s text NOT NULL,
    start_offset_m integer DEFAULT 0 NOT NULL,
    estimated boolean DEFAULT false NOT NULL,
    reason character varying(255) NOT NULL,
    created_by character varying(60),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE SEQUENCE route_versions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE route_versions_id_seq OWNED BY route_versions.id;

CREATE TABLE shipments (
    id bigint NOT NULL,
    tracking_no character varying(20) NOT NULL,
    origin_warehouse_id bigint NOT NULL,
    recipient_name character varying(120) NOT NULL,
    recipient_phone character varying(20),
    dest_name character varying(160) NOT NULL,
    dest_address character varying(255) NOT NULL,
    dest_pin character varying(6) NOT NULL,
    dest_lat double precision NOT NULL,
    dest_lng double precision NOT NULL,
    weight_kg integer NOT NULL,
    deadline timestamp with time zone NOT NULL,
    status character varying(20) DEFAULT 'CREATED'::character varying NOT NULL,
    trip_id bigint,
    stop_id bigint,
    delivered_at timestamp with time zone,
    failure_reason character varying(255),
    attempts integer DEFAULT 0 NOT NULL,
    was_delayed boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT shipments_status_check CHECK (((status)::text = ANY ((ARRAY['CREATED'::character varying, 'PLANNED'::character varying, 'OUT_FOR_DELIVERY'::character varying, 'DELIVERED'::character varying, 'FAILED'::character varying, 'CANCELLED'::character varying])::text[]))),
    CONSTRAINT shipments_weight_kg_check CHECK ((weight_kg > 0))
);

CREATE SEQUENCE shipments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE shipments_id_seq OWNED BY shipments.id;

CREATE TABLE sla_breach_events (
    id bigint NOT NULL,
    shipment_id bigint NOT NULL,
    trip_id bigint,
    deadline timestamp with time zone NOT NULL,
    breached_at timestamp with time zone NOT NULL,
    minutes_late integer NOT NULL,
    kind character varying(20) NOT NULL,
    CONSTRAINT sla_breach_events_kind_check CHECK (((kind)::text = ANY ((ARRAY['LATE_DELIVERY'::character varying, 'FAILED'::character varying, 'DEADLINE_PASSED'::character varying])::text[])))
);

CREATE SEQUENCE sla_breach_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE sla_breach_events_id_seq OWNED BY sla_breach_events.id;

CREATE TABLE status_history (
    id bigint NOT NULL,
    entity_type character varying(20) NOT NULL,
    entity_id bigint NOT NULL,
    from_status character varying(20),
    to_status character varying(20) NOT NULL,
    reason character varying(255),
    actor character varying(60),
    changed_at timestamp with time zone NOT NULL
);

CREATE SEQUENCE status_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE status_history_id_seq OWNED BY status_history.id;

CREATE TABLE system_settings (
    key character varying(60) NOT NULL,
    value character varying(255) NOT NULL,
    label character varying(120) NOT NULL,
    description character varying(400) NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by character varying(60)
);

CREATE SEQUENCE tracking_no_seq
    START WITH 4100000
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE trip_assignments (
    id bigint NOT NULL,
    trip_id bigint NOT NULL,
    vehicle_id bigint NOT NULL,
    driver_id bigint NOT NULL,
    reason character varying(255) NOT NULL,
    assigned_by character varying(60),
    assigned_at timestamp with time zone NOT NULL
);

CREATE SEQUENCE trip_assignments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE trip_assignments_id_seq OWNED BY trip_assignments.id;

CREATE SEQUENCE trip_code_seq
    START WITH 100
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE trip_stops (
    id bigint NOT NULL,
    trip_id bigint NOT NULL,
    seq integer NOT NULL,
    kind character varying(12) DEFAULT 'DELIVERY'::character varying NOT NULL,
    name character varying(160) NOT NULL,
    address character varying(255) NOT NULL,
    lat double precision NOT NULL,
    lng double precision NOT NULL,
    service_min integer DEFAULT 10 NOT NULL,
    planned_arrival timestamp with time zone,
    eta timestamp with time zone,
    actual_arrival timestamp with time zone,
    actual_departure timestamp with time zone,
    status character varying(12) DEFAULT 'PENDING'::character varying NOT NULL,
    route_version_no integer DEFAULT 1 NOT NULL,
    leg_index integer DEFAULT 0 NOT NULL,
    CONSTRAINT trip_stops_kind_check CHECK (((kind)::text = ANY ((ARRAY['DELIVERY'::character varying, 'TRANSFER'::character varying])::text[]))),
    CONSTRAINT trip_stops_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ARRIVED'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying, 'SKIPPED'::character varying])::text[])))
);

CREATE SEQUENCE trip_stops_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE trip_stops_id_seq OWNED BY trip_stops.id;

CREATE TABLE trips (
    id bigint NOT NULL,
    code character varying(20) NOT NULL,
    warehouse_id bigint NOT NULL,
    vehicle_id bigint NOT NULL,
    driver_id bigint NOT NULL,
    status character varying(20) DEFAULT 'PLANNED'::character varying NOT NULL,
    planned_start timestamp with time zone NOT NULL,
    planned_end timestamp with time zone,
    actual_start timestamp with time zone,
    actual_end timestamp with time zone,
    total_weight_kg integer DEFAULT 0 NOT NULL,
    delay_adjust_min integer DEFAULT 0 NOT NULL,
    risk_score integer DEFAULT 0 NOT NULL,
    risk_band character varying(10) DEFAULT 'LOW'::character varying NOT NULL,
    risk_reasons text,
    risk_evaluated_at timestamp with time zone,
    created_by character varying(60),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    version integer DEFAULT 0 NOT NULL,
    CONSTRAINT trips_risk_band_check CHECK (((risk_band)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying, 'CRITICAL'::character varying])::text[]))),
    CONSTRAINT trips_status_check CHECK (((status)::text = ANY ((ARRAY['PLANNED'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying])::text[])))
);

CREATE SEQUENCE trips_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE trips_id_seq OWNED BY trips.id;

CREATE TABLE user_roles (
    user_id bigint NOT NULL,
    role_id smallint NOT NULL
);

CREATE TABLE users (
    id bigint NOT NULL,
    username character varying(60) NOT NULL,
    password_hash character varying(100) NOT NULL,
    full_name character varying(120) NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    email character varying(254) NOT NULL
);

CREATE SEQUENCE users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE users_id_seq OWNED BY users.id;

CREATE TABLE vehicle_locations (
    id bigint NOT NULL,
    vehicle_id bigint NOT NULL,
    trip_id bigint,
    recorded_at timestamp with time zone NOT NULL,
    lat double precision NOT NULL,
    lng double precision NOT NULL,
    speed_kph real
);

CREATE SEQUENCE vehicle_locations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE vehicle_locations_id_seq OWNED BY vehicle_locations.id;

CREATE TABLE vehicles (
    id bigint NOT NULL,
    registration character varying(20) NOT NULL,
    type character varying(30) NOT NULL,
    capacity_kg integer NOT NULL,
    home_warehouse_id bigint NOT NULL,
    status character varying(20) DEFAULT 'AVAILABLE'::character varying NOT NULL,
    last_lat double precision,
    last_lng double precision,
    last_seen_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT vehicles_capacity_kg_check CHECK ((capacity_kg > 0)),
    CONSTRAINT vehicles_status_check CHECK (((status)::text = ANY ((ARRAY['AVAILABLE'::character varying, 'ON_TRIP'::character varying, 'BROKEN_DOWN'::character varying, 'MAINTENANCE'::character varying, 'RETIRED'::character varying])::text[])))
);

CREATE SEQUENCE vehicles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE vehicles_id_seq OWNED BY vehicles.id;

CREATE TABLE warehouses (
    id bigint NOT NULL,
    code character varying(12) NOT NULL,
    name character varying(120) NOT NULL,
    city character varying(80) NOT NULL,
    address character varying(255) NOT NULL,
    lat double precision NOT NULL,
    lng double precision NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE SEQUENCE warehouses_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE warehouses_id_seq OWNED BY warehouses.id;

ALTER TABLE ONLY audit_log ALTER COLUMN id SET DEFAULT nextval('audit_log_id_seq'::regclass);

ALTER TABLE ONLY delivery_proofs ALTER COLUMN id SET DEFAULT nextval('delivery_proofs_id_seq'::regclass);

ALTER TABLE ONLY drivers ALTER COLUMN id SET DEFAULT nextval('drivers_id_seq'::regclass);

ALTER TABLE ONLY maintenance_records ALTER COLUMN id SET DEFAULT nextval('maintenance_records_id_seq'::regclass);

ALTER TABLE ONLY notifications ALTER COLUMN id SET DEFAULT nextval('notifications_id_seq'::regclass);

ALTER TABLE ONLY risk_evaluations ALTER COLUMN id SET DEFAULT nextval('risk_evaluations_id_seq'::regclass);

ALTER TABLE ONLY roles ALTER COLUMN id SET DEFAULT nextval('roles_id_seq'::regclass);

ALTER TABLE ONLY route_versions ALTER COLUMN id SET DEFAULT nextval('route_versions_id_seq'::regclass);

ALTER TABLE ONLY shipments ALTER COLUMN id SET DEFAULT nextval('shipments_id_seq'::regclass);

ALTER TABLE ONLY sla_breach_events ALTER COLUMN id SET DEFAULT nextval('sla_breach_events_id_seq'::regclass);

ALTER TABLE ONLY status_history ALTER COLUMN id SET DEFAULT nextval('status_history_id_seq'::regclass);

ALTER TABLE ONLY trip_assignments ALTER COLUMN id SET DEFAULT nextval('trip_assignments_id_seq'::regclass);

ALTER TABLE ONLY trip_stops ALTER COLUMN id SET DEFAULT nextval('trip_stops_id_seq'::regclass);

ALTER TABLE ONLY trips ALTER COLUMN id SET DEFAULT nextval('trips_id_seq'::regclass);

ALTER TABLE ONLY users ALTER COLUMN id SET DEFAULT nextval('users_id_seq'::regclass);

ALTER TABLE ONLY vehicle_locations ALTER COLUMN id SET DEFAULT nextval('vehicle_locations_id_seq'::regclass);

ALTER TABLE ONLY vehicles ALTER COLUMN id SET DEFAULT nextval('vehicles_id_seq'::regclass);

ALTER TABLE ONLY warehouses ALTER COLUMN id SET DEFAULT nextval('warehouses_id_seq'::regclass);

ALTER TABLE ONLY audit_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (id);

ALTER TABLE ONLY delivery_proofs
    ADD CONSTRAINT delivery_proofs_pkey PRIMARY KEY (id);

ALTER TABLE ONLY drivers
    ADD CONSTRAINT drivers_license_number_key UNIQUE (license_number);

ALTER TABLE ONLY drivers
    ADD CONSTRAINT drivers_pkey PRIMARY KEY (id);

ALTER TABLE ONLY drivers
    ADD CONSTRAINT drivers_user_id_key UNIQUE (user_id);


ALTER TABLE ONLY maintenance_records
    ADD CONSTRAINT maintenance_records_pkey PRIMARY KEY (id);

ALTER TABLE ONLY notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);

ALTER TABLE ONLY risk_evaluations
    ADD CONSTRAINT risk_evaluations_pkey PRIMARY KEY (id);

ALTER TABLE ONLY risk_rules
    ADD CONSTRAINT risk_rules_pkey PRIMARY KEY (code);

ALTER TABLE ONLY roles
    ADD CONSTRAINT roles_name_key UNIQUE (name);

ALTER TABLE ONLY roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);

ALTER TABLE ONLY route_versions
    ADD CONSTRAINT route_versions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY route_versions
    ADD CONSTRAINT route_versions_trip_id_version_no_key UNIQUE (trip_id, version_no);

ALTER TABLE ONLY shipments
    ADD CONSTRAINT shipments_pkey PRIMARY KEY (id);

ALTER TABLE ONLY shipments
    ADD CONSTRAINT shipments_tracking_no_key UNIQUE (tracking_no);

ALTER TABLE ONLY sla_breach_events
    ADD CONSTRAINT sla_breach_events_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sla_breach_events
    ADD CONSTRAINT sla_breach_events_shipment_id_kind_key UNIQUE (shipment_id, kind);

ALTER TABLE ONLY status_history
    ADD CONSTRAINT status_history_pkey PRIMARY KEY (id);

ALTER TABLE ONLY system_settings
    ADD CONSTRAINT system_settings_pkey PRIMARY KEY (key);

ALTER TABLE ONLY trip_assignments
    ADD CONSTRAINT trip_assignments_pkey PRIMARY KEY (id);

ALTER TABLE ONLY trip_stops
    ADD CONSTRAINT trip_stops_pkey PRIMARY KEY (id);

ALTER TABLE ONLY trip_stops
    ADD CONSTRAINT trip_stops_trip_id_seq_key UNIQUE (trip_id, seq);

ALTER TABLE ONLY trips
    ADD CONSTRAINT trips_code_key UNIQUE (code);

ALTER TABLE ONLY trips
    ADD CONSTRAINT trips_pkey PRIMARY KEY (id);

ALTER TABLE ONLY user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id);

ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

ALTER TABLE ONLY users
    ADD CONSTRAINT users_username_key UNIQUE (username);

ALTER TABLE ONLY vehicle_locations
    ADD CONSTRAINT vehicle_locations_pkey PRIMARY KEY (id);

ALTER TABLE ONLY vehicles
    ADD CONSTRAINT vehicles_pkey PRIMARY KEY (id);

ALTER TABLE ONLY vehicles
    ADD CONSTRAINT vehicles_registration_key UNIQUE (registration);

ALTER TABLE ONLY warehouses
    ADD CONSTRAINT warehouses_code_key UNIQUE (code);

ALTER TABLE ONLY warehouses
    ADD CONSTRAINT warehouses_pkey PRIMARY KEY (id);


CREATE INDEX ix_assignments_trip ON trip_assignments USING btree (trip_id);

CREATE INDEX ix_audit_entity ON audit_log USING btree (entity_type, entity_id);

CREATE INDEX ix_audit_time ON audit_log USING btree (occurred_at DESC);

CREATE INDEX ix_locations_trip_time ON vehicle_locations USING btree (trip_id, recorded_at);

CREATE INDEX ix_locations_vehicle_time ON vehicle_locations USING btree (vehicle_id, recorded_at);

CREATE INDEX ix_maintenance_vehicle ON maintenance_records USING btree (vehicle_id, started_at);

CREATE INDEX ix_notifications_user ON notifications USING btree (user_id, read_at, created_at);

CREATE INDEX ix_risk_eval_trip ON risk_evaluations USING btree (trip_id, evaluated_at);

CREATE INDEX ix_shipments_deadline ON shipments USING btree (deadline);

CREATE INDEX ix_shipments_status ON shipments USING btree (status);

CREATE INDEX ix_shipments_stop ON shipments USING btree (stop_id);

CREATE INDEX ix_shipments_trip ON shipments USING btree (trip_id);

CREATE INDEX ix_status_history_entity ON status_history USING btree (entity_type, entity_id, changed_at);

CREATE INDEX ix_stops_trip ON trip_stops USING btree (trip_id, seq);

CREATE INDEX ix_trips_planned_start ON trips USING btree (planned_start);

CREATE INDEX ix_trips_status ON trips USING btree (status);

CREATE UNIQUE INDEX ux_maintenance_open ON maintenance_records USING btree (vehicle_id) WHERE (ended_at IS NULL);

CREATE UNIQUE INDEX ux_route_active ON route_versions USING btree (trip_id) WHERE active;

CREATE UNIQUE INDEX ux_trips_live_driver ON trips USING btree (driver_id) WHERE ((status)::text = ANY ((ARRAY['PLANNED'::character varying, 'IN_PROGRESS'::character varying])::text[]));

CREATE UNIQUE INDEX ux_trips_live_vehicle ON trips USING btree (vehicle_id) WHERE ((status)::text = ANY ((ARRAY['PLANNED'::character varying, 'IN_PROGRESS'::character varying])::text[]));

CREATE UNIQUE INDEX ux_users_email ON users USING btree (lower((email)::text));

ALTER TABLE ONLY delivery_proofs
    ADD CONSTRAINT delivery_proofs_stop_id_fkey FOREIGN KEY (stop_id) REFERENCES trip_stops(id);

ALTER TABLE ONLY drivers
    ADD CONSTRAINT drivers_home_warehouse_id_fkey FOREIGN KEY (home_warehouse_id) REFERENCES warehouses(id);

ALTER TABLE ONLY drivers
    ADD CONSTRAINT drivers_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE ONLY maintenance_records
    ADD CONSTRAINT maintenance_records_vehicle_id_fkey FOREIGN KEY (vehicle_id) REFERENCES vehicles(id);

ALTER TABLE ONLY notifications
    ADD CONSTRAINT notifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE ONLY risk_evaluations
    ADD CONSTRAINT risk_evaluations_trip_id_fkey FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE;

ALTER TABLE ONLY route_versions
    ADD CONSTRAINT route_versions_trip_id_fkey FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE;

ALTER TABLE ONLY shipments
    ADD CONSTRAINT shipments_origin_warehouse_id_fkey FOREIGN KEY (origin_warehouse_id) REFERENCES warehouses(id);

ALTER TABLE ONLY shipments
    ADD CONSTRAINT shipments_stop_id_fkey FOREIGN KEY (stop_id) REFERENCES trip_stops(id);

ALTER TABLE ONLY shipments
    ADD CONSTRAINT shipments_trip_id_fkey FOREIGN KEY (trip_id) REFERENCES trips(id);

ALTER TABLE ONLY sla_breach_events
    ADD CONSTRAINT sla_breach_events_shipment_id_fkey FOREIGN KEY (shipment_id) REFERENCES shipments(id);

ALTER TABLE ONLY sla_breach_events
    ADD CONSTRAINT sla_breach_events_trip_id_fkey FOREIGN KEY (trip_id) REFERENCES trips(id);

ALTER TABLE ONLY trip_assignments
    ADD CONSTRAINT trip_assignments_driver_id_fkey FOREIGN KEY (driver_id) REFERENCES drivers(id);

ALTER TABLE ONLY trip_assignments
    ADD CONSTRAINT trip_assignments_trip_id_fkey FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE;

ALTER TABLE ONLY trip_assignments
    ADD CONSTRAINT trip_assignments_vehicle_id_fkey FOREIGN KEY (vehicle_id) REFERENCES vehicles(id);

ALTER TABLE ONLY trip_stops
    ADD CONSTRAINT trip_stops_trip_id_fkey FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE;

ALTER TABLE ONLY trips
    ADD CONSTRAINT trips_driver_id_fkey FOREIGN KEY (driver_id) REFERENCES drivers(id);

ALTER TABLE ONLY trips
    ADD CONSTRAINT trips_vehicle_id_fkey FOREIGN KEY (vehicle_id) REFERENCES vehicles(id);

ALTER TABLE ONLY trips
    ADD CONSTRAINT trips_warehouse_id_fkey FOREIGN KEY (warehouse_id) REFERENCES warehouses(id);

ALTER TABLE ONLY user_roles
    ADD CONSTRAINT user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES roles(id);

ALTER TABLE ONLY user_roles
    ADD CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE ONLY vehicle_locations
    ADD CONSTRAINT vehicle_locations_trip_id_fkey FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE SET NULL;

ALTER TABLE ONLY vehicle_locations
    ADD CONSTRAINT vehicle_locations_vehicle_id_fkey FOREIGN KEY (vehicle_id) REFERENCES vehicles(id);

ALTER TABLE ONLY vehicles
    ADD CONSTRAINT vehicles_home_warehouse_id_fkey FOREIGN KEY (home_warehouse_id) REFERENCES warehouses(id);


-- Starting data: roles, risk rules and settings.

INSERT INTO risk_rules (code, label, description, points, threshold, unit, enabled, sort_order, updated_at, updated_by) VALUES ('PROJECTED_LATE', 'Will be late', 'A parcel will arrive after its promised time. Needs action from the minutes set below; points per minute late (threshold), up to the maximum.', 45, 1.5, 'pts/min', true, 10, '2026-11-18 16:34:29.90583+05:30', NULL);
INSERT INTO risk_rules (code, label, description, points, threshold, unit, enabled, sort_order, updated_at, updated_by) VALUES ('DEADLINE_PRESSURE', 'Cutting it close', 'Heads-up: a parcel will arrive within the threshold of its promised time, or only a few minutes after it.', 15, 15, 'min', true, 20, '2026-11-18 16:34:29.90583+05:30', NULL);
INSERT INTO risk_rules (code, label, description, points, threshold, unit, enabled, sort_order, updated_at, updated_by) VALUES ('UNEXPECTED_STOP', 'Truck stuck on the road', 'The truck has not moved for longer than the threshold while away from any stop.', 25, 8, 'min', true, 30, '2026-11-18 16:34:29.90583+05:30', NULL);
INSERT INTO risk_rules (code, label, description, points, threshold, unit, enabled, sort_order, updated_at, updated_by) VALUES ('LOCATION_STALE', 'GPS lost', 'No position received for longer than the threshold.', 30, 5, 'min', true, 40, '2026-11-18 16:34:29.90583+05:30', NULL);
INSERT INTO risk_rules (code, label, description, points, threshold, unit, enabled, sort_order, updated_at, updated_by) VALUES ('ROUTE_DEVIATION', 'Off route', 'The truck is further than the threshold from its planned road.', 20, 500, 'm', true, 50, '2026-11-18 16:34:29.90583+05:30', NULL);
INSERT INTO risk_rules (code, label, description, points, threshold, unit, enabled, sort_order, updated_at, updated_by) VALUES ('LONG_STOP', 'Truck stuck at a stop', 'The truck has been at a stop longer than planned plus the threshold.', 15, 10, 'min', true, 60, '2026-11-18 16:34:29.90583+05:30', NULL);
INSERT INTO risk_rules (code, label, description, points, threshold, unit, enabled, sort_order, updated_at, updated_by) VALUES ('PRIOR_DELAYS', 'Late before', 'Heads-up: a parcel on board was already late or re-attempted once.', 10, NULL, NULL, true, 70, '2026-11-18 16:34:29.90583+05:30', NULL);
INSERT INTO risk_rules (code, label, description, points, threshold, unit, enabled, sort_order, updated_at, updated_by) VALUES ('BREAKDOWN', 'Breakdown', 'The driver reported a breakdown.', 60, NULL, NULL, true, 80, '2026-11-18 16:34:29.90583+05:30', NULL);

INSERT INTO roles (id, name) VALUES (2, 'DISPATCHER');
INSERT INTO roles (id, name) VALUES (3, 'DRIVER');

INSERT INTO system_settings (key, value, label, description, updated_at, updated_by) VALUES ('tracking.geofence-m', '120', 'Arrival radius (m)', 'A vehicle within this distance of its next stop is marked as arrived.', '2026-11-18 16:34:29.90583+05:30', NULL);
INSERT INTO system_settings (key, value, label, description, updated_at, updated_by) VALUES ('tracking.persist-every-s', '60', 'Store location every (s)', 'Location history is stored at most once per vehicle per this many seconds.', '2026-11-18 16:34:29.90583+05:30', NULL);
INSERT INTO system_settings (key, value, label, description, updated_at, updated_by) VALUES ('tracking.retention-days', '30', 'Location retention (days)', 'Location history older than this is deleted nightly.', '2026-11-18 16:34:29.90583+05:30', NULL);
INSERT INTO system_settings (key, value, label, description, updated_at, updated_by) VALUES ('sla.at-risk-min', '15', 'Cutting it close (min)', 'A parcel arriving within this many minutes of its promised time is a heads-up (Watch).', '2026-11-18 16:34:29.90583+05:30', NULL);
INSERT INTO system_settings (key, value, label, description, updated_at, updated_by) VALUES ('risk.late-action-min', '5', 'Late needs action from (min)', 'A parcel this many minutes or more past its promised time makes the trip Needs action and opens a "Will be late" incident.', '2026-11-18 16:34:29.962586+05:30', NULL);
INSERT INTO system_settings (key, value, label, description, updated_at, updated_by) VALUES ('clock.saved', '', 'Saved clock', 'Last saved demo time, restored on restart.', '2026-11-18 16:34:52.546147+05:30', 'system');


