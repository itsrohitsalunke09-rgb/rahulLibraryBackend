-- V3__pg_optimizations.sql
-- PostgreSQL-specific optimizations

-- Set default statistics target for better query planning
ALTER SYSTEM SET default_statistics_target = 100;
ALTER SYSTEM SET track_activities = on;
ALTER SYSTEM SET track_counts = on;
ALTER SYSTEM SET track_io_timing = on;
ALTER SYSTEM SET track_functions = 'all';
ALTER SYSTEM SET log_min_duration_statement = 1000; -- log queries > 1s

-- Enable pg_stat_statements (requires extension)
-- Note: Requires shared_preload_libraries = 'pg_stat_statements' in postgresql.conf
-- and restart. On Supabase, this may need to be enabled via dashboard.

-- Set work_mem for complex queries (per operation)
-- ALTER SYSTEM SET work_mem = '64MB';
-- ALTER SYSTEM SET maintenance_work_mem = '256MB';

-- Enable parallel query
ALTER SYSTEM SET max_parallel_workers_per_gather = 4;
ALTER SYSTEM SET parallel_leader_participation = on;
ALTER SYSTEM SET parallel_tuple_cost = 0.1;
ALTER SYSTEM SET parallel_setup_cost = 1000.0;

-- Effective cache size (adjust based on available RAM)
-- ALTER SYSTEM SET effective_cache_size = '4GB';

-- Random page cost for SSD
ALTER SYSTEM SET random_page_cost = 1.1;
ALTER SYSTEM SET seq_page_cost = 1.0;

-- Checkpoint settings for write performance
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
ALTER SYSTEM SET wal_buffers = '16MB';
ALTER SYSTEM SET min_wal_size = '1GB';
ALTER SYSTEM SET max_wal_size = '4GB';

-- Reload configuration
SELECT pg_reload_conf();