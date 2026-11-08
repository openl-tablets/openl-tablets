import React from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Empty, Space } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import type { SharedMessageText } from '../../types/localization'
import { useSharedStyles } from '../projects/sharedStyles'

interface MissingKeyPageProps {
    /** The key a rule asks for, which no localization file of the project has. */
    messageKey: string
    /** The default text a project the project depends on gives the key, when one does. */
    owner: SharedMessageText | undefined
    /** Whether the reader may add the key: the project is written to and has a default localization file. */
    canAdd: boolean
    onAdd: () => void
    /** Opens the key in the project that gives it. */
    onOpenOwner: (projectId: string) => void
}

/**
 * The page of a key a rule asks for, which no localization file of the project has: the reader adds it, or, when a
 * project the project depends on gives it, opens it there or overrides it here.
 */
export const MissingKeyPage = ({ messageKey, owner, canAdd, onAdd, onOpenOwner }: MissingKeyPageProps) => {
    const { styles: shared } = useSharedStyles()
    const { t } = useTranslation('localization')
    const ownerId = owner?.projectId
    return (
        <Empty
            className={shared.workspaceCentered}
            data-testid="localization-key-missing"
            description={owner === undefined
                ? t('key_missing', { key: messageKey })
                : t('key_in_dependency', { key: messageKey, project: owner.project })}
        >
            <Space>
                {owner !== undefined && ownerId !== undefined && (
                    <Button data-testid="localization-key-owner" onClick={() => onOpenOwner(ownerId)} type="primary">
                        {t('open_in_project', { project: owner.project })}
                    </Button>
                )}
                {canAdd && (
                    <Button
                        data-testid="localization-key-add"
                        icon={<PlusOutlined />}
                        onClick={onAdd}
                        type={owner === undefined ? 'primary' : 'default'}
                    >
                        {owner === undefined ? t('add_key') : t('override_key')}
                    </Button>
                )}
            </Space>
        </Empty>
    )
}
