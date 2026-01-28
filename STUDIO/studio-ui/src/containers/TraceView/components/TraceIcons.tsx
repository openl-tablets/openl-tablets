import React from 'react'
import {
    TableOutlined,
    FunctionOutlined,
    CheckSquareOutlined,
    CheckCircleFilled,
    CloseCircleFilled,
    MinusCircleOutlined,
    FilterOutlined,
    FileExcelOutlined,
    NumberOutlined,
    CodeOutlined,
    ToolOutlined,
    AimOutlined,
    BranchesOutlined,
    QuestionCircleOutlined,
} from '@ant-design/icons'

/**
 * Icon mapping for trace node types.
 * Maps legacy background-image icons to Ant Design icons.
 */
const nodeTypeIcons: Record<string, React.ComponentType<{ style?: React.CSSProperties }>> = {
    decisiontable: TableOutlined,
    method: FunctionOutlined,
    rule: CheckSquareOutlined,
    condition: FilterOutlined,
    spreadsheet: FileExcelOutlined,
    spreadsheetCell: NumberOutlined,
    tbasic: CodeOutlined,
    tbasicOperation: ToolOutlined,
    cmatch: AimOutlined,
    overloadedMethodChoice: BranchesOutlined,
}

/**
 * Status modifiers for node icons based on extraClasses.
 */
interface NodeStatus {
    isResult: boolean
    isFail: boolean
    isNoResult: boolean
}

/**
 * Parse extraClasses string to determine node status.
 */
export const parseNodeStatus = (extraClasses: string): NodeStatus => ({
    isResult: extraClasses.includes('result'),
    isFail: extraClasses.includes('fail'),
    isNoResult: extraClasses.includes('no_result'),
})

/**
 * Get the appropriate icon for a trace node based on its type and status.
 * @param type Node type (e.g., 'method', 'rule', 'decisiontable')
 * @param extraClasses CSS classes indicating result status
 * @returns React element with appropriate icon
 */
export const getTraceIcon = (
    type: string,
    extraClasses: string
): React.ReactNode => {
    const status = parseNodeStatus(extraClasses)

    // For rule nodes, show status-specific icons
    if (type === 'rule') {
        if (status.isResult) {
            return <CheckCircleFilled style={{ color: '#52c41a' }} />
        }
        if (status.isFail) {
            return <CloseCircleFilled style={{ color: '#ff4d4f' }} />
        }
        if (status.isNoResult) {
            return <MinusCircleOutlined style={{ color: '#8c8c8c' }} />
        }
        return <CheckSquareOutlined style={{ color: '#1890ff' }} />
    }

    // Get icon component for node type
    const IconComponent = nodeTypeIcons[type] || QuestionCircleOutlined
    return <IconComponent style={{ color: '#595959' }} />
}

/**
 * Get CSS class names for a trace node based on its status.
 * @param extraClasses Original extraClasses from backend
 * @returns Space-separated class names for styling
 */
export const getNodeClassName = (extraClasses: string): string => {
    const classes: string[] = []
    const status = parseNodeStatus(extraClasses)

    if (status.isResult) {
        classes.push('trace-node-result')
    }
    if (status.isFail) {
        classes.push('trace-node-fail')
    }
    if (status.isNoResult) {
        classes.push('trace-node-no-result')
    }

    return classes.join(' ')
}

export default getTraceIcon
