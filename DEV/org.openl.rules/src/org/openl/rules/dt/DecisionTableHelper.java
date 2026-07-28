package org.openl.rules.dt;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.openl.base.INamedThing;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.NumericStringComparator;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.domain.IDomain;
import org.openl.engine.OpenLManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.fuzzy.OpenLFuzzyUtils;
import org.openl.rules.fuzzy.OpenLFuzzyUtils.FuzzyResult;
import org.openl.rules.fuzzy.Token;
import org.openl.rules.helpers.ArraySplitter;
import org.openl.rules.helpers.CharRange;
import org.openl.rules.helpers.DateRange;
import org.openl.rules.helpers.DateRangeParser;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.IntRange;
import org.openl.rules.helpers.StringRange;
import org.openl.rules.helpers.StringRangeParser;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.binding.DTColumnsDefinition;
import org.openl.rules.lang.xls.binding.ExpressionIdentifier;
import org.openl.rules.lang.xls.binding.XlsDefinitions;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.load.SimpleSheetLoader;
import org.openl.rules.lang.xls.load.SimpleWorkbookLoader;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.DecisionTableMetaInfoReader;
import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IParameterDeclaration;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.AOpenClass;
import org.openl.types.impl.BelongsToModuleOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.IOUtils;

public final class DecisionTableHelper {

    public static final String HORIZONTAL_VERTICAL_CONDITIONS_SPLITTER = "/";
    private static final String RET1_COLUMN_NAME = DecisionTableColumnHeaders.RETURN.getHeaderKey() + "1";
    private static final String CRET1_COLUMN_NAME = DecisionTableColumnHeaders.COLLECT_RETURN.getHeaderKey() + "1";
    private static final List<Class<?>> INT_TYPES = Arrays.asList(byte.class,
            short.class,
            int.class,
            long.class,
            java.lang.Byte.class,
            java.lang.Short.class,
            Integer.class,
            Long.class,
            BigInteger.class);
    private static final List<Class<?>> DOUBLE_TYPES = Arrays
            .asList(float.class, double.class, java.lang.Float.class, java.lang.Double.class, BigDecimal.class);
    private static final List<Class<?>> CHAR_TYPES = Arrays.asList(char.class, Character.class);
    private static final List<Class<?>> STRING_TYPES = Arrays.asList(String.class);
    private static final List<Class<?>> DATE_TYPES = Collections.singletonList(Date.class);
    private static final List<Class<?>> RANGE_TYPES = Arrays
            .asList(IntRange.class, DoubleRange.class, CharRange.class, StringRange.class, DateRange.class);

    private static final List<Class<?>> IGNORED_CLASSES_FOR_COMPOUND_TYPE = Arrays.asList(null,
            byte.class,
            short.class,
            int.class,
            long.class,
            float.class,
            double.class,
            char.class,
            void.class,
            java.lang.Byte.class,
            java.lang.Short.class,
            Integer.class,
            Long.class,
            java.lang.Float.class,
            java.lang.Double.class,
            Character.class,
            String.class,
            BigInteger.class,
            BigDecimal.class,
            Date.class,
            IntRange.class,
            DoubleRange.class,
            CharRange.class,
            StringRange.class,
            DateRange.class,
            Object.class,
            Map.class,
            SortedMap.class,
            Set.class,
            SortedSet.class,
            List.class,
            Collections.class,
            ArrayList.class,
            LinkedList.class,
            HashSet.class,
            LinkedHashSet.class,
            HashMap.class,
            TreeSet.class,
            TreeMap.class,
            LinkedHashMap.class);

    private static final String[] EMPTY_STRING_ARRAY = new String[]{};

    private DecisionTableHelper() {
    }

    static boolean isValidConditionHeader(String s) {
        return s != null && s.length() >= 2 && s.charAt(0) == DecisionTableColumnHeaders.CONDITION.getHeaderKey()
                .charAt(0) && s.substring(1).chars().allMatch(Character::isDigit);
    }

    static boolean isValidHConditionHeader(String headerStr) {
        return headerStr != null && headerStr.startsWith(
                DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey()) && headerStr.length() > 2 && headerStr
                .substring(2)
                .chars()
                .allMatch(Character::isDigit);
    }

    static boolean isValidMergedConditionHeader(String headerStr) {
        return headerStr != null && headerStr.startsWith(
                DecisionTableColumnHeaders.MERGED_CONDITION.getHeaderKey()) && headerStr.length() > 2 && headerStr
                .substring(2)
                .chars()
                .allMatch(Character::isDigit);
    }

    static boolean isValidActionHeader(String s) {
        return s != null && s.length() >= 2 && s.charAt(0) == DecisionTableColumnHeaders.ACTION.getHeaderKey()
                .charAt(0) && s.substring(1).chars().allMatch(Character::isDigit);
    }

    static boolean isValidRetHeader(String s) {
        return s != null && s.length() >= 3 && s.startsWith(DecisionTableColumnHeaders.RETURN
                .getHeaderKey()) && (s.length() == 3 || s.substring(3).chars().allMatch(Character::isDigit));
    }

    static boolean isValidKeyHeader(String s) {
        return s != null && s.length() >= 3 && s.startsWith(DecisionTableColumnHeaders.KEY
                .getHeaderKey()) && (s.length() == 3 || s.substring(3).chars().allMatch(Character::isDigit));
    }

    static boolean isValidCRetHeader(String s) {
        return s != null && s.length() >= 4 && s.startsWith(DecisionTableColumnHeaders.COLLECT_RETURN
                .getHeaderKey()) && (s.length() == 4 || s.substring(4).chars().allMatch(Character::isDigit));
    }

    static boolean isValidRuleHeader(String s) {
        return Objects.equals(s, DecisionTableColumnHeaders.RULE.getHeaderKey());
    }

    static boolean isConditionHeader(String s) {
        return isValidConditionHeader(s) || isValidHConditionHeader(s) || isValidMergedConditionHeader(s);
    }

    /**
     * Creates virtual headers for condition and return columns to load simple Decision Table as an usual Decision Table
     *
     * @param decisionTable method description for simple Decision Table.
     * @param originalTable The original body of simple Decision Table.
     * @return prepared usual Decision Table.
     */
    static ILogicalTable preprocessDecisionTableWithoutHeaders(TableSyntaxNode tableSyntaxNode,
                                                               DecisionTable decisionTable,
                                                               ILogicalTable originalTable,
                                                               XlsModuleOpenClass module,
                                                               IBindingContext bindingContext) throws OpenLCompilationException {
        IWritableGrid virtualGrid = createVirtualGrid();
        var isSmartLookupAndResultTitleInFirstRow = isSmartLookupAndResultTitleInFirstRow(tableSyntaxNode,
                originalTable);
        writeVirtualHeaders(tableSyntaxNode,
                decisionTable,
                originalTable,
                virtualGrid,
                isSmartLookupAndResultTitleInFirstRow,
                module,
                new IdentityHashMap<>(),
                bindingContext);
        if (isSmartLookupAndResultTitleInFirstRow) {
            originalTable = cutResultTitleInFirstRow(originalTable);
        }
        // If the new table header size bigger than the size of the old table we
        // use the new table size
        int sizeOfVirtualGridTable = virtualGrid.getMaxColumnIndex(0) < originalTable.getSource()
                .getWidth() ? originalTable.getSource().getWidth() - 1 : virtualGrid.getMaxColumnIndex(0) - 1;
        var virtualGridTable = new GridTable(0,
                0,
                IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1,
                sizeOfVirtualGridTable,
                virtualGrid);

        var grid = new CompositeGrid(new IGridTable[]{virtualGridTable, originalTable.getSource()}, true);
        // If the new table header size bigger than the size of the old table we
        // use the new table size
        int sizeofGrid = virtualGridTable.getWidth() < originalTable.getSource().getWidth() ? originalTable.getSource()
                .getWidth() - 1 : virtualGridTable.getWidth() - 1;

        return LogicalTableHelper.logicalTable(new GridTable(0,
                0,
                originalTable.getSource().getHeight() + IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1,
                sizeofGrid,
                grid));
    }

    private static FuzzyContext buildFuzzyContext(TableSyntaxNode tableSyntaxNode,
                                                  DecisionTable decisionTable,
                                                  int numberOfHConditions,
                                                  IBindingContext bindingContext) {
        final ParameterTokens parameterTokens = buildParameterTokens(decisionTable);
        if (numberOfHConditions == 0) {
            IOpenClass returnType = getCompoundReturnType(tableSyntaxNode, decisionTable, bindingContext);
            if (isCompoundReturnType(returnType)) {
                var returnTypeFuzzyTokens = OpenLFuzzyUtils
                        .tokensMapToOpenClassWritableFieldsRecursively(returnType, returnType.getName(), 1);
                var returnTokens = returnTypeFuzzyTokens.keySet().toArray(new Token[]{});
                return new FuzzyContext(parameterTokens, returnTokens, returnTypeFuzzyTokens, returnType);
            }
        }
        return new FuzzyContext(parameterTokens);
    }

    public static boolean isSmartLookupAndResultTitleInFirstRow(TableSyntaxNode tableSyntaxNode,
                                                                ILogicalTable originalTable) {
        if (isSmartLookupTable(tableSyntaxNode) && StringUtils
                .isNotBlank(originalTable.getCell(0, 0).getStringValue())) {
            var firstCellHeight = originalTable.getSource().getCell(0, 0).getHeight();
            var width = originalTable.getSource().getWidth();
            var w = originalTable.getSource().getCell(0, 0).getWidth();
            while (w < width) {
                var cell = originalTable.getSource().getCell(w, 0);
                if (cell.getHeight() != firstCellHeight || StringUtils.isNotBlank(cell.getStringValue())) {
                    return false;
                }
                w = w + cell.getWidth();
            }
            if (firstCellHeight < originalTable.getSource().getHeight()) {
                return originalTable.getSource().getCell(0, firstCellHeight).getWidth() != width;
            }
        }
        return false;
    }

    public static ILogicalTable cutResultTitleInFirstRow(ILogicalTable originalTable) {
        return originalTable.getSubtable(0, 1, originalTable.getWidth(), originalTable.getHeight() - 1);
    }

    private static void writeVirtualHeaders(TableSyntaxNode tableSyntaxNode,
                                            DecisionTable decisionTable,
                                            ILogicalTable originalTable,
                                            IWritableGrid grid,
                                            boolean isSmartLookupAndResultTitleInFirstRow,
                                            XlsModuleOpenClass module,
                                            IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache,
                                            IBindingContext bindingContext) throws OpenLCompilationException {
        ILogicalTable uncutOriginalTable = null;
        if (isSmartLookupAndResultTitleInFirstRow) {
            uncutOriginalTable = originalTable;
            originalTable = cutResultTitleInFirstRow(originalTable);
        }

        int numberOfHConditions = isLookup(tableSyntaxNode) ? getNumberOfHConditions(originalTable) : 0;
        var firstColumnHeight = originalTable.getSource().getCell(0, 0).getHeight();
        var firstColumnForHCondition = -1;
        var withVerticalTitles = WithVerticalTitles.NO;

        if (numberOfHConditions > 0) {
            var p = getFirstColumnForHCondition(originalTable,
                    numberOfHConditions,
                    firstColumnHeight,
                    isSmartLookupTable(tableSyntaxNode));
            firstColumnForHCondition = p.getLeft();
            if (firstColumnForHCondition > 0) {
                withVerticalTitles = p.getRight();
            }
        }

        final FuzzyContext fuzzyContext = buildFuzzyContext(tableSyntaxNode,
                decisionTable,
                numberOfHConditions,
                bindingContext);

        final var numberOfColumnsUnderTitleCounter = new NumberOfColumnsUnderTitleCounter(
                originalTable,
                firstColumnHeight);

        var dtHeaders = getDTHeaders(tableSyntaxNode,
                decisionTable,
                originalTable,
                fuzzyContext,
                numberOfColumnsUnderTitleCounter,
                numberOfHConditions,
                firstColumnHeight,
                firstColumnForHCondition,
                withVerticalTitles,
                bindingContext);

        DeclaredDTHeader lookupReturnDtHeader = null;
        if (isSmartLookupAndResultTitleInFirstRow) {
            lookupReturnDtHeader = getLookupReturnDtHeader(tableSyntaxNode,
                    decisionTable,
                    uncutOriginalTable,
                    dtHeaders,
                    bindingContext);
            if (lookupReturnDtHeader == null) {
                var cellTable = uncutOriginalTable.getSource().getSubtable(0, 0, 1, 1);
                var sourceCodeModule = new GridCellSourceCodeModule(cellTable, bindingContext);
                SyntaxNodeException error = SyntaxNodeExceptionUtils
                        .createError("Expected external return is not found.", sourceCodeModule);
                bindingContext.addError(error);
            }
        }

        writeRule(decisionTable, originalTable, grid, dtHeaders, bindingContext);

        writeConditions(tableSyntaxNode,
                decisionTable,
                originalTable,
                grid,
                numberOfColumnsUnderTitleCounter,
                dtHeaders,
                firstColumnHeight,
                firstColumnForHCondition,
                withVerticalTitles,
                module,
                cache,
                bindingContext);

        writeUnmatchedColumns(decisionTable, originalTable, dtHeaders, firstColumnHeight, bindingContext);

        writeActions(decisionTable, originalTable, grid, dtHeaders, firstColumnHeight, module, cache, bindingContext);

        writeReturns(tableSyntaxNode,
                decisionTable,
                uncutOriginalTable,
                originalTable,
                grid,
                fuzzyContext,
                dtHeaders,
                lookupReturnDtHeader,
                module,
                cache,
                bindingContext);
    }

