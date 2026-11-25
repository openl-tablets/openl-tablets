// The icon a file of a project wears, the same one wherever its name is drawn, chosen by the format of the file and,
// for a folder, by the part it plays in the project.

import type { ComponentType, CSSProperties } from 'react'
import type { GlobalToken } from 'antd'
import {
    CodeOutlined,
    ConsoleSqlOutlined,
    DatabaseOutlined,
    DockerOutlined,
    FileExcelOutlined,
    FileMarkdownOutlined,
    FilePdfOutlined,
    FilePptOutlined,
    FileTextOutlined,
    FileUnknownOutlined,
    FileWordOutlined,
    FileZipOutlined,
    FolderOutlined,
    FontSizeOutlined,
    Html5Outlined,
    JavaOutlined,
    JavaScriptOutlined,
    KeyOutlined,
    PictureOutlined,
    PythonOutlined,
    SettingOutlined,
    SoundOutlined,
    TableOutlined,
    TagOutlined,
    VideoCameraOutlined,
} from '@ant-design/icons'
import { formatOf, type FormatIcon } from '../../utils/fileFormats'
import { isMessageFilePath } from '../../services/localization'
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
import type { FolderRole } from './projectStructure'

/** The colours an icon is drawn in: the preset colours of the theme, and a muted grey. */
type Tone = 'blue' | 'cyan' | 'geekblue' | 'gold' | 'green' | 'magenta' | 'muted' | 'orange' | 'purple' | 'red' | 'volcano'

/** The colour of a tone in the theme of the screen that draws the icons. */
const colorOf = (tone: Tone, token: GlobalToken): string => (tone === 'muted' ? token.colorTextTertiary : token[tone])

type IconComponent = ComponentType<{ style?: CSSProperties }>

/** The icon and the colour of each format, as the office programs and the code editors draw them. */
const FORMAT_ICONS: Record<FormatIcon, readonly [IconComponent, Tone]> = {
    archive: [FileZipOutlined, 'gold'],
    audio: [SoundOutlined, 'magenta'],
    code: [CodeTagOutlined, 'cyan'],
    config: [SettingOutlined, 'blue'],
    database: [DatabaseOutlined, 'cyan'],
    docker: [DockerOutlined, 'blue'],
    executable: [BinaryOutlined, 'muted'],
    font: [FontSizeOutlined, 'geekblue'],
    groovy: [GroovyOutlined, 'blue'],
    html: [Html5Outlined, 'volcano'],
    image: [PictureOutlined, 'purple'],
    java: [JavaOutlined, 'orange'],
    javascript: [JavaScriptOutlined, 'gold'],
    json: [JsonOutlined, 'gold'],
    key: [KeyOutlined, 'volcano'],
    markdown: [FileMarkdownOutlined, 'geekblue'],
    maven: [MavenOutlined, 'red'],
    pdf: [FilePdfOutlined, 'red'],
    presentation: [FilePptOutlined, 'orange'],
    properties: [SettingOutlined, 'gold'],
    python: [PythonOutlined, 'blue'],
    script: [CodeOutlined, 'green'],
    spreadsheet: [FileExcelOutlined, 'green'],
    sql: [ConsoleSqlOutlined, 'cyan'],
    stylesheet: [Css3Outlined, 'blue'],
    table: [TableOutlined, 'green'],
    tag: [TagOutlined, 'magenta'],
    text: [FileTextOutlined, 'muted'],
    typescript: [TypeScriptOutlined, 'blue'],
    video: [VideoCameraOutlined, 'magenta'],
    word: [FileWordOutlined, 'blue'],
    xml: [CodeTagOutlined, 'orange'],
}

/**
 * The colour of each folder of the structure of a project, as the code editors mark the roots of a project: the
 * modules blue as the sources, the tests green, the code they run cyan, the libraries orange, and the messages indigo
 * as the sign of translation is usually drawn.
 */
const FOLDER_TONES: Record<FolderRole, Tone> = {
    rules: 'blue',
    tests: 'green',
    sources: 'cyan',
    libraries: 'orange',
    messages: 'geekblue',
}

/** An icon to draw and the colour to draw it in. */
export interface FileIcon {
    Icon: IconComponent
    color: string
}

/**
 * The icon of a file, by its format; a file of an unknown format wears a question mark. A localization file of the
 * project, a `message*.properties` file of `i18n`, wears the sign of translation, as the folder holding it does.
 *
 * @param path the path of the file in the project
 */
export const fileIconOf = (path: string, token: GlobalToken): FileIcon => {
    if (isMessageFilePath(path)) {
        return { Icon: TranslateOutlined, color: colorOf(FOLDER_TONES.messages, token) }
    }
    const format = formatOf(path)
    const [Icon, tone] = format ? FORMAT_ICONS[format.icon] : [FileUnknownOutlined, 'muted' as const]
    return { Icon, color: colorOf(tone, token) }
}

/**
 * The icon of a folder: coloured after the part it plays in the project, grey for any other folder. The folder of the
 * messages wears the sign of translation instead, as it holds the languages of the project rather than its code.
 */
export const folderIconOf = (role: FolderRole | undefined, token: GlobalToken): FileIcon => ({
    Icon: role === 'messages' ? TranslateOutlined : FolderOutlined,
    color: colorOf(role ? FOLDER_TONES[role] : 'muted', token),
})
