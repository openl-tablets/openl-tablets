<!--
OpenL Tablets – Release Notes content template (version-specific pages)

AI filling rules (important):
- Start the release notes with a 1–2 paragraph overview that summarizes what’s most important in this release.
- Include ONLY the sections that have content (tickets/changes). If no tickets exist for a category, omit the entire section.
- The first section after the overview should be the first category that has items (e.g., New Features, Improvements, Fixed Bugs, Updated Libraries, or Runtime Dependencies).
- Keep labels/headers exactly (case, punctuation, colons) and keep bullet indentation consistent.
-->

## Release Notes

[{{version_tag}}]({{github_tag_url}}) on the GitHub

{{overview_paragraph_1}}

{{overview_paragraph_2_optional}}

<!-- ========================================================= -->
<!-- Optional section. Omit if there are no New Feature items.  -->
<!-- ========================================================= -->
### New Features

{{new_feature_1_title}}
{{new_feature_1_description_paragraphs}}

{{#if new_feature_1_has_bullets}}
  * {{new_feature_1_bullet_1}}
  * {{new_feature_1_bullet_2}}
{{/if}}

{{#if new_feature_1_has_images}}
![Image]({{new_feature_1_image_url_1}})

{{#if new_feature_1_image_url_2}}![Image]({{new_feature_1_image_url_2}}){{/if}}
{{/if}}

{{new_feature_2_title}}
{{new_feature_2_description_paragraphs}}

{{more_new_features_optional}}

<!-- ====================================================== -->
<!-- Optional section. Omit if there are no Improvements.    -->
<!-- ====================================================== -->
### Improvements

{{improvement_area_1}}:

  * {{improvement_area_1_item_1}}
  * {{improvement_area_1_item_2}}
  * {{improvement_area_1_item_3}}

{{#if improvement_area_1_has_followup_text}}
{{improvement_area_1_followup_paragraphs}}
{{/if}}

{{improvement_area_2_optional}}:

  * {{improvement_area_2_item_1}}
  * {{improvement_area_2_item_2}}

{{more_improvements_optional}}

<!-- ====================================================== -->
<!-- Optional section. Omit if there are no Fixed Bugs.      -->
<!-- ====================================================== -->
### Fixed Bugs

{{bugfix_area_1}}:

  * {{bugfix_area_1_item_1}}
  * {{bugfix_area_1_item_2}}

{{bugfix_area_2_optional}}:

  * {{bugfix_area_2_item_1}}

{{more_bugfixes_optional}}

<!-- ====================================================== -->
<!-- Optional section. Omit if there are no library updates. -->
<!-- Use ONE of the two formats below, based on the source   -->
<!-- style for that release.                                 -->
<!-- ====================================================== -->

<!-- FORMAT A (with heading) — used on many versions -->
### Updated Libraries

{{#if updated_libraries_simple_list}}
  * {{updated_library_1}}
  * {{updated_library_2}}
  * {{updated_library_3}}
{{/if}}

{{#if updated_libraries_structured}}
Fixed Vulnerabilities:

  * {{cve_or_security_item_1}}
  * {{cve_or_security_item_2}}

Runtime Dependencies:

  * {{runtime_dependency_1}}
  * {{runtime_dependency_2}}
  * {{runtime_dependency_3}}

Test Dependencies:

  * {{test_dependency_1}}
  * {{test_dependency_2}}

Maven Plugins:

* {{maven_plugin_1}}
* {{maven_plugin_2}}
* {{maven_plugin_3}}
{{/if}}

<!-- FORMAT B (no “### Updated Libraries” heading) — some versions start directly with dependency lists -->
{{#if updated_libraries_no_heading}}
Runtime Dependencies:

  * {{runtime_dependency_1}}
  * {{runtime_dependency_2}}
  * {{runtime_dependency_3}}

Test Dependencies:

  * {{test_dependency_1}}
  * {{test_dependency_2}}

Maven Plugins:

* {{maven_plugin_1}}
* {{maven_plugin_2}}
* {{maven_plugin_3}}
{{/if}}

<!-- Optional subsection (appears in some releases) -->
{{#if jakarta_ee10_support}}
Jakarta EE10 support

Fixed Vulnerabilities:

  * {{jakarta_cve_1}}
  * {{jakarta_cve_2}}

Runtime Dependencies:
  * {{jakarta_runtime_dependency_1}}
  * {{jakarta_runtime_dependency_2}}
  * {{jakarta_runtime_dependency_3}}
{{/if}}

<!-- ====================================================== -->
<!-- Optional section. Omit if no Known Issues.              -->
<!-- ====================================================== -->
### Known Issues

  * {{known_issue_1}}
  * {{known_issue_2}}

{{more_known_issues_optional}}

<!-- ====================================================== -->
<!-- Optional section. Omit if no Migration Notes.           -->
<!-- ====================================================== -->
### Migration Notes

{{migration_notes_paragraphs}}

{{#if migration_notes_has_bullets}}
  * {{migration_note_bullet_1}}
  * {{migration_note_bullet_2}}
{{/if}}
