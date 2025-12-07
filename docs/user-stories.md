# User Stories

## Epic 1: User Registration & Authentication

### US-1.1: User Registration
**As a** visitor
**I want to** create an account with a specific role
**So that** I can participate in the pet adoption ecosystem

**Acceptance Criteria:**
- User can select role: Foster, Adopter, Vet, or Rescue Organization
- Email verification is required before account activation
- Password meets security requirements (min 8 chars, mixed case, number)
- User receives confirmation email upon successful registration

### US-1.2: User Login
**As a** registered user
**I want to** log into my account
**So that** I can access role-specific features

**Acceptance Criteria:**
- User can log in with email and password
- Failed login attempts are rate-limited
- User is redirected to role-appropriate dashboard after login

### US-1.3: Password Recovery
**As a** registered user
**I want to** reset my password if forgotten
**So that** I can regain access to my account

**Acceptance Criteria:**
- User can request password reset via email
- Reset link expires after 24 hours
- User must create a new password meeting security requirements

---

## Epic 2: Foster - Pet Registration

### US-2.1: Register Pet for Adoption
**As a** foster
**I want to** register my pet for adoption
**So that** I can find a loving forever home for them

**Acceptance Criteria:**
- Foster can enter: name, age, breed, description, size, microchip number
- Foster can upload multiple images (max 5 at a time)
- Pet status is set to "Pending Rescue Assignment"
- Foster must select a rescue organization to work with

### US-2.2: Edit Pet Profile
**As a** foster
**I want to** update my pet's information
**So that** the profile remains accurate and appealing

**Acceptance Criteria:**
- Foster can edit all pet details except microchip number
- Foster can add/remove images
- Changes are saved and immediately visible
- Edit history is maintained for audit purposes

### US-2.3: View Pet Status
**As a** foster
**I want to** see the current status of my pet's adoption journey
**So that** I know where they are in the process

**Acceptance Criteria:**
- Foster can see status: Pending Rescue, Pending Vet, Available, Adoption in Progress, Adopted
- Foster receives notifications on status changes
- Foster can view vet sign-off details when complete

### US-2.4: Withdraw Pet from Adoption
**As a** foster
**I want to** remove my pet from the adoption listing
**So that** I can keep them if circumstances change

**Acceptance Criteria:**
- Foster can withdraw pet before adoption is finalized
- Withdrawal requires confirmation
- Rescue organization is notified of withdrawal
- Pet profile is archived, not deleted

---

## Epic 3: Rescue Organization Management

### US-3.1: Create Organization Profile
**As a** rescue organization representative
**I want to** create my organization's profile
**So that** fosters and adopters can find and trust us

**Acceptance Criteria:**
- Can enter: name, location, phone, website, description, logo
- Can add contact person details (name, email)
- Can add social media links
- Profile must be approved by admin before going live

### US-3.2: Manage Organization Profile
**As a** rescue organization representative
**I want to** update my organization's information
**So that** our details remain current

**Acceptance Criteria:**
- Can edit all organization details
- Logo upload supports common image formats
- Changes are reflected immediately after save

### US-3.3: Accept Pet Registrations
**As a** rescue organization
**I want to** accept or decline pet registrations from fosters
**So that** I can manage my capacity and ensure pet suitability

**Acceptance Criteria:**
- Organization receives notification of new pet registration requests
- Can view pet details before accepting
- Can accept or decline with optional message to foster
- Accepted pets appear in organization's pet list

### US-3.4: Assign Vet for Sign-off
**As a** rescue organization
**I want to** assign a verified vet to sign off on a pet
**So that** the pet can be cleared for adoption

**Acceptance Criteria:**
- Organization can select from list of registered vets
- Vet receives notification of assignment
- Pet status updates to "Pending Vet Sign-off"

### US-3.5: View Organization's Pets
**As a** rescue organization
**I want to** see all pets under my organization
**So that** I can manage them effectively

