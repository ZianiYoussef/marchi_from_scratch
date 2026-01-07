package org.example.ui;

import org.example.model.User;
import org.example.service.UserService;

import java.util.Scanner;

public class Menu {

    public static void show(User user) {
        switch (user.getRole()) {
            case "ADMIN" -> adminMenu(user);
            case "CASHIER" -> cashierMenu(user);
            case "STOCK_MANAGER" -> stockMenu(user);
            default -> System.out.println("Unknown role: " + user.getRole());
        }
    }

    private static void adminMenu(User user) {
        Scanner sc = new Scanner(System.in);
        UserService userService = new UserService();

        while (true) {
            System.out.println("\n=== ADMIN MENU (" + user.getUsername() + ") ===");
            System.out.println("1) Create user");
            System.out.println("2) List users");
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

                case "0" -> { return; }

                default -> System.out.println("❌ Invalid choice");
            }
        }
    }

    private static void cashierMenu(User user) {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n=== CASHIER MENU (" + user.getUsername() + ") ===");
            System.out.println("1) New invoice");
            System.out.println("2) Take payment");
            System.out.println("0) Logout");
            System.out.print("Choose: ");
            String choice = sc.nextLine();

            switch (choice) {
                case "1" -> System.out.println("TODO: new invoice");
                case "2" -> System.out.println("TODO: take payment");
                case "0" -> { return; }
                default -> System.out.println("Invalid choice");
            }
        }
    }

    private static void stockMenu(User user) {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n=== STOCK MANAGER MENU (" + user.getUsername() + ") ===");
            System.out.println("1) Add product");
            System.out.println("2) Stock IN");
            System.out.println("3) Stock OUT");
            System.out.println("0) Logout");
            System.out.print("Choose: ");
            String choice = sc.nextLine();

            switch (choice) {
                case "1" -> System.out.println("TODO: add product");
                case "2" -> System.out.println("TODO: stock in");
                case "3" -> System.out.println("TODO: stock out");
                case "0" -> { return; }
                default -> System.out.println("Invalid choice");
            }
        }
    }
}
