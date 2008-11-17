/*
 * Created on May 14, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax.impl;

import java.io.Reader;

import org.openl.util.text.IPosition;
import org.openl.util.text.TextInfo;

/**
 * @author snshor
 *
 * This class is the base for all JavaCC v3.0 and compatible grammars
 */
public abstract class JavaCC30Grammar extends Grammar
{
	
	
	

	public void parse(Reader r, String parseType) {
		ReInit(r);
		parseTopNode(parseType);
		
	}

	public void parseAsMethod(Reader r)
	{
		ReInit(r);
		parseTopNode("method.body");
	}

	public void parseAsMethodHeader(Reader r)
	{
		ReInit(r);
		parseTopNode("method.header");
	}

	public void parseAsModule(Reader r)
	{
		ReInit(r);
		parseTopNode("module");
	}

	public abstract void ReInit(Reader r);

	public abstract void parseTopNode(String rootType);

	protected IPosition pos(int line, int col)
	{
		return new JavaCC30Position(line, col);
	}

	public static final class JavaCC30Position implements IPosition
	{
		static final int JAVACC30_TABSIZE = 8;

		int jcc30line;
		int jcc30col;

		public JavaCC30Position(int jcc30line, int jcc30col)
		{
			this.jcc30col = jcc30col;
			this.jcc30line = jcc30line;
		}

		/* (non-Javadoc)
		 * @see org.openl.util.text.Position#getAbsolutePosition(org.openl.util.text.TextInfo)
		 */
		public int getAbsolutePosition(TextInfo info)
		{
			if (jcc30line == 0)
				return 0;

			int line = jcc30line - 1;
			int linePos = info.getPosition(line);
			int colPos =
				TextInfo.getPosition(
					info.getLine(line),
					jcc30col - 1,
					JAVACC30_TABSIZE);

			return linePos + colPos;
		}

		/* (non-Javadoc)
		 * @see org.openl.util.text.Position#getColumn(org.openl.util.text.TextInfo, int)
		 */
		public int getColumn(TextInfo info, int tabSize)
		{
			if (jcc30line == 0)
				return 0;
			int line = jcc30line - 1;
			//			int linePos = info.getPosition(line);
			int colPos =
				TextInfo.getPosition(
					info.getLine(line),
					jcc30col - 1,
					JAVACC30_TABSIZE);

			return TextInfo.getColumn(info.getLine(line), colPos, tabSize);
		}

		/* (non-Javadoc)
		 * @see org.openl.util.text.Position#getLine(org.openl.util.text.TextInfo)
		 */
		public int getLine(TextInfo info)
		{
			return jcc30line - 1;
		}

		public String toString()
		{
			return "(" + jcc30line + "," + jcc30col + ")";
		}

	}

	/**
	 *
	 */

	public void parseAsType(Reader reader)
	{
		ReInit(reader);
		parseTopNode("type");

	}

}