**Acceptance Criteria:**
- List shows all pets with current status
- Can filter by status (Pending Vet, Available, Adopted)
- Can sort by date registered, name, or status
- Can click through to individual pet profiles

### US-3.6: Facilitate Adoption
**As a** rescue organization
**I want to** manage the adoption process between foster and adopter
**So that** pets are transferred responsibly

**Acceptance Criteria:**
- Can view adoption applications for each pet
- Can approve or reject applications with reason
- Can mark adoption as complete
- Foster and adopter are notified of all status changes

---

## Epic 4: Veterinary Verification

### US-4.1: Create Vet Profile
**As a** veterinarian
**I want to** create my professional profile
**So that** rescue organizations can request my services

**Acceptance Criteria:**
- Can enter: clinic name, location, phone, website, description, logo
- Must provide license/registration number
- Profile requires admin verification before activation

### US-4.2: View Assigned Pets
**As a** veterinarian
**I want to** see pets assigned to me for sign-off
**So that** I can manage my verification workload

**Acceptance Criteria:**
- List shows all pets pending my sign-off
- Can see pet details and foster contact information
- Can sort by assignment date

### US-4.3: Sign Off on Pet
**As a** veterinarian
**I want to** verify a pet meets adoption requirements
**So that** adopters receive healthy, properly prepared pets

**Acceptance Criteria:**
- Can verify neutered status (yes/no with date)
- Can verify vaccination status (list vaccines with dates)
- Can verify health status (good/known conditions with notes)
- Can attach medical records or certificates
- Sign-off creates timestamped, immutable record

### US-4.4: Decline Sign-off
**As a** veterinarian
**I want to** decline signing off on a pet that doesn't meet requirements
**So that** only eligible pets enter the adoption pool

**Acceptance Criteria:**
- Can specify which requirements are not met
- Can add notes/recommendations for foster
- Rescue organization and foster are notified
- Pet status returns to "Pending" with reason visible

### US-4.5: View Sign-off History
**As a** veterinarian
**I want to** see all pets I've signed off on
**So that** I have records of my verifications

**Acceptance Criteria:**
- List shows all completed sign-offs
- Includes sign-off date and pet current status
- Can search by pet name or date range

---

## Epic 5: Adopter Experience

### US-5.1: Browse Available Pets
**As an** adopter
**I want to** browse pets available for adoption
**So that** I can find a pet that matches my preferences

**Acceptance Criteria:**
- Only pets with completed vet sign-off are visible
- List shows pet photo, name, breed, age, size
- Pagination or infinite scroll for large lists

### US-5.2: Filter Pets
**As an** adopter
**I want to** filter pets by various criteria
**So that** I can narrow down my search efficiently

**Acceptance Criteria:**
- Can filter by breed (multi-select)
- Can filter by size (small, medium, large)
- Can filter by age range
- Can filter by location/rescue organization
- Filters can be combined
- Results update in real-time

### US-5.3: View Pet Profile
**As an** adopter
**I want to** view detailed information about a pet
**So that** I can decide if they're right for me

**Acceptance Criteria:**
- Shows all pet details: name, age, breed, description, size
- Displays all uploaded images in gallery format
- Shows rescue organization contact details
- Shows vet verification badge/status
- Indicates microchip status

### US-5.4: Apply to Adopt
**As an** adopter
**I want to** submit an adoption application
**So that** I can express interest in a specific pet

**Acceptance Criteria:**
- Application form captures: living situation, experience with pets, reason for adopting
- Can only apply for one pet at a time (or configurable limit)
- Receives confirmation of application submission
- Can view application status in dashboard

### US-5.5: Track Application Status
**As an** adopter
**I want to** see the status of my adoption applications
**So that** I know where I am in the process

**Acceptance Criteria:**
- Dashboard shows all submitted applications
- Status visible: Under Review, Approved, Rejected, Adoption Complete
- Notifications sent on status changes

### US-5.6: Favorite Pets
**As an** adopter
**I want to** save pets to a favorites list
**So that** I can compare and decide later

