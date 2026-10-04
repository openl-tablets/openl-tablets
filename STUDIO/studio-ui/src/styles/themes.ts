import type { CSSProperties } from 'react'
import { theme as antdTheme } from 'antd'

/** The two appearances every theme is drawn in. */
export type Appearance = 'light' | 'dark'

/**
 * The hues a code editor writes the parts of a value in.
 *
 * A part the editor leaves in its plain text colour is absent.
 */
interface SyntaxHues {
    /** A property name. */
    name?: string
    string?: string
    number?: string
    boolean?: string
}

/** The colours a theme takes from the colour scheme of its code editor. */
export interface EditorColors {
    /** The background of the editor, which the containers of every screen take as well. */
    background: string
    /** The plain text of the editor. */
    foreground: string
    /**
     * The text of the screens, where the editor's own would not do for them: too faint to read as prose, or a
     * syntax hue rather than a neutral one. The screens take {@link foreground} otherwise.
     */
    text?: string
    /** The accent of the primary buttons, the links and the selection. */
    primary: string
}

/** A theme in one appearance. */
interface ThemeVariant {
    /** The colours of its code editor. The standard theme has none: it wears the colours of Ant Design. */
    editor?: EditorColors
    /** The hues its code editor writes a value in, which a parameter value is drawn in as well. */
    syntax: SyntaxHues
}

/**
 * The themes OpenL Studio offers, in the order the switcher lists them.
 *
 * Each has a light and a dark variant, and the appearance in force picks between them. The standard theme is Ant
 * Design's own. Every other one is the colour scheme of a well-known code editor: the screens take the editor's
 * background, text and accent, while the code editor and the code samples of the user guides are drawn in the
 * scheme itself (`codeMirrorThemes.ts`). Some schemes come in both appearances; the others are paired, such as
 * Dracula with Alucard.
 *
 * The colours are copies of the schemes', which are loaded only with the first editor or code sample; a test keeps
 * the two in step. Where the scheme names no accent of its own, the primary is the scheme's hue that keeps both a
 * link on the background and white button text on the primary at 3:1 at least.
 */
