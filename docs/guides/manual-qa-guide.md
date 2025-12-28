# Manual QA Testing Guide

#testing #qa #e2e

> Comprehensive end-to-end testing guide for the Forever Home pet adoption platform.

**Related Documentation:**
- [[domain-model]] - Entity definitions and relationships
- [[pet-status]] - Pet lifecycle state machine
- [[user-stories/index]] - Feature specifications

---

## Overview

This guide provides step-by-step test cases for manually testing the Forever Home platform. Tests are organized by user type and cover the complete user journey for each role.

### Test Environment Setup

**Prerequisites:**
- Application running locally (`./mvnw spring-boot:run`)
- Frontend running (if separate)
- PostgreSQL database running (auto-started via Docker Compose)

**Test URLs:**
- Frontend: `http://localhost:5173` (or configured port)
- Backend API: `http://localhost:8080`

### Test Accounts (Development Mode)

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@test.com | password123 |
| Foster | foster@test.com | password123 |
| Adopter | adopter@test.com | password123 |
| Vet | vet@test.com | password123 |
| Rescue Org | rescue@test.com | password123 |

---

## 1. Visitor (Unauthenticated User)

Visitors can browse the platform without logging in but cannot interact with pets or access dashboards.

### VIS-001: View Home Page

**Objective:** Verify the home page loads and displays platform information.

**Steps:**
1. Open browser and navigate to `/`
2. Observe the hero section
3. Look for platform mission/tagline
4. Verify featured pets section displays (if implemented)
5. Verify navigation links are visible

**Expected Results:**
- [ ] Home page loads without errors
- [ ] Hero section displays with clear messaging
- [ ] Navigation shows: Browse Pets, Login, Register
- [ ] Featured available pets display (if implemented)
- [ ] CTAs visible: "Adopt a Pet", "Rehome Your Pet"

---

### VIS-002: Browse Available Pets (Public)

**Objective:** Verify visitors can browse the public pet listings.

**Steps:**
1. Navigate to `/pets`
2. Observe the pet listing grid
3. Verify each pet card shows:
   - Primary image
   - Name
   - Breed
   - Age
   - Size
   - Sex
   - Status badge (Available)
4. Try clicking the heart/favorite icon (if visible)
5. Scroll to verify pagination or infinite scroll

**Expected Results:**
- [ ] Pet listing page loads
- [ ] Only pets with status "Available" are shown
- [ ] Pet cards display all required information
- [ ] Grid layout: 2 columns on mobile, 3-4 on desktop
- [ ] Heart icon either hidden or prompts login when clicked
- [ ] Pagination/infinite scroll works (if > 20 pets)

---

### VIS-003: Filter Pets

**Objective:** Verify pet filtering works for visitors.

**Steps:**
1. Navigate to `/pets`
2. Use the search box to search by pet name
3. Filter by species (Dogs, Cats, etc.)
4. Filter by size (Small, Medium, Large)
5. Filter by age range (if available)
6. Combine multiple filters
7. Clear all filters

**Expected Results:**
- [ ] Search by name filters results correctly
- [ ] Species filter works
- [ ] Size filter works
- [ ] Multiple filters combine (AND logic)
- [ ] Clear filters resets to show all available pets
- [ ] Results update in real-time (or with minimal delay)

---

### VIS-004: View Pet Detail Page (Public)

**Objective:** Verify visitors can view individual pet profiles.

**Steps:**
1. Navigate to `/pets`
2. Click on any pet card
3. Observe the pet detail page at `/pets/{id}`
4. Swipe/click through image gallery
5. Verify all pet information displays
6. Click "Apply to Adopt" button

**Expected Results:**
- [ ] Pet detail page loads
- [ ] Image gallery shows all pet photos
- [ ] Gallery is swipeable/navigable
- [ ] Pet details shown: name, breed, age, size, sex, description
- [ ] Vet verification badge visible (if verified)
- [ ] Rescue organization name and contact visible
- [ ] "Apply to Adopt" redirects to login/register page

---

### VIS-005: View Rescue Organization Profile

**Objective:** Verify visitors can view rescue organization public profiles.

**Steps:**
1. Navigate to a pet detail page
2. Click on the rescue organization name/link
3. Observe the rescue organization profile page

**Expected Results:**
- [ ] Organization profile page loads
- [ ] Shows: logo, name, description, location
- [ ] Contact information visible (phone, website)
- [ ] Social media links visible (if provided)
- [ ] List of available pets from this organization shown

---

### VIS-006: Registration Prompt

**Objective:** Verify visitors are prompted to register when attempting restricted actions.

**Steps:**
1. Navigate to `/pets/{id}`
2. Try to favorite a pet (click heart icon)
3. Try to apply for adoption (click apply button)

**Expected Results:**
- [ ] Attempting to favorite prompts login/registration
- [ ] Attempting to apply redirects to login page
- [ ] Clear messaging about why registration is needed

---

## 2. Adopter

Adopters browse pets, save favorites, and submit adoption applications.

### ADO-001: Adopter Registration

**Objective:** Verify new adopters can register accounts.

**Prerequisites:** No existing account with test email

