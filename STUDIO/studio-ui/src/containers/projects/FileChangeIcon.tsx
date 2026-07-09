import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import { Tooltip } from 'antd'
import { createStyles } from 'antd-style'
import type { ProjectFileChangeType } from '../../services/projectStatus'

const useStyles = createStyles(({ css, token }) => ({
    icon: css`
        display: inline-flex;
        flex: none;
        align-items: center;
        justify-content: center;
        width: 16px;
        height: 16px;
        border-radius: ${token.borderRadiusSM}px;
        font-size: 11px;
        line-height: 1;
    `,
    added: css`
        color: ${token.colorSuccess};
        background: ${token.colorSuccessBg};
    `,
    modified: css`
        color: ${token.colorWarning};
        background: ${token.colorWarningBg};
    `,
    deleted: css`
        color: ${token.colorError};
        background: ${token.colorErrorBg};
    `,
}))

interface FileChangeIconProps {
    type: ProjectFileChangeType
    title: string
    testId?: string
}

const iconByType = {
    added: PlusOutlined,
    modified: EditOutlined,
    deleted: DeleteOutlined,
}

export const FileChangeIcon = ({ type, title, testId }: FileChangeIconProps) => {
    const { styles, cx } = useStyles()
    const Icon = iconByType[type]

    return (
        <Tooltip title={title}>
            <span aria-label={title} className={cx(styles.icon, styles[type])} data-testid={testId}>
                <Icon />
            </span>
        </Tooltip>
    )
}
