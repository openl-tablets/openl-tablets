import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { TableLink } from './TableLink'

const navigate = vi.fn()
vi.mock('react-router-dom', async importOriginal => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => navigate,
}))

const draw = (props: { module?: string, basename?: string } = {}) => render(
    <MemoryRouter basename={props.basename ?? '/'} initialEntries={[props.basename ?? '/']}>
        <TableLink
            data-testid="table-link"
            module={props.module ?? 'Claims'}
            onOpen={vi.fn()}
            projectId="p1"
            tableId="t-1"
        >
            Greeting
        </TableLink>
    </MemoryRouter>
)

describe('TableLink', () => {
    it('keeps the reader on the page when the name is clicked', async () => {
        draw()

        await userEvent.click(screen.getByTestId('table-link'))

        expect(navigate).toHaveBeenCalledWith('/projects/p1/modules/Claims?table=t-1')
    })

    it('writes an address a new window can be opened at, under the path the application is served from', () => {
        draw({ basename: '/webstudio' })

        // Without the served path the address opens at the root of the server, where there is no application.
        expect(screen.getByTestId('table-link'))
            .toHaveAttribute('href', '/webstudio/projects/p1/modules/Claims?table=t-1')
    })

    it('writes the name alone for a table whose module is not known', () => {
        draw({ module: '' })

        expect(screen.getByTestId('table-link')).toHaveTextContent('Greeting')
        expect(screen.getByTestId('table-link')).not.toHaveAttribute('href')
    })
})
