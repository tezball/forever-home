# Forever Home - PM Snag List

**Last Updated:** December 14, 2025
**Reviewer:** PM Review via E2E Testing
**Environment:** http://localhost:8080 (Local Development)

---

## Current Issues Summary

| Priority | Total | Fixed | Open |
|----------|-------|-------|------|
| Critical (P0) | 5 | 4 | **1** |
| High (P1) | 5 | 3 | **2** |
| Medium (P2) | 4 | 1 | **3** |
| Low (P3) | 3 | 0 | 0 (Expected/By Design) |
| UX (P4) | 3 | 1 | **2** |

---

## Critical Issues (P0)

| # | Issue | Description | Steps to Reproduce | Expected | Actual | Status |
|---|-------|-------------|-------------------|----------|--------|--------|
| 1 | **403 on /login when authenticated** | Logged-in users navigating to /login see a Whitelabel Error Page | 1. Log in as any user 2. Navigate directly to /login | Redirect to homepage or dashboard | Shows "Whitelabel Error Page" with 403 Forbidden | ✅ FIXED |
| 2 | **403 on /contact page** | Contact Us page returns 403 Forbidden for all users | Click "Contact Us" in footer | Contact page loads | 403 Forbidden error | ✅ FIXED |
| 3 | **403 on /privacy page** | Privacy Policy page returns 403 Forbidden for all users | Click "Privacy Policy" in footer | Privacy policy loads | 403 Forbidden error | ✅ FIXED |
| 4 | **403 on /help page** | Help Center page returns 403 Forbidden for all users | Click "Help Center" in footer | Help page loads | 403 Forbidden error | ✅ FIXED |
| **15** | **Adopted pet shows in "Ready to Finalize"** | Oliver is ADOPTED but application status is still APPROVED, causing it to appear in "Ready to Finalize" section | Login as Rescue → Dashboard → View "Ready to Finalize" | Only IN_PROGRESS adoptions shown | Shows 3 including already-adopted Oliver | **🔴 OPEN** |

**Details on Issue #15:**
```sql
-- Database evidence of mismatch:
app_status | pet_name | pet_status
-----------+----------+-------------
 APPROVED  | Oliver   | ADOPTED     -- BUG: Should be FINALIZED
 APPROVED  | Daisy    | IN_PROGRESS -- Correct
 APPROVED  | Charlie  | IN_PROGRESS -- Correct
```

**Root Cause:** When a pet is marked as `ADOPTED`, the corresponding `adoption_applications` record is not updated to `FINALIZED` status.

**Fix Required:**
1. Update adoption finalization logic to sync application status with pet status
2. Add migration to fix existing inconsistent data
3. Consider adding database trigger or application validation to prevent future mismatches

---

## High Priority Issues (P1)

| # | Issue | Description | Steps to Reproduce | Expected | Actual | Status |
|---|-------|-------------|-------------------|----------|--------|--------|
| 5 | **Rescues page shows empty** | No rescue organizations appear on /rescues page | Navigate to /rescues | List of verified rescue organizations | "No rescue organizations found" message | ✅ FIXED (V21) |
| 6 | **Stats show 0 Rescue Partners** | Homepage stats counter shows 0 rescues even though rescue org exists | View homepage | Accurate count of verified rescue partners | Shows "0 Rescue Partners" | ✅ FIXED (V21) |
| 7 | **Grafana link points to localhost** | Admin dashboard Grafana link uses localhost:3000 | Login as Admin → Dashboard → Click "Grafana" | Opens Grafana in production environment | Links to http://localhost:3000 (won't work) | ✅ FIXED |
| **16** | **Adopter sees "Approved" for completed adoption** | Adopter Dashboard shows Oliver as "Approved" but pet is already ADOPTED | Login as Adopter → Dashboard → View Applications | Clear indication adoption is complete | Shows misleading "Approved" status | **🔴 OPEN** |
| **17** | **"In Progress" counter vs "Ready to Finalize" mismatch** | Rescue Dashboard shows "In Progress: 2" but "Ready to Finalize: 3" | Login as Rescue → Dashboard | Counts should be consistent or clearly differentiated | Counter shows 2, section shows 3 items | **🔴 OPEN** |

---

## Medium Priority Issues (P2)

| # | Issue | Description | Steps to Reproduce | Expected | Actual | Status |
|---|-------|-------------|-------------------|----------|--------|--------|
| 8 | **No action buttons for PENDING users** | In User Management, users with PENDING status have no action buttons | Login as Admin → Dashboard → User Management | Ability to activate/reject pending users | No "Activate" or "Reject" buttons shown | ✅ FIXED |
| 9 | **User menu dropdown sometimes doesn't appear** | Clicking user avatar doesn't always show dropdown menu | Click user avatar button in header | Dropdown menu appears | Sometimes requires multiple clicks | **🟡 OPEN** |
| **18** | **Pet status history always empty** | Pet detail pages show "No status history available" for all pets | View any pet detail page → Status History section | Shows pet's journey (Draft → Pending → Available) | Always shows "No status history available" | **🟡 OPEN** |
| **19** | **Vet onboarding UX could be clearer** | Vets see empty dashboard with no guidance until approved by rescue | Login as Vet → Dashboard | Clear onboarding explaining the approval workflow | Just shows empty state messages | **🟡 OPEN** |

