package org.example.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class StoreSimulation {

    // SEMAPHORE: Imagine a turnstile.
    // "1" means only 1 person can pass the counter at a time.
    private static final Semaphore checkoutQueue = new Semaphore(1);

    public static void startSimulation(int cashierId, CashierService cashierService) {
        System.out.println("\n=== 🤖 STARTING THREAD SIMULATION ===");
        System.out.println("Scenario: 5 clients rushing to buy Product #1...");

        int productEverybodyWants = 1; // Make sure Product ID 1 exists in DB!

        // Create 5 Threads (Robots)
        for (int i = 1; i <= 5; i++) {
            final int clientNum = i;

            Thread t = new Thread(() -> {
                try {
                    System.out.println("Client " + clientNum + " is shopping...");
                    Thread.sleep(new Random().nextInt(500)); // Random waiting time

                    // ACQUIRE: Ask permission to enter the checkout
                    checkoutQueue.acquire();
                    System.out.println("🔹 Client " + clientNum + " is at the counter.");

                    // Build their cart
                    Map<Integer, Integer> cart = new HashMap<>();
                    cart.put(productEverybodyWants, 5); // They want 5 items

                    // Try to pay
                    // We pass '0' as customerId (Guest)
                    boolean success = cashierService.processTransaction(cashierId, 0, cart);

                    if (success) {
                        System.out.println("😃 Client " + clientNum + " got their items!");
                    } else {
                        System.out.println("😡 Client " + clientNum + " leaves angry (Out of Stock).");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // RELEASE: Leave the counter
                    checkoutQueue.release();
                }
            });

            t.start();
        }
    }
}