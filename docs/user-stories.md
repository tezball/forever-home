# User Stories

> **Related Documentation:**
> - [Domain Model](./domain-model.md) - Entity definitions and relationships
> - [Pet Status](./pet-status.md) - Status lifecycle and transitions
> - [UI Style Guide](./ui-style-guide.md) - Component specifications

---

## Epic 1: User Registration & Authentication

**Related Entities:** `User`, `Foster`, `Adopter`, `Vet`, `RescueOrganization`

### US-1.1: User Registration
**As a** visitor
**I want to** create an account with a specific role
**So that** I can participate in the pet adoption ecosystem

**Acceptance Criteria:**
- User can select role: Foster, Adopter, Vet, or Rescue Organization
- Email verification is required before account activation
- Password meets security requirements (min 8 chars, mixed case, number)
- User receives confirmation email upon successful registration
- Account status set to `Pending` until email verified (then `Active`)

**Domain Notes:**
- Creates `User` entity with selected `UserRole`
- Role-specific profile (Foster, Adopter, etc.) created upon first login
- Vet and Rescue Organization accounts require admin approval (`verified: false`)

**Authentication:**
- JWT-based authentication
- Access token: 15 minute expiry, stored in memory
- Refresh token: 7 day expiry, stored in httpOnly cookie
- Email verification sends magic link with 24-hour expiry

**UI Components:**
- Role selection: Radio buttons with descriptions
- Form inputs: 48px height, 16px font (prevents iOS zoom)
- Primary button: "Create Account" (Forest green `#2D5A47`)
- Error states: Red border with inline error message

---

### US-1.2: User Login
**As a** registered user
**I want to** log into my account
**So that** I can access role-specific features

**Acceptance Criteria:**
- User can log in with email and password
- Failed login attempts are rate-limited (5 attempts, then 15-min lockout)
- User is redirected to role-appropriate dashboard after login
- "Remember me" option extends refresh token to 30 days

**Domain Notes:**
- Validates against `User.email` and `User.passwordHash`
- Checks `User.status` is `Active` before allowing login
- Updates `User.lastLoginAt` timestamp

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

---

### US-1.3: Password Recovery
**As a** registered user
**I want to** reset my password if forgotten
**So that** I can regain access to my account

**Acceptance Criteria:**
- User can request password reset via email
- Reset link expires after 24 hours
- User must create a new password meeting security requirements
- Confirmation shown after successful reset

**UI Components:**
- Success state: Green checkmark icon with confirmation message
- Email input with validation

---

### US-1.4: Complete Profile
**As a** newly registered user
**I want to** complete my role-specific profile
**So that** I can use the platform's features

**Acceptance Criteria:**
- After first login, user is prompted to complete their profile
- User can select/confirm their role (Foster, Adopter, Vet, Rescue Org)
- Role-specific fields are displayed based on selection:
  - **Foster/Adopter:** firstName, lastName, phone, location (city/state)
  - **Adopter additional:** livingSituation, petExperience
  - **Vet:** clinicName, licenseNumber, location, phone, website, description
  - **Rescue Org:** name, location, phone, website, description, contactName, contactEmail
- Profile is saved and `User.profileComplete` set to `true`
- User cannot access role features until profile is complete

**Domain Notes:**
- Creates role-specific entity (Foster, Adopter, Vet, or RescueOrganization)
- Updates `User.profileComplete = true`
- For Vet/Rescue Org: Sets `verified = false`, adds to admin approval queue

**UI Components:**
- Multi-step wizard based on role
- Progress indicator showing completion steps
- Form inputs: 48px height, 16px font
- Skip not allowed - must complete to proceed
- Primary CTA: "Complete Profile"

---

## Epic 2: Foster - Pet Registration

**Related Entities:** `Foster`, `Pet`, `PetImage`, `RescueOrganization`
**Status Transitions:** `Draft` → `PendingRescue`

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
- No authentication required to browse (public)
- Selection stored temporarily until pet registration submitted

**UI Components:**
- Organization card: Logo (60px), name, location, description truncated
- Filter: Location dropdown or search
- Grid layout: 1 column mobile, 2 columns tablet+
- Card click: Expands to full profile or navigates to org page
- "Select This Organization" button on profile

---

### US-2.1: Register Pet for Adoption
**As a** foster
**I want to** register my pet for adoption
**So that** I can find a loving forever home for them

