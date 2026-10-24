import type { ReactNode } from 'react'
import { render, screen } from '@testing-library/react'
import type { DragEndEvent } from '@dnd-kit/core'
import { describe, expect, it, vi } from 'vitest'

import { TagTable, type TagType } from './TagTable'

// A drag cannot be performed in jsdom, so the test ends one the way the drag context would.
let endDrag: (event: DragEndEvent) => void = () => {}
vi.mock('@dnd-kit/core', async () => ({
    ...await vi.importActual('@dnd-kit/core'),
    DndContext: ({ children, onDragEnd }: { children: ReactNode, onDragEnd: (event: DragEndEvent) => void }) => {
        endDrag = onDragEnd
        return <>{children}</>
    },
}))

vi.mock('react-i18next', () => {
    const t = (key: string, options?: Record<string, string>) => `${key}${options?.['name'] ?? ''}`
    return { useTranslation: () => ({ t }) }
})

const tagType = (id: number, name: string): TagType => ({ id, name, extensible: false, nullable: true, tags: []})

const types = [tagType(1, 'Domain'), tagType(2, 'LOB'), tagType(3, 'Region')]

const renderTable = (reorderTagTypes = vi.fn(), isLoading = false) => {
    render(
        <TagTable
            createTag={vi.fn()}
            deleteTag={vi.fn()}
            deleteTagType={vi.fn()}
            isLoading={isLoading}
            reorderTagTypes={reorderTagTypes}
            tagTypes={types}
            updateTag={vi.fn()}
            updateTagType={vi.fn()}
        />
    )
    return reorderTagTypes
}

const drag = (activeId: number, overId: number | null) => endDrag({
    active: { id: activeId },
    over: overId === null ? null : { id: overId },
} as unknown as DragEndEvent)

describe('TagTable', () => {
    it('offers a handle to drag each tag type by', () => {
        renderTable()

        expect(screen.getByTestId('tag-type-drag-Domain')).toHaveAccessibleName('tags:reorderDomain')
        expect(screen.getByTestId('tag-type-drag-Region')).toBeInTheDocument()
    })

    it('lets no tag type be moved while the table is loading', () => {
        renderTable(vi.fn(), true)

        // A change being stored would otherwise race the next one.
        expect(screen.getByTestId('tag-type-drag-Domain')).toHaveAttribute('aria-disabled', 'true')
    })

    it('hands back every tag type in the order the drag left them', () => {
        const reorderTagTypes = renderTable()

        drag(3, 1)

        expect(reorderTagTypes).toHaveBeenCalledWith([types[2], types[0], types[1]])
    })

    it('changes nothing when a tag type is dropped where it was or outside the table', () => {
        const reorderTagTypes = renderTable()

        drag(2, 2)
        drag(2, null)

        expect(reorderTagTypes).not.toHaveBeenCalled()
    })
})
