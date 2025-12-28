# Adopter User Stories

#mvp #core #enhanced

> **User Type:** People seeking to adopt a pet through verified rescue organizations

**Related:** [[index|User Stories Index]] | [[../domain-model|Domain Model]] | [[../Roadmap|Roadmap]]

## Overview

Adopters are individuals or families looking to welcome a new pet into their home. They browse available pets, submit adoption applications, and work with rescue organizations to complete the adoption process.

All pets on Forever Home have been verified by licensed veterinarians, giving adopters confidence that their new companion is healthy, vaccinated, and ready for their forever home.

## Related Documentation

- [Index](index.md) - Platform overview and all user types
- [Visitor Stories](visitor.md) - Public browsing (unauthenticated)
- [Rescue Organization Stories](rescue-organization.md) - Application review process
- [Domain Model](../domain-model.md) - Entity definitions
- [UI Style Guide](../ui-style-guide.md) - Component specifications

---

## User Journey

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Browse    │────►│  Register   │────►│  Complete   │────►│   Browse    │
│  (Visitor)  │     │   Account   │     │   Profile   │     │  & Filter   │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
                                                                   │
                    ┌──────────────────────────────────────────────┘
                    ▼
             ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
             │   View Pet  │────►│   Apply to  │────►│    Track    │
             │   Profile   │     │    Adopt    │     │   Status    │
             └─────────────┘     └─────────────┘     └─────────────┘
                    │                                       │
                    ▼                                       ▼
             ┌─────────────┐                         ┌─────────────┐
             │   Save to   │                         │   Adopt!    │
             │  Favorites  │                         │             │
             └─────────────┘                         └─────────────┘
```

**Typical Flow:**
1. Browse available pets (can start as visitor)
2. Register account with Adopter role
3. Complete profile (living situation, pet experience)
4. Filter and search for ideal pet match
5. View detailed pet profiles
6. Save favorites to compare later
7. Submit adoption application
8. Track application status
9. If approved, coordinate with rescue for adoption

---

## Registration & Authentication

### US-1.1: Adopter Registration

**As a** visitor
**I want to** create an Adopter account
**So that** I can apply to adopt pets

**Acceptance Criteria:**
- Select "Adopter" role during registration
- Provide email address and password
- Password meets security requirements (min 8 chars, mixed case, number)
- Email verification is required before account activation
- Receive confirmation email upon successful registration
- Account status set to `Pending` until email verified (then `Active`)

**Domain Notes:**
- Creates `User` entity with `UserRole = Adopter`
- `User.profileComplete = false` until US-1.4 completed
- No admin approval required for Adopter role

**Authentication:**
- JWT-based authentication
- Access token: 15 minute expiry, stored in memory
- Refresh token: 7 day expiry (30 days with "Remember me"), stored in httpOnly cookie
- Token rotation: Old refresh token revoked when new one issued
- Email verification sends magic link with 24-hour expiry
- Google OAuth: Alternative login with automatic email verification

**UI Components:**
- Role selection: Radio buttons with descriptions ("I want to adopt a pet")
- Google Sign-In button with branded styling
- Form inputs: 48px height, 16px font (prevents iOS zoom)
- Primary button: "Create Account" (Forest green `#2D5A47`)
- Error states: Red border with inline error message

**Priority:** P0 - MVP

---

### US-1.2: Adopter Login

**As a** registered adopter
**I want to** log into my account
**So that** I can browse pets and manage my applications

**Acceptance Criteria:**
- Log in with email and password
- Failed login attempts are rate-limited (5 attempts, then 15-min lockout)
- Redirected to Adopter dashboard after login
- "Remember me" option extends refresh token to 30 days
- If profile incomplete, redirect to profile completion

**Domain Notes:**
- Validates against `User.email` and `User.passwordHash`
- Checks `User.status` is `Active` before allowing login
- Updates `User.lastLoginAt` timestamp
- Checks `User.profileComplete` for redirect logic

