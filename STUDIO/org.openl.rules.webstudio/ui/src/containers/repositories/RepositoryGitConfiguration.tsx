import React, { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Checkbox, Input, InputNumber, InputPassword } from '../../components'
import { Divider, Form } from 'antd'
import {
    BranchModal,
    DefaultBranchModal, FlatFolderStructureModal,
    LocalPathModal,
    MewBranchRegexErrorModal, NewBranchRegexModal,
    ProtectedBranchesModal,
    RemoteRepositoryModal,
    URLModal,
} from './InfoFieldModals'
import { RepositoryDataType } from './constants'

interface RepositoryGitConfigurationProps {
    repositoryDataType: RepositoryDataType
}

export const RepositoryGitConfiguration: FC<RepositoryGitConfigurationProps> = ({ repositoryDataType }) => {
    const { t } = useTranslation()
    const form = Form.useFormInstance()
    const isRemoteRepository = Form.useWatch(['settings', 'remoteRepository'], form)
    const isFlatFolderStructure = Form.useWatch(['settings', 'flatFolderStructure'], form)

    return (
        <>
            <Checkbox label={t('repository:remote_repository')} name={['settings', 'remoteRepository']} tooltip={{ icon: RemoteRepositoryModal }}/>
            {isRemoteRepository && (
                <>
                    <Input
                        label={t('repository:url')}
                        name={['settings', 'url']}
                        rules={[{ required: true, message: t('common:validation.required') }]}
                        tooltip={{ icon: URLModal }}
                    />
                    <Input label={t('repository:login')} name={['settings', 'login']} />
                    <InputPassword label={t('repository:password')} name={['settings', 'password']} />
                    
                </>
            )}
            <Input
                label={t('repository:local_path')}
                name={['settings', 'localRepositoryPath']}
                rules={[{ required: true, message: t('common:validation.required') }]}
                tooltip={{ icon: LocalPathModal }}
            />
            {isRemoteRepository && (
                <Input label={t('repository:branch')} name={['settings', 'branch']} tooltip={{ icon: BranchModal }} />
            )}
            <Input label={t('repository:protected_branches')} name={['settings', 'protectedBranches']} tooltip={{ icon: ProtectedBranchesModal }} />
            {isRemoteRepository && (
                <>
                    <InputNumber name={['settings', 'listenerTimerPeriod']} label={t('repository:changes_check_interval')} />
                    <InputNumber name={['settings', 'connectionTimeout']} label={t('repository:connection_timeout')} />
                </>
            )}
            {repositoryDataType === RepositoryDataType.DESIGN && (
                <>
                    <Divider orientation="left">{t('repository:new_branch')}</Divider>
                    <Input name={['settings', 'newBranchTemplate']} label={t('repository:default_branch_name')} tooltip={{ icon: DefaultBranchModal }} />
                    <Input name={['settings', 'newBranchRegex']} label={t('repository:branch_name_pattern')} tooltip={{ icon: NewBranchRegexModal }} />
                    <Input name={['settings', 'newBranchRegexError']} label={t('repository:invalid_branch_name_message_hint')} tooltip={{ icon: MewBranchRegexErrorModal }} />
                    <Divider orientation="left">{t('repository:folder_structure')}</Divider>
                    <Checkbox name={['settings', 'flatFolderStructure']} label={t('repository:flat_folder_structure')} tooltip={{ icon: FlatFolderStructureModal }} />
                    {isFlatFolderStructure && (
                        <Input label={t('repository:path')} name={['settings', 'basePath']} />
                    )}
                </>
            )}

        </>
    )
}