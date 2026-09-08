ALTER TABLE event_registrations ADD COLUMN IF NOT EXISTS checked_in_at TIMESTAMP;
ALTER TABLE event_registrations ADD COLUMN IF NOT EXISTS confirmation_email_sent_at TIMESTAMP;
ALTER TABLE event_registrations ADD COLUMN IF NOT EXISTS confirmation_email_error VARCHAR(500);
ALTER TABLE membership_applications ADD COLUMN IF NOT EXISTS membership_agreement_version VARCHAR(40);
ALTER TABLE membership_applications ADD COLUMN IF NOT EXISTS membership_agreement_accepted_at TIMESTAMP;
ALTER TABLE membership_applications ADD COLUMN IF NOT EXISTS photography_notice_version VARCHAR(40);
ALTER TABLE membership_applications ADD COLUMN IF NOT EXISTS photography_notice_acknowledged_at TIMESTAMP;
ALTER TABLE membership_applications ADD COLUMN IF NOT EXISTS stripe_checkout_url VARCHAR(1200);
ALTER TABLE membership_applications ADD COLUMN IF NOT EXISTS checkout_created_at TIMESTAMP;
ALTER TABLE membership_applications ADD COLUMN IF NOT EXISTS amount_paid_cents INTEGER;
ALTER TABLE membership_applications ADD COLUMN IF NOT EXISTS welcome_email_sent_at TIMESTAMP;
ALTER TABLE membership_applications ADD COLUMN IF NOT EXISTS welcome_email_error VARCHAR(500);
ALTER TABLE membership_applications ADD COLUMN IF NOT EXISTS stripe_subscription_status VARCHAR(40);
ALTER TABLE membership_applications ADD COLUMN IF NOT EXISTS subscription_cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE membership_applications ADD COLUMN IF NOT EXISTS subscription_cancelled_at TIMESTAMP;
ALTER TABLE membership_applications ADD COLUMN IF NOT EXISTS last_stripe_invoice_id VARCHAR(255);
ALTER TABLE homepage_content ADD COLUMN IF NOT EXISTS newsletter_image_url VARCHAR(255);
ALTER TABLE homepage_content ADD COLUMN IF NOT EXISTS newsletter_image_public_id VARCHAR(255);

CREATE TABLE IF NOT EXISTS contact_page_content (
    id BIGINT PRIMARY KEY,
    content TEXT NOT NULL
);
