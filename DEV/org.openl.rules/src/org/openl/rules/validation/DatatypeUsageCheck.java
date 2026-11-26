package org.openl.rules.validation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.CompiledOpenClass;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.method.AOpenMethodDelegator;
import org.openl.binding.impl.module.ArrayOpenField;
import org.openl.binding.impl.module.RootDictionaryContext;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.DependencyType;
import org.openl.main.SourceCodeURLTool;
import org.openl.message.OpenLLintMessage;
import org.openl.message.OpenLMessage;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetHeaderDefinition;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.MatchNode;
import org.openl.rules.data.CollectionElementWithMultiRowField;
import org.openl.rules.data.FieldChain;
import org.openl.rules.data.ITableModel;
import org.openl.rules.datatype.binding.AliasDatatypeBoundNode;
import org.openl.rules.datatype.binding.DatatypeHelper;
import org.openl.rules.datatype.binding.DatatypeTableBoundNode;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.lang.xls.types.DatatypeOpenConstructor;
import org.openl.rules.lang.xls.types.DatatypeOpenField;
import org.openl.rules.lang.xls.types.meta.AliasDatatypeMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.ConstantsTableMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.DatatypeTableMetaInfoReader;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.StaticOpenClass;
import org.openl.types.impl.CollectionElementField;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.impl.OpenFieldDelegator;
import org.openl.types.java.BeanOpenField;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenConstructor;
import org.openl.types.java.JavaOpenMethod;
import org.openl.util.OpenClassUtils;
import org.openl.util.StringUtils;
import org.openl.util.text.LocationUtils;

