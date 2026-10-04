import { useMemo } from 'react'
import CodeMirror, { type Extension } from '@uiw/react-codemirror'
import { xml } from '@codemirror/lang-xml'
import { json } from '@codemirror/lang-json'
import { yaml } from '@codemirror/lang-yaml'
import { StreamLanguage } from '@codemirror/language'
import { properties } from '@codemirror/legacy-modes/mode/properties'
import { groovy } from '@codemirror/legacy-modes/mode/groovy'
import { createStyles, useThemeMode } from 'antd-style'
import { MOCKUP } from './projectsTheme'
import { useAppTheme } from '../../providers/AppThemeProvider'
import { editorTheme } from '../../styles/codeMirrorThemes'

const useStyles = createStyles(({ css, token }) => ({
    editor: css`
        height: 100%;

        .cm-editor {
            height: 100%;
            font-family: ${MOCKUP.fontMono};
            font-size: 13px;
        }

        .cm-editor.cm-focused {
            outline: none;
        }

        .cm-scroller {
            font-family: ${MOCKUP.fontMono};
        }
    `,
    /** The surfaces of the page, laid over CodeMirror's own light theme and One Dark for the standard theme. */
    pageSurfaces: css`
        .cm-editor {
            background: ${token.colorBgContainer};
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
 * {@link CodeEditorProps.readOnly} is set the content is shown for viewing only. The editor is drawn in the
 * theme in force and its appearance, light or dark, and follows both when the user switches: the standard
 * theme lays CodeMirror's own light theme or One Dark on the surface of the page, every other theme is the
 * colour scheme of its code editor.
 */
export const CodeEditor = ({ value, path, readOnly, onChange }: CodeEditorProps) => {
    const { styles, cx } = useStyles()
    const { isDarkMode } = useThemeMode()
    const { themeName } = useAppTheme()
    const theme = editorTheme(themeName, isDarkMode)
    const extensions = useMemo(() => languageFor(path), [path])
    return (
        <CodeMirror
            className={cx(styles.editor, themeName === 'standard' && styles.pageSurfaces)}
            editable={!readOnly}
            extensions={extensions}
            height="100%"
            theme={theme}
            value={value}
            {...(onChange ? { onChange } : {})}
        />
    )
}
