import { describe, expect, it } from 'vitest'
import { buildFileNodes } from './fileNodes'

describe('buildFileNodes', () => {
    it('derives the folders the paths run through', () => {
        const nodes = buildFileNodes(['rules/nested/Main.xlsx', 'rules.xml'])

        expect(nodes.map(node => node.path)).toEqual(['rules', 'rules.xml'])
        const [rules, descriptor] = nodes
        expect(descriptor!.isFile).toBe(true)
        expect(descriptor!.children).toBeUndefined()
        expect(rules!.isFile).toBe(false)
        expect(rules!.children?.map(node => node.path)).toEqual(['rules/nested'])
        expect(rules!.children?.[0]?.children).toEqual([
            { path: 'rules/nested/Main.xlsx', name: 'Main.xlsx', isFile: true },
        ])
    })

    it('reads folders before files, each in the order their paths read in', () => {
        const nodes = buildFileNodes(['b.xlsx', 'a.xlsx'], ['zzz', 'aaa'])

        expect(nodes.map(node => node.path)).toEqual(['aaa', 'zzz', 'a.xlsx', 'b.xlsx'])
    })

    it('keeps a folder that holds nothing yet', () => {
        const nodes = buildFileNodes([], ['drafts/wip'])

        expect(nodes[0]?.path).toBe('drafts')
        expect(nodes[0]?.children).toEqual([{ path: 'drafts/wip', name: 'wip', isFile: false, children: []}])
    })
})
