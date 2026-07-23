import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { StatusMark, StatusPill } from './StatusIndicator'
import { ProjectStatus } from '../../constants/project'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

describe('StatusMark', () => {
    it.each([
        [ProjectStatus.Editing, 'browser.status.editing'],
        [ProjectStatus.ViewingVersion, 'browser.status.viewing_version'],
    ])('marks %s, the states a user has to act on', (status, label) => {
        render(<StatusMark status={status} testId="mark" />)

        expect(screen.getByTestId('mark')).toHaveAttribute('aria-label', label)
    })

    it.each([ProjectStatus.Opened, ProjectStatus.Closed, ProjectStatus.Local])('leaves %s unmarked', status => {
        render(<StatusMark status={status} testId="mark" />)

        expect(screen.queryByTestId('mark')).toBeNull()
    })
})

describe('StatusPill', () => {
    it('names an open project by its status in a list', () => {
        render(<StatusPill status={ProjectStatus.Opened} testId="pill" />)

        expect(screen.getByTestId('pill')).toHaveTextContent('browser.status.no_changes')
    })

    it('reads an open project as having no changes on its own screen', () => {
        render(<StatusPill status={ProjectStatus.Opened} testId="pill" />)

        expect(screen.getByTestId('pill')).toHaveTextContent('browser.status.no_changes')
    })

    it('keeps one wording where a status has no separate one', () => {
        render(<StatusPill status={ProjectStatus.Editing} testId="pill" />)

        expect(screen.getByTestId('pill')).toHaveTextContent('browser.status.editing')
    })
})
