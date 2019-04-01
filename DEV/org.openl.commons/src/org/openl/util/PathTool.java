/*
 * Created on Dec 2, 2004
 *
 * Developed by OpenRules, Inc. 2003,2004
 */
package org.openl.util;

import java.util.ArrayList;
import java.util.List;

/**
 * @author snshor
 */
public class PathTool {

    static public String mergePath(String p1, String p2) {
        String[] pp1 = splitPath(p1);
        String[] pp2 = splitPath(p2);

        List<String> result = new ArrayList<>(pp1.length + pp2.length - 1);

        int len = p1.endsWith("/") ? pp1.length : pp1.length - 1;

        for (int i = 0; i < len; ++i) {
            if (pp1[i].equals(".")) {
                continue;
            }
            if (pp1[i].equals("..")) {
                if (result.isEmpty() || result.get(result.size() - 1).equals("..")) {
                } else {
                    result.remove(result.size() - 1);
                    continue;
                }
            }

            result.add(pp1[i]);
        }

        for (int i = 0; i < pp2.length; ++i) {
            if (pp2[i].equals(".")) {
                continue;
            }
            if (pp2[i].equals("..")) {
                result.remove(result.size() - 1);
                continue;
            }
            result.add(pp2[i]);
        }

        StringBuilder buf = new StringBuilder(50);

        if (p1.startsWith("/")) {
            buf.append('/');
        }

        for (int i = 0; i < result.size(); i++) {
            if (i > 0) {
                buf.append('/');
            }
            buf.append(result.get(i));
        }

        return buf.toString();

    }

    public static String[] splitPath(String path) {
        if (path.indexOf('/') >= 0) {
            return StringTool.tokenize(path, "/");
        }
        return StringTool.tokenize(path, "\\");
    }

}
