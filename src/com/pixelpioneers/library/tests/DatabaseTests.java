package com.pixelpioneers.library.tests;

import static com.pixelpioneers.library.tests.TestSupport.check;
import static com.pixelpioneers.library.tests.TestSupport.expectThrows;
import static com.pixelpioneers.library.tests.TestSupport.section;

import com.pixelpioneers.library.dao.BookDAO;
import com.pixelpioneers.library.dao.BorrowDAO;
import com.pixelpioneers.library.dao.FineDAO;
import com.pixelpioneers.library.dao.MemberDAO;
import com.pixelpioneers.library.dao.UserDAO;
import com.pixelpioneers.library.db.DBConnection;
import com.pixelpioneers.library.model.Book;
import com.pixelpioneers.library.model.BorrowTransaction;
import com.pixelpioneers.library.model.LibUser;
import com.pixelpioneers.library.model.Member;
import com.pixelpioneers.library.service.BusinessRuleException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Automated end-to-end tests of the Application and Database layers against the
 * real database (needs MySQL running with library_db loaded and db.properties
 * correct). Every record it creates is named "ZZ Test ..." and is deleted again
 * at the end, so it is safe to run on your own data.
 */
public class DatabaseTests {

    private static final BookDAO BOOKS = new BookDAO();
    private static final MemberDAO MEMBERS = new MemberDAO();
    private static final BorrowDAO BORROWS = new BorrowDAO();
    private static final FineDAO FINES = new FineDAO();
    private static final UserDAO USERS = new UserDAO();

    public static void main(String[] args) throws Exception {
        try {
            run();
        } finally {
            cleanup();
        }
        System.exit(TestSupport.finish());
    }

