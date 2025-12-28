# Domain Model

#mvp #architecture

> This document defines the core types in the Forever Home domain and the rationale for their existence.

**Related Documentation:**
- [[Roadmap]] - Implementation phases and status
- [[pet-status]] - Pet lifecycle state machine
- [[user-stories/index]] - User stories by role

## Domain Overview

Forever Home operates on a trust-based adoption model where pets must pass through verified intermediaries before reaching adopters. This ensures animal welfare standards are met and protects all parties involved.

```
┌─────────┐         ┌─────────────────┐         ┌─────────┐
│ Foster  │────────▶│ Rescue Org      │────────▶│ Adopter │
└─────────┘         └────────┬────────┘         └─────────┘
     │                       │
     │                       │
     ▼                       ▼
┌─────────┐         ┌─────────────────┐
│   Pet   │────────▶│      Vet        │
└─────────┘         └─────────────────┘
```

---

## User Types

### User
**Purpose:** Base identity for anyone interacting with the platform.

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Unique identifier |
| email | String | Login credential, unique |
| passwordHash | String | Encrypted password (null for Google-only users) |
| googleId | String | Google account ID (for OAuth users) |
| role | UserRole | Discriminator for user type |
| createdAt | Timestamp | Account creation date |
| lastLoginAt | Timestamp | Most recent login |
| status | AccountStatus | Active, Suspended, Pending |
| profileComplete | Boolean | Has completed role-specific profile |
| emailVerified | Boolean | Email has been verified (auto-true for Google users) |
| emailVerificationToken | String | Token for email verification |
| emailVerificationExpiry | Timestamp | When verification token expires |
| passwordResetToken | String | Token for password reset |
| passwordResetExpiry | Timestamp | When reset token expires |
| failedLoginAttempts | Integer | Count of consecutive failed logins |
| lockedUntil | Timestamp | Account lockout expiry (after 5 failed attempts) |
| notificationPrefs | NotificationPreferences | Email/in-app notification settings |

**Why it exists:** Provides authentication and authorization foundation. All specific user types extend from this base, enabling a unified login system while supporting role-specific functionality.

**Security Features:**
- Account locks after 5 failed login attempts (15-minute lockout)
- Email verification required before full access
- Password reset with 24-hour expiry tokens
- Google OAuth with automatic email verification

---

### Foster
**Purpose:** Represents a pet owner seeking to rehome their pet.

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Unique identifier |
| userId | UUID | Link to User |
| firstName | String | Foster's first name |
| lastName | String | Foster's last name |
| phone | String | Contact number |
| location | Address | General location (city/state) |

**Why it exists:** Fosters are the entry point for pets into the system. They have emotional investment in their pet's future and need visibility into the adoption process. Separating Foster from User allows a person to be both a Foster and Adopter with different pets.

**Relationships:**
- Has many Pets (as registrant)
- Works with Rescue Organizations

---

### Adopter
**Purpose:** Represents someone seeking to adopt a pet.

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Unique identifier |
| userId | UUID | Link to User |
| firstName | String | Adopter's first name |
| lastName | String | Adopter's last name |
| phone | String | Contact number |
| location | Address | Where pet will live |
| livingSituation | String | House, apartment, etc. |
| petExperience | Text | Previous pet ownership |

**Why it exists:** Adopters need profiles to submit applications. Capturing living situation and experience helps rescue organizations evaluate suitability without repeated questioning.

**Relationships:**
- Submits many Adoption Applications
- Favorites many Pets

---

### Vet
**Purpose:** Represents a veterinary professional who verifies pet health and compliance.

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Unique identifier |
| userId | UUID | Link to User |
| clinicName | String | Name of veterinary practice |
| licenseNumber | String | Professional license ID |
| location | Address | Clinic address |
| phone | String | Contact number |
| website | String | Clinic website |
| description | Text | About the practice |
| logo | Image | Clinic branding |
| verified | Boolean | Rescue organization has verified this vet |

**Why it exists:** Vets serve as trusted third parties who verify that pets meet adoption requirements (neutered, vaccinated, healthy). Their professional credentials add legitimacy to the verification process. Requiring vet sign-off protects adopters from receiving pets with undisclosed health issues and ensures basic animal welfare standards.

**Verification Note:** Vets are verified by rescue organizations (not admins). This ensures each rescue explicitly trusts the vets who perform health checks on their pets. A vet may be verified by multiple rescue organizations.

