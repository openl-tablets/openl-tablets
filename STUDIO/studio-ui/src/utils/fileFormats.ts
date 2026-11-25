/**
 * The well-known file formats: how a file of a project is shown, and which icon it wears.
 *
 * <p>A format is known by the whole name of the file, as `Dockerfile`, `LICENSE` or `pom.xml`, or else by the extension
 * of the name. A name that starts with a dot and has no other, as `.gitignore`, is known by what follows the dot.
 *
 * <p>A file of a format not listed here is of an unknown format: it is shown as a binary file, and a small one may be
 * opened as text on request.
 *
 * @author Yury Molchan
 */

/** How a file is shown: as text, as a picture the browser draws, or as bytes to download. */
export type FileKind = 'text' | 'image' | 'binary'

/** The icon a format wears, which also tells what the file holds. */
export type FormatIcon =
    | 'archive'
    | 'audio'
    | 'code'
    | 'config'
    | 'database'
    | 'docker'
    | 'executable'
    | 'font'
    | 'groovy'
    | 'html'
    | 'image'
    | 'java'
    | 'javascript'
    | 'json'
    | 'key'
    | 'markdown'
    | 'maven'
    | 'pdf'
    | 'presentation'
    | 'properties'
    | 'python'
    | 'script'
    | 'spreadsheet'
    | 'sql'
    | 'stylesheet'
    | 'table'
    | 'tag'
    | 'text'
    | 'typescript'
    | 'video'
    | 'word'
    | 'xml'

export interface FileFormat {
    kind: FileKind
    icon: FormatIcon
}

/** The extensions of each kind of file, by the icon they wear. */
export const FORMATS: Readonly<Record<FileKind, Partial<Record<FormatIcon, readonly string[]>>>> = {
    /** The pictures a browser draws itself. */
    image: {
        image: ['png', 'apng', 'jpg', 'jpeg', 'jpe', 'jfif', 'gif', 'svg', 'webp', 'avif', 'bmp', 'ico', 'cur'],
    },
    /** The formats kept as bytes: office documents, archives, media, compiled code and data stores, and the pictures a browser does not draw. */
    binary: {
        spreadsheet: ['xlsx', 'xls', 'xlsm', 'xlsb', 'xltx', 'xltm', 'xlt', 'ods', 'numbers'],
        word: ['doc', 'docx', 'docm', 'dot', 'dotx', 'dotm', 'odt', 'pages'],
        presentation: ['ppt', 'pptx', 'pptm', 'pps', 'ppsx', 'potx', 'odp', 'key'],
        pdf: ['pdf'],
        archive: [
            'zip',
            'jar',
            'war',
            'ear',
            'aar',
            'nar',
            '7z',
            'rar',
            'gz',
            'tgz',
            'tar',
            'bz2',
            'tbz2',
            'xz',
            'txz',
            'zst',
            'lz',
            'lzma',
            'lz4',
            'z',
            'cab',
            'arj',
        ],
        image: ['tif', 'tiff', 'psd', 'ai', 'eps', 'heic', 'heif', 'raw', 'cr2', 'nef', 'dng', 'xcf', 'jxl'],
        audio: ['mp3', 'wav', 'ogg', 'oga', 'flac', 'aac', 'm4a', 'wma', 'aiff', 'mid', 'midi', 'opus'],
        video: ['mp4', 'm4v', 'mov', 'avi', 'mkv', 'webm', 'wmv', 'flv', 'mpg', 'mpeg', '3gp', 'ogv'],
        font: ['ttf', 'otf', 'woff', 'woff2', 'eot'],
        database: ['db', 'sqlite', 'sqlite3', 'mdb', 'accdb', 'dbf', 'h2', 'frm', 'ibd', 'parquet', 'avro', 'orc'],
        key: ['jks', 'jceks', 'keystore', 'truststore', 'p12', 'pfx', 'der', 'p7b', 'kdbx', 'gpg'],
        executable: [
            'exe',
            'dll',
            'so',
            'dylib',
            'bin',
            'class',
            'o',
            'obj',
            'a',
            'lib',
            'msi',
            'dmg',
            'iso',
            'img',
            'apk',
            'aab',
            'ipa',
            'deb',
            'rpm',
            'pkg',
            'dat',
            'ser',
            'pyc',
            'pyo',
            'wasm',
            'swf',
            'sys',
            'com',
            'elf',
        ],
    },
    /** The formats written as text, which the file editor opens. */
    text: {
        text: [
            'txt',
            'text',
            'log',
            'out',
            'nfo',
            'asc',
            'rtf',
            'tex',
            'bib',
            'srt',
            'vtt',
            'diff',
            'patch',
            'po',
            'pot',
            'adoc',
            'asciidoc',
            'rst',
            'textile',
            'org',
            'gitignore',
            'gitattributes',
            'gitmodules',
            'gitkeep',
            'dockerignore',
            'npmignore',
            'eslintignore',
            'prettierignore',
            'mailmap',
            'lock',
            'sum',
        ],
        markdown: ['md', 'markdown', 'mdown', 'mkd', 'mdx'],
        table: ['csv', 'tsv', 'psv'],
        java: ['java', 'jsp', 'jspx', 'jav'],
        javascript: ['js', 'jsx', 'mjs', 'cjs'],
        typescript: ['ts', 'tsx', 'mts', 'cts'],
        python: ['py', 'pyw', 'pyi'],
        groovy: ['groovy', 'gvy', 'gy', 'gsh'],
        code: [
            'gradle',
            'kt',
            'kts',
            'scala',
            'sc',
            'sbt',
            'clj',
            'cljs',
            'go',
            'rs',
            'c',
            'h',
            'cpp',
            'cc',
            'cxx',
            'hpp',
            'hh',
            'cs',
            'vb',
            'fs',
            'swift',
            'm',
            'mm',
            'rb',
            'php',
            'pl',
            'pm',
            'r',
            'lua',
            'dart',
            'ex',
            'exs',
            'erl',
            'hs',
            'ml',
            'elm',
            'vue',
            'svelte',
            'graphql',
            'gql',
            'proto',
            'thrift',
            'drl',
            'dsl',
            'ftl',
            'vm',
            'mustache',
            'hbs',
            'handlebars',
            'twig',
            'j2',
            'jinja',
            'tpl',
            'http',
            'rest',
        ],
        script: ['sh', 'bash', 'zsh', 'ksh', 'csh', 'fish', 'cmd', 'bat', 'ps1', 'psm1', 'psd1', 'awk', 'sed'],
        html: ['html', 'htm', 'xhtml', 'shtml'],
        stylesheet: ['css', 'scss', 'sass', 'less', 'styl'],
        xml: ['xml', 'xsd', 'xsl', 'xslt', 'wsdl', 'dtd', 'pom', 'plist', 'xaml', 'fxml', 'rss', 'atom', 'xmi'],
        json: ['json', 'jsonc', 'json5', 'jsonl', 'ndjson', 'avsc', 'ipynb'],
        config: [
            'yaml',
            'yml',
            'toml',
            'ini',
            'cfg',
            'conf',
            'config',
            'env',
            'hcl',
            'tf',
            'tfvars',
            'editorconfig',
            'npmrc',
            'nvmrc',
            'yarnrc',
            'babelrc',
            'eslintrc',
            'prettierrc',
            'browserslistrc',
            'openapi',
        ],
        properties: ['properties'],
        sql: ['sql', 'ddl', 'dml', 'psql', 'pgsql', 'plsql', 'hql', 'cql'],
        key: ['pem', 'crt', 'cer', 'csr', 'pub'],
    },
}

