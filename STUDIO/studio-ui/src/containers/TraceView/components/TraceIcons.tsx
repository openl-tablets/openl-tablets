import React from 'react'
import {
    TableOutlined,
    FunctionOutlined,
    CheckSquareOutlined,
    CheckCircleFilled,
    CloseCircleFilled,
    MinusCircleOutlined,
    FileExcelOutlined,
    NumberOutlined,
    CodeOutlined,
    ToolOutlined,
    AimOutlined,
    BranchesOutlined,
    QuestionCircleOutlined,
    CheckCircleOutlined,
    PlayCircleOutlined,
} from '@ant-design/icons'

/**
 * Icon mapping for trace node types.
 * Maps legacy background-image icons to Ant Design icons.
 *
 * Original CSS mappings from traceView.xhtml:
 * - .decisiontable -> ruleset.gif
 * - .rule -> test_ok.gif
 * - .cmatch, .wcmatch, .wcmScore -> cmatch.gif
 * - .cmResult -> cmatch-check.gif
 * - .cmMatch -> cmatch-match.gif
 * - .tbasic -> tbasic.gif
 * - .tbasicMethod, .method -> method.gif
 * - .tbasicOperation -> tbasic-operation.gif
 * - .spreadsheet -> spreadsheet.gif
 * - .spreadsheetCell -> value.gif
 * - .overloadedMethodChoice -> tableview.gif
 * - .dtRule.result, .dtRule.no_result -> test_success.png (green check)
 * - .dtRule.fail -> test_fail.png (red X)
 */
const nodeTypeIcons: Record<string, React.ComponentType<{ style?: React.CSSProperties }>> = {
    // Decision table
    decisiontable: TableOutlined,

    // Rule (generic)
    rule: CheckSquareOutlined,

    // Column match variants
    cmatch: AimOutlined,
    wcmatch: AimOutlined,
    wcmScore: AimOutlined,
    cmResult: CheckCircleOutlined,
    cmMatch: AimOutlined,

    // TBasic
    tbasic: CodeOutlined,
    tbasicMethod: FunctionOutlined,
    tbasicOperation: ToolOutlined,

    // Method
    method: FunctionOutlined,

    // Spreadsheet
    spreadsheet: FileExcelOutlined,
    spreadsheetCell: NumberOutlined,

    // Overloaded method
    overloadedMethodChoice: BranchesOutlined,

    // Decision table rule (dtRule) - handled specially with status
    dtRule: PlayCircleOutlined,
}

/**
 * Status modifiers for node icons based on type/extraClasses.
 */
interface NodeStatus {
    isResult: boolean
    isFail: boolean
    isNoResult: boolean
}

/**
 * Parse type/extraClasses string to extract classes.
 * The type field contains space-separated class names like "dtRule no_result" or "rule".
 */
const parseClasses = (typeOrClasses: string): string[] => {
    return typeOrClasses.split(/\s+/).filter(Boolean)
}

/**
 * Parse type/extraClasses string to determine node status.
 */
export const parseNodeStatus = (typeOrClasses: string): NodeStatus => {
    const classes = parseClasses(typeOrClasses)
    return {
        isResult: classes.includes('result'),
        isFail: classes.includes('fail'),
        isNoResult: classes.includes('no_result'),
    }
}

/**
 * Get the base type (first class that matches a known icon type).
 */
const getBaseType = (typeString: string): string | null => {
    const classes = parseClasses(typeString)

    // Check each class against known icon types
    for (const cls of classes) {
        if (nodeTypeIcons[cls]) {
            return cls
        }
    }

    // Return first class as fallback
    return classes[0] || null
}

/**
 * Get the appropriate icon for a trace node based on its type and status.
 *
 * @param type Node type string (may contain space-separated classes like "dtRule no_result")
 * @param extraClasses CSS classes indicating result status (same as type usually)
 * @returns React element with appropriate icon
 */
export const getTraceIcon = (
    type: string,
    extraClasses: string
): React.ReactNode => {
    const classes = parseClasses(type)
    const status = parseNodeStatus(extraClasses || type)
    const baseType = getBaseType(type)

    // Special handling for dtRule - show status-specific icons
    if (classes.includes('dtRule')) {
        if (status.isFail) {
            // .dtRule.fail -> test_fail.png (red X)
            return <CloseCircleFilled style={{ color: '#ff4d4f' }} />
        }
        // .dtRule.result, .dtRule.no_result -> test_success.png (green check)
        return <CheckCircleFilled style={{ color: '#52c41a' }} />
    }

    // Special handling for rule - show status-specific icons
    if (classes.includes('rule')) {
        if (status.isFail) {
            return <CloseCircleFilled style={{ color: '#ff4d4f' }} />
        }
        if (status.isNoResult) {
            return <MinusCircleOutlined style={{ color: '#8c8c8c' }} />
        }
        if (status.isResult) {
            return <CheckCircleFilled style={{ color: '#52c41a' }} />
        }
        // Default rule icon
        return <CheckSquareOutlined style={{ color: '#1890ff' }} />
    }

    // Get icon component for base type
    if (baseType && nodeTypeIcons[baseType]) {
        const IconComponent = nodeTypeIcons[baseType]
        return <IconComponent style={{ color: '#595959' }} />
    }

    // Fallback to question mark for unknown types
    return <QuestionCircleOutlined style={{ color: '#8c8c8c' }} />
}

/**
 * Get CSS class names for a trace node based on its status.
 *
 * Original CSS styling from traceView.xhtml:
 * - .result > .fancytree-title -> font-weight: bold
 * - .fail > .fancytree-title, .no_result > .fancytree-title -> font-style: italic
 *
 * @param extraClasses Original extraClasses/type from backend
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
