package com.example.translator.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.translator.dto.TranslationResponse;
import com.example.translator.service.DictionaryUnavailableException;
import com.example.translator.service.TranslationService;
import com.example.translator.service.UserService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Tests the /api/translate endpoint with fake services (no MySQL needed). */
@WebMvcTest(TranslatorController.class)
@WithMockUser
class TranslatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TranslationService translationService;

    @MockitoBean
    private UserService userService;

    private ResultActions postJson(String json) throws Exception {
        return mockMvc.perform(post("/api/translate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void validRequestReturnsTranslation() throws Exception {
        when(translationService.translate(anyString(), anyString(), anyString()))
                .thenReturn(new TranslationResponse("translated text", "hi", "ta", 1,
                        List.of(), "DICTIONARY_DEMO", "note"));

        postJson("{\"text\":\"hello\",\"sourceLanguage\":\"hi\",\"targetLanguage\":\"ta\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translation").value("translated text"))
                .andExpect(jsonPath("$.sourceLanguage").value("hi"))
                .andExpect(jsonPath("$.targetLanguage").value("ta"));
    }

    @Test
    void blankTextIsRejected() throws Exception {
        postJson("{\"text\":\"   \",\"sourceLanguage\":\"hi\",\"targetLanguage\":\"ta\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void unsupportedLanguageIsRejected() throws Exception {
        postJson("{\"text\":\"hello\",\"sourceLanguage\":\"en\",\"targetLanguage\":\"ta\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void sameLanguageIsRejected() throws Exception {
        postJson("{\"text\":\"hello\",\"sourceLanguage\":\"ta\",\"targetLanguage\":\"ta\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingLanguageIsRejected() throws Exception {
        postJson("{\"text\":\"hello\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void missingDictionaryReturns503() throws Exception {
        when(translationService.translate(anyString(), anyString(), anyString()))
                .thenThrow(new DictionaryUnavailableException("dictionary is not available right now."));

        postJson("{\"text\":\"hello\",\"sourceLanguage\":\"ta\",\"targetLanguage\":\"hi\"}")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").exists());
    }
}
