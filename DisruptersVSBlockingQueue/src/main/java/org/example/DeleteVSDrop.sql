-- ==========================================
-- PRELIMINARY CLEANUP
-- ==========================================
DROP TABLE IF EXISTS idempotency_standard CASCADE;
DROP TABLE IF EXISTS idempotency_partitioned CASCADE;

-- ==========================================
-- SCENARIO A: THE STANDARD DELETE
-- ==========================================
CREATE TABLE idempotency_standard (
                                      id SERIAL PRIMARY KEY,
                                      key_value UUID,
                                      created_at TIMESTAMP
);

-- Insert 1 million rows spread across the last 3 days
INSERT INTO idempotency_standard (key_value, created_at)
SELECT gen_random_uuid(), NOW() - (random() * interval '3 days')
FROM generate_series(1, 1000000);

-- Benchmark the standard DELETE operation
EXPLAIN (ANALYZE, BUFFERS)
DELETE FROM idempotency_standard
WHERE created_at < NOW() - INTERVAL '2 days';


-- ==========================================
-- SCENARIO B: TIME-BASED PARTITIONING
-- ==========================================
CREATE TABLE idempotency_partitioned (
                                         id SERIAL,
                                         key_value UUID,
                                         created_at TIMESTAMP
) PARTITION BY RANGE (created_at);

-- Create a partition for old data (older than 48 hours)
CREATE TABLE idempotency_old_partition PARTITION OF idempotency_partitioned
    FOR VALUES FROM (MINVALUE) TO (NOW() - INTERVAL '2 days');

-- Create a partition for active data (last 48 hours to future)
CREATE TABLE idempotency_active_partition PARTITION OF idempotency_partitioned
    FOR VALUES FROM (NOW() - INTERVAL '2 days') TO (MAXVALUE);

-- Insert 1 million rows
INSERT INTO idempotency_partitioned (key_value, created_at)
SELECT gen_random_uuid(), NOW() - (random() * interval '3 days')
FROM generate_series(1, 1000000);

-- Benchmark the metadata operation
DROP TABLE idempotency_old_partition;


-- ==========================================
-- FINAL CLEANUP
-- ==========================================
DROP TABLE IF EXISTS idempotency_standard CASCADE;
DROP TABLE IF EXISTS idempotency_partitioned CASCADE;