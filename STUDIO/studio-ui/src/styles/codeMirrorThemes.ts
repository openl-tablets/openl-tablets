import { defaultHighlightStyle, HighlightStyle } from '@codemirror/language'
import { oneDarkHighlightStyle } from '@codemirror/theme-one-dark'
import { tags as t } from '@lezer/highlight'
import { createTheme, type CreateThemeOptions } from '@uiw/codemirror-themes'
import type { Extension } from '@uiw/react-codemirror'
import {
    basicDark,
    basicDarkStyle,
    basicLight,
    basicLightStyle,
    defaultSettingsBasicDark,
    defaultSettingsBasicLight,
} from '@uiw/codemirror-theme-basic'
import { defaultSettingsDracula, dracula, draculaDarkStyle } from '@uiw/codemirror-theme-dracula'
import {
    defaultSettingsDuotoneDark,
    defaultSettingsDuotoneLight,
    douToneLightStyle,
    duotoneDark,
    duotoneDarkStyle,
    duotoneLight,
} from '@uiw/codemirror-theme-duotone'
import { defaultSettingsEclipse, eclipseLightStyle } from '@uiw/codemirror-theme-eclipse'
import {
    defaultSettingsGithubDark,
    defaultSettingsGithubLight,
    githubDark,
    githubDarkStyle,
    githubLight,
    githubLightStyle,
} from '@uiw/codemirror-theme-github'
import {
    defaultSettingsGruvboxDark,
    defaultSettingsGruvboxLight,
    gruvboxDark,
    gruvboxDarkStyle,
    gruvboxLight,
} from '@uiw/codemirror-theme-gruvbox-dark'
import { defaultSettingsKimbie, kimbie, kimbieDarkStyle } from '@uiw/codemirror-theme-kimbie'
import {
    defaultSettingsMaterialDark,
    defaultSettingsMaterialLight,
    materialDark,
    materialDarkStyle,
    materialLight,
    materialLightStyle,
} from '@uiw/codemirror-theme-material'
import { defaultSettingsMonokai, monokai, monokaiDarkStyle } from '@uiw/codemirror-theme-monokai'
import { defaultSettingsNoctisLilac, noctisLilac, noctisLilacLightStyle } from '@uiw/codemirror-theme-noctis-lilac'
import { defaultSettingsNord, nord, nordDarkStyle } from '@uiw/codemirror-theme-nord'
import { defaultSettingsQuietlight, quietlight, quietlightStyle } from '@uiw/codemirror-theme-quietlight'
import {
    defaultSettingsSolarizedDark,
    defaultSettingsSolarizedLight,
    solarizedDark,
    solarizedDarkStyle,
    solarizedLight,
    solarizedLightStyle,
} from '@uiw/codemirror-theme-solarized'
import { defaultSettingsTokyoNight, tokyoNight, tokyoNightStyle } from '@uiw/codemirror-theme-tokyo-night'
import {
    defaultSettingsTokyoNightDay,
    tokyoNightDay,
    tokyoNightDayStyle,
} from '@uiw/codemirror-theme-tokyo-night-day'
import {
    defaultSettingsVscodeDark,
    defaultSettingsVscodeLight,
    vscodeDark,
    vscodeDarkStyle,
    vscodeLight,
    vscodeLightStyle,
} from '@uiw/codemirror-theme-vscode'
import {
    defaultSettingsXcodeDark,
    defaultSettingsXcodeLight,
    xcodeDark,
    xcodeDarkStyle,
    xcodeLight,
    xcodeLightStyle,
} from '@uiw/codemirror-theme-xcode'
import { cached } from '../utils/cached'
import type { Appearance, ThemeName } from './themes'

/** What a scheme is made of: the editor's own colours and the styles of the kinds of code. */
type SchemeColors = Pick<CreateThemeOptions, 'settings' | 'styles'>

/** The colour scheme of a code editor: the theme the editor is drawn in, and the colours it is made of. */
interface EditorScheme extends SchemeColors {
    extension: Extension
}

/** A scheme no package ships as it is wanted, with its theme built here from its colours. */
const drawn = (appearance: Appearance, colors: SchemeColors): EditorScheme =>
    ({ ...colors, extension: createTheme({ theme: appearance, ...colors }) })

/**
 * Alucard, the light counterpart of Dracula, in the colours of the Dracula specification.
 *
 * No package ships it, so it is drawn here, each kind of code in the hue Dracula's own scheme gives it.
 */
