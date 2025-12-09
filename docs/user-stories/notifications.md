# Notification User Stories

#enhanced

> **Cross-cutting Concern:** Email and in-app notifications for all authenticated user types

**Related:** [[index|User Stories Index]] | [[../domain-model|Domain Model]] | [[../Roadmap|Roadmap]]

## Overview

The notification system keeps all users informed about important events in their adoption journey. Notifications are delivered through two channels: email (for important updates when users are away) and in-app (for real-time updates while using the platform).

Each user type receives notifications relevant to their role and can configure their preferences.

## Related Documentation

- [Index](index.md) - Platform overview and all user types
- [Foster Stories](foster.md) - Foster-specific notifications
- [Adopter Stories](adopter.md) - Adopter-specific notifications
- [Rescue Organization Stories](rescue-organization.md) - Rescue-specific notifications
- [Vet Stories](vet.md) - Vet-specific notifications
- [Domain Model](../domain-model.md) - Entity definitions
- [UI Style Guide](../ui-style-guide.md) - Component specifications

---

## Email Notifications

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
- Uses AWS SES for email delivery

**Email Template Structure:**
```
┌─────────────────────────────────────────────────────────┐
│  [Forever Home Logo]                                    │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Hi [First Name],                                       │
│                                                         │
│  [Notification message with context]                    │
│                                                         │
│  [Primary CTA Button: "View Details"]                   │
│                                                         │
│  ─────────────────────────────────────────────────────  │
│  You received this email because [reason].              │
│  [Manage Preferences] | [Unsubscribe]                   │
│                                                         │
│  Forever Home - Finding loving homes for pets           │
└─────────────────────────────────────────────────────────┘
```

**UI Components:**
- Settings page: Toggle switches per notification type
- Email template: Clean, mobile-friendly, branded header

**Priority:** P2 - Enhanced

---

## In-App Notifications

### US-8.2: In-App Notifications

**As a** user
**I want to** see notifications within the platform
**So that** I can catch up on activity when logged in

**Acceptance Criteria:**
- Notification bell icon in header shows unread count (max "9+")
- Dropdown shows recent 10 notifications
- Each shows: icon, title, time ago, read/unread state
- Mark as read individually or "Mark all as read"
- Click navigates to relevant page
- Full notifications page shows all with pagination

**Domain Notes:**
- Creates `Notification` entity per event
- Fields: `userId`, `type`, `title`, `message`, `link`, `read`, `createdAt`
- Mark read: Updates `Notification.read = true`

**Notification Entity:**
```
Notification {
  id: UUID
  userId: UUID
  type: NotificationType
  title: String
  message: String
  link: String (deep link URL)
  read: Boolean (default: false)
  createdAt: Timestamp
}
```

**UI Components:**
- Bell icon: 24px, in top navigation
- Unread badge: Red circle with count (max "9+")
- Dropdown: 320px wide, max 400px height, scrollable
- Notification item: Icon + text + time + dot (if unread)
- Time format: "2m ago", "1h ago", "Yesterday", "Dec 1"
- Footer: "Mark all as read" | "View all notifications"

**Notification Dropdown Layout:**
```
┌─────────────────────────────────────┐
│  Notifications                [···] │
├─────────────────────────────────────┤
│  ● [🐕] Your pet Max was approved   │
│    by Happy Tails Rescue            │
│    2 hours ago                      │
├─────────────────────────────────────┤
│  ○ [📋] New application for Bella   │
│    from John D.                     │
│    Yesterday                        │
├─────────────────────────────────────┤
│  ○ [✓] Vet sign-off complete        │
│    Luna is now available            │
│    2 days ago                       │
├─────────────────────────────────────┤
│  [Mark all as read] [View all →]    │
└─────────────────────────────────────┘
```

**Priority:** P2 - Enhanced

---

## Notification Types by User Role

### Foster Notifications

| Event | Title | Message | Channel |
|-------|-------|---------|---------|
| Pet accepted by rescue | "Pet Accepted" | "[Pet name] was accepted by [Rescue name]" | Email + In-app |
| Pet declined by rescue | "Pet Declined" | "[Rescue name] declined [Pet name]: [reason]" | Email + In-app |
| Vet sign-off complete | "Vet Verification Complete" | "[Pet name] is now available for adoption" | Email + In-app |
| Vet declined sign-off | "Vet Sign-off Declined" | "[Pet name] needs: [requirements]" | Email + In-app |
| Application received | "New Application" | "Someone applied to adopt [Pet name]" | Email + In-app |
| Adoption approved | "Adoption in Progress" | "[Adopter name] was approved for [Pet name]" | Email + In-app |
| Adoption complete | "Adoption Complete!" | "[Pet name] has found their forever home!" | Email + In-app |

### Adopter Notifications

