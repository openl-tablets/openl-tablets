package org.openl.rules.data;

import java.util.Date;

public class MyProp {

    private String displayName;
    private String category;
    private Date effectiveDate;
    private int age;
    private Byte byteVal;
    private Short shortVal;
    private Float floatVal;
    private byte simpleByte;

    public byte getSimpleByte() {
        return simpleByte;
    }

    public void setSimpleByte(byte simpleByte) {
        this.simpleByte = simpleByte;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Short getShortVal() {
        return shortVal;
    }

    public void setShortVal(Short shortVal) {
        this.shortVal = shortVal;
    }

    public Float getFloatVal() {
        return floatVal;
    }

    public void setFloatVal(Float floatVal) {
        this.floatVal = floatVal;
    }

    public Byte getByteVal() {
        return byteVal;
    }

    public void setByteVal(Byte byteVal) {
        this.byteVal = byteVal;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

}
