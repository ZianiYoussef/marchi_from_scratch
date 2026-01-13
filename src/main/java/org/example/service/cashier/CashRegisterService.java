package org.example.service.cashier;

import org.example.db.Database;
import java.sql.*;

public class CashRegisterService {

    // This remembers your specific Shift ID while the app is running
    private int currentShiftId = -1;

    // Helper to check if we are working
    public boolean hasActiveShift() {
        return currentShiftId != -1;
    }

    // 1. START SHIFT (Put money in drawer)
    public void startShift(int cashierId, double openingBalance) {
        if (hasActiveShift()) {
            System.out.println("⚠️ You already have an open shift!");
            return;
        }

        String sql = "INSERT INTO cash_register (cashier_id, opening_balance) VALUES (?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, cashierId);
            ps.setDouble(2, openingBalance);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    currentShiftId = rs.getInt(1);
                    System.out.println("✅ Shift STARTED. Drawer contains: $" + openingBalance);
                }
            }

        } catch (SQLException e) {
            System.out.println("❌ Error starting shift: " + e.getMessage());
        }
    }

    // 2. END SHIFT (Count the money)
    public void endShift(int cashierId) {
        if (!hasActiveShift()) {
            System.out.println("⚠️ No active shift found.");
            return;
        }

        // We calculate total sales by summing invoices created SINCE this shift opened
        String sumSql = """
            SELECT SUM(total_amount) FROM invoices 
            WHERE cashier_id = ? 
            AND created_at >= (SELECT opened_at FROM cash_register WHERE id = ?)
        """;

        String closeSql = "UPDATE cash_register SET closing_balance = ?, closed_at = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection conn = Database.getConnection()) {
            double totalSales = 0;
            double openingBalance = 0;

            // 1. Get Opening Balance (What we started with)
            try (PreparedStatement ps = conn.prepareStatement("SELECT opening_balance FROM cash_register WHERE id = ?")) {
                ps.setInt(1, currentShiftId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) openingBalance = rs.getDouble(1);
            }

            // 2. Get Total Sales (What we earned)
            try (PreparedStatement ps = conn.prepareStatement(sumSql)) {
                ps.setInt(1, cashierId);
                ps.setInt(2, currentShiftId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) totalSales = rs.getDouble(1);
            }

            double finalTotal = openingBalance + totalSales;

            // 3. Save to DB
            try (PreparedStatement ps = conn.prepareStatement(closeSql)) {
                ps.setDouble(1, finalTotal);
                ps.setInt(2, currentShiftId);
                ps.executeUpdate();
            }

            System.out.println("\n=== 🏁 SHIFT ENDED ===");
            System.out.println("   Opening Balance: " + openingBalance);
            System.out.println("   Total Sales:     " + totalSales);
            System.out.println("   ---------------------------");
            System.out.println("   TOTAL IN DRAWER: " + finalTotal);

            currentShiftId = -1; // Reset so we can login again later

        } catch (SQLException e) {
            System.out.println("❌ Error ending shift: " + e.getMessage());
        }
    }
}