import React, { useMemo } from 'react'
import { Button, Divider, Form, Select as AntdSelect } from 'antd'
import type { DefaultOptionType } from 'rc-select/lib/Select'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import { NONE_ROLE_VALUE, roleOptions } from './utils'
import { useTranslation } from 'react-i18next'
import { SelectOption } from '../form/Select'
import { Repository } from '../../types/repositories'

interface DesignRepositoriesTabProps {
    designRepositories: Repository[]
    selectedRepositories: string[]
}

export const DesignRepositoriesTab: React.FC<DesignRepositoriesTabProps> = ({ designRepositories, selectedRepositories }) => {
    const { t } = useTranslation()

    const repositoryOptions: SelectOption[] = useMemo(() => {
        return designRepositories.map(repository => ({
            label: repository.name,
            value: repository.aclId,
            disabled: selectedRepositories.includes(repository.aclId)
        }))
    }, [designRepositories, selectedRepositories])

    const defaultRoleOptions = useMemo(() => ([
        { label: t('common:none'), value: NONE_ROLE_VALUE },
        ...roleOptions
    ]), [t])

    return (
        <>
            <Form.Item
                label={t('users:default_design_repository_role')}
                name="designRootRole"
                style={{ width: '100%' }}
                tooltip={t('users:default_design_repository_role_tooltip')}
            >
                <AntdSelect
                    options={defaultRoleOptions}
                    placeholder={t('common:select_role')}
                    style={{ width: '100%' }}
                />
            </Form.Item>
            <Divider style={{ marginTop: 16, marginBottom: 16 }} />
            <Form.List name="designRepos">
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
                                        options={repositoryOptions as DefaultOptionType[]}
                                        placeholder={t('common:design_repository')}
                                        style={{ width: '100%' }}
                                        filterOption={(input: string, option?: DefaultOptionType) => {
                                            if (!option || !option.label || !(typeof option.label === 'string')) {
                                                return false
                                            }
                                            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                                        }}
                                        filterSort={(optionA?: DefaultOptionType, optionB?: DefaultOptionType) => {
                                            if (!optionA || !optionB) return 0
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
