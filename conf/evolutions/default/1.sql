
# --- !Ups

CREATE TABLE location (
  address VARCHAR(20) PRIMARY KEY,
  label VARCHAR(40) NOT NULL
);


CREATE TABLE sensor (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(20),
  locationAddress VARCHAR(20)
);
ALTER TABLE sensor ADD CONSTRAINT unique__sensor UNIQUE(name, locationAddress);
ALTER TABLE sensor ADD CONSTRAINT fk_locationAddress
  FOREIGN KEY (locationAddress) REFERENCES location(address)
  ON UPDATE CASCADE;


CREATE TABLE measuredPhenomenon (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(20),
  unit VARCHAR(15),
  aggregationStrategy VARCHAR(20),
  sensorId BIGINT,
);
CREATE INDEX unique__measuredPhenomenon ON measuredPhenomenon(name, sensorId);

ALTER TABLE measuredPhenomenon ADD CONSTRAINT fk_sensorId
  FOREIGN KEY (sensorId) REFERENCES sensor(id)
  ON DELETE CASCADE
  ON UPDATE CASCADE;


CREATE TABLE measurement (
  value DOUBLE,
  measureTimestamp TIMESTAMP NOT NULL,
  measuredPhenomenonId BIGINT,
  aggregated BOOLEAN DEFAULT FALSE,
);
ALTER TABLE measurement ADD CONSTRAINT fk_measuredPhenomenonId
  FOREIGN KEY (measuredPhenomenonId) REFERENCES measuredPhenomenon(id)
  ON DELETE CASCADE
  ON UPDATE CASCADE;