**Relationships:**
- Creates many Vet Sign-offs
- Looks up Pets by microchip number (no pre-assignment required)
- Approved by many Rescue Organizations (via VetApproval)

---

### Rescue Organization
**Purpose:** Represents a registered rescue that facilitates adoptions.

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Unique identifier |
| userId | UUID | Link to User (primary contact) |
| name | String | Organization name |
| location | Address | Physical address |
| phone | String | Contact number |
| website | String | Organization website |
| description | Text | Mission and about |
| logo | Image | Organization branding |
| contactName | String | Primary contact person |
| contactEmail | String | Primary contact email |
| socialLinks | SocialLinks | Facebook, Instagram, etc. |
| verified | Boolean | Admin has verified legitimacy |

**Why it exists:** Rescue organizations are the central orchestrators of the adoption process. They exist because:
1. **Legal protection:** Many jurisdictions require adoptions go through registered rescues
2. **Quality control:** They screen both pets and adopters
3. **Accountability:** They provide a traceable chain of custody
4. **Support:** They can intervene if adoptions don't work out

**Relationships:**
- Manages many Pets
- Works with many Fosters
- Processes many Adoption Applications
- Approves many Vets (via VetApproval)

---

### Admin
**Purpose:** Platform administrator with elevated privileges.

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Unique identifier |
| userId | UUID | Link to User |

**Why it exists:** Admins ensure platform integrity by verifying rescue organizations, moderating content, and handling disputes. Kept minimal as most admin context comes from the User role.

**Note:** Vets are verified by rescue organizations, not admins. See the Vet entity for details.

---

### Vet Approval
**Purpose:** Records which rescue organizations have approved which vets.

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Unique identifier |
| vetId | UUID | The approved vet |
| rescueOrgId | UUID | The approving rescue organization |
| approvedAt | Timestamp | When approval was granted |
| approvedBy | UUID | User who approved (rescue org representative) |

**Why it exists:** Rescue organizations need to explicitly trust the vets who perform health checks on their pets. This many-to-many relationship allows:
1. **Organization-specific trust:** Each rescue controls which vets can verify their pets
2. **Vet flexibility:** Vets can work with multiple rescue organizations
3. **Audit trail:** Records who approved each vet and when

**Business Rule:** When a vet looks up a pet by microchip, the system checks if the vet is approved by the rescue organization that manages that pet.

---

### Vet Approval Request
**Purpose:** Tracks requests from vets to work with specific rescue organizations.

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Unique identifier |
| vetId | UUID | Requesting vet |
| rescueOrgId | UUID | Target rescue organization |
| status | VetApprovalRequestStatus | Pending, Approved, Rejected |
| requestedAt | Timestamp | When request was made |
| processedAt | Timestamp | When request was handled |
| processedBy | UUID | Who handled the request |
| rejectionReason | Text | If rejected, why |

**Why it exists:** Enables a bilateral approval system where:
1. **Vets can proactively request** approval from rescues they want to work with
2. **Rescues review and approve/reject** requests
3. **Prevents spam:** Vets must explicitly request approval
4. **Audit trail:** Records all approval decisions

---

## Authentication

### JWT Token Strategy

| Token | Storage | Expiry | Purpose |
|-------|---------|--------|---------|
| Access Token | Memory (client) | 15 minutes | API authorization |
| Refresh Token | httpOnly cookie | 7 days (30 with "Remember me") | Obtain new access tokens |

**Security Features:**
- **Token Rotation:** Refresh tokens are rotated on each use - old tokens are immediately revoked
- **Account Lockout:** 5 failed login attempts triggers 15-minute lockout
- **Email Verification:** Required before full access

**Endpoints:**
- `POST /auth/register` - Create account, returns tokens
- `POST /auth/login` - Authenticate, returns tokens
- `POST /auth/refresh` - Exchange refresh cookie for new access token (rotates refresh token)
- `POST /auth/logout` - Invalidate refresh token
- `POST /auth/google` - Google OAuth authentication
- `POST /auth/google/complete-registration` - Complete Google registration with role selection

**Token Payload:**
```json
{
  "sub": "user-uuid",
  "role": "Foster|Adopter|Vet|RescueOrg|Admin",
  "verified": true,
  "iat": 1234567890,
  "exp": 1234568790
}
```

