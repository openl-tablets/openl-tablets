import { memo, useEffect, useRef, useState } from 'react'
import { Button, Dropdown, type InputRef, Tooltip } from 'antd'
import { DownOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { SearchInput } from '../../components/SearchInput'
import { useAppNavigate } from '../../hooks/useAppNavigate'
import {
    activeScope,
    listedScopes,
    type SearchAction,
    type SearchScope,
    useSearchStore,
} from '../../store/searchStore'
import { useStyles } from './HeaderSearch.styles'

/** The id of the scope of a screen with nothing to search. */
const EVERYWHERE = 'everywhere'

/**
 * Whether `/` pressed on this element belongs to something else: text being written, or a dialog standing over
 * the page, whose reader would otherwise type into the field behind its mask.
 */
const keyBelongsElsewhere = (target: EventTarget | null): boolean =>
    target instanceof HTMLElement
    && (target.isContentEditable
        || ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName)
        || target.closest('[role="dialog"]') !== null)

interface ActionControlProps {
    action: SearchAction
    /** The class of a control whose choice narrows the search. */
    narrowedClass: string
}

/** The control a screen adds to the field: opened to choose where to look. */
const ActionControl = ({ action, narrowedClass }: ActionControlProps) => {
    const { choices, choice } = action
    const narrowed = choice !== undefined && choice !== choices[0]?.value
    return (
        <Tooltip title={action.label}>
            <Dropdown
                trigger={['click']}
                menu={{
                    items: choices.map(({ value, label }) => ({ key: value, label })),
                    onClick: ({ key }) => action.onChoice(key),
                    selectable: true,
                    selectedKeys: choice === undefined ? [] : [choice],
                }}
            >
                <Button
                    aria-label={action.label}
                    className={narrowed ? narrowedClass : ''}
                    data-testid="header-search-action"
                    icon={action.icon}
                    // The field keeps the caret, and the text just typed, while the control is opened.
                    onMouseDown={event => event.preventDefault()}
                    size="small"
                    type="text"
                />
            </Dropdown>
        </Tooltip>
    )
}

/**
 * The one search of OpenL Studio, in the header between the logo and the tabs.
 *
 * It searches what the screen shows: a screen hands it a scope through `useSearchScope` — the projects, the files
 * of a project, the tables of a module, the users — and the field names that scope before the text, filters as
 * the reader types, and shows the control the screen adds: the guide to search in. A screen may also act on Enter:
 * a module then searches every cell of its tables. A screen with several things to search lists them under the name.
 * A screen with none searches the projects: Enter opens their list with the text.
 *
 * `/` brings the caret here from anywhere outside a text field, and Escape clears the text, then leaves the field.
 */
