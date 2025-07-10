import React from 'react'
import { Checkbox, Input, InputPassword } from '../../components'
import { useTranslation } from 'react-i18next'
import { Form } from 'antd'

export const RepositoryDBConfiguration = () => {
    const { t } = useTranslation()
    const form = Form.useFormInstance()
    const isSecureConnection = Form.useWatch(['settings', 'secure'], form)

    return (
        <>
            <Input label={t('repository:url')} name={['settings', 'uri']} rules={[{ required: true, message: t('common:validation.required') }]} />
            <Checkbox label={t('repository:secure_connection')} name={['settings', 'secure']} />
            {isSecureConnection && (
                <>
                    <Input label={t('repository:login')} name={['settings', 'login']} />
                    <InputPassword label={t('repository:password')} name={['settings', 'password']} />
                </>
            )}
        </>
    )
}
