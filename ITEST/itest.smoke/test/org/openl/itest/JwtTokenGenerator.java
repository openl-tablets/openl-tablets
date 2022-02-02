package org.openl.itest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

public class JwtTokenGenerator {

    public static void main(String[] args) throws Exception {
        jwsWithJwksFile();
        jwsWithJwkEC();
    }

    private static void jwsWithJwksFile() throws Exception {
        RSAKey rsaJWK = new RSAKeyGenerator(2048).keyID("testId").generate();
        JWSSigner signer = new RSASSASigner(rsaJWK);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 10);
        Date exp = cal.getTime();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().issuer("itest")
            .audience("https://openl-tablets.org")
            .expirationTime(exp)
            .build();
        SignedJWT signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID()).build(),
            claimsSet);
        signedJWT.sign(signer);
        Map<String, JWK> keys = new HashMap<>();
        keys.put(rsaJWK.getKeyID(), rsaJWK);
        keys.put("testId2", new RSAKeyGenerator(2048).keyID("testId2").generate());
        JWKSet jwkSet = new JWKSet(new ArrayList<>(keys.values()));
        System.out.println("jwsWithJwksFile");
        System.out.println(signedJWT.serialize());
        System.out.println("JwksFile");
        System.out.println(jwkSet.toJSONObject());
    }

    private static void jwsWithJwkEC() throws Exception {
        ECKey jwk = new ECKeyGenerator(Curve.P_256).keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
            .keyID(UUID.randomUUID().toString()) // give the key a unique ID
            .generate();
        JWSSigner signer = new ECDSASigner(jwk);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 10);
        Date exp = cal.getTime();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().issuer("itest")
            .audience("https://openl-tablets.org")
            .expirationTime(exp)
            .build();
        SignedJWT signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).jwk(jwk).build(), claimsSet);
        signedJWT.sign(signer);
        System.out.println("jwsWithJwkEC");
        System.out.println(signedJWT.serialize());
    }
}
