package org.openl.rules.project;

import com.rits.cloning.Cloner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

public class SafeCloner extends Cloner {
    @Override
    @SuppressWarnings("unchecked")
    public <T> T cloneInternal(T o, Map<Object, Object> clones) throws IllegalAccessException {
        if (o instanceof Log) {
            return (T) LogFactory.getLog(o.getClass());
        }
        if (o instanceof ClassLoader) {
            // There is no need to clone ClassLoader
            return null;
        }
        return super.cloneInternal(o, clones);
    }
}
