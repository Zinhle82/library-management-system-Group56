package com.pixelpioneers.library.model;

import java.sql.Date;

/** A payment made against an overdue fine (Phase 3 ERD: FinePayments). */
public class FinePayment {
    private int paymentId;
    private int transactionId;
    private double amountPaid;
    private Date paymentDate;

    public FinePayment() { }

    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }
    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }
    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }
    public Date getPaymentDate() { return paymentDate; }
    public void setPaymentDate(Date paymentDate) { this.paymentDate = paymentDate; }
}
