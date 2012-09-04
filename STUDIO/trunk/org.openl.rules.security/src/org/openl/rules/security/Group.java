package org.openl.rules.security;


/**
 * An authority containing several other authorities
 * 
 * @author NSamatov
 */
public interface Group extends Privilege {

    String getDescription();

    Privilege[] getPrivileges();

    //List<Group> getGroups();

    /**
     * Returns true if this group contains given privilege
     * 
     * @param privilege checking privilege
     * @return true if this group contains given privilege
     */
    boolean hasPrivilege(String privilege);

}
