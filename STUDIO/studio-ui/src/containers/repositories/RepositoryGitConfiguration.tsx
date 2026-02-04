import React, { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Input, InputNumber, InputPassword } from '../../components'
import { Divider } from 'antd'
import {
    BranchModal,
    DefaultBranchModal,
    MewBranchRegexErrorModal,
    NewBranchRegexModal,
    ProtectedBranchesModal,
    URLModal,
} from './InfoFieldModals'
import { RepositoryDataType } from './constants'

interface RepositoryGitConfigurationProps {
    repositoryDataType: RepositoryDataType
}

export const RepositoryGitConfiguration: FC<RepositoryGitConfigurationProps> = ({ repositoryDataType }) => {
    const { t } = useTranslation()

    return (
        <>
            <Input
                label={t('repository:url')}
                name={['settings', 'uri']}
                rules={[{ required: true, message: t('common:validation.required') }]}
                tooltip={{ icon: URLModal }}
            />
            <Input label={t('repository:login')} name={['settings', 'login']} />
            <InputPassword label={t('repository:password')} name={['settings', 'password']} />
            <Input label={t('repository:branch')} name={['settings', 'branch']} tooltip={{ icon: BranchModal }} />
            <Input label={t('repository:protected_branches')} name={['settings', 'protectedBranches']} tooltip={{ icon: ProtectedBranchesModal }} />
            <InputNumber label={t('repository:changes_check_interval')} name={['settings', 'listenerTimerPeriod']} />
            <InputNumber label={t('repository:connection_timeout')} name={['settings', 'connectionTimeout']} />
            {repositoryDataType === RepositoryDataType.DESIGN && (
                <>
                    <Divider titlePlacement="start">{t('repository:new_branch')}</Divider>
                    <Input label={t('repository:default_branch_name')} name={['settings', 'newBranchTemplate']} tooltip={{ icon: DefaultBranchModal }} />
                    <Input label={t('repository:branch_name_pattern')} name={['settings', 'newBranchRegex']} tooltip={{ icon: NewBranchRegexModal }} />
                    <Input label={t('repository:invalid_branch_name_message_hint')} name={['settings', 'newBranchRegexError']} tooltip={{ icon: MewBranchRegexErrorModal }} />
                </>
            )}

        </>
    )
}
