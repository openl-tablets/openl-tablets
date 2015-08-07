/*
 * Created on Aug 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.main;

import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.openl.OpenL;
import org.openl.binding.exception.MethodNotFoundException;
import org.openl.conf.IUserContext;
import org.openl.conf.OpenConfigurationException;
import org.openl.conf.UserContext;
import org.openl.engine.OpenLManager;
import org.openl.exception.OpenLRuntimeException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.SourceLocator;
import org.openl.util.text.TextInfo;

/**
 * Starter for the openL.
 *
 * @author sam
 */
public class OpenLMain implements SourceCodeURLConstants { 
    public static final String ORG_OPENL = "org.openl";

    static public final String ARG_SOURCE_FILE_NAME = "-file";

    static public final String ARG_OPENL_NAME = "-openl";

    public String openlName;

    public String sourceFileName;

    public String methodName;

    public String[] methodArgs = {};

    public PrintStream err = System.err;

    public PrintStream out = System.out;

    IOpenSourceCodeModule openlSource;

    /**
     * Returns openl name for a given filename in the form: org.openl.<filename-extension>
     *
     * Examples: - file.j -> org.openl.j - file.j.simple -> org.openl.j.simple -
     * file.j.xml.dom -> org.openl.j.xml.dom
     */
    static public String getOpenlName(String filename) {
        filename = filename.replace('\\', '/');

        int idx = filename.lastIndexOf('/');
        if (idx < 0) {
            idx = 0;
        }

        idx = filename.indexOf('.', idx);
        if (idx < 0) {
            throw new RuntimeException("File: " + filename + " does not have an extension");
        }
        
        String longExt =  filename.substring(idx);
        
        String openlSuffix = extensionsMap.get(longExt); 
        
        if (openlSuffix == null)
        {
            idx = filename.lastIndexOf('.', idx);
            String shortExt = filename.substring(idx);
            openlSuffix = extensionsMap.get(shortExt);
            if (openlSuffix == null)
                openlSuffix = longExt;
        }    

        return ORG_OPENL + openlSuffix; 
    }
    
    static Map<String, String> extensionsMap = new HashMap<String, String>();
    
    static public Map<String, String> getExtensionsMap()
    {
        return extensionsMap;
    }
    
    static synchronized public void registerExtension(String ext, String mapped)
    {
        extensionsMap.put(ext, mapped);
    }

    

    // OpenL openl;

    /**
     * Trivial 'main' - delegates to non-static 'run'.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("OpenL main(),  Version " + OpenLVersion.getVersion()
                + " " + OpenLVersion.getURL() + " (c) " + OpenLVersion.getCopyrightYear());

        new OpenLMain(null).run(args);
    }

    // TextInfo openlTextInfo;

    // SourceLocator openlSourceLocator;

    public OpenLMain(String openlName) {
        this.openlName = openlName;
    }

    /**
     * Loads file as string.
     */
    // static String loadFile(String filename) throws Exception
    // {
    // FileReader reader = new FileReader(filename);
    //
    // int c;
    // StringBuilder buf = new StringBuilder();
    // while ((c = reader.read()) != -1)
    // buf.append((char) c);
    //
    // return buf.toString();
    // }
    //
    // TextInfo getOpenlTextInfo()
    // {
    // if (openlTextInfo == null)
    // {
    // openlTextInfo = new TextInfo(openlSource.getCode());
    // }
    // return openlTextInfo;
    // }
    SourceLocator getOpenlSourceLocator(IOpenSourceCodeModule src) {
        // if (openlTextInfo == null)
        // {
        // openlSourceLocator = new SourceLocator(getOpenlTextInfo());
        // }
        // return openlSourceLocator;

        return new SourceLocator(new TextInfo(src.getCode()));
    }

    /**
     * Returns user context: - home - current directory, launches start in the
     * project directory - classLoader - nothing, all required classes are
     * already in the project
     */
    IUserContext getUserContext() throws Exception {
        String userHome = new File(".").getCanonicalPath();

        ClassLoader cl = new URLClassLoader(new URL[0]);

        return new UserContext(cl, userHome);
    }

