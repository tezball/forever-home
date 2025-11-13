# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Forever Home is a pet adoption platform connecting animal rescue centers across Ireland with potential adopters through a swipe-based interface. The application is built with Spring Boot 3.5.7 and Java 25, configured for GraalVM native compilation.

## Build and Development Commands

### Running the Application
```bash
./mvnw spring-boot:run
```

### Running Tests
```bash
# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=ClassName

# Run a specific test method
./mvnw test -Dtest=ClassName#methodName
```

### Building
```bash
# Standard JAR build
./mvnw clean package

# Build native image with GraalVM
./mvnw native:compile -Pnative

# Build Docker image with native support
./mvnw spring-boot:build-image -Pnative
```

### Running in Native Mode
```bash
# After building with native:compile
./target/forever-home
```

### Testing Native Compatibility
```bash
./mvnw test -PnativeTest
```

## Available MCP Tools

Claude Code has access to various MCP (Model Context Protocol) tools to assist with development. These tools provide enhanced capabilities beyond standard file operations.

### JetBrains IDE Integration

Tools that leverage IntelliJ IDEA's powerful indexing and analysis capabilities:

**Project Management**
- `get_run_configurations` - List all run configurations in the project
- `execute_run_configuration` - Run a specific configuration (tests, builds, etc.)
- `get_project_dependencies` - Get all project dependencies
- `get_project_modules` - List project modules with their types

**File Operations**
- `get_file_text_by_path` - Read file contents (faster than standard Read for large files)
- `create_new_file` - Create new files in the project
- `replace_text_in_file` - Find and replace with regex support
- `reformat_file` - Apply IntelliJ's code formatting rules
- `open_file_in_editor` - Open a file in the IDE editor

**File Discovery**
- `find_files_by_name_keyword` - Fast file search by name using indexes (preferred for name searches)
- `find_files_by_glob` - Search using glob patterns (e.g., `**/*.java`)
- `list_directory_tree` - Get tree representation of directory structure
- `get_all_open_file_paths` - List currently open editor files

**Code Search & Analysis**
- `search_in_files_by_text` - Search for text across all files (faster than grep)
- `search_in_files_by_regex` - Search with regex patterns
- `get_symbol_info` - Get documentation, type info, and declaration for a symbol at a specific position
- `get_file_problems` - Analyze file for errors and warnings using IntelliJ inspections

**Refactoring**
- `rename_refactoring` - Context-aware symbol renaming across the entire project (updates all references)

**Terminal & VCS**
- `execute_terminal_command` - Execute shell commands in IDE's integrated terminal
- `get_repositories` - Get list of VCS roots in the project

**Best Practices:**
- Use JetBrains tools instead of command-line equivalents when available (faster, more accurate)
- Prefer `find_files_by_name_keyword` for simple file name searches (uses indexes)
- Use `search_in_files_by_text` instead of grep for codebase searches
- Use `rename_refactoring` instead of find-replace for renaming symbols

### GitHub Integration

Tools for interacting with GitHub repositories:

**Repository Management**
- `create_repository` - Create new GitHub repository
- `fork_repository` - Fork a repository to your account
- `search_repositories` - Search for repositories on GitHub
- `get_file_contents` - Get contents of files from GitHub repos
- `create_branch` - Create new branch in repository
- `list_commits` - Get commit history for a branch

**File Operations**
- `create_or_update_file` - Create or update a single file
- `push_files` - Push multiple files in a single commit

**Issues**
- `create_issue` - Create new issue
- `list_issues` - List and filter issues
- `get_issue` - Get details of specific issue
- `update_issue` - Update existing issue
- `add_issue_comment` - Comment on an issue

**Pull Requests**
- `create_pull_request` - Create new PR
- `list_pull_requests` - List and filter PRs
- `get_pull_request` - Get PR details
- `get_pull_request_files` - List files changed in PR
- `get_pull_request_status` - Get status checks for PR
- `get_pull_request_comments` - Get review comments
- `get_pull_request_reviews` - Get reviews on PR
- `create_pull_request_review` - Create review with comments
- `merge_pull_request` - Merge a PR
- `update_pull_request_branch` - Update PR branch with base branch changes

