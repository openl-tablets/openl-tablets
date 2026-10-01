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
import java.util.function.Consumer;
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
import org.jspecify.annotations.Nullable;

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
import org.openl.rules.convertor.IString2DataConvertor;
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
import org.openl.types.IOpenMethodHeader;
import org.openl.types.IParameterDeclaration;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.AOpenClass;
import org.openl.types.impl.BelongsToModuleOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.IOUtils;

public final class DecisionTableHelper {

    private static final String RETURN_PREFIX = "Return: ";

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
            resolveConflictsInFit(dtHeaders, usedMethodSignatureIdentifiers);
        }
    }

    /**
     * Renames the parameters of the declared headers of a fit that clash with the parameters of the method or with
     * each other, and the external parameters that refer to them.
     */
    private static void resolveConflictsInFit(List<DTHeader> dtHeaders, Set<String> usedMethodSignatureIdentifiers) {
        var usedAllParameterIdentifiers = new HashMap<String, Integer>();
        var externalParameters = new HashSet<String>();
        for (DTHeader dtHeader : dtHeaders) {
            if (dtHeader instanceof DeclaredDTHeader declaredDTHeader) {
                forEachColumnParameter(declaredDTHeader,
                        parameterDeclaration -> usedAllParameterIdentifiers.merge(parameterDeclaration.getName(),
                                1,
                                Integer::sum));
                externalParameters.addAll(
                        declaredDTHeader.getMatchedDefinition().getDtColumnsDefinition().getExternalParameters());
            }
        }
        var renamedParameters = new HashMap<String, String>();
        for (DTHeader dtHeader : dtHeaders) {
            if (dtHeader instanceof DeclaredDTHeader declaredDTHeader) {
                var usedLocalParameterIdentifiers = new HashSet<String>();
                forEachColumnParameter(declaredDTHeader,
                        parameterDeclaration -> usedLocalParameterIdentifiers
                                .add(toLowerCase(parameterDeclaration.getName())));
                forEachColumnParameter(declaredDTHeader,
                        parameterDeclaration -> renameConflictingParameter(declaredDTHeader,
                                parameterDeclaration,
                                usedMethodSignatureIdentifiers,
                                usedAllParameterIdentifiers,
                                externalParameters,
                                usedLocalParameterIdentifiers,
                                renamedParameters));
            }
        }
        for (DTHeader dtHeader : dtHeaders) {
            if (dtHeader instanceof DeclaredDTHeader declaredDTHeader) {
                renameExternalParameters(declaredDTHeader, renamedParameters);
            }
        }
    }

    private static void forEachColumnParameter(DeclaredDTHeader declaredDTHeader,
                                               Consumer<IParameterDeclaration> action) {
        for (var i = 0; i < declaredDTHeader.getColumnParameters().length; i++) {
            for (var j = 0; j < declaredDTHeader.getColumnParameters()[i].length; j++) {
                var parameterDeclaration = declaredDTHeader.getColumnParameters()[i][j];
                if (parameterDeclaration != null) {
                    action.accept(parameterDeclaration);
                }
            }
        }
    }

    private static void renameConflictingParameter(DeclaredDTHeader declaredDTHeader,
                                                   IParameterDeclaration parameterDeclaration,
                                                   Set<String> usedMethodSignatureIdentifiers,
                                                   Map<String, Integer> usedAllParameterIdentifiers,
                                                   Set<String> externalParameters,
                                                   Set<String> usedLocalParameterIdentifiers,
                                                   Map<String, String> renamedParameters) {
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

    private static void renameExternalParameters(DeclaredDTHeader declaredDTHeader,
                                                 Map<String, String> renamedParameters) {
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
        if (g instanceof BelongsToModuleOpenClass class1 && !module.isDependencyModule(class1.getModule(), cache)) {
            return class1.getExternalRefName() + "[]".repeat(Math.max(0, dim));
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
            var title = findReturnTitle(dtColumnsDefinition, d, lookupReturnHeader);
            if (title != null) {
                var parameters = dtColumnsDefinition.getParameters(title);
                var parameterNames = new ArrayList<String>();
                var typeOfColumns = new ArrayList<IOpenClass>();
                var totalColumnsUnder = getTotalColumnsUnder(originalTable, c);
                for (var paramIndex = 0; paramIndex < parameters.size(); paramIndex++) {
                    var param = parameters.get(paramIndex);
                    IOpenClass paramType = writeReturnParameter(grid,
                            declaredReturn,
                            param,
                            c,
                            parameterNames,
                            module,
                            cache);
                    typeOfColumns.add(paramType);
                    c = nextReturnParameterColumn(originalTable,
                            grid,
                            c,
                            paramType,
                            totalColumnsUnder - parameters.size(),
                            lookupReturnHeader);
                }
                if (!bindingContext.isExecutionMode()) {
                    writeMetaInfoForDeclaredReturn(tableSyntaxNode,
                            cell,
                            declaredReturn,
                            header,
                            parameterNames,
                            typeOfColumns);
                }
            }
        }

        if (c - declaredReturn.getColumn() > 1) {
            for (var row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1; row++) {
                grid.addMergedRegion(new GridRegion(row, declaredReturn.getColumn(), row, c - 1));
            }
        }
    }

    private static String findReturnTitle(DTColumnsDefinition dtColumnsDefinition,
                                          String d,
                                          boolean lookupReturnHeader) {
        for (String title : dtColumnsDefinition.getTitles()) {
            if (lookupReturnHeader || Objects.equals(d, title)) {
                return title;
            }
        }
        return null;
    }

    /**
     * Writes the declaration of a return parameter to its column.
     *
     * @return the type of the values of the column
     */
    private static IOpenClass writeReturnParameter(
            IWritableGrid grid,
            DeclaredDTHeader declaredReturn,
            IParameterDeclaration param,
            int c,
            List<String> parameterNames,
            XlsModuleOpenClass module,
            IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache) {
        IOpenClass paramType;
        if (param != null) {
            var paramName = declaredReturn.getMatchedDefinition().getParameter(param.getName());
            parameterNames.add(paramName);
            var value = getParameterDeclarationCode(param.getType(), paramName, module, cache);
            grid.setCellValue(c, 2, value);
            paramType = param.getType();
        } else {
            var compositeMethod = declaredReturn.getDtColumnsDefinition().getCompositeMethod();
            paramType = Objects.requireNonNull(compositeMethod, "composite method").getType();
        }
        return paramType;
    }

    private static String getParameterDeclarationCode(
            IOpenClass type,
            String paramName,
            XlsModuleOpenClass module,
            IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache) {
        return getTypeNameForCode(type,
                module,
                cache) + (paramName != null ? " " + paramName : "");
    }

    /**
     * Merges the cells of a return parameter. An array typed parameter also takes the columns that are left over
     * after every parameter got its own.
     *
     * @return the column that follows the columns of the parameter
     */
    private static int nextReturnParameterColumn(ILogicalTable originalTable,
                                                 IWritableGrid grid,
                                                 int c,
                                                 IOpenClass paramType,
                                                 int excessColumns,
                                                 boolean lookupReturnHeader) {
        if (lookupReturnHeader) {
            return c + 1;
        }
        var h = originalTable.getSource().getCell(c, 0).getHeight();
        var w1 = originalTable.getSource().getCell(c, h).getWidth();
        if (paramType != null && paramType.isArray()) {
            // If we have more columns than parameters use excess columns for array typed parameter
            var tmpC = c;
            for (var i = 0; i < excessColumns; i++) {
                var w2 = originalTable.getSource().getCell(tmpC, h).getWidth();
                w1 = w1 + w2;
                tmpC = tmpC + w2;
            }
        }
        if (w1 > 1) {
            grid.addMergedRegion(new GridRegion(2, c, 2, c + w1 - 1));
        }
        return c + w1;
    }

    private static void writeMetaInfoForDeclaredReturn(TableSyntaxNode tableSyntaxNode,
                                                       ICell cell,
                                                       DeclaredDTHeader declaredReturn,
                                                       String header,
                                                       List<String> parameterNames,
                                                       List<IOpenClass> typeOfColumns) {
        var sb = new StringBuilder();
        sb.append(RETURN_PREFIX).append(header);
        if (!StringUtils.isEmpty(declaredReturn.getStatement())) {
            sb.append("\n")
                    .append("Expression: ")
                    .append(declaredReturn.getStatement().replace("\n", StringUtils.SPACE));

        }
        DecisionTableMetaInfoReader.appendParameters(sb,
                parameterNames.toArray(EMPTY_STRING_ARRAY),
                typeOfColumns.toArray(IOpenClass.EMPTY));
        writeReturnMetaInfo(tableSyntaxNode,
                cell,
                sb.toString(),
                declaredReturn.getMatchedDefinition().getDtColumnsDefinition().getUri());
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
            String varName;
            type = fieldsChain[j].getType();
            if (j < fieldsChain.length - 1) {
                Map<IOpenField, String> vm = variables.get(currentVariable);
                if (vm == null || vm.get(fieldsChain[j]) == null) {
                    varName = generateVariableName(generatedNames);
                    sb.append(getTypeNameForCode(type, module, cache))
                            .append(" ")
                            .append(varName)
                            .append("=new ")
                            .append(getTypeNameForCode(type, module, cache))
                            .append("();");
                    sb.append("int ").append(varName).append("_").append("=0;");
                    vm = variables.computeIfAbsent(currentVariable, e -> new HashMap<>());
                    vm.put(fieldsChain[j], varName);
                    variableAssignments.add(currentVariable + "." + fieldsChain[j].getName() + "=" + varName
                            + "_>0?" + varName + ":null;");
                } else {
                    varName = vm.get(fieldsChain[j]);
                }
                currentVariable = varName;
                variablesInChain.add(currentVariable);
            } else {
                final var localVar = currentVariable + "." + fieldsChain[j].getName();
                appendFieldAssignment(sb, localVar, insertStatement, variablesInChain);
            }
        }
        return type;
    }

    private static String generateVariableName(Set<String> generatedNames) {
        var varName = RandomStringUtils.secure().next(8, true, false);
        while (generatedNames.contains(varName)) { // Prevent variable duplication
            varName = RandomStringUtils.secure().next(8, true, false);
        }
        generatedNames.add(varName);
        return varName;
    }

    /**
     * Appends the assignment of the field and the increments of the counters that tell the variables of the chain
     * got a value.
     */
    private static void appendFieldAssignment(StringBuilder sb,
                                              String localVar,
                                              String insertStatement,
                                              Set<String> variablesInChain) {
        sb.append(localVar).append("=").append(insertStatement).append(";");
        if (!variablesInChain.isEmpty()) {
            sb.append("if(").append(localVar).append("!=null){");
            for (String cv : variablesInChain) {
                sb.append(cv).append("_++;");
            }
            sb.append('}');
        }
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
                .filter(FuzzyDTHeader.class::isInstance)
                .map(e -> (FuzzyDTHeader) e)
                .filter(FuzzyDTHeader::isReturn)
                .toList();
        var m = groupReturnTokensByFieldsChain(fuzzyContext);

        var bestFuzzyResultsMap = findBestFuzzyResults(fuzzyContext, m, fuzzyReturns);

        var ambiguousReturnStatementMatching = new HashMap<String, Set<String>>();
        for (Entry<Token, List<Pair<IOpenField[], FuzzyResult>>> entry : bestFuzzyResultsMap.entrySet()) {
            var paramToken = entry.getKey();
            for (Pair<IOpenField[], FuzzyResult> pair : entry.getValue()) {
                var inputParameterStatement = buildInputParameterStatement(decisionTable, fuzzyContext, paramToken);
                final var statement = inputParameterStatement.getKey();
                var type = inputParameterStatement.getValue();
                var fieldsChain = pair.getKey();
                if (isImplicitlyCastToReturn(type, fuzzyContext.getFuzzyReturnType(), fieldsChain, bindingContext)) {
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

        ambiguousReturnStatementMatching.entrySet()
                .stream()
                .filter(e -> e.getValue().size() > 1)
                .forEach(e -> bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(
                "More than one input parameter is set to return '%s'.".formatted(e.getKey()),
                        tableSyntaxNode)));
    }

    private static Map<IOpenField[], List<Token>> groupReturnTokensByFieldsChain(FuzzyContext fuzzyContext) {
        var m = new HashMap<IOpenField[], List<Token>>();
        for (Token token : fuzzyContext.getFuzzyReturnTokens()) {
            var returnTypeFieldsChains = fuzzyContext.getFieldsChainsForReturnToken(token);
            for (IOpenField[] returnTypeFieldsChain : returnTypeFieldsChains) {
                if (!addToEqualFieldsChain(m, returnTypeFieldsChain, token)) {
                    var tokens = new ArrayList<Token>();
                    tokens.add(token);
                    m.put(returnTypeFieldsChain, tokens);
                }
            }
        }
        return m;
    }

    /**
     * Adds the token to the tokens of an equal fields chain.
     *
     * @return {@code false} when there is no equal fields chain yet
     */
    private static boolean addToEqualFieldsChain(Map<IOpenField[], List<Token>> m,
                                                 IOpenField[] returnTypeFieldsChain,
                                                 Token token) {
        for (Entry<IOpenField[], List<Token>> entry : m.entrySet()) {
            if (OpenLFuzzyUtils.isEqualsFieldsChains(entry.getKey(), returnTypeFieldsChain)) {
                entry.getValue().add(token);
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the fields of the return type that the input parameters match best, for the fields that no return
     * column sets.
     */
    private static Map<Token, List<Pair<IOpenField[], FuzzyResult>>> findBestFuzzyResults(
            FuzzyContext fuzzyContext,
            Map<IOpenField[], List<Token>> m,
            List<FuzzyDTHeader> fuzzyReturns) {
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
                    var resultList = getBestFuzzyResults(bestFuzzyResultsMap, fuzzyContext, fuzzyResult);
                    addBestFuzzyResult(resultList, fieldsChain, fuzzyResult);
                }
            }
        }
        return bestFuzzyResultsMap;
    }

    /**
     * Returns the best results found so far for the input parameter of the fuzzy result, adding an empty list for a
     * parameter that has none yet.
     */
    private static List<Pair<IOpenField[], FuzzyResult>> getBestFuzzyResults(
            Map<Token, List<Pair<IOpenField[], FuzzyResult>>> bestFuzzyResultsMap,
            FuzzyContext fuzzyContext,
            FuzzyResult fuzzyResult) {
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
        return resultList;
    }

    /**
     * Keeps the fields chain with the fuzzy result when the result is not worse than the best ones. A better result
     * replaces the best ones.
     */
    private static void addBestFuzzyResult(List<Pair<IOpenField[], FuzzyResult>> resultList,
                                           IOpenField[] fieldsChain,
                                           FuzzyResult fuzzyResult) {
        if (resultList.isEmpty()) {
            resultList.add(Pair.of(fieldsChain, fuzzyResult));
        } else {
            Pair<IOpenField[], FuzzyResult> existedResult = resultList.getFirst();
            var fuzzyResultCompare = fuzzyResult.compareTo(existedResult.getRight());
            if (fuzzyResultCompare <= 0) {
                if (fuzzyResultCompare < 0) {
                    resultList.clear();
                }
                if (!containsFieldsChain(resultList, fieldsChain)) {
                    resultList.add(Pair.of(fieldsChain, fuzzyResult));
                }
            }
        }
    }

    private static boolean containsFieldsChain(List<Pair<IOpenField[], FuzzyResult>> resultList,
                                               IOpenField[] fieldsChain) {
        for (Pair<IOpenField[], FuzzyResult> pair : resultList) {
            if (OpenLFuzzyUtils.isEqualsFieldsChains(pair.getKey(), fieldsChain)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds the statement that reads the input parameter of the token.
     *
     * @return the statement and its type
     */
    private static Pair<String, IOpenClass> buildInputParameterStatement(DecisionTable decisionTable,
                                                                        FuzzyContext fuzzyContext,
                                                                        Token paramToken) {
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
        return Pair.of(statement, type);
    }

    /**
     * Checks that a value of a simple type can be assigned to the field of the return type without an explicit cast.
     */
    private static boolean isImplicitlyCastToReturn(IOpenClass type,
                                                    IOpenClass returnType,
                                                    IOpenField[] fieldsChain,
                                                    IBindingContext bindingContext) {
        if (!isCompoundInputType(type)) {
            var p = buildStatementByFieldsChain(returnType,
                    fieldsChain);
            var cast = bindingContext.getCast(type, p.getValue());
            return cast != null && cast.isImplicit();
        }
        return false;
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
                .toList();

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
            generatedNames.add(RandomStringUtils.secure().next(8, true, false));
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
                sb1.append(RETURN_PREFIX).append(header);

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
            sb.append(RETURN_PREFIX).append(header);
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
            var retColumnName = isCollect ? CRET1_COLUMN_NAME : RET1_COLUMN_NAME;
            if (lookupReturnDtHeader != null) {
                writeReturnWithReturnDtHeader(tableSyntaxNode,
                        uncutOriginalTable,
                        originalTable,
                        grid,
                        lookupReturnDtHeader,
                        retColumnName,
                        true,
                        module,
                        cache,
                        bindingContext);
            } else {
                var retColumn = getRetColumn(dtHeaders);
                grid.setCellValue(retColumn, 0, retColumnName);
            }
            return;
        }

        if (dtHeaders.stream()
                .filter(DTHeader::isReturn)
                .anyMatch(e -> e.getColumn() + e.getWidth() - 1 >= originalTable.getSource().getWidth())) {
            throw new OpenLCompilationException("Wrong table structure: There is no column for return values.");
        }

        var returnColumns = new ReturnColumns(isCollect);
        var skipFuzzyReturns = false;
        for (DTHeader dtHeader : dtHeaders) {
            if (!dtHeader.isReturn()) {
                continue;
            }
            if (dtHeader instanceof DeclaredDTHeader header2) {
                writeReturnWithReturnDtHeader(tableSyntaxNode,
                        uncutOriginalTable,
                        originalTable,
                        grid,
                        header2,
                        returnColumns.nextReturnHeader(),
                        false,
                        module,
                        cache,
                        bindingContext);
            } else if (isSimpleReturn(dtHeader)) {
                writeSimpleReturn(tableSyntaxNode,
                        decisionTable,
                        originalTable,
                        grid,
                        dtHeader,
                        returnColumns,
                        bindingContext);
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
                        returnColumns.nextReturnHeader(),
                        module,
                        cache,
                        bindingContext);
                skipFuzzyReturns = true;
            }
        }
    }

    /**
     * Numbers the return columns of a table in the order they are written.
     */
    private static final class ReturnColumns {
        private final boolean isCollect;
        private int retNum = 1;
        private int cRetNum = 1;
        private int keyNum = 1;
        private int simpleReturnsCount;
        private int collectParameterIndex;

        private ReturnColumns(boolean isCollect) {
            this.isCollect = isCollect;
        }

        private String nextReturnHeader() {
            return isCollect ? DecisionTableColumnHeaders.COLLECT_RETURN.getHeaderKey() + cRetNum++
                    : DecisionTableColumnHeaders.RETURN.getHeaderKey() + retNum++;
        }

        private String nextKeyHeader() {
            return DecisionTableColumnHeaders.KEY.getHeaderKey() + keyNum++;
        }
    }

    /**
     * Checks whether the header returns the whole value of the table rather than a field of it.
     */
    private static boolean isSimpleReturn(DTHeader dtHeader) {
        return dtHeader instanceof SimpleReturnDTHeader || dtHeader instanceof FuzzyDTHeader header1 && header1
                .getFieldsChain() == null;
    }

    /**
     * Writes a column that returns the whole value of the table. The first such column of a collect table that
     * returns a map is the column of the keys.
     */
    private static void writeSimpleReturn(TableSyntaxNode tableSyntaxNode,
                                          DecisionTable decisionTable,
                                          ILogicalTable originalTable,
                                          IWritableGrid grid,
                                          DTHeader dtHeader,
                                          ReturnColumns returnColumns,
                                          IBindingContext bindingContext) {
        var isKey = false;
        String header;
        if (returnColumns.isCollect && tableSyntaxNode.getHeader()
                .getCollectParameters().length > 1 && returnColumns.simpleReturnsCount == 0 && ClassUtils
                .isAssignable(decisionTable.getType().getInstanceClass(), Map.class)) {
            header = returnColumns.nextKeyHeader();
            isKey = true;
        } else {
            header = returnColumns.nextReturnHeader();
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
                returnColumns.collectParameterIndex,
                bindingContext);
        returnColumns.simpleReturnsCount++;
        if (isKey) {
            returnColumns.collectParameterIndex++;
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
                            getParameterDeclarationCode(param.getType(), paramName, module, cache));
                    typeOfColumns.add(param.getType());
                } else {
                    parameterNames.add(null);
                    typeOfColumns.add(declaredDtHeader.getDtColumnsDefinition().getCompositeMethod().getType());
                }
                int w1 = getParameterColumnWidth(originalTable, declaredDtHeader, column, firstColumnHeight);
                if (w1 > 1) {
                    grid.addMergedRegion(new GridRegion(2, column, 2, column + w1 - 1));
                }
                lastParamFirstColumn = column;
                column = column + w1;
            }
        }

        if (!bindingContext.isExecutionMode()) {
            writeMetaInfoForDeclaredDtHeader(decisionTable,
                    originalTable,
                    declaredDtHeader,
                    header,
                    parameterNames,
                    typeOfColumns);
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

    private static int getParameterColumnWidth(ILogicalTable originalTable,
                                               DeclaredDTHeader declaredDtHeader,
                                               int column,
                                               int firstColumnHeight) {
        int w1;
        if (declaredDtHeader.isHCondition()) {
            w1 = 1;
        } else {
            w1 = originalTable.getSource().getCell(column, firstColumnHeight).getWidth();
        }
        return w1;
    }

    private static void writeMetaInfoForDeclaredDtHeader(DecisionTable decisionTable,
                                                         ILogicalTable originalTable,
                                                         DeclaredDTHeader declaredDtHeader,
                                                         String header,
                                                         List<String> parameterNames,
                                                         List<IOpenClass> typeOfColumns) {
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
                var res = compareValues(type, string2DataConverter, o1, cell2.getStringValue());
                if (res > 0) {
                    t1++;
                } else if (res < 0) {
                    t2++;
                }
            } finally {
                h = h + cell1.getHeight();
            }
        }
        return t1 <= t2;
    }

    /**
     * Compares a value with the one parsed from the text of another cell.
     *
     * @return the result of the comparison, or {@code 0} when the text cannot be parsed or the values cannot be
     * compared
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int compareValues(IOpenClass type,
                                     IString2DataConvertor<?> string2DataConverter,
                                     Object o1,
                                     String s2) {
        Object o2;
        try {
            o2 = string2DataConverter.parse(s2, null);
        } catch (IllegalArgumentException e) {
            return 0;
        }

        if (JavaOpenClass.STRING.equals(type) && o1 != null && o2 != null) {
            return NumericStringComparator.INSTANCE.compare((String) o1, (String) o2);
        } else if (o1 instanceof Comparable comparable && o2 instanceof Comparable) {
            return comparable.compareTo(o2);
        }
        return 0;
    }

    private static final String[] MIN_MAX_ORDER = new String[]{"min", "max"};
    private static final String[] MAX_MIN_ORDER = new String[]{"max", "min"};

    private static void writeUnmatchedColumns(DecisionTable decisionTable,
                                              ILogicalTable originalTable,
                                              List<DTHeader> dtHeaders,
                                              int firstColumnHeight,
                                              IBindingContext bindingContext) throws OpenLCompilationException {
        var unmatched = dtHeaders.stream()
                .filter(UnmatchedDtHeader.class::isInstance)
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
        var context = new ConditionsContext(decisionTable,
                originalTable,
                grid,
                numberOfColumnsUnderTitleCounter,
                firstColumnHeight,
                module,
                cache,
                bindingContext);
        for (DTHeader condition : conditions) {
            var column = condition.getColumn();
            if (!isLookup(tableSyntaxNode)) {
                validateConditionColumn(originalTable, column);
            }
            // write headers
            //

            String header;
            if (!condition.isHCondition()) {
                // write vertical condition
                //
                numOfVCondition++;
                header = getVConditionHeader(decisionTable, conditions, isCollect, numOfVCondition);
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
                writeCondition(context,
                        condition,
                        header,
                        numOfHCondition,
                        firstColumnForHConditionsOrReturns,
                        hConditionTypes);
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

    /**
     * The table whose condition headers are written, the layout of its column titles and the grid the headers are
     * written to.
     */
    private record ConditionsContext(DecisionTable decisionTable,
                                     ILogicalTable originalTable,
                                     IWritableGrid grid,
                                     NumberOfColumnsUnderTitleCounter numberOfColumnsUnderTitleCounter,
                                     int firstColumnHeight,
                                     XlsModuleOpenClass module,
                                     IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache,
                                     IBindingContext bindingContext) {
    }

    private static void validateConditionColumn(ILogicalTable originalTable,
                                                int column) throws OpenLCompilationException {
        if (column > originalTable.getSource().getWidth()) {
            var message = "Wrong table structure: Columns count is less than parameters count";
            throw new OpenLCompilationException(message);
        }
        if (column > originalTable.getSource().getWidth()) {
            var message = "Wrong table structure: There is no column for return values";
            throw new OpenLCompilationException(message);
        }
    }

    /**
     * Names a vertical condition. The only vertical condition of a table that returns a single value is merged.
     */
    private static String getVConditionHeader(DecisionTable decisionTable,
                                              List<DTHeader> conditions,
                                              boolean isCollect,
                                              int numOfVCondition) {
        if (numOfVCondition == 1 && (conditions.stream()
                .filter(e -> !e.isHCondition())
                .count() < 2) && !(isCollect && decisionTable.getType()
                .isArray() && !decisionTable.getType()
                .getComponentClass()
                .isArray()) && !(isCollect && ClassUtils
                .isAssignable(decisionTable.getType().getInstanceClass(), Collection.class))) {
            return DecisionTableColumnHeaders.MERGED_CONDITION.getHeaderKey() + numOfVCondition;
        } else {
            return DecisionTableColumnHeaders.CONDITION.getHeaderKey() + numOfVCondition;
        }
    }

    private static void writeCondition(ConditionsContext context,
                                       DTHeader condition,
                                       String header,
                                       int numOfHCondition,
                                       int firstColumnForHConditionsOrReturns,
                                       Map<DTHeader, IOpenClass> hConditionTypes) {
        var column = condition.getColumn();
        var numberOfColumnsUnderTitleCounter = context.numberOfColumnsUnderTitleCounter();
        context.grid().setCellValue(column, 0, header);
        final var numberOfColumnsUnderTitle = numberOfColumnsUnderTitleCounter.get(column);
        IOpenClass type = getTypeForCondition(context.decisionTable(), condition);
        if (isMinMaxCondition(condition, type, numberOfColumnsUnderTitle, numberOfColumnsUnderTitleCounter)) {
            writeMinMaxCondition(context, condition, header, type);
        } else {
            writeConditionWithTypeOfValues(context,
                    condition,
                    header,
                    numOfHCondition,
                    firstColumnForHConditionsOrReturns,
                    numberOfColumnsUnderTitle,
                    hConditionTypes);
        }
    }

    /**
     * Checks whether the condition is matched to two columns of comparable values that hold the bounds of a range.
     */
    private static boolean isMinMaxCondition(DTHeader condition,
                                             IOpenClass type,
                                             int numberOfColumnsUnderTitle,
                                             NumberOfColumnsUnderTitleCounter numberOfColumnsUnderTitleCounter) {
        var column = condition.getColumn();
        return condition instanceof FuzzyDTHeader && numberOfColumnsUnderTitle == 2 && condition
                .getWidthForMerge() == numberOfColumnsUnderTitleCounter.getWidth(column,
                0) + numberOfColumnsUnderTitleCounter.getWidth(column, 1) && type
                .getInstanceClass() != null && (type.getInstanceClass()
                .isPrimitive() || ClassUtils.isAssignable(type.getInstanceClass(), Comparable.class));
    }

    private static void writeMinMaxCondition(ConditionsContext context,
                                             DTHeader condition,
                                             String header,
                                             IOpenClass type) {
        var column = condition.getColumn();
        var grid = context.grid();
        var numberOfColumnsUnderTitleCounter = context.numberOfColumnsUnderTitleCounter();
        var module = context.module();
        var cache = context.cache();
        var minMaxOrder = getMinMaxOrder(context.originalTable(),
                numberOfColumnsUnderTitleCounter,
                context.firstColumnHeight(),
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
            writeMinMaxVCondition(context, condition, header, type, statement, minMaxOrder);
        }
    }

    private static void writeMinMaxVCondition(ConditionsContext context,
                                              DTHeader condition,
                                              String header,
                                              IOpenClass type,
                                              String statement,
                                              boolean minMaxOrder) {
        var column = condition.getColumn();
        if (!context.bindingContext().isExecutionMode()) {
            writeMetaInfoForVCondition(context.originalTable(),
                    context.decisionTable(),
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
                context.grid()
                        .addMergedRegion(new GridRegion(row, column, row, column + condition.getWidthForMerge() - 1));
            }
        }
    }

    private static void writeConditionWithTypeOfValues(ConditionsContext context,
                                                       DTHeader condition,
                                                       String header,
                                                       int numOfHCondition,
                                                       int firstColumnForHConditionsOrReturns,
                                                       int numberOfColumnsUnderTitle,
                                                       Map<DTHeader, IOpenClass> hConditionTypes) {
        var column = condition.getColumn();
        var grid = context.grid();
        // Set type of condition values(for Ranges and Array)
        var typeOfValue = getTypeForConditionColumn(context.decisionTable(),
                context.originalTable(),
                condition,
                numOfHCondition,
                firstColumnForHConditionsOrReturns,
                context.firstColumnHeight(),
                numberOfColumnsUnderTitle,
                context.module(),
                context.cache(),
                context.bindingContext());
        grid.setCellValue(column, 1, typeOfValue.getRight());
        grid.setCellValue(column,
                2,
                typeOfValue.getLeft().length == 1 ? typeOfValue.getLeft()[0]
                        : typeOfValue.getLeft()[0] + " " + typeOfValue.getLeft()[1]);
        if (condition.isHCondition()) {
            hConditionTypes.put(condition, typeOfValue.getMiddle());
        } else {
            if (!context.bindingContext().isExecutionMode()) {
                writeMetaInfoForVCondition(context.originalTable(),
                        context.decisionTable(),
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
        var hDtHeaders = conditions.stream().filter(DTHeader::isHCondition).toList();
        int minColumn = getMinColumnForHConditions(originalTable,
                conditions,
                hDtHeaders,
                firstColumnForHCondition,
                withVerticalTitles);
        var numOfCondition = 1;
        for (DTHeader condition : hDtHeaders) {
            var column = minColumn;
            while (column < originalTable.getSource().getWidth()) {
                var cell = originalTable.getSource().getCell(column, j);
                cell = cell.getTopLeftCellFromRegion();
                var cellValue = cell.getStringValue();
                if (cellValue != null && metaInfoReader instanceof DecisionTableMetaInfoReader reader) {
                    var type = getHConditionType(decisionTable, condition, hConditionTypes);
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

    private static int getMinColumnForHConditions(ILogicalTable originalTable,
                                                  List<DTHeader> conditions,
                                                  List<DTHeader> hDtHeaders,
                                                  int firstColumnForHCondition,
                                                  WithVerticalTitles withVerticalTitles) {
        int minColumn;
        if (!WithVerticalTitles.NO.equals(withVerticalTitles) && firstColumnForHCondition > 0) {
            minColumn = firstColumnForHCondition - originalTable.getSource()
                    .getCell(firstColumnForHCondition - 1, 0)
                    .getWidth();
            var vDtHeaders = conditions.stream()
                    .filter(e -> e.isCondition() && !e.isHCondition())
                    .toList();
            if (!vDtHeaders.isEmpty()) {
                var lastVCondition = vDtHeaders.getLast();
                if (lastVCondition instanceof DeclaredDTHeader declaredDTHeader
                        && !declaredDTHeader.isVerticalConditionWithMergedTitle()) {
                    minColumn = hDtHeaders.stream().mapToInt(DTHeader::getColumn).min().orElse(0);
                }
            }
        } else {
            minColumn = hDtHeaders.stream().mapToInt(DTHeader::getColumn).min().orElse(0);
        }
        return minColumn;
    }

    private static IOpenClass getHConditionType(DecisionTable decisionTable,
                                                DTHeader condition,
                                                Map<DTHeader, IOpenClass> hConditionTypes) {
        var type = hConditionTypes.get(condition);
        if (type == null) {
            type = getTypeForCondition(decisionTable, condition);
        }
        return type;
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

        var completeParameters = getCompleteParameters(definition);

        var methodParametersUsedInExpression = new HashSet<String>();
        var originalMethodParametersUsedInExpression = new HashMap<String, String>();
        collectMethodParametersUsedInExpression(identifiers,
                completeParameters,
                methodParametersUsedInExpression,
                originalMethodParametersUsedInExpression);

        var methodParametersToRename = new HashMap<String, String>();
        var usedMethodParameterIndexes = new HashSet<Integer>();
        var paramToIndex = new HashMap<String, Integer>();
        var usedParamIndexesByField = new HashSet<Integer>();
        if (matchParametersByName(methodParametersUsedInExpression,
                definition,
                header,
                paramToIndex,
                usedMethodParameterIndexes,
                methodParametersToRename,
                usedParamIndexesByField)) {
            mayHaveCompilationErrors = true;
        }

        var matchType = matchParametersByType(definition,
                header,
                methodParametersUsedInExpression,
                paramToIndex,
                usedMethodParameterIndexes,
                methodParametersToRename,
                bindingContext);
        if (matchType == null) {
            return null;
        }

        if (usedMethodParameterIndexes.size() != methodParametersUsedInExpression.size()) {
            if (numberOfHConditions > 0) {
                return null;
            }
            renameUnmatchedMethodParameters(header,
                    usedMethodParameterIndexes,
                    methodParametersUsedInExpression,
                    originalMethodParametersUsedInExpression,
                    methodParametersToRename);
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

    private static Map<String, IParameterDeclaration> getCompleteParameters(DTColumnsDefinition definition) {
        var completeParameters = new HashMap<String, IParameterDeclaration>();
        for (IParameterDeclaration parameter : definition.getParameters()) {
            if (parameter != null && parameter.getName() != null) {
                completeParameters.put(toLowerCase(parameter.getName()), parameter);
            }
        }
        return completeParameters;
    }

    /**
     * Collects the identifiers of the expression that are not parameters of the definition, in lower case and as
     * they are written.
     */
    private static void collectMethodParametersUsedInExpression(
            List<ExpressionIdentifier> identifiers,
            Map<String, IParameterDeclaration> completeParameters,
            Set<String> methodParametersUsedInExpression,
            Map<String, String> originalMethodParametersUsedInExpression) {
        for (ExpressionIdentifier identifier : identifiers) {
            if (!completeParameters.containsKey(toLowerCase(identifier.getIdentifier()))) {
                methodParametersUsedInExpression.add(toLowerCase(identifier.getIdentifier()));
                originalMethodParametersUsedInExpression.put(toLowerCase(identifier.getIdentifier()),
                        identifier.getIdentifier());
            }
        }
    }

    /**
     * Matches the identifiers of the expression with the parameters of the definition header and of the method by
     * name. An identifier that is not a parameter of the definition header is removed, as it names a field of a
     * parameter.
     *
     * @return {@code true} when such a field is found in more than one parameter of the method
     */
    private static boolean matchParametersByName(Set<String> methodParametersUsedInExpression,
                                                 DTColumnsDefinition definition,
                                                 IOpenMethodHeader header,
                                                 Map<String, Integer> paramToIndex,
                                                 Set<Integer> usedMethodParameterIndexes,
                                                 Map<String, String> methodParametersToRename,
                                                 Set<Integer> usedParamIndexesByField) {
        var mayHaveCompilationErrors = false;
        Iterator<String> itr = methodParametersUsedInExpression.iterator();
        while (itr.hasNext()) {
            var param = itr.next();
            var found = matchParameterByName(param,
                    definition,
                    header,
                    paramToIndex,
                    usedMethodParameterIndexes,
                    methodParametersToRename);
            if (!found) {
                var numberOfCandidates = countFieldCandidates(param, definition, header, usedParamIndexesByField);
                if (numberOfCandidates > 1) {
                    mayHaveCompilationErrors = true;
                }
                itr.remove();
            }
        }
        return mayHaveCompilationErrors;
    }

    /**
     * Matches the identifier with a parameter of the definition header and with the method parameter of the same
     * name and a compatible type.
     *
     * @return {@code true} when the identifier is a parameter of the definition header
     */
    private static boolean matchParameterByName(String param,
                                                DTColumnsDefinition definition,
                                                IOpenMethodHeader header,
                                                Map<String, Integer> paramToIndex,
                                                Set<Integer> usedMethodParameterIndexes,
                                                Map<String, String> methodParametersToRename) {
        for (var i = 0; i < definition.getHeader().getSignature().getNumberOfParameters(); i++) {
            if (param.equalsIgnoreCase(definition.getHeader().getSignature().getParameterName(i))) {
                paramToIndex.put(param, i);
                var type = definition.getHeader().getSignature().getParameterType(i);
                for (var j = 0; j < header.getSignature().getNumberOfParameters(); j++) {
                    if (param.equalsIgnoreCase(header.getSignature().getParameterName(j)) && type
                            .isAssignableFrom(header.getSignature().getParameterType(j))) {
                        usedMethodParameterIndexes.add(j);
                        methodParametersToRename.put(param, header.getSignature().getParameterName(j));
                        break;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Counts the method parameters that a field with the name of the identifier can be read from.
     */
    private static int countFieldCandidates(String param,
                                            DTColumnsDefinition definition,
                                            IOpenMethodHeader header,
                                            Set<Integer> usedParamIndexesByField) {
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
        return numberOfCandidates;
    }

    /**
     * Matches the parameters of the definition header that are not matched by name with the parameters of the
     * method: by name with a cast, then by type only, then by type with a cast.
     *
     * @return the kind of the last match, or {@code null} when a parameter matches more than one parameter of the
     * method
     */
    private static MatchType matchParametersByType(DTColumnsDefinition definition,
                                                   IOpenMethodHeader header,
                                                   Set<String> methodParametersUsedInExpression,
                                                   Map<String, Integer> paramToIndex,
                                                   Set<Integer> usedMethodParameterIndexes,
                                                   Map<String, String> methodParametersToRename,
                                                   IBindingContext bindingContext) {
        MatchType[] matchTypes = {MatchType.STRICT_CASTED,
                MatchType.METHOD_ARGS_RENAMED,
                MatchType.METHOD_ARGS_RENAMED_CASTED};

        var matchType = MatchType.STRICT;
        for (MatchType mt : matchTypes) {
            var itr = methodParametersUsedInExpression.iterator();
            while (itr.hasNext()) {
                var param = itr.next();
                if (methodParametersToRename.containsKey(param)) {
                    continue;
                }
                var j = paramToIndex.get(param);
                var type = definition.getHeader().getSignature().getParameterType(j);
                var parameterMatch = matchParameterByType(mt,
                        param,
                        type,
                        header,
                        usedMethodParameterIndexes,
                        methodParametersToRename,
                        bindingContext);
                if (parameterMatch == ParameterMatch.AMBIGUOUS) {
                    return null;
                } else if (parameterMatch == ParameterMatch.UNIQUE) {
                    matchType = mt;
                }
            }
        }
        return matchType;
    }

    private enum ParameterMatch {
        NONE,
        UNIQUE,
        AMBIGUOUS
    }

    /**
     * Matches a parameter of the definition header with the parameters of the method that are not used yet, and
     * renames the parameter to the matched one.
     */
    private static ParameterMatch matchParameterByType(MatchType mt,
                                                       String param,
                                                       IOpenClass type,
                                                       IOpenMethodHeader header,
                                                       Set<Integer> usedMethodParameterIndexes,
                                                       Map<String, String> methodParametersToRename,
                                                       IBindingContext bindingContext) {
        var duplicatedMatch = false;
        for (var i = 0; i < header.getSignature().getNumberOfParameters(); i++) {
            boolean predicate = isMatchedByType(mt, param, type, header, i, bindingContext);
            if (!usedMethodParameterIndexes.contains(i) && predicate) {
                if (duplicatedMatch) {
                    return ParameterMatch.AMBIGUOUS;
                }
                duplicatedMatch = true;
                usedMethodParameterIndexes.add(i);
                methodParametersToRename.put(param, getMatchedParameterName(mt, type, header, i, bindingContext));
            }
        }
        return duplicatedMatch ? ParameterMatch.UNIQUE : ParameterMatch.NONE;
    }

    private static boolean isMatchedByType(MatchType mt,
                                           String param,
                                           IOpenClass type,
                                           IOpenMethodHeader header,
                                           int i,
                                           IBindingContext bindingContext) {
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
        return predicate;
    }

    private static String getMatchedParameterName(MatchType mt,
                                                  IOpenClass type,
                                                  IOpenMethodHeader header,
                                                  int i,
                                                  IBindingContext bindingContext) {
        String newParam;
        switch (mt) {
            case STRICT_CASTED, METHOD_ARGS_RENAMED_CASTED:
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
        return newParam;
    }

    /**
     * Renames the identifiers of the expression that have the names of the method parameters they are not matched
     * with, so that the expression does not read those parameters.
     */
    private static void renameUnmatchedMethodParameters(IOpenMethodHeader header,
                                                        Set<Integer> usedMethodParameterIndexes,
                                                        Set<String> methodParametersUsedInExpression,
                                                        Map<String, String> originalMethodParametersUsedInExpression,
                                                        Map<String, String> methodParametersToRename) {
        var u = new HashSet<String>();
        for (var i = 0; i < header.getSignature().getNumberOfParameters(); i++) {
            u.add(header.getSignature().getParameterName(i));
        }
        for (var i = 0; i < header.getSignature().getNumberOfParameters(); i++) {
            String lowParamName = toLowerCase(header.getSignature().getParameterName(i));
            if (!usedMethodParameterIndexes.contains(i) && methodParametersUsedInExpression
                    .contains(lowParamName)) {
                var newParamName = new StringBuilder("_")
                        .append(originalMethodParametersUsedInExpression.get(lowParamName));
                while (u.contains(newParamName.toString())) {
                    newParamName.insert(0, '_');
                }
                u.add(newParamName.toString());
                methodParametersToRename.put(lowParamName, newParamName.toString());
            }
        }
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
                    addFieldsChainToken(entry, i, tokens, tokenToParameterIndex, tokenToFieldsChain, tokensToIgnore);
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

    /**
     * Adds the token of a field of the parameter. A token that more than one field has is ignored.
     */
    private static void addFieldsChainToken(Map.Entry<Token, IOpenField[][]> entry,
                                            int paramIndex,
                                            Set<Token> tokens,
                                            Map<Token, Integer> tokenToParameterIndex,
                                            Map<Token, IOpenField[]> tokenToFieldsChain,
                                            Set<Token> tokensToIgnore) {
        if (entry.getValue().length == 1 && !tokensToIgnore.contains(entry.getKey())) {
            if (!tokens.contains(entry.getKey())) {
                tokens.add(entry.getKey());
                tokenToParameterIndex.put(entry.getKey(), paramIndex);
                tokenToFieldsChain.put(entry.getKey(), entry.getValue()[0]);
            } else {
                tokens.remove(entry.getKey());
                tokenToParameterIndex.remove(entry.getKey());
                tokenToFieldsChain.remove(entry.getKey());
                tokensToIgnore.add(entry.getKey());
            }
        }
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

    /**
     * Matches the titles of the cells under the title of a column with the input parameters and with the fields of
     * the return type by fuzzy search. The title of a cell is joined with the titles of the cells above it.
     */
    private static final class FuzzyTitlesSearch {
        private final DecisionTable decisionTable;
        private final ILogicalTable originalTable;
        private final IGridTable gridTable;
        private final FuzzyContext fuzzyContext;
        private final NumberOfColumnsUnderTitleCounter numberOfColumnsUnderTitleCounter;
        private final int numberOfHConditions;
        private final List<DTHeader> dtHeaders;
        private final int firstColumnHeight;
        private final List<String> parts = new ArrayList<>();
        private final int sourceTableColumn;
        private final int firstColumnForHCondition;
        private final boolean skipNextColumn;
        private final WithVerticalTitles withVerticalTitles;
        private final boolean onlyReturns;

        private FuzzyTitlesSearch(DecisionTable decisionTable,
                                  TitlesLayout layout,
                                  FuzzyContext fuzzyContext,
                                  int sourceTableColumn,
                                  List<DTHeader> dtHeaders,
                                  boolean onlyReturns) {
            this.decisionTable = decisionTable;
            this.originalTable = layout.originalTable();
            this.fuzzyContext = fuzzyContext;
            this.numberOfColumnsUnderTitleCounter = layout.numberOfColumnsUnderTitleCounter();
            this.numberOfHConditions = layout.numberOfHConditions();
            this.dtHeaders = dtHeaders;
            this.firstColumnHeight = layout.firstColumnHeight();
            this.sourceTableColumn = sourceTableColumn;
            this.firstColumnForHCondition = layout.firstColumnForHCondition();
            this.withVerticalTitles = layout.withVerticalTitles();
            this.onlyReturns = onlyReturns;
            var w = originalTable.getSource().getCell(sourceTableColumn, 0).getWidth();
            this.gridTable = originalTable.getSource().getSubtable(sourceTableColumn, 0, w, firstColumnHeight);
            var w0 = sourceTableColumn + originalTable.getSource().getCell(sourceTableColumn, 0).getWidth();
            this.skipNextColumn = w0 + originalTable.getSource()
                    .getCell(w0, 0)
                    .getWidth() == firstColumnForHCondition && (WithVerticalTitles.EMPTY_COLUMN
                    .equals(withVerticalTitles) || WithVerticalTitles.MERGED_COLUMN.equals(withVerticalTitles));
        }

        private void match(int w, int h) {
            var w0 = gridTable.getCell(w, h).getWidth();
            var h0 = gridTable.getCell(w, h).getHeight();
            var d = gridTable.getCell(w, h).getStringValue();
            String mergedPartsTitle;
            if (isVerticalTitlesCell(h, d)) {
                if (!onlyReturns) {
                    addHorizontalTitlesDtHeaders(w, h, d);
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
                    match(w2, h + h0);
                    w2 = w2 + w1;
                }
            } else {
                addDtHeaders(mergedPartsTitle, w, h, w0);
            }
            parts.removeLast();
        }

        /**
         * Checks whether the cell is the last title cell of the column before the horizontal conditions, which holds
         * the titles of the horizontal conditions.
         */
        private boolean isVerticalTitlesCell(int h, String d) {
            return sourceTableColumn + originalTable.getSource()
                    .getCell(sourceTableColumn, 0)
                    .getWidth() == firstColumnForHCondition && h == firstColumnHeight - 1
                    && (WithVerticalTitles.SLASH_IN_TITLE.equals(withVerticalTitles) && StringUtils.isNotBlank(
                    d) && d.contains(HORIZONTAL_VERTICAL_CONDITIONS_SPLITTER) || WithVerticalTitles.MERGED_COLUMN
                    .equals(withVerticalTitles) || WithVerticalTitles.EMPTY_COLUMN.equals(withVerticalTitles));
        }

        private void addHorizontalTitlesDtHeaders(int w, int h, String d) {
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

        private void addDtHeaders(String mergedPartsTitle, int w, int h, int w0) {
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
                addConditionDtHeaders(tokenizedTitleString, mergedPartsTitle, w, h, w0);
            }
        }

        private void addConditionDtHeaders(String tokenizedTitleString, String mergedPartsTitle, int w, int h, int w0) {
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
            var isHorizontal = horizontal > 0;
            var conditionColumn = isHorizontal ? sourceTableColumn + horizontal - 1 : sourceTableColumn + w;
            var predicateColumn = isHorizontal ? sourceTableColumn + horizontal - 1 : sourceTableColumn;
            var headerWidth = isHorizontal ? 1 : w0;
            var headerWidthForMerge = isHorizontal ? 1 : widthForMerge;
            for (FuzzyResult fuzzyResult : fuzzyResults) {
                var paramIndex = fuzzyContext.getParameterTokens().getParameterIndex(fuzzyResult.getToken());
                if (paramIndex != null) {
                    var fieldsChain = fuzzyContext.getParameterTokens().getFieldsChain(fuzzyResult.getToken());
                    var conditionStatement = buildConditionStatement(decisionTable, paramIndex, fieldsChain);
                    dtHeaders.add(new FuzzyDTHeader(paramIndex,
                            conditionStatement,
                            title,
                            fieldsChain,
                            sourceTableColumn,
                            conditionColumn,
                            h,
                            headerWidth,
                            headerWidthForMerge,
                            fuzzyResult,
                            false,
                            isHorizontal));
                } else if (fuzzyResult.getToken() instanceof PredicateToken predicateToken) {
                    dtHeaders.add(new FuzzyDTHeader(predicateToken.isTrue() ? "true" : "false",
                            title,
                            new IOpenField[]{},
                            sourceTableColumn,
                            predicateColumn,
                            h,
                            headerWidth,
                            headerWidthForMerge,
                            fuzzyResult,
                            false,
                            isHorizontal));
                } else if (sourceTableColumn == 0 && fuzzyResult.getToken() instanceof RuleToken) {
                    dtHeaders.add(new FuzzyRulesDTHeader(title, sourceTableColumn, h, w0, fuzzyResult));
                }
            }

        }

        private static String buildConditionStatement(DecisionTable decisionTable,
                                                      int paramIndex,
                                                      IOpenField[] fieldsChain) {
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
            return conditionStatement.toString();
        }
    }

    private static List<DTHeader> matchWithFuzzySearch(DecisionTable decisionTable,
                                                       TitlesLayout layout,
                                                       FuzzyContext fuzzyContext,
                                                       int column,
                                                       int lastColumn,
                                                       List<DTHeader> dtHeaders,
                                                       boolean onlyReturns) {
        if (onlyReturns && !fuzzyContext.isFuzzySupportsForReturnType()) {
            return Collections.emptyList();
        }
        if (layout.numberOfHConditions() > 0 && column >= lastColumn) {
            return Collections.emptyList();
        }
        var newDtHeaders = new ArrayList<DTHeader>();
        new FuzzyTitlesSearch(decisionTable, layout, fuzzyContext, column, newDtHeaders, onlyReturns).match(0, 0);
        dtHeaders.addAll(newDtHeaders);
        return Collections.unmodifiableList(newDtHeaders);
    }

    private static final int FITS_MAX_LIMIT = 10000;
    private static final int MAX_NUMBER_OF_RETURNS = 3;

    /**
     * Searches for the fits of the matched headers into the columns of a table, trying every compatible header for
     * each column.
     */
    private static final class HeadersFitSearch {
        private final ILogicalTable originalTable;
        private final int lastColumn;
        private final int firstColumnHeight;
        private final List<DTHeader> dtHeaders;
        private final Map<Integer, List<Integer>> columnToIndex = new HashMap<>();
        private final boolean[][] matrix;
        private final int maxColumnIndex;
        private final List<Integer> usedIndexes = new ArrayList<>();
        private final List<DTHeader> used = new ArrayList<>();
        private final List<List<DTHeader>> fits = new ArrayList<>();
        private final Set<Integer> failedToFit = new HashSet<>();
        private final int numberOfParameters;
        private final int numberOfHConditions;

        private HeadersFitSearch(ILogicalTable originalTable,
                                 List<DTHeader> dtHeaders,
                                 int lastColumn,
                                 int firstColumnHeight,
                                 int numberOfParameters,
                                 int numberOfHConditions) {
            this.originalTable = originalTable;
            this.lastColumn = lastColumn;
            this.firstColumnHeight = firstColumnHeight;
            this.dtHeaders = dtHeaders;
            this.numberOfParameters = numberOfParameters;
            this.numberOfHConditions = numberOfHConditions;
            this.matrix = buildCompatibilityMatrix(dtHeaders, columnToIndex);
            this.maxColumnIndex = numberOfHConditions > 0 ? lastColumn + numberOfHConditions
                    : originalTable.getSource().getWidth();
        }

        private boolean bruteForceHeaders(int column,
                                          Set<Integer> usedParameterIndexes,
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
                addFit();
            }
            var lastColumnReached = column >= maxColumnIndex;
            if (indexes != null) {
                // The headers are tried whatever the flag already says, so not behind a short-circuit.
                var reachedByHeaders = tryHeaders(indexes,
                        column,
                        usedParameterIndexes,
                        numberOfReturns,
                        fuzzyReturnsFlag,
                        counter);
                lastColumnReached = lastColumnReached || reachedByHeaders;
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

                    lastColumnReached = bruteForceHeaders(column + cell.getWidth(),
                            usedParameterIndexes,
                            numberOfReturns,
                            fuzzyReturnsFlag,
                            counter + 1);
                    used.removeLast();
                }
            }
            return lastColumnReached;
        }

        private void addFit() {
            var fit = new ArrayList<DTHeader>(used);
            while (!fit.isEmpty() && (fit.getLast() instanceof UnmatchedDtHeader)) {
                fit.removeLast();
            }
            if (!fit.isEmpty()) {
                fits.add(Collections.unmodifiableList(fit));
            }
        }

        /**
         * Tries the headers that start in the column as the next header of the fit. The headers are remembered as
         * failed to fit when none of them can be used.
         *
         * @return {@code true} when a fit with one of the headers reaches the last column
         */
        private boolean tryHeaders(List<Integer> indexes,
                                   int column,
                                   Set<Integer> usedParameterIndexes,
                                   int numberOfReturns,
                                   int fuzzyReturnsFlag,
                                   int counter) {
            var lastColumnReached = false;
            var last = true;
            for (Integer index : indexes) {
                if (canUseHeader(index, numberOfReturns, fuzzyReturnsFlag)) {
                    last = false;
                    // The recursion walks the remaining columns whatever the flag already says, so it
                    // runs before the flag is updated rather than behind a short-circuit.
                    var reachedByHeader = useHeader(index,
                            column,
                            usedParameterIndexes,
                            numberOfReturns,
                            fuzzyReturnsFlag,
                            counter);
                    lastColumnReached = lastColumnReached || reachedByHeader;
                }
            }
            if (!indexes.isEmpty() && last) {
                failedToFit.addAll(indexes);
            }
            return lastColumnReached;
        }

        /**
         * Checks that the header is compatible with the headers of the fit and does not exceed the number of returns.
         */
        private boolean canUseHeader(Integer index, int numberOfReturns, int fuzzyReturnsFlag) {
            if (!isCompatibleWithUsedHeaders(index)) {
                return false;
            }
            var dtHeader = dtHeaders.get(index);
            var isFuzzyReturn = isFuzzyReturn(dtHeader);
            if (isFuzzyReturn && fuzzyReturnsFlag == 2) {
                return false;
            }
            int numberOfReturns1 = nextNumberOfReturns(dtHeader, isFuzzyReturn, numberOfReturns);
            int fuzzyReturnsFlag1 = nextFuzzyReturnsFlag(isFuzzyReturn, fuzzyReturnsFlag);
            return numberOfReturns1 + (fuzzyReturnsFlag1 > 1 ? 1 : 0) <= MAX_NUMBER_OF_RETURNS;
        }

        private boolean isCompatibleWithUsedHeaders(Integer index) {
            for (Integer usedIndex : usedIndexes) {
                if (!matrix[index][usedIndex]) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Adds the header to the fit and searches for the headers of the next columns.
         *
         * @return {@code true} when a fit with the header reaches the last column
         */
        private boolean useHeader(Integer index,
                                  int column,
                                  Set<Integer> usedParameterIndexes,
                                  int numberOfReturns,
                                  int fuzzyReturnsFlag,
                                  int counter) {
            var dtHeader = dtHeaders.get(index);
            var isFuzzyReturn = isFuzzyReturn(dtHeader);
            var usedParameterIndexesTo = new HashSet<Integer>(usedParameterIndexes);
            for (int i : dtHeader.getMethodParameterIndexes()) {
                usedParameterIndexesTo.add(i);
            }
            int numberOfReturns1 = nextNumberOfReturns(dtHeader, isFuzzyReturn, numberOfReturns);
            int fuzzyReturnsFlag1 = nextFuzzyReturnsFlag(isFuzzyReturn, fuzzyReturnsFlag);
            usedIndexes.add(index);
            used.add(dtHeaders.get(index));
            var reachedByHeader = bruteForceHeaders(column + dtHeader.getWidth(),
                    usedParameterIndexesTo,
                    numberOfReturns1,
                    fuzzyReturnsFlag1,
                    counter + 1);
            usedIndexes.removeLast();
            used.removeLast();
            return reachedByHeader;
        }

        private static boolean isFuzzyReturn(DTHeader dtHeader) {
            return dtHeader instanceof FuzzyDTHeader fuzzyDTHeader && fuzzyDTHeader.isReturn();
        }

        private static int nextNumberOfReturns(DTHeader dtHeader, boolean isFuzzyReturn, int numberOfReturns) {
            return dtHeader.isReturn() && !isFuzzyReturn ? numberOfReturns + 1 : numberOfReturns;
        }

        private static int nextFuzzyReturnsFlag(boolean isFuzzyReturn, int fuzzyReturnsFlag) {
            return isFuzzyReturn && fuzzyReturnsFlag != 1 ? fuzzyReturnsFlag + 1 : fuzzyReturnsFlag;
        }

        /**
         * Builds the matrix of the headers that can be used together in a fit, and groups the headers by their first
         * column.
         */
        private static boolean[][] buildCompatibilityMatrix(List<DTHeader> dtHeaders,
                                                            Map<Integer, List<Integer>> columnToIndex) {
            boolean[][] matrix = new boolean[dtHeaders.size()][dtHeaders.size()];
            for (var i = 0; i < dtHeaders.size(); i++) {
                for (var j = 0; j < dtHeaders.size(); j++) {
                    matrix[i][j] = true;
                }
            }
            for (var i = 0; i < dtHeaders.size(); i++) {
                List<Integer> indexes = columnToIndex.computeIfAbsent(dtHeaders.get(i).getColumn(), e -> new ArrayList<>());
                indexes.add(i);
                for (var j = i; j < dtHeaders.size(); j++) {
                    if (i == j || !isCompatibleHeaders(dtHeaders.get(i), dtHeaders.get(j))) {
                        matrix[i][j] = false;
                        matrix[j][i] = false;
                    }
                }
            }
            return matrix;
        }

        private static boolean isCompatibleHeaders(DTHeader a, DTHeader b) {
            var c1 = a.getColumn();
            var c2 = a.getColumn() + a.getWidth() - 1;
            var d1 = b.getColumn();
            var d2 = b.getColumn() + b.getWidth() - 1;

            if (intersects(d1, d2, c1, c2)) {
                return false;
            }

            if (mustPrecede(a, b) && c1 >= d1) {
                return false;
            }
            if (mustPrecede(b, a) && d1 >= c1) {
                return false;
            }

            if (a instanceof FuzzyDTHeader a1 && b instanceof FuzzyDTHeader b1 && isConflictingFuzzyHeaders(a1, b1)) {
                return false;
            }
            if (a instanceof DeclaredDTHeader a1 && b instanceof DeclaredDTHeader b1) {
                return !a1.getMatchedDefinition()
                        .getDtColumnsDefinition()
                        .equals(b1.getMatchedDefinition().getDtColumnsDefinition());
            }
            return true;
        }

        /**
         * Checks whether the first header is of a kind that goes before the kind of the second one in a table.
         */
        private static boolean mustPrecede(DTHeader first, DTHeader second) {
            return first.isRule() && second.isCondition() || first.isCondition() && second.isAction() || first
                    .isAction() && second.isReturn() || first.isCondition() && second.isReturn();
        }

        private static boolean isConflictingFuzzyHeaders(FuzzyDTHeader a1, FuzzyDTHeader b1) {
            if (a1.isMethodParameterUsed() && b1.isMethodParameterUsed() && a1.isCondition() && b1
                    .isCondition() && a1.getMethodParameterIndex() == b1.getMethodParameterIndex() && Arrays
                    .deepEquals(a1.getFieldsChain(), b1.getFieldsChain())) {
                return true;
            }

            if (a1.isReturn() && b1.isReturn() && fieldsChainsIsCrossed(a1.getFieldsChain(), b1.getFieldsChain())) {
                return true;
            }

            return !isSameKind(a1, b1) && a1.getTopColumn() == b1.getTopColumn();
        }

        private static boolean isSameKind(DTHeader a1, DTHeader b1) {
            return a1.isHCondition() && b1.isHCondition() || a1.isCondition() && b1.isCondition() || a1.isAction() && b1
                    .isAction() || a1.isReturn() && b1.isReturn();
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
        return indexes.stream().map(fits::get).collect(Collectors.toCollection(ArrayList::new));
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
        return indexes.stream().map(fits::get).collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<List<DTHeader>> filterHeadersByMatchType(DecisionTable decisionTable,
                                                                 List<List<DTHeader>> fits) {
        resolveConflictsInDeclaredDtHeaders(decisionTable, fits);
        MatchType[] matchTypes = MatchType.values();
        Arrays.sort(matchTypes, Comparator.comparingInt(MatchType::getPriority));
        for (MatchType type : matchTypes) {
            fits = filterHeadersByMax(fits,
                    e -> e.stream()
                            .filter(DeclaredDTHeader.class::isInstance)
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
                .toList();
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
        boolean[] f = findIntersectedDeclaredDtHeaders(dtHeaders);
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

    private static boolean[] findIntersectedDeclaredDtHeaders(List<DTHeader> dtHeaders) {
        boolean[] f = new boolean[dtHeaders.size()];
        Arrays.fill(f, false);
        for (var i = 0; i < dtHeaders.size() - 1; i++) {
            for (var j = i + 1; j < dtHeaders.size(); j++) {
                if (dtHeaders.get(i) instanceof DeclaredDTHeader d1
                        && dtHeaders.get(j) instanceof DeclaredDTHeader d2
                        && !d1.isHCondition() && !d2.isHCondition()
                        && !(d1.getColumn() == d2.getColumn() && d1.getWidth() == d2.getWidth())
                        && intersects(d1.getColumn(),
                                d1.getColumn() + d1.getWidth() - 1,
                                d2.getColumn(),
                                d2.getColumn() + d2.getWidth() - 1)) {
                    f[i] = true;
                    f[j] = true;
                }
            }
        }
        return f;
    }

    private static List<List<DTHeader>> fitFuzzyDtHeaders(List<List<DTHeader>> fits) {
        fits = filterHeadersByMax(fits,
                e -> e.stream()
                        .filter(FuzzyDTHeader.class::isInstance)
                        .map(x -> (FuzzyDTHeader) x)
                        .mapToInt(x -> x.getFuzzyResult().getFoundTokensCount())
                        .sum(),
                e -> true);
        fits = filterHeadersByMin(fits,
                e -> e.stream()
                        .filter(FuzzyDTHeader.class::isInstance)
                        .map(x -> (FuzzyDTHeader) x)
                        .mapToInt(x -> x.getFuzzyResult().getMissedTokensCount())
                        .sum(),
                e -> true);
        fits = filterHeadersByMin(fits,
                e -> e.stream()
                        .filter(FuzzyDTHeader.class::isInstance)
                        .map(x -> (FuzzyDTHeader) x)
                        .mapToInt(x -> x.getFuzzyResult().getToken().getDistance())
                        .sum(),
                e -> true);
        fits = filterHeadersByMin(fits,
                e -> e.stream()
                        .filter(FuzzyDTHeader.class::isInstance)
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
        var headersFitSearch = new HeadersFitSearch(originalTable,
                dtHeaders,
                lastColumn,
                firstColumnHeight,
                numberOfParameters,
                numberOfHConditions);
        headersFitSearch.bruteForceHeaders(0, new HashSet<>(), 0, 0, 0);
        List<List<DTHeader>> fits = headersFitSearch.fits;
        var failedToFit = headersFitSearch.failedToFit;

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
                        .filter(DeclaredDTHeader.class::isInstance)
                        .mapToLong(
                                x -> ((DeclaredDTHeader) x).getMatchedDefinition().getDtColumnsDefinition().getNumberOfTitles())
                        .sum(),
                all);

        fits = filterBasedOnDeclaredDtHeaders(fits);

        if (numberOfHConditions == 0) {
            // Prefer full matches with return headers
            fits = fits.stream().filter(e -> e.stream().anyMatch(DTHeader::isReturn)).toList();
        } else {
            // Lookup table with no returns columns
            fits = fits.stream().filter(e -> e.stream().noneMatch(DTHeader::isReturn)).toList();
        }

        // matches with min returns
        fits = filterHeadersByMin(fits, DecisionTableHelper::countReturns, all);

        fits = filterHeadersByMin(fits,
                e -> e.stream().filter(e1 -> e1 instanceof UnmatchedDtHeader && !e1.isHCondition()).count(),
                all);

        fits = filterHeadersByMin(fits,
                e -> e.stream()
                        .filter(DeclaredDTHeader.class::isInstance)
                        .map(x -> (DeclaredDTHeader) x)
                        .mapToLong(x -> x.getMatchedDefinition().isMayHaveCompilationErrors() ? 1 : 0)
                        .sum(),
                e -> e.stream().anyMatch(DeclaredDTHeader.class::isInstance));

        fits = filterHeadersByMatchType(decisionTable, fits);

        fits = filterHeadersByMax(fits,
                e -> e.stream().flatMapToInt(c -> Arrays.stream(c.getMethodParameterIndexes())).distinct().count(),
                e -> e.stream().anyMatch(x -> x.isCondition() && x instanceof DeclaredDTHeader));

        fits = filterHeadersByMin(fits,
                e -> e.stream().filter(SimpleReturnDTHeader.class::isInstance).count(),
                e -> e.stream().anyMatch(DTHeader::isReturn));

        fits = fitFuzzyDtHeaders(fits);

        fits = removeDuplicates(fits);

        if (numberOfHConditions == 0 && fits.isEmpty()) {
            throw new DTUnmatchedCompilationException(
                    buildNoMatchMessage(originalTable, dtHeaders, failedToFit, firstColumnHeight));
        }

        if (!fits.isEmpty()) {
            return selectBestFit(tableSyntaxNode, fits, bindingContext);
        }

        return Collections.emptyList();
    }

    private static String buildNoMatchMessage(ILogicalTable originalTable,
                                              List<DTHeader> dtHeaders,
                                              Set<Integer> failedToFit,
                                              int firstColumnHeight) {
        var c = failedToFit.stream().mapToInt(e -> dtHeaders.get(e).getColumn()).max();
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
        return message.toString();
    }

    private static List<DTHeader> selectBestFit(TableSyntaxNode tableSyntaxNode,
                                                List<List<DTHeader>> fits,
                                                IBindingContext bindingContext) {
        final Predicate<List<DTHeader>> all = e -> true;
        var bestFits = fits;
        if (bestFits.size() > 1) {
            warnAboutAmbiguousFits(tableSyntaxNode, bestFits, bindingContext);
        }
        // Select with min returns/actions/conditions
        bestFits = filterHeadersByMin(bestFits, e -> e.stream().filter(DTHeader::isReturn).count(), all);
        bestFits = filterHeadersByMin(bestFits, e -> e.stream().filter(DTHeader::isAction).count(), all);
        bestFits = filterHeadersByMin(bestFits, e -> e.stream().filter(DTHeader::isCondition).count(), all);
        if (bestFits.stream().anyMatch(FuzzyDTHeader.class::isInstance)) {
            bestFits = filterHeadersByMax(bestFits,
                    e -> e.stream()
                            .filter(FuzzyDTHeader.class::isInstance)
                            .mapToLong(e1 -> (long) ((FuzzyDTHeader) e1).getFuzzyResult()
                                    .getAcceptableSimilarity() * 1000000L)
                            .sum() / e.stream().filter(FuzzyDTHeader.class::isInstance).count(),
                    all);
        }
        return bestFits.getFirst();
    }

    private static void warnAboutAmbiguousFits(TableSyntaxNode tableSyntaxNode,
                                               List<List<DTHeader>> fits,
                                               IBindingContext bindingContext) {
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
        var ret = fits.stream()
                .filter(DecisionTableHelper::isExternalParametersDeclared)
                .collect(Collectors.toCollection(ArrayList::new));
        return ret.isEmpty() ? fits : ret;
    }

    /**
     * Checks that every external parameter of the declared headers of the fit is a parameter of one of them.
     */
    private static boolean isExternalParametersDeclared(List<DTHeader> fit) {
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
        for (String externalParameter : externalParameters) {
            if (!parameters.containsKey(toLowerCase(externalParameter))) {
                return false;
            }
        }
        return true;
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
            var verticalTitles = findVerticalTitles(originalTable, ret, firstColumnHeight);
            if (verticalTitles != null) {
                return verticalTitles;
            }
        }

        return Pair.of(ret, WithVerticalTitles.NO);
    }

    /**
     * Looks for the title that holds the titles of the vertical conditions next to the horizontal ones.
     *
     * @return the first column of the horizontal conditions and the way the vertical titles are placed, or
     * {@code null} when there is no such title
     */
    private static Pair<Integer, WithVerticalTitles> findVerticalTitles(ILogicalTable originalTable,
                                                                        int ret,
                                                                        int firstColumnHeight) {
        var begin = Math.max(ret, 0);
        int end = begin > 0 ? begin + 1 : originalTable.getSource().getWidth();
        var i = begin;
        while (i < end) {
            var value = originalTable.getSource().getCell(i, firstColumnHeight - 1).getStringValue();
            if (StringUtils.isNotBlank(value) && value.contains(HORIZONTAL_VERTICAL_CONDITIONS_SPLITTER)) {
                var verticalTitles = getVerticalTitles(originalTable, i, firstColumnHeight, value);
                if (verticalTitles != null) {
                    return verticalTitles;
                }
            }
            i = i + originalTable.getSource().getCell(i, 0).getWidth();
        }
        return null;
    }

    private static Pair<Integer, WithVerticalTitles> getVerticalTitles(ILogicalTable originalTable,
                                                                       int i,
                                                                       int firstColumnHeight,
                                                                       String value) {
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
        return null;
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

        var twoColumnsForReturn = isTwoColumnsForReturn(tableSyntaxNode, decisionTable);

        final var xlsDefinitions = ((XlsModuleOpenClass) decisionTable.getDeclaringClass())
                .getXlsDefinitions();

        var lastColumn = originalTable.getSource().getWidth();
        if (numberOfHConditions > 0 && firstColumnForHCondition > 0) {
            lastColumn = firstColumnForHCondition;
        }

        String returnTokenString = getReturnTokenString(fuzzyContext);
        var layout = new TitlesLayout(originalTable,
                numberOfColumnsUnderTitleCounter,
                numberOfHConditions,
                firstColumnHeight,
                firstColumnForHCondition,
                withVerticalTitles);
        var dtHeaders = new ArrayList<DTHeader>();
        if (isSmart) {
            matchColumnsWithDtColumnsDefinitions(decisionTable,
                    layout,
                    xlsDefinitions,
                    lastColumn,
                    dtHeaders,
                    bindingContext);
        }
        var strongDtHeaders = findStrongDtHeaders(originalTable, dtHeaders);
        var i = 0;
        var column = 0;
        SimpleReturnDTHeader lastSimpleReturnDTHeader = null;
        while (column < lastColumn) {
            var w = originalTable.getSource().getCell(column, 0).getWidth();
            if (!conflictsWithStrongDtHeader(strongDtHeaders,
                    withVerticalTitles,
                    firstColumnForHCondition,
                    column,
                    w)) {
                if (isSmart) {
                    lastSimpleReturnDTHeader = matchSmartTableColumn(decisionTable,
                            layout,
                            fuzzyContext,
                            column,
                            lastColumn,
                            dtHeaders,
                            returnTokenString);
                } else {
                    matchSimpleTableColumn(decisionTable, layout, fuzzyContext, column, lastColumn, dtHeaders, i);
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
            return addHConditionHeaders(tableSyntaxNode, decisionTable, layout, fit, bindingContext);
        } else {
            return fit;
        }

    }

    /**
     * The layout of the column titles of a table.
     */
    private record TitlesLayout(ILogicalTable originalTable,
                                NumberOfColumnsUnderTitleCounter numberOfColumnsUnderTitleCounter,
                                int numberOfHConditions,
                                int firstColumnHeight,
                                int firstColumnForHCondition,
                                WithVerticalTitles withVerticalTitles) {
    }

    private static String getReturnTokenString(FuzzyContext fuzzyContext) {
        return fuzzyContext != null && fuzzyContext.isFuzzySupportsForReturnType() ? OpenLFuzzyUtils
                .toTokenString(fuzzyContext.getFuzzyReturnType().getName()) : null;
    }

    private static void matchColumnsWithDtColumnsDefinitions(DecisionTable decisionTable,
                                                             TitlesLayout layout,
                                                             XlsDefinitions xlsDefinitions,
                                                             int lastColumn,
                                                             List<DTHeader> dtHeaders,
                                                             IBindingContext bindingContext) {
        var originalTable = layout.originalTable();
        var column = 0;
        while (column < lastColumn) {
            var w = originalTable.getSource().getCell(column, 0).getWidth();
            matchWithDtColumnsDefinitions(decisionTable, layout, column, xlsDefinitions, dtHeaders, bindingContext);
            column = column + w;
        }
    }

    /**
     * Matches the title of a column of a smart table with the input parameters and with the return type.
     *
     * @return the header that returns the values of the column when the table has no horizontal conditions,
     * otherwise {@code null}
     */
    private static SimpleReturnDTHeader matchSmartTableColumn(DecisionTable decisionTable,
                                                              TitlesLayout layout,
                                                              FuzzyContext fuzzyContext,
                                                              int column,
                                                              int lastColumn,
                                                              List<DTHeader> dtHeaders,
                                                              String returnTokenString) {
        var originalTable = layout.originalTable();
        var numberOfHConditions = layout.numberOfHConditions();
        var firstColumnHeight = layout.firstColumnHeight();
        var w = originalTable.getSource().getCell(column, 0).getWidth();
        var row = 0;
        var fuzzyHeaders = matchWithFuzzySearch(decisionTable,
                layout,
                fuzzyContext,
                column,
                lastColumn,
                dtHeaders,
                false);
        SimpleReturnDTHeader lastSimpleReturnDTHeader = null;
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
                        .noneMatch(DTHeader::isReturn) && layout.numberOfColumnsUnderTitleCounter()
                        .get(column) == 1 && (column + w >= lastColumn || columnWithFormulas(originalTable,
                        firstColumnHeight,
                        column))) {
                    dtHeaders.add(lastSimpleReturnDTHeader);
                }
            } else {
                dtHeaders.add(lastSimpleReturnDTHeader);
            }
        }
        return lastSimpleReturnDTHeader;
    }

    /**
     * Matches a column of a simple table, whose columns follow the order of the input parameters.
     */
    private static void matchSimpleTableColumn(DecisionTable decisionTable,
                                               TitlesLayout layout,
                                               FuzzyContext fuzzyContext,
                                               int column,
                                               int lastColumn,
                                               List<DTHeader> dtHeaders,
                                               int i) {
        var numberOfParameters = decisionTable.getSignature().getNumberOfParameters();
        var numberOfHConditions = layout.numberOfHConditions();
        var w = layout.originalTable().getSource().getCell(column, 0).getWidth();
        var row = 0;
        if (numberOfHConditions == 0 && i >= numberOfParameters) {
            matchWithFuzzySearch(decisionTable,
                    layout,
                    fuzzyContext,
                    column,
                    lastColumn,
                    dtHeaders,
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

    /**
     * Adds the horizontal conditions of a lookup table: the columns after the matched ones get the input
     * parameters that no header uses.
     */
    private static List<DTHeader> addHConditionHeaders(TableSyntaxNode tableSyntaxNode,
                                                       DecisionTable decisionTable,
                                                       TitlesLayout layout,
                                                       List<DTHeader> fit,
                                                       IBindingContext bindingContext) {
        var originalTable = layout.originalTable();
        var numberOfHConditions = layout.numberOfHConditions();
        var numberOfParameters = decisionTable.getSignature().getNumberOfParameters();
        var maxColumnMatched = fit.stream()
                .filter(e -> e.isCondition() && !e.isHCondition() || e.isAction())
                .mapToInt(e -> e.getColumn() + e.getWidth())
                .max()
                .orElse(0);
        var column = originalTable.getSource().getWidth() - 1;
        while (column > maxColumnMatched && calculateRowsCount(originalTable,
                column - 1,
                layout.firstColumnHeight()) == numberOfHConditions) {
            column--;
        }

        var fitHCond = new ArrayList<DTHeader>(fit);
        addUnmatchedHConditionHeaders(fitHCond, layout.numberOfColumnsUnderTitleCounter(), maxColumnMatched, column);

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
        assignFreeParametersToHConditions(decisionTable,
                fitHCond,
                parameterIsUsed,
                column,
                numberOfHConditions - hConditionsMatched);
        return Collections.unmodifiableList(fitHCond);
    }

    private static void addUnmatchedHConditionHeaders(List<DTHeader> fitHCond,
                                                      NumberOfColumnsUnderTitleCounter numberOfColumnsUnderTitleCounter,
                                                      int maxColumnMatched,
                                                      int column) {
        for (var c = maxColumnMatched; c < column; c++) {
            var num = numberOfColumnsUnderTitleCounter.get(c);
            var col1 = c;
            for (var j = 0; j < num; j++) {
                var width = numberOfColumnsUnderTitleCounter.getWidth(c, j);
                fitHCond.add(new UnmatchedDtHeader(StringUtils.EMPTY, col1, 0, width, false));
                col1 = col1 + width;
            }
        }
    }

    private static void assignFreeParametersToHConditions(DecisionTable decisionTable,
                                                          List<DTHeader> fitHCond,
                                                          boolean[] parameterIsUsed,
                                                          int column,
                                                          long numberOfUnmatchedHConditions) {
        var numberOfParameters = parameterIsUsed.length;
        var j = 0;
        var w = 0;
        var c = 0;
        var len = fitHCond.size();
        while (w < numberOfParameters && j < numberOfUnmatchedHConditions) {
            if (!parameterIsUsed[w]) {
                c = findUnmatchedHCondition(fitHCond, c, len);
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
    }

    private static int findUnmatchedHCondition(List<DTHeader> fitHCond, int from, int len) {
        var c = from;
        while (c < len) {
            var dth = fitHCond.get(c);
            if (dth instanceof UnmatchedDtHeader && dth.isHCondition()) {
                break;
            }
            c++;
        }
        return c;
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
                                                      TitlesLayout layout,
                                                      int column,
                                                      XlsDefinitions definitions,
                                                      List<DTHeader> dtHeaders,
                                                      IBindingContext bindingContext) {
        var originalTable = layout.originalTable();
        var firstColumnForHCondition = layout.firstColumnForHCondition();
        var withVerticalTitles = layout.withVerticalTitles();
        var parseAsHorizontalVerticalTitle = isHorizontalVerticalTitle(layout, column);
        var w0 = column + originalTable.getSource().getCell(column, 0).getWidth();
        var skipNextColumn = w0 + originalTable.getSource()
                .getCell(w0, 0)
                .getWidth() == firstColumnForHCondition && (WithVerticalTitles.EMPTY_COLUMN
                .equals(withVerticalTitles) || WithVerticalTitles.MERGED_COLUMN.equals(withVerticalTitles));
        if (parseAsHorizontalVerticalTitle || originalTable.getSource()
                .getCell(column, 0)
                .getHeight() == layout.firstColumnHeight()) {
            for (DTColumnsDefinition definition : definitions.getDtColumnsDefinitions()) {
                var titlesMatch = new DefinitionTitlesMatch(definition, layout, column, parseAsHorizontalVerticalTitle);
                titlesMatch.matchTitles();
                var dtHeader = titlesMatch.toDtHeader(decisionTable, skipNextColumn, bindingContext);
                if (dtHeader != null) {
                    dtHeaders.add(dtHeader);
                }
            }
        }
        if (!WithVerticalTitles.NO.equals(withVerticalTitles) && column + originalTable.getSource()
                .getCell(column, 0)
                .getWidth() == firstColumnForHCondition) {
            for (DTColumnsDefinition definition : definitions.getDtColumnsDefinitions()) {
                matchWithVerticalTitles(decisionTable, layout, column, definition, dtHeaders, bindingContext);
            }
        }
    }

    /**
     * Checks whether the title of the column holds the titles of the vertical conditions next to the horizontal
     * ones, split by a slash.
     */
    private static boolean isHorizontalVerticalTitle(TitlesLayout layout, int column) {
        return WithVerticalTitles.SLASH_IN_TITLE
                .equals(layout.withVerticalTitles()) && column + layout.originalTable()
                .getSource()
                .getCell(column, 0)
                .getWidth() == layout.firstColumnForHCondition();
    }

    /**
     * Matches the titles of a columns definition with the titles of the columns that follow each other from the
     * given column.
     */
    private static final class DefinitionTitlesMatch {
        private final DTColumnsDefinition definition;
        private final TitlesLayout layout;
        private final int column;
        private final Set<String> titles;
        private boolean parseAsHorizontalVerticalTitle;
        private Triple<String, String, Integer> extractedTitle;
        private Triple<String, String, Integer> lastExtractedTitle;
        private int i;
        private int x;
        private IParameterDeclaration[][] columnParameters;
        private boolean f1;
        private boolean f2;
        private boolean g;

        private DefinitionTitlesMatch(DTColumnsDefinition definition,
                                      TitlesLayout layout,
                                      int column,
                                      boolean parseAsHorizontalVerticalTitle) {
            this.definition = definition;
            this.layout = layout;
            this.column = column;
            this.parseAsHorizontalVerticalTitle = parseAsHorizontalVerticalTitle;
            titles = new HashSet<>(definition.getTitles());
            extractedTitle = extractTokenizedVerticalTitleString(layout.originalTable(),
                    column,
                    layout.firstColumnHeight(),
                    parseAsHorizontalVerticalTitle);
            lastExtractedTitle = extractedTitle;
            x = column;
            matchUnderColumns();
        }

        /**
         * Checks the parameters of the definition for the titles extracted from the column against the number of
         * columns under the title.
         */
        private void matchUnderColumns() {
            var numberOfColumnsUnderTitle = layout.numberOfColumnsUnderTitleCounter().get(x);
            f1 = isMatchedByUnderColumns(definition.getParameters(extractedTitle.getLeft()),
                    numberOfColumnsUnderTitle);
            f2 = !Objects.equals(extractedTitle.getLeft(),
                    extractedTitle.getMiddle()) && isMatchedByUnderColumns(
                    definition.getParameters(extractedTitle.getMiddle()),
                    numberOfColumnsUnderTitle);
        }

        private void matchTitles() {
            var originalTable = layout.originalTable();
            while (!titles
                    .isEmpty() && ((layout.numberOfHConditions() > 0 && x < layout
                    .firstColumnForHCondition() || x < originalTable
                    .getSource()
                    .getWidth()) && (f1 && titles.contains(
                    extractedTitle.getLeft()) || f2 && titles.contains(extractedTitle.getMiddle())))) {
                matchTitle();
            }
        }

        private void matchTitle() {
            var originalTable = layout.originalTable();
            g = false;
            if (f1) {
                titles.remove(extractedTitle.getLeft());
            } else {
                titles.remove(extractedTitle.getMiddle());
            }
            matchColumnParameters();
            i = i + 1;
            var w = originalTable.getSource().getCell(x, 0).getWidth();
            x = x + w;
            lastExtractedTitle = extractedTitle;
            extractedTitle = extractTokenizedVerticalTitleString(originalTable,
                    x,
                    layout.firstColumnHeight(),
                    parseAsHorizontalVerticalTitle);
            parseAsHorizontalVerticalTitle = isHorizontalVerticalTitle(layout, column);
            matchUnderColumns();
        }

        private void matchColumnParameters() {
            for (String s : definition.getTitles()) {
                var matchedByLeft = f1 && s.equals(extractedTitle.getLeft());
                if (matchedByLeft || f2 && s.equals(extractedTitle.getMiddle())) {
                    g = matchedByLeft;
                    if (columnParameters == null) {
                        columnParameters = new IParameterDeclaration[definition.getNumberOfTitles()][];
                    }
                    var matchedTitle = matchedByLeft ? extractedTitle.getLeft() : extractedTitle.getMiddle();
                    columnParameters[i] = definition.getParameters(matchedTitle)
                            .toArray(IParameterDeclaration.EMPTY);
                    break;
                }
            }
        }

        /**
         * Creates the header of the definition when all its titles are matched.
         *
         * @return the header, or {@code null} when a title is not matched or the definition does not fit the table
         */
        private DeclaredDTHeader toDtHeader(DecisionTable decisionTable,
                                            boolean skipNextColumn,
                                            IBindingContext bindingContext) {
            if (titles.isEmpty()) {
                MatchedDefinition matchedDefinition = matchByDTColumnDefinition(decisionTable,
                        definition,
                        layout.numberOfHConditions(),
                        bindingContext);
                if (matchedDefinition != null) {
                    return new DeclaredDTHeader(
                            matchedDefinition.getUsedMethodParameterIndexes(),
                            definition,
                            columnParameters,
                            column,
                            lastExtractedTitle.getRight(),
                            x - column + (skipNextColumn ? layout.originalTable()
                                    .getSource()
                                    .getCell(x, 0)
                                    .getWidth() : 0),
                            x - column,
                            matchedDefinition,
                            false,
                            g && parseAsHorizontalVerticalTitle);
                }
            }
            return null;
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
    }

    /**
     * Matches a definition with a single title with the titles of the horizontal conditions written in the column
     * of the vertical titles.
     */
    private static void matchWithVerticalTitles(DecisionTable decisionTable,
                                                TitlesLayout layout,
                                                int column,
                                                DTColumnsDefinition definition,
                                                List<DTHeader> dtHeaders,
                                                IBindingContext bindingContext) {
        if (definition.getNumberOfTitles() != 1) {
            return;
        }
        var originalTable = layout.originalTable();
        var firstColumnHeight = layout.firstColumnHeight();
        var numberOfHConditions = layout.numberOfHConditions();
        var definitionTitle = definition.getTitles().iterator().next();
        var h = 0;
        var x = 0;
        while (h < firstColumnHeight) {
            var h0 = originalTable.getSource().getCell(column, h).getHeight();
            var title = originalTable.getSource().getCell(column, h).getStringValue();
            if (h + h0 >= firstColumnHeight && WithVerticalTitles.SLASH_IN_TITLE
                    .equals(layout.withVerticalTitles())) {
                title = title.substring(title.indexOf(HORIZONTAL_VERTICAL_CONDITIONS_SPLITTER) + 1).trim();
            }
            if (x < numberOfHConditions) {
                title = OpenLFuzzyUtils.toTokenString(title);
                if (Objects.equals(title, definitionTitle)) {
                    var vDtHeader = createVerticalTitleDtHeader(decisionTable,
                            definition,
                            numberOfHConditions,
                            title,
                            column + originalTable.getSource().getCell(column, 0).getWidth() + x,
                            h,
                            bindingContext);
                    if (vDtHeader != null) {
                        dtHeaders.add(vDtHeader);
                        break;
                    }
                }
            }
            h = h + h0;
            x++;
        }
    }

    /**
     * Creates the header of a horizontal condition whose title is written in the column of the vertical titles.
     *
     * @return the header, or {@code null} when the definition does not fit the table
     */
    private static DeclaredDTHeader createVerticalTitleDtHeader(DecisionTable decisionTable,
                                                                DTColumnsDefinition definition,
                                                                int numberOfHConditions,
                                                                String title,
                                                                int column,
                                                                int h,
                                                                IBindingContext bindingContext) {
        MatchedDefinition matchedDefinition = matchByDTColumnDefinition(decisionTable,
                definition,
                numberOfHConditions,
                bindingContext);
        if (matchedDefinition != null) {
            IParameterDeclaration[][] columnParameters = new IParameterDeclaration[1][];
            columnParameters[0] = definition.getParameters(title)
                    .toArray(IParameterDeclaration.EMPTY);
            return new DeclaredDTHeader(
                    matchedDefinition.getUsedMethodParameterIndexes(),
                    definition,
                    columnParameters,
                    column,
                    h,
                    1,
                    1,
                    matchedDefinition,
                    true,
                    false);
        }
        return null;
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

    private static class CellValue {
        @Getter
        String value;
        @Getter
        ICell cell;

        public CellValue(ICell cell) {
            this.value = textOf(cell);
            this.cell = cell;
        }

        /**
         * Gives the text of the cell to guess the type of a column by.
         * <p>
         * A whole number beyond the {@code int} range is given in plain digits, as {@code 2147483648} rather than
         * {@code 2.147483648E9}, so that it is read as a value of the integer type of the condition.
         */
        private static @Nullable String textOf(ICell cell) {
            if (cell.getObjectValue() instanceof Double number) {
                var wholeNumber = RuleRowHelper.toWholeNumber(number);
                if (wholeNumber != null) {
                    return wholeNumber.toString();
                }
            }
            return cell.getStringValue();
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

        var typeGuess = new ConditionColumnTypeGuess(decisionTable, condition, type, bindingContext);
        typeGuess.checkValues(decisionValues, width, skip, numberOfColumnsForCondition);
        var simpleType = typeGuess.getSimpleType(module, cache);
        if (simpleType != null) {
            return simpleType;
        }
        typeGuess.checkRanges();
        return typeGuess.getType(module, cache);
    }

    /**
     * Guesses the type of the values of a condition column from its cells: values of the type of the condition,
     * arrays of them, or ranges.
     */
    private static final class ConditionColumnTypeGuess {
        private final DecisionTable decisionTable;
        private final DTHeader condition;
        private final IOpenClass type;
        private final IBindingContext bindingContext;

        private boolean isAllParsableAsRangeFlag = true;
        private boolean isAllLikelyNotRangeFlag = true;
        private boolean isAllElementsLikelyNotRangeFlag = true;
        private boolean isAllParsableAsSingleFlag = true;
        private boolean isAllParsableAsDomainFlag = true;
        private boolean isAllParsableAsDomainArrayFlag = true;
        private boolean isAllParsableAsArrayFlag = true;
        private boolean arraySeparatorFoundFlag;

        private boolean isNotParsableAsSingleRangeButParsableAsRangesArrayFlag;
        private boolean zeroStartedNumbersFoundFlag;

        private final boolean isIntType;
        private final boolean isDoubleType;
        private final boolean isCharType;
        private final boolean isDateType;
        private final boolean isStringType;
        private final boolean isRangeType;

        private boolean canMadeDecisionAboutSingle = true;

        private boolean[][] h;
        private boolean isMoreThanOneColumnIsUsed;
        private int skip;
        private int width;
        private final Map<Integer, Set<CellValue>> valuesMap = new HashMap<>();

        private ConditionColumnTypeGuess(DecisionTable decisionTable,
                                         DTHeader condition,
                                         IOpenClass type,
                                         IBindingContext bindingContext) {
            this.decisionTable = decisionTable;
            this.condition = condition;
            this.type = type;
            this.bindingContext = bindingContext;
            this.isIntType = INT_TYPES.contains(type.getInstanceClass());
            this.isDoubleType = DOUBLE_TYPES.contains(type.getInstanceClass());
            this.isCharType = CHAR_TYPES.contains(type.getInstanceClass());
            this.isDateType = DATE_TYPES.contains(type.getInstanceClass());
            this.isStringType = STRING_TYPES.contains(type.getInstanceClass());
            this.isRangeType = RANGE_TYPES.contains(type.getInstanceClass());
        }

        /**
         * Reads the values of the rules and checks the formulas, the constants and the values that are single
         * values of the type of the condition.
         */
        private void checkValues(IGridTable decisionValues, int width, int skip, int numberOfColumnsForCondition) {
            this.width = width;
            this.skip = skip;
            h = new boolean[width][numberOfColumnsForCondition];
            for (var i = 0; i < width; i++) {
                Arrays.fill(h[i], true);
            }

            isMoreThanOneColumnIsUsed = numberOfColumnsForCondition > 1;

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
                checkCellValues(valueNum, values);
            }
        }

        private void checkCellValues(int valueNum, Set<CellValue> values) {
            var cellNum = -1;
            for (CellValue cellValue : values) {
                cellNum++;
                if (cellValue == null) {
                    continue;
                }
                var value = cellValue.getValue();
                var formula = RuleRowHelper.isFormula(value) && !isRangeType;
                ConstantOpenField constantOpenField = formula ? null
                        : RuleRowHelper.findConstantField(bindingContext, value);

                if (formula) {
                    checkFormula(value);
                    h[valueNum][cellNum] = false;
                } else if (constantOpenField != null) {
                    checkTypeOfValue(constantOpenField.getType());
                    h[valueNum][cellNum] = false;
                    canMadeDecisionAboutSingle = canMadeDecisionAboutSingle && type.equals(constantOpenField.getType());
                } else {
                    checkValue(value);
                }
            }
        }

        private void checkFormula(String value) {
            try {
                bindingContext.pushErrors();
                bindingContext.pushMessages();
                var expressionCellSourceCodeModule = new StringSourceCodeModule(
                        value.substring(value.indexOf("=")).trim(),
                        null);
                CompositeMethod compositeMethod = OpenLManager.makeMethodWithUnknownType(
                        bindingContext.getOpenL(),
                        expressionCellSourceCodeModule,
                        RandomStringUtils.secure().next(16, true, false),
                        decisionTable.getSignature(),
                        decisionTable.getDeclaringClass(),
                        bindingContext);
                var cellType = compositeMethod.getType();
                canMadeDecisionAboutSingle = canMadeDecisionAboutSingle && type.equals(cellType);
                checkTypeOfValue(cellType);

            } finally {
                bindingContext.popMessages();
                bindingContext.popErrors();
            }
        }

        /**
         * Checks the type of a value that is computed by a formula or taken from a constant.
         */
        private void checkTypeOfValue(IOpenClass valueType) {
            if (valueType.isArray() && RANGE_TYPES
                    .contains(valueType.getComponentClass().getInstanceClass())) {
                isAllParsableAsArrayFlag = false;
                isNotParsableAsSingleRangeButParsableAsRangesArrayFlag = true;
                isAllLikelyNotRangeFlag = false;
                isAllElementsLikelyNotRangeFlag = false;
            }
            if (RANGE_TYPES.contains(valueType.getInstanceClass())) {
                isAllParsableAsArrayFlag = false;
                isAllLikelyNotRangeFlag = false;
                isAllElementsLikelyNotRangeFlag = false;
            }
            if (valueType.isArray()) {
                isAllParsableAsSingleFlag = false;
                isNotParsableAsSingleRangeButParsableAsRangesArrayFlag = true;
            }
        }

        private void checkValue(String value) {
            if (!arraySeparatorFoundFlag && ArraySplitter.isArray(value)) {
                arraySeparatorFoundFlag = true;
            }
            try {
                if ((isIntType || isDoubleType || isCharType) && isAllParsableAsSingleFlag
                        && !parsableAs(value, type.getInstanceClass(), bindingContext)) {
                    isAllParsableAsSingleFlag = false;
                } else if (isStringType) {
                    checkDomainValue(value);
                }
            } catch (Exception ignored) {
                // guessing the column type is a heuristic: a value the checks fail on does not affect it
            }
        }

        @SuppressWarnings("unchecked")
        private void checkDomainValue(String value) {
            if (isAllParsableAsDomainFlag && (type.getDomain() == null
                    || !((IDomain<String>) type.getDomain()).selectObject(value))) {
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

        /**
         * Returns the type when all values are single values or arrays of values of the type of the condition.
         *
         * @return the type, or {@code null} when the values have to be checked as ranges
         */
        private Triple<String[], IOpenClass, String> getSimpleType(
                XlsModuleOpenClass module,
                IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache) {
            if (canMadeDecisionAboutSingle) {
                if ((isIntType || isDoubleType || isCharType) && isAllParsableAsSingleFlag
                        || isStringType && isAllParsableAsDomainFlag) {
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
            return null;
        }

        /**
         * Checks the values that are neither formulas nor constants as ranges and as arrays.
         */
        private void checkRanges() {
            for (var valueNum = skip; valueNum < width; valueNum++) {
                Set<CellValue> values = valuesMap.get(valueNum);
                var cellNum = -1;
                for (CellValue cellValue : values) {
                    cellNum++;
                    if (cellValue == null || !h[valueNum][cellNum]) {
                        continue;
                    }
                    checkRange(cellValue);
                }
            }
        }

        private void checkRange(CellValue cellValue) {
            var value = cellValue.getValue();
            /* try to create range by values **/
            try {
                if (isIntType) {
                    checkNumberRange(value, IntRange.class);
                } else if (isDoubleType) {
                    checkNumberRange(value, DoubleRange.class);
                } else if (isCharType) {
                    checkParsableAsRange(value, CharRange.class);
                    checkParsableAsArray(value);
                } else if (isDateType) {
                    checkDateRange(cellValue, value);
                } else if (isStringType) {
                    checkStringRange(value);
                }
            } catch (Exception ignored) {
                // guessing the column type is a heuristic: a value the checks fail on does not affect it
            }
        }

        private void checkNumberRange(String value, Class<?> rangeClass) {
            checkParsableAsRange(value, rangeClass);
            if (isAllParsableAsArrayFlag) {
                var arrs = ArraySplitter.split(value);
                var g = allParsableAs(arrs, type.getInstanceClass(), bindingContext);
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
        }

        private void checkParsableAsRange(String value, Class<?> rangeClass) {
            if (isAllParsableAsRangeFlag || !isNotParsableAsSingleRangeButParsableAsRangesArrayFlag) {
                var arrs = ArraySplitter.split(value);
                var f = allParsableAs(arrs, rangeClass, bindingContext);
                var parsableAsSingleRange = parsableAs(value, rangeClass, bindingContext);
                if (!f && !parsableAsSingleRange) {
                    isAllParsableAsRangeFlag = false;
                }
                if (f && arrs.length > 1 && !parsableAsSingleRange) {
                    isNotParsableAsSingleRangeButParsableAsRangesArrayFlag = true;
                }
            }
        }

        private void checkParsableAsArray(String value) {
            if (isAllParsableAsArrayFlag) {
                var arrs = ArraySplitter.split(value);
                var g = allParsableAs(arrs, type.getInstanceClass(), bindingContext);
                if (!g) {
                    isAllParsableAsArrayFlag = false;
                }
            }
        }

        private void checkDateRange(CellValue cellValue, String value) {
            var o = cellValue.getCell().getObjectValue();
            if (!(o instanceof Date)) {
                if (o instanceof String && !parsableAs(value, type.getInstanceClass(), bindingContext)) {
                    isAllParsableAsSingleFlag = false;
                }
                String[] arrs = checkParsableAsDateRange(value);
                if (isAllLikelyNotRangeFlag && o instanceof String && DateRangeParser.getInstance()
                        .likelyRangeThanDate(value)) {
                    isAllLikelyNotRangeFlag = false;
                }
                if (isAllElementsLikelyNotRangeFlag) {
                    checkElementsLikelyRange(value, arrs, DateRangeParser.getInstance()::likelyRangeThanDate);
                }
                checkParsableAsArray(value);
            }
        }

        /**
         * Checks the value as a date range or an array of them.
         *
         * @return the elements of the value, or {@code null} when it is not split
         */
        private String[] checkParsableAsDateRange(String value) {
            String[] arrs = null;
            if (isAllParsableAsRangeFlag || !isNotParsableAsSingleRangeButParsableAsRangesArrayFlag) {
                arrs = ArraySplitter.split(value);
                var f = allParsableAs(arrs, DateRange.class, bindingContext);
                var parsableAsSingleRange = parsableAs(value, DateRange.class, bindingContext);
                if (isAllParsableAsRangeFlag && !f && !parsableAsSingleRange) {
                    isAllParsableAsRangeFlag = false;
                }
                if (f && arrs.length > 1 && !parsableAsSingleRange) {
                    isNotParsableAsSingleRangeButParsableAsRangesArrayFlag = true;
                }
            }
            return arrs;
        }

        private void checkStringRange(String value) {
            String[] arrs = checkParsableAsStringRange(value);
            if (isAllLikelyNotRangeFlag && StringRangeParser.getInstance().likelyRangeThanString(value)) {
                isAllLikelyNotRangeFlag = false;
            }
            if (isAllElementsLikelyNotRangeFlag) {
                checkElementsLikelyRange(value, arrs, StringRangeParser.getInstance()::likelyRangeThanString);
            }
        }

        /**
         * Checks the value as a string range or an array of them.
         *
         * @return the elements of the value, or {@code null} when it is not split
         */
        private String[] checkParsableAsStringRange(String value) {
            String[] arrs = null;
            if (isAllParsableAsRangeFlag || !isNotParsableAsSingleRangeButParsableAsRangesArrayFlag) {
                arrs = ArraySplitter.split(value);
                var f = allParsableAs(arrs, StringRange.class, bindingContext);
                if (isAllParsableAsRangeFlag && !f && !parsableAs(value,
                        StringRange.class,
                        bindingContext)) {
                    isAllParsableAsRangeFlag = false;
                }
                if (!isNotParsableAsSingleRangeButParsableAsRangesArrayFlag && f && arrs.length > 1) {
                    isNotParsableAsSingleRangeButParsableAsRangesArrayFlag = true;
                }
            }
            return arrs;
        }

        /**
         * Checks whether an element of the value looks like a range rather than a single value.
         *
         * @param arrs the elements of the value, or {@code null} when the value is not split yet
         */
        private void checkElementsLikelyRange(String value, String[] arrs, Predicate<String> likelyRange) {
            var elements = arrs;
            if (elements == null) {
                elements = ArraySplitter.split(value);
            }
            for (String v : elements) {
                if (likelyRange.test(v)) {
                    isAllElementsLikelyNotRangeFlag = false;
                    break;
                }
            }
        }

        private Triple<String[], IOpenClass, String> getType(
                XlsModuleOpenClass module,
                IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache) {
            var rangeType = getRangeType();
            if (rangeType != null) {
                return rangeType;
            }

            if (!type.isArray() && isAllParsableAsArrayFlag
                    && (!isAllParsableAsSingleFlag || arraySeparatorFoundFlag)) {
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
                return getNotParsableValuesType(module, cache);
            } else {
                return buildTripleForConditionColumnWithSimpleType(condition,
                        type,
                        false,
                        isMoreThanOneColumnIsUsed,
                        module,
                        cache);
            }
        }

        /**
         * Returns the range type when all values are ranges of the type of the condition.
         *
         * @return the range type, or {@code null} when the values are not ranges
         */
        private Triple<String[], IOpenClass, String> getRangeType() {
            if (isDateType && isAllParsableAsRangeFlag && isRangeLikely()) {
                return buildTripleForTypeForConditionColumn(DateRange.class,
                        condition,
                        isNotParsableAsSingleRangeButParsableAsRangesArrayFlag,
                        isMoreThanOneColumnIsUsed);
            } else if (isIntType && isAllParsableAsRangeFlag
                    && (!isAllParsableAsArrayFlag || zeroStartedNumbersFoundFlag)) {
                return buildTripleForTypeForConditionColumn(IntRange.class,
                        condition,
                        isNotParsableAsSingleRangeButParsableAsRangesArrayFlag,
                        isMoreThanOneColumnIsUsed);
            } else if (isDoubleType && isAllParsableAsRangeFlag
                    && (!isAllParsableAsArrayFlag || zeroStartedNumbersFoundFlag)) {
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
                    .getSyntaxNode()) && isStringType && !isAllParsableAsDomainFlag && isAllParsableAsRangeFlag
                    && isRangeLikely()) {
                return buildTripleForTypeForConditionColumn(StringRange.class,
                        condition,
                        isNotParsableAsSingleRangeButParsableAsRangesArrayFlag,
                        isMoreThanOneColumnIsUsed);
            }
            return null;
        }

        /**
         * Checks whether the values look like ranges rather than single values, or cannot be arrays of single
         * values.
         */
        private boolean isRangeLikely() {
            return (isNotParsableAsSingleRangeButParsableAsRangesArrayFlag ? !isAllElementsLikelyNotRangeFlag
                    : !isAllLikelyNotRangeFlag) || !isAllParsableAsArrayFlag;
        }

        /**
         * Returns the type for the values that are neither single values nor arrays of values of the type of the
         * condition.
         */
        private Triple<String[], IOpenClass, String> getNotParsableValuesType(
                XlsModuleOpenClass module,
                IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache) {
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
        }

        private static boolean allParsableAs(String[] values,
                                             Class<?> componentType,
                                             IBindingContext bindingContext) {
            try {
                for (String value : values) {
                    String2DataConvertorFactory.parse(componentType, value, bindingContext);
                }
            } catch (Exception e) {
                return false;
            }
            return true;
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
            return switch (type) {
                case 0 -> Triple.of(new String[]{rangeClass.getSimpleName()},
                        JavaOpenClass.getOpenClass(rangeClass),
                        condition.getStatement());
                case 1 -> {
                    final var paramName = "_" + condition.getStatement().replace('.', '_');
                    yield Triple.of(new String[]{rangeClass.getSimpleName() + "[]", paramName},
                            AOpenClass.getArrayType(JavaOpenClass.getOpenClass(rangeClass), 1),
                            "contains(" + paramName + ", " + condition.statement + ")");
                }
                default -> {
                    final var paramName = "_" + condition.getStatement().replace('.', '_');
                    yield Triple.of(new String[]{rangeClass.getSimpleName() + "[][]", paramName},
                            AOpenClass.getArrayType(JavaOpenClass.getOpenClass(rangeClass), 2),
                            "contains(" + paramName + ", " + condition.statement + ")");
                }
            };
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

            return switch (v) {
                case 0 -> Triple.of(new String[]{getTypeNameForCode(type, module, cache)}, type, condition.getStatement());
                case 1 -> Triple.of(new String[]{getTypeNameForCode(type, module, cache) + "[]"},
                        AOpenClass.getArrayType(type, 1),
                        condition.getStatement());
                default -> Triple.of(new String[]{getTypeNameForCode(type, module, cache) + "[][]"},
                        AOpenClass.getArrayType(type, 2),
                        condition.getStatement());
            };
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
            var compositeMethod = declaredDTHeader.getDtColumnsDefinition().getCompositeMethod();
            if (compositeMethod != null) {
                return compositeMethod.getType();
            }
        }
        if (condition.isMethodParameterUsed()) {
            return decisionTable.getSignature().getParameterTypes()[condition.getMethodParameterIndex()];
        }
        throw new IllegalStateException();
    }

    /**
     * @deprecated Use plain grid model aka 2d array instead of building memory expensive Excel files.
     */
    @Deprecated(since = "5.24.0")
    public static XlsSheetGridModel createVirtualGrid() {
        var workbook = new XSSFWorkbook();
        try {
            final var sheet = workbook.createSheet();
            final var sourceCodeModule = new StringSourceCodeModule("", null);
            final var workbookLoader = new SimpleWorkbookLoader(workbook);
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
        Token[] returnTokens;
        Map<Token, IOpenField[][]> returnTypeFuzzyTokens;
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
