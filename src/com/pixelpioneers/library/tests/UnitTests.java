package com.pixelpioneers.library.tests;

import static com.pixelpioneers.library.tests.TestSupport.check;
import static com.pixelpioneers.library.tests.TestSupport.section;

import com.pixelpioneers.library.service.LibraryRules;
import com.pixelpioneers.library.service.PasswordUtil;
import com.pixelpioneers.library.service.Validator;
import java.time.LocalDate;
import java.util.List;

/**
 * Automated tests for the Application Layer (validation, passwords, business
 * rules). They need no database, so they can run anywhere:
 * in NetBeans right-click this file and choose Run File.
 */
public class UnitTests {

    public static void main(String[] args) {
        validatorBooks();
        validatorMembers();
        validatorRegistration();
        passwords();
        rules();
        System.exit(TestSupport.finish());
    }

    private static void validatorBooks() {
        section("Book validation");
        check("V-01", "valid book is accepted",
                Validator.validateBook("Clean Code", "Robert C. Martin", "9780132350884", "Software").isEmpty());

        List<String> blanks = Validator.validateBook("", "  ", "", "");
        check("V-02", "blank title, author and ISBN are each reported (3 problems)", blanks.size() == 3);

        check("V-03", "ISBN with too few digits is rejected",
                !Validator.validateBook("T", "A", "12345", "").isEmpty());
        check("V-04", "hyphenated ISBN-13 is accepted and normalised",
                Validator.validateBook("T", "A", "978-0-13-235088-4", "").isEmpty()
                        && Validator.normalizeIsbn("978-0-13-235088-4").equals("9780132350884"));
        check("V-05", "ISBN-10 ending in X is accepted",
                Validator.validateBook("T", "A", "080442957x", "").isEmpty());
        check("V-06", "title longer than 100 characters is rejected",
                !Validator.validateBook("x".repeat(101), "A", "9780132350884", "").isEmpty());
        check("V-07", "ISBN containing letters is rejected",
                !Validator.validateBook("T", "A", "97801323ABCDE", "").isEmpty());
    }

    private static void validatorMembers() {
        section("Member validation");
        check("M-01", "valid member is accepted",
                Validator.validateMember("Lerato Molefe", "0731234567", "lerato@example.com").isEmpty());
        check("M-02", "member with no phone and no email is rejected",
                !Validator.validateMember("Lerato Molefe", "", "").isEmpty());
        check("M-03", "phone number that is too short is rejected",
                !Validator.validateMember("Lerato Molefe", "12345", "").isEmpty());
        check("M-04", "+27 international phone number is accepted",
                Validator.validateMember("Lerato Molefe", "+27731234567", "").isEmpty());
        check("M-05", "email without @ is rejected",
                !Validator.validateMember("Lerato Molefe", "", "lerato.example.com").isEmpty());
        check("M-06", "name containing digits is rejected",
                !Validator.validateMember("Lerato123", "0731234567", "").isEmpty());
        check("M-07", "phone with spaces is accepted and normalised",
                Validator.validateMember("Lerato Molefe", "073 123 4567", "").isEmpty()
                        && Validator.normalizePhone("073 123 4567").equals("0731234567"));
        check("M-08", "name with apostrophe and hyphen is accepted",
                Validator.validateMember("Sipho O'Neil-Dlamini", "0731234567", "").isEmpty());
    }

    private static void validatorRegistration() {
        section("Account validation");
        check("R-01", "valid registration is accepted",
                Validator.validateRegistration("Zinhle Zwane", "zwane@12", "secret1", "secret1").isEmpty());
        check("R-02", "password shorter than 6 characters is rejected",
                !Validator.validateRegistration("Zinhle Zwane", "zwane12", "abc", "abc").isEmpty());
        check("R-03", "mismatched confirmation is rejected",
                !Validator.validateRegistration("Zinhle Zwane", "zwane12", "secret1", "secret2").isEmpty());
        check("R-04", "username containing a space is rejected",
                !Validator.validateRegistration("Zinhle Zwane", "zwane 12", "secret1", "secret1").isEmpty());
        check("R-05", "password equal to the username is rejected",
                !Validator.validateRegistration("Zinhle Zwane", "zwane12", "zwane12", "zwane12").isEmpty());
        check("R-06", "completely blank form reports several problems at once",
                Validator.validateRegistration("", "", "", "").size() >= 3);
        check("R-07", "username shorter than 3 characters is rejected",
                !Validator.validateRegistration("Zinhle Zwane", "zw", "secret1", "secret1").isEmpty());
    }

    private static void passwords() {
        section("Password security");
        String stored = PasswordUtil.hash("secret1");
        check("P-01", "stored value is not the plain password and fits VARCHAR(100)",
                !stored.contains("secret1") && stored.length() <= 100);
        check("P-02", "correct password verifies", PasswordUtil.verify("secret1", stored));
        check("P-03", "wrong password does not verify", !PasswordUtil.verify("secret2", stored));
        check("P-04", "hashing the same password twice gives different results (random salt)",
                !PasswordUtil.hash("secret1").equals(PasswordUtil.hash("secret1")));
        check("P-05", "a corrupted stored value is rejected without crashing",
                !PasswordUtil.verify("secret1", "not-a-valid-hash") && !PasswordUtil.verify("secret1", "zz:yy"));
    }

    private static void rules() {
        section("Business rules (fines and loan period)");
        LocalDate due = LocalDate.of(2026, 10, 1);
        check("L-01", "returned on the due date: no fine", LibraryRules.calculateFine(due, due) == 0.0);
        check("L-02", "returned before the due date: no fine, never negative",
                LibraryRules.calculateFine(due, due.minusDays(3)) == 0.0
                        && LibraryRules.daysOverdue(due, due.minusDays(3)) == 0);
        check("L-03", "5 days late costs R10.00 (R2 per day)",
                LibraryRules.calculateFine(due, due.plusDays(5)) == 10.0);
        check("L-04", "16 days late costs R32.00", LibraryRules.calculateFine(due, due.plusDays(16)) == 32.0);
        check("L-05", "due date is 14 days after borrowing",
                LibraryRules.dueDateFor(LocalDate.of(2026, 10, 1)).equals(LocalDate.of(2026, 10, 15)));
        check("L-06", "a member may hold at most 3 books", LibraryRules.MAX_ACTIVE_LOANS == 3);
    }
}
