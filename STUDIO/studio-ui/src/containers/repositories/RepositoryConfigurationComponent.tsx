import React, { FC } from 'react'
import { designRepositoryTypeOptions, RepositoryDataType, RepositoryType } from './constants'
import { RepositoryAWSS3Configuration } from './RepositoryAWSS3Configuration'
import { RepositoryDBConfiguration } from './RepositoryDBConfiguration'
import { RepositoryAzureBlobConfiguration } from './RepositoryAzureBlobConfiguration'
import { RepositoryGitConfiguration } from './RepositoryGitConfiguration'
import { Input, Select } from '../../components'
import { useTranslation } from 'react-i18next'

interface RepositoryConfigurationComponentProps {
    onChangeType?: (type: string) => void
    configuration?: any
    repositoryType?: typeof RepositoryType
    repositoryDataType?: RepositoryDataType
}

export const RepositoryConfigurationComponent: FC<RepositoryConfigurationComponentProps> = ({ onChangeType, configuration, repositoryType, repositoryDataType = RepositoryDataType.DESIGN }) => {
    const { t } = useTranslation()

    const getConfigurationForm = () => {
        switch (repositoryType || configuration?.type) {
            case RepositoryType.AWS_S3:
                return <RepositoryAWSS3Configuration configuration={configuration} />
            case RepositoryType.JDBC:
            case RepositoryType.JNDI:
                return <RepositoryDBConfiguration />
            case RepositoryType.AZURE_BLOB:
                return <RepositoryAzureBlobConfiguration />
            case RepositoryType.GIT:
                return <RepositoryGitConfiguration repositoryDataType={repositoryDataType} />
            default:
                return null
        }
    }

    return (
        <>
            <Input label={t('repository:name')} name="name" />
            <Select label={t('repository:type')} name="type" onChange={onChangeType} options={designRepositoryTypeOptions} />
            {getConfigurationForm()}
        </>
    )
}
