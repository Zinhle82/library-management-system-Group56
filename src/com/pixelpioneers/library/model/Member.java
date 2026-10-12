package com.pixelpioneers.library.model;

/** A registered library member (Phase 3 ERD: Members). */
public class Member {
    private int memberId;
    private String fullName;
    private String contactNumber;
    private String email;

    public Member() { }

    public Member(int memberId, String fullName, String contactNumber, String email) {
        this.memberId = memberId;
        this.fullName = fullName;
        this.contactNumber = contactNumber;
        this.email = email;
    }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() { return fullName; }
}
