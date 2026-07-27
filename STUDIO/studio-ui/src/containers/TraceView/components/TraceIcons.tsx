import React from 'react'
import type { FrameKind } from 'types/trace'

/**
 * Hand-drawn SVG icons for the trace tree, recreating the table icons of the legacy trace
 * (originally GIFs under the RichFaces webapp's images/), so each rule kind is recognisable
 * at a glance. All icons are decorative — the row text carries the information.
 */
interface IconProps {
    /** Icon size in pixels. */
    size?: number | undefined
}

const svgProps = (size: number): React.SVGAttributes<SVGSVGElement> => ({
    width: size,
    height: size,
    viewBox: '0 0 16 16',
    'aria-hidden': true,
    focusable: 'false',
    style: { verticalAlign: 'middle', flex: '0 0 auto' },
})

// ruleset.gif — orange/yellow grid for decision tables
const RulesetIcon: React.FC<IconProps> = ({ size = 14 }) => (
    <svg {...svgProps(size)}>
        <rect fill="#ffd700" height="14" stroke="#c90" strokeWidth="1" width="14" x="1" y="1" />
        <line stroke="#c90" strokeWidth="1" x1="5" x2="5" y1="1" y2="15" />
        <line stroke="#c90" strokeWidth="1" x1="9" x2="9" y1="1" y2="15" />
        <line stroke="#c90" strokeWidth="1" x1="13" x2="13" y1="1" y2="15" />
        <line stroke="#c90" strokeWidth="1" x1="1" x2="15" y1="5" y2="5" />
        <line stroke="#c90" strokeWidth="1" x1="1" x2="15" y1="9" y2="9" />
        <line stroke="#c90" strokeWidth="1" x1="1" x2="15" y1="13" y2="13" />
        <rect fill="#f90" height="4" width="4" x="1" y="1" />
        <rect fill="#fc0" height="4" width="4" x="5" y="1" />
        <rect fill="#f90" height="4" width="4" x="9" y="1" />
    </svg>
)

// spreadsheet.gif — teal grid with a header row and column for spreadsheets
const SpreadsheetIcon: React.FC<IconProps> = ({ size = 14 }) => (
    <svg {...svgProps(size)}>
        <rect fill="#e0ffff" height="14" stroke="#008b8b" strokeWidth="1" width="14" x="1" y="1" />
        <line stroke="#008b8b" strokeWidth="1" x1="5" x2="5" y1="1" y2="15" />
        <line stroke="#008b8b" strokeWidth="1" x1="9" x2="9" y1="1" y2="15" />
        <line stroke="#008b8b" strokeWidth="1" x1="13" x2="13" y1="1" y2="15" />
        <line stroke="#008b8b" strokeWidth="1" x1="1" x2="15" y1="5" y2="5" />
        <line stroke="#008b8b" strokeWidth="1" x1="1" x2="15" y1="9" y2="9" />
        <line stroke="#008b8b" strokeWidth="1" x1="1" x2="15" y1="13" y2="13" />
        <rect fill="#20b2aa" height="4" width="14" x="1" y="1" />
        <rect fill="#5f9ea0" height="10" width="4" x="1" y="5" />
    </svg>
)

// value.gif — green filled circle for a spreadsheet cell
const ValueIcon: React.FC<IconProps> = ({ size = 14 }) => (
    <svg {...svgProps(size)}>
        <circle cx="8" cy="8" fill="#32cd32" r="5" stroke="#228b22" strokeWidth="1" />
    </svg>
)

// method.gif — italic "fx" for methods
const MethodIcon: React.FC<IconProps> = ({ size = 14 }) => (
    <svg {...svgProps(size)}>
        <text fill="#333" fontFamily="Times, serif" fontSize="11" fontStyle="italic" x="2" y="12">
            fx
        </text>
    </svg>
)

// test_ok.gif — checkbox with a checkmark for a decision-table rule
const RuleIcon: React.FC<IconProps> = ({ size = 14 }) => (
    <svg {...svgProps(size)}>
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

// cmatch.gif — blue grid for column match tables
const CMatchIcon: React.FC<IconProps> = ({ size = 14 }) => (
    <svg {...svgProps(size)}>
        <rect fill="#e6f3ff" height="14" stroke="#4169e1" strokeWidth="1" width="14" x="1" y="1" />
        <line stroke="#4169e1" strokeWidth="1" x1="5" x2="5" y1="1" y2="15" />
        <line stroke="#4169e1" strokeWidth="1" x1="9" x2="9" y1="1" y2="15" />
        <line stroke="#4169e1" strokeWidth="1" x1="13" x2="13" y1="1" y2="15" />
        <line stroke="#4169e1" strokeWidth="1" x1="1" x2="15" y1="5" y2="5" />
        <line stroke="#4169e1" strokeWidth="1" x1="1" x2="15" y1="9" y2="9" />
        <line stroke="#4169e1" strokeWidth="1" x1="1" x2="15" y1="13" y2="13" />
        <rect fill="#6495ed" height="4" width="14" x="1" y="1" />
    </svg>
)

// tbasic-operation.gif — green down arrow for an algorithm operation
const OperationIcon: React.FC<IconProps> = ({ size = 14 }) => (
    <svg {...svgProps(size)}>
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

const KIND_ICONS: Record<FrameKind, React.FC<IconProps> | null> = {
    decisionTable: RulesetIcon,
    spreadsheet: SpreadsheetIcon,
    method: MethodIcon,
    cmatch: CMatchIcon,
    tbasic: CMatchIcon,
    tbasicMethod: MethodIcon,
    // A step reference is not a table; its row keeps the link marker instead of a kind icon.
    stepRef: null,
}

/** The legacy table icon of a rule kind, shown before the table name. */
export const KindIcon: React.FC<{ kind: FrameKind } & IconProps> = ({ kind, size }) => {
    const Icon = KIND_ICONS[kind]
    return Icon ? <Icon size={size} /> : null
}

/**
 * The legacy icon of one step row, by the kind of the table the step belongs to: a rule row for a
 * decision table, an operation arrow for an algorithm, a green value circle for everything else.
 */
export const StepIcon: React.FC<{ kind?: FrameKind | undefined } & IconProps> = ({ kind, size }) => {
    if (kind === 'decisionTable') {
        return <RuleIcon size={size} />
    }
    if (kind === 'tbasic') {
        return <OperationIcon size={size} />
    }
    return <ValueIcon size={size} />
}
