import { walkDepthFirst, type Later } from 'utils/depthFirst'

interface Node {
    name: string
    children: Node[]
}

const node = (name: string, ...children: Node[]): Node => ({ name, children })

/** Lists the names on the way in and on the way out, as a recursive walk would. */
const visitOrder = (root: Node): string[] => {
    const seen: string[] = []
    walkDepthFirst(later => {
        const visit = (current: Node, queue: Later): void => {
            seen.push(`in ${current.name}`)
            queue(...current.children.map(child => () => visit(child, queue)), () => seen.push(`out ${current.name}`))
        }
        visit(root, later)
    })
    return seen
}

describe('walkDepthFirst', () => {
    it('visits the nodes in the order a recursive walk does, children before the step queued after them', () => {
        const tree = node('A', node('B', node('D')), node('C'))

        expect(visitOrder(tree)).toEqual(['in A', 'in B', 'in D', 'out D', 'out B', 'in C', 'out C', 'out A'])
    })

    it('walks a tree far deeper than the call stack allows', () => {
        let deepest = node('leaf')
        for (let i = 0; i < 100_000; i++) {
            deepest = node(`n${i}`, deepest)
        }

        expect(visitOrder(deepest)).toHaveLength(200_002)
    })
})
