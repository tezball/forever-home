---
description: Generate Playwright E2E test from a user story
user-invocable: true
args: "<story-name or feature description>"
---

Generate a Playwright E2E test for the specified user story or feature.

## Steps

1. **Understand the requirement**
   - Read `docs/user-stories.md` to find the relevant user story
   - Extract acceptance criteria that need test coverage
   - If no matching story, infer requirements from the feature description

2. **Study existing patterns**
   - Read `docs/ui-style-guide.md` for component selectors and accessibility patterns
   - Examine existing tests in `frontend/e2e/` for:
     - Auth helpers and fixtures (how to log in as different roles)
     - Page object patterns if used
     - Assertion styles and wait strategies
     - Test data setup/teardown

3. **Identify test scenarios**
   - Happy path (main success scenario)
   - Validation errors (required fields, format errors)
   - Authorization (correct role can access, others cannot)
   - Edge cases (empty states, max limits, special characters)

4. **Generate the test file**
   - Follow naming convention: `<feature>.spec.ts`
   - Use existing auth fixtures for role-based tests
   - Use data-testid or accessible selectors (role, label)
   - Include setup/teardown if test data is needed
   - Add descriptive test names that explain the scenario

5. **Validate the test**
   - Run `./dev.sh test e2e <new-test-name>` to verify it passes
   - If it fails, debug and fix issues
   - Ensure no flaky timing issues (use proper waits)

## Output

Create the test file in `frontend/e2e/` and report:
- File created with path
- Number of test cases
- Coverage of acceptance criteria
- Any assumptions made

## Example Test Structure

```typescript
import { test, expect } from '@playwright/test';
import { loginAs } from './helpers/auth';

test.describe('Feature Name', () => {
  test.beforeEach(async ({ page }) => {
    // Setup
  });

  test('should do the happy path thing', async ({ page }) => {
    // Arrange
    // Act
    // Assert
  });

  test('should show validation error when field is empty', async ({ page }) => {
    // Test validation
  });

  test('should deny access to unauthorized users', async ({ page }) => {
    // Test authorization
  });
});
```
