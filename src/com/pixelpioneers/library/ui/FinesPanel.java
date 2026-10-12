package com.pixelpioneers.library.ui;

import com.pixelpioneers.library.dao.BorrowDAO;
import com.pixelpioneers.library.dao.FineDAO;
import com.pixelpioneers.library.model.BorrowTransaction;
import com.pixelpioneers.library.service.BusinessRuleException;
import com.pixelpioneers.library.service.LibraryRules;
import com.pixelpioneers.library.util.RoundedButton;
import com.pixelpioneers.library.util.UIComponents;
import com.pixelpioneers.library.util.UITheme;
import java.awt.BorderLayout;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Presentation Layer: the Pay Fines screen (Phase 2, 2.3.6 "Pay Fines Activity").
 * Lists every returned book whose overdue fine is still unpaid; the librarian
 * selects one and registers the member's payment.
 */
public class FinesPanel extends JPanel implements DashboardFrame.Refreshable {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final BorrowDAO borrowDAO = new BorrowDAO();
    private final FineDAO fineDAO = new FineDAO();

    private DefaultTableModel modelRef;
    private JTable table;
    private final List<BorrowTransaction> rows = new ArrayList<>();
    private final JLabel totalLabel = new JLabel(" ");

    public FinesPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(UITheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        add(UIComponents.sectionTitle("Pay Fines"), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 20));
        center.setOpaque(false);
        center.add(buildTablePanel(), BorderLayout.CENTER);
        center.add(buildPayBar(), BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        loadFines();
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.CARD_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JLabel header = new JLabel("  Unpaid fines (books already returned late)");
        header.setFont(UITheme.FONT_LABEL_BOLD);
        header.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 0));
        panel.add(header, BorderLayout.NORTH);

        modelRef = new DefaultTableModel(
                new Object[]{"Txn ID", "Book", "Member", "Due Date", "Returned On", "Days Late", "Fine"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(modelRef);
        UIComponents.styleTable(table);
        table.getColumnModel().getColumn(0).setMaxWidth(70);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildPayBar() {
        JPanel bar = new JPanel(new BorderLayout(20, 0));
        bar.setBackground(UITheme.CARD_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 4, 0, new java.awt.Color(0xD4A017)),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)));

        RoundedButton payButton = new RoundedButton("Register Payment", UITheme.SUCCESS, UITheme.SUCCESS_DARK);
        payButton.addActionListener(e -> registerPayment());
        JPanel left = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(payButton);

        totalLabel.setFont(UITheme.FONT_FINE);
        totalLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);

        bar.add(left, BorderLayout.WEST);
        bar.add(totalLabel, BorderLayout.EAST);
        return bar;
    }

    private void loadFines() {
        try {
            rows.clear();
            rows.addAll(borrowDAO.getUnpaidFines());
            modelRef.setRowCount(0);
            double total = 0;
            for (BorrowTransaction t : rows) {
                long late = LibraryRules.daysOverdue(t.getDueDate().toLocalDate(), t.getReturnDate().toLocalDate());
                modelRef.addRow(new Object[]{
                        t.getTransactionId(), t.getBookTitle(), t.getMemberName(),
                        t.getDueDate().toLocalDate().format(DATE_FORMAT),
                        t.getReturnDate().toLocalDate().format(DATE_FORMAT),
                        late, String.format("R %.2f", t.getFineAmount())
                });
                total += t.getFineAmount() - t.getAmountPaid();
            }
            totalLabel.setText(String.format("Outstanding: R %.2f", total));
            totalLabel.setForeground(total > 0 ? UITheme.DANGER : UITheme.SUCCESS_DARK);
        } catch (SQLException ex) {
            Dialogs.databaseError(this, "The unpaid fines could not be loaded.", ex);
        }
    }

    private void registerPayment() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= rows.size()) {
            Dialogs.rule(this, "No Fine Selected", "Select an unpaid fine from the table first.");
            return;
        }
        BorrowTransaction t = rows.get(row);
        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Record a payment of R %.2f from %s for \"%s\"?",
                        t.getFineAmount(), t.getMemberName(), t.getBookTitle()),
                "Confirm Payment", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            double paid = fineDAO.payFine(t.getTransactionId());
            Dialogs.info(this, "Fine Paid", String.format("Payment of R %.2f recorded for %s.", paid, t.getMemberName()));
        } catch (BusinessRuleException ex) {
            Dialogs.rule(this, "Cannot Record Payment", ex.getMessage());
        } catch (SQLException ex) {
            Dialogs.databaseError(this, "The payment could not be recorded.", ex);
        }
        loadFines();
    }

    @Override
    public void refresh() {
        loadFines();
    }
}
