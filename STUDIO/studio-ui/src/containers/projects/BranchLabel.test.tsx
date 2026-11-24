import { screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { BranchLabel } from './BranchLabel'
import { hex, renderInTheme, tokenFor } from '../../testing/theme'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

const token = tokenFor('standard', false)
const colorOf = (element: HTMLElement) => hex(getComputedStyle(element).color)

describe('BranchLabel', () => {
    it('reads the default branch in blue, its name and its house alike', () => {
        renderInTheme(<BranchLabel isDefault name="master" testId="branch" />)

        expect(colorOf(screen.getByText('master'))).toBe(hex(token.blue))
        expect(screen.getByLabelText('browser.branch.default_tag')).toHaveAttribute('data-testid', 'branch-default')
        expect(colorOf(screen.getByTestId('branch-default'))).toBe(hex(token.blue))
    })

    it('reads a protected branch in orange, its name and its shield alike', () => {
        renderInTheme(<BranchLabel isProtected name="release-1" testId="branch" />)

        expect(colorOf(screen.getByText('release-1'))).toBe(hex(token.orange))
        expect(colorOf(screen.getByTestId('branch-protected'))).toBe(hex(token.orange))
        expect(screen.queryByTestId('branch-default')).toBeNull()
    })

    it('keeps the default branch blue when it is protected too, its shield saying so', () => {
        renderInTheme(<BranchLabel isDefault isProtected name="main" testId="branch" />)

        expect(colorOf(screen.getByText('main'))).toBe(hex(token.blue))
        expect(colorOf(screen.getByTestId('branch-protected'))).toBe(hex(token.orange))
    })

    it('leaves any other branch in the colour of where it stands, with no marks', () => {
        renderInTheme(<BranchLabel name="feature/rates" testId="branch" />)

        expect(colorOf(screen.getByText('feature/rates'))).not.toBe(hex(token.blue))
        expect(colorOf(screen.getByText('feature/rates'))).not.toBe(hex(token.orange))
        expect(screen.queryByTestId('branch-default')).toBeNull()
        expect(screen.queryByTestId('branch-protected')).toBeNull()
    })
})
