package org.openl.rules.webstudio.web.admin;

import java.util.Optional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionMetadata;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AWSS3Region {

    private final String id;
    private final String description;

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
