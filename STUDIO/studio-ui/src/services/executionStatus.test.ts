import { isFinished, readStatus } from './executionStatus'

describe('readStatus', () => {
    it('reads a bare status', () => {
        expect(readStatus('COMPLETED')).toEqual({ status: 'COMPLETED' })
    })

    it('reads a status quoted as JSON text', () => {
        expect(readStatus('"STARTED"')).toEqual({ status: 'STARTED' })
    })

    it('reads a failure with its reason', () => {
        expect(readStatus('{"status":"ERROR","message":"Cannot read the file"}'))
            .toEqual({ status: 'ERROR', message: 'Cannot read the file' })
    })

    it('says nothing about a frame it does not understand', () => {
        expect(readStatus('null')).toEqual({})
        expect(readStatus('42')).toEqual({})
        expect(readStatus('WHATEVER')).toEqual({})
        expect(readStatus('{"status":"WHATEVER"}')).toEqual({})
        expect(readStatus('')).toEqual({})
    })
})

describe('isFinished', () => {
    it('knows the statuses work stops at', () => {
        expect(isFinished('COMPLETED')).toBe(true)
        expect(isFinished('ERROR')).toBe(true)
        expect(isFinished('INTERRUPTED')).toBe(true)
        expect(isFinished('STARTED')).toBe(false)
        expect(isFinished('PENDING')).toBe(false)
        expect(isFinished(null)).toBe(false)
    })
})
