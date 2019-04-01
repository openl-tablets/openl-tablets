package org.openl.rules.validation.properties.dimentional;

import org.openl.rules.dt.DecisionTableColumnHeaders;

/**
 * The abstraction that represents the column(it may be a row in transparent view) of the decision table. Decision table
 * column contains next cells:
 * <ol>
 * <li>Column type</li>
 * <li>Code expression</li>
 * <li>Parameter declaration</li>
 * <li>Title</li>
 * <li>Number of rules one by one</li>
 * </ol>
 * Check the next example:
 * <table cellspacing="2">
 * <tr>
 * <td align="center" bgcolor="#ffa500"><b>Column type</b></td>
 * <td align="center" bgcolor="#ccffff"><b>C1</b></td>
 * <td align="center" bgcolor="#ccffff"><b>C2</b></td>
 * </tr>
 * <tr>
 * <td align="center" bgcolor="#ffa500"><b>Code expression</b></td>
 * <td align="center" bgcolor="#ccffff">paramLocal1==paramInc</td>
 * <td align="center" bgcolor="#ccffff">paramLocal2==paramInc</td>
 * </tr>
 * <tr>
 * <td align="center" bgcolor="#ffa500"><b>Parameter declaration</b></td>
 * <td align="center" bgcolor="#ccffff">String paramLocal1</td>
 * <td align="center" bgcolor="#ccffff">String paramLocal2</td>
 * </tr>
 * <tr>
 * <td align="center" bgcolor="#ffa500"><b>Title</b></td>
 * <td align="center" bgcolor="#00fa9a">Local Param 1</td>
 * <td align="center" bgcolor="#00fa9a">Local Param 2</td>
 * </tr>
 * <tr>
 * <td align="center" bgcolor="#ffa500">Rule #1</td>
 * <td align="center" bgcolor="#ffff99">value11</td>
 * <td align="center" bgcolor="#ffff99">value21</td>
 * </tr>
 * <tr>
 * <td align="center" bgcolor="#ffa500">Rule #2</td>
 * <td align="center" bgcolor="#ffff99">value12</td>
 * <td align="center" bgcolor="#ffff99">value22</td>
 * </tr>
 * <tr>
 * <td align="center" bgcolor="#ffa500">Rule #3</td>
 * <td align="center" bgcolor="#ffff99">value13</td>
 * <td align="center" bgcolor="#ffff99">value23</td>
 * </tr>
 * </table>
 *
 * @author DLiauchuk
 *
 */
public interface IDecisionTableColumn {

    /**
     * Gets the type of the column. For more information see {@link DecisionTableColumnHeaders}
     *
     * @return string representation type of the column.
     */
    String getColumnType();

    /**
     * Gets the string representation of the code expression cell(the next cell after column type definition see
     * {@link #getColumnType()}).
     *
     * @return string representation of the code expression cell.
     */
    String getCodeExpression();

    /**
     * Gets the string representation of the parameter declaration cell(the next cell after code expression cell
     * see{@link #getCodeExpression()}).
     *
     * @return the string representation of the parameter declaration cell
     */
    String getParameterDeclaration();

    /**
     * Gets the string representation of the title(business name) cell(the next cell after the parameter declaration
     * cell see{@link #getParameterDeclaration()}).
     *
     * @return the string representation of the title(business name) cell
     */
    String getTitle();

    /**
     * Gets the first value for given rule index. If the {@link #getNumberOfLocalParameters()} is more than 1. Use
     * {@link #getRuleValue(int, int)} to get all values.
     *
     * @return the first value for given rule.
     */
    String getRuleValue(int ruleIndex);

    /**
     * As condition may has several local parameters see {@link #getNumberOfLocalParameters()}, so the rule values is a
     * matrix, where first index is the rule index, the second - the index of local parameter. If the
     * {@link #getNumberOfLocalParameters()} is more than 1, use this method to get all rule values.
     *
     * @param ruleIndex index of the rule
     * @param localParameterIndex index of the local parameter
     *
     * @return the value for the appropriate rule and local parameter
     */
    String getRuleValue(int ruleIndex, int localParameterIndex);

    /**
     * Condition may has several local parameters. See example.
     * <table cellspacing="2">
     * <tr>
     * <td align="center" bgcolor="#ccffff"><b>C1</b></td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#ccffff">paramLocal1 == paramIncome || paramLocal2 == paramIncome</td>
     * </tr>
     * <tr>
     * <table cellspacing="2">
     * <tr>
     * <td align="center" bgcolor="#ccffff">String paramLocal1</td>
     * <td align="center" bgcolor="#ccffff">String paramLocal2</td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#00fa9a">Local Parameter number one</td>
     * <td align="center" bgcolor="#00fa9a">Local Parameter number two</td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#ffff99">value11</td>
     * <td align="center" bgcolor="#ffff99">value21</td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#ffff99">value12</td>
     * <td align="center" bgcolor="#ffff99">value22</td>
     * </tr>
     *
     * </table>
     * </tr>
     *
     * </table>
     *
     * @return
     */
    int getNumberOfLocalParameters();

}
