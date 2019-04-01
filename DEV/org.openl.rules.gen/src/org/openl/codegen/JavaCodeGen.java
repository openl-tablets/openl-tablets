package org.openl.codegen;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.openl.message.Severity;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.properties.def.TablePropertyDefinition.SystemValuePolicy;
import org.openl.rules.table.properties.expressions.match.MatchingExpression;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ISelector;
import org.openl.util.RuntimeExceptionWrapper;

public class JavaCodeGen implements ICodeGen {

    public static final String PRIVATE = "private";
    public static final String PUBLIC = "public";
    public static final String STATIC = "static";
    public static final String CLASS = "class";

    public static final String END_OF_EXPR = ";\n";
    public static final String START_MULTILINE_COMMENT = "/*\n";
    public static final String MULTILINE_COMMENT = " * ";
    public static final String END_MULTILINE_COMMENT = " */\n";
    public static final String START_SINGLELINE_COMMENT = " // ";
    public static final String ASSIGN_OP = "=";
    public static final String CHAIN_OP = ".";
    public static final String START_METHOD_BRACE = "(";
    public static final String END_METHOD_BRACE = ")";
    public static final String CLASS_SUFFIX = ".class";
    public static final String NEW_OP = "new";
    public static final String NULL_CONST = "null";

    public static final int MODULE_LEVEL = 0;
    public static final int CLASS_DECLARATION_LEVEL = 0;
    public static final int METHOD_DECLARATION_LEVEL = 1;
    public static final int METHOD_BODY_LEVEL = 2;

    public static final int MAX_STR_LEN = 80;

    private String jpackage = "org.openl.gen";

    private int genLevel = 0;
    private int dprecision = 4;
    private DecimalFormat format = new DecimalFormat("#.0###", new DecimalFormatSymbols(new Locale("en")));

    private JavaCodeGenController ctr = new JavaCodeGenController();

    public String getJpackage() {
        return jpackage;
    }

    public void setJpackage(String jpackage) {
        this.jpackage = jpackage;
    }

    @Override
    public StringBuilder genClass(IOpenClass ioc, ISelector<IOpenMember> sel, StringBuilder sb) {
        return null;
    }

    @Override
    public StringBuilder genField(IOpenField m, StringBuilder sb) {
        return null;
    }

    @Override
    public StringBuilder genLiteralArray(Object ary, ICodeGenController ctr, StringBuilder sb) {

        sb.append("new ").append(ary.getClass().getSimpleName()).append(" ");

        sb.append('{');

        int len = Array.getLength(ary);

        for (int i = 0; i < len; i++) {
            if (i > 0) {
                sb.append(", ");
            }

            ctr.processLiteralValue(Array.get(ary, i), this, sb);
        }

        return sb.append('}');
    }

    @Override
    public StringBuilder genLiteralChar(Character src, StringBuilder sb) {

        sb.append('\'');

        genEscapedChar(src, sb);

        return sb.append('\'');
    }

    @Override
    public StringBuilder genLiteralDouble(Double src, StringBuilder sb) {

        sb.append(format.format(src));

        return sb;
    }

    @Override
    public StringBuilder genLiteralInt(Integer src, StringBuilder sb) {

        sb.append(src.toString());

        return sb;
    }

    @Override
    public StringBuilder genLiteralString(String src, StringBuilder sb) {

        if (src == null) {
            return genLiteralNull(sb);
        }

        int len = src.length();

        sb.append("\"");

        for (int i = 0; i < len; i++) {

            char c = src.charAt(i);

            if (i % MAX_STR_LEN == 0 && i > 0) {
                sb.append("\"\r\n");
                startLine(sb);
                sb.append(" + \"");
            }

            genEscapedChar(c, sb);
        }

        sb.append('"');

        return sb;
    }

    @Override
    public StringBuilder genLiteralNull(StringBuilder sb) {
        return sb.append(NULL_CONST);
    }

    @Override
    public StringBuilder genMethod(IOpenMethod m, StringBuilder sb) {
        return null;
    }

    @Override
    public StringBuilder genMethodEnd(IOpenMethod m, StringBuilder sb) {
        return null;
    }

    @Override
    public StringBuilder genMethodStart(IOpenMethod m, StringBuilder sb) {
        return null;
    }

