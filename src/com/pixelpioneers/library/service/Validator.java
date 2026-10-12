package com.pixelpioneers.library.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Application Layer: input validation.
 * Each validateXxx method returns a list of plain-English problems, one per
 * field that is missing or invalid. An empty list means the input is valid.
 * Returning every problem at once (instead of stopping at the first) is the
 * "detailed error messages" improvement from the Phase 3 design iteration.
 */
public final class Validator {

    // Limits match the column sizes in the Phase 3 ERD.
    public static final int TITLE_MAX = 100;
    public static final int AUTHOR_MAX = 100;
    public static final int CATEGORY_MAX = 50;
    public static final int NAME_MAX = 100;
    public static final int EMAIL_MAX = 100;
    public static final int USERNAME_MIN = 3;
    public static final int USERNAME_MAX = 30;
    public static final int PASSWORD_MIN = 6;
    public static final int PASSWORD_MAX = 64;

    private static final Pattern NAME = Pattern.compile("^[\\p{L}][\\p{L} .'\\-]{1,99}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._@\\-]+$");
    private static final Pattern ISBN10 = Pattern.compile("^\\d{9}[\\dX]$");
    private static final Pattern ISBN13 = Pattern.compile("^\\d{13}$");
    private static final Pattern PHONE_LOCAL = Pattern.compile("^0\\d{9}$");
    private static final Pattern PHONE_INTL = Pattern.compile("^\\+27\\d{9}$");

    private Validator() { }

    // ---------- Books ----------

    public static List<String> validateBook(String title, String author, String isbn, String category) {
        List<String> problems = new ArrayList<>();

        if (isBlank(title)) {
            problems.add("Title is required.");
        } else if (title.trim().length() > TITLE_MAX) {
            problems.add("Title must be " + TITLE_MAX + " characters or fewer.");
        }

        if (isBlank(author)) {
            problems.add("Author is required.");
        } else if (author.trim().length() > AUTHOR_MAX) {
            problems.add("Author must be " + AUTHOR_MAX + " characters or fewer.");
        } else if (!author.matches(".*\\p{L}.*")) {
            problems.add("Author must contain letters.");
        }

        if (isBlank(isbn)) {
            problems.add("ISBN is required.");
        } else {
            String clean = normalizeIsbn(isbn);
            if (!ISBN10.matcher(clean).matches() && !ISBN13.matcher(clean).matches()) {
                problems.add("ISBN must have 10 or 13 digits (hyphens and spaces are allowed).");
            }
        }

        if (category != null && category.trim().length() > CATEGORY_MAX) {
            problems.add("Category must be " + CATEGORY_MAX + " characters or fewer.");
        }
        return problems;
    }

    /** Removes hyphens and spaces and upper-cases a trailing X so ISBNs compare consistently. */
    public static String normalizeIsbn(String isbn) {
        return isbn == null ? "" : isbn.replaceAll("[\\s\\-]", "").toUpperCase();
    }

    // ---------- Members ----------

    public static List<String> validateMember(String fullName, String phone, String email) {
        List<String> problems = new ArrayList<>();

        if (isBlank(fullName)) {
            problems.add("Full name is required.");
        } else if (!NAME.matcher(fullName.trim()).matches()) {
            problems.add("Full name may only contain letters, spaces, hyphens, apostrophes and full stops "
                    + "(2 to " + NAME_MAX + " characters).");
        }

        boolean hasPhone = !isBlank(phone);
        boolean hasEmail = !isBlank(email);

        if (!hasPhone && !hasEmail) {
            problems.add("Enter a contact number or an email address (at least one is required).");
        }
        if (hasPhone && !isValidPhone(phone)) {
            problems.add("Contact number must be a South African number: 10 digits starting with 0 "
                    + "(e.g. 0731234567) or +27 followed by 9 digits.");
        }
        if (hasEmail) {
            if (email.trim().length() > EMAIL_MAX) {
                problems.add("Email must be " + EMAIL_MAX + " characters or fewer.");
            } else if (!EMAIL.matcher(email.trim()).matches()) {
                problems.add("Email address is not valid (expected something like name@example.com).");
            }
        }
        return problems;
    }

    public static boolean isValidPhone(String phone) {
        String clean = normalizePhone(phone);
        return PHONE_LOCAL.matcher(clean).matches() || PHONE_INTL.matcher(clean).matches();
    }

    /** Removes spaces, hyphens and brackets from a phone number. */
    public static String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[\\s\\-()]", "");
    }

    // ---------- Librarian accounts ----------

    public static List<String> validateRegistration(String fullName, String username,
            String password, String confirmPassword) {
        List<String> problems = new ArrayList<>();

        if (isBlank(fullName)) {
            problems.add("Full name is required.");
        } else if (!NAME.matcher(fullName.trim()).matches()) {
            problems.add("Full name may only contain letters, spaces, hyphens, apostrophes and full stops.");
        }

        if (isBlank(username)) {
            problems.add("Username is required.");
        } else {
            String u = username.trim();
            if (u.length() < USERNAME_MIN || u.length() > USERNAME_MAX) {
                problems.add("Username must be " + USERNAME_MIN + " to " + USERNAME_MAX + " characters.");
            } else if (!USERNAME.matcher(u).matches()) {
                problems.add("Username may only contain letters, numbers and . _ @ - (no spaces).");
            }
        }

        if (password == null || password.isEmpty()) {
            problems.add("Password is required.");
        } else if (password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            problems.add("Password must be " + PASSWORD_MIN + " to " + PASSWORD_MAX + " characters.");
        } else if (!isBlank(username) && password.equalsIgnoreCase(username.trim())) {
            problems.add("Password must not be the same as the username.");
        }

        if (password != null && !password.equals(confirmPassword)) {
            problems.add("Password and confirmation do not match.");
        }
        return problems;
    }

    // ---------- Helpers ----------

    /** Formats a problem list as a bulleted message for a dialog box. */
    public static String formatProblems(List<String> problems) {
        StringBuilder sb = new StringBuilder("Please correct the following:\n");
        for (String p : problems) {
            sb.append("\n  \u2022  ").append(p);
        }
        return sb.toString();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
