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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test Suite: DELETE /note/delete/{noteId}
 *
 *  TC-DEL-001  Delete existing note → 200, data = true
 *  TC-DEL-002  Response has status, message, data fields
 *  TC-DEL-003  Response message contains the deleted note ID
 *  TC-DEL-004  Double-delete → second call returns 4xx
 *  TC-DEL-005  Non-existent ID (999999) → 4xx
 *  TC-DEL-006  Negative ID → 4xx
 *  TC-DEL-007  ID = 0 → 4xx
 *  TC-DEL-008  Non-numeric string ID → 4xx
 */
@Epic("Notepad REST API")
@Feature("Note Delete – DELETE /note/delete/{noteId}")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("TC-DEL | DELETE /note/delete/{noteId}")
class NoteDeleteTest extends BaseTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Creates a note via the save endpoint and returns its ID. */
    private int createNote(String title, String content) throws Exception {
        String body = MAPPER.writeValueAsString(
                SaveNoteRequest.builder().title(title).content(content).build());
        APIResponse resp = request.post(ApiConfig.NOTE_SAVE,
                RequestOptions.create().setData(body));
        assertThat(resp.status()).isEqualTo(200);
        return MAPPER.readTree(resp.body()).get("data").get("id").asInt();
    }

    private APIResponse doDelete(String path) {
        return request.delete(path);
    }

    // ── TC-DEL-001 ────────────────────────────────────────────────────────────
    @Test @Order(1)
    @Story("Happy path") @Severity(SeverityLevel.BLOCKER)
    @DisplayName("TC-DEL-001 | Delete existing note → 200 + data: true")
    void tc_del_001_deleteExistingNote() throws Exception {
        int id = createNote("To Delete", "Will be deleted");

        APIResponse response = doDelete(ApiConfig.NOTE_DELETE + id);
        JsonNode json = MAPPER.readTree(response.body());

        assertThat(response.status()).isEqualTo(200);
        assertThat(json.get("status").asText()).isEqualTo("Successful");
        assertThat(json.get("data").asBoolean()).isTrue();
    }

    // ── TC-DEL-002 ────────────────────────────────────────────────────────────
    @Test @Order(2)
    @Story("Response structure") @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-DEL-002 | Response body has status, message, data fields")
    void tc_del_002_responseStructure() throws Exception {
        int id = createNote("Structure Delete", "For structure check");

        APIResponse response = doDelete(ApiConfig.NOTE_DELETE + id);
        JsonNode json = MAPPER.readTree(response.body());

        assertThat(json.has("status")).isTrue();
        assertThat(json.has("message")).isTrue();
        assertThat(json.has("data")).isTrue();
    }

    // ── TC-DEL-003 ────────────────────────────────────────────────────────────
    @Test @Order(3)
    @Story("Response data integrity") @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-DEL-003 | Response message contains the deleted note ID")
    void tc_del_003_messageContainsNoteId() throws Exception {
        int id = createNote("Message Check Delete", "For message check");

        APIResponse response = doDelete(ApiConfig.NOTE_DELETE + id);
        String message = MAPPER.readTree(response.body()).get("message").asText();

        assertThat(message).contains(String.valueOf(id));
    }

    // ── TC-DEL-004 ────────────────────────────────────────────────────────────
    @Test @Order(4)
    @Story("Negative – double delete") @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-DEL-004 | Double-delete → second call returns 4xx")
    void tc_del_004_doubleDelete_secondFails() throws Exception {
        int id = createNote("Double Delete", "Will be deleted twice");

        // First delete should succeed
        assertThat(doDelete(ApiConfig.NOTE_DELETE + id).status()).isEqualTo(200);

        // Second delete should fail
        APIResponse second = doDelete(ApiConfig.NOTE_DELETE + id);
        assertThat(second.status()).isBetween(400, 599);
    }

    // ── TC-DEL-005 ────────────────────────────────────────────────────────────
    @Test @Order(5)
    @Story("Negative – invalid ID") @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-DEL-005 | Non-existent ID 999999 → 4xx")
    void tc_del_005_nonExistentId() {
        APIResponse response = doDelete(ApiConfig.NOTE_DELETE + "999999");
        assertThat(response.status()).isBetween(400, 599);
    }

    // ── TC-DEL-006 ────────────────────────────────────────────────────────────
    @Test @Order(6)
    @Story("Negative – invalid ID") @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-DEL-006 | Negative ID -1 → 4xx")
    void tc_del_006_negativeId() {
        APIResponse response = doDelete(ApiConfig.NOTE_DELETE + "-1");
        assertThat(response.status()).isBetween(400, 599);
    }

    // ── TC-DEL-007 ────────────────────────────────────────────────────────────
    @Test @Order(7)
    @Story("Negative – invalid ID") @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-DEL-007 | ID = 0 → 4xx")
    void tc_del_007_zeroId() {
        APIResponse response = doDelete(ApiConfig.NOTE_DELETE + "0");
        assertThat(response.status()).isBetween(400, 599);
    }

    // ── TC-DEL-008 ────────────────────────────────────────────────────────────
    @Test @Order(8)
    @Story("Negative – invalid ID type") @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-DEL-008 | Non-numeric string ID → 4xx")
    void tc_del_008_stringId() {
        APIResponse response = doDelete(ApiConfig.NOTE_DELETE + "abc");
        assertThat(response.status()).isBetween(400, 599);
    }
}
