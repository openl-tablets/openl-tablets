package org.openl.rules.ruleservice.oauth;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.openl.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Reader for jwk from url.
 *
 * @author ybiruk
 */
@Component
public class JWKInURLReader implements JWKTokenReader {

    private final Map<String, JWK> keyCache = new HashMap<>();

    private Environment env;

    @Autowired
    public JWKInURLReader(Environment env){
        this.env = env;
    }

    /**
     * Get jwk from url.
     * Url can be passed in token header or set in properties.
     *
     * @return JWK
     */
    @Override
    public JWK getJWK(SignedJWT jwt) {
        try {
            return getKey(jwt);
        } catch (IOException | ParseException e) {
            return null;
        }
    }

    private JWK getKey(SignedJWT jwt) throws IOException, ParseException {
        JWSHeader header = jwt.getHeader();
        JWK jwk = keyCache.get(header.getKeyID());
        if (jwk == null) {
            // update cache loading jwk public key data from url
            JWKSet jwkSet = loadJWKSet(header);
            if (jwkSet != null) {
                for (JWK key : jwkSet.getKeys()) {
                    keyCache.put(key.getKeyID(), key);
                }
                jwk = keyCache.get(header.getKeyID());
            }
        }
        return jwk;
    }

    private JWKSet loadJWKSet(JWSHeader header) throws IOException, ParseException {
        URI jwkHeaderUrl = header.getJWKURL();
        String jwkPropertyUrl = env.getProperty("ruleservice.authentication.jwks");
        if (jwkHeaderUrl == null && StringUtils.isEmpty(jwkPropertyUrl)) {
            return null;
        }
        URL url = jwkHeaderUrl != null ? jwkHeaderUrl.toURL() : new URL(jwkPropertyUrl);
        return JWKSet.load(url.openStream());
    }
}