const ALUCARD: EditorScheme = drawn('light', {
    settings: {
        background: '#fffbeb',
        foreground: '#1f1f1f',
        caret: '#1f1f1f',
        selection: '#cfcfde',
        selectionMatch: '#cfcfde80',
        gutterBackground: '#fffbeb',
        gutterForeground: '#6c664b',
        gutterBorder: 'transparent',
        lineHighlight: '#6c664b1a',
    },
    styles: [
        { tag: t.comment, color: '#6c664b' },
        { tag: t.string, color: '#846e15' },
        { tag: t.atom, color: '#644ac9' },
        { tag: t.meta, color: '#1f1f1f' },
        { tag: [t.keyword, t.operator, t.tagName], color: '#a3144d' },
        { tag: [t.function(t.propertyName), t.propertyName], color: '#036a96' },
        {
            tag: [t.definition(t.variableName), t.function(t.variableName), t.className, t.attributeName],
            color: '#14710a',
        },
    ],
})

/**
 * The styles the Gruvbox package draws its light scheme with. The package builds the scheme from them without
 * exporting them, so they are repeated here, and the code samples are highlighted with the same styles as the
 * editor.
 */
const GRUVBOX_LIGHT_STYLES: EditorScheme['styles'] = [
    { tag: t.keyword, color: '#9d0006' },
    { tag: [t.name, t.deleted, t.character, t.propertyName, t.macroName], color: '#427b58' },
    { tag: [t.variableName], color: '#076678' },
    { tag: [t.function(t.variableName)], color: '#79740e', fontStyle: 'bold' },
    { tag: [t.labelName], color: '#3c3836' },
    { tag: [t.color, t.constant(t.name), t.standard(t.name)], color: '#8f3f71' },
    { tag: [t.definition(t.name), t.separator], color: '#3c3836' },
    { tag: [t.brace], color: '#3c3836' },
    { tag: [t.annotation], color: '#9d0006' },
    { tag: [t.number, t.changed, t.annotation, t.modifier, t.self, t.namespace], color: '#8f3f71' },
    { tag: [t.typeName, t.className], color: '#b57614' },
    { tag: [t.operator, t.operatorKeyword], color: '#9d0006' },
    { tag: [t.tagName], color: '#427b58', fontStyle: 'bold' },
    { tag: [t.squareBracket], color: '#af3a03' },
    { tag: [t.angleBracket], color: '#076678' },
    { tag: [t.attributeName], color: '#427b58' },
    { tag: [t.regexp], color: '#427b58' },
    { tag: [t.quote], color: '#928374' },
    { tag: [t.string], color: '#3c3836' },
    { tag: t.link, color: '#7c6f64', textDecoration: 'underline', textUnderlinePosition: 'under' },
    { tag: [t.url, t.escape, t.special(t.string)], color: '#8f3f71' },
    { tag: [t.meta], color: '#b57614' },
    { tag: [t.comment], color: '#928374', fontStyle: 'italic' },
    { tag: t.strong, fontWeight: 'bold', color: '#af3a03' },
    { tag: t.emphasis, fontStyle: 'italic', color: '#79740e' },
    { tag: t.strikethrough, textDecoration: 'line-through' },
    { tag: t.heading, fontWeight: 'bold', color: '#79740e' },
    { tag: [t.heading1, t.heading2], fontWeight: 'bold', color: '#79740e' },
    { tag: [t.heading3, t.heading4], fontWeight: 'bold', color: '#b57614' },
    { tag: [t.heading5, t.heading6], color: '#b57614' },
    { tag: [t.atom, t.bool, t.special(t.variableName)], color: '#8f3f71' },
    { tag: [t.processingInstruction, t.inserted], color: '#076678' },
    { tag: [t.contentSeparator], color: '#9d0006' },
    { tag: t.invalid, color: '#af3a03', borderBottom: '1px dotted #9d0006' },
]

/**
 * The schemes of the themes drawn from a code editor: the theme each package builds, with the colours it builds
 * it from.
 */
