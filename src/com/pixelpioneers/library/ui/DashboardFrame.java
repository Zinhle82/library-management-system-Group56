package com.pixelpioneers.library.ui;

import com.pixelpioneers.library.model.LibUser;
import com.pixelpioneers.library.util.UIComponents;
import com.pixelpioneers.library.util.UITheme;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Presentation Layer: the main window shown after login. A dark sidebar
 * switches between the screens (each is a "card" in a CardLayout), and the
 * top bar greets whoever is logged in.
 */
public class DashboardFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final LibUser currentUser;
    private final Map<String, JPanel> navRows = new HashMap<>();
    private JPanel activeNavRow;

    public DashboardFrame(LibUser currentUser) {
        this.currentUser = currentUser;

        setTitle("Library Management System - Dashboard");
        setSize(1260, 780);
        setMinimumSize(new Dimension(1060, 700));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(buildSidebar(), BorderLayout.WEST);
        add(buildTopBar(), BorderLayout.NORTH);

        contentPanel.setBackground(UITheme.BACKGROUND);
        contentPanel.add(new HomePanel(currentUser, this::navigateTo), "HOME");
        contentPanel.add(new BookManagementPanel(), "BOOKS");
        contentPanel.add(new MemberManagementPanel(), "MEMBERS");
        contentPanel.add(new BorrowBookPanel(), "BORROW");
        contentPanel.add(new ReturnBookPanel(), "RETURN");
        contentPanel.add(new FinesPanel(), "FINES");
        contentPanel.add(new ReportsPanel(), "REPORTS");
        add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UITheme.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        JPanel brandRow = new JPanel(new BorderLayout(10, 0));
        brandRow.setOpaque(false);
        brandRow.setBorder(BorderFactory.createEmptyBorder(0, 14, 26, 0));
        brandRow.setAlignmentX(LEFT_ALIGNMENT);
        brandRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        JLabel logoLabel = new JLabel("Pixel Pioneers");
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        logoLabel.setForeground(Color.WHITE);
        brandRow.add(UIComponents.iconBadge("book", UITheme.ACCENT_BLUE, 30), BorderLayout.WEST);
        brandRow.add(logoLabel, BorderLayout.CENTER);
        sidebar.add(brandRow);

        sidebar.add(navRow("home", "Dashboard", "HOME", UITheme.ACCENT_BLUE));
        sidebar.add(navRow("book", "Book Management", "BOOKS", UITheme.ACCENT_TEAL));
        sidebar.add(navRow("people", "Member Management", "MEMBERS", UITheme.ACCENT_PURPLE));
        sidebar.add(navRow("plus", "Borrow Book", "BORROW", UITheme.ACCENT_ORANGE));
        sidebar.add(navRow("back", "Return Book", "RETURN", UITheme.ACCENT_PINK));
        sidebar.add(navRow("coin", "Pay Fines", "FINES", UITheme.GOLD));
        sidebar.add(navRow("chart", "Reports", "REPORTS", UITheme.ACCENT_GREEN));

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(navRow("back", "Log Out", "LOGOUT", UITheme.DANGER));

        // The Dashboard starts as the highlighted screen.
        activeNavRow = navRows.get("HOME");
        activeNavRow.setBackground(UITheme.SIDEBAR_HOVER);
        return sidebar;
    }

    private JPanel navRow(String iconKey, String text, String screen, Color accent) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 10));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        row.setOpaque(true);
        row.setBackground(UITheme.SIDEBAR_BG);
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel label = new JLabel(text);
        label.setFont(UITheme.FONT_LABEL_BOLD);
        label.setForeground(Color.WHITE);
        row.add(UIComponents.iconBadge(iconKey, accent, 32), BorderLayout.WEST);
        row.add(label, BorderLayout.CENTER);
        navRows.put(screen, row);

        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (row != activeNavRow) row.setBackground(UITheme.SIDEBAR_HOVER);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (row != activeNavRow) row.setBackground(UITheme.SIDEBAR_BG);
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                if ("LOGOUT".equals(screen)) {
                    dispose();
                    new LoginFrame().setVisible(true);
                } else {
                    navigateTo(screen);
                }
            }
        });
        return row;
    }

    /** Shows a screen, highlights its sidebar entry, and reloads its data so it is always current. */
    private void navigateTo(String screen) {
        JPanel row = navRows.get(screen);
        if (activeNavRow != null) {
            activeNavRow.setBackground(UITheme.SIDEBAR_BG);
        }
        if (row != null) {
            row.setBackground(UITheme.SIDEBAR_HOVER);
            activeNavRow = row;
        }
        cardLayout.show(contentPanel, screen);
        for (Component c : contentPanel.getComponents()) {
            if (c instanceof Refreshable && c.isVisible()) {
                ((Refreshable) c).refresh();
            }
        }
    }

    private JPanel buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER));
        topBar.setPreferredSize(new Dimension(0, 60));

        JPanel welcomeWrap = new JPanel(new BorderLayout(10, 0));
        welcomeWrap.setOpaque(false);
        welcomeWrap.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));

        JLabel welcome = new JLabel("Welcome, " + currentUser.getFullName() + "!");
        welcome.setFont(UITheme.FONT_LABEL_BOLD);
        welcome.setForeground(UITheme.TEXT_DARK);

        welcomeWrap.add(UIComponents.iconBadge("person", UITheme.ACCENT_BLUE, 36), BorderLayout.WEST);
        welcomeWrap.add(welcome, BorderLayout.CENTER);
        topBar.add(welcomeWrap, BorderLayout.WEST);
        return topBar;
    }

    /** Implemented by every screen so its data is reloaded each time the librarian navigates to it. */
    public interface Refreshable {
        void refresh();
    }
}
