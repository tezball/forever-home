# Pre-Launch Issues Report - Forever Home

**Generated:** 2025-12-26
**Reviewed By:** Dev Team (Frontend, Backend, QA, Security, DevOps)

---

## Executive Summary

Comprehensive pre-launch review identified **67+ issues** across all layers of the application. The most critical issues involve **security vulnerabilities** (test mode enabled in production, hardcoded secrets, insecure token storage) and **infrastructure gaps** (single point of failure, database wipe capability).

| Severity | Count | Status |
|----------|-------|--------|
| **CRITICAL** | 8 | MUST FIX |
| **HIGH** | 18 | FIX BEFORE LAUNCH |
| **MEDIUM** | 25 | SHOULD FIX |
| **LOW** | 16+ | NICE TO HAVE |

---

## CRITICAL ISSUES (MUST FIX - Launch Blockers)

### 1. Test Mode Enabled in Production
**Impact:** Complete authentication bypass
**Found By:** Playwright Testing, DevOps Review

**Evidence:**
- Login page shows "Test Mode - DEV ONLY" dropdown with quick login buttons
- `VITE_TEST_MODE=true` in `frontend/.env.production:6`
- `TEST_MODE_ENABLED` defaults to `true` in `application-prod.properties:39`
- `terraform/ecs.tf:255` passes test mode to container

**Fix:**
```bash
# frontend/.env.production
VITE_TEST_MODE=false

# Set in ECS environment or application-prod.properties
TEST_MODE_ENABLED=false
```

---

### 2. JWT Secret Hardcoded in Source Code
**Impact:** Any attacker can forge valid tokens
**Found By:** Security Review, Backend Review

**Files:**
- `src/main/resources/application.properties:21` - Default secret exposed
- `src/main/resources/application-dev.properties:22` - Same secret
- `config/JwtConfig.java:11` - Fallback value in code

**Current (INSECURE):**
```properties
jwt.secret=${JWT_SECRET:mySecretKeyForJWTTokenGenerationWhichMustBeLongEnough256BitsForHS256}
```

**Fix:** Remove fallback, require environment variable:
```properties
jwt.secret=${JWT_SECRET}
```

---

### 3. Tokens Stored in localStorage (XSS Vulnerable)
**Impact:** Any XSS attack can steal user sessions
**Found By:** Security Review

**Files:**
- `frontend/src/contexts/AuthContext.tsx:28-29, 59-61`
- `frontend/src/api/client.ts:13, 29`

**Current:**
```typescript
localStorage.setItem('accessToken', accessToken);
localStorage.setItem('refreshToken', refreshToken);
```

**Fix:** Move tokens to httpOnly cookies set by backend

---

### 4. Database Wipe Capability Active
**Impact:** Production data destruction
**Found By:** DevOps Review

**Files:**
- `terraform/ecs.tf:271-273` - `FLYWAY_CLEAN_ON_START` variable
- `application.properties:16` - Default is `true`

**Fix:**
```properties
# application-prod.properties - explicitly disable
spring.flyway.clean-on-start=false
spring.flyway.clean-disabled=true
```

---

### 5. Adoption Modal Buttons Cut Off (Mobile)
**Impact:** Users cannot complete adoptions on mobile
**Found By:** Playwright Testing

**Evidence:** Screenshot shows modal buttons outside viewport on mobile devices
- `frontend/src/pages/PetDetailPage.tsx` - Modal implementation
- Buttons are not scrollable/accessible

**Fix:** Add proper overflow handling to modal:
```css
.modal-content {
  max-height: 90vh;
  overflow-y: auto;
}
```

---

### 6. Mobile Menu Shows Logged-In State When Not Authenticated
**Impact:** Confusing UX, potential security implications
**Found By:** Playwright Testing

**Evidence:** Mobile hamburger menu shows "Profile", "Settings", "Sign Out" for unauthenticated users

**File:** `frontend/src/components/Header.tsx`

**Fix:** Add authentication check before rendering user menu items

---

### 7. Contact Email is Development Address
**Impact:** Users cannot contact support
**Found By:** Playwright Testing

**Evidence:** Contact page shows `support@foreverhome.local`

**File:** Check contact page component and API response

