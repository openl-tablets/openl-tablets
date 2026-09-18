const url = new URL(document.baseURI || '/', window.location.href)
const appContext: string = url.pathname.slice(0, -1) // remove the last slash

/** The one prefix the server mounts its HTTP API on, the WebSocket handshake included. */
export const API_PREFIX = '/rest'

const CONFIG = {
    CONTEXT: appContext,
    /** Where every HTTP call is addressed: the deployment context path followed by the API prefix. */
    API_ROOT: appContext + API_PREFIX,
}

export default CONFIG
