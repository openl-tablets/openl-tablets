import type { ReactElement } from 'react'
import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useProjectTags } from './useProjectTags'
import { getTagTypes } from '../../services/repositories'
import { deleteFile, rootFileExists, writeRootFile } from '../../services/files'

vi.mock('../../services/repositories', () => ({
    getTagTypes: vi.fn(),
}))

vi.mock('../../services/files', () => ({
    deleteFile: vi.fn(),
    rootFileExists: vi.fn(),
    writeRootFile: vi.fn(),
}))

// The options are written after the key, so a test sees which tag a message is about.
vi.mock('react-i18next', () => {
    const t = (key: string, options?: Record<string, string>) => (options ? `${key} ${JSON.stringify(options)}` : key)
    return { useTranslation: () => ({ t }) }
})

const onSaved = vi.fn()

/** The hook hands back two pieces, which the overview places in one row; the test renders both. */
const Host = ({ canEdit = true, tags = {} }: { canEdit?: boolean, tags?: Record<string, string> }) => {
    const { action, content } = useProjectTags({ canEdit, onSaved, projectId: 'p1', tags })
    return <div>{action}{content}</div>
}

/** Renders the tags and lets the read of the configured tag types, which every render starts, settle. */
const renderRead = async (ui: ReactElement) => {
    render(ui)
    await act(async () => {})
}

