import React, { useMemo } from 'react'
import { Layout, theme } from 'antd'
import { Header } from 'containers/Header'
import { DeployModal } from 'containers/DeployModal'
import { MergeModal } from 'containers/MergeModal'
import { DeleteBranchModal } from 'containers/DeleteBranchModal'
import { OpenProjectModal } from 'containers/projects/OpenProjectModal'
import { DeleteFileModal } from 'containers/DeleteFileModal'
import { DeleteProjectModal } from 'containers/DeleteProjectModal'
import { ConfirmModal } from 'containers/ConfirmModal'
import { BenchmarkLaunchHost, RunLaunchHost, TestsLaunchHost, TraceLaunchHost } from 'containers/TableInput'
import { TableGraphModal } from 'containers/TableGraphModal'
import { UpdateModuleModal } from 'containers/UpdateModuleModal'
import { UpdateProjectModal } from 'containers/UpdateProjectModal'
import { CreateTableModal } from 'containers/CreateTableModal'
import { CopyTableModal } from 'containers/CopyTableModal'
import { CopyProjectModalHost } from 'containers/CopyProjectModalHost'
import { SaveProjectModalHost } from 'containers/SaveProjectModalHost'
import { ExportProjectModalHost } from 'containers/ExportProjectModalHost'
import { LoadingOverlay } from 'components/LoadingOverlay'
import { Outlet, useLocation } from 'react-router'
import { useAppStore } from 'store'
import Forbidden from 'pages/403'
import { useWorkspaceChanges } from 'hooks/useWorkspaceChanges'
import { forgetWorkspaceReads } from 'services/keptReads'
import NotFound from 'pages/404'
import ServerError from 'pages/500'

const { Content: AntContent } = Layout

export const DefaultLayout = () => {
    const { showForbidden, showNotFound, showServerError, setShowForbidden, setShowNotFound, setShowServerError } = useAppStore()
    const location = useLocation()
    const { token } = theme.useToken()
    const layoutStyle: React.CSSProperties = { backgroundColor: token.colorBgContainer }
    // A change the server tells of, made in another session or by another user, drops what was kept for the next
    // screen whichever screen is open, so a screen the reader comes back to reads afresh.
    useWorkspaceChanges(forgetWorkspaceReads)

    React.useEffect(() => {
        if (showForbidden || showNotFound || showServerError) {
            setShowForbidden(false)
            setShowNotFound(false)
            setShowServerError(false)
        }
        // Only runs on location change
    }, [location])

    const content = useMemo(() => {
        if (showForbidden) {
            return <Forbidden />
        }
        if (showNotFound) {
            return <NotFound />
        }
        if (showServerError) {
            return <ServerError />
        }
        return <Outlet />
    }, [showForbidden, showNotFound, showServerError])

    return (
        <Layout style={layoutStyle}>
            <Header />
            <AntContent>
                {content}
            </AntContent>
            <DeployModal />
            <MergeModal />
            <DeleteBranchModal />
            <OpenProjectModal />
            <DeleteFileModal />
            <DeleteProjectModal />
            <TraceLaunchHost />
            <RunLaunchHost />
            <TestsLaunchHost />
            <BenchmarkLaunchHost />
            <TableGraphModal />
            <CreateTableModal />
            <CopyTableModal />
            <UpdateProjectModal />
            <UpdateModuleModal />
            <CopyProjectModalHost />
            <SaveProjectModalHost />
            <ExportProjectModalHost />
            <ConfirmModal />
            <LoadingOverlay />
        </Layout>
    )
}
