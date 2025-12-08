# Rescue Organization User Stories

> **User Type:** Verified entities that facilitate pet adoptions between fosters and adopters

## Overview

Rescue Organizations are the trusted intermediaries in the Forever Home adoption process. They review and accept pets from fosters, manage pet listings, evaluate adoption applications, and facilitate the handoff between fosters and adopters.

Rescue Organizations must be verified by platform administrators before they can accept pets or process adoptions, ensuring only legitimate organizations operate on the platform.

## Related Documentation

- [Index](index.md) - Platform overview and all user types
- [Foster Stories](foster.md) - Pet registration process
- [Adopter Stories](adopter.md) - Adoption application process
- [Vet Stories](vet.md) - Health verification workflow
- [Admin Stories](admin.md) - Verification approval process
- [Domain Model](../domain-model.md) - Entity definitions
- [Pet Status](../pet-status.md) - Status lifecycle details
- [UI Style Guide](../ui-style-guide.md) - Component specifications

---

## User Journey

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Register   │────►│  Complete   │────►│    Await    │────►│   Accept    │
│   Account   │     │   Profile   │     │  Approval   │     │    Pets     │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
                                                                   │
                    ┌──────────────────────────────────────────────┘
                    ▼
             ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
             │   Manage    │────►│   Review    │────►│  Facilitate │
             │    Pets     │     │Applications │     │  Adoptions  │
             └─────────────┘     └─────────────┘     └─────────────┘
```

**Typical Flow:**
1. Register account with Rescue Organization role
2. Complete organization profile (name, location, contact info, logo)
3. Await admin verification (cannot accept pets until verified)
4. Once verified, accept pet registrations from fosters
5. Instruct fosters to take pets to verified vets
6. Manage pets through verification to availability
7. Review adoption applications
8. Approve adopters and facilitate handoff
9. Mark adoptions as complete

---

## Registration & Authentication

### US-1.1: Rescue Organization Registration

**As a** rescue organization representative
**I want to** create an account for my organization
**So that** we can facilitate pet adoptions on the platform

**Acceptance Criteria:**
- Select "Rescue Organization" role during registration
- Provide email address and password
- Password meets security requirements (min 8 chars, mixed case, number)
- Email verification is required before account activation
- Receive confirmation email upon successful registration
- Account status set to `Pending` until email verified (then `Active`)
- **Important:** Organization requires admin approval before going live

**Domain Notes:**
- Creates `User` entity with `UserRole = RescueOrganization`
- `User.profileComplete = false` until US-1.4 completed
- **Requires admin approval:** `RescueOrganization.verified = false` initially

**Authentication:**
- JWT-based authentication
- Access token: 15 minute expiry, stored in memory
- Refresh token: 7 day expiry, stored in httpOnly cookie
- Email verification sends magic link with 24-hour expiry

**UI Components:**
- Role selection: Radio buttons with descriptions ("We're a rescue organization")
- Form inputs: 48px height, 16px font (prevents iOS zoom)
- Primary button: "Create Account" (Forest green `#2D5A47`)
- Error states: Red border with inline error message
- Note: "Your organization will be reviewed before activation"

**Priority:** P0 - MVP

---

### US-1.2: Rescue Organization Login

**As a** rescue organization representative
**I want to** log into my account
**So that** I can manage pets and adoptions

**Acceptance Criteria:**
- Log in with email and password
- Failed login attempts are rate-limited (5 attempts, then 15-min lockout)
- Redirected to Rescue Organization dashboard after login
- "Remember me" option extends refresh token to 30 days
- If profile incomplete, redirect to profile completion
- If not verified, show "Pending Approval" banner on dashboard

**Domain Notes:**
- Validates against `User.email` and `User.passwordHash`
- Checks `User.status` is `Active` before allowing login
- Updates `User.lastLoginAt` timestamp
- Checks `User.profileComplete` and `RescueOrganization.verified` for UI state

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

**As a** rescue organization representative
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

### US-1.4: Complete Organization Profile

**As a** newly registered rescue organization representative
**I want to** complete my organization's profile
**So that** fosters and adopters can find and trust us

**Acceptance Criteria:**
- Prompted to complete profile after first login
- Required fields: name, location (Address), phone, contactName, contactEmail
- Optional fields: website, description, logo
- Profile is saved and `User.profileComplete` set to `true`
- Organization added to admin approval queue
- Cannot accept pets until verified by admin

**Domain Notes:**
- Creates `RescueOrganization` entity linked to `User.id`
- Updates `User.profileComplete = true`
- `verified = false` until admin approval
- `Address` embedded object for location

**UI Components:**
- Multi-step wizard with progress indicator
- Form inputs: 48px height, 16px font
- Logo upload: Circular preview, 120px, with crop tool
- Address: Autocomplete or manual entry
- Skip not allowed - must complete to proceed
- Primary CTA: "Submit for Review"
- Confirmation: "Your organization is pending admin review"

