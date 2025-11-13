# Forever Home - GitHub Issues Roadmap

This document outlines the GitHub issues structure for visualizing the project roadmap from current state to MVP completion and beyond.

## Issue Labels

- `mvp-phase-1` - Core Platform Foundation
- `mvp-phase-2` - Core User Experience
- `mvp-phase-3` - Enhanced Pet Management
- `post-mvp` - Future Enhancements
- `enhancement` - New feature or request
- `bug` - Something isn't working
- `documentation` - Improvements or additions to docs
- `testing` - Test coverage improvements

## Milestones

1. **MVP Phase 1: Core Platform Foundation** (Target: Sprint 1-2)
2. **MVP Phase 2: Core User Experience** (Target: Sprint 3-4)
3. **MVP Phase 3: Enhanced Pet Management** (Target: Sprint 5)
4. **Post-MVP: Enhanced Features** (Target: Future)

---

## ✅ COMPLETED FEATURES

### Data Model & Database Setup (MVP Phase 1)
**Status:** COMPLETE
- Created User entity with roles
- Created Rescue Center entity with ownership
- Created Pet entity with status workflow
- Created Swipe Action entity
- Set up database schema with relationships
- Configured Spring Data repositories (now using Spring JDBC)

### Authentication & Authorization (MVP Phase 1)
**Status:** COMPLETE
- User registration with email/password
- Automatic "Adopter" role assignment
- User login with JWT tokens
- Session management
- Logout functionality
- Password encryption (BCrypt)
- Role-based access control

### Anonymous Visitor Features (MVP Phase 1)
**Status:** COMPLETE
- View list of all rescue centers
- Filter rescue centers by county
- View rescue center public profile
- View list of pets at rescue center
- View individual pet details
- County filter with counts
- Pet photo placeholder
- Pagination support

---

## 🚧 IN PROGRESS / NEXT UP

### Issue #1: Rescue Center Management for Authenticated Users
**Milestone:** MVP Phase 1
**Labels:** `mvp-phase-1`, `enhancement`
**Status:** TODO

**Description:**
Implement authenticated rescue center management for users with the RESCUE role.

**Acceptance Criteria:**
- [ ] User can create a rescue center from profile page
- [ ] "Rescue" role is granted upon rescue center creation
- [ ] User is logged out after creating rescue center
- [ ] User must log back in to access rescue features
- [ ] Owner can view their rescue center dashboard
- [ ] Owner can edit rescue center information
- [ ] Public profile page displays correctly
- [ ] Only center owner can access management features

**Technical Notes:**
- POST /api/rescue-centers (authenticated)
- PUT /api/rescue-centers/{id} (authenticated, owner only)
- GET /api/rescue-centers/my-center (authenticated, RESCUE role)
- Enforce ownership in security layer

---

### Issue #2: Pet Management - Basic CRUD Operations
**Milestone:** MVP Phase 1
**Labels:** `mvp-phase-1`, `enhancement`
**Status:** TODO

**Description:**
Enable rescue center owners to manage their pets with full CRUD operations.

**Acceptance Criteria:**
- [ ] Owner can add new pet with required fields (name, species, size, description, status)
- [ ] Owner can add optional fields (breed, age in years/months, photo)
- [ ] Photo upload supports JPEG, PNG, WebP (max 10MB)
- [ ] Owner can edit any pet information
- [ ] Owner can change pet adoption status
- [ ] Owner can delete pet with confirmation
- [ ] Pet is automatically associated with owner's rescue center
- [ ] Ownership validation prevents managing other centers' pets
- [ ] Only dogs and cats are supported
- [ ] All four status options work (Available, Reserved, Pending Adoption, Adopted)

**Technical Notes:**
- POST /api/pets (authenticated, RESCUE role)
- GET /api/pets/{id} (authenticated, owner only for management)
- PUT /api/pets/{id} (authenticated, owner only)
- DELETE /api/pets/{id} (authenticated, owner only)
- File upload handling with validation
- Ownership verification in service layer

---

## 📋 MVP PHASE 2: CORE USER EXPERIENCE

### Issue #3: Swipe Interface for Pet Discovery
**Milestone:** MVP Phase 2
**Labels:** `mvp-phase-2`, `enhancement`
**Status:** TODO

**Description:**
Build the core swipe interface for adopters to discover pets one at a time.

