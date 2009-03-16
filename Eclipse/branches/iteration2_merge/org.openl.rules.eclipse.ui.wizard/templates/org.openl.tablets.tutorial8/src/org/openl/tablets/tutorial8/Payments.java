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
}