**Fix:** Update to `support@forever-home.ie`

---

### 8. "Under Construction" Banner Still Visible
**Impact:** Unprofessional, confuses users
**Found By:** Playwright Testing

**Evidence:** Banner appears on all pages in production

**Fix:** Remove or conditionally hide banner for production

---

## HIGH PRIORITY ISSUES (Fix Before Launch)

### Security

| # | Issue | File | Line |
|---|-------|------|------|
| 9 | No CSRF protection | `SecurityConfig.java` | 42 |
| 10 | Weak password validation (8 chars only) | `RegisterRequest.java` | 15 |
| 11 | Google OAuth Client ID hardcoded | `application.properties` | 26 |
| 12 | Missing Google token issuer validation | `GoogleAuthService.java` | 88-92 |
| 13 | Admin bootstrap via env var risk | `application.properties` | 46 |
| 14 | No Content Security Policy header | N/A | - |
| 15 | CORS allows all headers (`*`) | `SecurityConfig.java` | 93 |

### Infrastructure

| # | Issue | File | Line |
|---|-------|------|------|
| 16 | Single NAT Gateway (single point of failure) | `vpc.tf` | 61-71 |
| 17 | S3 `force_destroy=true` | `s3.tf` | 7 |
| 18 | RDS backup only 7 days | `rds.tf` | 127 |
| 19 | ECS single task (no HA) | `ecs.tf` | 322 |
| 20 | No HTTPS enforcement by default | `alb.tf` | 92-124 |

### Backend Performance

| # | Issue | File | Line |
|---|-------|------|------|
| 21 | N+1 query in PublicController (org pet counts) | `PublicController.java` | 56-65 |
| 22 | N+1 query in RescueDashboardController | `RescueDashboardController.java` | 115-118 |
| 23 | Missing pagination on featured pets | `PetController.java` | 63-66 |

### QA/Testing

| # | Issue | File | Line |
|---|-------|------|------|
| 24 | Full adoption lifecycle test SKIPPED | `adoption-lifecycle.spec.ts` | 38 |
| 25 | Auth fixture has race condition (fixed 1s wait) | `auth.fixture.ts` | 29 |
| 26 | 60+ hard-coded waitForTimeout() calls | Multiple E2E files | - |

---

## MEDIUM PRIORITY ISSUES (Should Fix)

### Frontend

| # | Issue | File | Line |
|---|-------|------|------|
| 27 | Console.error statements in production (7 files) | Multiple | - |
| 28 | DOM style manipulation for scroll lock | `SwipeModePage.tsx` | 47-57 |
| 29 | Missing keyboard navigation for swipe cards | `SwipeCard.tsx` | - |
| 30 | Missing image lazy loading | `PetCard.tsx`, `ImageCarousel.tsx` | - |
| 31 | Dropdown lacks proper ARIA roles | `Header.tsx`, `Combobox.tsx` | - |
| 32 | Notification polling every 60s (no WebSocket) | `NotificationBell.tsx` | 22-25 |
| 33 | CSS boilerplate from Vite template | `App.css` | 1-3 |

### Backend

| # | Issue | File | Line |
|---|-------|------|------|
| 34 | Email verification token never expires | `AuthService.java` | 261-266 |
| 35 | Microchip ID not sanitized for S3 path | `PetImageService.java` | 71-74 |
| 36 | Missing rate limiting per user | `RateLimitFilter.java` | - |
| 37 | Pet history endpoint publicly accessible | `PetController.java` | 222-229 |
| 38 | File extension defaults to .jpg | `PetImageService.java` | 86-91 |
| 39 | No audit logging for sensitive operations | Various | - |

### Security

| # | Issue | File | Line |
|---|-------|------|------|
| 40 | User data stored in localStorage | `AuthContext.tsx` | 29, 61 |
| 41 | DevController test accounts exposed | `DevController.java` | 23-24 |
| 42 | No account lockout notification email | `AuthService.java` | 150-154 |
| 43 | Refresh token rotation has timing gap | `AuthService.java` | 215-253 |
| 44 | Timing attack possible on auth endpoints | `AuthService.java` | 274-280 |

### Infrastructure

