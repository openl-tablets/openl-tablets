package org.openl.rules;

import java.util.Map;

import org.openl.rules.table.properties.ITableProperties;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.ExecutableMethod;

public abstract class ExecutableRulesMethod extends ExecutableMethod {
    
    private ITableProperties properties;
    
    public ExecutableRulesMethod(IOpenMethodHeader header) {
        super(header);        
    }    

    public Map<String, Object> getProperties() {
        if (getMethodProperties() != null) {
            return getMethodProperties().getAllProperties();    
        } 
        return null;
        
    }    
    
    public ITableProperties getMethodProperties() {        
        return properties;
    }
    
    @Override
    public IMemberMetaInfo getInfo() {
        return this;
    }
    
    protected void initProperties(ITableProperties tableProperties) {        
        this.properties = tableProperties;
    }

}