---

## Low Priority Issues (P3)

| # | Issue | Description | Steps to Reproduce | Expected | Actual | Status |
|---|-------|-------------|-------------------|----------|--------|--------|
| 10 | **All pets show "No photos" placeholder** | Every pet displays placeholder instead of actual images | Browse any pet listing | Pet photos displayed | All show generic placeholder | ⚪ Expected (no images uploaded in test data) |
| 11 | **Test mode visible in production** | "DEV ONLY" test mode login is visible on login page | Navigate to /login | Only manual login form | Test mode quick login dropdown visible | ⚪ By Design (dev env) |
| **20** | **Quick Login button text generic before selection** | Button says "Login as Test User" before dropdown selection | Navigate to /login | Clearer disabled state | Minor polish issue | ⚪ Low Priority |

---

## UX Improvements (P4)

| # | Issue | Description | Recommendation | Status |
|---|-------|-------------|----------------|--------|
| 12 | **Footer links to non-existent pages** | Footer contains links to /help, /contact, /privacy that don't work | Either implement these pages or remove links from footer | ✅ FIXED |
| 13 | **No notification indicator** | Notification bell doesn't show count/badge | Add notification count badge when there are unread notifications | **🟡 OPEN** |
| 14 | **Missing "Get Started" button when logged in** | Homepage shows "Get Started" CTA even for logged-in users | Replace with role-appropriate CTA | **🟡 OPEN** |

---

## Statistics Verification (December 14, 2025)

All counters verified against database:

| Metric | Homepage | Admin Dashboard | Database | Status |
|--------|----------|-----------------|----------|--------|
| Pets Available | 2 | 2 | 2 | ✅ Correct |
| Total Adoptions | 1 | 1 | 1 | ✅ Correct |
| Total Rescues | 1 | - | 1 | ✅ Correct |
| Total Users | - | 9 | 9 | ✅ Correct |
| Total Pets | - | 10 | 10 | ✅ Correct |
| Pending Vet | - | 2 | 2 | ✅ Correct |

### Database Pet Status Distribution
| Status | Count | Notes |
|--------|-------|-------|
| ADOPTED | 1 | Oliver |
| AVAILABLE | 2 | Duke, Whiskers |
| DRAFT | 1 | Bella (Foster's pet) |
| IN_PROGRESS | 2 | Daisy, Charlie |
| PENDING_RESCUE | 2 | Shadow, Max |
| PENDING_VET | 2 | Luna, Rocky |

### User Distribution by Role
| Role | Count |
|------|-------|
| ADMIN | 1 |
| ADOPTER | 1 |
| FOSTER | 5 |
| RESCUE_ORG | 1 |
| VET | 1 |

---

## Verified Working Features

### Core Features ✅
- [x] Homepage with statistics and featured pets
- [x] Browse Pets with filtering (species, size, gender, search)
- [x] Pet detail pages with images and information
- [x] Rescue organization listing with profile links
- [x] FAQ page with comprehensive adoption information
- [x] Help Center, Contact, Privacy, About pages

### Authentication ✅
- [x] Quick login for all 5 test accounts
- [x] Sign out functionality
- [x] Role-based navigation adapts correctly
- [x] Role-based CTAs on homepage

### Role-Specific Dashboards ✅
- [x] **Foster:** Shows drafts (1), submit/edit/withdraw actions
- [x] **Rescue:** Pending review (2), available (2), in-progress (2), vet requests (0)
- [x] **Vet:** Approval request flow, microchip lookup, security enforcement
- [x] **Adopter:** Applications list, saved pets feature
- [x] **Admin:** User management, pet counts, approval workflows

### Security ✅
- [x] Vet cannot access pets from unapproved rescues (403 correctly returned)
- [x] Role-based route protection working

---

## Previously Fixed Issues

| File | Changes |
|------|---------|
| `SecurityConfig.java` | Added SPA frontend routes to permitAll() |
| `TestDataSeeder.java` | Set `verified = true` for rescue org |
| `V21__verify_test_rescue_organization.sql` | Migration to fix existing data |
| `AdminDashboard.tsx` | Added Activate/Reject buttons, configurable Grafana link |
| `.env.development` / `.env.production` | Added `VITE_GRAFANA_URL` config |
| `terraform/s3.tf` | Enabled public read access for pet images |

---

## Recommended Next Steps

### Immediate (Before Next Release)
1. **Fix Issue #15:** Update adoption finalization to sync application status
2. **Data Migration:** Fix Oliver's application status from APPROVED → FINALIZED

### Short Term
3. **Fix Issue #18:** Verify pet_status_history table is being populated on status changes
4. **Fix Issue #16/17:** Add "Completed Adoptions" section to dashboards
5. **Add Integration Tests:** Cover adoption finalization flow end-to-end

### Medium Term
6. **Improve Vet Onboarding:** Add clearer messaging about approval workflow
7. **Add Notification Badges:** Show unread notification count
8. **Homepage CTA Logic:** Customize based on user role

---

*Last reviewed: December 14, 2025*
