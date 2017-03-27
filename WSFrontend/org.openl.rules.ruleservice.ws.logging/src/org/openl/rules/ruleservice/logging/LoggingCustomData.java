package org.openl.rules.ruleservice.logging;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LoggingCustomData {
    private String stringValue1;
    private String stringValue2;
    private String stringValue3;
    private String stringValue4;
    private String stringValue5;
    private Date dateValue1;
    private Date dateValue2;
    private Date dateValue3;
    private Long numberValue1;
    private Long numberValue2;
    private Long numberValue3;
    private Long numberValue4;
    private Long numberValue5;
    
    private Map<String, Object> values = new HashMap<String, Object>();

    public String getStringValue1() {
        return stringValue1;
    }

    public void setStringValue1(String stringValue1) {
        this.stringValue1 = stringValue1;
    }

    public String getStringValue2() {
        return stringValue2;
    }

    public void setStringValue2(String stringValue2) {
        this.stringValue2 = stringValue2;
    }

    public String getStringValue3() {
        return stringValue3;
    }

    public void setStringValue3(String stringValue3) {
        this.stringValue3 = stringValue3;
    }

    public String getStringValue4() {
        return stringValue4;
    }

    public void setStringValue4(String stringValue4) {
        this.stringValue4 = stringValue4;
    }

    public String getStringValue5() {
        return stringValue5;
    }

    public void setStringValue5(String stringValue5) {
        this.stringValue5 = stringValue5;
    }

    public Date getDateValue1() {
        return dateValue1;
    }

    public void setDateValue1(Date dateValue1) {
        this.dateValue1 = dateValue1;
    }

    public Date getDateValue2() {
        return dateValue2;
    }

    public void setDateValue2(Date dateValue2) {
        this.dateValue2 = dateValue2;
    }

    public Date getDateValue3() {
        return dateValue3;
    }

    public void setDateValue3(Date dateValue3) {
        this.dateValue3 = dateValue3;
    }

    public Long getNumberValue1() {
        return numberValue1;
    }

    public void setNumberValue1(Long numberValue1) {
        this.numberValue1 = numberValue1;
    }

    public Long getNumberValue2() {
        return numberValue2;
    }

    public void setNumberValue2(Long numberValue2) {
        this.numberValue2 = numberValue2;
    }

    public Long getNumberValue3() {
        return numberValue3;
    }

    public void setNumberValue3(Long numberValue3) {
        this.numberValue3 = numberValue3;
    }

    public Long getNumberValue4() {
        return numberValue4;
    }

    public void setNumberValue4(Long numberValue4) {
        this.numberValue4 = numberValue4;
    }

    public Long getNumberValue5() {
        return numberValue5;
    }

    public void setNumberValue5(Long numberValue5) {
        this.numberValue5 = numberValue5;
    }
    
    public Object getValue(String key){
        return values.get(key);
    }
    
    public void setValue(String key, Object value){
        this.values.put(key, value);
    }

}