    /**
     * Returns true iff s has any char from chars.
     */
    boolean hasChars(String s, String chars) {
        for (int i = 0; i < chars.length(); i++) {
            if (s.indexOf(chars.charAt(i)) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper to make command-line arguments to run OpenlMain. Usage: - olm =
     * new OpenlMain() - olm.sourceFileName = ... - ..... - String commandLine =
     * "java OpenlMain " + olm.makeArgs());
     */
    public String makeArgs() {
        String s = "";

        s += " " + ARG_SOURCE_FILE_NAME + " " + quote(sourceFileName);

        if (openlName != null) {
            s += " " + ARG_OPENL_NAME + " " + openlName;
        }

        if (methodName != null) {
            s += " " + methodName + " " + methodArgsToString();
        }

        return s;
    }

    String methodArgsToString() {
        int size = methodArgs != null ? methodArgs.length : 0;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < size; i++) {
            result.append(quote(methodArgs[i])).append(' ');
        }
        return result.toString();
    }

    /**
     * Command-line arguments parsing.
     */
    boolean parseArgs(String[] args) {
        int i = 0;

        // parse switches
        while (i < args.length && args[i].startsWith("-")) {
            if (i + 1 >= args.length) {
                System.out.println(usage());
                throw new IllegalArgumentException(args[i]);
            }

            if (ARG_OPENL_NAME.equals(args[i])) {
                openlName = args[i + 1];
            } else if (ARG_SOURCE_FILE_NAME.equals(args[i])) {
                sourceFileName = args[i + 1];
            } else {
                System.out.println(usage());
                return false;
            }

            i += 2;
        }

        if (sourceFileName == null) {
            out.println(usage());
            // throw new IllegalArgumentException(ARG_SOURCE_FILE_NAME);
            return false;
        }

        // parse method name and arguments
        if (i < args.length) {
            methodName = args[i++];

            if (i < args.length) {
                int argsCount = args.length - i;

                methodArgs = new String[argsCount];

                System.arraycopy(args, i, methodArgs, 0, argsCount);
            }
        }
        return true;
    }

    /**
     * Quote string if it contains delimiters or quotes.
     */
    String quote(String s) {
        if (s.indexOf('"') >= 0) {
            StringBuilder result = new StringBuilder(s.length() * 2);
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) == '"') {
                    result.append('\\');
                }
                result.append(s.charAt(i));
            }
            return '"' + result.toString() + '"';
        }

        String delimiters = " ,'\t";
        if (hasChars(s, delimiters)) {
            return '"' + s + '"';
        }

        return s;
    }

    void run(String[] args) {
        if (!parseArgs(args)) {
            return;
        }

        // out.println(getClass().getName() + makeArgs());

        openlSource = new FileSourceCodeModule(new File(sourceFileName), null);

        safeRunOpenl(openlName != null ? openlName : getOpenlName(sourceFileName), openlSource, methodName,
                new Object[] { methodArgs });
    }

    public Object safeRunOpenl(String openlName, IOpenSourceCodeModule source, String method, Object[] params) {

        try {
            openlSource = source;
            methodName = method;
            OpenL openl = OpenL.getInstance(openlName);

            if (methodName != null) {
                return OpenLManager.runMethod(openl, openlSource, methodName, null, params);
            } else {
                return OpenLManager.runScript(openl, openlSource);
            }

        } catch (OpenConfigurationException e) {
            e.printStackTrace();
            return null;
        } catch (OpenLRuntimeException rt) {
            rt.printStackTrace(err);
            return null;
        } catch (SyntaxNodeException exception) {
            err.println(exception.getMessage());
            return null;
        } catch (MethodNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
        // finally
        // {
        // return null;
        // }

    }

    String usage() {
        String USAGE = "usage:" + " java {0} " + ARG_SOURCE_FILE_NAME + " <sourcefile> " + ARG_OPENL_NAME
                + " [<openl>] " + " [<method> (arg)*] ";

        return MessageFormat.format(USAGE, new Object[] { getClass().getName(), });
    }

}
