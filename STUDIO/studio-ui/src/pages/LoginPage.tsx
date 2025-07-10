import React, { useState } from 'react'
import { Button, Form, Input, Alert } from 'antd'
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
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const onFinish = (values: { username: string; password: string }) => {
        setLoading(true)
        setError(null)
        // TODO: Replace with real authentication logic
        setTimeout(() => {
            if (values.username === 'admin' && values.password === 'admin') {
                window.location.href = '/'
            } else {
                setError('Invalid username or password')
                setLoading(false)
            }
        }, 1000)
    }

    return (
        <div style={containerStyle}>
            <div style={cardStyle}>
                <Logo height={72} width={72} />
                <div style={titleStyle}>Sign in to OpenL Studio</div>
                {error && <Alert showIcon message={error} style={{ marginBottom: 16, width: '100%' }} type="error" />}
                <Form layout="vertical" onFinish={onFinish} style={{ width: '100%' }}>
                    <Form.Item label="Username" name="username" rules={[{ required: true, message: 'Please enter your username' }]}> 
                        <Input autoFocus autoComplete="username" size="large" />
                    </Form.Item>
                    <Form.Item label="Password" name="password" rules={[{ required: true, message: 'Please enter your password' }]}> 
                        <Input.Password autoComplete="current-password" size="large" />
                    </Form.Item>
                    <Form.Item>
                        <Button block htmlType="submit" loading={loading} size="large" type="primary">
                            Log in
                        </Button>
                    </Form.Item>
                </Form>
            </div>
        </div>
    )
}

export default LoginPage