package com.pixelpioneers.library.ui;

import com.pixelpioneers.library.dao.MemberDAO;
import com.pixelpioneers.library.model.Member;
import com.pixelpioneers.library.service.Validator;
import com.pixelpioneers.library.util.RoundedButton;
import com.pixelpioneers.library.util.UIComponents;
import com.pixelpioneers.library.util.UITheme;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/** Presentation Layer: register, update, delete and search members (Phase 3 Member Management Screen). */
public class MemberManagementPanel extends JPanel implements DashboardFrame.Refreshable {

    private final MemberDAO memberDAO = new MemberDAO();
    private DefaultTableModel modelRef;
    private JTable tableRef;

    private final javax.swing.JTextField searchField = UIComponents.styledField(18);
    private final javax.swing.JTextField nameField = UIComponents.styledField(18);
    private final javax.swing.JTextField contactField = UIComponents.styledField(18);
    private final javax.swing.JTextField emailField = UIComponents.styledField(18);

    private Integer selectedMemberId = null;

    public MemberManagementPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(UITheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        add(buildHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(20, 0));
        center.setOpaque(false);
        center.add(buildTablePanel(), BorderLayout.CENTER);
        center.add(buildFormPanel(), BorderLayout.EAST);
        add(center, BorderLayout.CENTER);

        loadMembers(memberDAO::getAllMembers);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIComponents.sectionTitle("Member Management"), BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchPanel.setOpaque(false);
        searchField.setToolTipText("Search by name, email, or contact number");
        RoundedButton searchButton = new RoundedButton("Search", UITheme.ACCENT_PURPLE, UITheme.ACCENT_PURPLE.darker());
        RoundedButton clearSearchButton = new RoundedButton("Show All", UITheme.TEXT_MUTED, UITheme.TEXT_DARK);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(clearSearchButton);
        header.add(searchPanel, BorderLayout.EAST);

        searchButton.addActionListener(e -> {
            String kw = searchField.getText().trim();
            if (kw.isEmpty()) loadMembers(memberDAO::getAllMembers);
            else loadMembers(() -> memberDAO.searchMembers(kw));
        });
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            loadMembers(memberDAO::getAllMembers);
        });

        return header;
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.CARD_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        modelRef = new DefaultTableModel(new Object[]{"ID", "Full Name", "Contact Number", "Email"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tableRef = new JTable(modelRef);
        UIComponents.styleTable(tableRef);
        tableRef.getColumnModel().getColumn(0).setMaxWidth(50);

        tableRef.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tableRef.getSelectedRow() != -1) populateFormFromSelection();
        });

        panel.add(new JScrollPane(tableRef), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildFormPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(UITheme.CARD_BG);
        outer.setPreferredSize(new java.awt.Dimension(300, 0));
        outer.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(4, 0, 4, 0);

        gc.gridy = 0;
        gc.insets = new Insets(0, 0, 16, 0);
        form.add(UIComponents.sectionTitle("Member Details"), gc);

        addField(form, gc, 1, "Full Name", nameField);
        addField(form, gc, 3, "Contact Number", contactField);
        addField(form, gc, 5, "Email", emailField);

        JPanel buttonRow = new JPanel(new GridLayout(1, 2, 8, 0));
        buttonRow.setOpaque(false);
        RoundedButton saveButton = new RoundedButton("Save", UITheme.SUCCESS, UITheme.SUCCESS_DARK);
        RoundedButton clearButton = new RoundedButton("Clear", UITheme.TEXT_MUTED, UITheme.TEXT_DARK);
        buttonRow.add(saveButton);
        buttonRow.add(clearButton);
        gc.gridy = 7;
        gc.insets = new Insets(16, 0, 8, 0);
        form.add(buttonRow, gc);

        RoundedButton deleteButton = new RoundedButton("Delete Selected", UITheme.DANGER, UITheme.DANGER_DARK);
        deleteButton.setHorizontalAlignment(SwingConstants.CENTER);
        gc.gridy = 8;
        gc.insets = new Insets(4, 0, 0, 0);
        form.add(deleteButton, gc);

        saveButton.addActionListener(e -> saveMember());
        clearButton.addActionListener(e -> clearForm());
        deleteButton.addActionListener(e -> deleteSelectedMember());

        outer.add(form, BorderLayout.NORTH);
        return outer;
    }

    private void addField(JPanel form, GridBagConstraints gc, int startY, String label, javax.swing.JTextField field) {
        gc.gridy = startY;
        gc.insets = new Insets(0, 0, 4, 0);
        form.add(UIComponents.formLabel(label), gc);
        gc.gridy = startY + 1;
        gc.insets = new Insets(0, 0, 10, 0);
        form.add(field, gc);
    }

    private void populateFormFromSelection() {
        int row = tableRef.getSelectedRow();
        selectedMemberId = (Integer) modelRef.getValueAt(row, 0);
        nameField.setText((String) modelRef.getValueAt(row, 1));
        contactField.setText((String) modelRef.getValueAt(row, 2));
        emailField.setText((String) modelRef.getValueAt(row, 3));
    }

    private void saveMember() {
        String name = nameField.getText().trim();
        String contact = contactField.getText().trim();
        String email = emailField.getText().trim();

        List<String> problems = Validator.validateMember(name, contact, email);
        if (!problems.isEmpty()) {
            Dialogs.validation(this, problems);
            return;
        }
        contact = Validator.normalizePhone(contact);

        try {
            if (!email.isEmpty() && memberDAO.emailExists(email, selectedMemberId)) {
                Dialogs.validation(this, java.util.Collections.singletonList(
                        "Another member already uses the email " + email + "."));
                return;
            }
            Member member = new Member();
            member.setFullName(name);
            member.setContactNumber(contact);
            member.setEmail(email);

            if (selectedMemberId == null) {
                memberDAO.addMember(member);
                JOptionPane.showMessageDialog(this, "Member registered successfully.");
            } else {
                member.setMemberId(selectedMemberId);
                memberDAO.updateMember(member);
                JOptionPane.showMessageDialog(this, "Member updated successfully.");
            }
            clearForm();
            loadMembers(memberDAO::getAllMembers);
        } catch (Exception ex) {
            Dialogs.databaseError(this, "The member could not be saved.", ex);
        }
    }

    private void deleteSelectedMember() {
        if (selectedMemberId == null) {
            JOptionPane.showMessageDialog(this, "Select a member from the table first.",
                    "No Member Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this member record?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                memberDAO.deleteMember(selectedMemberId);
                clearForm();
                loadMembers(memberDAO::getAllMembers);
            } catch (java.sql.SQLIntegrityConstraintViolationException ex) {
                JOptionPane.showMessageDialog(this,
                        "This member can't be deleted because they have borrowing history on record "
                                + "(past or current). Members are kept once they've borrowed something, "
                                + "to preserve the library's records.",
                        "Cannot Delete Member", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error deleting member: " + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearForm() {
        selectedMemberId = null;
        nameField.setText("");
        contactField.setText("");
        emailField.setText("");
        if (tableRef != null) tableRef.clearSelection();
    }

    private interface MemberSupplier {
        List<Member> get() throws Exception;
    }

    private void loadMembers(MemberSupplier supplier) {
        try {
            modelRef.setRowCount(0);
            for (Member m : supplier.get()) {
                modelRef.addRow(new Object[]{m.getMemberId(), m.getFullName(), m.getContactNumber(), m.getEmail()});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading members: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refresh() {
        loadMembers(memberDAO::getAllMembers);
    }
}