    @Override
    public StringBuilder genMultiLineComment(String comment, StringBuilder sb) {

        startLine(sb);
        sb.append(START_MULTILINE_COMMENT);

        String[] lines = comment.split("\n");

        for (String line : lines) {
            startLine(sb);
            sb.append(MULTILINE_COMMENT).append(line).append("\r\n");
        }

        startLine(sb);

        sb.append(END_MULTILINE_COMMENT);

        return sb;
    }

    @Override
    public StringBuilder genSingleLineComment(String comment, StringBuilder sb) {
        sb.append(START_SINGLELINE_COMMENT).append(comment).append("\r\n");

        return sb;
    }

    @Override
    public StringBuilder genClassEnd(IOpenClass ioc, StringBuilder sb) {
        return null;
    }

    @Override
    public StringBuilder genClassStart(IOpenClass ioc, StringBuilder sb) {
        return null;
    }

    @Override
    public StringBuilder genModuleEnd(IOpenClass ioc, StringBuilder sb) {
        return sb;
    }

    @Override
    public StringBuilder genModuleStart(IOpenClass ioc, StringBuilder sb) {
        sb.append("package ").append(jpackage).append(END_OF_EXPR);
        return sb;
    }

    public StringBuilder genEscapedChar(char c, StringBuilder sb) {

        if (c > 0xff) {

            sb.append("\\u");

            for (int mask = 0x1000; mask > 0; mask >>= 4) {

                char c1 = (char) (c / mask);
                c1 += c1 < 10 ? '0' : 'a' - 10;
                sb.append(c1);
                c -= c / mask * mask;
            }
        } else {
            switch (c) {
                case '\b':
                    sb.append("\\b");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\'':
                    sb.append("\\'");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append('\\');

                        for (int mask = 0x40; mask > 0; mask >>= 3) {
                            char c1 = (char) (c / mask + '0');
                            sb.append(c1);
                            c -= c / mask * mask;
                        }
                    } else {
                        sb.append(c);
                    }
            }
        }