**Steps:**
1. Navigate to `/register`
2. Select "Adopter" role (or equivalent option like "I want to adopt a pet")
3. Enter:
   - Email: unique test email
   - Password: meeting requirements (8+ chars, mixed case, number)
   - Confirm Password
4. Click "Create Account"
5. Check for email verification (if implemented)

**Expected Results:**
- [ ] Registration form accepts valid input
- [ ] Password validation enforces requirements
- [ ] Confirmation passwords must match
- [ ] Success message displayed
- [ ] Redirected to profile completion OR email verification pending
- [ ] Verification email received (if implemented)

---

### ADO-002: Adopter Login

**Objective:** Verify adopters can log in.

**Steps:**
1. Navigate to `/login`
2. Enter adopter credentials
3. Click "Sign In"
4. Observe redirect

**Expected Results:**
- [ ] Login form accepts credentials
- [ ] Successful login redirects to dashboard
- [ ] User name/avatar appears in navigation
- [ ] "Remember me" option available (if implemented)

---

### ADO-003: Complete Adopter Profile

**Objective:** Verify adopters must complete their profile before applying.

**Prerequisites:** Fresh adopter account without completed profile

**Steps:**
1. Login as adopter with incomplete profile
2. Observe redirect to profile completion
3. Fill in required fields:
   - First Name
   - Last Name
   - Phone
   - Location (City/State)
4. Fill in additional fields:
   - Living Situation
   - Pet Experience
5. Submit profile

**Expected Results:**
- [ ] User is prompted to complete profile
- [ ] Cannot skip profile completion
- [ ] All required fields validated
- [ ] Profile saves successfully
- [ ] Redirected to dashboard after completion

---

### ADO-004: View Adopter Dashboard

**Objective:** Verify the adopter dashboard displays correct information.

**Prerequisites:** Logged in as adopter

**Steps:**
1. Navigate to `/adopter/dashboard` (or click Dashboard in nav)
2. Observe dashboard sections

**Expected Results:**
- [ ] Dashboard loads without errors
- [ ] Stats overview visible (Favorites, Applications, etc.)
- [ ] "My Applications" section visible
- [ ] "Saved Pets" section visible
- [ ] "Browse Pets" CTA available
- [ ] Empty states show appropriately if no data

---

### ADO-005: Browse Pets (Authenticated)

**Objective:** Verify authenticated adopters can browse and favorite pets.

**Prerequisites:** Logged in as adopter

**Steps:**
1. Navigate to `/pets`
2. Browse pet listings
3. Click on a pet to view details
4. Return to listing and try another pet

**Expected Results:**
- [ ] Pet listing shows all available pets
- [ ] Heart/favorite icon is interactive
- [ ] Pet cards link to detail pages
- [ ] All filters work as expected

---

### ADO-006: Favorite a Pet

**Objective:** Verify adopters can save pets to favorites.

**Prerequisites:** Logged in as adopter with complete profile

**Steps:**
1. Navigate to `/pets`
2. Click heart icon on any pet card
   - OR: Navigate to pet detail and click heart in header
3. Observe heart icon state change (outline to filled)
4. Navigate to Dashboard
5. Check "Saved Pets" section

**Expected Results:**
- [ ] Heart icon toggles state on click
- [ ] Visual feedback confirms favorite added
- [ ] Pet appears in Dashboard "Saved Pets" section
- [ ] Can unfavorite by clicking again
- [ ] Favorites persist across sessions

---

### ADO-007: Submit Adoption Application

**Objective:** Verify adopters can apply to adopt a pet.

**Prerequisites:**
- Logged in as adopter with complete profile
- At least one pet in "Available" status
- Fewer than 3 active applications

**Steps:**
1. Navigate to pet detail page (`/pets/{id}`)
2. Click "Apply to Adopt"
3. Fill in application form:
   - Living Situation (50+ chars required)
   - Pet Experience (50+ chars required)
   - Why you want to adopt this pet (50+ chars required)
4. Submit application

**Expected Results:**
- [ ] Application form opens/displays
- [ ] Form pre-fills from profile data (if available)
- [ ] All fields required with minimum character counts
- [ ] Validation errors display for incomplete fields
- [ ] Success confirmation shown after submit
- [ ] Cannot submit duplicate application for same pet
- [ ] Application count limit enforced (max 3 active)

---

### ADO-008: Track Application Status

**Objective:** Verify adopters can view their application status.

**Prerequisites:** Logged in as adopter with at least one submitted application

**Steps:**
1. Navigate to Dashboard
2. View "My Applications" section
3. Click on an application to view details (if available)

**Expected Results:**
- [ ] All applications listed
- [ ] Each shows: pet photo, pet name, status, date
- [ ] Status badges correctly colored:
  - Submitted: Gray
  - Under Review: Gold/Yellow
  - Approved: Green
  - Rejected: Red
  - Withdrawn: Gray (strikethrough)
- [ ] Can click to view full application details

---

### ADO-009: Withdraw Application

**Objective:** Verify adopters can withdraw pending applications.

**Prerequisites:**
- Logged in as adopter
- At least one application NOT in Approved status

**Steps:**
1. Navigate to Dashboard
2. Find application in "Submitted" or "Under Review" status
3. Click withdraw/cancel option
4. Confirm withdrawal

