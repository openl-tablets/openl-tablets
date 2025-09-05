// Error handling utilities for consistent error management across the application

export interface ErrorInfo {
  message: string;
  stack?: string;
  componentStack?: string;
  timestamp: Date;
  userId?: string;
  sessionId?: string;
  url?: string;
  userAgent?: string;
}

export class ErrorHandler {
    private static instance: ErrorHandler
    private errorLog: ErrorInfo[] = []
    private maxLogSize = 100

    private constructor() {}

    static getInstance(): ErrorHandler {
        if (!ErrorHandler.instance) {
            ErrorHandler.instance = new ErrorHandler()
        }
        return ErrorHandler.instance
    }

    // Log an error with additional context
    logError(error: Error, context?: Partial<ErrorInfo>): void {
        const errorInfo: ErrorInfo = {
            message: error.message,
            stack: error.stack,
            timestamp: new Date(),
            url: window.location.href,
            userAgent: navigator.userAgent,
            ...context,
        }

        // Add to local log
        this.errorLog.push(errorInfo)
        if (this.errorLog.length > this.maxLogSize) {
            this.errorLog.shift()
        }

        // Log to console in development
        if (process.env.NODE_ENV === 'development') {
            console.error('Error logged:', errorInfo)
        }

        // You can send to external error reporting service here
        this.sendToErrorService(errorInfo)
    }

    // Send error to external service (implement as needed)
    private sendToErrorService(_: ErrorInfo): void {
    // Example: Send to Sentry, LogRocket, or your own error tracking service
    // if (process.env.REACT_APP_ERROR_REPORTING_URL) {
    //   fetch(process.env.REACT_APP_ERROR_REPORTING_URL, {
    //     method: 'POST',
    //     headers: { 'Content-Type': 'application/json' },
    //     body: JSON.stringify(errorInfo),
    //   }).catch(console.error);
    // }
    }
}

// Export singleton instance
export const errorHandler = ErrorHandler.getInstance()

// Global error event handlers
export const setupGlobalErrorHandling = (): void => {
    const originalOnError = window.onerror
    const originalOnUnhandledRejection = window.onunhandledrejection

    window.onerror = (message, source, lineno, colno, error) => {
        if (error) {
            errorHandler.logError(error, {
                message: `Global Error: ${message}`,
                url: source,
            })
        }
    
        if (originalOnError) {
            return originalOnError(message, source, lineno, colno, error)
        }
    }

    window.onunhandledrejection = (event) => {
        const error = event.reason instanceof Error ? event.reason : new Error(String(event.reason))
        errorHandler.logError(error, {
            message: `Unhandled Promise Rejection: ${event.reason}`,
        })
    
        if (originalOnUnhandledRejection) {
            // @ts-ignore
            return originalOnUnhandledRejection(event)
        }
    }
}
