package com.neuroval.notepad.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import com.neuroval.notepad.config.ApiConfig;
import com.neuroval.notepad.config.BaseTest;
import com.neuroval.notepad.models.SaveNoteRequest;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test Suite: GET /note/get-all/page/{page}
 *
 *  TC-GET-001  Page 0 → 200 Successful
 *  TC-GET-002  Response has status, message, data fields
 *  TC-GET-003  status=Successful, message=All notes returned!
 *  TC-GET-004  data field is a JSON array
 *  TC-GET-005  Every note in data has title, content, author
 *  TC-GET-006  No note has a null title
 *  TC-GET-007  Page 1 returns 200
 *  TC-GET-008  Very high page number → 200 with empty or valid array
 *  TC-GET-009  Negative page → 4xx
 *  TC-GET-010  Non-numeric page string → 4xx
 *  TC-GET-011  A saved note appears in page 0 results
 *  TC-GET-012  Response Content-Type is application/json
 *  TC-GET-013  Parameterized – pages 0, 1, 2, 5 all return 200
 */
@Epic("Notepad REST API")
@Feature("Get All Notes – GET /note/get-all/page/{page}")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("TC-GET | GET /note/get-all/page/{page}")
class NoteGetAllTest extends BaseTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private APIResponse getPage(String page) {
        return request.get(ApiConfig.NOTE_GET_ALL + page);
    }

    // ── TC-GET-001 ────────────────────────────────────────────────────────────
    @Test @Order(1)
    @Story("Happy path") @Severity(SeverityLevel.BLOCKER)
    @DisplayName("TC-GET-001 | Page 0 → 200 Successful")
    void tc_get_001_page0_returns200() throws Exception {
        APIResponse response = getPage("0");
        assertThat(response.status()).isEqualTo(200);
        JsonNode json = MAPPER.readTree(response.body());
        assertThat(json.get("status").asText()).isEqualTo("Successful");
    }

    // ── TC-GET-002 ────────────────────────────────────────────────────────────
    @Test @Order(2)
    @Story("Response structure") @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-GET-002 | Response has status, message, data fields")
    void tc_get_002_responseHasRequiredFields() throws Exception {
        JsonNode json = MAPPER.readTree(getPage("0").body());
        assertThat(json.has("status")).isTrue();
        assertThat(json.has("message")).isTrue();
        assertThat(json.has("data")).isTrue();
    }

    // ── TC-GET-003 ────────────────────────────────────────────────────────────
    @Test @Order(3)
    @Story("Response data integrity") @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-GET-003 | status=Successful, message=All notes returned!")
    void tc_get_003_correctStatusAndMessage() throws Exception {
        JsonNode json = MAPPER.readTree(getPage("0").body());
        assertThat(json.get("status").asText()).isEqualTo("Successful");
        assertThat(json.get("message").asText()).isEqualTo("All notes returned!");
    }

    // ── TC-GET-004 ────────────────────────────────────────────────────────────
    @Test @Order(4)
    @Story("Response data integrity") @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-GET-004 | data field is a JSON array")
    void tc_get_004_dataIsArray() throws Exception {
        JsonNode json = MAPPER.readTree(getPage("0").body());
        assertThat(json.get("data").isArray()).isTrue();
    }

    // ── TC-GET-005 ────────────────────────────────────────────────────────────
    @Test @Order(5)
    @Story("Response data integrity") @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-GET-005 | Every note has title, content, author fields")
    void tc_get_005_eachNoteHasRequiredFields() throws Exception {
        JsonNode data = MAPPER.readTree(getPage("0").body()).get("data");
        for (JsonNode note : data) {
            assertThat(note.has("title")).isTrue();
            assertThat(note.has("content")).isTrue();
            assertThat(note.has("author")).isTrue();
        }
    }

    // ── TC-GET-006 ────────────────────────────────────────────────────────────
    @Test @Order(6)
    @Story("Response data integrity") @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-GET-006 | No note has a null title")
    void tc_get_006_noNullTitles() throws Exception {
        JsonNode data = MAPPER.readTree(getPage("0").body()).get("data");
        for (JsonNode note : data) {
            assertThat(note.get("title").isNull()).isFalse();
        }
    }

    // ── TC-GET-007 ────────────────────────────────────────────────────────────
    @Test @Order(7)
    @Story("Pagination") @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-GET-007 | Page 1 returns 200")
    void tc_get_007_page1_returns200() {
        assertThat(getPage("1").status()).isEqualTo(200);
    }

    // ── TC-GET-008 ────────────────────────────────────────────────────────────
    @Test @Order(8)
    @Story("Pagination – boundary") @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-GET-008 | Very high page → 200, data is empty array")
    void tc_get_008_highPage_emptyArray() throws Exception {
        APIResponse response = getPage("99999");
        assertThat(response.status()).isEqualTo(200);
        JsonNode data = MAPPER.readTree(response.body()).get("data");
        assertThat(data.isArray()).isTrue();
    }

    // ── TC-GET-009 ────────────────────────────────────────────────────────────
    @Test @Order(9)
    @Story("Negative") @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-GET-009 | Negative page -1 → 4xx")
    void tc_get_009_negativePage_expectsError() {
        assertThat(getPage("-1").status()).isBetween(400, 599);
    }

    // ── TC-GET-010 ────────────────────────────────────────────────────────────
    @Test @Order(10)
    @Story("Negative") @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-GET-010 | Non-numeric page 'abc' → 4xx")
    void tc_get_010_nonNumericPage_expectsError() {
        assertThat(getPage("abc").status()).isBetween(400, 599);
    }

    // ── TC-GET-011 ────────────────────────────────────────────────────────────
    @Test @Order(11)
    @Story("Data persistence verification") @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-GET-011 | Saved note appears in page 0 listing")
    void tc_get_011_savedNoteAppearsInListing() throws Exception {
        String uniqueTitle = "UniqueTitle_" + System.currentTimeMillis();

        String body = MAPPER.writeValueAsString(
                SaveNoteRequest.builder().title(uniqueTitle).content("Content").build());
        request.post(ApiConfig.NOTE_SAVE, RequestOptions.create().setData(body));

        JsonNode data = MAPPER.readTree(getPage("0").body()).get("data");

        boolean found = false;
        for (JsonNode note : data) {
            if (uniqueTitle.equals(note.get("title").asText())) {
                found = true;
                break;
            }
        }
        assertThat(found).as("Saved note with title '%s' should appear on page 0", uniqueTitle).isTrue();
    }

    // ── TC-GET-012 ────────────────────────────────────────────────────────────
    @Test @Order(12)
    @Story("Response headers") @Severity(SeverityLevel.MINOR)
    @DisplayName("TC-GET-012 | Response Content-Type is application/json")
    void tc_get_012_contentTypeIsJson() {
        APIResponse response = getPage("0");
        assertThat(response.headers().get("content-type"))
                .containsIgnoringCase("application/json");
    }

    // ── TC-GET-013 – Parameterized ────────────────────────────────────────────
    @ParameterizedTest(name = "Page {0} returns 200")
    @ValueSource(ints = {0, 1, 2, 5, 10})
    @Order(13)
    @Story("Pagination") @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-GET-013 | Pages 0, 1, 2, 5, 10 all return 200")
    void tc_get_013_variousPages_return200(int page) throws Exception {
        APIResponse response = getPage(String.valueOf(page));
        assertThat(response.status()).isEqualTo(200);
        assertThat(MAPPER.readTree(response.body()).get("status").asText())
                .isEqualTo("Successful");
    }
}