**Acceptance Criteria:**
- Foster can enter: name, age (with unit: months/years), breed, description (max 500 chars), size, sex, microchip number
- Microchip number is required (used for vet lookup and ownership tracking)
- Foster can select species: Dog, Cat, Rabbit, Bird, Other
- Foster can select sex: Male, Female
- Foster can upload multiple images (max 5 at a time)
- Foster must designate one image as primary (shown in listings)
- Pet status is set to `Draft` during creation, `PendingRescue` on submit
- Foster must select a rescue organization to work with

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

---

### US-2.2: Edit Pet Profile
**As a** foster
**I want to** update my pet's information
**So that** the profile remains accurate and appealing

**Acceptance Criteria:**
- Foster can edit all pet details except microchip number
- Foster can add/remove images (max 5 total)
- Foster can change primary image
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

---

### US-2.3: View Pet Status
**As a** foster
**I want to** see the current status of my pet's adoption journey
**So that** I know where they are in the process

**Acceptance Criteria:**
- Foster can see current status with visual indicator
- Status displayed: Draft, Pending Rescue, Pending Vet, Available, In Progress, Adopted, Withdrawn, On Hold
- Foster receives notifications on status changes
- Foster can view vet sign-off details when complete
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

**Status Reference:**
| Status | Visible to Public | Foster Can Edit |
|--------|-------------------|-----------------|
| Draft | No | Yes |
| PendingRescue | No | Limited |
| PendingVet | No | No |
| Available | Yes | No |
| InProgress | Yes (marked) | No |
| Adopted | No | No |

---

### US-2.4: Withdraw Pet from Adoption
**As a** foster
**I want to** remove my pet from the adoption listing
**So that** I can keep them if circumstances change

**Acceptance Criteria:**
- Foster can withdraw pet before adoption is finalized
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

---

## Epic 3: Rescue Organization Management

**Related Entities:** `RescueOrganization`, `Pet`, `AdoptionApplication`, `Adoption`

### US-3.1: Create Organization Profile
**As a** rescue organization representative
**I want to** create my organization's profile
**So that** fosters and adopters can find and trust us

**Acceptance Criteria:**
- Can enter: name, location (Address), phone, website, description, logo
- Can add contact person details (contactName, contactEmail)
- Can add social media links (Facebook, Instagram, Twitter, TikTok)
- Logo upload: JPG, PNG, max 2MB, displayed at 80px in listings
- Profile must be approved by admin before going live (`verified: false` → `true`)

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

---

### US-3.2: Manage Organization Profile
**As a** rescue organization representative
**I want to** update my organization's information
**So that** our details remain current

**Acceptance Criteria:**
- Can edit all organization details
- Logo upload supports common image formats (JPG, PNG, WebP)
- Changes are reflected immediately after save
- Cannot edit while admin review is pending

**UI Components:**
- Pre-filled form matching creation form
- Logo: Click to change, hover shows "Change logo" overlay

---

### US-3.3: Accept Pet Registrations
**As a** rescue organization
**I want to** accept or decline pet registrations from fosters
**So that** I can manage my capacity and ensure pet suitability

**Acceptance Criteria:**
- Organization receives notification of new pet registration requests
- Queue shows pets in `PendingRescue` status assigned to this org
- Can view full pet details and foster contact before deciding
- Can accept (→ `PendingVet`) or decline (→ `Draft`) with optional message
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

---

### ~~US-3.4: Assign Vet for Sign-off~~ (REMOVED)

> **Note:** This story has been removed from scope. Vets now self-select pets for verification by looking up the microchip number (see US-4.2). This simplifies the workflow:
> 1. Rescue accepts pet → status becomes `PendingVet`
> 2. Foster takes pet to any verified vet
> 3. Vet looks up pet by microchip and completes sign-off
>
> This eliminates the need for rescue-to-vet assignment and allows fosters to choose their preferred vet.

---

### US-3.5: View Organization's Pets
**As a** rescue organization
**I want to** see all pets under my organization
**So that** I can manage them effectively

**Acceptance Criteria:**
- List shows all pets where `rescueOrgId` matches organization
- Each pet shows: image, name, breed, status badge, days in current status
- Can filter by status (PendingVet, Available, InProgress, Adopted)
- Can sort by: date registered, name, status
- Can click through to individual pet profiles
- Shows aggregate counts by status at top

**Domain Notes:**
- Query: `Pet WHERE rescueOrgId = :orgId`
- Status counts for dashboard metrics

**UI Components:**
- Filter chips: Scrollable horizontal list
- Pet cards: Grid view (2 cols mobile, 3-4 cols desktop)
- Status badge on each card
- Sort dropdown: Top right
- Stats bar: "12 Available · 3 Pending · 45 Adopted"

