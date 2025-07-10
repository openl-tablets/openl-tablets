import React from 'react'
import { Divider, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { Input } from '../../components'
import InfoFieldModal from '../../components/modal/InfoFieldModal'

const UserFilterModal = (
    <InfoFieldModal
        text={(
            <p>
                The LDAP filter string to search for the user being authenticated.
                <br />
                Occurrences of {'{0}'} are replaced with the <b>login@domain</b>.
                <br />
                Occurrences of {'{1}'} are replaced with the <b>login</b> only.
            </p>
        )}
    />
)

const GroupFilterModal = (
    <InfoFieldModal
        text={(
            <p>
                The LDAP filter string to search for the groups belong to the user.
                <br />
                Occurrences of {'{0}'} are replaced with the <b>login@domain</b>.
                <br />
                Occurrences of {'{1}'} are replaced with the <b>login</b> only.
                <br />
                Occurrences of {'{2}'} are replaced with the DN of the found user.
            </p>
        )}
    />
)

export const ActiveDirectoryMode = () => {
    const { t } = useTranslation()

    return (
        <>
            <Divider orientation="left">{t('security:configure_active_directory')}</Divider>
            <Typography.Paragraph>{t('security:configure_active_directory_info')}</Typography.Paragraph>
            <Input required label={t('security:active_directory_domain')} name="domain" />
            <Input required label={t('security:active_directory_url')} name="serverUrl" />
            <Input required label={t('security:user_filter')} name="searchFilter" tooltip={{ icon: UserFilterModal }} />
            <Input label={t('security:group_filter')} name="groupFilter" tooltip={{ icon: GroupFilterModal }} />
            {/*<Typography.Paragraph>{t('security:login_and_password_info')}</Typography.Paragraph>*/}
            {/*<Input label={t('common:login')} name="" />*/}
            {/*<InputPassword label={t('common:password')} name="" />*/}
        </>
    )
}
