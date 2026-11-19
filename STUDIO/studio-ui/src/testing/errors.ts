import { NotFoundError } from '../services/apiCall'

/** The answer the server gives to a request for a module the project does not have. */
export const moduleNotFound = (): NotFoundError => new NotFoundError('The module is not found in the project.',
    { code: 'openl.error.404.project.module.identifier.message' })

/** The answer the server gives to a request for a table the module does not hold. */
export const tableNotFound = (): NotFoundError => new NotFoundError("The table is not found in module 'Bank Rating'.",
    { code: 'openl.error.404.table.in.module.message' })

/** The answer the server gives when it cannot resolve the project a request names. */
export const projectNotFound = (): NotFoundError => new NotFoundError('The project identifier is invalid or not found.',
    { code: 'openl.error.404.project.identifier.message' })
