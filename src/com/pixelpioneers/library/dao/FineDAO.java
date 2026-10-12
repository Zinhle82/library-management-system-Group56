package com.pixelpioneers.library.dao;

import com.pixelpioneers.library.db.DBConnection;
import com.pixelpioneers.library.service.BusinessRuleException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Database Layer: fine payments (Phase 2, 2.3.6 "Pay Fines Activity").
 * Following the Phase 3 relationship design, a transaction has at most one
 * payment, and that payment settles the whole fine.
 */
public class FineDAO {

    /**
     * Registers payment of the full fine on a returned transaction.
     * @return the amount paid
     * @throws BusinessRuleException if the book is not returned, there is no fine, or it is already paid
     */
    public double payFine(int transactionId) throws SQLException, BusinessRuleException {
        Connection conn = DBConnection.getConnection();
        conn.setAutoCommit(false);
        try {
            double fine;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT fine_amount, return_date FROM borrow_transactions "
                    + "WHERE transaction_id = ? FOR UPDATE")) {
                ps.setInt(1, transactionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new BusinessRuleException("That borrowing record no longer exists.");
                    }
                    if (rs.getDate("return_date") == null) {
                        throw new BusinessRuleException("The book has not been returned yet, so the fine is not final.");
                    }
                    fine = rs.getDouble("fine_amount");
                }
            }
            if (fine <= 0) {
                throw new BusinessRuleException("There is no fine to pay on this transaction.");
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM fine_payments WHERE transaction_id = ?")) {
                ps.setInt(1, transactionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new BusinessRuleException("This fine has already been paid.");
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO fine_payments (transaction_id, amount_paid, payment_date) VALUES (?, ?, ?)")) {
                ps.setInt(1, transactionId);
                ps.setDouble(2, fine);
                ps.setDate(3, Date.valueOf(LocalDate.now()));
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
}
