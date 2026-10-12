package com.pixelpioneers.library.ui;

import com.pixelpioneers.library.dao.BookDAO;
import com.pixelpioneers.library.dao.BorrowDAO;
import com.pixelpioneers.library.dao.MemberDAO;
import com.pixelpioneers.library.model.Book;
import com.pixelpioneers.library.model.BorrowTransaction;
import com.pixelpioneers.library.model.Member;
import com.pixelpioneers.library.service.BusinessRuleException;
import com.pixelpioneers.library.service.LibraryRules;
import com.pixelpioneers.library.util.RoundedButton;
import com.pixelpioneers.library.util.UIComponents;
import com.pixelpioneers.library.util.UITheme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Presentation Layer: the Borrow Book Screen (Phase 2, 2.3.4).
 * The librarian picks an available book and a member; the system shows at once
 * whether that member is allowed to borrow, and sets the due date automatically.
 */
public class BorrowBookPanel extends JPanel implements DashboardFrame.Refreshable {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final BookDAO bookDAO = new BookDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final BorrowDAO borrowDAO = new BorrowDAO();

    private final JComboBox<Book> bookCombo = new JComboBox<>();
    private final JComboBox<Member> memberCombo = new JComboBox<>();
    private final JLabel eligibilityLabel = new JLabel(" ");
    private final JLabel dueDateValue = new JLabel("-");
    private RoundedButton borrowButton;
    private DefaultTableModel modelRef;
    private boolean loading = false;

    public BorrowBookPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(UITheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        add(UIComponents.sectionTitle("Borrow Book"), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 20));
        center.setOpaque(false);
        center.add(buildBorrowForm(), BorderLayout.NORTH);
        center.add(buildActiveBorrowsTable(), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        refresh();
    }