**Authentication:**
- Returns JWT access token (15 min) + refresh token (7 days, or 30 days with "Remember me")
- Refresh token stored in httpOnly secure cookie
- Access token returned in response body, stored in memory by client
- `/auth/refresh` endpoint to obtain new access token using refresh cookie

**UI Components:**
- Bottom sheet modal on mobile, centered modal on desktop
- Input focus state: Primary border with 3px ring
- Tertiary button: "Forgot password?" link
- Checkbox: "Remember me for 30 days"

**Priority:** P0 - MVP

---

### US-1.3: Password Recovery

**As an** adopter
**I want to** reset my password if forgotten
**So that** I can regain access to my account

**Acceptance Criteria:**
- Request password reset via email
- Reset link expires after 24 hours
- Must create a new password meeting security requirements
- Confirmation shown after successful reset

**UI Components:**
- Success state: Green checkmark icon with confirmation message
- Email input with validation

**Priority:** P2 - Enhanced

---

### US-1.4: Complete Adopter Profile

**As a** newly registered adopter
**I want to** complete my profile information
**So that** rescue organizations can evaluate my applications

**Acceptance Criteria:**
- Prompted to complete profile after first login
- Required fields: firstName, lastName, phone, location (city/state)
- Additional fields: livingSituation, petExperience
- Profile is saved and `User.profileComplete` set to `true`
- Cannot submit applications until profile is complete

**Domain Notes:**
- Creates `Adopter` entity linked to `User.id`
- Updates `User.profileComplete = true`
- Adopter immediately active (no admin approval needed)
- `livingSituation` and `petExperience` pre-fill adoption applications

**UI Components:**
- Multi-step wizard with progress indicator
- Form inputs: 48px height, 16px font
- Textarea for living situation and pet experience
- Skip not allowed - must complete to proceed
- Primary CTA: "Complete Profile"

**Priority:** P0 - MVP

---

## Pet Discovery

### US-5.1: Browse Available Pets (Authenticated)

**As an** adopter
**I want to** browse pets available for adoption
**So that** I can find a pet that matches my preferences

**Access:** Authenticated (Adopter role)

**Acceptance Criteria:**
- Only pets with `status = Available` are visible
- List shows: primary pet photo, name, breed, age, size, sex
- Grid layout: 2 columns mobile, 3-4 columns desktop
- Pagination or infinite scroll for large lists (20 per page)
- Can toggle between grid and list view
- Favorite button is functional (saves to favorites list)

**Domain Notes:**
- Query: `Pet WHERE status = 'Available' ORDER BY createdAt DESC`
- Join to `PetImage WHERE isPrimary = true` for thumbnail
- Favorite creates many-to-many relation between `Adopter` and `Pet`

**UI Components:**
- Pet card (grid view):
  - Image: 3:2 aspect ratio, rounded corners (16px)
  - Name: Lora font, 20px
  - Meta: "Breed - Size" in secondary text
  - Status badge: "Available" (green)
  - Favorite button: Heart icon, top-right (toggleable)
- Card hover: Shadow elevation + slight translateY
- View toggle: Grid/List icons in header
- Loading: Skeleton cards during fetch

**Priority:** P0 - MVP

---

### US-5.2: Filter Pets

**As an** adopter
**I want to** filter pets by various criteria
**So that** I can narrow down my search efficiently

**Acceptance Criteria:**
- Filter by species (Dog, Cat, Rabbit, Bird, Other)
- Filter by breed (multi-select, populated from available pets)
- Filter by size (Small, Medium, Large - multi-select)
- Filter by age range (slider or min/max inputs)
- Filter by location/rescue organization
- Filters can be combined (AND logic)
- Results update in real-time (debounced 300ms)
- Active filter count shown on filter button
- Can clear all filters with one tap

**UI Components:**
- Filter button: Icon + "Filter" + count badge
- Filter panel: Bottom sheet (mobile) / sidebar (desktop)
- Chips for active filters below search
- Multi-select: Checkbox list per category
- Age range: Dual-handle slider
- Clear all: Tertiary button at bottom

