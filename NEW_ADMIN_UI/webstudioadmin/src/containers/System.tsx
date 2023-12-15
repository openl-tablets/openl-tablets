import { Button, Checkbox, Divider, Input, Form, Typography, Row } from 'antd'
import React from 'react'
import { WarningFilled } from '@ant-design/icons'
import { Trans, useTranslation } from 'react-i18next'

export const System: React.FC = () => {
    const { t } = useTranslation()

    return (
        <>
            <Typography.Title level={4}>
                {t('system:core')}
            </Typography.Title>
            <Form
                labelAlign="left"
                labelCol={{ span: 5 }}
                wrapperCol={{ span: 18 }}
            >
                <Form.Item label={t('system:dispatching_validation')}>
                    <Checkbox />
                </Form.Item>
                <Form.Item label={t('system:verify_on_edit')}>
                    <Checkbox />
                </Form.Item>

            </Form>
            <Divider />
            <Typography.Title level={4}>
                {t('system:testing')}
            </Typography.Title>
            <Form>
                <Form.Item label={t('system:thread_number_for_tests')}>
                    <Input placeholder="4" />
                </Form.Item>
            </Form>
            <Divider />
            <Typography.Title level={4}>
                {t('system:webstudio_settings')}
            </Typography.Title>
            <p>
                <WarningFilled style={{ color: 'red' }} />
                <Trans components={[ <b style={{ color: 'red' }} /> ]} i18nKey="system:restore_defaults_warning" />
            </p>
            <Row justify="end">
                <Button danger style={{ marginTop: 15 }}>
                    {t('system:restore_defaults_and_restart')}
                </Button>
            </Row>
            <Divider />
            <Typography.Title level={4}>
                {t('system:user_workspace')}
            </Typography.Title>
            <Form
                labelAlign="left"
                labelCol={{ span: 7 }}
                wrapperCol={{ span: 18 }}
            >
                <Form.Item label={t('system:workspace_directory')}>
                    <Input defaultValue="./openl-demo/user-workspace" />
                </Form.Item>
            </Form>
            <Divider />
            <Typography.Title level={4}>
                {t('system:history')}
            </Typography.Title>
            <Form
                labelWrap
                labelAlign="left"
                labelCol={{ span: 7 }}
                wrapperCol={{ span: 18 }}
            >
                <Form.Item label={t('system:maximum_count_of_changes')}>
                    <Input defaultValue="100" style={{ marginTop: 7 }} />
                </Form.Item>
            </Form>
            <Row justify="end">
                <Button>{t('system:clear_all_history')}</Button>
            </Row>
            <Divider />
            <Typography.Title level={4}>
                {t('system:other')}
            </Typography.Title>
            <Form
                labelWrap
                labelAlign="left"
                labelCol={{ span: 7 }}
                wrapperCol={{ span: 18 }}
            >
                <Form.Item label={t('system:update_table_properties')}>
                    <Checkbox style={{ marginTop: 12 }} />
                </Form.Item>
                <Form.Item label={t('system:date_format')}>
                    <Input defaultValue="MM/dd/yyyy" />
                </Form.Item>
                <Form.Item label={t('system:time_format')}>
                    <Input defaultValue="hh:mm:ss" />
                </Form.Item>
            </Form>
            <Row justify="end">
                <Button style={{ marginTop: 5 }} type="primary">
                    {t('system:apply')}
                </Button>
            </Row>
        </>
    )
}
