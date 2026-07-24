import { render, screen, waitFor, within } from '@testing-library/react'
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

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

const onSaved = vi.fn()

/** The hook hands back two pieces, which the overview places in one row; the test renders both. */
const Host = ({ canEdit = true, tags = {} }: { canEdit?: boolean, tags?: Record<string, string> }) => {
    const { action, content } = useProjectTags({ canEdit, onSaved, projectId: 'p1', tags })
    return <div>{action}{content}</div>
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

    it('reads the tags of the project as a table of type and value', () => {
        render(<Host tags={{ Region: 'EU' }} />)

        expect(screen.getByText('Region')).toBeInTheDocument()
        expect(screen.getByText('EU')).toBeInTheDocument()
        expect(screen.queryByTestId('project-tags-editor')).toBeNull()
    })

    it('shows every tag the file names, configured or not', () => {
        // The file is the source of truth: a tag no administrator configured is still the project's tag.
        render(<Host tags={{ 'Home-made': 'anything' }} />)

        expect(screen.getByText('Home-made')).toBeInTheDocument()
        expect(screen.getByText('anything')).toBeInTheDocument()
    })

    it('offers no way in for a user who may not write the project', () => {
        render(<Host canEdit={false} tags={{ Region: 'EU' }} />)

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
