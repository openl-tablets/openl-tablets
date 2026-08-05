import React, { useMemo } from 'react'
import { Layout } from 'antd'
import { Header } from 'containers/Header'
import { DeployModal } from 'containers/DeployModal'
import { MergeModal } from 'containers/MergeModal'
import { DeleteBranchModal } from 'containers/DeleteBranchModal'
import { OpenProjectModal } from 'containers/projects/OpenProjectModal'
import { DeleteFileModal } from 'containers/DeleteFileModal'
import { DeleteProjectModal } from 'containers/DeleteProjectModal'
import { ConfirmModal } from 'containers/ConfirmModal'
import { TraceExecutionModal } from 'containers/TraceExecutionModal'
import { TableGraphModal } from 'containers/TableGraphModal'
import { UpdateModuleModal } from 'containers/UpdateModuleModal'
import { UpdateProjectModal } from 'containers/UpdateProjectModal'
import { CreateTableModal } from 'containers/CreateTableModal'
import { CopyTableModal } from 'containers/CopyTableModal'
import { JsfIslandHost } from 'components/JsfIslandHost'
import { LoadingOverlay } from 'components/LoadingOverlay'
import { Outlet, useLocation } from 'react-router-dom'
import { useAppStore } from 'store'
import Forbidden from 'pages/403'
import NotFound from 'pages/404'
import ServerError from 'pages/500'

const { Content: AntContent } = Layout

const layoutStyle: React.CSSProperties = {
    backgroundColor: '#fff',
}

export const DefaultLayout = () => {
    const { showForbidden, showNotFound, showServerError, setShowForbidden, setShowNotFound, setShowServerError } = useAppStore()
    const location = useLocation()

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
            <TraceExecutionModal />
            <TableGraphModal />
            <CreateTableModal />
            <CopyTableModal />
            <UpdateProjectModal />
            <UpdateModuleModal />
            <ConfirmModal />
            <LoadingOverlay />
            <JsfIslandHost />
        </Layout>
    )
}
