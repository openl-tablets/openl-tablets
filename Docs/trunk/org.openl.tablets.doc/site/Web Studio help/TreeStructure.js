
// You can find instructions for this file here:
// http://www.treeview.net

// Decide if the names are links or just the icons
USETEXTLINKS = 1;  //replace 0 with 1 for hyperlinks

// Decide if the tree is to start all open or just showing the root folders
STARTALLOPEN = 0; //replace 0 with 1 to show the whole tree

ICONPATH = 'icons/'; //change if the gif's folder is a subfolder, for example: 'images/'
  foldersTree = gFld("OpenL Web Studio User's Guide", "");
aux1 = insFld(foldersTree, gFld("Preface", "User_Preface.html#preface1"));
insDoc(aux1, gLnk("R", "Audience", "User_Audience.html#audience2"));
insDoc(aux1, gLnk("R", "Related Information", "User_RelatedInformation.html#related_information3"));
aux1 = insFld(foldersTree, gFld("Chapter 1: Introducing OpenL Web Studio", "User_IntroducingOpenLWebStudio.html#chapter_1_introducing_openl_web_studio4"));
insDoc(aux1, gLnk("R", "What is OpenL Web Studio?", "User_WhatIsOpenLWebStudio.html#what_is_openl_web_studio5"));
insDoc(aux1, gLnk("R", "Working with Projects in OpenL Web Studio", "User_WorkingWithProjectsInOpenLWebStudio.html#working_with_projects_in_openl_web_studio6"));
insDoc(aux1, gLnk("R", "OpenL Web Studio Components", "User_OpenLWebStudioComponents.html#openl_web_studio_components7"));
insDoc(aux1, gLnk("R", "Security Overview", "User_SecurityOverview.html#security_overview8"));
aux2 = insFld(aux1, gFld("User Perspectives", "User_UserPerspectives.html#user_perspectives9"));
insDoc(aux2, gLnk("R", "Business User's Perspective", "User_BusinessUserSPerspective.html#business_user_s_perspective10"));
insDoc(aux2, gLnk("R", "Developer's Perspective", "User_DeveloperSPerspective.html#developer_s_perspective11"));
aux1 = insFld(foldersTree, gFld("Chapter 2: Getting Started", "User_GettingStarted.html#chapter_2_getting_started12"));
insDoc(aux1, gLnk("R", "Logging In to OpenL Web Studio", "User_LoggingInToOpenLWebStudio.html#logging_in_to_openl_web_studio13"));
aux2 = insFld(aux1, gFld("Understanding the User Interface", "User_UnderstandingTheUserInterface.html#understanding_the_user_interface14"));
insDoc(aux2, gLnk("R", "Rule Editor", "User_RuleEditor.html#rule_editor15"));
insDoc(aux2, gLnk("R", "Repository Editor", "User_RepositoryEditor.html#repository_editor18"));
aux1 = insFld(foldersTree, gFld("Chapter 3: Using Rule Editor", "User_UsingRuleEditor.html#chapter_3_using_rule_editor19"));
insDoc(aux1, gLnk("R", "Opening a Module", "User_OpeningAModule.html#opening_a_module20"));
aux2 = insFld(aux1, gFld("Managing Projects", "User_ManagingProjects.html#managing_projects21"));
insDoc(aux2, gLnk("R", "Checking Out and Checking In a Project", "User_CheckingOutAndCheckingInAProject.html#checking_out_and_checking_in_a_project22"));
insDoc(aux2, gLnk("R", "Uploading Projects to Design Time Repository", "User_UploadingProjectsToDesignTimeRepository.html#uploading_projects_to_design_time_repository23"));
insDoc(aux1, gLnk("R", "Viewing Tables", "User_ViewingTables.html#viewing_tables24"));
insDoc(aux1, gLnk("R", "Modifying Tables", "User_ModifyingTables.html#modifying_tables25"));
aux2 = insFld(aux1, gFld("Performing a Search", "User_PerformingASearch.html#performing_a_search26"));
insDoc(aux2, gLnk("R", "Simple Search", "User_SimpleSearch.html#simple_search27"));
insDoc(aux2, gLnk("R", "Advanced Search", "User_AdvancedSearch.html#advanced_search28"));
insDoc(aux2, gLnk("R", "Index", "User_Index.html#index29"));
aux1 = insFld(foldersTree, gFld("Chapter 4: Using Repository Editor", "User_UsingRepositoryEditor.html#chapter_4_using_repository_editor30"));
insDoc(aux1, gLnk("R", "Browsing Design Time Repository", "User_BrowsingDesignTimeRepository.html#browsing_design_time_repository31"));
insDoc(aux1, gLnk("R", "Filtering the Project Tree", "User_FilteringTheProjectTree.html#filtering_the_project_tree32"));
insDoc(aux1, gLnk("R", "Uploading a Project", "User_UploadingAProject.html#uploading_a_project33"));
insDoc(aux1, gLnk("R", "Creating a Project", "User_CreatingAProject.html#creating_a_project34"));
insDoc(aux1, gLnk("R", "Opening a Project", "User_OpeningAProject.html#opening_a_project35"));
insDoc(aux1, gLnk("R", "Closing a Project", "User_ClosingAProject.html#closing_a_project36"));
insDoc(aux1, gLnk("R", "Checking Out a Project", "User_CheckingOutAProject.html#checking_out_a_project37"));
insDoc(aux1, gLnk("R", "Checking In a Project", "User_CheckingInAProject.html#checking_in_a_project38"));
insDoc(aux1, gLnk("R", "Defining Project Dependencies", "User_DefiningProjectDependencies.html#defining_project_dependencies39"));
aux2 = insFld(aux1, gFld("Modifying a Project", "User_ModifyingAProject.html#modifying_a_project40"));
insDoc(aux2, gLnk("R", "Modifying Project Properties", "User_ModifyingProjectProperties.html#modifying_project_properties41"));
insDoc(aux2, gLnk("R", "Modifying Project Contents", "User_ModifyingProjectContents.html#modifying_project_contents42"));
insDoc(aux1, gLnk("R", "Copying a Project", "User_CopyingAProject.html#copying_a_project46"));
aux2 = insFld(aux1, gFld("Removing a Project", "User_RemovingAProject.html#removing_a_project47"));
insDoc(aux2, gLnk("R", "Deleting a Project", "User_DeletingAProject.html#deleting_a_project48"));
insDoc(aux2, gLnk("R", "Erasing a Project", "User_ErasingAProject.html#erasing_a_project49"));
aux2 = insFld(aux1, gFld("Deploying Projects", "User_DeployingProjects.html#deploying_projects50"));
insDoc(aux2, gLnk("R", "Creating a Deployment Project", "User_CreatingADeploymentProject.html#creating_a_deployment_project51"));
insDoc(aux2, gLnk("R", "Defining Deployment Project Descriptors", "User_DefiningDeploymentProjectDescriptors.html#defining_deployment_project_descriptors52"));
insDoc(aux2, gLnk("R", "Deploying a Deployment Project", "User_DeployingADeploymentProject.html#deploying_a_deployment_project53"));
insDoc(aux2, gLnk("R", "Opening Deployed Projects", "User_OpeningDeployedProjects.html#opening_deployed_projects54"));
insDoc(aux2, gLnk("R", "Redeploying Projects", "User_RedeployingProjects.html#redeploying_projects55"));
insDoc(aux1, gLnk("R", "Comparing Project Versions", "User_ComparingProjectVersions.html#comparing_project_versions56"));
aux1 = insFld(foldersTree, gFld("Chapter 5: Advanced Functionality", "User_AdvancedFunctionality.html#chapter_5_advanced_functionality57"));
insDoc(aux1, gLnk("R", "Unit Tests", "User_UnitTests.html#unit_tests58"));
insDoc(aux1, gLnk("R", "Validation", "User_Validation.html#validation59"));
insDoc(aux1, gLnk("R", "Tracing", "User_Tracing.html#tracing60"));
insDoc(aux1, gLnk("R", "Benchmarking", "User_Benchmarking.html#benchmarking61"));
