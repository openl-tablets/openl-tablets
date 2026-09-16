import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import Forbidden from './403'
import NotFound from './404'
import ServerError from './500'

describe('error pages', () => {
    it('shows the status and what to do about it', () => {
        render(<NotFound />)

        expect(screen.getByText('404')).toBeInTheDocument()
        expect(screen.getByText(/Page not found/)).toBeInTheDocument()
        expect(screen.getByText('Home')).toBeInTheDocument()
    })

    it('offers a way out of a forbidden page', () => {
        render(<Forbidden />)

        expect(screen.getByText('403')).toBeInTheDocument()
        expect(screen.getByRole('link', { name: 'Log out' })).toHaveAttribute('href', 'logout')
    })

    it('reports a server error', () => {
        render(<ServerError />)

        expect(screen.getByText('500')).toBeInTheDocument()
        expect(screen.getByText(/Internal server error/)).toBeInTheDocument()
    })

    it('wears the same card on every page, so the three cannot drift apart', () => {
        const { container: notFound, unmount } = render(<NotFound />)
        const card = notFound.firstElementChild!.firstElementChild!.className
        unmount()

        const { container: serverError } = render(<ServerError />)

        expect(serverError.firstElementChild!.firstElementChild!.className).toBe(card)
    })
})
