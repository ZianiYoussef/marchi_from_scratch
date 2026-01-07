-- =========================
-- USERS & ROLES
-- =========================
CREATE TABLE IF NOT EXISTS users (
                                     id INTEGER PRIMARY KEY AUTOINCREMENT,
                                     username TEXT NOT NULL UNIQUE,
                                     password TEXT NOT NULL,
                                     role TEXT NOT NULL CHECK (role IN ('ADMIN', 'CASHIER', 'STOCK_MANAGER')),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

-- =========================
-- CATEGORIES
-- =========================
CREATE TABLE IF NOT EXISTS categories (
                                          id INTEGER PRIMARY KEY AUTOINCREMENT,
                                          name TEXT NOT NULL UNIQUE
);

-- =========================
-- SUPPLIERS
-- =========================
CREATE TABLE IF NOT EXISTS suppliers (
                                         id INTEGER PRIMARY KEY AUTOINCREMENT,
                                         name TEXT NOT NULL,
                                         phone TEXT,
                                         email TEXT
);

-- =========================
-- PRODUCTS
-- =========================
CREATE TABLE IF NOT EXISTS products (
                                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                                        name TEXT NOT NULL,
                                        barcode TEXT UNIQUE,
                                        category_id INTEGER,
                                        supplier_id INTEGER,
                                        purchase_price REAL NOT NULL,
                                        selling_price REAL NOT NULL,
                                        stock_quantity INTEGER DEFAULT 0,
                                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                        FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
    );

-- =========================
-- CUSTOMERS
-- =========================
CREATE TABLE IF NOT EXISTS customers (
                                         id INTEGER PRIMARY KEY AUTOINCREMENT,
                                         name TEXT NOT NULL,
                                         phone TEXT,
                                         email TEXT
);

-- =========================
-- INVOICES
-- =========================
CREATE TABLE IF NOT EXISTS invoices (
                                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                                        cashier_id INTEGER NOT NULL,
                                        customer_id INTEGER,
                                        total_amount REAL NOT NULL,
                                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                        FOREIGN KEY (cashier_id) REFERENCES users(id),
    FOREIGN KEY (customer_id) REFERENCES customers(id)
    );

-- =========================
-- INVOICES
-- =========================
CREATE TABLE IF NOT EXISTS invoices (
                                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                                        cashier_id INTEGER NOT NULL,
                                        customer_id INTEGER,
                                        total_amount REAL NOT NULL,
                                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                        FOREIGN KEY (cashier_id) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL
    );

-- =========================
-- INVOICE ITEMS
-- =========================
CREATE TABLE IF NOT EXISTS invoice_items (
                                             id INTEGER PRIMARY KEY AUTOINCREMENT,
                                             invoice_id INTEGER NOT NULL,
                                             product_id INTEGER NOT NULL,
                                             quantity INTEGER NOT NULL,
                                             unit_price REAL NOT NULL,
                                             FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
    );

-- =========================
-- STOCK MOVEMENTS
-- =========================
CREATE TABLE IF NOT EXISTS stock_movements (
                                               id INTEGER PRIMARY KEY AUTOINCREMENT,
                                               product_id INTEGER NOT NULL,
                                               user_id INTEGER NOT NULL,
                                               movement_type TEXT NOT NULL CHECK (movement_type IN ('IN', 'OUT', 'ADJUSTMENT')),
    quantity INTEGER NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
    );

-- =========================
-- CASH REGISTER
-- =========================
CREATE TABLE IF NOT EXISTS cash_register (
                                             id INTEGER PRIMARY KEY AUTOINCREMENT,
                                             cashier_id INTEGER NOT NULL,
                                             opening_balance REAL NOT NULL,
                                             closing_balance REAL,
                                             opened_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                             closed_at DATETIME,
                                             FOREIGN KEY (cashier_id) REFERENCES users(id)
    );
