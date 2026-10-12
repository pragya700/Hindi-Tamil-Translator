package com.example.translator.service;

import com.example.translator.dto.TranslationResponse;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * BASIC DEMONSTRATION translator between Hindi (hi) and Tamil (ta), in both directions.
 *
 * IMPORTANT: This is NOT machine translation. It looks words and short phrases up in
 * dictionary files:
 *   hi -> ta : src/main/resources/dictionary/hi_ta_dictionary.txt
 *   ta -> hi : src/main/resources/dictionary/ta_hi_dictionary.txt
 * The two files are written separately (Hindi and Tamil do not map one-to-one).
 * It does not understand grammar, so it only translates the vocabulary and phrases that
 * are in the files. Words that are not in the dictionary are left unchanged and reported.
 * It works completely offline.
 */
@Service
public class TranslationService {

    private static final Logger log = LoggerFactory.getLogger(TranslationService.class);

    public static final String HINDI = "hi";
    public static final String TAMIL = "ta";

    private static final int MAX_PHRASE_WORDS = 4;
    private static final String MODE = "DICTIONARY_DEMO";
    private static final String NOTE =
            "Basic dictionary demo - not a full machine translation system. "
            + "It only knows the vocabulary and phrases in its dictionary.";

    // A "word" = letters, combining marks (Hindi/Tamil vowel signs) and digits.
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{M}\\p{N}]+");

    // Which dictionary file belongs to which direction. Key format: "source-target".
    private static final Map<String, String> DICTIONARY_FILES = Map.of(
            "hi-ta", "dictionary/hi_ta_dictionary.txt",
            "ta-hi", "dictionary/ta_hi_dictionary.txt");

    // direction ("hi-ta") -> (source word/phrase -> target text). Loaded once at startup.
    private final Map<String, Map<String, String>> dictionaries = new HashMap<>();

    /** Runs once when the application starts. A broken file disables only that direction. */
    @PostConstruct
    public void loadDictionaries() {
        dictionaries.clear();
        for (Map.Entry<String, String> entry : DICTIONARY_FILES.entrySet()) {
            Map<String, String> loaded = readDictionary(entry.getValue());
            if (loaded.isEmpty()) {
                log.error("Dictionary {} is missing or empty. Direction {} is unavailable.",
                        entry.getValue(), entry.getKey());
            } else {
                dictionaries.put(entry.getKey(), loaded);
                log.info("Loaded {} entries for {} from {}",
                        loaded.size(), entry.getKey(), entry.getValue());
            }
        }
    }

    /** Reads one dictionary file. Returns an empty map if the file cannot be read. */
    private Map<String, String> readDictionary(String path) {
        Map<String, String> result = new HashMap<>();
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            return result;
        }
        int lineNumber = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.replace("\uFEFF", "").trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // skip blank lines and comments
                }
                int equalsIndex = line.indexOf('=');
                if (equalsIndex <= 0 || equalsIndex == line.length() - 1) {
                    log.warn("{}: skipping malformed line {}", path, lineNumber);
                    continue;
                }
                String from = line.substring(0, equalsIndex).trim();
                String to = line.substring(equalsIndex + 1).trim();
                result.put(normalizeKey(from), to);
            }
        } catch (IOException e) {
            log.error("Could not read dictionary {}: {}", path, e.getMessage());
            return new HashMap<>();
        }
        return result;
    }

    /** True for the supported language codes ("hi" and "ta"). */
    public static boolean isSupportedLanguage(String code) {
        return HINDI.equals(code) || TAMIL.equals(code);
    }

    /** Number of dictionary entries loaded for a direction (0 if it is unavailable). */
    public int getDictionarySize(String sourceLanguage, String targetLanguage) {
        Map<String, String> dictionary = dictionaries.get(sourceLanguage + "-" + targetLanguage);
        return dictionary == null ? 0 : dictionary.size();
    }

    /**
     * Translates text from sourceLanguage to targetLanguage.
     *
     * @throws UnsupportedLanguagePairException if a code is not "hi"/"ta" or both are equal
     * @throws DictionaryUnavailableException   if that direction's dictionary failed to load
     * @throws IllegalArgumentException         if the text is null or blank
     */
    public TranslationResponse translate(String text, String sourceLanguage, String targetLanguage) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Please enter some text to translate.");
        }
        String source = normalizeCode(sourceLanguage);
        String target = normalizeCode(targetLanguage);

        if (!isSupportedLanguage(source) || !isSupportedLanguage(target)) {
            throw new UnsupportedLanguagePairException(
                    "Unsupported language. Only Hindi (hi) and Tamil (ta) are supported.");
        }
        if (source.equals(target)) {
            throw new UnsupportedLanguagePairException(
                    "Source and target language must be different.");
        }

        Map<String, String> dictionary = dictionaries.get(source + "-" + target);
        if (dictionary == null) {
            throw new DictionaryUnavailableException(
                    "The " + languageName(source) + " to " + languageName(target)
                    + " dictionary is not available right now.");
        }

        return translateWithDictionary(text, source, target, dictionary);
    }

    // ---------------------------------------------------------------------------------
    // Everything below is the dictionary-lookup algorithm (same for both directions).
    // ---------------------------------------------------------------------------------

    private TranslationResponse translateWithDictionary(String text, String source, String target,
                                                        Map<String, String> dictionary) {
        String input = Normalizer.normalize(text, Normalizer.Form.NFC);
        if (HINDI.equals(source)) {
            // Hindi full stop (danda) becomes a normal full stop.
            input = input.replace('\u0964', '.').replace('\u0965', '.');
        }

        // Step 1: find the start and end position of every word.
        List<int[]> words = new ArrayList<>();
        Matcher matcher = WORD.matcher(input);
        while (matcher.find()) {
            words.add(new int[] {matcher.start(), matcher.end()});
        }

        StringBuilder output = new StringBuilder();
        Set<String> unknownWords = new LinkedHashSet<>();
        int translatedWords = 0;
        int lastEnd = 0; // everything before this position is already in the output
        int i = 0;

        // Step 2: go through the words. Try the longest phrase first, then shorter ones.
        while (i < words.size()) {
            int start = words.get(i)[0];
            output.append(input, lastEnd, start); // copy spaces, punctuation, line breaks

            int maxWords = Math.min(MAX_PHRASE_WORDS, words.size() - i);
            String found = null;
            int matchedWords = 0;

            for (int n = maxWords; n >= 1; n--) {
                if (!separatedOnlyBySpaces(input, words, i, n)) {
                    continue; // a comma or line break sits inside this group
                }
                int end = words.get(i + n - 1)[1];
                String candidate = dictionary.get(normalizeKey(input.substring(start, end)));
                if (candidate != null) {
                    found = candidate;
                    matchedWords = n;
                    break;
                }
            }

            if (found != null) {
                output.append(found);
                translatedWords += matchedWords;
                lastEnd = words.get(i + matchedWords - 1)[1];
                i += matchedWords;
            } else {
                // Unknown word: keep it as it is, so nothing is lost or invented.
                int end = words.get(i)[1];
                String word = input.substring(start, end);
                output.append(word);
                if (!isNumber(word)) {
                    unknownWords.add(word);
                }
                lastEnd = end;
                i++;
            }
        }
        output.append(input.substring(lastEnd)); // trailing punctuation / spaces

        return new TranslationResponse(output.toString(), source, target,
                translatedWords, new ArrayList<>(unknownWords), MODE, NOTE);
    }

    /** True if only spaces/tabs separate the n words starting at index 'first'. */
    private boolean separatedOnlyBySpaces(String input, List<int[]> words, int first, int n) {
        for (int k = first; k < first + n - 1; k++) {
            String between = input.substring(words.get(k)[1], words.get(k + 1)[0]);
            if (!between.matches("[ \\t]+")) {
                return false;
            }
        }
        return true;
    }

    private boolean isNumber(String word) {
        return word.chars().allMatch(Character::isDigit);
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toLowerCase();
    }

    private String languageName(String code) {
        return HINDI.equals(code) ? "Hindi" : "Tamil";
    }

    /**
     * Makes dictionary keys and user input comparable:
     * - same Unicode form (NFC)
     * - chandrabindu (ँ) treated like anusvara (ं), e.g. हूँ = हूं (only affects Hindi)
     * - several spaces become one space
     */
    private String normalizeKey(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC);
        normalized = normalized.replace('\u0901', '\u0902');
        return normalized.replaceAll("\\s+", " ").trim();
    }
}