    private static DeclaredDTHeader getLookupReturnDtHeader(TableSyntaxNode tableSyntaxNode,
                                                            DecisionTable decisionTable,
                                                            ILogicalTable originalTable,
                                                            List<DTHeader> dtHeaders,
                                                            IBindingContext bindingContext) {
        var retColumn = getRetColumn(dtHeaders);
        DeclaredDTHeader lookupReturnDtHeader = null;
        final var definitions = ((XlsModuleOpenClass) decisionTable.getDeclaringClass()).getXlsDefinitions();
        final String title = OpenLFuzzyUtils.toTokenString(originalTable.getCell(0, 0).getStringValue());
        for (DTColumnsDefinition definition : definitions.getDtColumnsDefinitions()) {
            if (definition.isReturn() && definition.getTitles().size() == 1 && Objects
                    .equals(definition.getTitles().iterator().next(), title)) {
                MatchedDefinition matchedDefinition = matchByDTColumnDefinition(decisionTable,
                        definition,
                        1,
                        bindingContext);
                if (matchedDefinition != null) {
                    IParameterDeclaration[][] columnParameters = new IParameterDeclaration[1][];
                    columnParameters[0] = definition.getParameters(title).toArray(IParameterDeclaration.EMPTY);
                    if (lookupReturnDtHeader == null) {
                        lookupReturnDtHeader = new DeclaredDTHeader(matchedDefinition.getUsedMethodParameterIndexes(),
                                definition,
                                columnParameters,
                                retColumn,
                                0,
                                1,
                                1,
                                matchedDefinition,
                                true,
                                false);
                    } else {
                        bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(
                                "Ambiguous matching of column titles to DT return columns. Use more appropriate titles for return columns.",
                                tableSyntaxNode));
                        return lookupReturnDtHeader;
                    }
                }
            }
        }
        return lookupReturnDtHeader;
    }

    private static int getRetColumn(List<DTHeader> dtHeaders) {
        return dtHeaders.stream()
                .filter(e -> e.isCondition() || e.isAction())
                .mapToInt(e -> e.getColumn() + e.getWidth())
                .max()
                .orElse(0);
    }

    private static void resolveConflictsInDeclaredDtHeaders(DecisionTable decisionTable, List<List<DTHeader>> fits) {
        var usedMethodSignatureIdentifiers = new HashSet<String>();
        for (var i = 0; i < decisionTable.getSignature().getNumberOfParameters(); i++) {
            usedMethodSignatureIdentifiers.add(toLowerCase(decisionTable.getSignature().getParameterName(i)));
        }
        for (List<DTHeader> dtHeaders : fits) {
            var usedAllParameterIdentifiers = new HashMap<String, Integer>();
            var externalParameters = new HashSet<String>();
            for (DTHeader dtHeader : dtHeaders) {
                if (dtHeader instanceof DeclaredDTHeader declaredDTHeader) {
                    for (var i = 0; i < declaredDTHeader.getColumnParameters().length; i++) {
                        for (var j = 0; j < declaredDTHeader.getColumnParameters()[i].length; j++) {
                            var parameterDeclaration = declaredDTHeader.getColumnParameters()[i][j];
                            if (parameterDeclaration != null) {
                                usedAllParameterIdentifiers.merge(parameterDeclaration.getName(), 1, Integer::sum);
                            }
                        }
                    }
                    externalParameters.addAll(
                            declaredDTHeader.getMatchedDefinition().getDtColumnsDefinition().getExternalParameters());
                }
            }
            var renamedParameters = new HashMap<String, String>();
            for (DTHeader dtHeader : dtHeaders) {
                if (dtHeader instanceof DeclaredDTHeader declaredDTHeader) {
                    var usedLocalParameterIdentifiers = new HashSet<String>();
                    for (var i = 0; i < declaredDTHeader.getColumnParameters().length; i++) {
                        for (var j = 0; j < declaredDTHeader.getColumnParameters()[i].length; j++) {
                            var parameterDeclaration = declaredDTHeader.getColumnParameters()[i][j];
                            if (parameterDeclaration != null) {
                                usedLocalParameterIdentifiers.add(toLowerCase(parameterDeclaration.getName()));
                            }
                        }
                    }
                    for (var i = 0; i < declaredDTHeader.getColumnParameters().length; i++) {
                        for (var j = 0; j < declaredDTHeader.getColumnParameters()[i].length; j++) {
                            var parameterDeclaration = declaredDTHeader.getColumnParameters()[i][j];
                            if (parameterDeclaration != null) {
                                var param = parameterDeclaration.getName();
                                String lowerCasedParam = toLowerCase(param);
                                if (usedMethodSignatureIdentifiers.contains(
                                        lowerCasedParam) || usedAllParameterIdentifiers.get(param) > 1 && externalParameters
                                        .contains(param)) {
                                    var v = usedAllParameterIdentifiers.get(param);
                                    if (v != null) {
                                        if (v > 1) {
                                            usedAllParameterIdentifiers.put(param, v - 1);
                                        } else {
                                            usedAllParameterIdentifiers.remove(param);
                                        }
                                    }
                                    var newParamName = "_" + param;
                                    String newParamNameLowerCased = toLowerCase(newParamName);
                                    var k = 1;
                                    while (usedMethodSignatureIdentifiers
                                            .contains(newParamNameLowerCased) || usedAllParameterIdentifiers
                                            .containsKey(newParamName) || usedLocalParameterIdentifiers
                                            .contains(newParamNameLowerCased)) {
                                        newParamName = "_" + parameterDeclaration.getName() + "_" + k;
                                        newParamNameLowerCased = toLowerCase(newParamName);
                                        k++;
                                    }
                                    param = newParamName;
                                    usedAllParameterIdentifiers.put(newParamName, 1);
                                }
                                if (!StringUtils.equalsIgnoreCase(parameterDeclaration.getName(), param)) {
                                    declaredDTHeader.getMatchedDefinition()
                                            .renameParameterName(parameterDeclaration.getName(), param);
                                    renamedParameters.put(parameterDeclaration.getName(), param);
                                }
                            }
                        }
                    }
                }
            }
            for (DTHeader dtHeader : dtHeaders) {
                if (dtHeader instanceof DeclaredDTHeader declaredDTHeader) {
                    for (String externalParameter : declaredDTHeader.getMatchedDefinition()
                            .getDtColumnsDefinition()
                            .getExternalParameters()) {
                        var renamedParameter = renamedParameters.get(externalParameter);
                        if (renamedParameter != null) {
                            declaredDTHeader.getMatchedDefinition()
                                    .renameExternalParameter(externalParameter, renamedParameter);
                        }
                    }
                }
            }
        }
    }

    private static boolean isCompoundReturnType(IOpenClass compoundType) {
        if (IGNORED_CLASSES_FOR_COMPOUND_TYPE.contains(compoundType.getInstanceClass())) {
            return false;
        } else if (compoundType.getConstructor(IOpenClass.EMPTY) == null) {
            return false;
        } else if (ClassUtils.isAssignable(compoundType.getInstanceClass(), SpreadsheetResult.class)) {
            return false;
        } else {
            var count = 0;
            for (IOpenField field : compoundType.getFields()) {
                if (!field.isConst() && !field.isStatic() && field.isWritable()) {
                    count++;
                }
            }
            return count > 0;
        }
    }

    private static boolean isCompoundInputType(IOpenClass type) {
        if (IGNORED_CLASSES_FOR_COMPOUND_TYPE.contains(type.getInstanceClass())) {
            return false;
        }
        var count = 0;
        for (IOpenField field : type.getFields()) {
            if (!field.isConst() && !field.isStatic() && field.isReadable()) {
                count++;
            }
        }
        return count > 0;
    }

    private static void validateCompoundReturnType(IOpenClass compoundType) throws OpenLCompilationException {
        try {
            compoundType.getInstanceClass().getConstructor();
        } catch (ReflectiveOperationException e) {
            throw new OpenLCompilationException(
                    "Invalid return type: There is no default constructor found in type '%s'.".formatted(
                            compoundType.getDisplayName(0)));
        }
    }

    private static void writeReturnMetaInfo(TableSyntaxNode tableSyntaxNode,
                                            ICell cell,
                                            String description,
                                            String uri) {
        var metaReader = tableSyntaxNode.getMetaInfoReader();
        if (metaReader instanceof DecisionTableMetaInfoReader metaInfoReader) {
            metaInfoReader.addReturn(cell.getTopLeftCellFromRegion().getAbsoluteRow(),
                    cell.getTopLeftCellFromRegion().getAbsoluteColumn(),
                    description,
                    uri);
        }
    }

    private static IOpenClass getCompoundReturnType(TableSyntaxNode tableSyntaxNode,
                                                    DecisionTable decisionTable,
                                                    IBindingContext bindingContext) {
        IOpenClass compoundType;
        if (isCollect(tableSyntaxNode)) {
            if (tableSyntaxNode.getHeader().getCollectParameters().length > 0) {
                compoundType = bindingContext.findType(
                        tableSyntaxNode.getHeader()
                                .getCollectParameters()[tableSyntaxNode.getHeader().getCollectParameters().length - 1]);
            } else {
                if (decisionTable.getType().isArray()) {
                    compoundType = decisionTable.getType().getComponentClass();
                } else {
                    compoundType = decisionTable.getType();
                }
            }
        } else {
            compoundType = decisionTable.getType();
        }
        return compoundType;
    }

    private static Pair<String, IOpenClass> buildStatementByFieldsChain(IOpenClass type, IOpenField[] fieldsChain) {
        var fieldsChainSb = new StringBuilder();
        for (var i = 0; i < fieldsChain.length; i++) {
            var openField = type.getField(fieldsChain[i].getName(), true);
            fieldsChainSb.append(openField.getName());
            if (i < fieldsChain.length - 1) {
                fieldsChainSb.append(".");
            }
            type = fieldsChain[i].getType();
        }
        return Pair.of(fieldsChainSb.toString(), type);
    }

    private static String getTypeNameForCode(IOpenClass type,
                                             XlsModuleOpenClass module,
                                             IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache) {
        var g = type;
        var dim = 0;
        while (g.isArray()) {
            g = g.getComponentClass();
            dim++;
        }
        if (g instanceof BelongsToModuleOpenClass class1) {
            if (!module.isDependencyModule(class1.getModule(), cache)) {
                return class1.getExternalRefName() + "[]".repeat(Math.max(0, dim));
            }
        }
        if (NullOpenClass.the.equals(g)) {
            return JavaOpenClass.OBJECT.getName() + "[]".repeat(Math.max(0, dim));
        }
        return type.getName();
    }

    private static void writeReturnWithReturnDtHeader(TableSyntaxNode tableSyntaxNode,
                                                      ILogicalTable uncutOriginalTable,
                                                      ILogicalTable originalTable,
                                                      IWritableGrid grid,
                                                      DeclaredDTHeader declaredReturn,
                                                      String header,
                                                      boolean lookupReturnHeader,
                                                      XlsModuleOpenClass module,
                                                      IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache,
                                                      IBindingContext bindingContext) {
        grid.setCellValue(declaredReturn.getColumn(), 0, header);
        grid.setCellValue(declaredReturn.getColumn(), 1, declaredReturn.getStatement());
        var dtColumnsDefinition = declaredReturn.getMatchedDefinition().getDtColumnsDefinition();
        var c = declaredReturn.getColumn();
        while (c < declaredReturn.getColumn() + declaredReturn.getWidthForMerge()) {
            ICell cell = lookupReturnHeader ? uncutOriginalTable.getSource().getCell(0, 0)
                    : originalTable.getSource().getCell(c, 0);
            var d = cell.getStringValue();
            d = OpenLFuzzyUtils.toTokenString(d);
            for (String title : dtColumnsDefinition.getTitles()) {
                if (lookupReturnHeader || Objects.equals(d, title)) {
                    var parameters = dtColumnsDefinition.getParameters(title);
                    var parameterNames = new ArrayList<String>();
                    var typeOfColumns = new ArrayList<IOpenClass>();
                    var totalColumnsUnder = getTotalColumnsUnder(originalTable, c);
                    for (var paramIndex = 0; paramIndex < parameters.size(); paramIndex++) {
                        var param = parameters.get(paramIndex);
                        IOpenClass paramType;
                        if (param != null) {
                            var paramName = declaredReturn.getMatchedDefinition().getParameter(param.getName());
                            parameterNames.add(paramName);
                            var value = getTypeNameForCode(param.getType(),
                                    module,
                                    cache) + (paramName != null ? " " + paramName : "");
                            grid.setCellValue(c, 2, value);
                            paramType = param.getType();
                        } else {
                            paramType = declaredReturn.getDtColumnsDefinition().getCompositeMethod().getType();
                        }
                        typeOfColumns.add(paramType);
                        if (!lookupReturnHeader) {
                            var h = originalTable.getSource().getCell(c, 0).getHeight();
                            var w1 = originalTable.getSource().getCell(c, h).getWidth();
                            if (paramType != null && paramType.isArray()) {
                                // If we have more columns than parameters use excess columns for array typed parameter
                                var tmpC = c;
                                for (var i = 0; i < totalColumnsUnder - parameters.size(); i++) {
                                    var w2 = originalTable.getSource().getCell(tmpC, h).getWidth();
                                    w1 = w1 + w2;
                                    tmpC = tmpC + w2;
                                }
                            }
                            if (w1 > 1) {
                                grid.addMergedRegion(new GridRegion(2, c, 2, c + w1 - 1));
                            }
                            c = c + w1;
                        } else {
                            c = c + 1;
                        }
                    }
                    if (!bindingContext.isExecutionMode()) {
                        var sb = new StringBuilder();
                        sb.append("Return: ").append(header);
                        if (!StringUtils.isEmpty(declaredReturn.getStatement())) {
                            sb.append("\n")
                                    .append("Expression: ")
                                    .append(declaredReturn.getStatement().replaceAll("\n", StringUtils.SPACE));

                        }
                        DecisionTableMetaInfoReader.appendParameters(sb,
                                parameterNames.toArray(EMPTY_STRING_ARRAY),
                                typeOfColumns.toArray(IOpenClass.EMPTY));
                        writeReturnMetaInfo(tableSyntaxNode,
                                cell,
                                sb.toString(),
                                declaredReturn.getMatchedDefinition().getDtColumnsDefinition().getUri());
                    }
                    break;
                }
            }
        }

        if (c - declaredReturn.getColumn() > 1) {
            for (var row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1; row++) {
                grid.addMergedRegion(new GridRegion(row, declaredReturn.getColumn(), row, c - 1));
            }
        }
    }

    private static int getTotalColumnsUnder(ILogicalTable originalTable, int c) {
        var column = c;
        var totalColumnsUnder = 0;
        var maxColumn = c + originalTable.getSource().getCell(column, 0).getWidth();
        while (column < maxColumn) {
            var h = originalTable.getSource().getCell(column, 0).getHeight();
            column = column + originalTable.getSource().getCell(column, h).getWidth();
            totalColumnsUnder++;
        }
        return totalColumnsUnder;
    }

    private static final String FUZZY_RET_VARIABLE_NAME = "$Rn";

    private static IOpenClass writeReturnStatement(IOpenClass type,
                                                   IOpenField[] fieldsChain,
                                                   Set<String> generatedNames,
                                                   Map<String, Map<IOpenField, String>> variables,
                                                   String insertStatement,
                                                   Set<String> variableAssignments,
                                                   StringBuilder sb,
                                                   XlsModuleOpenClass module,
                                                   IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache) {
        if (fieldsChain == null) {
            return type;
        }
        var currentVariable = FUZZY_RET_VARIABLE_NAME;
        var variablesInChain = new HashSet<String>();
        variablesInChain.add(currentVariable);
        for (var j = 0; j < fieldsChain.length; j++) {
            String var;
            type = fieldsChain[j].getType();
            if (j < fieldsChain.length - 1) {
                Map<IOpenField, String> vm = variables.get(currentVariable);
                if (vm == null || vm.get(fieldsChain[j]) == null) {
                    var = RandomStringUtils.random(8, true, false);
                    while (generatedNames.contains(var)) { // Prevent
                        // variable
                        // duplication
                        var = RandomStringUtils.random(8, true, false);
                    }
                    generatedNames.add(var);
                    sb.append(getTypeNameForCode(type, module, cache))
                            .append(" ")
                            .append(var)
                            .append("=new ")
                            .append(getTypeNameForCode(type, module, cache))
                            .append("();");
                    sb.append("int ").append(var).append("_").append("=0;");
                    vm = variables.computeIfAbsent(currentVariable, e -> new HashMap<>());
                    vm.put(fieldsChain[j], var);
                    variableAssignments
                            .add(currentVariable + "." + fieldsChain[j].getName() + "=" + var + "_>0?" + var + ":null;");
                } else {
                    var = vm.get(fieldsChain[j]);
                }
                currentVariable = var;
                variablesInChain.add(currentVariable);
            } else {
                final var localVar = currentVariable + "." + fieldsChain[j].getName();
                sb.append(localVar).append("=").append(insertStatement).append(";");
                if (!variablesInChain.isEmpty()) {
                    sb.append("if(").append(localVar).append("!=null){");
                    for (String cv : variablesInChain) {
                        sb.append(cv).append("_++;");
                    }
                    sb.append('}');
                }
            }
        }
        return type;
    }

    private static void writeInputParametersToReturnMetaInfo(DecisionTable decisionTable,
                                                             String statementInInputParameters,
                                                             String statementInReturn) {
        var metaReader = decisionTable.getSyntaxNode().getMetaInfoReader();
        if (metaReader instanceof DecisionTableMetaInfoReader metaInfoReader) {
            metaInfoReader.addParameterToReturn(statementInInputParameters, statementInReturn);
        }
    }

    private static void writeInputParametersToReturn(TableSyntaxNode tableSyntaxNode,
                                                     DecisionTable decisionTable,
                                                     FuzzyContext fuzzyContext,
                                                     List<DTHeader> dtHeaders,
                                                     Set<String> generatedNames,
                                                     Map<String, Map<IOpenField, String>> variables,
                                                     Set<String> variableAssignments,
                                                     StringBuilder sb,
                                                     XlsModuleOpenClass module,
                                                     IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache,
                                                     IBindingContext bindingContext) {
        var fuzzyReturns = dtHeaders.stream()
                .filter(e -> e instanceof FuzzyDTHeader)
                .map(e -> (FuzzyDTHeader) e)
                .filter(FuzzyDTHeader::isReturn)
                .collect(toList());
        var m = new HashMap<IOpenField[], List<Token>>();
        for (Token token : fuzzyContext.getFuzzyReturnTokens()) {
            var returnTypeFieldsChains = fuzzyContext.getFieldsChainsForReturnToken(token);
            for (IOpenField[] returnTypeFieldsChain : returnTypeFieldsChains) {
                var f = false;
                for (Entry<IOpenField[], List<Token>> entry : m.entrySet()) {
                    if (OpenLFuzzyUtils.isEqualsFieldsChains(entry.getKey(), returnTypeFieldsChain)) {
                        entry.getValue().add(token);
                        f = true;
                        break;
                    }
                }
                if (!f) {
                    var tokens = new ArrayList<Token>();
                    tokens.add(token);
                    m.put(returnTypeFieldsChain, tokens);
                }
            }
        }

        var bestFuzzyResultsMap = new HashMap<Token, List<Pair<IOpenField[], FuzzyResult>>>();

        for (Entry<IOpenField[], List<Token>> entry : m.entrySet()) {
            final var fieldsChain = entry.getKey();
            final var foundInReturns = fuzzyReturns.stream()
                    .anyMatch(e -> OpenLFuzzyUtils.isEqualsFieldsChains(e.getFieldsChain(), fieldsChain));
            if (foundInReturns) {
                continue;
            }
            for (Token token : entry.getValue()) {
                var fuzzyResults = OpenLFuzzyUtils
                        .fuzzyExtract(token.getValue(), fuzzyContext.getParameterTokens().getTokens(), false);
                for (FuzzyResult fuzzyResult : fuzzyResults) {
                    final var paramIndex = fuzzyContext.getParameterTokens().getParameterIndex(fuzzyResult.getToken());
                    final var paramFieldsChain = fuzzyContext.getParameterTokens()
                            .getFieldsChain(fuzzyResult.getToken());
                    List<Pair<IOpenField[], FuzzyResult>> resultList = bestFuzzyResultsMap.get(fuzzyResult.getToken());
                    if (resultList == null) {
                        resultList = bestFuzzyResultsMap.entrySet().stream().filter(e -> {
                            final var eParamIndex = fuzzyContext.getParameterTokens().getParameterIndex(e.getKey());
                            return paramIndex == eParamIndex && OpenLFuzzyUtils.isEqualsFieldsChains(paramFieldsChain,
                                    fuzzyContext.getParameterTokens().getFieldsChain(e.getKey()));
                        }).map(Entry::getValue).findFirst().orElse(null);
                        if (resultList == null) {
                            resultList = new ArrayList<>();
                            bestFuzzyResultsMap.put(fuzzyResult.getToken(), resultList);
                        }
                    }
                    if (resultList.isEmpty()) {
                        resultList.add(Pair.of(fieldsChain, fuzzyResult));
                    } else {
                        Pair<IOpenField[], FuzzyResult> existedResult = resultList.getFirst();
                        var fuzzyResultCompare = fuzzyResult.compareTo(existedResult.getRight());
                        if (fuzzyResultCompare <= 0) {
                            if (fuzzyResultCompare < 0) {
                                resultList.clear();
                            }
                            var f = true;
                            for (Pair<IOpenField[], FuzzyResult> pair : resultList) {
                                if (OpenLFuzzyUtils.isEqualsFieldsChains(pair.getKey(), fieldsChain)) {
                                    f = false;
                                    break;
                                }
                            }
                            if (f) {
                                resultList.add(Pair.of(fieldsChain, fuzzyResult));
                            }
                        }
                    }
                }
            }
        }

        var ambiguousReturnStatementMatching = new HashMap<String, Set<String>>();
        for (Entry<Token, List<Pair<IOpenField[], FuzzyResult>>> entry : bestFuzzyResultsMap.entrySet()) {
            var paramToken = entry.getKey();
            for (Pair<IOpenField[], FuzzyResult> pair : entry.getValue()) {
                final var paramIndex = fuzzyContext.getParameterTokens().getParameterIndex(paramToken);
                var type = decisionTable.getSignature().getParameterType(paramIndex);
                final var paramFieldsChain = fuzzyContext.getParameterTokens().getFieldsChain(paramToken);
                final String statement;
                if (paramFieldsChain != null) {
                    var v = buildStatementByFieldsChain(type, paramFieldsChain);
                    statement = decisionTable.getSignature().getParameterName(paramIndex) + "." + v.getKey();
                    type = v.getValue();
                } else {
                    statement = decisionTable.getSignature().getParameterName(paramIndex);
                }
                if (!isCompoundInputType(type)) {
                    var fieldsChain = pair.getKey();
                    var p = buildStatementByFieldsChain(fuzzyContext.getFuzzyReturnType(),
                            fieldsChain);
                    var cast = bindingContext.getCast(type, p.getValue());
                    if (cast != null && cast.isImplicit()) {
                        writeReturnStatement(fuzzyContext.getFuzzyReturnType(),
                                fieldsChain,
                                generatedNames,
                                variables,
                                statement,
                                variableAssignments,
                                sb,
                                module,
                                cache);
                        final var statementInReturn = getTypeNameForCode(fuzzyContext.getFuzzyReturnType(),
                                module,
                                cache) + "." + buildStatementByFieldsChain(fuzzyContext.getFuzzyReturnType(), fieldsChain)
                                .getKey();
                        var matchedStatements = ambiguousReturnStatementMatching
                                .computeIfAbsent(statementInReturn, k -> new HashSet<>());
                        matchedStatements.add(statement);
                        if (!bindingContext.isExecutionMode()) {
                            writeInputParametersToReturnMetaInfo(decisionTable, statement, statementInReturn);
                        }
                    }
                }
            }
        }

        ambiguousReturnStatementMatching.entrySet()
                .stream()
                .filter(e -> e.getValue().size() > 1)
                .forEach(e -> bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(
                "More than one input parameter is set to return '%s'.".formatted(e.getKey()),
                        tableSyntaxNode)));
    }

    private static void writeFuzzyReturns(TableSyntaxNode tableSyntaxNode,
                                          DecisionTable decisionTable,
                                          ILogicalTable originalTable,
                                          IWritableGrid grid,
                                          FuzzyContext fuzzyContext,
                                          List<DTHeader> dtHeaders,
                                          IOpenClass compoundReturnType,
                                          String header,
                                          XlsModuleOpenClass module,
                                          IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache,
                                          IBindingContext bindingContext) throws OpenLCompilationException {
        validateCompoundReturnType(compoundReturnType);

        var fuzzyReturns = dtHeaders.stream()
                .filter(e -> e instanceof FuzzyDTHeader && e.isReturn())
                .map(e -> (FuzzyDTHeader) e)
                .filter(e -> e.getFieldsChain() != null)
                .collect(toList());

        var variableAssignments = new HashSet<String>();

        if (fuzzyReturns.isEmpty()) {
            throw new IllegalStateException("DT headers are not found.");
        }

        var sb = new StringBuilder();
        sb.append(getTypeNameForCode(compoundReturnType, module, cache))
                .append(" ")
                .append(FUZZY_RET_VARIABLE_NAME)
                .append(" = new ")
                .append(getTypeNameForCode(compoundReturnType, module, cache))
                .append("();");
        sb.append("int ").append(FUZZY_RET_VARIABLE_NAME).append("_").append(" = 0;");

        var generatedNames = new HashSet<String>();
        while (generatedNames.size() < fuzzyReturns.size()) {
            generatedNames.add(RandomStringUtils.random(8, true, false));
        }
        var compoundColumnParamNames = generatedNames.toArray(EMPTY_STRING_ARRAY);
        var variables = new HashMap<String, Map<IOpenField, String>>();

        writeInputParametersToReturn(tableSyntaxNode,
                decisionTable,
                fuzzyContext,
                dtHeaders,
                generatedNames,
                variables,
                variableAssignments,
                sb,
                module,
                cache,
                bindingContext);

        var i = 0;
        for (FuzzyDTHeader fuzzyDTHeader : fuzzyReturns) {
            IOpenClass type = writeReturnStatement(compoundReturnType,
                    fuzzyDTHeader.getFieldsChain(),
                    generatedNames,
                    variables,
                    compoundColumnParamNames[i],
                    variableAssignments,
                    sb,
                    module,
                    cache);

            grid.setCellValue(fuzzyDTHeader.getColumn(),
                    2,
                    getTypeNameForCode(type, module, cache) + " " + compoundColumnParamNames[i]);

            if (fuzzyDTHeader.getWidth() > 1) {
                grid.addMergedRegion(new GridRegion(2,
                        fuzzyDTHeader.getColumn(),
                        2,
                        fuzzyDTHeader.getColumn() + fuzzyDTHeader.getWidth() - 1));
            }

            if (!bindingContext.isExecutionMode()) {
                var firstColumnHeight = originalTable.getCell(0, 0).getHeight();
                var cell = originalTable.getSource().getCell(fuzzyDTHeader.getColumn(), firstColumnHeight - 1);
                cell = cell.getTopLeftCellFromRegion();
                var statement = buildStatementByFieldsChain(compoundReturnType, fuzzyDTHeader.getFieldsChain())
                        .getKey();
                var sb1 = new StringBuilder();
                sb1.append("Return: ").append(header);

                if (!StringUtils.isEmpty(statement)) {
                    sb1.append("\n")
                            .append("Expression: value for return ")
                            .append(compoundReturnType.getDisplayName(INamedThing.SHORT))
                            .append(".")
                            .append(statement);
                }
                DecisionTableMetaInfoReader.appendParameters(sb1, null, new IOpenClass[]{type});

                writeReturnMetaInfo(tableSyntaxNode, cell, sb1.toString(), null);
            }
            i++;
        }
        variableAssignments.forEach(sb::append);
        sb.append(FUZZY_RET_VARIABLE_NAME).append("_ > 0 ? ").append(FUZZY_RET_VARIABLE_NAME).append(" : null;");
        final var expression = sb.toString();
        if (expression.length() > SpreadsheetVersion.EXCEL2007.getMaxTextLength()) {
            throw new IllegalStateException("Generated expression is too long!");
        }
        grid.setCellValue(fuzzyReturns.getFirst().getColumn(), 0, header);
        grid.setCellValue(fuzzyReturns.getFirst().getColumn(), 1, expression);
        var j = fuzzyReturns.size() - 1;
        if (fuzzyReturns.get(j).getColumn() + fuzzyReturns.get(j).getWidth() - fuzzyReturns.getFirst().getColumn() > 1) {
            for (var row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1; row++) {
                grid.addMergedRegion(new GridRegion(row,
                        fuzzyReturns.getFirst().getColumn(),
                        row,
                        fuzzyReturns.get(j).getColumn() + fuzzyReturns.get(j).getWidth() - 1));
            }
        }
    }

    private static void writeSimpleDTReturnHeader(TableSyntaxNode tableSyntaxNode,
                                                  DecisionTable decisionTable,
                                                  ILogicalTable originalTable,
                                                  IWritableGrid grid,
                                                  SimpleReturnDTHeader simpleReturnDTHeader,
                                                  String header,
                                                  int collectParameterIndex,
                                                  IBindingContext bindingContext) {
        grid.setCellValue(simpleReturnDTHeader.getColumn(), 0, header);

        if (tableSyntaxNode.getHeader().getCollectParameters().length > 0) {
            grid.setCellValue(simpleReturnDTHeader.getColumn(),
                    2,
                    tableSyntaxNode.getHeader().getCollectParameters()[collectParameterIndex]);
        }

        if (!bindingContext.isExecutionMode()) {
            var sb = new StringBuilder();
            sb.append("Return: ").append(header);
            var cell = originalTable.getSource().getCell(simpleReturnDTHeader.getColumn(), 0);
            if (!StringUtils.isEmpty(simpleReturnDTHeader.getStatement())) {
                sb.append("\n").append("Expression: ").append(simpleReturnDTHeader.getStatement());
            }
            DecisionTableMetaInfoReader
                    .appendParameters(sb, null, new IOpenClass[]{decisionTable.getHeader().getType()});
            writeReturnMetaInfo(tableSyntaxNode, cell, sb.toString(), null);
        }

        if (simpleReturnDTHeader.getWidth() > 1) {
            for (var row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT; row++) {
                grid.addMergedRegion(new GridRegion(row,
                        simpleReturnDTHeader.getColumn(),
                        row,
                        simpleReturnDTHeader.getColumn() + simpleReturnDTHeader.getWidth() - 1));
            }
        }
    }

    private static void writeReturns(TableSyntaxNode tableSyntaxNode,
                                     DecisionTable decisionTable,
                                     ILogicalTable uncutOriginalTable,
                                     ILogicalTable originalTable,
                                     IWritableGrid grid,
                                     FuzzyContext fuzzyContext,
                                     List<DTHeader> dtHeaders,
                                     DeclaredDTHeader lookupReturnDtHeader,
                                     XlsModuleOpenClass module,
                                     IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache,
                                     IBindingContext bindingContext) throws OpenLCompilationException {
        final var isCollect = isCollect(tableSyntaxNode);

        if (isLookup(tableSyntaxNode)) {
            if (lookupReturnDtHeader != null) {
                writeReturnWithReturnDtHeader(tableSyntaxNode,
                        uncutOriginalTable,
                        originalTable,
                        grid,
                        lookupReturnDtHeader,
                        isCollect ? CRET1_COLUMN_NAME : RET1_COLUMN_NAME,
                        true,
                        module,
                        cache,
                        bindingContext);
            } else {
                var retColumn = getRetColumn(dtHeaders);
                grid.setCellValue(retColumn, 0, isCollect ? CRET1_COLUMN_NAME : RET1_COLUMN_NAME);
            }
            return;
        }

        if (dtHeaders.stream()
                .filter(DTHeader::isReturn)
                .anyMatch(e -> e.getColumn() + e.getWidth() - 1 >= originalTable.getSource().getWidth())) {
            throw new OpenLCompilationException("Wrong table structure: There is no column for return values.");
        }

        var retNum = 1;
        var cRetNum = 1;
        var i = 0;
        var collectParameterIndex = 0;
        var keyNum = 1;
        var skipFuzzyReturns = false;
        for (DTHeader dtHeader : dtHeaders) {
            if (dtHeader.isReturn()) {
                if (dtHeader instanceof DeclaredDTHeader header2) {
                    writeReturnWithReturnDtHeader(tableSyntaxNode,
                            uncutOriginalTable,
                            originalTable,
                            grid,
                            header2,
                            isCollect ? DecisionTableColumnHeaders.COLLECT_RETURN.getHeaderKey() + cRetNum++
                                    : DecisionTableColumnHeaders.RETURN.getHeaderKey() + retNum++,
                            false,
                            module,
                            cache,
                            bindingContext);
                } else if (dtHeader instanceof SimpleReturnDTHeader || dtHeader instanceof FuzzyDTHeader header1 && header1
                        .getFieldsChain() == null) {
                    var isKey = false;
                    String header;
                    if (isCollect && tableSyntaxNode.getHeader()
                            .getCollectParameters().length > 1 && i == 0 && ClassUtils
                            .isAssignable(decisionTable.getType().getInstanceClass(), Map.class)) {
                        header = DecisionTableColumnHeaders.KEY.getHeaderKey() + keyNum++;
                        isKey = true;
                    } else {
                        header = isCollect ? DecisionTableColumnHeaders.COLLECT_RETURN.getHeaderKey() + cRetNum++
                                : DecisionTableColumnHeaders.RETURN.getHeaderKey() + retNum++;
                    }
                    SimpleReturnDTHeader simpleDTReturnHeader;
                    if (dtHeader instanceof FuzzyDTHeader fuzzyDTHeader) {
                        simpleDTReturnHeader = new SimpleReturnDTHeader(fuzzyDTHeader.getStatement(),
                                fuzzyDTHeader.getTitle(),
                                fuzzyDTHeader.getColumn(),
                                0,
                                fuzzyDTHeader.getWidth());
                    } else {
                        simpleDTReturnHeader = (SimpleReturnDTHeader) dtHeader;
                    }
                    writeSimpleDTReturnHeader(tableSyntaxNode,
                            decisionTable,
                            originalTable,
                            grid,
                            simpleDTReturnHeader,
                            header,
                            collectParameterIndex,
                            bindingContext);
                    i++;
                    if (isKey) {
                        collectParameterIndex++;
                    }
                } else if (dtHeader instanceof FuzzyDTHeader && !skipFuzzyReturns) {
                    IOpenClass compoundReturnType = getCompoundReturnType(tableSyntaxNode,
                            decisionTable,
                            bindingContext);

                    writeFuzzyReturns(tableSyntaxNode,
                            decisionTable,
                            originalTable,
                            grid,
                            fuzzyContext,
                            dtHeaders,
                            compoundReturnType,
                            isCollect ? DecisionTableColumnHeaders.COLLECT_RETURN.getHeaderKey() + cRetNum++
                                    : DecisionTableColumnHeaders.RETURN.getHeaderKey() + retNum++,
                            module,
                            cache,
                            bindingContext);
                    skipFuzzyReturns = true;
                }
            }
        }
    }

    private static void writeDeclaredDtHeader(DecisionTable decisionTable,
                                              ILogicalTable originalTable,
                                              IWritableGrid grid,
                                              DeclaredDTHeader declaredDtHeader,
                                              String header,
                                              int firstColumnHeight,
                                              XlsModuleOpenClass module,
                                              IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache,
                                              IBindingContext bindingContext) {
        var column = declaredDtHeader.getColumn();
        grid.setCellValue(column, 0, header);
        grid.setCellValue(column, 1, declaredDtHeader.getStatement());

        var firstColumn = column;
        var lastParamFirstColumn = firstColumn;

        var parameterNames = new ArrayList<String>();
        var typeOfColumns = new ArrayList<IOpenClass>();
        for (var j = 0; j < declaredDtHeader.getColumnParameters().length; j++) {
            for (var k = 0; k < declaredDtHeader.getColumnParameters()[j].length; k++) {
                var param = declaredDtHeader.getColumnParameters()[j][k];
                if (param != null) {
                    var paramName = declaredDtHeader.getMatchedDefinition().getParameter(param.getName());
                    parameterNames.add(paramName);
                    grid.setCellValue(column,
                            2,
                            getTypeNameForCode(param.getType(),
                                    module,
                                    cache) + (paramName != null ? " " + paramName : ""));
                    typeOfColumns.add(param.getType());
                } else {
                    parameterNames.add(null);
                    typeOfColumns.add(declaredDtHeader.getDtColumnsDefinition().getCompositeMethod().getType());
                }
                int w1;
                if (declaredDtHeader.isHCondition()) {
                    w1 = 1;
                } else {
                    w1 = originalTable.getSource().getCell(column, firstColumnHeight).getWidth();
                }
                if (w1 > 1) {
                    grid.addMergedRegion(new GridRegion(2, column, 2, column + w1 - 1));
                }
                lastParamFirstColumn = column;
                column = column + w1;
            }
        }

        if (!bindingContext.isExecutionMode()) {
            var column1 = declaredDtHeader.getColumn();
            while (column1 < declaredDtHeader.getColumn() + declaredDtHeader.getWidth()) {
                if (declaredDtHeader.isAction()) {
                    writeMetaInfoForAction(decisionTable,
                            originalTable,
                            column1,
                            declaredDtHeader.getRow(),
                            header,
                            parameterNames.toArray(EMPTY_STRING_ARRAY),
                            declaredDtHeader.getStatement(),
                            typeOfColumns.toArray(IOpenClass.EMPTY),
                            declaredDtHeader.getMatchedDefinition().getDtColumnsDefinition().getUri());
                } else if (declaredDtHeader.isCondition() && !declaredDtHeader.isHCondition()) {
                    writeMetaInfoForVCondition(originalTable,
                            decisionTable,
                            column1,
                            declaredDtHeader.getRow(),
                            header,
                            parameterNames.toArray(EMPTY_STRING_ARRAY),
                            declaredDtHeader.getStatement(),
                            typeOfColumns.toArray(IOpenClass.EMPTY),
                            declaredDtHeader.getMatchedDefinition().getDtColumnsDefinition().getUri());
                }
                column1 = column1 + originalTable.getSource().getCell(column1, declaredDtHeader.getRow()).getWidth();
            }
        }

        if (column < firstColumn + declaredDtHeader.getWidthForMerge()) {
            grid.addMergedRegion(new GridRegion(IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1,
                    lastParamFirstColumn,
                    IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1,
                    firstColumn + declaredDtHeader.getWidthForMerge() - 1));
            column = firstColumn + declaredDtHeader.getWidthForMerge();
        }
        // merge columns
        if (column - firstColumn > 1) {
            for (var row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1; row++) {
                grid.addMergedRegion(new GridRegion(row, firstColumn, row, column - 1));
            }
        }
    }

    private static void writeRule(DecisionTable decisionTable,
                                  ILogicalTable originalTable,
                                  IWritableGrid grid,
                                  List<DTHeader> dtHeaders,
                                  IBindingContext bindingContext) throws OpenLCompilationException {
        var rules = dtHeaders.stream()
                .filter(DTHeader::isRule)
                .collect(collectingAndThen(toList(), Collections::unmodifiableList));
        if (!rules.isEmpty()) {
            if (rules.size() > 1) {
                var message = "Wrong table structure: Wrong number of rule numbers columns.";
                throw new OpenLCompilationException(message);
            }
            var rule = rules.getFirst();
            if (rule.getColumn() != 0) {
                var message = "Wrong table structure: Wrong rule numbers column index.";
                throw new OpenLCompilationException(message);
            }
            if (rule instanceof FuzzyRulesDTHeader fuzzyRulesDTHeader) {
                grid.setCellValue(fuzzyRulesDTHeader.getColumn(), 0, DecisionTableColumnHeaders.RULE);
                if (!bindingContext.isExecutionMode()) {
                    writeMetaInfoForRule(decisionTable, originalTable, fuzzyRulesDTHeader.getColumn(), 0);
                }
            }
        }
    }

    private static void writeActions(DecisionTable decisionTable,
                                     ILogicalTable originalTable,
                                     IWritableGrid grid,
                                     List<DTHeader> dtHeaders,
                                     int firstColumnHeight,
                                     XlsModuleOpenClass module,
                                     IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache,
                                     IBindingContext bindingContext) throws OpenLCompilationException {
        var actions = dtHeaders.stream()
                .filter(DTHeader::isAction)
                .collect(collectingAndThen(toList(), Collections::unmodifiableList));
        var num = 0;
        for (DTHeader action : actions) {
            if (action.getColumn() >= originalTable.getSource().getWidth()) {
                var message = "Wrong table structure: Wrong number of action columns.";
                throw new OpenLCompilationException(message);
            }

            var declaredAction = (DeclaredDTHeader) action;
            var header = (DecisionTableColumnHeaders.ACTION.getHeaderKey() + (num + 1));
            writeDeclaredDtHeader(decisionTable,
                    originalTable,
                    grid,
                    declaredAction,
                    header,
                    firstColumnHeight,
                    module,
                    cache,
                    bindingContext);
            num++;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean getMinMaxOrder(ILogicalTable originalTable,
                                          NumberOfColumnsUnderTitleCounter numberOfColumnsUnderTitleCounter,
                                          int firstColumnHeight,
                                          int column,
                                          IOpenClass type) {
        var h = firstColumnHeight;
        var height = originalTable.getSource().getHeight();
        var t1 = 0;
        var t2 = 0;
        var string2DataConverter = String2DataConvertorFactory
                .getConvertor(type.getInstanceClass());
        while (h < height) {
            var cell1 = originalTable.getSource().getCell(column, h);
            try {
                var s1 = cell1.getStringValue();
                Object o1;
                try {
                    o1 = string2DataConverter.parse(s1, null);
                } catch (IllegalArgumentException e) {
                    continue;
                }

                var cell2 = originalTable.getSource()
                        .getCell(column + numberOfColumnsUnderTitleCounter.getWidth(column, 0), h);
                var s2 = cell2.getStringValue();
                Object o2;
                try {
                    o2 = string2DataConverter.parse(s2, null);
                } catch (IllegalArgumentException e) {
                    continue;
                }

                if (JavaOpenClass.STRING.equals(type) && o1 != null && o2 != null) {
                    var res = NumericStringComparator.INSTANCE.compare((String) o1, (String) o2);
                    if (res > 0) {
                        t1++;
                    } else if (res < 0) {
                        t2++;
                    }
                } else if (o1 instanceof Comparable comparable && o2 instanceof Comparable) {
                    var res = comparable.compareTo(o2);
                    if (res > 0) {
                        t1++;
                    } else if (res < 0) {
                        t2++;
                    }
                }
            } finally {
                h = h + cell1.getHeight();
            }
        }
        return t1 <= t2;
    }

    private static final String[] MIN_MAX_ORDER = new String[]{"min", "max"};
    private static final String[] MAX_MIN_ORDER = new String[]{"max", "min"};

    private static void writeUnmatchedColumns(DecisionTable decisionTable,
                                              ILogicalTable originalTable,
                                              List<DTHeader> dtHeaders,
                                              int firstColumnHeight,
                                              IBindingContext bindingContext) throws OpenLCompilationException {
        var unmatched = dtHeaders.stream()
                .filter(e -> e instanceof UnmatchedDtHeader)
                .collect(collectingAndThen(toList(), Collections::unmodifiableList));
        for (DTHeader dtHeader : unmatched) {
            var column = dtHeader.getColumn();
            if (column > originalTable.getSource().getWidth()) {
                var message = "Wrong table structure: Columns count is less than parameters count";
                throw new OpenLCompilationException(message);
            }
            if (column == originalTable.getSource().getWidth()) {
                var message = "Wrong table structure: There is no column for return values";
                throw new OpenLCompilationException(message);
            }
            if (!bindingContext.isExecutionMode()) {
                writeMetaInfoForUnmatched(originalTable, decisionTable, column, firstColumnHeight - 1);
            }
            var eGridCellSourceCodeModule = new GridCellSourceCodeModule(originalTable.getSource(),
                    dtHeader.getColumn(),
                    firstColumnHeight - 1,
                    bindingContext);
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(
                    "Smart table has unmatched title '%s'.".formatted(eGridCellSourceCodeModule.getCode()),
                    eGridCellSourceCodeModule);
            bindingContext.addError(error);
        }
    }

    private static void writeConditions(TableSyntaxNode tableSyntaxNode,
                                        DecisionTable decisionTable,
                                        ILogicalTable originalTable,
                                        IWritableGrid grid,
                                        NumberOfColumnsUnderTitleCounter numberOfColumnsUnderTitleCounter,
                                        List<DTHeader> dtHeaders,
                                        int firstColumnHeight,
                                        int firstColumnForHCondition,
                                        WithVerticalTitles withVerticalTitles,
                                        XlsModuleOpenClass module,
                                        IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache,
                                        IBindingContext bindingContext) throws OpenLCompilationException {

        var conditions = dtHeaders.stream()
                .filter(e -> !(e instanceof UnmatchedDtHeader))
                .filter(DTHeader::isCondition)
                .collect(collectingAndThen(toList(), Collections::unmodifiableList));

        var numOfVCondition = 0;
        var numOfHCondition = 0;

        var firstColumnForHConditionsOrReturns = dtHeaders.stream()
                .filter(e -> e.isCondition() && !e.isHCondition() || e.isAction())
                .mapToInt(e -> e.getColumn() + e.getWidth())
                .max()
                .orElse(0);
        var isCollect = isCollect(tableSyntaxNode);
        var hConditionTypes = new HashMap<DTHeader, IOpenClass>();
        for (DTHeader condition : conditions) {
            var column = condition.getColumn();
            if (!isLookup(tableSyntaxNode)) {
                if (column > originalTable.getSource().getWidth()) {
                    var message = "Wrong table structure: Columns count is less than parameters count";
                    throw new OpenLCompilationException(message);
                }
                if (column > originalTable.getSource().getWidth()) {
                    var message = "Wrong table structure: There is no column for return values";
                    throw new OpenLCompilationException(message);
                }
            }
            // write headers
            //

            String header;
            if (!condition.isHCondition()) {
                // write vertical condition
                //
                numOfVCondition++;
                if (numOfVCondition == 1 && (conditions.stream()
                        .filter(e -> !e.isHCondition())
                        .count() < 2) && !(isCollect && decisionTable.getType()
                        .isArray() && !decisionTable.getType()
                        .getComponentClass()
                        .isArray()) && !(isCollect && ClassUtils
                        .isAssignable(decisionTable.getType().getInstanceClass(), Collection.class))) {
                    header = (DecisionTableColumnHeaders.MERGED_CONDITION.getHeaderKey() + numOfVCondition);
                } else {
                    header = (DecisionTableColumnHeaders.CONDITION.getHeaderKey() + numOfVCondition);
                }
            } else {
                // write horizontal condition
                //
                numOfHCondition++;
                header = (DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey() + numOfHCondition);
            }

            if (condition instanceof DeclaredDTHeader tHeader) {
                writeDeclaredDtHeader(decisionTable,
                        originalTable,
                        grid,
                        tHeader,
                        header,
                        firstColumnHeight,
                        module,
                        cache,
                        bindingContext);
            } else {
                grid.setCellValue(column, 0, header);
                final var numberOfColumnsUnderTitle = numberOfColumnsUnderTitleCounter.get(column);
                IOpenClass type = getTypeForCondition(decisionTable, condition);
                if (condition instanceof FuzzyDTHeader && numberOfColumnsUnderTitle == 2 && condition
                        .getWidthForMerge() == numberOfColumnsUnderTitleCounter.getWidth(column,
                        0) + numberOfColumnsUnderTitleCounter.getWidth(column, 1) && type
                        .getInstanceClass() != null && (type.getInstanceClass()
                        .isPrimitive() || ClassUtils.isAssignable(type.getInstanceClass(), Comparable.class))) {
                    var minMaxOrder = getMinMaxOrder(originalTable,
                            numberOfColumnsUnderTitleCounter,
                            firstColumnHeight,
                            column,
                            type);
                    String statement;
                    var stringOperator = StringUtils.EMPTY;
                    if (JavaOpenClass.STRING.equals(type)) {
                        stringOperator = "string";
                    }
                    if (minMaxOrder) {
                        statement = "min " + stringOperator + "<= " + condition.getStatement() + " && " + condition
                                .getStatement() + " " + stringOperator + "< max";
                    } else {
                        statement = "max " + stringOperator + "> " + condition.getStatement() + " && " + condition
                                .getStatement() + " " + stringOperator + ">= min";
                    }
                    grid.setCellValue(column, 1, statement);
                    grid.setCellValue(column,
                            2,
                            getTypeNameForCode(type, module, cache) + " " + (minMaxOrder ? "min" : "max"));
                    var w1 = numberOfColumnsUnderTitleCounter.getWidth(column, 0);
                    if (w1 > 1) {
                        grid.addMergedRegion(new GridRegion(2, column, 2, column + w1 - 1));
                    }
                    grid.setCellValue(column + w1,
                            2,
                            getTypeNameForCode(type, module, cache) + " " + (minMaxOrder ? "max" : "min"));
                    var w2 = numberOfColumnsUnderTitleCounter.getWidth(column, 1);
                    if (w2 > 1) {
                        grid.addMergedRegion(new GridRegion(2, column + w1, 2, column + w1 + w2 - 1));
                    }
                    if (!condition.isHCondition()) {
                        if (!bindingContext.isExecutionMode()) {
                            writeMetaInfoForVCondition(originalTable,
                                    decisionTable,
                                    condition.getColumn(),
                                    condition.getRow(),
                                    header,
                                    minMaxOrder ? MIN_MAX_ORDER : MAX_MIN_ORDER,
                                    statement,
                                    new IOpenClass[]{type, type},
                                    null);
                        }
                        if (condition.getWidthForMerge() > 1) {
                            for (var row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1; row++) {
                                grid.addMergedRegion(
                                        new GridRegion(row, column, row, column + condition.getWidthForMerge() - 1));
                            }
                        }
                    }
                } else {
                    // Set type of condition values(for Ranges and Array)
                    var typeOfValue = getTypeForConditionColumn(decisionTable,
                            originalTable,
                            condition,
                            numOfHCondition,
                            firstColumnForHConditionsOrReturns,
                            firstColumnHeight,
                            numberOfColumnsUnderTitle,
                            module,
                            cache,
                            bindingContext);
                    grid.setCellValue(column, 1, typeOfValue.getRight());
                    grid.setCellValue(column,
                            2,
                            typeOfValue.getLeft().length == 1 ? typeOfValue.getLeft()[0]
                                    : typeOfValue.getLeft()[0] + " " + typeOfValue.getLeft()[1]);
                    if (condition.isHCondition()) {
                        hConditionTypes.put(condition, typeOfValue.getMiddle());
                    } else {
                        if (!bindingContext.isExecutionMode()) {
                            writeMetaInfoForVCondition(originalTable,
                                    decisionTable,
                                    condition.getColumn(),
                                    condition.getRow(),
                                    header,
                                    typeOfValue.getLeft().length == 1 ? null : new String[]{typeOfValue.getLeft()[1]},
                                    typeOfValue.getRight(),
                                    new IOpenClass[]{typeOfValue.getMiddle()},
                                    null);
                        }
                        if (condition.getWidth() > 1) {
                            for (var row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT; row++) {
                                grid.addMergedRegion(
                                        new GridRegion(row, column, row, column + condition.getWidth() - 1));
                            }
                        }
                    }
                }
            }
        }

        if (!bindingContext.isExecutionMode()) {
            writeMetaInfoForHConditions(originalTable,
                    decisionTable,
                    conditions,
                    firstColumnForHCondition,
                    withVerticalTitles,
                    hConditionTypes);
        }
    }

    private static void writeMetaInfoForVCondition(ILogicalTable originalTable,
                                                   DecisionTable decisionTable,
                                                   int column,
                                                   int row,
                                                   String header,
                                                   String[] parameterNames,
                                                   String conditionStatement,
                                                   IOpenClass[] typeOfColumns,
                                                   String url) {
        Objects.requireNonNull(header);
        var metaReader = decisionTable.getSyntaxNode().getMetaInfoReader();
        if (metaReader instanceof DecisionTableMetaInfoReader metaInfoReader) {
            var cell = originalTable.getSource().getCell(column, row);
            cell = cell.getTopLeftCellFromRegion();
            metaInfoReader.addCondition(cell.getAbsoluteRow(),
                    cell.getAbsoluteColumn(),
                    header,
                    parameterNames,
                    conditionStatement,
                    typeOfColumns,
                    url,
                    null,
                    false);
        }
    }

    private static void writeMetaInfoForUnmatched(ILogicalTable originalTable,
                                                  DecisionTable decisionTable,
                                                  int column,
                                                  int row) {
        var metaReader = decisionTable.getSyntaxNode().getMetaInfoReader();
        if (metaReader instanceof DecisionTableMetaInfoReader metaInfoReader) {
            var cell = originalTable.getSource().getCell(column, row);
            cell = cell.getTopLeftCellFromRegion();
            metaInfoReader.addUnmatched(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
        }
    }

    private static void writeMetaInfoForRule(DecisionTable decisionTable,
                                             ILogicalTable originalTable,
                                             int column,
                                             int row) {
        var metaReader = decisionTable.getSyntaxNode().getMetaInfoReader();
        if (metaReader instanceof DecisionTableMetaInfoReader metaInfoReader) {
            var cell = originalTable.getSource().getCell(column, row);
            cell = cell.getTopLeftCellFromRegion();
            metaInfoReader.addRule(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
        }
    }

    private static void writeMetaInfoForAction(DecisionTable decisionTable,
                                               ILogicalTable originalTable,
                                               int column,
                                               int row,
                                               String header,
                                               String[] parameterNames,
                                               String conditionStatement,
                                               IOpenClass[] typeOfColumns,
                                               String url) {
        Objects.requireNonNull(header);
        var metaReader = decisionTable.getSyntaxNode().getMetaInfoReader();
        if (metaReader instanceof DecisionTableMetaInfoReader metaInfoReader) {
            var cell = originalTable.getSource().getCell(column, row);
            cell = cell.getTopLeftCellFromRegion();
            metaInfoReader.addAction(cell.getAbsoluteRow(),
                    cell.getAbsoluteColumn(),
                    header,
                    parameterNames,
                    conditionStatement,
                    typeOfColumns,
                    url,
                    null);
        }
    }

    private static void writeMetaInfoForHConditions(ILogicalTable originalTable,
                                                    DecisionTable decisionTable,
                                                    List<DTHeader> conditions,
                                                    int firstColumnForHCondition,
                                                    WithVerticalTitles withVerticalTitles,
                                                    Map<DTHeader, IOpenClass> hConditionTypes) {
        var metaInfoReader = decisionTable.getSyntaxNode().getMetaInfoReader();
        var j = 0;
        var hDtHeaders = conditions.stream().filter(DTHeader::isHCondition).collect(toList());
        int minColumn;
        if (!WithVerticalTitles.NO.equals(withVerticalTitles) && firstColumnForHCondition > 0) {
            minColumn = firstColumnForHCondition - originalTable.getSource()
                    .getCell(firstColumnForHCondition - 1, 0)
                    .getWidth();
            var vDtHeaders = conditions.stream()
                    .filter(e -> e.isCondition() && !e.isHCondition())
                    .collect(toList());
            if (!vDtHeaders.isEmpty()) {
                var lastVCondition = vDtHeaders.getLast();
                if (lastVCondition instanceof DeclaredDTHeader declaredDTHeader) {
                    if (!declaredDTHeader.isVerticalConditionWithMergedTitle()) {
                        minColumn = hDtHeaders.stream().mapToInt(DTHeader::getColumn).min().orElse(0);
                    }
                }
            }
        } else {
            minColumn = hDtHeaders.stream().mapToInt(DTHeader::getColumn).min().orElse(0);
        }
        var numOfCondition = 1;
        for (DTHeader condition : hDtHeaders) {
            var column = minColumn;
            while (column < originalTable.getSource().getWidth()) {
                var cell = originalTable.getSource().getCell(column, j);
                cell = cell.getTopLeftCellFromRegion();
                var cellValue = cell.getStringValue();
                if (cellValue != null && metaInfoReader instanceof DecisionTableMetaInfoReader reader) {
                    var type = hConditionTypes.get(condition);
                    if (type == null) {
                        type = getTypeForCondition(decisionTable, condition);
                    }
                    reader.addCondition(cell.getAbsoluteRow(),
                            cell.getAbsoluteColumn(),
                            (DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey() + numOfCondition),
                            null,
                            condition.getStatement(),
                            new IOpenClass[]{type},
                            condition instanceof DeclaredDTHeader ddth ? ddth.getMatchedDefinition()
                                    .getDtColumnsDefinition()
                                    .getUri() : null,
                            null,
                            true);
                }
                column = column + cell.getWidth();
            }
            j = j + originalTable.getSource().getCell(originalTable.getSource().getWidth() - 1, j).getHeight();
            numOfCondition++;
        }
    }

    private static String toLowerCase(String x) {
        return x != null ? x.toLowerCase() : null;
    }

    private static MatchedDefinition matchByDTColumnDefinition(DecisionTable decisionTable,
                                                               DTColumnsDefinition definition,
                                                               int numberOfHConditions,
                                                               IBindingContext bindingContext) {
        var header = decisionTable.getHeader();
        var mayHaveCompilationErrors = false;
        if (definition.isReturn()) {
            var methodReturnType = header.getType();
            if (definition.getCompositeMethod() == null) {
                return null;
            }
            var definitionType = definition.getCompositeMethod().getType();
            var openCast = bindingContext.getCast(definitionType, methodReturnType);
            if (openCast == null || !openCast.isImplicit()) {
                mayHaveCompilationErrors = true;
            }
        }

        List<ExpressionIdentifier> identifiers = definition.getIdentifiers();

        var completeParameters = new HashMap<String, IParameterDeclaration>();
        for (IParameterDeclaration parameter : definition.getParameters()) {
            if (parameter != null && parameter.getName() != null) {
                completeParameters.put(toLowerCase(parameter.getName()), parameter);
            }
        }

        var methodParametersUsedInExpression = new HashSet<String>();
        var originalMethodParametersUsedInExpression = new HashMap<String, String>();
        for (ExpressionIdentifier identifier : identifiers) {
            if (!completeParameters.containsKey(toLowerCase(identifier.getIdentifier()))) {
                methodParametersUsedInExpression.add(toLowerCase(identifier.getIdentifier()));
                originalMethodParametersUsedInExpression.put(toLowerCase(identifier.getIdentifier()),
                        identifier.getIdentifier());
            }
        }

        var methodParametersToRename = new HashMap<String, String>();
        var usedMethodParameterIndexes = new HashSet<Integer>();
        Iterator<String> itr = methodParametersUsedInExpression.iterator();
        var matchType = MatchType.STRICT;
        var paramToIndex = new HashMap<String, Integer>();
        var usedParamIndexesByField = new HashSet<Integer>();
        while (itr.hasNext()) {
            var param = itr.next();
            var found = false;
            for (var i = 0; i < definition.getHeader().getSignature().getNumberOfParameters(); i++) {
                if (param.equalsIgnoreCase(definition.getHeader().getSignature().getParameterName(i))) {
                    paramToIndex.put(param, i);
                    found = true;
                    var type = definition.getHeader().getSignature().getParameterType(i);
                    for (var j = 0; j < header.getSignature().getNumberOfParameters(); j++) {
                        if (param.equalsIgnoreCase(header.getSignature().getParameterName(j)) && type
                                .isAssignableFrom(header.getSignature().getParameterType(j))) {
                            usedMethodParameterIndexes.add(j);
                            methodParametersToRename.put(param, header.getSignature().getParameterName(j));
                            break;
                        }
                    }
                    break;
                }
            }
            if (!found) {
                var numberOfCandidates = 0;
                for (var i = 0; i < definition.getHeader().getSignature().getNumberOfParameters(); i++) {
                    var paramType = definition.getHeader().getSignature().getParameterType(i);
                    var field = paramType.getField(param, false);
                    if (field != null) {
                        for (var j = 0; j < header.getSignature().getNumberOfParameters(); j++) {
                            if (paramType.isAssignableFrom(header.getSignature().getParameterType(j))) {
                                usedParamIndexesByField.add(j);
                                numberOfCandidates++;
                            }
                        }
                    }
                }
                if (numberOfCandidates > 1) {
                    mayHaveCompilationErrors = true;
                }
                itr.remove();
            }
        }

        MatchType[] matchTypes = {MatchType.STRICT_CASTED,
                MatchType.METHOD_ARGS_RENAMED,
                MatchType.METHOD_ARGS_RENAMED_CASTED};

        for (MatchType mt : matchTypes) {
            itr = methodParametersUsedInExpression.iterator();
            while (itr.hasNext()) {
                var param = itr.next();
                if (methodParametersToRename.containsKey(param)) {
                    continue;
                }
                var j = paramToIndex.get(param);
                var type = definition.getHeader().getSignature().getParameterType(j);
                var duplicatedMatch = false;
                for (var i = 0; i < header.getSignature().getNumberOfParameters(); i++) {
                    boolean predicate;
                    var openCast = bindingContext.getCast(header.getSignature().getParameterType(i), type);
                    switch (mt) {
                        case METHOD_ARGS_RENAMED_CASTED:
                            predicate = openCast != null && openCast.isImplicit();
                            break;
                        case STRICT_CASTED:
                            predicate = openCast != null && openCast.isImplicit() && param
                                    .equalsIgnoreCase(header.getSignature().getParameterName(i));
                            break;
                        case METHOD_ARGS_RENAMED:
                            predicate = type.isAssignableFrom(header.getSignature().getParameterType(i));
                            break;
                        default:
                            throw new IllegalStateException();
                    }

                    if (!usedMethodParameterIndexes.contains(i) && predicate) {
                        if (duplicatedMatch) {
                            return null;
                        }
                        duplicatedMatch = true;
                        matchType = mt;
                        usedMethodParameterIndexes.add(i);
                        String newParam;
                        switch (mt) {
                            case STRICT_CASTED:
                            case METHOD_ARGS_RENAMED_CASTED:
                                var typeName = type.getInstanceClass().getSimpleName();
                                if (bindingContext.findType(typeName) == null) {
                                    typeName = type.getJavaName();
                                }
                                newParam = "((" + typeName + ")" + header.getSignature().getParameterName(i) + ")";
                                break;
                            case METHOD_ARGS_RENAMED:
                                newParam = header.getSignature().getParameterName(i);
                                break;
                            default:
                                throw new IllegalStateException();
                        }
                        methodParametersToRename.put(param, newParam);
                    }
                }
            }
        }

        if (usedMethodParameterIndexes.size() != methodParametersUsedInExpression.size()) {
            if (numberOfHConditions > 0) {
                return null;
            }
            var u = new HashSet<String>();
            for (var i = 0; i < header.getSignature().getNumberOfParameters(); i++) {
                u.add(header.getSignature().getParameterName(i));
            }
            for (var i = 0; i < header.getSignature().getNumberOfParameters(); i++) {
                String lowParamName = toLowerCase(header.getSignature().getParameterName(i));
                if (!usedMethodParameterIndexes.contains(i) && methodParametersUsedInExpression
                        .contains(lowParamName)) {
                    var newParamName = "_" + originalMethodParametersUsedInExpression.get(lowParamName);
                    while (u.contains(newParamName)) {
                        newParamName = "_" + newParamName;
                    }
                    u.add(newParamName);
                    methodParametersToRename.put(lowParamName, newParamName);
                }
            }
            mayHaveCompilationErrors = true;
        }

        final var code = definition.getExpression();

        var usedParamIndexes = new HashSet<Integer>(usedMethodParameterIndexes);
        usedParamIndexes.addAll(usedParamIndexesByField);

        int[] usedMethodParameterIndexesArray = ArrayUtils.toPrimitive(usedParamIndexes.toArray(new Integer[0]));

        return switch (matchType) {
            case STRICT -> new MatchedDefinition(definition,
                        code,
                        usedMethodParameterIndexesArray,
                        methodParametersToRename,
                        identifiers,
                        MatchType.STRICT,
                        mayHaveCompilationErrors);
            case STRICT_CASTED -> new MatchedDefinition(definition,
                        code,
                        usedMethodParameterIndexesArray,
                        methodParametersToRename,
                        identifiers,
                        MatchType.STRICT_CASTED,
                        mayHaveCompilationErrors);
            case METHOD_ARGS_RENAMED -> new MatchedDefinition(definition,
                        code,
                        usedMethodParameterIndexesArray,
                        methodParametersToRename,
                        identifiers,
                        MatchType.METHOD_ARGS_RENAMED,
                        mayHaveCompilationErrors);
            case METHOD_ARGS_RENAMED_CASTED -> new MatchedDefinition(definition,
                        code,
                        usedMethodParameterIndexesArray,
                        methodParametersToRename,
                        identifiers,
                        MatchType.METHOD_ARGS_RENAMED_CASTED,
                        mayHaveCompilationErrors);
            default -> null;
        };
    }

    private static ParameterTokens buildParameterTokens(DecisionTable decisionTable) {
        var numberOfParameters = decisionTable.getSignature().getNumberOfParameters();
        var tokenToParameterIndex = new HashMap<Token, Integer>();
        var tokenToFieldsChain = new HashMap<Token, IOpenField[]>();
        var tokens = new HashSet<Token>();
        var tokensToIgnore = new HashSet<Token>();
        for (var i = 0; i < numberOfParameters; i++) {
            var parameterType = decisionTable.getSignature().getParameterType(i);
            if (isCompoundInputType(parameterType) && !parameterType.isArray()) {
                var openClassFuzzyTokens = OpenLFuzzyUtils
                        .tokensMapToOpenClassReadableFieldsRecursively(parameterType,
                                decisionTable.getSignature().getParameterName(i),
                                1);
                for (Map.Entry<Token, IOpenField[][]> entry : openClassFuzzyTokens.entrySet()) {
                    if (entry.getValue().length == 1 && !tokensToIgnore.contains(entry.getKey())) {
                        if (!tokens.contains(entry.getKey())) {
                            tokens.add(entry.getKey());
                            tokenToParameterIndex.put(entry.getKey(), i);
                            tokenToFieldsChain.put(entry.getKey(), entry.getValue()[0]);
                        } else {
                            tokens.remove(entry.getKey());
                            tokenToParameterIndex.remove(entry.getKey());
                            tokenToFieldsChain.remove(entry.getKey());
                            tokensToIgnore.add(entry.getKey());
                        }
                    }
                }
            }
        }
        for (var i = 0; i < numberOfParameters; i++) {
            String tokenString = OpenLFuzzyUtils
                    .toTokenString(OpenLFuzzyUtils.phoneticFix(decisionTable.getSignature().getParameterName(i)));
            var token = new Token(tokenString, 0);
            tokenToParameterIndex.put(token, i);
            tokens.add(token);
        }

        return new ParameterTokens(tokens.toArray(new Token[]{}), tokenToParameterIndex, tokenToFieldsChain);
    }

    private static class PredicateToken extends Token {
        @Getter
        boolean isTrue;

        public PredicateToken(String value, int distance, int minMatchedTokens, boolean isTrue) {
            super(value, distance, minMatchedTokens);
            this.isTrue = isTrue;
        }
    }

    private static class RuleToken extends Token {
        public RuleToken(String value, int distance, int minMatchedTokens) {
            super(value, distance, minMatchedTokens);
        }
    }

    private static void matchWithFuzzySearchRec(DecisionTable decisionTable,
                                                ILogicalTable originalTable,
                                                IGridTable gridTable,
                                                FuzzyContext fuzzyContext,
                                                NumberOfColumnsUnderTitleCounter numberOfColumnsUnderTitleCounter,
                                                int numberOfHConditions,
                                                List<DTHeader> dtHeaders,
                                                int firstColumnHeight,
                                                int w,
                                                int h,
                                                List<String> parts,
                                                int sourceTableColumn,
                                                int firstColumnForHCondition,
                                                boolean skipNextColumn,
                                                WithVerticalTitles withVerticalTitles,
                                                boolean onlyReturns) {
        var w0 = gridTable.getCell(w, h).getWidth();
        var h0 = gridTable.getCell(w, h).getHeight();
        var d = gridTable.getCell(w, h).getStringValue();
        String mergedPartsTitle;
        if (sourceTableColumn + originalTable.getSource()
                .getCell(sourceTableColumn, 0)
                .getWidth() == firstColumnForHCondition && h == firstColumnHeight - 1 && (WithVerticalTitles.SLASH_IN_TITLE
                .equals(withVerticalTitles) && StringUtils.isNotBlank(
                d) && d.contains(HORIZONTAL_VERTICAL_CONDITIONS_SPLITTER) || WithVerticalTitles.MERGED_COLUMN
                .equals(withVerticalTitles) || WithVerticalTitles.EMPTY_COLUMN.equals(withVerticalTitles))) {
            if (!onlyReturns) {
                var hTitles = new ArrayList<String>(parts);
                var p = d;
                if (WithVerticalTitles.SLASH_IN_TITLE.equals(withVerticalTitles)) {
                    p = d.substring(d.indexOf(HORIZONTAL_VERTICAL_CONDITIONS_SPLITTER) + 1).trim();
                }
                hTitles.add(p);
                var horizontal = 0;
                for (String hTitle : hTitles) {
                    String tokenizedTitleString = OpenLFuzzyUtils.toTokenString(hTitle);
                    var tokens = fuzzyContext.getParameterTokens().getTokens();
                    tokens = addTrueFalseTokens(fuzzyContext.getMaxDistance(), tokens);
                    var fuzzyResults = OpenLFuzzyUtils.fuzzyExtract(tokenizedTitleString, tokens, true);
                    addFuzzyDtHeader(decisionTable,
                            fuzzyContext,
                            w,
                            h,
                            hTitle,
                            sourceTableColumn + originalTable.getSource().getCell(sourceTableColumn, 0).getWidth(),
                            1,
                            1,
                            fuzzyResults,
                            dtHeaders,
                            horizontal + 1);
                    horizontal++;
                }
            }
            String p;
            if (WithVerticalTitles.SLASH_IN_TITLE.equals(withVerticalTitles)) {
                p = d.substring(0, d.indexOf(HORIZONTAL_VERTICAL_CONDITIONS_SPLITTER)).trim();
            } else {
                return;
            }
            parts.add(p);
            mergedPartsTitle = p;
        } else {
            parts.add(d);
            mergedPartsTitle = String.join(" | ", parts);
        }
        if (h + h0 < firstColumnHeight) {
            var w2 = w;
            while (w2 < w + w0) {
                var w1 = gridTable.getCell(w2, h + h0).getWidth();
                matchWithFuzzySearchRec(decisionTable,
                        originalTable,
                        gridTable,
                        fuzzyContext,
                        numberOfColumnsUnderTitleCounter,
                        numberOfHConditions,
                        dtHeaders,
                        firstColumnHeight,
                        w2,
                        h + h0,
                        parts,
                        sourceTableColumn,
                        firstColumnForHCondition,
                        skipNextColumn,
                        withVerticalTitles,
                        onlyReturns);
                w2 = w2 + w1;
            }
        } else {
            String tokenizedTitleString = OpenLFuzzyUtils.toTokenString(mergedPartsTitle);
            if (fuzzyContext.isFuzzySupportsForReturnType()) {
                var fuzzyResults = OpenLFuzzyUtils
                        .fuzzyExtract(mergedPartsTitle, fuzzyContext.getFuzzyReturnTokens(), true);
                for (FuzzyResult fuzzyResult : fuzzyResults) {
                    var fieldsChains = fuzzyContext.getFieldsChainsForReturnToken(fuzzyResult.getToken());
                    for (IOpenField[] fieldsChain : fieldsChains) {
                        Objects.requireNonNull(fieldsChain);
                        dtHeaders.add(new FuzzyDTHeader(-1,
                                null,
                                mergedPartsTitle,
                                fieldsChain,
                                sourceTableColumn,
                                sourceTableColumn + w,
                                h,
                                w0,
                                w0,
                                fuzzyResult,
                                true,
                                false));
                    }
                }
            }
            if (!onlyReturns) {
                var tokens = fuzzyContext.getParameterTokens().getTokens();
                if (numberOfColumnsUnderTitleCounter.get(sourceTableColumn) == 1) {
                    if (firstColumnForHCondition < 0 && numberOfHConditions > 0 && Arrays
                            .stream(decisionTable.getSignature().getParameterTypes())
                            .anyMatch(
                                    e -> e.getInstanceClass() == Boolean.class || e.getInstanceClass() == boolean.class)) {
                        tokens = ArrayUtils.addAll(tokens,
                                new PredicateToken("is true", fuzzyContext.getMaxDistance() + 1, 2, true),
                                new PredicateToken("is false", fuzzyContext.getMaxDistance() + 1, 2, false));
                    } else {
                        tokens = addTrueFalseTokens(fuzzyContext.getMaxDistance(), tokens);
                    }
                    if (sourceTableColumn == 0) {
                        tokens = ArrayUtils.addAll(tokens, new RuleToken("rule", fuzzyContext.getMaxDistance() + 1, 1));
                    }
                }
                var fuzzyResults = OpenLFuzzyUtils.fuzzyExtract(tokenizedTitleString, tokens, true);
                addFuzzyDtHeader(decisionTable,
                        fuzzyContext,
                        w,
                        h,
                        mergedPartsTitle,
                        sourceTableColumn,
                        skipNextColumn ? w0 + originalTable.getSource().getCell(sourceTableColumn + w0, h).getWidth() : w0,
                        w0,
                        fuzzyResults,
                        dtHeaders,
                        0);
            }
        }
        parts.removeLast();
    }

    private static Token[] addTrueFalseTokens(int maxDistance, Token[] tokens) {
        return ArrayUtils.addAll(tokens,
                new PredicateToken("is true", maxDistance + 1, 2, true),
                new PredicateToken("is false", maxDistance + 1, 2, false),
                new PredicateToken("true", maxDistance + 1, 1, true),
                new PredicateToken("false", maxDistance + 1, 1, false));
    }

    private static void addFuzzyDtHeader(DecisionTable decisionTable,
                                         FuzzyContext fuzzyContext,
                                         int w,
                                         int h,
                                         String title,
                                         int sourceTableColumn,
                                         int w0,
                                         int widthForMerge,
                                         List<FuzzyResult> fuzzyResults,
                                         List<DTHeader> dtHeaders,
                                         int horizontal) {
        for (FuzzyResult fuzzyResult : fuzzyResults) {
            var paramIndex = fuzzyContext.getParameterTokens().getParameterIndex(fuzzyResult.getToken());
            if (paramIndex != null) {
                var fieldsChain = fuzzyContext.getParameterTokens().getFieldsChain(fuzzyResult.getToken());
                var conditionStatement = new StringBuilder(
                        decisionTable.getSignature().getParameterName(paramIndex));
                if (fieldsChain != null) {
                    var c = buildStatementByFieldsChain(
                            decisionTable.getSignature().getParameterType(paramIndex),
                            fieldsChain);
                    var chainStatement = c.getLeft();
                    conditionStatement.append(".");
                    conditionStatement.append(chainStatement);
                }
                dtHeaders.add(new FuzzyDTHeader(paramIndex,
                        conditionStatement.toString(),
                        title,
                        fieldsChain,
                        sourceTableColumn,
                        horizontal > 0 ? sourceTableColumn + horizontal - 1 : sourceTableColumn + w,
                        h,
                        horizontal > 0 ? 1 : w0,
                        horizontal > 0 ? 1 : widthForMerge,
                        fuzzyResult,
                        false,
                        horizontal > 0));
            } else {
                if (fuzzyResult.getToken() instanceof PredicateToken) {
                    var predicateToken = (PredicateToken) fuzzyResult.getToken();
                    dtHeaders.add(new FuzzyDTHeader(predicateToken.isTrue() ? "true" : "false",
                            title,
                            new IOpenField[]{},
                            sourceTableColumn,
                            horizontal > 0 ? sourceTableColumn + horizontal - 1 : sourceTableColumn,
                            h,
                            horizontal > 0 ? 1 : w0,
                            horizontal > 0 ? 1 : widthForMerge,
                            fuzzyResult,
                            false,
                            horizontal > 0));
                }
                if (sourceTableColumn == 0 && fuzzyResult.getToken() instanceof RuleToken) {
                    dtHeaders.add(new FuzzyRulesDTHeader(title, sourceTableColumn, h, w0, fuzzyResult));
                }
            }
        }

    }

    private static List<DTHeader> matchWithFuzzySearch(DecisionTable decisionTable,
                                                       ILogicalTable originalTable,
                                                       FuzzyContext fuzzyContext,
                                                       NumberOfColumnsUnderTitleCounter numberOfColumnsUnderTitleCounter,
                                                       int numberOfHConditions,
                                                       int column,
                                                       int lastColumn,
                                                       List<DTHeader> dtHeaders,
                                                       int firstColumnHeight,
                                                       int firstColumnForHCondition,
                                                       WithVerticalTitles withVerticalTitles,
                                                       boolean onlyReturns) {
        if (onlyReturns && !fuzzyContext.isFuzzySupportsForReturnType()) {
            return Collections.emptyList();
        }
        if (numberOfHConditions > 0 && column >= lastColumn) {
            return Collections.emptyList();
        }
        var w = originalTable.getSource().getCell(column, 0).getWidth();
        var gt = originalTable.getSource().getSubtable(column, 0, w, firstColumnHeight);
        var newDtHeaders = new ArrayList<DTHeader>();

        var w0 = column + originalTable.getSource().getCell(column, 0).getWidth();
        var skipNextColumn = w0 + originalTable.getSource()
                .getCell(w0, 0)
                .getWidth() == firstColumnForHCondition && (WithVerticalTitles.EMPTY_COLUMN
                .equals(withVerticalTitles) || WithVerticalTitles.MERGED_COLUMN.equals(withVerticalTitles));

        matchWithFuzzySearchRec(decisionTable,
                originalTable,
                gt,
                fuzzyContext,
                numberOfColumnsUnderTitleCounter,
                numberOfHConditions,
                newDtHeaders,
                firstColumnHeight,
                0,
                0,
                new ArrayList<>(),
                column,
                firstColumnForHCondition,
                skipNextColumn,
                withVerticalTitles,
                onlyReturns);
        dtHeaders.addAll(newDtHeaders);
        return Collections.unmodifiableList(newDtHeaders);
    }

    private static boolean isCompatibleHeaders(DTHeader a, DTHeader b) {
        var c1 = a.getColumn();
        var c2 = a.getColumn() + a.getWidth() - 1;
        var d1 = b.getColumn();
        var d2 = b.getColumn() + b.getWidth() - 1;

        if (c1 <= d1 && d1 <= c2 || c1 <= d2 && d2 <= c2 || d1 <= c2 && c2 <= d2 || d1 <= c1 && c1 <= d2) {
            return false;
        }

        if ((a.isRule() && b.isCondition() || a.isCondition() && b.isAction() || a.isAction() && b.isReturn() || a
                .isCondition() && b.isReturn()) && c1 >= d1) {
            return false;
        }
        if ((b.isRule() && a.isCondition() || b.isCondition() && a.isAction() || b.isAction() && a.isReturn() || b
                .isCondition() && a.isReturn()) && d1 >= c1) {
            return false;
        }

        if (a instanceof FuzzyDTHeader a1 && b instanceof FuzzyDTHeader b1) {
            if (a1.isMethodParameterUsed() && b1.isMethodParameterUsed()) {
                if (a1.isCondition() && b1
                        .isCondition() && a1.getMethodParameterIndex() == b1.getMethodParameterIndex() && Arrays
                        .deepEquals(a1.getFieldsChain(), b1.getFieldsChain())) {
                    return false;
                }
            }

            if (a1.isReturn() && b1.isReturn() && fieldsChainsIsCrossed(a1.getFieldsChain(), b1.getFieldsChain())) {
                return false;
            }

            if (!(a1.isHCondition() && b1.isHCondition() || a1.isCondition() && b1.isCondition() || a1.isAction() && b1
                    .isAction() || a1.isReturn() && b1.isReturn()) && a1.getTopColumn() == b1.getTopColumn()) {
                return false;
            }
        }
        if (a instanceof DeclaredDTHeader a1 && b instanceof DeclaredDTHeader b1) {
            return !a1.getMatchedDefinition()
                    .getDtColumnsDefinition()
                    .equals(b1.getMatchedDefinition().getDtColumnsDefinition());
        }
        return true;
    }

    private static final int FITS_MAX_LIMIT = 10000;
    private static final int MAX_NUMBER_OF_RETURNS = 3;

    private static boolean bruteForceHeaders(ILogicalTable originalTable,
                                             int column,
                                             int lastColumn,
                                             int firstColumnHeight,
                                             List<DTHeader> dtHeaders,
                                             boolean[][] matrix,
                                             Map<Integer, List<Integer>> columnToIndex,
                                             int maxColumnIndex,
                                             List<Integer> usedIndexes,
                                             List<DTHeader> used,
                                             Set<Integer> usedParameterIndexes,
                                             List<List<DTHeader>> fits,
                                             Set<Integer> failedToFit,
                                             int numberOfParameters,
                                             int numberOfHConditions,
                                             int numberOfReturns,
                                             int fuzzyReturnsFlag,
                                             int counter) {
        if (fits.size() > FITS_MAX_LIMIT) {
            return column >= maxColumnIndex;
        }
        List<Integer> indexes = columnToIndex.get(column);
        if (indexes == null || numberOfHConditions == 1 && usedParameterIndexes
                .size() >= numberOfParameters - numberOfHConditions + used.stream()
                .filter(DTHeader::isHCondition)
                .count()) {
            var fit = new ArrayList<DTHeader>(used);
            while (!fit.isEmpty() && (fit.getLast() instanceof UnmatchedDtHeader)) {
                fit.removeLast();
            }
            if (!fit.isEmpty()) {
                fits.add(Collections.unmodifiableList(fit));
            }
        }
        var lastColumnReached = column >= maxColumnIndex;
        if (indexes != null) {
            var last = true;
            for (Integer index : indexes) {
                var f = true;
                for (Integer usedIndex : usedIndexes) {
                    if (!matrix[index][usedIndex]) {
                        f = false;
                        break;
                    }
                }
                if (f) {
                    var dtHeader = dtHeaders.get(index);
                    var isFuzzyReturn = false;
                    if (dtHeader instanceof FuzzyDTHeader fuzzyDTHeader) {
                        if (fuzzyDTHeader.isReturn()) {
                            isFuzzyReturn = true;
                        }
                    }
                    if (isFuzzyReturn && fuzzyReturnsFlag == 2) {
                        continue;
                    }
                    var usedParameterIndexesTo = new HashSet<Integer>(usedParameterIndexes);
                    for (int i : dtHeader.getMethodParameterIndexes()) {
                        usedParameterIndexesTo.add(i);
                    }
                    int numberOfReturns1 = dtHeader.isReturn() && !isFuzzyReturn ? numberOfReturns + 1
                            : numberOfReturns;
                    int fuzzyReturnsFlag1 = isFuzzyReturn && fuzzyReturnsFlag != 1 ? fuzzyReturnsFlag + 1
                            : fuzzyReturnsFlag;
                    if (numberOfReturns1 + (fuzzyReturnsFlag1 > 1 ? 1 : 0) <= MAX_NUMBER_OF_RETURNS) {
                        last = false;
                        usedIndexes.add(index);
                        used.add(dtHeaders.get(index));
                        lastColumnReached = lastColumnReached | bruteForceHeaders(originalTable,
                                column + dtHeader.getWidth(),
                                lastColumn,
                                firstColumnHeight,
                                dtHeaders,
                                matrix,
                                columnToIndex,
                                maxColumnIndex,
                                usedIndexes,
                                used,
                                usedParameterIndexesTo,
                                fits,
                                failedToFit,
                                numberOfParameters,
                                numberOfHConditions,
                                numberOfReturns1,
                                fuzzyReturnsFlag1,
                                counter + 1);
                        usedIndexes.removeLast();
                        used.removeLast();
                    }
                }
            }
            if (!indexes.isEmpty() && last) {
                failedToFit.addAll(indexes);
            }
        }
        if (!lastColumnReached && (numberOfReturns + (fuzzyReturnsFlag > 1 ? 1 : 0)) == 0) {
            var cell = originalTable.getSource().getCell(column, firstColumnHeight - 1);
            if (column + cell.getWidth() <= maxColumnIndex) {
                var isHorizontal = column + cell.getWidth() >= lastColumn;
                used.add(new UnmatchedDtHeader(StringUtils.EMPTY,
                        column,
                        firstColumnHeight - 1,
                        cell.getWidth(),
                        isHorizontal));

                lastColumnReached = bruteForceHeaders(originalTable,
                        column + cell.getWidth(),
                        lastColumn,
                        firstColumnHeight,
                        dtHeaders,
                        matrix,
                        columnToIndex,
                        maxColumnIndex,
                        usedIndexes,
                        used,
                        usedParameterIndexes,
                        fits,
                        failedToFit,
                        numberOfParameters,
                        numberOfHConditions,
                        numberOfReturns,
                        fuzzyReturnsFlag,
                        counter + 1);
                used.removeLast();
            }
        }
        return lastColumnReached;
    }

    private static List<List<DTHeader>> filterHeadersByMax(List<List<DTHeader>> fits,
                                                           ToLongFunction<List<DTHeader>> function,
                                                           Predicate<List<DTHeader>> predicate) {
        var max = Long.MIN_VALUE;
        var functionIndexes = new HashSet<Integer>();
        var matchIndexes = new HashSet<Integer>();
        var index = 0;
        for (List<DTHeader> fit : fits) {
            if (predicate.test(fit)) {
                var current = function.applyAsLong(fit);
                if (current > max) {
                    max = current;
                    functionIndexes.clear();
                    functionIndexes.add(index);
                } else if (current == max) {
                    functionIndexes.add(index);
                }
            } else {
                matchIndexes.add(index);
            }
            index++;
        }

        var indexes = new HashSet<Integer>(matchIndexes);
        indexes.addAll(functionIndexes);
        var newFits = new ArrayList<List<DTHeader>>();
        for (Integer i : indexes) {
            newFits.add(fits.get(i));
        }
        return newFits;
    }

    private static List<List<DTHeader>> filterHeadersByMin(List<List<DTHeader>> fits,
                                                           ToLongFunction<List<DTHeader>> function,
                                                           Predicate<List<DTHeader>> predicate) {
        var min = Long.MAX_VALUE;
        var functionIndexes = new HashSet<Integer>();
        var matchIndexes = new HashSet<Integer>();
        var index = 0;
        for (List<DTHeader> fit : fits) {
            if (predicate.test(fit)) {
                var current = function.applyAsLong(fit);
                if (current < min) {
                    min = current;
                    functionIndexes.clear();
                    functionIndexes.add(index);
                } else if (current == min) {
                    functionIndexes.add(index);
                }
            } else {
                matchIndexes.add(index);
            }
            index++;
        }
        var indexes = new HashSet<Integer>(matchIndexes);
        indexes.addAll(functionIndexes);
        var newFits = new ArrayList<List<DTHeader>>();
        for (Integer i : indexes) {
            newFits.add(fits.get(i));
        }
        return newFits;
    }

    private static List<List<DTHeader>> filterHeadersByMatchType(DecisionTable decisionTable,
                                                                 List<List<DTHeader>> fits) {
        resolveConflictsInDeclaredDtHeaders(decisionTable, fits);
        MatchType[] matchTypes = MatchType.values();
        Arrays.sort(matchTypes, Comparator.comparingInt(MatchType::getPriority));
        for (MatchType type : matchTypes) {
            fits = filterHeadersByMax(fits,
                    e -> e.stream()
                            .filter(x -> x instanceof DeclaredDTHeader)
                            .map(x -> (DeclaredDTHeader) x)
                            .filter(x -> type.equals(x.getMatchedDefinition().getMatchType()))
                            .mapToLong(x -> x.getMatchedDefinition().getDtColumnsDefinition().getNumberOfTitles())
                            .sum(),
                    e -> true);
        }
        return fits;
    }

    private static boolean isLastDtColumnValid(DTHeader dtHeader, int maxColumn, int columnsForReturn) {
        if (dtHeader.isReturn()) {
            return dtHeader.getColumn() + dtHeader.getWidth() == maxColumn;
        }
        if (!dtHeader.isHCondition() && dtHeader.isCondition() || dtHeader.isAction()) {
            return dtHeader.getColumn() + dtHeader.getWidth() < maxColumn - columnsForReturn;
        }
        return true;
    }

    private static List<List<DTHeader>> filterWithWrongStructure(ILogicalTable originalTable,
                                                                 List<List<DTHeader>> fits,
                                                                 boolean twoColumnsInReturn) {
        var maxColumn = originalTable.getSource().getWidth();
        var w = 0;
        if (maxColumn > 0 && twoColumnsInReturn) {
            w = originalTable.getSource().getCell(maxColumn - 1, 0).getWidth();
            if (maxColumn - w > 0) {
                w = w + originalTable.getSource().getCell(maxColumn - 1 - w, 0).getWidth();
            }
        }
        final var w1 = w;

        return fits.stream()
                .filter(
                        e -> e.isEmpty() || isLastDtColumnValid(e.getLast(), maxColumn, twoColumnsInReturn ? w1 : 0))
                .collect(toList());
    }

    private static boolean fieldsChainsIsCrossed(IOpenField[] m1, IOpenField[] m2) {
        if (m1 == null && m2 == null) {
            return true;
        }
        if (m1 != null && m2 != null) {
            var i = 0;
            while (i < m1.length && i < m2.length) {
                if (m1[i].equals(m2[i])) {
                    i++;
                } else {
                    break;
                }
            }
            return i == m1.length || i == m2.length;
        }
        return false;
    }

    private static boolean isAmbiguousFits(List<List<DTHeader>> fits, Predicate<DTHeader> predicate) {
        if (fits.size() <= 1) {
            return false;
        }
        var dtHeaders0 = fits.getFirst().stream().filter(predicate).toArray(DTHeader[]::new);
        for (var i = 1; i < fits.size(); i++) {
            var dtHeaders1 = fits.get(i).stream().filter(predicate).toArray(DTHeader[]::new);
            if (!Arrays.equals(dtHeaders0, dtHeaders1)) {
                return true;
            }
        }
        return false;
    }

    private static boolean intersects(int b1, int e1, int b2, int e2) {
        return b2 <= b1 && b1 <= e2 || b2 <= e1 && e1 <= e2 || b1 <= b2 && b2 <= e1 || b1 <= e2 && e2 <= e1;
    }

    private static List<DTHeader> findStrongDtHeaders(ILogicalTable originalTable, List<DTHeader> dtHeaders) {
        // Remove headers that intersect with declared dt header if declared dt header is matched 100%
        boolean[] f = new boolean[dtHeaders.size()];
        Arrays.fill(f, false);
        for (var i = 0; i < dtHeaders.size() - 1; i++) {
            for (var j = i + 1; j < dtHeaders.size(); j++) {
                if (dtHeaders.get(i) instanceof DeclaredDTHeader && dtHeaders.get(j) instanceof DeclaredDTHeader) {
                    var d1 = (DeclaredDTHeader) dtHeaders.get(i);
                    var d2 = (DeclaredDTHeader) dtHeaders.get(j);
                    if (!d1.isHCondition() && !d2.isHCondition()) {
                        if (!(d1.getColumn() == d2.getColumn() && d1.getWidth() == d2.getWidth()) && intersects(
                                d1.getColumn(),
                                d1.getColumn() + d1.getWidth() - 1,
                                d2.getColumn(),
                                d2.getColumn() + d2.getWidth() - 1)) {
                            f[i] = true;
                            f[j] = true;
                        }
                    }
                }
            }
        }
        final var lastColumn = originalTable.getSource().getWidth();
        var ret = new ArrayList<DTHeader>();
        for (var i = 0; i < dtHeaders.size(); i++) {
            var dtHeader = dtHeaders.get(i);
            // Exclude from optimization conditions and actions that matches to the last column, where return is
            // expected.
            if (!dtHeader.isHCondition() && (dtHeader.isCondition() || dtHeader.isAction()) && dtHeader
                    .getColumn() + dtHeader.getWidth() >= lastColumn) {
                continue;
            }
            if (dtHeader.isHCondition() || !f[i]) {
                ret.add(dtHeader);
            }
        }
        return ret;
    }

    private static List<List<DTHeader>> fitFuzzyDtHeaders(List<List<DTHeader>> fits) {
        fits = filterHeadersByMax(fits,
                e -> e.stream()
                        .filter(x -> x instanceof FuzzyDTHeader)
                        .map(x -> (FuzzyDTHeader) x)
                        .mapToInt(x -> x.getFuzzyResult().getFoundTokensCount())
                        .sum(),
                e -> true);
        fits = filterHeadersByMin(fits,
                e -> e.stream()
                        .filter(x -> x instanceof FuzzyDTHeader)
                        .map(x -> (FuzzyDTHeader) x)
                        .mapToInt(x -> x.getFuzzyResult().getMissedTokensCount())
                        .sum(),
                e -> true);
        fits = filterHeadersByMin(fits,
                e -> e.stream()
                        .filter(x -> x instanceof FuzzyDTHeader)
                        .map(x -> (FuzzyDTHeader) x)
                        .mapToInt(x -> x.getFuzzyResult().getToken().getDistance())
                        .sum(),
                e -> true);
        fits = filterHeadersByMin(fits,
                e -> e.stream()
                        .filter(x -> x instanceof FuzzyDTHeader)
                        .map(x -> (FuzzyDTHeader) x)
                        .mapToInt(x -> x.getFuzzyResult().getUnmatchedTokensCount())
                        .sum(),
                e -> true);
        return fits;
    }

    private static boolean isTheSameFit(List<DTHeader> a, List<DTHeader> b) {
        if (a.size() == b.size()) {
            for (var i = 0; i < a.size(); i++) {
                if (!Objects.equals(a.get(i), b.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static List<List<DTHeader>> removeDuplicates(List<List<DTHeader>> fits) {
        var ret = new ArrayList<List<DTHeader>>();
        for (List<DTHeader> fit : fits) {
            var f = false;
            for (List<DTHeader> e : ret) {
                if (isTheSameFit(fit, e)) {
                    f = true;
                    break;
                }
            }
            if (!f) {
                ret.add(fit);
            }
        }

        return ret;
    }

    private static List<DTHeader> fitDtHeaders(TableSyntaxNode tableSyntaxNode,
                                               DecisionTable decisionTable,
                                               ILogicalTable originalTable,
                                               List<DTHeader> dtHeaders,
                                               int lastColumn,
                                               int numberOfHConditions,
                                               boolean twoColumnsForReturn,
                                               int firstColumnHeight,
                                               IBindingContext bindingContext) throws OpenLCompilationException {
        var numberOfParameters = decisionTable.getSignature().getNumberOfParameters();
        boolean[][] matrix = new boolean[dtHeaders.size()][dtHeaders.size()];
        for (var i = 0; i < dtHeaders.size(); i++) {
            for (var j = 0; j < dtHeaders.size(); j++) {
                matrix[i][j] = true;
            }
        }
        var columnToIndex = new HashMap<Integer, List<Integer>>();
        for (var i = 0; i < dtHeaders.size(); i++) {
            List<Integer> indexes = columnToIndex.computeIfAbsent(dtHeaders.get(i).getColumn(), ArrayList::new);
            indexes.add(i);
            for (var j = i; j < dtHeaders.size(); j++) {
                if (i == j || !isCompatibleHeaders(dtHeaders.get(i), dtHeaders.get(j))) {
                    matrix[i][j] = false;
                    matrix[j][i] = false;
                }
            }
        }
        List<List<DTHeader>> fits = new ArrayList<>();
        var failedToFit = new HashSet<Integer>();
        bruteForceHeaders(originalTable,
                0,
                lastColumn,
                firstColumnHeight,
                dtHeaders,
                matrix,
                columnToIndex,
                numberOfHConditions > 0 ? lastColumn + numberOfHConditions : originalTable.getSource().getWidth(),
                new ArrayList<>(),
                new ArrayList<>(),
                new HashSet<>(),
                fits,
                failedToFit,
                numberOfParameters,
                numberOfHConditions,
                0,
                0,
                0);

        if (fits.size() > FITS_MAX_LIMIT) {
            bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(
                    "Ambiguous matching of column titles to DT conditions. Too many options are found.",
                    tableSyntaxNode));
        }

        final Predicate<List<DTHeader>> all = e -> true;

        fits = filterHeadersByMax(fits,
                e -> e.stream()
                        .map(DTHeader::getMethodParameterIndexes)
                        .filter(Objects::nonNull)
                        .flatMapToInt(Arrays::stream)
                        .distinct()
                        .count() <= numberOfParameters - numberOfHConditions + e.stream().filter(DTHeader::isHCondition).count()
                        ? 1
                        : 0,
                all);

        fits = filterWithWrongStructure(originalTable, fits, twoColumnsForReturn);

        // Declared covered columns filter
        fits = filterHeadersByMax(fits,
                e -> e.stream()
                        .filter(x -> x instanceof DeclaredDTHeader)
                        .mapToLong(
                                x -> ((DeclaredDTHeader) x).getMatchedDefinition().getDtColumnsDefinition().getNumberOfTitles())
                        .sum(),
                all);

        fits = filterBasedOnDeclaredDtHeaders(fits);

        if (numberOfHConditions == 0) {
            // Prefer full matches with return headers
            fits = fits.stream().filter(e -> e.stream().anyMatch(DTHeader::isReturn)).collect(toList());
        } else {
            // Lookup table with no returns columns
            fits = fits.stream().filter(e -> e.stream().noneMatch(DTHeader::isReturn)).collect(toList());
        }

        // matches with min returns
        fits = filterHeadersByMin(fits, DecisionTableHelper::countReturns, all);

        fits = filterHeadersByMin(fits,
                e -> e.stream().filter(e1 -> e1 instanceof UnmatchedDtHeader && !e1.isHCondition()).count(),
                all);

        fits = filterHeadersByMin(fits,
                e -> e.stream()
                        .filter(x -> x instanceof DeclaredDTHeader)
                        .map(x -> (DeclaredDTHeader) x)
                        .mapToLong(x -> x.getMatchedDefinition().isMayHaveCompilationErrors() ? 1 : 0)
                        .sum(),
                e -> e.stream().anyMatch(x -> x instanceof DeclaredDTHeader));

        fits = filterHeadersByMatchType(decisionTable, fits);

        fits = filterHeadersByMax(fits,
                e -> e.stream().flatMapToInt(c -> Arrays.stream(c.getMethodParameterIndexes())).distinct().count(),
                e -> e.stream().anyMatch(x -> x.isCondition() && x instanceof DeclaredDTHeader));

        fits = filterHeadersByMin(fits,
                e -> e.stream().filter(x -> x instanceof SimpleReturnDTHeader).count(),
                e -> e.stream().anyMatch(DTHeader::isReturn));

        fits = fitFuzzyDtHeaders(fits);

        fits = removeDuplicates(fits);

        if (numberOfHConditions == 0 && fits.isEmpty()) {
            final List<DTHeader> dths = dtHeaders;
            var c = failedToFit.stream().mapToInt(e -> dths.get(e).getColumn()).max();
            var message = new StringBuilder();
            message.append("Failed to compile a decision table.");
            if (c.isPresent()) {
                var c0 = c.getAsInt();
                var sb = new StringBuilder();
                for (var i = 0; i < firstColumnHeight; i++) {
                    if (i > 0) {
                        sb.append(StringUtils.SPACE);
                        sb.append("|");
                        sb.append(StringUtils.SPACE);
                    }
                    sb.append(originalTable.getSource().getCell(c0, i).getStringValue());
                }
                message.append(StringUtils.SPACE);
                message.append("There is no match for column '").append(sb).append("'.");
            }
            throw new DTUnmatchedCompilationException(message.toString());
        }

        if (!fits.isEmpty()) {
            if (fits.size() > 1) {
                var mCount = 0;
                OpenLMessage warnMessage = null;
                if (isAmbiguousFits(fits, DTHeader::isCondition)) {
                    warnMessage = OpenLMessagesUtils.newWarnMessage(
                            "Ambiguous matching of column titles to DT conditions. Use more appropriate titles for condition columns.",
                            tableSyntaxNode);
                    mCount++;
                }
                if (isAmbiguousFits(fits, DTHeader::isAction)) {
                    warnMessage = OpenLMessagesUtils.newWarnMessage(
                            "Ambiguous matching of column titles to DT action columns. Use more appropriate titles for action columns.",
                            tableSyntaxNode);
                    mCount++;
                }
                if (isAmbiguousFits(fits, DTHeader::isReturn)) {
                    warnMessage = OpenLMessagesUtils.newWarnMessage(
                            "Ambiguous matching of column titles to DT return columns. Use more appropriate titles for return columns.",
                            tableSyntaxNode);
                    mCount++;
                }
                if (mCount == 1) {
                    bindingContext.addMessage(warnMessage);
                } else if (mCount > 0) {
                    bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(
                            "Ambiguous matching of column titles to DT columns. Use more appropriate titles.",
                            tableSyntaxNode));
                }
            }
            // Select with min returns/actions/conditions
            fits = filterHeadersByMin(fits, e -> e.stream().filter(DTHeader::isReturn).count(), all);
            fits = filterHeadersByMin(fits, e -> e.stream().filter(DTHeader::isAction).count(), all);
            fits = filterHeadersByMin(fits, e -> e.stream().filter(DTHeader::isCondition).count(), all);
            if (fits.stream().anyMatch(e -> e instanceof FuzzyDTHeader)) {
                fits = filterHeadersByMax(fits,
                        e -> e.stream()
                                .filter(e1 -> e1 instanceof FuzzyDTHeader)
                                .mapToLong(
                                        e1 -> (long) ((FuzzyDTHeader) e1).getFuzzyResult().getAcceptableSimilarity() * 1000000L)
                                .sum() / e.stream().filter(e1 -> e1 instanceof FuzzyDTHeader).count(),
                        all);
            }
            return fits.getFirst();
        }

        return Collections.emptyList();
    }

    private static long countReturns(List<DTHeader> dtHeaders) {
        var countReturns = 0;
        var fuzzyReturn = false;
        for (DTHeader dtHeader : dtHeaders) {
            if (dtHeader.isReturn()) {
                // If fieldsChain == null then it is a return for whole return type. It is not a part of fuzzy columns
                // returns.
                if (dtHeader instanceof FuzzyDTHeader header && header.getFieldsChain() != null) {
                    if (!fuzzyReturn) {
                        countReturns++;
                    }
                    fuzzyReturn = true;
                } else {
                    fuzzyReturn = false;
                    countReturns++;
                }
            }
        }
        return countReturns;
    }

    private static List<List<DTHeader>> filterBasedOnDeclaredDtHeaders(List<List<DTHeader>> fits) {
        var ret = new ArrayList<List<DTHeader>>();
        for (List<DTHeader> fit : fits) {
            var externalParameters = new HashSet<String>();
            var parameters = new HashMap<String, Integer>();
            for (DTHeader dtHeader : fit) {
                if (dtHeader instanceof DeclaredDTHeader declaredDTHeader) {
                    externalParameters.addAll(declaredDTHeader.getMatchedDefinition()
                            .getDtColumnsDefinition()
                            .getExternalParameters()
                            .stream()
                            .map(DecisionTableHelper::toLowerCase)
                            .collect(Collectors.toSet()));
                    for (IParameterDeclaration parameter : declaredDTHeader.getMatchedDefinition()
                            .getDtColumnsDefinition()
                            .getParameters()) {
                        if (parameter != null && parameter.getName() != null) {
                            parameters.merge(toLowerCase(parameter.getName()), 1, Integer::sum);
                        }
                    }
                }
            }
            var f = true;
            for (String externalParameter : externalParameters) {
                if (!parameters.containsKey(toLowerCase(externalParameter))) {
                    f = false;
                    break;
                }
            }
            if (f) {
                ret.add(fit);
            }
        }
        return ret.isEmpty() ? fits : ret;
    }

    private enum WithVerticalTitles {
        NO,
        SLASH_IN_TITLE,
        EMPTY_COLUMN,
        MERGED_COLUMN
    }

    public static Pair<Integer, WithVerticalTitles> getFirstColumnForHCondition(ILogicalTable originalTable,
                                                                                int numberOfHConditions,
                                                                                int firstColumnHeight,
                                                                                boolean isSmartLookup) {
        var w = originalTable.getSource().getWidth();
        var column = 0;
        var ret = -1;
        while (column < w) {
            var rowsCount = calculateRowsCount(originalTable, column, firstColumnHeight);
            if (rowsCount != numberOfHConditions) {
                ret = -1;
            }
            if (rowsCount > 1 && rowsCount == numberOfHConditions && ret < 0) {
                ret = column;
            }
            column = column + originalTable.getSource().getCell(column, 0).getWidth();
        }

        if (isSmartLookup && ret < w - 1) {
            var begin = Math.max(ret, 0);
            int end = begin > 0 ? begin + 1 : originalTable.getSource().getWidth();
            var i = begin;
            while (i < end) {
                var value = originalTable.getSource().getCell(i, firstColumnHeight - 1).getStringValue();
                if (StringUtils.isNotBlank(value) && value.contains(HORIZONTAL_VERTICAL_CONDITIONS_SPLITTER)) {
                    var part1 = value.substring(0, value.indexOf(HORIZONTAL_VERTICAL_CONDITIONS_SPLITTER));
                    var part2 = value.substring(value.indexOf(HORIZONTAL_VERTICAL_CONDITIONS_SPLITTER) + 1);
                    if (StringUtils.isNotBlank(part1) && StringUtils.isNotBlank(part2)) {
                        return Pair.of(i + originalTable.getSource().getCell(i, 0).getWidth(),
                                WithVerticalTitles.SLASH_IN_TITLE);
                    } else if (StringUtils.isBlank(part1) && StringUtils.isNotBlank(part2)) {
                        return Pair.of(i + originalTable.getSource().getCell(i, 0).getWidth(),
                                WithVerticalTitles.EMPTY_COLUMN);
                    } else if (i > 0 && StringUtils.isBlank(part1) && StringUtils.isNotBlank(part2)) {
                        var w1 = originalTable.getSource().getCell(i - 1, firstColumnHeight).getWidth();
                        var w2 = originalTable.getSource().getCell(i - 1, firstColumnHeight - 1).getWidth();
                        var w3 = originalTable.getSource().getCell(i, firstColumnHeight - 1).getWidth();
                        if (w1 == w2 + w3) {
                            return Pair.of(i + originalTable.getSource().getCell(i, 0).getWidth(),
                                    WithVerticalTitles.MERGED_COLUMN);
                        }
                    }
                }
                i = i + originalTable.getSource().getCell(i, 0).getWidth();
            }
        }

        return Pair.of(ret, WithVerticalTitles.NO);
    }

    private static boolean columnWithFormulas(ILogicalTable originalTable, int firstColumnHeight, int column) {
        var h = firstColumnHeight;
        var height = originalTable.getSource().getHeight();
        var c = 0;
        var t = 0;
        while (h < height) {
            var cell = originalTable.getSource().getCell(column, h);
            var s = cell.getStringValue();
            if (!StringUtils.isEmpty(s != null ? s.trim() : null) && !RuleRowHelper.isFormula(s)) {
                c++;
            }
            t++;
            h = h + cell.getHeight();
        }
        return c <= t / 2 + t % 2;
    }

    private static boolean conflictsWithStrongDtHeader(List<DTHeader> strongDtHeaders,
                                                       WithVerticalTitles withVerticalTitles,
                                                       int firstColumnForHCondition,
                                                       int column,
                                                       int width) {
        if (!WithVerticalTitles.NO.equals(withVerticalTitles) && column + width == firstColumnForHCondition) {
            return false;
        }
        for (DTHeader dtHeader : strongDtHeaders) {
            if (intersects(dtHeader.getColumn(),
                    dtHeader.getColumn() + dtHeader.getWidth() - 1,
                    column,
                    column + width - 1)) {
                return true;
            }
        }
        return false;
    }

    private static List<DTHeader> getDTHeaders(TableSyntaxNode tableSyntaxNode,
                                               DecisionTable decisionTable,
                                               ILogicalTable originalTable,
                                               FuzzyContext fuzzyContext,
                                               NumberOfColumnsUnderTitleCounter numberOfColumnsUnderTitleCounter,
                                               int numberOfHConditions,
                                               int firstColumnHeight,
                                               int firstColumnForHCondition,
                                               WithVerticalTitles withVerticalTitles,
                                               IBindingContext bindingContext) throws OpenLCompilationException {
        var isSmart = isSmart(tableSyntaxNode);

        var numberOfParameters = decisionTable.getSignature().getNumberOfParameters();
        var twoColumnsForReturn = isTwoColumnsForReturn(tableSyntaxNode, decisionTable);

        final var xlsDefinitions = ((XlsModuleOpenClass) decisionTable.getDeclaringClass())
                .getXlsDefinitions();

        var lastColumn = originalTable.getSource().getWidth();
        if (numberOfHConditions > 0 && firstColumnForHCondition > 0) {
            lastColumn = firstColumnForHCondition;
        }

        String returnTokenString = fuzzyContext != null && fuzzyContext.isFuzzySupportsForReturnType() ? OpenLFuzzyUtils
                .toTokenString(fuzzyContext.getFuzzyReturnType().getName()) : null;
        var dtHeaders = new ArrayList<DTHeader>();
        var i = 0;
        var column = 0;
        if (isSmart) {
            while (column < lastColumn) {
                var w = originalTable.getSource().getCell(column, 0).getWidth();
                matchWithDtColumnsDefinitions(decisionTable,
                        originalTable,
                        column,
                        xlsDefinitions,
                        numberOfColumnsUnderTitleCounter,
                        dtHeaders,
                        firstColumnForHCondition,
                        withVerticalTitles,
                        firstColumnHeight,
                        numberOfHConditions,
                        bindingContext);
                column = column + w;
                i++;
            }
        }
        var strongDtHeaders = findStrongDtHeaders(originalTable, dtHeaders);
        i = 0;
        column = 0;
        SimpleReturnDTHeader lastSimpleReturnDTHeader = null;
        while (column < lastColumn) {
            var w = originalTable.getSource().getCell(column, 0).getWidth();
            var row = 0;
            if (!conflictsWithStrongDtHeader(strongDtHeaders,
                    withVerticalTitles,
                    firstColumnForHCondition,
                    column,
                    w)) {
                if (isSmart) {
                    var fuzzyHeaders = matchWithFuzzySearch(decisionTable,
                            originalTable,
                            fuzzyContext,
                            numberOfColumnsUnderTitleCounter,
                            numberOfHConditions,
                            column,
                            lastColumn,
                            dtHeaders,
                            firstColumnHeight,
                            firstColumnForHCondition,
                            withVerticalTitles,
                            false);
                    if (numberOfHConditions == 0) {
                        String titleForColumn = getTitleForColumn(originalTable, firstColumnHeight, column);
                        var width = originalTable.getSource().getCell(column, 0).getWidth();
                        lastSimpleReturnDTHeader = new SimpleReturnDTHeader(null, titleForColumn, column, row, width);
                        if (fuzzyContext != null && fuzzyContext.isFuzzySupportsForReturnType()) {
                            var returnTypeFuzzyExtractResult = OpenLFuzzyUtils
                                    .fuzzyExtract(titleForColumn, new Token[]{new Token(returnTokenString, -1)}, true);
                            if (!returnTypeFuzzyExtractResult.isEmpty()) {
                                dtHeaders.add(new FuzzyDTHeader(column,
                                        null,
                                        titleForColumn,
                                        null,
                                        column,
                                        column,
                                        row,
                                        width,
                                        width,
                                        returnTypeFuzzyExtractResult.getFirst(),
                                        true,
                                        false));
                            } else if (fuzzyHeaders.stream()
                                    .noneMatch(DTHeader::isReturn) && numberOfColumnsUnderTitleCounter
                                    .get(column) == 1 && (column + w >= lastColumn || columnWithFormulas(originalTable,
                                    firstColumnHeight,
                                    column))) {
                                dtHeaders.add(lastSimpleReturnDTHeader);
                            }
                        } else {
                            dtHeaders.add(lastSimpleReturnDTHeader);
                        }
                    }
                } else {
                    if (numberOfHConditions == 0 && i >= numberOfParameters) {
                        matchWithFuzzySearch(decisionTable,
                                originalTable,
                                fuzzyContext,
                                numberOfColumnsUnderTitleCounter,
                                numberOfHConditions,
                                column,
                                lastColumn,
                                dtHeaders,
                                firstColumnHeight,
                                firstColumnForHCondition,
                                withVerticalTitles,
                                true);
                    }
                    if (i < numberOfParameters - numberOfHConditions) {
                        var simpleDTHeader = new SimpleDTHeader(i,
                                decisionTable.getSignature().getParameterName(i),
                                null,
                                column,
                                row,
                                w);
                        dtHeaders.add(simpleDTHeader);
                    } else if (numberOfHConditions == 0) {
                        var simpleReturnDTHeader = new SimpleReturnDTHeader(null,
                                null,
                                column,
                                row,
                                w);
                        dtHeaders.add(simpleReturnDTHeader);
                    }
                }
            }
            column = column + w;
            i++;
        }

        if (lastSimpleReturnDTHeader != null && dtHeaders.stream().noneMatch(DTHeader::isReturn)) {
            dtHeaders.add(lastSimpleReturnDTHeader);
        }

        var fit = fitDtHeaders(tableSyntaxNode,
                decisionTable,
                originalTable,
                dtHeaders,
                lastColumn,
                numberOfHConditions,
                twoColumnsForReturn,
                firstColumnHeight,
                bindingContext);

        if (numberOfHConditions > 0) {
            var maxColumnMatched = fit.stream()
                    .filter(e -> e.isCondition() && !e.isHCondition() || e.isAction())
                    .mapToInt(e -> e.getColumn() + e.getWidth())
                    .max()
                    .orElse(0);
            column = originalTable.getSource().getWidth() - 1;
            while (column > maxColumnMatched && calculateRowsCount(originalTable,
                    column - 1,
                    firstColumnHeight) == numberOfHConditions) {
                column--;
            }

            var fitHCond = new ArrayList<DTHeader>(fit);
            for (var c = maxColumnMatched; c < column; c++) {
                var num = numberOfColumnsUnderTitleCounter.get(c);
                var col1 = c;
                for (var j = 0; j < num; j++) {
                    var width = numberOfColumnsUnderTitleCounter.getWidth(c, j);
                    fitHCond.add(new UnmatchedDtHeader(StringUtils.EMPTY, col1, 0, width, false));
                    col1 = col1 + width;
                }
            }

            boolean[] parameterIsUsed = new boolean[numberOfParameters];
            Arrays.fill(parameterIsUsed, false);
            for (DTHeader dtHeader : fit) {
                for (int paramIndex : dtHeader.getMethodParameterIndexes()) {
                    parameterIsUsed[paramIndex] = true;
                }
            }
            var freeParameters = 0;
            for (boolean f : parameterIsUsed) {
                if (!f) {
                    freeParameters++;
                }
            }

            var hConditionsMatched = fit.stream()
                    .filter(e -> e.isHCondition() && !(e instanceof UnmatchedDtHeader))
                    .count();
            if (freeParameters + hConditionsMatched < numberOfHConditions) {
                SyntaxNodeException error = SyntaxNodeExceptionUtils
                        .createError("No input parameter found for horizontal condition.", tableSyntaxNode);
                bindingContext.addError(error);
                return fitHCond;
            }
            var j = 0;
            var w = 0;
            var c = 0;
            var len = fitHCond.size();
            while (w < numberOfParameters && j < numberOfHConditions - hConditionsMatched) {
                if (!parameterIsUsed[w]) {
                    while (c < len) {
                        var dth = fitHCond.get(c);
                        if (dth instanceof UnmatchedDtHeader && dth.isHCondition()) {
                            break;
                        }
                        c++;
                    }
                    if (c < len) {
                        fitHCond.set(c,
                                new SimpleDTHeader(w, decisionTable.getSignature().getParameterName(w), column + j, j));
                        c++;
                    } else {
                        fitHCond.add(
                                new SimpleDTHeader(w, decisionTable.getSignature().getParameterName(w), column + j, j));
                    }
                    j++;
                }
                w++;
            }
            return Collections.unmodifiableList(fitHCond);
        } else {
            return fit;
        }

    }

    private static String getTitleForColumn(ILogicalTable originalTable, int firstColumnHeight, int column) {
        var sb = new StringBuilder();
        for (var j = 0; j < firstColumnHeight; j++) {
            if (j > 0) {
                sb.append(StringUtils.SPACE);
            }
            sb.append(originalTable.getSource().getCell(column, 0).getStringValue());
        }
        return sb.toString();
    }

    public static int getNumberOfHConditions(ILogicalTable originalTable) {
        return calculateRowsCount(originalTable,
                originalTable.getSource().getWidth() - 1,
                originalTable.getSource().getCell(0, 0).getHeight());
    }

    private static boolean isTwoColumnsForReturn(TableSyntaxNode tableSyntaxNode, DecisionTable decisionTable) {
        return isCollect(tableSyntaxNode) && ClassUtils.isAssignable(decisionTable.getType().getInstanceClass(),
                Map.class);
    }

    private static void matchWithDtColumnsDefinitions(DecisionTable decisionTable,
                                                      ILogicalTable originalTable,
                                                      int column,
                                                      XlsDefinitions definitions,
                                                      NumberOfColumnsUnderTitleCounter numberOfColumnsUnderTitleCounter,
                                                      List<DTHeader> dtHeaders,
                                                      int firstColumnForHCondition,
                                                      WithVerticalTitles withVerticalTitles,
                                                      int firstColumnHeight,
                                                      int numberOfHConditions,
                                                      IBindingContext bindingContext) {
        var parseAsHorizontalVerticalTitle = WithVerticalTitles.SLASH_IN_TITLE
                .equals(withVerticalTitles) && column + originalTable.getSource()
                .getCell(column, 0)
                .getWidth() == firstColumnForHCondition;
        var w0 = column + originalTable.getSource().getCell(column, 0).getWidth();
        var skipNextColumn = w0 + originalTable.getSource()
                .getCell(w0, 0)
                .getWidth() == firstColumnForHCondition && (WithVerticalTitles.EMPTY_COLUMN
                .equals(withVerticalTitles) || WithVerticalTitles.MERGED_COLUMN.equals(withVerticalTitles));
        if (parseAsHorizontalVerticalTitle || originalTable.getSource()
                .getCell(column, 0)
                .getHeight() == firstColumnHeight) {
            for (DTColumnsDefinition definition : definitions.getDtColumnsDefinitions()) {
                var titles = new HashSet<String>(definition.getTitles());
                var extractedTitle = extractTokenizedVerticalTitleString(originalTable,
                        column,
                        firstColumnHeight,
                        parseAsHorizontalVerticalTitle);
                Triple<String, String, Integer> lastExtractedTitle = extractedTitle;
                var i = 0;
                var x = column;
                IParameterDeclaration[][] columnParameters = null;
                var numberOfColumnsUnderTitle = numberOfColumnsUnderTitleCounter.get(x);
                var f1 = isMatchedByUnderColumns(definition.getParameters(extractedTitle.getLeft()),
                        numberOfColumnsUnderTitle);
                var f2 = !Objects.equals(extractedTitle.getLeft(),
                        extractedTitle.getMiddle()) && isMatchedByUnderColumns(
                        definition.getParameters(extractedTitle.getMiddle()),
                        numberOfColumnsUnderTitle);
                var g = false;
                while (!titles
                        .isEmpty() && ((numberOfHConditions > 0 && x < firstColumnForHCondition || x < originalTable
                        .getSource()
                        .getWidth()) && (f1 && titles.contains(
                        extractedTitle.getLeft()) || f2 && titles.contains(extractedTitle.getMiddle())))) {
                    g = false;
                    if (f1) {
                        titles.remove(extractedTitle.getLeft());
                    } else {
                        titles.remove(extractedTitle.getMiddle());
                    }
                    for (String s : definition.getTitles()) {
                        if (f1 && s.equals(extractedTitle.getLeft())) {
                            g = true;
                            if (columnParameters == null) {
                                columnParameters = new IParameterDeclaration[definition.getNumberOfTitles()][];
                            }
                            columnParameters[i] = definition.getParameters(extractedTitle.getLeft())
                                    .toArray(IParameterDeclaration.EMPTY);
                            break;
                        }
                        if (f2 && s.equals(extractedTitle.getMiddle())) {
                            if (columnParameters == null) {
                                columnParameters = new IParameterDeclaration[definition.getNumberOfTitles()][];
                            }
                            columnParameters[i] = definition.getParameters(extractedTitle.getMiddle())
                                    .toArray(IParameterDeclaration.EMPTY);
                            break;
                        }
                    }
                    i = i + 1;
                    var w = originalTable.getSource().getCell(x, 0).getWidth();
                    x = x + w;
                    lastExtractedTitle = extractedTitle;
                    extractedTitle = extractTokenizedVerticalTitleString(originalTable,
                            x,
                            firstColumnHeight,
                            parseAsHorizontalVerticalTitle);
                    parseAsHorizontalVerticalTitle = WithVerticalTitles.SLASH_IN_TITLE
                            .equals(withVerticalTitles) && column + originalTable.getSource()
                            .getCell(column, 0)
                            .getWidth() == firstColumnForHCondition;
                    numberOfColumnsUnderTitle = numberOfColumnsUnderTitleCounter.get(x);
                    f1 = isMatchedByUnderColumns(definition.getParameters(extractedTitle.getLeft()),
                            numberOfColumnsUnderTitle);
                    f2 = !Objects.equals(extractedTitle.getLeft(),
                            extractedTitle.getMiddle()) && isMatchedByUnderColumns(
                            definition.getParameters(extractedTitle.getMiddle()),
                            numberOfColumnsUnderTitle);
                }
                if (titles.isEmpty()) {
                    MatchedDefinition matchedDefinition = matchByDTColumnDefinition(decisionTable,
                            definition,
                            numberOfHConditions,
                            bindingContext);
                    if (matchedDefinition != null) {
                        var dtHeader = new DeclaredDTHeader(
                                matchedDefinition.getUsedMethodParameterIndexes(),
                                definition,
                                columnParameters,
                                column,
                                lastExtractedTitle.getRight(),
                                x - column + (skipNextColumn ? originalTable.getSource().getCell(x, 0).getWidth() : 0),
                                x - column,
                                matchedDefinition,
                                false,
                                g && parseAsHorizontalVerticalTitle);
                        dtHeaders.add(dtHeader);
                    }
                }
            }
        }
        if (!WithVerticalTitles.NO.equals(withVerticalTitles) && column + originalTable.getSource()
                .getCell(column, 0)
                .getWidth() == firstColumnForHCondition) {
            for (DTColumnsDefinition definition : definitions.getDtColumnsDefinitions()) {
                if (definition.getNumberOfTitles() == 1) {
                    var definitionTitle = definition.getTitles().iterator().next();
                    var h = 0;
                    var x = 0;
                    while (h < firstColumnHeight) {
                        var h0 = originalTable.getSource().getCell(column, h).getHeight();
                        var title = originalTable.getSource().getCell(column, h).getStringValue();
                        if (h + h0 >= firstColumnHeight && WithVerticalTitles.SLASH_IN_TITLE
                                .equals(withVerticalTitles)) {
                            title = title.substring(title.indexOf(HORIZONTAL_VERTICAL_CONDITIONS_SPLITTER) + 1).trim();
                        }
                        if (x < numberOfHConditions) {
                            title = OpenLFuzzyUtils.toTokenString(title);
                            if (Objects.equals(title, definitionTitle)) {
                                MatchedDefinition matchedDefinition = matchByDTColumnDefinition(decisionTable,
                                        definition,
                                        numberOfHConditions,
                                        bindingContext);
                                if (matchedDefinition != null) {
                                    IParameterDeclaration[][] columnParameters = new IParameterDeclaration[1][];
                                    columnParameters[0] = definition.getParameters(title)
                                            .toArray(IParameterDeclaration.EMPTY);
                                    var vDtHeader = new DeclaredDTHeader(
                                            matchedDefinition.getUsedMethodParameterIndexes(),
                                            definition,
                                            columnParameters,
                                            column + originalTable.getSource().getCell(column, 0).getWidth() + x,
                                            h,
                                            1,
                                            1,
                                            matchedDefinition,
                                            true,
                                            false);
                                    dtHeaders.add(vDtHeader);
                                    break;
                                }
                            }
                        }
                        h = h + h0;
                        x++;
                    }
                }
            }
        }
    }

    private static Triple<String, String, Integer> extractTokenizedVerticalTitleString(ILogicalTable originalTable,
                                                                                       int column,
                                                                                       int firstColumnHeight,
                                                                                       boolean parseAsHorizontalVerticalTitle) {
        if (parseAsHorizontalVerticalTitle) {
            var title = originalTable.getSource().getCell(column, firstColumnHeight - 1).getStringValue();
            if (StringUtils.isNotBlank(title) && title.contains(HORIZONTAL_VERTICAL_CONDITIONS_SPLITTER)) {
                var cutTitle = title.substring(0, title.indexOf(HORIZONTAL_VERTICAL_CONDITIONS_SPLITTER)).trim();
                return Triple.of(OpenLFuzzyUtils.toTokenString(cutTitle),
                        OpenLFuzzyUtils.toTokenString(title),
                        firstColumnHeight - 1);
            }
        }
        var title = originalTable.getSource().getCell(column, 0).getStringValue();
        String tokenizedTitle = OpenLFuzzyUtils.toTokenString(title);
        return Triple.of(tokenizedTitle, tokenizedTitle, 0);
    }

    private static boolean isMatchedByUnderColumns(List<IParameterDeclaration> parameters,
                                                   int numberOfColumnsUnderTitle) {
        var isAnyArrayTypePresented = parameters.stream()
                .anyMatch(e -> e != null && e.getType() != null && e.getType().isArray());
        return isAnyArrayTypePresented ? numberOfColumnsUnderTitle >= parameters.size()
                : numberOfColumnsUnderTitle == parameters.size();
    }

    private static boolean parsableAs(String[] values, Class<?> componentType, IBindingContext bindingContext) {
        try {
            for (String value : values) {
                String2DataConvertorFactory.parse(componentType, value, bindingContext);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean parsableAs(String src, Class<?> clazz, IBindingContext bindingContext) {
        try {
            String2DataConvertorFactory.parse(clazz, src, bindingContext);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private static int calculateRowsCount(ILogicalTable originalTable, int column, int height) {
        var h = 0;
        var k = 0;
        while (h < height && h < originalTable.getSource().getHeight()) {
            h = h + originalTable.getSource().getCell(column, h).getHeight();
            k++;
        }
        return k;
    }

    private static Triple<String[], IOpenClass, String> buildTripleForTypeForConditionColumn(Class<?> rangeClass,
                                                                                             DTHeader condition,
                                                                                             boolean isArray,
                                                                                             boolean isMoreThanOneColumnIsUsed) {
        int type;
        if (isArray) {
            type = isMoreThanOneColumnIsUsed ? 2 : 1;
        } else {
            type = isMoreThanOneColumnIsUsed ? 1 : 0;
        }
        if (type == 0) {
            return Triple.of(new String[]{rangeClass.getSimpleName()},
                    JavaOpenClass.getOpenClass(rangeClass),
                    condition.getStatement());
        } else if (type == 1) {
            final var paramName = "_" + condition.getStatement().replaceAll("\\.", "_");
            return Triple.of(new String[]{rangeClass.getSimpleName() + "[]", paramName},
                    AOpenClass.getArrayType(JavaOpenClass.getOpenClass(rangeClass), 1),
                    "contains(" + paramName + ", " + condition.statement + ")");
        } else {
            final var paramName = "_" + condition.getStatement().replaceAll("\\.", "_");
            return Triple.of(new String[]{rangeClass.getSimpleName() + "[][]", paramName},
                    AOpenClass.getArrayType(JavaOpenClass.getOpenClass(rangeClass), 2),
                    "contains(" + paramName + ", " + condition.statement + ")");
        }
    }

    private static class CellValue {
        @Getter
        String value;
        @Getter
        ICell cell;

        public CellValue(ICell cell) {
            this.value = cell.getStringValue();
            this.cell = cell;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            var cellValue = (CellValue) o;
            return Objects.equals(value, cellValue.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    /**
     * Check type of condition values. If condition values are complex(Range, Array) then types of complex values will
     * be returned
     */
    @SuppressWarnings("unchecked")
    private static Triple<String[], IOpenClass, String> getTypeForConditionColumn(DecisionTable decisionTable,
                                                                                  ILogicalTable originalTable,
                                                                                  DTHeader condition,
                                                                                  int indexOfHCondition,
                                                                                  int firstColumnForHConditionsOrReturns,
                                                                                  int firstColumnHeight,
                                                                                  int numberOfColumnsUnderTitle,
                                                                                  XlsModuleOpenClass module,
                                                                                  IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache,
                                                                                  IBindingContext bindingContext) {
        var column = condition.getColumn();

        IOpenClass type = getTypeForCondition(decisionTable, condition);

        IGridTable decisionValues;
        int width;
        int skip;
        int numberOfColumnsForCondition;
        if (condition.isHCondition()) {
            decisionValues = originalTable.getSource().getRow(indexOfHCondition - 1);
            width = decisionValues.getWidth();
            skip = firstColumnForHConditionsOrReturns;
            numberOfColumnsForCondition = 1;
        } else {
            decisionValues = originalTable.getSource().getColumns(column, column + numberOfColumnsUnderTitle - 1);
            width = decisionValues.getHeight();
            skip = firstColumnHeight;
            numberOfColumnsForCondition = numberOfColumnsUnderTitle;
        }

        var isAllParsableAsRangeFlag = true;
        var isAllLikelyNotRangeFlag = true;
        var isAllElementsLikelyNotRangeFlag = true;
        var isAllParsableAsSingleFlag = true;
        var isAllParsableAsDomainFlag = true;
        var isAllParsableAsDomainArrayFlag = true;
        var isAllParsableAsArrayFlag = true;
        var arraySeparatorFoundFlag = false;

        var isNotParsableAsSingleRangeButParsableAsRangesArrayFlag = false;
        var zeroStartedNumbersFoundFlag = false;

        var isIntType = INT_TYPES.contains(type.getInstanceClass());
        var isDoubleType = DOUBLE_TYPES.contains(type.getInstanceClass());
        var isCharType = CHAR_TYPES.contains(type.getInstanceClass());
        var isDateType = DATE_TYPES.contains(type.getInstanceClass());
        var isStringType = STRING_TYPES.contains(type.getInstanceClass());
        var isRangeType = RANGE_TYPES.contains(type.getInstanceClass());

        var canMadeDecisionAboutSingle = true;

        boolean[][] h = new boolean[width][numberOfColumnsForCondition];
        for (var i = 0; i < width; i++) {
            Arrays.fill(h[i], true);
        }

        var isMoreThanOneColumnIsUsed = numberOfColumnsForCondition > 1;

        var valuesMap = new HashMap<Integer, LinkedHashSet<CellValue>>();
        for (var valueNum = skip; valueNum < width; valueNum++) {
            IGridTable cellValues = condition.isHCondition() ? decisionValues.getColumn(valueNum)
                    : decisionValues.getRow(valueNum);
            Set<CellValue> values = valuesMap.computeIfAbsent(valueNum, e -> new LinkedHashSet<>());
            for (var cellNum = 0; cellNum < numberOfColumnsForCondition; cellNum++) {
                var cell = cellValues.getCell(0, cellNum);
                var value = cellValues.getCell(0, cellNum).getStringValue();
                if (value == null || StringUtils.isEmpty(value)) {
                    values.add(null);
                    h[valueNum][cellNum] = false;
                } else {
                    values.add(new CellValue(cell));
                }
            }
            var cellNum = -1;
            for (CellValue cellValue : values) {
                cellNum++;
                if (cellValue == null) {
                    continue;
                }
                var value = cellValue.getValue();

                if (RuleRowHelper.isFormula(value) && !isRangeType) {
                    try {
                        bindingContext.pushErrors();
                        bindingContext.pushMessages();
                        var expressionCellSourceCodeModule = new StringSourceCodeModule(
                                value.substring(value.indexOf("=")).trim(),
                                null);
                        CompositeMethod compositeMethod = OpenLManager.makeMethodWithUnknownType(
                                bindingContext.getOpenL(),
                                expressionCellSourceCodeModule,
                                RandomStringUtils.random(16, true, false),
                                decisionTable.getSignature(),
                                decisionTable.getDeclaringClass(),
                                bindingContext);
                        var cellType = compositeMethod.getType();
                        canMadeDecisionAboutSingle = canMadeDecisionAboutSingle && type.equals(cellType);
                        if (cellType.isArray() && RANGE_TYPES
                                .contains(cellType.getComponentClass().getInstanceClass())) {
                            isAllParsableAsArrayFlag = false;
                            isNotParsableAsSingleRangeButParsableAsRangesArrayFlag = true;
                            isAllLikelyNotRangeFlag = false;
                            isAllElementsLikelyNotRangeFlag = false;
                        }
                        if (RANGE_TYPES.contains(cellType.getInstanceClass())) {
                            isAllParsableAsArrayFlag = false;
                            isAllLikelyNotRangeFlag = false;
                            isAllElementsLikelyNotRangeFlag = false;
                        }
                        if (cellType.isArray()) {
                            isAllParsableAsSingleFlag = false;
                            isNotParsableAsSingleRangeButParsableAsRangesArrayFlag = true;
                        }

                    } finally {
                        bindingContext.popMessages();
                        bindingContext.popErrors();
                    }
                    h[valueNum][cellNum] = false;
                    continue;
                }

                ConstantOpenField constantOpenField = RuleRowHelper.findConstantField(bindingContext, value);
                if (constantOpenField != null) {
                    if (constantOpenField.getType().isArray() && RANGE_TYPES
                            .contains(constantOpenField.getType().getComponentClass().getInstanceClass())) {
                        isAllParsableAsArrayFlag = false;
                        isNotParsableAsSingleRangeButParsableAsRangesArrayFlag = true;
                        isAllLikelyNotRangeFlag = false;
                        isAllElementsLikelyNotRangeFlag = false;
                    }
                    if (RANGE_TYPES.contains(constantOpenField.getType().getInstanceClass())) {
                        isAllParsableAsArrayFlag = false;
                        isAllLikelyNotRangeFlag = false;
                        isAllElementsLikelyNotRangeFlag = false;
                    }
                    if (constantOpenField.getType().isArray()) {
                        isAllParsableAsSingleFlag = false;
                        isNotParsableAsSingleRangeButParsableAsRangesArrayFlag = true;
                    }
                    h[valueNum][cellNum] = false;
                    canMadeDecisionAboutSingle = canMadeDecisionAboutSingle && type.equals(constantOpenField.getType());
                    continue;
                }
                if (!arraySeparatorFoundFlag && ArraySplitter.isArray(value)) {
                    arraySeparatorFoundFlag = true;
                }
                try {
                    if ((isIntType || isDoubleType || isCharType) && isAllParsableAsSingleFlag && !parsableAs(value,
                            type.getInstanceClass(),
                            bindingContext)) {
                        isAllParsableAsSingleFlag = false;
                    } else if (isStringType) {
                        if (isAllParsableAsDomainFlag && (type
                                .getDomain() == null || !((IDomain<String>) type.getDomain()).selectObject(value))) {
                            isAllParsableAsDomainFlag = false;
                        }
                        if (isAllParsableAsDomainArrayFlag) {
                            if (type.getDomain() == null) {
                                isAllParsableAsDomainArrayFlag = false;
                            } else {
                                for (String s : ArraySplitter.split(value)) {
                                    if (!((IDomain<String>) type.getDomain()).selectObject(s)) {
                                        isAllParsableAsDomainArrayFlag = false;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (canMadeDecisionAboutSingle) {
            if ((isIntType || isDoubleType || isCharType) && isAllParsableAsSingleFlag || isStringType && isAllParsableAsDomainFlag) {
                return buildTripleForConditionColumnWithSimpleType(condition,
                        type,
                        false,
                        isMoreThanOneColumnIsUsed,
                        module,
                        cache);
            }

            if (isStringType && isAllParsableAsDomainArrayFlag) {
                return buildTripleForConditionColumnWithSimpleType(condition,
                        type,
                        true,
                        isMoreThanOneColumnIsUsed,
                        module,
                        cache);
            }
        }

        for (var valueNum = skip; valueNum < width; valueNum++) {
            Set<CellValue> values = valuesMap.get(valueNum);
            var cellNum = -1;
            for (CellValue cellValue : values) {
                cellNum++;
                if (cellValue == null || !h[valueNum][cellNum]) {
                    continue;
                }
                var value = cellValue.getValue();
                /* try to create range by values **/
                try {
                    if (isIntType) {
                        if (isAllParsableAsRangeFlag || !isNotParsableAsSingleRangeButParsableAsRangesArrayFlag) {
                            var arrs = ArraySplitter.split(value);
                            var f = parsableAs(arrs, IntRange.class, bindingContext);
                            var parsableAsSingleRange = parsableAs(value, IntRange.class, bindingContext);
                            if (!f && !parsableAsSingleRange) {
                                isAllParsableAsRangeFlag = false;
                            }
                            if (f && arrs.length > 1 && !parsableAsSingleRange) {
                                isNotParsableAsSingleRangeButParsableAsRangesArrayFlag = true;
                            }
                        }
                        if (isAllParsableAsArrayFlag) {
                            var arrs = ArraySplitter.split(value);
                            var g = parsableAs(arrs, type.getInstanceClass(), bindingContext);
                            if (g && !zeroStartedNumbersFoundFlag) { // If array element
                                // starts with 0 and
                                // can be range
                                // and
                                // array for all elements then use Range by default. But if
                                // no zero started elements then default String[]
                                zeroStartedNumbersFoundFlag = Arrays.stream(arrs)
                                        .anyMatch(e -> e != null && e.length() > 1 && e.startsWith("0"));
                            }
                            if (!g) {
                                isAllParsableAsArrayFlag = false;
                            }
                        }
                    } else if (isDoubleType) {
                        if (isAllParsableAsRangeFlag || !isNotParsableAsSingleRangeButParsableAsRangesArrayFlag) {
                            var arrs = ArraySplitter.split(value);
                            var f = parsableAs(arrs, DoubleRange.class, bindingContext);
                            var parsableAsSingleRange = parsableAs(value, DoubleRange.class, bindingContext);
                            if (!f && !parsableAsSingleRange) {
                                isAllParsableAsRangeFlag = false;
                            }
                            if (f && arrs.length > 1 && !parsableAsSingleRange) {
                                isNotParsableAsSingleRangeButParsableAsRangesArrayFlag = true;
                            }
                        }
                        if (isAllParsableAsArrayFlag) {
                            var arrs = ArraySplitter.split(value);
                            var g = parsableAs(arrs, type.getInstanceClass(), bindingContext);
                            if (g && !zeroStartedNumbersFoundFlag) {
                                zeroStartedNumbersFoundFlag = Arrays.stream(arrs)
                                        .anyMatch(e -> e != null && e.length() > 1 && e.startsWith("0"));
                            }
                            if (!g) {
                                isAllParsableAsArrayFlag = false;
                            }
                        }
                    } else if (isCharType) {
                        if (isAllParsableAsRangeFlag || !isNotParsableAsSingleRangeButParsableAsRangesArrayFlag) {
                            var arrs = ArraySplitter.split(value);
                            var f = parsableAs(arrs, CharRange.class, bindingContext);
                            var parsableAsSingleRange = parsableAs(value, CharRange.class, bindingContext);
                            if (!f && !parsableAsSingleRange) {
                                isAllParsableAsRangeFlag = false;
                            }
                            if (f && arrs.length > 1 && !parsableAsSingleRange) {
                                isNotParsableAsSingleRangeButParsableAsRangesArrayFlag = true;
                            }
                        }
                        if (isAllParsableAsArrayFlag) {
                            var arrs = ArraySplitter.split(value);
                            var g = parsableAs(arrs, type.getInstanceClass(), bindingContext);
                            if (!g) {
                                isAllParsableAsArrayFlag = false;
                            }
                        }
                    } else if (isDateType) {
                        var o = cellValue.getCell().getObjectValue();
                        if (o instanceof Date) {
                            continue;
                        }
                        if (o instanceof String && !parsableAs(value, type.getInstanceClass(), bindingContext)) {
                            isAllParsableAsSingleFlag = false;
                        }
                        String[] arrs = null;
                        if (isAllParsableAsRangeFlag || !isNotParsableAsSingleRangeButParsableAsRangesArrayFlag) {
                            arrs = ArraySplitter.split(value);
                            var f = parsableAs(arrs, DateRange.class, bindingContext);
                            var parsableAsSingleRange = parsableAs(value, DateRange.class, bindingContext);
                            if (isAllParsableAsRangeFlag && !f && !parsableAsSingleRange) {
                                isAllParsableAsRangeFlag = false;
                            }
                            if (f && arrs.length > 1 && !parsableAsSingleRange) {
                                isNotParsableAsSingleRangeButParsableAsRangesArrayFlag = true;
                            }
                        }
                        if (isAllLikelyNotRangeFlag && o instanceof String && DateRangeParser.getInstance()
                                .likelyRangeThanDate(value)) {
                            isAllLikelyNotRangeFlag = false;
                        }
                        if (isAllElementsLikelyNotRangeFlag) {
                            if (arrs == null) {
                                arrs = ArraySplitter.split(value);
                            }
                            for (String v : arrs) {
                                if (DateRangeParser.getInstance().likelyRangeThanDate(v)) {
                                    isAllElementsLikelyNotRangeFlag = false;
                                    break;
                                }
                            }
                        }
                        if (isAllParsableAsArrayFlag) {
                            arrs = ArraySplitter.split(value);
                            var g = parsableAs(arrs, type.getInstanceClass(), bindingContext);
                            if (!g) {
                                isAllParsableAsArrayFlag = false;
                            }
                        }
                    } else if (isStringType) {
                        String[] arrs = null;
                        if (isAllParsableAsRangeFlag || !isNotParsableAsSingleRangeButParsableAsRangesArrayFlag) {
                            arrs = ArraySplitter.split(value);
                            var f = parsableAs(arrs, StringRange.class, bindingContext);
                            if (isAllParsableAsRangeFlag && !f && !parsableAs(value,
                                    StringRange.class,
                                    bindingContext)) {
                                isAllParsableAsRangeFlag = false;
                            }
                            if (!isNotParsableAsSingleRangeButParsableAsRangesArrayFlag && f && arrs.length > 1) {
                                isNotParsableAsSingleRangeButParsableAsRangesArrayFlag = true;
                            }
                        }
                        if (isAllLikelyNotRangeFlag && StringRangeParser.getInstance().likelyRangeThanString(value)) {
                            isAllLikelyNotRangeFlag = false;
                        }
                        if (isAllElementsLikelyNotRangeFlag) {
                            if (arrs == null) {
                                arrs = ArraySplitter.split(value);
                            }
                            for (String v : arrs) {
                                if (StringRangeParser.getInstance().likelyRangeThanString(v)) {
                                    isAllElementsLikelyNotRangeFlag = false;
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (isDateType && isAllParsableAsRangeFlag && ((isNotParsableAsSingleRangeButParsableAsRangesArrayFlag ? !isAllElementsLikelyNotRangeFlag
                : !isAllLikelyNotRangeFlag) || !isAllParsableAsArrayFlag)) {
            return buildTripleForTypeForConditionColumn(DateRange.class,
                    condition,
                    isNotParsableAsSingleRangeButParsableAsRangesArrayFlag,
                    isMoreThanOneColumnIsUsed);
        } else if (isIntType && isAllParsableAsRangeFlag && (!isAllParsableAsArrayFlag || zeroStartedNumbersFoundFlag)) {
            return buildTripleForTypeForConditionColumn(IntRange.class,
                    condition,
                    isNotParsableAsSingleRangeButParsableAsRangesArrayFlag,
                    isMoreThanOneColumnIsUsed);
        } else if (isDoubleType && isAllParsableAsRangeFlag && (!isAllParsableAsArrayFlag || zeroStartedNumbersFoundFlag)) {
            return buildTripleForTypeForConditionColumn(DoubleRange.class,
                    condition,
                    isNotParsableAsSingleRangeButParsableAsRangesArrayFlag,
                    isMoreThanOneColumnIsUsed);
        } else if (isCharType && isAllParsableAsRangeFlag && !isAllParsableAsArrayFlag) {
            return buildTripleForTypeForConditionColumn(CharRange.class,
                    condition,
                    isNotParsableAsSingleRangeButParsableAsRangesArrayFlag,
                    isMoreThanOneColumnIsUsed);
        } else if (isSmart(decisionTable
                .getSyntaxNode()) && isStringType && !isAllParsableAsDomainFlag && isAllParsableAsRangeFlag && ((isNotParsableAsSingleRangeButParsableAsRangesArrayFlag ? !isAllElementsLikelyNotRangeFlag
                : !isAllLikelyNotRangeFlag) || !isAllParsableAsArrayFlag)) {
            return buildTripleForTypeForConditionColumn(StringRange.class,
                    condition,
                    isNotParsableAsSingleRangeButParsableAsRangesArrayFlag,
                    isMoreThanOneColumnIsUsed);
        }

        if (!type.isArray() && isAllParsableAsArrayFlag && (!isAllParsableAsSingleFlag || arraySeparatorFoundFlag)) {
            return buildTripleForConditionColumnWithSimpleType(condition,
                    type,
                    true,
                    isMoreThanOneColumnIsUsed,
                    module,
                    cache);
        }

        if (isAllParsableAsSingleFlag) {
            return buildTripleForConditionColumnWithSimpleType(condition,
                    type,
                    false,
                    isMoreThanOneColumnIsUsed,
                    module,
                    cache);
        }

        if (!type.isArray()) {
            if (isDateType) {
                return buildTripleForTypeForConditionColumn(DateRange.class,
                        condition,
                        true,
                        isMoreThanOneColumnIsUsed);
            } else if (isIntType) {
                return buildTripleForTypeForConditionColumn(IntRange.class, condition, true, isMoreThanOneColumnIsUsed);
            } else if (isDoubleType) {
                return buildTripleForTypeForConditionColumn(DoubleRange.class,
                        condition,
                        true,
                        isMoreThanOneColumnIsUsed);
            } else if (isCharType) {
                return buildTripleForTypeForConditionColumn(CharRange.class,
                        condition,
                        true,
                        isMoreThanOneColumnIsUsed);
            } else if (isStringType && isSmart(decisionTable.getSyntaxNode()) && !isAllParsableAsDomainFlag) {
                return buildTripleForTypeForConditionColumn(StringRange.class,
                        condition,
                        true,
                        isMoreThanOneColumnIsUsed);
            }
            return buildTripleForConditionColumnWithSimpleType(condition,
                    type,
                    true,
                    isMoreThanOneColumnIsUsed,
                    module,
                    cache);
        } else {
            return buildTripleForConditionColumnWithSimpleType(condition,
                    type,
                    false,
                    isMoreThanOneColumnIsUsed,
                    module,
                    cache);
        }
    }

    private static Triple<String[], IOpenClass, String> buildTripleForConditionColumnWithSimpleType(DTHeader condition,
                                                                                                    IOpenClass type,
                                                                                                    boolean isArray,
                                                                                                    boolean isMoreThanOneColumnIsUsed,
                                                                                                    XlsModuleOpenClass module,
                                                                                                    IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache) {
        if (type.isArray() && type.getComponentClass().isArray()) {
            return Triple.of(new String[]{getTypeNameForCode(type, module, cache)}, type, condition.getStatement());
        }
        int v;
        if (isArray) {
            v = isMoreThanOneColumnIsUsed ? 2 : 1;
        } else {
            v = isMoreThanOneColumnIsUsed ? 1 : 0;
        }

        if (v == 0) {
            return Triple.of(new String[]{getTypeNameForCode(type, module, cache)}, type, condition.getStatement());
        } else if (v == 1) {
            return Triple.of(new String[]{getTypeNameForCode(type, module, cache) + "[]"},
                    AOpenClass.getArrayType(type, 1),
                    condition.getStatement());
        } else {
            return Triple.of(new String[]{getTypeNameForCode(type, module, cache) + "[][]"},
                    AOpenClass.getArrayType(type, 2),
                    condition.getStatement());
        }
    }

    private static IOpenClass getTypeForCondition(DecisionTable decisionTable, DTHeader condition) {
        if (condition instanceof FuzzyDTHeader fuzzyCondition) {
            if (fuzzyCondition.isMethodParameterUsed()) {
                if (fuzzyCondition.getFieldsChain() != null) {
                    return fuzzyCondition.getFieldsChain()[fuzzyCondition.getFieldsChain().length - 1].getType();
                }
            } else {
                if (fuzzyCondition.getFuzzyResult().getToken() instanceof PredicateToken) {
                    return JavaOpenClass.getOpenClass(Boolean.class);
                }
            }
        } else if (condition instanceof DeclaredDTHeader declaredDTHeader) {
            return declaredDTHeader.getDtColumnsDefinition().getCompositeMethod().getType();
        }
        if (condition.isMethodParameterUsed()) {
            return decisionTable.getSignature().getParameterTypes()[condition.getMethodParameterIndex()];
        }
        throw new IllegalStateException();
    }

    /**
     * @deprecated Use plain grid model aka 2d array instead of building memory expensive Excel files.
     */
    @Deprecated
    public static XlsSheetGridModel createVirtualGrid() {
        var workbook = new XSSFWorkbook();
        try {
            final var sheet = workbook.createSheet();
            final var sourceCodeModule = new StringSourceCodeModule("", null);
            final var workbookLoader = new SimpleWorkbookLoader(sheet.getWorkbook());
            var mockWorkbookSource = new XlsWorkbookSourceCodeModule(sourceCodeModule,
                    workbookLoader);
            var mockSheetSource = new XlsSheetSourceCodeModule(new SimpleSheetLoader(sheet),
                    mockWorkbookSource);

            return new XlsSheetGridModel(mockSheetSource);
        } catch (Exception e) {
            // If exception is thrown, we must close workbook in this method and rethrow exception.
            // If no exception, workbook will be closed later.
            IOUtils.closeQuietly(workbook);
            throw e;
        }
    }

    public static boolean isCollect(TableSyntaxNode tableSyntaxNode) {
        return tableSyntaxNode.getHeader().isCollect();
    }

    public static boolean isSmart(TableSyntaxNode tableSyntaxNode) {
        return isSmartDecisionTable(tableSyntaxNode) || isSmartLookupTable(tableSyntaxNode);
    }

    public static boolean isSimple(TableSyntaxNode tableSyntaxNode) {
        return isSimpleDecisionTable(tableSyntaxNode) || isSimpleLookupTable(tableSyntaxNode);
    }

    public static boolean isLookup(TableSyntaxNode tableSyntaxNode) {
        return isSimpleLookupTable(tableSyntaxNode) || isSmartLookupTable(tableSyntaxNode);
    }

    public static boolean isSmartDecisionTable(TableSyntaxNode tableSyntaxNode) {
        var dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();
        return IXlsTableNames.SMART_DECISION_TABLE.equals(dtType);
    }

    public static boolean isSimpleDecisionTable(TableSyntaxNode tableSyntaxNode) {
        var dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();
        return IXlsTableNames.SIMPLE_DECISION_TABLE.equals(dtType);
    }

    public static boolean isSmartLookupTable(TableSyntaxNode tableSyntaxNode) {
        var dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();
        return IXlsTableNames.SMART_DECISION_LOOKUP.equals(dtType);
    }

    public static boolean isSimpleLookupTable(TableSyntaxNode tableSyntaxNode) {
        var dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();
        return IXlsTableNames.SIMPLE_DECISION_LOOKUP.equals(dtType);
    }

    public static boolean isRulesTable(TableSyntaxNode tableSyntaxNode) {
        var dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();
        return IXlsTableNames.DECISION_TABLE.equals(dtType) || IXlsTableNames.DECISION_TABLE2.equals(dtType);
    }

    public static boolean isDecisionTable(TableSyntaxNode tableSyntaxNode) {
        return isRulesTable(tableSyntaxNode) || isSmartDecisionTable(tableSyntaxNode) || isSimpleDecisionTable(
                tableSyntaxNode) || isLookup(
                tableSyntaxNode) || isSmartLookupTable(tableSyntaxNode) || isSimpleLookupTable(tableSyntaxNode);
    }

    static int countHConditionsByHeaders(ILogicalTable table) {
        var width = table.getWidth();
        var cnt = 0;

        for (var i = 0; i < width; i++) {
            var value = table.getColumn(i).getSource().getCell(0, 0).getStringValue();
            if (value != null) {
                value = value.toUpperCase();
                if (isValidHConditionHeader(value)) {
                    ++cnt;
                }
            }
        }
        return cnt;
    }

    static int countVConditionsByHeaders(ILogicalTable table) {
        var width = table.getWidth();
        var cnt = 0;
        for (var i = 0; i < width; i++) {
            var value = table.getColumn(i).getSource().getCell(0, 0).getStringValue();
            if (value != null) {
                value = value.toUpperCase();
                if (isValidConditionHeader(value) || isValidMergedConditionHeader(value)) {
                    cnt++;
                }
            }
        }
        return cnt;
    }

    static Pair<Integer, Integer> countAllHeaderTypes(ILogicalTable table) {
        var width = table.getWidth();
        var cnt = 0;
        var nonHeaderCnt = 0;
        for (var i = 0; i < width; i++) {
            var value = table.getColumn(i).getSource().getCell(0, 0).getStringValue();
            if (value != null && !StringUtils.isEmpty(value)) {
                value = value.toUpperCase();
                if (isConditionHeader(value) || isValidRetHeader(value) || isValidCRetHeader(
                        value) || isValidActionHeader(value) || isValidKeyHeader(value) || isValidRuleHeader(value)) {
                    cnt++;
                } else {
                    nonHeaderCnt++;
                }
            }
        }
        return Pair.of(cnt, nonHeaderCnt);
    }

    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    private static final class ParameterTokens {
        @Getter
        final Token[] tokens;
        final Map<Token, Integer> tokensToParameterIndex;
        final Map<Token, IOpenField[]> tokenToFieldsChain;

        IOpenField[] getFieldsChain(Token value) {
            return tokenToFieldsChain.get(value);
        }

        Integer getParameterIndex(Token value) {
            return tokensToParameterIndex.get(value);
        }
    }

    @RequiredArgsConstructor
    public static class NumberOfColumnsUnderTitleCounter {
        final ILogicalTable logicalTable;
        final int firstColumnHeight;
        final Map<Integer, List<Integer>> numberOfColumnsMap = new HashMap<>();

        private List<Integer> init(int column) {
            var w = logicalTable.getSource().getCell(column, 0).getWidth();
            var i = 0;
            var w1 = new ArrayList<Integer>();
            while (i < w) {
                var w0 = logicalTable.getSource().getCell(column + i, firstColumnHeight).getWidth();
                i = i + w0;
                w1.add(w0);
            }
            return w1;
        }

        public int get(int column) {
            List<Integer> numberOfColumns = numberOfColumnsMap.computeIfAbsent(column, e -> init(column));
            return numberOfColumns.size();
        }

        public int getWidth(int column, int num) {
            List<Integer> numberOfColumns = numberOfColumnsMap.computeIfAbsent(column, e -> init(column));
            return numberOfColumns.get(num);
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class FuzzyContext {
        @Getter(AccessLevel.PACKAGE)
        final ParameterTokens parameterTokens;
        Token[] returnTokens = null;
        Map<Token, IOpenField[][]> returnTypeFuzzyTokens = null;
        @Getter(AccessLevel.PACKAGE)
        IOpenClass fuzzyReturnType;
        @Getter
        int maxDistance;

        private FuzzyContext(ParameterTokens parameterTokens,
                             Token[] returnTokens,
                             Map<Token, IOpenField[][]> returnTypeFuzzyTokens,
                             IOpenClass returnType) {
            this(parameterTokens);
            this.returnTokens = returnTokens;
            this.returnTypeFuzzyTokens = returnTypeFuzzyTokens;
            this.fuzzyReturnType = returnType;
            this.maxDistance = Arrays.stream(parameterTokens.getTokens()).mapToInt(Token::getDistance).max().orElse(0);
        }

        Token[] getFuzzyReturnTokens() {
            return returnTokens;
        }

        IOpenField[][] getFieldsChainsForReturnToken(Token token) {
            return returnTypeFuzzyTokens.get(token);
        }

        boolean isFuzzySupportsForReturnType() {
            return returnTypeFuzzyTokens != null && returnTokens != null && fuzzyReturnType != null;
        }
    }
}
