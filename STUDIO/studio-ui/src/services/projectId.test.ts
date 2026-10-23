import { encodeProjectPath, moduleRoute, projectFileRoute } from './projectId'

describe('moduleRoute', () => {
    it('puts the project id in the address as the server issued it', () => {
        expect(moduleRoute('2QK5V769WQEQ8', 'Main Module', 'tbl-1', 'D101'))
            .toBe('/projects/2QK5V769WQEQ8/modules/Main%20Module?table=tbl-1&errorCell=D101')
    })
})

describe('projectFileRoute', () => {
    it('opens the Files tab on the file', () => {
        expect(projectFileRoute('2QK5V769WQEQ8', 'rules/Main.xlsx'))
            .toBe('/projects/2QK5V769WQEQ8?tab=files&file=rules%2FMain.xlsx')
    })
})

describe('encodeProjectPath', () => {
    it('keeps the separators and encodes each segment', () => {
        expect(encodeProjectPath('rules/My Module#1.xlsx')).toBe('rules/My%20Module%231.xlsx')
    })
})
