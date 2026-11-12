import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { type SearchScope, useSearchStore } from '../../store/searchStore'
import { HeaderSearch } from './HeaderSearch'
import { searchScopeOf } from '../../testing/search'

const { navigate } = vi.hoisted(() => ({ navigate: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string, values?: { scope?: string }) => (values?.scope ? `${key}|${values.scope}` : key)
    return { useTranslation: () => ({ t }) }
})

vi.mock('../../hooks/useAppNavigate', () => ({ useAppNavigate: () => navigate }))

const scope = searchScopeOf

/**
 * Puts a scope on screen, as a part of the screen does, and gives it back with the handler the field writes to.
 * Like a part of the screen, it keeps the text it is given, unless the test hands it a handler of its own.
 */
const mount = (id: string, over: Partial<SearchScope> = {}) => {
    const mounted: SearchScope = scope(id, {
        onChange: vi.fn((value: string) => useSearchStore.getState().mount({ ...mounted, value })),
        ...over,
    })
    act(() => useSearchStore.getState().mount(mounted))
    return mounted
}

const field = () => screen.getByTestId('header-search')
const input = () => screen.getByTestId<HTMLInputElement>('header-search-input')
const name = () => screen.getByTestId('header-search-scope')

describe('HeaderSearch', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useSearchStore.setState({ scopes: [], picked: null })
    })

    it('searches what the screen shows, naming it before the text', async () => {
        render(<HeaderSearch />)
        const files = mount('files', { label: 'Files', placeholder: 'Search files', value: 'xml' })

        expect(field()).toHaveAttribute('data-scope', 'files')
        expect(field()).toHaveAttribute('role', 'search')
        expect(name()).toHaveTextContent('Files')
        // One thing to search: the name is no button, there is nothing to choose.
        expect(name().tagName).toBe('SPAN')
        expect(input()).toHaveValue('xml')
        expect(input()).toHaveAttribute('placeholder', 'Search files')
        expect(input()).toHaveAccessibleName('common:search.label|Files')

        await userEvent.clear(input())
        await userEvent.type(input(), 'r')

        expect(files.onChange).toHaveBeenLastCalledWith('r')
    })

    it('searches the projects from a screen with nothing to search, once Enter is pressed', async () => {
        render(<HeaderSearch />)

        expect(field()).toHaveAttribute('data-scope', 'everywhere')
        expect(name()).toHaveTextContent('common:search.scopes.projects')
        expect(input()).toHaveAttribute('placeholder', 'common:search.projects_jump')

        await userEvent.type(input(), ' bank & trust ')
        expect(navigate).not.toHaveBeenCalled()
        // The key that sends the text off is shown once there is a text to send.
        expect(field()).toHaveTextContent('↵')

        await userEvent.keyboard('{Enter}')

        expect(navigate).toHaveBeenCalledWith('/projects?q=bank%20%26%20trust')
        expect(input()).toHaveValue('')
    })

    it('opens no list of projects for an empty text, or from a screen that filters as it is typed', async () => {
        render(<HeaderSearch />)

        await userEvent.type(input(), '   {Enter}')
        expect(navigate).not.toHaveBeenCalled()

        mount('users')
        await userEvent.type(input(), 'ada{Enter}')
        expect(navigate).not.toHaveBeenCalled()
    })

    it('lists the things a screen offers to search, the main part first, and searches the one chosen', async () => {
        render(<HeaderSearch />)
        const tree = mount('tree', { label: 'Projects', secondary: true, value: 'corp' })
        mount('files', { label: 'Files' })

        // The main part of the screen has the field until the reader chooses otherwise.
        expect(name()).toHaveTextContent('Files')
        expect(name()).toHaveAccessibleName('common:search.pick_scope|Files')

        await userEvent.click(name())
        const offered = await screen.findAllByRole('menuitem')
        expect(offered.map(item => item.textContent)).toEqual(['Files', 'Projects'])

        await userEvent.click(offered[1]!)

        await waitFor(() => expect(field()).toHaveAttribute('data-scope', 'tree'))
        // Each scope keeps its own text, and the caret goes back to the field.
        expect(input()).toHaveValue('corp')
        expect(input()).toHaveFocus()

        await userEvent.type(input(), 'o')
        expect(tree.onChange).toHaveBeenLastCalledWith('corpo')
    })

    it('goes back to the main part of the screen when the chosen panel is put away', () => {
        render(<HeaderSearch />)
        mount('tree', { secondary: true })
        mount('files')
        act(() => useSearchStore.getState().pick('tree'))
        expect(field()).toHaveAttribute('data-scope', 'tree')

        act(() => useSearchStore.getState().unmount('tree'))

        expect(field()).toHaveAttribute('data-scope', 'files')
        expect(name().tagName).toBe('SPAN')
    })

    it('presses the control of the screen with the text typed so far', async () => {
        const onClick = vi.fn()
        render(<HeaderSearch />)
        mount('tables', { action: { icon: <span>filter</span>, label: 'Extended search', onClick } })

        await userEvent.type(input(), ' greet ')
        await userEvent.click(screen.getByRole('button', { name: 'Extended search' }))

        expect(onClick).toHaveBeenCalledWith(' greet ')
    })

    it('lets the reader choose where the search looks, and marks a choice that narrows it', async () => {
        const onChoice = vi.fn()
        const choices = [{ value: '', label: 'All Guides' }, { value: 'studio/', label: 'Studio Guide' }]
        const action = { choice: '', choices, icon: <span>book</span>, label: 'Choose a guide', onChoice }
        render(<HeaderSearch />)
        mount('guides', { action })
        const control = screen.getByTestId('header-search-action')
        const wide = control.className

        await userEvent.click(control)
        const offered = await screen.findAllByRole('menuitem')
        expect(offered.map(item => item.textContent)).toEqual(['All Guides', 'Studio Guide'])
        await userEvent.click(offered[1]!)

        expect(onChoice).toHaveBeenCalledWith('studio/')

        // The screen takes the choice: the control wears another colour while the search is narrowed.
        mount('guides', { action: { ...action, choice: 'studio/' }, label: 'Studio Guide' })
        expect(name()).toHaveTextContent('Studio Guide')
        expect(screen.getByTestId('header-search-action').className).not.toBe(wide)
    })

    it('draws no control for a screen that adds none', () => {
        render(<HeaderSearch />)
        mount('users')

        expect(screen.queryByTestId('header-search-action')).toBeNull()
    })

    it('takes the caret on the / key, unless the reader is writing somewhere', () => {
        render(
            <>
                <HeaderSearch />
                <textarea data-testid="elsewhere" />
            </>
        )
        // The key is offered while the field is empty and the caret is elsewhere.
        expect(field()).toHaveTextContent('/')

        fireEvent.keyDown(screen.getByTestId('elsewhere'), { key: '/' })
        expect(input()).not.toHaveFocus()

        fireEvent.keyDown(window, { key: '/', ctrlKey: true })
        expect(input()).not.toHaveFocus()

        fireEvent.keyDown(document.body, { key: '/' })
        expect(input()).toHaveFocus()
        expect(field()).not.toHaveTextContent('/')
    })

    it('leaves `/` to a screen that took it, and to a dialog over the page', () => {
        render(
            <>
                <HeaderSearch />
                <div
                    data-testid="grid"
                    onKeyDown={event => event.key === '/' && event.preventDefault()}
                    tabIndex={0}
                />
                <div role="dialog">
                    <button type="button">OK</button>
                </div>
            </>
        )

        // A picked cell of a table opens on the key and keeps the caret.
        fireEvent.keyDown(screen.getByTestId('grid'), { key: '/' })
        expect(input()).not.toHaveFocus()

        // A dialog keeps its reader: the field behind the mask does not take the caret.
        fireEvent.keyDown(screen.getByRole('button', { name: 'OK' }), { key: '/' })
        expect(input()).not.toHaveFocus()
    })

    it('clears the text on Escape, and leaves the field on the next one', async () => {
        render(<HeaderSearch />)
        const users = mount('users')

        await userEvent.type(input(), 'ada')
        await userEvent.keyboard('{Escape}')

        expect(users.onChange).toHaveBeenLastCalledWith('')
        expect(input()).toHaveValue('')
        expect(input()).toHaveFocus()

        await userEvent.keyboard('{Escape}')
        expect(input()).not.toHaveFocus()
    })

    it('keeps what is being typed when the text of the screen comes back late', async () => {
        render(<HeaderSearch />)
        // A screen that answers when it pleases: the test says when its text comes back.
        const onChange = vi.fn()
        mount('projects', { onChange })

        await userEvent.type(input(), 'ban')
        // The screen answers with the text it had two keys ago.
        mount('projects', { onChange, value: 'b' })
        expect(input()).toHaveValue('ban')

        // Once the caret leaves, the field shows what the screen holds.
        await userEvent.tab()
        expect(input()).toHaveValue('b')
        mount('projects', { onChange, value: 'ban' })
        expect(input()).toHaveValue('ban')
    })

    it('goes on from the text of the screen when the caret comes back after the screen cleared it', async () => {
        render(<HeaderSearch />)
        const onChange = vi.fn()
        mount('projects', { onChange })
        await userEvent.type(input(), 'bank')
        mount('projects', { onChange, value: 'bank' })
        await userEvent.tab()

        // The screen cleared its search — a button of its empty state — while the caret was elsewhere.
        mount('projects', { onChange, value: '' })
        await userEvent.click(input())

        expect(input()).toHaveValue('')
        await userEvent.type(input(), 'x')
        expect(onChange).toHaveBeenLastCalledWith('x')
    })

    it('leaves Enter and Escape to an input method composing a text', () => {
        render(<HeaderSearch />)
        const users = mount('users')
        fireEvent.change(input(), { target: { value: 'あ' } })

        fireEvent.keyDown(input(), { key: 'Escape', isComposing: true })
        expect(input()).toHaveValue('あ')

        act(() => useSearchStore.getState().unmount('users'))
        fireEvent.change(input(), { target: { value: 'ば' } })
        fireEvent.keyDown(input(), { key: 'Enter', isComposing: true })
        expect(navigate).not.toHaveBeenCalled()

        fireEvent.keyDown(input(), { key: 'Enter' })
        expect(navigate).toHaveBeenCalledWith('/projects?q=%E3%81%B0')
        expect(users.onChange).toHaveBeenLastCalledWith('あ')
    })

    it('leaves the Enter that confirms a composition to the input method, as Safari sends it', async () => {
        render(<HeaderSearch />)
        fireEvent.change(input(), { target: { value: 'ば' } })

        // Safari ends the composition first, then sends the Enter that confirmed it, no longer composing.
        fireEvent.compositionStart(input())
        fireEvent.compositionEnd(input())
        fireEvent.keyDown(input(), { key: 'Enter' })
        expect(navigate).not.toHaveBeenCalled()

        // The next Enter is the reader's own.
        await act(() => new Promise(resolve => setTimeout(resolve, 0)))
        fireEvent.keyDown(input(), { key: 'Enter' })
        expect(navigate).toHaveBeenCalledWith('/projects?q=%E3%81%B0')
    })

    it('keeps the caret, and the text typed, while the control of the screen is pressed', async () => {
        const onClick = vi.fn()
        render(<HeaderSearch />)
        // A screen whose text comes back late: the press must still carry what was typed.
        mount('tables', { onChange: vi.fn(), action: { icon: <span>filter</span>, label: 'Extended search', onClick } })

        await userEvent.type(input(), 'greet')
        await userEvent.click(screen.getByRole('button', { name: 'Extended search' }))

        expect(input()).toHaveFocus()
        expect(onClick).toHaveBeenCalledWith('greet')
    })

    it('shows the text the screen changed itself while the caret is elsewhere', () => {
        render(<HeaderSearch />)
        mount('projects', { value: 'bank' })
        expect(input()).toHaveValue('bank')

        // The screen cleared its search: a button of its empty state, a link opened.
        mount('projects', { value: '' })

        expect(input()).toHaveValue('')
    })

    it('shows the text of the screen the reader moved to, whatever was typed on the last one', async () => {
        render(<HeaderSearch />)
        mount('projects')
        await userEvent.type(input(), 'bank')

        act(() => useSearchStore.getState().unmount('projects'))
        mount('files', { value: 'xml' })

        expect(input()).toHaveValue('xml')
    })
})