**Priority:** P0 - MVP

---

## Organization Profile

### US-3.1: Create Organization Profile

**As a** rescue organization representative
**I want to** create my organization's profile
**So that** fosters and adopters can find and trust us

**Acceptance Criteria:**
- Enter: name, location (Address), phone, website, description, logo
- Add contact person details (contactName, contactEmail)
- Add social media links (Facebook, Instagram, Twitter, TikTok)
- Logo upload: JPG, PNG, max 2MB, displayed at 80px in listings
- Profile must be approved by admin before going live (`verified: false` -> `true`)

**Domain Notes:**
- Creates `RescueOrganization` entity linked to `User.id`
- `Address` embedded object for location
- `SocialLinks` embedded object for social media
- `verified` boolean set by admin

**UI Components:**
- Multi-section form with collapsible sections
- Logo upload: Circular preview, 120px, with crop tool
- Social media inputs: Icon prefix (Facebook icon, etc.)
- Location: Address autocomplete or manual entry
- Submit triggers admin review queue

**Priority:** P0 - MVP

---

### US-3.2: Manage Organization Profile

**As a** rescue organization representative
**I want to** update my organization's information
**So that** our details remain current

**Acceptance Criteria:**
- Edit all organization details
- Logo upload supports common image formats (JPG, PNG, WebP)
- Changes are reflected immediately after save
- Cannot edit while admin review is pending

**UI Components:**
- Pre-filled form matching creation form
- Logo: Click to change, hover shows "Change logo" overlay

**Priority:** P3 - Polish

---

## Pet Management

### US-3.3: Accept Pet Registrations

**As a** rescue organization
**I want to** accept or decline pet registrations from fosters
**So that** I can manage my capacity and ensure pet suitability

**Access:** Requires `verified = true`

**Acceptance Criteria:**
- Receive notification of new pet registration requests
- Queue shows pets in `PendingRescue` status assigned to this org
- View full pet details and foster contact before deciding
- Accept (-> `PendingVet`) or decline (-> `Draft`) with optional message
- Accepted pets appear in organization's pet list
- Upon acceptance, foster is instructed to take pet to any verified vet with microchip number

**Domain Notes:**
- Filters `Pet` where `rescueOrgId` matches and `status` = `PendingRescue`
- Accept: Sets `Pet.status` = `PendingVet`
- Decline: Sets `Pet.status` = `Draft`, notifies foster with reason
- No vet assignment needed - any verified vet can look up pet by microchip (US-4.2)

**UI Components:**
- Pet card (list view): 80px image, name, breed, foster name
- Action buttons: "Accept" (primary) | "Decline" (secondary)
- Decline modal: Textarea for optional reason
- Accept confirmation: Shows microchip number and instructions for foster
- Empty state: Illustration with "No pending requests" message

**Status Transition:**
```
PendingRescue ──[Rescue accepts]──► PendingVet
PendingRescue ──[Rescue declines]──► Draft
```

**Priority:** P0 - MVP

---

### US-3.5: View Organization's Pets

**As a** rescue organization
**I want to** see all pets under my organization
**So that** I can manage them effectively

**Acceptance Criteria:**
- List shows all pets where `rescueOrgId` matches organization
- Each pet shows: image, name, breed, status badge, days in current status
- Filter by status (PendingVet, Available, InProgress, Adopted)
- Sort by: date registered, name, status
- Click through to individual pet profiles
- Shows aggregate counts by status at top

**Domain Notes:**
- Query: `Pet WHERE rescueOrgId = :orgId`
- Status counts for dashboard metrics

**UI Components:**
- Filter chips: Scrollable horizontal list
- Pet cards: Grid view (2 cols mobile, 3-4 cols desktop)
- Status badge on each card
- Sort dropdown: Top right
- Stats bar: "12 Available - 3 Pending - 45 Adopted"

**Priority:** P1 - Core

---

## Adoption Facilitation

### US-3.6: Facilitate Adoption

**As a** rescue organization
**I want to** manage the adoption process between foster and adopter
**So that** pets are transferred responsibly

**Acceptance Criteria:**
- View all adoption applications for each pet
- Application shows: adopter name, living situation, experience, motivation
- Approve application (-> `InProgress`) or reject with reason
- Only one application can be approved per pet at a time
- Mark adoption as complete (-> `Adopted`)
- Cancel adoption if it falls through (-> `Available`)
- Foster and adopter are notified of all status changes

**Domain Notes:**
- Reads `AdoptionApplication` entities for pet
- Approve: Sets `AdoptionApplication.status` = `Approved`, `Pet.status` = `InProgress`
- Complete: Creates `Adoption` record, sets `Pet.status` = `Adopted`

