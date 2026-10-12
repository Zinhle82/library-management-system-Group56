-- ============================================================
-- Library Management System - Database Schema
-- Group 56 (Pixel Pioneers)
-- ============================================================

DROP DATABASE IF EXISTS library_db;
CREATE DATABASE library_db;
USE library_db;

-- MySQL Workbench's "safe update mode" blocks UPDATE/DELETE statements that
-- don't filter directly by a primary key column. This script's seed data
-- includes an UPDATE filtered by title, so we turn that safeguard off just
-- for this session.
SET SQL_SAFE_UPDATES = 0;

-- Users table - librarians register their own account on first use.
-- The password column holds a salted PBKDF2 hash (97 characters), never plain text.
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    full_name VARCHAR(100) NOT NULL
);

-- Books table
CREATE TABLE books (
    book_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    author VARCHAR(100) NOT NULL,
    isbn VARCHAR(20) NOT NULL,
    category VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'Available'
);

-- Members table
CREATE TABLE members (
    member_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    contact_number VARCHAR(20),
    email VARCHAR(100)
);

-- BorrowTransactions table
CREATE TABLE borrow_transactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    book_id INT NOT NULL,
    member_id INT NOT NULL,
    borrow_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE NULL,
    fine_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    FOREIGN KEY (book_id) REFERENCES books(book_id),
    FOREIGN KEY (member_id) REFERENCES members(member_id)
);

-- FinePayments table
CREATE TABLE fine_payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    transaction_id INT NOT NULL,
    amount_paid DECIMAL(10,2) NOT NULL,
    payment_date DATE NOT NULL,
    UNIQUE (transaction_id),
    FOREIGN KEY (transaction_id) REFERENCES borrow_transactions(transaction_id)
);

-- ============================================================
-- Sample demo data (books/members only - no default login,
-- create your own account the first time you open the app)
-- ============================================================

INSERT INTO books (title, author, isbn, category, status) VALUES
('Clean Code', 'Robert C. Martin', '9780132350884', 'Software Engineering', 'Available'),
('The Hobbit', 'J.R.R. Tolkien', '9780547928227', 'Fiction', 'Available'),
('A Brief History of Time', 'Stephen Hawking', '9780553380163', 'Science', 'Available'),
('Introduction to Algorithms', 'Thomas H. Cormen', '9780262033848', 'Computer Science', 'Available'),
('Sapiens', 'Yuval Noah Harari', '9780062316097', 'History', 'Available'),
('1984', 'George Orwell', '9780451524935', 'Fiction', 'Available'),
('Atomic Habits', 'James Clear', '9780735211292', 'Self-Development', 'Available'),
('Cry, the Beloved Country', 'Alan Paton', '9780743262170', 'African Literature', 'Available'),
('To Kill a Mockingbird', 'Harper Lee', '9780061120084', 'Fiction', 'Available'),
('The Great Gatsby', 'F. Scott Fitzgerald', '9780743273565', 'Fiction', 'Available'),
('Pride and Prejudice', 'Jane Austen', '9780141439518', 'Fiction', 'Available'),
('The Catcher in the Rye', 'J.D. Salinger', '9780316769488', 'Fiction', 'Available'),
('The Pragmatic Programmer', 'David Thomas', '9780135957059', 'Software Engineering', 'Available'),
('Design Patterns', 'Erich Gamma', '9780201633610', 'Software Engineering', 'Available'),
('Head First Java', 'Kathy Sierra', '9780596009205', 'Computer Science', 'Available'),
('Cosmos', 'Carl Sagan', '9780345539434', 'Science', 'Available'),
('The Selfish Gene', 'Richard Dawkins', '9780198788607', 'Science', 'Available'),
('Guns, Germs, and Steel', 'Jared Diamond', '9780393317558', 'History', 'Available'),
('Long Walk to Freedom', 'Nelson Mandela', '9780316548182', 'History', 'Available'),
('Rich Dad Poor Dad', 'Robert Kiyosaki', '9781612680194', 'Business', 'Available'),
('Things Fall Apart', 'Chinua Achebe', '9780385474542', 'African Literature', 'Available'),
('Disgrace', 'J.M. Coetzee', '9780099289524', 'African Literature', 'Available');

INSERT INTO members (full_name, contact_number, email) VALUES
('Lerato Molefe', '0731234567', 'lerato.molefe@example.com'),
('Kabelo Mokoena', '0827654321', 'kabelo.mokoena@example.com'),
('Thandiwe Nkosi', '0712345678', 'thandiwe.nkosi@example.com'),
('Sipho Dlamini', '0839876543', 'sipho.dlamini@example.com'),
('Ayanda Khumalo', '0765432198', 'ayanda.khumalo@example.com'),
('Naledi Sithole', '0821239876', 'naledi.sithole@example.com'),
('Bongani Zulu', '0748889977', 'bongani.zulu@example.com');

-- ============================================================
-- Realistic borrowing activity so the dashboard/reports aren't
-- empty on a fresh install: some books currently overdue, some
-- currently out but not yet due, and some already returned
-- (a couple of those late, with a fine on record).
-- ============================================================

