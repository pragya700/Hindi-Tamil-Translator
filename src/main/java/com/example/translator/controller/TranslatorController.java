package com.example.translator.controller;

import com.example.translator.dto.ErrorResponse;
import com.example.translator.dto.TranslationRequest;
import com.example.translator.model.User;
import com.example.translator.service.DictionaryUnavailableException;
import com.example.translator.service.TranslationService;
import com.example.translator.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * The translator page (HTML) and the translation API (JSON).
 * The controller only checks the HTTP request; the real work is done in TranslationService.
 */
@Controller
public class TranslatorController {

    private static final int MAX_TEXT_LENGTH = 5000;

    private final TranslationService translationService;
    private final UserService userService;

    public TranslatorController(TranslationService translationService, UserService userService) {
        this.translationService = translationService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home(Authentication authentication, Model model) {
        // authentication.getName() is the email of the logged-in user
        String userName = userService.findByEmail(authentication.getName())
                .map(User::getName)
                .orElse("User");
        model.addAttribute("userName", userName);
        return "home"; // templates/home.html
    }

    @PostMapping("/api/translate")
    @ResponseBody
    public ResponseEntity<?> translate(@RequestBody TranslationRequest request) {
        // 1. Validate the text
        if (request == null || request.text() == null || request.text().isBlank()) {
            return badRequest("Please enter some text to translate.");
        }
        if (request.text().length() > MAX_TEXT_LENGTH) {
            return badRequest("Text is too long. Maximum is " + MAX_TEXT_LENGTH + " characters.");
        }

        // 2. Validate the languages
        String source = clean(request.sourceLanguage());
        String target = clean(request.targetLanguage());
        if (source.isEmpty() || target.isEmpty()) {
            return badRequest("Please select both the source and the target language.");
        }
        if (!TranslationService.isSupportedLanguage(source)
                || !TranslationService.isSupportedLanguage(target)) {
            return badRequest("Unsupported language. Only Hindi (hi) and Tamil (ta) are supported.");
        }
        if (source.equals(target)) {
            return badRequest("Source and target language must be different.");
        }

        // 3. Translate
        try {
            return ResponseEntity.ok(translationService.translate(request.text(), source, target));
        } catch (IllegalArgumentException e) {
            // includes UnsupportedLanguagePairException
            return badRequest(e.getMessage());
        } catch (DictionaryUnavailableException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    private ResponseEntity<ErrorResponse> badRequest(String message) {
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }

    private String clean(String code) {
        return code == null ? "" : code.trim().toLowerCase();
    }
}
