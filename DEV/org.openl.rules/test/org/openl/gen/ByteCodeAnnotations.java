package org.openl.gen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * The annotations of the fields and the methods of a generated class, read from its byte code.
 *
 * <p>The generators write annotations of packages this module does not depend on, which reflection leaves out,
 * so a test reads them as byte code. Each annotation is a map of its values: a nested annotation is a map, an
 * array is a list.
 */
public final class ByteCodeAnnotations {

    private ByteCodeAnnotations() {
    }

    /** The annotations of every field and method, by member name, then by annotation descriptor. */
    public static Map<String, Map<String, Map<String, Object>>> read(byte[] byteCode) {
        var members = new LinkedHashMap<String, Map<String, Map<String, Object>>>();
        new ClassReader(byteCode).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                return new FieldVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        return annotationOf(members, name, descriptor);
                    }
                };
            }

            @Override
            public MethodVisitor visitMethod(int access,
                                             String name,
                                             String descriptor,
                                             String signature,
                                             String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        return annotationOf(members, name, descriptor);
                    }
                };
            }
        }, 0);
        return members;
    }

    private static AnnotationVisitor annotationOf(Map<String, Map<String, Map<String, Object>>> members,
                                                  String member,
                                                  String descriptor) {
        return collect(members.computeIfAbsent(member, key -> new LinkedHashMap<>())
                .computeIfAbsent(descriptor, key -> new LinkedHashMap<>()));
    }

    private static AnnotationVisitor collect(Map<String, Object> values) {
        return new AnnotationVisitor(Opcodes.ASM9) {
            @Override
            public void visit(String name, Object value) {
                values.put(name, value);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                var nested = new LinkedHashMap<String, Object>();
                values.put(name, nested);
                return collect(nested);
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                var items = new ArrayList<>();
                values.put(name, items);
                return new AnnotationVisitor(Opcodes.ASM9) {
                    @Override
                    public void visit(String ignored, Object value) {
                        items.add(value);
                    }
                };
            }
        };
    }

    /** The values of one annotation of a member, or {@code null} when the member does not carry it. */
    public static Map<String, Object> of(Map<String, Map<String, Map<String, Object>>> members,
                                         String member,
                                         String descriptor) {
        var annotations = members.get(member);
        return annotations == null ? null : annotations.get(descriptor);
    }

    @SuppressWarnings("unchecked")
    public static List<Object> list(Object value) {
        return (List<Object>) value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}