**Why JWT:** Stateless authentication scales horizontally. Short-lived access tokens limit exposure if compromised. Refresh tokens in httpOnly cookies prevent XSS theft. Token rotation prevents replay attacks.

### Google OAuth

| Field | Type | Description |
|-------|------|-------------|
| googleId | String | Google account ID (linked to User) |

**Flow:**
1. User clicks "Sign in with Google"
2. Google returns ID token
3. If new user: redirect to role selection page
4. If existing user: authenticate and return tokens
5. Google users have automatic email verification

---

## Core Entities

### Pet
**Purpose:** The central entity - an animal seeking a forever home.

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Unique identifier |
| name | String | Pet's name |
| species | Species | Dog, Cat, etc. |
| breed | String | Breed or mix |
| age | Integer | Age in years |
| ageUnit | AgeUnit | Years or Months |
| sex | PetSex | Male or Female |
| size | PetSize | Small, Medium, Large |
| description | Text | Personality, history, needs (max 500 chars) |
| microchipId | String | Microchip number (required, immutable) |
| status | PetStatus | Current lifecycle stage |
| moderationStatus | ModerationStatus | AI moderation status (PENDING, APPROVED, FLAGGED, REJECTED) |
| fosterId | UUID | Who registered this pet (null for rescue-owned) |
| rescueOrgId | UUID | Managing organization |
| createdAt | Timestamp | Registration date |
| updatedAt | Timestamp | Last modification |

**Why it exists:** Pets are the reason the platform exists. The Pet entity captures everything an adopter needs to make an informed decision and everything the system needs to track the adoption journey.

**Key design decisions:**
- **Microchip is immutable:** Prevents fraud and ensures traceability
- **Microchip enables vet lookup:** Vets find pets by microchip number rather than being assigned (see US-4.2)
- **Status is denormalized:** Avoids complex joins for the most common query (listing available pets)
- **Age as integer + unit:** Handles puppies/kittens (months) and adults (years) cleanly
- **No vet assignment:** Any verified vet can sign off on any `PendingVet` pet via microchip lookup
- **Dual ownership models:** Pets can be foster-owned (fosterId set) or rescue-owned (fosterId null, rescueOrgId set)
- **AI moderation:** Pets must pass moderation before becoming publicly visible

**Relationships:**
- Belongs to one Foster (registrant) OR is rescue-owned (fosterId null)
- Belongs to one Rescue Organization
- Has many Pet Images
- Has one Vet Sign-off (when verified)
- Has many Adoption Applications

---

### Pet Image
**Purpose:** Photos of the pet to help adopters connect emotionally.

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Unique identifier |
| petId | UUID | Parent pet |
| url | String | Image storage location |
| isPrimary | Boolean | Main listing photo |
| uploadedAt | Timestamp | When uploaded |
| order | Integer | Display sequence |

**Why it exists:** Images significantly increase adoption rates. Separated from Pet to:
1. Support multiple images (max 5)
2. Enable lazy loading for performance
3. Allow reordering without updating Pet record

---

### Vet Sign-off
**Purpose:** Immutable record of veterinary verification.

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Unique identifier |
| petId | UUID | Pet being verified |
| vetId | UUID | Verifying veterinarian |
| neuteredDate | Date | When neutered/spayed |
| vaccinationRecords | VaccinationRecord[] | List of vaccines |
| healthStatus | HealthStatus | Good, Known Conditions |
| healthNotes | Text | Details on conditions |
| signedOffAt | Timestamp | Verification date |
| attachments | Attachment[] | Medical documents |

**Why it exists:** The sign-off is the gate between "pending" and "available." It exists as a separate entity because:
1. **Immutability:** Once signed, it cannot be modified (audit trail)
2. **Accountability:** Links verification to specific vet
3. **Compliance:** Captures the three required checks (neutered, vaccinated, healthy)
4. **Documentation:** Stores supporting medical records

---

### Vaccination Record
**Purpose:** Individual vaccination entry within a sign-off.

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Unique identifier |
| signOffId | UUID | Parent sign-off |
| vaccineName | String | e.g., Rabies, DHPP |
| dateAdministered | Date | When given |
| expirationDate | Date | When booster needed |

**Why it exists:** Vaccines have different schedules and expiration dates. Normalizing this allows accurate tracking and potential future features like "booster due" alerts.

---

