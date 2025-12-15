# Notifications Map

This document maps all notifications within the Forever Home application, organized by user roles and triggering events.

## Overview

The notification system provides both **in-app notifications** and **email notifications**. Users can configure their preferences to control which notifications they receive via email.

### Notification Types

| Type | Description |
|------|-------------|
| `PET_STATUS_CHANGE` | Pet lifecycle updates (submitted, accepted, declined, vet sign-off) |
| `NEW_APPLICATION` | New adoption applications or vet approval requests |
| `APPLICATION_UPDATE` | Application status changes (approved, rejected, finalized) |
| `FAVORITE_UPDATE` | Favorited pet becomes available |
| `SYSTEM_ALERT` | System-level alerts (vet approvals, account verification) |

---

## Notifications by User Role

### Foster

| Event | Notification Type | Title | In-App | Email |
|-------|-------------------|-------|--------|-------|
| Rescue org accepts pet | `PET_STATUS_CHANGE` | Pet Accepted | Yes | Yes* |
| Rescue org declines pet | `PET_STATUS_CHANGE` | Pet Declined | Yes | Yes* |
| Vet signs off on pet (approved) | `PET_STATUS_CHANGE` | Pet Available for Adoption | Yes | Yes* |
| Vet declines pet | `PET_STATUS_CHANGE` | Pet Declined by Vet | Yes | Yes* |

*If `emailStatusChanges` preference is enabled

---

### Adopter

| Event | Notification Type | Title | In-App | Email |
|-------|-------------------|-------|--------|-------|
| Application approved | `APPLICATION_UPDATE` | Application Approved | Yes | Yes* |
| Application rejected | `APPLICATION_UPDATE` | Application Rejected | Yes | Yes* |
| Adoption finalized | `APPLICATION_UPDATE` | Adoption Finalized | Yes | Yes* |
| Favorited pet available | `FAVORITE_UPDATE` | Pet Now Available | Yes | Yes** |

*If `emailStatusChanges` preference is enabled
**If `emailFavoriteUpdates` preference is enabled

---

### Rescue Organization

| Event | Notification Type | Title | In-App | Email |
|-------|-------------------|-------|--------|-------|
| Foster submits pet | `PET_STATUS_CHANGE` | New Pet Submitted | Yes | Yes* |
| Vet requests approval | `NEW_APPLICATION` | Vet Approval Request | Yes | Yes** |
| Adopter submits application | `NEW_APPLICATION` | New Adoption Application | Yes | Yes** |

*If `emailStatusChanges` preference is enabled
**If `emailNewApplications` preference is enabled

---

### Vet

| Event | Notification Type | Title | In-App | Email |
|-------|-------------------|-------|--------|-------|
| Pet ready for sign-off | `PET_STATUS_CHANGE` | Pet Pending Vet Sign-off | Yes | Yes* |
| Rescue org approves vet | `SYSTEM_ALERT` | Vet Approval Granted | Yes | Yes* |
| Rescue org revokes vet approval | `SYSTEM_ALERT` | Vet Approval Revoked | Yes | Yes* |
| Vet approval request approved | `APPLICATION_UPDATE` | Request Approved | Yes | Yes* |
| Vet approval request rejected | `APPLICATION_UPDATE` | Request Rejected | Yes | Yes* |

*If `emailStatusChanges` preference is enabled

---

## Notifications by Event Flow

### Pet Submission Flow

```
Foster submits pet for rescue review
    └── Rescue Org receives: "New Pet Submitted" (PET_STATUS_CHANGE)

Rescue Org accepts pet
    └── Foster receives: "Pet Accepted" (PET_STATUS_CHANGE)

Rescue Org declines pet
    └── Foster receives: "Pet Declined" (PET_STATUS_CHANGE)
```

### Vet Sign-off Flow

```
Pet ready for vet review (status: PendingVet)
    └── Vet receives: "Pet Pending Vet Sign-off" (PET_STATUS_CHANGE)

Vet approves pet
    └── Foster receives: "Pet Available for Adoption" (PET_STATUS_CHANGE)

Vet declines pet
    └── Foster receives: "Pet Declined by Vet" (PET_STATUS_CHANGE)
```

### Vet Approval Request Flow

```
Vet requests approval from rescue
    └── Rescue Org receives: "Vet Approval Request" (NEW_APPLICATION)

Rescue Org approves request
    └── Vet receives: "Request Approved" (APPLICATION_UPDATE)

Rescue Org rejects request
    └── Vet receives: "Request Rejected" (APPLICATION_UPDATE)
```

### Adoption Flow

