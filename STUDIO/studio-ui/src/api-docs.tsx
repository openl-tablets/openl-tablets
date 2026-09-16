import { createRoot } from 'react-dom/client'

import { ApiDocs } from './containers/ApiDocs'

// The documentation is a page of its own: no application shell, nothing to log in to, nothing else on it.
const container = document.getElementById('appRoot') as HTMLElement
createRoot(container).render(<ApiDocs />)
