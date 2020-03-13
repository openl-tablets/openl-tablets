package org.openl.rules.webstudio.web;

import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.springframework.core.env.PropertyResolver;


public class Props {

    public static String text(String key) {
        return  WebStudioUtils.getBean("environment", PropertyResolver.class).getProperty(key);
    }

    public static boolean bool(String key) {
        return Boolean.parseBoolean(text(key));
    }
}
