import React from 'react'

/**
 * Custom SVG icons matching legacy RichFaces trace icons.
 * Original GIF icons located in: STUDIO/org.openl.rules.webstudio/webapp/images/
 *
 * All icons support accessibility via ariaLabel prop:
 * - When ariaLabel is provided: role="img" and aria-label for screen readers
 * - When ariaLabel is omitted: aria-hidden="true" for decorative icons
 */

/**
 * Common props for all trace icon components.
 */
interface TraceIconProps {
    /** Icon size in pixels (default: 16) */
    size?: number
    /** Accessible label for screen readers. If omitted, icon is treated as decorative. */
    ariaLabel?: string
}

/**
 * Get ARIA attributes based on whether ariaLabel is provided.
 * Decorative icons (no label) are hidden from screen readers.
 */
const getAriaProps = (ariaLabel?: string): React.SVGAttributes<SVGSVGElement> => {
    if (ariaLabel) {
        return {
            role: 'img',
            'aria-label': ariaLabel,
        }
    }
    return {
        'aria-hidden': true,
        focusable: 'false',
    }
}

// ruleset.gif - Orange/yellow 4x4 grid for decision tables
const RulesetIcon: React.FC<TraceIconProps> = ({ size = 16, ariaLabel }) => (
    <svg
        height={size}
        style={{ verticalAlign: 'middle' }}
        viewBox="0 0 16 16"
        width={size}
        {...getAriaProps(ariaLabel)}
    >
        <rect fill="#ffd700" height="14" stroke="#c90" strokeWidth="1" width="14" x="1" y="1" />
        <line stroke="#c90" strokeWidth="1" x1="5" x2="5" y1="1" y2="15" />
        <line stroke="#c90" strokeWidth="1" x1="9" x2="9" y1="1" y2="15" />
        <line stroke="#c90" strokeWidth="1" x1="13" x2="13" y1="1" y2="15" />
        <line stroke="#c90" strokeWidth="1" x1="1" x2="15" y1="5" y2="5" />
        <line stroke="#c90" strokeWidth="1" x1="1" x2="15" y1="9" y2="9" />
        <line stroke="#c90" strokeWidth="1" x1="1" x2="15" y1="13" y2="13" />
        {/* Orange header cells */}
        <rect fill="#f90" height="4" width="4" x="1" y="1" />
        <rect fill="#fc0" height="4" width="4" x="5" y="1" />
        <rect fill="#f90" height="4" width="4" x="9" y="1" />
    </svg>
)

// spreadsheet.gif - Teal/cyan grid for spreadsheets
const SpreadsheetIcon: React.FC<TraceIconProps> = ({ size = 16, ariaLabel }) => (
    <svg
        height={size}
        style={{ verticalAlign: 'middle' }}
        viewBox="0 0 16 16"
        width={size}
        {...getAriaProps(ariaLabel)}
    >
        <rect fill="#e0ffff" height="14" stroke="#008b8b" strokeWidth="1" width="14" x="1" y="1" />
        <line stroke="#008b8b" strokeWidth="1" x1="5" x2="5" y1="1" y2="15" />
        <line stroke="#008b8b" strokeWidth="1" x1="9" x2="9" y1="1" y2="15" />
        <line stroke="#008b8b" strokeWidth="1" x1="13" x2="13" y1="1" y2="15" />
        <line stroke="#008b8b" strokeWidth="1" x1="1" x2="15" y1="5" y2="5" />
        <line stroke="#008b8b" strokeWidth="1" x1="1" x2="15" y1="9" y2="9" />
        <line stroke="#008b8b" strokeWidth="1" x1="1" x2="15" y1="13" y2="13" />
        {/* Teal header row */}
        <rect fill="#20b2aa" height="4" width="14" x="1" y="1" />
        {/* Teal first column */}
        <rect fill="#5f9ea0" height="10" width="4" x="1" y="5" />
    </svg>
)

// value.gif - Green filled circle for spreadsheet cells
const ValueIcon: React.FC<TraceIconProps> = ({ size = 16, ariaLabel }) => (
    <svg
        height={size}
        style={{ verticalAlign: 'middle' }}
        viewBox="0 0 16 16"
        width={size}
        {...getAriaProps(ariaLabel)}
    >
        <circle cx="8" cy="8" fill="#32cd32" r="5" stroke="#228b22" strokeWidth="1" />
    </svg>
)

// method.gif - "fx" italic text for methods
const MethodIcon: React.FC<TraceIconProps> = ({ size = 16, ariaLabel }) => (
    <svg
        height={size}
        style={{ verticalAlign: 'middle' }}
        viewBox="0 0 16 16"
        width={size}
        {...getAriaProps(ariaLabel)}
    >
        <text
            fill="#333"
            fontFamily="Times, serif"
            fontSize="11"
            fontStyle="italic"
            x="2"
            y="12"
        >
            fx
        </text>
    </svg>
)

