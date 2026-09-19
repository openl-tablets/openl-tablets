import React from 'react'
import { useHref, useNavigate } from 'react-router-dom'
import { Typography } from 'antd'
import { moduleRoute } from 'services/projectId'

const { Link, Text } = Typography

interface TableLinkProps {
    /** The project the table belongs to, as the server issued its id. */
    projectId: string
    /** The table the link leads to, as the APIs of the project report it. */
    tableId: string
    /** The module the table is read through; without it there is no screen to open. */
    module?: string | undefined
    /** How the link reads, when its subject passed or failed. */
    type?: 'success' | 'danger' | undefined
    'data-testid'?: string
    /** The screen has been left behind: the editor is going to the table. */
    onOpen: () => void
    children: React.ReactNode
}

/**
 * The name of a table, as a link to it in the editor.
 *
 * The editor opens a module and reads a table through it, so the link carries both — which the result it
 * belongs to already names, and no address has to be asked for. The address is a real one, so it can be
 * copied and opened in a window of its own; following it here keeps the reader in the same page.
 *
 * A table whose module is not named is written as its name alone: there is nothing to open it with.
 */
export const TableLink: React.FC<TableLinkProps> = ({
    projectId,
    tableId,
    module,
    type,
    onOpen,
    children,
    ...rest
}) => {
    const navigate = useNavigate()
    // The address the browser is given, which carries the path the application is deployed under — a route
    // alone would be opened at the root of the server and find nothing there.
    const to = moduleRoute(projectId, module ?? '', tableId)
    const href = useHref(to)

    if (!module) {
        return <Text data-testid={rest['data-testid']} {...(type && { type })}>{children}</Text>
    }

    return (
        <Link
            data-testid={rest['data-testid']}
            href={href}
            onClick={event => {
                event.preventDefault()
                onOpen()
                navigate(to)
            }}
            {...(type && { type })}
        >
            {children}
        </Link>
    )
}

export default TableLink
