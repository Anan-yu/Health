-- Repair city snapshots that were accidentally encoded as UTF-8 twice.
-- The conversion is guarded by common mojibake markers so valid Chinese text is untouched.

UPDATE gold_member_account
SET city = CONVERT(CAST(CONVERT(city USING latin1) AS BINARY) USING utf8mb4)
WHERE city IS NOT NULL
  AND city REGEXP '[ÃÂâåæçèéêëìíîïñòóôõöùúûüýþÿ]';

UPDATE gold_member_order
SET registration_city = CONVERT(CAST(CONVERT(registration_city USING latin1) AS BINARY) USING utf8mb4)
WHERE registration_city IS NOT NULL
  AND registration_city REGEXP '[ÃÂâåæçèéêëìíîïñòóôõöùúûüýþÿ]';

UPDATE gold_member_trade_listing
SET region_city = CONVERT(CAST(CONVERT(region_city USING latin1) AS BINARY) USING utf8mb4)
WHERE region_city IS NOT NULL
  AND region_city REGEXP '[ÃÂâåæçèéêëìíîïñòóôõöùúûüýþÿ]';
