package com.pixelpioneers.library.dao;

import com.pixelpioneers.library.db.DBConnection;
import com.pixelpioneers.library.model.BorrowTransaction;
import com.pixelpioneers.library.service.BusinessRuleException;
import com.pixelpioneers.library.service.LibraryRules;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Database Layer: borrow and return transactions.
 *
 * Borrowing and returning each change two tables (borrow_transactions and
 * books). Both changes are wrapped in a database transaction: either both
 * succeed (commit) or neither does (rollback), so a book's status can never
 * disagree with its borrowing record. The business rules themselves (loan
 * length, fine rate, loan limit) live in {@link LibraryRules}.
 */
public class BorrowDAO {

    /** Shared SELECT: a transaction joined with its book title, member name and amount paid so far. */
    private static final String BASE_SELECT =
            "SELECT bt.*, b.title AS book_title, m.full_name AS member_name, "
            + "COALESCE((SELECT SUM(fp.amount_paid) FROM fine_payments fp "
            + "WHERE fp.transaction_id = bt.transaction_id), 0) AS amount_paid "
            + "FROM borrow_transactions bt "
            + "JOIN books b ON bt.book_id = b.book_id "
            + "JOIN members m ON bt.member_id = m.member_id ";

    /** SQL fragment: a returned transaction whose fine has not been fully paid. */
    private static final String UNPAID_CONDITION =
            "bt.return_date IS NOT NULL AND bt.fine_amount > 0 AND bt.fine_amount > "
            + "COALESCE((SELECT SUM(fp.amount_paid) FROM fine_payments fp "
            + "WHERE fp.transaction_id = bt.transaction_id), 0)";

    /**
     * Phase 2, 2.3.4: "System determines if the member can borrow".
     * @return null if the member may borrow, otherwise the reason they may not
     */
    public String checkBorrowEligibility(int memberId) throws SQLException {
        Connection conn = DBConnection.getConnection();
        Date today = Date.valueOf(LocalDate.now());

        int overdue = count(conn, "SELECT COUNT(*) FROM borrow_transactions "
                + "WHERE member_id = ? AND return_date IS NULL AND due_date < ?", memberId, today);
        if (overdue > 0) {
            return "This member has " + overdue + " overdue book" + (overdue > 1 ? "s" : "")
                    + " that must be returned first.";
        }

        int unpaid = count(conn, "SELECT COUNT(*) FROM borrow_transactions bt WHERE bt.member_id = ? AND "
                + UNPAID_CONDITION, memberId);
        if (unpaid > 0) {
            return "This member has " + unpaid + " unpaid fine" + (unpaid > 1 ? "s" : "")
                    + " that must be paid first.";
        }

        int active = count(conn, "SELECT COUNT(*) FROM borrow_transactions "
                + "WHERE member_id = ? AND return_date IS NULL", memberId);
        if (active >= LibraryRules.MAX_ACTIVE_LOANS) {
            return "This member already has " + active + " books out (the maximum is "
                    + LibraryRules.MAX_ACTIVE_LOANS + ").";
        }
        return null;
    }

    /**
     * Records a borrow: checks the book is still available and the member is eligible,
     * creates the transaction with the due date set automatically, and marks the book Borrowed.
     */
    public void borrowBook(int bookId, int memberId) throws SQLException, BusinessRuleException {
        Connection conn = DBConnection.getConnection();
        conn.setAutoCommit(false);
        try {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT status FROM books WHERE book_id = ? FOR UPDATE")) {
                ps.setInt(1, bookId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new BusinessRuleException("That book no longer exists.");
                    }
                    if (!"Available".equals(rs.getString("status"))) {
                        throw new BusinessRuleException("That book is already borrowed.");
                    }
                }
            }

            String reason = checkBorrowEligibility(memberId);
            if (reason != null) {
                throw new BusinessRuleException(reason);
            }

