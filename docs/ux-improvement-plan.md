# UX Improvement Plan

This document tracks UX improvements identified during the PM/UX review of forever-home.ie. The goal is to reduce clicks, minimize dashboard noise, and surface meaningful content for each user type.

---

## Phase 1: Quick Wins (High Impact, Low Effort)

### 1.1 Hide Marketing Sections for Authenticated Users
- [ ] **Homepage: Hide "Join Our Community" section** when user is logged in
  - File: `src/main/resources/static/js/pages/home.js` or equivalent React component
  - Condition: Check authentication state, conditionally render section

- [ ] **Homepage: Hide "How Forever Home Works" section** for authenticated users
  - Same file as above
  - These users already understand the workflow

- [ ] **Footer: Simplify for authenticated users**
  - Hide "For Adopters" and "For Partners" sections when logged in
  - Collapse to single line with essential links (Help, Contact, Privacy)

### 1.2 Humanize Breed Display Names
- [ ] **Create breed display name formatter**
  - Convert `CAVALIER_KING_CHARLES` → `Cavalier King Charles`
  - Location: Create utility function, apply to all pet cards and detail pages
  - Files affected:
    - Pet browse page
    - Pet detail page
    - All dashboard pet listings
    - Adopter applications list

### 1.3 Use Relative Timestamps
- [ ] **Replace absolute dates with relative time**
  - "12/18/2025" → "3 days ago" or "Today"
  - Create date formatting utility
  - Apply to:
    - Adopter dashboard application dates
    - Foster dashboard pet dates
    - Rescue dashboard application dates

### 1.4 Collapse Empty Dashboard Sections
- [ ] **Adopter Dashboard: Collapse empty "Saved Pets"**
  - Current: Full card with icon, heading, paragraph, button
  - Target: Single line: "No saved pets yet · [Browse Pets]"

- [ ] **Foster Dashboard: Hide zero-count status cards**
  - Only show statuses with count > 0
  - Or collapse all zeros to single summary

- [ ] **Rescue Dashboard: Merge empty sections**
  - Combine "Pending Review" and "Adoption Applications" when both empty
  - Show: "No items need your attention" (single line)

- [ ] **Vet Dashboard: Consolidate empty state CTAs**
  - Current: 3 separate "Request Approval" buttons
  - Target: Single onboarding card for new vets

---

## Phase 2: Dashboard Restructuring (High Impact, Medium Effort)

### 2.1 Rescue Dashboard Overhaul (Highest Priority)
The rescue dashboard is the most complex and needs significant restructuring.

- [ ] **Create "Action Required" section at top**
  - Aggregate all pending actions with counts:
    - Pets awaiting review
    - Applications pending decision
    - Adoptions ready to finalize
  - Use alert/warning styling to draw attention

- [ ] **Elevate "Ready to Finalize" prominently**
  - Move from buried position to primary CTA
  - Add visual emphasis (color, size, position)

- [ ] **Consolidate stat counters**
  - Current: 5 stats (Pending Review, Available, Applications, In Progress, Vet Requests)
  - Target: 3 stats (Action Needed, Available, Completed)

- [ ] **Simplify Active Listings filters**
  - Current: 5 tabs (All, Pending Vet, Available, In Progress, Adopted)
  - Target: Dropdown filter + "Needs Attention" default view
  - Or: Hide completed by default, toggle to show

### 2.2 Adopter Dashboard Improvements
- [ ] **Group applications by action state**
  - "Action Needed": Approved applications waiting for adopter
  - "Pending": Applications under review
  - "Completed": Adopted pets (collapsed by default)

- [ ] **Add next-step guidance for approved applications**
  - Show: "Approved! Contact [Rescue Name] to schedule pickup"
  - Add direct contact button/link

### 2.3 Foster Dashboard Improvements
- [ ] **Consolidate status counters**
  - Current: 6 statuses (Drafts, With Rescue, Available, In Progress, Adopted, Withdrawn)
  - Target: 3 groups (Active, In Progress, Completed)

- [ ] **Add status timeline/progress indicator**
  - Show where each pet is in the journey
  - Especially for "With Rescue" status - what's next?

### 2.4 Vet Dashboard Improvements
- [ ] **Move Pet Lookup to top**
  - This is the vet's primary task
  - Should be the first thing they see

- [ ] **Streamline onboarding flow**
  - Single clear onboarding card for new vets
  - Progress indicator: "Step 1: Request approval from a rescue"

---

## Phase 3: Browse & Detail Page Improvements

### 3.1 Pet Browse Page
- [ ] **Add breed filter dropdown**
  - Populate from available breeds in current results
  - Consider searchable/autocomplete for many breeds

- [ ] **Add dismissible filter chips**
  - Show active filters as chips above results
  - Each chip has (x) to remove that filter
  - Reduces need for "Clear all filters" click

- [ ] **Show pagination at top and bottom**
  - Current: Only at bottom
  - Add count + page controls above results

- [ ] **Add rescue organization to pet cards**
  - Small badge or text showing which rescue
  - Saves a click for users with rescue preferences

### 3.2 Pet Detail Page
- [ ] **Auto-redirect after login**
  - When user clicks "Sign In to Apply"
  - After successful login, return to this pet's page
  - Optionally: auto-open application modal

- [ ] **Show save icon for unauthenticated users**
  - Display heart icon but prompt login on click
  - Don't hide functionality, just gate it

---

## Phase 4: Navigation & Global Improvements

### 4.1 Navigation Enhancements
- [ ] **Role-specific dashboard labels**
  - Current: Generic "Dashboard" for all roles
  - Target:
    - Adopter: "My Applications"
    - Foster: "My Pets"
    - Rescue: "Manage Pets"
    - Vet: "Sign-offs"

- [ ] **Add notification badge count**
  - Show unread count on bell icon
  - Current: Just icon, no count

### 4.2 Status Badge Consistency
- [ ] **Standardize status badge colors across all pages**
  - Green: Available
  - Blue: Pending (Pending Vet, Pending Review)
  - Yellow/Orange: In Progress
  - Gray: Completed (Adopted, Withdrawn)
  - Red: Action Required

---

## Phase 5: Registration & Onboarding (Lower Priority)

### 5.1 Registration Flow
- [ ] **Visual role selector**
  - Current: Dropdown with text options
  - Target: Visual cards with icons for each role
  - Faster recognition, more engaging

- [ ] **Consider social login (future)**
  - Google OAuth to reduce friction
  - Lower priority, requires backend work

---

## Implementation Notes

### Files Likely Affected
Based on standard React/frontend structure:
- `src/main/resources/static/` - Frontend assets
- Component files for each page/dashboard
- Shared utility functions for formatting
- CSS/styling for status badges, empty states

### Testing Considerations
- Test all dashboards with:
  - Empty state (new user)
  - Partial data
  - Full data with all statuses
- Test authenticated vs unauthenticated views
- Test all user roles

### Metrics to Track
- Time to complete key actions (apply for pet, finalize adoption)
- Click depth to reach primary actions
- Dashboard engagement (do users scroll to bottom?)
- Empty state conversion (do users click CTAs?)

---

## Progress Tracking

| Phase | Status | Completion |
|-------|--------|------------|
| Phase 1: Quick Wins | Not Started | 0% |
| Phase 2: Dashboard Restructuring | Not Started | 0% |
| Phase 3: Browse & Detail Pages | Not Started | 0% |
| Phase 4: Navigation & Global | Not Started | 0% |
| Phase 5: Registration & Onboarding | Not Started | 0% |

---

## Changelog

- **2025-12-18**: Initial plan created from UX review
