# Foster User Stories

#mvp #core #enhanced

> **User Type:** Pet owners looking to rehome their pets through rescue organizations

**Related:** [[index|User Stories Index]] | [[../domain-model|Domain Model]] | [[../pet-status|Pet Status]] | [[../Roadmap|Roadmap]]

## Overview

Fosters are pet owners who need to find new loving homes for their pets. They register pets on the platform, work with rescue organizations to facilitate the adoption process, and coordinate with veterinarians for health verification.

A Foster's primary goal is to ensure their pet finds a safe, loving forever home through the trusted network of verified rescue organizations.

## Related Documentation

- [Index](index.md) - Platform overview and all user types
- [Rescue Organization Stories](rescue-organization.md) - How rescues process pet registrations
- [Vet Stories](vet.md) - Health verification process
- [Domain Model](../domain-model.md) - Entity definitions
- [Pet Status](../pet-status.md) - Status lifecycle details
- [UI Style Guide](../ui-style-guide.md) - Component specifications

---

## User Journey

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Register   │────►│  Complete   │────►│   Browse    │────►│  Register   │
│  Account    │     │   Profile   │     │   Rescues   │     │    Pet      │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
                                                                   │
                    ┌──────────────────────────────────────────────┘
                    ▼
             ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
             │   Rescue    │────►│  Take Pet   │────►│  Pet Goes   │
             │   Accepts   │     │   to Vet    │     │  Available  │
             └─────────────┘     └─────────────┘     └─────────────┘
                                                            │
                                                            ▼
                                                     ┌─────────────┐
                                                     │   Handoff   │
                                                     │  to Adopter │
                                                     └─────────────┘
```

**Typical Flow:**
1. Register account with Foster role
2. Complete profile (name, phone, location)
3. Browse rescue organizations and select one to work with
4. Register pet with photos, details, and microchip number
5. Wait for rescue to accept pet registration
6. Upon acceptance, take pet to any verified vet for health sign-off
7. Pet becomes available for adoption
8. Rescue facilitates adoption; foster hands off pet to adopter

---

## Registration & Authentication

### US-1.1: Foster Registration

**As a** visitor
**I want to** create a Foster account
**So that** I can register my pet for adoption

**Acceptance Criteria:**
- Select "Foster" role during registration
- Provide email address and password
- Password meets security requirements (min 8 chars, mixed case, number)
- Email verification is required before account activation
- Receive confirmation email upon successful registration
- Account status set to `Pending` until email verified (then `Active`)

**Domain Notes:**
- Creates `User` entity with `UserRole = Foster`
- `User.profileComplete = false` until US-1.4 completed
- No admin approval required for Foster role

**Authentication:**
- JWT-based authentication
- Access token: 15 minute expiry, stored in memory
- Refresh token: 7 day expiry, stored in httpOnly cookie
- Email verification sends magic link with 24-hour expiry

**UI Components:**
- Role selection: Radio buttons with descriptions ("I want to find a home for my pet")
- Form inputs: 48px height, 16px font (prevents iOS zoom)
- Primary button: "Create Account" (Forest green `#2D5A47`)
- Error states: Red border with inline error message

**Priority:** P0 - MVP

---

### US-1.2: Foster Login

**As a** registered foster
**I want to** log into my account
**So that** I can manage my pet's adoption journey

**Acceptance Criteria:**
- Log in with email and password
- Failed login attempts are rate-limited (5 attempts, then 15-min lockout)
- Redirected to Foster dashboard after login
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

**As a** foster
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

### US-1.4: Complete Foster Profile

**As a** newly registered foster
**I want to** complete my profile information
**So that** rescue organizations can contact me about my pet

**Acceptance Criteria:**
- Prompted to complete profile after first login
- Required fields: firstName, lastName, phone, location (city/state)
- Profile is saved and `User.profileComplete` set to `true`
- Cannot register pets until profile is complete

**Domain Notes:**
- Creates `Foster` entity linked to `User.id`
- Updates `User.profileComplete = true`
- Foster immediately active (no admin approval needed)

**UI Components:**
- Multi-step wizard with progress indicator
- Form inputs: 48px height, 16px font
- Skip not allowed - must complete to proceed
- Primary CTA: "Complete Profile"

**Priority:** P0 - MVP

---

## Pet Registration

### US-2.0: Browse Rescue Organizations

**As a** foster
**I want to** browse available rescue organizations
**So that** I can choose one to work with for my pet's adoption

**Acceptance Criteria:**
- List shows all verified rescue organizations (`verified = true`)
- Each org shows: logo, name, location, description preview
- Can filter by location (city/state)
- Can view full organization profile before selecting
- Can select organization directly from list or from profile page

**Domain Notes:**
- Query: `RescueOrganization WHERE verified = true`
- Selection stored temporarily until pet registration submitted

**UI Components:**
- Organization card: Logo (60px), name, location, description truncated
- Filter: Location dropdown or search
- Grid layout: 1 column mobile, 2 columns tablet+
- Card click: Expands to full profile or navigates to org page
- "Select This Organization" button on profile

**Priority:** P0 - MVP

---

### US-2.1: Register Pet for Adoption

**As a** foster
**I want to** register my pet for adoption
**So that** I can find a loving forever home for them

**Acceptance Criteria:**
- Enter: name, age (with unit: months/years), breed, description (max 500 chars), size, sex, microchip number
- Microchip number is required (used for vet lookup and ownership tracking)
- Select species: Dog, Cat, Rabbit, Bird, Other
- Select sex: Male, Female
- Upload multiple images (max 5 at a time)
- Designate one image as primary (shown in listings)
- Pet status is set to `Draft` during creation, `PendingRescue` on submit
- Must select a rescue organization to work with

