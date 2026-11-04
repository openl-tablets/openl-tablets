import React from 'react'
import { Tag, Tooltip } from 'antd'
import { WarningOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'

/** Marks a file a merge stops at as a conflict, for the user to resolve it. */
export const ConflictTag: React.FC = () => {
    const { t } = useTranslation()
    return (
        <Tooltip title={t('repository:browser.files.conflict_hint')}>
            <Tag color="error" data-testid="file-conflict" icon={<WarningOutlined />}>
                {t('repository:browser.files.conflict')}
            </Tag>
        </Tooltip>
    )
}
