package com.pixelpioneers.library.model;

import java.sql.Date;

/** One borrowing event: which member took which book, when it is due, and any fine (Phase 3 ERD: BorrowTransactions). */
public class BorrowTransaction {
    private int transactionId;
    private int bookId;
    private int memberId;
    private Date borrowDate;
    private Date dueDate;
    private Date returnDate;
    private double fineAmount;
    private double amountPaid;
    private String bookTitle;
    private String memberName;

    public BorrowTransaction() { }

    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }
    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }
    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }
    public Date getBorrowDate() { return borrowDate; }
    public void setBorrowDate(Date borrowDate) { this.borrowDate = borrowDate; }
    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }
    public Date getReturnDate() { return returnDate; }
    public void setReturnDate(Date returnDate) { this.returnDate = returnDate; }
    public double getFineAmount() { return fineAmount; }
    public void setFineAmount(double fineAmount) { this.fineAmount = fineAmount; }
    /** Total paid so far against this transaction's fine (joined from FinePayments). */
    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }

    /** "-" when there is no fine, otherwise "Paid" or "Unpaid". */
    public String getFineStatus() {
        if (fineAmount <= 0) {
            return "-";
        }
        return amountPaid >= fineAmount ? "Paid" : "Unpaid";
    }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
}
