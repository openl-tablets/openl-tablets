package org.openl.studio.projects.service.trace;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.openl.binding.ILocalVar;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.calc.CustomSpreadsheetResultField;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.element.SpreadsheetCellField;
import org.openl.rules.calc.element.SpreadsheetRangeField;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.lang.xls.types.DatatypeOpenField;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.OpenFieldDelegator;

/**
 * Resolves the values a spreadsheet step's formula consumed from the values its frame recorded.
 */
public final class FormulaInputs {

    private FormulaInputs() {
    }

    /** An input a step's formula consumed, ranked so the list reads steps → parameters → constants. */
    private record StepInput(int rank, int order, String name, @Nullable Object value, @Nullable IOpenClass type) {
    }

    /**
     * Accumulates a step's inputs as they are resolved: unique by name, and tracking which bare fields were
     * narrowed into a dotted access ({@code $Cell.$Field}, {@code policy.census}) so a later pass skips them.
     */
    private static final class InputCollector {
        private final List<StepInput> inputs = new ArrayList<>();
        private final Set<String> seen = new HashSet<>();
        private final Set<IOpenField> narrowed = new HashSet<>();

        void add(@Nullable StepInput input) {
            if (input != null && seen.add(input.name())) {
                inputs.add(input);
            }
        }
    }

    /**
     * The values a step's formula consumed, named as the formula writes them: sibling steps such as
     * {@code $LimitIndex}, the table's own parameters, fields read off another step's result such as
     * {@code $Rate.$Value} (element-wise for an array of results), fields read off a parameter such as
     * {@code policy.census}, fields opened into the table's scope such as {@code currentFinancialData},
     * and module constants such as {@code MaxLimit}.
     *
     * <p>Resolved from the compiled cell's binding dependencies against the frame's recorded values —
     * nothing is re-evaluated. A sibling step that has not executed yet is omitted, and so is a dependency
     * the recorded data cannot resolve.
     */
    public static List<ParameterWithValueDeclaration> resolve(CompositeMethod composite, DebugFrame frame,
                                                              Spreadsheet spreadsheet, Map<String, Object> executed) {
        var dependencies = new RulesBindingDependencies();
        composite.updateDependency(dependencies);
        List<IOpenField> fields = new ArrayList<>(dependencies.getFieldsMap().values());
        var collector = new InputCollector();
        collectResultFieldInputs(fields, executed, collector);
        collectParameterFieldInputs(fields, frame, spreadsheet, collector);
        collectOtherInputs(fields, frame, spreadsheet, executed, collector);
        return collector.inputs.stream()
                .sorted(Comparator.comparingInt(StepInput::rank)
                        .thenComparingInt(StepInput::order)
                        .thenComparing(StepInput::name))
                .map(input -> new ParameterWithValueDeclaration(input.name(), input.value(), input.type()))
                .toList();
    }

    /** A field picked from another step's result ({@code $Cell.$Field}): listed as the dotted name with the
     * field's value; the bare result the formula only reached through is narrowed away. */
    private static void collectResultFieldInputs(List<IOpenField> fields, Map<String, Object> executed,
                                                 InputCollector collector) {
        for (IOpenField field : fields) {
            if (field instanceof CustomSpreadsheetResultField resultField) {
                collector.add(resultFieldInput(resultField, fields, executed, collector.narrowed));
            }
        }
    }

    /** A field read explicitly off a parameter ({@code policy.census}): listed as the dotted name with the
     * field's value; the bare parameter the formula only reached through is narrowed away. */
    private static void collectParameterFieldInputs(List<IOpenField> fields, DebugFrame frame, Spreadsheet spreadsheet,
                                                    InputCollector collector) {
        for (IOpenField field : fields) {
            if (field instanceof DatatypeOpenField datatypeField) {
                collector.add(parameterFieldInput(datatypeField, fields, frame, spreadsheet, collector.narrowed));
            }
        }
    }

    /** Everything else the formula consumed — sibling steps, whole parameters, constants — skipping fields
     * already narrowed into a dotted access above. */
    private static void collectOtherInputs(List<IOpenField> fields, DebugFrame frame, Spreadsheet spreadsheet,
                                           Map<String, Object> executed, InputCollector collector) {
        for (IOpenField field : fields) {
            if (!(field instanceof CustomSpreadsheetResultField) && !collector.narrowed.contains(field)) {
                resolveStepInputs(field, frame, spreadsheet, executed).forEach(collector::add);
            }
        }
    }