| # | Issue | File | Line |
|---|-------|------|------|
| 45 | Terraform remote state not configured | `main.tf` | 18-25 |
| 46 | Missing CloudWatch alarms | N/A | - |
| 47 | ECR image tags mutable | `ecr.tf` | 5 |
| 48 | No container resource limits | `ecs.tf` | 191-310 |
| 49 | S3 lifecycle rules too aggressive | `s3.tf` | 46-73 |
| 50 | Missing WAF configuration | N/A | - |

### QA

| # | Issue | File | Line |
|---|-------|------|------|
| 51 | 6 E2E tests skipped (auth timeout) | Multiple | - |
| 52 | 8+ placeholder tests (only check main visible) | `adopter-journey.spec.ts` | 91-172 |
| 53 | No concurrent scenario tests | N/A | - |
| 54 | Missing account lockout test | `auth-flows.spec.ts` | - |
| 55 | Missing token expiry redirect test | N/A | - |

---

## LOW PRIORITY ISSUES (Nice to Have)

| # | Issue | Category |
|---|-------|----------|
| 56 | Add exponential backoff to auth fixture retry | QA |
| 57 | Replace waitForTimeout with waitForSelector | QA |
| 58 | Add VPC Flow Logs | DevOps |
| 59 | Implement secrets rotation | DevOps |
| 60 | Add CloudFront for S3 images | DevOps |
| 61 | Increase RDS backup to 30+ days | DevOps |
| 62 | Add database connection pool config | Backend |
| 63 | Add post-deployment smoke tests | DevOps |
| 64 | Implement structured JSON logging | Backend |
| 65 | Add request/response logging in prod | Backend |
| 66 | Remove Vite CSS boilerplate | Frontend |
| 67 | Add fetchpriority to above-fold images | Frontend |

---

## Quick Wins (Can Fix in < 30 mins)

1. **Set `VITE_TEST_MODE=false`** in `frontend/.env.production`
2. **Set `TEST_MODE_ENABLED=false`** in ECS task definition
3. **Update contact email** to `support@forever-home.ie`
4. **Remove Under Construction banner** or add env toggle
5. **Remove console.error statements** from production code
6. **Set `force_destroy=false`** on S3 bucket
7. **Set `spring.flyway.clean-on-start=false`** in prod properties

---

## Files with Most Issues

| File | Issue Count | Severity |
|------|-------------|----------|
| `application.properties` | 6 | CRITICAL/HIGH |
| `frontend/.env.production` | 3 | CRITICAL |
| `SecurityConfig.java` | 3 | HIGH |
| `AuthService.java` | 5 | MEDIUM-HIGH |
| `adopter-journey.spec.ts` | 20+ | MEDIUM |
| `terraform/ecs.tf` | 5 | HIGH |
| `terraform/s3.tf` | 4 | HIGH |

---

## Recommended Launch Checklist

### Before Production Deploy

- [ ] Set `VITE_TEST_MODE=false`
- [ ] Set `TEST_MODE_ENABLED=false`
- [ ] Set strong `JWT_SECRET` (remove default)
- [ ] Set `FLYWAY_CLEAN_ON_START=false`
- [ ] Update contact email to production address
- [ ] Remove/hide Under Construction banner
- [ ] Enable HTTPS redirect (`create_certificate=true`)
- [ ] Set `force_destroy=false` on S3
- [ ] Increase ECS `desired_count` to 2+
- [ ] Configure CloudWatch alarms

### Security Hardening

- [ ] Move tokens from localStorage to httpOnly cookies
- [ ] Enable CSRF protection
- [ ] Add password complexity requirements
- [ ] Add Content-Security-Policy header
- [ ] Restrict CORS allowed headers
- [ ] Add email verification token expiration

### Performance

- [ ] Fix N+1 queries in PublicController
- [ ] Add pagination to featured pets endpoint
- [ ] Add image lazy loading

### Testing

- [ ] Enable and fix skipped adoption lifecycle test
- [ ] Replace waitForTimeout with proper waits
- [ ] Add concurrent scenario tests

---

## Notes

- Production URL: https://forever-home.ie
- All line numbers reference current codebase as of 2025-12-26
- Issues discovered via Playwright browser testing and static code analysis
