# Vet User Stories

> **User Type:** Licensed veterinarians who verify pet health for adoption readiness

## Overview

Veterinarians play a critical role in the Forever Home adoption process by verifying that pets are healthy, neutered/spayed, and properly vaccinated before they become available for adoption. Their professional sign-off gives adopters confidence in their new pet's health.

Vets must be verified by rescue organizations before they can access pets or complete sign-offs. This ensures the rescue organization trusts the vet to perform health checks on their pets.

## Related Documentation

- [Index](index.md) - Platform overview and all user types
- [Foster Stories](foster.md) - Pet registration process (fosters bring pets to vets)
- [Rescue Organization Stories](rescue-organization.md) - Workflow after vet sign-off and vet approval
- [Domain Model](../domain-model.md) - Entity definitions
- [Pet Status](../pet-status.md) - Status lifecycle details
- [UI Style Guide](../ui-style-guide.md) - Component specifications

---

## User Journey

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Register   │────►│  Complete   │────►│    Await    │────►│  Look Up    │
│   Account   │     │   Profile   │     │ Verification│     │  Pet by     │
└─────────────┘     └─────────────┘     └─────────────┘     │  Microchip  │
                                                            └──────┬──────┘
                                                                   │
                    ┌──────────────────────────────────────────────┘
                    ▼
             ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
             │  Review Pet │────►│  Complete   │────►│    View     │
             │   Details   │     │  Sign-Off   │     │   History   │
             └─────────────┘     └─────────────┘     └─────────────┘
                    │
                    ▼
             ┌─────────────┐
             │   Decline   │
             │  (if needed)│
             └─────────────┘
```

**Typical Flow:**
1. Register account with Vet role
2. Complete profile (clinic name, license number, location)
3. Await rescue organization approval
4. Once verified, foster brings pet to clinic
5. Look up pet by microchip number
6. Examine pet and verify health requirements
7. Complete sign-off (pet becomes `Available`) or decline with notes
8. View sign-off history for records

---

## Registration & Authentication

### US-1.1: Vet Registration

**As a** veterinarian
**I want to** create an account for my practice
**So that** I can verify pets for adoption

**Acceptance Criteria:**
- Select "Vet" role during registration
- Provide email address and password
- Password meets security requirements (min 8 chars, mixed case, number)
- Email verification is required before account activation
- Receive confirmation email upon successful registration
- Account status set to `Pending` until email verified (then `Active`)
- **Important:** Vet accounts require rescue organization verification before they can verify pets

**Domain Notes:**
- Creates `User` entity with `UserRole = Vet`
- `User.profileComplete = false` until US-1.4 completed
- **Requires admin approval:** `Vet.verified = false` initially

**Authentication:**
- JWT-based authentication
- Access token: 15 minute expiry, stored in memory
- Refresh token: 7 day expiry, stored in httpOnly cookie
- Email verification sends magic link with 24-hour expiry

**UI Components:**
- Role selection: Radio buttons with descriptions ("I'm a licensed veterinarian")
- Form inputs: 48px height, 16px font (prevents iOS zoom)
- Primary button: "Create Account" (Forest green `#2D5A47`)
- Error states: Red border with inline error message
- Note: "Your license will be verified before activation"

**Priority:** P0 - MVP

---

### US-1.2: Vet Login

**As a** registered veterinarian
**I want to** log into my account
**So that** I can verify pets and manage sign-offs

**Acceptance Criteria:**
- Log in with email and password
- Failed login attempts are rate-limited (5 attempts, then 15-min lockout)
- Redirected to Vet dashboard after login
- "Remember me" option extends refresh token to 30 days
- If profile incomplete, redirect to profile completion
- If not verified, show "Pending Verification" banner on dashboard

**Domain Notes:**
- Validates against `User.email` and `User.passwordHash`
- Checks `User.status` is `Active` before allowing login
- Updates `User.lastLoginAt` timestamp
- Checks `User.profileComplete` and `Vet.verified` for UI state

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