    private static List<StepInput> resolveStepInputs(IOpenField field, DebugFrame frame, Spreadsheet spreadsheet,
                                                     Map<String, Object> executed) {
        if (field instanceof SpreadsheetRangeField range) {
            // A cell range ($First:$Last) reads as the individual steps it spans, like the tree shows it.
            List<StepInput> inputs = new ArrayList<>();
            for (int row = range.getStartRowIndex(); row <= range.getEndRowIndex(); row++) {
                for (int column = range.getStartColumnIndex(); column <= range.getEndColumnIndex(); column++) {
                    StepInput input = rangeCellInput(spreadsheet, executed, row, column);
                    if (input != null) {
                        inputs.add(input);
                    }
                }
            }
            return inputs;
        }
        StepInput single = resolveStepInput(field, frame, spreadsheet, executed);
        return single == null ? List.of() : List.of(single);
    }

    /**
     * A field read from another step's result, e.g. {@code $BalanceQualityIndexCalculation.$Value$BalanceQualityIndex}:
     * pair the field with the sibling step of its declaring result type, read the field off that step's
     * recorded value, and mark the bare step as narrowed so it is not listed on top of its field.
     */
    private static @Nullable StepInput resultFieldInput(CustomSpreadsheetResultField field, List<IOpenField> fields,
                                                        Map<String, Object> executed, Set<IOpenField> narrowed) {
        for (IOpenField candidate : fields) {
            SpreadsheetCellField cellField = resultCellOf(candidate, field.getDeclaringClass());
            if (cellField == null) {
                continue;
            }
            SpreadsheetCell cell = cellField.getCell();
            String ref = CurrentLocation.cellRef(cell.getRowIndex(), cell.getColumnIndex());
            if (!executed.containsKey(ref)) {
                return null;
            }
            try {
                Object result = executed.get(ref);
                if (cellField.getType().isArray()) {
                    // `$Plans.$Lives` over an array of results reads the field off each element into a matrix,
                    // its type an array of the field's own type. The whole array stays listed too: unlike a
                    // scalar result reached only through its field, an array is commonly also passed whole.
                    IOpenClass type = field.getType().getAggregateInfo().getIndexedAggregateType(field.getType());
                    return new StepInput(0, gridOrder(cell.getRowIndex(), cell.getColumnIndex()),
                            cellField.getName() + "." + field.getName(), mapResultField(field, result), type);
                }
                narrowed.add(cellField);
                Object value = result == null ? null : field.get(result, null);
                return new StepInput(0, gridOrder(cell.getRowIndex(), cell.getColumnIndex()),
                        cellField.getName() + "." + field.getName(), value, field.getType());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /** The referenced result cell whose type — or, for an array of results, its element type — is the field's
     * declaring class; {@code null} for any other dependency. */
    private static @Nullable SpreadsheetCellField resultCellOf(IOpenField candidate, IOpenClass declaring) {
        if (!(candidate instanceof SpreadsheetCellField cellField)) {
            return null;
        }
        IOpenClass type = cellField.getType();
        IOpenClass element = type.isArray() ? type.getComponentClass() : type;
        return element.getName().equals(declaring.getName()) ? cellField : null;
    }

    /** Read a field off each element of an array of results, as OpenL's {@code $array.$field} matrix syntax does. */
    private static @Nullable Object mapResultField(CustomSpreadsheetResultField field, @Nullable Object array) {
        if (array == null) {
            return null;
        }
        int length = Array.getLength(array);
        Object values = field.getType().getAggregateInfo().makeIndexedAggregate(field.getType(), length);
        for (int i = 0; i < length; i++) {
            Object element = Array.get(array, i);
            Array.set(values, i, element == null ? null : field.get(element, null));
        }
        return values;
    }

    /** A cell's position key for a table-shaped ordering: row-major, with room for many columns per row. */
    private static int gridOrder(int row, int column) {
        return row * 10_000 + column;
    }

    /** One executed cell of a referenced range, named by its OpenL cell name. */
    private static @Nullable StepInput rangeCellInput(Spreadsheet spreadsheet, Map<String, Object> executed,
                                                      int row, int column) {
        SpreadsheetCell[][] cells = spreadsheet.getCells();
        SpreadsheetCell cell = row < cells.length && column < cells[row].length ? cells[row][column] : null;
        if (cell == null || !cell.isMethodCell()) {
            return null;
        }
        String ref = CurrentLocation.cellRef(row, column);
        if (!executed.containsKey(ref)) {
            return null;
        }
        return new StepInput(0, gridOrder(row, column), SpreadsheetCellNames.of(spreadsheet, cell),
                executed.get(ref), cell.getType());
    }

    private static @Nullable StepInput resolveStepInput(IOpenField field, DebugFrame frame, Spreadsheet spreadsheet,
                                                        Map<String, Object> executed) {
        if (field instanceof SpreadsheetCellField cellField) {
            SpreadsheetCell used = cellField.getCell();
            String ref = CurrentLocation.cellRef(used.getRowIndex(), used.getColumnIndex());
            // A referenced step that has not executed yet has no recorded value to show.
            if (!executed.containsKey(ref)) {
                return null;
            }
            int order = gridOrder(used.getRowIndex(), used.getColumnIndex());
            return new StepInput(0, order, field.getName(), executed.get(ref), cellField.getType());
        }
        if (field instanceof ILocalVar) {
            // The table's own parameter used by name (e.g. `bank`).
            IMethodSignature signature = spreadsheet.getSignature();
            Object[] params = frame.getParams();
            int count = Math.min(params.length, signature.getNumberOfParameters());
            for (int i = 0; i < count; i++) {
                if (field.getName().equals(signature.getParameterName(i))) {
                    return new StepInput(1, i, field.getName(), params[i], signature.getParameterType(i));
                }
            }
            return null;
        }
        if (field instanceof ConstantOpenField constant) {
            return new StepInput(3, 0, constant.getName(), constant.getValue(), constant.getType());
        }
        if (field instanceof OpenFieldDelegator delegator) {
            return parameterScopeInput(delegator, frame, spreadsheet);
        }
        return null;
    }

    /**
     * The index of the sole table parameter whose type the given class can be read from, or {@code -1}.
     * Both parameter-field resolvers pair a field with the parameter of its declaring type this way.
     *
     * <p>Returns {@code -1} when no parameter matches and also when more than one does: the field alone
     * does not say which same-typed parameter the formula read, so the caller lists the whole parameters
     * rather than guessing — and mislabelling — the first.
     */
    private static int matchingParameterIndex(IOpenClass declaring, IMethodSignature signature, int paramsLength) {
        int count = Math.min(paramsLength, signature.getNumberOfParameters());
        int found = -1;
        for (int i = 0; i < count; i++) {
            if (declaring.isAssignableFrom(signature.getParameterType(i))) {
                if (found >= 0) {
                    return -1;
                }
                found = i;
            }
        }
        return found;
    }

    /**
     * A field of a parameter opened into the table's scope (e.g. {@code currentFinancialData} resolved
     * as a field of the {@code bank} parameter): read it from that parameter's recorded value.
     */
    private static @Nullable StepInput parameterScopeInput(OpenFieldDelegator field, DebugFrame frame,
                                                           Spreadsheet spreadsheet) {
        IOpenClass declaring = field.getDeclaringClass();
        if (declaring == null) {
            return null;
        }
        Object[] params = frame.getParams();
        int i = matchingParameterIndex(declaring, spreadsheet.getSignature(), params.length);
        if (i < 0) {
            return null;
        }
        try {
            Object value = params[i] == null ? null : field.getDelegate().get(params[i], null);
            return new StepInput(2, i, field.getName(), value, field.getType());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * A field read explicitly off a parameter, e.g. {@code policy.census}: pair the datatype field with the
     * parameter of its declaring type, read the field off that parameter's recorded value, and name it with
     * the dotted path. The bare parameter, present only as the root of the access, is narrowed so it is not
     * listed on top of its field.
     */
    private static @Nullable StepInput parameterFieldInput(DatatypeOpenField field, List<IOpenField> fields,
                                                           DebugFrame frame, Spreadsheet spreadsheet,
                                                           Set<IOpenField> narrowed) {
        IOpenClass declaring = field.getDeclaringClass();
        if (declaring == null) {
            return null;
        }
        IMethodSignature signature = spreadsheet.getSignature();
        Object[] params = frame.getParams();
        int i = matchingParameterIndex(declaring, signature, params.length);
        if (i < 0) {
            return null;
        }
        String parameter = signature.getParameterName(i);
        try {
            Object value = params[i] == null ? null : field.get(params[i], null);
            // Narrow the bare parameter only once the read succeeds, so a throwing getter leaves the whole
            // parameter listed rather than dropping both it and the field it could not resolve.
            fields.stream()
                    .filter(candidate -> candidate instanceof ILocalVar && parameter.equals(candidate.getName()))
                    .forEach(narrowed::add);
            return new StepInput(2, i, parameter + "." + field.getName(), value, field.getType());
        } catch (Exception e) {
            return null;
        }
    }
}
