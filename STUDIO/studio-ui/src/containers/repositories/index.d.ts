import { AWS_SSE_ALGORITHM, RepositoryType } from './constants'
import { FormInstance } from 'antd'
import { InputTextField } from '../../global'

export interface DefaultSettings {
    basePath?: string
    commentTemplate: string
    commentTemplateOld?: string
    commentValidationPattern?: string
    defaultCommentArchive?: string
    defaultCommentCopiedFrom?: string
    defaultCommentCreate?: string
    defaultCommentErase?: string
    defaultCommentRestore?: string
    defaultCommentRestoredFrom?: string
    defaultCommentSave?: string
    invalidCommentMessage: string
    mainBranchOnly?: boolean
    useCustomComments?: boolean
}

interface AWSS3Region {
    description: string
    id: string
}

export interface AWSS3RepositorySettings extends DefaultSettings {
    accessKey?: string
    allAllowedRegions?: AWSS3Region[]
    allSseAlgorithms?: AWS_SSE_ALGORITHM[]
    bucketName: string
    listenerTimerPeriod: number // "format" : "int32", "minimum" : 1
    regionName: string
    secretKey?: string
    serviceEndpoint?: string,
    sseAlgorithm?: AWS_SSE_ALGORITHM
}

export interface AzureBlobRepositorySettings extends DefaultSettings {
    accountKey?: string
    accountName?: string
    listenerTimerPeriod: number // "format" : "int32", "minimum" : 1
    uri: string
}

export interface CommonRepositorySettings extends DefaultSettings {
    login?: string
    password?: string
    secure?: boolean
    uri: string
}

export interface GitRepositorySettings extends DefaultSettings {
    branch?: string
    connectionTimeout: number // "format" : "int32", "minimum" : 1
    failedAuthenticationSeconds: number // "format" : "int32", "minimum" : 1
    listenerTimerPeriod: number // "format" : "int32", "minimum" : 1
    localRepositoryPath: string
    login?: string
    maxAuthenticationAttempts?: number // "format" : "int32"
    newBranchRegex?: string
    newBranchRegexError?: string
    newBranchTemplate?: string
    password?: string
    protectedBranches?: string
    remoteRepository?: boolean
    tagPrefix?: string
    uri?: string
}

export interface LocalRepositorySettings extends DefaultSettings {
    uri: string
}

export interface RepositoryResponse {
    errorMessage?: string
    id: string
    name: InputTextField
    settings: AWSS3RepositorySettings | AzureBlobRepositorySettings | CommonRepositorySettings | GitRepositorySettings | LocalRepositorySettings
    type: RepositoryType
}

export interface FormRefProps {
    getForm: () => FormInstance
    addRepository: () => Promise<void>
    isEditingNewRepository: () => boolean
}
