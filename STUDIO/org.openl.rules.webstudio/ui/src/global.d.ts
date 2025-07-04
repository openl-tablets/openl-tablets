/// <reference types="react-scripts" />

declare global {
    interface Window {
        // Global runtime variables
        __APP_SOURCE_PATH__: string
        __APP_PUBLIC_PATH__: string
    }
}

export {}