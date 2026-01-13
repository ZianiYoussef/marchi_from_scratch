package org.example.service.cashier;

import org.example.db.Database;
import java.sql.*;
import java.util.Map;

public class InvoiceService {

    // SYNCHRONIZED: Prevents Race Conditions (two people buying the last item at once)
    public synchronized boolean createInvoice(int cashierId, Integer customerId, Map<Integer, Integer> cart) {
        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false); // START TRANSACTION

            double totalInvoiceAmount = 0;

            // 1. CHECK STOCK FIRST (For all items)
            for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
                int prodId = entry.getKey();
                int qtyWanted = entry.getValue();

                String checkStock = "SELECT name, selling_price, stock_quantity FROM products WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(checkStock)) {
                    ps.setInt(1, prodId);
                    ResultSet rs = ps.executeQuery();

                    if (!rs.next()) throw new RuntimeException("Product ID " + prodId + " not found.");

                    String name = rs.getString("name");
                    double price = rs.getDouble("selling_price");
                    int stock = rs.getInt("stock_quantity");

                    if (stock < qtyWanted) {
                        throw new RuntimeException("OUT OF STOCK: " + name + " (Has " + stock + ", Wanted " + qtyWanted + ")");
                    }

                    totalInvoiceAmount += (price * qtyWanted);
                }
            }

            // 2. CREATE INVOICE ROW
            int invoiceId;
            String insertInv = "INSERT INTO invoices (cashier_id, customer_id, total_amount) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertInv, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, cashierId);
                // If customerId is 0 or null, we set it to SQL NULL
                if (customerId == null || customerId == 0) {
                    ps.setNull(2, Types.INTEGER);
                } else {
                    ps.setInt(2, customerId);
                }
                ps.setDouble(3, totalInvoiceAmount);
                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();
                rs.next();
                invoiceId = rs.getInt(1);
            }

            // 3. DECREASE STOCK AND SAVE ITEMS
            String updateStock = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE id = ?";
            String insertItem = "INSERT INTO invoice_items (invoice_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";

            for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
                int prodId = entry.getKey();
                int qty = entry.getValue();

                // Get price one last time to be safe
                double unitPrice = getPrice(conn, prodId);

                // A. Decrease Stock
                try (PreparedStatement ps = conn.prepareStatement(updateStock)) {
                    ps.setInt(1, qty);
                    ps.setInt(2, prodId);
                    ps.executeUpdate();
                }

                // B. Add line item
                try (PreparedStatement ps = conn.prepareStatement(insertItem)) {
                    ps.setInt(1, invoiceId);
                    ps.setInt(2, prodId);
                    ps.setInt(3, qty);
                    ps.setDouble(4, unitPrice);
                    ps.executeUpdate();
                }
            }

            // 🌟 4. LOYALTY POINTS LOGIC 🌟
            // Rule: 10 points for every $100 spent.
            if (customerId != null && customerId > 0) {
                int pointsEarned = (int) (totalInvoiceAmount / 100) * 10;
                if (pointsEarned > 0) {
                    String updatePoints = "UPDATE customers SET points = points + ? WHERE id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(updatePoints)) {
                        ps.setInt(1, pointsEarned);
                        ps.setInt(2, customerId);
                        ps.executeUpdate();
                        System.out.println("🎉 Customer earned " + pointsEarned + " loyalty points!");
                    }
                }
            }

            conn.commit(); // ✅ SAVE EVERYTHING

            // 5. PRINT RECEIPT
            printReceipt(invoiceId, cart, conn);

            System.out.println("✅ Sale successful! Total: $" + totalInvoiceAmount);
            return true;

        } catch (Exception e) {
            // ROLLBACK: If anything failed, undo all changes
            try { if (conn != null) conn.rollback(); } catch (Exception ex) {}
            System.out.println("❌ Transaction Failed: " + e.getMessage());
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception ex) {}
        }
    }

    // ==========================================
    // 🧾 HELPER: PRINT RECEIPT (Using Streams)
    // ==========================================
    private void printReceipt(int invoiceId, Map<Integer, Integer> cart, Connection conn) {
        System.out.println("\n" + "=".repeat(30));
        System.out.println("      🧾 OFFICIAL RECEIPT      ");
        System.out.println("      Invoice ID: #" + invoiceId);
        System.out.println("=".repeat(30));
        System.out.printf("%-15s %5s %10s\n", "Item", "Qty", "Price");
        System.out.println("-".repeat(30));

        double totalCalculated = cart.entrySet().stream()
                .mapToDouble(entry -> {
                    int prodId = entry.getKey();
                    int qty = entry.getValue();
                    try {
                        String sql = "SELECT name, selling_price FROM products WHERE id = ?";
                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setInt(1, prodId);
                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) {
                                String name = rs.getString("name");
                                double price = rs.getDouble("selling_price");
                                double lineTotal = price * qty;
                                System.out.printf("%-15s %5d %10.2f\n",
                                        (name.length() > 15 ? name.substring(0, 15) : name),
                                        qty,
                                        lineTotal);
                                return lineTotal;
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    return 0.0;
                })
                .sum();

        System.out.println("-".repeat(30));
        System.out.printf("TOTAL PAID:        $%10.2f\n", totalCalculated);
        System.out.println("=".repeat(30));
        System.out.println("   Thank you for shopping!   \n");
    }

    // ==========================================
    // 💰 HELPER: GET PRICE
    // ==========================================
    private double getPrice(Connection conn, int id) throws SQLException {
        try(PreparedStatement ps = conn.prepareStatement("SELECT selling_price FROM products WHERE id=?")){
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }
}