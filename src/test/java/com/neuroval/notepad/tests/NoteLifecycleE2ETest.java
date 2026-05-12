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
 * End-to-End Integration Tests – full CRUD lifecycle
 *
 *  TC-E2E-001  Create → Edit → Delete (full happy path)
 *  TC-E2E-002  Create 3 notes → all appear in get-all page 0
 *  TC-E2E-003  Create → Delete → second delete on same ID fails
 *  TC-E2E-004  Create → Edit → edited content visible in listing
 */
@Epic("Notepad REST API")
@Feature("End-to-End Lifecycle")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("TC-E2E | Full CRUD Lifecycle")
class NoteLifecycleE2ETest extends BaseTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private int saveNote(String title, String content) throws Exception {
        String body = MAPPER.writeValueAsString(
                SaveNoteRequest.builder().title(title).content(content).build());
        APIResponse resp = request.post(ApiConfig.NOTE_SAVE,
                RequestOptions.create().setData(body));
        assertThat(resp.status()).isEqualTo(200);
        return MAPPER.readTree(resp.body()).get("data").get("id").asInt();
    }

    private APIResponse editNote(int id, String content) throws Exception {
        String body = MAPPER.writeValueAsString(
                EditNoteRequest.builder().id(id).content(content).build());
        return request.put(ApiConfig.NOTE_EDIT, RequestOptions.create().setData(body));
    }

    private APIResponse deleteNote(int id) {
        return request.delete(ApiConfig.NOTE_DELETE + id);
    }

    private JsonNode getAllPage0() throws Exception {
        return MAPPER.readTree(request.get(ApiConfig.NOTE_GET_ALL + "0").body()).get("data");
    }

    // ── TC-E2E-001 ────────────────────────────────────────────────────────────
    @Test @Order(1)
    @Story("Full CRUD") @Severity(SeverityLevel.BLOCKER)
    @DisplayName("TC-E2E-001 | Create → Edit → Delete: full happy path")
    void tc_e2e_001_fullCrudLifecycle() throws Exception {
        // 1. Create
        int id = saveNote("E2E Lifecycle Note", "Original content");
        assertThat(id).isPositive();

        // 2. Edit
        APIResponse editResp = editNote(id, "Updated content");
        assertThat(editResp.status()).isEqualTo(200);
        assertThat(MAPPER.readTree(editResp.body()).get("message").asText())
                .isEqualTo("Note updated!");

        // 3. Verify in listing
        boolean found = false;
        for (JsonNode note : getAllPage0()) {
            if ("E2E Lifecycle Note".equals(note.get("title").asText())) { found = true; break; }
        }
        assertThat(found).isTrue();

        // 4. Delete
        APIResponse delResp = deleteNote(id);
        assertThat(delResp.status()).isEqualTo(200);
        assertThat(MAPPER.readTree(delResp.body()).get("data").asBoolean()).isTrue();
    }

    // ── TC-E2E-002 ────────────────────────────────────────────────────────────
    @Test @Order(2)
    @Story("Multiple notes") @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-E2E-002 | Create 3 notes → all appear in page 0 listing")
    void tc_e2e_002_createMultipleNotes_allInListing() throws Exception {
        long ts = System.currentTimeMillis();
        String[] titles = {
                "E2E Note A " + ts,
                "E2E Note B " + ts,
                "E2E Note C " + ts
        };

        for (String title : titles) saveNote(title, "Content for " + title);

        JsonNode data = getAllPage0();
        for (String title : titles) {
            boolean found = false;
            for (JsonNode note : data) {
                if (title.equals(note.get("title").asText())) { found = true; break; }
            }
            assertThat(found).as("Note '%s' should be in listing", title).isTrue();
        }
    }

    // ── TC-E2E-003 ────────────────────────────────────────────────────────────
    @Test @Order(3)
    @Story("Create then delete") @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-E2E-003 | Create → Delete → second delete on same ID fails")
    void tc_e2e_003_deletedNoteCannotBeDeletedAgain() throws Exception {
        int id = saveNote("To Be Deleted " + System.currentTimeMillis(), "Content");

        assertThat(deleteNote(id).status()).isEqualTo(200);
        assertThat(deleteNote(id).status()).isBetween(400, 599);
    }

    // ── TC-E2E-004 ────────────────────────────────────────────────────────────
    @Test @Order(4)
    @Story("Edit and verify") @Severity(SeverityLevel.CRITICAL)
    @DisplayName("TC-E2E-004 | Create → Edit → edited content visible in listing")
    void tc_e2e_004_editedContentVisibleInListing() throws Exception {
        long ts = System.currentTimeMillis();
        String title          = "Edit Verify E2E " + ts;
        String updatedContent = "UPDATED content " + ts;

        int id = saveNote(title, "Original content " + ts);
        assertThat(editNote(id, updatedContent).status()).isEqualTo(200);

        boolean contentFound = false;
        for (JsonNode note : getAllPage0()) {
            if (updatedContent.equals(note.get("content").asText())) { contentFound = true; break; }
        }
        assertThat(contentFound)
                .as("Updated content '%s' should appear in listing", updatedContent)
                .isTrue();
    }
}
