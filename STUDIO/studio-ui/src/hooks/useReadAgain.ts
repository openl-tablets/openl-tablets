import { useCallback, useRef } from 'react'

/**
 * Tells a read a screen makes again from its first one.
 *
 * The first read of what the key names may take what another screen kept of it, so moving between the screens of a
 * project costs no request. Every later read asks the server: it follows an action, a switch of the branch or a ping,
 * and the kept read no longer describes the workspace. A new key, such as another project, starts over.
 *
 * @returns a function answering, for each read the screen makes, whether it is a read again
 */
export const useReadAgain = (key: string): (() => boolean) => {
    const readFor = useRef<string | null>(null)
    return useCallback(() => {
        const again = readFor.current === key
        readFor.current = key
        return again
    }, [key])
}
