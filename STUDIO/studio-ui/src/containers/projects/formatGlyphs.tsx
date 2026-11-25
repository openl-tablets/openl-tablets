import type { CSSProperties, ReactNode } from 'react'
import Icon from '@ant-design/icons'

// The signs Ant Design draws no icon for, drawn on its grid of 1024 units so they stand beside its own.

/** The gear of Ant Design, filled: drawn twice, small, an outline would be thinner than a pixel. */
const GEAR = 'M512.5 390.6c-29.9 0-57.9 11.6-79.1 32.8-21.1 21.2-32.8 49.2-32.8 79.1 0 29.9 11.7 57.9 32.8 79.1 21.2 21.1 49.2 32.8 79.1 32.8 29.9 0 57.9-11.7 79.1-32.8 21.1-21.2 32.8-49.2 32.8-79.1 0-29.9-11.7-57.9-32.8-79.1a110.96 110.96 0 00-79.1-32.8zm412.3 235.5l-65.4-55.9c3.1-19 4.7-38.4 4.7-57.7s-1.6-38.8-4.7-57.7l65.4-55.9a32.03 32.03 0 009.3-35.2l-.9-2.6a442.5 442.5 0 00-79.6-137.7l-1.8-2.1a32.12 32.12 0 00-35.1-9.5l-81.2 28.9c-30-24.6-63.4-44-99.6-57.5l-15.7-84.9a32.05 32.05 0 00-25.8-25.7l-2.7-.5c-52-9.4-106.8-9.4-158.8 0l-2.7.5a32.05 32.05 0 00-25.8 25.7l-15.8 85.3a353.44 353.44 0 00-98.9 57.3l-81.8-29.1a32 32 0 00-35.1 9.5l-1.8 2.1a445.93 445.93 0 00-79.6 137.7l-.9 2.6c-4.5 12.5-.8 26.5 9.3 35.2l66.2 56.5c-3.1 18.8-4.6 38-4.6 57 0 19.2 1.5 38.4 4.6 57l-66 56.5a32.03 32.03 0 00-9.3 35.2l.9 2.6c18.1 50.3 44.8 96.8 79.6 137.7l1.8 2.1a32.12 32.12 0 0035.1 9.5l81.8-29.1c29.8 24.5 63 43.9 98.9 57.3l15.8 85.3a32.05 32.05 0 0025.8 25.7l2.7.5a448.27 448.27 0 00158.8 0l2.7-.5a32.05 32.05 0 0025.8-25.7l15.7-84.9c36.2-13.6 69.6-32.9 99.6-57.5l81.2 28.9a32 32 0 0035.1-9.5l1.8-2.1c34.8-41.1 61.5-87.4 79.6-137.7l.9-2.6c4.3-12.4.6-26.3-9.5-35zm-412.3 52.2c-97.1 0-175.8-78.7-175.8-175.8s78.7-175.8 175.8-175.8 175.8 78.7 175.8 175.8-78.7 175.8-175.8 175.8z'

interface StrokesProps {
    width: number
    /** Whether the ends and the corners of the strokes are rounded, as the outlined icons of Ant Design draw them. */
    round?: boolean
    children: ReactNode
}

/** A sign drawn in strokes of the colour of the text around it. */
const Strokes = ({ width, round = true, children }: StrokesProps) => (
    <svg
        aria-hidden="true"
        fill="none"
        height="1em"
        stroke="currentColor"
        strokeLinecap={round ? 'round' : undefined}
        strokeLinejoin={round ? 'round' : undefined}
        strokeWidth={width}
        viewBox="0 0 1024 1024"
        width="1em"
    >
        {children}
    </svg>
)

/** Braces around a semicolon, as the code editors mark a JSON file. */
const JsonSvg = () => (
    <Strokes width={72}>
        <path d="M330 150c-90 0-110 50-110 130v110c0 60-30 122-110 122 80 0 110 62 110 122v110c0 80 20 130 110 130" />
        <path d="M694 150c90 0 110 50 110 130v110c0 60 30 122 110 122-80 0-110 62-110 122v110c0 80-20 130-110 130" />
        <circle cx="512" cy="392" fill="currentColor" r="40" stroke="none" />
        <path d="M512 612v30c0 50-20 90-60 120" />
    </Strokes>
)

/** Two gears in mesh, as a file of compiled code or data is usually marked: something a machine reads. */
const BinarySvg = () => (
    <svg aria-hidden="true" fill="currentColor" height="1em" viewBox="0 0 1024 1024" width="1em">
        <path d={GEAR} transform="translate(42 42) scale(0.66)" />
        <path d={GEAR} transform="translate(504 504) scale(0.5)" />
    </svg>
)

