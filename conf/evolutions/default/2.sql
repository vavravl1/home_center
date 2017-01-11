

# --- !Ups

CREATE TABLE watering (
  timestamp TIMESTAMP NOT NULL,
  actual_humidity INT NOT NULL,
  aggregated BOOLEAN DEFAULT FALSE
);

# --- !Downs