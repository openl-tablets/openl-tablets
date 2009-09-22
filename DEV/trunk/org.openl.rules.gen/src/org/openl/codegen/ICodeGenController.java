package org.openl.codegen;

public interface ICodeGenController {
    StringBuilder processLiteralValue(Object value, ICodeGen gen, StringBuilder sb);
}
