package org.example.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CustomerSeeder {

    public static void seed() {
        String sqlCheck = "SELECT COUNT(*) FROM customers";
        String sqlInsert = "INSERT INTO customers (name, phone, email, points) VALUES (?, ?, ?, 0)";

        try (Connection conn = Database.getConnection()) {

            // 1. Check if we already have customers
            try (PreparedStatement ps = conn.prepareStatement(sqlCheck);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("ℹ️ Customers already exist. Skipping seed.");
                    return;
                }
            }

            // 2. Insert "John Doe" (This will be Customer ID 1)
            try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
                ps.setString(1, "John Doe");
                ps.setString(2, "555-0199");
                ps.setString(3, "john@example.com");
                ps.executeUpdate();
                System.out.println("✅ Default Customer created: John Doe (ID: 1)");
            }

        } catch (Exception e) {
            System.out.println("❌ Failed to seed customers: " + e.getMessage());
        }
    }
}