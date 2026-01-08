package org.example.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.stream.Collectors;

public class SchemaInitializer {

    public static void init() {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            InputStream in = SchemaInitializer.class.getClassLoader().getResourceAsStream("schema.sql");
            if (in == null) {
                throw new RuntimeException("schema.sql not found in resources");
            }

            String sql = new BufferedReader(new InputStreamReader(in))
                    .lines()
                    .collect(Collectors.joining("\n"));

            // Split by ; and run each statement
            for (String s : sql.split(";")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed + ";");
                }
            }

            // ✅ seed after tables exist (and while conn is still open)
            seedCategories(conn);
            seedSuppliers(conn);

            System.out.println("✅ Database tables created/verified successfully.");

        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to init database schema", e);
        }
    }

    private static void seedCategories(Connection conn) {
        String countSql = "SELECT COUNT(*) FROM categories";
        String insertSql = """
            INSERT INTO categories (name) VALUES
            ('Produce (Fruits & Vegetables)'),
            ('Dairy & Eggs'),
            ('Meat & Poultry'),
            ('Seafood'),
            ('Bakery & Bread'),
            ('Deli & Prepared Foods'),
            ('Frozen Foods'),
            ('Pantry / Dry Goods'),
            ('Snacks & Sweets'),
            ('Beverages'),
            ('Canned Foods'),
            ('Condiments & Spices'),
            ('Breakfast & Cereals'),
            ('Health & Beauty'),
            ('Household Supplies'),
            ('Baby Products'),
            ('Pet Supplies');
            """;

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(countSql)) {

            int count = rs.next() ? rs.getInt(1) : 0;
            if (count > 0) {
                System.out.println("ℹ️ Categories already seeded (" + count + ").");
                return;
            }

            st.executeUpdate(insertSql);
            System.out.println("✅ Categories seeded successfully.");

        } catch (Exception e) {
            System.out.println("❌ Failed to seed categories: " + e.getMessage());
        }
    }
    private static void seedSuppliers(Connection conn) {
        String countSql = "SELECT COUNT(*) FROM suppliers";
        String insertSql = """
    INSERT INTO suppliers (name) VALUES
    ('Produce Supplier'),
    ('Dairy & Eggs Supplier'),
    ('Meat & Poultry Supplier'),
    ('Seafood Supplier'),
    ('Bakery & Bread Supplier'),
    ('Deli & Prepared Foods Supplier'),
    ('Frozen Foods Distributor'),
    ('Pantry / Dry Goods Wholesaler'),
    ('Snacks & Sweets Supplier'),
    ('Beverages Distributor'),
    ('Canned Foods Supplier'),
    ('Condiments & Spices Supplier'),
    ('Breakfast & Cereals Supplier'),
    ('Health & Beauty Supplier'),
    ('Household Supplies Supplier'),
    ('Baby Products Supplier'),
    ('Pet Supplies Supplier');
    """;



        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(countSql)) {

            int count = rs.next() ? rs.getInt(1) : 0;
            if (count > 0) {
                System.out.println("ℹ️ Suppliers already seeded (" + count + ").");
                return;
            }

            st.executeUpdate(insertSql);
            System.out.println("✅ Suppliers seeded successfully.");

        } catch (Exception e) {
            System.out.println("❌ Failed to seed suppliers: " + e.getMessage());
        }
    }

}
