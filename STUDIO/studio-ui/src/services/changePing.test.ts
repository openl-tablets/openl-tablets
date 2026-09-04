import { describe, expect, it } from 'vitest'
import { isOwnEcho, parseChangePing } from './changePing'
import { CLIENT_ID } from './clientId'

describe('parseChangePing', () => {
    it('reads the files and the origins the backend named', () => {
        expect(parseChangePing(JSON.stringify({ files: ['rules/A.xlsx'], origins: ['tab-1']}), 'project'))
            .toEqual({ files: ['rules/A.xlsx'], origins: ['tab-1'], scope: 'project' })
    })

    it('reads a ping that carries origins alone — the workspace ping and the broadcast', () => {
        expect(parseChangePing(JSON.stringify({ origins: ['tab-1']}), 'workspace'))
            .toEqual({ files: [], origins: ['tab-1'], scope: 'workspace' })
    })

    it('reads a malformed body as a change of unknown files and unknown origin', () => {
        // Never an echo, so nothing is dropped on a body the client cannot understand.
        expect(parseChangePing('not json', 'project')).toEqual({ files: [], origins: [], scope: 'project' })
        expect(parseChangePing(JSON.stringify({ files: 'all', origins: 7 }), 'project'))
            .toEqual({ files: [], origins: [], scope: 'project' })
    })
})

describe('isOwnEcho', () => {
    it('recognises a ping this tab caused on its own', () => {
        expect(isOwnEcho({ files: [], origins: [CLIENT_ID], scope: 'project' })).toBe(true)
    })

    it('keeps a ping that stands for another session as well', () => {
        // One ping can coalesce the changes of several clients; dropping it would lose the others.
        expect(isOwnEcho({ files: [], origins: [CLIENT_ID, 'another-tab'], scope: 'project' })).toBe(false)
    })

    it('keeps a ping made outside a request, which belongs to nobody', () => {
        expect(isOwnEcho({ files: [], origins: [], scope: 'project' })).toBe(false)
    })
})
