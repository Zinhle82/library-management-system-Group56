package com.pixelpioneers.library.dao;

import com.pixelpioneers.library.db.DBConnection;
import com.pixelpioneers.library.model.Member;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Database Layer: all SQL for the Members table. */
public class MemberDAO {

    public void addMember(Member member) throws SQLException {
        String sql = "INSERT INTO members (full_name, contact_number, email) VALUES (?, ?, ?)";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, member.getFullName());
            ps.setString(2, member.getContactNumber());
            ps.setString(3, member.getEmail());
            ps.executeUpdate();
        }
    }

    /** True if another member already uses this email (case-insensitive). */
    public boolean emailExists(String email, Integer excludeMemberId) throws SQLException {
        String sql = "SELECT 1 FROM members WHERE LOWER(email) = LOWER(?) AND member_id <> ? LIMIT 1";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setInt(2, excludeMemberId == null ? -1 : excludeMemberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void updateMember(Member member) throws SQLException {
        String sql = "UPDATE members SET full_name = ?, contact_number = ?, email = ? WHERE member_id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, member.getFullName());
            ps.setString(2, member.getContactNumber());
            ps.setString(3, member.getEmail());
            ps.setInt(4, member.getMemberId());
            ps.executeUpdate();
        }
    }

    public void deleteMember(int memberId) throws SQLException {
        String sql = "DELETE FROM members WHERE member_id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            ps.executeUpdate();
        }
    }

    public List<Member> getAllMembers() throws SQLException {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT * FROM members ORDER BY full_name";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) members.add(mapRow(rs));
        }
        return members;
    }

    public List<Member> searchMembers(String keyword) throws SQLException {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT * FROM members WHERE full_name LIKE ? OR email LIKE ? OR contact_number LIKE ? ORDER BY full_name";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) members.add(mapRow(rs));
            }
        }
        return members;
    }

    private Member mapRow(ResultSet rs) throws SQLException {
        return new Member(rs.getInt("member_id"), rs.getString("full_name"),
                rs.getString("contact_number"), rs.getString("email"));
    }
}
