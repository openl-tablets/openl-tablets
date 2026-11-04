import React from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ScreenMemory, useRemembered } from './useRemembered'

/** A part of a screen counting the clicks it was given, remembered under its name. */
const Counter: React.FC<{ name: string }> = ({ name }) => {
    const [count, setCount] = useRemembered(name, () => 0)
    return <button onClick={() => setCount(current => current + 1)} type="button">{`${name} ${count}`}</button>
}

describe('useRemembered', () => {
    it('gives a part drawn again what it was, on a screen that remembers', async () => {
        const memory = new Map<string, unknown>()
        const screenOf = (drawn: boolean) => (
            <ScreenMemory.Provider value={memory}>{drawn && <Counter name="cases" />}</ScreenMemory.Provider>
        )
        const { rerender } = render(screenOf(true))
        await userEvent.click(screen.getByText('cases 0'))
        await userEvent.click(screen.getByText('cases 1'))

        // Scrolled out of sight, and back.
        rerender(screenOf(false))
        rerender(screenOf(true))

        expect(screen.getByText('cases 2')).toBeInTheDocument()
    })

    it('remembers what a part was told after it went out of sight, as a read that came back late', async () => {
        const memory = new Map<string, unknown>()
        let tellLate: ((count: number) => void) | undefined
        const Reader: React.FC = () => {
            const [count, setCount] = useRemembered('read', () => 0)
            tellLate = setCount
            return <span>{`read ${count}`}</span>
        }
        const screenOf = (drawn: boolean) => (
            <ScreenMemory.Provider value={memory}>{drawn && <Reader />}</ScreenMemory.Provider>
        )
        const { rerender } = render(screenOf(true))
        rerender(screenOf(false))

        tellLate?.(5)
        rerender(screenOf(true))

        expect(screen.getByText('read 5')).toBeInTheDocument()
        // A part nobody changed takes no room.
        expect([...memory.keys()]).toEqual(['read'])
    })

    it('starts a part drawn again anew on a screen that remembers nothing', async () => {
        const { rerender } = render(<Counter name="cases" />)
        await userEvent.click(screen.getByText('cases 0'))

        rerender(<span />)
        rerender(<Counter name="cases" />)

        expect(screen.getByText('cases 0')).toBeInTheDocument()
    })
})
