package com.pixelpioneers.library.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Application Layer: the library's business rules in one place.
 * Changing a rule here (loan length, fine rate, loan limit) changes it
 * everywhere in the system. All methods are pure functions so they can be
 * unit tested without a database.
 */
public final class LibraryRules {

    /** Number of days a book may be kept before it is due back. */
    public static final int LOAN_PERIOD_DAYS = 14;

    /** Fine charged per day a book is overdue, in rand. */
    public static final double FINE_PER_DAY = 2.00;

    /** Maximum number of books one member may have out at the same time. */
    public static final int MAX_ACTIVE_LOANS = 3;

    private LibraryRules() { }

    /** The due date for a book borrowed on the given date. */
    public static LocalDate dueDateFor(LocalDate borrowDate) {
        return borrowDate.plusDays(LOAN_PERIOD_DAYS);
    }

    /** Whole days past the due date (0 if the book is on time or early). */
    public static long daysOverdue(LocalDate dueDate, LocalDate asOf) {
        long days = ChronoUnit.DAYS.between(dueDate, asOf);
        return Math.max(days, 0);
    }

    /** Fine in rand for a book due on dueDate and returned (or checked) on returnedOn. */
    public static double calculateFine(LocalDate dueDate, LocalDate returnedOn) {
        double fine = daysOverdue(dueDate, returnedOn) * FINE_PER_DAY;
        return Math.round(fine * 100.0) / 100.0;
    }
}
