package org.openl.rules.data;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

public class MyProp {

    @Getter
    @Setter
    private String displayName;
    @Getter
    @Setter
    private String category;
    @Getter
    @Setter
    private Date effectiveDate;
    @Getter
    @Setter
    private int age;
    @Getter
    @Setter
    private Byte byteVal;
    @Getter
    @Setter
    private Short shortVal;
    @Getter
    @Setter
    private Float floatVal;
    @Getter
    @Setter
    private byte simpleByte;

}
