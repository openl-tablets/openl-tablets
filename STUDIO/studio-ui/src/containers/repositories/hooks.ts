import { useEffect, useState } from 'react'
import { RepositoryDataType } from './constants'
import { apiCall } from '../../services'
import { Modal, notification } from 'antd'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router-dom'
import { RepositoryResponse } from './index'

export const useRepositoryConfiguration = (repositoryDataType: RepositoryDataType) => {
    const { t } = useTranslation()
    const [configuration, setConfiguration] = useState<RepositoryResponse | RepositoryResponse[] | null>(null)
    const [searchParams, setSearchParams] = useSearchParams()

    const setURLSearchParam = (repositoryID: string) => {
        searchParams.set('r', repositoryID)
        setSearchParams(searchParams)
    }

    const fetchConfiguration = async () => {
        const response = await apiCall(`/admin/settings/repos/${repositoryDataType}`)
        setConfiguration(response)
    }

    const fetchRepositoryConfigurationTemplate = async (repositoryType: string) => {
        return await apiCall(`/admin/settings/repos/${repositoryDataType}/template`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                type: repositoryType
            })
        })
    }

    const updateRepositoryConfiguration = async (values: any) => {
        await apiCall(`/admin/settings/repos/${repositoryDataType}`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/merge-patch+json',
            },
            body: JSON.stringify(values)
        }, true).then(() => {
            window.location.reload()
        }).catch(error => {
            notification.error({ message: error.toString() })
        })
    }

    const deleteRepositoryConfiguration = async (id: string) => {
        await apiCall(`/admin/settings/repos/${repositoryDataType}/${id}`, {
            method: 'DELETE'
        }, true).then(() => {
            window.location.reload()
        }).catch(error => {
            notification.error({ message: error.toString() })
        })
    }

    const handleDeleteRepository = async (id: string) => {
        Modal.confirm({
            title: t('repository:confirm_delete_repository'),
            content: t('repository:confirm_delete_repository_message'),
            onOk: () => {
                deleteRepositoryConfiguration(id)
            },
        })
    }

    const handleApplyConfiguration = async (values: any) => {
        Modal.confirm({
            title: t('repository:confirm_apply_configuration'),
            content: t('repository:confirm_apply_configuration_message'),
            onOk: () => {
                updateRepositoryConfiguration(values)
            },
        })
    }

    useEffect(() => {
        fetchConfiguration()
    }, [repositoryDataType])

    return {
        configuration,
        fetchRepositoryConfigurationTemplate,
        updateRepositoryConfiguration,
        deleteRepositoryConfiguration,
        handleApplyConfiguration,
        handleDeleteRepository,
        setURLSearchParam
    }
}
