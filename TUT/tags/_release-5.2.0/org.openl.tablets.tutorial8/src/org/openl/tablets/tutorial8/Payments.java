package org.openl.tablets.tutorial8;

import java.util.ArrayList;
import java.util.List;

public class Payments {
    private final List<Double> amounts;
    private final List<Double> commissions;

    public Payments() {
        amounts = new ArrayList<Double>();
        commissions = new ArrayList<Double>();
    }

    public int getYears() {
        return amounts.size();
    }

    public double getAmount(int yearIndex) {
        return amounts.get(yearIndex);
    }

    public double getCommission(int yearIndex) {
        return commissions.get(yearIndex);
    }

    public double getToPay(int yearIndex) {
        return getAmount(yearIndex) + getCommission(yearIndex);
    }

    public void addPayment(double amount, double commission) {
        amounts.add(amount);
        commissions.add(commission);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(256);
        sb.append("Payments [");
        for (int i = 0; i < getYears(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format("[year=%d, amount=%.2f, commission=%.2f]", (i+1), getAmount(i), getCommission(i)));
        }
        sb.append("]");

        return sb.toString();
    }
}