/** A character beside a letter, as the localization of an application is usually marked. */
const TranslateSvg = () => (
    <Strokes round={false} width={84}>
        <path d="M40 210H660M350 60V210M570 210C510 430 340 590 110 740M220 340C310 490 410 580 530 660" />
        <path d="M500 980L720 450L940 980M590 800H850" />
    </Strokes>
)

/** A lowercase `m`, as the code editors mark the `pom.xml` of a Maven project. */
const MavenSvg = () => (
    <Strokes round={false} width={100}>
        <path d="M170 880V340M170 500C170 400 235 340 335 340C435 340 512 400 512 500V880M512 500C512 400 589 340 689 340C789 340 854 400 854 500V880" />
    </Strokes>
)

/** Angle brackets around a slash, as markup and code are usually marked: `</>`. */
const TagSvg = () => (
    <Strokes width={88}>
        <path d="M330 250L80 512L330 774M694 250L944 512L694 774M600 160L424 864" />
    </Strokes>
)

/** A star with a sweep under it, after the star of the Groovy language. */
const GroovySvg = () => (
    <Strokes width={76}>
        <path d="M530 50L621 295L882 306L677 468L747 719L530 575L313 719L383 468L178 306L439 295Z" />
        <path d="M64 720C190 930 690 960 980 530" />
    </Strokes>
)

/** The letters `TS`, drawn as Ant Design draws `JS`: its `S`, and a `T` of the same weight. */
const TypeScriptSvg = () => (
    <svg aria-hidden="true" fill="currentColor" fillRule="evenodd" height="1em" viewBox="64 64 896 896" width="1em">
        <path d="M63 176H448V300H318V865H193V300H63Z" />
        <path d="M765.49 160C610.26 160 512 248.13 512 364.6c0 100.32 75.67 163.13 185.7 203.64 79.57 28.36 111.03 53.7 111.03 95.22 0 45.57-36.36 74.96-105.13 74.96-63.87 0-121.85-21.31-161.15-42.58v-.04L512 822.43C549.36 843.73 619.12 865 694.74 865 876.52 865 961 767.75 961 653.3c0-97.25-54.04-160.04-170.94-204.63-86.47-34.44-122.81-53.67-122.81-97.23 0-34.45 31.45-65.84 96.3-65.84 63.83 0 107.73 21.45 133.3 34.64l38.34-128.19C895.1 174.46 841.11 160 765.5 160" />
    </svg>
)

/** The shield of Ant Design's HTML5 icon around a `3`, after the sign of CSS. */
const Css3Svg = () => (
    <svg aria-hidden="true" fill="currentColor" height="1em" viewBox="64 64 896 896" width="1em">
        <path d="M145 96l66 746.6L511.8 928l299.6-85.4L878.7 96H145zm610.9 700.6l-244.1 69.6-245.2-69.6-56.7-641.2h603.8l-57.8 641.2z" />
        <path d="M340 300H680L520 470C625 468 690 525 690 610C690 700 620 760 512 760C432 760 372 735 330 700" fill="none" stroke="currentColor" strokeLinejoin="round" strokeWidth="90" />
    </svg>
)

interface GlyphProps {
    style?: CSSProperties
}

/** An icon of the sign drawn by the given drawing, sized and coloured as the icons of Ant Design are. */
const glyph = (drawing: () => ReactNode) => {
    const Glyph = (props: GlyphProps) => <Icon component={drawing} {...props} />
    return Glyph
}

/** The sign of a JSON file: `{;}`. */
export const JsonOutlined = glyph(JsonSvg)

/** The sign of a binary file: two gears. */
export const BinaryOutlined = glyph(BinarySvg)

/** The sign of the localization of the project: `文A`. */
export const TranslateOutlined = glyph(TranslateSvg)

/** The sign of the `pom.xml` of a Maven project: `m`. */
export const MavenOutlined = glyph(MavenSvg)

/** The sign of markup and code: `</>`. */
export const CodeTagOutlined = glyph(TagSvg)

/** The sign of a Groovy script: a star. */
export const GroovyOutlined = glyph(GroovySvg)

/** The sign of a TypeScript file: `TS`. */
export const TypeScriptOutlined = glyph(TypeScriptSvg)

/** The sign of a stylesheet: a shield with a `3`. */
export const Css3Outlined = glyph(Css3Svg)
