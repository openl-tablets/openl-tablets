
// You can find instructions for this file here:
// http://www.treeview.net

// Decide if the names are links or just the icons
USETEXTLINKS = 1;  //replace 0 with 1 for hyperlinks

// Decide if the tree is to start all open or just showing the root folders
STARTALLOPEN = 0; //replace 0 with 1 to show the whole tree

ICONPATH = 'icons/'; //change if the gif's folder is a subfolder, for example: 'images/'
  foldersTree = gFld("OpenL Tablets Reference Guide", "");
aux1 = insFld(foldersTree, gFld("Preface", "Ref_Preface.html#preface1"));
insDoc(aux1, gLnk("R", "Audience", "Ref_Audience.html#audience2"));
insDoc(aux1, gLnk("R", "Related Information", "Ref_RelatedInformation.html#related_information3"));
aux1 = insFld(foldersTree, gFld("Chapter 1: Introducing OpenL Tablets", "Ref_IntroducingOpenLTablets.html#chapter_1_introducing_openl_tablets4"));
insDoc(aux1, gLnk("R", "What Is Exigen Decision Services?", "Ref_WhatIsExigenDecisionServices.html#what_is_exigen_decision_services5"));
insDoc(aux1, gLnk("R", "What Is OpenL Tablets?", "Ref_WhatIsOpenLTablets.html#what_is_openl_tablets6"));
aux2 = insFld(aux1, gFld("Basic Concepts", "Ref_BasicConcepts.html#basic_concepts7"));
insDoc(aux2, gLnk("R", "Rules", "Ref_Rules.html#rules8"));
insDoc(aux2, gLnk("R", "Tables", "Ref_Tables.html#tables9"));
insDoc(aux2, gLnk("R", "Projects", "Ref_Projects.html#projects10"));
insDoc(aux2, gLnk("R", "Wrappers", "Ref_Wrappers.html#wrappers11"));
insDoc(aux1, gLnk("R", "System Overview", "Ref_SystemOverview.html#system_overview12"));
insDoc(aux1, gLnk("R", "Installing OpenL Tablets", "Ref_InstallingOpenLTablets.html#installing_openl_tablets13"));
aux2 = insFld(aux1, gFld("Tutorials and Examples", "Ref_TutorialsAndExamples.html#tutorials_and_examples14"));
insDoc(aux2, gLnk("R", "Tutorials", "Ref_Tutorials.html#tutorials15"));
insDoc(aux2, gLnk("R", "Examples", "Ref_Examples.html#examples16"));
aux1 = insFld(foldersTree, gFld("Chapter 2: Creating Tables for OpenL Tablets", "Ref_CreatingTablesForOpenLTablets.html#chapter_2_creating_tables_for_openl_tablets17"));
insDoc(aux1, gLnk("R", "Table Recognition Algorithm", "Ref_TableRecognitionAlgorithm.html#table_recognition_algorithm18"));
aux2 = insFld(aux1, gFld("Table Types", "Ref_TableTypes.html#table_types19"));
insDoc(aux2, gLnk("R", "Decision Table", "Ref_DecisionTable.html#decision_table20"));
insDoc(aux2, gLnk("R", "Data Type Table", "Ref_DataTypeTable.html#data_type_table28"));
insDoc(aux2, gLnk("R", "Data Table", "Ref_DataTable.html#data_table29"));
insDoc(aux2, gLnk("R", "Test Table", "Ref_TestTable.html#test_table34"));
insDoc(aux2, gLnk("R", "Run Method Table", "Ref_RunMethodTable.html#run_method_table35"));
insDoc(aux2, gLnk("R", "Method Table", "Ref_MethodTable.html#method_table36"));
insDoc(aux2, gLnk("R", "Configuration Table", "Ref_ConfigurationTable.html#configuration_table37"));
aux1 = insFld(foldersTree, gFld("Chapter 3: Working With Projects", "Ref_WorkingWithProjects.html#chapter_3_working_with_projects38"));
insDoc(aux1, gLnk("R", "Project Structure", "Ref_ProjectStructure.html#project_structure39"));
insDoc(aux1, gLnk("R", "Creating a Project", "Ref_CreatingAProject.html#creating_a_project40"));
aux2 = insFld(aux1, gFld("Generating a Wrapper", "Ref_GeneratingAWrapper.html#generating_a_wrapper41"));
insDoc(aux2, gLnk("R", "Configuring the Ant Task File", "Ref_ConfiguringTheAntTaskFile.html#configuring_the_ant_task_file42"));
insDoc(aux2, gLnk("R", "Executing the Ant Task File", "Ref_ExecutingTheAntTaskFile.html#executing_the_ant_task_file43"));
aux1 = insFld(foldersTree, gFld("Appendix A: BEX Language Overview", "Ref_BEXLanguageOverview.html#appendix_a_bex_language_overview44"));
insDoc(aux1, gLnk("R", "Introduction to BEX", "Ref_IntroductionToBEX.html#introduction_to_bex45"));
insDoc(aux1, gLnk("R", "Keywords", "Ref_Keywords.html#keywords46"));
aux2 = insFld(aux1, gFld("Simplifying Expressions", "Ref_SimplifyingExpressions.html#simplifying_expressions47"));
insDoc(aux2, gLnk("R", "Notation of Explanatory Variables", "Ref_NotationOfExplanatoryVariables.html#notation_of_explanatory_variables48"));
insDoc(aux2, gLnk("R", "Uniqueness of Scope", "Ref_UniquenessOfScope.html#uniqueness_of_scope49"));
