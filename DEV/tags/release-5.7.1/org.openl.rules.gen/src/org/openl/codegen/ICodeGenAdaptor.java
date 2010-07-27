package org.openl.codegen;

public interface ICodeGenAdaptor {

    void processInsertTag(String line, StringBuilder sb);

    void processEndInsertTag(String line, StringBuilder sb);

}