**Expected Results:**
- [ ] Withdraw option available for non-approved applications
- [ ] Confirmation dialog appears
- [ ] Application status changes to "Withdrawn"
- [ ] Cannot withdraw approved applications

---

### ADO-010: View Favorite Updates

**Objective:** Verify adopters are notified of changes to favorited pets.

**Prerequisites:**
- Logged in as adopter
- At least one favorited pet

**Steps:**
1. Check notifications when favorited pet status changes
2. View notification bell (if implemented)

**Expected Results:**
- [ ] Notification when favorited pet status changes
- [ ] Notification when favorited pet is adopted

---

## 3. Foster

Fosters register pets for adoption and work with rescue organizations.

### FOS-001: Foster Registration

**Objective:** Verify new fosters can register accounts.

**Prerequisites:** No existing account with test email

**Steps:**
1. Navigate to `/register`
2. Select "Foster" role (or "I want to find a home for my pet")
3. Enter registration details
4. Complete registration

**Expected Results:**
- [ ] Registration form accepts valid input
- [ ] Foster role correctly assigned
- [ ] Success message displayed
- [ ] Redirected to profile completion

---

### FOS-002: Complete Foster Profile

**Objective:** Verify fosters must complete their profile.

**Prerequisites:** Fresh foster account

**Steps:**
1. Login as foster
2. Complete profile:
   - First Name
   - Last Name
   - Phone
   - Location (City/State)
3. Submit profile

**Expected Results:**
- [ ] Required fields validated
- [ ] Profile saves successfully
- [ ] Can now access dashboard
- [ ] Can now register pets

---

### FOS-003: View Foster Dashboard

**Objective:** Verify the foster dashboard displays correctly.

**Prerequisites:** Logged in as foster

**Steps:**
1. Navigate to `/foster/dashboard`
2. Observe dashboard sections

**Expected Results:**
- [ ] Dashboard loads without errors
- [ ] Stats: Drafts, Pending, Active, Completed
- [ ] "My Pets" list visible
- [ ] "Register New Pet" CTA available
- [ ] Notifications section (if implemented)

---

### FOS-004: Browse Rescue Organizations

**Objective:** Verify fosters can browse and select rescue organizations.

**Prerequisites:** Logged in as foster

**Steps:**
1. Navigate to rescue organizations list (before pet registration)
2. Browse available organizations
3. Filter by location (if available)
4. View organization details

**Expected Results:**
- [ ] List shows verified rescue organizations only
- [ ] Each org shows: logo, name, location, description
- [ ] Can click to view full profile
- [ ] Can select organization for pet registration

---

### FOS-005: Register Pet - Basic Info

**Objective:** Verify fosters can start registering a pet.

**Prerequisites:** Logged in as foster with complete profile

**Steps:**
1. Navigate to `/foster/pets/new` or click "Register New Pet"
2. Fill in basic pet information:
   - Name
   - Species (Dog, Cat, Rabbit, Bird, Other)
   - Breed
   - Age (number + unit: months/years)
   - Sex (Male/Female)
   - Size (Small/Medium/Large)
3. Continue to next step

**Expected Results:**
- [ ] Form accepts all required fields
- [ ] Species dropdown works
- [ ] Age unit selector (months/years) works
- [ ] Sex radio buttons work
- [ ] Size radio buttons work
- [ ] Cannot proceed with missing required fields

---

### FOS-006: Register Pet - Description & Microchip

**Objective:** Verify pet description and microchip entry.

**Steps:**
1. Continue pet registration
2. Enter:
   - Description (max 500 chars)
   - Microchip ID (required)
3. Select rescue organization to work with

**Expected Results:**
- [ ] Description field has character counter
- [ ] Description enforces 500 char max
- [ ] Microchip ID is required
- [ ] Microchip format validation (if applicable)
- [ ] Rescue organization selector works

---

### FOS-007: Register Pet - Images

**Objective:** Verify pet image upload functionality.

**Steps:**
1. Continue pet registration
2. Upload pet images:
   - Try drag-and-drop upload
   - Try click-to-browse upload
3. Upload multiple images (up to 5)
4. Set primary image
5. Reorder images (if supported)
6. Remove an image

**Expected Results:**
- [ ] Drag-drop upload works
- [ ] Click upload works
- [ ] Multiple images can be uploaded
- [ ] Maximum 5 images enforced
- [ ] Can designate primary image
- [ ] Can reorder images
- [ ] Can delete uploaded images
- [ ] Image preview displays

---

### FOS-008: Register Pet - Submit

**Objective:** Verify pet registration submission.

**Steps:**
1. Complete all pet registration steps
2. Review pet information (if review step exists)
3. Click "Submit for Review"

**Expected Results:**
- [ ] Summary/review shows all entered data
- [ ] Submit creates pet in "Draft" status first
- [ ] Submission moves status to "PendingRescue"
- [ ] Success confirmation shown
- [ ] Rescue organization is notified
- [ ] Pet appears on foster dashboard

---

### FOS-009: Edit Pet Profile

**Objective:** Verify fosters can edit pet details.

**Prerequisites:**
- Logged in as foster
- Pet in Draft or PendingRescue status

