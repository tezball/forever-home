# Forever Home - Testing Strategy

This document outlines the testing strategy for the Forever Home pet adoption platform.

## Testing Layers

### 1. Unit Tests

**Purpose**: Test individual components in isolation.

**Coverage Target**: 80% of business logic code

**Location**: `src/test/java/com/example/foreverhome/`

**What to Test**:
- Domain model validations and state transitions
- Service layer business logic
- Utility functions
- DTO mappings

**Example Test Categories**:
```
domain/
  - PetTest.java (pet status transitions)
  - UserTest.java (account status, password hashing)
  - AgeUnitTest.java (age calculations)

service/
  - PetServiceTest.java
  - AuthServiceTest.java
  - NotificationServiceTest.java
```

**Running Unit Tests**:
```bash
./mvnw test -Dtest=*Test
```

### 2. Integration Tests

**Purpose**: Test components working together with real dependencies.

**Coverage Target**: All API endpoints, all database operations

**Location**: `src/test/java/com/example/foreverhome/integration/`

**What to Test**:
- Controller endpoints with real service layer
- Repository queries with test database
- Security configuration
- Transaction management

**Test Setup**:
```java
@SpringBootTest
@Testcontainers
class PetControllerIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    // Tests...
}
```

**Running Integration Tests**:
```bash
./mvnw test -Dtest=*IntegrationTest
```

### 3. Load Tests (Gatling)

**Purpose**: Verify system performance under load.

**Location**: `src/test/java/com/example/foreverhome/simulation/`

**Simulations**:
| Simulation | Purpose | Users | Duration |
|------------|---------|-------|----------|
| UserRegistrationSimulation | Test registration throughput | 50 | 2 min |
| RegistrationStressSimulation | Stress test capacity | 200 | 5 min |
| AllUserFlowsSimulation | Full workflow coverage | 100 | 10 min |
| FullFeatureEnduranceSimulation | Long-running stability | 50 | 30 min |

**Running Load Tests**:
```bash
# Default simulation
./mvnw gatling:test

# Specific simulation
./mvnw gatling:test -Dgatling.simulationClass=com.example.foreverhome.simulation.AllUserFlowsSimulation

# Custom parameters
./mvnw gatling:test -DBASE_URL=http://localhost:8080 -DUSERS=100 -DRAMP_DURATION=60
```

**Performance Targets**:
- Response time p95: < 500ms
- Error rate: < 1%
- Throughput: > 100 requests/second

### 4. End-to-End Tests (Playwright)

**Purpose**: Verify complete user workflows from UI to database.

**Location**: `frontend/e2e/` (Playwright tests)

**Test Files**:
| File | Description |
|------|-------------|
| `homepage.spec.ts` | Homepage rendering, hero section, navigation |
| `auth.spec.ts` | Login, register, forgot password, protected routes |
| `pet-browsing.spec.ts` | Browse pets, filters, pet details, rescue orgs |
| `navigation.spec.ts` | Header, footer, mobile menu, static pages, 404 |
| `authenticated-flows.spec.ts` | Dashboard tests for all user roles |

**Test Fixtures**:
```typescript
// e2e/fixtures/auth.fixture.ts
export const TEST_ACCOUNTS = {
  admin: { email: 'admin@test.com', password: 'password123' },
  foster: { email: 'foster@test.com', password: 'password123' },
  adopter: { email: 'adopter@test.com', password: 'password123' },
  vet: { email: 'vet@test.com', password: 'password123' },
  rescue: { email: 'rescue@test.com', password: 'password123' },
};
```

**Test Scenarios**:

#### Foster Flow
1. Register as foster
2. Complete profile
3. Create pet with images
4. Submit pet for rescue review
5. View pet status updates

#### Adopter Flow
1. Register as adopter
2. Browse available pets
3. Add pets to favorites
4. Submit adoption application
5. Track application status

#### Rescue Org Flow
1. Register as rescue org
2. Admin approves rescue org
3. Review pending pets
4. Accept/decline pets
5. Manage vet approvals
6. Process applications

#### Vet Flow
1. Register as vet
2. Get approved by rescue org
3. Lookup pet by microchip
4. Sign off on pet health
5. View sign-off history

#### Admin Flow
1. Login as admin
2. View pending approvals
3. Approve/reject rescue orgs
4. Manage users
5. View analytics

