# Workspace Copy ACL Outcomes

These scenarios verify the final workspace state for opened and in-editing project copies after Studio ACL
permissions change:

- Revoking write access keeps an opened copy available as read-only.
- Revoking write access keeps an in-editing copy, rejects saves, and retains its edit lock.
- Revoking read access removes either copy, discards local changes, and releases the edit lock.
- Restoring read access lists the source project as closed without restoring the removed copy.
