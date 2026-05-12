# Notepad API — Test Automation Suite

Automated API test suite for the **Notepad REST Service** at `https://notepad.neuroval.com`.
Built with **Microsoft Playwright for Java**, **JUnit 5**, **AssertJ**, and **Allure Reports**.

---
## TEST CASES
src/test/resources/notepad-api-test-cases.xlsx
## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [Running the Tests](#running-the-tests)
- [Test Reports](#test-reports)
- [Test Suite Breakdown](#test-suite-breakdown)
- [Architecture Decisions](#architecture-decisions)
- [Known Issues & Gotchas](#known-issues--gotchas)
- [CI/CD Integration](#cicd-integration)
- [Contributing](#contributing)

---

## Overview

This project validates all four endpoints of the Notepad REST API through a comprehensive set of **53 test cases** covering positive paths, negative/validation scenarios, boundary conditions, and end-to-end lifecycle flows.

| Suite | Class | Test Cases |
|---|---|---|
| POST /v1/note/save | `NoteSaveTest` | 17 |
| DELETE /v1/note/delete/{id} | `NoteDeleteTest` | 8 |
| PUT /v1/note/edit | `NoteEditTest` | 11 |
| GET /v1/note/get-all/page/{page} | `NoteGetAllTest` | 13 |
| End-to-End Lifecycle | `NoteLifecycleE2ETest` | 4 |
| **Total** | | **53** |

---

## Tech Stack

| Library | Version | Purpose |
|---|---|---|
| [Microsoft Playwright for Java](https://playwright.dev/java/) | 1.44.0 | HTTP API client (`APIRequestContext`) |
| JUnit 5 | 5.10.2 | Test runner, `@ParameterizedTest`, `@Order` |
| AssertJ | 3.25.3 | Fluent, readable assertions |
| Jackson Databind | 2.17.0 | JSON serialization / deserialization |
| Allure JUnit5 | 2.27.0 | Rich HTML test reports |
| Lombok | 1.18.32 | Boilerplate reduction (`@Builder`, `@Data`) |
| Logback | 1.5.3 | Console and file logging |
| Maven Surefire | 3.2.5 | Test execution and JUnit Platform integration |

---

## Prerequisites

| Tool | Minimum Version | Install |
|---|---|---|
| Java (JDK) | 17 | https://adoptium.net |
| Apache Maven | 3.8 | https://maven.apache.org/download.cgi |
| Allure CLI *(optional, for reports)* | 2.x | https://allurereport.org/docs/install/ |

Verify your environment:

```bash
java -version      # must show 17 or higher
mvn -version       # must show 3.8 or higher
allure --version   # optional
```

> **Playwright browsers**: Playwright Java uses its own bundled browser binaries for web testing. For API testing only (`APIRequestContext`), no browser binaries are needed — the library uses plain HTTP.

---

## Project Structure

```
notepad-api-tests/
│
├── pom.xml                                     # Maven build & dependency config
│
└── src/test/java/com/neuroval/notepad/
    │
    ├── config/
    │   ├── ApiConfig.java                      # Base URL + all endpoint constants
    │   └── BaseTest.java                       # Playwright setup/teardown (@BeforeAll / @AfterAll)
    │
    ├── models/
    │   ├── SaveNoteRequest.java                # Request body for POST /note/save
    │   └── EditNoteRequest.java                # Request body for PUT /note/edit
    │
    └── tests/
        ├── NoteSaveTest.java                   # 17 tests — POST /v1/note/save
        ├── NoteDeleteTest.java                 # 8 tests  — DELETE /v1/note/delete/{id}
        ├── NoteEditTest.java                   # 11 tests — PUT /v1/note/edit
        ├── NoteGetAllTest.java                 # 13 tests — GET /v1/note/get-all/page/{page}
        └── NoteLifecycleE2ETest.java           # 4 tests  — Full CRUD lifecycle
```

---

## Configuration

All environment settings live in one place: `src/test/java/com/neuroval/notepad/config/ApiConfig.java`.

```java
public static final String BASE_URL     = "https://notepad.neuroval.com";

public static final String NOTE_SAVE    = BASE_URL + "/v1/note/save";
public static final String NOTE_EDIT    = BASE_URL + "/v1/note/edit";
public static final String NOTE_DELETE  = BASE_URL + "/v1/note/delete/";
public static final String NOTE_GET_ALL = BASE_URL + "/v1/note/get-all/page/";
```

To target a different environment (e.g. local dev or staging), change `BASE_URL`:

```java
public static final String BASE_URL = "http://localhost:8080";
```

> **Important — Playwright `baseURL` gotcha**: Playwright's `APIRequestContext` drops path segments like `/v1` when resolving relative URLs against a `baseURL`. To avoid this, all endpoints in this project are defined as **full absolute URLs**. Do not switch to relative paths without understanding this behaviour.

---

## Running the Tests

### Run all tests

```bash
cd notepad-api-tests
mvn test
```

### Run a single test class

```bash
mvn test -Dtest=NoteSaveTest
mvn test -Dtest=NoteDeleteTest
mvn test -Dtest=NoteEditTest
mvn test -Dtest=NoteGetAllTest
mvn test -Dtest=NoteLifecycleE2ETest
```

### Run a single test method

```bash
mvn test -Dtest="NoteSaveTest#tc_save_001_validRequest_returns200"
```

### Run tests in parallel (experimental)

Add to `pom.xml` inside the Surefire `<configuration>` block:

```xml
<parallel>classes</parallel>
<threadCount>4</threadCount>
```

> Note: Tests within `NoteLifecycleE2ETest` should remain sequential — parallel execution can cause ordering issues on shared server data.

### Skip tests

```bash
mvn install -DskipTests
```

---

## Test Reports

### Allure HTML Report (recommended)

Allure produces a rich interactive report showing pass/fail per test, request and response details, severity levels, and trend charts.

```bash
# Step 1 – run the tests (generates raw results in target/allure-results)
mvn test

# Step 2 – generate the HTML report
mvn allure:report

# Step 3 – open in browser (starts a local server)
mvn allure:open
```

The report will open at `http://localhost:PORT/` and includes:

- **Suites** view — tests grouped by class and feature
- **Behaviors** view — tests grouped by Epic / Feature / Story (`@Epic`, `@Feature`, `@Story` annotations)
- **Timeline** — execution duration per test
- **Categories** — failed vs broken vs passed breakdown

### Maven Surefire plain-text report

Located at `target/surefire-reports/` after running `mvn test`. Useful for quick console-level inspection.

---

## Test Suite Breakdown

### `NoteSaveTest` — POST /v1/note/save

Tests the note creation endpoint with 17 cases:

- **Happy path**: valid title + content returns 200, all fields populated correctly, title and content echoed back in response
- **Schema validation**: response has `status`, `message`, `data` fields; `data.id` is a positive integer; `data.author` is non-blank; dates are set
- **Boundary**: single-char title, 255-char title, 5000-char content
- **Special characters**: titles and content with HTML entities, emojis, symbols
- **Negative**: empty title, empty content, null title, null content, whitespace-only title
- **Headers**: `Content-Type: application/json` present in response
- **Parameterized**: 3 different title/content pairs saved in one test

### `NoteDeleteTest` — DELETE /v1/note/delete/{id}

Tests note deletion with 8 cases:

- **Happy path**: delete a pre-created note returns 200, `data: true`, message contains note ID
- **Double-delete**: deleting the same ID twice — first succeeds, second must return 4xx
- **Invalid IDs**: non-existent ID (999999), negative ID (-1), zero (0), non-numeric string ("abc")

### `NoteEditTest` — PUT /v1/note/edit

Tests note content editing with 11 cases:

- **Happy path**: valid ID + new content returns 200 with `"Note updated!"`
- **Schema validation**: response structure, exact status and message values
- **Double-edit**: editing the same note twice — both calls should succeed
- **Negative**: empty content, null content, non-existent ID, null ID, negative ID
- **Boundary**: 5000-character content
- **Special characters**: HTML tags, emojis, symbols in content

### `NoteGetAllTest` — GET /v1/note/get-all/page/{page}

Tests the paginated listing endpoint with 13 cases:

- **Happy path**: page 0 returns 200, correct status/message, data is an array
- **Schema validation**: every note in the array has `title`, `content`, `author`; no null titles
- **Pagination**: pages 0, 1, 2, 5, 10 all return 200 (parameterized); very high page (99999) returns 200 with empty array
- **Cross-endpoint persistence**: saves a note with a unique title and verifies it appears in the listing
- **Negative**: negative page (-1), non-numeric page ("abc")
- **Headers**: `Content-Type: application/json`

### `NoteLifecycleE2ETest` — Full CRUD Lifecycle

Four end-to-end scenarios that span multiple endpoints in sequence:

| Test | Flow |
|---|---|
| TC-E2E-001 | Create → Edit → Verify in listing → Delete |
| TC-E2E-002 | Create 3 notes → Verify all 3 appear on page 0 |
| TC-E2E-003 | Create → Delete → Attempt second delete (must fail) |
| TC-E2E-004 | Create → Edit → Verify edited content appears in listing |

---

## Architecture Decisions

### Why Playwright for API testing?

Microsoft Playwright's `APIRequestContext` provides a clean, modern HTTP client with built-in support for headers, request body, and response inspection — all without needing a browser. It integrates naturally with the same toolchain used for browser-based E2E tests, making it easy to extend this suite into UI testing later.

### Why absolute URLs instead of `baseURL`?

Playwright resolves relative URLs against `baseURL` using standard URL resolution rules. When `baseURL` ends with a path segment (e.g. `/v1`), a relative path starting with `/` replaces that path entirely — so `/note/save` resolves to `https://host/note/save`, not `https://host/v1/note/save`. Using full absolute URLs in `ApiConfig` avoids this entirely and makes each endpoint self-documenting.

### One `Playwright` + `APIRequestContext` per test class

`@BeforeAll` / `@AfterAll` in `BaseTest` create and dispose the Playwright instance at the class level. This is a good balance — cheaper than per-test creation, and avoids shared state leaking between unrelated classes.

### Helper methods for test setup

Tests that need a pre-existing note (Delete, Edit, E2E) use a private `createNote()` helper rather than relying on static fixture data. This makes tests self-contained and resilient to database resets.

---

## Known Issues & Gotchas

**Negative test status codes**: Tests for invalid inputs (empty fields, null IDs, etc.) use `isBetween(400, 599)` rather than asserting a specific code. The exact error code (400, 404, 422, 500) depends on your Spring Boot validation configuration. Once you confirm the API's error behaviour, tighten these to single expected values.

**Server-side validation**: Some negative tests (empty title, whitespace title) may currently return 200 if the server does not enforce these constraints. These are intentional test-document-first cases — they define expected behaviour and will correctly fail until server-side validation is added.

**Test ordering**: Tests within each class run alphabetically by method name (configured in `pom.xml`). The `@Order` annotations on methods are for documentation clarity and Allure report ordering — they do not affect Surefire execution order.

**Shared server state**: The suite creates and deletes real data on `notepad.neuroval.com`. Running the suite multiple times accumulates test notes on the server. Consider adding a cleanup step or running against a dedicated test environment.

---

## CI/CD Integration

### GitHub Actions

```yaml
name: API Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run API tests
        run: mvn test --no-transfer-progress

      - name: Generate Allure report
        if: always()
        run: mvn allure:report

      - name: Upload Allure results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: allure-report
          path: target/site/allure-maven-plugin/
```

### Azure DevOps

```yaml
- task: Maven@4
  displayName: 'Run API Tests'
  inputs:
    mavenPomFile: 'pom.xml'
    goals: 'test'
    options: '--no-transfer-progress'
    publishJUnitResults: true
    testResultsFiles: '**/surefire-reports/TEST-*.xml'
    javaHomeOption: 'JDKVersion'
    jdkVersionOption: '1.17'

- task: Maven@4
  displayName: 'Generate Allure Report'
  condition: always()
  inputs:
    mavenPomFile: 'pom.xml'
    goals: 'allure:report'
```

### Jenkins (Declarative Pipeline)

```groovy
pipeline {
    agent any
    tools { jdk 'JDK17'; maven 'Maven3' }
    stages {
        stage('Test') {
            steps {
                sh 'mvn test --no-transfer-progress'
            }
            post {
                always {
                    sh 'mvn allure:report'
                    junit 'target/surefire-reports/*.xml'
                    publishHTML([
                        reportDir: 'target/site/allure-maven-plugin',
                        reportFiles: 'index.html',
                        reportName: 'Allure Report'
                    ])
                }
            }
        }
    }
}
```

---

## Contributing

1. Add new test cases to the appropriate existing test class, or create a new class for a new endpoint.
2. Follow the naming convention: `tc_<suite>_<number>_<short_description>`.
3. Use `@Story`, `@Severity`, and `@DisplayName` annotations so the Allure report stays organised.
4. Add a helper method if your test needs pre-created data — never rely on IDs or state from other tests.
5. Update `notepad-api-test-cases.xlsx` to document any new test cases.

