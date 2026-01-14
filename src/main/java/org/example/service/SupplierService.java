package org.example.service;

import org.example.db.Database;
import org.example.model.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SupplierService {

    // Add new supplier using Thread (async operation)
    public CompletableFuture<Boolean> addSupplierAsync(String name, String phone, String email) {
        return CompletableFuture.supplyAsync(() -> {
            if (name == null || name.trim().isEmpty()) {
                System.out.println("❌ Supplier name cannot be empty.");
                return false;
            }

            String sql = "INSERT INTO suppliers(name, phone, email) VALUES(?, ?, ?)";

            try (Connection conn = Database.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, name.trim());
                ps.setString(2, phone != null && !phone.trim().isEmpty() ? phone.trim() : null);
                ps.setString(3, email != null && !email.trim().isEmpty() ? email.trim() : null);

                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int newId = rs.getInt(1);
                        System.out.println("✅ Supplier created successfully (ID: " + newId + ")");
                        return true;
                    }
                }

            } catch (Exception e) {
                System.out.println("❌ Failed to create supplier: " + e.getMessage());
                return false;
            }
            return false;
        });
    }

    // Synchronous version
    public boolean addSupplier(String name, String phone, String email) {
        try {
            return addSupplierAsync(name, phone, email).get();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            return false;
        }
    }

    // Get all suppliers using Stream
    public List<Supplier> getAllSuppliers() {
        List<Supplier> suppliers = new ArrayList<>();
        String sql = "SELECT id, name, phone, email FROM suppliers ORDER BY name";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                suppliers.add(new Supplier(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("email")
                ));
            }

        } catch (Exception e) {
            System.out.println("❌ Failed to load suppliers: " + e.getMessage());
        }

        return suppliers;
    }

    // Filter suppliers by name using Stream and Filter
    public List<Supplier> filterSuppliersByName(String searchTerm) {
        return getAllSuppliers().stream()
                .filter(supplier ->
                        supplier.getName().toLowerCase().contains(searchTerm.toLowerCase()))
                .collect(Collectors.toList());
    }

    // Filter suppliers with email using Stream and Filter
    public List<Supplier> filterSuppliersWithEmail() {
        return getAllSuppliers().stream()
                .filter(supplier -> supplier.getEmail() != null && !supplier.getEmail().isEmpty())
                .collect(Collectors.toList());
    }

    // Filter suppliers with phone using Stream and Filter
    public List<Supplier> filterSuppliersWithPhone() {
        return getAllSuppliers().stream()
                .filter(supplier -> supplier.getPhone() != null && !supplier.getPhone().isEmpty())
                .collect(Collectors.toList());
    }
}