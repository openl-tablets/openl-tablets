import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { Project } from '../../types/projects'
import { getProjects } from '../../services/repositories'
import { ProjectSwitcher } from './ProjectSwitcher'
import { recentProjects } from '../../utils/recentChoices'

vi.mock('../../services/repositories', () => ({ getProjects: vi.fn() }))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('../../components/SearchInput', () => ({
    SearchInput: (props: Record<string, unknown>) => (
        <input
            data-testid={props['data-testid'] as string}
            onChange={props['onChange'] as never}
            placeholder={props['placeholder'] as string}
            value={props['value'] as string}
        />
    ),
}))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

vi.mock('@ant-design/icons', () => ({
    DownOutlined: () => null,
    LoadingOutlined: (props: Record<string, unknown>) => <i data-testid={props['data-testid'] as string} />,
}))

const listed = (...names: string[]) => ({
    content: names.map((name, index) => ({ id: `p${index}`, name }) as Project),
    pageNumber: 0,
    pageSize: names.length,
    numberOfElements: names.length,
    total: names.length,
})

const draw = (onSelect = vi.fn(), withLink = true) => {
    render(
        <ProjectSwitcher
            currentName="Bank Rating"
            currentProjectId="p0"
            onSelect={onSelect}
            testId="crumb-project"
            withLink={withLink}
        />
    )
    return onSelect
}

/** The names the list offers, in its order. */
const offered = () => screen.getAllByRole('menuitem').map(item => item.textContent)

describe('ProjectSwitcher', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        localStorage.clear()
        vi.mocked(getProjects).mockResolvedValue(listed('Bank Rating', 'Car Rating'))
    })

    it('asks for no project until the reader opens the list', async () => {
        draw()

        // Listing a workspace of hundreds takes seconds; a reader who never opens the list waits for nothing.
        expect(getProjects).not.toHaveBeenCalled()

        await userEvent.click(screen.getByTestId('crumb-project-trigger'))

        await waitFor(() => expect(getProjects).toHaveBeenCalledTimes(1))
        expect(await screen.findByText('Car Rating')).toBeInTheDocument()
    })

    it('says the projects are being read rather than showing none to choose from', async () => {
        let answer = (_projects: unknown) => undefined as void
        vi.mocked(getProjects).mockReturnValue(new Promise(resolve => { answer = resolve as never }))
        draw()

        await userEvent.click(screen.getByTestId('crumb-project-trigger'))

        expect(await screen.findByTestId('crumb-project-list-loading')).toBeInTheDocument()
        answer(listed('Bank Rating'))
        await waitFor(() => expect(screen.queryByTestId('crumb-project-list-loading')).toBeNull())
    })

    it('narrows the list by name without asking the server again', async () => {
        draw()
        await userEvent.click(screen.getByTestId('crumb-project-trigger'))
        await screen.findByText('Car Rating')

        await userEvent.type(screen.getByTestId('crumb-project-search'), 'car')

        // The one left is the one searched for; the project that is open still names the trigger.
        expect(screen.getAllByText('Bank Rating')).toHaveLength(1)
        expect(screen.getByText('Car Rating')).toBeInTheDocument()
        expect(getProjects).toHaveBeenCalledTimes(1)
    })

    it('opens the project the reader picked', async () => {
        const onSelect = draw()
        await userEvent.click(screen.getByTestId('crumb-project-trigger'))

        await userEvent.click(await screen.findByText('Car Rating'))

        expect(onSelect).toHaveBeenCalledWith('p1')
    })

    it('says nothing matched rather than leaving the list blank', async () => {
        draw()
        await userEvent.click(screen.getByTestId('crumb-project-trigger'))
        await screen.findByText('Car Rating')

        await userEvent.type(screen.getByTestId('crumb-project-search'), 'nothing')

        expect(screen.getByText('browser.module.project_no_match')).toBeInTheDocument()
    })

    it('leads with the projects opened most recently, then lists the others by name', async () => {
        vi.mocked(getProjects).mockResolvedValue(listed('Delta', 'alpha', 'Charlie', 'Bravo'))
        recentProjects.remember('p2')
        recentProjects.remember('p0')
        draw()

        await userEvent.click(screen.getByTestId('crumb-project-trigger'))
        await screen.findByText('alpha')

        expect(offered()).toEqual(['Delta', 'Charlie', 'alpha', 'Bravo'])
        expect(document.querySelectorAll('.ant-dropdown-menu-item-divider')).toHaveLength(1)
    })

    it('narrows the recent projects and the others alike', async () => {
        vi.mocked(getProjects).mockResolvedValue(listed('Delta', 'Alpha Rating', 'Charlie Rating'))
        recentProjects.remember('p2')
        draw()

        await userEvent.click(screen.getByTestId('crumb-project-trigger'))
        await screen.findByText('Delta')
        await userEvent.type(screen.getByTestId('crumb-project-search'), 'rating')

        expect(offered()).toEqual(['Charlie Rating', 'Alpha Rating'])
    })

    it('forgets a recent project the list no longer holds', async () => {
        recentProjects.remember('gone')
        recentProjects.remember('p1')
        draw()

        await userEvent.click(screen.getByTestId('crumb-project-trigger'))
        await screen.findByText('Car Rating')

        expect(recentProjects.read()).toEqual(['p1'])
    })

    it('opens the list from the name itself where the name leads nowhere', async () => {
        draw(vi.fn(), false)

        expect(screen.queryByTestId('crumb-project-follow')).toBeNull()
        await userEvent.click(screen.getByTestId('crumb-project'))

        expect(await screen.findByText('Car Rating')).toBeInTheDocument()
    })
})
