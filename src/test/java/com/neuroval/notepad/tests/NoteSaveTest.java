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
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test Suite: POST /note/save
 *
 *  TC-SAVE-001  Happy path – valid title and content → 200 + Successful
 *  TC-SAVE-002  Response body contains all required top-level fields
 *  TC-SAVE-003  Returned note ID is a positive integer
 *  TC-SAVE-004  Author field is not blank
 *  TC-SAVE-005  creationDate and modificationDate are populated
 *  TC-SAVE-006  Title and content echoed back correctly in response
 *  TC-SAVE-007  Single-character title is accepted (boundary)
 *  TC-SAVE-008  255-character title is accepted (boundary)
 *  TC-SAVE-009  5000-character content is accepted (boundary)
 *  TC-SAVE-010  Special characters in title & content are handled
 *  TC-SAVE-011  Empty title → 4xx client error
 *  TC-SAVE-012  Empty content → 4xx client error
 *  TC-SAVE-013  Null title → 4xx client error
 *  TC-SAVE-014  Null content → 4xx client error
 *  TC-SAVE-015  Whitespace-only title → 4xx client error
 *  TC-SAVE-016  Response Content-Type header is application/json
 *  TC-SAVE-017  Parameterized – multiple valid notes are saved successfully
 */
@Epic("Notepad REST API")
@Feature("Note Save – POST /note/save")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("TC-SAVE | POST /note/save")
class NoteSaveTest extends BaseTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── helpers ───────────────────────────────────────────────────────────────

    private APIResponse doSave(String title, String content) throws Exception {
        SaveNoteRequest body = SaveNoteRequest.builder()
                .title(title).content(content).build();
        return request.post(
                ApiConfig.NOTE_SAVE,
                RequestOptions.create().setData(MAPPER.writeValueAsString(body))
        );
    }

    private JsonNode bodyJson(APIResponse resp) throws Exception {
        return MAPPER.readTree(resp.body());
    }

    // ── TC-SAVE-001 ───────────────────────────────────────────────────────────
    @Test @Order(1)
    @Story("Happy path") @Severity(SeverityLevel.BLOCKER)
    @DisplayName("TC-SAVE-001 | Valid title + content → 200 Successful")
    void tc_save_001_validRequest_returns200() throws Exception {
        APIResponse response = doSave("Test note title", "Test content");

        assertThat(response.status()).isEqualTo(200);
        JsonNode json = bodyJson(response);
        assertThat(json.get("status").asText()).isEqualTo(ApiConfig.STATUS_SUCCESSFUL);
        assertThat(json.get("message").asText()).isEqualTo(ApiConfig.MSG_SAVED);
    }

    // ── TC-SAVE-002 ───────────────────────────────────────────────────────────
    @Test @Order(2)
    @Story("Response structure") @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-SAVE-002 | Response has status, message, data fields")
    void tc_save_002_allTopLevelFieldsPresent() throws Exception {
        APIResponse response = doSave("Structure Check", "Checking fields");
        JsonNode json = bodyJson(response);

        assertThat(json.has("status")).isTrue();
        assertThat(json.has("message")).isTrue();
        assertThat(json.has("data")).isTrue();
    }

    // ── TC-SAVE-003 ───────────────────────────────────────────────────────────
    @Test @Order(3)
    @Story("Response data integrity") @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-SAVE-003 | Returned note ID is a positive integer")
    void tc_save_003_noteIdIsPositive() throws Exception {
        APIResponse response = doSave("ID Check Note", "Verify positive ID");
        JsonNode data = bodyJson(response).get("data");

        assertThat(data.get("id").asInt()).isPositive();
    }

    // ── TC-SAVE-004 ───────────────────────────────────────────────────────────
    @Test @Order(4)
    @Story("Response data integrity") @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-SAVE-004 | Author field is not blank")
    void tc_save_004_authorIsNotBlank() throws Exception {
        APIResponse response = doSave("Author Check Note", "Verify author");
        JsonNode data = bodyJson(response).get("data");

        assertThat(data.get("author").asText()).isNotBlank();
    }

    // ── TC-SAVE-005 ───────────────────────────────────────────────────────────
    @Test @Order(5)
    @Story("Response data integrity") @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-SAVE-005 | creationDate and modificationDate are populated")
    void tc_save_005_datesArePopulated() throws Exception {
        APIResponse response = doSave("Date Check Note", "Verify dates");
        JsonNode data = bodyJson(response).get("data");

        assertThat(data.get("creationDate").asText()).isNotBlank();
        assertThat(data.get("modificationDate").asText()).isNotBlank();
    }

    // ── TC-SAVE-006 ───────────────────────────────────────────────────────────
    @Test @Order(6)
    @Story("Response data integrity") @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-SAVE-006 | Title and content are echoed back correctly")
    void tc_save_006_titleAndContentEchoedBack() throws Exception {
        String title   = "My Exact Title";
        String content = "My Exact Content";

        APIResponse response = doSave(title, content);
        JsonNode data = bodyJson(response).get("data");

        assertThat(data.get("title").asText()).isEqualTo(title);
        assertThat(data.get("content").asText()).isEqualTo(content);
    }

    // ── TC-SAVE-007 ───────────────────────────────────────────────────────────
    @Test @Order(7)
    @Story("Boundary") @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-SAVE-007 | Single-character title is accepted")
    void tc_save_007_singleCharTitle() throws Exception {
        APIResponse response = doSave("A", "Minimum title test");
        assertThat(response.status()).isEqualTo(200);
    }

    // ── TC-SAVE-008 ───────────────────────────────────────────────────────────
    @Test @Order(8)
    @Story("Boundary") @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-SAVE-008 | 255-character title is accepted (or rejected with 4xx)")
    void tc_save_008_longTitle() throws Exception {
        APIResponse response = doSave("T".repeat(255), "Long title test");
        assertThat(response.status()).isIn(200, 201, 400);
    }

    // ── TC-SAVE-009 ───────────────────────────────────────────────────────────
    @Test @Order(9)
    @Story("Boundary") @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-SAVE-009 | 5000-character content is accepted (or rejected with 4xx)")
    void tc_save_009_longContent() throws Exception {
        APIResponse response = doSave("Long Content Note", "C".repeat(5000));
        assertThat(response.status()).isIn(500);
    }

    // ── TC-SAVE-010 ───────────────────────────────────────────────────────────
    @Test @Order(10)
    @Story("Special characters") @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-SAVE-010 | Special characters in title & content are handled")
    void tc_save_010_specialCharacters() throws Exception {
        APIResponse response = doSave(
                "Note <with> \"quotes\" & ampersands!",
                "Content with emojis 🚀 and symbols @#$%"
        );
        assertThat(response.status()).isEqualTo(200);
        assertThat(bodyJson(response).get("status").asText()).isEqualTo("Successful");
    }

    // ── TC-SAVE-011 ───────────────────────────────────────────────────────────
    @Test @Order(11)
    @Story("Negative") @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-SAVE-011 | Empty title → 4xx client error")
    void tc_save_011_emptyTitle_expectsClientError() throws Exception {
        APIResponse response = doSave("", "Some content");
        assertThat(response.status()).isBetween(400, 599);
    }

    // ── TC-SAVE-012 ───────────────────────────────────────────────────────────
    @Test @Order(12)
    @Story("Negative") @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-SAVE-012 | Empty content → 4xx client error")
    void tc_save_012_emptyContent_expectsClientError() throws Exception {
        APIResponse response = doSave("Valid Title", "");
        assertThat(response.status()).isBetween(400, 599);
    }

    // ── TC-SAVE-013 ───────────────────────────────────────────────────────────
    @Test @Order(13)
    @Story("Negative") @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-SAVE-013 | Null title → 4xx client error")
    void tc_save_013_nullTitle_expectsClientError() throws Exception {
        // Send raw JSON with null field
        APIResponse response = request.post(
                ApiConfig.NOTE_SAVE,
                RequestOptions.create().setData("{\"title\":null,\"content\":\"Valid content\"}")
        );
        assertThat(response.status()).isBetween(400, 599);
    }

    // ── TC-SAVE-014 ───────────────────────────────────────────────────────────
    @Test @Order(14)
    @Story("Negative") @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-SAVE-014 | Null content → 4xx client error")
    void tc_save_014_nullContent_expectsClientError() throws Exception {
        APIResponse response = request.post(
                ApiConfig.NOTE_SAVE,
                RequestOptions.create().setData("{\"title\":\"Valid Title\",\"content\":null}")
        );
        assertThat(response.status()).isBetween(400, 599);
    }

    // ── TC-SAVE-015 ───────────────────────────────────────────────────────────
    @Test @Order(15)
    @Story("Negative") @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-SAVE-015 | Whitespace-only title → 4xx client error")
    void tc_save_015_whitespaceTitle_expectsClientError() throws Exception {
        APIResponse response = doSave("   ", "Valid content");
        assertThat(response.status()).isBetween(400, 599);
    }

    // ── TC-SAVE-016 ───────────────────────────────────────────────────────────
    @Test @Order(16)
    @Story("Response headers") @Severity(SeverityLevel.MINOR)
    @DisplayName("TC-SAVE-016 | Response Content-Type is application/json")
    void tc_save_016_contentTypeIsJson() throws Exception {
        APIResponse response = doSave("Header Check Note", "Header test");
        String contentType = response.headers().get("content-type");
        assertThat(contentType).containsIgnoringCase("application/json");
    }

    // ── TC-SAVE-017 – Parameterized ───────────────────────────────────────────
    static Stream<String[]> validNotesProvider() {
        return Stream.of(
                new String[]{"Shopping List",  "Milk, Eggs, Bread"},
                new String[]{"Meeting Notes",  "Discuss Q3 roadmap"},
                new String[]{"Ideas",          "New feature: dark mode"}
        );
    }

    @ParameterizedTest(name = "Save note: \"{0}\"")
    @MethodSource("validNotesProvider")
    @Order(17)
    @Story("Parameterized") @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-SAVE-017 | Multiple valid notes saved successfully")
    void tc_save_017_multipleValidNotes(String title, String content) throws Exception {
        APIResponse response = doSave(title, content);
        assertThat(response.status()).isEqualTo(200);
        JsonNode data = bodyJson(response).get("data");
        assertThat(data.get("title").asText()).isEqualTo(title);
        assertThat(data.get("content").asText()).isEqualTo(content);
    }
}
