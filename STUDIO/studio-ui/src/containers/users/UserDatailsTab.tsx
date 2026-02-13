import React, { FC, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { Divider, Form, Space, Button, notification, Row, Col, Input as AntInput } from 'antd'
import { Input, Select, InputPassword } from '../../components'
import { useTranslation } from 'react-i18next'
import { DisplayUserName } from '../../constants'
import { UserExternalFlags, UserProfile, UserDetails } from '../../types/user'
import { SystemContext } from '../../contexts'
import { apiCall } from '../../services'
import './UserDetailsTab.scss'

interface UserDetailsTabProps {
    isNewUser?: boolean
    displayPasswordField?: boolean
    externalFlags?: UserExternalFlags
    showResendVerification?: boolean
    userProfile?: UserProfile | UserDetails
    resendLoading?: boolean
    cooldown?: number
    onResendVerification?: () => void
}

export const UserDetailsTab: FC<UserDetailsTabProps> = ({ isNewUser, externalFlags, displayPasswordField = true, showResendVerification = false, userProfile, resendLoading, cooldown, onResendVerification }) => {
    const { t } = useTranslation()
    const form = Form.useFormInstance()
    const { isExternalAuthSystem, systemSettings } = useContext(SystemContext)
    const firstName = Form.useWatch('firstName', form)
    const lastName = Form.useWatch('lastName', form)
    const [isDisplayNameFieldDisabled, setIsDisplayNameFieldDisabled] = useState<boolean>(true)
    const previousDisplayNameSelect = useRef('')
    // Use parent-provided resend logic if available, otherwise manage locally
    const [localResendLoading, setLocalResendLoading] = useState(false)
    const [localCooldown, setLocalCooldown] = useState(0)
    const resendLoadingToUse = typeof resendLoading === 'boolean' ? resendLoading : localResendLoading
    const cooldownToUse = typeof cooldown === 'number' ? cooldown : localCooldown

    // Track if form is dirty (any field changed)
    const watchedFields = Form.useWatch([], form)
    const [isFormDirty, setIsFormDirty] = useState(false)
    useEffect(() => {
        setIsFormDirty(form.isFieldsTouched())
    }, [watchedFields, form])

    const handleResend = async () => {
        if (onResendVerification) {
            onResendVerification()
            return
        }
        if (!userProfile?.username) {
            notification.error({ message: t('users:cannot_determine_current_user') })
            return
        }
        setLocalResendLoading(true)
        try {
            await apiCall(`/mail/send/${userProfile.username}`, { method: 'POST' }, true)
            notification.success({ message: t('users:verification_email_sent') })
            setLocalCooldown(60)
        } catch (_) {
            notification.error({ message: t('users:failed_to_send_verification_email') })
        } finally {
            setLocalResendLoading(false)
        }
    }
    React.useEffect(() => {
        let timer: NodeJS.Timeout | undefined
        if (!cooldown && localCooldown > 0) {
            timer = setInterval(() => {
                setLocalCooldown((prev) => (prev > 0 ? prev - 1 : 0))
            }, 1000)
        }
        return () => {
            if (timer) clearInterval(timer)
        }
    }, [localCooldown, cooldown])

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

    const isResendVerificationButtonVisible: Boolean = useMemo(() => {
        return !!(showResendVerification && userProfile && userProfile.externalFlags && !userProfile.externalFlags.emailVerified && systemSettings?.supportedFeatures?.emailVerification)
    }, [showResendVerification, userProfile, systemSettings])

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

    return (
        <>
            <Divider titlePlacement="start">{t('users:account')}</Divider>
            <Input
                disabled={!isNewUser}
                label={t('users:edit_modal.username')}
                name="username"
                rules={[{
                    required: isNewUser,
                    message: t('users:edit_modal.username_required')
                }]}
            />
            <Form.Item
                label={t('users:edit_modal.email')}
                name="email"
                style={displayPasswordField ? { marginBottom: 24 } : {}}
                rules={[{
                    type: 'email',
                    message: t('users:edit_modal.email_invalid')
                }, {
                    max: 254,
                    message: t('users:edit_modal.email_max_length')
                }]}
            >
                <Row align="top" className="profile-email-row" gutter={8} style={{ width: '100%', height: 32 }}>
                    <Col className={`${isResendVerificationButtonVisible ? 'profile-email-input' : ''}`} flex="auto">
                        <AntInput
                            disabled={externalFlags?.emailExternal}
                            name="email"
                            type="email"
                        />
                    </Col>
                    {isResendVerificationButtonVisible && (
                        <Col>
                            <Button
                                disabled={cooldownToUse > 0 || isFormDirty}
                                loading={resendLoadingToUse}
                                onClick={handleResend}
                                type="primary"
                            >
                                {t('users:resend_verification_email')}
                            </Button>
                            {cooldownToUse > 0 && (
                                <div style={{ color: '#888', fontSize: 12, textAlign: 'right' }}>
                                    {t('users:resend_verification_email_timer', { seconds: cooldownToUse })}
                                </div>
                            )}
                        </Col>
                    )}
                </Row>
            </Form.Item>
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
            <Divider titlePlacement="start">{t('users:name')}</Divider>
            <Input
                disabled={externalFlags?.firstNameExternal}
                label={t('users:edit_modal.first_name')}
                name="firstName"
                tooltip={t('users:edit_modal.first_name_info')}
                rules={[{
                    max: 25,
                    message: t('users:edit_modal.first_name_max_length')
                }]}
            />
            <Input
                disabled={externalFlags?.lastNameExternal}
                label={t('users:edit_modal.last_name')}
                name="lastName"
                tooltip={t('users:edit_modal.last_name_info')}
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
                        disabled={isDisplayNameFieldDisabled || !!externalFlags?.displayNameExternal}
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
