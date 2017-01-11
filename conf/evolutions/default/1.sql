

# --- !Ups

CREATE TABLE bc_measure (
  phenomenon VARCHAR(20) NOT NULL,
  sensor VARCHAR(20) NOT NULL,
  measure_timestamp   TIMESTAMP NOT NULL,
  value       INT       NOT NULL,
  unit        VARCHAR(5) NOT NULL,
  aggregated BOOLEAN DEFAULT FALSE
);

CREATE INDEX unique__bc_measure__sensor__measure_timestamp ON bc_measure(sensor, measure_timestamp)

# --- !Downs