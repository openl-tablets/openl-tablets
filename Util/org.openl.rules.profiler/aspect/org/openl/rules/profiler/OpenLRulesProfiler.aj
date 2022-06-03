package org.openl.rules.profiler;

import java.io.IOException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.binding.IBoundMethodNode;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetBoundNode;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.ColumnMatchBoundNode;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.method.table.MethodTableBoundNode;
import org.openl.rules.method.table.TableMethod;
import org.openl.rules.tbasic.Algorithm;
import org.openl.rules.tbasic.AlgorithmBoundNode;
import org.openl.types.IOpenMethodHeader;
import org.openl.util.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public aspect OpenLRulesProfiler {
    private Logger log = LoggerFactory.getLogger(getClass());

    private static final String PACKAGE_NAME_FOR_ENHANCED_CLASSES = "org.openl.rules.profiler";

    private static String getWrapperClassName(IOpenMethodHeader header, AMethodBasedNode boundNode) {
        return PACKAGE_NAME_FOR_ENHANCED_CLASSES + "." + header
                .getName() + "$" + boundNode.getModule().getName().replaceAll("[. -]", "_");
    }

    private static Class<?> makeWrapperClass(String wrapperClassName, Class<?> superclass) throws IOException,
            Exception {
        synchronized (OpenLRulesProfiler.class) {
            final String wrapperClassName1 = wrapperClassName;
            boolean f = false;
            int i = 1;
            while (!f) {
                try {
                    Thread.currentThread().getContextClassLoader().loadClass(wrapperClassName);
                    wrapperClassName = wrapperClassName1 + "$" + i++;
                } catch (ClassNotFoundException e) {
                    f = true;
                }
            }

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            cw.visit(Opcodes.V1_8,
                    Opcodes.ACC_PUBLIC,
                    wrapperClassName.replace(".", "/"),
                    null,
                    Type.getInternalName(superclass),
                    null);
            new ClassReader(Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(superclass.getName().replace('.', '/') + ".class"))
                    .accept(new ExecutableRulesMethodVisitor(Opcodes.ASM5, cw, superclass), ClassReader.EXPAND_FRAMES);
            cw.visitEnd();

            return ClassUtils
                    .defineClass(wrapperClassName, cw.toByteArray(), Thread.currentThread().getContextClassLoader());
        }
    }

    DecisionTable around(IOpenMethodHeader header, AMethodBasedNode boundNode): call(DecisionTable.new(IOpenMethodHeader, AMethodBasedNode)) && args(header, boundNode) {
        try {
            final String wrapperClassName = getWrapperClassName(header, boundNode);
            Class<?> subClass = makeWrapperClass(wrapperClassName, DecisionTable.class);
            return (DecisionTable) subClass.getConstructor(IOpenMethodHeader.class, AMethodBasedNode.class)
                    .newInstance(header, boundNode);
        } catch (Exception e) {
            log.error("OpenLRulesProfiler: InstantiationError!", e);
            return proceed(header, boundNode);
        }
    }

    Spreadsheet around(IOpenMethodHeader header, SpreadsheetBoundNode boundNode, boolean customSpreadsheetType): call(Spreadsheet.new(IOpenMethodHeader, SpreadsheetBoundNode, boolean)) && args(header, boundNode, customSpreadsheetType) {
        try {
            final String wrapperClassName = getWrapperClassName(header, boundNode);
            Class<?> subClass = makeWrapperClass(wrapperClassName, Spreadsheet.class);
            return (Spreadsheet) subClass
                    .getConstructor(IOpenMethodHeader.class, SpreadsheetBoundNode.class, boolean.class)
                    .newInstance(header, boundNode, customSpreadsheetType);
        } catch (Exception e) {
            log.error("OpenLRulesProfiler: InstantiationError!", e);
            return proceed(header, boundNode, customSpreadsheetType);
        }
    }

    TableMethod around(IOpenMethodHeader header,
                       IBoundMethodNode methodBodyBoundNode,
                       MethodTableBoundNode methodTableBoundNode): call(TableMethod.new(IOpenMethodHeader, IBoundMethodNode, MethodTableBoundNode)) && args(header, methodBodyBoundNode, methodTableBoundNode) {
        try {
            final String wrapperClassName = getWrapperClassName(header, methodTableBoundNode);
            Class<?> subClass = makeWrapperClass(wrapperClassName, TableMethod.class);
            return (TableMethod) subClass
                    .getConstructor(IOpenMethodHeader.class, IBoundMethodNode.class, MethodTableBoundNode.class)
                    .newInstance(header, methodBodyBoundNode, methodTableBoundNode);
        } catch (Exception e) {
            log.error("OpenLRulesProfiler: InstantiationError!", e);
            return proceed(header, methodBodyBoundNode, methodTableBoundNode);
        }
    }

    ColumnMatch around(IOpenMethodHeader header, ColumnMatchBoundNode node): call(ColumnMatch.new(IOpenMethodHeader, ColumnMatchBoundNode)) && args(header, node) {
        try {
            final String wrapperClassName = getWrapperClassName(header, node);
            Class<?> subClass = makeWrapperClass(wrapperClassName, ColumnMatch.class);
            return (ColumnMatch) subClass.getConstructor(IOpenMethodHeader.class, ColumnMatchBoundNode.class)
                    .newInstance(header, node);
        } catch (Exception e) {
            log.error("OpenLRulesProfiler: InstantiationError!", e);
            return proceed(header, node);
        }
    }

    Algorithm around(IOpenMethodHeader header, AlgorithmBoundNode node): call(Algorithm.new(IOpenMethodHeader, AlgorithmBoundNode)) && args(header, node) {
        try {
            final String wrapperClassName = getWrapperClassName(header, node);
            Class<?> subClass = makeWrapperClass(wrapperClassName, Algorithm.class);
            return (Algorithm) subClass.getConstructor(IOpenMethodHeader.class, AlgorithmBoundNode.class)
                    .newInstance(header, node);
        } catch (Exception e) {
            log.error("OpenLRulesProfiler: InstantiationError!", e);
            return proceed(header, node);
        }
    }

}
