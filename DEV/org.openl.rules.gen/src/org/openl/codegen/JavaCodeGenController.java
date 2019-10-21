/**
 *
 */
package org.openl.codegen;

import java.util.HashMap;
import java.util.Map;

import org.openl.message.Severity;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.properties.def.TablePropertyDefinition.SystemValuePolicy;
import org.openl.rules.table.properties.expressions.match.MatchingExpression;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenEnum;

/**
 * @author snshor Created Jul 23, 2009
 *
 */
public class JavaCodeGenController implements ICodeGenController {

    private Map<Class<?>, Processor> map = new HashMap<>();

    public JavaCodeGenController() {
        init();
    }

    private void init() {

        map.put(String.class, (value, gen, sb) -> gen.genLiteralString((String) value, sb));
        map.put(Integer.class, (value, gen, sb) -> gen.genLiteralInt((Integer) value, sb));
        map.put(Boolean.class, (value, gen, sb) -> gen.genLiteralBool((Boolean) value, sb));
        map.put(Character.class, (value, gen, sb) -> gen.genLiteralChar((Character) value, sb));
        map.put(Double.class, (value, gen, sb) -> gen.genLiteralDouble((Double) value, sb));
        map.put(Long.class, (value, gen, sb) -> gen.genLiteralLong((Long) value, sb));
        map.put(JavaOpenClass.class, (value, gen, sb) -> gen.genLiteralJavaOpenClass((JavaOpenClass) value, sb));
        // FIXME: copied code for JavaOpenClass.class, refactor to support
        // processing of successor classes
        map.put(JavaOpenEnum.class, (value, gen, sb) -> gen.genLiteralJavaOpenClass((JavaOpenClass) value, sb));
        map.put(Constraints.class, (value, gen, sb) -> gen.genLiteralConstraints((Constraints) value, sb));

        map.put(SystemValuePolicy.class,
            (value, gen, sb) -> gen.genLiteralSystemValuePolicy((SystemValuePolicy) value, sb));

        map.put(InheritanceLevel.class,
            (value, gen, sb) -> gen.genLiteralLevelInheritance((InheritanceLevel) value, sb));

        map.put(MatchingExpression.class,
            (value, gen, sb) -> gen.genLiteralMatchingExpression((MatchingExpression) value, sb));
        map.put(XlsNodeTypes.class, (value, gen, sb) -> gen.genLiteralTableType((XlsNodeTypes) value, sb));
        map.put(Severity.class, (value, gen, sb) -> gen.genLiteralErrorSeverity((Severity) value, sb));
    }

    @Override
    public StringBuilder processLiteralValue(Object value, ICodeGen gen, StringBuilder sb) {

        if (value == null) {
            return gen.genLiteralNull(sb);
        }

        Class<?> c = value.getClass();

        if (c.isArray()) {
            return gen.genLiteralArray(value, this, sb);
        }

        Processor p = map.get(c);

        if (p == null) {
            throw new RuntimeException("Cannot process literal class: " + c.getName());
        }

        return p.processValue(value, gen, sb);
    }

    public interface Processor {
        StringBuilder processValue(Object value, ICodeGen gen, StringBuilder sb);
    }
}
