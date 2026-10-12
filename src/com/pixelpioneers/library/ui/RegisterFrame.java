package com.pixelpioneers.library.ui;

import com.pixelpioneers.library.dao.UserDAO;
import com.pixelpioneers.library.model.LibUser;
import com.pixelpioneers.library.service.Validator;
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
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/** Presentation Layer: creates a new librarian account (validated, password stored as a salted hash) and logs the person straight in. */
public class RegisterFrame extends JFrame {

    private final JTextField fullNameField;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JPasswordField confirmField;
    private final JLabel errorLabel;

    public RegisterFrame() {
        setTitle("Library Management System - Create Account");
        setSize(950, 640);
        setMinimumSize(new Dimension(850, 580));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel leftPanel = new LoginFrame.GradientPanel(UITheme.ACCENT_TEAL, UITheme.ACCENT_GREEN);
        leftPanel.setLayout(new GridBagLayout());
        leftPanel.setPreferredSize(new Dimension(380, 0));
        JPanel leftContent = new JPanel();
        leftContent.setOpaque(false);
        leftContent.setLayout(new javax.swing.BoxLayout(leftContent, javax.swing.BoxLayout.Y_AXIS));

        JPanel logo = com.pixelpioneers.library.util.UIComponents.iconBadge("people", new Color(255, 255, 255, 60), 100);
        logo.setAlignmentX(JLabel.CENTER_ALIGNMENT);

        JLabel brand = new JLabel("Join the Team");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 28));
        brand.setForeground(Color.WHITE);
        brand.setAlignmentX(JLabel.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Create your librarian account");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(new Color(0xE4FFF6));
        sub.setAlignmentX(JLabel.CENTER_ALIGNMENT);

        leftContent.add(logo);
        leftContent.add(javax.swing.Box.createVerticalStrut(16));
        leftContent.add(brand);
        leftContent.add(javax.swing.Box.createVerticalStrut(6));
        leftContent.add(sub);
        leftPanel.add(leftContent);

        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(UITheme.BACKGROUND);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(6, 0, 6, 0);

        JLabel title = new JLabel("Create Account");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_DARK);
        gc.gridy = 0;
        gc.insets = new Insets(0, 0, 20, 0);
        form.add(title, gc);

        fullNameField = UIComponents.styledField(20);
        usernameField = UIComponents.styledField(20);
        passwordField = new JPasswordField(20);
        confirmField = new JPasswordField(20);
        stylePasswordField(passwordField);
        stylePasswordField(confirmField);

        addField(form, gc, 1, "Full Name", fullNameField);
        addField(form, gc, 3, "Choose a Username", usernameField);
        addField(form, gc, 5, "Choose a Password", passwordField);
        addField(form, gc, 7, "Confirm Password", confirmField);

        errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_LABEL);
        errorLabel.setForeground(UITheme.DANGER);
        gc.gridy = 9;
        gc.insets = new Insets(0, 0, 12, 0);
        form.add(errorLabel, gc);

        RoundedButton registerButton = new RoundedButton("Create Account & Log In", UITheme.SUCCESS, UITheme.SUCCESS_DARK);
        registerButton.setHorizontalAlignment(SwingConstants.CENTER);
        gc.gridy = 10;
        gc.insets = new Insets(4, 0, 0, 0);
        form.add(registerButton, gc);

        JLabel loginPrompt = new JLabel("Already have an account? Log in", SwingConstants.CENTER);
        loginPrompt.setFont(new Font("Segoe UI", Font.BOLD, 13));
        loginPrompt.setForeground(UITheme.ACCENT_TEAL);
        loginPrompt.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginPrompt.setHorizontalAlignment(SwingConstants.CENTER);
        gc.gridy = 11;
        gc.insets = new Insets(18, 0, 0, 0);
        form.add(loginPrompt, gc);

        loginPrompt.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
                SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
            }
        });

        registerButton.addActionListener(e -> attemptRegister());

        rightPanel.add(form);
        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
    }

    private void stylePasswordField(JPasswordField field) {
        field.setFont(UITheme.FONT_FIELD);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
    }

    private void addField(JPanel form, GridBagConstraints gc, int startY, String label, javax.swing.JComponent field) {
        gc.gridy = startY;
        gc.insets = new Insets(0, 0, 4, 0);
        form.add(UIComponents.formLabel(label), gc);
        gc.gridy = startY + 1;
        gc.insets = new Insets(0, 0, 10, 0);
        form.add(field, gc);
    }

    private void attemptRegister() {
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());

        List<String> problems = Validator.validateRegistration(fullName, username, password, confirm);
        if (!problems.isEmpty()) {
            Dialogs.validation(this, problems);
            return;
        }

        try {
            UserDAO userDAO = new UserDAO();
            if (userDAO.usernameExists(username)) {
                errorLabel.setText("That username is already taken - choose another.");
                return;
            }
            LibUser newUser = userDAO.register(username, password, fullName);
            errorLabel.setText(" ");
            dispose();
            SwingUtilities.invokeLater(() -> new DashboardFrame(newUser).setVisible(true));
        } catch (Exception ex) {
            errorLabel.setText("Database error: " + ex.getMessage());
        }
    }
}
