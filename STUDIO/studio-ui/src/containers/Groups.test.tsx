import React from 'react'
import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { GroupsContext } from 'contexts/GroupsContext'

vi.mock('services', async () => ({
    apiCall: vi.fn(),
}))

vi.mock('react-i18next', async () => {
    const t = (key: string) => key
    const i18n = { language: 'en' }
    return {
        useTranslation: () => ({ t, i18n }),
        Trans: ({ i18nKey }: { i18nKey: string }) => <span>{i18nKey}</span>,
    }
})

vi.mock('antd', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
    return {
        ...actual,
        Modal: {
            ...actual.Modal,
            confirm: vi.fn(),
        },
        notification: {
            ...actual.notification,
            success: vi.fn(),
            error: vi.fn(),
        },
    }
})

vi.mock('containers/EditUserGroupDetailsWithAccessRights', async () => ({
    EditUserGroupDetailsWithAccessRights: (props: any) => (
        <div data-open={props.isOpenFromParent} data-sid={props.sid} data-testid="edit-drawer" />
    ),
}))

vi.mock('components/DefaultGroupInfo', async () => ({
    DefaultGroupInfo: () => <div data-testid="default-group-info" />,
}))

import { Groups } from 'containers/Groups'

const mockGroups = [
    { name: 'Administrators', admin: true, id: 1, description: 'Full access', numberOfMembers: 2 },
    { name: 'Viewers', admin: false, id: 2, description: 'Read-only access', numberOfMembers: 5 },
]

const renderGroups = (overrides: any = {}) => {
    const groupsCtx = {
        groups: mockGroups,
        loading: false,
        error: null,
        reloadGroups: vi.fn(),
        ...overrides.groups,
    }
    return render(
        <GroupsContext.Provider value={groupsCtx}>
            <Groups />
        </GroupsContext.Provider>
    )
}

describe('Groups', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('renders the groups table with column headers', async () => {
        await act(async () => {
            renderGroups()
        })

        expect(screen.getByText('groups:table.name')).toBeInTheDocument()
        expect(screen.getByText('groups:table.description')).toBeInTheDocument()
        expect(screen.getByText('groups:table.members')).toBeInTheDocument()
        expect(screen.getByText('groups:table.actions')).toBeInTheDocument()
        expect(screen.getByText('Administrators')).toBeInTheDocument()
        expect(screen.getByText('Viewers')).toBeInTheDocument()
    })

    it('lays out the Default Group info and the search field in equal halves', async () => {
        await act(async () => {
            renderGroups()
        })

        expect(screen.getByTestId('default-group-info').closest('.ant-col')).toHaveClass('ant-col-12')
        expect(screen.getByTestId('groups-search-input').closest('.ant-col')).toHaveClass('ant-col-12')
    })

    it('filters groups by name', async () => {
        await act(async () => {
            renderGroups()
        })

        await userEvent.type(screen.getByTestId('groups-search-input'), 'view')

        expect(screen.getByText('Viewers')).toBeInTheDocument()
        expect(screen.queryByText('Administrators')).not.toBeInTheDocument()
    })

    it('clears the name filter', async () => {
        await act(async () => {
            renderGroups()
        })

        await userEvent.type(screen.getByTestId('groups-search-input'), 'nothing-matches')
        expect(screen.queryByText('Viewers')).not.toBeInTheDocument()

        await userEvent.clear(screen.getByTestId('groups-search-input'))
        expect(screen.getByText('Viewers')).toBeInTheDocument()
        expect(screen.getByText('Administrators')).toBeInTheDocument()
    })
})
