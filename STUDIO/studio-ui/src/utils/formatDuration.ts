/** Format a millisecond duration compactly: `1.2 s`, `45 ms`, `3.4 ms`, `0 ms`. */
export const formatMs = (ms: number): string => {
    if (ms >= 1000) {
        return `${(ms / 1000).toFixed(1)} s`
    }
    if (ms >= 10) {
        return `${Math.round(ms)} ms`
    }
    if (ms > 0) {
        return `${ms.toFixed(1)} ms`
    }
    return '0 ms'
}
