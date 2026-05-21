import apiCall, { ApiHttpError, NotFoundError, EmptyError, ForbiddenError, isApiHttpError } from './apiCall'
import type { ApiCallOptions } from './apiCall'
import CONFIG from './config'
import webSocketService from './websocket'
import { fetchProjectStatus, subscribeProjectStatus } from './projectStatus'
import type {
    ProjectStatusUpdate,
    ProjectStatusSubscription,
    ProjectStatusMessage,
    ProjectStatusSeverity,
    ProjectCompileState,
    ProjectStatusCompilation,
    ProjectStatusCompilationMessages,
    ProjectStatusCompilationModules,
    ProjectStatusCompilationTests,
    ProjectStatusCompilationTables,
} from './projectStatus'

export {
    apiCall,
    CONFIG,
    webSocketService,
    ApiHttpError,
    NotFoundError,
    EmptyError,
    ForbiddenError,
    isApiHttpError,
    fetchProjectStatus,
    subscribeProjectStatus,
}
export type {
    ApiCallOptions,
    ProjectStatusUpdate,
    ProjectStatusSubscription,
    ProjectStatusMessage,
    ProjectStatusSeverity,
    ProjectCompileState,
    ProjectStatusCompilation,
    ProjectStatusCompilationMessages,
    ProjectStatusCompilationModules,
    ProjectStatusCompilationTests,
    ProjectStatusCompilationTables,
}
