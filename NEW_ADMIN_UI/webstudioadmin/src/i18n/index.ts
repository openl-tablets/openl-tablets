import { loadRemote } from '@module-federation/runtime'

import './common.en'
import './email.en'
import './groups.en'
import './notification.en'
import './repository.en'
import './system.en'
import './tags.en'
import './user.en'
import './users.en'

loadRemote('claimEditorPlugin/i18n').catch((e) => console.error('Failed to load claimEditorPlugin/i18n', e))