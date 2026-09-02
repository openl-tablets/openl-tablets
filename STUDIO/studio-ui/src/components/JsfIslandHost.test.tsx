import { act, render, screen, within } from '@testing-library/react'
import { JsfIslandHost } from './JsfIslandHost'

vi.mock('./islandRegistry', () => ({
    ISLAND_REGISTRY: {
        'test-island': (dataset: DOMStringMap) => (
            <div data-testid="island">{dataset['projectId']}</div>
        ),
    },
}))

// A DOM mutation triggers the host's MutationObserver, which delivers records on a
// microtask and updates React state. Both must happen inside act(...).
const mutate = (change: () => void) =>
    act(async () => {
        change()
        await new Promise((resolve) => setTimeout(resolve, 0))
    })

const addIsland = async (island: string, data: Record<string, string> = {}): Promise<HTMLElement> => {
    const node = document.createElement('div')
    node.dataset['island'] = island
    Object.entries(data).forEach(([key, value]) => { node.dataset[key] = value })
    await mutate(() => document.body.appendChild(node))
    return node
}

const removeAll = () =>
    mutate(() => document.querySelectorAll('[data-island]').forEach((node) => node.remove()))

describe('JsfIslandHost', () => {
    afterEach(async () => {
        if (document.querySelector('[data-island]')) {
            await removeAll()
        }
    })

    it('renders nothing until a registered placeholder appears', () => {
        render(<JsfIslandHost />)
        expect(screen.queryByTestId('island')).toBeNull()
    })

    it('ignores placeholders with an unregistered island name', async () => {
        render(<JsfIslandHost />)
        await addIsland('unknown-island', { projectId: 'X' })
        expect(screen.queryByTestId('island')).toBeNull()
    })

    it('portals the registered component into the placeholder with its data-* props', async () => {
        render(<JsfIslandHost />)
        const node = await addIsland('test-island', { projectId: 'my-project' })

        const island = within(node).getByTestId('island')
        expect(island).toHaveTextContent('my-project')
    })

    it('drops the portal when the placeholder leaves the DOM', async () => {
        render(<JsfIslandHost />)
        await addIsland('test-island', { projectId: 'my-project' })
        expect(screen.getByTestId('island')).toBeInTheDocument()

        await removeAll()

        expect(screen.queryByTestId('island')).toBeNull()
    })

    it('remounts when the placeholder is replaced for another project', async () => {
        render(<JsfIslandHost />)
        await addIsland('test-island', { projectId: 'first' })
        expect(screen.getByTestId('island')).toHaveTextContent('first')

        await removeAll()
        await addIsland('test-island', { projectId: 'second' })

        expect(screen.getByTestId('island')).toHaveTextContent('second')
    })

    it('portals islands nested inside an observed region (#leftContent under #center), not via the body fallback', async () => {
        // #center exists before the host mounts, so it is picked up as an observed region — not via the
        // whole-document fallback. #leftContent/#content/#rightContent nest under it and are reached
        // through subtree observation.
        const center = document.createElement('div')
        center.id = 'center'
        const leftContent = document.createElement('div')
        leftContent.id = 'leftContent'
        center.appendChild(leftContent)
        document.body.appendChild(center)
        try {
            render(<JsfIslandHost />)
            const node = document.createElement('div')
            node.dataset['island'] = 'test-island'
            node.dataset['projectId'] = 'in-left'
            await mutate(() => leftContent.appendChild(node))
            expect(within(node).getByTestId('island')).toHaveTextContent('in-left')
        } finally {
            await mutate(() => center.remove())
        }
    })
})
