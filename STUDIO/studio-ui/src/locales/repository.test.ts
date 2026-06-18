import i18n from '../i18n'
import './repository.en'

describe('repository locale', () => {
    it('does not HTML-escape interpolated branch names in the delete-success message', () => {
        const text = i18n.t('repository:notifications.branch_deleted_description', {
            branch: 'Example1-BankRating/admin/20260617',
        })

        expect(text).toContain('Example1-BankRating/admin/20260617')
        expect(text).not.toContain('&#x2F;')
    })
})
