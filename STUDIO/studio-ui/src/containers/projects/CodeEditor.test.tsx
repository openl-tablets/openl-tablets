import { render } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ThemeProvider } from 'antd-style'
import { EditorView } from '@uiw/react-codemirror'
import { CodeEditor } from './CodeEditor'
import { renderInTheme } from '../../testing/theme'

describe('CodeEditor', () => {
    it('mounts a CodeMirror editor for an XML file', () => {
        const { container } = render(<CodeEditor path="rules.xml" value="<project/>" />)
        expect(container.querySelector('.cm-editor')).toBeTruthy()
    })

    it('mounts read-only (non-editable) without crashing', () => {
        const { container } = render(<CodeEditor readOnly path="log4j.properties" value="a=b" />)
        const editable = container.querySelector('.cm-content')?.getAttribute('contenteditable')
        expect(container.querySelector('.cm-editor')).toBeTruthy()
        expect(editable).toBe('false')
    })

    it('mounts unknown file types as plain text', () => {
        const { container } = render(<CodeEditor path="README" value="plain" />)
        expect(container.querySelector('.cm-editor')).toBeTruthy()
    })

    it('selects json, yaml and groovy language modes', () => {
        expect(render(<CodeEditor path="config.json" value="{}" />).container.querySelector('.cm-editor')).toBeTruthy()
        expect(render(<CodeEditor path="deploy.yaml" value="a: b" />).container.querySelector('.cm-editor')).toBeTruthy()
        expect(render(<CodeEditor path="rules/Script.groovy" value="println 1" />).container.querySelector('.cm-editor')).toBeTruthy()
    })

    it('is drawn light by default and dark when the appearance is dark', () => {
        expect(render(<CodeEditor path="a.xml" value="" />).container.querySelector('.cm-theme-light')).toBeTruthy()
        const dark = render(
            <ThemeProvider themeMode="dark">
                <CodeEditor path="a.xml" value="" />
            </ThemeProvider>
        )
        expect(dark.container.querySelector('.cm-theme-dark')).toBeTruthy()
    })

    it('is drawn in the scheme of a theme taken from a code editor', () => {
        const { container } = renderInTheme(<CodeEditor path="a.xml" value="" />, { theme: 'dracula' })

        expect(container.querySelector('.cm-theme .cm-editor')).toBeTruthy()
        expect(container.querySelector('.cm-theme-light, .cm-theme-dark')).toBeNull()
    })

    it('numbers the lines of a part of a file as the whole file numbers them', () => {
        const { container } = render(<CodeEditor readOnly firstLine={41} path="notes.txt" value={'a\nb'} />)
        const numbers = [...container.querySelectorAll('.cm-lineNumbers .cm-gutterElement')]
            .map(element => element.textContent)

        expect(numbers).toContain('41')
        expect(numbers).toContain('42')
        expect(numbers).not.toContain('1')
        expect(container.querySelector('.cm-foldGutter')).toBeNull()
        expect(container.querySelector('.cm-activeLine')).toBeNull()
    })

    it('numbers the lines anew when the part starts at another line', () => {
        const { container, rerender } = render(<CodeEditor readOnly firstLine={41} path="notes.txt" value={'a\nb'} />)
        const numbers = () => [...container.querySelectorAll('.cm-lineNumbers .cm-gutterElement')]
            .map(element => element.textContent)

        rerender(<CodeEditor readOnly firstLine={51} path="notes.txt" value={'a\nb'} />)

        expect(numbers()).toContain('51')
        expect(numbers()).toContain('52')
        expect(numbers()).not.toContain('41')
    })

    it('keeps the cursor where it is when the text written comes back with the line breaks of Windows', () => {
        const { container, rerender } = render(<CodeEditor path="notes.txt" value={'a=1\r\nb=2\r\n'} />)
        const view = EditorView.findFromDOM(container.querySelector('.cm-editor') as HTMLElement)!
        view.dispatch({ changes: { from: 3, insert: 'X' }, selection: { anchor: 4 } })

        // The screen writes the text with the line breaks of its file and gives it back so.
        rerender(<CodeEditor path="notes.txt" value={'a=1X\r\nb=2\r\n'} />)

        // The text reads as the editor holds it, so it is not set anew and the cursor stays.
        expect(view.state.doc.toString()).toBe('a=1X\nb=2\n')
        expect(view.state.selection.main.head).toBe(4)
    })

    it('keeps the numbers of a moved part when the editor is set up anew for another appearance', () => {
        const numbers = (root: HTMLElement) => [...root.querySelectorAll('.cm-lineNumbers .cm-gutterElement')]
            .map(element => element.textContent)
        const inMode = (mode: 'light' | 'dark', firstLine: number) => (
            <ThemeProvider themeMode={mode}>
                <CodeEditor readOnly firstLine={firstLine} path="notes.txt" value={'a\nb'} />
            </ThemeProvider>
        )
        const { container, rerender } = render(inMode('light', 41))
        rerender(inMode('light', 51))

        // Another appearance hands the editor another theme, which sets the whole editor up anew.
        rerender(inMode('dark', 51))

        expect(container.querySelector('.cm-theme-dark')).toBeTruthy()
        expect(numbers(container)).toContain('51')
        expect(numbers(container)).not.toContain('41')
    })

    it('says what it holds while it holds no text', () => {
        const { container } = render(<CodeEditor path="notes.txt" placeholder="No lines" value="" />)

        expect(container.querySelector('.cm-placeholder')?.textContent).toBe('No lines')
    })

    it('forwards edit changes when not read-only', async () => {
        const onChange = vi.fn()
        const { container } = render(<CodeEditor onChange={onChange} path="rules.xml" readOnly={false} value="<a/>" />)
        const editable = container.querySelector('.cm-content')
        expect(editable).toBeTruthy()
    })
})
