import React, { useEffect, useState } from 'react'
import { Typography } from 'antd'
import { openTableInEditor, tableUrl } from 'services/tableNavigation'

const { Link } = Typography

export interface TableLinkProps {
    /** The table the link leads to, as the APIs of the project report it. */
    tableId: string
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
 * The address is the server's to give, so it is asked for as the link is shown and the link then carries a
 * real address the browser follows. Until the address is there, and for a table that has none here, the click
 * asks the editor to go to the table instead.
 */
export const TableLink: React.FC<TableLinkProps> = ({ tableId, type, onOpen, children, ...rest }) => {
    const [url, setUrl] = useState<string | null>(null)

    useEffect(() => {
        let active = true
        // The address of the table before it is not this one's; the link waits for its own.
        setUrl(null)
        void tableUrl(tableId).then(address => {
            if (active) {
                setUrl(address)
            }
        })
        return () => {
            active = false
        }
    }, [tableId])

    return (
        <Link
            data-testid={rest['data-testid']}
            onClick={event => {
                // The address is known: the browser follows it, and the screen steps aside.
                if (url !== null) {
                    onOpen()
                    return
                }
                event.preventDefault()
                void openTableInEditor(tableId).then(opened => opened && onOpen())
            }}
            {...(url !== null && { href: url })}
            {...(type && { type })}
        >
            {children}
        </Link>
    )
}

export default TableLink