**Steps:**
1. Navigate to foster dashboard
2. Click on a pet to view details
3. Click "Edit" (if available based on status)
4. Modify pet details (except microchip)
5. Save changes

**Expected Results:**
- [ ] Edit available for Draft/PendingRescue status
- [ ] All fields editable except microchip
- [ ] Microchip field shows as read-only/disabled
- [ ] Changes save successfully
- [ ] Cannot edit pets in InProgress or later status

---

### FOS-010: View Pet Status

**Objective:** Verify fosters can track their pet's status.

**Prerequisites:** Logged in as foster with registered pet

**Steps:**
1. Navigate to foster dashboard
2. View pet status badge
3. Click on pet to view details
4. Check status timeline (if implemented)

**Expected Results:**
- [ ] Status badge visible on pet card
- [ ] Status correctly displays current state
- [ ] Timeline shows status history with dates
- [ ] Vet sign-off details visible when applicable

---

### FOS-011: Withdraw Pet

**Objective:** Verify fosters can withdraw a pet from adoption.

**Prerequisites:**
- Logged in as foster
- Pet NOT in "Adopted" status

**Steps:**
1. Navigate to pet detail page
2. Click "Withdraw Pet"
3. Confirm withdrawal in modal

**Expected Results:**
- [ ] Withdraw button visible for eligible statuses
- [ ] Confirmation modal appears
- [ ] Status changes to "Withdrawn"
- [ ] Rescue organization is notified
- [ ] If InProgress: requires rescue approval

---

### FOS-012: Resubmit Withdrawn Pet

**Objective:** Verify fosters can resubmit a withdrawn pet.

**Prerequisites:**
- Logged in as foster
- Pet in "Withdrawn" status

**Steps:**
1. Navigate to withdrawn pet
2. Click "Resubmit" or equivalent
3. Confirm resubmission

**Expected Results:**
- [ ] Resubmit option available for withdrawn pets
- [ ] Status changes to "PendingRescue"
- [ ] Rescue organization is notified

---

## 4. Rescue Organization

Rescue organizations accept pets, approve vets, and facilitate adoptions.

### RES-001: Rescue Organization Registration

**Objective:** Verify new rescue orgs can register.

**Steps:**
1. Navigate to `/register`
2. Select "Rescue Organization" role
3. Complete registration form
4. Submit

**Expected Results:**
- [ ] Registration successful
- [ ] Note displayed about admin approval required
- [ ] Account created but not verified
- [ ] Redirected to profile completion

---

### RES-002: Complete Organization Profile

**Objective:** Verify rescue orgs must complete their profile.

**Prerequisites:** Fresh rescue org account

**Steps:**
1. Login as rescue org
2. Complete organization profile:
   - Organization Name
   - Location (Address)
   - Phone
   - Contact Name
   - Contact Email
   - Website (optional)
   - Description (optional)
   - Logo (optional)
3. Add social media links (optional)
4. Submit for verification

**Expected Results:**
- [ ] All required fields validated
- [ ] Logo upload works
- [ ] Social media fields accept URLs
- [ ] Profile submits successfully
- [ ] Shows "Pending Admin Approval" message

---

### RES-003: View Dashboard (Unverified)

**Objective:** Verify unverified rescue orgs see limited dashboard.

**Prerequisites:**
- Logged in as rescue org
- NOT verified by admin

**Steps:**
1. Navigate to dashboard
2. Observe available features

**Expected Results:**
- [ ] Dashboard loads
- [ ] "Pending Approval" banner visible
- [ ] Cannot accept pets
- [ ] Cannot process applications
- [ ] View-only access to profile

---

### RES-004: View Dashboard (Verified)

**Objective:** Verify verified rescue orgs have full dashboard access.

**Prerequisites:**
- Logged in as verified rescue org

**Steps:**
1. Navigate to `/rescue/dashboard`
2. Observe all sections

**Expected Results:**
- [ ] Dashboard loads without errors
- [ ] Stats visible: Pending Review, Available, Applications, In Progress
- [ ] Pet Registration Queue visible
- [ ] Application Queue visible
- [ ] Active Listings visible
- [ ] Vet Approval Queue visible

---

### RES-005: Accept Pet Registration

**Objective:** Verify rescue orgs can accept pet registrations.

**Prerequisites:**
- Logged in as verified rescue org
- At least one pet in "PendingRescue" status for this org

**Steps:**
1. Navigate to dashboard
2. Find "Pending Review" section
3. View pet details
4. Click "Accept"
5. Confirm acceptance

**Expected Results:**
- [ ] Pet card shows in queue
- [ ] Can view full pet and foster details
- [ ] Accept button works
- [ ] Pet status changes to "PendingVet"
- [ ] Foster is notified to take pet to vet
- [ ] Acceptance shows microchip number for vet visit

---

### RES-006: Decline Pet Registration

**Objective:** Verify rescue orgs can decline pet registrations.

**Prerequisites:**
- Logged in as verified rescue org
- Pet in "PendingRescue" status

**Steps:**
1. Find pending pet
2. Click "Decline"
3. Enter reason in modal
4. Confirm decline

**Expected Results:**
- [ ] Decline modal appears
- [ ] Reason field available (optional or required)
- [ ] Pet status changes to "Draft"
- [ ] Foster is notified with reason

