import type { MockedFunction } from 'vitest'
import apiCall from './apiCall'
import { auditQueryParams, exportAuditEvents, getAuditEvents } from './audit'

vi.mock('./apiCall', () => ({ default: vi.fn() }))

const mockApiCall = apiCall as MockedFunction<typeof apiCall>

describe('audit service', () => {
    it('leaves out the criteria without a value and repeats the actions', () => {
        const params = auditQueryParams({
            from: '2026-10-01T00:00:00.000Z',
            actor: '  jdoe ',
            actions: ['signIn', 'auditLogRead'],
            authentication: 'token',
            target: ' ',
            clientAddress: '10.0.0.7',
        })

        expect(params.toString()).toBe('from=2026-10-01T00%3A00%3A00.000Z&actor=jdoe&action=signIn'
            + '&action=auditLogRead&authentication=token&clientAddress=10.0.0.7')
    })

    it('reads a page of events as JSON', async () => {
        const page = { content: [], pageNumber: 2, pageSize: 50, total: 100 }
        mockApiCall.mockResolvedValue(page)

        await expect(getAuditEvents({ outcome: 'denied' }, 2, 50)).resolves.toBe(page)

        expect(mockApiCall).toHaveBeenCalledWith('/admin/audit?outcome=denied&page=2&size=50',
            { headers: { Accept: 'application/json' } }, { throwError: true })
    })

    it('exports every matching event as a CSV file', async () => {
        const csv = new Blob(['time'])
        mockApiCall.mockResolvedValue(csv)

        await expect(exportAuditEvents({ subject: 'Analysts' })).resolves.toBe(csv)

        expect(mockApiCall).toHaveBeenCalledWith('/admin/audit?subject=Analysts&unpaged=true',
            { headers: { Accept: 'text/csv' } }, { throwError: true, responseType: 'blob' })
    })
})