**Search**
- `search_code` - Search for code across GitHub
- `search_issues` - Search issues and PRs
- `search_users` - Search for GitHub users

### Library Documentation (Context7)

Tools for fetching up-to-date documentation for libraries and frameworks:

- `resolve-library-id` - Convert library name to Context7-compatible ID (call this first)
- `get-library-docs` - Fetch current documentation for a library

**Usage Pattern:**
1. Call `resolve-library-id` with library name (e.g., "Spring Boot")
2. Use returned ID with `get-library-docs` to fetch documentation
3. Optionally specify `topic` parameter to focus on specific areas

**Note:** Skip `resolve-library-id` if user provides ID in format `/org/project` or `/org/project/version`

### Browser Automation (Playwright)

Tools for automated browser testing and interaction:

**Navigation & Control**
- `browser_navigate` - Navigate to URL
- `browser_navigate_back` - Go back to previous page
- `browser_close` - Close the browser page
- `browser_resize` - Resize browser window

**Interaction**
- `browser_click` - Click elements (single, double, with modifiers)
- `browser_type` - Type text into elements
- `browser_press_key` - Press keyboard keys
- `browser_hover` - Hover over elements
- `browser_drag` - Drag and drop between elements
- `browser_select_option` - Select dropdown options
- `browser_fill_form` - Fill multiple form fields at once
- `browser_file_upload` - Upload files

**Inspection**
- `browser_snapshot` - Capture accessibility snapshot (preferred over screenshot for actions)
- `browser_take_screenshot` - Take screenshot (visual only, not for actions)
- `browser_console_messages` - Get console output
- `browser_network_requests` - Get network request log

**Tab Management**
- `browser_tabs` - List, create, close, or select tabs

**Utilities**
- `browser_handle_dialog` - Handle alert/confirm dialogs
- `browser_evaluate` - Execute JavaScript in page
- `browser_wait_for` - Wait for text to appear/disappear or time to pass
- `browser_install` - Install browser if not already installed

### Design Integration (Lunacy)

Tools for working with Lunacy design files:

- `get_selected` - Get selected objects in JSON format
- `set_selection` - Set selected objects in JSON format
- `get_components` - Get multiple components by IDs
- `get_color_variables` - Get color variables array
- `get_image` - Get image by ID
- `get_documentation` - Get complete Lunacy FREE format documentation

### MCP Resource Access

Tools for reading resources from configured MCP servers:

- `ListMcpResourcesTool` - List available resources from MCP servers (optionally filter by server)
- `ReadMcpResourceTool` - Read a specific resource by server name and URI

### IDE Diagnostics

- `getDiagnostics` - Get diagnostic information for a file (errors, warnings, etc.)

## Development Practices

### Test-Driven Development (TDD)

All features MUST be developed using Test-Driven Development:

1. **Write the test first**: Before implementing any feature, write a failing test that describes the expected behavior
2. **Run the test**: Verify the test fails for the right reason
3. **Implement the feature**: Write the minimum code necessary to make the test pass
4. **Run all tests**: Ensure the new test passes and all existing tests still pass
5. **Refactor**: Improve the code while keeping all tests green

This applies to:
- Service layer business logic
- Repository data access
- Controller endpoints
- Domain model behavior
- Utility functions

### Test Requirements

Before considering any feature complete:

1. **All tests must pass**: Run `./mvnw test` and verify that ALL tests pass with no failures or errors
2. **No skipped tests**: Do not mark tests as `@Disabled` or skip them unless there is a documented reason
3. **Verify test execution**: Check the test output to ensure tests actually ran and weren't skipped due to configuration issues

A feature is NOT complete until the entire test suite passes successfully.

## Architecture

### Domain Model

The application implements a pet adoption platform with three core entities:

**User**: Adopters and rescue center owners. Users can have both "Adopter" and "Rescue" roles simultaneously. The Adopter role is assigned on registration; the Rescue role is gained when creating a rescue center.

**Rescue Center**: Organizations managed by users with the Rescue role. Each rescue center is owned by exactly one user. Centers are located in one of Ireland's 32 counties and contain a collection of pets.

**Pet**: Animals (dogs and cats only) belonging to rescue centers. Pets have four adoption statuses:
- Available: Visible in swipe interface
- Reserved: On hold, not swipeable
- Pending Adoption: Paperwork in progress, not swipeable
- Adopted: Forever home found, not swipeable but retained in history

