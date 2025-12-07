# Domain Model

This document defines the core types in the Forever Home domain and the rationale for their existence.

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
| passwordHash | String | Encrypted password |
| role | UserRole | Discriminator for user type |
| createdAt | Timestamp | Account creation date |
| status | AccountStatus | Active, Suspended, Pending |

**Why it exists:** Provides authentication and authorization foundation. All specific user types extend from this base, enabling a unified login system while supporting role-specific functionality.

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
| verified | Boolean | Admin has verified credentials |

**Why it exists:** Vets serve as trusted third parties who verify that pets meet adoption requirements (neutered, vaccinated, healthy). Their professional credentials add legitimacy to the verification process. Requiring vet sign-off protects adopters from receiving pets with undisclosed health issues and ensures basic animal welfare standards.

**Relationships:**
- Creates many Vet Sign-offs
- Assigned by Rescue Organizations

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
- Partners with many Vets
- Processes many Adoption Applications

---

### Admin
**Purpose:** Platform administrator with elevated privileges.

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Unique identifier |
| userId | UUID | Link to User |

**Why it exists:** Admins ensure platform integrity by verifying rescue organizations and vets, moderating content, and handling disputes. Kept minimal as most admin context comes from the User role.

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
| size | PetSize | Small, Medium, Large |
| description | Text | Personality, history, needs |
| microchipId | String | Microchip number (immutable) |
| status | PetStatus | Current lifecycle stage |
| fosterId | UUID | Who registered this pet |
| rescueOrgId | UUID | Managing organization |
| createdAt | Timestamp | Registration date |
| updatedAt | Timestamp | Last modification |

**Why it exists:** Pets are the reason the platform exists. The Pet entity captures everything an adopter needs to make an informed decision and everything the system needs to track the adoption journey.

**Key design decisions:**
- **Microchip is immutable:** Prevents fraud and ensures traceability
- **Status is denormalized:** Avoids complex joins for the most common query (listing available pets)
- **Age as integer + unit:** Handles puppies/kittens (months) and adults (years) cleanly

**Relationships:**
- Belongs to one Foster (registrant)
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

## Enumerations

### UserRole
```
Admin | Foster | Adopter | Vet | RescueOrg
```

### AccountStatus
```
Pending | Active | Suspended
```

### Species
```
Dog | Cat | Rabbit | Bird | Other
```

### PetSize
```
Small | Medium | Large
```

### AgeUnit
```
Months | Years
```

### PetStatus
```
Draft | PendingRescue | PendingVet | Available | InProgress | Adopted | Withdrawn | OnHold
```
*See [pet-status.md](./pet-status.md) for detailed definitions.*

### HealthStatus
```
Good | KnownConditions
```

### ApplicationStatus
```
Submitted | UnderReview | Approved | Rejected | Withdrawn
```

### NotificationType
```
StatusChange | NewApplication | ApplicationUpdate | SystemAlert
```

---

## Entity Relationship Summary

```
User (1) ──────── (0..1) Foster
     (1) ──────── (0..1) Adopter
     (1) ──────── (0..1) Vet
     (1) ──────── (0..1) RescueOrg
     (1) ──────── (0..1) Admin

Foster (1) ──────── (0..*) Pet
Pet (1) ──────── (0..*) PetImage
Pet (1) ──────── (0..1) VetSignOff
Pet (1) ──────── (0..*) AdoptionApplication
Pet (0..1) ──────── (1) Adoption

RescueOrg (1) ──────── (0..*) Pet
Vet (1) ──────── (0..*) VetSignOff

Adopter (1) ──────── (0..*) AdoptionApplication
Adopter (1) ──────── (0..*) Adoption

VetSignOff (1) ──────── (0..*) VaccinationRecord
```