**Priority:** P1 - Core

---

### US-5.3: View Pet Profile (Authenticated)

**As an** adopter
**I want to** view detailed information about a pet
**So that** I can decide if they're right for me

**Access:** Authenticated (Adopter role)

**Acceptance Criteria:**
- Shows all pet details: name, age, breed, species, description, size
- Displays all uploaded images in swipeable gallery
- Shows vet verification badge with vet name
- Shows rescue organization name with contact details
- Indicates microchip status (has microchip: yes/no)
- Shows "Apply to Adopt" CTA prominently
- Favorite button in header (functional)
- Shows if already applied: "Application Submitted" state

**Domain Notes:**
- Query: `Pet WHERE id = :id`
- Join to `PetImage` for gallery
- Join to `VetSignOff` for verification details
- Join to `RescueOrganization` for contact info
- Check `AdoptionApplication WHERE petId = :id AND adopterId = :currentUser`

**UI Components:**
- Image gallery: Full-width hero, 4:3 aspect ratio
  - Dot indicators for image count
  - Swipe to navigate (mobile)
  - Tap for fullscreen viewer
- Pet name: Lora font, H2 (24px)
- Stats row: Age | Size | Sex in bordered boxes
- Verified badge: Green checkmark + "Verified by Dr. [Name]"
- Description: Body text, max 500 chars
- Rescue section: Org name, location icon, phone icon
- Sticky footer: "Apply to Adopt [Name]" primary button
  - If already applied: Disabled button "Application Submitted"

**Priority:** P0 - MVP

---

## Adoption Application

### US-5.4: Apply to Adopt

**As an** adopter
**I want to** submit an adoption application
**So that** I can express interest in a specific pet

**Access:** Authenticated (Adopter role with complete profile)

**Acceptance Criteria:**
- Application form captures:
  - Living situation (textarea)
  - Experience with pets (textarea)
  - Why you want to adopt this pet (textarea)
- Form pre-fills adopter profile data if available
- Can only have one active application per pet
- Limit of 3 active applications total (configurable)
- Receives confirmation screen upon submission
- Can view application status in dashboard

**Domain Notes:**
- Creates `AdoptionApplication` entity:
  - `petId`, `adopterId`
  - `status` = `Submitted`
  - `livingSituation`, `petExperience`, `whyAdopt`
  - `submittedAt` timestamp
- Check: No existing application for same pet by same adopter
- Check: Adopter has fewer than 3 active applications

**UI Components:**
- Multi-field form with character counts
- Textarea: 120px min-height, resize vertical
- Pre-fill notice: "Some fields pre-filled from your profile"
- Validation: All fields required, min 50 chars each
- Submit button: "Submit Application"
- Success screen: Checkmark animation + "Application Submitted"

**Priority:** P1 - Core

---

### US-5.5: Track Application Status

**As an** adopter
**I want to** see the status of my adoption applications
**So that** I know where I am in the process

**Access:** Authenticated (Adopter only - sees own applications)

**Acceptance Criteria:**
- Dashboard shows all submitted applications
- Each shows: pet photo, pet name, status, submitted date
- Status values: Submitted, Under Review, Approved, Rejected, Withdrawn
- Notifications sent on status changes
- Can withdraw application (if not yet approved)

**Domain Notes:**
- Query: `AdoptionApplication WHERE adopterId = :id ORDER BY submittedAt DESC`
- Join to `Pet` for photo/name

**Application Status Flow:**
```
Submitted ──[Rescue reviews]──► Under Review
Under Review ──[Approved]──► Approved
Under Review ──[Rejected]──► Rejected
Any (except Approved) ──[Adopter withdraws]──► Withdrawn
```

**UI Components:**
- Application card:
  - Pet thumbnail (60px square)
  - Pet name, rescue org name
  - Status badge
  - "Submitted [date]"
