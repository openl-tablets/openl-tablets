import { useSyncExternalStore } from 'react'
import type { RecentChoices } from '../utils/recentChoices'

/**
 * The choices of the scope made most recently, the latest first, drawn again whenever they change: in this switcher,
 * in another one on screen, or in another tab. A change of another scope draws nothing again.
 *
 * @param choices the store of the choices, such as the recent branches
 * @param scope   the list of the store to read, such as the repository; the single list when absent
 */
export const useRecentChoices = (choices: RecentChoices, scope = ''): readonly string[] =>
    useSyncExternalStore(choices.subscribe, () => choices.read(scope))