-- Currently overdue (borrowed a while ago, still not returned)
INSERT INTO borrow_transactions (book_id, member_id, borrow_date, due_date, return_date, fine_amount)
SELECT b.book_id, m.member_id, DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_SUB(CURDATE(), INTERVAL 16 DAY), NULL, 0
FROM books b, members m WHERE b.title = 'The Hobbit' AND m.full_name = 'Kabelo Mokoena';

INSERT INTO borrow_transactions (book_id, member_id, borrow_date, due_date, return_date, fine_amount)
SELECT b.book_id, m.member_id, DATE_SUB(CURDATE(), INTERVAL 25 DAY), DATE_SUB(CURDATE(), INTERVAL 11 DAY), NULL, 0
FROM books b, members m WHERE b.title = 'Sapiens' AND m.full_name = 'Thandiwe Nkosi';

INSERT INTO borrow_transactions (book_id, member_id, borrow_date, due_date, return_date, fine_amount)
SELECT b.book_id, m.member_id, DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_SUB(CURDATE(), INTERVAL 6 DAY), NULL, 0
FROM books b, members m WHERE b.title = 'Design Patterns' AND m.full_name = 'Sipho Dlamini';

-- Currently borrowed, still within the loan period
INSERT INTO borrow_transactions (book_id, member_id, borrow_date, due_date, return_date, fine_amount)
SELECT b.book_id, m.member_id, DATE_SUB(CURDATE(), INTERVAL 4 DAY), DATE_ADD(CURDATE(), INTERVAL 10 DAY), NULL, 0
FROM books b, members m WHERE b.title = '1984' AND m.full_name = 'Ayanda Khumalo';

INSERT INTO borrow_transactions (book_id, member_id, borrow_date, due_date, return_date, fine_amount)
SELECT b.book_id, m.member_id, DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 12 DAY), NULL, 0
FROM books b, members m WHERE b.title = 'Atomic Habits' AND m.full_name = 'Naledi Sithole';

INSERT INTO borrow_transactions (book_id, member_id, borrow_date, due_date, return_date, fine_amount)
SELECT b.book_id, m.member_id, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY), NULL, 0
FROM books b, members m WHERE b.title = 'The Pragmatic Programmer' AND m.full_name = 'Bongani Zulu';

-- Already returned on time (no fine)
INSERT INTO borrow_transactions (book_id, member_id, borrow_date, due_date, return_date, fine_amount)
SELECT b.book_id, m.member_id, DATE_SUB(CURDATE(), INTERVAL 40 DAY), DATE_SUB(CURDATE(), INTERVAL 26 DAY), DATE_SUB(CURDATE(), INTERVAL 28 DAY), 0
FROM books b, members m WHERE b.title = 'A Brief History of Time' AND m.full_name = 'Lerato Molefe';

INSERT INTO borrow_transactions (book_id, member_id, borrow_date, due_date, return_date, fine_amount)
SELECT b.book_id, m.member_id, DATE_SUB(CURDATE(), INTERVAL 35 DAY), DATE_SUB(CURDATE(), INTERVAL 21 DAY), DATE_SUB(CURDATE(), INTERVAL 23 DAY), 0
FROM books b, members m WHERE b.title = 'Cosmos' AND m.full_name = 'Kabelo Mokoena';

-- Already returned, but late (fine on record)
INSERT INTO borrow_transactions (book_id, member_id, borrow_date, due_date, return_date, fine_amount)
SELECT b.book_id, m.member_id, DATE_SUB(CURDATE(), INTERVAL 45 DAY), DATE_SUB(CURDATE(), INTERVAL 31 DAY), DATE_SUB(CURDATE(), INTERVAL 26 DAY), 10.00
FROM books b, members m WHERE b.title = 'Introduction to Algorithms' AND m.full_name = 'Thandiwe Nkosi';

INSERT INTO borrow_transactions (book_id, member_id, borrow_date, due_date, return_date, fine_amount)
SELECT b.book_id, m.member_id, DATE_SUB(CURDATE(), INTERVAL 38 DAY), DATE_SUB(CURDATE(), INTERVAL 24 DAY), DATE_SUB(CURDATE(), INTERVAL 21 DAY), 6.00
FROM books b, members m WHERE b.title = 'Pride and Prejudice' AND m.full_name = 'Sipho Dlamini';

-- Record one of those late fines as paid, to populate fine_payments too
INSERT INTO fine_payments (transaction_id, amount_paid, payment_date)
SELECT bt.transaction_id, 6.00, DATE_SUB(CURDATE(), INTERVAL 21 DAY)
FROM borrow_transactions bt
JOIN books b ON bt.book_id = b.book_id
WHERE b.title = 'Pride and Prejudice' AND bt.fine_amount = 6.00;

-- Flip the currently-borrowed books (overdue + active) to 'Borrowed' status
UPDATE books SET status = 'Borrowed'
WHERE title IN ('The Hobbit', 'Sapiens', 'Design Patterns', '1984', 'Atomic Habits', 'The Pragmatic Programmer');
