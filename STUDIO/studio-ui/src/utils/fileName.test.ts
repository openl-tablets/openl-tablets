import { describe, expect, it } from 'vitest'
import { isValidFileName } from './fileName'

describe('isValidFileName', () => {
    it.each(['Rates.xlsx', 'rules.xml', 'a b.txt', 'v1.2'])('takes %j', name => {
        expect(isValidFileName(name)).toBe(true)
    })

    it.each(['a\\b', 'a/b', 'a:b', 'a;b', 'a<b', 'a>b', 'a?b', 'a*b', 'a%b', "a'b", 'a[b', 'a]b', 'a|b', 'a"b',
        ' a', 'a ', 'a.'])('refuses %j', name => {
        expect(isValidFileName(name)).toBe(false)
    })
})
