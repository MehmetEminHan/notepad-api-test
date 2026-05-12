package com.neuroval.notepad.config;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.HashMap;
import java.util.Map;

/**
 * Base test class.
 * Uses absolute URLs in all requests to avoid Playwright's baseURL
 * path-joining behavior that strips path segments like /v1.
 */
public abstract class BaseTest {

    protected static Playwright        playwright;
    protected static APIRequestContext request;

    @BeforeAll
    static void createPlaywright() {
        playwright = Playwright.create();

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept",       "application/json");

        // No baseURL set — all test methods use full absolute URLs from ApiConfig
        request = playwright.request().newContext(
                new APIRequest.NewContextOptions()
                        .setExtraHTTPHeaders(headers)
        );
    }

    @AfterAll
    static void closePlaywright() {
        if (request    != null) request.dispose();
        if (playwright != null) playwright.close();
    }
}
