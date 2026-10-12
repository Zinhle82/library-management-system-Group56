package com.pixelpioneers.library.dao;

import com.pixelpioneers.library.db.DBConnection;
import com.pixelpioneers.library.model.LibUser;
import com.pixelpioneers.library.service.PasswordUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Database Layer: librarian accounts. Passwords are stored as salted hashes (see PasswordUtil). */
public class UserDAO {

    /** Returns the matching user, or null if the username or password is wrong. */
    public LibUser authenticate(String username, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && PasswordUtil.verify(password, rs.getString("password"))) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Creates a new librarian account (hashing the password) and returns it with its generated ID. */
    public LibUser register(String username, String password, String fullName) throws SQLException {
        String sql = "INSERT INTO users (username, password, full_name) VALUES (?, ?, ?)";
        String stored = PasswordUtil.hash(password);
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, stored);
            ps.setString(3, fullName);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                int newId = keys.next() ? keys.getInt(1) : 0;
                return new LibUser(newId, username, stored, fullName);
            }
        }
    }

    private LibUser mapRow(ResultSet rs) throws SQLException {
        LibUser user = new LibUser();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setFullName(rs.getString("full_name"));
        return user;
    }
}