**Swipe Action**: Records user interactions with pets via the swipe interface. Each user can swipe on each pet exactly once (Like or Pass). Unliking a pet allows it to be swiped again.

### Multi-Role System

Users can have multiple roles:
- **Anonymous**: Browse rescue centers and view pet profiles
- **Adopter**: Swipe interface, like/unlike pets, manage profile
- **Rescue**: All Adopter features plus manage own rescue center and pets

Important: When a user creates a rescue center and gains the Rescue role, they must log out and log back in for the role to take effect.

### Swipe Logic

- Only pets with "Available" status appear in swipe interface
- Each pet appears exactly once unless unliked
- Unliking a pet makes it swipeable again
- Pass actions are permanent and cannot be undone

### Ownership and Security

- Rescue owners can ONLY manage pets belonging to their own rescue center
- Pet creation automatically links to the owner's rescue center
- Cannot view, edit, or delete pets from other rescue centers
- Public profiles are accessible to all users (anonymous and authenticated)

### Geographic Scope

Platform covers all 32 Irish counties organized by province:
- **Connacht**: Galway, Leitrim, Mayo, Roscommon, Sligo
- **Leinster**: Carlow, Dublin, Kildare, Kilkenny, Laois, Longford, Louth, Meath, Offaly, Westmeath, Wexford, Wicklow
- **Munster**: Clare, Cork, Kerry, Limerick, Tipperary, Waterford
- **Ulster**: Cavan, Donegal, Monaghan

County filtering shows only counties with active rescue centers, with counts displayed in parentheses.

## Technical Details

### Spring Boot Configuration

- Uses Spring Boot DevTools for development hot-reload
- Spring Boot Actuator enabled for monitoring
- Docker Compose support configured (compose.yaml currently empty)
- GraalVM native compilation configured for lightweight deployments

### Image Handling

Pet photos are optional with the following constraints:
- Formats: JPEG, PNG, WebP
- Maximum size: 10MB
- Placeholder shown when no image provided

### Package Structure

Base package: `com.example.foreverhome`

Standard Spring Boot structure:
- `src/main/java`: Application source code
- `src/main/resources`: Configuration and static resources
  - `static/`: Static web assets
  - `templates/`: Template files
- `src/test/java`: Test code

## Domain Specification

Complete domain specification with all business rules, user journeys, and workflows is documented in `docs/DOMAIN_SPECIFICATION.md`. Consult this file when implementing features to ensure alignment with business requirements.

## Feature Implementation List

The `docs/FEATURE_IMPLEMENTATION_LIST.md` file contains a comprehensive breakdown of all features organized by implementation priority:

- **MVP Phase 1**: Core platform foundation (data model, authentication, anonymous browsing, rescue center and pet management)
- **MVP Phase 2**: Core user experience (swipe interface, liked pets management, user profiles)
- **MVP Phase 3**: Enhanced pet management (Kanban dashboard)
- **Post-MVP**: Advanced features including password recovery, search/filtering, notifications, analytics, and mobile optimization

When implementing features, follow the priority order defined in this list. Each feature includes checkboxes to track implementation progress.

## GitHub Issues and Project Management

### GitHub Issues Roadmap

The `docs/GITHUB_ISSUES_ROADMAP.md` file contains a detailed breakdown of all features as GitHub issues for roadmap visualization and project management. This document provides:

- **Issue Descriptions**: Detailed descriptions for each feature with acceptance criteria
- **Labels**: Organized by phase (mvp-phase-1, mvp-phase-2, mvp-phase-3, post-mvp)
- **Milestones**: Clear milestones for each development phase
- **Priority Matrix**: Features prioritized by business value and dependencies
- **Progress Tracking**: Visual representation of what's done, in progress, and planned

### Issue Labels

Use the following labels to categorize issues:

- `mvp-phase-1` - Core Platform Foundation
- `mvp-phase-2` - Core User Experience
- `mvp-phase-3` - Enhanced Pet Management
- `post-mvp` - Future Enhancements
- `enhancement` - New feature or request
- `bug` - Something isn't working
- `documentation` - Improvements or additions to docs
- `testing` - Test coverage improvements
- `security` - Security-related issues
- `performance` - Performance optimization