**UI Components:**
- Application list: Card per application with adopter details
- Expandable sections for full application text
- Action buttons: "Approve" (primary) | "Reject" (destructive)
- Finalize modal: Confirmation with checkbox "I confirm this adoption is complete"

**Application Review Interface:**
| Field | Description |
|-------|-------------|
| Adopter Name | Full name from profile |
| Location | City, State |
| Living Situation | Free text from application |
| Pet Experience | Free text from application |
| Why This Pet | Free text from application |
| Submitted | Date of application |

**Status Transitions:**
```
Available ──[Application approved]──► InProgress
InProgress ──[Adoption finalized]──► Adopted
InProgress ──[Adoption cancelled]──► Available
```

**Priority:** P1 - Core

---

## Removed Feature

### ~~US-3.4: Assign Vet for Sign-off~~ (REMOVED)

> **Note:** This story has been removed from scope. Vets now self-select pets for verification by looking up the microchip number (see [Vet Stories - US-4.2](vet.md#us-42-look-up-pet-by-microchip)). This simplifies the workflow:
> 1. Rescue accepts pet -> status becomes `PendingVet`
> 2. Foster takes pet to any verified vet
> 3. Vet looks up pet by microchip and completes sign-off
>
> This eliminates the need for rescue-to-vet assignment and allows fosters to choose their preferred vet.

---

## Interactions with Other Users

### Working with Fosters

When fosters submit pets:
1. Rescue receives notification of new pet registration
2. Rescue reviews pet details and foster information
3. Rescue accepts or declines with feedback
4. If accepted, foster takes pet to vet for verification
5. After vet sign-off, pet becomes available
6. Once adopter is approved, rescue coordinates handoff between foster and adopter

See [Foster Stories](foster.md) for the foster's perspective.

### Working with Vets

After accepting a pet:
1. Foster takes pet to any verified vet
2. Vet looks up pet by microchip number
3. Vet verifies health requirements and signs off
4. Pet automatically becomes `Available`
5. Rescue is notified of vet sign-off

See [Vet Stories](vet.md) for the verification process.

### Working with Adopters

When adopters apply:
1. Rescue receives notification of new application
2. Rescue reviews adopter profile and application details
3. Rescue approves or rejects with optional feedback
4. If approved, pet status becomes `InProgress`
5. Rescue coordinates handoff between foster and adopter
6. Rescue marks adoption as complete when transfer is done

See [Adopter Stories](adopter.md) for the adopter's perspective.

### Admin Verification

Before a rescue can operate:
1. Organization completes profile
2. Admin reviews organization details
3. Admin verifies legitimacy
4. Admin approves (`verified = true`) or rejects

See [Admin Stories](admin.md) for the verification process.

---

## Rescue Organization Dashboard

The Rescue Organization dashboard displays:

### Stats Overview
- **Pending Requests**: Pets awaiting review
- **Active Pets**: Pets in `PendingVet` or `Available` status
- **In Progress**: Adoptions underway
- **Total Adopted**: Lifetime completed adoptions

### Queues
- **Pet Registration Queue**: Pets in `PendingRescue` awaiting acceptance
- **Application Queue**: Pending adoption applications to review

### Pet List
- All pets managed by this organization
- Filter by status
- Quick actions: View, Edit (if allowed), Archive

### Notifications
- New pet registrations
- Vet sign-offs completed
- New adoption applications
- Application status changes

**Dashboard Layout:**
```
┌─────────────────────────────────────────────────────────┐
│  Stats: [12 Active] [3 Pending] [5 Applications] [45]  │
├─────────────────────────────────────────────────────────┤
│  Pet Registration Queue (3)                    [View All]│
│  ┌──────────────────────────────────────────────────┐   │
│  │ [img] Max - Golden Retriever - Foster: Jane     │   │
│  │       [Accept] [Decline]                        │   │
│  └──────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────┤
│  Application Queue (5)                         [View All]│
│  ┌──────────────────────────────────────────────────┐   │
│  │ [img] Bella - Application from John D.          │   │
│  │       Submitted 2 days ago  [Review]            │   │
│  └──────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────┤
│  Recent Activity                                         │
│  - Vet signed off on Luna (2h ago)                      │
│  - New application for Max (4h ago)                      │
└─────────────────────────────────────────────────────────┘
```

---

## Verification Status

Rescue Organizations have two states:

| State | Capabilities |
|-------|-------------|
| **Unverified** (`verified = false`) | Can view dashboard, cannot accept pets or process applications |
| **Verified** (`verified = true`) | Full access to all features |

**Unverified State UI:**
- Banner at top: "Your organization is pending admin review"
- All action buttons disabled
- Contact link to admin if delayed
