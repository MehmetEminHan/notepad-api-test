package com.neuroval.notepad.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import com.neuroval.notepad.config.ApiConfig;
import com.neuroval.notepad.config.BaseTest;
import com.neuroval.notepad.models.EditNoteRequest;
import com.neuroval.notepad.models.SaveNoteRequest;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test Suite: PUT /note/edit
 *
 *  TC-EDIT-001  Valid ID + content → 200 + "Note updated!"
 *  TC-EDIT-002  Response has status, message, data fields
 *  TC-EDIT-003  status = "Successful", message = "Note updated!"
 *  TC-EDIT-004  Empty content → 4xx
 *  TC-EDIT-005  Null content → 4xx
 *  TC-EDIT-006  Non-existent ID → 4xx
 *  TC-EDIT-007  Null ID → 4xx
 *  TC-EDIT-008  Negative ID → 4xx
 *  TC-EDIT-009  Special characters in new content are handled
 *  TC-EDIT-010  5000-char content boundary
 *  TC-EDIT-011  Double-edit the same note – both succeed
 */
@Epic("Notepad REST API")
@Feature("Note Edit – PUT /note/edit")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("TC-EDIT | PUT /note/edit")
class NoteEditTest extends BaseTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private int createNote(String title, String content) throws Exception {
        String body = MAPPER.writeValueAsString(
                SaveNoteRequest.builder().title(title).content(content).build());
        APIResponse resp = request.post(ApiConfig.NOTE_SAVE,
                RequestOptions.create().setData(body));
        return MAPPER.readTree(resp.body()).get("data").get("id").asInt();
    }

    private APIResponse doEdit(Integer id, String content) throws Exception {
        String body = MAPPER.writeValueAsString(
                EditNoteRequest.builder().id(id).content(content).build());
        return request.put(ApiConfig.NOTE_EDIT, RequestOptions.create().setData(body));
    }

    // ── TC-EDIT-001 ───────────────────────────────────────────────────────────
    @Test @Order(1)
    @Story("Happy path") @Severity(SeverityLevel.BLOCKER)
    @DisplayName("TC-EDIT-001 | Valid ID + content → 200 Successful")
    void tc_edit_001_validEdit_returns200() throws Exception {
        int id = createNote("Editable Note", "Original content");

        APIResponse response = doEdit(id, "Updated content");
        JsonNode json = MAPPER.readTree(response.body());

        assertThat(response.status()).isEqualTo(200);
        assertThat(json.get("status").asText()).isEqualTo("Successful");
        assertThat(json.get("message").asText()).isEqualTo("Note updated!");
    }

    // ── TC-EDIT-002 ───────────────────────────────────────────────────────────
    @Test @Order(2)
    @Story("Response structure") @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-EDIT-002 | Response has status, message, data fields")
    void tc_edit_002_responseStructure() throws Exception {
        int id = createNote("Structure Edit Note", "Original");
        APIResponse response = doEdit(id, "Checking structure");
        JsonNode json = MAPPER.readTree(response.body());

        assertThat(json.has("status")).isTrue();
        assertThat(json.has("message")).isTrue();
        assertThat(json.has("data")).isTrue();
    }

    // ── TC-EDIT-003 ───────────────────────────────────────────────────────────
    @Test @Order(3)
    @Story("Response data integrity") @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-EDIT-003 | status=Successful, message=Note updated!")
    void tc_edit_003_correctStatusAndMessage() throws Exception {
        int id = createNote("Message Check", "Original");
        APIResponse response = doEdit(id, "New content");
        JsonNode json = MAPPER.readTree(response.body());

        assertThat(json.get("status").asText()).isEqualTo("Successful");
        assertThat(json.get("message").asText()).isEqualTo("Note updated!");
    }

    // ── TC-EDIT-004 ───────────────────────────────────────────────────────────
    @Test @Order(4)
    @Story("Negative") @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-EDIT-004 | Empty content → 4xx")
    void tc_edit_004_emptyContent_expectsError() throws Exception {
        int id = createNote("Empty Content Edit", "Original");
        APIResponse response = doEdit(id, "");
        assertThat(response.status()).isBetween(400, 599);
    }

    // ── TC-EDIT-005 ───────────────────────────────────────────────────────────
    @Test @Order(5)
    @Story("Negative") @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-EDIT-005 | Null content → 4xx")
    void tc_edit_005_nullContent_expectsError() throws Exception {
        int id = createNote("Null Content Edit", "Original");
        // Serialize null content explicitly
        String body = "{\"id\":" + id + ",\"content\":null}";
        APIResponse response = request.put(ApiConfig.NOTE_EDIT,
                RequestOptions.create().setData(body));
        assertThat(response.status()).isBetween(400, 599);
    }

    // ── TC-EDIT-006 ───────────────────────────────────────────────────────────
    @Test @Order(6)
    @Story("Negative") @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-EDIT-006 | Non-existent note ID → 4xx")
    void tc_edit_006_nonExistentId_expectsError() throws Exception {
        APIResponse response = doEdit(999999, "Ghost content");
        assertThat(response.status()).isBetween(400, 599);
    }

    // ── TC-EDIT-007 ───────────────────────────────────────────────────────────
    @Test @Order(7)
    @Story("Negative") @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-EDIT-007 | Null ID → 4xx")
    void tc_edit_007_nullId_expectsError() throws Exception {
        String body = "{\"id\":null,\"content\":\"Some content\"}";
        APIResponse response = request.put(ApiConfig.NOTE_EDIT,
                RequestOptions.create().setData(body));
        assertThat(response.status()).isBetween(400, 599);
    }

    // ── TC-EDIT-008 ───────────────────────────────────────────────────────────
    @Test @Order(8)
    @Story("Negative") @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-EDIT-008 | Negative ID → 4xx")
    void tc_edit_008_negativeId_expectsError() throws Exception {
        APIResponse response = doEdit(-5, "Negative ID test");
        assertThat(response.status()).isBetween(400, 599);
    }

    // ── TC-EDIT-009 ───────────────────────────────────────────────────────────
    @Test @Order(9)
    @Story("Special characters") @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-EDIT-009 | Special characters in content are handled")
    void tc_edit_009_specialCharacters() throws Exception {
        int id = createNote("Special Chars Edit", "Original");
        APIResponse response = doEdit(id, "Updated: <script>alert('xss')</script> 🎉 @#$");
        assertThat(response.status()).isEqualTo(200);
    }

    // ── TC-EDIT-010 ───────────────────────────────────────────────────────────
    @Test @Order(10)
    @Story("Boundary") @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-EDIT-010 | 5000-char content boundary")
    void tc_edit_010_veryLongContent() throws Exception {
        int id = createNote("Long Content Edit", "Original");
        APIResponse response = doEdit(id, "X".repeat(5000));
        assertThat(response.status()).isIn(200, 400);
    }

    // ── TC-EDIT-011 ───────────────────────────────────────────────────────────
    @Test @Order(11)
    @Story("Double edit") @Severity(SeverityLevel.NORMAL)
    @DisplayName("TC-EDIT-011 | Double-edit the same note – both succeed")
    void tc_edit_011_doubleEdit_bothSucceed() throws Exception {
        int id = createNote("Double Edit Note", "Original");

        assertThat(doEdit(id, "First edit").status()).isEqualTo(200);
        assertThat(doEdit(id, "Second edit").status()).isEqualTo(200);
    }
}
