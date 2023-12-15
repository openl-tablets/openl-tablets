import React, { useState } from 'react'
import { Button, Checkbox, Form, Input, Typography, Row } from 'antd'
import { useTranslation } from 'react-i18next'

export const Email: React.FC = () => {
    const { t } = useTranslation()
    const [ active, setActive ] = useState(true)

    return (
        <>
            <Typography.Title level={4}>
                {t('email:email_server_configuration')}
            </Typography.Title>
            <Form
                labelAlign="left"
                labelCol={{ span: 8 }}
                wrapperCol={{ span: 16 }}
            >
                <Form.Item label={t('email:enable_verification')}>
                    <Checkbox onChange={() => setActive(!active)} />
                </Form.Item>
            </Form>
            {!active && (
                <Form
                    labelAlign="left"
                    labelCol={{ span: 8 }}
                    wrapperCol={{ span: 16 }}
                >
                    <Form.Item label={t('email:url')}>
                        <Input />
                    </Form.Item>
                    <Form.Item label={t('email:username')}>
                        <Input defaultValue="admin" />
                    </Form.Item>
                    <Form.Item label={t('email:password')}>
                        <Input.Password />
                    </Form.Item>
                </Form>
            )}
            <Row justify="end">
                <Button style={{ marginTop: 20, marginRight: 20 }} type="primary">
                    {t('email:apply')}
                </Button>
            </Row>
        </>
    )
}
