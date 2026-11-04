import { joinClasses } from './joinClasses'

describe('joinClasses', () => {
    it('joins the names given by spaces, skipping the ones left out', () => {
        expect(joinClasses('cell', false, 'picked', undefined, null, 'raised')).toBe('cell picked raised')
    })

    it('joins nothing into an empty name', () => {
        expect(joinClasses(false, undefined)).toBe('')
    })
})
