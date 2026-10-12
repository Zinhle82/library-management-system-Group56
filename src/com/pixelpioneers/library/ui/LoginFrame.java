package com.pixelpioneers.library.ui;

import com.pixelpioneers.library.dao.UserDAO;
import com.pixelpioneers.library.model.LibUser;
import com.pixelpioneers.library.util.RoundedButton;
import com.pixelpioneers.library.util.UIComponents;
import com.pixelpioneers.library.util.UITheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/** Presentation Layer: the Login Screen (Phase 3). Checks credentials through UserDAO and opens the dashboard. */
public class LoginFrame extends JFrame {

    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JLabel errorLabel;

    public LoginFrame() {
        setTitle("Library Management System - Login");
        setSize(950, 580);
        setMinimumSize(new Dimension(850, 520));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Left brand panel - gradient-style colorful block
        JPanel leftPanel = new GradientPanel(UITheme.ACCENT_PURPLE, UITheme.ACCENT_BLUE);
        leftPanel.setLayout(new GridBagLayout());
        leftPanel.setPreferredSize(new Dimension(380, 0));
        JPanel leftContent = new JPanel();
        leftContent.setOpaque(false);
        leftContent.setLayout(new javax.swing.BoxLayout(leftContent, javax.swing.BoxLayout.Y_AXIS));

        JPanel logo = com.pixelpioneers.library.util.UIComponents.iconBadge("book", new Color(255, 255, 255, 60), 100);
        logo.setAlignmentX(JLabel.CENTER_ALIGNMENT);

        JLabel brand = new JLabel("Pixel Pioneers");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 28));
        brand.setForeground(Color.WHITE);
        brand.setAlignmentX(JLabel.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Library Management System");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(new Color(0xE8E4FF));
        sub.setAlignmentX(JLabel.CENTER_ALIGNMENT);

        leftContent.add(logo);
        leftContent.add(javax.swing.Box.createVerticalStrut(16));
        leftContent.add(brand);
        leftContent.add(javax.swing.Box.createVerticalStrut(6));
        leftContent.add(sub);
        leftPanel.add(leftContent);

        // Right login form panel
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(UITheme.BACKGROUND);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(6, 0, 6, 0);

        JLabel title = new JLabel("Welcome Back");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_DARK);
        gc.gridy = 0;
        gc.insets = new Insets(0, 0, 4, 0);
        form.add(title, gc);

        JLabel subtitle = new JLabel("Sign in to manage the library");
        subtitle.setFont(UITheme.FONT_LABEL);
        subtitle.setForeground(UITheme.TEXT_MUTED);
        gc.gridy = 1;
        gc.insets = new Insets(0, 0, 26, 0);
        form.add(subtitle, gc);

        JLabel userLabel = UIComponents.formLabel("Username");
        gc.gridy = 2;
        gc.insets = new Insets(0, 0, 4, 0);
        form.add(userLabel, gc);

        usernameField = UIComponents.styledField(20);
        gc.gridy = 3;
        gc.insets = new Insets(0, 0, 16, 0);
        form.add(usernameField, gc);

        JLabel passLabel = UIComponents.formLabel("Password");
        gc.gridy = 4;
        gc.insets = new Insets(0, 0, 4, 0);
        form.add(passLabel, gc);

        passwordField = new JPasswordField(20);
        passwordField.setFont(UITheme.FONT_FIELD);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        gc.gridy = 5;
        gc.insets = new Insets(0, 0, 6, 0);
        form.add(passwordField, gc);

        errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_LABEL);
        errorLabel.setForeground(UITheme.DANGER);
        gc.gridy = 6;
        gc.insets = new Insets(0, 0, 12, 0);
        form.add(errorLabel, gc);

        RoundedButton loginButton = new RoundedButton("Log In", UITheme.PRIMARY, UITheme.PRIMARY_DARK);
        loginButton.setHorizontalAlignment(SwingConstants.CENTER);
        gc.gridy = 7;
        gc.insets = new Insets(4, 0, 0, 0);
        form.add(loginButton, gc);

        JLabel registerPrompt = new JLabel("New here? Create an account", SwingConstants.CENTER);
        registerPrompt.setFont(new Font("Segoe UI", Font.BOLD, 13));
        registerPrompt.setForeground(UITheme.ACCENT_PURPLE);
        registerPrompt.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerPrompt.setHorizontalAlignment(SwingConstants.CENTER);
        gc.gridy = 8;
        gc.insets = new Insets(18, 0, 0, 0);
        form.add(registerPrompt, gc);

        registerPrompt.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
                SwingUtilities.invokeLater(() -> new RegisterFrame().setVisible(true));
            }
        });

        rightPanel.add(form);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);

        loginButton.addActionListener(e -> attemptLogin());
        passwordField.addActionListener(e -> attemptLogin());
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both username and password.");
            return;
        }

        try {
            UserDAO userDAO = new UserDAO();
            LibUser user = userDAO.authenticate(username, password);
            if (user != null) {
                errorLabel.setText(" ");
                dispose();
                SwingUtilities.invokeLater(() -> new DashboardFrame(user).setVisible(true));
            } else {
                errorLabel.setText("Incorrect username or password.");
            }
        } catch (Exception ex) {
            errorLabel.setText("Database error: " + ex.getMessage());
        }
    }

    /** Simple diagonal two-color gradient panel used for the brand side of Login/Register. */
    static class GradientPanel extends JPanel {
        private final Color from, to;
        GradientPanel(Color from, Color to) {
            this.from = from;
            this.to = to;
        }
        @Override
        protected void paintComponent(java.awt.Graphics g) {
            // NOTE: deliberately does NOT call super.paintComponent(g) here.
            // This panel is opaque, and calling super afterwards would repaint
            // the plain default background color over our gradient, hiding it.
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            java.awt.GradientPaint gp = new java.awt.GradientPaint(0, 0, from, getWidth(), getHeight(), to);
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }
}
