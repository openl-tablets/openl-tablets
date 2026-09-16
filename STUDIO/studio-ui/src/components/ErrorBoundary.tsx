import { Component, ErrorInfo, ReactNode } from 'react'
import { Button, Result, Typography } from 'antd'
import { ReloadOutlined, HomeOutlined, BugOutlined } from '@ant-design/icons'
import { errorHandler } from 'utils/errorHandling'
import { CONFIG } from '../services'
import { useStyles } from './ErrorBoundary.styles'

const { Text, Paragraph } = Typography

/**
 * The technical detail of a crash, shown by the development build only.
 *
 * It is a component of its own because the boundary around it is a class — the only place that can catch
 * a render error — and the theme is read with a hook.
 */
const ErrorDetails = ({ componentStack, error }: { componentStack: string | null | undefined; error: Error }) => {
    const { styles } = useStyles()

    return (
        <div className={styles.details}>
            <Text strong>Error Details (Development):</Text>
            <Paragraph style={{ marginTop: 8, marginBottom: 8 }}>
                <Text code>{error.toString()}</Text>
            </Paragraph>
            {componentStack && (
                <Paragraph style={{ marginBottom: 0 }}>
                    <Text strong>Component Stack:</Text>
                    <pre className={styles.stack}>{componentStack}</pre>
                </Paragraph>
            )}
        </div>
    )
}

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

        errorHandler.logError(error, { componentStack: errorInfo.componentStack ?? undefined })

        // Call custom error handler if provided
        if (this.props.onError) {
            this.props.onError(error, errorInfo)
        }
    }

    handleReload = () => {
        window.location.reload()
    }

    handleGoHome = () => {
        window.location.href = `${CONFIG.CONTEXT}/`
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
                        icon={<BugOutlined />}
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
                        {import.meta.env.DEV && import.meta.env.MODE !== 'test' && this.state.error && (
                            <ErrorDetails
                                componentStack={this.state.errorInfo?.componentStack}
                                error={this.state.error}
                            />
                        )}
                    </Result>
                </div>
            )
        }

        return this.props.children
    }
}

export default ErrorBoundary
