package template;

import gen.template.TemplateJavaWrapper;

/**
 * This class shows how to execute OpenL Tablets methods using Java wrapper.
 * Looks really simple...
 */
public class Main {

    public static void main(String[] args) {
        TemplateJavaWrapper jw = new TemplateJavaWrapper();
        jw.hello1(10);
    }
}