**As a** veterinarian
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

### US-1.4: Complete Vet Profile

**As a** newly registered veterinarian
**I want to** complete my professional profile
**So that** I can be verified and start examining pets

**Acceptance Criteria:**
- Prompted to complete profile after first login
- Required fields: clinicName, licenseNumber, location, phone
- Optional fields: website, description, logo
- Profile is saved and `User.profileComplete` set to `true`
- Vet can request verification from rescue organizations
- Cannot access pet verification until verified by a rescue organization

**Domain Notes:**
- Creates `Vet` entity linked to `User.id`
- Updates `User.profileComplete = true`
- `licenseNumber` stored for admin verification
- `verified = false` until admin approval

**UI Components:**
- Professional form layout
- License number: Text input with format hint
- Form inputs: 48px height, 16px font
- Skip not allowed - must complete to proceed
- Primary CTA: "Submit for Verification"
- Confirmation: "Your license is being verified"
- Pending badge shown until verified: "Awaiting Verification"

**Priority:** P0 - MVP

---

## Vet Profile

### US-4.1: Create Vet Profile

**As a** veterinarian
**I want to** create my professional profile
**So that** rescue organizations can request my services

**Acceptance Criteria:**
- Enter: clinic name, location, phone, website, description, logo
- Must provide license/registration number (`licenseNumber`)
- Profile requires admin verification before activation (`verified: false`)
- Cannot verify pets until verified

**Domain Notes:**
- Creates `Vet` entity linked to `User.id`
- `licenseNumber` stored for admin verification
- `verified` boolean controlled by admin

**UI Components:**
- Professional form layout
- License number: Text input with format hint
- Pending badge shown until verified: "Awaiting Verification"

**Priority:** P3 - Polish

---

## Pet Verification

### US-4.2: Look Up Pet by Microchip

**As a** veterinarian
**I want to** find a pet by its microchip number
**So that** I can verify and sign off on pets brought to my clinic

**Access:** Requires `verified = true`

**Acceptance Criteria:**
- Search for pet by microchip number
- Search returns pet if it exists and is in `PendingVet` status
- See pet details: name, breed, age, species, images
- See foster contact information
- Shows rescue organization managing the pet
- Clear error if microchip not found or pet not in `PendingVet` status

**Domain Notes:**
- Query: `Pet WHERE microchipId = :microchip AND status = 'PendingVet'`
- Join to `Foster` for contact details
- Join to `RescueOrganization` for org details

**UI Components:**
- Search input: Microchip number field with scan icon
- Search button: "Look Up Pet"
- Result: Pet card with full details
- Error states:
  - "No pet found with this microchip" (not in system)
  - "Pet not pending verification" (wrong status)
  - "Pet already verified" (already signed off)
- Success state: Pet profile with "Begin Verification" button

**Search Results Display:**
```
┌─────────────────────────────────────────────────────────┐
│  [Pet Image Gallery]                                    │
├─────────────────────────────────────────────────────────┤
│  Max                              Status: Pending Vet   │
│  Golden Retriever - Male - 2 years                      │
│  Size: Large                                            │
│  Microchip: 123456789012345                             │
├─────────────────────────────────────────────────────────┤
│  Foster: Jane Smith                                     │
│  Phone: (555) 123-4567                                  │
├─────────────────────────────────────────────────────────┤
│  Rescue: Happy Tails Rescue                             │
│  Location: Austin, TX                                   │
├─────────────────────────────────────────────────────────┤
│              [Begin Verification]                       │
└─────────────────────────────────────────────────────────┘
```

**Priority:** P0 - MVP

---

### US-4.3: Sign Off on Pet

**As a** veterinarian
**I want to** verify a pet meets adoption requirements
**So that** adopters receive healthy, properly prepared pets

**Prerequisite:** Vet has looked up pet via microchip (US-4.2)

**Access:** Requires `verified = true`