---

### RES-007: View Organization's Pets

**Objective:** Verify rescue orgs can view all their pets.

**Prerequisites:** Logged in as verified rescue org with pets

**Steps:**
1. Navigate to pet list
2. Filter by status
3. Sort by different criteria
4. Click on a pet to view details

**Expected Results:**
- [ ] All pets for this org displayed
- [ ] Status filter works
- [ ] Sort options work
- [ ] Aggregate counts shown
- [ ] Can click through to pet details

---

### RES-008: View Pending Vet Approvals

**Objective:** Verify rescue orgs can see vets requesting approval.

**Prerequisites:** Logged in as verified rescue org

**Steps:**
1. Navigate to Vet Approval Queue
2. View pending vet requests

**Expected Results:**
- [ ] Queue shows vets requesting approval
- [ ] Each shows: clinic name, license number, location
- [ ] Approve and Decline buttons visible

---

### RES-009: Approve Vet

**Objective:** Verify rescue orgs can approve vets.

**Prerequisites:**
- Logged in as verified rescue org
- At least one vet requesting approval

**Steps:**
1. Find vet in approval queue
2. Review vet details
3. Click "Approve"
4. Confirm approval

**Expected Results:**
- [ ] Vet details visible for review
- [ ] Approve button works
- [ ] VetApproval record created
- [ ] Vet can now verify pets for this rescue
- [ ] Vet is notified of approval

---

### RES-010: Decline Vet

**Objective:** Verify rescue orgs can decline vets.

**Steps:**
1. Find vet in approval queue
2. Click "Decline"
3. Enter reason (optional)
4. Confirm decline

**Expected Results:**
- [ ] Decline modal appears
- [ ] Can enter reason
- [ ] Vet removed from queue
- [ ] Vet is notified of decline

---

### RES-011: View Approved Vets

**Objective:** Verify rescue orgs can view their approved vets.

**Steps:**
1. Navigate to approved vets list
2. View approved vet details

**Expected Results:**
- [ ] List of approved vets displayed
- [ ] Shows: clinic name, location, approval date
- [ ] Revoke option available

---

### RES-012: Revoke Vet Approval

**Objective:** Verify rescue orgs can revoke vet approval.

**Steps:**
1. Find approved vet
2. Click "Revoke"
3. Confirm revocation

**Expected Results:**
- [ ] Confirmation modal appears
- [ ] Vet removed from approved list
- [ ] Vet can no longer verify pets for this rescue

---

### RES-013: Review Adoption Application

**Objective:** Verify rescue orgs can review adoption applications.

**Prerequisites:**
- Logged in as verified rescue org
- Pet with at least one application

**Steps:**
1. Navigate to Application Queue
2. Select an application
3. Review adopter details:
   - Living Situation
   - Pet Experience
   - Why This Pet

**Expected Results:**
- [ ] Applications listed for organization's pets
- [ ] Adopter profile information visible
- [ ] Application text fully readable
- [ ] Submission date shown
- [ ] Approve and Reject buttons visible

---

### RES-014: Approve Adoption Application

**Objective:** Verify rescue orgs can approve applications.

**Prerequisites:** Pet in "Available" status with application

**Steps:**
1. Find application
2. Click "Approve"
3. Confirm approval

**Expected Results:**
- [ ] Confirmation modal appears
- [ ] Application status → "Approved"
- [ ] Pet status → "InProgress"
- [ ] Foster and adopter notified
- [ ] Other applications for this pet can be rejected

---

### RES-015: Reject Adoption Application

**Objective:** Verify rescue orgs can reject applications.

**Steps:**
1. Find application
2. Click "Reject"
3. Enter rejection reason
4. Confirm rejection

**Expected Results:**
- [ ] Reject modal with reason field
- [ ] Application status → "Rejected"
- [ ] Adopter notified with reason (optional)
- [ ] Pet remains "Available"

---

### RES-016: Finalize Adoption

**Objective:** Verify rescue orgs can complete adoptions.

**Prerequisites:** Pet in "InProgress" status

**Steps:**
1. Navigate to pet detail
2. Click "Finalize Adoption" or equivalent
3. Confirm completion

**Expected Results:**
- [ ] Confirmation modal with checkbox
- [ ] Pet status → "Adopted"
- [ ] Adoption record created
- [ ] All parties notified
- [ ] Pet removed from public listings

---

### RES-017: Cancel In-Progress Adoption

**Objective:** Verify rescue orgs can cancel adoptions that fall through.

**Prerequisites:** Pet in "InProgress" status

**Steps:**
1. Navigate to pet detail
2. Click "Cancel Adoption"
3. Enter reason
4. Confirm cancellation

**Expected Results:**
- [ ] Reason field available
- [ ] Pet status → "Available"
- [ ] Pet returns to public listings
- [ ] Adopter notified

---

### RES-018: Place Pet On Hold

**Objective:** Verify rescue orgs can place pets on hold.

**Prerequisites:** Pet in "Available" status

**Steps:**
1. Navigate to pet detail
2. Click "Place On Hold"
3. Select/enter reason
4. Confirm

**Expected Results:**
- [ ] Pet status → "OnHold"
- [ ] Pet visible but marked as "On Hold" publicly
- [ ] No new applications accepted
- [ ] Can lift hold later