export const HeaderSearch = memo(() => {
    const { t } = useTranslation()
    const { styles, cx } = useStyles()
    const navigate = useAppNavigate()
    const scopes = useSearchStore(state => state.scopes)
    const picked = useSearchStore(state => state.picked)
    const pick = useSearchStore(state => state.pick)
    const inputRef = useRef<InputRef>(null)
    const [jump, setJump] = useState('')
    const [focused, setFocused] = useState(false)
    // What the reader typed, in which scope, every text the field handed the screen since the caret came — the text of
    // the screen it started from first — and how far the screen has come back with them. The text of a scope comes back
    // from its screen a render later, in the order it was handed, so while the caret is in the field the typed text
    // wins over a text on its way back; once the caret leaves, in another scope, or when the screen changes the text
    // itself — to one it was not handed, or back to one before the last it showed — the screen's does.
    const [draft, setDraft] = useState<{ id: string, text: string, handed: string[], back: number } | null>(null)
    // Set while an input method composes a text, and for the moment after: Safari ends the composition before it
    // sends the Enter that confirmed it, and that Enter no longer says it was composing.
    const composing = useRef(false)

    // A screen with nothing to search searches the projects: Enter opens their list with the text, which the list
    // takes over, so the field lets it go.
    const scope: SearchScope = activeScope({ scopes, picked }) ?? {
        id: EVERYWHERE,
        label: t('common:search.scopes.projects'),
        placeholder: t('common:search.projects_jump'),
        value: jump,
        onChange: setJump,
        onEnter: found => {
            navigate(`/projects?q=${encodeURIComponent(found)}`)
            write('')
        },
    }
    const drafted = focused && draft?.id === scope.id ? draft : null
    const returned = drafted === null ? -1 : drafted.handed.lastIndexOf(scope.value)
    if (drafted !== null && returned < drafted.back) {
        // The screen changed the text itself, such as a filter it cleared: what was typed before is let go.
        setDraft(null)
    } else if (drafted !== null && returned > drafted.back) {
        setDraft({ ...drafted, back: returned })
    }
    const text = drafted !== null && returned >= drafted.back ? drafted.text : scope.value

    const write = (value: string) => {
        const { id, value: shown } = scope
        setDraft(current => (current?.id === id
            ? { ...current, text: value, handed: [...current.handed, value] }
            : { id, text: value, handed: [shown, value], back: 0 }))
        scope.onChange(value)
    }

    useEffect(() => {
        const onKeyDown = (event: KeyboardEvent) => {
            // A key the screen took already — a picked cell of a table opens on it — is the screen's.
            const free = event.key === '/' && !event.defaultPrevented && !event.metaKey && !event.ctrlKey && !event.altKey
            if (!free || keyBelongsElsewhere(event.target)) {
                return
            }
            event.preventDefault()
            inputRef.current?.focus()
        }
        window.addEventListener('keydown', onKeyDown)
        return () => window.removeEventListener('keydown', onKeyDown)
    }, [])

    const onKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
        // A key that confirms or cancels the text an input method is composing belongs to the input method.
        if (event.nativeEvent.isComposing || composing.current) {
            return
        }
        if (event.key === 'Escape') {
            if (text === '') {
                inputRef.current?.blur()
            } else {
                write('')
            }
        } else if (event.key === 'Enter' && scope.onEnter && text.trim() !== '') {
            scope.onEnter(text.trim())
        }
    }

    const keyHint = () => {
        if (scope.onEnter && text.trim() !== '') {
            return <kbd aria-hidden className={styles.key}>↵</kbd>
        }
        return !focused && text === '' ? <kbd aria-hidden className={styles.key}>/</kbd> : undefined
    }

    const name = <span className={styles.nameText}>{scope.label}</span>

    return (
        <div
            className={cx(styles.field, focused && styles.focused)}
            data-scope={scope.id}
            data-testid="header-search"
            role="search"
        >
            {scopes.length > 1
                ? (
                    <Dropdown
                        trigger={['click']}
                        menu={{
                            items: listedScopes(scopes).map(({ id, label }) => ({ key: id, label })),
                            onClick: ({ key }) => {
                                pick(key)
                                inputRef.current?.focus()
                            },
                            selectable: true,
                            selectedKeys: [scope.id],
                        }}
                    >
                        <button
                            aria-label={t('common:search.pick_scope', { scope: scope.label })}
                            className={cx(styles.name, styles.nameButton)}
                            data-testid="header-search-scope"
                            type="button"
                        >
                            {name}
                            <DownOutlined />
                        </button>
                    </Dropdown>
                )
                : <span className={styles.name} data-testid="header-search-scope">{name}</span>}
            <SearchInput
                ref={inputRef}
                aria-label={t('common:search.label', { scope: scope.label })}
                autoComplete="off"
                className={styles.input}
                data-testid="header-search-input"
                onBlur={() => setFocused(false)}
                onChange={event => write(event.target.value)}
                onCompositionEnd={() => setTimeout(() => { composing.current = false })}
                onCompositionStart={() => { composing.current = true }}
                onKeyDown={onKeyDown}
                placeholder={scope.placeholder}
                spellCheck={false}
                suffix={keyHint()}
                value={text}
                variant="borderless"
                onFocus={() => {
                    // What was typed before the caret left is gone: the screen's text is the one to go on from.
                    setDraft(null)
                    setFocused(true)
                }}
            />
            {scope.action && (
                <span className={styles.action}>
                    <ActionControl action={scope.action} narrowedClass={styles.narrowed} />
                </span>
            )}
        </div>
    )
})

HeaderSearch.displayName = 'HeaderSearch'
