package org.openl.rules.cmatch.algorithm;

import java.util.LinkedList;
import java.util.List;

import org.openl.binding.IBindingContext;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.MatchNode;
import org.openl.rules.cmatch.SubValue;
import org.openl.rules.cmatch.TableRow;
import org.openl.rules.cmatch.matcher.IMatcher;
import org.openl.rules.cmatch.matcher.MatcherFactory;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.java.JavaOpenClass;

public class WeightAlgorithmCompiler extends MatchAlgorithmCompiler {
    public static final String WEIGHT = "weight";

    public static final String ROW_TOTAL_SCORE = "Total Score";
    public static final String ROW_SCORE = "Score";

    public static final int ROW_TOTAL_SCORE_IDX = 1;
    public static final int ROW_SCORE_IDX = 2;

    protected static final List<ColumnDefinition> WEIGHT_COLUMN_DEFINITION = new LinkedList<>();
    private static final WeightAlgorithmExecutor WEIGHT_EXECUTOR = new WeightAlgorithmExecutor();

    static {
        WEIGHT_COLUMN_DEFINITION.addAll(MATCH_COLUMN_DEFINITION);
        WEIGHT_COLUMN_DEFINITION.add(new ColumnDefinition(WEIGHT, false));
    }

    @Override
    protected void assignExecutor(ColumnMatch columnMatch) {
        columnMatch.setAlgorithmExecutor(WEIGHT_EXECUTOR);
    }

    @Override
    protected MatchNode buildTree(List<TableRow> rows, MatchNode[] nodes) throws SyntaxNodeException {
        MatchNode rootNode = new MatchNode(-1);

        for (int i = getSpecialRowCount(); i < rows.size(); i++) {
            MatchNode node = nodes[i];
            TableRow row = rows.get(i);
            SubValue nameSV = row.get(NAMES)[0];
            int indent = nameSV.getIndent();

            if (indent == 0) {
                rootNode.add(node);
            } else {
                String msg = "Sub node are prohibited here!";
                throw SyntaxNodeExceptionUtils.createError(msg, nameSV.getStringValue().asSourceCodeModule());
            }
        }

        return rootNode;
    }

    @Override
    protected void checkSpecialRows(ColumnMatch columnMatch) throws SyntaxNodeException {
        super.checkSpecialRows(columnMatch);

        List<TableRow> rows = columnMatch.getRows();
        checkRowName(rows.get(ROW_TOTAL_SCORE_IDX), ROW_TOTAL_SCORE);
        checkRowName(rows.get(ROW_SCORE_IDX), ROW_SCORE);
    }

    @Override
    protected List<ColumnDefinition> getColumnDefinition() {
        return WEIGHT_COLUMN_DEFINITION;
    }

    @Override
    protected int getSpecialRowCount() {
        return 3; // return values, total score, score
    }

    @Override
    protected void parseSpecialRows(IBindingContext bindingContext,
            ColumnMatch columnMatch) throws SyntaxNodeException {
        super.parseSpecialRows(bindingContext, columnMatch);

        int retValuesCount = columnMatch.getReturnValues().length;

        // total score
        MatchNode totalScore = new MatchNode(ROW_TOTAL_SCORE_IDX);
        TableRow totalScoreRow = columnMatch.getRows().get(ROW_TOTAL_SCORE_IDX);

        SubValue operationSV = totalScoreRow.get(OPERATION)[0];
        IMatcher totalScoreMatcher = MatcherFactory.getMatcher(operationSV.getString(),
            JavaOpenClass.getOpenClass(Integer.class));
        if (totalScoreMatcher == null) {
            String msg = "Column " + OPERATION + " of special row " + ROW_TOTAL_SCORE + " must be defined!";
            throw SyntaxNodeExceptionUtils.createError(msg, operationSV.getStringValue().asSourceCodeModule());
        }
        totalScore.setMatcher(totalScoreMatcher);

        parseCheckValues(bindingContext, columnMatch, totalScoreRow, totalScore, retValuesCount);
        columnMatch.setTotalScore(totalScore);

        // score
        TableRow scoreRow = columnMatch.getRows().get(ROW_SCORE_IDX);
        operationSV = scoreRow.get(OPERATION)[0];
        if (!"".equals(operationSV.getString())) {
            String msg = "Column " + OPERATION + " of special row " + ROW_SCORE + " must be empty!";
            throw SyntaxNodeExceptionUtils.createError(msg, operationSV.getStringValue().asSourceCodeModule());
        }

        // score(s)
        Object[] objScores = parseValues(bindingContext,
            columnMatch,
            scoreRow,
            JavaOpenClass.getOpenClass(Integer.class));
        int[] scores = new int[retValuesCount];
        for (int i = 0; i < retValuesCount; i++) {
            scores[i] = (Integer) objScores[i];
        }
        columnMatch.setColumnScores(scores);
    }

    @Override
    protected MatchNode[] prepareNodes(IBindingContext bindingContext,
            ColumnMatch columnMatch,
            ArgumentsHelper argumentsHelper,
            int retValuesCount) throws SyntaxNodeException {
        MatchNode[] nodes = super.prepareNodes(bindingContext, columnMatch, argumentsHelper, retValuesCount);

        List<TableRow> rows = columnMatch.getRows();

        // parse weight(s) of each row
        for (int i = getSpecialRowCount(); i < rows.size(); i++) {
            TableRow row = rows.get(i);
            SubValue weightSV = row.get(WEIGHT)[0];

            ConstantOpenField constantOpenField = RuleRowHelper.findConstantField(bindingContext, weightSV.getString());
            Integer rowWeight;
            if (constantOpenField != null && constantOpenField.getValue() != null) {
                setMetaInfoForConstant(bindingContext, columnMatch, weightSV, weightSV.getString(), constantOpenField);
                rowWeight = (Integer) RuleRowHelper.castConstantToExpectedType(bindingContext,
                    constantOpenField,
                    JavaOpenClass.getOpenClass(Integer.class));
            } else {
                IString2DataConvertor convertor = String2DataConvertorFactory.getConvertor(Integer.class);
                rowWeight = (Integer) convertor.parse(weightSV.getString(), null);
            }
            nodes[i].setWeight(rowWeight);
        }

        return nodes;
    }

    /**
     * Overrides to do nothing.
     *
     * @see #buildTree
     */
    @Override
    protected void validateTree(MatchNode rootNode, List<TableRow> rows, MatchNode[] nodes) {
        // DO NOTHING!!!
    }
}
