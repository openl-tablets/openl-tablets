import { Outlet } from 'react-router'
import { Flex, theme } from 'antd'
import MainMenu from '../components/MainMenu'
import { GroupsProvider } from '../contexts'

export const AdministrationLayout = () => {
    const { token } = theme.useToken()
    return (
        <GroupsProvider>
            <Flex>
                <MainMenu />
                <div style={{ padding: token.paddingMD, width: '100%' }}>
                    <Outlet />
                </div>
            </Flex>
        </GroupsProvider>
    )
}
