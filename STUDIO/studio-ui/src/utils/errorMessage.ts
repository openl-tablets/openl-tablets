/** Extract a human-readable message from an unknown thrown value. */
export const errorMessage = (error: unknown): string => (error instanceof Error ? error.message : String(error))

/** The message of a thrown error, or the fallback when the value is no error or its message is empty. */
export const errorMessageOr = (error: unknown, fallback: string): string => (
    (error instanceof Error && error.message) || fallback
)
