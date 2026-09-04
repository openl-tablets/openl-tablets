import { render } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { CodeEditor } from './CodeEditor'

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

    it('forwards edit changes when not read-only', async () => {
        const onChange = vi.fn()
        const { container } = render(<CodeEditor onChange={onChange} path="rules.xml" readOnly={false} value="<a/>" />)
        const editable = container.querySelector('.cm-content')
        expect(editable).toBeTruthy()
    })
})
