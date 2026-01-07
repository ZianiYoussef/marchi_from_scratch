package org.example.db;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

public class SchemaInitializer {

    public static void init() {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = new BufferedReader(new InputStreamReader(
                    SchemaInitializer.class.getClassLoader().getResourceAsStream("schema.sql")
            )).lines().collect(Collectors.joining("\n"));

            // Split by ; and run each statement
            for (String s : sql.split(";")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed + ";");
                }
            }

            System.out.println("✅ Database tables created/verified successfully.");

        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to init database schema", e);
        }
    }
}
