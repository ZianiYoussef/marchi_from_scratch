// src/main/java/org/example/ui/Menu.java
package org.example.ui;

import org.example.model.User;
import org.example.service.CategoryService;
import org.example.service.StockManagerService;
import org.example.service.SupplierService;
import org.example.service.UserService;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.List;
public class Menu {

    public static void show(User user) {
        switch (user.getRole()) {
            case "ADMIN" -> adminMenu(user);
            case "CASIER" -> cashierMenu(user);
            case "STOCK_MANAGER" -> stockMenu(user);
            default -> System.out.println("Unknown role: " + user.getRole());
        }
    }

    private static void adminMenu(User user) {
        Scanner sc = new Scanner(System.in);
        UserService userService = new UserService();
        CategoryService categoryService = new CategoryService();
        SupplierService supplierService = new SupplierService();

        while (true) {
            System.out.println("\n=== ADMIN MENU (" + user.getUsername() + ") ===");
            System.out.println("1) Create user");
            System.out.println("2) List users");
            System.out.println("3) Add category");
            System.out.println("4) List categories");
            System.out.println("5) Search categories");
            System.out.println("6) Add supplier");
            System.out.println("7) List suppliers");
            System.out.println("8) Search suppliers");
            System.out.println("9) List suppliers with email");
            System.out.println("0) Logout");
            System.out.print("Choose: ");
            String choice = sc.nextLine();

            switch (choice) {
                case "1" -> {
                    System.out.print("Username: ");
                    String username = sc.nextLine();

                    System.out.print("Password: ");
                    String password = sc.nextLine();

                    System.out.print("Role (ADMIN / CASHIER / STOCK_MANAGER): ");
                    String role = sc.nextLine().toUpperCase();

                    if (!role.equals("ADMIN") && !role.equals("CASHIER") && !role.equals("STOCK_MANAGER")) {
                        System.out.println("❌ Invalid role.");
                        break;
                    }

                    userService.createUser(username, password, role);
                }

                case "2" -> {
                    System.out.println("\n--- USERS LIST ---");
                    userService.getAllUsers().forEach(u ->
                            System.out.println(u.getId() + " | " + u.getUsername() + " | " + u.getRole())
                    );
                }

                case "3" -> {
                    System.out.print("Category name: ");
                    String categoryName = sc.nextLine();

                    // Using Thread (async) - show processing message
                    System.out.println("⏳ Processing category creation...");
                    CompletableFuture<Boolean> future = categoryService.addCategoryAsync(categoryName);

                    // Wait for completion and show result
                    future.thenAccept(success -> {
                        if (success) {
                            System.out.println("✅ Category creation completed!");
                        }
                    }).join(); // Wait for async operation to complete
                }

                case "4" -> {
                    System.out.println("\n--- CATEGORIES LIST ---");
                    categoryService.getAllCategories().forEach(category ->
                            System.out.println(category.toString())
                    );
                }

                case "5" -> {
                    System.out.print("Search term: ");
                    String searchTerm = sc.nextLine();

                    // Using Stream and Filter
                    System.out.println("\n--- SEARCH RESULTS ---");
                    List<org.example.model.Category> results = categoryService.filterCategoriesByName(searchTerm);

                    if (results.isEmpty()) {
                        System.out.println("No categories found matching: " + searchTerm);
                    } else {
                        results.forEach(category -> System.out.println(category.toString()));
                    }
                }

                case "6" -> {
                    System.out.print("Supplier name: ");
                    String supplierName = sc.nextLine();

                    System.out.print("Phone (optional, press Enter to skip): ");
                    String phone = sc.nextLine();
                    if (phone.trim().isEmpty()) phone = null;

                    System.out.print("Email (optional, press Enter to skip): ");
                    String email = sc.nextLine();
                    if (email.trim().isEmpty()) email = null;

                    // Using Thread (async)
                    System.out.println("⏳ Processing supplier creation...");
                    CompletableFuture<Boolean> future = supplierService.addSupplierAsync(supplierName, phone, email);

                    future.thenAccept(success -> {
                        if (success) {
                            System.out.println("✅ Supplier creation completed!");
                        }
                    }).join();
                }

                case "7" -> {
                    System.out.println("\n--- SUPPLIERS LIST ---");
                    supplierService.getAllSuppliers().forEach(supplier ->
                            System.out.println(supplier.toString())
                    );
                }

                case "8" -> {
                    System.out.print("Search term: ");
                    String searchTerm = sc.nextLine();

                    // Using Stream and Filter
                    System.out.println("\n--- SEARCH RESULTS ---");
                    List<org.example.model.Supplier> results = supplierService.filterSuppliersByName(searchTerm);

                    if (results.isEmpty()) {
                        System.out.println("No suppliers found matching: " + searchTerm);
                    } else {
                        results.forEach(supplier -> System.out.println(supplier.toString()));
                    }
                }

                case "9" -> {
                    // Using Stream and Filter to show only suppliers with email
                    System.out.println("\n--- SUPPLIERS WITH EMAIL ---");
                    List<org.example.model.Supplier> suppliersWithEmail = supplierService.filterSuppliersWithEmail();

                    if (suppliersWithEmail.isEmpty()) {
                        System.out.println("No suppliers with email found.");
                    } else {
                        suppliersWithEmail.forEach(supplier -> System.out.println(supplier.toString()));
                    }
                }

                case "0" -> { return; }

                default -> System.out.println("❌ Invalid choice");
            }
        }
    }

