/*
 * Created on Dec 15, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.rules.webstudio.util;

/**
 * MS Word launcher.
 * 
 * @author sam
 */
public class WordLauncher {

    private String scriptPath;

    private String wdPath;
    private String wdName;
    private String wdParStart;
    private String wdParEnd;

    public WordLauncher(String scriptPath, String wdPath, String wdName, String wdParStart, String wdParEnd) {
        this.scriptPath = scriptPath;
        this.wdPath = wdPath;
        this.wdName = wdName;
        this.wdParStart = wdParStart;
        this.wdParEnd = wdParEnd;
    }

    public void launch() throws Exception {
        if (wdParStart == null || wdParStart.equals("null")) {
            wdParStart = "1";
        }
        if (wdParEnd == null || wdParEnd.equals("null")) {
            wdParEnd = wdParStart;
        }

        String[] cmdarray = { "wscript", "" + "/" + scriptPath, wdPath, wdName, wdParStart, wdParEnd };

        Runtime.getRuntime().exec(cmdarray).waitFor();
    }

    public static void launch(String scriptPath, String wdPath, String wdName, String wdParStart, String wdParEnd) throws Exception {
        new WordLauncher(scriptPath, wdPath, wdName, wdParStart, wdParEnd).launch();
    }

}
