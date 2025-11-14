import React, { useEffect, useMemo } from 'react'
import { apiCall } from '../../services'
import { Button, Divider, Form, Select as AntdSelect } from 'antd'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import { NONE_ROLE_VALUE, roleOptions } from './utils'
import { SelectOption } from '../form/Select'
import { Repository } from '../../types/repositories'
import { useTranslation } from 'react-i18next'

export const DeployRepositoriesTab: React.FC<{selectedRepositories: string[]}> = ({ selectedRepositories }) => {
    const { t } = useTranslation()
    const [deployRepositories, setDeployRepositories] = React.useState<SelectOption[]>([])

    const fetchDeployRepositories = async () => {
        const response: Repository[] = await apiCall('/production-repos')
        setDeployRepositories(response.map(repo => ({ label: repo.name, value: repo.aclId })))
    }
    useEffect(() => {
        fetchDeployRepositories()
    }, [])

    const repositoryOptions = useMemo(() => {
        return deployRepositories.map(repository => ({
            label: repository.label,
            value: repository.value,
            disabled: selectedRepositories.includes(repository.value as string)
        }))
    }, [deployRepositories, selectedRepositories])

    const defaultRoleOptions = useMemo(() => ([
        { label: t('common:none'), value: NONE_ROLE_VALUE },
        ...roleOptions
    ]), [t])

    return (
        <>
            <Form.Item
                label={t('users:default_deploy_repository_role')}
                name="deployRootRole"
                style={{ width: '100%' }}
                tooltip={t('users:default_deploy_repository_role_tooltip')}
            >
                <AntdSelect
                    options={defaultRoleOptions}
                    placeholder={t('common:select_role')}
                    style={{ width: '100%' }}
                />
            </Form.Item>
            <Divider style={{ marginTop: 16, marginBottom: 16 }} />
            <Form.List name="deployRepos">
                {(fields, { add, remove }) => (
                    <>
                        {fields.map(({ key, name, ...restField }) => (
                            <div key={key} style={{ display: 'flex', width: '100%', marginBottom: 8, gap: 8, alignItems: 'center' }}>
                                <Form.Item
                                    {...restField}
                                    name={[name, 'id']}
                                    rules={[{ required: true, message: t('common:select_repository') }]}
                                    style={{ flex: 1, minWidth: 0, marginBottom: 0 }}
                                >
                                    <AntdSelect
                                        showSearch
                                        options={repositoryOptions}
                                        placeholder={t('common:deploy_repository')}
                                        style={{ width: '100%' }}
                                        filterOption={(input: string, option: any) => {
                                            if (!option || !option.label || !(typeof option.label === 'string')) {
                                                return false
                                            }
                                            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                                        }}
                                        filterSort={(optionA: any, optionB: any) => {
                                            return optionA.disabled === optionB.disabled ? 0 : optionA.disabled ? 1 : -1
                                        }}
                                    />
                                </Form.Item>
                                <Form.Item
                                    {...restField}
                                    name={[name, 'role']}
                                    rules={[{ required: true, message: t('common:select_role') }]}
                                    style={{ flex: 1, minWidth: 0, marginBottom: 0 }}
                                >
                                    <AntdSelect
                                        options={roleOptions}
                                        placeholder={t('common:role_t')}
                                        style={{ width: '100%' }}
                                    />
                                </Form.Item>
                                <DeleteOutlined onClick={() => remove(name)} style={{ fontSize: 16, cursor: 'pointer', flexShrink: 0, alignSelf: 'flex-start', marginTop: 8 }} />
                            </div>
                        ))}
                        <Form.Item style={{ marginTop: 24 }}>
                            <Button block icon={<PlusOutlined />} onClick={() => add()} type="dashed">
                                {t('common:add_role')}
                            </Button>
                        </Form.Item>
                    </>
                )}
            </Form.List>
        </>
    )
}