### Milestones

Project is organized into four main milestones:

1. **MVP Phase 1: Core Platform Foundation**
   - Target: Sprint 1-2
   - Focus: Authentication, data model, anonymous browsing, rescue center and pet CRUD

2. **MVP Phase 2: Core User Experience**
   - Target: Sprint 3-4
   - Focus: Swipe interface, liked pets management, user profiles

3. **MVP Phase 3: Enhanced Pet Management**
   - Target: Sprint 5
   - Focus: Kanban dashboard, county data

4. **Post-MVP: Enhanced Features**
   - Target: Future sprints
   - Focus: Advanced features, optimizations, scaling

### Current Status (as of initial setup)

**Completed (MVP Phase 1):**
- ✅ Data Model & Database Setup
- ✅ Authentication & Authorization
- ✅ Anonymous Visitor Features
- ✅ Public browsing of rescue centers and pets

**In Progress (MVP Phase 1):**
- 🚧 Rescue Center Management (authenticated RESCUE role)
- 🚧 Pet Management - Basic CRUD operations

**Next Up (MVP Phase 2):**
- ⏳ Swipe Interface for Pet Discovery
- ⏳ Liked Pets Management
- ⏳ User Profile Management

### Creating GitHub Issues

When creating issues for new features:

1. **Use clear, descriptive titles** that summarize the feature
2. **Include acceptance criteria** as checkboxes for tracking completion
3. **Add appropriate labels** for categorization and filtering
4. **Assign to milestone** to track sprint/phase progress
5. **Reference related issues** using #issue-number
6. **Link to documentation** (DOMAIN_SPECIFICATION.md, FEATURE_IMPLEMENTATION_LIST.md)

### Example Issue Template

```markdown
## Description
[Brief description of the feature and its purpose]

## Acceptance Criteria
- [ ] Criterion 1
- [ ] Criterion 2
- [ ] Criterion 3

## Technical Notes
- API endpoints to implement
- Database changes required
- Security considerations
- Dependencies on other issues

## Related Documentation
- docs/DOMAIN_SPECIFICATION.md - [Relevant section]
- docs/FEATURE_IMPLEMENTATION_LIST.md - [Feature number]

## Testing Requirements
- Unit tests for service layer
- Integration tests for API endpoints
- Test coverage target: 90%+
```

### Using GitHub Projects

For visual project management:

1. **Create a GitHub Project board** with columns:
   - Backlog
   - Ready
   - In Progress
   - In Review
   - Done

2. **Link issues to the project** for automatic tracking

3. **Move issues across columns** as work progresses

4. **Use filters** to view specific phases or labels

5. **Track velocity** by monitoring issues completed per sprint

### Issue Workflow

1. **Backlog**: Issue created but not yet prioritized
2. **Ready**: Issue refined, acceptance criteria clear, ready to start
3. **In Progress**: Actively being developed
4. **In Review**: PR submitted, awaiting code review
5. **Done**: PR merged, feature deployed and tested

### MCP Tools for GitHub Issues

Claude Code can use MCP GitHub tools to manage issues programmatically:

```bash
# Create a new issue
create_issue(title, body, labels, milestone)

# List issues filtered by label or milestone
list_issues(labels=["mvp-phase-1"], state="open")

# Update issue status
update_issue(issue_number, state="closed")

# Add comments to issues
add_issue_comment(issue_number, comment_body)
```

### Connecting Issues to Code

When implementing features, reference issue numbers in commits:

```bash
git commit -m "feat: implement swipe interface for pet discovery

- Add SwipeController with /api/swipe endpoints
- Implement swipe action tracking in SwipeService
- Add tests for swipe functionality

Closes #3"
```

This automatically links commits to issues and closes issues when PRs are merged.

### Progress Reporting

Track overall project progress by monitoring:
- **Issues closed vs. total issues** per milestone
- **Velocity**: Average issues completed per sprint
- **Burndown**: Remaining work over time
- **Blockers**: Issues labeled with "blocked" requiring attention

Refer to `docs/GITHUB_ISSUES_ROADMAP.md` for the complete list of planned issues and their detailed specifications.
