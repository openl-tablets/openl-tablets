package org.openl.rules.project;

import java.util.Map;

import com.rits.cloning.Cloner;
import com.rits.cloning.ObjenesisInstantiationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SafeCloner extends Cloner {
    public SafeCloner() {
        super(new ObjenesisInstantiationStrategy());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T cloneInternal(T o, Map<Object, Object> clones) {
        if (o instanceof Logger) {
            return (T) LoggerFactory.getLogger(o.getClass());
        }
        if (o instanceof ClassLoader) {
            // There is no need to clone ClassLoader
            return null;
        }
        return super.cloneInternal(o, clones);
    }
}
