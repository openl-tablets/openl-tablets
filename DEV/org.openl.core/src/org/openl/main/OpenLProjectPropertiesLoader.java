/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.main;

import java.io.*;
import java.util.*;

import org.openl.util.FileTool;
import org.openl.util.Log;
import org.openl.util.StringTool;

/**
 * @author snshor
 *
 */
public class OpenLProjectPropertiesLoader {

    public static final String OPENL_PROPERTIES_FNAME = "openl.project.classpath.properties";

    public static final String OPENL_CLASSPATH_PROPERTY = "openl.project.classpath";

    public static final String OPENL_CLASSPATH_SEPARATOR_PROPERTY = "openl.project.classpath.separator";

    public static final String DISPLAY_NAME_SUFFIX = ".display.name";

    public static String getOpenLPropertiesFolder(String projectHome) {
        return projectHome + "/build";
    }

    private static boolean isTheSame(String ecp, String[] cp) {
        String[] ecps = StringTool.tokenize(ecp, File.pathSeparator);
        if (ecps.length != cp.length) {
            return false;
        }
        for (int i = 0; i < ecps.length; i++) {
            boolean found = false;
            for (int j = 0; j < cp.length; j++) {
                if (cp[j].equals(ecps[i])) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }

        }
        return true;
    }

    public static void main(String[] args) throws IOException {
        String[] x = makeClasspath(".",
            System.getProperty("java.class.path"),
            ".*apache.ant.*|.*apache.commons.*|.*apache.tomcat.*|.*javacc.*");
        Arrays.sort(x);
        for (int i = 0; i < x.length; i++) {
            System.out.println(x[i]);
        }
    }

    public static String[] makeClasspath(String phome, String longPath, String excludeFilter) throws IOException {
        List<String> v = new ArrayList<>();
        String[] ecps = StringTool.tokenize(longPath, File.pathSeparator);
        for (int i = 0; i < ecps.length; i++) {
            if (excludeFilter != null && ecps[i].matches(excludeFilter)) {
                continue;
            }

            File f = FileTool.buildRelativePath(new File(phome), new File(ecps[i]));
            v.add(f.getPath());
        }

        return v.toArray(new String[0]);
    }

    /**
     * @param key
     * @return
     */
    private String getStringSplitter(Object key) {
        if (OPENL_CLASSPATH_PROPERTY.equals(key)) {
            return File.pathSeparator;
        }
        return null;
    }

    public String loadExistingClasspath(String projectHome) {
        Properties p = loadProjectProperties(projectHome);
        if (p == null) {
            return null;
        }
        return p.getProperty(OPENL_CLASSPATH_PROPERTY);

    }

    public String loadExistingClasspathSeparator(String projectHome) {
        Properties p = loadProjectProperties(projectHome);
        if (p == null) {
            return null;
        }
        return p.getProperty(OPENL_CLASSPATH_SEPARATOR_PROPERTY);

    }

    public Properties loadProjectProperties(String projectHome) {
        FileInputStream fis = null;
        try {
            Properties p = new Properties();
            fis = new FileInputStream(new File(getOpenLPropertiesFolder(projectHome), OPENL_PROPERTIES_FNAME));
            p.load(fis);

            return p;
        } catch (Exception ex) {
            return null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    Log.error(e);
                }
            }
        }

    }

    /**
     * @param p
     * @param old
     * @return
     */
    Properties mergeProperties(Properties p, Properties old) {
        if (old == null) {
            return p;
        }
        boolean newProps = false;
        for (Map.Entry<Object, Object> element : old.entrySet()) {

            Object x = p.get(element.getKey());
            if (x == null) {
                p.put(element.getKey(), element.getValue());
            } else if (!x.equals(element.getValue())) {
                newProps = true;
            }
        }

        if (newProps || p.size() != old.size()) {
            return p;
        }

        return null;
    }

    public void saveClasspath(String[] cp, String projectHome) throws IOException {
        String ecp = loadExistingClasspath(projectHome);
        if (ecp == null || !isTheSame(ecp, cp)) {
            String folder = getOpenLPropertiesFolder(projectHome);
            FileWriter fw = null;
            try {
                fw = new FileWriter(new File(folder, OPENL_PROPERTIES_FNAME));
                fw.write(OPENL_CLASSPATH_PROPERTY + "=");
                for (int i = 0; i < cp.length; i++) {
                    fw.write("\\\n" + cp[i] + File.pathSeparator);
                }
            } catch (Exception ex) {
                Log.error("Error writing " + folder + "/" + OPENL_PROPERTIES_FNAME, ex);
            } finally {
                if (fw != null) {
                    fw.close();
                }
            }

        }
    }

    public void saveProperties(String projectHome, Properties p) {
        saveProperties(projectHome, p, false);
    }

    /**
     *
     * @param projectHome
     * @param p
     * @param override - if false, only updates existing properties
     */

    public void saveProperties(String projectHome, Properties p, boolean override) {
        if (override) {
            writeProperties(projectHome, p);
        } else {
            Properties old = loadProjectProperties(projectHome);

            Properties m = mergeProperties(p, old);
            if (m != null) {
                writeProperties(projectHome, m);
            }
        }

    }

    /**
     * @param projectHome
     * @param p
     */
    public void writeProperties(String projectHome, Properties p) {
        String folder = getOpenLPropertiesFolder(projectHome);
        FileWriter fw = null;
        try {
            fw = new FileWriter(new File(folder, OPENL_PROPERTIES_FNAME));

            for (Map.Entry<Object, Object> element : p.entrySet()) {
                writeSingleProperty(element, fw);
            }

        } catch (Exception ex) {
            Log.error("Error writing " + folder + "/" + OPENL_PROPERTIES_FNAME, ex);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    Log.error("Error writing " + folder + "/" + OPENL_PROPERTIES_FNAME, e);
                }
            }
        }

    }

    /**
     * @param element
     * @param w
     * @throws IOException
     */
    public void writeSingleProperty(Map.Entry<Object, Object> element, Writer w) throws IOException {
        w.write("" + element.getKey() + "=");

        String splitter = getStringSplitter(element.getKey());

        if (splitter == null) {
            writeValue(element.getValue(), w);
            w.write("\n\n");
            return;
        }

        String[] tokens = StringTool.tokenize((String) element.getValue(), splitter);

        for (int i = 0; i < tokens.length; i++) {
            if (i + 1 < tokens.length) {
                writeValue(tokens[i] + splitter + '\\', w);
            } else {
                writeValue(tokens[i] + splitter + '\n', w);
            }
            w.write('\n');
        }

    }

    /**
     * @param value
     * @param w
     * @throws IOException
     */
    private void writeValue(Object value, Writer w) throws IOException {
        w.write((String) value);
    }

}
