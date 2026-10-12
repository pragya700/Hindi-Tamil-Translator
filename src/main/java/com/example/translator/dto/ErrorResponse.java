package com.example.translator.dto;

/** JSON sent back when a request is invalid: {"message": "..."} */
public record ErrorResponse(String message) {
}
