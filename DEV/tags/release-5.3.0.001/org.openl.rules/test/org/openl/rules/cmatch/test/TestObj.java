package org.openl.rules.cmatch.test;

import org.junit.Ignore;

@Ignore("Auxiliary class")
public class TestObj {
    private String tempLevel;
    private Integer wind;

    public TestObj(String tempLevel, int wind) {
        this.tempLevel = tempLevel;
        this.wind = wind;
    }

    public String getTempLevel() {
        return tempLevel;
    }

    public Integer getWind() {
        return wind;
    }

    public void setTempLevel(String tempLevel) {
        this.tempLevel = tempLevel;
    }

    public void setWind(Integer wind) {
        this.wind = wind;
    }
}
