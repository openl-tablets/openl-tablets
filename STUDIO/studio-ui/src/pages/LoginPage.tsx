import React, { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { Alert, Button, Form, Input } from 'antd'
import { useTranslation } from 'react-i18next'
import { CONFIG } from '../services'
import Logo from '../components/Logo'

const containerStyle: React.CSSProperties = {
    minHeight: '100vh',
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'center',
    alignItems: 'center',
    background: 'linear-gradient(135deg, #f0f4fa 0%, #e9effa 100%)',
}

const cardStyle: React.CSSProperties = {
    background: '#fff',
    padding: '48px 40px',
    borderRadius: 16,
    boxShadow: '0 4px 24px rgba(0,0,0,0.08)',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    maxWidth: 400,
    width: '100%',
}

const titleStyle: React.CSSProperties = {
    fontSize: 28,
    fontWeight: 700,
    color: '#1763C6',
    margin: '24px 0 16px 0',
    textAlign: 'center',
}

const LoginPage = () => {
    const { t } = useTranslation('security')
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const { pathname, search } = useLocation()

    const onFinish = (values: { username: string; password: string }) => {
        setLoading(true)
        setError(null)
        const encodedBody = new URLSearchParams({
            username: values.username,
            password: values.password,
        }).toString()
        fetch(`${CONFIG.CONTEXT}${pathname}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: encodedBody
        }).then((response) => {
            if (response.redirected) {
                window.location.href = response.url // manually trigger browser redirect
            } else if (response.ok) {
                window.location.href = `${CONFIG.CONTEXT}/` // or default success redirect
            } else {
                setError(t('login_failed_message'))
            }
        }).finally(() => {
            setLoading(false)
        })
    }

    useEffect(() => {
        if (search === '?error') {
            setError(t('login_failed_message'))
        }
    }, [search, t])

    return (
        <div style={containerStyle}>
            <div style={cardStyle}>
                <Logo height={72} width={72} />
                <div style={titleStyle}>{t('sign_in_to_openl_studio')}</div>
                {error && <Alert showIcon title={error} style={{ marginBottom: 16, width: '100%' }} type="error" />}
                <Form layout="vertical" onFinish={onFinish} style={{ width: '100%' }}>
                    <Form.Item label={t('username_label')} name="username" rules={[{ required: true, message: t('username_required') }]}>
                        <Input autoFocus autoComplete="username" size="large" />
                    </Form.Item>
                    <Form.Item label={t('password_label')} name="password" rules={[{ required: true, message: t('password_required') }]}>
                        <Input.Password autoComplete="current-password" size="large" />
                    </Form.Item>
                    <Form.Item>
                        <Button block htmlType="submit" loading={loading} size="large" type="primary">
                            {t('log_in_button')}
                        </Button>
                    </Form.Item>
                </Form>
            </div>
        </div>
    )
}

export default LoginPage
