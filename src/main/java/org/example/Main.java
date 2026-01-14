package org.example;

import org.example.db.CustomerSeeder;
import org.example.db.SchemaInitializer;
import org.example.db.UserSeeder;
import org.example.model.User;
import org.example.service.AuthService;
import org.example.ui.Menu;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        SchemaInitializer.init();
        UserSeeder.seedDefaultAdmin();
        org.example.db.CustomerSeeder.seed();

        Scanner sc = new Scanner(System.in);
        AuthService auth = new AuthService();

        while (true) {
            System.out.println("\n==products= LOGIN ===");
            System.out.print("Username: ");
            String username = sc.nextLine();
            System.out.print("Password: ");
            String password = sc.nextLine();

            User user = auth.login(username, password);

            if (user == null) {
                System.out.println("❌ Invalid username or password. Try again.");
            } else {
                System.out.println("✅ Welcome " + user.getUsername() + " (" + user.getRole() + ")");
                Menu.show(user);
                System.out.println("Logged out.");
            }
        }
    }
}
