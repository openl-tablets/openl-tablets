import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'

i18n
    .use(initReactI18next) // passes i18n down to react-i18next
    .init({
        fallbackLng: 'en',
        debug: process?.env?.NODE_ENV === 'development',
        lng: 'en',
    })

export default i18n
