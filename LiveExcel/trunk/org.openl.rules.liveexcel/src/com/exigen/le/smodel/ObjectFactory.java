
package com.exigen.le.smodel;

import javax.xml.bind.annotation.XmlRegistry;

import com.exigen.le.smodel.Function.FunctionArgument;
import com.exigen.le.smodel.TableDesc.ColumnDesc;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.eclipse.example.library package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.eclipse.example.library
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ServiceModel }
     * 
     */
    public ServiceModel createServiceModel() {
        return new ServiceModel();
    }

    /**
     * Create an instance of {@link Function }
     * 
     */
    public Function createFunction() {
        return new Function();
    }

    /**
     * Create an instance of {@link FunctionArgument }
     * 
     */
    public FunctionArgument createFunctionArgument() {
        return new FunctionArgument();
    }
    /**
     * Create an instance of {@link Type }
     * 
     */
    public Type createType() {
        return new Type();
    }

    /**
     * Create an instance of {@link Property }
     * 
     */
    public Property createProperty() {
        return new Property();
    }
    /**
     * Create an instance of {@link TableDesc }
     * 
     */
    public TableDesc createTableDesc() {
        return new TableDesc();
    }
    /**
     * Create an instance of {@link ColumnDesc }
     * 
     */
    public ColumnDesc createColumnDesc() {
        return new ColumnDesc();
    }
}