**Acceptance Criteria:**
- [ ] Display one pet at a time in card format
- [ ] Show pet photo, name, species, breed, age, size, description, rescue center
- [ ] "Like" button creates swipe action
- [ ] "Pass" button creates swipe action
- [ ] Automatically show next available pet after swipe
- [ ] Only show pets with "Available" status
- [ ] Track swipe actions to prevent duplicates
- [ ] Show "no more pets" message when all pets reviewed
- [ ] Handle case where no pets are available
- [ ] Require authentication (ADOPTER role)

**Technical Notes:**
- GET /api/swipe/next-pet (authenticated)
- POST /api/swipe/like/{petId} (authenticated)
- POST /api/swipe/pass/{petId} (authenticated)
- Query must exclude already-swiped pets
- Efficient pagination for large pet datasets

---

### Issue #4: Liked Pets Management
**Milestone:** MVP Phase 2
**Labels:** `mvp-phase-2`, `enhancement`
**Status:** TODO

**Description:**
Allow adopters to view and manage their liked pets collection.

**Acceptance Criteria:**
- [ ] View gallery of all liked pets
- [ ] Display pet thumbnails in grid layout
- [ ] View full details for each liked pet
- [ ] Unlike pet functionality removes from liked list
- [ ] Unliked pets become swipeable again (removes swipe action)
- [ ] Navigate to pet detail page from liked gallery
- [ ] Navigate to rescue center profile from liked pet
- [ ] Show empty state when no liked pets
- [ ] Pagination for large collections

**Technical Notes:**
- GET /api/swipe/liked-pets (authenticated)
- DELETE /api/swipe/unlike/{petId} (authenticated)
- Deletes the swipe action record
- Pet becomes available in swipe interface again

---

### Issue #5: User Profile Management
**Milestone:** MVP Phase 2
**Labels:** `mvp-phase-2`, `enhancement`
**Status:** TODO

**Description:**
Enable users to view and update their profile information.

**Acceptance Criteria:**
- [ ] View profile page with current information
- [ ] Update full name
- [ ] Update county of residence
- [ ] Add/update phone number
- [ ] Add/update address
- [ ] Display email as read-only (cannot change)
- [ ] Show "Create Rescue Center" link for users without Rescue role
- [ ] Hide "Create Rescue Center" link for users with Rescue role
- [ ] Show link to rescue center dashboard if user has Rescue role
- [ ] Form validation for all fields

**Technical Notes:**
- GET /api/users/profile (authenticated)
- PUT /api/users/profile (authenticated)
- Email field should be displayed but not editable
- County dropdown with all 32 Irish counties

---

## 📊 MVP PHASE 3: ENHANCED PET MANAGEMENT

### Issue #6: Pet Dashboard with Kanban View
**Milestone:** MVP Phase 3
**Labels:** `mvp-phase-3`, `enhancement`
**Status:** TODO

**Description:**
Provide rescue owners with a visual Kanban board to manage pet adoption statuses.

**Acceptance Criteria:**
- [ ] Four columns: Available, Reserved, Pending Adoption, Adopted
- [ ] Display pet count in each column header
- [ ] Show pet cards with thumbnail, name, species, breed, age
- [ ] Drag and drop pets between status columns
- [ ] Update pet status in database when moved
- [ ] Click pet card to view full details
- [ ] Responsive layout for mobile/tablet
- [ ] Loading states during status updates
- [ ] Error handling for failed updates
- [ ] Only show pets from owner's rescue center

**Technical Notes:**
- GET /api/pets/dashboard (authenticated, RESCUE role)
- PATCH /api/pets/{id}/status (authenticated, owner only)
- Consider using a drag-and-drop library (HTML5 Drag & Drop API, or React DnD if frontend is React)
- Optimistic UI updates with rollback on error

---

### Issue #7: County Data Setup
**Milestone:** MVP Phase 3
**Labels:** `mvp-phase-3`, `enhancement`, `data`
**Status:** TODO

**Description:**
Populate and manage the complete list of 32 Irish counties.

**Acceptance Criteria:**
- [ ] All 32 counties stored in database or enum
- [ ] Counties organized by province (Connacht, Leinster, Munster, Ulster)
- [ ] County dropdown component in forms
- [ ] Filter showing only counties with rescue centers
- [ ] County display with rescue center counts (e.g., "Dublin (5)")
- [ ] "All Counties" option in filters