    private JPanel buildBorrowForm() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(UITheme.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 4, 0, UITheme.ACCENT_ORANGE),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 10, 6, 10);
        gc.anchor = GridBagConstraints.WEST;

        styleCombo(bookCombo);
        styleCombo(memberCombo);

        gc.gridx = 0; gc.gridy = 0;
        card.add(UIComponents.formLabel("Select Book"), gc);
        gc.gridx = 1;
        card.add(bookCombo, gc);

        gc.gridx = 0; gc.gridy = 1;
        card.add(UIComponents.formLabel("Select Member"), gc);
        gc.gridx = 1;
        card.add(memberCombo, gc);

        gc.gridx = 0; gc.gridy = 2;
        card.add(UIComponents.formLabel("Member Status"), gc);
        gc.gridx = 1;
        eligibilityLabel.setFont(UITheme.FONT_LABEL_BOLD);
        card.add(eligibilityLabel, gc);

        gc.gridx = 0; gc.gridy = 3;
        card.add(UIComponents.formLabel("Due Date"), gc);
        gc.gridx = 1;
        dueDateValue.setFont(UITheme.FONT_LABEL_BOLD);
        dueDateValue.setForeground(UITheme.TEXT_DARK);
        card.add(dueDateValue, gc);

        borrowButton = new RoundedButton("Confirm Borrow", UITheme.SUCCESS, UITheme.SUCCESS_DARK);
        gc.gridx = 1; gc.gridy = 4;
        gc.insets = new Insets(16, 10, 6, 10);
        card.add(borrowButton, gc);

        borrowButton.addActionListener(e -> confirmBorrow());
        memberCombo.addActionListener(e -> updateEligibility());
        bookCombo.addActionListener(e -> updateEligibility());

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setFont(UITheme.FONT_FIELD);
        combo.setForeground(UITheme.TEXT_DARK);
        combo.setBackground(java.awt.Color.WHITE);
        combo.setPreferredSize(new Dimension(420, 34));
    }

    private JPanel buildActiveBorrowsTable() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.CARD_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JLabel header = new JLabel("  Currently Borrowed Books");
        header.setFont(UITheme.FONT_LABEL_BOLD);
        header.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 0));
        panel.add(header, BorderLayout.NORTH);

        modelRef = new DefaultTableModel(
                new Object[]{"Txn ID", "Book", "Member", "Borrow Date", "Due Date", "Status"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(modelRef);
        UIComponents.styleTable(table);
        table.getColumnModel().getColumn(0).setMaxWidth(70);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void loadCombos() {
        loading = true;
        try {
            bookCombo.removeAllItems();
            for (Book b : bookDAO.getAvailableBooks()) bookCombo.addItem(b);
            memberCombo.removeAllItems();
            for (Member m : memberDAO.getAllMembers()) memberCombo.addItem(m);
        } catch (SQLException ex) {
            Dialogs.databaseError(this, "The book and member lists could not be loaded.", ex);
        } finally {
            loading = false;
        }
        dueDateValue.setText(LibraryRules.dueDateFor(LocalDate.now()).format(DATE_FORMAT)
                + "   (" + LibraryRules.LOAN_PERIOD_DAYS + "-day loan, set automatically)");
        updateEligibility();
    }

    /** Shows right away whether the chosen member may borrow, and enables/disables the button. */
    private void updateEligibility() {
        if (loading) {
            return;
        }
        Member member = (Member) memberCombo.getSelectedItem();
        Book book = (Book) bookCombo.getSelectedItem();

        if (book == null) {
            eligibilityLabel.setForeground(UITheme.WARNING);
            eligibilityLabel.setText("No books are currently available to borrow.");
            borrowButton.setEnabled(false);
            return;
        }
        if (member == null) {
            eligibilityLabel.setForeground(UITheme.WARNING);
            eligibilityLabel.setText("Register a member first (Member Management).");
            borrowButton.setEnabled(false);
            return;
        }
        try {
            String reason = borrowDAO.checkBorrowEligibility(member.getMemberId());
            if (reason == null) {
                eligibilityLabel.setForeground(UITheme.SUCCESS_DARK);
                eligibilityLabel.setText("Eligible to borrow");
            } else {
                eligibilityLabel.setForeground(UITheme.DANGER);
                eligibilityLabel.setText("Cannot borrow: " + reason);
            }
            borrowButton.setEnabled(reason == null);
        } catch (SQLException ex) {
            eligibilityLabel.setForeground(UITheme.DANGER);
            eligibilityLabel.setText("Could not check eligibility (database problem).");
            borrowButton.setEnabled(false);
        }
    }

    private void loadActiveBorrows() {
        try {
            modelRef.setRowCount(0);
            LocalDate today = LocalDate.now();
            for (BorrowTransaction t : borrowDAO.getActiveBorrows()) {
                long late = LibraryRules.daysOverdue(t.getDueDate().toLocalDate(), today);
                modelRef.addRow(new Object[]{
                        t.getTransactionId(), t.getBookTitle(), t.getMemberName(),
                        t.getBorrowDate().toLocalDate().format(DATE_FORMAT),
                        t.getDueDate().toLocalDate().format(DATE_FORMAT),
                        late > 0 ? "OVERDUE by " + late + (late == 1 ? " day" : " days") : "On loan"
                });
            }
        } catch (SQLException ex) {
            Dialogs.databaseError(this, "The borrowing records could not be loaded.", ex);
        }
    }

    private void confirmBorrow() {
        Book book = (Book) bookCombo.getSelectedItem();
        Member member = (Member) memberCombo.getSelectedItem();
        if (book == null || member == null) {
            Dialogs.rule(this, "Cannot Borrow", "Choose a book and a member first.");
            return;
        }
        try {
            borrowDAO.borrowBook(book.getBookId(), member.getMemberId());
            Dialogs.info(this, "Book Borrowed", "\"" + book.getTitle() + "\" was borrowed by "
                    + member.getFullName() + ".\nDue back on "
                    + LibraryRules.dueDateFor(LocalDate.now()).format(DATE_FORMAT) + ".");
        } catch (BusinessRuleException ex) {
            Dialogs.rule(this, "Cannot Borrow", ex.getMessage());
        } catch (SQLException ex) {
            Dialogs.databaseError(this, "The borrow could not be recorded.", ex);
        }
        refresh();
    }

    @Override
    public void refresh() {
        loadCombos();
        loadActiveBorrows();
    }
}
