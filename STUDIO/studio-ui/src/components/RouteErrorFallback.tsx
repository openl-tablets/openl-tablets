import React from 'react'
import { Button } from 'antd'
import { ReloadOutlined, HomeOutlined } from '@ant-design/icons'

export const RouteErrorFallback: React.FC = () => (
    <div
        style={{ 
            minHeight: '100vh', 
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'center',
            padding: '20px',
            marginTop: '-10vh'
        }}
    >
        <div style={{ textAlign: 'center' }}>
            <h2>Oops! Something went wrong</h2>
            <p>We encountered an error while loading this page.</p>
            <Button 
                icon={<ReloadOutlined />} 
                onClick={() => window.location.reload()}
                style={{ marginRight: 8 }}
                type="primary"
            >
                Reload Page
            </Button>
            <Button
                icon={<HomeOutlined />}
                onClick={() => window.location.href = '/'}
            >
                Go Home
            </Button>
        </div>
    </div>
)
