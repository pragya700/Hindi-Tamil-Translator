package com.example.translator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.translator.dto.TranslationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Plain unit tests: no Spring, no database, no internet needed. */
class TranslationServiceTest {

    private TranslationService service;

    @BeforeEach
    void setUp() {
        service = new TranslationService();
        service.loadDictionaries();
    }

    private TranslationResponse hiToTa(String text) {
        return service.translate(text, "hi", "ta");
    }

    private TranslationResponse taToHi(String text) {
        return service.translate(text, "ta", "hi");
    }

    // ---------------- dictionaries ----------------

    @Test
    void bothDictionariesAreLoaded() {
        assertTrue(service.getDictionarySize("hi", "ta") > 100);
        assertTrue(service.getDictionarySize("ta", "hi") > 100);
    }

    // ---------------- Hindi -> Tamil (existing behaviour) ----------------

    @Test
    void hiToTa_translatesSingleWord() {
        assertEquals("வணக்கம்", hiToTa("नमस्ते").translation());
    }

    @Test
    void hiToTa_translatesPhraseAndKeepsPunctuation() {
        TranslationResponse result = hiToTa("नमस्ते, आप कैसे हैं?");
        assertEquals("வணக்கம், நீங்கள் எப்படி இருக்கிறீர்கள்?", result.translation());
        assertTrue(result.unknownWords().isEmpty());
        assertEquals("hi", result.sourceLanguage());
        assertEquals("ta", result.targetLanguage());
    }

    @Test
    void hiToTa_handlesChandrabinduAndDanda() {
        assertEquals("நான் தமிழ் கற்றுக்கொள்கிறேன்.", hiToTa("मैं तमिल सीख रहा हूँ।").translation());
    }

    @Test
    void hiToTa_newCollegePhrase() {
        assertEquals("நான் பள்ளிக்கு போகிறேன்.", hiToTa("मैं स्कूल जा रहा हूँ।").translation());
    }

    @Test
    void hiToTa_unknownWordsAreKeptAndReported() {
        TranslationResponse result = hiToTa("नमस्ते राहुल");
        assertEquals("வணக்கம் राहुल", result.translation());
        assertEquals(1, result.unknownWords().size());
        assertEquals("राहुल", result.unknownWords().get(0));
    }

    @Test
    void hiToTa_preservesLineBreaks() {
        assertEquals("வணக்கம்\nநன்றி", hiToTa("नमस्ते\nधन्यवाद").translation());
    }

    // ---------------- Tamil -> Hindi (new) ----------------

    @Test
    void taToHi_translatesSingleWord() {
        assertEquals("नमस्ते", taToHi("வணக்கம்").translation());
    }

    @Test
    void taToHi_translatesPhraseAndKeepsPunctuation() {
        TranslationResponse result = taToHi("வணக்கம், நீங்கள் எப்படி இருக்கிறீர்கள்?");
        assertEquals("नमस्ते, आप कैसे हैं?", result.translation());
        assertTrue(result.unknownWords().isEmpty());
        assertEquals("ta", result.sourceLanguage());
        assertEquals("hi", result.targetLanguage());
    }

    @Test
    void taToHi_translatesVerbPhrase() {
        assertEquals("मैं तमिल सीख रहा हूँ.", taToHi("நான் தமிழ் கற்றுக்கொள்கிறேன்.").translation());
    }

    @Test
    void taToHi_handlesInflectedPlaceName() {
        assertEquals("मैं स्कूल जा रहा हूँ", taToHi("நான் பள்ளிக்கு போகிறேன்").translation());
    }

    @Test
    void taToHi_unknownWordsAreKeptAndReported() {
        TranslationResponse result = taToHi("வணக்கம் ராகுல்");
        assertEquals("नमस्ते ராகுல்", result.translation());
        assertEquals(1, result.unknownWords().size());
        assertEquals("ராகுல்", result.unknownWords().get(0));
    }

    @Test
    void taToHi_preservesLineBreaks() {
        assertEquals("नमस्ते\nधन्यवाद", taToHi("வணக்கம்\nநன்றி").translation());
    }

    // ---------------- validation and errors ----------------

    @Test
    void doesNotCrashOnEnglishOrSymbols() {
        assertEquals("Hello 123 !!!", hiToTa("Hello 123 !!!").translation());
        assertEquals("Hello 123 !!!", taToHi("Hello 123 !!!").translation());
    }

    @Test
    void languageCodesAreCaseInsensitive() {
        assertEquals("வணக்கம்", service.translate("नमस्ते", " HI ", "Ta").translation());
    }

    @Test
    void unsupportedLanguageIsRejected() {
        assertThrows(UnsupportedLanguagePairException.class,
                () -> service.translate("hello", "en", "ta"));
        assertThrows(UnsupportedLanguagePairException.class,
                () -> service.translate("नमस्ते", "hi", "fr"));
        assertThrows(UnsupportedLanguagePairException.class,
                () -> service.translate("नमस्ते", null, "ta"));
    }

    @Test
    void sameSourceAndTargetIsRejected() {
        assertThrows(UnsupportedLanguagePairException.class,
                () -> service.translate("नमस्ते", "hi", "hi"));
    }

    @Test
    void blankTextIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.translate("   ", "hi", "ta"));
        assertThrows(IllegalArgumentException.class, () -> service.translate(null, "hi", "ta"));
    }

    @Test
    void missingDictionaryGivesMeaningfulError() {
        TranslationService notLoaded = new TranslationService(); // loadDictionaries() NOT called
        DictionaryUnavailableException ex = assertThrows(DictionaryUnavailableException.class,
                () -> notLoaded.translate("नमस्ते", "hi", "ta"));
        assertTrue(ex.getMessage().contains("not available"));
    }
}
