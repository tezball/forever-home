# Feature Implementation List

This document breaks down all features from the domain specification into implementation phases, with clear prioritization for MVP (Minimum Viable Product).

**For GitHub Issues:** See [GITHUB_ISSUES_ROADMAP.md](GITHUB_ISSUES_ROADMAP.md) for detailed issue descriptions, labels, milestones, and acceptance criteria for roadmap visualization and project management.

## Status Legend
- ✅ = Completed section (all items done)
- 🚧 = In progress
- [x] = Completed item
- [ ] = Pending item

## MVP - Phase 1: Core Platform Foundation

### 1.1 Data Model & Database Setup ✅
- [x] Create User entity (email, password, full name, county, phone, address, roles)
- [x] Create Rescue Center entity (name, county, address, phone, email, website, description, owner)
- [x] Create Pet entity (name, species, breed, age, size, description, photo, status, rescue center)
- [x] Create Swipe Action entity (user, pet, action type, timestamp)
- [x] Set up database schema with proper relationships and constraints
- [x] Configure Spring Data repositories (using Spring JDBC instead of JPA)

### 1.2 Authentication & Authorization ✅
- [x] User registration (email, password, full name, county optional)
- [x] Assign "Adopter" role on registration
- [x] User login with email and password
- [x] Session management (stay logged in across sessions via JWT)
- [x] Logout functionality
- [x] Password encryption/hashing (BCrypt)
- [x] Role-based access control infrastructure

### 1.3 Anonymous Visitor Features ✅
- [x] View list of all rescue centers
- [x] Filter rescue centers by county (32 Irish counties)
- [x] View rescue center public profile page
- [x] View list of pets at a rescue center
- [x] View individual pet detail page
- [x] Display pet information (name, photo, species, breed, age, size, description, status)
- [x] County filter showing counts (e.g., "Dublin (5)")
- [x] Pet photo placeholder when no image provided

### 1.4 Rescue Center Management 🚧
- [ ] Create rescue center (name, county required; address, phone, email, website, description optional)
- [ ] Grant "Rescue" role when rescue center is created
- [ ] Force logout after rescue center creation
- [ ] Edit rescue center information
- [ ] View own rescue center dashboard
- [x] Rescue center public profile page (completed as part of anonymous browsing)

### 1.5 Pet Management - Basic CRUD 🚧
- [ ] Add new pet (name, species, size, description, status required; breed, age, photo optional)
- [ ] Photo upload (JPEG, PNG, WebP, max 10MB)
- [ ] Edit pet information
- [ ] Delete pet (with confirmation)
- [ ] View list of pets belonging to rescue center
- [ ] Automatic association of pet with owner's rescue center
- [ ] Ownership validation (can only manage own pets)
- [ ] Support for dogs and cats only
- [ ] Four status options: Available, Reserved, Pending Adoption, Adopted

## MVP - Phase 2: Core User Experience

### 2.1 Swipe Interface
- [ ] Display one pet at a time in card format
- [ ] Show pet photo, name, species, breed, age, size, description, rescue center
- [ ] "Like" button functionality
- [ ] "Pass" button functionality
- [ ] Automatically show next available pet after swipe
- [ ] Only show pets with "Available" status
- [ ] Track swipe actions (prevent duplicate swipes)
- [ ] "No more pets" message when all pets reviewed
- [ ] Handle case where no pets are available

### 2.2 Liked Pets Management
- [ ] View gallery of all liked pets
- [ ] View full details for each liked pet
- [ ] Unlike pet functionality (removes from liked list)
- [ ] Make unliked pets swipeable again
- [ ] Navigate to pet detail page from liked gallery
- [ ] Navigate to rescue center profile from liked pet

### 2.3 User Profile Management
- [ ] View profile page
- [ ] Update full name
- [ ] Update county of residence
- [ ] Add/update phone number
- [ ] Add/update address
- [ ] Display email (read-only, cannot change)
- [ ] Link to create rescue center (for users without Rescue role)

## MVP - Phase 3: Enhanced Pet Management

