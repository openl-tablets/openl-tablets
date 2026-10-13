import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MergeScopeTabs } from 'containers/MergeModal/MergeScopeTabs'
import type { CheckMergeResult, MergeScope } from 'containers/MergeModal/types'

vi.mock('react-i18next', () => {
    const t = (key: string, options?: Record<string, unknown>) =>
        options && 'count' in options ? `${key}:${String(options['count'])}` : key
    return {
        useTranslation: () => ({ t, i18n: { language: 'en', resolvedLanguage: 'en' } }),
    }
})

const checked = (scope?: MergeScope): CheckMergeResult => ({
    sourceBranch: 'feature',
    targetBranch: 'main',
    status: scope ? 'mergeable' : 'up-to-date',
    canMerge: true,
    ...(scope ? { scope } : {}),
})

const receiveScope: MergeScope = {
    projects: [
        {
            id: 'cmVwbzpQcmljaW5n',
            name: 'Pricing',
            path: 'DESIGN/rules/Pricing',
            current: true,
            status: 'modified',
            changedFiles: 3,
            changedInBoth: ['DESIGN/rules/Pricing/rules/Main.xlsx'],
        },
        {
            id: 'cmVwbzpSYXRpbmc',
            name: 'Rating',
            path: 'DESIGN/rules/Rating',
            current: false,
            status: 'added',
            changedFiles: 2,
        },
        { current: false, changedFiles: 1 },
    ],
    hiddenProjects: 1,
}

describe('MergeScopeTabs', () => {
    it('lists the projects a receive brings in, the current one marked apart from the others', () => {
        render(<MergeScopeTabs receive={checked(receiveScope)} send={checked()} />)

        const projects = screen.getAllByTestId('merge-scope-project')
        expect(projects).toHaveLength(3)
        expect(document.querySelector('.ant-listy')).not.toBeNull()
        expect(within(projects[0]!).getByText('Pricing')).toBeInTheDocument()
        expect(within(projects[0]!).getByText('merge:scope.current')).toBeInTheDocument()
        expect(within(projects[0]!).getByText('merge:scope.changed_files:3')).toBeInTheDocument()
        expect(within(projects[1]!).getByText('merge:scope.other')).toBeInTheDocument()
        expect(within(projects[1]!).getByText('repository:browser.files.change.added')).toBeInTheDocument()
        expect(within(projects[2]!).getByText('merge:scope.outside')).toBeInTheDocument()
    })

    it('names the files both branches changed', () => {
        render(<MergeScopeTabs receive={checked(receiveScope)} send={checked()} />)

        expect(screen.getByTestId('merge-scope-changed-in-both')).toHaveTextContent('DESIGN/rules/Pricing/rules/Main.xlsx')
    })

    it('warns that the merge reaches other projects and counts those the user may not read', () => {
        render(<MergeScopeTabs receive={checked(receiveScope)} send={checked()} />)

        expect(screen.getByTestId('merge-scope-whole-branch')).toBeInTheDocument()
        expect(screen.getByTestId('merge-scope-hidden')).toHaveTextContent('merge:scope.hidden:1')
    })

    it('counts the projects of each direction in its tab', () => {
        render(<MergeScopeTabs receive={checked(receiveScope)} send={checked()} />)

        // Pricing, Rating and the project the user may not read; the files outside projects are no project.
        expect(screen.getAllByText('merge:scope.tab:3')).not.toHaveLength(0)
        expect(screen.getAllByText('merge:scope.tab:0')).not.toHaveLength(0)
    })

    it('says a direction with nothing to merge brings in nothing', async () => {
        render(<MergeScopeTabs receive={checked(receiveScope)} send={checked()} />)

        await userEvent.click(screen.getAllByText('merge:scope.tab:0')[0]!)

        expect(await screen.findByText('merge:scope.nothing')).toBeInTheDocument()
    })

    it('says what a direction brings in is unknown when the server could not tell it, and counts nothing', () => {
        const told: CheckMergeResult = { sourceBranch: 'feature', targetBranch: 'main', status: 'mergeable', canMerge: true }
        render(<MergeScopeTabs receive={told} send={null} />)

        expect(screen.getByTestId('merge-scope-unknown')).toHaveTextContent('merge:scope.unknown')
        expect(screen.queryByText('merge:scope.nothing')).not.toBeInTheDocument()
        // A direction whose check failed is unknown too: neither tab counts projects.
        expect(screen.getAllByText('merge:actions.receive')).not.toHaveLength(0)
        expect(screen.getAllByText('merge:actions.send')).not.toHaveLength(0)
    })

    it('counts the projects of a direction that changes only projects the user cannot read, with no list', () => {
        render(<MergeScopeTabs receive={checked({ projects: [], hiddenProjects: 3 })} send={null} />)

        expect(screen.getByTestId('merge-scope-hidden')).toHaveTextContent('merge:scope.hidden_only:3')
        expect(screen.queryByTestId('merge-scope-project')).not.toBeInTheDocument()
        expect(document.querySelector('.ant-listy')).toBeNull()
    })

    it('does not warn about other projects when only the current project changes', () => {
        const scope: MergeScope = { projects: [receiveScope.projects[0]!], hiddenProjects: 0 }
        render(<MergeScopeTabs receive={checked(scope)} send={null} />)

        expect(screen.queryByTestId('merge-scope-whole-branch')).not.toBeInTheDocument()
        expect(screen.queryByTestId('merge-scope-hidden')).not.toBeInTheDocument()
    })
})
