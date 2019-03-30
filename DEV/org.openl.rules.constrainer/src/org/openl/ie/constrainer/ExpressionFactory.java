package org.openl.ie.constrainer;

///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000
 * 320 Amboy Ave., Metuchen, NJ, 08840, USA, www.exigengroup.com
 *
 * The copyright to the computer program(s) herein
 * is the property of Exigen Group, USA. All rights reserved.
 * The program(s) may be used and/or copied only with
 * the written permission of Exigen Group
 * or in accordance with the terms and conditions
 * stipulated in the agreement/contract under which
 * the program(s) have been supplied.
 */
///////////////////////////////////////////////////////////////////////////////
/**
 * An interface for the expression factory.
 */
public interface ExpressionFactory extends Undoable {
    /**
     * Returns new or cached expression of the required class and arguments.
     */
    public Expression getExpression(Class clazz, Object[] args);

    /**
     * Returns new or cached expression of the required class, arguments and argument types. Better performance than
     * {@link #getExpression(Class,Object[])}.
     */
    public Expression getExpression(Class clazz, Object[] args, Class[] types);

    /**
     * Returns true if the cache is used.
     */
    public boolean useCache();

    /**
     * Sets the flag if the cache should be used.
     */
    public void useCache(boolean flag);

} // ~ExpressionFactory
