-- Incidents and customer complaints are gone from the app.
drop table if exists incident_actions cascade;
drop table if exists incidents cascade;
drop table if exists complaints cascade;
drop sequence if exists incident_code_seq;
drop sequence if exists complaint_code_seq;