describe('useProjectTags', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(getTagTypes).mockResolvedValue([
            { name: 'Region', values: ['EU', 'US'], nullable: true, extensible: false },
        ] as never)
        vi.mocked(rootFileExists).mockResolvedValue(true)
        vi.mocked(writeRootFile).mockResolvedValue(undefined)
        vi.mocked(deleteFile).mockResolvedValue(undefined)
    })

    it('reads the tags of the project as a table of type and value', async () => {
        await renderRead(<Host tags={{ Region: 'EU' }} />)

        expect(screen.getByText('Region')).toBeInTheDocument()
        expect(screen.getByText('EU')).toBeInTheDocument()
        expect(screen.queryByTestId('project-tags-editor')).toBeNull()
    })

    it('shows every tag the file names, marking the types no administrator configured', async () => {
        // The file is the source of truth: a tag no administrator configured is still the project's tag.
        await renderRead(<Host tags={{ 'Home-made': 'anything', Region: 'EU' }} />)

        expect(screen.getByText('Home-made')).toBeInTheDocument()
        expect(screen.getByText('anything')).toBeInTheDocument()
        expect(screen.getByTestId('tag-unconfigured-Home-made')).toBeInTheDocument()
        expect(screen.queryByTestId('tag-unconfigured-Region')).toBeNull()
        expect(screen.getByTestId('tags-unconfigured'))
            .toHaveTextContent('browser.tags.unconfigured {"types":"Home-made"}')
    })

    it('shows the tags in the order of the configured tag types, then the others as the file names them', async () => {
        vi.mocked(getTagTypes).mockResolvedValue([
            { name: 'Region', values: [], nullable: true, extensible: true },
            { name: 'Domain', values: [], nullable: true, extensible: true },
        ] as never)
        await renderRead(<Host tags={{ Zeta: '1', domain: '2', Alpha: '3', Region: '4' }} />)

        expect(screen.getByTestId('project-tags')).toHaveTextContent(/^Region4domain2Zeta1Alpha3/)

        // The editor starts from the same order, so saving writes the file in it.
        await userEvent.click(screen.getByTestId('edit-tags'))
        await userEvent.click(screen.getByTestId('tags-save'))
        await waitFor(() => expect(writeRootFile).toHaveBeenCalledWith(
            'p1', 'tags.properties', 'Region=4\ndomain=2\nZeta=1\nAlpha=3\n', 'overwrite'
        ))
    })

    it('marks no type as unconfigured while the configured ones cannot be read', async () => {
        vi.mocked(getTagTypes).mockRejectedValue(new Error('offline'))
        await renderRead(<Host tags={{ 'Home-made': 'anything' }} />)

        expect(screen.getByText('Home-made')).toBeInTheDocument()
        expect(screen.queryByTestId('tag-unconfigured-Home-made')).toBeNull()
        expect(screen.queryByTestId('tags-unconfigured')).toBeNull()
    })

    it('lets an unconfigured tag be renamed or removed, and stops warning about it', async () => {
        await renderRead(<Host tags={{ Home: 'anything', Other: 'one' }} />)

        await userEvent.click(screen.getByTestId('edit-tags'))
        expect(screen.getByTestId('tags-unconfigured'))
            .toHaveTextContent('browser.tags.unconfigured {"types":"Home, Other"}')

        await userEvent.clear(within(screen.getByTestId('edit-tag-0')).getByRole('combobox'))
        await userEvent.type(screen.getByTestId('edit-tag-0'), 'Region')
        expect(screen.getByTestId('tags-unconfigured'))
            .toHaveTextContent('browser.tags.unconfigured {"types":"Other"}')

        await userEvent.click(screen.getByTestId('edit-tag-1-remove'))
        expect(screen.queryByTestId('tags-unconfigured')).toBeNull()
    })

    it('offers no way in for a user who may not write the project', async () => {
        await renderRead(<Host canEdit={false} tags={{ Region: 'EU' }} />)

        expect(screen.queryByTestId('edit-tags')).toBeNull()
    })

    it('saves the edited tags as the tags.properties file', async () => {
        render(<Host tags={{ Region: 'EU' }} />)

        await userEvent.click(screen.getByTestId('edit-tags'))
        expect(screen.getByTestId('project-tags-editor')).toBeInTheDocument()

        await userEvent.click(screen.getByTestId('tags-save'))

        await waitFor(() => expect(writeRootFile).toHaveBeenCalledWith(
            'p1', 'tags.properties', 'Region=EU\n', 'overwrite'
        ))
        expect(onSaved).toHaveBeenCalled()
        await waitFor(() => expect(screen.queryByTestId('project-tags-editor')).toBeNull())
    })

    it('takes any key and any value, not only the configured ones', async () => {
        vi.mocked(rootFileExists).mockResolvedValue(false)
        render(<Host tags={{}} />)

        await userEvent.click(screen.getByTestId('edit-tags'))
        await userEvent.click(screen.getByTestId('edit-tag-add'))
        await userEvent.type(screen.getByTestId('edit-tag-0'), 'Anything')
        await userEvent.type(screen.getByTestId('edit-tag-0-value'), 'goes here')

        await userEvent.click(screen.getByTestId('tags-save'))

        // The file did not exist, so it is created rather than overwritten.
        await waitFor(() => expect(writeRootFile).toHaveBeenCalledWith(
            'p1', 'tags.properties', 'Anything=goes here\n', 'create'
        ))
    })

    it('offers every configured type in a key field that already holds one', async () => {
        vi.mocked(getTagTypes).mockResolvedValue([
            { name: 'Region', values: ['EU', 'US'], nullable: true, extensible: false },
            { name: 'Domain', values: [], nullable: true, extensible: false },
        ] as never)
        render(<Host tags={{ Region: 'EU' }} />)

        await userEvent.click(screen.getByTestId('edit-tags'))
        await waitFor(() => expect(getTagTypes).toHaveBeenCalled())
        await userEvent.click(within(screen.getByTestId('edit-tag-0')).getByRole('combobox'))

        // Like the type fields of Create Table, the list opens whole rather than filtered by the current value.
        expect(await screen.findByTitle('Domain')).toBeInTheDocument()
    })

    it('narrows the offered types to what is typed, ignoring case', async () => {
        vi.mocked(getTagTypes).mockResolvedValue([
            { name: 'Region', values: [], nullable: true, extensible: false },
            { name: 'Domain', values: [], nullable: true, extensible: false },
        ] as never)
        render(<Host tags={{}} />)

        await userEvent.click(screen.getByTestId('edit-tags'))
        await waitFor(() => expect(getTagTypes).toHaveBeenCalled())
        await userEvent.click(screen.getByTestId('edit-tag-add'))
        await userEvent.type(screen.getByTestId('edit-tag-0'), 'dom')

        expect(await screen.findByTitle('Domain')).toBeInTheDocument()
        expect(screen.queryByTitle('Region')).toBeNull()
    })

    it('refuses to save while a key is used twice, whatever its case', async () => {
        render(<Host tags={{ Region: 'EU' }} />)

        await userEvent.click(screen.getByTestId('edit-tags'))
        await userEvent.click(screen.getByTestId('edit-tag-add'))
        await userEvent.type(screen.getByTestId('edit-tag-1'), 'region')
        await userEvent.type(screen.getByTestId('edit-tag-1-value'), 'US')

        // A properties file cannot repeat a key: saving waits until the duplicate is resolved.
        expect(screen.getByTestId('tags-duplicate-keys')).toBeInTheDocument()
        expect(screen.getByTestId('tags-save')).toBeDisabled()
        expect(writeRootFile).not.toHaveBeenCalled()

        await userEvent.clear(within(screen.getByTestId('edit-tag-1')).getByRole('combobox'))
        await userEvent.type(screen.getByTestId('edit-tag-1'), 'Domain')

        expect(screen.queryByTestId('tags-duplicate-keys')).toBeNull()
        await userEvent.click(screen.getByTestId('tags-save'))
        await waitFor(() => expect(writeRootFile).toHaveBeenCalledWith(
            'p1', 'tags.properties', 'Region=EU\nDomain=US\n', 'overwrite'
        ))
    })

    it('deletes the file when the last tag is removed', async () => {
        render(<Host tags={{ Region: 'EU' }} />)

        await userEvent.click(screen.getByTestId('edit-tags'))
        await userEvent.click(screen.getByTestId('edit-tag-0-remove'))
        await userEvent.click(screen.getByTestId('tags-save'))

        await waitFor(() => expect(deleteFile).toHaveBeenCalledWith('p1', 'tags.properties'))
        expect(writeRootFile).not.toHaveBeenCalled()
        expect(onSaved).toHaveBeenCalled()
    })

    it('leaves the tags as they were when the editing is called off', async () => {
        render(<Host tags={{ Region: 'EU' }} />)

        await userEvent.click(screen.getByTestId('edit-tags'))
        await userEvent.click(screen.getByTestId('tags-cancel'))

        expect(screen.queryByTestId('project-tags-editor')).toBeNull()
        expect(writeRootFile).not.toHaveBeenCalled()
    })

    it('still edits and saves when the configured tags cannot be read', async () => {
        // The catalog only feeds the suggestions, so losing it must not block the file edit.
        vi.mocked(getTagTypes).mockRejectedValue(new Error('offline'))
        render(<Host tags={{ Region: 'EU' }} />)

        await userEvent.click(screen.getByTestId('edit-tags'))
        await userEvent.click(screen.getByTestId('tags-save'))

        await waitFor(() => expect(writeRootFile).toHaveBeenCalled())
    })

    it('warns about a mandatory tag type the project leaves without a value', async () => {
        vi.mocked(getTagTypes).mockResolvedValue([
            { name: 'Region', values: ['EU'], nullable: true, extensible: false },
            { name: 'Domain', values: ['Policy'], nullable: false, extensible: false },
        ] as never)
        render(<Host tags={{ Region: 'EU' }} />)

        // The project names no Domain at all, which is exactly the case to warn about.
        expect(await screen.findByTestId('tags-mandatory'))
            .toHaveTextContent('browser.tags.mandatory {"type":"Domain"}')

        await userEvent.click(screen.getByTestId('edit-tags'))
        expect(screen.getByTestId('tags-mandatory')).toBeInTheDocument()

        // Filling the type in clears the warning before anything is saved.
        await userEvent.click(screen.getByTestId('edit-tag-add'))
        await userEvent.type(screen.getByTestId('edit-tag-1'), 'domain')
        expect(screen.getByTestId('tags-mandatory')).toBeInTheDocument()
        await userEvent.type(screen.getByTestId('edit-tag-1-value'), 'Policy')

        expect(screen.queryByTestId('tags-mandatory')).toBeNull()
    })

    it('warns next to a value that a non-extensible tag type does not take', async () => {
        render(<Host tags={{ Region: 'Mars' }} />)

        expect(await screen.findByTestId('tag-not-allowed-Region'))
            .toHaveTextContent('browser.tags.not_allowed {"type":"Region","value":"Mars"}')

        await userEvent.click(screen.getByTestId('edit-tags'))
        expect(screen.getByTestId('tag-not-allowed-Region')).toBeInTheDocument()

        await userEvent.clear(within(screen.getByTestId('edit-tag-0-value')).getByRole('combobox'))
        await userEvent.type(screen.getByTestId('edit-tag-0-value'), 'US')

        expect(screen.queryByTestId('tag-not-allowed-Region')).toBeNull()
    })

    it('takes no value for a non-extensible tag type that has no values', async () => {
        // The catalog leaves the values out of a type that has none.
        vi.mocked(getTagTypes).mockResolvedValue([{ name: 'LOB', nullable: true, extensible: false }] as never)
        render(<Host tags={{ LOB: 'Auto' }} />)

        expect(await screen.findByTestId('tag-not-allowed-LOB'))
            .toHaveTextContent('browser.tags.not_allowed {"type":"LOB","value":"Auto"}')
    })

    it('takes a new value of an extensible tag type and any case of a configured value', async () => {
        vi.mocked(getTagTypes).mockResolvedValue([
            { name: 'Region', values: ['EU', 'US'], nullable: false, extensible: false },
            { name: 'Product', values: ['Auto'], nullable: true, extensible: true },
            { name: 'Domain', values: ['Policy'], nullable: false, extensible: false },
        ] as never)
        render(<Host tags={{ Product: 'Boat', region: 'eu' }} />)

        // The missing Domain shows that the tag types were read before the checks are looked at.
        const mandatory = await screen.findByTestId('tags-mandatory')
        expect(mandatory).toHaveTextContent('browser.tags.mandatory {"type":"Domain"}')
        expect(mandatory).not.toHaveTextContent('Region')
        expect(screen.queryByTestId('tag-not-allowed-Product')).toBeNull()
        expect(screen.queryByTestId('tag-not-allowed-region')).toBeNull()
    })

    it('keeps the tag types read before when reading them again fails', async () => {
        render(<Host tags={{ Region: 'Mars' }} />)
        expect(await screen.findByTestId('tag-not-allowed-Region')).toBeInTheDocument()

        vi.mocked(getTagTypes).mockRejectedValue(new Error('offline'))
        await userEvent.click(screen.getByTestId('edit-tags'))

        await waitFor(() => expect(getTagTypes).toHaveBeenCalledTimes(2))
        expect(screen.getByTestId('tag-not-allowed-Region')).toBeInTheDocument()
    })

    it('reads the configured tag types again when the editing starts', async () => {
        render(<Host tags={{ Region: 'Mars' }} />)
        expect(await screen.findByTestId('tag-not-allowed-Region')).toBeInTheDocument()

        // An administrator added the value meanwhile: the editor knows it without reloading the page.
        vi.mocked(getTagTypes).mockResolvedValue([
            { name: 'Region', values: ['EU', 'Mars'], nullable: true, extensible: false },
        ] as never)
        await userEvent.click(screen.getByTestId('edit-tags'))

        await waitFor(() => expect(screen.queryByTestId('tag-not-allowed-Region')).toBeNull())
        expect(getTagTypes).toHaveBeenCalledTimes(2)
    })

    it('saves the tags in spite of a warning', async () => {
        // The file is the source of truth: a project may carry what the configuration no longer allows.
        render(<Host tags={{ Region: 'Mars' }} />)

        await userEvent.click(screen.getByTestId('edit-tags'))
        expect(await screen.findByTestId('tag-not-allowed-Region')).toBeInTheDocument()
        await userEvent.click(screen.getByTestId('tags-save'))

        await waitFor(() => expect(writeRootFile).toHaveBeenCalledWith(
            'p1', 'tags.properties', 'Region=Mars\n', 'overwrite'
        ))
    })

    it('says when the file could not be written', async () => {
        vi.mocked(writeRootFile).mockRejectedValue(new Error('locked by someone'))
        render(<Host tags={{ Region: 'EU' }} />)

        await userEvent.click(screen.getByTestId('edit-tags'))
        await userEvent.click(screen.getByTestId('tags-save'))

        expect(await screen.findByTestId('tags-error')).toHaveTextContent('locked by someone')
        // The editor stays open, so nothing typed is lost.
        expect(screen.getByTestId('project-tags-editor')).toBeInTheDocument()
    })
})
