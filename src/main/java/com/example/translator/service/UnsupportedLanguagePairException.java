package com.example.translator.service;

/** Thrown when the chosen source/target language is not supported. */
public class UnsupportedLanguagePairException extends IllegalArgumentException {

    public UnsupportedLanguagePairException(String message) {
        super(message);
    }
}
