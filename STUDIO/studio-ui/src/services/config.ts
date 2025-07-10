const url = new URL(document.baseURI || '/', window.location.href)
const appContext: string = url.pathname.slice(0, -1) // remove the last slash

const CONFIG = {
    CONTEXT: appContext,
    LOGIN_URL: `${appContext}/faces/pages/login.xhtml`,
}

export default CONFIG
