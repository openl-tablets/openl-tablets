import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { WorkspaceHeader } from './WorkspaceHeader'

describe('WorkspaceHeader', () => {
    it('closes the trail with a separator, so the title reads as its next step', () => {
        render(<WorkspaceHeader crumbs={<a href="/projects">Projects</a>} testId="header" title="Bank Rating" />)

        const trail = screen.getByRole('link', { name: 'Projects' }).parentElement
        expect(trail?.lastElementChild).toHaveTextContent('/')
        expect(trail?.lastElementChild).toHaveAttribute('aria-hidden')
    })
})
