package com.pixelpioneers.library.ui;

import com.pixelpioneers.library.dao.BookDAO;
import com.pixelpioneers.library.model.Book;
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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/** Presentation Layer: add, update, delete and search books (Phase 3 Book Management Screen). */
public class BookManagementPanel extends JPanel implements DashboardFrame.Refreshable {

    private final BookDAO bookDAO = new BookDAO();
    private DefaultTableModel modelRef;
    private JTable tableRef;

    private final javax.swing.JTextField searchField = UIComponents.styledField(18);
    private final javax.swing.JTextField titleField = UIComponents.styledField(18);
    private final javax.swing.JTextField authorField = UIComponents.styledField(18);
    private final javax.swing.JTextField isbnField = UIComponents.styledField(18);
    private final javax.swing.JTextField categoryField = UIComponents.styledField(18);

    private Integer selectedBookId = null;

    public BookManagementPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(UITheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        add(buildHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(20, 0));
        center.setOpaque(false);
        center.add(buildTablePanel(), BorderLayout.CENTER);
        center.add(buildFormPanel(), BorderLayout.EAST);
        add(center, BorderLayout.CENTER);

        loadBooks(bookDAO::getAllBooks);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIComponents.sectionTitle("Book Management"), BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchPanel.setOpaque(false);
        searchField.setToolTipText("Search by title, author, or ISBN");
        RoundedButton searchButton = new RoundedButton("Search", UITheme.ACCENT_TEAL, UITheme.ACCENT_TEAL.darker());
        RoundedButton clearSearchButton = new RoundedButton("Show All", UITheme.TEXT_MUTED, UITheme.TEXT_DARK);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(clearSearchButton);
        header.add(searchPanel, BorderLayout.EAST);

        searchButton.addActionListener(e -> {
            String kw = searchField.getText().trim();
            if (kw.isEmpty()) loadBooks(bookDAO::getAllBooks);
            else loadBooks(() -> bookDAO.searchBooks(kw));
        });
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            loadBooks(bookDAO::getAllBooks);
        });

        return header;
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.CARD_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        modelRef = new DefaultTableModel(new Object[]{"ID", "Title", "Author", "ISBN", "Category", "Status"}, 0) {
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
        form.add(UIComponents.sectionTitle("Book Details"), gc);

        addField(form, gc, 1, "Title", titleField);
        addField(form, gc, 3, "Author", authorField);
        addField(form, gc, 5, "ISBN", isbnField);
        addField(form, gc, 7, "Category", categoryField);

        JPanel buttonRow = new JPanel(new GridLayout(1, 2, 8, 0));
        buttonRow.setOpaque(false);
        RoundedButton saveButton = new RoundedButton("Save", UITheme.SUCCESS, UITheme.SUCCESS_DARK);
        RoundedButton clearButton = new RoundedButton("Clear", UITheme.TEXT_MUTED, UITheme.TEXT_DARK);
        buttonRow.add(saveButton);
        buttonRow.add(clearButton);
        gc.gridy = 9;
        gc.insets = new Insets(16, 0, 8, 0);
        form.add(buttonRow, gc);

        RoundedButton deleteButton = new RoundedButton("Delete Selected", UITheme.DANGER, UITheme.DANGER_DARK);
        deleteButton.setHorizontalAlignment(SwingConstants.CENTER);
        gc.gridy = 10;
        gc.insets = new Insets(4, 0, 0, 0);
        form.add(deleteButton, gc);

        saveButton.addActionListener(e -> saveBook());
        clearButton.addActionListener(e -> clearForm());
        deleteButton.addActionListener(e -> deleteSelectedBook());

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
        selectedBookId = (Integer) modelRef.getValueAt(row, 0);
        titleField.setText((String) modelRef.getValueAt(row, 1));
        authorField.setText((String) modelRef.getValueAt(row, 2));
        isbnField.setText((String) modelRef.getValueAt(row, 3));
        categoryField.setText((String) modelRef.getValueAt(row, 4));
    }

    private void saveBook() {
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String isbn = isbnField.getText().trim();
        String category = categoryField.getText().trim();

        List<String> problems = Validator.validateBook(title, author, isbn, category);
        if (!problems.isEmpty()) {
            Dialogs.validation(this, problems);
            return;
        }
        isbn = Validator.normalizeIsbn(isbn);

        try {
            if (bookDAO.isbnExists(isbn, selectedBookId)) {
                Dialogs.validation(this, java.util.Collections.singletonList(
                        "A book with ISBN " + isbn + " already exists. Each book needs a unique ISBN."));
                return;
            }
            Book book = new Book();
            book.setTitle(title);
            book.setAuthor(author);
            book.setIsbn(isbn);
            book.setCategory(category);

            if (selectedBookId == null) {
                bookDAO.addBook(book);
                JOptionPane.showMessageDialog(this, "Book added successfully.");
            } else {
                book.setBookId(selectedBookId);
                bookDAO.updateBook(book);
                JOptionPane.showMessageDialog(this, "Book updated successfully.");
            }
            clearForm();
            loadBooks(bookDAO::getAllBooks);
        } catch (Exception ex) {
            Dialogs.databaseError(this, "The book could not be saved.", ex);
        }
    }

    private void deleteSelectedBook() {
        if (selectedBookId == null) {
            JOptionPane.showMessageDialog(this, "Select a book from the table first.",
                    "No Book Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this book record?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                bookDAO.deleteBook(selectedBookId);
                clearForm();
                loadBooks(bookDAO::getAllBooks);
            } catch (java.sql.SQLIntegrityConstraintViolationException ex) {
                JOptionPane.showMessageDialog(this,
                        "This book can't be deleted because it has borrowing history on record "
                                + "(past or current). Books are kept once they've been borrowed, "
                                + "to preserve the library's records.",
                        "Cannot Delete Book", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error deleting book: " + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearForm() {
        selectedBookId = null;
        titleField.setText("");
        authorField.setText("");
        isbnField.setText("");
        categoryField.setText("");
        if (tableRef != null) tableRef.clearSelection();
    }

    private interface BookSupplier {
        List<Book> get() throws Exception;
    }

    private void loadBooks(BookSupplier supplier) {
        try {
            modelRef.setRowCount(0);
            for (Book b : supplier.get()) {
                modelRef.addRow(new Object[]{b.getBookId(), b.getTitle(), b.getAuthor(), b.getIsbn(), b.getCategory(), b.getStatus()});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading books: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refresh() {
        loadBooks(bookDAO::getAllBooks);
    }
}
