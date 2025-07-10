import React from 'react'
import { useTranslation } from 'react-i18next'
import { Input } from '../../components'

export const SingleMode: React.FC = () => {
    const { t } = useTranslation()

    return (
        <>
            <Input
                required
                label={t('security:username')}
                name={['user', 'username']}
                rules={[{
                    max: 125,
                    message: t('security:username_max_length'),
                }]}
            />
            <Input
                label={t('security:email')}
                name={['user', 'email']}
                rules={[{
                    type: 'email',
                    message: t('common:validation.invalid_email'),
                }]}
            />
            <Input
                required
                label={t('security:first_name')}
                name={['user', 'firstName']}
                rules={[{
                    max: 125,
                    message: t('security:first_name_max_length'),
                }]}
            />
            <Input
                required
                label={t('security:last_name')}
                name={['user', 'lastName']}
                rules={[{
                    max: 125,
                    message: t('security:last_name_max_length'),
                }]}
            />
            <Input
                required
                label={t('security:display_name')}
                name={['user', 'displayName']}
                rules={[{
                    max: 255,
                    message: t('security:display_name_max_length')
                }]}
            />
        </>
    )
}
