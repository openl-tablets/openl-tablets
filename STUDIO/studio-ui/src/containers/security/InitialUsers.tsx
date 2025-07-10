import React, { FC } from 'react'
import { Divider, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import {Checkbox, Select} from '../../components'

interface InitialUsersProps {
    userGroups: { label: string; value: string }[]
}

export const InitialUsers: FC<InitialUsersProps> = ({ userGroups }) => {
    const { t } = useTranslation()
    return (
        <div>
            <Divider orientation="left">{t('security:configure_initial_users')}</Divider>
            <Typography.Paragraph>
                {t('security:configure_initial_users_info')}
            </Typography.Paragraph>
            <Select
                required
                label={t('security:administrators')}
                mode="tags"
                name="administrators"
                open={false}
                options={[]}
                suffixIcon={null}
                tokenSeparators={[',']}
            />
            <Select label={t('security:default_group')} name="defaultGroup" options={userGroups} />
        </div>
    )
}
