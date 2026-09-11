import type { ReactNode } from 'react'
import {
    ApartmentOutlined,
    BlockOutlined,
    CaretRightOutlined,
    ColumnWidthOutlined,
    DatabaseOutlined,
    FileExcelOutlined,
    FileTextOutlined,
    FolderOutlined,
    FunctionOutlined,
    LayoutOutlined,
    TableOutlined,
} from '@ant-design/icons'

/**
 * The icon a table wears, chosen to read as the one the old Editor drew for it.
 *
 * The old tree used a small picture per table type: a cylinder for a Data table, `fx` for a Method, a green
 * triangle for a Run table, a grid of letters for a Decision table, a banded grid for a Spreadsheet. Each is
 * matched here by the drawn icon closest to it, so the tree reads the same to someone who knew the old one.
 */
const BY_KIND: Record<string, ReactNode> = {
    // dt3.png — a grid with the letters of its conditions.
    'Rules': <TableOutlined />,
    'Conditions': <TableOutlined />,
    'Actions': <TableOutlined />,
    'Returns': <TableOutlined />,
    // spreadsheet.gif — a grid under a banded header.
    'Spreadsheet': <LayoutOutlined />,
    'Constants': <LayoutOutlined />,
    // dataobject.gif — a small object box.
    'Datatype': <BlockOutlined />,
    // data.gif — a database cylinder.
    'Data': <DatabaseOutlined />,
    'Properties': <DatabaseOutlined />,
    // method.gif — `fx`.
    'Method': <FunctionOutlined />,
    'Test': <FunctionOutlined />,
    'Environment': <FunctionOutlined />,
    // run.gif — a play triangle.
    'Run': <CaretRightOutlined />,
    // tbasic.gif — a flow of steps.
    'TBasic': <ApartmentOutlined />,
    // cmatch.gif — a grid read by its columns.
    'Column Match': <ColumnWidthOutlined />,
}

/** The icon of a table, by the family it belongs to. */
export const tableIcon = (kind: string): ReactNode => BY_KIND[kind] ?? <FileTextOutlined />

/** The icon of a group, by what it gathers the tables under. */
export const groupIcon = (groupedBy: string | undefined): ReactNode =>
    // worksheet.gif — a plain grid, for the sheet a table is written on.
    groupedBy === 'sheet' ? <FileExcelOutlined /> : <FolderOutlined />
