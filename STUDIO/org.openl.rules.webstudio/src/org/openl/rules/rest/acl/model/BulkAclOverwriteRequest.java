package org.openl.rules.rest.acl.model;

import java.beans.Transient;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.util.StringUtils;


@Schema(description = "Bulk ACL overwrite request for multiple resources")
public class BulkAclOverwriteRequest {

    private static final Predicate<AclSubject> SUB_USER_FILTER = sub -> Boolean.TRUE.equals(sub.getPrincipal());

    @Parameter(description = "List of ACL configurations per resource")
    @NotEmpty
    @Valid
    private final List<AclResourceAccess> resources;

    @JsonCreator
    public BulkAclOverwriteRequest(@JsonProperty("resources") List<AclResourceAccess> resources) {
        this.resources = resources;
    }

    public List<AclResourceAccess> getResources() {
        return resources;
    }

    @Transient
    public Set<String> getGroups() {
        return collectSids(SUB_USER_FILTER.negate());
    }

    @Transient
    public Set<String> getUsers() {
        return collectSids(SUB_USER_FILTER);
    }

    private Set<String> collectSids(Predicate<AclSubject> subFilter) {
        return resources.stream()
                .map(AclResourceAccess::getAces)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(AccessControlEntry::getSub)
                .filter(subFilter)
                .map(AclSubject::getSid)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

}
