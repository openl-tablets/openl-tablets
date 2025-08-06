const url = new URL(document.baseURI || '/', window.location.href)
const appContext: string = url.pathname.slice(0, -1) // remove the last slash

const CONFIG = {
    CONTEXT: appContext,
}

export default CONFIG
