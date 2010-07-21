
// You can find instructions for this file here:
// http://www.treeview.net

// Decide if the names are links or just the icons
USETEXTLINKS = 1;  //replace 0 with 1 for hyperlinks

// Decide if the tree is to start all open or just showing the root folders
STARTALLOPEN = 0; //replace 0 with 1 to show the whole tree

ICONPATH = 'icons/'; //change if the gif's folder is a subfolder, for example: 'images/'
  foldersTree = gFld("OpenL WebStudio Application", "");
aux1 = insFld(foldersTree, gFld("Preface", "Preface.html#preface1"));
insDoc(aux1, gLnk("R", "Audience", "Audience.html#audience2"));
insDoc(aux1, gLnk("R", "Related Information", "RelatedInformation.html#related_information3"));
insDoc(aux1, gLnk("R", "Typographic Conventions", "TypographicConventions.html#typographic_conventions4"));
aux1 = insFld(foldersTree, gFld("Chapter 1: Introducing OpenL Web Studio", "IntroducingOpenLWebStudio.html#chapter_1_introducing_openl_web_studio5"));
insDoc(aux1, gLnk("R", "What Is OpenL Web Studio?", "WhatIsOpenLWebStudio.html#what_is_openl_web_studio6"));
insDoc(aux1, gLnk("R", "Working with Projects in OpenL Web Studio", "WorkingWithProjectsInOpenLWebStudio.html#working_with_projects_in_openl_web_studio7"));
insDoc(aux1, gLnk("R", "OpenL Web Studio Components", "OpenLWebStudioComponents.html#openl_web_studio_components8"));
insDoc(aux1, gLnk("R", "Security Overview", "SecurityOverview.html#security_overview9"));
aux2 = insFld(aux1, gFld("User Perspectives", "UserPerspectives.html#user_perspectives10"));
insDoc(aux2, gLnk("R", "Business User's Perspective", "BusinessUserSPerspective.html#business_user_s_perspective11"));
insDoc(aux2, gLnk("R", "Developer's Perspective", "DeveloperSPerspective.html#developer_s_perspective12"));
aux1 = insFld(foldersTree, gFld("Chapter 2: Getting Started", "GettingStarted.html#chapter_2_getting_started13"));
insDoc(aux1, gLnk("R", "Logging In to OpenL Web Studio", "LoggingInToOpenLWebStudio.html#logging_in_to_openl_web_studio14"));
aux2 = insFld(aux1, gFld("Understanding the User Interface", "UnderstandingTheUserInterface.html#understanding_the_user_interface15"));
insDoc(aux2, gLnk("R", "Rule Editor", "RuleEditor.html#rule_editor16"));
insDoc(aux2, gLnk("R", "Repository Editor", "RepositoryEditor.html#repository_editor19"));
aux1 = insFld(foldersTree, gFld("Chapter 3: Using Rule Editor", "UsingRuleEditor.html#chapter_3_using_rule_editor20"));
insDoc(aux1, gLnk("R", "Opening a Module", "OpeningAModule.html#opening_a_module21"));
aux2 = insFld(aux1, gFld("Managing Projects", "ManagingProjects.html#managing_projects22"));
insDoc(aux2, gLnk("R", "Checking Out and Checking In a Project", "CheckingOutAndCheckingInAProject.html#checking_out_and_checking_in_a_project23"));
insDoc(aux2, gLnk("R", "Uploading Projects to Design Time Repository", "UploadingProjectsToDesignTimeRepository.html#uploading_projects_to_design_time_repository24"));
insDoc(aux1, gLnk("R", "Viewing Tables", "ViewingTables.html#viewing_tables25"));
insDoc(aux1, gLnk("R", "Modifying Tables", "ModifyingTables.html#modifying_tables26"));
aux2 = insFld(aux1, gFld("Performing a Search", "PerformingASearch.html#performing_a_search27"));
insDoc(aux2, gLnk("R", "Simple Search", "SimpleSearch.html#simple_search28"));
insDoc(aux2, gLnk("R", "Business Search", "BusinessSearch.html#business_search29"));
insDoc(aux2, gLnk("R", "Advanced Search", "AdvancedSearch.html#advanced_search30"));
insDoc(aux2, gLnk("R", "Index", "Index.html#index31"));
aux2 = insFld(aux1, gFld("Creating New Table", "CreatingNewTable.html#creating_new_table32"));
insDoc(aux2, gLnk("R", "Datatype table wizard", "DatatypeTableWizard.html#datatype_table_wizard33"));
aux1 = insFld(foldersTree, gFld("Chapter 4: Using Repository Editor", "UsingRepositoryEditor.html#chapter_4_using_repository_editor34"));
insDoc(aux1, gLnk("R", "Browsing Design Time Repository", "BrowsingDesignTimeRepository.html#browsing_design_time_repository35"));
insDoc(aux1, gLnk("R", "Filtering the Project Tree", "FilteringTheProjectTree.html#filtering_the_project_tree36"));
insDoc(aux1, gLnk("R", "Uploading a Project", "UploadingAProject.html#uploading_a_project37"));
insDoc(aux1, gLnk("R", "Creating a Project", "CreatingAProject.html#creating_a_project38"));
insDoc(aux1, gLnk("R", "Opening a Project", "OpeningAProject.html#opening_a_project39"));
insDoc(aux1, gLnk("R", "Closing a Project", "ClosingAProject.html#closing_a_project40"));
insDoc(aux1, gLnk("R", "Checking Out a Project", "CheckingOutAProject.html#checking_out_a_project41"));
insDoc(aux1, gLnk("R", "Checking In a Project", "CheckingInAProject.html#checking_in_a_project42"));
insDoc(aux1, gLnk("R", "Defining Project Dependencies", "DefiningProjectDependencies.html#defining_project_dependencies43"));
aux2 = insFld(aux1, gFld("Modifying a Project", "ModifyingAProject.html#modifying_a_project44"));
insDoc(aux2, gLnk("R", "Modifying Project Properties", "ModifyingProjectProperties.html#modifying_project_properties45"));
insDoc(aux2, gLnk("R", "Modifying Project Contents", "ModifyingProjectContents.html#modifying_project_contents46"));
insDoc(aux1, gLnk("R", "Copying a Project", "CopyingAProject.html#copying_a_project50"));
aux2 = insFld(aux1, gFld("Removing a Project", "RemovingAProject.html#removing_a_project51"));
insDoc(aux2, gLnk("R", "Deleting a Project", "DeletingAProject.html#deleting_a_project52"));
insDoc(aux2, gLnk("R", "Erasing a Project", "ErasingAProject.html#erasing_a_project53"));
aux2 = insFld(aux1, gFld("Deploying Projects", "DeployingProjects.html#deploying_projects54"));
insDoc(aux2, gLnk("R", "Creating a Deployment Project", "CreatingADeploymentProject.html#creating_a_deployment_project55"));
insDoc(aux2, gLnk("R", "Defining Deployment Project Descriptors", "DefiningDeploymentProjectDescriptors.html#defining_deployment_project_descriptors56"));
insDoc(aux2, gLnk("R", "Deploying a Deployment Project", "DeployingADeploymentProject.html#deploying_a_deployment_project57"));
insDoc(aux2, gLnk("R", "Opening Deployed Projects", "OpeningDeployedProjects.html#opening_deployed_projects58"));
insDoc(aux2, gLnk("R", "Redeploying Projects", "RedeployingProjects.html#redeploying_projects59"));
insDoc(aux1, gLnk("R", "Comparing Project Versions", "ComparingProjectVersions.html#comparing_project_versions60"));
aux1 = insFld(foldersTree, gFld("Chapter 5: Advanced Functionality", "AdvancedFunctionality.html#chapter_5_advanced_functionality61"));
aux2 = insFld(aux1, gFld("Unit Tests", "UnitTests.html#unit_tests62"));
insDoc(aux2, gLnk("R", "Navigation", "Navigation.html#navigation63"));
insDoc(aux2, gLnk("R", "Run Tests", "RunTests.html#run_tests64"));
insDoc(aux2, gLnk("R", "Creating New Test", "CreatingNewTest.html#creating_new_test65"));
insDoc(aux1, gLnk("R", "Validation", "Validation.html#validation66"));
insDoc(aux1, gLnk("R", "Tracing", "Tracing.html#tracing67"));
insDoc(aux1, gLnk("R", "Benchmarking", "Benchmarking.html#benchmarking68"));
aux1 = insFld(foldersTree, gFld("Index", "Inde1.html#index69"));