**Acceptance Criteria:**
- Verify neutered/spayed status with date
- Record vaccinations with name and date for each
- Assess health status: Good or Known Conditions
- Add health notes for conditions or special needs
- Attach medical records or certificates (PDF, JPG)
- Sign-off creates timestamped, immutable `VetSignOff` record
- Pet status updates to `Available` upon sign-off
- Foster and rescue organization are notified

**Domain Notes:**
- Creates `VetSignOff` entity with:
  - `petId`, `vetId`
  - `neuteredDate`
  - `vaccinationRecords[]` (array of `VaccinationRecord`)
  - `healthStatus` enum (Good, KnownConditions)
  - `healthNotes`
  - `signedOffAt` timestamp
  - `attachments[]`
- `VetSignOff` is immutable once created (audit requirement)
- Sets `Pet.status` = `Available`
- Any verified vet can sign off (no pre-assignment required)

**Verification Checklist:**
| Requirement | Input | Required |
|-------------|-------|----------|
| Neutered/Spayed | Checkbox + Date | Yes |
| Vaccinations | List of vaccine + date | Yes (at least 1) |
| Health Status | Radio: Good / Known Conditions | Yes |
| Health Notes | Textarea | Required if Known Conditions |
| Attachments | File upload (PDF, JPG) | Optional |

**UI Components:**
- Checklist form with required sections
- Neutered: Checkbox + date picker
- Vaccinations: Dynamic list, add/remove rows
  - Each row: Vaccine name dropdown + date picker
  - Common vaccines: Rabies, DHPP, Bordetella, FVRCP
- Health status: Radio buttons (Good / Known Conditions)
- Health notes: Textarea (required if Known Conditions selected)
- File upload: Drag-drop zone, multiple files allowed
- Primary CTA: "Complete Sign-Off" with confirmation

**Status Transition:**
```
PendingVet ──[Vet signs off]──► Available
```

**Priority:** P0 - MVP

---

### US-4.4: Decline Sign-off

**As a** veterinarian
**I want to** decline signing off on a pet that doesn't meet requirements
**So that** only eligible pets enter the adoption pool

**Access:** Requires `verified = true`

**Acceptance Criteria:**
- Specify which requirements are not met (checkboxes)
- Add notes/recommendations for foster
- Rescue organization and foster are notified
- Pet status returns to `PendingRescue` with decline reason visible
- Rescue can guide foster to resolve issues and return to vet

**Domain Notes:**
- Does NOT create `VetSignOff` (no record of incomplete verification)
- Sets `Pet.status` = `PendingRescue`
- Creates `Notification` for foster and rescue with reason

**Decline Reasons:**
| Reason | Description |
|--------|-------------|
| Not neutered/spayed | Pet requires spay/neuter procedure |
| Vaccinations incomplete | Missing required vaccinations |
| Health concerns | Medical issues need treatment |

**UI Components:**
- Decline modal with checkboxes:
  - [ ] Not neutered/spayed
  - [ ] Vaccinations incomplete
  - [ ] Health concerns
- Textarea: Required notes field (min 50 chars)
- Destructive button: "Decline Sign-Off"

**Status Transition:**
```
PendingVet ──[Vet declines]──► PendingRescue
```

**Priority:** P2 - Enhanced

---

### US-4.5: View Sign-off History

**As a** veterinarian
**I want to** see all pets I've signed off on
**So that** I have records of my verifications

**Acceptance Criteria:**
- List shows all completed sign-offs by this vet
- Includes: pet name, sign-off date, current pet status
- Search by pet name or date range
- Download/print individual sign-off records
- Shows total sign-off count for profile

**Domain Notes:**
- Query: `VetSignOff WHERE vetId = :vetId ORDER BY signedOffAt DESC`
- Join to `Pet` for current status

**UI Components:**
- Table view: Pet name, Date, Status badge
- Search input: Filters by pet name
- Date range picker: Filter by sign-off date
- Export button: Download as PDF