// test_ok.gif - Checkbox with checkmark for rules
const TestOkIcon: React.FC<TraceIconProps> = ({ size = 16, ariaLabel }) => (
    <svg
        height={size}
        style={{ verticalAlign: 'middle' }}
        viewBox="0 0 16 16"
        width={size}
        {...getAriaProps(ariaLabel)}
    >
        <rect fill="#fff" height="12" rx="1" stroke="#333" strokeWidth="1" width="12" x="2" y="2" />
        <path
            d="M4 8 L7 11 L12 5"
            fill="none"
            stroke="#228b22"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="2"
        />
    </svg>
)

// test_success.png - Green checkmark for successful results
const TestSuccessIcon: React.FC<TraceIconProps> = ({ size = 16, ariaLabel }) => (
    <svg
        height={size}
        style={{ verticalAlign: 'middle' }}
        viewBox="0 0 16 16"
        width={size}
        {...getAriaProps(ariaLabel)}
    >
        <path
            d="M2 8 L6 12 L14 4"
            fill="none"
            stroke="#32cd32"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="2.5"
        />
    </svg>
)

// test_fail.png - Red X in circle for failures
const TestFailIcon: React.FC<TraceIconProps> = ({ size = 16, ariaLabel }) => (
    <svg
        height={size}
        style={{ verticalAlign: 'middle' }}
        viewBox="0 0 16 16"
        width={size}
        {...getAriaProps(ariaLabel)}
    >
        <circle cx="8" cy="8" fill="#ff4444" r="7" stroke="#cc0000" strokeWidth="1" />
        <path
            d="M5 5 L11 11 M11 5 L5 11"
            fill="none"
            stroke="#fff"
            strokeLinecap="round"
            strokeWidth="2"
        />
    </svg>
)

// cmatch.gif - Blue grid for column match
const CMatchIcon: React.FC<TraceIconProps> = ({ size = 16, ariaLabel }) => (
    <svg
        height={size}
        style={{ verticalAlign: 'middle' }}
        viewBox="0 0 16 16"
        width={size}
        {...getAriaProps(ariaLabel)}
    >
        <rect fill="#e6f3ff" height="14" stroke="#4169e1" strokeWidth="1" width="14" x="1" y="1" />
        <line stroke="#4169e1" strokeWidth="1" x1="5" x2="5" y1="1" y2="15" />
        <line stroke="#4169e1" strokeWidth="1" x1="9" x2="9" y1="1" y2="15" />
        <line stroke="#4169e1" strokeWidth="1" x1="13" x2="13" y1="1" y2="15" />
        <line stroke="#4169e1" strokeWidth="1" x1="1" x2="15" y1="5" y2="5" />
        <line stroke="#4169e1" strokeWidth="1" x1="1" x2="15" y1="9" y2="9" />
        <line stroke="#4169e1" strokeWidth="1" x1="1" x2="15" y1="13" y2="13" />
        {/* Blue header */}
        <rect fill="#6495ed" height="4" width="14" x="1" y="1" />
    </svg>
)

// tbasic.gif - Blue grid for TBasic (similar to cmatch)
const TBasicIcon: React.FC<TraceIconProps> = ({ size = 16, ariaLabel }) => (
    <svg
        height={size}
        style={{ verticalAlign: 'middle' }}
        viewBox="0 0 16 16"
        width={size}
        {...getAriaProps(ariaLabel)}
    >
        <rect fill="#e6f3ff" height="14" stroke="#4169e1" strokeWidth="1" width="14" x="1" y="1" />
        <line stroke="#4169e1" strokeWidth="1" x1="5" x2="5" y1="1" y2="15" />
        <line stroke="#4169e1" strokeWidth="1" x1="9" x2="9" y1="1" y2="15" />
        <line stroke="#4169e1" strokeWidth="1" x1="13" x2="13" y1="1" y2="15" />
        <line stroke="#4169e1" strokeWidth="1" x1="1" x2="15" y1="5" y2="5" />
        <line stroke="#4169e1" strokeWidth="1" x1="1" x2="15" y1="9" y2="9" />
        <line stroke="#4169e1" strokeWidth="1" x1="1" x2="15" y1="13" y2="13" />
        {/* Blue header */}
        <rect fill="#6495ed" height="4" width="14" x="1" y="1" />
    </svg>
)

// tbasic-operation.gif - Green down arrow for TBasic operations
const TBasicOperationIcon: React.FC<TraceIconProps> = ({ size = 16, ariaLabel }) => (
    <svg
        height={size}
        style={{ verticalAlign: 'middle' }}
        viewBox="0 0 16 16"
        width={size}
        {...getAriaProps(ariaLabel)}
    >
        <path
            d="M8 2 L8 11 M4 8 L8 12 L12 8"
            fill="none"
            stroke="#228b22"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="2"
        />
    </svg>
)

