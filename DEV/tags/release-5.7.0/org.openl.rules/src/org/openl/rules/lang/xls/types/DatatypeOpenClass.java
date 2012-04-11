/*
 * Created on Jul 25, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.types;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenSchema;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.DynamicArrayAggregateInfo;
import org.openl.vm.IRuntimeEnv;

/**
 * Open class for types represented as datatype table components in openl.
 * 
 * @author snshor
 *
 */
public class DatatypeOpenClass extends ADynamicClass {
    
    private static final Log LOG = LogFactory.getLog(DatatypeOpenClass.class);
    
    public DatatypeOpenClass(IOpenSchema schema, String name) {
        super(schema, name, null);
    }

    @Override
    public IAggregateInfo getAggregateInfo() {
        return DynamicArrayAggregateInfo.aggregateInfo;
    }

    public Object newInstance(IRuntimeEnv env) {
        Object instance = null;
        try {
            instance = getInstanceClass().newInstance();
        } catch (InstantiationException e) {            
            LOG.error(this, e);
        } catch (IllegalAccessException e) {            
            LOG.error(this, e);
        }
        return instance;
    }

}
