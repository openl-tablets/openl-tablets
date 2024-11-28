package org.openl.rules.openapi;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.ReferenceTypeUtils;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.lang3.StringUtils;

/**
 * This converter fix some cases when references of the schemas are inlined in OpenAPI instead of keep real model structure.
 * The inlined schema are still correct to represent API, but cannot be used to restore back Java beans inheritance.
 *
 * @author Yury Molchan
 */
class InheritanceFixConverter implements ModelConverter {
    private final List<ModelConverter> converters;

    public InheritanceFixConverter(List<ModelConverter> converters) {
        this.converters = Objects.requireNonNull(converters, "converters cannot be null");
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Schema resolve(AnnotatedType annotatedType, ModelConverterContext context, Iterator<ModelConverter> chain) {
        var modelConverterContext = new ModelConverterContextImpl(converters);
        modelConverterContext.resolve(annotatedType);
        for (AnnotatedType aType : new LinkedHashSet<>(modelConverterContext.modelByType.keySet())) {
            var type = aType.getType();
            Class<?> clazz = null;
            if (type instanceof JavaType) {
                clazz = ((JavaType) type).getRawClass();
            } else if (type instanceof Class) {
                clazz = (Class<?>) type;
            }
            Class<?> parentClass = clazz;
            while (parentClass != null && parentClass.getSuperclass() != null && parentClass.getSuperclass() != Object.class) {
                parentClass = parentClass.getSuperclass();
            }
            if (parentClass != clazz) {
                // Collect and cache the parent schemas
                var modelConverterContext1 = new ModelConverterContextImpl(converters);
                modelConverterContext1.resolve(new AnnotatedType(parentClass));
                modelConverterContext1.getDefinedModels().forEach(modelConverterContext::defineModel);
                modelConverterContext.modelByType.putAll(modelConverterContext1.modelByType);
            }
        }
        var schema = modelConverterContext.resolve(annotatedType); // The second call just reuse the cached parents schemas
        modelConverterContext.getDefinedModels().forEach(context::defineModel);
        return schema;
    }

    /**
     * Used to access to the private fields without illegal reflection access.
     *
     * @see io.swagger.v3.core.converter.ModelConverterContextImpl (v2.2.22)
     */
    static class ModelConverterContextImpl implements ModelConverterContext {
        private final List<ModelConverter> converters;
        private final Map<String, Schema<?>> modelByName = new TreeMap<>();
        private final HashMap<AnnotatedType, Schema<?>> modelByType = new HashMap<>();
        private final Set<AnnotatedType> processedTypes = new HashSet<>();

        public ModelConverterContextImpl(List<ModelConverter> converters) {
            this.converters = converters;
        }

        @Override
        public Iterator<ModelConverter> getConverters() {
            return converters.iterator();
        }

        @Override
        public void defineModel(String name, Schema model) {
            modelByName.put(name, model);
        }

        @Override
        public void defineModel(String name, Schema model, Type type, String prevName) {
            defineModel(name, model, new AnnotatedType(type), prevName);
        }

        @Override
        public void defineModel(String name, Schema model, AnnotatedType type, String prevName) {
            modelByName.put(name, model);

            if (StringUtils.isNotBlank(prevName) && !prevName.equals(name)) {
                modelByName.remove(prevName);
            }

            if (type != null && type.getType() != null) {
                modelByType.put(type, model);
            }
        }

        @Override
        public Map<String, Schema> getDefinedModels() {
            return Collections.unmodifiableMap(modelByName);
        }

        @Override
        public Schema<?> resolve(AnnotatedType type) {

            if (converters.isEmpty()) {
                return null;
            }

            AnnotatedType aType = ReferenceTypeUtils.unwrapReference(type);
            if (aType != null) {
                type = aType;
            }

            if (processedTypes.contains(type)) {
                return modelByType.get(type);
            } else {
                processedTypes.add(type);
            }
            var itr = converters.iterator();
            var resolved = itr.next().resolve(type, this, itr);
            if (resolved != null) {
                modelByType.put(type, resolved);

                if (resolved.getName() != null) {
                    modelByName.put(resolved.getName(), resolved);
                }
            } else {
                processedTypes.remove(type);
            }

            return resolved;
        }
    }
}
