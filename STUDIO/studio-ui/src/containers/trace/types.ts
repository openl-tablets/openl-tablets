export interface TraceNodeApi {
    key: number
    title: string
    tooltip?: string
    lazy?: boolean
    extraClasses?: string
}

export interface TraceDetails {
    inputParameters?: any
    resultParameters?: any
    spreadsheetSteps?: Array<{
        step: string
        description: string
        value: string
    }>
}

export interface FavoriteNode {
    key: number
    path: number[]
    title?: string
}
