/*
 * Created on Dec 15, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.rules.webtools;



/**
 * 
 * @author sam
 */
public class WordLauncher
{
  String scriptName;
  

  String wdPath;
  String wdName;
  String wdParStart;
  String wdParEnd;
  
  public void launch() throws Exception
  {
  	if (wdParStart == null || wdParStart.equals("null"))
  		wdParStart = "1";
  	if (wdParEnd == null || wdParEnd.equals("null"))
  		wdParEnd = wdParStart;
  	
    String[] cmdarray =
      { "wscript", Launcher.getLaunchScriptsDir() + "/" + scriptName, wdPath, wdName, wdParStart, wdParEnd };

    Runtime.getRuntime().exec(cmdarray).waitFor();
  }

  public WordLauncher(
    String scriptName,
    String wdPath,
    String wdName,
    String wdParStart,
    String wdParEnd)
  {
    this.scriptName = scriptName;
    this.wdPath = wdPath;
    this.wdName = wdName;
    this.wdParStart = wdParStart;
    this.wdParEnd = wdParEnd;
  }

  static public void launch(String url) throws Exception
  {
  	WordUrlParser p = new WordUrlParser();
    p.parse(url);

    launch(p.scriptName, p.wdPath, p.wdName, p.wdParStart, p.wdParEnd);
  }

  static public void launch(
    String scriptName,
    String wdPath,
    String wdName,
    String wdParStart,
    String wdParEnd)
    throws Exception
  {
    WordLauncher l =
      new WordLauncher(scriptName, wdPath, wdName, wdParStart, wdParEnd);

    l.launch();
  }

  
  public static void main(String[] args) throws Exception
	{
		launch("LaunchWord.vbs", "C:\\___DEV\\org.openl.dev_3.1\\eclipse\\workspace\\com.exigen.srp.idat\\docs","WorkflowUIAnalysis_5.6.6_Final.doc", "1240", "1250" );
	}
  
}
