package org.openl.generated.beans;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

public class Policy {

    protected String id;
    protected Date effectiveDate;
    protected String transaction;
    protected Vehicle[] vehicles;
    protected Integer[] brandCodes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    public Vehicle[] getVehicles() {
        return vehicles;
    }

    public void setVehicles(Vehicle[] vehicles) {
        this.vehicles = vehicles;
    }

    public Integer[] getBrandCodes() {
        return brandCodes;
    }

    public void setBrandCodes(Integer[] brandCodes) {
        this.brandCodes = brandCodes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Policy policy = (Policy) o;
        return Objects.equals(id, policy.id) &&
                Objects.equals(effectiveDate, policy.effectiveDate) &&
                Objects.equals(transaction, policy.transaction) &&
                Arrays.equals(vehicles, policy.vehicles) &&
                Arrays.equals(brandCodes, policy.brandCodes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, effectiveDate, transaction);
        result = 31 * result + Arrays.hashCode(vehicles);
        result = 31 * result + Arrays.hashCode(brandCodes);
        return result;
    }

    @Override
    public String toString() {
        return "Policy{" +
                "id='" + id + '\'' +
                ", effectiveDate=" + effectiveDate +
                ", transaction='" + transaction + '\'' +
                ", vehicles=" + Arrays.toString(vehicles) +
                ", brandCodes=" + Arrays.toString(brandCodes) +
                '}';
    }
}
