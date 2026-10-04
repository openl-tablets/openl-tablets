import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AboutModal } from './AboutModal'
import { SystemContext } from '../../contexts'
import type { License } from '../../services/licenses'
import type { OpenlInfo } from '../../types/system'

const { fetchLicenses, openLicense } = vi.hoisted(() => ({
    fetchLicenses: vi.fn(),
    openLicense: vi.fn(),
}))

vi.mock('../../services/licenses', async importOriginal => ({
    ...await importOriginal<typeof import('../../services/licenses')>(),
    fetchLicenses,
    openLicense,
}))

vi.mock('react-i18next', () => {
    const t = (key: string, options?: { count?: number }) =>
        [key, options?.count].filter(part => part !== undefined).join(' ')
    return { useTranslation: () => ({ t }) }
})

const FRONTEND: License[] = [
    { name: 'react', version: '19.3.0', identifier: 'MIT', text: 'MIT License' },
    { name: 'no-license-file', version: '1.0.0', identifier: 'ISC' },
]
const BACKEND: License[] = [
    { name: 'org.slf4j:slf4j-api', version: '2.0.17', identifier: 'MIT', url: 'https://opensource.org/license/mit' },
    { name: 'com.example:undeclared', version: '1.0.0', url: 'https://example.com/license' },
]

const openlInfo = (buildDate: string): OpenlInfo => ({
    'openl.site': 'https://openl-tablets.org',
    'openl.version': '6.5.0',
    'openl.build.date': buildDate,
    'openl.start.milli': '0',
    'openl.start.time': '2026-10-04T00:00:00Z',
    'openl.start.hash': 'ABCDEFGH',
})

const renderAbout = (buildDate = '2026-10-04') => {
    const tree = (isOpen: boolean) => (
        <SystemContext.Provider
            value={{
                appVersion: '6.5.0',
                openlInfo: openlInfo(buildDate),
                isExternalAuthSystem: false,
                isUserManagementEnabled: false,
                isGroupsManagementEnabled: false,
                isPersonalAccessTokenEnabled: false,
            }}
        >
            <AboutModal onClose={vi.fn()} open={isOpen} />
        </SystemContext.Provider>
    )
    const view = render(tree(true))
    return { ...view, reopen: (isOpen: boolean) => view.rerender(tree(isOpen)) }
}

describe('AboutModal', () => {
    beforeEach(() => {
        fetchLicenses.mockImplementation(async (side: string) => (side === 'frontend' ? FRONTEND : BACKEND))
    })

    it('shows the version, the build date in the UI language and the license of OpenL Studio', async () => {
        renderAbout()

        await screen.findByText('common:about.version')
        const terms = screen.getAllByRole('term').map(term => [term.textContent, term.nextElementSibling?.textContent])
        expect(terms).toEqual([
            ['common:about.version', '6.5.0'],
            ['common:about.build_date', 'Oct 4, 2026'],
            ['common:about.license', 'common:about.lgpl'],
        ])
        const license = screen.getByRole('link', { name: 'common:about.lgpl' })
        expect(license).toHaveAttribute('href', 'https://www.gnu.org/licenses/lgpl-3.0.html')
        expect(license).toHaveAttribute('target', '_blank')
        expect(license).toHaveAttribute('rel', 'noopener noreferrer')
    })

    it('shows a build date the build did not stamp as the server wrote it', async () => {
        renderAbout('????-??-??')

        const buildDate = (await screen.findByText('common:about.build_date')).nextElementSibling
        expect(buildDate).toHaveTextContent('????-??-??')
    })

    it('shows both sides collapsed, drawing no library until a side is expanded', async () => {
        renderAbout()

        expect(await screen.findByText('common:about.frontend 2')).toBeInTheDocument()
        expect(screen.getByText('common:about.backend 2')).toBeInTheDocument()
        expect(screen.queryByRole('list')).not.toBeInTheDocument()
        expect(fetchLicenses).toHaveBeenCalledWith('frontend')
        expect(fetchLicenses).toHaveBeenCalledWith('backend')
    })

    it('lists the libraries of an expanded side and opens the license chosen', async () => {
        renderAbout()

        await userEvent.click(await screen.findByText('common:about.backend 2'))

        const items = within(screen.getByRole('list')).getAllByRole('listitem')
        expect(items.map(item => item.textContent)).toEqual([
            'org.slf4j:slf4j-api 2.0.17MIT',
            'com.example:undeclared 1.0.0common:about.license',
        ])
        await userEvent.click(within(items[0]!).getByRole('button', { name: 'MIT' }))
        expect(openLicense).toHaveBeenCalledWith(BACKEND[0])
    })

    it('names the license of a library it cannot open without offering to open it', async () => {
        renderAbout()

        await userEvent.click(await screen.findByText('common:about.frontend 2'))

        const items = within(screen.getByRole('list')).getAllByRole('listitem')
        expect(within(items[0]!).getByRole('button', { name: 'MIT' })).toBeInTheDocument()
        expect(items[1]).toHaveTextContent('no-license-file 1.0.0ISC')
        expect(within(items[1]!).queryByRole('button')).not.toBeInTheDocument()
    })

    it('hides the libraries again when the side is collapsed', async () => {
        renderAbout()
        const summary = await screen.findByText('common:about.frontend 2')

        await userEvent.click(summary)
        expect(await screen.findByRole('list')).toBeInTheDocument()
        await userEvent.click(summary)

        await vi.waitFor(() => expect(screen.queryByRole('list')).not.toBeInTheDocument())
    })

    it('reports a list the server cannot answer with, and still shows the other', async () => {
        fetchLicenses.mockImplementation(async (side: string) => {
            if (side === 'backend') {
                throw new Error('Failed to read the backend licenses: 404')
            }
            return FRONTEND
        })
        renderAbout()

        await userEvent.click(await screen.findByText('common:about.backend 0'))

        expect(screen.getByText('common:about.unavailable')).toBeInTheDocument()
        await userEvent.click(screen.getByText('common:about.frontend 2'))
        expect(screen.getAllByRole('listitem')).toHaveLength(2)
    })

    it('reads the lists once, however often the dialog is shown', async () => {
        const { reopen } = renderAbout()
        await screen.findByText('common:about.frontend 2')

        reopen(false)
        reopen(true)

        expect(await screen.findByText('common:about.frontend 2')).toBeInTheDocument()
        expect(fetchLicenses).toHaveBeenCalledTimes(2)
    })
})
