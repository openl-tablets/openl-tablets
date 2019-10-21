package org.openl.rules.maven.decompiler;

import java.io.File;

import org.apache.maven.plugin.logging.Log;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;

public class BytecodeDecompiler {

    private final File sourceRoot;
    private final FernflowerLoggerAdapter log;

    public BytecodeDecompiler(Log log, File sourceRoot) {
        this.sourceRoot = sourceRoot;
        this.log = new FernflowerLoggerAdapter(log);
    }

    public BytecodeDecompiler(Log log, String sourceRoot) {
        this(log, new File(sourceRoot));
    }

    public void decompile(String name, byte[] bytecode) {
        JavaFileSaver javaFilesSaver = new JavaFileSaver(sourceRoot, log.getLog());
        Fernflower engine = new Fernflower(wrapBytecode(bytecode),
            javaFilesSaver,
            IFernflowerPreferences.DEFAULTS,
            log);

        // emulate file system because of Fernflower works with files
        File file = new File(name.replace('.', '/') + ".class");
        engine.getStructContext().addSpace(file, true);
        engine.decompileContext();
        if (!javaFilesSaver.isFileSaved()) {
            throw new RuntimeException("Java file has not been saved for class " + name);
        }
    }

    private IBytecodeProvider wrapBytecode(byte[] bytecode) {
        return new IBytecodeProvider() {
            @Override
            public byte[] getBytecode(String externalPath, String internalPath) {
                return bytecode;
            }
        };
    }

}
