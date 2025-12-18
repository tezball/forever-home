# Forever Home - User Stories

#mvp #core #enhanced #polish

> This documentation describes all user stories for the Forever Home pet adoption platform, organized by user type.

## Related Documentation

- [[Home]] - Vault home page
- [[Roadmap]] - Implementation roadmap and priorities
- [[domain-model]] - Entity definitions and relationships
- [[pet-status]] - Status lifecycle and transitions
- [[ui-style-guide]] - Component specifications

---

## User Types

Forever Home serves six distinct user types, each with their own journey through the platform:

| User Type | Description | Documentation |
|-----------|-------------|---------------|
| [[visitor\|Visitor]] | Unauthenticated users exploring the platform | Public browsing and discovery |
| [[foster\|Foster]] | Pet owners looking to rehome their pets | Pet registration and rehoming journey |
| [[adopter\|Adopter]] | People seeking to adopt a pet | Browsing, applying, and adoption journey |
| [[rescue-organization\|Rescue Organization]] | Verified entities facilitating adoptions | Pet intake and adoption management |
| [[vet\|Vet]] | Licensed veterinarians verifying pet health | Health verification and sign-off |
| [[admin\|Admin]] | Platform administrators | User approvals, moderation, analytics |

Cross-cutting concerns:
- [[notifications]] - Email and in-app notification system

---

## Platform Overview

Forever Home connects pet owners (Fosters) with potential adopters through rescue organizations, with veterinary verification ensuring all pets are healthy and ready for adoption.

### The Adoption Flow

```
Foster                    Rescue Org                 Vet                      Adopter
  │                          │                        │                          │
  │ 1. Register pet          │                        │                          │
  │─────────────────────────►│                        │                          │
  │                          │                        │                          │
  │                          │ 2. Accept/Review       │                          │
  │◄─────────────────────────│                        │                          │
  │                          │                        │                          │
  │ 3. Take pet to vet ──────┼───────────────────────►│                          │
  │                          │                        │                          │
  │                          │                        │ 4. Verify & sign off     │
  │                          │◄───────────────────────│                          │
  │                          │                        │                          │
  │                          │         Pet becomes Available                     │
  │                          │                        │                          │
  │                          │                        │    5. Browse & apply     │
  │                          │◄───────────────────────┼──────────────────────────│
  │                          │                        │                          │
  │                          │ 6. Review & approve    │                          │
  │                          │─────────────────────────────────────────────────►│
  │                          │                        │                          │
  │ 7. Handoff               │                        │        8. Adopt!         │
  │◄─────────────────────────┼────────────────────────┼─────────────────────────►│
```

---

## Pet Status Lifecycle

Pets progress through the following statuses during the adoption journey:

```
                    ┌─────────────────┐
                    │     Draft       │ ◄── Foster creates
                    └────────┬────────┘
                             │ Foster submits
                             ▼
                    ┌─────────────────┐
                    │ Pending Rescue  │ ◄── Rescue reviews
                    └────────┬────────┘
            Rescue  │        │ Rescue accepts
           declines │        ▼
        (back to ───┘┌─────────────────┐
          Draft)     │  Pending Vet    │ ◄── Vet reviews
                     └────────┬────────┘
                              │ Vet signs off
                              ▼
                     ┌─────────────────┐
                     │    Available    │ ◄── Public listing
                     └────────┬────────┘
                              │ Application approved
                              ▼
                     ┌─────────────────┐
                     │   In Progress   │ ◄── Adoption underway
                     └────────┬────────┘
                              │ Adoption finalized
                              ▼
                     ┌─────────────────┐
                     │     Adopted     │ ◄── Complete!
                     └─────────────────┘

Additional statuses:
- Withdrawn: Foster removed pet from adoption (can resubmit)
- On Hold: Temporarily unavailable
```

### Status Visibility

| Status | Visible to Public | Foster Can Edit |
|--------|-------------------|-----------------|
| Draft | No | Yes |
| PendingRescue | No | Limited |
| PendingVet | No | No |
| Available | Yes | No |
| InProgress | Yes (marked) | No |
| Adopted | No | No |
| Withdrawn | No | No |

---

