package com.example.translator.service;

/** Thrown when the dictionary for the requested direction could not be loaded. */
public class DictionaryUnavailableException extends RuntimeException {

    public DictionaryUnavailableException(String message) {
        super(message);
    }
}
