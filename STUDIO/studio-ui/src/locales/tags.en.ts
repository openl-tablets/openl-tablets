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
    templates_saved: 'Templates saved successfully',
    templates_save_error: 'Failed to save templates',
    fill_tags_error: 'Failed to fill tags for projects',
    fill_result: {
        title: 'Filled tags',
        summary: '{{updated}} project(s) updated, {{skipped}} left alone.',
        result_column: 'Result',
        close: 'Close',
        left_alone: 'Left alone: {{reason}}',
        not_assigned: 'The project did not get this value: it is not in the list of tags, and its tag type '
            + 'does not take new values, or the value is not a valid tag name.',
        reason: {
            nothingToAssign: 'None of the missing values can be assigned. Add them to their tag types, or make '
                + 'the tag types extensible, then fill the project again. A value that is not a valid tag name, '
                + 'such as one that ends with a dot, needs another project name or template.',
            failed: 'The tags could not be written. The OpenL Studio log names the reason.',
        },
    },
    fill_blocker: {
        locked: 'The project is being edited by {{lockedBy}}. Fill it again after they save or close it.',
        lockedByYou: 'You still hold a lock on the project from an earlier change. Open the project and close it '
            + 'to release the lock, then fill it again.',
        another_user: 'another user',
        branchProtected: 'The branch "{{branch}}" of the project is protected, so its changes go through a merge. '
            + 'Fill the project in another branch, then merge that branch.',
        noPermission: 'You do not have permission to change the project. Ask for write access to it, then fill '
            + 'it again.',
        olderRevision: 'You opened an older revision of the project. Open the latest revision or close the '
            + 'project, then fill it again.',
        archive: 'The project is closed, and its repository keeps projects as archives. Open the project, then '
            + 'fill it again.',
        unknown: 'The project cannot be changed now.',
    },
    fill_preview: {
        title: 'Projects without tags',
        project_column: 'Project',
        tags_column: 'Tags',
        apply: 'Fill Tags',
        legend: 'A white tag is assigned as it is, a green one is created for its extensible tag type, '
            + 'a red one cannot be assigned, and a grey one is what the project carries now.',
        nothing_to_fill: 'Every project that matches a template already carries its tags.',
        state: {
            assign: 'The value exists in the list of tags and is assigned to the project.',
            create: 'The value does not exist in the list of tags, and the tag type is extensible, '
                + 'so it is created and assigned to the project.',
            rejected: 'The value does not exist in the list of tags, and the tag type is not extensible or the '
                + 'value is not a valid tag name, so it is neither created nor assigned, and the tag remains None.',
            keep: 'The project already carries this value.',
        },
    },
})