    // ... rest of the methods (cashierMenu, stockMenu, readInt, readDouble) remain the same ...
    private static void cashierMenu(User user) {
        Scanner sc = new Scanner(System.in);
        org.example.service.CashierService cashierService = new org.example.service.CashierService();

        while (true) {
            System.out.println("\n=== CASHIER MENU (" + user.getUsername() + ") ===");
            System.out.println("1) 🏁 Start Shift");
            System.out.println("2) 🛒 New Sale (Client Mode)"); // <--- UPDATED
            System.out.println("3) 🤖 Run Simulation");
            System.out.println("4) 🏁 End Shift");
            System.out.println("0) Logout");
            System.out.print("Choose: ");
            String choice = sc.nextLine();

            switch (choice) {
                case "1" -> {
                    double amount = readDouble(sc, "Enter opening cash amount: ");
                    cashierService.startShift(user.getId(), amount);
                }
                case "2" -> {
                    // === 🛒 CLIENT INTERFACE START ===
                    java.util.Map<Integer, Integer> cart = new java.util.HashMap<>();
                    int customerId = readInt(sc, "Enter Customer ID (0 for Guest): ");

                    while (true) {
                        double currentTotal = cashierService.getCartTotal(cart);
                        System.out.println("\n   --- 🛍️ CLIENT CART ---");
                        System.out.println("   Items in cart: " + cart.size());
                        System.out.printf("   Current Total: $%.2f\n", currentTotal);
                        System.out.println("   ----------------------");
                        System.out.println("   1. Add Item (Enter ID)");
                        System.out.println("   2. Browse Categories 📂"); // <--- NEW OPTION
                        System.out.println("   3. Remove Item");
                        System.out.println("   4. ✅ PAY & CHECKOUT");
                        System.out.println("   0. Cancel Sale");
                        System.out.print("   Action: ");
                        String subChoice = sc.nextLine();

                        if (subChoice.equals("1")) {
                            int pId = readInt(sc, "   ➡ Product ID: ");
                            int qty = readInt(sc, "   ➡ Quantity: ");
                            cart.put(pId, cart.getOrDefault(pId, 0) + qty);
                            System.out.println("   ✅ Added to cart.");

                        } else if (subChoice.equals("2")) {
                            // 👇 NEW BROWSING LOGIC 👇
                            cashierService.listCategories();
                            int catId = readInt(sc, "   ➡ Select Category ID (0 to go back): ");

                            if (catId != 0) {
                                boolean hasItems = cashierService.listProductsByCategory(catId);
                                if (hasItems) {
                                    int pId = readInt(sc, "   ➡ Enter Product ID to buy (0 to cancel): ");
                                    if (pId != 0) {
                                        int qty = readInt(sc, "   ➡ Quantity: ");
                                        cart.put(pId, cart.getOrDefault(pId, 0) + qty);
                                        System.out.println("   ✅ Added to cart.");
                                    }
                                }
                            }

                        } else if (subChoice.equals("3")) {
                            int pId = readInt(sc, "   ➡ Product ID to remove: ");
                            if (cart.containsKey(pId)) {
                                cart.remove(pId);
                                System.out.println("   🗑️ Item removed.");
                            } else {
                                System.out.println("   ⚠️ Item not in cart.");
                            }

                        } else if (subChoice.equals("4")) {
                            if (cart.isEmpty()) {
                                System.out.println("   ⚠️ Cart is empty!");
                            } else {
                                System.out.printf("   💰 TOTAL DUE: $%.2f\n", currentTotal);
                                double cash = readDouble(sc, "   💵 Cash Given: ");

                                if (cash < currentTotal) {
                                    System.out.println("   ❌ Insufficient funds.");
                                } else {
                                    boolean success = cashierService.processTransaction(user.getId(), customerId, cart, cash);
                                    if (success) break;
                                }
                            }
                        } else if (subChoice.equals("0")) {
                            break;
                        }
                    }
                }
                case "3" -> {
                    // Note: Simulation needs update to pass a dummy "cash amount"
                    // Since we changed the method signature, we need to fix StoreSimulation.java too!
                    org.example.service.StoreSimulation.startSimulation(user.getId(), cashierService);
                }
                case "4" -> cashierService.endShift(user.getId());
                case "0" -> { return; }
                default -> System.out.println("Invalid choice");
            }
        }
    }

