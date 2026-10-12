package com.pixelpioneers.library.ui;

import com.pixelpioneers.library.dao.BookDAO;
import com.pixelpioneers.library.dao.BorrowDAO;
import com.pixelpioneers.library.dao.MemberDAO;
import com.pixelpioneers.library.model.Book;
import com.pixelpioneers.library.model.BorrowTransaction;
import com.pixelpioneers.library.model.Member;
import com.pixelpioneers.library.service.LibraryRules;
import com.pixelpioneers.library.util.UIComponents;
import com.pixelpioneers.library.util.UITheme;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Presentation Layer: the Reports Screen (Phase 2, 2.3.7).
 * Four reports: overdue books (with the fine building up so far), all books,
 * all members, and the borrowing history, which can be filtered by member
 * (Phase 1 feature: "view the borrowing history of books by member").
 */
public class ReportsPanel extends JPanel implements DashboardFrame.Refreshable {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final String ALL_MEMBERS = "All members";

    private final BookDAO bookDAO = new BookDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final BorrowDAO borrowDAO = new BorrowDAO();

    private DefaultTableModel overdueModel;
    private DefaultTableModel booksModel;
    private DefaultTableModel membersModel;
    private DefaultTableModel historyModel;
    private final JComboBox<Object> memberFilter = new JComboBox<>();
    private boolean loadingFilter = false;

    public ReportsPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(UITheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        add(UIComponents.sectionTitle("Reports"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UITheme.FONT_LABEL_BOLD);
        tabs.addTab("Overdue Books", buildOverdueTab());
        tabs.addTab("All Books", buildBooksTab());
        tabs.addTab("All Members", buildMembersTab());
        tabs.addTab("Borrowing History", buildHistoryTab());
        add(tabs, BorderLayout.CENTER);

        refresh();
    }

    private JPanel wrapInCard(JTable table) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.CARD_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private DefaultTableModel readOnlyModel(Object... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private JPanel buildOverdueTab() {
        overdueModel = readOnlyModel("Txn ID", "Book", "Member", "Due Date", "Days Overdue", "Fine So Far");
        JTable t = new JTable(overdueModel);
        UIComponents.styleTable(t);
        return wrapInCard(t);
    }

    private JPanel buildBooksTab() {
        booksModel = readOnlyModel("ID", "Title", "Author", "ISBN", "Category", "Status");
        JTable t = new JTable(booksModel);
        UIComponents.styleTable(t);
        return wrapInCard(t);
    }

    private JPanel buildMembersTab() {
        membersModel = readOnlyModel("ID", "Full Name", "Contact Number", "Email");
        JTable t = new JTable(membersModel);
        UIComponents.styleTable(t);
        return wrapInCard(t);
    }

    private JPanel buildHistoryTab() {
        historyModel = readOnlyModel("Txn ID", "Book", "Member", "Borrow Date", "Due Date",
                "Return Date", "Fine", "Fine Status");
        JTable t = new JTable(historyModel);
        UIComponents.styleTable(t);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filterBar.setBackground(UITheme.CARD_BG);
        JLabel label = UIComponents.formLabel("Show history for:");
        memberFilter.setFont(UITheme.FONT_FIELD);
        memberFilter.setForeground(UITheme.TEXT_DARK);
        memberFilter.setBackground(java.awt.Color.WHITE);
        memberFilter.setPreferredSize(new java.awt.Dimension(300, 32));
        memberFilter.addActionListener(e -> {
            if (!loadingFilter) {
                loadHistory();
            }
        });
        filterBar.add(label);
        filterBar.add(memberFilter);

        JPanel panel = wrapInCard(t);
        panel.add(filterBar, BorderLayout.NORTH);
        return panel;
    }

    private void loadHistory() {
        try {
            historyModel.setRowCount(0);
            Object choice = memberFilter.getSelectedItem();
            java.util.List<BorrowTransaction> list = (choice instanceof Member)
                    ? borrowDAO.getTransactionsByMember(((Member) choice).getMemberId())
                    : borrowDAO.getAllTransactions();
            for (BorrowTransaction t : list) {
                historyModel.addRow(new Object[]{
                        t.getTransactionId(), t.getBookTitle(), t.getMemberName(),
                        t.getBorrowDate().toLocalDate().format(DATE_FORMAT),
                        t.getDueDate().toLocalDate().format(DATE_FORMAT),
                        t.getReturnDate() != null ? t.getReturnDate().toLocalDate().format(DATE_FORMAT) : "Not returned",
                        String.format("R %.2f", t.getFineAmount()),
                        t.getFineStatus()
                });
            }
        } catch (SQLException ex) {
            Dialogs.databaseError(this, "The borrowing history could not be loaded.", ex);
        }
    }

    private void loadAllReports() {
        try {
            overdueModel.setRowCount(0);
            LocalDate today = LocalDate.now();
            for (BorrowTransaction t : borrowDAO.getOverdueBorrows()) {
                LocalDate due = t.getDueDate().toLocalDate();
                overdueModel.addRow(new Object[]{
                        t.getTransactionId(), t.getBookTitle(), t.getMemberName(),
                        due.format(DATE_FORMAT), LibraryRules.daysOverdue(due, today),
                        String.format("R %.2f", LibraryRules.calculateFine(due, today))
                });
            }

            booksModel.setRowCount(0);
            for (Book b : bookDAO.getAllBooks()) {
                booksModel.addRow(new Object[]{b.getBookId(), b.getTitle(), b.getAuthor(),
                        b.getIsbn(), b.getCategory(), b.getStatus()});
            }

            membersModel.setRowCount(0);
            java.util.List<Member> members = memberDAO.getAllMembers();
            for (Member m : members) {
                membersModel.addRow(new Object[]{m.getMemberId(), m.getFullName(), m.getContactNumber(), m.getEmail()});
            }

            // rebuild the member filter, keeping the current choice if that member still exists
            Object previous = memberFilter.getSelectedItem();
            loadingFilter = true;
            memberFilter.removeAllItems();
            memberFilter.addItem(ALL_MEMBERS);
            Object toSelect = ALL_MEMBERS;
            for (Member m : members) {
                memberFilter.addItem(m);
                if (previous instanceof Member && ((Member) previous).getMemberId() == m.getMemberId()) {
                    toSelect = m;
                }
            }
            memberFilter.setSelectedItem(toSelect);
            loadingFilter = false;
            loadHistory();
        } catch (SQLException ex) {
            loadingFilter = false;
            Dialogs.databaseError(this, "The reports could not be loaded.", ex);
        }
    }

    @Override
    public void refresh() {
        loadAllReports();
    }
}