### Adoption Application
**Purpose:** An adopter's formal request to adopt a specific pet.

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Unique identifier |
| petId | UUID | Pet being applied for |
| adopterId | UUID | Applicant |
| status | ApplicationStatus | Submitted, Under Review, Approved, Rejected |
| livingSituation | Text | Housing details |
| petExperience | Text | History with animals |
| whyAdopt | Text | Motivation |
| submittedAt | Timestamp | Application date |
| reviewedAt | Timestamp | Decision date |
| reviewedBy | UUID | Rescue org user who reviewed |
| rejectionReason | Text | If rejected, why |

**Why it exists:** Applications formalize adopter intent and give rescue organizations information to evaluate fit. Stored as a separate entity to:
1. Support multiple applications per pet
2. Track application history for both adopters and rescues
3. Enable the In Progress → Adopted workflow

---

### Adoption
**Purpose:** Record of a completed adoption.

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Unique identifier |
| petId | UUID | Adopted pet |
| fosterId | UUID | Original owner |
| adopterId | UUID | New owner |
| rescueOrgId | UUID | Facilitating rescue |
| vetId | UUID | Verifying vet |
| applicationId | UUID | Approved application |
| adoptedAt | Timestamp | Completion date |

**Why it exists:** The adoption record is the "receipt" of a successful adoption. It captures the complete chain of custody and serves as:
1. **Legal record:** Proof of transfer
2. **Success metric:** Platform analytics
3. **Reference:** If issues arise post-adoption

---

## Supporting Entities

### Address
**Purpose:** Reusable location structure.

| Field | Type | Description |
|-------|------|-------------|
| street | String | Street address (optional for privacy) |
| city | String | City |
| state | String | State/Province |
| postalCode | String | ZIP/Postal code |
| country | String | Country |

**Why it exists:** Multiple entities need locations. Standardizing the structure ensures consistent display and enables future geo-search features.

---

### Social Links
**Purpose:** Collection of social media URLs.

| Field | Type | Description |
|-------|------|-------------|
| facebook | String | Facebook page URL |
| instagram | String | Instagram handle |
| twitter | String | Twitter/X handle |
| tiktok | String | TikTok handle |

**Why it exists:** Rescue organizations use social media for outreach. Structured separately to allow null values without cluttering the main entity.

---

### Notification
**Purpose:** System message to a user.

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Unique identifier |
| userId | UUID | Recipient |
| type | NotificationType | Category of notification |
| title | String | Short summary |
| message | Text | Full content |
| link | String | Relevant page URL |
| read | Boolean | Has been viewed |
| createdAt | Timestamp | When generated |

**Why it exists:** Keeps users informed of adoption progress without requiring constant platform checks. Separated from email to support in-app notification center.

---

### Favorite
**Purpose:** Tracks pets that adopters have saved for later.

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Unique identifier |
| adopterId | UUID | Who favorited |
| petId | UUID | Which pet |
| createdAt | Timestamp | When favorited |

**Why it exists:** Allows adopters to build a shortlist of pets they're interested in. Separated as join table to support efficient queries and notifications when favorited pets' status changes.

---

### Pet Status History
**Purpose:** Audit trail of all pet status transitions.

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Unique identifier |
| petId | UUID | Pet whose status changed |
| fromStatus | PetStatus | Previous status |
| toStatus | PetStatus | New status |
| reason | Text | Why status changed |
| changedBy | UUID | User who made the change |
| changedAt | Timestamp | When change occurred |

**Why it exists:** Provides complete audit trail for:
1. **Transparency:** Fosters and adopters can see the pet's journey
2. **Debugging:** Track issues in the adoption flow
3. **Analytics:** Understand typical pet lifecycles
4. **Compliance:** Legal record of custody changes

---

### Content Flag
**Purpose:** Moderator-submitted flags for problematic content.

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Unique identifier |
| contentType | ContentType | PET, PROFILE, APPLICATION |
| contentId | UUID | ID of flagged content |
| reason | Text | Why content was flagged |
| flaggedBy | UUID | User who flagged |
| flaggedAt | Timestamp | When flagged |
| status | FlagStatus | PENDING, APPROVED, DISMISSED |
| resolvedBy | UUID | Admin who resolved |
| resolvedAt | Timestamp | When resolved |
| resolution | Text | Resolution notes |

**Why it exists:** Enables community moderation and admin oversight of platform content.

---

