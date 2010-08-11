package org.openl.tablets.tutorial7;

public class Issue {
    private String area;
    private boolean isMundane;
    private double money;

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public boolean isMundane() {
        return isMundane;
    }

    public void setMundane(boolean isMundane) {
        this.isMundane = isMundane;
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
    }

    public String toString() {
        return "Issue [area=" + area + ", isMundane=" + isMundane + ", money=" + money + "]";
    }
}
