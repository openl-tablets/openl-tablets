import { describe, expect, it } from 'vitest'
import type { GlobalToken } from 'antd'
import {
    CodeOutlined,
    FileExcelOutlined,
    FileTextOutlined,
    FileUnknownOutlined,
    FileZipOutlined,
    FolderOutlined,
    JavaOutlined,
    PictureOutlined,
    SettingOutlined,
    TableOutlined,
    TagOutlined,
} from '@ant-design/icons'
import { fileIconOf, folderIconOf } from './fileIcons'
import {
    BinaryOutlined,
    CodeTagOutlined,
    Css3Outlined,
    GroovyOutlined,
    JsonOutlined,
    MavenOutlined,
    TranslateOutlined,
    TypeScriptOutlined,
} from './formatGlyphs'

const palette = {
    blue: 'blue',
    cyan: 'cyan',
    geekblue: 'geekblue',
    gold: 'gold',
    green: 'green',
    magenta: 'magenta',
    colorTextTertiary: 'grey',
    orange: 'orange',
    purple: 'purple',
    red: 'red',
    volcano: 'volcano',
} as GlobalToken

describe('fileIcons', () => {
    it('draws a file with the icon and the colour of its format', () => {
        expect(fileIconOf('Main.xlsx', palette)).toEqual({ Icon: FileExcelOutlined, color: 'green' })
        expect(fileIconOf('Rate.java', palette)).toEqual({ Icon: JavaOutlined, color: 'orange' })
        expect(fileIconOf('lib.zip', palette)).toEqual({ Icon: FileZipOutlined, color: 'gold' })
        expect(fileIconOf('rules.xml', palette)).toEqual({ Icon: CodeTagOutlined, color: 'orange' })
        expect(fileIconOf('openapi.json', palette)).toEqual({ Icon: JsonOutlined, color: 'gold' })
        expect(fileIconOf('data.csv', palette)).toEqual({ Icon: TableOutlined, color: 'green' })
        expect(fileIconOf('notes.txt', palette)).toEqual({ Icon: FileTextOutlined, color: 'grey' })
        expect(fileIconOf('application.properties', palette)).toEqual({ Icon: SettingOutlined, color: 'gold' })
        // A configuration file wears the gear too, in blue.
        expect(fileIconOf('config.yaml', palette)).toEqual({ Icon: SettingOutlined, color: 'blue' })
        expect(fileIconOf('pyproject.toml', palette)).toEqual({ Icon: SettingOutlined, color: 'blue' })
        expect(fileIconOf('setup.exe', palette)).toEqual({ Icon: BinaryOutlined, color: 'grey' })
    })

    it('draws Groovy, TypeScript and CSS with their signs, code with </> and a script with a terminal', () => {
        expect(fileIconOf('scripts/Rules.groovy', palette)).toEqual({ Icon: GroovyOutlined, color: 'blue' })
        expect(fileIconOf('build.gradle', palette)).toEqual({ Icon: CodeTagOutlined, color: 'cyan' })
        expect(fileIconOf('web/app.ts', palette)).toEqual({ Icon: TypeScriptOutlined, color: 'blue' })
        expect(fileIconOf('web/app.css', palette)).toEqual({ Icon: Css3Outlined, color: 'blue' })
        expect(fileIconOf('bin/start.sh', palette)).toEqual({ Icon: CodeOutlined, color: 'green' })
        expect(fileIconOf('bin/start.cmd', palette).Icon).toBe(CodeOutlined)
    })

    it('knows the build of a Maven project and the tags of the project by their whole name', () => {
        expect(fileIconOf('pom.xml', palette)).toEqual({ Icon: MavenOutlined, color: 'red' })
        expect(fileIconOf('module/POM.xml', palette).Icon).toBe(MavenOutlined)
        expect(fileIconOf('tags.properties', palette)).toEqual({ Icon: TagOutlined, color: 'magenta' })
        // Any other XML file is drawn as markup.
        expect(fileIconOf('rules-deploy.xml', palette)).toEqual({ Icon: CodeTagOutlined, color: 'orange' })
    })

    it('draws every picture alike, whatever its format', () => {
        for (const name of ['logo.png', 'photo.jpg', 'photo.jpeg', 'spinner.gif', 'diagram.svg', 'scan.tiff']) {
            expect(fileIconOf(name, palette)).toEqual({ Icon: PictureOutlined, color: 'purple' })
        }
    })

    it('marks the localization files of the project with the sign of translation', () => {
        expect(fileIconOf('i18n/message.properties', palette)).toEqual({ Icon: TranslateOutlined, color: 'geekblue' })
        expect(fileIconOf('i18n/message_de_DE.properties', palette).Icon).toBe(TranslateOutlined)
        // A properties file anywhere else, or not named as a message file, is a properties file.
        expect(fileIconOf('rules/message.properties', palette).Icon).toBe(SettingOutlined)
        expect(fileIconOf('i18n/labels.properties', palette).Icon).toBe(SettingOutlined)
    })

    it('marks a file of an unknown format with a question', () => {
        expect(fileIconOf('notes.rates', palette)).toEqual({ Icon: FileUnknownOutlined, color: 'grey' })
    })

    it('colours a folder after the part it plays in the project, and leaves any other grey', () => {
        expect(folderIconOf('rules', palette)).toEqual({ Icon: FolderOutlined, color: 'blue' })
        expect(folderIconOf('tests', palette).color).toBe('green')
        expect(folderIconOf('sources', palette).color).toBe('cyan')
        expect(folderIconOf('libraries', palette).color).toBe('orange')
        expect(folderIconOf('libraries', palette).Icon).toBe(FolderOutlined)
        // The messages wear the sign of translation, not a folder.
        expect(folderIconOf('messages', palette)).toEqual({ Icon: TranslateOutlined, color: 'geekblue' })
        expect(folderIconOf(undefined, palette).color).toBe('grey')
    })
})