**History Table:**
| Pet Name | Sign-off Date | Current Status | Actions |
|----------|---------------|----------------|---------|
| Max | Dec 1, 2024 | Adopted | [View] [Export] |
| Bella | Nov 28, 2024 | Available | [View] [Export] |
| Luna | Nov 15, 2024 | In Progress | [View] [Export] |

**Priority:** P2 - Enhanced

---

## Interactions with Other Users

### Working with Fosters

The verification workflow:
1. Rescue accepts pet from foster
2. Foster receives instructions to visit any verified vet
3. Foster brings pet to vet clinic with microchip number
4. Vet looks up pet and performs examination
5. Vet completes sign-off or provides decline feedback
6. Foster receives notification of result

See [Foster Stories](foster.md) for the foster's perspective.

### Working with Rescue Organizations

After sign-off:
1. Rescue receives notification that pet is now `Available`
2. Pet appears in public listings
3. Vet sign-off details visible to adopters viewing pet profile
4. Rescue can reference vet verification when reviewing applications

See [Rescue Organization Stories](rescue-organization.md) for the rescue's perspective.

### Rescue Organization Verification

Before a vet can verify pets for a rescue organization:
1. Vet completes profile with license number
2. Vet requests verification from a rescue organization
3. Rescue organization reviews the vet's credentials
4. Rescue approves (`verified = true`) or declines

This workflow ensures rescue organizations explicitly trust the vets who will perform health checks on their pets. Each rescue organization maintains their own list of approved vets.

See [Rescue Organization Stories](rescue-organization.md) for the approval process.

---

## Vet Dashboard

The Vet dashboard displays:

### Stats Overview
- **Total Sign-offs**: Lifetime completed verifications
- **This Month**: Sign-offs in current month
- **Pending**: Pets looked up but not yet verified (session only)

### Quick Actions
- **Look Up Pet**: Microchip search input
- **View History**: Link to sign-off history

### Recent Activity
- List of recent sign-offs with pet names and dates
- Quick access to view details or export

**Dashboard Layout:**
```
┌─────────────────────────────────────────────────────────┐
│  Welcome, Dr. Smith                                     │
│  Austin Veterinary Clinic                               │
├─────────────────────────────────────────────────────────┤
│  Stats: [156 Total Sign-offs] [12 This Month]          │
├─────────────────────────────────────────────────────────┤
│  Look Up Pet                                            │
│  ┌─────────────────────────────────────────────────┐   │
│  │ Microchip Number: [_______________] [Search]    │   │
│  └─────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────┤
│  Recent Sign-offs                              [View All]│
│  - Max (Golden Retriever) - Dec 1, 2024                │
│  - Bella (Tabby Cat) - Nov 28, 2024                    │
│  - Luna (Beagle) - Nov 15, 2024                         │
└─────────────────────────────────────────────────────────┘
```

---

## Verification Status

Vets have two states:

| State | Capabilities |
|-------|-------------|
| **Unverified** (`verified = false`) | Can view dashboard, cannot look up or verify pets |
| **Verified** (`verified = true`) | Full access to pet lookup and sign-off features |

**Unverified State UI:**
- Banner at top: "Your license is being verified by our team"
- Search input disabled
- Contact link to admin if verification delayed

---

## Sign-off Record Format

Each `VetSignOff` contains:

```
VetSignOff {
  id: UUID
  petId: UUID
  vetId: UUID
  neuteredDate: Date
  vaccinationRecords: [
    { vaccine: "Rabies", date: "2024-11-01" },
    { vaccine: "DHPP", date: "2024-11-01" }
  ]
  healthStatus: "Good" | "KnownConditions"
  healthNotes: String (optional)
  attachments: [URL]
  signedOffAt: Timestamp
}
```

**Immutability:** Once created, sign-off records cannot be modified. This ensures audit integrity. If corrections are needed, the rescue must return the pet to `PendingVet` status for a new verification.
