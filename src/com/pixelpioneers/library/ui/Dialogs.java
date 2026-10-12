package com.pixelpioneers.library.ui;

import com.pixelpioneers.library.service.Validator;
import java.awt.Component;
import java.util.List;
import javax.swing.JOptionPane;

/**
 * Presentation Layer: one place for the pop-up messages, so every screen
 * words and styles its errors the same way.
 */
final class Dialogs {

    private Dialogs() { }

    /** Lists every missing/invalid field at once (Phase 3: "detailed error messages"). */
    static void validation(Component parent, List<String> problems) {
        JOptionPane.showMessageDialog(parent, Validator.formatProblems(problems),
                "Please Check Your Input", JOptionPane.WARNING_MESSAGE);
    }

    /** A business rule stopped the action (not a fault; the message explains why). */
    static void rule(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE);
    }

    /** An unexpected database problem. */
    static void databaseError(Component parent, String what, Exception ex) {
        JOptionPane.showMessageDialog(parent,
                what + "\n\nDetails: " + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    static void info(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }
}
