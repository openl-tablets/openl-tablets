import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { apiCall } from 'services'
import { RenderGroupCell } from './RenderGroupCell'
import { UserGroupType } from '../../constants'

vi.mock('services', () => ({ apiCall: vi.fn() }))

vi.mock('react-i18next', () => ({
    useTranslation: () => ({ t: (key: string) => key }),
}))

vi.mock('../EditUserGroupDetailsWithAccessRights', () => ({
    EditUserGroupDetailsWithAccessRights: ({ renderButton }: { renderButton: (open: () => void) => React.ReactNode }) => (
        <>{renderButton(vi.fn())}</>
    ),
}))

describe('RenderGroupCell', () => {
    it('collapses expanded unmatched groups and allows expanding them again', async () => {
        vi.mocked(apiCall).mockResolvedValue(['external-a', 'external-b'])
        const user = userEvent.setup()

        render(
            <RenderGroupCell
                groups={[]}
                notMatchedExternalGroupsCount={2}
                onCloseEditDrawer={vi.fn()}
                reloadGroups={vi.fn()}
                reloadUsers={vi.fn()}
                userGroups={[{ name: 'Employees', type: UserGroupType.External }]}
                username="jdoe"
            />
        )

        expect(screen.getByText('Employees')).toBeInTheDocument()
        expect(screen.getByText('+2')).toBeInTheDocument()
        expect(screen.queryByRole('button', { name: 'users:collapse_groups' })).not.toBeInTheDocument()

        await user.click(screen.getByText('+2'))

        await waitFor(() => expect(screen.getByText('external-a')).toBeInTheDocument())
        expect(screen.getByText('external-b')).toBeInTheDocument()
        expect(screen.queryByText('+2')).not.toBeInTheDocument()

        await user.click(screen.getByRole('button', { name: 'users:collapse_groups' }))

        expect(screen.getByText('+2')).toBeInTheDocument()
        expect(screen.queryByText('external-a')).not.toBeInTheDocument()
        expect(screen.queryByText('external-b')).not.toBeInTheDocument()

        await user.click(screen.getByText('+2'))
        await waitFor(() => expect(screen.getByText('external-a')).toBeInTheDocument())
        expect(apiCall).toHaveBeenCalledTimes(2)
        expect(apiCall).toHaveBeenCalledWith('/users/jdoe/groups/external?matched=false')
    })
})
