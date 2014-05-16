package org.openl.rules.security.standalone.authentication;

import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author nsamatov.
 */
public class OpenLPasswordEncoder implements PasswordEncoder {
    // For backward-compatibility
    private final Md5PasswordEncoder oldPasswordEncoder = new Md5PasswordEncoder();
    private final PasswordEncoder passwordEncoder;

    public OpenLPasswordEncoder(int strength) {
        this.passwordEncoder = new BCryptPasswordEncoder(strength);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (!rehashIsNeeded(encodedPassword)) {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        }
        return oldPasswordEncoder.isPasswordValid(encodedPassword, rawPassword.toString(), null);
    }

    public boolean rehashIsNeeded(String encodedPassword) {
        return !encodedPassword.startsWith("$");
    }
}
