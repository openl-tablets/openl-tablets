<!--
OpenL Tablets – Release Notes content template (version-specific pages)

AI filling rules (important):
- Start the release notes with a 1–2 paragraph overview that summarizes what's most important in this release.
- Include ONLY the sections that have content (tickets/changes). If no tickets exist for a category, omit the entire section.
- The first section after the overview should be the first category that has items (e.g., New Features, Improvements, Bug Fixes, Security & Library Updates, Known Issues, or Migration Notes).
- Keep labels/headers exactly (case, punctuation, colons) and keep bullet indentation consistent.
- Use --- (horizontal rule) between individual items within a section to visually separate them. Omit the trailing --- after the last item in each section.
- Use --- between subsections within Migration Notes. Omit the trailing --- after the last migration topic.
-->

## Release Notes

[{{version_tag}}]({{github_tag_url}}) on the GitHub

{{overview_paragraph_1}}

{{overview_paragraph_2_optional}}

<!-- ========================================================= -->
<!-- Optional section. Omit if there are no New Feature items.  -->
<!-- ========================================================= -->
## **New Features**

### **{{new_feature_1_title}}**

{{new_feature_1_description_paragraphs}}

{{#if new_feature_1_has_bullets}}
  * {{new_feature_1_bullet_1}}
  * {{new_feature_1_bullet_2}}
{{/if}}

{{#if new_feature_1_has_images}}
![Image]({{new_feature_1_image_url_1}})

{{#if new_feature_1_image_url_2}}![Image]({{new_feature_1_image_url_2}}){{/if}}
{{/if}}

---

### **{{new_feature_2_title}}**

{{new_feature_2_description_paragraphs}}

---

{{more_new_features_each_separated_by_horizontal_rule}}

<!-- ====================================================== -->
<!-- Optional section. Omit if there are no Improvements.    -->
<!-- ====================================================== -->
## **Improvements**

### **{{improvement_area_1}}**

  * {{improvement_area_1_item_1}}
  * {{improvement_area_1_item_2}}
  * {{improvement_area_1_item_3}}

{{#if improvement_area_1_has_followup_text}}
{{improvement_area_1_followup_paragraphs}}
{{/if}}

---

### **{{improvement_area_2_optional}}**

  * {{improvement_area_2_item_1}}
  * {{improvement_area_2_item_2}}

{{more_improvement_areas_each_separated_by_horizontal_rule}}

<!-- ====================================================== -->
<!-- Optional section. Omit if there are no Breaking Changes -->
<!-- ====================================================== -->
## **Breaking Changes**

### **{{breaking_change_1_title}}**

{{breaking_change_1_description_paragraphs}}

{{#if breaking_change_1_has_migration_steps}}
#### **Migration Steps**

{{breaking_change_1_migration_steps}}
{{/if}}

---

### **{{breaking_change_2_title}}**

{{breaking_change_2_description_paragraphs}}

{{more_breaking_changes_each_separated_by_horizontal_rule}}

<!-- ====================================================== -->
<!-- Optional section. Omit if there are no Fixed Bugs.      -->
<!-- ====================================================== -->
## **Bug Fixes**

  * {{bugfix_1}}
  * {{bugfix_2}}
  * {{bugfix_3}}

<!-- ====================================================== -->
<!-- Optional section. Omit if there are no library updates. -->
<!-- Use ONE of the two formats below, based on the source   -->
<!-- style for that release.                                 -->
<!-- ====================================================== -->

<!-- FORMAT A (DEFAULT) — use for all new releases -->
## **Security & Library Updates**

### **Security Vulnerability Fixes**

  * {{cve_or_security_item_1}}
  * {{cve_or_security_item_2}}

---

### **Major Library Upgrades**

#### **Runtime Dependencies**

  * {{runtime_dependency_1}}
  * {{runtime_dependency_2}}
  * {{runtime_dependency_3}}

#### **Test Dependencies**

  * {{test_dependency_1}}
  * {{test_dependency_2}}

#### **Removed Dependencies**

  * {{removed_dependency_1}}

<!-- FORMAT B (LEGACY only) — flat list, only for older releases that used this style.
     Do NOT use for new releases. Do NOT mix Format A and Format B in the same output. -->
<!--
  * {{updated_library_1}}
  * {{updated_library_2}}
  * {{updated_library_3}}
-->

<!-- ====================================================== -->
<!-- Optional section. Omit if no Known Issues.              -->
<!-- ====================================================== -->
## **Known Issues**

  * {{known_issue_1}}
  * {{known_issue_2}}

<!-- ====================================================== -->
<!-- Optional section. Omit if no Migration Notes.           -->
<!-- ====================================================== -->
## **Migration Notes**

### **{{migration_topic_1}}**

{{migration_topic_1_paragraphs}}

{{#if migration_topic_1_has_bullets}}
  * {{migration_topic_1_bullet_1}}
  * {{migration_topic_1_bullet_2}}
{{/if}}

---

### **{{migration_topic_2}}**

{{migration_topic_2_paragraphs}}

---

{{more_migration_topics_each_separated_by_horizontal_rule}}
