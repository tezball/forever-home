# Gaps and Decisions

#decision #architecture

> This document tracks identified gaps in the documentation and records decisions made to resolve them.

**Related Documentation:**
- [[Roadmap]] - Implementation roadmap and priorities
- [[domain-model]] - Entity definitions affected by decisions
- [[pet-status]] - Status lifecycle decisions
- [[e2e-review]] - Testing and bug findings

---

## Critical Gaps (Block MVP)

### GAP-1: No Foster/Adopter Profile Creation Story

**Issue:** US-1.1 creates a User account with email/password, but there's no story for completing the role-specific profile (firstName, lastName, phone, location for Foster; livingSituation, petExperience for Adopter).

**Options:**
- A) Add profile fields to registration form (single step)
- B) Separate "Complete Profile" step after registration
- C) Prompt for profile when user first tries to perform role action (lazy creation)

**Decision:**
> B: Separate "Complete Profile" step after registration. Allow the user to pick their profile after login.

**Notes:**


---

### GAP-2: No Rescue Organization Discovery for Fosters

**Issue:** US-2.1 says "Foster must select a rescue organization to work with" but there's no story for how fosters discover or browse available rescue organizations.

**Options:**
- A) Add rescue org dropdown to pet registration form (shows all verified orgs)
- B) Add new story US-2.0: Browse Rescue Organizations (dedicated page)
- C) Foster enters rescue org by name/code (invite-based model)
- D) Location-based: Show rescues near foster's location

**Decision:**
> B

**Notes:**
> _[Any additional context]_

---

### GAP-3: Pet Entity Missing `sex` Field

**Issue:** US-5.3 UI shows "Age | Size | Sex" in pet stats, but the Pet entity in domain-model.md doesn't have a sex/gender field.

**Options:**
- A) Add `sex` enum: Male, Female
- B) Add `sex` enum: Male, Female, Neutered Male, Spayed Female
- C) Don't track sex (remove from UI)

**Decision:**
> A

**Notes:**
> _[Any additional context]_

---

### GAP-4: Microchip Requirement Unclear

**Issue:** Microchip is used as the vet lookup key (US-4.2), but it's not specified whether it's mandatory. What if a pet doesn't have a microchip?

**Options:**
- A) Microchip required (no exceptions) - simplest for MVP
- B) Microchip optional, but required for vet sign-off (must get chipped first)
- C) Alternative lookup for unchipped pets (generate system ID)

**Decision:**
> A

**Notes:**
> It's a must for the app. A microchip is used to move ownership of a dog within the platform

---

## Medium Gaps (Should Fix Before P1)

### GAP-5: pet-status.md Outdated (Vet Assignment References)

**Issue:** The PendingVet section still says "Rescue organization accepts pet and assigns vet" and "Assigned vet receives sign-off request" but we removed vet assignment in favor of microchip lookup.

**Options:**
- A) Update pet-status.md to reflect microchip-based flow
- B) Leave as-is (low priority)

**Decision:**
> A

**Notes:**
> _[Any additional context]_

---

### GAP-6: What Happens to Other Applications When One is Approved?

**Issue:** If multiple adopters apply for the same pet and one application is approved (pet → InProgress), what happens to the other pending applications?

**Options:**
- A) Auto-reject all other applications with system message
- B) Keep other applications pending (as backup if approved adoption falls through)
- C) Mark as "On Hold" until adoption finalizes, then auto-reject
- D) Notify other applicants that pet is "Adoption Pending" - they can withdraw or wait

**Decision:**
> Given the platform doesn't deal with the process of adoption, we don't need to worry about this.

**Notes:**
> _[Any additional context]_

---

### GAP-7: Favorites Entity Missing from Domain Model

**Issue:** US-5.6 references "Many-to-many: Adopter ↔ Pet (favorites join table)" but this entity is not defined in domain-model.md.

**Options:**
- A) Add `Favorite` entity with adopterId, petId, createdAt
- B) Add favorites as array field on Adopter entity
- C) Defer - not in MVP

**Decision:**
> A

**Notes:**
> _[Any additional context]_

---

### GAP-8: User Entity Missing Fields

**Issue:** The following fields are referenced in user stories but not in the User entity:
- `lastLoginAt` (US-1.2: "Updates User.lastLoginAt timestamp")
- Notification preferences (US-8.1: "Users can configure notification preferences")

**Options:**
- A) Add `lastLoginAt: Timestamp` and `notificationPrefs: NotificationPreferences` to User
- B) Add fields only when implementing those stories
- C) Create separate UserPreferences entity

**Decision:**
> A

**Notes:**
> _[Any additional context]_

---

### GAP-9: Public vs Authenticated Access Unclear

**Issue:** Can visitors browse pets without creating an account? The docs don't clearly define what's public vs what requires authentication.

**Options:**
- A) Browse/View = Public; Apply/Favorite = Auth required
- B) Everything requires authentication (closed platform)
- C) Browse = Public; View full profile = Auth required

**Decision:**
> A

**Notes:**
> _[Any additional context]_

---

## Minor Gaps (Can Defer)

### GAP-10: No Text Search for Pets

**Issue:** US-5.2 defines filtering (by breed, size, age, etc.) but there's no text search to find pets by name or description keywords.

**Options:**
- A) Add search input to US-5.2 criteria
- B) Create separate search story
- C) Defer to post-MVP

**Decision:**
> C

**Notes:**
> _[Any additional context]_

---

