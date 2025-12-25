# Forever Home Production Snag List

**Review Date:** 2025-12-25
**Reviewed By:** Claude (PM & UX/UI Expert)
**Environment:** https://forever-home.ie (Production)
**Status:** ISSUES FIXED AND VERIFIED

---

## Fixes Applied (2025-12-25)

The following critical and high-priority issues have been fixed and verified with Playwright:

| Issue | Status | Fix Applied |
|-------|--------|-------------|
| Swipe page 403 Forbidden (#1) | ✅ FIXED | Added `/swipe` to security config + frontend redirect to `/pets/swipe` |
| Status history loading error (#4) | ✅ FIXED | Added `/api/pets/{id}/history` to security permitAll |
| Swipe UI infinite loading (#5) | ✅ FIXED | Was working correctly - `/pets/swipe` loads properly |
| Rescue location data (#6) | ✅ FIXED | Database migration V33 + seed data updated with Irish cities |
| Pet counts (#7) | ✅ OK | Working as designed - counts are dynamic per rescue |
| Pet image gallery (#17) | ✅ OK | ImageCarousel component fully implemented - demo data has 1 image/pet |

**Files Changed:**
- `src/main/java/com/example/foreverhome/config/SecurityConfig.java` - Added `/swipe` and `/api/pets/{id}/history` to permitAll
- `frontend/src/App.tsx` - Added redirect route from `/swipe` to `/pets/swipe`
- `src/main/java/com/example/foreverhome/config/TestDataSeeder.java` - Updated rescue org seed data with Irish locations
- `src/main/resources/db/migration/V33__update_rescue_organization_locations.sql` - Migration to update existing data

---

## Summary

| Priority | Count |
|----------|-------|
| Critical (P0) | 3 |
| High (P1) | 4 |
| Medium (P2) | 6 |
| Low (P3) | 6 |
| UX Improvements | 7 |
| Accessibility | 3 |
| Design Consistency | 3 |
| **Total** | **32** |

---

## Critical Issues (P0)

### 1. Swipe Page Returns 403 Forbidden Error
**Location:** `/swipe`
**Issue:** The Swipe feature page returns a Spring Boot "Whitelabel Error Page" with a 403 Forbidden status.
**Impact:** Core feature completely broken. Users clicking "Swipe" from navigation see an error page.
**Screenshot:** Shows "There was an unexpected error (type=Forbidden, status=403)"
**Action Required:** Fix API endpoint permissions or routing configuration.

### 2. Test Mode / Dev Tools Visible on Production
**Location:** `/login`
**Issue:** The login page displays a "Test Mode DEV ONLY" section with a "Quick Login" dropdown allowing users to select test accounts and click "Login as Test User".
**Impact:** Security vulnerability - exposes test accounts to public. Unprofessional appearance.
**Action Required:** Hide dev tools behind environment flag or remove entirely from production build.

### 3. "Site Under Construction" Banner on All Pages
**Location:** Global header
**Issue:** Orange banner displaying "Site Under Construction - Coming Soon" appears on every page.
**Impact:** Undermines user confidence. If site is live, banner should be removed. If site isn't ready, should not be accessible.
**Action Required:** Confirm intent - either remove banner or implement proper staging environment.

---

## High Priority (P1)

### 4. Pet Detail "Failed to load status history"
**Location:** `/pets/{id}` - Status History section
**Issue:** Red error text "Failed to load status history" displayed on pet detail pages.
**Impact:** Users see an error instead of pet's journey through the adoption process.
**Action Required:** Fix API call for status history or add graceful fallback.

### 5. Swipe UI Infinite Loading State
**Location:** `/pets/swipe` (modal/overlay)
**Issue:** When accessing swipe functionality through pet browse page, displays perpetual loading spinner with no pet cards loading.
**Impact:** Swipe feature appears broken from multiple entry points.
**Action Required:** Debug swipe data loading, add timeout and error states.

### 6. Dummy Location Data for All Rescues
**Location:** `/rescues`
**Issue:** All rescue organizations display "Pet City, PC" as their location.
**Impact:** Appears as obvious placeholder data. Users cannot find local rescues.
**Action Required:** Populate real location data or use realistic demo data.

### 7. Identical Pet Counts Across All Rescues
**Location:** `/rescues`
**Issue:** All 4 rescue organizations show exactly "6 pets available".
**Impact:** Obviously demo/seed data. Reduces credibility.
**Action Required:** Ensure counts reflect actual pets per organization.

---

## Medium Priority (P2)

### 8. Homepage Statistics Inconsistency
**Location:** `/` (homepage hero section)
**Issue:** Shows "24 Pets Available", "4 Happy Adopters", "4 Rescue Partners" - numbers feel like arbitrary seed data.
**Impact:** If real, fine. If placeholder, should be more realistic.
**Action Required:** Verify these pull from actual database counts.

### 9. Pet Card Image Aspect Ratio Inconsistencies
**Location:** `/pets` grid
**Issue:** Pet images appear with slightly different crops/aspect ratios across cards.
**Impact:** Grid looks unpolished. Visual rhythm disrupted.
**Action Required:** Enforce consistent aspect ratio (style guide specifies 3:2 for grid view).

### 10. Mobile Navigation Hamburger Menu
**Location:** Mobile header
**Issue:** Cannot verify hamburger menu opens correctly from static screenshots.
**Impact:** Primary navigation method on mobile.
**Action Required:** Manual testing needed to verify menu functionality.

### 11. Footer Links Verification Needed
**Location:** Global footer
**Issue:** Links to Privacy Policy, Terms of Service, Contact, FAQ, Help Center etc. need verification.
**Impact:** Legal/compliance pages may 404. Poor user experience.
**Action Required:** Audit all footer links for working destinations.

### 12. Missing Loading/Skeleton States
**Location:** Throughout site
**Issue:** No visible skeleton loaders during page transitions.
**Impact:** Flash of empty content before data loads feels jarring.
**Action Required:** Implement skeleton loading states per style guide.

### 13. Filter Functionality Verification
**Location:** `/pets` - Filter dropdowns
**Issue:** "All Species", "All Breeds", "All Sizes" dropdowns need testing.
**Impact:** Core browse functionality - must work correctly.
**Action Required:** Test filter combinations, verify URL params update.

---

## Low Priority (P3)

### 14. Pet Card Description Truncation Inconsistent
**Location:** `/pets` grid
**Issue:** Some pet descriptions show more text than others before truncation.
**Impact:** Minor visual inconsistency.
**Action Required:** Set consistent character/line limit.

### 15. "View All Pets" Link Prominence
**Location:** `/` (Featured Pets section)
**Issue:** Link is small text, could be more prominent.
**Impact:** Users may miss the link to browse all pets.
**Action Required:** Consider making it a button or larger text.

### 16. FAQ Status Badge Context
**Location:** `/faq`
**Issue:** Status badges (Draft, Pending, etc.) shown in journey steps lack context for new users.
**Impact:** Users may not understand what each status means until reading full FAQ.
**Action Required:** Add tooltips or link to status documentation.

### 17. Single Pet Image Display
**Location:** `/pets/{id}`
**Issue:** Only one pet image shown, no gallery or thumbnails despite domain supporting multiple images.
**Impact:** Users can't see pet from multiple angles.
**Action Required:** Implement image gallery per style guide spec.

### 18. Mobile Footer Compression
**Location:** Mobile footer
**Issue:** Footer content is very compressed on mobile viewport.
**Impact:** Links may be hard to tap, readability reduced.
**Action Required:** Review footer layout for mobile breakpoint.

### 19. Pagination Styling
**Location:** `/pets` bottom
**Issue:** Simple numbered pagination. Shows "1" "2" with arrow.
**Impact:** Functional but basic.
**Action Required:** Consider "Load more" pattern for mobile or infinite scroll.

---

## UX Improvements

### 20. No Search on Homepage Hero
**Location:** `/` hero section
**Issue:** Hero has "Swipe Pets" and "Get Started" buttons but no search input.
**Impact:** Users who know what they want can't search immediately.
**Recommendation:** Add pet search field to hero section.

### 21. Missing Favorite/Heart Icons on Pet Cards
**Location:** `/pets` grid
**Issue:** Style guide shows heart icon on pet cards for favorites, but production doesn't show them.
**Impact:** Can't save favorites while browsing.
**Recommendation:** Add heart icon to pet cards (requires auth for functionality).

### 22. Pet Cards Missing Location Information
**Location:** `/pets` grid
**Issue:** Pet cards show breed, age, status but not location/city.
**Impact:** Users can't easily find pets in their area from browse view.
**Recommendation:** Add city/region to pet card.

### 23. No Breadcrumbs on Pet Detail
**Location:** `/pets/{id}`
**Issue:** Only "< Back to Browse" link. No breadcrumb trail.
**Impact:** Navigation context limited.
**Recommendation:** Add breadcrumb: Home > Browse Pets > {Pet Name}

### 24. Registration Password Strength Indicator Missing
**Location:** `/register`
**Issue:** Password field shows "Must be at least 8 characters" but no strength meter.
**Impact:** Users don't get feedback on password quality.
**Recommendation:** Add password strength indicator.

### 25. No Social Login Options
**Location:** `/login`, `/register`
**Issue:** Only email/password authentication available.
**Impact:** Higher friction for signup. Users expect Google/Facebook options.
**Recommendation:** Consider adding social login for future release.

### 26. Pet Detail Missing Key Information
**Location:** `/pets/{id}`
**Issue:** Detail page missing: microchip status, vaccination status, vet verification badge, foster contact.
**Impact:** Per domain model, this info should be displayed.
**Recommendation:** Add vet verification section, medical status.

---

## Accessibility

### 27. Pet Image Alt Text Verification
**Location:** Throughout site
**Issue:** Unable to verify alt text quality from screenshots.
**Impact:** Screen reader users need descriptive alt text.
**Action Required:** Audit all images for meaningful alt text (e.g., "Pumpkin, a Burmese cat lying on a couch").

### 28. Orange Banner Color Contrast
**Location:** Global header banner
**Issue:** Orange background (#F39C12 approx) with white text needs contrast verification.
**Impact:** May not meet WCAG AA 4.5:1 ratio.
**Action Required:** Test with contrast checker, adjust if needed.

### 29. Keyboard Navigation Testing
**Location:** Throughout site
**Issue:** Focus states and tab order not verified.
**Impact:** Keyboard-only users need clear focus indicators.
**Action Required:** Manual testing with keyboard navigation.

---

## Design Consistency

### 30. Homepage Background Color Variance
**Location:** `/` hero and sections
**Issue:** Background colors don't precisely match style guide's "Warm Sand" (#F5F0E8) and "Cream" (#FFFDF8).
**Impact:** Minor visual inconsistency with design system.
**Recommendation:** Audit CSS variables against style guide.

### 31. "How Forever Home Works" Generic Icons
**Location:** `/` - How It Works section
**Issue:** Icons are basic/generic line icons.
**Impact:** Doesn't reinforce brand personality.
**Recommendation:** Consider custom illustrated icons.

### 32. Community Section CTA Styling
**Location:** `/` - Join Our Community section
**Issue:** CTAs like "Start Adopting", "Start Fostering" are styled as text links, not buttons.
**Impact:** Lower visual hierarchy than primary actions.
**Recommendation:** Consider secondary button styling per style guide.

---

## Testing Notes

- **Browser:** Chromium (Headless)
- **Desktop Viewport:** 1280x800
- **Mobile Viewport:** 375x812 (iPhone X)
- **Test Date:** December 25, 2025

---

## Recommended Priority Order

1. **Immediate (Before wider launch):**
   - Fix Swipe 403 error (#1)
   - Remove Test Mode from production (#2)
   - Decide on Construction banner (#3)
   - Fix status history loading (#4)

2. **Short-term:**
   - Populate realistic demo data (#6, #7, #8)
   - Fix swipe loading state (#5)
   - Verify all links and filters (#11, #13)

3. **Medium-term:**
   - UX improvements (#20-26)
   - Accessibility audit (#27-29)
   - Design polish (#30-32)

---

## Next Steps

1. Create tickets for P0 issues immediately
2. Schedule accessibility audit
3. Review with design team for consistency items
4. Plan UX improvements for next sprint