---

### US-3.6: Facilitate Adoption
**As a** rescue organization
**I want to** manage the adoption process between foster and adopter
**So that** pets are transferred responsibly

**Acceptance Criteria:**
- Can view all adoption applications for each pet
- Application shows: adopter name, living situation, experience, motivation
- Can approve application (→ `InProgress`) or reject with reason
- Only one application can be approved per pet at a time
- Can mark adoption as complete (→ `Adopted`)
- Can cancel adoption if it falls through (→ `Available`)
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

**Status Transitions:**
```
Available ──[Application approved]──► InProgress
InProgress ──[Adoption finalized]──► Adopted
InProgress ──[Adoption cancelled]──► Available
```

---

## Epic 4: Veterinary Verification

**Related Entities:** `Vet`, `VetSignOff`, `VaccinationRecord`

### US-4.1: Create Vet Profile
**As a** veterinarian
**I want to** create my professional profile
**So that** rescue organizations can request my services

**Acceptance Criteria:**
- Can enter: clinic name, location, phone, website, description, logo
- Must provide license/registration number (`licenseNumber`)
- Profile requires admin verification before activation (`verified: false`)
- Cannot be assigned pets until verified

**Domain Notes:**
- Creates `Vet` entity linked to `User.id`
- `licenseNumber` stored for admin verification
- `verified` boolean controlled by admin

**UI Components:**
- Professional form layout
- License number: Text input with format hint
- Pending badge shown until verified: "Awaiting Verification"

---

### US-4.2: Look Up Pet by Microchip
**As a** veterinarian
**I want to** find a pet by its microchip number
**So that** I can verify and sign off on pets brought to my clinic

**Acceptance Criteria:**
- Vet can search for pet by microchip number
- Search returns pet if it exists and is in `PendingVet` status
- Can see pet details: name, breed, age, species, images
- Can see foster contact information
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
- Empty state: "No pet found" or "Pet not pending verification"
- Success state: Pet profile with "Begin Verification" button

---

### US-4.3: Sign Off on Pet
**As a** veterinarian
**I want to** verify a pet meets adoption requirements
**So that** adopters receive healthy, properly prepared pets

**Prerequisite:** Vet has looked up pet via microchip (US-4.2)

**Acceptance Criteria:**
- Must verify neutered/spayed status with date
- Must record vaccinations with name and date for each
- Must assess health status: Good or Known Conditions
- Can add health notes for conditions or special needs
- Can attach medical records or certificates (PDF, JPG)
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

**UI Components:**
- Checklist form with required sections
- Neutered: Checkbox + date picker
- Vaccinations: Dynamic list, add/remove rows
  - Each row: Vaccine name dropdown + date picker
- Health status: Radio buttons (Good / Known Conditions)
- Health notes: Textarea (required if Known Conditions selected)
- File upload: Drag-drop zone, multiple files allowed
- Primary CTA: "Complete Sign-Off" with confirmation

**Status Transition:**
```
PendingVet ──[Vet signs off]──► Available
```

---

### US-4.4: Decline Sign-off
**As a** veterinarian
**I want to** decline signing off on a pet that doesn't meet requirements
**So that** only eligible pets enter the adoption pool

**Acceptance Criteria:**
- Can specify which requirements are not met (checkboxes)
- Can add notes/recommendations for foster
- Rescue organization and foster are notified
- Pet status returns to `PendingRescue` with decline reason visible
- Rescue can reassign to same or different vet after issues resolved

**Domain Notes:**
- Does NOT create `VetSignOff` (no record of incomplete verification)
- Sets `Pet.status` = `PendingRescue`
- Creates `Notification` for foster and rescue with reason

**UI Components:**
- Decline modal with checkboxes:
  - [ ] Not neutered/spayed
  - [ ] Vaccinations incomplete
  - [ ] Health concerns
- Textarea: Required notes field
- Destructive button: "Decline Sign-Off"

**Status Transition:**
```
PendingVet ──[Vet declines]──► PendingRescue
```

---

### US-4.5: View Sign-off History
**As a** veterinarian
**I want to** see all pets I've signed off on
**So that** I have records of my verifications

**Acceptance Criteria:**
- List shows all completed sign-offs by this vet
- Includes: pet name, sign-off date, current pet status
- Can search by pet name or date range
- Can download/print individual sign-off records
- Shows total sign-off count for profile

