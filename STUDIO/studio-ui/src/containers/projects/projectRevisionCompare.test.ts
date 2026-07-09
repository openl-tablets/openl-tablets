import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getFileBlob } from '../../services/files'
import { getProjectFiles } from '../../services/repositories'
import {
    fileName,
    openComparePopup,
    openLegacyExcelCompare,
    writeCompareLoading,
} from '../../utils/legacyCompare'
import { listProjectRevisionExcelFiles, openProjectRevisionFileCompare } from './projectRevisionCompare'

vi.mock('../../services/files', () => ({
    getFileBlob: vi.fn(),
}))

vi.mock('../../services/repositories', () => ({
    getProjectFiles: vi.fn(),
}))

vi.mock('../../utils/legacyCompare', () => ({
    fileName: vi.fn(),
    isExcelFile: (path: string) => path.endsWith('.xlsx'),
    openComparePopup: vi.fn(),
    openLegacyExcelCompare: vi.fn(),
    writeCompareLoading: vi.fn(),
}))

describe('projectRevisionCompare', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('lists excel files present in both revisions', async () => {
        vi.mocked(getProjectFiles).mockImplementation(async (_projectId, _recursive, version) => {
            if (version === 'from') {
                return [
                    { path: 'rules/B.xlsx', name: 'B.xlsx', type: 'file', basePath: 'rules' },
                    { path: 'rules/A.xlsx', name: 'A.xlsx', type: 'file', basePath: 'rules' },
                    { path: 'rules/readme.txt', name: 'readme.txt', type: 'file', basePath: 'rules' },
                ]
            }
            return [
                { path: 'rules/A.xlsx', name: 'A.xlsx', type: 'file', basePath: 'rules' },
                { path: 'rules/C.xlsx', name: 'C.xlsx', type: 'file', basePath: 'rules' },
            ]
        })

        await expect(listProjectRevisionExcelFiles('p1', 'from', 'to')).resolves.toEqual(['rules/A.xlsx'])

        expect(getProjectFiles).toHaveBeenCalledWith('p1', true, 'from')
        expect(getProjectFiles).toHaveBeenCalledWith('p1', true, 'to')
    })

    it('opens a legacy excel compare popup for a shared file', async () => {
        const popup = { close: vi.fn() }
        const fromBlob = new Blob(['from'])
        const toBlob = new Blob(['to'])
        vi.mocked(openComparePopup).mockReturnValue(popup as never)
        vi.mocked(getFileBlob).mockImplementation(async (_projectId, path, version) => {
            if (version === 'from') {
                return fromBlob
            }
            return toBlob
        })
        vi.mocked(fileName).mockReturnValue('A.xlsx')
        vi.mocked(openLegacyExcelCompare).mockResolvedValue()

        await openProjectRevisionFileCompare('p1', 'rules/A.xlsx', 'from', 'to')

        expect(writeCompareLoading).toHaveBeenCalledWith(popup, 'rules/A.xlsx')
        expect(openLegacyExcelCompare).toHaveBeenCalledWith(
            popup,
            'rules/A.xlsx',
            fromBlob,
            toBlob,
            'from-A.xlsx',
            'to-A.xlsx'
        )
    })

    it('closes the popup when compare loading fails', async () => {
        const popup = { close: vi.fn() }
        vi.mocked(openComparePopup).mockReturnValue(popup as never)
        vi.mocked(getFileBlob).mockRejectedValue(new Error('network'))

        await expect(openProjectRevisionFileCompare('p1', 'rules/A.xlsx', 'from', 'to')).rejects.toThrow('network')
        expect(popup.close).toHaveBeenCalled()
    })
})
