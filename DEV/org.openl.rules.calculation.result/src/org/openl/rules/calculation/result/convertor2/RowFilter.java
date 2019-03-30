package org.openl.rules.calculation.result.convertor2;

/*-
 * #%L
 * OpenL - DEV - Rules - Calculation Result
 * %%
 * Copyright (C) 2015 - 2017 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

public interface RowFilter {

    boolean excludeRow(String rowName);

}
