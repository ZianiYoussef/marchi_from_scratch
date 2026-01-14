package org.example.service;

import org.example.db.Database;
import org.example.service.cashier.CashRegisterService;
import org.example.service.cashier.InvoiceService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

public class CashierService {

    private final CashRegisterService cashRegisterService;
    private final InvoiceService invoiceService;

    public CashierService() {
        this.cashRegisterService = new CashRegisterService();
        this.invoiceService = new InvoiceService();
    }

    // ==========================================
    // 🏁 SHIFT MANAGEMENT
    // ==========================================
    public void startShift(int cashierId, double amount) {
        cashRegisterService.startShift(cashierId, amount);
    }

    public void endShift(int cashierId) {
        cashRegisterService.endShift(cashierId);
    }

    // ==========================================
    // 🛒 CLIENT INTERFACE HELPERS
    // ==========================================

    // 1. Get Cart Total (For UI Preview before paying)
    public double getCartTotal(Map<Integer, Integer> cart) {
        return invoiceService.calculateCartTotal(cart);
    }

    // 2. Process Transaction (Now accepts 'cashGiven' for change calculation)
    public boolean processTransaction(int cashierId, Integer customerId, Map<Integer, Integer> cart, double cashGiven) {
        // Rule: Can't sell if the register is closed
        if (!cashRegisterService.hasActiveShift()) {
            System.out.println("⚠️ You must START SHIFT before selling!");
            return false;
        }
        return invoiceService.createInvoice(cashierId, customerId, cart, cashGiven);
    }

    // ==========================================
    // 📂 BROWSING & DISPLAY METHODS (New!)
    // ==========================================

    // 3. List Categories (The "Aisles")
    public void listCategories() {
        String sql = "SELECT id, name FROM categories ORDER BY id";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n   --- 📂 AISLES (CATEGORIES) ---");
            while (rs.next()) {
                System.out.printf("   [%d] %s\n", rs.getInt("id"), rs.getString("name"));
            }
        } catch (Exception e) {
            System.out.println("❌ Error listing categories: " + e.getMessage());
        }
    }

    // 4. List Products by Category (The "Shelf")
    public boolean listProductsByCategory(int categoryId) {
        String sql = "SELECT id, name, selling_price, stock_quantity FROM products WHERE category_id = ?";
        boolean foundAny = false;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, categoryId);
            ResultSet rs = ps.executeQuery();

            System.out.println("\n   --- 🥫 PRODUCTS ON SHELF ---");
            System.out.printf("   %-5s %-20s %-10s %s\n", "ID", "Name", "Price", "Stock");
            System.out.println("   ------------------------------------------------");

            while (rs.next()) {
                foundAny = true;
                System.out.printf("   %-5d %-20s $%-9.2f %d left\n",
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("selling_price"),
                        rs.getInt("stock_quantity"));
            }
            if (!foundAny) System.out.println("   (This aisle is empty!)");

        } catch (Exception e) {
            System.out.println("❌ Error listing products: " + e.getMessage());
        }
        return foundAny;
    }
}