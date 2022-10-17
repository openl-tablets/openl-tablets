package org.openl.rules.binding;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openl.util.CollectionUtils;
import org.openl.util.PropertiesUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenL Project MessageSource service. Loads all i18n message properties for required {@link Locale}
 */
public class OpenLMessageSource {

    Logger LOG = LoggerFactory.getLogger(OpenLMessageSource.class);

    private static final String DEFAULT_MSG_SRC = "i18n/message";

    private final ClassLoader classLoader;

    // Cache to hold already loaded properties per locale.
    private final Map<Locale, MessageBundle> cacheLocalProperties = new ConcurrentHashMap<>();
    // Cache to hold already loaded properties per name.
    private final Map<String, Map<String, String>> cacheProperties = new ConcurrentHashMap<>();

    public OpenLMessageSource(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Get message bundle for target {@link Locale}
     *
     * @param locale target locale
     * @return merged message bundle for target {@link Locale} or empty
     */
    public MessageBundle getMessageBundle(Locale locale) {
        // seems like no cache for target locale yet
        return cacheLocalProperties.computeIfAbsent(locale, this::loadMessageBundle);
    }

    private MessageBundle loadMessageBundle(Locale locale) {
        return loadMessageBundle(DEFAULT_MSG_SRC, locale);
    }

    private Map<String, String> getProperties(String basename) {
        return cacheProperties.computeIfAbsent(basename, this::loadProperties);
    }

    private Map<String, String> loadProperties(String basename) {
        var result = new HashMap<String, String>();

        String propFileName = basename + ".properties";
        Enumeration<URL> urls = null;
        try {
            urls = classLoader.getResources(propFileName);
        } catch (IOException ex) {
            LOG.error("Failed to collect resources for '{}'", propFileName, ex);
        }
        if (urls != null) {
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try {
                    PropertiesUtils.load(url, result::put);
                } catch (IOException ex) {
                    LOG.error("Failed to process resource '{}'", url, ex);
                }
            }
        }

        return result;
    }

    /**
     * Loads the bundle for the given properties basename and Locale, appending language code, country code, and
     * variant code.
     * <p>
     * For example, basename "messages", Locale "de_AT_oo" &rarr; "messages_de_AT_OO", "messages_de_AT", "messages_de".
     * <p>
     * Follows the rules defined by {@link java.util.Locale#toString()}.
     *
     * @param basename the basename of the bundle
     * @param locale   the locale
     * @return the message bundle
     */
    private MessageBundle loadMessageBundle(String basename, Locale locale) {
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();
        StringBuilder temp = new StringBuilder(basename);

        var properties = new HashMap<String, String>(getProperties(basename));

        temp.append('_');
        if (StringUtils.isNotEmpty(language)) {
            temp.append(language);
            properties.putAll(getProperties(temp.toString()));
        }

        temp.append('_');
        if (StringUtils.isNotEmpty(country)) {
            temp.append(country);
            properties.putAll(getProperties(temp.toString()));
        }

        if (StringUtils.isNotEmpty(variant) && (StringUtils.isNotEmpty(language) || StringUtils.isNotEmpty(country))) {
            temp.append('_').append(variant);
            properties.putAll(getProperties(temp.toString()));
        }

        return new MessageBundle(properties, locale);
    }

    public static class MessageBundle {

        private final Map<String, String> properties;
        private final Locale locale;

        // Cache to hold already generated MessageFormats per message code.
        private final Map<String, MessageFormat> cachedMessageFormats = new ConcurrentHashMap<>();

        public MessageBundle(Map<String, String> properties, Locale locale) {
            this.properties = properties;
            this.locale = locale;
        }

        public String msg(String code, Object... args) {
            if (code == null) {
                return null;
            }
            String msg = properties.get(code);
            if (msg == null) {
                return code;
            }
            if (CollectionUtils.isEmpty(args)) {
                return msg;
            }
            return cachedMessageFormats.computeIfAbsent(msg, (x) -> new MessageFormat(x, locale)).format(args);
        }
    }

}
