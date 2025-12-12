# Forever Home - PM Snag List

**Date:** 2025-12-11
**Reviewer:** PM Review via E2E Testing
**Environment:** http://forever-home-dev-alb-1055924695.eu-west-1.elb.amazonaws.com/

---

## Critical Issues (P0)

| # | Issue | Description | Steps to Reproduce | Expected | Actual | Status |
|---|-------|-------------|-------------------|----------|--------|--------|
| 1 | **403 on /login when authenticated** | Logged-in users navigating to /login see a Whitelabel Error Page | 1. Log in as any user 2. Navigate directly to /login | Redirect to homepage or dashboard | Shows "Whitelabel Error Page" with 403 Forbidden | ✅ FIXED |
| 2 | **403 on /contact page** | Contact Us page returns 403 Forbidden for all users | Click "Contact Us" in footer | Contact page loads | 403 Forbidden error | ✅ FIXED |
| 3 | **403 on /privacy page** | Privacy Policy page returns 403 Forbidden for all users | Click "Privacy Policy" in footer | Privacy policy loads | 403 Forbidden error | ✅ FIXED |
| 4 | **403 on /help page** | Help Center page returns 403 Forbidden for all users | Click "Help Center" in footer | Help page loads | 403 Forbidden error | ✅ FIXED |

**Fix:** Updated `SecurityConfig.java` to permit all SPA frontend routes (login, register, contact, privacy, help, pets, rescues, profile, settings, notifications, and role-specific dashboard routes).

## High Priority Issues (P1)

| # | Issue | Description | Steps to Reproduce | Expected | Actual | Status |
|---|-------|-------------|-------------------|----------|--------|--------|
| 5 | **Rescues page shows empty** | No rescue organizations appear on /rescues page | Navigate to /rescues | List of verified rescue organizations | "No rescue organizations found" message | ✅ FIXED (V21) |
| 6 | **Stats show 0 Rescue Partners** | Homepage stats counter shows 0 rescues even though rescue org exists | View homepage | Accurate count of verified rescue partners | Shows "0 Rescue Partners" | ✅ FIXED (V21) |
| 7 | **Grafana link points to localhost** | Admin dashboard Grafana link uses localhost:3000 | Login as Admin → Dashboard → Click "Grafana" | Opens Grafana in production environment | Links to http://localhost:3000/d/forever-home-admin-analytics (won't work) | ✅ FIXED |

**Fix for #5 & #6:** Created migration `V21__verify_test_rescue_organization.sql` and updated `TestDataSeeder.java` to set `verified = true` for rescue organizations.

**Fix for #7:** Made Grafana URL configurable via `VITE_GRAFANA_URL` environment variable. Link is now hidden in production when not configured.

## Medium Priority Issues (P2)

| # | Issue | Description | Steps to Reproduce | Expected | Actual | Status |
|---|-------|-------------|-------------------|----------|--------|--------|
| 8 | **No action buttons for PENDING users** | In User Management, users with PENDING status have no action buttons | Login as Admin → Dashboard → User Management | Ability to activate/reject pending users | No "Activate" or "Reject" buttons shown | ✅ FIXED |
| 9 | **User menu dropdown sometimes doesn't appear** | Clicking user avatar doesn't always show dropdown menu | Click user avatar button in header | Dropdown menu appears | Sometimes requires multiple clicks | Open |

**Fix for #8:** Added "Activate" and "Reject" buttons for PENDING users in `AdminDashboard.tsx`.

## Low Priority Issues (P3)

| # | Issue | Description | Steps to Reproduce | Expected | Actual | Status |
|---|-------|-------------|-------------------|----------|--------|--------|
| 10 | **All pets show "No photos" placeholder** | Every pet displays placeholder instead of actual images | Browse any pet listing | Pet photos displayed | All show generic placeholder with "No photos" label | Expected (no images uploaded) |
| 11 | **Test mode visible in production** | "DEV ONLY" test mode login is visible on login page | Navigate to /login | Only manual login form | Test mode quick login dropdown visible | By Design (dev env) |

## UX Improvements (P4)

| # | Issue | Description | Recommendation | Status |
|---|-------|-------------|----------------|--------|
| 12 | **Footer links to non-existent pages** | Footer contains links to /help, /contact, /privacy that don't work | Either implement these pages or remove links from footer | ✅ FIXED (routes now permitted) |
| 13 | **No notification indicator** | Notification bell doesn't show count/badge | Add notification count badge when there are unread notifications | Open |
| 14 | **Missing "Get Started" button when logged in** | Homepage shows "Get Started" CTA even for logged-in users | Replace with role-appropriate CTA (e.g., "Browse Pets" or "Go to Dashboard") | Open |

---

## Summary

**Total Issues Found: 14**
- Critical (P0): 4 → **4 FIXED**
- High (P1): 3 → **3 FIXED**
- Medium (P2): 2 → **1 FIXED**, 1 Open
- Low (P3): 2 → Expected behavior / By Design
- UX Improvements (P4): 3 → **1 FIXED**, 2 Open

## Fixed Issues Summary

| File | Changes |
|------|---------|
| `SecurityConfig.java` | Added SPA frontend routes to permitAll() |
| `TestDataSeeder.java` | Set `verified = true` for rescue org (lines 260, 323) |
| `V21__verify_test_rescue_organization.sql` | Migration to fix existing data |
| `AdminDashboard.tsx` | Added Activate/Reject buttons for PENDING users, made Grafana link configurable |
| `.env.development` | Added `VITE_GRAFANA_URL` config |
| `.env.production` | Added `VITE_GRAFANA_URL` config (empty by default) |
| `terraform/s3.tf` | Enabled public read access for pet images (disabled public access blocking, added bucket policy) |

## Remaining Open Issues

- **#9**: User menu dropdown intermittent - may be a race condition in React state
- **#13**: No notification count badge
- **#14**: "Get Started" CTA shown when logged in

## S3 Image Storage Verification

**Status:** ✅ CONFIRMED WORKING

S3 image storage has been tested and verified to work correctly:

| Test | Result |
|------|--------|
| Image Upload | ✅ Successfully uploads to S3 bucket `forever-home-dev-images-0d0eb6cd` |
| Image Retrieval | ✅ Images publicly accessible via S3 URL |
| Server-side Encryption | ✅ AES256 encryption enabled |
| CORS Configuration | ✅ Configured for frontend access |
| Lifecycle Rules | ✅ Transition to STANDARD_IA after 90 days, cleanup of old versions |

**Test Results:**
- Upload test: Image uploaded successfully, received S3 URL in response
- Retrieval test: HTTP 200, Content-Type: image/png, publicly accessible
- Sample URL: `https://forever-home-dev-images-0d0eb6cd.s3.amazonaws.com/pets/{petId}/{imageId}.png`

**Infrastructure Changes Applied:**
- Updated `terraform/s3.tf` to enable public read access for images
- Added `PublicReadAccess` policy statement to bucket policy
- Disabled public access blocking to allow bucket policy

---

## Notes

- Issues #5 and #6 require deployment of V21 migration to take effect
- Issue #11 (test mode visible) is controlled by `VITE_TEST_MODE` env variable - intentionally enabled for dev environment
- Issue #10 (no pet photos) is expected since no actual images have been uploaded to S3 - **S3 storage now confirmed working**