/**
 * Finds the datatypes and the vocabularies of a project, and the fields of its datatypes, that no table of the project
 * uses.
 *
 * <p>A field is used when a table reads or sets it: an expression of a rule, directly or through its getter, its setter
 * or the constructor that takes every field, a row of a ColumnMatch table named after it, or a column of a Data, Test
 * or Run table. A field bound to a property of the runtime context is used too, as the engine reads it to fill the
 * context. A field a datatype inherits is used through the datatype that declares it. A datatype or a vocabulary is
 * used when a table refers to it: as a type of a rule, of a Data or Test table, of an expression, of a used field, of a
 * constant or of a Spreadsheet step, or as a type an expression names, as {@code getValues(Region)} does. A finding
 * tells what can be removed with no compilation error, so a type the declaration of any datatype names, as the type of
 * a field or as the parent, is used while the declaration stays, though nothing uses the field or the child; its own
 * fields are told one by one. A Java class stands for the datatype of its Java name: a project may hold the classes of
 * its datatypes compiled, and a module that imports their package sees each as a Java class.
 *
 * <p>The modules a module depends on as modules are looked at, not the projects it depends on, and the caller may
 * leave out some of them, such as the modules of another project. A project that depends on this one may use what this
 * one does not, and it is not compiled with it. Java and Groovy code of the project is not looked at either.
 *
 * <p>Every finding is an information message of the linter, {@link OpenLLintMessage}. A field that is not used is
 * told on its name, a datatype or a vocabulary that is not used at all on its table, without the fields.
 *
 * <p>A table of a type with a compilation error is not checked: what it declares is not known. A project with an
 * error of any other table or file of its modules is not checked at all: what the table or the file uses is not known,
 * and every field it reads would be told as not used. An error of a project it depends on, or of no place, such as one
 * of a check of the whole project, tells nothing of its tables.
 *
 * <p>A compilation in the execution mode is not checked: it keeps nothing of its tables.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DatatypeUsageCheck {

    /**
     * Checks the datatypes and the vocabularies of the modules of a project that the given test accepts.
     *
     * <p>A module it leaves out is neither checked nor read, and its errors stop nothing: a module of another project,
     * which a module of this one depends on, holds no table of this one.
     *
     * @param project the compiled project, or one module of it with the modules it depends on
     * @param checked whether a module is one of the project
     * @return the messages, by the module that declares the type they are about
     */
    public static Map<XlsModuleOpenClass, List<OpenLMessage>> check(CompiledOpenClass project,
                                                                    Predicate<XlsModuleOpenClass> checked) {
        var compiled = ValidationUtils.modulesOf(project.getOpenClassWithErrors(), DatatypeUsageCheck::isModule);
        var modules = compiled.stream().filter(checked).toList();
        var errors = ownMessagesOf(project, compiled, modules)
                .filter(message -> message.isError() && hasPlace(message))
                .map(message -> new XlsUrlParser(message.getSourceLocation()))
                .toList();
        var tables = modules.stream().flatMap(DatatypeUsageCheck::tablesOf).toList();
        var typeTables = tables.stream().filter(table -> table.getNodeType() == XlsNodeTypes.XLS_DATATYPE).toList();
        if (errors.stream().anyMatch(error -> hidesUsages(error, typeTables))) {
            return Map.of();
        }
        var declared = new IdentityHashMap<XlsModuleOpenClass, List<TableSyntaxNode>>();
        for (var module : modules) {
            var types = checkedTypeTables(module, errors);
            if (!types.isEmpty()) {
                declared.put(module, types);
            }
        }
        if (declared.isEmpty()) {
            return Map.of();
        }
        var usages = new Usages(modules, tables);
        modules.forEach(usages::collect);
        typeTables.forEach(usages::collectDeclaredTypes);
        tables.forEach(usages::collectConstants);
        declared.values().forEach(types -> types.forEach(usages::collectContextFields));
        var messages = new IdentityHashMap<XlsModuleOpenClass, List<OpenLMessage>>();
        declared.forEach((module, types) -> {
            var found = new ArrayList<OpenLMessage>();
            types.forEach(table -> report(table, usages, found));
            if (!found.isEmpty()) {
                messages.put(module, List.copyOf(found));
            }
        });
        return messages;
    }

    /**
     * Whether a message names a place in a file of the project. An error of the whole project, such as one of a check
     * made after the compilation, names none, or names no module.
     */
    private static boolean hasPlace(OpenLMessage message) {
        var place = message.getSourceLocation();
        return StringUtils.isNotEmpty(place) && !SourceCodeURLTool.NO_MODULE.equals(place);
    }

    private static boolean isModule(CompiledDependency dependency) {
        return dependency.getDependencyType() == DependencyType.MODULE;
    }

    /**
     * The messages of the checked modules, each telling its own: the compiled project, when it is one of them, and the
     * modules any compiled module depends on as modules, such as the modules a project of several compiles with. A
     * project it depends on tells its messages to itself, and so does a module left out of the check.
     */
    private static Stream<OpenLMessage> ownMessagesOf(CompiledOpenClass project,
                                                      List<XlsModuleOpenClass> compiled,
                                                      List<XlsModuleOpenClass> checked) {
        var dependencies = compiled.stream()
                .flatMap(module -> module.getDependencies().stream())
                .filter(DatatypeUsageCheck::isModule)
                .map(CompiledDependency::getCompiledOpenClass);
        return Stream.concat(Stream.of(project), dependencies)
                .distinct()
                .filter(module -> checked.stream().anyMatch(own -> own == module.getOpenClassWithErrors()))
                .flatMap(module -> module.getMessages().stream());
    }

    private static Stream<TableSyntaxNode> tablesOf(XlsModuleOpenClass module) {
        var info = module.getXlsMetaInfo();
        return info == null ? Stream.empty() : Arrays.stream(info.getXlsModuleNode().getXlsTableSyntaxNodes());
    }

    /**
     * Whether an error of the given place leaves unknown what the tables use: any place but a Datatype or a Vocabulary
     * table, whose error leaves out that table only. An error of a table of rules, Data or Test loses what the table
     * reads, and an error of a whole file, such as a Markdown file that cannot be read, loses the tables of the file.
     */
    private static boolean hidesUsages(XlsUrlParser error, List<TableSyntaxNode> typeTables) {
        return typeTables.stream().noneMatch(table -> table.getUriParser().intersects(error));
    }

    /**
     * The Datatype and the Vocabulary tables of a module that compiled without errors.
     *
     * <p>Their meta info tells their rows apart. A table that failed to compile has none, and neither has a table
     * compiled in the execution mode. A table with an error is left to the error: what it declares is not known.
     */
    private static List<TableSyntaxNode> checkedTypeTables(XlsModuleOpenClass module, List<XlsUrlParser> errors) {
        return tablesOf(module)
                .filter(table -> table.getMetaInfoReader() instanceof DatatypeTableMetaInfoReader
                        || table.getMetaInfoReader() instanceof AliasDatatypeMetaInfoReader)
                .filter(table -> errors.stream().noneMatch(table.getUriParser()::intersects))
                .toList();
    }

    private static void report(TableSyntaxNode table, Usages usages, List<OpenLMessage> found) {
        switch (table.getMetaInfoReader()) {
            case DatatypeTableMetaInfoReader reader -> reportDatatype(reader.getBoundNode(), usages, found);
            case AliasDatatypeMetaInfoReader reader -> reportVocabulary(reader.getBoundNode(), usages, found);
            default -> {
                // Only the tables of types are checked.
            }
        }
    }

    /** Tells the datatype of the table when it is not used, or else each of its fields that is not used. */
    private static void reportDatatype(DatatypeTableBoundNode table, Usages usages, List<OpenLMessage> found) {
        var datatype = table.getDataType();
        if (!usages.datatypes.contains(datatype.getJavaName())) {
            found.add(new OpenLLintMessage("Datatype '%s' is not used in the current project."
                    .formatted(datatype.getName()), table.getTableSyntaxNode()));
            return;
        }
        var rows = table.getTable();
        if (rows == null) {
            return;
        }
        var nameColumn = Objects.requireNonNullElse(table.getColumnTitlesOrder(), Map.<String, Integer>of())
                .getOrDefault(DatatypeHelper.NAME_COLUMN_TITLE, 1);
        for (var row = 0; row < rows.getHeight(); row++) {
            if (table.getFieldOfRow(row) instanceof DatatypeOpenField field
                    && !usages.fields.contains(keyOf(field))) {
                var name = DatatypeTableBoundNode.getCellSource(rows.getRow(row), null, nameColumn);
                // The name stands in its cell with what follows it, such as the suffix of a transient field. A place
                // ends at its last character.
                var start = Math.max(0, name.getCode().indexOf(field.getName()));
                var place = LocationUtils.createTextInterval(start, start + field.getName().length() - 1);
                found.add(new OpenLLintMessage("Field '%s' of datatype '%s' is not used in the current project."
                        .formatted(field.getName(), datatype.getName()),
                        new IdentifierNode("field", place, field.getName(), name)));
            }
        }
    }

    /** Tells the vocabulary of the table when it is not used. */
    private static void reportVocabulary(AliasDatatypeBoundNode table, Usages usages, List<OpenLMessage> found) {
        var vocabulary = table.getDomainOpenClass().getName();
        if (!usages.vocabularies.contains(vocabulary)) {
            found.add(new OpenLLintMessage("Vocabulary '%s' is not used in the current project."
                    .formatted(vocabulary), table.getTableSyntaxNode()));
        }
    }

    private static String keyOf(IOpenField field) {
        return field.getDeclaringClass().getJavaName() + "#" + field.getName();
    }

    /**
     * What the tables of the project use: the fields of datatypes, the datatypes and the vocabularies.
     *
     * <p>Only the tables of the modules checked are read. A module holds the rules and the Data tables of the projects
     * it depends on too, which cannot use the datatypes of this one.
     */
    private static final class Usages extends BindingDependencies {
        /** The fields used, each by its datatype and name: every module sees the datatypes it uses on its own. */
        private final Set<String> fields = new HashSet<>();
        /** The datatypes used, by their Java names. */
        private final Set<String> datatypes = new HashSet<>();
        /** The vocabularies used, by their names: an array of a vocabulary is a type of its own. */
        private final Set<String> vocabularies = new HashSet<>();
        private final Set<IOpenMethod> methods = Collections.newSetFromMap(new IdentityHashMap<>());
        /** The members an expression calls or reads, each looked into once, however many expressions name it. */
        private final Set<Object> members = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Set<ITableModel> tables = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Set<IOpenClass> ownModules = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Set<TableSyntaxNode> ownTables = Collections.newSetFromMap(new IdentityHashMap<>());

        private Usages(List<XlsModuleOpenClass> modules, List<TableSyntaxNode> tables) {
            ownModules.addAll(modules);
            ownTables.addAll(tables);
        }

        /** Collects what the rules and the Data tables of a module use. */
        private void collect(XlsModuleOpenClass module) {
            OpenMethodDispatcherHelper.extractMethods(module)
                    .stream()
                    .filter(method -> ownModules.contains(method.getDeclaringClass()))
                    .forEach(this::collectMethod);
            var dataBase = module.getDataBase();
            if (dataBase != null) {
                // A Test or a Run table comes with its method: the data base keeps one table of overloaded ones
                dataBase.getTables()
                        .stream()
                        .filter(table -> ownTables.contains(table.getTableSyntaxNode()))
                        .filter(table -> table.getTableSyntaxNode().getNodeType() == XlsNodeTypes.XLS_DATA)
                        .forEach(table -> addTable(table.getDataModel()));
            }
        }

        private void collectMethod(IOpenMethod method) {
            if (!methods.add(method)) {
                return;
            }
            addType(method.getType());
            Arrays.stream(method.getSignature().getParameterTypes()).forEach(this::addType);
            switch (method) {
                case TestSuiteMethod test -> addTable(test.getDataModel());
                case ColumnMatch table -> {
                    addMatched(table.getCheckTree());
                    addMatched(table.getTotalScore());
                }
                case Spreadsheet table when table.getBoundNode() != null -> {
                    table.getBoundNode().updateDependency(this);
                    addSpreadsheetTypes(table);
                }
                // A decision table, a Method or a TBasic table: its expressions are read with the types they take
                case ExecutableRulesMethod table when table.getBoundNode() != null ->
                        table.getBoundNode().updateDependency(this);
                default -> {
                    // A method no table defines, or a table compiled for the execution, uses nothing of the project.
                }
            }
        }

        /**
         * Adds the types a Spreadsheet names in the headers of its steps and its columns, and the types its cells take.
         * A step names its type though its cell holds a value or nothing, not a formula, and though its column gives the
         * cell a type of its own.
         */
        private void addSpreadsheetTypes(Spreadsheet table) {
            var structure = table.getBoundNode().getStructureBuilder();
            if (structure != null) {
                Stream.concat(structure.getRowHeaders().values().stream(),
                                structure.getColumnHeaders().values().stream())
                        .map(SpreadsheetHeaderDefinition::getType)
                        .forEach(this::addType);
            }
            Arrays.stream(table.getCells())
                    .flatMap(Arrays::stream)
                    .filter(Objects::nonNull)
                    .map(SpreadsheetCell::getType)
                    .forEach(this::addType);
        }

        /** Adds the fields the rows of a ColumnMatch table read by their names, from the row given down. */
        private void addMatched(@Nullable MatchNode row) {
            if (row != null) {
                if (row.getArgument() != null) {
                    addField(row.getArgument().getField());
                }
                row.getChildren().forEach(this::addMatched);
            }
        }

        /** Adds the columns of a Data, Test or Run table: each sets or checks the fields of its path. */
        private void addTable(@Nullable ITableModel table) {
            if (table != null && tables.add(table)) {
                addType(table.getType());
                Arrays.stream(table.getDescriptors())
                        .filter(Objects::nonNull)
                        .forEach(column -> Arrays.stream(column.getPathFields()).forEach(this::addField));
            }
        }

        /**
         * Collects the types a datatype names in its declaration: the type of each of its fields and its parent. They
         * cannot be removed while the datatype declares them, whether the datatype and its fields are used or not, so
         * they are used; their own fields are told one by one. A datatype that names itself, as the type of its
         * children, does not use itself so.
         */
        private void collectDeclaredTypes(TableSyntaxNode table) {
            if (table.getMetaInfoReader() instanceof DatatypeTableMetaInfoReader reader) {
                var datatype = reader.getBoundNode().getDataType();
                addType(datatype.getSuperClass());
                datatype.getDeclaredFields()
                        .stream()
                        .map(IOpenField::getType)
                        .filter(type -> !datatype.equals(OpenClassUtils.getRootComponentClass(type)))
                        .forEach(this::addType);
            }
        }

        /**
         * Collects the types of the constants of a Constants table and what their formulas read. A type cannot be
         * removed while the table declares a constant of it, whether a rule reads the constant or not.
         */
        private void collectConstants(TableSyntaxNode table) {
            if (table.getMetaInfoReader() instanceof ConstantsTableMetaInfoReader reader) {
                var constants = reader.getBoundNode();
                constants.getConstantOpenFields().forEach(constant -> addType(constant.getType()));
                constants.getFormulas().forEach(formula -> formula.updateDependency(this));
            }
        }

        /**
         * Collects the fields of a used datatype that fill the runtime context. The engine reads them for every rule
         * the datatype is passed to, so they and their types are used. A datatype no table uses fills nothing.
         */
        private void collectContextFields(TableSyntaxNode table) {
            if (table.getMetaInfoReader() instanceof DatatypeTableMetaInfoReader reader) {
                var datatype = reader.getBoundNode().getDataType();
                if (datatypes.contains(datatype.getJavaName())) {
                    datatype.getDeclaredFields().stream().filter(IOpenField::isContextProperty).forEach(this::addField);
                }
            }
        }

        @Override
        public void visit(@Nullable IBoundNode node) {
            if (node != null) {
                addType(node.getType());
            }
            super.visit(node);
        }

        @Override
        public void addFieldDependency(IOpenField field, IBoundNode node) {
            addField(field);
        }

        /**
         * A getter or a setter of a datatype reads or sets its field, and the constructor that takes every field sets
         * them all, also when the call is wrapped. The getter, the setter and the constructor that takes every field of
         * a Java class do so too: the class may be the bean of a datatype.
         */
        @Override
        public void addMethodDependency(IOpenMethod method, IBoundNode node) {
            var called = method;
            while (called instanceof AOpenMethodDelegator delegator) {
                // A call for each element of an array, or one whose result is cast
                called = delegator.getDelegate();
            }
            if (!members.add(called)) {
                return;
            }
            switch (called) {
                // Takes the methods of a datatype too. A getter takes no argument and a setter one.
                case JavaOpenMethod accessor when !accessor.isStatic() && accessor.getNumberOfParameters() <= 1 ->
                        accessor.getDeclaringClass()
                                .getFields()
                                .stream()
                                .filter(field -> isAccessedBy(field, accessor.getJavaMethod()))
                                .forEach(this::addField);
                case DatatypeOpenConstructor constructor when constructor.getNumberOfParameters() > 0 ->
                        constructor.getDeclaringClass().getFields().forEach(this::addField);
                case JavaOpenConstructor constructor -> addFieldsSetBy(constructor);
                default -> {
                    // A call that reads and sets no field by itself.
                }
            }
        }

        /**
         * Adds the properties of a Java class its constructor that takes every field sets, as a module importing the bean
         * of a datatype calls it: no other constructor of the bean takes as many arguments as the bean has properties.
         */
        private void addFieldsSetBy(JavaOpenConstructor constructor) {
            var properties = constructor.getDeclaringClass()
                    .getFields()
                    .stream()
                    .filter(BeanOpenField.class::isInstance)
                    .toList();
            if (!properties.isEmpty() && properties.size() == constructor.getNumberOfParameters()) {
                properties.forEach(this::addField);
            }
        }

        private static boolean isAccessedBy(IOpenField field, Method accessor) {
            return switch (field) {
                case DatatypeOpenField datatypeField ->
                        accessor.equals(datatypeField.getGetter()) || accessor.equals(datatypeField.getSetter());
                case BeanOpenField beanField ->
                        accessor.equals(beanField.getReadMethod()) || accessor.equals(beanField.getWriteMethod());
                default -> false;
            };
        }

        /**
         * Adds a type and its parents. A Java class counts as the datatype of its name: a project may hold the beans of
         * its datatypes compiled, and a module that imports their package sees each as a Java class.
         */
        private void addType(@Nullable IOpenClass type) {
            switch (type == null ? null : OpenClassUtils.getRootComponentClass(type)) {
                // The parents of a type are added once, when the type is added
                case DatatypeOpenClass datatype when datatypes.add(datatype.getJavaName()) ->
                        addType(datatype.getSuperClass());
                case JavaOpenClass javaClass when datatypes.add(javaClass.getJavaName()) ->
                        javaClass.superClasses().forEach(this::addType);
                case DomainOpenClass vocabulary -> vocabularies.add(vocabulary.getName());
                // A type written in an expression, such as the vocabulary of getValues(Region)
                case StaticOpenClass reference -> addType(reference.getDelegate());
                case null, default -> {
                    // A type no table of the project declares.
                }
            }
        }

        /** Adds the field of a datatype the given field stands for, and every field it is read through. */
        private void addField(@Nullable IOpenField field) {
            if (field == null || !members.add(field)) {
                return;
            }
            switch (field) {
                case FieldChain chain -> Arrays.stream(chain.getFields()).forEach(this::addField);
                case CollectionElementField element -> addField(element.getField());
                case CollectionElementWithMultiRowField element -> addField(element.getField());
                case ArrayOpenField array -> addField(array.getField());
                case OpenFieldDelegator delegator -> {
                    addField(RootDictionaryContext.parentOf(delegator));
                    addField(delegator.getDelegate());
                }
                // The field of a datatype, or of a Java class: the bean of a datatype a module sees as a Java class
                case IOpenField member when member.getDeclaringClass() instanceof DatatypeOpenClass
                        || member.getDeclaringClass() instanceof JavaOpenClass -> {
                    fields.add(keyOf(member));
                    addType(member.getDeclaringClass());
                    addType(member.getType());
                }
                default -> {
                    // A field of no class: a variable, a cell of a Spreadsheet.
                }
            }
        }
    }
}