**Acceptance Criteria:**
- Can add/remove pets from favorites with one click
- Favorites list accessible from dashboard
- Notification if favorited pet is adopted by someone else

---

## Epic 6: Admin Management

### US-6.1: Approve User Registrations
**As an** admin
**I want to** approve rescue organization and vet registrations
**So that** only legitimate entities operate on the platform

**Acceptance Criteria:**
- Queue shows pending registrations with details
- Can view submitted documentation/credentials
- Can approve or reject with reason
- User is notified of decision via email

### US-6.2: Manage All Users
**As an** admin
**I want to** view and manage all platform users
**So that** I can maintain platform integrity

**Acceptance Criteria:**
- Can search/filter users by role, status, registration date
- Can suspend or reactivate accounts
- Can reset user passwords
- Can view user activity history

### US-6.3: Platform Analytics
**As an** admin
**I want to** view platform statistics
**So that** I can understand platform usage and success

**Acceptance Criteria:**
- Dashboard shows: total users by role, total pets, adoption rate
- Charts show trends over time
- Can export reports

### US-6.4: Content Moderation
**As an** admin
**I want to** review flagged content
**So that** I can maintain appropriate platform content

**Acceptance Criteria:**
- Queue shows flagged pet profiles or user reports
- Can remove inappropriate content
- Can warn or suspend offending users
- Audit log of all moderation actions

---

## Epic 7: Public Pages

### US-7.1: View Home Page
**As a** visitor
**I want to** understand the platform's purpose
**So that** I can decide if it's right for me

**Acceptance Criteria:**
- Clear explanation of platform mission
- Highlights adoption process steps
- Shows success stories/statistics
- Clear calls-to-action for each user type

### US-7.2: View Rescue Organization Public Profile
**As a** visitor
**I want to** view a rescue organization's public profile
**So that** I can learn about them before engaging

**Acceptance Criteria:**
- Shows organization details, logo, contact info
- Lists pets currently available through them
- Shows social media links
- Does not require login to view

### US-7.3: View Vet Public Profile
**As a** visitor
**I want to** see vet information
**So that** I can verify they're legitimate

**Acceptance Criteria:**
- Shows clinic name, location, contact details
- Shows number of pets verified
- Professional credentials visible

---

## Epic 8: Notifications & Communication

### US-8.1: Email Notifications
**As a** user
**I want to** receive email notifications for important events
**So that** I stay informed without constantly checking the site

**Acceptance Criteria:**
- Notifications for: status changes, new applications, approvals/rejections
- Users can configure notification preferences
- Emails include direct links to relevant pages

### US-8.2: In-App Notifications
**As a** user
**I want to** see notifications within the platform
**So that** I can catch up on activity when logged in

**Acceptance Criteria:**
- Notification bell shows unread count
- Dropdown shows recent notifications
- Can mark as read individually or all at once
- Click navigates to relevant page

---

## Priority Matrix

| Priority | User Stories |
|----------|-------------|
| P0 - MVP | US-1.1, US-1.2, US-2.1, US-3.1, US-3.3, US-4.3, US-5.1, US-5.3 |
| P1 - Core | US-2.2, US-2.3, US-3.4, US-3.5, US-3.6, US-4.2, US-5.2, US-5.4, US-6.1 |
| P2 - Enhanced | US-1.3, US-2.4, US-4.4, US-4.5, US-5.5, US-5.6, US-6.2, US-8.1, US-8.2 |
| P3 - Polish | US-3.2, US-4.1, US-6.3, US-6.4, US-7.1, US-7.2, US-7.3 |

---

## Glossary

| Term | Definition |
|------|------------|
| Foster | A pet owner looking to rehome their pet |
| Adopter | A person seeking to adopt a pet |
| Rescue Organization | A verified entity that facilitates adoptions |
| Vet Sign-off | Veterinary verification that a pet is neutered, vaccinated, and healthy |
| Pet Status | The current stage in the adoption journey |
