package org.openl.tablets.tutorial7;

public class Expense {
    private String area;
    private double money;

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
    }

    public String toString() {
        return "Expense [area=" + area + ", money=" + money + "]";
    }
}
