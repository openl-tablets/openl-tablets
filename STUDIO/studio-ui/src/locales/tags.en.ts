import i18next from 'i18next'

i18next.addResourceBundle('en', 'tags', {
    extensible: 'Extensible',
    nullable: 'Nullable',
    add_tag: 'Add Tag',
    tags: 'Tags',
    tag_type: 'Tag Type',
    actions: 'Actions',
    // Description
    tag_types_and_values: 'Tag Types and Values',
    tag_type_description: '<0>Tag type</0> is a category that includes tag values of the same group. For example, the Product tag type can include tags Auto, Life, and Home. <1>Proceed as follows:</1>',
    tag_type_instruction_p1: 'To add a tag type, in the <0>New Tag Type field</0>, enter the tag type name and press <0>Enter</0> or <0>Tab</0>. The tag type is added, and fields for tag values appear',
    tag_type_instruction_p2: 'To add a tag value, in the <0>New Tag</0> field, enter the tag name and press <0>Enter</0>',
    tag_type_auto_save_notice: 'All created tag types and values are saved automatically',
    tag_input_placeholder: 'New Tag Type',
    tags_from_a_project_name: 'Tags from a Project Name',
    tag_project_instruction_p1: 'Tags can be extracted from a project name using a project name template',
    tag_project_instruction_p2: 'Each template must be defined on its own line. The order of the templates is important: the first template has the highest priority, the last template has the lowest priority',
    tag_project_instruction_p3: 'Tag types are wrapped with the percentage \'%\' symbol',
    tag_project_instruction_p4: '\'?\' stands for any symbol',
    tag_project_instruction_p5: '\'*\' stands for any text of any length',
    example: 'Example',
    example_template: 'For the <0>%Domain%-%LOB%-*</0> template, for the <0>Policy-L&A-rules</0> project, the tags are <0>Policy</0> for the <0>Domain</0> tag type and <0>L&A</0> for <0>LOB</0>',
    project_name_templates: 'Project Name Templates',
    save_templates: 'Save Templates',
    fill_tags_for_project: 'Fill Tags for Project',
})
