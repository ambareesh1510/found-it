-- Timeline searches submit location history transiently, so their audit row has no point.
ALTER TABLE lost_searches ALTER COLUMN location DROP NOT NULL;

ALTER TABLE lost_searches
    ADD CONSTRAINT lost_search_location_mode CHECK (used_timeline OR location IS NOT NULL);