```
Adopter submits application
    └── Rescue Org receives: "New Adoption Application" (NEW_APPLICATION)

Rescue Org approves application
    └── Adopter receives: "Application Approved" (APPLICATION_UPDATE)

Rescue Org rejects application
    └── Adopter receives: "Application Rejected" (APPLICATION_UPDATE)

Adoption finalized
    └── Adopter receives: "Adoption Finalized" (APPLICATION_UPDATE)
```

### Favorite Updates

```
Favorited pet becomes available
    └── Adopter receives: "Pet Now Available" (FAVORITE_UPDATE)
```

---

## Email Notifications

### Transactional Emails (Always Sent)

These emails are sent regardless of user preferences:

| Email Type | Trigger | Recipient |
|------------|---------|-----------|
| Verification Email | User registration | New user |
| Welcome Email | Registration complete | New user |
| Password Reset | User requests reset | User |
| Admin Password Reset | Admin resets password | User |

### Preference-Based Emails

Email delivery is controlled by user notification preferences:

| Preference | Controls |
|------------|----------|
| `emailStatusChanges` | Pet status updates, application status updates, vet approval notifications |
| `emailNewApplications` | New adoption applications, vet approval requests |
| `emailFavoriteUpdates` | Favorited pet availability notifications |

---

## Notification Preferences

Users can manage their notification preferences via the profile settings.

### Default Settings

All preferences are enabled by default:
- `emailStatusChanges`: true
- `emailNewApplications`: true
- `emailFavoriteUpdates`: true
- `inAppEnabled`: true

### API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/profile/notifications` | GET | Get current preferences |
| `/api/profile/notifications` | PUT | Update preferences |

---

## In-App Notification API

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/notifications` | GET | Get all notifications (ordered by date desc) |
| `/api/notifications/unread` | GET | Get unread notifications only |
| `/api/notifications/unread/count` | GET | Get count of unread notifications |
| `/api/notifications/{id}/read` | PUT | Mark notification as read |
| `/api/notifications/read-all` | PUT | Mark all notifications as read |

---

## Technical Implementation

### Services

| Service | Responsibility |
|---------|----------------|
| `NotificationService` | Orchestrates in-app and email notifications |
| `EmailService` | Interface for sending emails |
| `SesEmailService` | AWS SES implementation (production) |
| `SmtpEmailService` | SMTP/Mailpit implementation (development) |
| `ConsoleEmailService` | Console logging (development) |

### Notification Triggers by Service

| Service | Method | Notifications Triggered |
|---------|--------|------------------------|
| `PetService` | `submitForRescue()` | notifyRescueOrgPetSubmitted |
| `PetService` | `acceptPet()` | notifyFosterPetAccepted |
| `PetService` | `declinePet()` | notifyFosterPetDeclined |
| `PetService` | `vetSignOff()` (approve) | notifyFosterPetAvailable |
| `PetService` | `vetSignOff()` (decline) | notifyFosterPetDeclined |
| `AdoptionService` | `submitApplication()` | notifyNewApplication |
| `AdoptionService` | `approveApplication()` | notifyApplicationStatusChange |
| `AdoptionService` | `rejectApplication()` | notifyApplicationStatusChange |
| `AdoptionService` | `finalizeAdoption()` | notifyApplicationStatusChange |
| `VetApprovalService` | `approveVet()` | notifyVetApproved |
| `VetApprovalRequestService` | `requestApproval()` | notifyRescueOrgVetRequest |
| `VetApprovalRequestService` | `approveRequest()` | notifyVetRequestApproved |
| `VetApprovalRequestService` | `rejectRequest()` | notifyVetRequestRejected |

### Database Schema

```sql
CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id),
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    link VARCHAR(500),
    read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

---

## Summary Matrix

| Role | Pet Status | New Apps | App Updates | Favorites | System Alerts |
|------|------------|----------|-------------|-----------|---------------|
| Foster | Receives | - | - | - | - |
| Adopter | - | - | Receives | Receives | - |
| Rescue Org | Receives | Receives | - | - | - |
| Vet | Receives | - | Receives | - | Receives |
| Admin | - | - | - | - | - |

---

## PM Review: Gaps & Issues

### Missing Notifications (Should Be Implemented)

#### High Priority

| Missing Event | Who Should Be Notified | Spec Reference | Impact |
|--------------|------------------------|----------------|--------|
| **Application submitted confirmation** | Adopter | notifications.md line 164 | Adopters don't know their application was received |
| **Application received** | Foster | notifications.md line 156 | Foster doesn't know someone wants their pet |
| **Adoption approved (InProgress)** | Foster | notifications.md line 157 | Foster doesn't know to prepare for handoff |
| **Adoption complete** | Foster | notifications.md line 158 | Foster doesn't get closure on their pet's journey |
| **Foster withdraws pet** | Rescue Org | pet-status.md line 177 | Rescue org doesn't know pet was withdrawn |
| **Rescue org verified** | Rescue Org | notifications.md line 180 | Org doesn't know they can start operating |

