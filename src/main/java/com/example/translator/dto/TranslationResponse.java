package com.example.translator.dto;

import java.util.List;

/**
 * JSON sent back to the browser.
 *
 * translation     - the translated text
 * sourceLanguage  - language code of the input ("hi" or "ta")
 * targetLanguage  - language code of the output ("hi" or "ta")
 * translatedWords - how many input words were found in the dictionary
 * unknownWords    - input words that were NOT in the dictionary (left unchanged)
 * mode            - always "DICTIONARY_DEMO" so the UI can be honest about it
 * note            - a short explanation shown to the user
 */
public record TranslationResponse(
        String translation,
        String sourceLanguage,
        String targetLanguage,
        int translatedWords,
        List<String> unknownWords,
        String mode,
        String note) {
}
