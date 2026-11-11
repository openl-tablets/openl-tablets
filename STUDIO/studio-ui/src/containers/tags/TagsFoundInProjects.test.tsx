import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'

import { TagTypesFoundInProjects } from './TagsFoundInProjects'
import type { FoundTagType } from './tagsInProjects'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

// The page reads the styles once and hands them down; their class names do not matter here.
const styles = new Proxy({}, { get: (_, name) => String(name) }) as never

const found: FoundTagType[] = [
    { type: 'Region', defined: true, values: [{ value: 'Asia', projects: 2 }]},
    { type: 'Product', defined: false, values: [{ value: 'Auto', projects: 1 }, { value: 'Home', projects: 3 }]},
]

const renderFound = (types = found) => {
    const onDefine = vi.fn()
    render(
        <MemoryRouter>
            <TagTypesFoundInProjects onDefine={onDefine} styles={styles} types={types} />
        </MemoryRouter>
    )
    return onDefine
}

describe('TagTypesFoundInProjects', () => {
    it('shows nothing when the projects carry only what is configured', () => {
        renderFound([])

        expect(screen.queryByTestId('tag-types-found')).not.toBeInTheDocument()
    })

    it('lists the values a configured tag type lacks, each opening its projects and added by its plus', async () => {
        const onDefine = renderFound()

        const chip = screen.getByTestId('found-tag-Region-Asia')
        expect(chip).toHaveTextContent('Asia2')
        expect(within(chip).getByRole('link')).toHaveAttribute('href', '/projects?tags=Region%3AAsia')

        await userEvent.click(screen.getByTestId('found-tag-add-Region-Asia'))
        expect(onDefine).toHaveBeenCalledWith('Region', ['Asia'])
    })

    it('offers to add a tag type only when it is not configured', async () => {
        const onDefine = renderFound()

        // Region exists already: there is no tag type to add, only its values.
        expect(screen.queryByTestId('found-type-add-Region')).not.toBeInTheDocument()

        await userEvent.click(screen.getByTestId('found-type-add-Product'))
        expect(onDefine).toHaveBeenCalledWith('Product', ['Auto', 'Home'])
    })
})