---

### RES-019: Lift Pet Hold

**Objective:** Verify rescue orgs can remove hold status.

**Prerequisites:** Pet in "OnHold" status

**Steps:**
1. Navigate to pet detail
2. Click "Lift Hold"
3. Confirm

**Expected Results:**
- [ ] Pet status → "Available"
- [ ] Pet accepts applications again

---

## 5. Veterinarian

Vets verify pet health and provide sign-offs for adoption readiness.

### VET-001: Vet Registration

**Objective:** Verify new vets can register.

**Steps:**
1. Navigate to `/register`
2. Select "Vet" role
3. Complete registration

**Expected Results:**
- [ ] Registration successful
- [ ] Note about rescue org verification required
- [ ] Redirected to profile completion

---

### VET-002: Complete Vet Profile

**Objective:** Verify vets must complete their professional profile.

**Prerequisites:** Fresh vet account

**Steps:**
1. Login as vet
2. Complete profile:
   - Clinic Name
   - License Number
   - Location
   - Phone
   - Website (optional)
   - Description (optional)
   - Logo (optional)
3. Submit for verification

**Expected Results:**
- [ ] Required fields validated
- [ ] License number format validated (if applicable)
- [ ] Profile submits successfully
- [ ] "Pending Verification" message shown

---

### VET-003: Request Rescue Org Approval

**Objective:** Verify vets can request approval from rescue organizations.

**Prerequisites:**
- Logged in as vet
- Profile complete

**Steps:**
1. Navigate to rescue organization list
2. Find organization to request approval from
3. Click "Request Approval" or equivalent
4. Confirm request

**Expected Results:**
- [ ] List of rescue organizations shown
- [ ] Can request approval from specific orgs
- [ ] Request sent successfully
- [ ] Shows pending status for requested orgs

---

### VET-004: View Dashboard (Unverified)

**Objective:** Verify unverified vets see limited dashboard.

**Prerequisites:**
- Logged in as vet
- NOT verified by any rescue org

**Steps:**
1. Navigate to dashboard
2. Observe available features

**Expected Results:**
- [ ] Dashboard loads
- [ ] "Pending Verification" banner visible
- [ ] Pet lookup disabled
- [ ] Cannot sign off on pets

---

### VET-005: View Dashboard (Verified)

**Objective:** Verify verified vets have full dashboard access.

**Prerequisites:**
- Logged in as vet
- Verified by at least one rescue org

**Steps:**
1. Navigate to `/vet/dashboard`
2. Observe all sections

**Expected Results:**
- [ ] Dashboard loads without errors
- [ ] Stats: Total Sign-offs, This Month
- [ ] Pet lookup search box available
- [ ] Recent sign-offs list visible

---

### VET-006: Look Up Pet by Microchip

**Objective:** Verify vets can find pets by microchip number.

**Prerequisites:**
- Logged in as verified vet
- Pet in "PendingVet" status with known microchip

**Steps:**
1. Navigate to dashboard
2. Enter microchip number in search
3. Click "Search" or "Look Up"

**Expected Results:**
- [ ] Search accepts microchip input
- [ ] Pet found and details displayed:
  - Name, breed, age, sex
  - Images
  - Foster contact info
  - Rescue organization info
- [ ] "Begin Verification" button visible
- [ ] Error shown if microchip not found
- [ ] Error shown if pet not in "PendingVet" status
- [ ] Error shown if vet not approved by pet's rescue org

---

### VET-007: Complete Vet Sign-Off

**Objective:** Verify vets can sign off on pets.

**Prerequisites:**
- Pet found via microchip lookup
- Vet approved by pet's rescue org

**Steps:**
1. Click "Begin Verification" on found pet
2. Complete sign-off form:
   - Neutered/Spayed: checkbox + date
   - Vaccinations: add vaccine name + date (multiple)
   - Health Status: Good / Known Conditions
   - Health Notes: (required if Known Conditions)
3. Upload attachments (optional)
4. Click "Complete Sign-Off"
5. Confirm

**Expected Results:**
- [ ] Form displays all required fields
- [ ] Neutered date required when checked
- [ ] At least 1 vaccination required
- [ ] Common vaccines in dropdown (Rabies, DHPP, etc.)
- [ ] Health notes required if "Known Conditions"
- [ ] File upload works for attachments
- [ ] Sign-off creates immutable record
- [ ] Pet status → "Available"
- [ ] Foster and rescue notified
- [ ] Success confirmation shown

---

### VET-008: Decline Sign-Off

**Objective:** Verify vets can decline signing off.

**Prerequisites:** Pet found via microchip lookup

**Steps:**
1. Click "Decline" on found pet
2. Select reasons:
   - Not neutered/spayed
   - Vaccinations incomplete
   - Health concerns
3. Enter notes/recommendations
4. Confirm decline

**Expected Results:**
- [ ] Reason checkboxes available
- [ ] Notes field available (min 50 chars)
- [ ] Decline modal with confirmation
- [ ] Pet status → "PendingRescue"
- [ ] Foster and rescue notified with reasons
- [ ] No VetSignOff record created

---

### VET-009: View Sign-Off History

