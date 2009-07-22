package org.openl.codegen;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.util.ISelector;
import org.openl.util.StringTool;

public class JavaCodeGen implements ICodeGen {

    static final public String PRIVATE = "private", PUBLIC = "public",
            STATIC = "static", CLASS = "class";

    static final public String END_OF_EXPR = ";\n",
            START_MULTILINE_COMMENT = "/*\n", MULTILINE_COMMENT = " * ",
            END_MULTILINE_COMMENT = " */\n", START_SINGLELINE_COMMENT = " // ";

    private String jpackage;
    private String comment;
    private ICodeGenContext cxt;
    private int genLevel;

    public JavaCodeGen(String jpackage, String comment, int genLevel) {
        this.jpackage = jpackage;
        this.comment = comment;
        this.genLevel = genLevel;
    }

    public StringBuilder genClass(IOpenClass ioc, ISelector<IOpenMember> sel,
            StringBuilder sb) {
        // TODO Auto-generated method stub

        return null;
    }

    public StringBuilder genField(IOpenField m, StringBuilder sb) {
        // TODO Auto-generated method stub
        return null;
    }

    public StringBuilder genLiteralArray(Object ary, StringBuilder sb) {
        // TODO Auto-generated method stub
        return null;
    }

    public StringBuilder genLiteralChar(Character src, StringBuilder sb) {
        sb.append('\'');
        genEscapedChar(src, sb);
        return sb.append('\'');
    }

    public StringBuilder genLiteralDouble(Double src, int dprecision,
            StringBuilder sb) {
        // TODO Auto-generated method stub
        return null;
    }

    public StringBuilder genLiteralInt(Integer src, StringBuilder sb) {
        // TODO Auto-generated method stub
        return null;
    }

    static public int MAX_STR_LEN;

    public StringBuilder genLiteralString(String src, StringBuilder sb) {
        if (src == null)
            return genLiteralNull(sb);
        int len = src.length();

        sb.append("\"");
        for (int i = 0; i < len; i++) {
            if (i % MAX_STR_LEN == 0 && i > 0) {
                sb.append("\"\n");
                startLine(sb);
                sb.append(" + \"");
            }
        }
        return sb;
    }

    public StringBuilder genLiteralNull(StringBuilder sb) {
        return sb.append("null");
    }

    public StringBuilder genMethod(IOpenMethod m, StringBuilder sb) {
        // TODO Auto-generated method stub
        return null;
    }

    public StringBuilder genMethodEnd(IOpenMethod m, StringBuilder sb) {
        // TODO Auto-generated method stub
        return null;
    }

    public StringBuilder genMethodStart(IOpenMethod m, StringBuilder sb) {
        // TODO Auto-generated method stub
        return null;
    }

    public StringBuilder genMultiLineComment(String comment, StringBuilder sb) {
        startLine(sb);
        sb.append(START_MULTILINE_COMMENT);
        String[] lines = StringTool.tokenize(comment, "\n");

        for (String line : lines) {
            startLine(sb);
            sb.append(MULTILINE_COMMENT).append(line).append('\n');
        }
        startLine(sb);
        sb.append(END_MULTILINE_COMMENT);

        return sb;
    }

    public StringBuilder genSingleLineComment(String comment, StringBuilder sb) {
        sb.append(START_SINGLELINE_COMMENT).append(comment).append('\n');
        return sb;
    }

    public StringBuilder genClassEnd(IOpenClass ioc, StringBuilder sb) {
        // TODO Auto-generated method stub
        return null;
    }

    public StringBuilder genClassStart(IOpenClass ioc, StringBuilder sb) {
        // TODO Auto-generated method stub
        return null;
    }

    public StringBuilder genModuleEnd(IOpenClass ioc, StringBuilder sb) {
        return sb;
    }

    public StringBuilder genModuleStart(IOpenClass ioc, StringBuilder sb) {
        sb.append("package ").append(jpackage).append(END_OF_EXPR);
        return sb;
    }

    public StringBuilder genBeanAttribute(IOpenField f, StringBuilder sb) {
        String name = f.getName();
        String type = cxt.addReferredType(f.getType());

        String cmt = "Bean Attribute: " + name;

        genMultiLineComment(comment, sb);

        startLine(sb);
        return null;
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
            case 't':
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
                } else
                    sb.append(c);
            }
        }

        return sb;
    }

    private void startLine(StringBuilder sb) {
        for (int i = 0; i < genLevel; i++) {
            sb.append('\t');
        }
    }

    public StringBuilder genAttribute(IOpenField m, StringBuilder sb) {
        // TODO Auto-generated method stub
        return null;
    }

    public StringBuilder genLiteralBool(Boolean src, StringBuilder sb) {
        // TODO Auto-generated method stub
        return null;
    }

}
