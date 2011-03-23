package org.openl.rules.security.standalone.persistence;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * AccessControlEntry.
 *
 * @author Andrey Naumenko
 */
@Entity
public class AccessControlEntry extends PersistentObject {
    private static final long serialVersionUID = 1L;
    private User user;
    private Group group;
    private String object;
    private String permission;

    @ManyToOne(targetEntity = Group.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "GroupID", nullable = true)
    public Group getGroup() {
        return group;
    }

    @Override
    @Id
    @GeneratedValue(generator = "nativeId")
    @GenericGenerator(name = "nativeId", strategy = "native")
    @Column(name = "ACEID")
    @Type(type = "java.lang.Long")
    public Long getId() {
        return super.getId();
    }

    @Column(length = 1000, nullable = false)
    public String getObject() {
        return object;
    }

    @Column(length = 1000, nullable = false)
    public String getPermission() {
        return permission;
    }

    @ManyToOne(targetEntity = User.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = true)
    public User getUser() {
        return user;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
