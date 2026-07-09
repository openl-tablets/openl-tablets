import { apiCall } from '../services'

const COMPARE_WINDOW_FEATURES = 'width=1240,height=700,screenX=50,screenY=100,resizable=yes,scrollbars=yes,status=yes'

const EXCEL_EXTENSION = /\.(xlsx?|xlsm|xlsb)$/i

export const isExcelFile = (path: string): boolean => EXCEL_EXTENSION.test(path)

export const fileName = (path: string): string => path.split('/').pop() || 'file'

export const htmlEscape = (value: string): string =>
    value
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;')

export const openComparePopup = (): Window => {
    const popup = window.open('', 'Compare', COMPARE_WINDOW_FEATURES)
    if (!popup) {
        throw new Error('The comparison window was blocked by the browser.')
    }
    return popup
}

export const writeCompareLoading = (popup: Window, path: string) => {
    popup.document.open()
    popup.document.write(`
        <!doctype html>
        <html>
            <head><title>Compare ${htmlEscape(fileName(path))}</title></head>
            <body style="font-family: sans-serif; padding: 24px;">Loading comparison...</body>
        </html>
    `)
    popup.document.close()
}

export const openLegacyExcelCompare = async (
    popup: Window,
    path: string,
    left: Blob,
    right: Blob,
    leftName = `left-${fileName(path)}`,
    rightName = `right-${fileName(path)}`
) => {
    const body = new FormData()
    body.append('file1', left, leftName)
    body.append('file2', right, rightName)
    body.append('fileName', fileName(path))

    const response = await apiCall('/public/compare/xls', { method: 'POST', body }, {
        throwError: true,
        responseType: 'response',
    }) as Response
    if (!response.url) {
        throw new Error('Failed to open Excel comparison.')
    }
    popup.location.href = response.url
}
