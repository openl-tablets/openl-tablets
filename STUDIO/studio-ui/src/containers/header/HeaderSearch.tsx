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

/** Whether a key pressed on this element is text being written, which the shortcut must leave alone. */
const takesText = (target: EventTarget | null): boolean =>
    target instanceof HTMLElement
    && (target.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName))

interface ActionControlProps {
    action: SearchAction
    text: string
    /** The class of a control whose choice narrows the search. */
    narrowedClass: string
}

/** The control a screen adds to the field: pressed with the text, or opened to choose where to look. */
const ActionControl = ({ action, text, narrowedClass }: ActionControlProps) => {
    const { choices, choice } = action
    const narrowed = choices !== undefined && choice !== undefined && choice !== choices[0]?.value
    const button = (
        <Button
            aria-label={action.label}
            className={narrowed ? narrowedClass : ''}
            data-testid="header-search-action"
            icon={action.icon}
            onClick={choices ? undefined : () => action.onClick?.(text)}
            // The field keeps the caret, and the text just typed, while the control is pressed.
            onMouseDown={event => event.preventDefault()}
            size="small"
            type="text"
        />
    )
    return (
        <Tooltip title={action.label}>
            {choices
                ? (
                    <Dropdown
                        trigger={['click']}
                        menu={{
                            items: choices.map(({ value, label }) => ({ key: value, label })),
                            onClick: ({ key }) => action.onChoice?.(key),
                            selectable: true,
                            selectedKeys: choice === undefined ? [] : [choice],
                        }}
                    >
                        {button}
                    </Dropdown>
                )
                : button}
        </Tooltip>
    )
}

/**
 * The one search of OpenL Studio, in the header between the logo and the tabs.
 *
 * It searches what the screen shows: a screen hands it a scope through `useSearchScope` — the projects, the files
 * of a project, the tables of a module, the users — and the field names that scope before the text, filters as
 * the reader types, and shows the control the screen adds: the extended search of a module, the guide to search
 * in. A screen with several things to search lists them under the name. A screen with none searches the projects:
 * Enter opens their list with the text.
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
    // What the reader typed, and in which scope. The text of a scope comes back from its screen a render later, so
    // while the caret is in the field the typed text wins; once it leaves, or in another scope, the screen's does.
    const [draft, setDraft] = useState<{ id: string, text: string } | null>(null)

    // A screen with nothing to search searches the projects: Enter opens their list with the text.
    const scope: SearchScope = activeScope({ scopes, picked }) ?? {
        id: EVERYWHERE,
        label: t('common:search.scopes.projects'),
        placeholder: t('common:search.projects_jump'),
        value: jump,
        onChange: setJump,
        onEnter: found => navigate(`/projects?q=${encodeURIComponent(found)}`),
    }
    const text = focused && draft?.id === scope.id ? draft.text : scope.value

    const write = (value: string) => {
        setDraft({ id: scope.id, text: value })
        scope.onChange(value)
    }

    useEffect(() => {
        const onKeyDown = (event: KeyboardEvent) => {
            if (event.key !== '/' || event.metaKey || event.ctrlKey || event.altKey || takesText(event.target)) {
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
        if (event.nativeEvent.isComposing) {
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
            write('')
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
                    <ActionControl action={scope.action} narrowedClass={styles.narrowed} text={text} />
                </span>
            )}
        </div>
    )
})

HeaderSearch.displayName = 'HeaderSearch'
