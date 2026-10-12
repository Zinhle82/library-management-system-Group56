# Library Management System - Group 56 (Pixel Pioneers)
Module NPRT63 | Sol Plaatje University | Lecturer: Mr Melvin Kisten

A Java Swing desktop application (hand-coded, no GUI builder) with a MySQL database,
built in NetBeans. Three-layer architecture from the Phase 1 proposal:

| Layer | Package | What is in it |
|---|---|---|
| Presentation | `ui`, `util` | Login, Register, Dashboard, Books, Members, Borrow, Return, Pay Fines, Reports |
| Application | `service` | Business rules (14-day loan, R2/day fine, 3-book limit), input validation, password hashing |
| Database | `dao`, `db`, `model` | JDBC access (PreparedStatements, commit/rollback), connection, entity classes |

## How to run it (on any computer)

1. **Install** Java (JDK 17 or newer), MySQL Server and MySQL Workbench.
2. **Create the database:** open `sql/library_db.sql` in MySQL Workbench and run the whole
   script (lightning bolt with the document icon). It creates `library_db`, the five tables from the
   Phase 3 ERD, and demo data (22 books, 7 members, borrowing history with overdue books and fines).
3. **Check `db.properties`** (in the project folder): set `db.password` to your MySQL password.
   No recompiling is needed to change it.
4. **Add the MySQL driver** (`mysql-connector-j-x.x.x.jar`, from dev.mysql.com/downloads/connector/j)
   to the project's Libraries in NetBeans.
5. **Run** `com.pixelpioneers.library.Main`. Click **"New here? Create an account"** the first time -
   there is no default login. (Passwords are stored as salted PBKDF2 hashes.)

If MySQL is not running or the password is wrong, the program shows a clear message and closes.

## Features (all from the Phase 1-3 documents)
- Librarian registration and secure login
- Add / update / delete / search books; add / update / delete / search members
- Detailed validation messages listing every problem at once (ISBN, phone, email, names, passwords)
- Borrow a book: the system checks the member may borrow (no overdue books, no unpaid fines,
  maximum 3 books out), sets the due date automatically and marks the book Borrowed
- Return a book: live fine preview in bold, fine calculated, book marked Available
- Pay Fines screen: register payment of overdue fines (one payment per fine)
- Reports: overdue books (with fine so far), all books, all members, borrowing history
  (filterable by member)
- Dashboard with summary figures, large navigation cards, and an "overdue right now" list

## Automatic tests (70 checks)
Run these from NetBeans (right-click the file, **Run File**) - output shows one PASS/FAIL line per check:
- `com.pixelpioneers.library.tests.UnitTests` - 33 checks, no database needed
- `com.pixelpioneers.library.tests.DatabaseTests` - 37 checks against the real database
  (creates "ZZ Test" records and deletes them again, so it is safe to run on your data)

## Project structure
```
src/com/pixelpioneers/library/
  Main.java
  ui/        screens        util/   colours, buttons, drawn icons
  service/   LibraryRules, Validator, PasswordUtil, BusinessRuleException
  dao/       BookDAO, MemberDAO, UserDAO, BorrowDAO, FineDAO
  db/        DBConnection (reads db.properties)
  model/     Book, Member, LibUser, BorrowTransaction, FinePayment
  tests/     UnitTests, DatabaseTests, TestSupport
sql/library_db.sql   schema + demo data
db.properties        database connection settings
```
