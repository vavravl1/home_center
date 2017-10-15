
# --- !Ups

CREATE INDEX unique__measurement_measuredPhenomenonId_measureTimestamp
  ON measurement(measuredPhenomenonId, measureTimestamp);