#### Medium Priority

| Missing Event | Who Should Be Notified | Spec Reference | Impact |
|--------------|------------------------|----------------|--------|
| **Vet sign-off complete** | Rescue Org | notifications.md line 176-177 | Rescue doesn't know pet is available |
| **Vet declined sign-off** | Rescue Org | notifications.md line 177 | Rescue doesn't know pet needs attention |
| **Application under review** | Adopter | notifications.md line 165 | Adopter doesn't know their application is being reviewed |
| **Favorited pet adopted** | Adopter | notifications.md line 168 | Adopter isn't informed to stop waiting |
| **Adoption cancelled (falls through)** | Foster, Adopter | pet-status.md line 147 | Neither party knows adoption failed |

#### Low Priority

| Missing Event | Who Should Be Notified | Spec Reference | Impact |
|--------------|------------------------|----------------|--------|
| **Admin approval requests** | Admin | notifications.md line 193 | Admin must manually check queue |
| **Vet account rejected** | Vet | notifications.md line 187 | Vet doesn't know why they can't verify pets |
| **Reminder: incomplete profile** | All users | notifications.md line 300-301 | Users abandon incomplete registrations |
| **Follow-up: inactivity** | All users | notifications.md line 301 | Re-engagement opportunity missed |

---

### Notifications That Should Be Removed or Reconsidered

#### Remove: `notifyVetSignOffNeeded`

**Problem:** The current codebase appears to have a notification for vets when a pet becomes `PendingVet`. This contradicts the system design.

**Why it's wrong:**
- Per the domain model and user stories, vets are NOT assigned to pets
- Fosters choose any verified vet and bring the pet to them
- The vet looks up the pet by microchip - they don't receive pets

**Expected workflow:**
1. Rescue accepts pet → `PendingVet`
2. Foster receives notification with instructions to visit any verified vet
3. Foster brings pet to vet of their choice
4. Vet scans microchip and looks up pet

**Recommendation:** Remove `notifyVetSignOffNeeded()` or rename to send to **Foster** instead (telling them to take the pet to a vet).

---

### Notification Logic Inconsistencies

#### 1. Foster Not Notified Throughout Adoption Journey

The spec clearly states fosters should receive:
- "New Application" when someone applies
- "Adoption in Progress" when adopter is approved
- "Adoption Complete!" when adoption finalizes

Currently, fosters are only notified about pet status changes (accepted/declined/vet sign-off), not adoption progress. This leaves fosters in the dark about their pet's adoption journey.

#### 2. Rescue Org Not Notified of Vet Results

The spec says rescue orgs should receive:
- "Pet Verified" when vet approves
- "Verification Issue" when vet declines

Currently only fosters receive these notifications. Rescue orgs need visibility into vet outcomes to manage their pipeline.

#### 3. Adopter Confirmation Missing

When an adopter submits an application, they receive no confirmation. The spec explicitly lists "Application Received" as an email+in-app notification. This is a poor user experience - users expect confirmation of important actions.

---

### Database Schema Issue

The notifications table constraint only allows 4 types:
```sql
TYPE CHECK: STATUS_CHANGE, NEW_APPLICATION, APPLICATION_UPDATE, SYSTEM_ALERT
```

But the code has 5 types including `FAVORITE_UPDATE`. Either:
- Add `FAVORITE_UPDATE` to the database constraint, or
- Map it to `STATUS_CHANGE` (since it's technically a status change notification)

---

### Deep Links Not Populated

The notification system supports a `link` field for deep links to relevant pages, but the current implementation passes `null` for all links. Per the spec, notifications should include:

| Notification Type | Expected Deep Link |
|-------------------|-------------------|
| Pet status change | `/pets/{petId}` |
| Application update | `/applications/{applicationId}` |
| New application (rescue) | `/rescue/applications/{applicationId}` |
| Favorited pet update | `/pets/{petId}` |

---

### Summary: Recommended Actions

1. **Critical**: Add adopter application confirmation notification
2. **Critical**: Add foster notifications for adoption journey (application received, approved, complete)
3. **High**: Add rescue org notifications for vet results
4. **High**: Add foster withdrawn notification to rescue org
5. **Medium**: Remove or repurpose `notifyVetSignOffNeeded`
6. **Medium**: Populate deep links in notifications
7. **Low**: Fix database schema for `FAVORITE_UPDATE`
8. **Low**: Add admin queue notifications
9. **Future**: Implement reminder/follow-up emails
