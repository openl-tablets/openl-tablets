package org.openl.gen;

import java.util.Objects;
import java.util.Optional;
import org.objectweb.asm.Type;

/**
 * Type description.
 *
 * @author Vladyslav Pikus
 */
public class TypeDescription {

    private final String typeName;
    private final String canonicalName;
    private final String typeDescriptor;
    private final AnnotationDescription[] annotations;

    /**
     * Initialize type description with given parameters
     *
     * @param typeName type name
     * @throws NullPointerException if type name is null
     */
    public TypeDescription(String typeName) {
        this(typeName, AnnotationDescription.EMPTY_ANNOTATIONS);
    }

    /**
     * Initialize type description with given parameters
     *
     * @param typeName type name
     * @param annotations type annotations
     * @throws NullPointerException if type name is null
     */
    TypeDescription(String typeName, AnnotationDescription[] annotations) {
        this.typeName = Objects.requireNonNull(typeName, "Type name is null.");
        this.canonicalName = Type.getObjectType(typeName).getClassName();
        switch (typeName) {
            case "byte":
                this.typeDescriptor = "B";
                break;
            case "short":
                this.typeDescriptor = "S";
                break;
            case "int":
                this.typeDescriptor = "I";
                break;
            case "long":
                this.typeDescriptor = "J";
                break;
            case "float":
                this.typeDescriptor = "F";
                break;
            case "double":
                this.typeDescriptor = "D";
                break;
            case "boolean":
                this.typeDescriptor = "Z";
                break;
            case "char":
                this.typeDescriptor = "C";
                break;
            case "void":
                this.typeDescriptor = "V";
                break;
            default:
                String internal = typeName;
                if (typeName.charAt(0) != '[') {
                    internal = 'L' + internal + ';';
                }
                this.typeDescriptor = internal.replace('.', '/');
                break;
        }
        this.annotations = Optional.ofNullable(annotations).orElse(AnnotationDescription.EMPTY_ANNOTATIONS);
    }

    /**
     * Get original type name
     *
     * @return original type name
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Get type descriptor
     *
     * @return type descriptor
     */
    public String getTypeDescriptor() {
        return typeDescriptor;
    }

    /**
     * Check if type is an array
     * 
     * @return {@code true} if type is an array, otherwise {@code false}
     */
    public boolean isArray() {
        return typeName.indexOf('[') >= 0;
    }

    /**
     * Get type annotations
     *
     * @return type annotations
     */
    public AnnotationDescription[] getAnnotations() {
        return annotations;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TypeDescription that = (TypeDescription) o;
        return Objects.equals(typeName, that.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName);
    }

    @Override
    public String toString() {
        return typeName;
    }
}
