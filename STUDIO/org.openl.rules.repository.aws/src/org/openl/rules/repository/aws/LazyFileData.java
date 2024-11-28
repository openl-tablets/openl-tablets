package org.openl.rules.repository.aws;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.UserInfo;
import org.openl.util.StringUtils;

class LazyFileData extends FileData {
    static final String METADATA_AUTHOR = "author";
    static final String METADATA_COMMENT = "comment";

    private S3Client s3;
    private final String bucketName;

    public LazyFileData(S3Client s3, String bucketName) {
        this.s3 = s3;
        this.bucketName = bucketName;
    }

    @Override
    public UserInfo getAuthor() {
        verifyLoaded();
        return super.getAuthor();
    }

    @Override
    public void setAuthor(UserInfo author) {
        s3 = null;
        super.setAuthor(author);
    }

    @Override
    public String getComment() {
        verifyLoaded();
        return super.getComment();
    }

    @Override
    public void setComment(String comment) {
        s3 = null;
        super.setComment(comment);
    }

    private void verifyLoaded() {
        S3Client api = s3;
        if (api != null) {
            var request = HeadObjectRequest.builder().bucket(bucketName).key(getName()).versionId(getVersion()).build();

            var response = api.headObject(request);
            Map<String, String> userMetadata = response.metadata();
            super.setAuthor(new UserInfo(decode(userMetadata.get(METADATA_AUTHOR))));
            super.setComment(decode(userMetadata.get(METADATA_COMMENT)));

            s3 = null;
        }
    }

    static String encode(String url) {
        if (StringUtils.isBlank(url)) {
            return url;
        }
        return URLEncoder.encode(url, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
    }

    static String decode(String url) {
        if (StringUtils.isBlank(url)) {
            return url;
        }
        return URLDecoder.decode(url, StandardCharsets.UTF_8);
    }
}
