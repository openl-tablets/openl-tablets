import React, { useEffect, useMemo, useState } from 'react'
import { Button, Divider, Form, Row, Typography } from 'antd'
import { stringify } from 'querystring'
import { apiCall } from '../../services'
import { GroupTableItem } from '../../types/group'
import { Role } from '../../constants'
import { Checkbox, Input, Select } from '../../components'
import { useTranslation } from 'react-i18next'

interface EditGroupProps {
    group: GroupTableItem | undefined
    updateGroup: (updatedGroup: any) => void
    onAddGroup: () => void
    closeModal: () => void
}

interface Project {
    id: string
    name: string
    role: Role
}

interface FormValues {
    name: string
    description: string
    admin: boolean
}

export const EditGroupModal: React.FC<EditGroupProps> = ({ group, updateGroup, onAddGroup, closeModal }) => {
    const { t } = useTranslation()
    const [isNewGroup, setIsNewGroup] = useState(!group?.id)
    const [searchString, setSearchString] = useState('')
    const [externalGroups, setExternalGroups] = useState<string[]>([])

    const [form] = Form.useForm()

    const initialValues = useMemo(() => ({
        name: group?.name,
        description: group?.description,
        admin: group?.admin,
    }), [group])

    const fetchExternalGroups = async () => {
        const response: string[] = await apiCall(`/admin/management/groups/external?search=${searchString}`)
        setExternalGroups(response)
    }

    useEffect(() => {
        if (searchString) {
            fetchExternalGroups()
        }
    }, [searchString])

    const saveGroup = async (values: FormValues) => {
        const updatedGroup = {
            ...group,
            name: values.name,
            description: values.description,
            admin: !!values.admin
        }

        if (!isNewGroup) {
            updatedGroup.oldName = group?.oldName || updatedGroup.name
        }

        const encodedBody = stringify(updatedGroup)

        try {
            const headers = new Headers()
            headers.append('Content-Type', 'application/x-www-form-urlencoded')
            headers.append('Accept', 'application/json')

            const response = await apiCall('/admin/management/groups', {
                method: 'POST',
                headers,
                body: encodedBody,
            })
            if (response) {
                updatedGroup.oldName = updatedGroup.name
                updateGroup(updatedGroup)
                if (isNewGroup) {
                    setIsNewGroup(false)
                    onAddGroup()
                } else {
                    closeModal()
                }
            } else {
                throw new Error('Error updating group')
            }
        } catch (error) {
            console.error('Error updating group:', error)
        }
    }

    const handleSearch = (newValue: string) => {
        setSearchString(newValue)
    }

    const handleChange = (newValue: string) => {
        if (newValue) {
            setSearchString('')
            form.setFieldsValue({ name: newValue })
        }
    }

    const onBlurNameField = () => {
        if (searchString) {
            form.setFieldsValue({ name: searchString })
        }
    }

    const groupOptions = useMemo(() => {
        return externalGroups.map(group => ({
            value: group,
            label: group,
        }))
    }, [externalGroups])

    const onFinish = async (values: FormValues) => {
        await saveGroup(values)
    }

    const title = useMemo(() => {
        return isNewGroup ? t('groups:invite_group') : t('groups:edit_group_details')
    }, [isNewGroup, t])

    return (
        <Form
            form={form}
            initialValues={initialValues}
            labelCol={{ sm: { span: 6 } }}
            onFinish={onFinish}
        >
            <Typography.Title level={4} style={{ marginTop: 0 }}>{title}</Typography.Title>
            <Divider />
            <Select
                showSearch
                defaultActiveFirstOption={false}
                filterOption={false}
                label={t('groups:name')}
                name="name"
                notFoundContent={null}
                onBlur={onBlurNameField}
                onChange={handleChange}
                onSearch={handleSearch}
                options={groupOptions}
                style={{ width: '100%' }}
                suffixIcon={null}
                rules={[
                    { required: true, message: t('groups:name_required') },
                    { max: 65, message: t('groups:group_name_max_length') }
                ]}
            />
            <Input
                label={t('groups:description')}
                name="description"
                rules={[{ max: 200, message: t('groups:description_max_length') }]}
            />
            <Checkbox label={t('groups:admin')} name="admin" valuePropName="checked" />
            <Row justify="end">
                <Button key="back" onClick={closeModal} style={{ marginRight: 20 }}>
                    {t('groups:cancel')}
                </Button>
                <Button
                    htmlType="submit"
                    type="primary"
                >
                    {isNewGroup ? t('common:btn.invite') : t('common:btn.save')}
                </Button>
            </Row>
        </Form>
    )
}