**Domain Notes:**
- Query: `VetSignOff WHERE vetId = :vetId ORDER BY signedOffAt DESC`
- Join to `Pet` for current status

**UI Components:**
- Table view: Pet name, Date, Status badge
- Search input: Filters by pet name
- Date range picker: Filter by sign-off date
- Export button: Download as PDF

---

## Epic 5: Adopter Experience

**Related Entities:** `Adopter`, `Pet`, `AdoptionApplication`

### US-5.1: Browse Available Pets
**As a** visitor or adopter
**I want to** browse pets available for adoption
**So that** I can find a pet that matches my preferences

**Access:** Public (no authentication required)

**Acceptance Criteria:**
- Only pets with `status = Available` are visible
- List shows: primary pet photo, name, breed, age, size, sex
- Grid layout: 2 columns mobile, 3-4 columns desktop
- Pagination or infinite scroll for large lists (20 per page)
- Can toggle between grid and list view
- Favorite button shown but requires login to use

**Domain Notes:**
- Query: `Pet WHERE status = 'Available' ORDER BY createdAt DESC`
- Join to `PetImage WHERE isPrimary = true` for thumbnail

**UI Components:**
- Pet card (grid view):
  - Image: 3:2 aspect ratio, rounded corners (16px)
  - Name: Lora font, 20px
  - Meta: "Breed · Size" in secondary text
  - Status badge: "Available" (green)
  - Favorite button: Heart icon, top-right
- Card hover: Shadow elevation + slight translateY
- View toggle: Grid/List icons in header
- Loading: Skeleton cards during fetch

---

### US-5.2: Filter Pets
**As an** adopter
**I want to** filter pets by various criteria
**So that** I can narrow down my search efficiently

**Acceptance Criteria:**
- Can filter by species (Dog, Cat, Rabbit, Bird, Other)
- Can filter by breed (multi-select, populated from available pets)
- Can filter by size (Small, Medium, Large - multi-select)
- Can filter by age range (slider or min/max inputs)
- Can filter by location/rescue organization
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

---

### US-5.3: View Pet Profile
**As a** visitor or adopter
**I want to** view detailed information about a pet
**So that** I can decide if they're right for me

**Access:** Public (no authentication required)

**Acceptance Criteria:**
- Shows all pet details: name, age, breed, species, description, size
- Displays all uploaded images in swipeable gallery
- Shows vet verification badge with vet name
- Shows rescue organization name with contact details
- Indicates microchip status (has microchip: yes/no)
- Shows "Apply to Adopt" CTA prominently
- Favorite button in header

**Domain Notes:**
- Query: `Pet WHERE id = :id`
- Join to `PetImage` for gallery
- Join to `VetSignOff` for verification details
- Join to `RescueOrganization` for contact info

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

---

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
- Withdraw: Tertiary destructive link

---

### US-5.6: Favorite Pets
**As an** adopter
**I want to** save pets to a favorites list
**So that** I can compare and decide later

**Access:** Authenticated (Adopter role required)

**Acceptance Criteria:**
- Can add/remove pets from favorites with one click (heart icon)
- Heart icon toggles filled/outline state
- Favorites list accessible from bottom navigation ("Saved")
- Notification if favorited pet is adopted or status changes
- Favorites persist across sessions

**Domain Notes:**
- Many-to-many: `Adopter` ↔ `Pet` (favorites join table)
- Trigger notification when favorited pet's status changes

**UI Components:**
- Favorite button: Heart icon, 44px touch target
  - Outline: Not favorited (muted color)
  - Filled: Favorited (Terracotta `#C4705A`)
- Animation: Scale pulse on toggle (300ms)
- Saved page: Grid of favorited pets
- Empty state: Heart icon + "No saved pets yet"

---

## Epic 6: Admin Management

**Related Entities:** `Admin`, `User`, `RescueOrganization`, `Vet`

### US-6.1: Approve User Registrations
**As an** admin
**I want to** approve rescue organization and vet registrations
**So that** only legitimate entities operate on the platform

**Acceptance Criteria:**
- Queue shows pending registrations (Vets and Rescue Orgs with `verified: false`)
- Can view submitted profile details and credentials
- For vets: Shows license number for verification
- Can approve (`verified: true`) or reject with reason
- Rejection deletes account or allows resubmission
- User is notified of decision via email

**Domain Notes:**
- Query: `Vet WHERE verified = false` UNION `RescueOrganization WHERE verified = false`
- Approve: Sets `verified = true`
- Reject: Sets `User.status = Suspended` with reason

