package org.openl.codegen.dev;

import java.io.IOException;

import org.openl.codegen.FileCodeGen;
import org.openl.codegen.ICodeGenAdaptor;
import org.openl.codegen.JavaCodeGen;
import org.openl.rules.runtime.RuleEngineFactory;
import org.openl.rules.table.properties.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.TablePropertyDefinition;
import org.openl.util.StringTool;

public class GenTablePropertyDefinitions implements ICodeGenAdaptor {
    
    private static final String SOURCE_LOC = "../org.openl.rules/src/",
            //TMP_FILE = "DefaultPropertyDefinitions.java";//replace TMP_FILE with null, it will generate java "in-place"
            TMP_FILE = null;//replace TMP_FILE with null, it will generate java "in-place"
    private static final String ARRAY_NAME = "definitions";
    private static final String DEFINITIONS_XLS = "../org.openl.rules/doc/TablePropertyDefinition.xlsx";

    public static void main(String[] args) throws IOException {
        new GenTablePropertyDefinitions().run();
    }

    TablePropertyDefinition[] tpdd;
    
    private void run() throws IOException 
    {
        tpdd = loadDefinitions();
        
        //Once you achieve the desired result, replace TMP_FILE with null, it will generate java "in-place"
        FileCodeGen fileGen = new FileCodeGen(SOURCE_LOC + StringTool.getFileNameOfJavaClass(DefaultPropertyDefinitions.class), TMP_FILE, null);
        
        fileGen.processFile(this);
    }

    static interface IDefinitionLoader
    {
        TablePropertyDefinition[] getDefinitions();
    }
    
    private TablePropertyDefinition[] loadDefinitions() {
        RuleEngineFactory<IDefinitionLoader> rf = new RuleEngineFactory<IDefinitionLoader>(DEFINITIONS_XLS, IDefinitionLoader.class);
        return rf.newInstance().getDefinitions();
    }
    
    

    public void processInsertTag(String line, StringBuilder sb) 
    {
        JavaCodeGen jcgen = new JavaCodeGen();
        jcgen.setGenLevel(JavaCodeGen.METHOD_BODY_LEVEL);
        jcgen.genInitializeBeanArray(ARRAY_NAME, tpdd, TablePropertyDefinition.class, null, sb);
    }
    

}
