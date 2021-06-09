OpenL Rules Project.

PROJECT STRUCTURE

Project structure uses default Maven project structure and contains the following files:

|- ${artifactId}/                                           Project root folder
|  |- pom.xml                                               Maven project file
|  |
|  |- src/
|  |  |
|  |  |- main/
|  |  |  |
|  |  |  |- openl/                                          Contains all OpenL-related resources (rules, xml etc.)
|  |  |  |  |
|  |  |  |  |- rules.xml                                    OpenL project descriptor
|  |  |  |  |- rules-deploy.xml                             OpenL project deployment configuration
|  |  |  |  |- rules/
|  |  |  |  |  |- Algorithm-CW-01012020-01012020.xlsx       File with rules
|  |  |
|  |  |- test/                                              Contains tests run upon build generation
|  |  |  |
|  |  |  |- openl/                                          OpenL project for testing the rules
|  |  |  |  |
|  |  |  |  |- rules.xml                                    OpenL project descriptor, contains dependency on the tested OpenL project
|  |  |  |  |- rules/
|  |  |  |  |  |- Test.xlsx                                 File with OpenL tests

OPENL PROJECT DESCRIPTOR

OpenL project descriptor is just simple xml file which contains information about current OpenL project. It is used
by OpenL WebStudio and Rule Services deploy manager to obtain information about project.

For more information about OpenL Tablets visit our site https://openl-tablets.org .
