package com.pixelpioneers.library.service;

/**
 * Thrown when a request is valid input but breaks a library business rule
 * (for example: borrowing a book that is already out, or paying a fine twice).
 * Keeping this separate from SQLException lets the UI show a friendly
 * explanation instead of a database error.
 */
public class BusinessRuleException extends Exception {

    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String message) {
        super(message);
    }
}
