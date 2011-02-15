package com.exigen.le.evaluator;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.le.project.ProjectManager;

/**
 * Class stores objects associated with some UUID.
 * 
 * @author PUdalau
 */
public class DataPool {
	private static final Log LOG = LogFactory.getLog(DataPool.class);

    private Map<String, Object> data = new HashMap<String, Object>();
    public static final String SERVICE_OBJECT_UUID_FORMAT = "%s-%s%s";
    public static final String SERVICE_OBJECT_UUID_SUFFIX = "(LE_UUID)";

    /**
     * @param newElement New object to add.
     * @return Generated UUID for this object.
     */
    public String add(Object newElement) {
        String id = generateUUID(newElement);
        data.put(id, newElement);
        return id;
    }

    private String generateUUID(Object object) {
        return String.format(SERVICE_OBJECT_UUID_FORMAT, object.getClass().getName(), UUID.randomUUID().toString(),SERVICE_OBJECT_UUID_SUFFIX);
    }

    /**
     * @param id UUID of previously added object;
     * @return Object associated with specified UUID;
     */
    public Object get(String id) {
    	Object result = data.get(id);
    	if(result == null){
    		String msg = "DataPool for thread "+Thread.currentThread()+" does not contain object:"+id; 
    		LOG.error(msg);
    		throw new RuntimeException(msg);
    	}
        return data.get(id);
    }
    /**
     * @param id String that maybe our UUID
     * @return
     */
    public static boolean isOurUUID(String id){
    	return id.endsWith(SERVICE_OBJECT_UUID_SUFFIX);
    }

    public boolean isPoolObject(String id){
    	return data.containsKey(id);
    }
   /**
     * Cleans up pool.
     */
    public void removeAll() {
        data.clear();
    }
}
