package org.openl.rules.ui.tablewizard;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.faces.model.SelectItem;

import javax.faces.model.SelectItem;

import org.openl.base.INamedThing;
import org.openl.rules.lang.xls.classes.ClassFinder;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Aliaksandr Antonik.
 */
public final class WizardUtils {

    private WizardUtils() {
    }

    static List<String> declaredDatatypes() {
        return getProjectOpenClass().getTypes()
            .stream()
            .filter(t -> t instanceof DatatypeOpenClass)
            .map(IOpenClass::getName)
            .sorted()
            .collect(Collectors.toList());
    }

    static List<String> declaredAliases() {
        return getProjectOpenClass().getTypes()
            .stream()
            .filter(t -> t instanceof DomainOpenClass)
            .map(IOpenClass::getName)
            .sorted()
            .collect(Collectors.toList());
    }

    static List<String> importedClasses() {
        return getImportedClasses().stream()
            .filter(t -> !(t instanceof DomainOpenClass))
            .map(IOpenClass::getName)
            .sorted()
            .collect(Collectors.toList());
    }

    public static IOpenClass getProjectOpenClass() {
        return WebStudioUtils.getProjectModel().getCompiledOpenClass().getOpenClassWithErrors();
    }

    public static XlsModuleSyntaxNode getXlsModuleNode() {
        return WebStudioUtils.getProjectModel().getXlsModuleNode();
    }

    public static TableSyntaxNode[] getTableSyntaxNodes() {
        return WebStudioUtils.getProjectModel().getTableSyntaxNodes();
    }

    /**
     * Get imported classes for current project
     *
     * @return collection, containing an imported classes
     */
    public static Collection<IOpenClass> getImportedClasses() {
        Set<IOpenClass> classes = new TreeSet<>(
            (o1, o2) -> o1.getDisplayName(INamedThing.SHORT).compareToIgnoreCase(o2.getDisplayName(INamedThing.SHORT)));

        ClassFinder finder = new ClassFinder();
        for (String packageName : getXlsModuleNode().getImports()) {
            if ("org.openl.rules.enumeration".equals(packageName)) {
                // This package is added automatically in XlsLoader.addInnerImports() for inner usage, not for user.
                continue;
            }
            ClassLoader classLoader = WebStudioUtils.getProjectModel().getCompiledOpenClass().getClassLoader();
            for (Class<?> type : finder.getClasses(packageName, classLoader)) {
                IOpenClass openType;
                try {
                    openType = JavaOpenClass.getOpenClass(type);
                } catch (Exception e) {
                    // For example NoClassDefFoundError when the class for some of the fields is absent.
                    final Logger log = LoggerFactory.getLogger(WizardUtils.class);
                    log.debug("Cannot load the class, skip it because it's not valid. Cause: {}", e.getMessage(), e);
                    continue;
                }
                if (!isValid(openType)) {
                    continue;
                }

                classes.add(openType);
            }
        }

        return classes;
    }

    /**
     * Check if type is valid (for example, it can be used in a DataType tables, Data tables etc)
     *
     * @param openType checked type
     * @return true if class is valid.
     */
    private static boolean isValid(IOpenClass openType) {
        Class<?> instanceClass = openType.getInstanceClass();

        int modifiers = instanceClass.getModifiers();
        if (!Modifier.isPublic(modifiers) || Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)) {
            return false;
        }

        // Every field has a "class" field. We skip a classes that does not
        // have any other field.
        return !openType.getFields().isEmpty();

    }

    /**
     * Creates an array of <code>SelectItem</code>s from collection of <code>String</code>s.
     *
     * @param values an array of <code>SelectItem</code> values.
     * @return array of JSF objects representing items.
     */
    public static SelectItem[] createSelectItems(Collection<String> values) {
        SelectItem[] items = new SelectItem[values.size()];
        int index = 0;
        for (String value : values) {
            items[index++] = new SelectItem(value);
        }
        return items;
    }
}