export const THEMES = {
    standard: {
        // CodeMirror's own light theme
        light: { syntax: { string: '#aa1111', number: '#116644', boolean: '#221199' } },
        // One Dark
        dark: { syntax: { name: '#e06c75', string: '#98c379', number: '#e5c07b', boolean: '#d19a66' } },
    },
    dracula: {
        // Alucard
        light: {
            editor: { background: '#fffbeb', foreground: '#1f1f1f', primary: '#644ac9' },
            syntax: { name: '#036a96', string: '#846e15' },
        },
        dark: {
            editor: { background: '#282a36', foreground: '#f8f8f2', primary: '#bd93f9' },
            syntax: { name: '#66d9ef', string: '#f1fa8c' },
        },
    },
    solarized: {
        light: {
            // base02 — the body tone base00 stays under 4.5:1 on base3
            editor: { background: '#fdf6e3', foreground: '#657b83', text: '#073642', primary: '#268bd2' },
            syntax: { name: '#268bd2', string: '#2aa198', number: '#d33682', boolean: '#268bd2' },
        },
        dark: {
            // base2 — the body tone base0 stays under 4.5:1 on base03 once Ant Design softens it
            editor: { background: '#002b36', foreground: '#839496', text: '#eee8d5', primary: '#268bd2' },
            syntax: { name: '#268bd2', string: '#2aa198', number: '#d33682', boolean: '#268bd2' },
        },
    },
    // The primary is the system blue of Apple's platforms.
    xcode: {
        light: {
            editor: { background: '#ffffff', foreground: '#3d3d3d', primary: '#007aff' },
            syntax: { name: '#032f62', string: '#d23423' },
        },
        dark: {
            editor: { background: '#292a30', foreground: '#cecfd0', primary: '#0a84ff' },
            syntax: { name: '#6baa9f', string: '#ff8170' },
        },
    },
    vscode: {
        light: {
            editor: { background: '#ffffff', foreground: '#383a42', primary: '#007acc' },
            syntax: { name: '#0070c1', string: '#a31515', number: '#098658', boolean: '#0000ff' },
        },
        dark: {
            // the editor's plain text is the light blue of a variable; the screens take its neutral grey
            editor: { background: '#1e1e1e', foreground: '#9cdcfe', text: '#d4d4d4', primary: '#3794ff' },
            syntax: { name: '#9cdcfe', string: '#ce9178', number: '#b5cea8', boolean: '#569cd6' },
        },
    },
    gruvbox: {
        light: {
            editor: { background: '#fbf1c7', foreground: '#3c3836', primary: '#076678' },
            syntax: { name: '#427b58', string: '#3c3836', number: '#8f3f71', boolean: '#8f3f71' },
        },
        dark: {
            editor: { background: '#282828', foreground: '#ebdbb2', primary: '#83a598' },
            syntax: { name: '#8ec07c', string: '#ebdbb2', number: '#d3869b', boolean: '#d3869b' },
        },
    },
    material: {
        light: {
            // the scheme's text is a pale blue grey; the screens take a deeper one of the same family
            editor: { background: '#fafafa', foreground: '#90a4ae', text: '#37474f', primary: '#6182b8' },
            syntax: { name: '#6182b8', string: '#91b859', number: '#f76d47', boolean: '#90a4ae' },
        },
        dark: {
            // the keyword purple: the signature teal leaves white button text under 3:1
            editor: { background: '#2e3235', foreground: '#bdbdbd', primary: '#cf6edf' },
            syntax: { name: '#facf4e', string: '#99d066', number: '#ffad42', boolean: '#56c8d8' },
        },
    },
    github: {
        light: {
            editor: { background: '#ffffff', foreground: '#24292e', primary: '#0969da' },
            syntax: { name: '#6f42c1', string: '#032f62', number: '#005cc5', boolean: '#e36209' },
        },
        dark: {
            editor: { background: '#0d1117', foreground: '#c9d1d9', primary: '#58a6ff' },
            syntax: { name: '#d2a8ff', string: '#a5d6ff', number: '#79c0ff', boolean: '#ffab70' },
        },
    },
    // A duotone scheme dims its plain text on purpose; the screens take the bright tone of its names.
    duotone: {
        light: {
            editor: { background: '#faf8f5', foreground: '#b29762', text: '#2d2006', primary: '#1659df' },
            syntax: { name: '#b29762', string: '#1659df', number: '#063289' },
        },
        dark: {
            editor: { background: '#2a2734', foreground: '#6c6783', text: '#eeebff', primary: '#9a86fd' },
            syntax: { name: '#9a86fd', string: '#ffb870', number: '#ffcc99' },
        },
    },
    basic: {
        light: {
            editor: { background: '#ffffff', foreground: '#2e3440', primary: '#5e81ac' },
            syntax: { name: '#d08770', string: '#d08770', number: '#88c0d0', boolean: '#d08770' },
        },
        dark: {
            editor: { background: '#2e3235', foreground: '#dddddd', primary: '#6fb3d2' },
            syntax: { name: '#b5bd68', string: '#b5bd68', number: '#fda331', boolean: '#8abeb7' },
        },
    },
    tokyoNight: {
        // Tokyo Night Day — its blue text, deepened until it reads as prose on the grey background
        light: {
            editor: { background: '#e1e2e7', foreground: '#3760bf', text: '#24438a', primary: '#2e7de9' },
            syntax: { name: '#3760bf', string: '#587539', number: '#b15c00', boolean: '#3760bf' },
        },
        dark: {
            // the editor's plain text is the comment grey; the screens take the scheme's foreground
            editor: { background: '#1a1b26', foreground: '#787c99', text: '#a9b1d6', primary: '#7aa2f7' },
            syntax: { name: '#7aa2f7', string: '#9ece6a', number: '#ff9e64', boolean: '#c0caf5' },
        },
    },
    monokai: {
        // Noctis Lilac
        light: {
            editor: { background: '#f2f1f8', foreground: '#0c006b', primary: '#5c49e9' },
            syntax: { name: '#fa8900', string: '#00b368', number: '#5842ff', boolean: '#5842ff' },
        },
        dark: {
            // the constant purple: the cyan leaves white button text under 3:1
            editor: { background: '#272822', foreground: '#f8f8f2', primary: '#ae81ff' },
            syntax: { name: '#66d9ef', string: '#e6db74', number: '#ae81ff', boolean: '#fd971f' },
        },
    },
    nord: {
        // Quietlight
        light: {
            editor: { background: '#f5f5f5', foreground: '#333333', primary: '#4b69c6' },
            syntax: { name: '#aa3731', string: '#448c27', number: '#9c5d27', boolean: '#7a3e9d' },
        },
        dark: {
            // nord9: the brighter frost leaves white button text under 3:1
            editor: { background: '#2e3440', foreground: '#ffffff', primary: '#81a1c1' },
            syntax: { name: '#88c0d0', string: '#a3be8c', number: '#b48ead', boolean: '#d08770' },
        },
    },
    kimbie: {
        // Eclipse
        light: {
            editor: { background: '#ffffff', foreground: '#000000', primary: '#0000c0' },
            syntax: { name: '#116644', string: '#2a00ff', number: '#116644' },
        },
        dark: {
            // the brown of the scheme's own focus border
            editor: { background: '#221a0f', foreground: '#d3af86', primary: '#a57a4c' },
            syntax: { name: '#7e602c', string: '#889b4a', number: '#f79a32', boolean: '#dc3958' },
        },
    },
} satisfies Record<string, Record<Appearance, ThemeVariant>>

/** The theme a user may pick. */
export type ThemeName = keyof typeof THEMES

/** The themes in the order the switcher offers them, the standard one first. */
export const THEME_ORDER = Object.keys(THEMES) as ThemeName[]

/** A theme in the given appearance. */
export const variantOf = (name: ThemeName, isDarkMode: boolean): ThemeVariant =>
    THEMES[name][isDarkMode ? 'dark' : 'light']

/** The accent a theme shows in the given appearance: the primary of its editor, or Ant Design's own. */
export const accentOf = (name: ThemeName, isDarkMode: boolean): string =>
    variantOf(name, isDarkMode).editor?.primary ?? antdTheme.defaultSeed.colorPrimary

/**
 * The background and the text colour of a block of code: those of the theme's code editor, so a code sample reads
 * as the same code does in the editor.
 *
 * The standard theme gives neither: its editor lies on the surface of the page, and so does a code sample.
 */
export const codeBlockColors = (name: ThemeName, isDarkMode: boolean): CSSProperties => {
    const { editor } = variantOf(name, isDarkMode)
    return editor ? { background: editor.background, color: editor.foreground } : {}
}