**UI Components:**
- Approval queue: List with entity type badge (Vet/Rescue)
- Detail panel: Slide-out or modal with full profile
- Action buttons: "Approve" (primary) | "Reject" (destructive)
- Reject modal: Reason textarea (required)

---

### US-6.2: Manage All Users
**As an** admin
**I want to** view and manage all platform users
**So that** I can maintain platform integrity

**Acceptance Criteria:**
- Can search users by name or email
- Can filter users by role (Foster, Adopter, Vet, RescueOrg)
- Can filter by status (Active, Pending, Suspended)
- Can suspend or reactivate accounts
- Can trigger password reset for any user
- Can view user activity summary

**Domain Notes:**
- Query: `User` with filters
- Suspend: Sets `User.status = Suspended`
- Password reset: Generates reset token, sends email

**UI Components:**
- Search input: Top of page
- Filter dropdowns: Role, Status
- User table: Name, Email, Role, Status, Actions
- Actions menu: Suspend, Reset Password, View Activity

---

### US-6.3: Platform Analytics
**As an** admin
**I want to** view platform statistics
**So that** I can understand platform usage and success

**Acceptance Criteria:**
- Dashboard shows key metrics:
  - Total users by role
  - Total pets by status
  - Adoption rate (completed / total listed)
  - Average time to adoption
- Charts show trends over time (30/60/90 days)
- Can export reports as CSV

**UI Components:**
- Stat cards: Large number + label + trend indicator
- Line charts: Registrations, adoptions over time
- Pie chart: Pets by status
- Export button: Download CSV

---

### US-6.4: Content Moderation
**As an** admin
**I want to** review flagged content
**So that** I can maintain appropriate platform content

**Acceptance Criteria:**
- Queue shows flagged pet profiles and user reports
- Can view full content and flag reason
- Can dismiss flag (false positive)
- Can remove content (hide pet profile)
- Can warn user via email
- Can suspend repeat offenders
- Audit log of all moderation actions

**Domain Notes:**
- Flagged content stored with reason and reporter
- Actions logged with admin ID and timestamp

**UI Components:**
- Moderation queue: Card per flagged item
- Flag reason highlighted
- Actions: Dismiss, Remove, Warn User, Suspend User

---

## Epic 7: Public Pages

### US-7.1: View Home Page
**As a** visitor
**I want to** understand the platform's purpose
**So that** I can decide if it's right for me

**Acceptance Criteria:**
- Hero section explains platform mission
- Visual showing adoption process steps (Foster → Rescue → Vet → Adopter)
- Statistics: "X pets adopted" counter
- Featured available pets (3-4 cards)
- Clear CTAs for each user type: "Adopt a Pet", "Rehome Your Pet", "I'm a Rescue"

**UI Components:**
- Hero: Display typography (Lora 32px+), Warm Sand background
- Process steps: Horizontal stepper with icons
- Pet cards: Grid of 3-4 featured pets
- CTAs: Primary and secondary buttons

---

### US-7.2: View Rescue Organization Public Profile
**As a** visitor
**I want to** view a rescue organization's public profile
**So that** I can learn about them before engaging

**Acceptance Criteria:**
- Shows: logo, name, description, location, contact info
- Shows social media links with icons
- Lists pets currently available through them (status = Available)
- Does not require login to view
- Links to individual pet profiles

**Domain Notes:**
- Public query: `RescueOrganization WHERE id = :id AND verified = true`
- Pets query: `Pet WHERE rescueOrgId = :id AND status = 'Available'`

**UI Components:**
- Header: Logo (80px) + org name
- Contact section: Location, phone, website with icons
- Social icons: Horizontal row
- Pets section: Grid of available pets

---

### US-7.3: View Vet Public Profile
**As a** visitor
**I want to** see vet information
**So that** I can verify they're legitimate

**Acceptance Criteria:**
- Shows: clinic name, logo, location, contact details
- Shows total pets verified count
- Professional credentials visible (license number partially masked)
- Does not show pending verifications

**Domain Notes:**
- Public query: `Vet WHERE id = :id AND verified = true`
- Count: `VetSignOff WHERE vetId = :id`

**UI Components:**
- Header: Logo + clinic name
- Stats: "X pets verified" badge
- Contact info: Location, phone, website

---

## Epic 8: Notifications & Communication

**Related Entities:** `Notification`

### US-8.1: Email Notifications
**As a** user
**I want to** receive email notifications for important events
**So that** I stay informed without constantly checking the site

