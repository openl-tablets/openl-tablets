import React from 'react'
import type { FrameKind } from 'types/trace'

/**
 * Hand-drawn SVG icons for the trace tree, recreating the table icons of the legacy trace
 * (originally GIFs under the RichFaces webapp's images/), so each rule kind is recognisable
 * at a glance. All icons are decorative — the row text carries the information.
 *
 * Each icon is built once as a static element and reused by every row, so re-rendering a large
 * tree never rebuilds identical SVG structures.
 */

/** Table icons sit next to a table name; step icons are a notch smaller, next to a step label. */
const KIND_SIZE = 14
const STEP_SIZE = 12

const ICON_STYLE: React.CSSProperties = { verticalAlign: 'middle', flex: '0 0 auto' }

const icon = (size: number, children: React.ReactNode): React.ReactElement => (
    <svg aria-hidden focusable="false" height={size} style={ICON_STYLE} viewBox="0 0 16 16" width={size}>
        {children}
    </svg>
)

/** The 4x4 grid skeleton shared by the table icons: the outer rect plus the cell lines. */
const grid = (fill: string, stroke: string): React.ReactElement => (
    <>
        <rect fill={fill} height="14" stroke={stroke} strokeWidth="1" width="14" x="1" y="1" />
        {[5, 9, 13].map(p => (
            <React.Fragment key={p}>
                <line stroke={stroke} strokeWidth="1" x1={p} x2={p} y1="1" y2="15" />
                <line stroke={stroke} strokeWidth="1" x1="1" x2="15" y1={p} y2={p} />
            </React.Fragment>
        ))}
    </>
)

// ruleset.gif — orange/yellow grid with header cells for decision tables
const rulesetIcon = icon(KIND_SIZE, (
    <>
        {grid('#ffd700', '#c90')}
        <rect fill="#f90" height="4" width="4" x="1" y="1" />
        <rect fill="#fc0" height="4" width="4" x="5" y="1" />
        <rect fill="#f90" height="4" width="4" x="9" y="1" />
    </>
))

// spreadsheet.gif — teal grid with a header row and column for spreadsheets
const spreadsheetIcon = icon(KIND_SIZE, (
    <>
        {grid('#e0ffff', '#008b8b')}
        <rect fill="#20b2aa" height="4" width="14" x="1" y="1" />
        <rect fill="#5f9ea0" height="10" width="4" x="1" y="5" />
    </>
))

// cmatch.gif — blue grid for column match and algorithm tables
const cmatchIcon = icon(KIND_SIZE, (
    <>
        {grid('#e6f3ff', '#4169e1')}
        <rect fill="#6495ed" height="4" width="14" x="1" y="1" />
    </>
))

// method.gif — italic "fx" for methods
const methodIcon = icon(KIND_SIZE, (
    <text fill="#333" fontFamily="Times, serif" fontSize="11" fontStyle="italic" x="2" y="12">
        fx
    </text>
))

// value.gif — green filled circle for a spreadsheet cell
const valueIcon = icon(STEP_SIZE, (
    <circle cx="8" cy="8" fill="#32cd32" r="5" stroke="#228b22" strokeWidth="1" />
))

// test_ok.gif — checkbox with a checkmark for a decision-table rule
const ruleIcon = icon(STEP_SIZE, (
    <>
        <rect fill="#fff" height="12" rx="1" stroke="#333" strokeWidth="1" width="12" x="2" y="2" />
        <path
            d="M4 8 L7 11 L12 5"
            fill="none"
            stroke="#228b22"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="2"
        />
    </>
))

// tbasic-operation.gif — green down arrow for an algorithm operation
const operationIcon = icon(STEP_SIZE, (
    <path
        d="M8 2 L8 11 M4 8 L8 12 L12 8"
        fill="none"
        stroke="#228b22"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
    />
))

const KIND_ICONS: Record<FrameKind, React.ReactElement | null> = {
    decisionTable: rulesetIcon,
    spreadsheet: spreadsheetIcon,
    method: methodIcon,
    cmatch: cmatchIcon,
    tbasic: cmatchIcon,
    tbasicMethod: methodIcon,
    // A step reference is not a table; its row keeps the link marker instead of a kind icon.
    stepRef: null,
}

/** The legacy table icon of a rule kind, shown before the table name. */
export const kindIcon = (kind: FrameKind): React.ReactElement | null => KIND_ICONS[kind]

/**
 * The legacy icon of one step row, by the kind of the table the step belongs to: a rule row for a
 * decision table, an operation arrow for an algorithm, a green value circle for everything else.
 */
export const stepIcon = (kind: FrameKind | undefined): React.ReactElement => {
    if (kind === 'decisionTable') {
        return ruleIcon
    }
    return kind === 'tbasic' ? operationIcon : valueIcon
}
