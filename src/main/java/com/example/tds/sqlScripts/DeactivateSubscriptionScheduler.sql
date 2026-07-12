SELECT cron.schedule(
   'deactivate_expired_subscriptions',
   '0 0 * * *',
    $$
        UPDATE subscriptions
        SET is_active = FALSE
        WHERE end_date::date < CURRENT_DATE;
    $$
);