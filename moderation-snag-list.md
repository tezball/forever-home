# Moderation Service UI - Snag List

**Review Date:** 2025-12-22
**Reviewer:** Support Agent QA Review
**Service URL:** http://localhost:8081
**Screenshots:** `.playwright-mcp/moderation-*.png`

---

## Summary

| Severity | Count |
|----------|-------|
| Critical | 1 |
| Major | 1 |
| Minor | 3 |
| Cosmetic | 1 |
| **Total** | **6** |

---

## Critical Issues

### SNAG-001: Mobile Navigation Completely Missing

**Severity:** Critical
**Page:** All pages
**Screenshot:** `moderation-mobile-nav-issue.png`

**Description:**
On mobile viewports (< 768px), the navigation menu links are hidden using `md:flex` (Tailwind's medium breakpoint), but there is no hamburger menu or alternative navigation provided. Users on mobile devices cannot navigate between pages.

**Steps to Reproduce:**
1. Open any page on the moderation service
2. Resize browser to mobile width (< 768px) or use mobile device
3. Observe that navigation links disappear completely

**Expected Behavior:**
A hamburger menu icon should appear that opens a mobile navigation drawer/menu.

**Actual Behavior:**
Navigation links simply disappear, leaving users stranded on the current page with no way to navigate (except Quick Actions on Dashboard or browser back button).

**Impact:**
Mobile users cannot use the application effectively. They can only navigate via:
- Quick Action buttons on Dashboard (limited pages)
- Manually typing URLs
- Browser back/forward buttons

**Affected Files:**
- `moderation-service/src/main/resources/templates/layout.html`
- All page templates that duplicate the nav structure

---

## Major Issues

### SNAG-002: Dev API Endpoints Return 500 Errors

**Severity:** Major
**Component:** Backend API Integration

**Description:**
The dev seed endpoints on the main application return 500 Internal Server errors, preventing test data from being loaded for moderation testing.

**Steps to Reproduce:**
1. Start dev services with `./dev.sh start`
2. Call `POST http://localhost:8080/api/dev/seed-demo-data`
3. Observe 500 error response

**Expected Behavior:**
Demo data should be seeded successfully, populating pets for moderation.

**Actual Behavior:**
```json
{"status":500,"error":"Internal Server Error","message":"An unexpected error occurred"}
```

**Impact:**
Cannot test full moderation workflow without manually creating test data through the main application UI.

---

## Minor Issues

### SNAG-003: Missing Favicon (404 Error)

**Severity:** Minor
**Page:** All pages

**Description:**
Console shows 404 error for `/favicon.ico` - the moderation service has no favicon configured.

**Steps to Reproduce:**
1. Open any page
2. Check browser console (F12 > Console)
3. Observe: `Failed to load resource: the server responded with a status of 404 () @ http://localhost:8081/favicon.ico`

**Expected Behavior:**
A favicon should be present, or the request should be handled gracefully.

**Actual Behavior:**
404 error logged to console on every page load.

**Suggested Fix:**
Add a favicon.ico to `src/main/resources/static/` or add a `<link rel="icon" href="data:,">` to suppress the request.

---

### SNAG-004: Tailwind CDN Warning

**Severity:** Minor
**Page:** All pages

**Description:**
Console shows warning that Tailwind CDN should not be used in production.

**Console Warning:**
```
cdn.tailwindcss.com should not be used in production. To use Tailwind CSS in production, install it as a PostCSS plugin or use the Tailwind CLI.
```

**Impact:**
- Performance: CDN adds latency and depends on external service
- Reliability: External CDN could be unavailable
- Not suitable for production deployment

**Suggested Fix:**
Build Tailwind CSS at compile time and serve static CSS file.

---

### SNAG-005: Form Select Dropdowns Missing Border Styling

**Severity:** Minor
**Page:** Multiple (Flagged, Batch, Jobs, Logs)

**Description:**
The `<select>` dropdowns use Tailwind's `border-gray-300` class but it doesn't render visibly in the current setup. Dropdowns appear without clear borders, making them less obvious as interactive elements.

**Affected Elements:**
- Category filter on Flagged page
- Limit filters on all pages
- Type filter on Logs page
- Pet Limit on Batch page

**Expected Behavior:**
Dropdowns should have visible borders matching the design system.

**Actual Behavior:**
Dropdowns render with very faint or no visible borders.

---

## Cosmetic Issues

### SNAG-006: Inconsistent Active Nav Styling

**Severity:** Cosmetic
**Page:** All pages

**Description:**
Each page template hardcodes its own navigation with the active state, rather than using a shared template with dynamic active state. This is a code maintainability issue that could lead to inconsistencies.

**Observation:**
- `layout.html` has Thymeleaf conditionals for active state (`th:classappend`)
- Individual page templates (dashboard.html, flagged.html, etc.) have hardcoded nav with active state already applied
- Templates don't use the layout fragment system consistently

**Impact:**
- Code duplication
- Risk of inconsistent navigation styling if templates diverge
- Harder to maintain

---

## Positive Findings (Working Correctly)

### Navigation (Desktop)
- All 6 navigation links work correctly
- Active page highlighting works
- "Moderation Service" logo links to dashboard

### API Status Indicator
- Shows green "API Connected" when main app is available
- Correctly positioned in header

### Empty States
- All pages display appropriate empty state messages
- Empty state text is helpful and actionable

### Error Handling
- Invalid UUID format: Shows "Invalid pet ID format: [value]" error
- Pet not found: Shows "Pet not found: [uuid]" error
- Errors displayed in styled alert boxes

### Forms & Filters
- Category dropdown populated with all moderation categories
- Limit dropdowns have reasonable options
- Filter buttons work correctly

### Tables
- Proper column headers
- Empty state spans full width
- Clickable rows have hover state (jobs, logs tables)

### Footer
- Present on all pages
- Consistent text

### Data Display
- Stats cards on Dashboard and Logs pages
- Progress indicators ready for job data
- Date formatting prepared

---

## Recommendations

### Priority 1 (Must Fix Before Release)
1. **SNAG-001**: Implement mobile hamburger menu navigation

### Priority 2 (Should Fix)
2. **SNAG-002**: Investigate and fix dev API seed endpoints
3. **SNAG-004**: Build Tailwind CSS at compile time for production

### Priority 3 (Nice to Have)
4. **SNAG-003**: Add favicon
5. **SNAG-005**: Improve dropdown border visibility
6. **SNAG-006**: Refactor templates to use shared layout consistently

---

## Test Coverage Notes

### Tested
- [x] All 6 main pages (Dashboard, Flagged, Batch, Pet Lookup, Jobs, Logs)
- [x] Navigation between pages
- [x] Empty states
- [x] Error handling (invalid UUID, not found)
- [x] Mobile responsiveness
- [x] Console errors
- [x] Filter dropdowns
- [x] API status indicator

### Not Tested (No Test Data)
- [ ] Tables with actual data
- [ ] Clickable row navigation to detail pages
- [ ] Approve/Reject actions on flagged content
- [ ] Batch moderation execution
- [ ] Single pet moderation
- [ ] Reset All Data functionality
- [ ] Clear Logs functionality
- [ ] Job details page with results
- [ ] Log details page with prompt/response
- [ ] Progress bars with actual progress
- [ ] Image display in pet details

### Requires Test Data
To fully test the UI, need pets with:
- Moderation status = PENDING
- Images uploaded
- Text content (name, description, health notes)