### GAP-11: Admin Bootstrap Undefined

**Issue:** No mechanism to create the first admin account. US-6.1 requires an admin to approve Rescue Organizations, but how does the first admin get created?

**Options:**
- A) Seed data in database migrations
- B) CLI command: `./mvnw exec:java -Dexec.args="create-admin email@example.com"`
- C) First registered user becomes admin (dangerous)
- D) Environment variable with admin email auto-creates on startup

**Decision:**
> D

**Notes:**
> _[Any additional context]_

---

### GAP-12: Image Storage Undefined

**Issue:** Pet images and logos are uploaded but storage location is not specified. Where do files go?

**Options:**
- A) AWS S3 (recommended for production)
- B) Local filesystem (dev only)
- C) Database BLOB (not recommended)
- D) Cloudinary or similar CDN service

**Decision:**
> A

**Notes:**
> _[Any additional context]_

---

### GAP-13: Email Service Undefined

**Issue:** US-8.1 mentions email notifications but no email service provider is specified.

**Options:**
- A) AWS SES
- B) SendGrid
- C) Mailgun
- D) SMTP (self-hosted)
- E) Defer - log emails to console for MVP

**Decision:**
> A

**Notes:**
> _[Any additional context]_

---

### GAP-14: Adopter Application Limit Enforcement

**Issue:** US-5.4 says "Limit of 3 active applications total (configurable)" but doesn't define when an application becomes inactive.

**Options:**
- A) Active = Submitted or UnderReview; Inactive = Approved, Rejected, Withdrawn
- B) Active = all non-terminal states; Inactive = only after adoption completes
- C) No limit for MVP

**Decision:**
> The platform won't deal with the process of adoption, so we don't need to worry about this.

**Notes:**
> _[Any additional context]_

---

### GAP-15: Pet Description Length Constraint

**Issue:** US-5.3 UI says "Description: Body text, max 500 chars" but the Pet entity in domain-model.md has no length constraint specified.

**Options:**
- A) Add constraint to domain model: `description: Text (max 500 chars)`
- B) Frontend validation only
- C) Increase to 1000 chars (500 feels short)

**Decision:**
> A

**Notes:**
> _[Any additional context]_

---

### GAP-16: Vet Verification - Admin vs Rescue Organization

**Issue:** Original docs stated vets are verified by platform admins, but this doesn't fit the trust model. Rescue organizations need to explicitly approve the vets who will perform health checks on their pets.

**Options:**
- A) Admin verifies vets (global platform trust)
- B) Rescue organization verifies vets (organization-specific trust)
- C) Both - admin does initial verification, rescue does per-org approval

**Decision:**
> B: Rescue organization verifies vets. This ensures each rescue explicitly trusts the vets who perform health checks on their pets. A vet can be approved by multiple rescue organizations.

**Rationale:**
1. **Domain alignment:** Rescue organizations are responsible for their pets' welfare
2. **Distributed trust:** Different rescues may have different vet preferences
3. **Scalability:** Reduces admin bottleneck for vet approvals
4. **Relationship building:** Encourages vet-rescue partnerships

**Affected Documentation:**
- `user-stories/vet.md` - Updated verification flow
- `user-stories/rescue-organization.md` - Added US-3.7: Approve Vets
- `user-stories/admin.md` - Removed vet approval from admin scope
- `domain-model.md` - Added VetApproval entity

---

## Decision Log

| Gap | Decision | Date | Decided By |
|-----|----------|------|------------|
| GAP-1 | B: Separate profile completion step | 2025-12-07 | Product |
| GAP-2 | B: Add US-2.0 Browse Rescue Organizations | 2025-12-07 | Product |
| GAP-3 | A: Add sex enum (Male, Female) | 2025-12-07 | Product |
| GAP-4 | A: Microchip required (no exceptions) | 2025-12-07 | Product |
| GAP-5 | A: Update pet-status.md for microchip flow | 2025-12-07 | Product |
| GAP-6 | N/A: Platform doesn't handle adoption process | 2025-12-07 | Product |
| GAP-7 | A: Add Favorite entity | 2025-12-07 | Product |
| GAP-8 | A: Add lastLoginAt and notificationPrefs | 2025-12-07 | Product |
| GAP-9 | A: Browse/View public, Apply/Favorite auth | 2025-12-07 | Product |
| GAP-10 | C: Defer text search to post-MVP | 2025-12-07 | Product |
| GAP-11 | D: Admin via ADMIN_EMAIL env var | 2025-12-07 | Product |
| GAP-12 | A: AWS S3 for image storage | 2025-12-07 | Product |
| GAP-13 | A: AWS SES for email | 2025-12-07 | Product |
| GAP-14 | N/A: Platform doesn't handle adoption process | 2025-12-07 | Product |
| GAP-15 | A: Add description constraint (500 chars) | 2025-12-07 | Product |
| GAP-16 | B: Vets verified by Rescue Organizations | 2025-12-08 | Product |

---

## Post-Decision Actions

Once decisions are made, update the following docs:
- [x] `domain-model.md` - Add missing fields/entities (GAP-3, GAP-7, GAP-8, GAP-15)
- [x] `user-stories.md` - Add/update stories (GAP-1, GAP-2, GAP-6, GAP-9, GAP-10)
- [x] `pet-status.md` - Update PendingVet section (GAP-5)
- [x] `CLAUDE.md` - Add infra decisions (GAP-11, GAP-12, GAP-13)
