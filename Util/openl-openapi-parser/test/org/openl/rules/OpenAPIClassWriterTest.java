package org.openl.rules;

import java.io.FileOutputStream;

import org.junit.Ignore;
import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.openapi.OpenAPIModelConverter;
import org.openl.rules.openapi.impl.OpenAPIClassWriter;
import org.openl.rules.openapi.impl.OpenAPIScaffoldingConverter;
import org.openl.util.generation.InterfaceTransformer;

public class OpenAPIClassWriterTest {

    @Test
    public void testOpenAPIPathInfo() throws Throwable {
        OpenAPIModelConverter converter = new OpenAPIScaffoldingConverter();
        ProjectModel projectModel = converter.extractProjectModel("test.converter/paths/slashProblem.json");
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        OpenAPIClassWriter openAPIClassWriter = new OpenAPIClassWriter(cw, projectModel);
        InterfaceTransformer it = new InterfaceTransformer(OpenAPIInterface.class,
            "org.openl.rules.project/OpenAPIService");
        it.accept(openAPIClassWriter);
        cw.visitEnd();
        byte[] b = cw.toByteArray();

        try (FileOutputStream fos = new FileOutputStream("OpenAPIService.class")) {
            fos.write(b);
        }

    }

    public interface OpenAPIInterface {
    }
}
