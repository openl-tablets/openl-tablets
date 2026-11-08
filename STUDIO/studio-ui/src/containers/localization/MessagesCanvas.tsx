import React from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Empty, Skeleton } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { ClosedProject } from '../projects/ClosedProject'
import { useSharedStyles } from '../projects/sharedStyles'
import type { BundleLanguage, KeyRow } from './bundle'
import { KeyPanel, type KeyPanelProps } from './KeyPanel'
import { MessagesTable, type MessagesTableProps } from './MessagesTable'

interface MessagesCanvasProps {
    /** Whether the project is closed, so nothing of it is read. */
    closed: boolean
    /** Whether the closed project is being opened. */
    opening: boolean
    /** Opens the closed project, asking about its dependencies first. */
    onOpenProject: () => void
    /** Why the messages could not be read, or null. */
    failure: string | null
    /** The languages of the project, or null while they, or the modules of the project, are read. */
    languages: BundleLanguage[] | null
    canWrite: boolean
    /** Creates the default localization file of a project without any. */
    onCreateDefault: () => void
    /** The page of a key a rule asks for, which no file of the project has, or null. */
    missing: React.ReactNode
    /** The key on its own page, null while it is read, or undefined on the table of every key. */
    shownKey: KeyRow | null | undefined
    keyPanel: Omit<KeyPanelProps, 'row' | 'languages'>
    table: Omit<MessagesTableProps, 'languages'>
}

/**
 * What the screen of the messages shows under its band: the table of every key, or the page of one key, once they are
 * read; or why there is nothing to show yet — a closed project, a failed read, a project without localization files,
 * a key no file has.
 */
export const MessagesCanvas = ({
    closed,
    opening,
    onOpenProject,
    failure,
    languages,
    canWrite,
    onCreateDefault,
    missing,
    shownKey,
    keyPanel,
    table,
}: MessagesCanvasProps) => {
    const { t } = useTranslation('localization')
    const { styles: shared } = useSharedStyles()
    if (closed) {
        return (
            <ClosedProject
                description={t('closed')}
                onOpen={onOpenProject}
                opening={opening}
                testId="localization-closed"
            />
        )
    }
    if (failure !== null) {
        return <Alert showIcon description={failure} title={t('load_failed')} type="error" />
    }
    if (languages === null) {
        return <Skeleton active />
    }
    if (languages.length === 0) {
        return (
            <Empty className={shared.workspaceCentered} description={t('no_files')}>
                {canWrite && (
                    <Button data-testid="localization-create" icon={<PlusOutlined />} onClick={onCreateDefault} type="primary">
                        {t('create_default')}
                    </Button>
                )}
            </Empty>
        )
    }
    if (missing) {
        return missing
    }
    if (shownKey === undefined) {
        return <MessagesTable {...table} languages={languages} />
    }
    return shownKey === null ? <Skeleton active /> : <KeyPanel {...keyPanel} languages={languages} row={shownKey} />
}
