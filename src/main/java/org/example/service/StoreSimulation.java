package org.example.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class StoreSimulation {

    // SEMAPHORE: Only 1 robot can be at the checkout counter at a time.
    private static final Semaphore checkoutQueue = new Semaphore(1);

    // Robot Names for fun
    private static final String[] BOT_NAMES = {"🤖 T-800", "🤖 R2-D2", "🤖 Wall-E", "🤖 Optimus", "🤖 Megatron"};

    public static void startSimulation(int cashierId, CashierService cashierService) {
        System.out.println("\n=== ⚡ STARTING CHAOS MODE (Random Shopping) ⚡ ===");

        // We launch 5 Threads (one for each bot name)
        for (String botName : BOT_NAMES) {

            Thread t = new Thread(() -> {
                try {
                    // 1. Simulate "Shopping Time" (Random sleep 0-2 seconds)
                    Random rand = new Random();
                    System.out.println(botName + " is browsing the aisles...");
                    Thread.sleep(rand.nextInt(2000));

                    // 2. Build a Random Cart
                    // They pick 1 to 3 different products
                    Map<Integer, Integer> cart = new HashMap<>();
                    int numberOfItems = rand.nextInt(3) + 1;

                    for (int j = 0; j < numberOfItems; j++) {
                        // Random Product ID between 1 and 5 (Milk, Bread, etc.)
                        int prodId = rand.nextInt(5) + 1;
                        // Random Quantity between 1 and 4
                        int qty = rand.nextInt(4) + 1;

                        cart.put(prodId, qty);
                    }

                    // 3. Get in line (Acquire Lock)
                    System.out.println(botName + " is getting in line...");
                    checkoutQueue.acquire();

                    System.out.println("🔹 " + botName + " reached the counter.");

                    // 4. Pay (We give them $5000 cash so they always have enough)
                    // We use Customer ID 0 (Guest)
                    boolean success = cashierService.processTransaction(cashierId, 0, cart, 5000.0);

                    if (success) {
                        System.out.println("😃 " + botName + " leaves happy with groceries.");
                    } else {
                        System.out.println("😡 " + botName + " leaves angry (Issue at checkout).");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // 5. Leave the counter (Release Lock) so the next robot enters
                    checkoutQueue.release();
                }
            });

            t.start();
        }
    }
}