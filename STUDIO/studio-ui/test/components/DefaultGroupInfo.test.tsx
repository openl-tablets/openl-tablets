import React from 'react'
import { render, screen } from '@testing-library/react'
import { SystemContext } from 'contexts/SystemContext'

jest.mock('services', () => ({
    apiCall: jest.fn(),
}))

jest.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string) => key,
        i18n: { language: 'en' },
    }),
    Trans: ({ i18nKey }: { i18nKey: string }) => <span>{i18nKey}</span>,
}))

jest.mock('hooks/useDefaultGroup', () => ({
    useDefaultGroup: jest.fn(),
}))

jest.mock('containers/groups/useGroups', () => ({
    useGroups: jest.fn(),
}))

import { useDefaultGroup } from 'hooks/useDefaultGroup'
import { useGroups } from 'containers/groups/useGroups'

const mockUseDefaultGroup = useDefaultGroup as jest.MockedFunction<typeof useDefaultGroup>
const mockUseGroups = useGroups as jest.MockedFunction<typeof useGroups>

// eslint-disable-next-line @typescript-eslint/no-require-imports
const { DefaultGroupInfo } = require('components/DefaultGroupInfo')

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
        jest.clearAllMocks()
        mockUseGroups.mockReturnValue({
            groups: [{ name: 'Viewers', admin: false, id: 1, description: '', numberOfMembers: 2 }],
            loading: false,
            error: null,
            reloadGroups: jest.fn(),
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
            reloadGroups: jest.fn(),
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
