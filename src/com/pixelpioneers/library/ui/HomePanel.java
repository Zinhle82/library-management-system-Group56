package com.pixelpioneers.library.ui;

import com.pixelpioneers.library.dao.BookDAO;
import com.pixelpioneers.library.dao.BorrowDAO;
import com.pixelpioneers.library.dao.MemberDAO;
import com.pixelpioneers.library.model.BorrowTransaction;
import com.pixelpioneers.library.model.LibUser;
import com.pixelpioneers.library.service.LibraryRules;
import com.pixelpioneers.library.util.UIComponents;
import com.pixelpioneers.library.util.UITheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/**
 * Presentation Layer: the Dashboard Screen (Phase 3). Shows summary figures,
 * large icon cards that jump straight to each module (Phase 3 design
 * iteration), and the books that are overdue right now.
 */
public class HomePanel extends JPanel implements DashboardFrame.Refreshable {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final Consumer<String> onNavigate;
    private final JLabel booksValue = new JLabel("0");
    private final JLabel membersValue = new JLabel("0");
    private final JLabel activeValue = new JLabel("0");
    private final JLabel overdueValue = new JLabel("0");
    private DefaultTableModel overdueModel;

    /**
     * @param currentUser the logged-in librarian (used for the greeting)
     * @param onNavigate  called with a screen name (e.g. "BOOKS") when a card is clicked
     */
    public HomePanel(LibUser currentUser, Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
        setLayout(new BorderLayout(0, 18));
        setBackground(UITheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(26, 30, 26, 30));

        String firstName = currentUser.getFullName().trim().split("\\s+")[0];
        JLabel title = new JLabel("Welcome back, " + firstName + "!");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_DARK);
        JLabel subtitle = new JLabel("Here's what's happening in your library today");
        subtitle.setFont(UITheme.FONT_LABEL);
        subtitle.setForeground(UITheme.TEXT_MUTED);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(title);
        header.add(Box.createVerticalStrut(2));
        header.add(subtitle);
        add(header, BorderLayout.NORTH);

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(buildStatRow());
        top.add(Box.createVerticalStrut(18));
        top.add(sectionLabel("Quick Actions"));
        top.add(Box.createVerticalStrut(8));
        top.add(buildActionRow());

        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(top, BorderLayout.NORTH);
        center.add(buildOverdueCard(), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        refresh();
    }

    private JLabel sectionLabel(String text) {
        JLabel label = UIComponents.sectionTitle(text);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JPanel buildStatRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 18, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.add(statCard("book", "Total Books", booksValue, UITheme.ACCENT_TEAL));
        row.add(statCard("people", "Total Members", membersValue, UITheme.ACCENT_PURPLE));
        row.add(statCard("plus", "Active Borrows", activeValue, UITheme.ACCENT_ORANGE));
        row.add(statCard("warning", "Overdue Books", overdueValue, UITheme.DANGER));
        return row;
    }

    private JPanel statCard(String iconKey, String label, JLabel valueLabel, Color accent) {
        JPanel panel = new JPanel(new BorderLayout(14, 0));
        panel.setBackground(UITheme.CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 4, 0, accent),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        panel.add(UIComponents.iconBadge(iconKey, accent, 46), BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
        valueLabel.setForeground(UITheme.TEXT_DARK);
        JLabel caption = new JLabel(label);
        caption.setFont(UITheme.FONT_LABEL);
        caption.setForeground(UITheme.TEXT_MUTED);
        text.add(valueLabel);
        text.add(caption);
        panel.add(text, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActionRow() {
        JPanel row = new JPanel(new GridLayout(1, 6, 14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.add(actionCard("book", "Books", "BOOKS", UITheme.ACCENT_TEAL));
        row.add(actionCard("people", "Members", "MEMBERS", UITheme.ACCENT_PURPLE));
        row.add(actionCard("plus", "Borrow", "BORROW", UITheme.ACCENT_ORANGE));
        row.add(actionCard("back", "Return", "RETURN", UITheme.ACCENT_PINK));
        row.add(actionCard("coin", "Pay Fines", "FINES", UITheme.GOLD));
        row.add(actionCard("chart", "Reports", "REPORTS", UITheme.ACCENT_GREEN));
        return row;
    }

    /** A large clickable card (icon above label) that opens one module. */
    private JPanel actionCard(String iconKey, String label, String screen, Color accent) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UITheme.CARD_BG);
        card.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel badge = UIComponents.iconBadge(iconKey, accent, 44);
        badge.setAlignmentX(CENTER_ALIGNMENT);
        JLabel text = new JLabel(label, SwingConstants.CENTER);
        text.setFont(UITheme.FONT_LABEL_BOLD);
        text.setForeground(UITheme.TEXT_DARK);
        text.setAlignmentX(CENTER_ALIGNMENT);

        card.add(badge);
        card.add(Box.createVerticalStrut(8));
        card.add(text);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { card.setBackground(UITheme.TABLE_ROW_ALT); }
            @Override
            public void mouseExited(MouseEvent e) { card.setBackground(UITheme.CARD_BG); }
            @Override
            public void mouseClicked(MouseEvent e) { onNavigate.accept(screen); }
        });
        return card;
    }

    private JPanel buildOverdueCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UITheme.CARD_BG);
        card.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JLabel header = new JLabel("  Overdue right now");
        header.setFont(UITheme.FONT_LABEL_BOLD);
        header.setForeground(UITheme.DANGER);
        header.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 0));
        card.add(header, BorderLayout.NORTH);

        overdueModel = new DefaultTableModel(
                new Object[]{"Book", "Member", "Due Date", "Days Overdue", "Fine So Far"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(overdueModel);
        UIComponents.styleTable(table);
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        return card;
    }

    @Override
    public void refresh() {
        try {
            booksValue.setText(String.valueOf(new BookDAO().getAllBooks().size()));
            membersValue.setText(String.valueOf(new MemberDAO().getAllMembers().size()));
            activeValue.setText(String.valueOf(new BorrowDAO().getActiveBorrows().size()));

            java.util.List<BorrowTransaction> overdue = new BorrowDAO().getOverdueBorrows();
            overdueValue.setText(String.valueOf(overdue.size()));
            overdueModel.setRowCount(0);
            LocalDate today = LocalDate.now();
            for (BorrowTransaction t : overdue) {
                LocalDate due = t.getDueDate().toLocalDate();
                overdueModel.addRow(new Object[]{
                        t.getBookTitle(), t.getMemberName(), due.format(DATE_FORMAT),
                        LibraryRules.daysOverdue(due, today),
                        String.format("R %.2f", LibraryRules.calculateFine(due, today))
                });
            }
        } catch (SQLException ex) {
            // Leave the previous figures in place if the database can't be read just now.
        }
    }
}
