import React, { useMemo } from 'react'
import { Button, Form, Select, Space } from 'antd'
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons'
import { roleOptions } from './utils'
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


    return (
        <Form.List name="designRepos">
            {(fields, { add, remove }) => (
                <>
                    {fields.map(({ key, name, ...restField }) => (
                        <Space key={key} align="baseline" style={{ display: 'flex', marginBottom: 8 }}>
                            <Form.Item
                                {...restField}
                                name={[name, 'id']}
                                rules={[{ required: true, message: t('common:select_repository') }]}
                            >
                                <Select
                                    showSearch
                                    options={repositoryOptions}
                                    placeholder={t('common:design_repository')}
                                    style={{ width: 250 }}
                                    filterOption={(input, option) => {
                                        if (!option || !option.label || !(typeof option.label === 'string')) {
                                            return false
                                        }
                                        return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                                    }}
                                    filterSort={(optionA, optionB) => {
                                        return optionA.disabled === optionB.disabled ? 0 : optionA.disabled ? 1 : -1
                                    }}
                                />
                            </Form.Item>
                            <Form.Item
                                {...restField}
                                name={[name, 'role']}
                                rules={[{ required: true, message: t('common:select_role') }]}
                            >
                                <Select options={roleOptions} placeholder={t('common:role_t')} style={{ width: 250 }} />
                            </Form.Item>
                            <MinusCircleOutlined onClick={() => remove(name)} />
                        </Space>
                    ))}
                    <Form.Item>
                        <Button block icon={<PlusOutlined />} onClick={() => add()} type="dashed">
                            {t('common:add_role')}
                        </Button>
                    </Form.Item>
                </>
            )}
        </Form.List>
    )
}
