

# --- !Ups

CREATE TABLE bc_sensor_location (
  location VARCHAR(20) PRIMARY KEY,
  label VARCHAR(40) NOT NULL
);
INSERT INTO bc_sensor_location(location, label) VALUES('bridge/0', 'upstairs corridor');
INSERT INTO bc_sensor_location(location, label) VALUES('remote/0', 'upstairs corridor');

ALTER TABLE bc_measure ADD FOREIGN KEY (location) REFERENCES bc_sensor_location(location)

# --- !Downs