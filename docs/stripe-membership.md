# Stripe annual membership backend

The backend owns the Stripe Price ID and creates hosted Checkout Sessions. The browser never sends an amount and never activates a membership.

## Required environment variables

```text
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_MEMBERSHIP_PRICE_ID=price_...
FRONTEND_URL=http://localhost:3000
```

Create a one-time `$199 USD` Price in Stripe and assign its `price_...` identifier to `STRIPE_MEMBERSHIP_PRICE_ID`. Renewals are not automatic; the admin member directory provides a manual reminder action before the recorded membership end date.

## API flow

1. `POST /api/v1/membership-applications` stores the completed membership application and returns `applicationId`.
2. `POST /api/v1/membership-applications/{applicationId}/checkout-session` creates a Stripe-hosted one-time payment Checkout Session and returns `checkoutUrl`.
3. Redirect the browser to `checkoutUrl`.
4. Stripe sends payment events to `POST /api/v1/stripe/webhook`.
5. `GET /api/v1/membership-applications/{applicationId}/status` returns a safe status for the confirmation page.

The success redirect is not proof of payment. Only a verified Stripe webhook changes membership status to `ACTIVE`.

## Webhook events

Configure the Stripe endpoint to send:

- `checkout.session.completed`
- `checkout.session.async_payment_succeeded`
- `checkout.session.async_payment_failed`
- `payment_intent.payment_failed`
- `charge.refunded`

Webhook event IDs are stored in `processed_stripe_events`, so Stripe retries are idempotent.

## Local webhook verification

With the backend running on port 8080 and the Stripe CLI authenticated:

```bash
stripe listen --forward-to localhost:8080/api/v1/stripe/webhook
```

Copy the displayed `whsec_...` value into `STRIPE_WEBHOOK_SECRET`, restart the backend, then trigger test events from Stripe. Use Stripe test mode until the complete application, Checkout, webhook, and confirmation flow has been verified.