**Technical Notes:**
- Create County enum or reference table
- Endpoint: GET /api/counties
- Endpoint: GET /api/counties/with-centers (only counties that have rescue centers)
- Add province field to county data if needed for grouping

---

## 🚀 POST-MVP FEATURES

### Issue #8: Password Recovery Flow
**Milestone:** Post-MVP
**Labels:** `post-mvp`, `enhancement`
**Priority:** High

**Description:**
Implement forgot password and password reset functionality.

**Acceptance Criteria:**
- [ ] "Forgot Password" link on login page
- [ ] Email-based password reset request
- [ ] Generate secure reset token with expiration
- [ ] Send password reset email with link
- [ ] Reset token validation
- [ ] Password reset form
- [ ] Confirmation message after successful reset
- [ ] Token expires after use or timeout (e.g., 1 hour)

---

### Issue #9: Advanced Pet Search and Filtering
**Milestone:** Post-MVP
**Labels:** `post-mvp`, `enhancement`
**Priority:** Medium

**Description:**
Add comprehensive search and filtering capabilities for pet browsing.

**Acceptance Criteria:**
- [ ] Search pets by name
- [ ] Filter by species (dog/cat)
- [ ] Filter by size (Small, Medium, Large, Extra Large)
- [ ] Filter by county/location
- [ ] Filter by age range
- [ ] Combined filters work together
- [ ] Search results show count
- [ ] Clear all filters option
- [ ] Search and filters work on swipe interface
- [ ] Preserve filter state across navigation

---

### Issue #10: Multiple Pet Photos
**Milestone:** Post-MVP
**Labels:** `post-mvp`, `enhancement`
**Priority:** Medium

**Description:**
Allow rescue centers to upload multiple photos per pet.

**Acceptance Criteria:**
- [ ] Upload multiple photos per pet (e.g., max 5-10)
- [ ] Set primary photo
- [ ] Photo gallery view on pet detail page
- [ ] Delete individual photos
- [ ] Reorder photos
- [ ] Image cropping/resizing interface
- [ ] Automatic image compression
- [ ] Thumbnail generation for performance

---

### Issue #11: Email Notifications System
**Milestone:** Post-MVP
**Labels:** `post-mvp`, `enhancement`
**Priority:** Medium

**Description:**
Implement email notifications for key events.

**Acceptance Criteria:**
- [ ] Welcome email on registration
- [ ] Email when rescue center is created
- [ ] Email to rescue center when pet is liked
- [ ] Email notification preferences in user profile
- [ ] Unsubscribe links in emails
- [ ] Email templates with branding
- [ ] SMTP configuration for production

---

### Issue #12: Analytics Dashboard for Rescue Centers
**Milestone:** Post-MVP
**Labels:** `post-mvp`, `enhancement`
**Priority:** Low

**Description:**
Provide rescue centers with insights and statistics.

**Acceptance Criteria:**
- [ ] View count for pet profiles
- [ ] Number of likes per pet
- [ ] Most popular breeds/sizes
- [ ] Adoption statistics
- [ ] Time to adoption metrics
- [ ] Charts and visualizations
- [ ] Date range filtering
- [ ] Export data as CSV/PDF

---

### Issue #13: Adoption Application Workflow
**Milestone:** Post-MVP
**Labels:** `post-mvp`, `enhancement`
**Priority:** High

**Description:**
Create a formal adoption application and inquiry system.

**Acceptance Criteria:**
- [ ] Application form on pet detail page
- [ ] Required fields: name, email, phone, address, reason for adopting
- [ ] Optional fields: household info, other pets, experience
- [ ] Rescue center receives application notification
- [ ] Application status tracking (Submitted, Under Review, Approved, Rejected)
- [ ] Adopter can view application history
- [ ] Rescue center can manage applications
- [ ] Communication thread between adopter and rescue

---

### Issue #14: Mobile Responsive Design
**Milestone:** Post-MVP
**Labels:** `post-mvp`, `enhancement`
**Priority:** High

**Description:**
Ensure excellent mobile experience across all features.

**Acceptance Criteria:**
- [ ] Responsive layout for all pages
- [ ] Touch-friendly swipe gestures
- [ ] Mobile-optimized navigation
- [ ] Fast loading on mobile networks
- [ ] Test on various devices and screen sizes
- [ ] Progressive Web App (PWA) support
- [ ] Add to home screen functionality

---

### Issue #15: Admin Role and Platform Management
**Milestone:** Post-MVP
**Labels:** `post-mvp`, `enhancement`
**Priority:** Medium

