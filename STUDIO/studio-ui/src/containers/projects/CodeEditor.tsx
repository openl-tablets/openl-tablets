import { useMemo } from 'react'
import CodeMirror, { type Extension } from '@uiw/react-codemirror'
import { xml } from '@codemirror/lang-xml'
import { json } from '@codemirror/lang-json'
import { yaml } from '@codemirror/lang-yaml'
import { StreamLanguage } from '@codemirror/language'
import { properties } from '@codemirror/legacy-modes/mode/properties'
import { groovy } from '@codemirror/legacy-modes/mode/groovy'
import { createStyles } from 'antd-style'
import { MOCKUP } from './projectsTheme'

const useStyles = createStyles(({ css, token }) => ({
    editor: css`
        height: 100%;

        .cm-editor {
            height: 100%;
            font-family: ${MOCKUP.fontMono};
            font-size: 13px;
            background: ${token.colorBgContainer};
        }

        .cm-editor.cm-focused {
            outline: none;
        }

        .cm-scroller {
            font-family: ${MOCKUP.fontMono};
        }

        .cm-gutters {
            background: ${token.colorFillQuaternary};
            border-right: 1px solid ${token.colorBorderSecondary};
            color: ${token.colorTextQuaternary};
        }

        .cm-activeLine,
        .cm-activeLineGutter {
            background: ${token.colorFillTertiary};
        }
    `,
}))

/** CodeMirror language extensions for a file, chosen from its extension. Unknown types render as plain text. */
const languageFor = (path: string): Extension[] => {
    const ext = path.slice(path.lastIndexOf('.') + 1).toLowerCase()
    switch (ext) {
        case 'xml':
        case 'html':
        case 'xhtml':
            return [xml()]
        case 'json':
            return [json()]
        case 'yaml':
        case 'yml':
            return [yaml()]
        case 'properties':
            return [StreamLanguage.define(properties)]
        case 'groovy':
            return [StreamLanguage.define(groovy)]
        default:
            return []
    }
}

interface CodeEditorProps {
    value: string
    path: string
    /** Renders the content without a cursor and rejects edits. */
    readOnly?: boolean
    onChange?: (value: string) => void
}

/**
 * Syntax-highlighted view of a project text file, backed by CodeMirror. Highlighting is chosen from the
 * file extension (XML, JSON, YAML, .properties, Groovy); other types show as plain monospace text. When
 * {@link CodeEditorProps.readOnly} is set the content is shown for viewing only.
 */
export const CodeEditor = ({ value, path, readOnly, onChange }: CodeEditorProps) => {
    const { styles } = useStyles()
    const extensions = useMemo(() => languageFor(path), [path])
    return (
        <CodeMirror
            className={styles.editor}
            editable={!readOnly}
            extensions={extensions}
            height="100%"
            theme="light"
            value={value}
            {...(onChange ? { onChange } : {})}
        />
    )
}