## Priority Matrix

Stories are prioritized into four tiers:

| Priority | Focus | Key Stories |
|----------|-------|-------------|
| **P0 - MVP** | Core registration, profile, verification, and browsing flow | US-1.1, US-1.2, US-1.4, US-2.0, US-2.1, US-3.1, US-3.3, US-4.2, US-4.3, US-5.1, US-5.3, US-6.1 |
| **P1 - Core** | Complete adoption workflow | US-2.2, US-2.3, US-3.5, US-3.6, US-5.2, US-5.4 |
| **P2 - Enhanced** | User experience improvements | US-1.3, US-2.4, US-4.4, US-4.5, US-5.5, US-5.6, US-6.2, US-8.1, US-8.2 |
| **P3 - Polish** | Public pages and analytics | US-3.2, US-4.1, US-6.3, US-6.4, US-7.1, US-7.2, US-7.3 |

---

## Glossary

| Term | Definition | Related Entity |
|------|------------|----------------|
| Foster | A pet owner looking to rehome their pet | `Foster` |
| Adopter | A person seeking to adopt a pet | `Adopter` |
| Rescue Organization | A verified entity that facilitates adoptions | `RescueOrganization` |
| Vet | Licensed veterinarian who verifies pet health | `Vet` |
| Vet Sign-off | Verification that pet is neutered, vaccinated, and healthy | `VetSignOff` |
| Pet Status | Current stage in adoption journey | `Pet.status` |
| Application | Formal request from adopter to adopt a specific pet | `AdoptionApplication` |
| Adoption | Completed transfer of pet to new owner | `Adoption` |
| Microchip | Required pet identifier used for vet lookup and ownership tracking | `Pet.microchipId` |

---

## Authentication

All authenticated users share a common authentication system:

- **JWT-based**: Stateless authentication with short-lived tokens
- **Access token**: 15 minute expiry, stored in memory
- **Refresh token**: 7 day expiry (30 days with "Remember me"), stored in httpOnly cookie
- **Email verification**: Magic link with 24-hour expiry

### Account States

| State | Description |
|-------|-------------|
| Pending | Email not yet verified |
| Active | Email verified, account usable |
| Suspended | Disabled by admin |

### Role-Specific Verification

Some roles require admin approval before becoming fully active:

- **Vet**: Must have `verified: true` (admin verifies license)
- **Rescue Organization**: Must have `verified: true` (admin verifies legitimacy)

---

## Story Index

### Epic 1: Registration & Authentication
- US-1.1: User Registration (all roles)
- US-1.2: User Login (all roles)
- US-1.3: Password Recovery (all roles)
- US-1.4: Complete Profile (all roles)

### Epic 2: Foster - Pet Registration
- US-2.0: Browse Rescue Organizations
- US-2.1: Register Pet for Adoption
- US-2.2: Edit Pet Profile
- US-2.3: View Pet Status
- US-2.4: Withdraw Pet from Adoption

### Epic 3: Rescue Organization Management
- US-3.1: Create Organization Profile
- US-3.2: Manage Organization Profile
- US-3.3: Accept Pet Registrations
- US-3.5: View Organization's Pets
- US-3.6: Facilitate Adoption

### Epic 4: Veterinary Verification
- US-4.1: Create Vet Profile
- US-4.2: Look Up Pet by Microchip
- US-4.3: Sign Off on Pet
- US-4.4: Decline Sign-off
- US-4.5: View Sign-off History

### Epic 5: Adopter Experience
- US-5.1: Browse Available Pets
- US-5.2: Filter Pets
- US-5.3: View Pet Profile
- US-5.4: Apply to Adopt
- US-5.5: Track Application Status
- US-5.6: Favorite Pets

### Epic 6: Admin Management
- US-6.1: Approve User Registrations
- US-6.2: Manage All Users
- US-6.3: Platform Analytics
- US-6.4: Content Moderation

### Epic 7: Public Pages
- US-7.1: View Home Page
- US-7.2: View Rescue Organization Public Profile
- US-7.3: View Vet Public Profile

### Epic 8: Notifications
- US-8.1: Email Notifications
- US-8.2: In-App Notifications
