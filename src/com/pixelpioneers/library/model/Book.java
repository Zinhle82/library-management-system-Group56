package com.pixelpioneers.library.model;

/** A library book (Phase 3 ERD: Books). Status is "Available" or "Borrowed". */
public class Book {
    private int bookId;
    private String title;
    private String author;
    private String isbn;
    private String category;
    private String status;

    public Book() { }

    public Book(int bookId, String title, String author, String isbn, String category, String status) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.category = category;
        this.status = status;
    }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() { return title + " (" + isbn + ")"; }
}
