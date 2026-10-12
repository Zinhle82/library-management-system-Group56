package com.pixelpioneers.library;

import com.pixelpioneers.library.db.DBConnection;
import com.pixelpioneers.library.ui.LoginFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Program entry point: checks the database is reachable, then opens the Login screen. */
public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to the default look and feel if the system one isn't available.
        }

        SwingUtilities.invokeLater(() -> {
            try {
                DBConnection.getConnection();
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(null,
                        ex.getMessage() + "\n\nThe program will now close.",
                        "Cannot Start - Database Problem", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            new LoginFrame().setVisible(true);
        });
    }
}
