/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.impl;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.openl.IOpenSourceCodeModule;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.text.AbsolutePosition;
import org.openl.util.text.TextInterval;

/**
 * @author snshor
 * 
 */
public class TokenizerParser
{

    String delim;

    static final int EOF = -1;

    static Map tokenizers = new HashMap();

    static synchronized TokenizerParser getTokenizer(String delim)
    {
	TokenizerParser tp = (TokenizerParser) tokenizers.get(delim);
	if (tp == null)
	{
	    tp = new TokenizerParser(delim);
	    tokenizers.put(delim, tp);
	}

	return tp;
    }

    static public IdentifierNode[] tokenize(IOpenSourceCodeModule src,
	    String delim)
    {
	return getTokenizer(delim).parse(src);
    }

    static public IdentifierNode firstToken(IOpenSourceCodeModule src,
	    String delim)
    {
	return getTokenizer(delim).firstToken(src);
    }

    private IdentifierNode firstToken(IOpenSourceCodeModule src)
    {
	try
	{
	    Reader reader = src.getCharacterStream();

	    int c;

	    StringBuffer buf = null;
	    int startToken = 0;

	    int pos = 0;
	    for (;; pos++)
	    {
		c = reader.read();
		if (c == EOF || isDelim(c))
		{
		    if (buf != null)
		    {
			IdentifierNode node = new IdentifierNode(
				"token",
				new TextInterval(new AbsolutePosition(
					startToken), new AbsolutePosition(pos)),
				buf.toString(), src);
			return node;
		    }
		    if (c == EOF)
			break;
		} else
		{
		    if (buf == null)
		    {
			buf = new StringBuffer();
			startToken = pos;
		    }
		    buf.append((char) c);
		}
	    }

	    return new IdentifierNode("token", new TextInterval(new AbsolutePosition(0), new AbsolutePosition(0)), "", src);

	} catch (IOException e)
	{
	    throw RuntimeExceptionWrapper.wrap(e);
	}
    }

    public TokenizerParser(String delim)
    {
	this.delim = delim;
	makeTable(delim);
    }

    boolean isDelim2(int c)
    {
	return delim.indexOf(c) >= 0;
    }

    final boolean isDelim(int c)
    {
	return c < table.length && table[c];
    }

    void makeTable(String s)
    {
	int min = 0;
	for (int i = 0; i < s.length(); i++)
	{
	    if (s.charAt(i) > min)
		min = s.charAt(i);
	}

	table = new boolean[min + 1];

	for (int i = 0; i < s.length(); i++)
	{
	    table[s.charAt(i)] = true;
	}

    }

    boolean[] table = {};

    IdentifierNode[] parse(IOpenSourceCodeModule src)
    {
	try
	{
	    Vector nodes = new Vector();
	    Reader reader = src.getCharacterStream();

	    int c;

	    StringBuffer buf = null;
	    int startToken = 0;

	    int pos = 0;
	    for (;; pos++)
	    {
		c = reader.read();
		if (c == EOF || isDelim(c))
		{
		    if (buf != null)
		    {
			IdentifierNode node = new IdentifierNode(
				"token",
				new TextInterval(new AbsolutePosition(
					startToken), new AbsolutePosition(pos)),
				buf.toString(), src);
			nodes.add(node);
			buf = null;
		    }
		    if (c == EOF)
			break;
		} else
		{
		    if (buf == null)
		    {
			buf = new StringBuffer();
			startToken = pos;
		    }
		    buf.append((char) c);
		}
	    }

	    return (IdentifierNode[]) nodes.toArray(new IdentifierNode[nodes
		    .size()]);

	} catch (IOException e)
	{
	    throw RuntimeExceptionWrapper.wrap(e);
	}

    }

}
