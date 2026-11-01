-- STEP 3: convert donation money columns from DOUBLE to DECIMAL(12,2)
-- Run with the application stopped, after a verified database backup.
-- Run this after step2-payment-method.sql.

-- Preflight: identify NULL values. They are handled below by converting to 0.00.
SELECT 'donation_records.amount' AS column_name, COUNT(*) AS null_rows
FROM donation_records
WHERE amount IS NULL
UNION ALL
SELECT 'donation_history.amount' AS column_name, COUNT(*) AS null_rows
FROM donation_history
WHERE amount IS NULL
UNION ALL
SELECT 'donation_goals.target_amount' AS column_name, COUNT(*) AS null_rows
FROM donation_goals
WHERE target_amount IS NULL
UNION ALL
SELECT 'donation_goals.current_amount' AS column_name, COUNT(*) AS null_rows
FROM donation_goals
WHERE current_amount IS NULL;

-- Preflight: review values that cannot be represented safely or exactly.
-- Stop and correct these rows before running the ALTER statements.
SELECT 'donation_records.amount' AS column_name, record_id AS row_id, amount AS value
FROM donation_records
WHERE amount IS NOT NULL
  AND (ABS(amount - ROUND(amount, 2)) > 0.0000001
       OR amount > 9999999999.99
       OR amount < 0)
UNION ALL
SELECT 'donation_history.amount' AS column_name, id AS row_id, amount AS value
FROM donation_history
WHERE amount IS NOT NULL
  AND (ABS(amount - ROUND(amount, 2)) > 0.0000001
       OR amount > 9999999999.99
       OR amount < 0)
UNION ALL
SELECT 'donation_goals.target_amount' AS column_name, id AS row_id, target_amount AS value
FROM donation_goals
WHERE target_amount IS NOT NULL
  AND (ABS(target_amount - ROUND(target_amount, 2)) > 0.0000001
       OR target_amount > 9999999999.99
       OR target_amount < 0)
UNION ALL
SELECT 'donation_goals.current_amount' AS column_name, id AS row_id, current_amount AS value
FROM donation_goals
WHERE current_amount IS NOT NULL
  AND (ABS(current_amount - ROUND(current_amount, 2)) > 0.0000001
       OR current_amount > 9999999999.99
       OR current_amount < 0)
ORDER BY column_name, row_id;

-- Handle NULLs before applying NOT NULL and DEFAULT 0.00.
UPDATE donation_records SET amount = 0.00 WHERE amount IS NULL;
UPDATE donation_history SET amount = 0.00 WHERE amount IS NULL;
UPDATE donation_goals SET target_amount = 0.00 WHERE target_amount IS NULL;
UPDATE donation_goals SET current_amount = 0.00 WHERE current_amount IS NULL;

-- Capture totals before conversion for the verification query.
DROP TEMPORARY TABLE IF EXISTS _money_sums_before;
CREATE TEMPORARY TABLE _money_sums_before AS
SELECT
    (SELECT CAST(COALESCE(SUM(amount), 0) AS DECIMAL(20,2)) FROM donation_records) AS records_amount,
    (SELECT CAST(COALESCE(SUM(amount), 0) AS DECIMAL(20,2)) FROM donation_history) AS history_amount,
    (SELECT CAST(COALESCE(SUM(target_amount), 0) AS DECIMAL(20,2)) FROM donation_goals) AS target_amount,
    (SELECT CAST(COALESCE(SUM(current_amount), 0) AS DECIMAL(20,2)) FROM donation_goals) AS current_amount;

ALTER TABLE donation_records
    MODIFY COLUMN amount DECIMAL(12,2) NOT NULL DEFAULT 0.00;

ALTER TABLE donation_history
    MODIFY COLUMN amount DECIMAL(12,2) NOT NULL DEFAULT 0.00;

ALTER TABLE donation_goals
    MODIFY COLUMN target_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00;

ALTER TABLE donation_goals
    MODIFY COLUMN current_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00;

-- Verification: compare the four SUM values before and after conversion.
SELECT
    b.records_amount AS records_before,
    COALESCE((SELECT SUM(amount) FROM donation_records), 0.00) AS records_after,
    b.records_amount - COALESCE((SELECT SUM(amount) FROM donation_records), 0.00) AS records_difference,
    b.history_amount AS history_before,
    COALESCE((SELECT SUM(amount) FROM donation_history), 0.00) AS history_after,
    b.history_amount - COALESCE((SELECT SUM(amount) FROM donation_history), 0.00) AS history_difference,
    b.target_amount AS target_before,
    COALESCE((SELECT SUM(target_amount) FROM donation_goals), 0.00) AS target_after,
    b.target_amount - COALESCE((SELECT SUM(target_amount) FROM donation_goals), 0.00) AS target_difference,
    b.current_amount AS current_before,
    COALESCE((SELECT SUM(current_amount) FROM donation_goals), 0.00) AS current_after,
    b.current_amount - COALESCE((SELECT SUM(current_amount) FROM donation_goals), 0.00) AS current_difference
FROM _money_sums_before b;

SHOW COLUMNS FROM donation_records LIKE 'amount';
SHOW COLUMNS FROM donation_history LIKE 'amount';
SHOW COLUMNS FROM donation_goals LIKE 'target_amount';
SHOW COLUMNS FROM donation_goals LIKE 'current_amount';

DROP TEMPORARY TABLE IF EXISTS _money_sums_before;
