import { useCallback, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { toUrlSafeId } from '../services/projectId'

/**
 * Keeps the address of a project screen on the project's id.
 *
 * A link may name a project by its name, or spell its id the older way, and the server reads both. Once the
 * project is read for such an address, the address is replaced by the project's id, and the screen shows the
 * project there. A link by name stops working once a second project takes that name; the id keeps pointing at
 * this project.
 *
 * Only the first read of an address may replace it. A read that follows, after an action or a change pushed by the
 * server, leaves the address as it is.
 *
 * `routeOf` builds the screen's address for an id. It keeps what the address opens, such as the tab, the module or
 * the table.
 *
 * @returns `readdress`, which takes the id the server issued for the project just read. It answers whether it
 * replaced the address: the screen then reads the project again at the new address rather than showing it here.
 */
export const useCanonicalProjectAddress = (projectId: string | undefined, routeOf: (id: string) => string) => {
    const navigate = useNavigate()
    // Read when the project arrives rather than when the screen draws: a builder that changes with the tab must not
    // make the screen read its project again.
    const route = useRef(routeOf)
    useEffect(() => {
        route.current = routeOf
    }, [routeOf])
    // The address whose first read has answered already.
    const settled = useRef<string | undefined>(undefined)

    return useCallback((id: string): boolean => {
        if (projectId === undefined || settled.current === projectId) {
            return false
        }
        settled.current = projectId
        const canonical = toUrlSafeId(id)
        if (canonical === projectId) {
            return false
        }
        navigate(route.current(canonical), { replace: true })
        return true
    }, [navigate, projectId])
}
