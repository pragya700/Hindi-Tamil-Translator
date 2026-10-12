package com.example.translator.dto;

/**
 * JSON sent by the browser, for example:
 * {"text": "नमस्ते", "sourceLanguage": "hi", "targetLanguage": "ta"}
 * Language codes: "hi" = Hindi, "ta" = Tamil.
 */
public record TranslationRequest(String text, String sourceLanguage, String targetLanguage) {
}
