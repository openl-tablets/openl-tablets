package org.openl.rules.repository.jcr;

import org.openl.rules.repository.RUser;

/**
 * Defines JCR User
 */
public class JcrUser implements RUser {
    private String name;

    public JcrUser(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
}