**Objective:** Verify vets can view their verification history.

**Prerequisites:**
- Logged in as vet
- At least one completed sign-off

**Steps:**
1. Navigate to sign-off history
2. View list of past sign-offs
3. Search/filter by date or pet name
4. Click on entry to view details

**Expected Results:**
- [ ] All past sign-offs listed
- [ ] Shows: pet name, date, current status
- [ ] Can search/filter
- [ ] Can view individual sign-off details
- [ ] Export/download option (if implemented)

---

## 6. Admin

Admins manage the platform, verify organizations, and moderate content.

### ADM-001: Admin Login

**Objective:** Verify admin login works.

**Note:** First admin is created via `ADMIN_EMAIL` environment variable.

**Steps:**
1. Navigate to `/login`
2. Enter admin credentials
3. Click "Sign In"

**Expected Results:**
- [ ] Login successful
- [ ] Redirected to admin dashboard
- [ ] Full admin navigation available

---

### ADM-002: View Admin Dashboard

**Objective:** Verify admin dashboard displays key metrics.

**Prerequisites:** Logged in as admin

**Steps:**
1. Navigate to `/admin/dashboard`
2. Observe dashboard sections

**Expected Results:**
- [ ] Dashboard loads without errors
- [ ] Stats visible: Total Users, Total Pets, Adoptions, Pending
- [ ] Rescue Org Approval Queue visible
- [ ] Moderation Queue visible (if implemented)
- [ ] Recent Activity feed

---

### ADM-003: View Pending Rescue Organizations

**Objective:** Verify admin can see pending rescue org approvals.

**Steps:**
1. Navigate to Approvals tab/section
2. View list of pending organizations

**Expected Results:**
- [ ] Queue shows unverified rescue orgs
- [ ] Each shows: name, location, contact, submitted date
- [ ] Approve and Reject buttons visible

---

### ADM-004: Approve Rescue Organization

**Objective:** Verify admin can approve rescue organizations.

**Prerequisites:** At least one pending rescue org

**Steps:**
1. Find pending organization
2. Click "View" to review details
3. Verify organization information
4. Click "Approve"
5. Confirm approval

**Expected Results:**
- [ ] Can view full organization profile
- [ ] Approve button works
- [ ] Organization.verified → true
- [ ] Organization notified via email
- [ ] Org can now accept pets

---

### ADM-005: Reject Rescue Organization

**Objective:** Verify admin can reject rescue organizations.

**Steps:**
1. Find pending organization
2. Click "Reject"
3. Enter rejection reason
4. Confirm rejection

**Expected Results:**
- [ ] Reject modal with reason field (required)
- [ ] Organization status updated
- [ ] Organization notified with reason
- [ ] Can allow resubmission or suspend

---

### ADM-006: Search Users

**Objective:** Verify admin can search and view users.

**Steps:**
1. Navigate to User Management
2. Use search box to find user by name/email
3. Filter by role (Foster, Adopter, Vet, Rescue Org)
4. Filter by status (Active, Pending, Suspended)

**Expected Results:**
- [ ] Search returns matching users
- [ ] Role filter works
- [ ] Status filter works
- [ ] User table displays: Name, Email, Role, Status, Joined, Last Active

---

### ADM-007: View User Details

**Objective:** Verify admin can view user information.

**Steps:**
1. Find user in management table
2. Click to view details

**Expected Results:**
- [ ] User profile information shown
- [ ] Activity summary visible
- [ ] Role-specific details (e.g., pets for foster, applications for adopter)

---

### ADM-008: Suspend User

**Objective:** Verify admin can suspend user accounts.

**Steps:**
1. Find active user
2. Click "Suspend" from actions menu
3. Enter suspension reason
4. Confirm suspension

**Expected Results:**
- [ ] Confirmation modal with reason field
- [ ] User status → "Suspended"
- [ ] User logged out immediately
- [ ] User cannot log in
- [ ] User notified via email

---

### ADM-009: Reactivate User

**Objective:** Verify admin can reactivate suspended users.

**Prerequisites:** At least one suspended user

**Steps:**
1. Find suspended user
2. Click "Reactivate" from actions menu
3. Confirm reactivation

**Expected Results:**
- [ ] Reactivate option visible for suspended users
- [ ] User status → "Active"
- [ ] User can log in again
- [ ] User notified via email

---

### ADM-010: Trigger Password Reset

**Objective:** Verify admin can send password reset emails.

**Steps:**
1. Find any user
2. Click "Reset Password" from actions menu
3. Confirm

**Expected Results:**
- [ ] Reset triggered successfully
- [ ] User receives password reset email
- [ ] Admin sees confirmation

---

### ADM-011: View Platform Analytics

**Objective:** Verify admin can view platform statistics.

**Steps:**
1. Navigate to Analytics tab
2. View metrics and charts

**Expected Results:**
- [ ] User counts by role
- [ ] Pet counts by status
- [ ] Adoption rate metric
- [ ] Average time to adoption
- [ ] Charts/graphs (if implemented)
- [ ] Date range selector
- [ ] Export option (if implemented)

---

### ADM-012: Moderate Flagged Content

**Objective:** Verify admin can review and act on flagged content.

