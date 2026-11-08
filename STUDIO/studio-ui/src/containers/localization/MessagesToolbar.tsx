import React from 'react'
import { useTranslation } from 'react-i18next'
import { DeleteOutlined, GlobalOutlined, PlusOutlined, RedoOutlined, SaveOutlined, UndoOutlined } from '@ant-design/icons'
import { useActionBandStyles } from '../modules/actionBand.styles'
import { BandAction } from '../modules/BandAction'

interface MessagesToolbarProps {
    /** Whether anything is written and not saved. */
    dirty: boolean
    saving: boolean
    /** Whether there is a step to take back, and one taken back to do again. */
    canUndo: boolean
    canRedo: boolean
    /** Whether a key can be added: the project has a default language to write it in. */
    canAddKey: boolean
    /** Whether a language can be added: the project has a localization file to start from. */
    canAddLanguage: boolean
    onSave: () => void
    onUndo: () => void
    onRedo: () => void
    onAddKey: () => void
    onAddLanguage: () => void
    /** Takes the key of the page out of every language; absent on a page of no key. */
    onRemoveKey?: (() => void) | undefined
}

/**
 * What is done to the localization files, in a band above the messages, the way the band above a table offers what
 * is done to the table: each action its picture and its name. The project's own actions stay in the header.
 *
 * <p>Undo takes back the last thing done to the messages, on any page, and Redo does it again, as the band over a
 * table does.
 */
export const MessagesToolbar = ({
    dirty,
    saving,
    canUndo,
    canRedo,
    canAddKey,
    canAddLanguage,
    onSave,
    onUndo,
    onRedo,
    onAddKey,
    onAddLanguage,
    onRemoveKey,
}: MessagesToolbarProps) => {
    const { t } = useTranslation('localization')
    const { styles } = useActionBandStyles()

    const action = (key: string, icon: React.ReactNode, onClick: () => void, enabled: boolean) => (
        <BandAction
            key={key}
            disabled={!enabled}
            icon={icon}
            label={t(key)}
            loading={key === 'save' && saving}
            onClick={onClick}
            testId={`localization-${key.replaceAll('_', '-')}`}
        />
    )

    return (
        <div className={styles.bar} data-testid="localization-toolbar">
            {action('save', <SaveOutlined />, onSave, dirty)}
            {action('undo', <UndoOutlined />, onUndo, canUndo && !saving)}
            {action('redo', <RedoOutlined />, onRedo, canRedo && !saving)}
            {action('add_key', <PlusOutlined />, onAddKey, canAddKey)}
            {action('add_language', <GlobalOutlined />, onAddLanguage, canAddLanguage)}
            {onRemoveKey !== undefined && action('remove_key', <DeleteOutlined />, onRemoveKey, true)}
        </div>
    )
}