    private static void run() throws Exception {
        // ----- accounts -----
        section("Librarian accounts");
        cleanup(); // start from a clean slate if an earlier run was interrupted
        check("D-01", "username that does not exist is reported as free", !USERS.usernameExists("zz_test_user"));
        LibUser created = USERS.register("zz_test_user", "secret1", "ZZ Test User");
        check("D-02", "new account is saved and can be found", USERS.usernameExists("zz_test_user") && created.getUserId() > 0);
        check("D-03", "correct username and password log in", USERS.authenticate("zz_test_user", "secret1") != null);
        check("D-04", "wrong password is refused", USERS.authenticate("zz_test_user", "wrong-pass") == null);
        check("D-05", "unknown username is refused", USERS.authenticate("zz_nobody", "secret1") == null);
        check("D-06", "password is stored hashed, not as typed", !queryString(
                "SELECT password FROM users WHERE username = 'zz_test_user'").contains("secret1"));

        // ----- books and members -----
        section("Books and members");
        addBook("ZZ Test Book 1", "9780000000011");
        addBook("ZZ Test Book 2", "9780000000028");
        addBook("ZZ Test Book 3", "9780000000035");
        addBook("ZZ Test Book 4", "9780000000042");
        int b1 = bookId("ZZ Test Book 1"), b2 = bookId("ZZ Test Book 2");
        int b3 = bookId("ZZ Test Book 3"), b4 = bookId("ZZ Test Book 4");
        check("D-07", "added books are found by search", BOOKS.searchBooks("ZZ Test Book").size() == 4);
        check("D-08", "duplicate ISBN is detected", BOOKS.isbnExists("9780000000011", null));
        check("D-09", "a book does not clash with its own ISBN when edited", !BOOKS.isbnExists("9780000000011", b1));
        check("D-10", "new books start as Available", "Available".equals(bookStatus(b1)));

        Member a = new Member();
        a.setFullName("ZZ Test Member A");
        a.setContactNumber("0710000001");
        a.setEmail("zz.test.a@example.com");
        MEMBERS.addMember(a);
        Member bMember = new Member();
        bMember.setFullName("ZZ Test Member B");
        bMember.setContactNumber("0710000002");
        bMember.setEmail("zz.test.b@example.com");
        MEMBERS.addMember(bMember);
        int memberA = memberId("ZZ Test Member A"), memberB = memberId("ZZ Test Member B");
        check("D-11", "duplicate member email is detected", MEMBERS.emailExists("ZZ.TEST.A@example.com", null));

        // ----- borrowing -----
        section("Borrowing");
        check("D-12", "a member with a clean record is eligible", BORROWS.checkBorrowEligibility(memberA) == null);
        BORROWS.borrowBook(b1, memberA);
        check("D-13", "borrowing marks the book Borrowed", "Borrowed".equals(bookStatus(b1)));
        BorrowTransaction t1 = findActive(memberA, "ZZ Test Book 1");
        check("D-14", "borrowing creates a record with a due date 14 days away",
                t1 != null && daysBetween(t1.getBorrowDate(), t1.getDueDate()) == 14 && t1.getReturnDate() == null);
        expectThrows("D-15", "borrowing a book that is already out is refused", BusinessRuleException.class,
                () -> BORROWS.borrowBook(b1, memberB));
        BORROWS.borrowBook(b2, memberA);
        BORROWS.borrowBook(b3, memberA);
        String limitReason = BORROWS.checkBorrowEligibility(memberA);
        check("D-16", "a member with 3 books out is no longer eligible (limit reached)",
                limitReason != null && limitReason.contains("maximum"));
        expectThrows("D-17", "a 4th book is refused", BusinessRuleException.class,
                () -> BORROWS.borrowBook(b4, memberA));
        check("D-18", "the refused book stays Available (rollback worked)", "Available".equals(bookStatus(b4)));

        // ----- returning on time -----
        section("Returning");
        double fine1 = BORROWS.returnBook(t1.getTransactionId());
        check("D-19", "returning on time costs no fine", fine1 == 0.0);
        check("D-20", "returning marks the book Available again", "Available".equals(bookStatus(b1)));
        expectThrows("D-21", "returning the same book twice is refused", BusinessRuleException.class,
                () -> BORROWS.returnBook(t1.getTransactionId()));
        expectThrows("D-22", "paying a fine on an on-time return is refused", BusinessRuleException.class,
                () -> FINES.payFine(t1.getTransactionId()));
        BORROWS.returnBook(findActive(memberA, "ZZ Test Book 2").getTransactionId());
        BORROWS.returnBook(findActive(memberA, "ZZ Test Book 3").getTransactionId());
        check("D-23", "after returning, the member can borrow again", BORROWS.checkBorrowEligibility(memberA) == null);

        // ----- overdue, fines, payment -----
        section("Overdue books and fines");
        // back-dated record: borrowed 19 days ago, due 5 days ago, still out
        execute("INSERT INTO borrow_transactions (book_id, member_id, borrow_date, due_date, fine_amount) "
                + "VALUES (" + b4 + ", " + memberB + ", DATE_SUB(CURDATE(), INTERVAL 19 DAY), "
                + "DATE_SUB(CURDATE(), INTERVAL 5 DAY), 0)");
        execute("UPDATE books SET status = 'Borrowed' WHERE book_id = " + b4);
        String overdueReason = BORROWS.checkBorrowEligibility(memberB);
        check("D-24", "a member with an overdue book cannot borrow",
                overdueReason != null && overdueReason.contains("overdue"));
        check("D-25", "the overdue book appears in the overdue report", containsBook(BORROWS.getOverdueBorrows(), "ZZ Test Book 4"));
        expectThrows("D-26", "borrowing is refused for the overdue member", BusinessRuleException.class,
                () -> BORROWS.borrowBook(b1, memberB));

        BorrowTransaction late = findActive(memberB, "ZZ Test Book 4");
        double fine = BORROWS.returnBook(late.getTransactionId());
        check("D-27", "returning 5 days late costs R10.00", fine == 10.0);
        check("D-28", "the unpaid fine appears in the Pay Fines list", containsBook(BORROWS.getUnpaidFines(), "ZZ Test Book 4"));
        String unpaidReason = BORROWS.checkBorrowEligibility(memberB);
        check("D-29", "a member with an unpaid fine cannot borrow", unpaidReason != null && unpaidReason.contains("unpaid"));

        double paid = FINES.payFine(late.getTransactionId());
        check("D-30", "paying the fine records the full amount", paid == 10.0);
        check("D-31", "a paid fine leaves the Pay Fines list", !containsBook(BORROWS.getUnpaidFines(), "ZZ Test Book 4"));
        check("D-32", "after paying, the member can borrow again", BORROWS.checkBorrowEligibility(memberB) == null);
        expectThrows("D-33", "paying the same fine twice is refused", BusinessRuleException.class,
                () -> FINES.payFine(late.getTransactionId()));

        // ----- history and protection of records -----
        section("History and data protection");
        List<BorrowTransaction> history = BORROWS.getTransactionsByMember(memberB);
        check("D-34", "history by member shows only that member's record, with fine status Paid",
                history.size() == 1 && "Paid".equals(history.get(0).getFineStatus()));
        check("D-35", "full history includes the test records", containsBook(BORROWS.getAllTransactions(), "ZZ Test Book 1"));
        expectThrows("D-36", "a member with borrowing history cannot be deleted", SQLException.class,
                () -> MEMBERS.deleteMember(memberB));
        expectThrows("D-37", "a book with borrowing history cannot be deleted", SQLException.class,
                () -> BOOKS.deleteBook(b4));
    }

