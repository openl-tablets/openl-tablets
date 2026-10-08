import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { Help } from './Help'
import { SystemContext } from '../contexts'
import type { OpenlInfo } from '../types/system'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

const openlInfo = (version: string): OpenlInfo => ({
    'openl.site': 'https://openl-tablets.org',
    'openl.version': version,
    'openl.start.milli': '0',
    'openl.build.date': '2026-10-01',
    'openl.start.time': '2026-10-01T00:00:00Z',
    'openl.start.hash': 'ABCDEFGH',
})

const renderHelp = (info?: OpenlInfo) => render(
    <MemoryRouter>
        <SystemContext.Provider
            value={{
                openlInfo: info,
                isExternalAuthSystem: false,
                isUserManagementEnabled: false,
                isGroupsManagementEnabled: false,
                isPersonalAccessTokenEnabled: false,
            }}
        >
            <Help />
        </SystemContext.Provider>
    </MemoryRouter>
)

const guideLinks = () => Object.fromEntries(
    [
        'Getting Started',
        'Installation Guide',
        'Reference Guide',
        'OpenL Studio User Guide',
        'Rule Services Usage and Customization Guide',
    ].map(title => [title, screen.getByRole('link', { name: title }).getAttribute('href')])
)

describe('Help', () => {
    it('opens every guide inside OpenL Studio, where the build ships it', () => {
        renderHelp(openlInfo('6.4.0'))

        expect(guideLinks()).toEqual({
            'Getting Started': '/docs/getting-started/',
            'Installation Guide': '/docs/installation-guide/',
            'Reference Guide': '/docs/reference-guide/',
            'OpenL Studio User Guide': '/docs/openl-studio/',
            'Rule Services Usage and Customization Guide': '/docs/rule-services/',
        })
    })

    it('opens the guides in the same tab, as a screen of the application', () => {
        renderHelp(openlInfo('7.0.0'))

        expect(screen.getByRole('link', { name: 'Reference Guide' })).not.toHaveAttribute('target')
    })

    it('offers no PDF guides any more', () => {
        renderHelp(openlInfo('6.4.0'))

        expect(screen.queryByRole('link', { name: 'Developer Guide' })).not.toBeInTheDocument()
        expect(document.querySelectorAll('a[href$=".pdf"]')).toHaveLength(0)
    })

    it('links the official site of the build', () => {
        renderHelp({ ...openlInfo('6.4.0'), 'openl.site': 'https://example.org' })

        expect(screen.getByRole('link', { name: 'Official Website' })).toHaveAttribute('href', 'https://example.org')
        expect(screen.getByRole('link', { name: 'OpenL Tablets News' })).toHaveAttribute('href', 'https://example.org/news')
    })

    it('falls back to the official site while the build information is not loaded', () => {
        renderHelp()

        expect(screen.getByRole('link', { name: 'Official Website' })).toHaveAttribute('href', 'https://openl-tablets.org')
    })
})
