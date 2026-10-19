import { encodeProjectPath, moduleRoute, projectFileRoute } from './projectId'

describe('moduleRoute', () => {
    it('puts the project id in the address as the server issued it', () => {
        expect(moduleRoute('FeZdnMnl3XRa', 'Main Module', 'tbl-1', 'D101'))
            .toBe('/projects/FeZdnMnl3XRa/modules/Main%20Module?table=tbl-1&errorCell=D101')
    })
})

describe('projectFileRoute', () => {
    it('opens the Files tab on the file', () => {
        expect(projectFileRoute('FeZdnMnl3XRa', 'rules/Main.xlsx'))
            .toBe('/projects/FeZdnMnl3XRa?tab=files&file=rules%2FMain.xlsx')
    })
})

describe('encodeProjectPath', () => {
    it('keeps the separators and encodes each segment', () => {
        expect(encodeProjectPath('rules/My Module#1.xlsx')).toBe('rules/My%20Module%231.xlsx')
    })
})
