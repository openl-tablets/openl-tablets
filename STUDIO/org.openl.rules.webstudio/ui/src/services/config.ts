const re = /\/web$/
const basePath: string = window.__APP_PUBLIC_PATH__ || '/web'
const appContext: string = window.__APP_PUBLIC_PATH__?.replace(re, '') || ''

const CONFIG = {
    CONTEXT: appContext,
    BASE_PATH: basePath,
}

export default CONFIG