export const SCHEMES: Record<Exclude<ThemeName, 'standard'>, Record<Appearance, EditorScheme>> = {
    dracula: {
        light: ALUCARD,
        dark: { extension: dracula, settings: defaultSettingsDracula, styles: draculaDarkStyle },
    },
    solarized: {
        light: { extension: solarizedLight, settings: defaultSettingsSolarizedLight, styles: solarizedLightStyle },
        dark: { extension: solarizedDark, settings: defaultSettingsSolarizedDark, styles: solarizedDarkStyle },
    },
    xcode: {
        light: { extension: xcodeLight, settings: defaultSettingsXcodeLight, styles: xcodeLightStyle },
        dark: { extension: xcodeDark, settings: defaultSettingsXcodeDark, styles: xcodeDarkStyle },
    },
    vscode: {
        light: { extension: vscodeLight, settings: defaultSettingsVscodeLight, styles: vscodeLightStyle },
        dark: { extension: vscodeDark, settings: defaultSettingsVscodeDark, styles: vscodeDarkStyle },
    },
    gruvbox: {
        light: { extension: gruvboxLight, settings: defaultSettingsGruvboxLight, styles: GRUVBOX_LIGHT_STYLES },
        dark: { extension: gruvboxDark, settings: defaultSettingsGruvboxDark, styles: gruvboxDarkStyle },
    },
    material: {
        light: { extension: materialLight, settings: defaultSettingsMaterialLight, styles: materialLightStyle },
        dark: { extension: materialDark, settings: defaultSettingsMaterialDark, styles: materialDarkStyle },
    },
    github: {
        light: { extension: githubLight, settings: defaultSettingsGithubLight, styles: githubLightStyle },
        dark: { extension: githubDark, settings: defaultSettingsGithubDark, styles: githubDarkStyle },
    },
    duotone: {
        light: { extension: duotoneLight, settings: defaultSettingsDuotoneLight, styles: douToneLightStyle },
        dark: { extension: duotoneDark, settings: defaultSettingsDuotoneDark, styles: duotoneDarkStyle },
    },
    basic: {
        light: { extension: basicLight, settings: defaultSettingsBasicLight, styles: basicLightStyle },
        dark: { extension: basicDark, settings: defaultSettingsBasicDark, styles: basicDarkStyle },
    },
    tokyoNight: {
        light: { extension: tokyoNightDay, settings: defaultSettingsTokyoNightDay, styles: tokyoNightDayStyle },
        dark: { extension: tokyoNight, settings: defaultSettingsTokyoNight, styles: tokyoNightStyle },
    },
    monokai: {
        light: { extension: noctisLilac, settings: defaultSettingsNoctisLilac, styles: noctisLilacLightStyle },
        dark: { extension: monokai, settings: defaultSettingsMonokai, styles: monokaiDarkStyle },
    },
    nord: {
        light: { extension: quietlight, settings: defaultSettingsQuietlight, styles: quietlightStyle },
        dark: { extension: nord, settings: defaultSettingsNord, styles: nordDarkStyle },
    },
    kimbie: {
        // The package draws a white caret on its white background, which hides it; the caret takes the text colour.
        light: drawn('light', { settings: { ...defaultSettingsEclipse, caret: '#000000' }, styles: eclipseLightStyle }),
        dark: { extension: kimbie, settings: defaultSettingsKimbie, styles: kimbieDarkStyle },
    },
}

const schemeOf = (name: Exclude<ThemeName, 'standard'>, isDarkMode: boolean): EditorScheme =>
    SCHEMES[name][isDarkMode ? 'dark' : 'light']

/**
 * The theme the code editor is drawn in, for a theme in the given appearance.
 *
 * The standard theme keeps CodeMirror's own light theme and One Dark, named as the editor component takes them.
 * Every other theme is drawn in its scheme. The same theme is handed out every time, so the editor is not
 * reconfigured on every render.
 */
export const editorTheme = (name: ThemeName, isDarkMode: boolean): Extension | Appearance => {
    if (name === 'standard') {
        return isDarkMode ? 'dark' : 'light'
    }
    return schemeOf(name, isDarkMode).extension
}

const highlightStyles = new Map<EditorScheme, HighlightStyle>()

/**
 * The colours the code editor gives each kind of code, for code drawn outside the editor.
 *
 * They are the styles of the editor's own theme: CodeMirror's default ones and One Dark's for the standard theme,
 * and the scheme's for every other.
 */
export const codeHighlightStyle = (name: ThemeName, isDarkMode: boolean): HighlightStyle => {
    if (name === 'standard') {
        return isDarkMode ? oneDarkHighlightStyle : defaultHighlightStyle
    }
    const scheme = schemeOf(name, isDarkMode)
    return cached(highlightStyles, scheme, () => HighlightStyle.define(scheme.styles))
}
