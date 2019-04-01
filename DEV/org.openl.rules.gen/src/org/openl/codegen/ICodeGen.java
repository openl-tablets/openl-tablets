package org.openl.codegen;

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

public interface ICodeGen {

    StringBuilder genModuleStart(IOpenClass ioc, StringBuilder sb);

    StringBuilder genModuleEnd(IOpenClass ioc, StringBuilder sb);

    StringBuilder genClass(IOpenClass ioc, ISelector<IOpenMember> sel, StringBuilder sb);

    StringBuilder genClassStart(IOpenClass ioc, StringBuilder sb);

    StringBuilder genClassEnd(IOpenClass ioc, StringBuilder sb);

    StringBuilder genMethod(IOpenMethod m, StringBuilder sb);

    StringBuilder genMethodStart(IOpenMethod m, StringBuilder sb);

    StringBuilder genMethodEnd(IOpenMethod m, StringBuilder sb);

    StringBuilder genField(IOpenField m, StringBuilder sb);

    StringBuilder genAttribute(IOpenField m, StringBuilder sb);

    StringBuilder genLiteralString(String src, StringBuilder sb);

    StringBuilder genLiteralInt(Integer src, StringBuilder sb);

    StringBuilder genLiteralDouble(Double src, StringBuilder sb);

    StringBuilder genLiteralLong(Long src, StringBuilder sb);

    StringBuilder genLiteralChar(Character src, StringBuilder sb);

    StringBuilder genLiteralBool(Boolean src, StringBuilder sb);

    StringBuilder genLiteralArray(Object ary, ICodeGenController codeGenController, StringBuilder sb);

    StringBuilder genMultiLineComment(String comment, StringBuilder sb);

    StringBuilder genSingleLineComment(String comment, StringBuilder sb);

    int setDoublePrecision(int dprecision);

    StringBuilder genLiteralNull(StringBuilder sb);

    StringBuilder genLiteralJavaOpenClass(JavaOpenClass jc, StringBuilder sb);

    StringBuilder genLiteralConstraints(Constraints value, StringBuilder sb);

    StringBuilder genLiteralSystemValuePolicy(SystemValuePolicy value, StringBuilder sb);

    StringBuilder genLiteralLevelInheritance(InheritanceLevel value, StringBuilder sb);

    StringBuilder genLiteralMatchingExpression(MatchingExpression value, StringBuilder sb);

    StringBuilder genLiteralTableType(XlsNodeTypes value, StringBuilder sb);

    StringBuilder genLiteralErrorSeverity(Severity value, StringBuilder sb);

}
