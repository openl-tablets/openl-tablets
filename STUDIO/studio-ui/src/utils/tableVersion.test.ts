import { formatLocalDateTime } from './dateFormat'
import { dimensionValue, versionLabel } from './tableVersion'

describe('tableVersion', () => {
    it('names a table written in one version as it is', () => {
        expect(versionLabel('carPrice')).toBe('carPrice')
        expect(versionLabel('carPrice', {})).toBe('carPrice')
    })

    it('names a version by the dimensions that tell it from the others, in the order they are given', () => {
        const effective = '2020-07-01T10:30:00.000'

        expect(versionLabel('carPrice', { effectiveDate: effective, state: ['AR']}))
            .toBe(`carPrice [effectiveDate=${formatLocalDateTime(effective)}, state=AR]`)
    })

    it('writes several values of one dimension separated by commas', () => {
        expect(dimensionValue(['AR', 'AZ'])).toBe('AR,AZ')
        expect(dimensionValue(true)).toBe('true')
    })

    it('writes a date in the format of the dates, with no time of day at midnight', () => {
        expect(dimensionValue('2020-01-01T00:00:00.000')).toBe('Jan 1, 2020')
        expect(dimensionValue('2020-07-01T10:30:00.000')).toMatch(/^Jul 1, 2020 10:30:00\sAM$/)
    })

    it('leaves a text that only looks like a date part as it is', () => {
        expect(dimensionValue('2020-07-01')).toBe('2020-07-01')
    })
})
