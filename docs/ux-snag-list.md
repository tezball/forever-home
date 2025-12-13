# Forever Home - UX Snag List

> **Reviewed:** December 13, 2025
> **Reviewer:** PM/UX Review
> **Focus:** User flow optimization - reducing steps per task

---

## Critical Issues (Bugs)

### 1. Profile Page Returns 404
- **Location:** `/profile` (linked from user dropdown menu)
- **Impact:** HIGH - Users cannot view/edit their profile
- **Fix:** Implement profile page or remove link from navigation

### 2. User Menu Stays Open After Navigating
- **Location:** Global navigation dropdown
- **Impact:** MEDIUM - Menu overlay blocks content and buttons, causes click failures
- **Fix:** Close dropdown menu when clicking anywhere outside or after navigation

---

## Flow Optimization by User Role

### Adopter Dashboard

| Issue | Current State | Recommended Improvement |
|-------|--------------|------------------------|
| Empty dashboard | Only shows "Saved Pets" section with no guidance | Add quick actions: "Browse Pets", "View Applications", show application status if any exist |
| No application tracking | Users can't see their submitted applications | Add "My Applications" section showing status of all submitted applications |
| No pet recommendations | Empty state just says "browse pets" | Show personalized pet recommendations based on profile/preferences |

**Current flow to adopt:**
1. Browse Pets
2. Click pet
3. Click "Apply to Adopt"
4. Fill optional message
5. Submit

**Steps:** 5 (acceptable, but no visibility into application status afterward)

---

### Foster Dashboard

| Issue | Current State | Recommended Improvement |
|-------|--------------|------------------------|
| Two-step pet submission | Must create pet first, then go to separate "Submit for Review" page | Combine into single flow - allow rescue selection during pet creation |
| Photo upload delayed | "You'll be able to upload photos after saving" | Allow photo upload during initial creation to reduce steps |
| No status tracking | Only shows "Drafts" section | Add sections for "Pending Review", "With Rescue", "Pending Vet", "Available" to show full pipeline |
| Edit/Submit/Withdraw in same row | Action buttons cramped on pet cards | Consider a more prominent "Continue" CTA for drafts |

**Current flow to register pet:**
1. Click "Register a Pet"
2. Fill form (name, species, breed, sex, age, size, microchip, description, health notes)
3. Click "Create Pet"
4. Navigate back to dashboard
5. Click "Submit for Review"
6. Select rescue organization
7. Submit

**Steps:** 7 (could be reduced to 4-5 by combining creation + submission)

---

### Vet Dashboard

| Issue | Current State | Recommended Improvement |
|-------|--------------|------------------------|
| Manual microchip entry | Vet must type microchip ID to find pets | Add "Pending Pets" queue showing pets awaiting vet sign-off from approved rescues |
| No pending work visible | Dashboard shows approvals and lookup only | Show count of pets waiting for sign-off upfront |
| Separate approval request page | Must navigate away to request rescue approvals | Inline approval request or modal to reduce navigation |

**Current flow to sign off on pet:**
1. Go to Dashboard
2. Type microchip ID in lookup
3. Click Search
4. Review pet details
5. Complete sign-off form

**Steps:** 5 (acceptable, but step 2 is friction - vet shouldn't need to know microchip ID)

**Recommended flow:**
1. Go to Dashboard
2. See "Pending Sign-offs" queue
3. Click pet
4. Complete sign-off

**Steps:** 4 (removes manual lookup friction)

---

### Rescue Organization Dashboard

| Issue | Current State | Recommended Improvement |
|-------|--------------|------------------------|
| Accept/Decline inline only | Quick actions but no details visible | Add expandable pet details or quick-view modal before accepting |
| No batch operations | Must handle one pet at a time | Add bulk accept/decline for efficiency |
| Separate vet management page | "Manage Vets" is on another page | Consider inline vet approval section or at least show pending vet requests count |

**Current flow to approve foster pet:**
1. Go to Dashboard
2. See "Pending Review" section
3. Click Accept or Decline

**Steps:** 3 (good, but lacks detail before decision)

---

### Admin Dashboard

| Issue | Current State | Recommended Improvement |
|-------|--------------|------------------------|
| Sparse main view | Only tabs for approvals and user management | Add key metrics/alerts visible immediately (pending approvals, flagged content) |
| Hidden analytics | Requires navigation to separate page | Surface key stats on dashboard overview |

---

## Global UX Improvements

### Navigation & Information Architecture

1. **Contextual "Back to Dashboard" navigation is inconsistent**
   - Some pages have it, others don't
   - Standardize secondary navigation across all sub-pages

2. **No breadcrumbs**
   - Deep pages lack context of where user is in the app
   - Add breadcrumbs for pages beyond dashboard level

3. **Dashboard link goes to role-specific dashboard**
   - Good - this works correctly for each user type

### Empty States

4. **Empty states lack actionable guidance**
   - Vet history: "No sign-offs yet" - Add link to lookup or pending pets
   - Adopter saved pets: Could suggest popular pets
   - Notifications: Good empty state, but could suggest enabling notifications

### Form UX

5. **Pet registration form is long**
   - Single long form could be broken into steps (wizard)
   - Or use progressive disclosure (show more fields after basics filled)

6. **Required field marking inconsistent**
   - Some fields marked with `*`, validation not always clear
   - Add inline validation and clearer error states

### Pet Detail Page

7. **Limited pet information visible**
   - Only shows basic stats (age, size, sex)
   - Missing: vaccination status, good with kids/pets, rescue org info, location

8. **Single photo view**
   - Gallery exists but could be more prominent
   - Add image carousel indicators

### Adoption Application

9. **Minimal application form**
   - Only optional text field for "why adopt"
   - Consider: pre-filled adopter profile info, specific questions about living situation

10. **No application preview/confirmation**
    - One-click submit with no review step
    - Add confirmation screen showing what will be submitted

---

## Quick Wins (Low Effort, High Impact)

| Priority | Issue | Effort | Impact |
|----------|-------|--------|--------|
| 1 | Fix Profile 404 | Low | High |
| 2 | Close menu on outside click | Low | High |
| 3 | Add "My Applications" to Adopter dashboard | Medium | High |
| 4 | Show pending pet count on Vet dashboard | Low | Medium |
| 5 | Add pet status sections to Foster dashboard | Medium | High |

---

## Summary Statistics

| Category | Count |
|----------|-------|
| Critical Bugs | 2 |
| Dashboard Improvements | 12 |
| Global UX Issues | 10 |
| Quick Wins Identified | 5 |

---

## Appendix: Screenshots

Screenshots captured during review are available in:
`.playwright-mcp/` directory:
- `homepage.png`
- `login-page.png`
- `register-page.png`
- `adopter-dashboard.png`
- `foster-dashboard.png`
- `foster-register-pet.png`
- `foster-submit-review.png`
- `vet-dashboard.png`
- `vet-approvals.png`
- `vet-history.png`
- `rescue-dashboard.png`
- `rescue-manage-vets.png`
- `admin-dashboard.png`
- `admin-analytics.png`
- `browse-pets.png`
- `pet-detail.png`
- `adoption-application-modal.png`
- `settings-page.png`
- `notifications-page.png`
- `profile-404.png`
