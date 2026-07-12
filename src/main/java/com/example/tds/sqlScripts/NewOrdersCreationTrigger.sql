CREATE OR REPLACE FUNCTION create_new_orders()
    RETURNS TRIGGER AS $$
BEGIN

    INSERT INTO trigger_logs(
        event_name,
        status
    )
    VALUES(
              'subscription_activated',
              'TRIGGER_EXECUTED'
          );

    PERFORM pg_notify(
            'new_orders_channel',
            NEW.subscription_id::text
            );

    RETURN NEW;

END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER new_orders_trigger
    AFTER UPDATE ON subscriptions
    FOR EACH ROW
    WHEN (NEW.is_active = TRUE and OLD.is_active = FALSE)
EXECUTE FUNCTION create_new_orders();
