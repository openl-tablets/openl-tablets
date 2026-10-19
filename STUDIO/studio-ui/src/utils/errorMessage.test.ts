import { errorMessage, errorMessageOr } from './errorMessage'

describe('errorMessage', () => {
    it('reads the message of an error, and writes any other value as text', () => {
        expect(errorMessage(new Error('Forbidden'))).toBe('Forbidden')
        expect(errorMessage('timeout')).toBe('timeout')
    })
})

describe('errorMessageOr', () => {
    it('reads the message of an error', () => {
        expect(errorMessageOr(new Error('Forbidden'), 'Step failed')).toBe('Forbidden')
    })

    it('falls back for an error without a message and for a value that is no error', () => {
        expect(errorMessageOr(new Error(''), 'Step failed')).toBe('Step failed')
        expect(errorMessageOr({ message: 'not an error' }, 'Step failed')).toBe('Step failed')
        expect(errorMessageOr(undefined, 'Step failed')).toBe('Step failed')
    })
})
