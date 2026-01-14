package org.example.service.cashier;

import org.example.db.Database;
import java.sql.*;
import java.util.Map;

public class InvoiceService {

    // 1. HELPER: Calculate total without changing stock (For UI Preview)
    public double calculateCartTotal(Map<Integer, Integer> cart) {
        double total = 0;
        try (Connection conn = Database.getConnection()) {
            for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
                double price = getPrice(conn, entry.getKey());
                total += price * entry.getValue();
            }
        } catch (SQLException e) {
            System.out.println("❌ Error calculating total: " + e.getMessage());
        }
        return total;
    }

    // 2. MAIN TRANSACTION: Now accepts 'cashGiven' to calculate Change
    public synchronized boolean createInvoice(int cashierId, Integer customerId, Map<Integer, Integer> cart, double cashGiven) {
        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false); // START TRANSACTION

            double totalInvoiceAmount = 0;

            // A. CHECK STOCK & CALCULATE TRUE TOTAL
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

            // B. VALIDATE PAYMENT
            if (cashGiven < totalInvoiceAmount) {
                throw new RuntimeException("Insufficient funds! Total is $" + totalInvoiceAmount + " but received $" + cashGiven);
            }
            double change = cashGiven - totalInvoiceAmount;

            // C. CREATE INVOICE
            int invoiceId;
            String insertInv = "INSERT INTO invoices (cashier_id, customer_id, total_amount) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertInv, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, cashierId);
                if (customerId == null || customerId == 0) ps.setNull(2, Types.INTEGER);
                else ps.setInt(2, customerId);
                ps.setDouble(3, totalInvoiceAmount);
                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();
                rs.next();
                invoiceId = rs.getInt(1);
            }

            // D. UPDATE STOCK & SAVE ITEMS
            String updateStock = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE id = ?";
            String insertItem = "INSERT INTO invoice_items (invoice_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";

            for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
                int prodId = entry.getKey();
                int qty = entry.getValue();
                double unitPrice = getPrice(conn, prodId);

                try (PreparedStatement ps = conn.prepareStatement(updateStock)) {
                    ps.setInt(1, qty);
                    ps.setInt(2, prodId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(insertItem)) {
                    ps.setInt(1, invoiceId);
                    ps.setInt(2, prodId);
                    ps.setInt(3, qty);
                    ps.setDouble(4, unitPrice);
                    ps.executeUpdate();
                }
            }

            // E. LOYALTY POINTS
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

            // F. PRINT RECEIPT (Now with Change)
            printReceipt(invoiceId, cart, conn, cashGiven, change);

            System.out.println("✅ Sale successful!");
            return true;

        } catch (Exception e) {
            try { if (conn != null) conn.rollback(); } catch (Exception ex) {}
            System.out.println("❌ Transaction Failed: " + e.getMessage());
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception ex) {}
        }
    }

    private void printReceipt(int invoiceId, Map<Integer, Integer> cart, Connection conn, double cash, double change) {
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
                                System.out.printf("%-15s %5d %10.2f\n", (name.length() > 15 ? name.substring(0, 15) : name), qty, lineTotal);
                                return lineTotal;
                            }
                        }
                    } catch (SQLException e) { e.printStackTrace(); }
                    return 0.0;
                }).sum();

        System.out.println("-".repeat(30));
        System.out.printf("TOTAL:             $%10.2f\n", totalCalculated);
        System.out.printf("CASH:              $%10.2f\n", cash);
        System.out.printf("CHANGE:            $%10.2f\n", change);
        System.out.println("=".repeat(30));
        System.out.println("   Thank you for shopping!   \n");
    }

    private double getPrice(Connection conn, int id) throws SQLException {
        try(PreparedStatement ps = conn.prepareStatement("SELECT selling_price FROM products WHERE id=?")){
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }
}