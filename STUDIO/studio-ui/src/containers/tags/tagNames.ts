/** What a tag type or a tag value is matched by: names match regardless of case and surrounding spaces. */
export const nameKey = (name: string): string => name.trim().toLowerCase()

/** Whether two tag type or tag value names name the same thing. */
export const sameName = (one: string, other: string): boolean => nameKey(one) === nameKey(other)
