package org.openl.tablets.tutorial8;

public class Loan {
    private String purpose;
    private double amount;
    private double rate;
    private double years;

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public double getYears() {
        return years;
    }

    public void setYears(double years) {
        this.years = years;
    }

    public String toString() {
        return String.format("Loan [purpose=%s, amount=%f, rate=%f, years=%f]", 
                purpose, amount, rate, years);
    }
}