**Acceptance Criteria:**
- Notifications sent for:
  - Status changes (pet, application)
  - New applications (for rescue orgs)
  - Approvals/rejections
  - Favorited pet status changes (for adopters)
- Users can configure notification preferences (per type)
- Emails include direct deep-links to relevant pages
- Unsubscribe link in all emails

**Domain Notes:**
- Notification preferences stored on user profile
- Email service triggered by status change events

**UI Components:**
- Settings page: Toggle switches per notification type
- Email template: Clean, mobile-friendly, branded header

---

### US-8.2: In-App Notifications
**As a** user
**I want to** see notifications within the platform
**So that** I can catch up on activity when logged in

**Acceptance Criteria:**
- Notification bell icon in header shows unread count (max "9+")
- Dropdown shows recent 10 notifications
- Each shows: icon, title, time ago, read/unread state
- Can mark as read individually or "Mark all as read"
- Click navigates to relevant page
- Full notifications page shows all with pagination

**Domain Notes:**
- Creates `Notification` entity per event
- Fields: `userId`, `type`, `title`, `message`, `link`, `read`, `createdAt`
- Mark read: Updates `Notification.read = true`

**UI Components:**
- Bell icon: 24px, in top navigation
- Unread badge: Red circle with count
- Dropdown: 320px wide, max 400px height
- Notification item: Icon + text + time + dot (if unread)
- Time format: "2m ago", "1h ago", "Yesterday"

---

## Priority Matrix

| Priority | User Stories | Focus |
|----------|-------------|-------|
| **P0 - MVP** | US-1.1, US-1.2, US-1.4, US-2.0, US-2.1, US-3.1, US-3.3, US-4.2, US-4.3, US-5.1, US-5.3, US-6.1 | Core registration, profile, verification, and browsing flow |
| **P1 - Core** | US-2.2, US-2.3, US-3.5, US-3.6, US-5.2, US-5.4 | Complete adoption workflow |
| **P2 - Enhanced** | US-1.3, US-2.4, US-4.4, US-4.5, US-5.5, US-5.6, US-6.2, US-8.1, US-8.2 | User experience improvements |
| **P3 - Polish** | US-3.2, US-4.1, US-6.3, US-6.4, US-7.1, US-7.2, US-7.3 | Public pages and analytics |

**MVP Changes:**
- Added US-1.4 (Complete Profile) - required to complete role-specific profile after registration
- Added US-2.0 (Browse Rescue Organizations) - fosters need to discover rescue orgs
- Added US-6.1 (Admin Approvals) - required for Vet/Rescue verification
- Added US-4.2 (Microchip Lookup) - vets find pets by microchip
- Removed US-3.4 (Vet Assignment) - replaced by microchip lookup flow

---

## Status Transition Summary

```
                    ┌─────────────────┐
                    │     Draft       │ ◄── Foster creates
                    └────────┬────────┘
                             │ US-2.1: Foster submits
                             ▼
                    ┌─────────────────┐
                    │ Pending Rescue  │ ◄── Rescue reviews
                    └────────┬────────┘
            US-3.3: │        │ US-3.3: Rescue accepts
     Rescue declines│        ▼
         (back to ──┘┌─────────────────┐
          Draft)     │  Pending Vet    │ ◄── Vet reviews
                     └────────┬────────┘
                              │ US-4.3: Vet signs off
                              ▼
                     ┌─────────────────┐
                     │    Available    │ ◄── Public listing
                     └────────┬────────┘
                              │ US-3.6: Application approved
                              ▼
                     ┌─────────────────┐
                     │   In Progress   │ ◄── Adoption underway
                     └────────┬────────┘
                              │ US-3.6: Adoption finalized
                              ▼
                     ┌─────────────────┐
                     │     Adopted     │ ◄── Complete!
                     └─────────────────┘
```

---

## Glossary

| Term | Definition | Related Entity |
|------|------------|----------------|
| Foster | A pet owner looking to rehome their pet | `Foster` |
| Adopter | A person seeking to adopt a pet | `Adopter` |
| Rescue Organization | A verified entity that facilitates adoptions | `RescueOrganization` |
| Vet | Licensed veterinarian who verifies pet health | `Vet` |
| Vet Sign-off | Verification that pet is neutered, vaccinated, and healthy | `VetSignOff` |
| Pet Status | Current stage in adoption journey (see [pet-status.md](./pet-status.md)) | `Pet.status` |
| Application | Formal request from adopter to adopt a specific pet | `AdoptionApplication` |
| Adoption | Completed transfer of pet to new owner | `Adoption` |
