import React from 'react'
import { useTranslation } from 'react-i18next'
import { Divider, Typography } from 'antd'
import { Input, TextArea } from '../../components'

export const SAMLMode = () => {
    const { t } = useTranslation()

    return (
        <>
            <Divider titlePlacement="start">{t('security:configure_saml')}</Divider>
            <Typography.Paragraph>{t('security:configure_saml_info')}</Typography.Paragraph>
            <Input required label={t('security:entity_id')} name="entityId" />
            <Input required label={t('security:saml_server_metadata_url')} name="metadataUrl" />
            <TextArea label={t('security:saml_remote_server_certificate')} name="serverCertificate" rows={4} />
            <Input label={t('security:attribute_for_username')} name={['attributes', 'username']} />
            <Input label={t('security:attribute_for_first_name')} name={['attributes', 'firstName']} />
            <Input label={t('security:attribute_for_last_name')} name={['attributes', 'lastName']} />
            <Input label={t('security:attribute_for_display_name')} name={['attributes', 'displayName']} />
            <Input label={t('security:attribute_for_email')} name={['attributes', 'email']} />
            <Input label={t('security:attribute_for_groups')} name={['attributes', 'groups']} />
        </>
    )
}
