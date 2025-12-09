# Pet Status Definitions

#mvp #core

> This document defines the lifecycle statuses a pet goes through from registration to adoption.

**Related Documentation:**
- [[Roadmap]] - Implementation phases and status
- [[domain-model]] - Entity definitions (Pet, PetStatus enum)
- [[user-stories/foster]] - Foster workflow
- [[user-stories/rescue-organization]] - Rescue workflow
- [[user-stories/vet]] - Vet verification process

## Status Overview

```
┌─────────────────┐
│     Draft       │
└────────┬────────┘
         │ Foster submits
         ▼
┌─────────────────┐
│ Pending Rescue  │◄─────────────────┐
└────────┬────────┘                  │
         │ Rescue accepts            │ Vet declines
         ▼                           │
┌─────────────────┐                  │
│  Pending Vet    │──────────────────┘
└────────┬────────┘
         │ Vet signs off
         ▼
┌─────────────────┐
│    Available    │◄─────────────────┐
└────────┬────────┘                  │
         │ Application approved      │ Adoption falls through
         ▼                           │
┌─────────────────┐                  │
│   In Progress   │──────────────────┘
└────────┬────────┘
         │ Adoption finalized
         ▼
┌─────────────────┐
│     Adopted     │
└─────────────────┘

Special statuses (can occur at any stage):
┌─────────────────┐    ┌─────────────────┐
│    Withdrawn    │    │    On Hold      │
└─────────────────┘    └─────────────────┘
```

## Status Definitions

### Draft
**Description:** Pet profile is being created but has not been submitted for adoption.

| Attribute | Value |
|-----------|-------|
| Visible to public | No |
| Editable by foster | Yes (all fields) |
| Triggered by | Foster starts registration |
| Next status | Pending Rescue |

---

### Pending Rescue
**Description:** Pet has been submitted and is awaiting acceptance by a rescue organization.

| Attribute | Value |
|-----------|-------|
| Visible to public | No |
| Editable by foster | Yes (limited fields) |
| Triggered by | Foster submits pet for adoption |
| Next status | Pending Vet (if accepted) |
| Notifications | Rescue organization receives new pet request |

**Actions available:**
- Rescue can **accept** → moves to Pending Vet
- Rescue can **decline** → foster is notified with reason, pet returns to Draft
- Foster can **withdraw** → moves to Withdrawn

---

### Pending Vet
**Description:** Pet has been accepted by a rescue and is awaiting veterinary verification.

| Attribute | Value |
|-----------|-------|
| Visible to public | No |
| Editable by foster | No |
| Triggered by | Rescue organization accepts pet |
| Next status | Available (if approved) or Pending Rescue (if declined) |
| Notifications | Foster notified to take pet to any verified vet with microchip number |

**How it works:**
1. Rescue accepts the pet → status becomes Pending Vet
2. Foster takes pet to any verified veterinarian
3. Vet looks up the pet by microchip number (see US-4.2)
4. Vet completes verification and signs off

**Vet verification requirements:**
- [ ] Neutered/spayed (with date)
- [ ] Vaccinations up to date (with records)
- [ ] Health status confirmed (good or known conditions documented)

**Actions available:**
- Vet can **sign off** (after microchip lookup) → moves to Available
- Vet can **decline** → moves back to Pending Rescue with notes
- Foster can **withdraw** → moves to Withdrawn

---

### Available
**Description:** Pet has passed all verifications and is publicly listed for adoption.

| Attribute | Value |
|-----------|-------|
| Visible to public | Yes |
| Editable by foster | No |
| Triggered by | Vet completes sign-off |
| Next status | In Progress (when application approved) |
| Notifications | Foster notified pet is now available |

**Actions available:**
- Adopters can **submit applications**
- Rescue can **approve application** → moves to In Progress
- Rescue can **place on hold** → moves to On Hold
- Foster can **withdraw** → moves to Withdrawn

---

### In Progress
**Description:** An adoption application has been approved and the adoption process is underway.

| Attribute | Value |
|-----------|-------|
| Visible to public | Yes (marked as "Adoption Pending") |
| Editable by foster | No |
| Triggered by | Rescue approves an adoption application |
| Next status | Adopted (success) or Available (falls through) |
| Notifications | Foster and adopter notified |

**Actions available:**
- Rescue can **finalize adoption** → moves to Adopted
- Rescue can **cancel adoption** → moves back to Available
- Foster can **withdraw** → moves to Withdrawn (requires rescue approval)

---

### Adopted
**Description:** Pet has been successfully adopted. This is a terminal status.

| Attribute | Value |
|-----------|-------|
| Visible to public | No (or shown in "Success Stories") |
| Editable by foster | No |
| Triggered by | Rescue finalizes adoption |
| Next status | None (terminal) |
| Notifications | All parties notified, confirmation emails sent |

**Record keeping:**
- Adoption date recorded
- Adopter information linked
- Foster, rescue, and vet information preserved for records

---

### Withdrawn
**Description:** Foster has removed the pet from the adoption process.

| Attribute | Value |
|-----------|-------|
| Visible to public | No |
| Editable by foster | No |
| Triggered by | Foster withdraws pet |
| Next status | Pending Rescue (if resubmitted) |
| Notifications | Rescue organization notified |

**Notes:**
- Pet profile is archived, not deleted
- Foster can resubmit pet later, starting from Pending Rescue
- If withdrawn during In Progress, rescue must approve

---

### On Hold
**Description:** Pet is temporarily unavailable for new applications.

| Attribute | Value |
|-----------|-------|
| Visible to public | Yes (marked as "On Hold") |
| Editable by foster | No |
| Triggered by | Rescue places pet on hold |
| Next status | Available (when hold lifted) |
| Notifications | None |

**Reasons for hold:**
- Medical treatment needed
- Behavioral assessment in progress
- Administrative review
- Foster temporarily unavailable

---

## Status Transition Matrix

| From | To | Triggered By | Condition |
|------|----|--------------|-----------|
| Draft | Pending Rescue | Foster | Submits complete profile |
| Pending Rescue | Pending Vet | Rescue | Accepts pet |
| Pending Rescue | Draft | Rescue | Declines pet |
| Pending Vet | Available | Vet | Signs off on all requirements |
| Pending Vet | Pending Rescue | Vet | Declines with notes |
| Available | In Progress | Rescue | Approves adoption application |
| Available | On Hold | Rescue | Places on hold |
| In Progress | Adopted | Rescue | Finalizes adoption |
| In Progress | Available | Rescue | Adoption falls through |
| On Hold | Available | Rescue | Lifts hold |
| Any (except Adopted) | Withdrawn | Foster | Withdraws pet |
| Withdrawn | Pending Rescue | Foster | Resubmits pet |
