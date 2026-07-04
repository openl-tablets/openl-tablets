import { formatMs } from 'utils/formatDuration'

describe('formatMs', () => {
    it('renders sub-millisecond and zero durations', () => {
        expect(formatMs(0)).toBe('0 ms')
        expect(formatMs(3.4)).toBe('3.4 ms')
    })

    it('rounds durations of ten milliseconds or more', () => {
        expect(formatMs(45.6)).toBe('46 ms')
        expect(formatMs(999)).toBe('999 ms')
    })

    it('switches to seconds at a thousand milliseconds', () => {
        expect(formatMs(1000)).toBe('1.0 s')
        expect(formatMs(1234)).toBe('1.2 s')
    })
})
