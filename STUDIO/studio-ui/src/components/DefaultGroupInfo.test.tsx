import React from 'react'
import { render, screen } from '@testing-library/react'
import { SystemContext } from 'contexts/SystemContext'
import type { MockedFunction } from 'vitest'

vi.mock('services', () => ({
    apiCall: vi.fn(),
}))

vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string) => key,
        i18n: { language: 'en' },
    }),
    Trans: ({ i18nKey }: { i18nKey: string }) => <span>{i18nKey}</span>,
}))

vi.mock('hooks/useDefaultGroup', () => ({
    useDefaultGroup: vi.fn(),
}))

vi.mock('containers/groups/useGroups', () => ({
    useGroups: vi.fn(),
}))

import { useDefaultGroup } from 'hooks/useDefaultGroup'
import { useGroups } from 'containers/groups/useGroups'
import { DefaultGroupInfo } from 'components/DefaultGroupInfo'

const mockUseDefaultGroup = useDefaultGroup as MockedFunction<typeof useDefaultGroup>
const mockUseGroups = useGroups as MockedFunction<typeof useGroups>

const renderWithContext = (overrides: Partial<React.ContextType<typeof SystemContext>> = {}) => {
    const defaultCtx = {
        isExternalAuthSystem: false,
        isUserManagementEnabled: true,
        isGroupsManagementEnabled: true,
        isPersonalAccessTokenEnabled: false,
        systemSettings: { userMode: 'multi' } as any,
    }
    return render(
        <SystemContext.Provider value={{ ...defaultCtx, ...overrides }}>
            <DefaultGroupInfo />
        </SystemContext.Provider>
    )
}

describe('DefaultGroupInfo', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mockUseGroups.mockReturnValue({
            groups: [{ name: 'Viewers', admin: false, id: 1, description: '', numberOfMembers: 2 }],
            loading: false,
            error: null,
            reloadGroups: vi.fn(),
        })
    })

    it('renders nothing when groups management is disabled', () => {
        mockUseDefaultGroup.mockReturnValue({ defaultGroupName: 'Viewers', loading: false })
        const { container } = renderWithContext({ isGroupsManagementEnabled: false })
        expect(container.innerHTML).toBe('')
    })

    it('renders nothing while loading', () => {
        mockUseDefaultGroup.mockReturnValue({ defaultGroupName: undefined, loading: true })
        const { container } = renderWithContext()
        expect(container.innerHTML).toBe('')
    })

    it('renders "none" text when no default group is set', () => {
        mockUseDefaultGroup.mockReturnValue({ defaultGroupName: undefined, loading: false })
        renderWithContext()
        expect(screen.getByText('security:default_group_none')).toBeInTheDocument()
    })

    it('renders group name tag when default group exists', () => {
        mockUseDefaultGroup.mockReturnValue({ defaultGroupName: 'Viewers', loading: false })
        renderWithContext()
        expect(screen.getByText('Viewers')).toBeInTheDocument()
    })

    it('renders warning tooltip when group is not found', () => {
        mockUseDefaultGroup.mockReturnValue({ defaultGroupName: 'Deleted', loading: false })
        mockUseGroups.mockReturnValue({
            groups: [],
            loading: false,
            error: null,
            reloadGroups: vi.fn(),
        })
        renderWithContext()
        expect(screen.getByText('Deleted')).toBeInTheDocument()
    })

    it('renders info icon with tooltip', () => {
        mockUseDefaultGroup.mockReturnValue({ defaultGroupName: 'Viewers', loading: false })
        renderWithContext()
        expect(screen.getByLabelText('security:default_group_info_aria')).toBeInTheDocument()
    })
})