**Prerequisites:** At least one flagged item (if feature implemented)

**Steps:**
1. Navigate to Moderation Queue
2. View flagged item
3. Take action: Dismiss, Remove, Warn User, Suspend User

**Expected Results:**
- [ ] Flagged items listed with reason and severity
- [ ] Full content visible for review
- [ ] All action options available
- [ ] Actions logged for audit
- [ ] Users notified of actions taken

---

### ADM-013: Create Additional Admin

**Objective:** Verify admin can create other admin accounts.

**Steps:**
1. Navigate to admin user creation
2. Enter new admin email
3. Confirm creation

**Expected Results:**
- [ ] New admin account created
- [ ] New admin receives setup email
- [ ] New admin can log in after setup

---

## 7. Cross-Role Workflows

These tests verify complete end-to-end workflows across multiple user roles.

### E2E-001: Complete Adoption Journey

**Objective:** Test the full adoption flow from pet registration to adoption.

**Actors:** Foster, Rescue Org, Vet, Adopter

**Steps:**
1. **Foster:** Register and submit pet
2. **Rescue Org:** Accept pet registration
3. **Foster:** Note microchip number for vet visit
4. **Vet:** Look up pet by microchip
5. **Vet:** Complete sign-off
6. **Adopter:** Browse and find pet
7. **Adopter:** Submit adoption application
8. **Rescue Org:** Approve application
9. **Rescue Org:** Finalize adoption

**Expected Results:**
- [ ] Pet moves through statuses: Draft → PendingRescue → PendingVet → Available → InProgress → Adopted
- [ ] All parties notified at each step
- [ ] Adoption record created at end

---

### E2E-002: Rescue Organization Onboarding

**Objective:** Test complete rescue org setup and first pet.

**Actors:** Rescue Org, Admin, Foster

**Steps:**
1. **Rescue Org:** Register account
2. **Rescue Org:** Complete profile
3. **Admin:** Approve rescue organization
4. **Foster:** Submit pet to this organization
5. **Rescue Org:** Accept pet

**Expected Results:**
- [ ] Rescue org verified by admin
- [ ] Can accept pets after verification
- [ ] Pet workflow proceeds normally

---

### E2E-003: Vet Approval and First Sign-Off

**Objective:** Test vet onboarding and first pet verification.

**Actors:** Vet, Rescue Org, Foster

**Steps:**
1. **Vet:** Register and complete profile
2. **Vet:** Request approval from rescue org
3. **Rescue Org:** Approve vet
4. **Foster:** Submit pet (accepted by rescue)
5. **Vet:** Look up pet by microchip
6. **Vet:** Complete sign-off

**Expected Results:**
- [ ] Vet approved by rescue org
- [ ] Can look up pets for that org
- [ ] Sign-off completes successfully

---

### E2E-004: Adoption Fall-Through and Recovery

**Objective:** Test adoption cancellation and pet return to availability.

**Actors:** Adopter, Rescue Org, Second Adopter

**Steps:**
1. **Adopter:** Apply for pet
2. **Rescue Org:** Approve application (InProgress)
3. **Rescue Org:** Cancel adoption (falls through)
4. **Second Adopter:** Apply for same pet
5. **Rescue Org:** Approve and finalize

**Expected Results:**
- [ ] First adopter notified of cancellation
- [ ] Pet returns to Available
- [ ] Second application can be approved
- [ ] Final adoption succeeds

---

### E2E-005: Pet Withdrawal and Resubmission

**Objective:** Test foster withdrawing and resubmitting a pet.

**Actors:** Foster, Rescue Org

**Steps:**
1. **Foster:** Submit pet
2. **Rescue Org:** Accept pet
3. **Foster:** Withdraw pet
4. **Foster:** Resubmit pet later
5. **Rescue Org:** Accept again

**Expected Results:**
- [ ] Pet status → Withdrawn
- [ ] Rescue org notified
- [ ] Pet can be resubmitted
- [ ] Returns to PendingRescue on resubmission

---

## Appendix A: Status Reference

### Pet Status Transitions
```
Draft → PendingRescue → PendingVet → Available → InProgress → Adopted
                ↑           │              │           │
                └───────────┘              │           │
                (vet decline)              │           │
                                           ↓           ↓
                                       OnHold    Available
                                                  (cancelled)

Any (except Adopted) → Withdrawn → PendingRescue (resubmit)
```

### Application Status Transitions
```
Submitted → Under Review → Approved
                       ↘ Rejected
Any (except Approved) → Withdrawn
```

---

## Appendix B: Common Issues Checklist

Before marking a test as passing, verify:

- [ ] No console errors (browser dev tools)
- [ ] No 4xx or 5xx API errors (network tab)
- [ ] Loading states display during async operations
- [ ] Error states display for failed operations
- [ ] Success messages confirm completed actions
- [ ] Notifications sent to relevant parties
- [ ] Data persists after page refresh
- [ ] Responsive design works on mobile viewports

---

## Appendix C: Test Data Cleanup

After testing, consider cleaning up test data:

1. **Delete test pets** not needed for future testing
2. **Reset test accounts** to initial state
3. **Clear test favorites** and applications
4. **Remove test images** from storage

Or use the test data seeder to reset to a known state.