**Running E2E Tests**:
```bash
cd frontend

# Run all tests (headless)
npm run test:e2e

# Run with UI mode (interactive)
npm run test:e2e:ui

# Run with headed browsers (visible)
npm run test:e2e:headed

# Run specific test file
npx playwright test homepage.spec.ts

# Run specific test
npx playwright test -g "should display the homepage"

# Run on specific browser
npx playwright test --project=chromium
npx playwright test --project=firefox
npx playwright test --project=webkit
npx playwright test --project="Mobile Chrome"
```

**Playwright Configuration** (`frontend/playwright.config.ts`):
- Browsers: Chromium, Firefox, WebKit, Mobile Chrome
- Auto-starts dev server before tests
- Retries on CI, screenshots on failure
- HTML report generated

## Test Data Management

### Seeded Test Accounts

The application seeds test accounts when `app.test-mode.enabled=true`:

| Role | Email | Password |
|------|-------|----------|
| Foster | foster@test.com | password123 |
| Adopter | adopter@test.com | password123 |
| Rescue Org | rescue@test.com | password123 |
| Vet | vet@test.com | password123 |
| Admin | admin@test.com | password123 |

### Test Database

Integration tests use Testcontainers to spin up PostgreSQL:

```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
    .withDatabaseName("foreverhome_test")
    .withUsername("test")
    .withPassword("test");
```

### Test Fixtures

Create reusable test data builders:

```java
public class TestFixtures {
    public static Pet createTestPet() {
        return Pet.create(
            UUID.randomUUID(),  // fosterId
            "Test Dog",
            Species.DOG,
            "Labrador",
            2, AgeUnit.YEARS,
            PetSex.MALE,
            PetSize.LARGE,
            "MC" + System.currentTimeMillis(),
            "A friendly test dog",
            null
        );
    }
}
```

## Continuous Integration

### GitHub Actions Workflow

```yaml
name: Test

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_DB: foreverhome_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'graalvm'

      - name: Run Tests
        run: ./mvnw test

      - name: Upload Coverage
        uses: codecov/codecov-action@v4
        with:
          file: target/site/jacoco/jacoco.xml
```

## Code Coverage

### Coverage Requirements

| Package | Minimum Coverage |
|---------|------------------|
| domain/ | 90% |
| service/ | 80% |
| controller/ | 70% |
| config/ | 50% |

### Generating Coverage Report

```bash
./mvnw test jacoco:report
```

Report location: `target/site/jacoco/index.html`

## Testing Best Practices

### 1. Test Naming Convention

```java
@Test
void shouldCreatePetWithValidData() { }

@Test
void shouldThrowExceptionWhenMicrochipDuplicate() { }

@Test
void shouldReturnEmptyListWhenNoAvailablePets() { }
```

### 2. Arrange-Act-Assert Pattern

```java
@Test
void shouldTransitionPetToAvailableAfterVetSignOff() {
    // Arrange
    Pet pet = createPetInPendingVetStatus();
    UUID vetUserId = createApprovedVet();

    // Act
    PetDto result = petService.signOffByVet(pet.getId(), vetUserId);

    // Assert
    assertThat(result.status()).isEqualTo(PetStatus.AVAILABLE);
}
```

### 3. Test Isolation

- Each test should be independent
- Use @Transactional for database cleanup
- Avoid shared mutable state

### 4. Meaningful Assertions

```java
// Good
assertThat(pets)
    .hasSize(3)
    .extracting(Pet::getStatus)
    .containsOnly(PetStatus.AVAILABLE);

// Avoid
assertTrue(pets.size() == 3);
```

## Known Test Gaps

Areas that need additional test coverage:

1. **Email Service**: Mock SES for email tests
2. **S3 Upload**: Mock S3 client or use LocalStack
3. **JWT Token Expiry**: Test token refresh flow
4. **Rate Limiting**: Test rate limit enforcement
5. **Concurrent Operations**: Test race conditions

## Running the Full Test Suite

```bash
# All tests
./mvnw verify

# With coverage report
./mvnw verify jacoco:report

# Skip slow tests for quick feedback
./mvnw test -DexcludedGroups=slow,integration
```

## Test Maintenance

### Quarterly Review

- Review and update test coverage targets
- Remove obsolete tests
- Update test data fixtures
- Review load test thresholds

### Test Documentation

Keep test documentation updated in:
- This document (testing-strategy.md)
- E2E test checklist (e2e-review.md)
- API documentation (swagger-ui)
