package org.openl.rules.repository.aws;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;

import org.openl.rules.repository.api.FileData;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;

class LazyFileData extends FileData {
    static final String METADATA_AUTHOR = "author";
    static final String METADATA_COMMENT = "comment";

    private AmazonS3 s3;
    private String bucketName;

    public LazyFileData(AmazonS3 s3, String bucketName) {
        this.s3 = s3;
        this.bucketName = bucketName;
    }

    @Override
    public String getAuthor() {
        verifyLoaded();
        return super.getAuthor();
    }

    @Override
    public void setAuthor(String author) {
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
        AmazonS3 api = s3;
        if (api != null) {
            GetObjectMetadataRequest request = new GetObjectMetadataRequest(bucketName, getName(), getVersion());
            ObjectMetadata metadata = api.getObjectMetadata(request);

            Map<String, String> userMetadata = metadata.getUserMetadata();
            super.setAuthor(decode(userMetadata.get(METADATA_AUTHOR)));
            super.setComment(decode(userMetadata.get(METADATA_COMMENT)));

            s3 = null;
        }
    }

    static String encode(String url) {
        String encodedUrl = null;
        if (StringUtils.isBlank(url)) {
            return url;
        }
        try {
            encodedUrl = URLEncoder.encode(url, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            Logger log = LoggerFactory.getLogger(LazyFileData.class);
            log.error(e.getMessage(), e);
        }
        return encodedUrl;
    }

    static String decode(String url) {
        String decodedUrl = null;
        if (StringUtils.isBlank(url)) {
            return url;
        }
        try {
            decodedUrl = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Logger log = LoggerFactory.getLogger(LazyFileData.class);
            log.error(e.getMessage(), e);
        }
        return decodedUrl;
    }
}
