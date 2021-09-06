-- # SELECT day,
-- #        COUNT(DISTINCT user_id)                                                            AS total_uv,
-- #        COUNT(DISTINCT CASE WHEN flag IN ('android', 'iphone') THEN user_id ELSE NULL END) AS app_uv,
-- #        COUNT(DISTINCT CASE WHEN flag IN ('wap', 'other') THEN user_id ELSE NULL END)      AS web_uv
-- # FROM T
-- # GROUP BY day;


-- SELECT day,
--        COUNT(DISTINCT user_id)                                                 AS total_uv,
--        COUNT(DISTINCT user_id)    FILTER (WHERE flag IN ('android', 'iphone')) AS app_uv,
--        COUNT(DISTINCT user_id)    FILTER (WHERE flag IN ('wap', 'other'))      AS web_uv
-- FROM T
-- GROUP BY day;
