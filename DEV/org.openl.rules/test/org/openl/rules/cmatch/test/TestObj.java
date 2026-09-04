package org.openl.rules.cmatch.test;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Disabled;

@Disabled("Auxiliary class")
public class TestObj {
    @Getter
    @Setter
    private String tempLevel;
    @Getter
    @Setter
    private Integer wind;

    public TestObj(String tempLevel, int wind) {
        this.tempLevel = tempLevel;
        this.wind = wind;
    }
}
