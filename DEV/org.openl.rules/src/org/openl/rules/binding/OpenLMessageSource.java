package org.openl.rules.binding;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpenL Project MessageSource service. Loads all i18n message properties for required {@link Locale}
 */
class OpenLMessageSource {

    private static final String DEFAULT_MSG_SRC = "i18n/message";

    private final ClassLoader classLoader;

    // Cache to hold already loaded properties per locale.
    private final Map<Locale, MessageBundle> cacheLocalProperties = new ConcurrentHashMap<>();
    // Cache to hold already loaded properties per name.
    private final Map<String, Properties> cacheProperties = new ConcurrentHashMap<>();

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
        MessageBundle result = cacheLocalProperties.get(locale);
        if (result != null) {
            return result;
        }
        // seems like no cache for target locale yet
        return loadMessageBundle(locale);
    }

    private MessageBundle loadMessageBundle(Locale locale) {
        Properties properties = new Properties();
        List<String> fileNames = calculateAllFilenames(DEFAULT_MSG_SRC, locale);
        for (int i = fileNames.size() - 1; i > -1; i--) {
            String basename = fileNames.get(i);
            properties.putAll(getProperties(basename));
        }
        MessageBundle mergedProperties = new MessageBundle(properties, locale);
        cacheLocalProperties.put(locale, mergedProperties);
        return mergedProperties;
    }

    private Properties getProperties(String basename) {
        Properties result = cacheProperties.get(basename);
        if (result != null) {
            return result;
        }
        result = new Properties();

        Enumeration<URL> urls = null;
        try {
            urls = classLoader.getResources(basename + ".properties");
        } catch (IOException ignored) {
        }
        if (urls != null) {
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (var is = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
                    result.load(is);
                } catch (IOException ignored) {
                }
            }
        }

        cacheProperties.put(basename, result);
        return result;
    }

    private List<String> calculateAllFilenames(String basename, Locale locale) {
        List<String> filenames = calculateFilenamesForLocale(basename, locale);
        filenames.add(basename);
        return filenames;
    }

    /**
     * Calculate the filenames for the given bundle basename and Locale, appending language code, country code, and
     * variant code.
     * <p>
     * For example, basename "messages", Locale "de_AT_oo" &rarr; "messages_de_AT_OO", "messages_de_AT", "messages_de".
     * <p>
     * Follows the rules defined by {@link java.util.Locale#toString()}.
     * 
     * @param basename the basename of the bundle
     * @param locale the locale
     * @return the List of filenames to check
     */
    private List<String> calculateFilenamesForLocale(String basename, Locale locale) {
        List<String> result = new ArrayList<>(3);
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();
        StringBuilder temp = new StringBuilder(basename);

        temp.append('_');
        if (language.length() > 0) {
            temp.append(language);
            result.add(0, temp.toString());
        }

        temp.append('_');
        if (country.length() > 0) {
            temp.append(country);
            result.add(0, temp.toString());
        }

        if (variant.length() > 0 && (language.length() > 0 || country.length() > 0)) {
            temp.append('_').append(variant);
            result.add(0, temp.toString());
        }

        return result;
    }

    public static class MessageBundle {

        private final Properties properties;
        private final Locale locale;

        // Cache to hold already generated MessageFormats per message code.
        private final Map<String, MessageFormat> cachedMessageFormats = new ConcurrentHashMap<>();

        public MessageBundle(Properties properties, Locale locale) {
            this.properties = properties;
            this.locale = locale;
        }

        public String getProperty(String code) {
            return code != null ? properties.getProperty(code) : null;
        }

        public MessageFormat getMessageFormat(String code) {
            if (code == null) {
                return null;
            }
            MessageFormat result = cachedMessageFormats.get(code);
            if (result != null) {
                return result;
            }
            String msg = properties.getProperty(code);
            if (msg == null) {
                return null;
            }
            result = new MessageFormat(msg, locale);
            cachedMessageFormats.put(code, result);
            return result;
        }
    }

}
