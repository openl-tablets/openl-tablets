import React, { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Form, Modal, Select } from 'antd'

interface AddLanguageModalProps {
    open: boolean
    /** The tags of the languages the messages have already, which may not be added again. */
    tags: readonly string[]
    onAdd: (tag: string) => void
    onClose: () => void
}

interface AddLanguageForm {
    language: string
    country?: string
}

const LETTERS = 'abcdefghijklmnopqrstuvwxyz'
const TWO_LETTER_CODES = [...LETTERS].flatMap(first => [...LETTERS].map(second => first + second))
/** Codes Java has replaced by newer ones (`iw` by `he`), or that name no country of their own. */
const LEFT_OUT = new Set(['iw', 'in', 'ji', 'jw', 'mo', 'sh', 'EU', 'EZ', 'UN', 'QO', 'ZZ', 'XA', 'XB'])

/**
 * The codes of a kind the standard has a name for, named in the language of the screen, by name.
 *
 * <p>A file name of the messages takes a two-letter code of a language and of a country, so those are listed.
 */
const namedCodes = (type: 'language' | 'region', uiLocale: string) => {
    const names = new Intl.DisplayNames([uiLocale], { type, fallback: 'none' })
    return TWO_LETTER_CODES
        .map(code => (type === 'region' ? code.toUpperCase() : code))
        .filter(code => !LEFT_OUT.has(code))
        .flatMap(code => {
            const name = names.of(code)
            return name === undefined ? [] : [{ value: code, label: `${name} (${code})`, name }]
        })
        .sort((a, b) => a.name.localeCompare(b.name, uiLocale))
}

/**
 * Asks for a language to add to the messages, and the country it is written for when it is a regional one.
 *
 * <p>The languages and the countries are listed by name and found by name or by code. A language the messages have
 * already cannot be added again.
 */
export const AddLanguageModal = ({ open, tags, onAdd, onClose }: AddLanguageModalProps) => {
    const { t, i18n } = useTranslation('localization')
    const uiLocale = i18n.resolvedLanguage ?? i18n.language
    const [form] = Form.useForm<AddLanguageForm>()
    const languages = useMemo(() => namedCodes('language', uiLocale), [uiLocale])
    const countries = useMemo(() => namedCodes('region', uiLocale), [uiLocale])
    const tagOf = ({ language, country }: AddLanguageForm) => (country ? `${language}_${country}` : language)

    return (
        <Modal
            destroyOnHidden
            cancelText={t('common:btn.cancel')}
            okButtonProps={{ 'data-testid': 'localization-add-language-ok' }}
            okText={t('add_language')}
            onCancel={onClose}
            onOk={() => form.submit()}
            open={open}
            title={t('add_language')}
        >
            <Form<AddLanguageForm> form={form} layout="vertical" onFinish={values => onAdd(tagOf(values))}>
                <Form.Item
                    label={t('language')}
                    name="language"
                    rules={[{ required: true, message: t('language_required') }]}
                >
                    <Select
                        data-testid="localization-add-language-language"
                        options={languages}
                        showSearch={{ optionFilterProp: 'label' }}
                    />
                </Form.Item>
                <Form.Item
                    dependencies={['language']}
                    label={t('country')}
                    name="country"
                    rules={[{
                        validator: () => {
                            const values = form.getFieldsValue()
                            return values.language && tags.includes(tagOf(values))
                                ? Promise.reject(new Error(t('language_exists')))
                                : Promise.resolve()
                        },
                    }]}
                >
                    <Select
                        allowClear
                        data-testid="localization-add-language-country"
                        options={countries}
                        placeholder={t('country_any')}
                        showSearch={{ optionFilterProp: 'label' }}
                    />
                </Form.Item>
            </Form>
        </Modal>
    )
}
