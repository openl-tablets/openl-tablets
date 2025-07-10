import React, { useEffect, useMemo } from 'react'
import { Button, Divider, Form, Modal, notification, Row, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { WIDTH_OF_FORM_LABEL } from '../constants'
import {Checkbox, RadioGroup} from '../components/form'
import { SecurityUserMode } from '../constants/security'
import { InitialUsers } from './security/InitialUsers'
import { apiCall } from '../services'
import { ActiveDirectoryMode } from './security/ActiveDirectoryMode'
import { SAMLMode } from './security/SAMLMode'
import { OAuth2Mode } from './security/OAuth2Mode'
import InfoFieldModal from '../components/modal/InfoFieldModal'
import { SingleMode } from './security/SingleMode'

const radioGroupStyle: React.CSSProperties = {
    display: 'flex',
    flexDirection: 'column',
    gap: 8,
}

const UserModeModal = (
    <InfoFieldModal
        text={(
            <>
                <p>
                    <b>Single-User</b> - Only the currently logged in user can run OpenL Studio.
                </p>
                <p>
                    <b>Multi-User</b> - Multiple users can run OpenL Studio using their unique user names. User credentials are managed in OpenL Studio.
                </p>
                <p>
                    <b>Active Directory</b>, <b>SSO: SAML</b>, and <b>SSO: OAuth2</b> modes allow multiple users to run OpenL Studio using their unique user names. User credentials are managed by the respective identity provider server.
                </p>
            </>
        )}
    />
)

export const Security = () => {
    const { t } = useTranslation()
    const [form] = Form.useForm()
    const [securitySettings, setSecuritySettings] = React.useState<any>(undefined)
    const [userGroups, setUserGroups] = React.useState<{label: string, value: string}[]>([])
    const userMode = Form.useWatch('userMode', form)

    const userModeOptions = [
        { label: t('security:user_modes.single'), value: SecurityUserMode.SINGLE },
        { label: t('security:user_modes.multi'), value: SecurityUserMode.MULTI },
        { label: t('security:user_modes.ad'), value: SecurityUserMode.AD },
        { label: t('security:user_modes.saml'), value: SecurityUserMode.SAML },
        { label: t('security:user_modes.oauth2'), value: SecurityUserMode.OAUTH2 },
    ]

    const fetchSecuritySettings = async () => {
        const response = await apiCall('/admin/settings/authentication')
        setSecuritySettings(response)
        form.setFieldsValue(response)
    }

    const saveSecuritySettings = async (values: any) => {
        const requestMethod = values.userMode === securitySettings?.userMode ? 'PATCH' : 'POST'

        await apiCall('/admin/settings/authentication', {
            method: requestMethod,
            headers: {
                'Content-Type': 'application/merge-patch+json',
            },
            body: JSON.stringify(values),
        }, true)
            .then(() => {
                window.location.reload()
            })
            .catch(error => {
                notification.error({ message: error.toString() })
            })
    }

    const fetchSecuritySettingsTemplate = async () => {
        const response = await apiCall('/admin/settings/authentication/template', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ userMode }),
        })
        form.setFieldsValue(response)
    }

    const fetchUserGroups = async () => {
        const response = await apiCall('/admin/management/groups')
        if (response && Object.keys(response).length) {
            const userGroups = Object.keys(response).map(group => ({
                label: group,
                value: group,
            }))
            // Add None option for empty group
            userGroups.unshift({ label: t('common:none'), value: '' })
            setUserGroups(userGroups)
        }
    }

    const onFinish = (values: any) => {
        Modal.confirm({
            title: t('security:confirm_apply_configuration'),
            content: t('security:confirm_apply_configuration_message'),
            onOk: () => {
                saveSecuritySettings(values)
            },
        })
    }

    useEffect(() => {
        fetchSecuritySettings()
    }, [])

    useEffect(() => {
        if (typeof userMode !== 'object' && (userMode !== securitySettings?.userMode && userMode !== securitySettings?.userMode?.value)) {
            fetchSecuritySettingsTemplate()
        } else if (!securitySettings?.userMode?.readOnly) {
            form.resetFields()
        }
    }, [userMode])

    useEffect(() => {
        if (userMode && userMode !== SecurityUserMode.SINGLE && userGroups.length === 0) {
            fetchUserGroups()
        }
    }, [userMode])

    const Component = useMemo(() => {
        switch (userMode) {
            case SecurityUserMode.SINGLE:
                return <SingleMode />
            case SecurityUserMode.AD:
                return <ActiveDirectoryMode />
            case SecurityUserMode.SAML:
                return <SAMLMode />
            case SecurityUserMode.OAUTH2:
                return <OAuth2Mode />
            default:
                return null
        }
    }, [userMode])

    return (
        <Form
            labelWrap
            form={form}
            initialValues={securitySettings}
            labelAlign="right"
            labelCol={{ flex: WIDTH_OF_FORM_LABEL }}
            onFinish={onFinish}
            wrapperCol={{ flex: 1 }}
        >
            <Divider orientation="left">{t('security:select_user_mode')}</Divider>
            <Typography.Paragraph>{t('security:select_user_mode_info')}</Typography.Paragraph>
            <RadioGroup
                label={t('security:user_mode')}
                name="userMode"
                options={userModeOptions}
                style={radioGroupStyle}
                tooltip={{ icon: UserModeModal }}
            />
            {Component}
            {userMode && userMode !== SecurityUserMode.SINGLE && (
                <InitialUsers userGroups={userGroups} />
            )}
            <Checkbox label={t('security:allowProjectCreateDelete')} name="allowProjectCreateDelete" />
            <Row justify="end">
                <Button htmlType="submit" type="primary">
                    {t('common:btn.apply')}
                </Button>
            </Row>
        </Form>
    )
}
