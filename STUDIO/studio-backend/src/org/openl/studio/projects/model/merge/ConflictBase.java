package org.openl.studio.projects.model.merge;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Version of a conflicted file to retrieve.
 *
 * <p>The code of each version is written in lower case. The upper-case names that earlier versions of OpenL Studio
 * took are accepted as well.
 */
@Schema(description = "Version of a conflicted file")
public enum ConflictBase {

    /**
     * Base version: common ancestor before branches diverged.
     */
    @JsonProperty("base")
    @JsonAlias("BASE")
    BASE,

    /**
     * Ours version: from the current branch.
     */
    @JsonProperty("ours")
    @JsonAlias("OURS")
    OURS,

    /**
     * Theirs version: from the merging branch.
     */
    @JsonProperty("theirs")
    @JsonAlias("THEIRS")
    THEIRS

}
