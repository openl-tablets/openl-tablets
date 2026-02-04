import React from 'react'
import { useTranslation } from 'react-i18next'
import { Divider, Typography } from 'antd'
import { Input, InputPassword } from '../../components'

export const OAuth2Mode = () => {
    const { t } = useTranslation()

    return (
        <>
            <Divider titlePlacement="start">{t('security:configure_oauth2')}</Divider>
            <Typography.Paragraph>{t('security:configure_oauth2_info')}</Typography.Paragraph>
            <Input
                required
                label={t('security:client_id')}
                name="clientId"
            />
            <Input
                required
                label={t('security:issuer_uri')}
                name="issuerUri"
            />
            <InputPassword
                required
                label={t('security:client_secret')}
                name="clientSecret"
            />
            <Input
                required
                label={t('security:scope')}
                name="scope"
            />
            <Input label={t('security:attribute_for_username')} name={['attributes', 'username']} />
            <Input label={t('security:attribute_for_first_name')} name={['attributes', 'firstName']} />
            <Input label={t('security:attribute_for_last_name')} name={['attributes', 'lastName']} />
            <Input label={t('security:attribute_for_display_name')} name={['attributes', 'displayName']} />
            <Input label={t('security:attribute_for_email')} name={['attributes', 'email']} />
            <Input label={t('security:attribute_for_groups')} name={['attributes', 'groups']} />
        </>
    )
}