    // ---------- helpers ----------

    private static void addBook(String title, String isbn) throws SQLException {
        Book b = new Book();
        b.setTitle(title);
        b.setAuthor("ZZ Test Author");
        b.setIsbn(isbn);
        b.setCategory("Testing");
        BOOKS.addBook(b);
    }

    private static int bookId(String title) throws SQLException {
        return queryInt("SELECT book_id FROM books WHERE title = '" + title + "'");
    }

    private static int memberId(String name) throws SQLException {
        return queryInt("SELECT member_id FROM members WHERE full_name = '" + name + "'");
    }

    private static String bookStatus(int bookId) throws SQLException {
        return queryString("SELECT status FROM books WHERE book_id = " + bookId);
    }

    private static BorrowTransaction findActive(int memberId, String title) throws SQLException {
        for (BorrowTransaction t : BORROWS.getActiveBorrows()) {
            if (t.getMemberId() == memberId && t.getBookTitle().equals(title)) {
                return t;
            }
        }
        return null;
    }

    private static boolean containsBook(List<BorrowTransaction> list, String title) {
        for (BorrowTransaction t : list) {
            if (title.equals(t.getBookTitle())) {
                return true;
            }
        }
        return false;
    }

    private static long daysBetween(java.sql.Date from, java.sql.Date to) {
        return java.time.temporal.ChronoUnit.DAYS.between(from.toLocalDate(), to.toLocalDate());
    }

    private static int queryInt(String sql) throws SQLException {
        try (Statement st = DBConnection.getConnection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    private static String queryString(String sql) throws SQLException {
        try (Statement st = DBConnection.getConnection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : "";
        }
    }

    private static void execute(String sql) throws SQLException {
        try (Statement st = DBConnection.getConnection().createStatement()) {
            st.executeUpdate(sql);
        }
    }

    /** Deletes every "ZZ Test" record (children first, because of the foreign keys). */
    private static void cleanup() throws SQLException {
        List<String> steps = new ArrayList<>();
        steps.add("DELETE FROM fine_payments WHERE transaction_id IN (SELECT transaction_id FROM borrow_transactions "
                + "WHERE book_id IN (SELECT book_id FROM books WHERE title LIKE 'ZZ Test%'))");
        steps.add("DELETE FROM borrow_transactions WHERE book_id IN (SELECT book_id FROM books WHERE title LIKE 'ZZ Test%')");
        steps.add("DELETE FROM borrow_transactions WHERE member_id IN (SELECT member_id FROM members WHERE full_name LIKE 'ZZ Test%')");
        steps.add("DELETE FROM books WHERE title LIKE 'ZZ Test%'");
        steps.add("DELETE FROM members WHERE full_name LIKE 'ZZ Test%'");
        steps.add("DELETE FROM users WHERE username = 'zz_test_user'");
        Connection conn = DBConnection.getConnection();
        for (String sql : steps) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.executeUpdate();
            }
        }
    }
}
