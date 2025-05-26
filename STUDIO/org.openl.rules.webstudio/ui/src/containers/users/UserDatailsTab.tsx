import React, { FC, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { Divider, Form, Space } from 'antd'
import { Input, Select, InputPassword } from '../../components'
import { useTranslation } from 'react-i18next'
import { DisplayUserName } from '../../constants'
import { UserExternalFlags } from '../../types/user'
import { SystemContext } from '../../contexts'

interface UserDetailsTabProps {
    isNewUser: boolean
    displayPasswordField?: boolean
    externalFlags?: UserExternalFlags | null
}

export const UserDetailsTab: FC<UserDetailsTabProps> = ({ isNewUser, externalFlags, displayPasswordField = true }) => {
    const { t } = useTranslation()
    const form = Form.useFormInstance()
    const { isExternalAuthSystem } = useContext(SystemContext)
    const firstName = Form.useWatch('firstName', form)
    const lastName = Form.useWatch('lastName', form)
    const [isDisplayNameFieldDisabled, setIsDisplayNameFieldDisabled] = useState<boolean>(true)
    const previousDisplayNameSelect = useRef('')

    const displayNameOptions = useMemo(() => (
        [
            {
                value: DisplayUserName.FirstLast,
                label: t('users:first_last'),
            },
            {
                value: DisplayUserName.LastFirst,
                label: t('users:last_first'),
            },
            {
                value: DisplayUserName.Other,
                label: t('users:other'),
            },
        ]
    ), [t])

    useEffect(() => {
        if (form.getFieldValue('displayNameSelect') === DisplayUserName.FirstLast) {
            form.setFieldsValue({
                displayName: (firstName + ' ' + lastName).trim()
            })
        } else if (form.getFieldValue('displayNameSelect') === DisplayUserName.LastFirst) {
            form.setFieldsValue({
                displayName: (lastName + ' ' + firstName).trim()
            })
        }
        form.validateFields(['displayName'])
    }, [firstName, lastName])

    console.log(isDisplayNameFieldDisabled,  externalFlags?.displayNameExternal)

    return (
        <>
            <Divider orientation="left">{t('users:account')}</Divider>
            <Input
                disabled={!isNewUser}
                label={t('users:edit_modal.username')}
                name="username"
                rules={[{
                    required: isNewUser,
                    message: t('users:edit_modal.username_required')
                }]}
            />
            <Input
                disabled={externalFlags?.emailExternal}
                label={t('users:edit_modal.email')}
                name="email"
                rules={[{
                    type: 'email',
                    message: t('users:edit_modal.email_invalid')
                }, {
                    max: 254,
                    message: t('users:edit_modal.email_max_length')
                }]}
            />
            {displayPasswordField && !isExternalAuthSystem && (
                <InputPassword
                    disabled={externalFlags?.emailExternal}
                    label={t('users:edit_modal.password')}
                    name="password"
                    rules={[{
                        required: isNewUser,
                        message: t('users:edit_modal.password_required')
                    }]}
                />
            )}
            <Divider orientation="left">{t('users:name')}</Divider>
            <Input
                disabled={externalFlags?.firstNameExternal}
                label={t('users:edit_modal.first_name')}
                name="firstName"
                rules={[{
                    max: 25,
                    message: t('users:edit_modal.first_name_max_length')
                }]}
            />
            <Input
                disabled={externalFlags?.lastNameExternal}
                label={t('users:edit_modal.last_name')}
                name="lastName"
                rules={[{
                    max: 25,
                    message: t('users:edit_modal.last_name_max_length')
                }]}
            />
            <Form.Item label={t('users:edit_modal.display_name')}>
                <Space.Compact>
                    <Select
                        disabled={externalFlags?.displayNameExternal}
                        name="displayNameSelect"
                        options={displayNameOptions}
                        style={{ width: 120 }}
                    />
                    <Input
                        dependencies={['displayNameSelect']}
                        disabled={isDisplayNameFieldDisabled || externalFlags?.displayNameExternal}
                        name="displayName"
                        style={{ width: 248 }}
                        rules={[
                            {
                                max: 64,
                                message: t('users:edit_modal.display_name_max_length')
                            },
                            // @ts-ignore
                            ({ getFieldValue, setFieldValue }) => {
                                if (previousDisplayNameSelect.current !== getFieldValue('displayNameSelect')) {
                                    previousDisplayNameSelect.current = getFieldValue('displayNameSelect')
                                    if (getFieldValue('displayNameSelect') === DisplayUserName.Other) {
                                        setIsDisplayNameFieldDisabled(false)
                                    } else {
                                        setIsDisplayNameFieldDisabled(true)
                                    }
                                    if (getFieldValue('displayNameSelect') === DisplayUserName.FirstLast) {
                                        setFieldValue('displayName', (getFieldValue('firstName') + ' ' + getFieldValue('lastName')).trim())
                                    } else if (getFieldValue('displayNameSelect') === DisplayUserName.LastFirst) {
                                        setFieldValue('displayName', (getFieldValue('lastName') + ' ' + getFieldValue('firstName')).trim())
                                    }
                                }

                                return Promise.resolve()
                            }
                        ]}
                    />
                </Space.Compact>
            </Form.Item>
        </>
    )
}