- Status colors:
  - Submitted: Gray
  - Under Review: Gold
  - Approved: Green
  - Rejected: Red
  - Withdrawn: Gray (strikethrough)
- Withdraw: Tertiary destructive link

**Priority:** P2 - Enhanced

---

### US-5.7: Swipe Mode Discovery

**As an** adopter
**I want to** discover pets using a swipe interface
**So that** I can quickly browse through available pets in an engaging way

**Access:** Public (can start as visitor) or Authenticated (saves likes)

**Acceptance Criteria:**
- Full-screen, immersive card-based interface
- Swipe left to pass, swipe right to like (add to favorites)
- Tap card or swipe up for detailed pet information
- Filter pets by species, size, and sex before swiping
- Keyboard shortcuts: Arrow keys to swipe, Escape to exit
- Lazy loading: Fetches next batch as user approaches end of current set
- Empty states for "no results" and "all swiped"
- Liked pets saved to favorites list (if authenticated)

**Domain Notes:**
- Reuses existing pet browsing endpoints with pagination
- Likes create/update entries in Favorites table
- Session state tracks already-seen pets to prevent duplicates

**UI Components:**
- SwipeContainer: Full-screen wrapper with filters header
- SwipeCard: Individual pet card with gesture support
  - Pet photo (full-card background)
  - Name overlay at bottom
  - Quick stats (breed, age, sex)
- SwipeActions: Pass/Like/Info buttons below card
- SwipeFilters: Species/Size/Sex dropdown filters
- SwipeEmptyState: Illustrations for empty/completed states
- Gesture support via react-spring and @use-gesture/react

**Priority:** P2 - Enhanced

---

## Favorites

### US-5.6: Favorite Pets

**As an** adopter
**I want to** save pets to a favorites list
**So that** I can compare and decide later

**Access:** Authenticated (Adopter role required)

**Acceptance Criteria:**
- Add/remove pets from favorites with one click (heart icon)
- Heart icon toggles filled/outline state
- Favorites list accessible from bottom navigation ("Saved")
- Notification if favorited pet is adopted or status changes
- Favorites persist across sessions

**Domain Notes:**
- Many-to-many: `Adopter` <-> `Pet` (favorites join table)
- Trigger notification when favorited pet's status changes

**UI Components:**
- Favorite button: Heart icon, 44px touch target
  - Outline: Not favorited (muted color)
  - Filled: Favorited (Terracotta `#C4705A`)
- Animation: Scale pulse on toggle (300ms)
- Saved page: Grid of favorited pets
- Empty state: Heart icon + "No saved pets yet"

**Priority:** P2 - Enhanced

---

## Interactions with Other Users

### Working with Rescue Organizations

After submitting an application:
1. Rescue organization receives notification
2. Rescue reviews application details (living situation, experience, motivation)
3. Rescue either **approves** or **rejects** with optional feedback
4. If approved, pet status becomes `InProgress`
5. Rescue coordinates handoff between foster and adopter

See [Rescue Organization Stories](rescue-organization.md) for the rescue's perspective.

### Adoption Completion

Once approved:
1. Rescue contacts adopter with next steps
2. Adopter coordinates with foster for pet pickup/delivery
3. Adopter receives pet
4. Rescue marks adoption as complete
5. Pet status becomes `Adopted`

---

## Adopter Dashboard

The Adopter dashboard displays:
- **My Applications**: List of submitted applications with status badges
- **Saved Pets**: Quick access to favorites
- **Recommended Pets**: Personalized suggestions based on preferences
- **Notifications**: Application updates, favorited pet changes

Dashboard layout:
- Application cards showing: pet thumbnail, name, status, days since submitted
- Quick stats: "X Applications - Y Saved"
- "Browse Pets" primary CTA

### Application Limits

| Limit | Value | Rationale |
|-------|-------|-----------|
| Active applications | 3 | Prevents spam, ensures serious intent |
| Applications per pet | 1 | No duplicate applications |
| Favorites | Unlimited | Encourage discovery |
