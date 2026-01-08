package org.example.service;

import org.example.db.Database;

import java.sql.*;

public class StockManagerService {

    // ============ 1) ADD PRODUCT ============

    // Adds the product, and if initialStockQty > 0 it also logs a stock_movements 'IN'
    public void addProduct(String name,
                           String barcode,
                           int categoryId,
                           int supplierId,
                           double purchasePrice,
                           double sellingPrice,
                           int initialStockQty,
                           int userId) {

        if (initialStockQty < 0) {
            System.out.println("❌ Initial stock quantity cannot be negative.");
            return;
        }

        Connection conn = null;

        String insertProduct = """
            INSERT INTO products
            (name, barcode, category_id, supplier_id, purchase_price, selling_price, stock_quantity, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;

        String insertMovement = """
            INSERT INTO stock_movements (product_id, user_id, movement_type, quantity, created_at)
            VALUES (?, ?, 'IN', ?, CURRENT_TIMESTAMP)
        """;

        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);

            int newProductId;

            try (PreparedStatement ps = conn.prepareStatement(insertProduct, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setString(2, barcode);
                ps.setInt(3, categoryId);
                ps.setInt(4, supplierId);
                ps.setDouble(5, purchasePrice);
                ps.setDouble(6, sellingPrice);
                ps.setInt(7, initialStockQty);

                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new RuntimeException("Failed to get inserted product id.");
                    newProductId = keys.getInt(1);
                }
            }

            // log initial stock as IN movement (only if > 0)
            if (initialStockQty > 0) {
                try (PreparedStatement ps2 = conn.prepareStatement(insertMovement)) {
                    ps2.setInt(1, newProductId);
                    ps2.setInt(2, userId);
                    ps2.setInt(3, initialStockQty);
                    ps2.executeUpdate();
                }
            }

            conn.commit();
            System.out.println("✅ Product added (id=" + newProductId + ").");

        } catch (Exception e) {
            try {
                if (conn != null) conn.rollback();
            } catch (Exception ignored) {
            }
            System.out.println("❌ Failed to add product: " + e.getMessage());
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (Exception ignored) {
            }
        }
    }

    // ============ 2) STOCK IN ============

    public void stockIn(int productId, int qty, int userId) {
        if (qty <= 0) {
            System.out.println("❌ Quantity must be > 0");
            return;
        }

        Connection conn = null;

        String updateProduct = "UPDATE products SET stock_quantity = stock_quantity + ? WHERE id = ?";
        String insertMovement = """
            INSERT INTO stock_movements (product_id, user_id, movement_type, quantity, created_at)
            VALUES (?, ?, 'IN', ?, CURRENT_TIMESTAMP)
        """;

        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps1 = conn.prepareStatement(updateProduct)) {
                ps1.setInt(1, qty);
                ps1.setInt(2, productId);
                int updated = ps1.executeUpdate();
                if (updated == 0) {
                    throw new RuntimeException("Product not found (id=" + productId + ")");
                }
            }

            try (PreparedStatement ps2 = conn.prepareStatement(insertMovement)) {
                ps2.setInt(1, productId);
                ps2.setInt(2, userId);
                ps2.setInt(3, qty);
                ps2.executeUpdate();
            }

            conn.commit();
            System.out.println("✅ Stock IN done.");

        } catch (Exception e) {
            try {
                if (conn != null) conn.rollback();
            } catch (Exception ignored) {
            }
            System.out.println("❌ Stock IN failed: " + e.getMessage());
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (Exception ignored) {
            }
        }
    }

    // ============ 3) STOCK OUT ============

    public void stockOut(int productId, int qty, int userId) {
        if (qty <= 0) {
            System.out.println("❌ Quantity must be > 0");
            return;
        }

        Connection conn = null;

        String selectQty = "SELECT stock_quantity FROM products WHERE id = ?";
        String updateProduct = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE id = ?";
        String insertMovement = """
            INSERT INTO stock_movements (product_id, user_id, movement_type, quantity, created_at)
            VALUES (?, ?, 'OUT', ?, CURRENT_TIMESTAMP)
        """;

        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);

            int currentQty;
            try (PreparedStatement ps0 = conn.prepareStatement(selectQty)) {
                ps0.setInt(1, productId);
                try (ResultSet rs = ps0.executeQuery()) {
                    if (!rs.next()) {
                        throw new RuntimeException("Product not found (id=" + productId + ")");
                    }
                    currentQty = rs.getInt("stock_quantity");
                }
            }

            if (currentQty < qty) {
                throw new RuntimeException("Not enough stock. Current=" + currentQty + ", requested=" + qty);
            }

            try (PreparedStatement ps1 = conn.prepareStatement(updateProduct)) {
                ps1.setInt(1, qty);
                ps1.setInt(2, productId);
                ps1.executeUpdate();
            }

            try (PreparedStatement ps2 = conn.prepareStatement(insertMovement)) {
                ps2.setInt(1, productId);
                ps2.setInt(2, userId);
                ps2.setInt(3, qty);
                ps2.executeUpdate();
            }

            conn.commit();
            System.out.println("✅ Stock OUT done.");

        } catch (Exception e) {
            try {
                if (conn != null) conn.rollback();
            } catch (Exception ignored) {
            }
            System.out.println("❌ Stock OUT failed: " + e.getMessage());
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (Exception ignored) {
            }
        }
    }

    public void listCategories() {
        String sql = "SELECT id, name FROM categories ORDER BY id";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            boolean found = false;

            System.out.println("\n--- Categories ---");
            while (rs.next()) { // iterate rows
                found = true;
                int id = rs.getInt("id");
                String name = rs.getString("name");
                System.out.println(id + " - " + name);
            }

            if (!found) {
                System.out.println("(No categories found)");
            }

        } catch (Exception e) {
            System.out.println("❌ Failed to list categories: " + e.getMessage());
        }
    }
    public void listSuppliers() {
        String sql = "SELECT id, name FROM suppliers ORDER BY id";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            boolean found = false;
            System.out.println("\n--- Suppliers ---");

            while (rs.next()) {
                found = true;
                System.out.println(rs.getInt("id") + " - " + rs.getString("name"));
            }

            if (!found) System.out.println("(No suppliers found)");

        } catch (Exception e) {
            System.out.println("❌ Failed to list suppliers: " + e.getMessage());
        }
    }
    public void checkStock(String query) {
        if (query == null || query.trim().isEmpty()) {
            System.out.println("❌ Please enter an id or a name.");
            return;
        }

        query = query.trim();

        // 1) Try as ID
        try {
            int id = Integer.parseInt(query);

            String sql = "SELECT id, name, stock_quantity FROM products WHERE id = ?";
            try (Connection conn = Database.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("❌ Product not found (id=" + id + ")");
                        return;
                    }
                    System.out.println("✅ " + rs.getInt("id") + " - " + rs.getString("name")
                            + " | Stock: " + rs.getInt("stock_quantity"));
                }
            }

            return; // done

        } catch (NumberFormatException ignored) {
            // not a number -> search by name
        } catch (Exception e) {
            System.out.println("❌ Failed to check stock by id: " + e.getMessage());
            return;
        }

        // 2) Search by name (partial match)
        String sql = "SELECT id, name, stock_quantity FROM products WHERE name LIKE ? ORDER BY id";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + query + "%"); // LIKE pattern

            try (ResultSet rs = ps.executeQuery()) {
                boolean found = false;

                System.out.println("\n--- Matching products ---");
                while (rs.next()) {
                    found = true;
                    System.out.println(rs.getInt("id") + " - " + rs.getString("name")
                            + " | Stock: " + rs.getInt("stock_quantity"));
                }

                if (!found) System.out.println("(No products matched)");
            }

        } catch (Exception e) {
            System.out.println("❌ Failed to check stock by name: " + e.getMessage());
        }
    }

}