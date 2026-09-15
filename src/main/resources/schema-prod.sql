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

ALTER TABLE newsletter_subscribers ADD COLUMN IF NOT EXISTS source VARCHAR(120);
ALTER TABLE newsletter_subscribers ADD COLUMN IF NOT EXISTS resend_contact_id VARCHAR(100);
ALTER TABLE newsletter_subscribers ADD COLUMN IF NOT EXISTS resend_sync_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE newsletter_subscribers ADD COLUMN IF NOT EXISTS resend_sync_error VARCHAR(500);
ALTER TABLE newsletter_subscribers ADD COLUMN IF NOT EXISTS welcome_email_sent_at TIMESTAMP;
ALTER TABLE newsletter_subscribers ADD COLUMN IF NOT EXISTS welcome_email_error VARCHAR(500);
CREATE INDEX IF NOT EXISTS idx_newsletter_status ON newsletter_subscribers(status);
CREATE INDEX IF NOT EXISTS idx_newsletter_subscribed_at ON newsletter_subscribers(subscribed_at);

CREATE TABLE IF NOT EXISTS newsletter_webhook_events (
    event_id VARCHAR(255) PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS contact_page_content (
    id BIGINT PRIMARY KEY,
    content TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS admin_credentials (
    id BIGINT PRIMARY KEY,
    username VARCHAR(254) NOT NULL UNIQUE,
    display_name VARCHAR(160) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_sign_in_at TIMESTAMP,
    reset_token_hash VARCHAR(64),
    reset_token_expires_at TIMESTAMP,
    password_changed_at TIMESTAMP
);
ALTER TABLE admin_credentials ADD COLUMN IF NOT EXISTS display_name VARCHAR(160);
ALTER TABLE admin_credentials ADD COLUMN IF NOT EXISTS email VARCHAR(254);
ALTER TABLE admin_credentials ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE admin_credentials ADD COLUMN IF NOT EXISTS last_sign_in_at TIMESTAMP;
