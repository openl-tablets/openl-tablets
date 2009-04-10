/*
 * Created on Sep 5, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.eclipse.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.jface.text.IRegion;
import org.openl.eclipse.util.StringMatcher;
import org.openl.main.SourceCodeURLConstants;

/**
 * @author sam
 *
 */
public class ConsoleLineTrackerTool
{
  
  static final public int DEFAULT_PRIORITY = 100;
  
  protected static List ALL = new ArrayList();

  static class HyperlinkHandlerInfo
  {
    IHyperlinkFactory factory;
//    String matchString;
    int priority;
    StringMatcher includeMatcher;
	  StringMatcher excludeMatcher;

    HyperlinkHandlerInfo(
      IHyperlinkFactory factory,
      String matchIncludeString,
	    String matchExcludeString,
      int priority)
    {
      this.factory = factory;
//      this.matchString = matchString;
      this.priority = priority;
      this.includeMatcher = new StringMatcher(matchIncludeString, false, false);
      if (matchExcludeString != null)
	    this.excludeMatcher = new StringMatcher(matchExcludeString, false, false);
    }
  }

  public static void register(
    IHyperlinkFactory factory,
    String matchIncludeString,
	  String matchExcludeString,
    
    int priority)
  {
    synchronized (ALL)
    {
      int i;
      for (i = 0; i < ALL.size(); i++)
      {
        HyperlinkHandlerInfo info = (HyperlinkHandlerInfo)ALL.get(i);
        if (priority >= info.priority)
          break;
      }

      ALL.add(i, new HyperlinkHandlerInfo(factory, matchIncludeString, matchExcludeString, priority));
    }
  }

  protected IConsole console;

  protected static String getUrl(String s)
  {
    if (s.startsWith(SourceCodeURLConstants.AT_PREFIX))
      return s.substring(SourceCodeURLConstants.AT_PREFIX.length());

    return null;
  }

  static HyperlinkHandlerInfo getInfo(String text)
  {
    synchronized (ALL)
    {
      for (int i = 0; i < ALL.size(); i++)
      {
        HyperlinkHandlerInfo info = (HyperlinkHandlerInfo)ALL.get(i);
        if (info.includeMatcher.match(text) && (info.excludeMatcher == null || !info.excludeMatcher.match(text)))
          return info;
      }

      return null;
    }
  }

  static public void lineAppended(IConsole console, IRegion line)
  {
    try
    {
      int textOffset = line.getOffset();
      int textLength = line.getLength();
      String text = console.getDocument().get(textOffset, textLength);

      HyperlinkHandlerInfo info = getInfo(text);
      String url = getUrl(text);

      if (info == null || url == null)
        return;

      console.addLink(
        info.factory.createHyperlink(console, url),
        textOffset,
        textLength);
    }
    catch (Exception e)
    {}

  }
}
