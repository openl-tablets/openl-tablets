import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'

i18n
    .use(initReactI18next) // passes i18n down to react-i18next
    .init({
        fallbackLng: 'en',
        lng: 'en',
        // React escapes every rendered value already, so i18next must not escape interpolations on top of
        // that: it would turn a branch like "rates/jdoe/2026" into "rates&#x2F;jdoe&#x2F;2026" in messages.
        interpolation: { escapeValue: false },
    })

export default i18n
