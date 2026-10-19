import React from 'react'
import { render, screen } from '@testing-library/react'
import { RedirectRoute } from './RedirectRoute'

const route = vi.hoisted((): { page?: string | undefined } => ({}))

vi.mock('react-router', () => ({ useParams: () => route }))
vi.mock('../services', () => ({ CONFIG: { CONTEXT: '/studio' } }))
vi.mock('../pages/404', () => ({ default: () => <div>not found</div> }))

describe('RedirectRoute', () => {
    beforeEach(() => {
        route.page = undefined
        vi.stubGlobal('location', { href: 'http://localhost/studio/x' })
    })

    afterEach(() => {
        vi.unstubAllGlobals()
    })

    it('leaves for the editor page of the server', () => {
        route.page = 'editor'

        const { container } = render(<RedirectRoute />)

        expect(container).toBeEmptyDOMElement()
        expect(window.location.href).toBe('/studio/')
    })

    it('leaves for the sign-out of the server', () => {
        const { container } = render(<RedirectRoute to="logout" />)

        expect(container).toBeEmptyDOMElement()
        expect(window.location.href).toBe('/studio/logout')
    })

    it('shows that any other page is not found, and stays', () => {
        route.page = 'other'

        render(<RedirectRoute />)

        expect(screen.getByText('not found')).toBeInTheDocument()
        expect(window.location.href).toBe('http://localhost/studio/x')
    })
})
