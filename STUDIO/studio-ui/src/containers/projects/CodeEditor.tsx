import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import CodeMirror, {
    Compartment,
    EditorView,
    lineNumbers,
    type Extension,
    type ReactCodeMirrorRef,
} from '@uiw/react-codemirror'
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
            /* A long line scrolled sideways runs under the line numbers, so the fill lies on a solid surface. */
            background: linear-gradient(${token.colorFillQuaternary}, ${token.colorFillQuaternary}),
                ${token.colorBgContainer};
            border-right: 1px solid ${token.colorBorderSecondary};
            color: ${token.colorTextQuaternary};
        }

        .cm-activeLine,
        .cm-activeLineGutter {
            background: ${token.colorFillTertiary};
        }
    `,
}))

/**
 * The languages of the files, defined once: CodeMirror keeps every language it is given for good, so an editor that
 * defined its own would leave one behind each time it is drawn.
 */
const XML: Extension[] = [xml()]
const JSON_LANGUAGE: Extension[] = [json()]
const YAML: Extension[] = [yaml()]
const PROPERTIES: Extension[] = [StreamLanguage.define(properties)]
const GROOVY: Extension[] = [StreamLanguage.define(groovy)]
const PLAIN_TEXT: Extension[] = []

/** CodeMirror language extensions for a file, chosen from its extension. Unknown types render as plain text. */
const languageFor = (path: string): Extension[] => {
    const ext = path.slice(path.lastIndexOf('.') + 1).toLowerCase()
    switch (ext) {
        case 'xml':
        case 'html':
        case 'xhtml':
            return XML
        case 'json':
            return JSON_LANGUAGE
        case 'yaml':
        case 'yml':
            return YAML
        case 'properties':
            return PROPERTIES
        case 'groovy':
            return GROOVY
        default:
            return PLAIN_TEXT
    }
}

/** Lines with the line breaks CodeMirror holds them with, which are those of Unix whatever a text is written with. */
const unixLines = (text: string): string => text.replace(/\r\n?/g, '\n')

/**
 * Puts the cursor at the start of a line, counted from 1, and scrolls the line to the middle of the view. A number
 * past the last line goes to the last one.
 */
const goToLine = (view: EditorView, number: number) => {
    const doc = view.state.doc
    const line = doc.line(Math.min(Math.max(number, 1), doc.lines))
    view.dispatch({ selection: { anchor: line.from }, effects: EditorView.scrollIntoView(line.from, { y: 'center' }) })
}

/** Numbers the lines of a part of a file as the whole file numbers them, from the number of its first line. */
const numbersFrom = (firstLine: number): Extension =>
    lineNumbers({ formatNumber: line => String(line + firstLine - 1) })

/** A part of a file shown on its own numbers its lines itself, and neither folds them nor marks the line it is at. */
const FRAGMENT_SETUP = {
    lineNumbers: false,
    foldGutter: false,
    highlightActiveLine: false,
    highlightActiveLineGutter: false,
}

/** A part of a file that is only read leaves out what helps to write: an editor stands in each part of a merge. */
const READ_ONLY_FRAGMENT_SETUP = {
    ...FRAGMENT_SETUP,
    autocompletion: false,
    bracketMatching: false,
    closeBrackets: false,
    closeBracketsKeymap: false,
    completionKeymap: false,
    crosshairCursor: false,
    dropCursor: false,
    highlightSelectionMatches: false,
    indentOnInput: false,
    rectangularSelection: false,
}

interface CodeEditorProps {
    value: string
    path: string
    /** Renders the content without a cursor and rejects edits. */
    readOnly?: boolean
    onChange?: (value: string) => void
    /** The number of the first line, when the editor holds a part of a file: its lines are numbered from it. */
    firstLine?: number | undefined
    /** What the editor says while it holds no text. */
    placeholder?: string | undefined
    /**
     * The line to put the cursor at, counted from 1, such as the one a compilation message points to. The editor
     * goes there when it opens and whenever the line changes, and leaves the cursor to the reader in between.
     */
    line?: number | undefined
}

/**
 * Syntax-highlighted view of a project text file, backed by CodeMirror. Highlighting is chosen from the
 * file extension (XML, JSON, YAML, .properties, Groovy); other types show as plain monospace text. When
 * {@link CodeEditorProps.readOnly} is set the content is shown for viewing only. The editor is drawn in the
 * theme in force and its appearance, light or dark, and follows both when the user switches: the standard
 * theme lays CodeMirror's own light theme or One Dark on the surface of the page, every other theme is the
 * colour scheme of its code editor.
 *
 * A part of a file shown on its own numbers its lines from {@link CodeEditorProps.firstLine}, as they are numbered
 * in the whole file. It folds nothing and marks no line as the one the cursor is at, since it stands among other
 * parts.
 *
 * The editor holds a text with the line breaks of Unix, as CodeMirror does: a text written with those of Windows is
 * shown as it reads, and the text {@link CodeEditorProps.onChange} is given has the line breaks of Unix.
 *
 * Given a {@link CodeEditorProps.line}, the editor puts the cursor at its start and scrolls it into the middle of the
 * view, which marks it as the line the cursor is at.
 */
export const CodeEditor = ({ value, path, readOnly, onChange, firstLine, placeholder, line }: CodeEditorProps) => {
    const { styles, cx } = useStyles()
    const { isDarkMode } = useThemeMode()
    const { themeName } = useAppTheme()
    const theme = editorTheme(themeName, isDarkMode)
    const fragment = firstLine !== undefined
    const shown = useMemo(() => unixLines(value), [value])
    // The numbers of a part are kept apart from the rest of the setup: when the parts before it grow or shrink, its
    // numbers are drawn again without the editor being set up anew.
    const [numbering] = useState(() => new Compartment())
    const [firstNumbers] = useState(() => numbersFrom(firstLine ?? 1))
    const editor = useRef<ReactCodeMirrorRef>(null)
    // The first line the numbers are drawn from; they are drawn again only when it moves.
    const numberedFrom = useRef(firstLine)
    const extensions = useMemo(() => fragment
        ? [...languageFor(path), numbering.of(firstNumbers)]
        : languageFor(path),
    [path, fragment, numbering, firstNumbers])
    useEffect(() => {
        const view = editor.current?.view
        if (view && firstLine !== undefined && firstLine !== numberedFrom.current) {
            view.dispatch({ effects: numbering.reconfigure(numbersFrom(firstLine)) })
            numberedFrom.current = firstLine
        }
    }, [firstLine, numbering])
    // The line the cursor was last sent to: the editor goes to a line once, not each time it is drawn
    const shownLine = useRef<number | undefined>(undefined)
    const showLine = useCallback((view: EditorView) => {
        if (line !== undefined && line !== shownLine.current) {
            goToLine(view, line)
            shownLine.current = line
        }
    }, [line])
    useEffect(() => {
        const view = editor.current?.view
        if (view) {
            showLine(view)
        }
    }, [showLine])
    return (
        <CodeMirror
            ref={editor}
            className={cx(styles.editor, themeName === 'standard' && styles.pageSurfaces)}
            editable={!readOnly}
            extensions={extensions}
            height="100%"
            onCreateEditor={showLine}
            theme={theme}
            // The text is handed over with the line breaks the editor holds it with: a text with other line breaks
            // would differ from what the editor holds and be set anew whenever it is drawn, moving the cursor away.
            value={shown}
            {...(fragment ? { basicSetup: readOnly ? READ_ONLY_FRAGMENT_SETUP : FRAGMENT_SETUP } : {})}
            {...(placeholder ? { placeholder } : {})}
            {...(onChange ? { onChange } : {})}
        />
    )
}
