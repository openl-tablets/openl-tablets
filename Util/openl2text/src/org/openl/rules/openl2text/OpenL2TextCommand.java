package org.openl.rules.openl2text;

import static org.openl.rules.project.ai.OpenL2TextUtils.createObjectMapper;
import static org.openl.rules.project.ai.OpenL2TextUtils.methodToString;
import static org.openl.rules.project.ai.OpenL2TextUtils.openClassToString;
import static org.openl.rules.project.ai.OpenL2TextUtils.tableSyntaxNodeToString;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.CompiledOpenClass;
import org.openl.conf.UserContext;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.project.ai.OpenL2TextUtils;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.syntax.code.IParsedCode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.xls.Parser;

public class OpenL2TextCommand {
    private final Logger log = LoggerFactory.getLogger(OpenL2TextCommand.class);

    private final Path dir;
    private final Path workspace;
    private final Path outputDir;
    private final boolean omitTypes;
    private final boolean includeDimensionalProperties;
    private final boolean omitMethodRefs;
    private final boolean omitDispatchingMethods;
    private final int maxTypesDeep;
    private final boolean replaceAliasesWithBaseTypes;
    private final boolean includeAllRulesMethods;
    private final boolean tableAsCode;
    private volatile boolean flag = false;
    private final boolean onlyMethodCells;
    private final int maxRows;
    private final boolean parsingMode;

    public OpenL2TextCommand(Path dir,
            Path workspace,
            Path outputDir,
            boolean omitTypes,
            int maxTypesDeep,
            boolean replaceAliasesWithBaseTypes,
            boolean includeDimensionalProperties,
            boolean omitMethodRefs,
            boolean includeAllRulesMethods,
            boolean omitDispatchingMethods,
            boolean tableAsCode,
            boolean onlyMethodCells,
            int maxRows,
            boolean parsingMode) {
        this.dir = Objects.requireNonNull(dir, "dir cannot be null");
        this.workspace = Objects.requireNonNull(workspace, "workspace cannot be null");
        this.outputDir = Objects.requireNonNull(outputDir, "outputDir cannot be null");
        this.omitTypes = omitTypes;
        this.includeDimensionalProperties = includeDimensionalProperties;
        this.omitMethodRefs = omitMethodRefs;
        this.omitDispatchingMethods = omitDispatchingMethods;
        this.maxTypesDeep = maxTypesDeep;
        this.replaceAliasesWithBaseTypes = replaceAliasesWithBaseTypes;
        this.includeAllRulesMethods = includeAllRulesMethods;
        this.tableAsCode = tableAsCode;
        this.onlyMethodCells = onlyMethodCells;
        this.maxRows = maxRows;
        this.parsingMode = parsingMode;
    }

    private String getFileName(ExecutableRulesMethod executableRulesMethod) {
        String headerToken = executableRulesMethod.getSyntaxNode().getHeader().getHeaderToken().getIdentifier();
        headerToken = headerToken.toLowerCase();
        String fileName = executableRulesMethod.getName() + "-" + UUID.randomUUID() + "." + headerToken;
        // Replace special characters with underscores
        fileName = fileName.replaceAll("[\\\\/*?:\"|]", "_");
        return fileName;
    }

