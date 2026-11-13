import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { booleanText, storedSetting } from '../utils/localStore'
import { useStoredSetting } from './useStoredSetting'

const folded = storedSetting('test.folded', false, booleanText)

/** A panel that is folded and unfolded by a click, keeping the choice in the browser. */
const Panel = ({ initial }: { initial?: boolean }) => {
    const [isFolded, fold] = useStoredSetting(folded, initial)
    return <button onClick={() => fold(!isFolded)} type="button">{isFolded ? 'folded' : 'open'}</button>
}

describe('useStoredSetting', () => {
    beforeEach(() => localStorage.clear())

    it('starts from the choice kept, and keeps every change at once', async () => {
        localStorage.setItem('test.folded', 'true')
        render(<Panel />)

        expect(screen.getByRole('button')).toHaveTextContent('folded')

        await userEvent.click(screen.getByRole('button'))

        expect(screen.getByRole('button')).toHaveTextContent('open')
        // open is the default, kept as no key at all
        expect(localStorage.getItem('test.folded')).toBeNull()
    })

    it('starts from what its caller decides, and still keeps a later change', async () => {
        render(<Panel initial />)

        expect(screen.getByRole('button')).toHaveTextContent('folded')
        expect(localStorage.getItem('test.folded')).toBeNull()

        await userEvent.click(screen.getByRole('button'))
        await userEvent.click(screen.getByRole('button'))

        expect(localStorage.getItem('test.folded')).toBe('true')
    })
})