### Audit Log
**Purpose:** Records administrative actions for accountability.

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Unique identifier |
| adminId | UUID | Admin who took action |
| action | String | Action type (APPROVE, REJECT, SUSPEND, etc.) |
| targetType | String | Entity type affected |
| targetId | UUID | ID of affected entity |
| details | Text | Additional context |
| createdAt | Timestamp | When action occurred |

**Why it exists:** Provides accountability for administrative actions and enables investigation of disputes.

---

### NotificationPreferences
**Purpose:** User settings for notification delivery (embedded in User).

| Field | Type | Description |
|-------|------|-------------|
| emailStatusChanges | Boolean | Email on pet/application status changes |
| emailNewApplications | Boolean | Email on new applications (rescue orgs) |
| emailFavoriteUpdates | Boolean | Email when favorited pet status changes |
| inAppEnabled | Boolean | Show in-app notifications |

**Why it exists:** Allows users to control notification volume. Embedded in User rather than separate table for simplicity.

---

## Enumerations

### UserRole
```
ADMIN | FOSTER | ADOPTER | VET | RESCUE_ORG
```

### AccountStatus
```
PENDING | ACTIVE | SUSPENDED
```

### Species
```
DOG | CAT | RABBIT | BIRD | OTHER
```

### Breed
Over 100 breeds supported including:
- **Dogs:** LABRADOR_RETRIEVER, GERMAN_SHEPHERD, GOLDEN_RETRIEVER, BULLDOG, POODLE, BEAGLE, ROTTWEILER, etc.
- **Cats:** PERSIAN, MAINE_COON, SIAMESE, RAGDOLL, BENGAL, BRITISH_SHORTHAIR, etc.
- **Mixed:** MIXED_BREED (for both species)

### PetSize
```
EXTRA_SMALL | SMALL | MEDIUM | LARGE | EXTRA_LARGE
```

### PetSex
```
MALE | FEMALE
```

### AgeUnit
```
WEEKS | MONTHS | YEARS
```

### PetStatus
```
DRAFT | PENDING_RESCUE | PENDING_VET | AVAILABLE | IN_PROGRESS | ADOPTED | WITHDRAWN | ON_HOLD
```
*See [[pet-status]] for detailed definitions and transitions.*

### HealthStatus
```
GOOD | NEEDS_ATTENTION | CRITICAL
```

### ApplicationStatus
```
SUBMITTED | UNDER_REVIEW | APPROVED | REJECTED | WITHDRAWN | FINALIZED
```

### NotificationType
```
PET_STATUS_CHANGE | NEW_APPLICATION | APPLICATION_UPDATE | FAVORITE_UPDATE | SYSTEM_ALERT
```

### VetApprovalRequestStatus
```
PENDING | APPROVED | REJECTED
```

### ContentType (for moderation)
```
PET | PROFILE | APPLICATION
```

### FlagStatus
```
PENDING | APPROVED | DISMISSED
```

### ModerationStatus (AI content moderation)
```
PENDING | APPROVED | FLAGGED | REJECTED
```
*Pets must be APPROVED to be publicly visible. See moderation-service documentation.*

---

## Entity Relationship Summary

```
User (1) ──────── (0..1) Foster
     (1) ──────── (0..1) Adopter
     (1) ──────── (0..1) Vet
     (1) ──────── (0..1) RescueOrg
     (1) ──────── (0..1) Admin
     (1) ──────── (0..*) Notification

Foster (1) ──────── (0..*) Pet [as registrant]
RescueOrg (1) ──────── (0..*) Pet [as owner, can create directly]

Pet (1) ──────── (0..*) PetImage
Pet (1) ──────── (0..1) VetSignOff
Pet (1) ──────── (0..*) AdoptionApplication
Pet (1) ──────── (0..*) Favorite
Pet (1) ──────── (0..*) PetStatusHistory
Pet (0..1) ──────── (1) Adoption

RescueOrg (1) ──────── (0..*) VetApproval
RescueOrg (1) ──────── (0..*) VetApprovalRequest

Vet (1) ──────── (0..*) VetSignOff
Vet (1) ──────── (0..*) VetApproval
Vet (1) ──────── (0..*) VetApprovalRequest

Adopter (1) ──────── (0..*) Favorite
Adopter (1) ──────── (0..*) AdoptionApplication
Adopter (1) ──────── (0..*) Adoption

VetSignOff (1) ──────── (0..*) VaccinationRecord

Admin (1) ──────── (0..*) AuditLog
ContentFlag (0..*) ──────── (1) Admin [resolver]
```
