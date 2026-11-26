import { colourDistance, opaque, readableOn } from './colorMath'
import type { SyntaxHues } from './themes'
import type { RawTableCellUsage } from 'types/tables'

/** What a piece of a cell text names that is drawn in a colour of its own: a rule, a datatype or a field. */
export type ColouredUsage = Extract<RawTableCellUsage['kind'], 'rule' | 'datatype' | 'field'>

/** The colour a piece of a cell text is drawn in by what it names. */
export type UsageInks = Readonly<Record<ColouredUsage, string>>

/** Whether a usage is of a kind drawn in a colour of its own. */
export const isColouredUsage = (kind: RawTableCellUsage['kind']): kind is ColouredUsage =>
    kind === 'rule' || kind === 'datatype' || kind === 'field'

/**
 * The colours of a table in the formatting of its workbook: the standard Blue of Excel for a rule, the dark green
 * Excel writes its Good style in for a datatype, and its dark grey of the text for a field. Each reads on the white
 * of the paper at 4.5:1.
 */
export const WORKBOOK_USAGE_INKS: UsageInks = { rule: '#0070c0', datatype: '#006100', field: '#595959' }

/** How far apart two colours stand, at the least, for a reader to tell them at a glance. */
const APART = 40

/** Whether two colours read as the same one. */
const alike = (one: string, other: string): boolean => colourDistance(one, other) < APART

/** The colours of a theme a kind falls back on. */
interface FallbackColours {
    colorText: string
    colorLink: string
    colorInfoText: string
    colorSuccessText: string
    colorWarningText: string
    colorErrorText: string
    colorTextTertiary: string
    colorTextSecondary: string
}

/**
 * The colours of a table in the Studio look: the hues the code editor of the theme writes the name of a method
 * called, of a type and of a member of an object in, moved as little as it takes to read on every ground.
 *
 * <p>Each kind keeps a colour of its own, apart from the text and from the kinds before it. A theme whose editor
 * names no hue for a kind, writes it in its plain text, or writes it as it writes a kind before it, gives that kind
 * the first colour of the theme that stands apart: a link, then info, for a rule; success, then warning, for a
 * datatype; the faintest grey that still reads, then the secondary text, for a field — the blue, green and grey of
 * the formatting of a workbook. Where none of them stands apart, as a grey does not from a grey text, the kind takes
 * the first hue the editor writes a value in that does, then a state of the theme.
 *
 * @param hues    the hues of the code editor of the theme
 * @param token   the colours of the theme
 * @param grounds the opaque colours the text is drawn on, the surface first
 */
export const usageInksOf = (hues: SyntaxHues, token: FallbackColours, grounds: readonly string[]): UsageInks => {
    const surface = grounds[0] ?? '#ffffff'
    const readable = (colour: string) => readableOn(opaque(colour, surface), grounds)
    const taken = [opaque(token.colorText, surface)]
    // The colours every kind turns to once its own stand too close to the colours taken.
    const shared = [
        hues.name,
        hues.number,
        hues.boolean,
        hues.string,
        token.colorInfoText,
        token.colorWarningText,
        token.colorErrorText,
    ]
    const pick = (...own: Array<string | undefined>): string => {
        const inks = [...own, ...shared].filter(colour => colour !== undefined).map(readable)
        const ink = inks.find(candidate => !taken.some(other => alike(candidate, other))) ?? readable(token.colorText)
        taken.push(ink)
        return ink
    }
    return {
        rule: pick(hues.call, token.colorLink, token.colorInfoText),
        datatype: pick(hues.type, token.colorSuccessText, token.colorWarningText),
        field: pick(hues.field, token.colorTextTertiary, token.colorTextSecondary),
    }
}
