package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class UserGroupId implements Serializable {

    @Column(name = "loginName", nullable = false, length = 50)
    private String loginName;

    @Column(name = "groupID", nullable = false)
    private Long groupId;
}
