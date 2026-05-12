package com.neuroval.notepad.config;

public final class ApiConfig {

    public static final String BASE_URL    = "https://notepad.neuroval.com";

    // Full absolute endpoint URLs - avoids Playwright baseURL path-joining quirks
    public static final String NOTE_SAVE      = BASE_URL + "/v1/note/save";
    public static final String NOTE_EDIT      = BASE_URL + "/v1/note/edit";
    public static final String NOTE_DELETE    = BASE_URL + "/v1/note/delete/";   // append {id}
    public static final String NOTE_GET_ALL   = BASE_URL + "/v1/note/get-all/page/"; // append {page}

    // Expected HTTP codes
    public static final int STATUS_OK         = 200;

    // Expected response values
    public static final String STATUS_SUCCESSFUL = "Successful";
    public static final String MSG_SAVED         = "Note saved!";
    public static final String MSG_UPDATED       = "Note updated!";
    public static final String MSG_ALL_RETURNED  = "All notes returned!";

    private ApiConfig() {}
}
