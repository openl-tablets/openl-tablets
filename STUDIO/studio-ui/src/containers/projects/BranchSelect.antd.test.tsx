import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { BranchSelect } from './BranchSelect'

// Only AntD is left real here; the branch marks beside each name speak through i18n, which the suite does
// not boot.
vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

// BranchSelect.test.tsx stands in for AntD's Select to reach the component's own logic. What that stand-in
// cannot show is the dropdown AntD itself renders: left to its own devices it fills an empty list with its
// own empty state, which is what made a branch list still being read look like a repository without a
// single branch. These cases therefore run against the real Select.
describe('BranchSelect over the real AntD Select', () => {
    const openDropdown = async () => {
        await userEvent.click(screen.getByRole('combobox'))
        await waitFor(() => expect(document.querySelector('.ant-select-dropdown')).toBeInTheDocument())
        return document.querySelector('.ant-select-dropdown')!
    }

    it('spins in the list place while the branches are being read, instead of reading as No data', async () => {
        const { rerender } = render(<BranchSelect loading branchNames={[]} onChange={() => {}} value="" />)

        const dropdown = await openDropdown()
        expect(dropdown.querySelector('.ant-spin')).toBeInTheDocument()
        // AntD's own empty state, whatever it says in the active locale, must not be what stands there.
        expect(dropdown.querySelector('.ant-empty')).toBeNull()

        // The branches land: the spinner goes with them and the list offers what was read.
        rerender(<BranchSelect branchNames={['main', 'release']} onChange={() => {}} value="" />)

        await waitFor(() => expect(dropdown.querySelector('.ant-spin')).toBeNull())
        expect(dropdown.textContent).toContain('main')
    })

    it('keeps spinning in the field once the list already offers a branch', async () => {
        // The create and copy forms preselect the configured branch as soon as the repository settings
        // arrive, well before the branches themselves: the list is not empty, so nothing stands in its
        // place, and only the field can say that the rest are still on their way.
        const { container, rerender } = render(
            <BranchSelect allowNew loading branchNames={['main']} onChange={() => {}} value="main" />
        )

        const dropdown = await openDropdown()
        expect(dropdown.querySelector('.ant-spin')).toBeNull()
        expect(container.querySelector('.ant-select-suffix .anticon-loading')).toBeInTheDocument()

        // The rest of the branches land: the field goes back to its branch icon.
        rerender(<BranchSelect allowNew branchNames={['main', 'release']} onChange={() => {}} value="main" />)

        await waitFor(() => expect(container.querySelector('.ant-select-suffix .anticon-loading')).toBeNull())
        expect(container.querySelector('.ant-select-suffix .anticon-branches')).toBeInTheDocument()
        expect(dropdown.textContent).toContain('release')
    })

    it('leaves a settled empty list to AntD, which says there is nothing to offer', async () => {
        render(<BranchSelect branchNames={[]} onChange={() => {}} value="" />)

        const dropdown = await openDropdown()

        expect(dropdown.querySelector('.ant-spin')).toBeNull()
        expect(dropdown.querySelector('.ant-empty')).toBeInTheDocument()
    })
})
