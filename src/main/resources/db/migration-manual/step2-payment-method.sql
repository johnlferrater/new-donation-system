-- STEP 2: convert payment_method from MySQL ENUM to VARCHAR(30)
-- Run with the application stopped, after a verified database backup.
-- Existing payment_method values are preserved; no table is dropped or recreated.

-- Preflight: review existing values and look for NULLs.
SELECT 'donation_records' AS table_name, payment_method, COUNT(*) AS row_count
FROM donation_records
GROUP BY payment_method
ORDER BY payment_method;

SELECT 'donation_history' AS table_name, payment_method, COUNT(*) AS row_count
FROM donation_history
GROUP BY payment_method
ORDER BY payment_method;

SELECT 'donation_records' AS table_name, COUNT(*) AS null_payment_methods
FROM donation_records
WHERE payment_method IS NULL
UNION ALL
SELECT 'donation_history' AS table_name, COUNT(*) AS null_payment_methods
FROM donation_history
WHERE payment_method IS NULL;

-- If either NULL count is non-zero, review those rows before continuing.
-- The ALTER statements intentionally fail rather than changing existing values.
ALTER TABLE donation_records
    MODIFY COLUMN payment_method VARCHAR(30) NOT NULL;

ALTER TABLE donation_history
    MODIFY COLUMN payment_method VARCHAR(30) NOT NULL;

SHOW COLUMNS FROM donation_records LIKE 'payment_method';
SHOW COLUMNS FROM donation_history LIKE 'payment_method';
