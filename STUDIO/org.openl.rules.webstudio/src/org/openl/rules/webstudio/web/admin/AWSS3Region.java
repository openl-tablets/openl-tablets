package org.openl.rules.webstudio.web.admin;

import java.util.Optional;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionMetadata;

public class AWSS3Region {

    private final String id;
    private final String description;

    private AWSS3Region(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public static AWSS3Region from(Region region) {
        return new AWSS3Region(region.id(), Optional.ofNullable(region.metadata())
                .map(RegionMetadata::description)
                .orElseGet(region::id));
    }
}