### 3.1 Pet Dashboard (Kanban View)
- [ ] Four-column Kanban board (Available, Reserved, Pending Adoption, Adopted)
- [ ] Display pet count in each column
- [ ] Show pet cards with thumbnail, name, species, breed, age
- [ ] Drag and drop pets between status columns
- [ ] Update pet status when moved between columns
- [ ] Click pet card to view full details

### 3.2 County Data
- [ ] Populate all 32 Irish counties
- [ ] Organize by province (Connacht, Leinster, Munster, Ulster)
- [ ] County dropdown/selection component
- [ ] Filter showing only counties with rescue centers

## Post-MVP: Enhanced Features

### 4.1 Password Recovery
- [ ] "Forgot Password" link on login page
- [ ] Email-based password reset flow
- [ ] Reset token generation and validation
- [ ] Password reset form

### 4.2 Search & Advanced Filtering
- [ ] Search pets by name
- [ ] Filter pets by species (dog/cat)
- [ ] Filter pets by size (Small, Medium, Large, Extra Large)
- [ ] Filter pets by county/location
- [ ] Filter pets by age range
- [ ] Combined filters

### 4.3 Enhanced Image Management
- [ ] Multiple photos per pet
- [ ] Photo gallery view
- [ ] Image cropping/resizing
- [ ] Image compression for performance
- [ ] Delete/replace individual photos

### 4.4 Notifications
- [ ] Email notifications when rescue center is created
- [ ] Email notifications when pet is liked
- [ ] In-app notification system
- [ ] Notification preferences

### 4.5 Analytics & Reporting
- [ ] View count for pet profiles
- [ ] Number of likes per pet
- [ ] Most popular breeds/sizes
- [ ] Adoption statistics for rescue centers
- [ ] Time to adoption metrics

### 4.6 Enhanced Rescue Center Features
- [ ] Rescue center verification/badge system
- [ ] Multiple administrators per rescue center
- [ ] Operating hours information
- [ ] Social media links
- [ ] Photo gallery for rescue center

### 4.7 Adopter Features
- [ ] Save search preferences
- [ ] Get notified of new pets matching preferences
- [ ] Application/inquiry form for adoption
- [ ] Communication thread with rescue centers
- [ ] Adoption application status tracking

### 4.8 Administrative Features
- [ ] Admin role for platform management
- [ ] Approve/reject rescue center registrations
- [ ] Moderate content (pet descriptions, photos)
- [ ] View platform-wide statistics
- [ ] User management

## Technical Enhancements (Post-MVP)

### 5.1 Performance Optimization
- [ ] Database indexing optimization
- [ ] Query optimization
- [ ] Image CDN integration
- [ ] Caching strategy (Redis)
- [ ] Lazy loading for images

### 5.2 Security Enhancements
- [ ] Two-factor authentication
- [ ] Rate limiting for API endpoints
- [ ] CSRF protection
- [ ] XSS prevention
- [ ] SQL injection prevention
- [ ] Security headers configuration

### 5.3 Mobile & Responsive
- [ ] Mobile-responsive design
- [ ] Touch-friendly swipe gestures
- [ ] Progressive Web App (PWA) support
- [ ] Native mobile app consideration

### 5.4 Testing & Quality
- [ ] Integration tests for all workflows
- [ ] End-to-end tests for critical paths
- [ ] Performance testing
- [ ] Security testing
- [ ] Accessibility testing (WCAG compliance)

---

## MVP Summary

**The MVP includes three phases focused on:**

1. **Phase 1**: Basic infrastructure - users can register, rescue centers can be created, pets can be added, and anonymous visitors can browse
2. **Phase 2**: Core value proposition - the swipe interface for pet discovery and liked pets management
3. **Phase 3**: Enhanced pet management with the Kanban dashboard for rescue centers

**MVP excludes:**
- Password recovery (can be added later)
- Advanced search/filtering (browse and swipe are sufficient initially)
- Multiple photos per pet (single photo is adequate)
- Notifications (not critical for core functionality)
- Analytics and reporting
- Communication between adopters and rescue centers

**MVP success criteria:**
- Rescue centers can register and add pets
- Adopters can register and swipe through available pets
- Adopters can like pets and view their liked list
- Anonymous users can browse rescue centers and pets
- Rescue centers can manage pet status through the Kanban dashboard
