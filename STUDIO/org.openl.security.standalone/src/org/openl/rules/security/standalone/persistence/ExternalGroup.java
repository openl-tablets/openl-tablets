package org.openl.rules.security.standalone.persistence;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "OpenL_External_Groups")
@IdClass(ExternalGroup.PK.class)
public class ExternalGroup implements Serializable {

    @Serial
    private static final long serialVersionUID = 5117085519399896506L;

    @Id
    @Setter
    @Column(name = "groupName", length = 65)
    @Getter
    private String groupName;

    @Id
    @Setter
    @Column(name = "loginName", length = 50)
    @Getter
    private String loginName;

    public static class PK implements Serializable {
        @Getter
        @Setter
        private String groupName;
        @Getter
        @Setter
        private String loginName;

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(groupName, pk.groupName) && Objects.equals(loginName, pk.loginName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(groupName, loginName);
        }
    }
}