    public void run() throws ProjectResolvingException, RulesInstantiationException {
        // This method can't be run twice, flag is used to prevent it
        if (flag) {
            throw new IllegalStateException("Can't be run twice on the same instance");
        }
        flag = true;
        if (parsingMode) {
            // Iterate over Excel files in the dir folder
            try (Stream<Path> paths = Files.walk(dir)) {
                paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".xls") || p.toString().endsWith(".xlsx") || p.toString()
                        .endsWith(".xlsm") || p.toString().endsWith(".xlsb"))
                    .forEach(p -> {
                        try {
                            Parser parser = new Parser(
                                new UserContext(Thread.currentThread().getContextClassLoader(), "."));
                            IParsedCode parsedCode = parser.parseAsModule(new URLSourceCodeModule(p.toUri().toURL()));
                            TableSyntaxNode[] tableSyntaxNodes = ((XlsModuleSyntaxNode) parsedCode.getTopNode())
                                .getXlsTableSyntaxNodes();
                            for (TableSyntaxNode tableSyntaxNode : tableSyntaxNodes) {
                                writeExcelTable(tableSyntaxNode);
                            }
                        } catch (Exception e) {
                            log.error("Failed to parse excel file: {}", p, e);
                        }
                    });
            } catch (IOException e) {
                log.error("Failed to iterate over files in the dir folder: {}", dir, e);
            }
        } else {
            SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = null;
            try {
                simpleProjectEngineFactory = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>()
                    .setProject(dir.toFile().getPath())
                    .setWorkspace(workspace.toFile().getPath())
                    .setExecutionMode(false)
                    .setProvideVariations(false)
                    .setProvideRuntimeContext(false)
                    .build();
                CompiledOpenClass compiledOpenClass = simpleProjectEngineFactory.getCompiledOpenClass();
                if (compiledOpenClass.hasErrors()) {
                    StringBuilder sb = new StringBuilder();
                    for (OpenLMessage message : OpenLMessagesUtils
                        .filterMessagesBySeverity(compiledOpenClass.getAllMessages(), Severity.ERROR)) {
                        sb.append("    ").append(message.getSummary()).append("\n");
                    }
                    log.error("Rules '%s' are compiled with errors: \n{}", sb.toString());
                }
                IOpenClass openClass = compiledOpenClass.getOpenClassWithErrors();
                final ObjectMapper objectMapper = createObjectMapper();
                Set<String> allRulesMethods = new HashSet<>();
                if (includeAllRulesMethods) {
                    for (IOpenMethod openMethod : openClass.getMethods()) {
                        allRulesMethods
                            .add(OpenL2TextUtils.methodHeaderToString(openMethod, replaceAliasesWithBaseTypes));
                    }
                }

                listRulesMethods(openClass).forEach(e -> writeMethod(e, allRulesMethods, objectMapper));
            } finally {
                // Release locked jars
                if (simpleProjectEngineFactory != null) {
                    simpleProjectEngineFactory.getDependencyManager().resetAll();
                }
            }
        }
    }

    private Stream<ExecutableRulesMethod> listRulesMethods(IOpenClass openClass) {
        return openClass.getMethods().stream().flatMap(openMethod -> {
            if (openMethod instanceof OpenMethodDispatcher) {
                if (omitDispatchingMethods) {
                    // Return only first candidate
                    return Stream.of(((OpenMethodDispatcher) openMethod).getCandidates().get(0));
                } else {
                    return ((OpenMethodDispatcher) openMethod).getCandidates().stream();
                }
            } else {
                return Stream.of(openMethod);
            }
        }).filter(e -> e instanceof ExecutableRulesMethod).map(e -> (ExecutableRulesMethod) e);
    }

    private void writeExcelTable(TableSyntaxNode tableSyntaxNode) {
        String fileName = UUID.randomUUID().toString();
        String content = tableSyntaxNodeToString(tableSyntaxNode, false, false, Integer.MAX_VALUE);
        writeContentToFile(fileName, content);
    }

    private void writeMethod(IOpenMethod openMethod, Set<String> allRulesMethods, ObjectMapper objectMapper) {
        if (!(openMethod instanceof ExecutableRulesMethod)) {
            return;
        }
        ExecutableRulesMethod rulesMethod = (ExecutableRulesMethod) openMethod;
        String fileName = getFileName(rulesMethod);
        StringBuilder sb = new StringBuilder();
        // Write a table as a text
        sb.append(methodToString(rulesMethod, replaceAliasesWithBaseTypes, tableAsCode, onlyMethodCells, maxRows));
        if (includeDimensionalProperties) {
            String dimensionalProperties = OpenL2TextUtils.dimensionalPropertiesToString(rulesMethod, objectMapper);
            if (dimensionalProperties != null) {
                // Write header for dimensional properties section
                sb.append("\n\n");
                sb.append("***").append("\n");
                sb.append(dimensionalProperties);
            }
        }
        Set<IOpenMethod> methodRefs = null;
        if (!omitTypes) {
            methodRefs = OpenL2TextUtils.methodRefs(rulesMethod.getSyntaxNode());
            Set<IOpenClass> types = new HashSet<>();
            for (IOpenClass type : OpenL2TextUtils.methodTypes(rulesMethod.getSyntaxNode())) {
                OpenL2TextUtils.collectTypes(type, types, maxTypesDeep, replaceAliasesWithBaseTypes);
            }
            for (IOpenMethod method : methodRefs) {
                OpenL2TextUtils.collectTypes(method.getType(), types, maxTypesDeep, replaceAliasesWithBaseTypes);
            }
            if (!types.isEmpty()) {
                // Write header for types section
                sb.append("\n\n");
                sb.append("###").append("\n");
                boolean f = true;
                for (IOpenClass type : types) {
                    if (f) {
                        f = false;
                    } else {
                        sb.append("\n\n");
                    }
                    sb.append(openClassToString(type, replaceAliasesWithBaseTypes));
                }
            }
        }
        if (!omitMethodRefs) {
            if (methodRefs == null) {
                methodRefs = OpenL2TextUtils.methodRefs(rulesMethod.getSyntaxNode());
            }
            Set<String> refs = new HashSet<>();
            for (IOpenMethod method : methodRefs) {
                refs.add(OpenL2TextUtils.methodHeaderToString(method, replaceAliasesWithBaseTypes));
            }
            if (!refs.isEmpty()) {
                // Write header for method refs section
                sb.append("\n\n");
                sb.append("$$$").append("\n");
                sb.append(String.join(" {}\n", refs));
            }
        }
        if (includeAllRulesMethods && allRulesMethods != null && !allRulesMethods.isEmpty()) {
            sb.append("\n\n");
            sb.append("^^^").append("\n");
            sb.append(String.join(" {}\n", allRulesMethods));
        }
        writeContentToFile(fileName, sb.toString());
    }

    private void writeContentToFile(String fileName, String content) {
        // Write the content to a file
        Path filePath = outputDir.resolve(fileName);
        try {
            Files.writeString(filePath, content);
        } catch (IOException e) {
            String errorMessage = String
                .format("Error writing content to file '%s' while processing directory '%s'.", filePath, dir);
            log.error(errorMessage, e);
        }
    }
}
