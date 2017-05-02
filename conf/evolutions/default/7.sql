

# --- !Ups

CREATE TABLE location (
  address VARCHAR(20) PRIMARY KEY,
  label VARCHAR(40) NOT NULL
);

CREATE TABLE sensor (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(20),
  measuredPhenomenon VARCHAR(20),
  unit VARCHAR(5),
  location_address VARCHAR(20)
);
ALTER TABLE sensor ADD FOREIGN KEY (location_address) REFERENCES location(address);
CREATE INDEX unique__sensor ON sensor(measuredPhenomenon, location_address);


CREATE TABLE measurement (
  value DOUBLE,
  measureTimestamp TIMESTAMP NOT NULL,
  aggregated BOOLEAN DEFAULT FALSE,
  sensor_id BIGINT
);
ALTER TABLE measurement ADD FOREIGN KEY (sensor_id) REFERENCES sensor(id);