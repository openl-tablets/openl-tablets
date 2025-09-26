import { useEffect, useState } from 'react'
import { RepositoryDataType } from './constants'
import { apiCall } from '../../services'
import { notification } from 'antd'
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
        return await apiCall(`/admin/settings/repos/${repositoryDataType}`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/merge-patch+json',
            },
            body: JSON.stringify(values)
        }, true).then(() => {
            notification.success({
                message: t('repository:notifications.configuration_applied'),
                description: t('repository:notifications.configuration_applied_description'),
            })
            window.location.reload()
        }).catch(error => {
            notification.error({ message: error.toString() })
            throw error
        })
    }

    const deleteRepositoryConfiguration = async (id: string) => {
        return await apiCall(`/admin/settings/repos/${repositoryDataType}/${id}`, {
            method: 'DELETE'
        }, true).then(() => {
            notification.success({
                message: t('repository:notifications.repository_deleted'),
                description: t('repository:notifications.repository_deleted_description'),
            })
            window.location.reload()
        }).catch(error => {
            notification.error({ message: error.toString() })
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
        setURLSearchParam
    }
}
