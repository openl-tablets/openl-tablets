import React from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Empty } from 'antd'
import { useSharedStyles } from './sharedStyles'

interface ClosedProjectProps {
    /** Why the screen shows nothing of the project. */
    description: string
    /** Whether the project is being opened. */
    opening: boolean
    /** Opens the project, asking about its dependencies first. */
    onOpen: () => void
    /** Names the placeholder, and its button with `-open` after it. */
    testId: string
}

/** What a screen of a closed project shows in place of the project: why it shows nothing, and Open. */
export const ClosedProject = ({ description, opening, onOpen, testId }: ClosedProjectProps) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    return (
        <Empty className={shared.workspaceCentered} data-testid={testId} description={description}>
            <Button data-testid={`${testId}-open`} loading={opening} onClick={onOpen} type="primary">
                {t('browser.open')}
            </Button>
        </Empty>
    )
}
