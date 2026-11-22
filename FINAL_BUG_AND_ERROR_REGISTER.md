# SANKHYA — FINAL BUG AND ERROR REGISTER
**Phase:** Phase 6 Final Integration & Local Verification
**Status:** All Verified Defects Resolved & Tested

---

## 1. Defect Summary Table

| ID | Priority | Module | Description | Root Cause | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **DEF-01** | High | JPA Configuration | `CustomerClusterAssignmentRepository` bean missing | `DatabaseConfig.java` omitted `com.sankhya.ml.repository` from `@EnableJpaRepositories` | **RESOLVED** |
| **DEF-02** | Medium | Gemini AI | `DatePreset` symbol not found in `GeminiPromptService` | Incorrect package import (`.dto` instead of `.enums`) | **RESOLVED** |
| **DEF-03** | Medium | Analytics DTO | `getAverageBasketValue()` method missing | Renamed to `getAverageTransactionValue()` in Phase 3 | **RESOLVED** |
| **DEF-04** | Medium | Gemini Prompting | Type mismatch in top products mapping | Expected `List<Object[]>`, stream returned `List<Map<String, Object>>` | **RESOLVED** |
| **DEF-05** | High | Unit Testing | Controller test constructors outdated | Added Phase 5 dependencies without updating test mocks | **RESOLVED** |
| **DEF-06** | Medium | Date Filtering | Custom date filter inverted dates returned 0 results | Missing date normalization in `DateRangeFilter` | **RESOLVED** |
| **DEF-07** | Critical | Security / Tenancy | Potential IDOR vulnerability in workspace switching | Request parameters previously accepted unverified workspace IDs | **RESOLVED** |

---

## 2. Detailed Defect Analysis & Fixes

### DEF-01: Missing Base Package in DatabaseConfig
- **Impact:** Application failed to boot with `NoSuchBeanDefinitionException` when injecting ML repositories.
- **Affected File:** `src/main/java/com/sankhya/config/DatabaseConfig.java`
- **Correction:** 
  ```java
  @EnableJpaRepositories(basePackages = {
      "com.sankhya.repository",
      "com.sankhya.dataset.repository",
      "com.sankhya.analytics.recommendations.repository",
      "com.sankhya.ml.repository" // Added in Phase 5
  })
  ```
- **Verification:** Verified via `SankhyaApplicationTests` and context boot.

### DEF-02: DatePreset Package Import
- **Impact:** Maven compilation error in `GeminiPromptService.java`.
- **Affected File:** `src/main/java/com/sankhya/ai/service/GeminiPromptService.java`
- **Correction:** Changed import from `com.sankhya.analytics.common.dto.DatePreset` to `com.sankhya.analytics.common.enums.DatePreset`.
- **Verification:** `mvn testCompile` succeeded.

### DEF-03: DTO Method Name Inconsistency
- **Impact:** Compilation error in prompt service when pulling executive overview metrics.
- **Affected File:** `src/main/java/com/sankhya/ai/service/GeminiPromptService.java`
- **Correction:** Substituted `overview.getAverageBasketValue()` with `overview.getAverageTransactionValue()`.
- **Verification:** Prompt service successfully builds executive prompt text with live transaction data.

### DEF-04: Stream Type Inference in Prompt Generation
- **Impact:** Incompatible types compile error in `buildProductPrompt`.
- **Affected File:** `src/main/java/com/sankhya/ai/service/GeminiPromptService.java`
- **Correction:** Updated variable declaration to `List<Map<String, Object>>` matching the repository projected stream output.
- **Verification:** Tested with `GeminiPromptServiceTest`.

### DEF-05: Unit Test Mock Constructors Out of Sync
- **Impact:** `SalesAnalyticsControllerTest` and `AiInsightsControllerTest` failed during Maven test phase.
- **Affected Files:** `SalesAnalyticsControllerTest.java`, `AiInsightsControllerTest.java`
- **Correction:** Added `@Mock` declarations for `ForecastResultRepository`, `MLJobService`, `GeminiInsightService`, and `GeminiChatService`, and injected them into the controller constructor.
- **Verification:** All controller unit tests passed (3/3 and 2/2).

### DEF-06: Date Inversion Handling in DateRangeFilter
- **Impact:** If a user accidentally selected a start date later than the end date in the UI datepicker, SQL range queries failed with empty results.
- **Affected File:** `src/main/java/com/sankhya/analytics/common/dto/DateRangeFilter.java`
- **Correction:**
  ```java
  if (this.startDate.isAfter(this.endDate)) {
      LocalDate tmp = this.startDate;
      this.startDate = this.endDate;
      this.endDate = tmp;
  }
  ```
- **Verification:** Unit tests confirm date normalization.

### DEF-07: Cross-Workspace Query Isolation Boundary
- **Impact:** Unauthorized store access if an attacker tampered with workspace query parameters.
- **Affected Files:** All controllers in `com.sankhya.analytics.*` and `com.sankhya.dashboard.*`.
- **Correction:** Centralized active workspace resolution through `WorkspaceService.getActiveWorkspace(session, userDetails.getId())` which queries `workspace_memberships` and throws `UnauthorizedAccessException` on unauthorized access.
- **Verification:** Verified via `WorkspaceServiceTest` and multi-tenant isolation tests.
