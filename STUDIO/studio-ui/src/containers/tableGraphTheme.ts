import type { GlobalToken } from 'antd'
import { DISPATCHER_KIND } from './tableGraph'

/**
 * A token that holds a colour: a greyscale alias, or one shade of an Ant Design preset palette.
 *
 * Ant Design does not export the union, so it is derived from the tokens that carry a string.
 */
type HueToken = { [K in keyof GlobalToken]: GlobalToken[K] extends string ? K : never }[keyof GlobalToken]

/**
 * The hue each table kind is drawn in, named as a token rather than written out.
 *
 * A kind keeps its place in the spectrum — rules blue, spreadsheets purple, datatypes teal — while the
 * appearance decides how light or how deep that place is, so the graph reads the same on a dark page.
 */
const KIND_HUES: Record<string, HueToken> = {
    [DISPATCHER_KIND]: 'gold9',
    'Rules': 'blue6',
    'Smart Rules': 'geekblue6',
    'Spreadsheet': 'purple6',
    'Datatype': 'cyan6',
    'Vocabulary': 'cyan7',
    'Data': 'green6',
    'Test': 'orange6',
    'Run': 'lime6',
    'TBasic': 'magenta6',
    'Column Match': 'purple5',
    'Method': 'blue5',
    'Constants': 'gold6',
    'Conditions': 'volcano6',
    'Actions': 'red6',
    'Returns': 'geekblue5',
    'Environment': 'colorTextTertiary',
    'Properties': 'colorTextQuaternary',
}

/** The hue of a kind this version does not know — grey, so an unknown kind never borrows a meaning. */
const DEFAULT_HUE: HueToken = 'colorTextTertiary'

/** The colour a table of the given kind is drawn in, under the appearance the token describes. */
export const kindColor = (token: GlobalToken, kind?: string): string =>
    token[(kind && Object.hasOwn(KIND_HUES, kind) ? KIND_HUES[kind] : undefined) ?? DEFAULT_HUE]

/** The kinds this version paints, each as a Cytoscape rule. A kind outside the list keeps the node default. */
export const kindRules = (token: GlobalToken) => Object.keys(KIND_HUES).map(kind => ({
    selector: `node[kind = "${kind}"]`,
    style: { 'background-color': kindColor(token, kind) },
}))

/**
 * Every colour the dependency graph is drawn with, for the appearance in force.
 *
 * Cytoscape paints on a canvas and cannot read a CSS custom property, so the graph takes real colours from
 * the Ant Design token rather than through {@code LIST_PAGE_COLORS}. One accent is reserved for selection
 * and never names a table kind; red means "problem" only.
 */
export const graphPalette = (token: GlobalToken) => ({
    /** The ring around the table the reader picked. */
    select: token.orange6,
    /** The border of the ring, so the selection reads against a light fill as well. */
    selectBorder: token.orange9,
    /** A cycle, or a table nothing uses and that uses nothing. */
    problem: token.colorError,
    /** The generated table that selects one overloaded version. */
    dispatcher: token.gold5,
    /** Inheritance and field edges, the hue the Datatype kind already carries. */
    dataModel: token.cyan6,
    /** The frame around the data model of one project, and its title. */
    areaBg: token.cyan1,
    areaText: token.cyan7,
    /** The board the graph is placed on, and the blueprint dots on it. */
    canvas: token.colorBgLayout,
    dot: token.colorBorder,
    /** The thin rule around the board, the legend and the side panel. */
    hairline: token.colorBorderSecondary,
    /** A table label and the halo that keeps it readable on a pale fill. */
    label: token.colorWhite,
    labelHalo: token.colorBgMask,
    /** The border whose width says how widely a table is used. */
    weight: token.colorTextBase,
    /** The surface and the text of an entity box of the data model. */
    entityBg: token.colorBgContainer,
    entityText: token.colorText,
    /** A dependency line and its arrowhead. */
    edge: token.colorTextQuaternary,
    /** The floating legend panel. */
    panelBg: token.colorBgElevated,
    panelShadow: token.boxShadowSecondary,
    /** The pair of swatches that explain the border width in the legend. */
    weightThin: token.colorTextQuaternary,
    weightThick: token.colorTextSecondary,
})
