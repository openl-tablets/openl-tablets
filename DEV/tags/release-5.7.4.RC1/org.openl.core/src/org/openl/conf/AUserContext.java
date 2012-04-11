/*
 * Created on Jul 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.util.Properties;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * The implementation of the hashCode() and equals() for the derives classes.
 *
 * @author sam
 */
public abstract class AUserContext implements IUserContext {
    static Properties props(IUserContext cxt) {
        Properties properties = cxt.getUserProperties();
        return properties != null ? properties : PropertyFileLoader.NO_PROPERTIES;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof IUserContext)) {
            return false;
        }
        IUserContext c = (IUserContext) obj;

        return new EqualsBuilder()
        // .append(name, k.name)
//                .append(getUserClassLoader(), c.getUserClassLoader())
                .append(getUserHome(), c.getUserHome()).append(
                        getUserProperties(), c.getUserProperties()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
//        .append(getUserClassLoader())
        .append(getUserHome())
                .append(getUserProperties()).toHashCode();
    }

}
