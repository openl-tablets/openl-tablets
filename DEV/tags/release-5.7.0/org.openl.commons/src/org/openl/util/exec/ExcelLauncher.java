/*
 * Created on Dec 15, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.util.exec;

/**
 * MS Excel launcher.
 *
 * @author sam
 */
public class ExcelLauncher {

    String scriptPath;

    String wbPath;
    String wbName;
    String wsName;
    String range;

    public ExcelLauncher(String scriptPath, String wbPath, String wbName, String wsName, String range) {
        this.scriptPath = scriptPath;
        this.wbPath = wbPath;
        this.wbName = wbName;
        this.wsName = wsName;
        this.range = range;
    }

    public static void launch(String scriptPath, String wbPath, String wbName, String wsName, String range)
            throws Exception {
        ExcelLauncher l = new ExcelLauncher(scriptPath, wbPath, wbName, wsName, range);
        l.launch();
    }

    public void launch() throws Exception {
        if (range == null || range.equals("null")) {
            range = "A1";
        }
        String[] cmdarray = { "wscript", scriptPath, wbPath, wbName, wsName == null ? "1" : wsName, range, };

        Runtime.getRuntime().exec(cmdarray).waitFor();
    }

}
