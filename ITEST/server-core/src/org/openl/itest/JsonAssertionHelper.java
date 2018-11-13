package org.openl.itest;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

public class JsonAssertionHelper {

    private final DocumentContext jsonDoc;

    public JsonAssertionHelper(String content) {
        this.jsonDoc = JsonPath.using(Configuration.defaultConfiguration()).parse(content);
    }

    private Object evaluateExpression(String expression) {
        return jsonDoc.read(expression);
    }

}
