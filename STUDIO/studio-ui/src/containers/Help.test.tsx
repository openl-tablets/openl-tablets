import React from 'react'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
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
    it('links every guide of the release to its folder on the documentation site', () => {
        renderHelp(openlInfo('6.4.0'))

        expect(guideLinks()).toEqual({
            'Getting Started': 'https://openl-tablets.org/openl-tablets/6.4.0/user-guides/getting-started',
            'Installation Guide': 'https://openl-tablets.org/openl-tablets/6.4.0/user-guides/installation-guide',
            'Reference Guide': 'https://openl-tablets.org/openl-tablets/6.4.0/user-guides/reference-guide',
            'OpenL Studio User Guide': 'https://openl-tablets.org/openl-tablets/6.4.0/user-guides/openl-studio',
            'Rule Services Usage and Customization Guide':
                'https://openl-tablets.org/openl-tablets/6.4.0/user-guides/rule-services',
        })
    })

    it('links the guides of the next release from a snapshot build', () => {
        renderHelp(openlInfo('6.5.0-SNAPSHOT'))

        expect(Object.values(guideLinks())).toSatisfy((links: (string | null)[]) =>
            links.every(link => link?.startsWith('https://openl-tablets.org/openl-tablets/next/user-guides/')))
    })

    it('offers no PDF guides any more', () => {
        renderHelp(openlInfo('6.4.0'))

        expect(screen.queryByRole('link', { name: 'Developer Guide' })).not.toBeInTheDocument()
        expect(document.querySelectorAll('a[href$=".pdf"]')).toHaveLength(0)
    })

    it('falls back to the official site while the build information is not loaded', () => {
        renderHelp()

        expect(screen.getByRole('link', { name: 'Installation Guide' }))
            .toHaveAttribute('href', 'https://openl-tablets.org/openl-tablets/unknown/user-guides/installation-guide')
    })
})
