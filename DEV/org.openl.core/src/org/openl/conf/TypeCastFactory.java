package org.openl.conf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openl.binding.impl.StaticClassLibrary;
import org.openl.binding.impl.cast.CastFactory;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * @author Yury Molchan
 */
public class TypeCastFactory {

    TypeCastFactory(IOpenLConfiguration configuration) {
        this.configuration = configuration;
    }

    private final IOpenLConfiguration configuration;

    private final List<CastFactory> components = new ArrayList<>();

    public void addJavaCast(Class<?> libraryClassName) {
        CastFactory castFactory = new CastFactory();

        castFactory.setMethodFactory(new StaticClassLibrary(JavaOpenClass.getOpenClass(libraryClassName)));
        castFactory.setGlobalCastFactory(configuration);
        components.add(castFactory);
    }

    public Collection<CastFactory> getJavaCastComponents() {
        return Collections.unmodifiableList(components);
    }

    public IOpenCast getCast(IOpenClass from, IOpenClass to) {
        for (var component : components) {
            IOpenCast openCast = component.getCast(from, to);
            if (openCast != null) {
                return openCast;
            }
        }
        return null;
    }
}
