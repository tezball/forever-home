# Visitor User Stories

> **User Type:** Unauthenticated users exploring the Forever Home platform

## Overview

Visitors are unauthenticated users who can browse the platform to discover available pets, learn about rescue organizations and veterinarians, and understand the platform's purpose before deciding to register.

Visitors have **read-only access** to public information. To interact with pets (favorite, apply) or access dashboards, they must register and become an authenticated user.

## Related Documentation

- [Index](index.md) - Platform overview and all user types
- [Adopter Stories](adopter.md) - Authenticated browsing and adoption features
- [Domain Model](../domain-model.md) - Entity definitions
- [UI Style Guide](../ui-style-guide.md) - Component specifications

---

## User Journey

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Land on    │────►│   Browse    │────►│  View Pet   │────►│  Decide to  │
│  Home Page  │     │   Pets      │     │   Profile   │     │  Register   │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
                           │                   │
                           ▼                   ▼
                    ┌─────────────┐     ┌─────────────┐
                    │ View Rescue │     │  View Vet   │
                    │   Profile   │     │   Profile   │
                    └─────────────┘     └─────────────┘
```

**Typical Flow:**
1. Visitor arrives at home page and learns about the platform
2. Browses available pets in the public listing
3. Views individual pet profiles to learn more
4. May explore rescue organization or vet profiles
5. Decides to register as Foster (to rehome) or Adopter (to adopt)

---

## Public Pages

### US-7.1: View Home Page

**As a** visitor
**I want to** understand the platform's purpose
**So that** I can decide if it's right for me

**Acceptance Criteria:**
- Hero section explains platform mission
- Visual showing adoption process steps (Foster -> Rescue -> Vet -> Adopter)
- Statistics: "X pets adopted" counter
- Featured available pets (3-4 cards)
- Clear CTAs for each user type: "Adopt a Pet", "Rehome Your Pet", "I'm a Rescue"

**UI Components:**
- Hero: Display typography (Lora 32px+), Warm Sand background
- Process steps: Horizontal stepper with icons
- Pet cards: Grid of 3-4 featured pets
- CTAs: Primary and secondary buttons

**Priority:** P3 - Polish

---

## Pet Browsing (Public Access)

### US-5.1: Browse Available Pets (Public)

**As a** visitor
**I want to** browse pets available for adoption
**So that** I can find a pet that matches my preferences

**Access:** Public (no authentication required)

**Acceptance Criteria:**
- Only pets with `status = Available` are visible
- List shows: primary pet photo, name, breed, age, size, sex
- Grid layout: 2 columns mobile, 3-4 columns desktop
- Pagination or infinite scroll for large lists (20 per page)
- Can toggle between grid and list view
- Favorite button shown but requires login to use (prompts registration)

**Domain Notes:**
- Query: `Pet WHERE status = 'Available' ORDER BY createdAt DESC`
- Join to `PetImage WHERE isPrimary = true` for thumbnail

**UI Components:**
- Pet card (grid view):
  - Image: 3:2 aspect ratio, rounded corners (16px)
  - Name: Lora font, 20px
  - Meta: "Breed - Size" in secondary text
  - Status badge: "Available" (green)
  - Favorite button: Heart icon, top-right (disabled state for visitors)
- Card hover: Shadow elevation + slight translateY
- View toggle: Grid/List icons in header
- Loading: Skeleton cards during fetch

**Priority:** P0 - MVP

**Note:** For authenticated filtering and favorites, see [Adopter Stories](adopter.md).

---

### US-5.3: View Pet Profile (Public)

**As a** visitor
**I want to** view detailed information about a pet
**So that** I can decide if they're right for me

**Access:** Public (no authentication required)

**Acceptance Criteria:**
- Shows all pet details: name, age, breed, species, description, size
- Displays all uploaded images in swipeable gallery
- Shows vet verification badge with vet name
- Shows rescue organization name with contact details
- Indicates microchip status (has microchip: yes/no)
- Shows "Apply to Adopt" CTA prominently (redirects to login if not authenticated)
- Favorite button in header (prompts login for visitors)

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
  - For visitors: Button redirects to login/register page

**Priority:** P0 - MVP

---

## Organization & Vet Profiles

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

**Priority:** P3 - Polish

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

**Priority:** P3 - Polish

---

## Conversion Points

Visitors are encouraged to register at these key moments:

| Action | Prompt | Target Role |
|--------|--------|-------------|
| Click favorite (heart) | "Sign in to save pets to your favorites" | Adopter |
| Click "Apply to Adopt" | "Create an account to apply for adoption" | Adopter |
| Click "Rehome Your Pet" | "Register to find a loving home for your pet" | Foster |
| Click "I'm a Rescue" | "Register your rescue organization" | Rescue Org |

After registration, visitors transition to their respective authenticated user journeys:
- [Foster Journey](foster.md)
- [Adopter Journey](adopter.md)
- [Rescue Organization Journey](rescue-organization.md)
- [Vet Journey](vet.md)
