package org.example.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserSeeder {

    public static void seedDefaultAdmin() {
        final String adminUsername = "admin";
        final String adminPassword = "admin123"; // later we hash it
        final String adminRole = "ADMIN";

        String checkSql = "SELECT 1 FROM users WHERE username = ?";
        String insertSql = "INSERT INTO users(username, password, role) VALUES(?,?,?)";

        try (Connection conn = Database.getConnection()) {

            // check if admin exists
            try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                check.setString(1, adminUsername);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("ℹ️ Default admin already exists (admin).");
                        return;
                    }
                }
            }

            // insert admin
            try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                insert.setString(1, adminUsername);
                insert.setString(2, adminPassword);
                insert.setString(3, adminRole);
                insert.executeUpdate();
            }

            System.out.println("✅ Default ADMIN created: admin / admin123");

        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to seed default admin", e);
        }
    }
}