/**
 * The files known by their whole name, whatever their extension says: build and container files, the notices, the
 * build of a Maven project and the tags of a project. All of them are text.
 */
export const NAMED_FORMATS: Readonly<Record<string, FormatIcon>> = {
    dockerfile: 'docker',
    containerfile: 'docker',
    makefile: 'code',
    gnumakefile: 'code',
    jenkinsfile: 'code',
    vagrantfile: 'code',
    gemfile: 'code',
    rakefile: 'code',
    procfile: 'config',
    mvnw: 'script',
    gradlew: 'script',
    license: 'text',
    licence: 'text',
    notice: 'text',
    readme: 'text',
    changelog: 'text',
    changes: 'text',
    authors: 'text',
    contributors: 'text',
    copying: 'text',
    todo: 'text',
    codeowners: 'text',
    'pom.xml': 'maven',
    'tags.properties': 'tag',
}

/** The largest file of an unknown format that may be opened as text: 10 KB. */
export const TEXT_PREVIEW_LIMIT = 10 * 1024

const BY_EXTENSION: ReadonlyMap<string, FileFormat> = new Map(
    Object.entries(FORMATS).flatMap(([kind, groups]) => Object.entries(groups).flatMap(([icon, extensions]) =>
        (extensions ?? []).map(extension => [extension, { kind: kind as FileKind, icon: icon as FormatIcon }] as const)))
)

/** The name of the file at a path written with either separator: what follows its last separator. */
export const fileName = (path: string): string => path.slice(Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\')) + 1)

/**
 * The format of the file at a path, or undefined for a format not known.
 *
 * <p>The name is read in any case: `Rules.XLSX` is a workbook.
 */
export const formatOf = (path: string): FileFormat | undefined => {
    const name = fileName(path).toLowerCase()
    // Only the names listed: a file named `constructor` is not the constructor of the list.
    const named = Object.hasOwn(NAMED_FORMATS, name) ? NAMED_FORMATS[name] : undefined
    if (named) {
        return { kind: 'text', icon: named }
    }
    const dot = name.lastIndexOf('.')
    return dot < 0 ? undefined : BY_EXTENSION.get(name.slice(dot + 1))
}

/** How the file at a path is shown, or undefined for a format not known. */
export const kindOf = (path: string): FileKind | undefined => formatOf(path)?.kind
