package org.openl.tablets.tutorial7;

public class Expense {
    private String area;
    private double money;
    private boolean paysCompany;

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

    public boolean isPaysCompany() {
        return paysCompany;
    }

    public void setPaysCompany(boolean paysCompany) {
        this.paysCompany = paysCompany;
    }

    public String toString() {
        return "Expense [area=" + area + ", money=" + money + ", paysCompany=" + paysCompany + "]";
    }
}
