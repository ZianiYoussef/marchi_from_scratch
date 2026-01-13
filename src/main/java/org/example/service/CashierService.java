package org.example.service;

import org.example.service.cashier.CashRegisterService;
import org.example.service.cashier.InvoiceService;
import java.util.Map;

public class CashierService {

    // We use the two specialized services we just wrote
    private final CashRegisterService cashRegisterService;
    private final InvoiceService invoiceService;

    public CashierService() {
        this.cashRegisterService = new CashRegisterService();
        this.invoiceService = new InvoiceService();
    }

    // --- Shift Management ---
    public void startShift(int cashierId, double amount) {
        cashRegisterService.startShift(cashierId, amount);
    }

    public void endShift(int cashierId) {
        cashRegisterService.endShift(cashierId);
    }

    // --- Sales ---
    public boolean processTransaction(int cashierId, Integer customerId, Map<Integer, Integer> cart) {
        // Rule: Can't sell if the register is closed
        if (!cashRegisterService.hasActiveShift()) {
            System.out.println("⚠️ You must START SHIFT before selling!");
            return false;
        }
        return invoiceService.createInvoice(cashierId, customerId, cart);
    }
}