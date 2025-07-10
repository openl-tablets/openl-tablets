import React from 'react'
import { Button } from 'antd'
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

const codeStyle: React.CSSProperties = {
    fontSize: 72,
    fontWeight: 700,
    color: '#1763C6',
    marginBottom: 8,
}

const messageStyle: React.CSSProperties = {
    fontSize: 20,
    color: '#333',
    marginBottom: 24,
    textAlign: 'center',
}

const NotFound = () => {
    return (
        <div style={containerStyle}>
            <div style={cardStyle}>
                <Logo height={72} width={72} />
                <div style={codeStyle}>404</div>
                <div style={messageStyle}>Page not found.<br />Go to the home page.</div>
                <Button href="/" size="large" type="primary">Home</Button>
            </div>
        </div>
    )
}

export default NotFound