    private static void stockMenu(User user) {
        Scanner sc = new Scanner(System.in);
        StockManagerService stockService = new StockManagerService();

        while (true) {
            System.out.println("\n=== STOCK MANAGER MENU (" + user.getUsername() + ") ===");
            System.out.println("1) Add product");
            System.out.println("2) resuply stock");
            System.out.println("3) liquidate stock");
            System.out.println("4) List categories");
            System.out.println("5) List suppliers");
            System.out.println("6) check stock ");
            System.out.println("0) Logout");
            System.out.print("Choose: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    System.out.print("Name: ");
                    String name = sc.nextLine();

                    System.out.print("Barcode: ");
                    String barcode = sc.nextLine();

                    int categoryId = readInt(sc, "Category ID: ");
                    int supplierId = readInt(sc, "Supplier ID: ");
                    double purchasePrice = readDouble(sc, "Purchase price: ");
                    double sellingPrice = readDouble(sc, "Selling price: ");
                    int initialQty = readInt(sc, "Initial stock quantity: ");

                    stockService.addProduct(
                            name, barcode, categoryId, supplierId,
                            purchasePrice, sellingPrice, initialQty,
                            user.getId()
                    );
                }

                case "2" -> {
                    int productId = readInt(sc, "Product ID: ");
                    int qty = readInt(sc, "Quantity to add (IN): ");
                    stockService.stockIn(productId, qty, user.getId());
                }

                case "3" -> {
                    int productId = readInt(sc, "Product ID: ");
                    int qty = readInt(sc, "Quantity to remove (OUT): ");
                    stockService.stockOut(productId, qty, user.getId());
                }
                case "4" -> stockService.listCategories();
                case "5" -> stockService.listSuppliers();
                case "6" -> {
                    System.out.print("Enter product ID or name: ");
                    String q = sc.nextLine();
                    stockService.checkStock(q);
                }

                case "0" -> {
                    return;
                }

                default -> System.out.println("Invalid choice");
            }
        }
    }

    // ---- small helpers to avoid Scanner bugs ----
    private static int readInt(Scanner sc, String msg) {
        while (true) {
            try {
                System.out.print(msg);
                String s = sc.nextLine().trim();
                return Integer.parseInt(s);
            } catch (Exception e) {
                System.out.println("❌ Please enter a valid integer.");
            }
        }
    }

    private static double readDouble(Scanner sc, String msg) {
        while (true) {
            try {
                System.out.print(msg);
                String s = sc.nextLine().trim();
                return Double.parseDouble(s);
            } catch (Exception e) {
                System.out.println("❌ Please enter a valid number.");
            }
        }
    }
}