            LocalDate today = LocalDate.now();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO borrow_transactions (book_id, member_id, borrow_date, due_date, fine_amount) "
                    + "VALUES (?, ?, ?, ?, 0)")) {
                ps.setInt(1, bookId);
                ps.setInt(2, memberId);
                ps.setDate(3, Date.valueOf(today));
                ps.setDate(4, Date.valueOf(LibraryRules.dueDateFor(today)));
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE books SET status = 'Borrowed' WHERE book_id = ?")) {
                ps.setInt(1, bookId);
                ps.executeUpdate();
            }
            conn.commit();
        } catch (SQLException | BusinessRuleException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Records a return today: stores the return date and the calculated fine, and marks the book Available.
     * @return the fine in rand (0.0 if returned on time)
     */
    public double returnBook(int transactionId) throws SQLException, BusinessRuleException {
        Connection conn = DBConnection.getConnection();
        conn.setAutoCommit(false);
        try {
            int bookId;
            LocalDate dueDate;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT book_id, due_date, return_date FROM borrow_transactions "
                    + "WHERE transaction_id = ? FOR UPDATE")) {
                ps.setInt(1, transactionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new BusinessRuleException("That borrowing record no longer exists.");
                    }
                    if (rs.getDate("return_date") != null) {
                        throw new BusinessRuleException("That book has already been returned.");
                    }
                    bookId = rs.getInt("book_id");
                    dueDate = rs.getDate("due_date").toLocalDate();
                }
            }

            LocalDate today = LocalDate.now();
            double fine = LibraryRules.calculateFine(dueDate, today);

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE borrow_transactions SET return_date = ?, fine_amount = ? WHERE transaction_id = ?")) {
                ps.setDate(1, Date.valueOf(today));
                ps.setDouble(2, fine);
                ps.setInt(3, transactionId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE books SET status = 'Available' WHERE book_id = ?")) {
                ps.setInt(1, bookId);
                ps.executeUpdate();
            }
            conn.commit();
            return fine;
        } catch (SQLException | BusinessRuleException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /** Books currently out (not yet returned), earliest due date first. */
    public List<BorrowTransaction> getActiveBorrows() throws SQLException {
        return runQuery(BASE_SELECT + "WHERE bt.return_date IS NULL ORDER BY bt.due_date");
    }

    /** Books currently out whose due date has passed, most overdue first. */
    public List<BorrowTransaction> getOverdueBorrows() throws SQLException {
        return runQuery(BASE_SELECT + "WHERE bt.return_date IS NULL AND bt.due_date < ? ORDER BY bt.due_date",
                Date.valueOf(LocalDate.now()));
    }

    /** Full borrowing history, newest first. */
    public List<BorrowTransaction> getAllTransactions() throws SQLException {
        return runQuery(BASE_SELECT + "ORDER BY bt.borrow_date DESC, bt.transaction_id DESC");
    }

    /** Phase 1 feature: "View the borrowing history of books by member". */
    public List<BorrowTransaction> getTransactionsByMember(int memberId) throws SQLException {
        return runQuery(BASE_SELECT + "WHERE bt.member_id = ? ORDER BY bt.borrow_date DESC, bt.transaction_id DESC",
                memberId);
    }

    /** Returned books whose overdue fine has not yet been paid. */
    public List<BorrowTransaction> getUnpaidFines() throws SQLException {
        return runQuery(BASE_SELECT + "WHERE " + UNPAID_CONDITION + " ORDER BY bt.return_date");
    }

    private List<BorrowTransaction> runQuery(String sql, Object... params) throws SQLException {
        List<BorrowTransaction> list = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BorrowTransaction t = new BorrowTransaction();
                    t.setTransactionId(rs.getInt("transaction_id"));
                    t.setBookId(rs.getInt("book_id"));
                    t.setMemberId(rs.getInt("member_id"));
                    t.setBorrowDate(rs.getDate("borrow_date"));
                    t.setDueDate(rs.getDate("due_date"));
                    t.setReturnDate(rs.getDate("return_date"));
                    t.setFineAmount(rs.getDouble("fine_amount"));
                    t.setAmountPaid(rs.getDouble("amount_paid"));
                    t.setBookTitle(rs.getString("book_title"));
                    t.setMemberName(rs.getString("member_name"));
                    list.add(t);
                }
            }
        }
        return list;
    }

    private int count(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
