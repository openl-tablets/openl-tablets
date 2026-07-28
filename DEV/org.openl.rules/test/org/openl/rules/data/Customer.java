package org.openl.rules.data;

import lombok.Getter;
import lombok.Setter;

public class Customer {

    @Getter
    @Setter
    private String firstName;
    @Getter
    @Setter
    private String lastName;
    @Getter
    @Setter
    private int age;
    @Getter
    @Setter
    private String[] products;
    @Getter
    @Setter
    private String[] problems;

}