// tableview.gif - Teal horizontal lines for overloaded method choice
const TableViewIcon: React.FC<TraceIconProps> = ({ size = 16, ariaLabel }) => (
    <svg
        height={size}
        style={{ verticalAlign: 'middle' }}
        viewBox="0 0 16 16"
        width={size}
        {...getAriaProps(ariaLabel)}
    >
        <rect fill="#f0ffff" height="14" stroke="#008b8b" strokeWidth="1" width="14" x="1" y="1" />
        <line stroke="#20b2aa" strokeWidth="2" x1="3" x2="13" y1="4" y2="4" />
        <line stroke="#20b2aa" strokeWidth="2" x1="3" x2="13" y1="8" y2="8" />
        <line stroke="#20b2aa" strokeWidth="2" x1="3" x2="13" y1="12" y2="12" />
    </svg>
)

// Gray minus circle for no result
const NoResultIcon: React.FC<TraceIconProps> = ({ size = 16, ariaLabel }) => (
    <svg
        height={size}
        style={{ verticalAlign: 'middle' }}
        viewBox="0 0 16 16"
        width={size}
        {...getAriaProps(ariaLabel)}
    >
        <circle cx="8" cy="8" fill="#d9d9d9" r="6" stroke="#999" strokeWidth="1" />
        <line stroke="#666" strokeLinecap="round" strokeWidth="2" x1="4" x2="12" y1="8" y2="8" />
    </svg>
)

// Unknown/default icon - question mark
const UnknownIcon: React.FC<TraceIconProps> = ({ size = 16, ariaLabel }) => (
    <svg
        height={size}
        style={{ verticalAlign: 'middle' }}
        viewBox="0 0 16 16"
        width={size}
        {...getAriaProps(ariaLabel)}
    >
        <circle cx="8" cy="8" fill="#f5f5f5" r="6" stroke="#999" strokeWidth="1" />
        <text fill="#666" fontSize="9" textAnchor="middle" x="8" y="11">?</text>
    </svg>
)

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
 * Get the appropriate icon for a trace node based on its type and status.
 *
 * Icon mapping from legacy traceView.xhtml:
 * - .decisiontable -> ruleset.gif (orange grid)
 * - .rule -> test_ok.gif (checkbox with check)
 * - .cmatch, .wcmatch, .wcmScore -> cmatch.gif (blue grid)
 * - .tbasic -> tbasic.gif (blue grid)
 * - .tbasicMethod, .method -> method.gif (fx symbol)
 * - .tbasicOperation -> tbasic-operation.gif (green arrow)
 * - .spreadsheet -> spreadsheet.gif (teal grid)
 * - .spreadsheetCell -> value.gif (green circle)
 * - .overloadedMethodChoice -> tableview.gif (horizontal lines)
 * - .dtRule.result, .dtRule.no_result -> test_success.png (green check)
 * - .dtRule.fail -> test_fail.png (red X)
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

    // Special handling for dtRule - show status-specific icons
    if (classes.includes('dtRule')) {
        if (status.isFail) {
            return <TestFailIcon />
        }
        // .dtRule.result, .dtRule.no_result -> test_success.png (green check)
        return <TestSuccessIcon />
    }

    // Special handling for rule - show status-specific icons
    if (classes.includes('rule')) {
        if (status.isFail) {
            return <TestFailIcon />
        }
        if (status.isNoResult) {
            return <NoResultIcon />
        }
        if (status.isResult) {
            return <TestSuccessIcon />
        }
        // Default rule icon - checkbox with checkmark
        return <TestOkIcon />
    }

    // Decision table
    if (classes.includes('decisiontable')) {
        return <RulesetIcon />
    }

    // Column match variants
    if (classes.includes('cmatch') || classes.includes('wcmatch') || classes.includes('wcmScore')) {
        return <CMatchIcon />
    }
    if (classes.includes('cmResult') || classes.includes('cmMatch')) {
        return <CMatchIcon />
    }

    // TBasic
    if (classes.includes('tbasic')) {
        return <TBasicIcon />
    }
    if (classes.includes('tbasicOperation')) {
        return <TBasicOperationIcon />
    }

    // Method (including tbasicMethod)
    if (classes.includes('method') || classes.includes('tbasicMethod')) {
        return <MethodIcon />
    }

    // Spreadsheet
    if (classes.includes('spreadsheet')) {
        return <SpreadsheetIcon />
    }
    if (classes.includes('spreadsheetCell')) {
        return <ValueIcon />
    }

    // Overloaded method choice
    if (classes.includes('overloadedMethodChoice')) {
        return <TableViewIcon />
    }

    // Fallback to unknown icon
    return <UnknownIcon />
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
export const getNodeClassName = (extraClasses: string, hasError?: boolean): string => {
    const classes: string[] = []
    const status = parseNodeStatus(extraClasses)

    if (hasError) {
        classes.push('trace-node-error')
    }
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