**Domain Notes:**
- Creates `Pet` entity linked to `Foster.id`
- `Pet.microchipId` is immutable after creation (fraud prevention)
- Creates `PetImage` entities with `order` and `isPrimary` flags
- `Pet.status` = `PendingRescue` triggers notification to selected rescue

**UI Components:**
- Multi-step form with progress indicator
- Image upload: Drag-drop zone or camera icon button
- Image gallery: 64px thumbnails, drag to reorder
- Size selector: Radio buttons (Small, Medium, Large)
- Sex selector: Radio buttons (Male, Female)
- Age input: Number field + dropdown (Months/Years)
- Microchip input: Text field (required, validated format)
- Primary CTA: "Submit for Review" button
- Card preview showing how pet will appear in listings

**Status Transition:**
```
Draft ──[Foster submits]──► PendingRescue
```

**Priority:** P0 - MVP

---

### US-2.2: Edit Pet Profile

**As a** foster
**I want to** update my pet's information
**So that** the profile remains accurate and appealing

**Acceptance Criteria:**
- Can edit all pet details except microchip number
- Can add/remove images (max 5 total)
- Can change primary image
- Changes are saved and immediately visible
- Edit history is maintained for audit purposes
- Editing disabled once pet reaches `InProgress` status

**Domain Notes:**
- Updates `Pet` entity, `Pet.updatedAt` timestamp refreshed
- `Pet.microchipId` field is read-only in edit form
- Image changes update `PetImage` entities

**UI Components:**
- Pre-filled form with current values
- Microchip field: Disabled input with lock icon
- Image management: Thumbnail grid with delete (X) buttons
- Secondary button: "Cancel" | Primary button: "Save Changes"

**Priority:** P1 - Core

---

### US-2.3: View Pet Status

**As a** foster
**I want to** see the current status of my pet's adoption journey
**So that** I know where they are in the process

**Acceptance Criteria:**
- See current status with visual indicator
- Status displayed: Draft, Pending Rescue, Pending Vet, Available, In Progress, Adopted, Withdrawn, On Hold
- Receive notifications on status changes
- View vet sign-off details when complete
- Timeline shows status history with timestamps

**Domain Notes:**
- Reads `Pet.status` enum value
- Joins to `VetSignOff` when status is `Available` or later
- Status changes create `Notification` entities

**UI Components:**
- Status badge with semantic colors:
  - Available: Green background `#E8F5EC`
  - Pending: Gold background `#FFF5E6`
  - Adopted: Purple background `#F0E8F5`
  - On Hold: Gray background `#E8E4DC`
  - Withdrawn: Red background `#F5E8E8`
- Timeline component with status dots and dates
- Verified badge when vet sign-off complete (green checkmark)

**Status Visibility (Foster Perspective):**

| Status | Foster Can Edit | Notes |
|--------|-----------------|-------|
| Draft | Yes | Initial creation |
| PendingRescue | Limited | Awaiting rescue review |
| PendingVet | No | Take pet to vet |
| Available | No | Listed publicly |
| InProgress | No | Adoption in progress |
| Adopted | No | Complete! |
| Withdrawn | No | Can resubmit |

**Priority:** P1 - Core

---

### US-2.4: Withdraw Pet from Adoption

**As a** foster
**I want to** remove my pet from the adoption listing
**So that** I can keep them if circumstances change

**Acceptance Criteria:**
- Can withdraw pet before adoption is finalized
- Withdrawal requires confirmation modal
- Rescue organization is notified of withdrawal
- Pet profile is archived, not deleted
- If status is `InProgress`, rescue must approve withdrawal

**Domain Notes:**
- Sets `Pet.status` = `Withdrawn`
- Creates `Notification` for rescue organization
- Pet record preserved for audit trail

**UI Components:**
- Destructive button: "Withdraw Pet" (red `#C45A5A`)
- Confirmation modal with warning text
- Bottom sheet on mobile, centered modal on desktop

**Status Transition:**
```
Any (except Adopted) ──[Foster withdraws]──► Withdrawn
Withdrawn ──[Foster resubmits]──► PendingRescue
```

**Priority:** P2 - Enhanced

---

## Interactions with Other Users

### Working with Rescue Organizations

When a foster submits a pet for adoption:
1. The selected rescue organization receives a notification
2. Rescue reviews the pet and foster details
3. Rescue either **accepts** (pet moves to `PendingVet`) or **declines** (pet returns to `Draft`)
4. If accepted, foster receives instructions to take pet to any verified vet

See [Rescue Organization Stories](rescue-organization.md) for the rescue's perspective.

### Working with Vets

After rescue accepts the pet:
1. Foster takes pet to any verified veterinarian
2. Vet looks up pet by microchip number
3. Vet verifies health, vaccination, and neuter status
4. Vet signs off (pet becomes `Available`) or declines with notes

See [Vet Stories](vet.md) for the vet verification process.

### Handoff to Adopter

Once an adopter is approved by the rescue:
1. Foster is notified that adoption is `InProgress`
2. Rescue coordinates handoff between foster and adopter
3. Foster transfers pet to adopter
4. Rescue marks adoption as complete (`Adopted`)

---

## Foster Dashboard

The Foster dashboard displays:
- **My Pets**: List of all registered pets with status badges
- **Notifications**: Status updates, messages from rescues
- **Quick Actions**: Register new pet, view organizations

Dashboard layout:
- Pet cards showing: thumbnail, name, status, days in status
- Filter by status (All, Active, Adopted, Withdrawn)
- "Register New Pet" primary CTA
