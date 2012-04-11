package org.openl.util;

import java.util.HashMap;
import java.util.Map;

public class StringTransformer {

    public interface IVarHolder {

        String find(String name);

    }
    static class MapVarHolder implements IVarHolder {

        Map<String, String> map;

        public MapVarHolder(Map<String, String> map) {
            super();
            this.map = map;
        }

        public String find(String name) {
            return map.get(name);
        }

    }

    static final int NORMAL = 0, BSLASH = 1, DOLLAR = 2, IN_NAME = 3;

    boolean processBackslash = false;

    boolean reportNotFoundVarException = true;

    public static void main(String[] args) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("abc", "abc value");
        System.out.println(new StringTransformer().transform("abcd\\s${abc}", map, new StringBuilder()));
    }

    public StringTransformer() {

    }

    public StringTransformer(boolean processBackslash, boolean reportNotFoundVarException) {
        super();
        this.processBackslash = processBackslash;
        this.reportNotFoundVarException = reportNotFoundVarException;
    }

    public String transform(String src, IVarHolder vars) {
        return transform(src, vars, new StringBuilder(100)).toString();
    }

    public StringBuilder transform(String src, IVarHolder vars, StringBuilder buf) {

        int len = src.length();
        int state = NORMAL;
        StringBuilder nameBuilder = null;
        for (int i = 0; i < len; i++) {
            char c = src.charAt(i);

            switch (state) {
                case NORMAL:
                    if (c == '\\' && processBackslash) {
                        state = BSLASH;
                    } else if (c == '$') {
                        state = DOLLAR;
                    } else {
                        buf.append(c);
                    }
                    break;
                case BSLASH:
                    state = NORMAL;
                    buf.append(c);
                    break;
                case DOLLAR:
                    if (c == '{') {
                        state = IN_NAME;
                        nameBuilder = new StringBuilder();
                    } else {
                        state = NORMAL;
                        buf.append('$').append(c);
                    }
                    break;
                case IN_NAME:
                    if (c == '}') {
                        String name = nameBuilder.toString();
                        String subst = vars.find(name);
                        if (subst == null) {
                            if (reportNotFoundVarException) {
                                throw new RuntimeException("Can not find var:" + name);
                            } else {
                                buf.append("${").append(name).append('}');
                            }
                        } else {
                            buf.append(subst);
                        }
                        state = NORMAL;
                    } else {
                        nameBuilder.append(c);
                    }
            }

        }

        if (state == DOLLAR) {
            buf.append('$');
        } else if (state != NORMAL) {
            throw new RuntimeException("Can not transform the string:" + src);
        }

        return buf;

    }

    public StringBuilder transform(String src, Map<String, String> map, StringBuilder buf) {
        return transform(src, new MapVarHolder(map), buf);
    }

}
