const url = new URL(document.baseURI || '/', window.location.href)
const appContext: string = url.pathname.slice(0, -1) // remove the last slash

/** The one prefix the server mounts its REST API on. The WebSocket handshake has an address of its own, `/ws`. */
const API_PREFIX = '/rest'

const CONFIG = {
    CONTEXT: appContext,
    /** Where every REST call is addressed: the deployment context path followed by the API prefix. */
    API_ROOT: appContext + API_PREFIX,
}

export default CONFIG
