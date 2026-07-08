import dayjs from 'dayjs'
import localizedFormat from 'dayjs/plugin/localizedFormat'
import i18next from 'i18next'

dayjs.extend(localizedFormat)

// The date format follows the application language. A day.js locale for every new application
// language must be registered statically next to its i18n bundle, e.g. import 'dayjs/locale/de'.
const applyLanguage = (language?: string) => {
    if (language) {
        dayjs.locale(language.toLowerCase())
    }
}

// i18next may have been initialized before this module is loaded, so the current language
// is applied once on load and then kept in sync on every change.
applyLanguage(i18next.language)
i18next.on('languageChanged', applyLanguage)

/**
 * Formats a date without time, such as "Aug 16, 2018", using the date format of the current language.
 */
export const formatDate = (value: string): string => dayjs(value).format('ll')

/**
 * Formats a date with time, such as "Aug 16, 2018 8:02 PM", using the date format of the current language.
 */
export const formatDateTime = (value: string): string => dayjs(value).format('lll')
