import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { needsDocumentLoad, useAppNavigate } from './useAppNavigate'

vi.mock('../services', () => ({ CONFIG: { CONTEXT: '/web' } }))

const Probe = ({ to }: { to: string }) => {
    const appNavigate = useAppNavigate()
    const { pathname } = useLocation()
    return (
        <>
            <span data-testid="path">{pathname}</span>
            <button onClick={() => appNavigate(to)} type="button">go</button>
        </>
    )
}

describe('needsDocumentLoad', () => {
    it('keeps a transition between the app\'s own screens inside the app', () => {
        expect(needsDocumentLoad('/projects', '/deployments')).toBe(false)
        expect(needsDocumentLoad('/administration/system', '/projects')).toBe(false)
        expect(needsDocumentLoad('/projects/abc', '/projects')).toBe(false)
    })

    it('loads a whole document to or from a legacy server-rendered page', () => {
        expect(needsDocumentLoad('/projects', '/')).toBe(true)
        expect(needsDocumentLoad('/', '/projects')).toBe(true)
        expect(needsDocumentLoad('/faces/main.xhtml', '/projects')).toBe(true)
    })
})

describe('useAppNavigate', () => {
    it('swaps the screen in place between the app\'s own screens', async () => {
        render(
            <MemoryRouter initialEntries={['/projects']}>
                <Probe to="/deployments" />
            </MemoryRouter>
        )

        await userEvent.click(screen.getByText('go'))

        expect(screen.getByTestId('path').textContent).toBe('/deployments')
    })
})