**Description:**
Create admin role for platform-wide management.

**Acceptance Criteria:**
- [ ] Admin role assignment
- [ ] Approve/reject rescue center registrations
- [ ] Moderate pet descriptions and photos
- [ ] View platform-wide statistics
- [ ] User management (disable accounts, reset passwords)
- [ ] Admin dashboard
- [ ] Audit logs for admin actions

---

### Issue #16: Security Enhancements
**Milestone:** Post-MVP
**Labels:** `post-mvp`, `security`
**Priority:** High

**Description:**
Implement additional security measures for production.

**Acceptance Criteria:**
- [ ] Two-factor authentication option
- [ ] Rate limiting for API endpoints
- [ ] CSRF protection
- [ ] XSS prevention
- [ ] SQL injection prevention (parameterized queries)
- [ ] Security headers configuration
- [ ] Regular dependency updates
- [ ] Security audit and penetration testing

---

### Issue #17: Performance Optimization
**Milestone:** Post-MVP
**Labels:** `post-mvp`, `performance`
**Priority:** Medium

**Description:**
Optimize application performance for production scale.

**Acceptance Criteria:**
- [ ] Database indexing optimization
- [ ] Query optimization (N+1 problems)
- [ ] Image CDN integration
- [ ] Redis caching strategy
- [ ] Lazy loading for images
- [ ] API response time monitoring
- [ ] Load testing and benchmarking
- [ ] GraalVM native image for faster startup

---

### Issue #18: Comprehensive Testing Suite
**Milestone:** Post-MVP
**Labels:** `post-mvp`, `testing`
**Priority:** High

**Description:**
Expand test coverage across all features.

**Acceptance Criteria:**
- [ ] Unit tests for all services (target 90%+ coverage)
- [ ] Integration tests for all workflows
- [ ] End-to-end tests for critical user journeys
- [ ] Performance testing
- [ ] Security testing
- [ ] Accessibility testing (WCAG 2.1 AA compliance)
- [ ] Cross-browser testing
- [ ] Mobile device testing

---

## Priority Summary

### Immediate Next Steps (MVP Phase 1)
1. Issue #1: Rescue Center Management
2. Issue #2: Pet Management CRUD

### Sprint 2 (MVP Phase 2)
3. Issue #3: Swipe Interface
4. Issue #4: Liked Pets Management
5. Issue #5: User Profile Management

### Sprint 3 (MVP Phase 3)
6. Issue #6: Pet Dashboard Kanban
7. Issue #7: County Data Setup

### Post-MVP (Future Sprints)
- High Priority: #8 (Password Recovery), #13 (Adoption Applications), #14 (Mobile), #16 (Security), #18 (Testing)
- Medium Priority: #9 (Search), #10 (Multiple Photos), #11 (Notifications), #12 (Analytics), #15 (Admin), #17 (Performance)

---

## GitHub Issue Creation Commands

To create these issues programmatically (requires GitHub CLI):

```bash
# Ensure you're authenticated
gh auth status

# Create milestone for each phase
gh issue milestone create "MVP Phase 1: Core Foundation" --description "Core platform infrastructure and authenticated features"
gh issue milestone create "MVP Phase 2: User Experience" --description "Swipe interface and user engagement features"
gh issue milestone create "MVP Phase 3: Pet Management" --description "Enhanced pet management with Kanban board"
gh issue milestone create "Post-MVP: Enhancements" --description "Future features and optimizations"

# Create labels
gh label create "mvp-phase-1" --description "Core Platform Foundation" --color "0e8a16"
gh label create "mvp-phase-2" --description "Core User Experience" --color "1d76db"
gh label create "mvp-phase-3" --description "Enhanced Pet Management" --color "5319e7"
gh label create "post-mvp" --description "Future Enhancements" --color "d93f0b"

# Create issues (example for Issue #1)
gh issue create --title "Rescue Center Management for Authenticated Users" \
  --body "$(cat docs/github_issues/issue_01.md)" \
  --label "mvp-phase-1,enhancement" \
  --milestone "MVP Phase 1: Core Foundation"

# Repeat for all issues
```

---

## Tracking Progress

- Use GitHub Projects to create a Kanban board
- Columns: Backlog, Ready, In Progress, In Review, Done
- Link issues to project for automatic tracking
- Update issue status as work progresses
- Close issues when feature is complete and tested