| Event | Title | Message | Channel |
|-------|-------|---------|---------|
| Application submitted | "Application Received" | "Your application for [Pet name] was submitted" | Email + In-app |
| Application under review | "Application Under Review" | "[Rescue name] is reviewing your application" | In-app only |
| Application approved | "Application Approved!" | "Congratulations! You've been approved for [Pet name]" | Email + In-app |
| Application rejected | "Application Update" | "Your application for [Pet name] was not approved" | Email + In-app |
| Favorited pet adopted | "Pet Adopted" | "[Pet name] has been adopted" | Email + In-app |
| Favorited pet status change | "Pet Update" | "[Pet name] is now [status]" | In-app only |

### Rescue Organization Notifications

| Event | Title | Message | Channel |
|-------|-------|---------|---------|
| New pet registration | "New Pet Request" | "[Foster name] submitted [Pet name] for adoption" | Email + In-app |
| Vet sign-off complete | "Pet Verified" | "[Pet name] passed vet verification" | Email + In-app |
| Vet declined sign-off | "Verification Issue" | "[Pet name] needs additional vet work" | Email + In-app |
| New application | "New Application" | "[Adopter name] applied for [Pet name]" | Email + In-app |
| Foster withdrew pet | "Pet Withdrawn" | "[Foster name] withdrew [Pet name]" | Email + In-app |
| Account verified | "Account Approved" | "Your organization has been verified" | Email + In-app |

### Vet Notifications

| Event | Title | Message | Channel |
|-------|-------|---------|---------|
| Account verified | "Account Approved" | "Your veterinary license has been verified" | Email + In-app |
| Account rejected | "Verification Issue" | "Your account verification was declined" | Email + In-app |

### Admin Notifications

| Event | Title | Message | Channel |
|-------|-------|---------|---------|
| New approval request | "New Approval Request" | "[Type]: [Name] is awaiting verification" | In-app only |
| Content flagged | "Content Flagged" | "[Type] flagged: [reason]" | In-app only |

---

## Notification Preferences

Users can configure which notifications they receive via email:

### Preference Categories

| Category | Default | Description |
|----------|---------|-------------|
| Status Updates | On | Pet and application status changes |
| New Activity | On | New applications, registrations |
| Reminders | On | Pending actions, incomplete profiles |
| Marketing | Off | Platform news, tips (requires opt-in) |

### Preference UI

```
┌─────────────────────────────────────────────────────────┐
│  Notification Preferences                               │
├─────────────────────────────────────────────────────────┤
│  Email Notifications                                    │
│  ─────────────────────────────────────────────────────  │
│                                                         │
│  Status Updates                              [Toggle On]│
│  Pet status changes, application updates                │
│                                                         │
│  New Activity                                [Toggle On]│
│  New applications, pet registrations                    │
│                                                         │
│  Reminders                                   [Toggle On]│
│  Pending actions, profile completion                    │
│                                                         │
│  Platform Updates                           [Toggle Off]│
│  News, tips, and feature announcements                  │
│                                                         │
├─────────────────────────────────────────────────────────┤
│  In-App Notifications                                   │
│  ─────────────────────────────────────────────────────  │
│  All in-app notifications are enabled                   │
│                                                         │
│                               [Save Preferences]        │
└─────────────────────────────────────────────────────────┘
```

---

## Email Service

**Provider:** AWS SES (Simple Email Service)

**Configuration:**
- From address: `notifications@foreverhome.com`
- Reply-to: `support@foreverhome.com`
- Bounce handling: Automatic suppression list
- Complaint handling: Automatic unsubscribe

**Email Types:**
| Type | Description | Template |
|------|-------------|----------|
| Transactional | Status updates, approvals | `status-update.html` |
| Verification | Email verification, password reset | `verification.html` |
| Welcome | New user onboarding | `welcome.html` |
| Marketing | Platform updates (opt-in only) | `newsletter.html` |

**Rate Limits:**
- Maximum 50 emails per user per day
- Batch notifications grouped (max 1 email per event type per hour)

---

## Deep Links

All notifications include deep links to relevant pages:

| Notification Type | Deep Link |
|-------------------|-----------|
| Pet status change | `/pets/{petId}` |
| Application update | `/applications/{applicationId}` |
| New application (rescue) | `/rescue/applications/{applicationId}` |
| Vet sign-off | `/pets/{petId}/verification` |
| Account verification | `/dashboard` |
| Favorited pet update | `/pets/{petId}` |

**Deep Link Format:**
```
https://foreverhome.com{path}?utm_source=notification&utm_medium=email&utm_campaign={type}
```

---

## Notification Timing

**Real-time Events:**
- Application submitted/updated
- Pet status changes
- Vet sign-off complete
- Account verification

**Batched Events (hourly digest):**
- Multiple applications for same pet
- Multiple status changes

**Delayed Events:**
- Reminder emails (24h after incomplete action)
- Follow-up prompts (7d after inactivity)
