package com.example.client;

import com.example.MyService;
import com.example.domain.MyRequest;

import org.openl.rules.calc.SpreadsheetResult;

/**
 * A plain Java consumer that implements the OpenL-generated service interface.
 * It compiles only if MyService (api jar), MyRequest (datatype) and SpreadsheetResult all resolve on the classpath.
 */
public class Client implements MyService {
    @Override
    public SpreadsheetResult calc(MyRequest request) {
        return null;
    }
}
