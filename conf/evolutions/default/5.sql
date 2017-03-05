

# --- !Ups

ALTER TABLE bc_measure ADD location VARCHAR(20);
UPDATE bc_measure SET location='bridge/0';
ALTER TABLE bc_measure ALTER location SET NOT NULL;

# --- !Downs