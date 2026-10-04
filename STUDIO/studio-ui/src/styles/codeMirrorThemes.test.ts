import { describe, expect, it } from 'vitest'
import { defaultHighlightStyle } from '@codemirror/language'
import { oneDarkHighlightStyle } from '@codemirror/theme-one-dark'
import { EditorState, EditorView } from '@uiw/react-codemirror'
import { codeHighlightStyle, editorTheme, SCHEMES } from './codeMirrorThemes'
import { THEME_ORDER, variantOf } from './themes'
import { hex } from '../testing/theme'

const CODE_THEMES = THEME_ORDER.filter(name => name !== 'standard')

describe('codeMirrorThemes', () => {
    it.each(CODE_THEMES)('keeps the copies of the editor colours of %s in step with its scheme', name => {
        // The screens are painted before the schemes load, so the themes carry copies of these two colours.
        [false, true].forEach(isDarkMode => {
            const { settings } = SCHEMES[name][isDarkMode ? 'dark' : 'light']
            const editor = variantOf(name, isDarkMode).editor

            expect(hex(editor?.background)).toBe(hex(settings.background))
            expect(hex(editor?.foreground)).toBe(hex(settings.foreground))
        })
    })

    it.each(CODE_THEMES)('draws the dark variant of %s as a dark editor and the light one as a light editor', name => {
        // The editor picks its own panels, tooltips and cursor by this flag.
        [false, true].forEach(isDarkMode => {
            const state = EditorState.create({ extensions: SCHEMES[name][isDarkMode ? 'dark' : 'light'].extension })

            expect(state.facet(EditorView.darkTheme)).toBe(isDarkMode)
        })
    })

    it('draws the standard theme in CodeMirror\'s own light theme and in One Dark', () => {
        expect(editorTheme('standard', false)).toBe('light')
        expect(editorTheme('standard', true)).toBe('dark')
        expect(codeHighlightStyle('standard', false)).toBe(defaultHighlightStyle)
        expect(codeHighlightStyle('standard', true)).toBe(oneDarkHighlightStyle)
    })

    it('hands out the same theme every time', () => {
        expect(editorTheme('dracula', true)).toBe(editorTheme('dracula', true))
        expect(editorTheme('dracula', true)).not.toBe(editorTheme('dracula', false))
        expect(codeHighlightStyle('nord', true)).toBe(codeHighlightStyle('nord', true))
    })

    it('draws Alucard in the colours of the Dracula specification', () => {
        expect(SCHEMES.dracula.light.settings).toMatchObject({ background: '#fffbeb', foreground: '#1f1f1f' })
        expect(SCHEMES.dracula.light.styles).toEqual(expect.arrayContaining([
            expect.objectContaining({ color: '#a3144d' }),
            expect.objectContaining({ color: '#846e15' }),
            expect.objectContaining({ color: '#14710a' }),
        ]))
    })

    it('shows the caret of Eclipse on its white background', () => {
        expect(SCHEMES.kimbie.light.settings.caret).not.toBe(SCHEMES.kimbie.light.settings.background)
    })

})
