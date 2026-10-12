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
import java.time.LocalDate;
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
 * Presentation Layer: the Return Book Screen (Phase 2, 2.3.5).
 * Selecting a borrowed book immediately shows the fine that would apply if it
 * were returned today, in large bold type (Phase 3 user feedback).
 */
public class ReturnBookPanel extends JPanel implements DashboardFrame.Refreshable {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final BorrowDAO borrowDAO = new BorrowDAO();
    private final FineDAO fineDAO = new FineDAO();

    private DefaultTableModel modelRef;
    private JTable table;
    private final List<BorrowTransaction> rows = new ArrayList<>();

    private final JLabel overdueInfo = new JLabel("Select a book from the table to see its fine.");
    private final JLabel fineLabel = new JLabel(" ");

    public ReturnBookPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(UITheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        add(UIComponents.sectionTitle("Return Book"), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 20));
        center.setOpaque(false);
        center.add(buildTablePanel(), BorderLayout.CENTER);
        center.add(buildReturnBar(), BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        loadActiveBorrows();
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.CARD_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        modelRef = new DefaultTableModel(
                new Object[]{"Txn ID", "Book", "Member", "Borrow Date", "Due Date", "Status"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(modelRef);
        UIComponents.styleTable(table);
        table.getColumnModel().getColumn(0).setMaxWidth(70);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateFinePreview();
            }
        });
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildReturnBar() {
        JPanel bar = new JPanel(new BorderLayout(20, 0));
        bar.setBackground(UITheme.CARD_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 4, 0, UITheme.ACCENT_PINK),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)));

        RoundedButton returnButton = new RoundedButton("Confirm Return", UITheme.PRIMARY, UITheme.PRIMARY_DARK);
        returnButton.addActionListener(e -> confirmReturn());
        JPanel left = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(returnButton);

        overdueInfo.setFont(UITheme.FONT_LABEL_BOLD);
        overdueInfo.setForeground(UITheme.TEXT_DARK);
        overdueInfo.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        fineLabel.setFont(UITheme.FONT_FINE);
        fineLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new javax.swing.BoxLayout(right, javax.swing.BoxLayout.Y_AXIS));
        overdueInfo.setAlignmentX(RIGHT_ALIGNMENT);
        fineLabel.setAlignmentX(RIGHT_ALIGNMENT);
        right.add(overdueInfo);
        right.add(fineLabel);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private BorrowTransaction selected() {
        int row = table.getSelectedRow();
        return (row >= 0 && row < rows.size()) ? rows.get(row) : null;
    }

    /** Shows the days overdue and the fine that would apply if the selected book were returned today. */
    private void updateFinePreview() {
        BorrowTransaction t = selected();
        if (t == null) {
            overdueInfo.setText("Select a book from the table to see its fine.");
            fineLabel.setText(" ");
            return;
        }
        LocalDate due = t.getDueDate().toLocalDate();
        long late = LibraryRules.daysOverdue(due, LocalDate.now());
        double fine = LibraryRules.calculateFine(due, LocalDate.now());
        overdueInfo.setText(late > 0
                ? "Overdue by " + late + (late == 1 ? " day" : " days") + "  (R "
                        + String.format("%.2f", LibraryRules.FINE_PER_DAY) + " per day)"
                : "Not overdue - no fine");
        fineLabel.setText(String.format("Fine if returned today: R %.2f", fine));
        fineLabel.setForeground(fine > 0 ? UITheme.DANGER : UITheme.SUCCESS_DARK);
    }

    private void loadActiveBorrows() {
        try {
            rows.clear();
            rows.addAll(borrowDAO.getActiveBorrows());
            modelRef.setRowCount(0);
            LocalDate today = LocalDate.now();
            for (BorrowTransaction t : rows) {
                long late = LibraryRules.daysOverdue(t.getDueDate().toLocalDate(), today);
                modelRef.addRow(new Object[]{
                        t.getTransactionId(), t.getBookTitle(), t.getMemberName(),
                        t.getBorrowDate().toLocalDate().format(DATE_FORMAT),
                        t.getDueDate().toLocalDate().format(DATE_FORMAT),
                        late > 0 ? "OVERDUE by " + late + (late == 1 ? " day" : " days") : "On loan"
                });
            }
            updateFinePreview();
        } catch (SQLException ex) {
            Dialogs.databaseError(this, "The borrowed books could not be loaded.", ex);
        }
    }

    private void confirmReturn() {
        BorrowTransaction t = selected();
        if (t == null) {
            Dialogs.rule(this, "No Book Selected", "Select a borrowed book from the table first.");
            return;
        }
        try {
            double fine = borrowDAO.returnBook(t.getTransactionId());
            showReturnResult(t, fine);
        } catch (BusinessRuleException ex) {
            Dialogs.rule(this, "Cannot Return", ex.getMessage());
        } catch (SQLException ex) {
            Dialogs.databaseError(this, "The return could not be recorded.", ex);
        }
        loadActiveBorrows();
    }

    private void showReturnResult(BorrowTransaction t, double fine) {
        long late = LibraryRules.daysOverdue(t.getDueDate().toLocalDate(), LocalDate.now());

        JPanel message = new JPanel();
        message.setLayout(new javax.swing.BoxLayout(message, javax.swing.BoxLayout.Y_AXIS));

        JLabel status = new JLabel("\"" + t.getBookTitle() + "\" returned by " + t.getMemberName());
        status.setFont(UITheme.FONT_LABEL_BOLD);
        message.add(status);
        message.add(javax.swing.Box.createVerticalStrut(6));
        message.add(new JLabel(late > 0 ? "It was " + late + (late == 1 ? " day" : " days") + " overdue."
                : "It was returned on time."));
        message.add(javax.swing.Box.createVerticalStrut(10));

        JLabel fineText = new JLabel(String.format("Fine Due: R %.2f", fine));
        fineText.setFont(UITheme.FONT_FINE);
        fineText.setForeground(fine > 0 ? UITheme.DANGER : UITheme.SUCCESS_DARK);
        message.add(fineText);

        if (fine > 0) {
            message.add(javax.swing.Box.createVerticalStrut(10));
            message.add(new JLabel("Has the member paid this fine now?"));
            message.add(new JLabel("(Choose No to pay later from the Pay Fines screen.)"));
        }

        int option = JOptionPane.showConfirmDialog(this, message, "Return Confirmation",
                fine > 0 ? JOptionPane.YES_NO_OPTION : JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (fine > 0 && option == JOptionPane.YES_OPTION) {
            try {
                double paid = fineDAO.payFine(t.getTransactionId());
                Dialogs.info(this, "Fine Paid", String.format("Payment of R %.2f recorded.", paid));
            } catch (BusinessRuleException ex) {
                Dialogs.rule(this, "Cannot Record Payment", ex.getMessage());
            } catch (SQLException ex) {
                Dialogs.databaseError(this, "The payment could not be recorded.", ex);
            }
        }
    }

    @Override
    public void refresh() {
        loadActiveBorrows();
    }
}
