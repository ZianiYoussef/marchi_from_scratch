package org.example.service;

import org.example.db.Database;
import org.example.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CategoryService {

    // Add new category using Thread (async operation)
    public CompletableFuture<Boolean> addCategoryAsync(String name) {
        return CompletableFuture.supplyAsync(() -> {
            if (name == null || name.trim().isEmpty()) {
                System.out.println("❌ Category name cannot be empty.");
                return false;
            }

            String sql = "INSERT INTO categories(name) VALUES(?)";

            try (Connection conn = Database.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, name.trim());
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int newId = rs.getInt(1);
                        System.out.println("✅ Category created successfully (ID: " + newId + ")");
                        return true;
                    }
                }

            } catch (Exception e) {
                if (e.getMessage().contains("UNIQUE constraint")) {
                    System.out.println("❌ Category '" + name + "' already exists.");
                } else {
                    System.out.println("❌ Failed to create category: " + e.getMessage());
                }
                return false;
            }
            return false;
        });
    }

    // Synchronous version for immediate use
    public boolean addCategory(String name) {
        try {
            return addCategoryAsync(name).get(); // Wait for async operation
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            return false;
        }
    }

    // Get all categories using Stream and Filter
    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT id, name FROM categories ORDER BY name";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                categories.add(new Category(
                        rs.getInt("id"),
                        rs.getString("name")
                ));
            }

        } catch (Exception e) {
            System.out.println("❌ Failed to load categories: " + e.getMessage());
        }

        return categories;
    }

    // Filter categories by name pattern using Stream and Filter
    public List<Category> filterCategoriesByName(String searchTerm) {
        return getAllCategories().stream()
                .filter(category ->
                        category.getName().toLowerCase().contains(searchTerm.toLowerCase()))
                .collect(Collectors.toList());
    }

    // Check if category exists using Stream
    public boolean categoryExists(String name) {
        return getAllCategories().stream()
                .anyMatch(cat -> cat.getName().equalsIgnoreCase(name.trim()));
    }
}