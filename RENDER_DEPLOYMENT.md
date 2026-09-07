# Render deployment

This service is deployed as a Docker web service and uses Render PostgreSQL in production.

## Create the PostgreSQL database

Create a Render PostgreSQL database in the same region as the web service. Keep its generated database name, user, and password available for the next step.

## Create the web service

Create a **Web Service**, connect this repository, and select the **Docker** runtime. The repository's `Dockerfile` supplies the build and start commands. Set the health check path to `/actuator/health`, then choose and purchase the desired instance plan.

## Environment variables

Add these variables in the web service's Environment page:

| Variable | Value |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_HOST` | PostgreSQL internal hostname |
| `DB_PORT` | PostgreSQL internal port (normally `5432`) |
| `DB_NAME` | PostgreSQL database name |
| `DB_USER` | PostgreSQL user |
| `DB_PASSWORD` | PostgreSQL password |
| `CORS_ALLOWED_ORIGINS` | Production frontend origin, such as `https://example.com` |
| `FRONTEND_URL` | Production frontend URL, without a trailing slash |
| `MEMBERSHIP_ADMIN_TOKEN` | A random secret of at least 32 characters |
| `SERVICES_ADMIN_TOKEN` | A random secret of at least 32 characters |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary cloud name |
| `CLOUDINARY_API_KEY` | Cloudinary API key |
| `CLOUDINARY_API_SECRET` | Cloudinary API secret |
| `STRIPE_SECRET_KEY` | Stripe secret key |
| `STRIPE_MEMBERSHIP_PRICE_ID` | Stripe membership Price ID |
| `STRIPE_WEBHOOK_SECRET` | Stripe endpoint signing secret |

For multiple browser origins, separate `CORS_ALLOWED_ORIGINS` values with commas. Do not add paths or trailing slashes to origins.

After the first deployment, create a Stripe webhook endpoint at:

`https://YOUR-RENDER-HOST/api/v1/stripe/webhook`

Copy its signing secret into `STRIPE_WEBHOOK_SECRET` and redeploy. Confirm that `https://YOUR-RENDER-HOST/actuator/health` responds with `{"status":"UP"}`.