        return sb;
    }

    /**
     * Generates array in form Foo[] x = new Foo[size];
     *
     * @param name
     * @param className
     * @param size
     * @param sb
     * @return
     */
    public StringBuilder genInitFixedSizeArrayVar(String name, String className, int size, StringBuilder sb) {

        sb.append(name)
            .append(" = ")
            .append("new ")
            .append(className)
            .append('[')
            .append(size)
            .append(']')
            .append(END_OF_EXPR);

        return sb;
    }

    public StringBuilder startLine(StringBuilder sb) {
        return startLine(sb, genLevel);
    }

    public StringBuilder startLine(StringBuilder sb, int genlevel) {

        for (int i = 0; i < genlevel; i++) {
            sb.append('\t');
        }

        return sb;
    }

    @Override
    public StringBuilder genAttribute(IOpenField m, StringBuilder sb) {
        return null;
    }

    @Override
    public StringBuilder genLiteralBool(Boolean src, StringBuilder sb) {
        return sb.append(src ? "true" : "false");
    }

    @Override
    public int setDoublePrecision(int dprecision) {
        if (this.dprecision == dprecision) {
            return dprecision;
        }

        this.dprecision = dprecision;
        format.setMaximumFractionDigits(dprecision);

        return this.dprecision;
    }

    @Override
    public StringBuilder genLiteralLong(Long src, StringBuilder sb) {
        return sb.append(src.toString()).append('L');
    }

    public void setGenLevel(int genLevel) {
        this.genLevel = genLevel;
    }

    public int getGenLevel() {
        return genLevel;
    }

    public StringBuilder genSetNewObjectInArray(int i,
            String aryName,
            Object bean,
            String className,
            StringBuilder sb) {
        sb.append(aryName);

        genConstIndexExpr(i, sb);
        genOp(ASSIGN_OP, sb);

        if (bean == null) {
            genLiteralNull(sb);
        } else {
            genNewObject(className, sb);
        }

        return sb.append(END_OF_EXPR);
    }

    public StringBuilder genNewObject(String className, StringBuilder sb) {

        return sb.append(NEW_OP).append(' ').append(className).append("()");
    }

    public StringBuilder genConstIndexExpr(int index, StringBuilder sb) {

        return sb.append('[').append(index).append(']');
    }

    @Override
    public StringBuilder genLiteralJavaOpenClass(JavaOpenClass jc, StringBuilder sb) {

        sb.append(JavaOpenClass.class.getName()).append(".getOpenClass(");

        if (jc.getInstanceClass().isArray()) {
            sb.append(jc.getInstanceClass().getComponentType().getName()).append("[]");
        } else {
            sb.append(jc.getInstanceClass().getName());
        }

        sb.append(CLASS_SUFFIX).append(")");

        return sb;
    }

    public StringBuilder genOp(String op, StringBuilder sb) {

        return sb.append(' ').append(op).append(' ');
    }

    public String genInitializeBeanArray(String aryName,
            Object[] beans,
            Class<?> beanClass,
            Collection<String> props,
            StringBuilder sb) {

        BeanInfo info = null;

        try {
            info = Introspector.getBeanInfo(beanClass);
        } catch (Throwable t) {
            throw RuntimeExceptionWrapper.wrap(t);
        }

        PropertyDescriptor[] pdd = info.getPropertyDescriptors();
        List<PropertyDescriptor> pdlist = new ArrayList<>();

        for (PropertyDescriptor pd : pdd) {
            if (props != null && !props.contains(pd.getName())) {
                continue;
            }

            if (pd.getWriteMethod() == null || pd.getReadMethod() == null) {
                continue;
            }

            pdlist.add(pd);
        }

        String shortClassName = beanClass.getSimpleName();

        startLine(sb);
        genInitFixedSizeArrayVar(aryName, shortClassName, beans.length, sb);

        for (int i = 0; i < beans.length; i++) {
            genBeanInArray(i, aryName, beans[i], shortClassName, pdlist, sb);
        }

        return sb.toString();
    }

    public StringBuilder genBeanInArray(int i,
            String aryName,
            Object bean,
            String className,
            List<PropertyDescriptor> pdlist,
            StringBuilder sb) {

        startLine(sb);
        genSetNewObjectInArray(i, aryName, bean, className, sb);

        if (bean == null) {
            return sb;
        }

        for (PropertyDescriptor propertyDescriptor : pdlist) {

            Object value = null;

            try {
                value = propertyDescriptor.getReadMethod().invoke(bean);
            } catch (Throwable t) {
                throw RuntimeExceptionWrapper.wrap(t);
            }

            if (value == null) {
                continue;
            }

            startLine(sb);
            sb.append(aryName);

            genConstIndexExpr(i, sb);

            sb.append(CHAIN_OP);
            sb.append(propertyDescriptor.getWriteMethod().getName());
            sb.append(START_METHOD_BRACE);

            ctr.processLiteralValue(value, this, sb);

            sb.append(END_METHOD_BRACE);
            sb.append(END_OF_EXPR);
        }

        return sb;
    }

    @Override
    public StringBuilder genLiteralConstraints(Constraints value, StringBuilder sb) {
        return sb.append("new ")
            .append(Constraints.class.getName())
            .append("(\"")
            .append(value.getConstraintsStr())
            .append("\")");
    }

    @Override
    public StringBuilder genLiteralSystemValuePolicy(SystemValuePolicy value, StringBuilder sb) {
        return sb.append(SystemValuePolicy.class.getSimpleName()).append(".").append(value);
    }

    @Override
    public StringBuilder genLiteralLevelInheritance(InheritanceLevel value, StringBuilder sb) {
        return sb.append(InheritanceLevel.class.getSimpleName()).append(".").append(value.name());
    }

    @Override
    public StringBuilder genLiteralMatchingExpression(MatchingExpression value, StringBuilder sb) {
        return sb.append("new ")
            .append(MatchingExpression.class.getName())
            .append("(\"")
            .append(value.getMatchExpressionStr())
            .append("\")");
    }

    @Override
    public StringBuilder genLiteralTableType(XlsNodeTypes value, StringBuilder sb) {
        return sb.append(XlsNodeTypes.class.getSimpleName()).append(".").append(value.name());
    }

    @Override
    public StringBuilder genLiteralErrorSeverity(Severity value, StringBuilder sb) {
        return sb.append(Severity.class.getSimpleName()).append(".").append(value.name());
    }
}
