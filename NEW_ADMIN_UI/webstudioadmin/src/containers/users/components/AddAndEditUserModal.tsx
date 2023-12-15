import React, { useState, useEffect, useMemo } from 'react'
import { Button, Row, Form as AForm, Typography, Col } from 'antd'
import { Form, FormSpy } from 'react-final-form'
import arrayMutators from 'final-form-arrays'
import { CheckboxGroup } from 'components/form'
import { Input, Select } from 'components'
import { FormApi } from 'final-form'
import { apiCall } from 'services'
import { useTranslation } from 'react-i18next'

const DISPLAY_NAME_FIRST_LAST = 'firstLast'
const DISPLAY_NAME_LAST_FIRST = 'lastFirst'
const DISPLAY_NAME_OTHER = 'other'

interface EditUserProps {
    user: {
        email: string;
        displayName: string;
        firstName: string;
        lastName: string;
        password: string;
        groups: string[];
        username: string;
        internalPassword: {
            password: string;
        };
        currentUser: boolean;
        superUser: boolean;
        unsafePassword: boolean;
        externalFlags: {
            firstNameExternal: boolean;
            displayNameExternal: boolean;
            emailExternal: boolean;
            lastNameExternal: boolean;
            emailVerified: boolean;
        };
        notMatchedExternalGroupsCount: number;
        online: boolean;
        userGroups: {
            name: string;
            type: 'ADMIN' | 'DEFAULT' | 'EXTERNAL';
        }[];
    };
    onCancel: () => void;
    onSubmit: any,
    isNewUser: boolean,
}

export const AddAndEditUserModal: React.FC<EditUserProps> = ({ isNewUser, onSubmit, user, onCancel }) => {
    const { t } = useTranslation()
    const [ groupNames, setGroupNames ] = useState<string[]>([])
    const [ isDisplayNameFieldDisabled, setIsDisplayNameFieldDisabled ] = useState<boolean>(true)

    const displayNameOptions = useMemo(() => (
        [
            {
                value: DISPLAY_NAME_FIRST_LAST,
                label: t('users:first_last'),
            },
            {
                value: DISPLAY_NAME_LAST_FIRST,
                label: t('users:last_first'),
            },
            {
                value: DISPLAY_NAME_OTHER,
                label: t('users:other'),
            },
        ]
    ), [ t ])

    const initialValues = useMemo(() => {
        const displayNameSelectInitialValue = () => {
            if (user.displayName === `${user.firstName} ${user.lastName}`) {
                return DISPLAY_NAME_FIRST_LAST
            }
            if (user.displayName === `${user.lastName} ${user.firstName}`) {
                return DISPLAY_NAME_LAST_FIRST
            }
            return DISPLAY_NAME_OTHER
        }

        return {
            username: user.username,
            email: user.email,
            password: user.password,
            firstName: user.firstName,
            lastName: user.lastName,
            displayName: user.displayName,
            displayNameSelect: displayNameSelectInitialValue(),
            groups: user.userGroups?.map((group) => group.name) || [],
        }
    }, [ user ])

    const fetchGroupData = async () => {
        try {
            const response = await apiCall('/admin/management/groups')
            if (response.ok) {
                const responseObject = await response.json()
                const names = Object.keys(responseObject)
                setGroupNames(names)
            } else {
                console.error('Failed to fetch groups:', response.statusText)
            }
        } catch (error) {
            console.error('Error fetching groups:', error)
        }
    }

    useEffect(() => {
        fetchGroupData()
    }, [])

    const handleSubmit = async (values: any) => {
        onSubmit(values)
    }

    // TODO: values is object with all form values. create a type for it
    const displayNameSetter = (values: any, form: FormApi) => {
        if (values.displayNameSelect === DISPLAY_NAME_OTHER) {
            setIsDisplayNameFieldDisabled(false)
        } else {
            if (values.displayNameSelect === DISPLAY_NAME_FIRST_LAST) {
                form.change('displayName', `${values.firstName} ${values.lastName}`)
            } else {
                form.change('displayName', `${values.lastName} ${values.firstName}`)
            }
            setIsDisplayNameFieldDisabled(true)
        }

        return null
    }

    const updateButtonText = useMemo(() => {
        return isNewUser ? t('users:edit_modal.create') : t('users:edit_modal.update')
    }, [ isNewUser, t ])

    return (
        <div>
            <Form
                initialValues={initialValues}
                mutators={{ ...arrayMutators }}
                onSubmit={handleSubmit}
            >
                {({ handleSubmit, form }) => {
                    return (
                        <AForm layout="vertical" onFinish={handleSubmit}>
                            <br />
                            {/*<Typography.Title level={5}>Account</Typography.Title>*/}
                            <Input disabled={!isNewUser} label={t('users:edit_modal.username')} name="username" />
                            <Input label={t('users:edit_modal.email')} name="email" />
                            <Input label={t('users:edit_modal.password')} name="password" type="password" />
                            {/*<Typography.Title level={5}>Name</Typography.Title>*/}
                            <Input label={t('users:edit_modal.first_name')} name="firstName" />
                            <Input label={t('users:edit_modal.last_name')} name="lastName" />
                            <Row gutter={16} justify="space-between">
                                <Col span={8}>
                                    <Select
                                        formItemStyle={{ margin: 0 }}
                                        label={t('users:edit_modal.display_name')}
                                        name="displayNameSelect"
                                        options={displayNameOptions}
                                    />
                                </Col>
                                <Col span={16}>
                                    <Input
                                        disabled={isDisplayNameFieldDisabled}
                                        formItemStyle={{ margin: 0 }}
                                        label=" "
                                        name="displayName"
                                    />
                                </Col>
                            </Row>
                            <Typography.Title level={5}>{t('users:edit_modal.groups')}</Typography.Title>
                            <CheckboxGroup name="groups" options={groupNames} />
                            <Row justify="end">
                                <Button key="back" onClick={onCancel} style={{ marginTop: 20, marginRight: 20 }}>
                                    {t('users:edit_modal.cancel')}
                                </Button>
                                <Button
                                    key="submit"
                                    htmlType="submit"
                                    style={{ marginTop: 20 }}
                                    type="primary"
                                >
                                    {updateButtonText}
                                </Button>
                            </Row>
                            <FormSpy subscription={{ values: true }}>
                                {({ values }) => displayNameSetter(values, form)}
                            </FormSpy>
                        </AForm>
                    )
                }}
            </Form>
        </div>
    )
}
