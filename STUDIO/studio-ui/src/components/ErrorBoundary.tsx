import React, { Component, ErrorInfo, ReactNode } from 'react'
import { Button, Result, Typography } from 'antd'
import { ReloadOutlined, HomeOutlined, BugOutlined } from '@ant-design/icons'

const { Text, Paragraph } = Typography

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
}

interface State {
  hasError: boolean;
  error: Error | null;
  errorInfo: ErrorInfo | null;
}

export class ErrorBoundary extends Component<Props, State> {
    constructor(props: Props) {
        super(props)
        this.state = {
            hasError: false,
            error: null,
            errorInfo: null,
        }
    }

    static getDerivedStateFromError(error: Error): State {
        return {
            hasError: true,
            error,
            errorInfo: null,
        }
    }

    componentDidCatch(error: Error, errorInfo: ErrorInfo) {
        this.setState({
            error,
            errorInfo,
        })

        // Log error to console in development
        if (process.env.NODE_ENV === 'development') {
            console.error('Error caught by ErrorBoundary:', error, errorInfo)
        }

        // Call custom error handler if provided
        if (this.props.onError) {
            this.props.onError(error, errorInfo)
        }

    // You can also log the error to an error reporting service here
    // Example: logErrorToService(error, errorInfo);
    }

    handleReload = () => {
        window.location.reload()
    }

    handleGoHome = () => {
        window.location.href = '/'
    }

    render() {
        if (this.state.hasError) {
            // Custom fallback UI
            if (this.props.fallback) {
                return this.props.fallback
            }

            // Default error UI
            return (
                <div
                    style={{ 
                        minHeight: '100vh', 
                        display: 'flex', 
                        alignItems: 'center', 
                        justifyContent: 'center',
                        padding: '20px'
                    }}
                >
                    <Result
                        icon={<BugOutlined style={{ color: '#ff4d4f' }} />}
                        status="error"
                        subTitle="We're sorry, but something unexpected happened. Please try again or contact support if the problem persists."
                        title="Something went wrong"
                        extra={[
                            <Button
                                key="reload"
                                icon={<ReloadOutlined />}
                                onClick={this.handleReload}
                                style={{ marginRight: 8 }}
                                type="primary"
                            >
                                Reload Page
                            </Button>,
                            <Button
                                key="home"
                                icon={<HomeOutlined />}
                                onClick={this.handleGoHome}
                            >
                                Go Home
                            </Button>,
                        ]}
                    >
                        {process.env.NODE_ENV === 'development' && this.state.error && (
                            <div
                                style={{ 
                                    marginTop: 24, 
                                    padding: 16, 
                                    backgroundColor: '#f5f5f5', 
                                    borderRadius: 6,
                                    maxWidth: '600px',
                                    overflow: 'auto'
                                }}
                            >
                                <Text strong>Error Details (Development):</Text>
                                <Paragraph style={{ marginTop: 8, marginBottom: 8 }}>
                                    <Text code>{this.state.error.toString()}</Text>
                                </Paragraph>
                                {this.state.errorInfo && (
                                    <Paragraph style={{ marginBottom: 0 }}>
                                        <Text strong>Component Stack:</Text>
                                        <pre
                                            style={{ 
                                                marginTop: 8, 
                                                fontSize: '12px', 
                                                color: '#666',
                                                whiteSpace: 'pre-wrap'
                                            }}
                                        >
                                            {this.state.errorInfo.componentStack}
                                        </pre>
                                    </Paragraph>
                                )}
                            </div>
                        )}
                    </Result>
                </div>
            )
        }

        return this.props.children
    }
}

export default ErrorBoundary
