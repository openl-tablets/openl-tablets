import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ResizeHandle, useDragSize } from './ResizeHandle'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

/** A panel dragged by its right edge, which is the shape every rail of the editor has. */
const Panel = () => {
    const { size, startResize } = useDragSize('openl.test.width', 'right', { min: 10, max: 100, fallback: 50 })
    return (
        <div data-testid="panel" style={{ position: 'relative', width: size }}>
            <ResizeHandle edge="right" onPointerDown={startResize} testId="grip" />
        </div>
    )
}

describe('useDragSize', () => {
    it('sizes the panel to where the pointer is taken, and keeps it for the next visit', () => {
        const { unmount } = render(<Panel />)

        fireEvent.pointerDown(screen.getByTestId('grip'))
        fireEvent(window, new MouseEvent('pointermove', { clientX: 80 }))

        expect(screen.getByTestId('panel')).toHaveStyle({ width: '80px' })

        fireEvent(window, new MouseEvent('pointerup', { clientX: 80 }))
        unmount()
        render(<Panel />)
        expect(screen.getByTestId('panel')).toHaveStyle({ width: '80px' })
    })

    it('stops following the pointer when the panel it belongs to goes away mid-drag', () => {
        const detached = vi.spyOn(window, 'removeEventListener')
        const { unmount } = render(<Panel />)

        fireEvent.pointerDown(screen.getByTestId('grip'))
        unmount()

        // A drag is followed on the window; left attached, it would keep sizing a panel nobody can see.
        expect(detached).toHaveBeenCalledWith('pointermove', expect.any(Function))
        expect(detached).toHaveBeenCalledWith('pointerup', expect.any(Function))
        detached.mockRestore()
    })
})
