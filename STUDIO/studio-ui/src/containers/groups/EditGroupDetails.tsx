import React, { useEffect, useMemo, useState } from 'react'
import { Divider, Form } from 'antd'
import { apiCall } from '../../services'
import { Checkbox, Input, Select } from '../../components'
import { useTranslation } from 'react-i18next'

interface FormValues {
    name: string
    description: string
    admin: boolean
}

export const EditGroupDetails: React.FC = () => {
    const { t } = useTranslation()
    const [searchString, setSearchString] = useState('')
    const [externalGroups, setExternalGroups] = useState<string[]>([])
    const form = Form.useFormInstance<FormValues>()

    const fetchExternalGroups = async () => {
        const response: string[] = await apiCall(`/admin/management/groups/external?search=${searchString}`)
        setExternalGroups(response)
    }

    useEffect(() => {
        if (searchString) {
            fetchExternalGroups()
        }
    }, [searchString])

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

    return (
        <>
            <Divider titlePlacement="start">{t('common:details')}</Divider>
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
        </>
    )
}
