OpenL simple project

STRUCTURE

Project has the following structure:

		|-- template													project root
		|  |-- .classpath												Eclipse classpath configuration file (for Eclipse only)
		|  |-- .info													Eclipse source folders configuration file (for Eclipse only)
		|  |-- .project													Eclipse project file (for Eclipse only)
		|  |-- build.properties											Eclipse build configuration file (for Eclipse only)
		|  |-- Generate Template Wrapper.launch							Wrapper generator launch file (for Eclipse only)
		|  |-- pom.xml													pom.xml file (for Maven based build process)
		|  |-- readme.txt												Current file
		|  
		|  |-- build													Build configurations
		|  |  |-- GenerateJavaWrapper.build.xml							Ant build scipt (defines steps to generate wrapper class)
		|  
		|  |-- gen														Default folder which used as target folder for generated classes
		|  |  
		|  |  |-- template
		|  |  |  |-- TemplateJavaWrapper.java							Generated wrapper class
		|  
		|  |-- META-INF
		|  |  |-- MANIFEST.MF											Eclipse manifest file (for Eclipse only)
		|  
		|  |-- rules													Default forlder which contains files with rules.
		|  |  |-- TemplateRules.xls										File with rules.
		|  |  
		|  
		|  |-- src														Java source folder
		|  |  
		|  |  |-- template
		|  |  |  |-- Main.java											Sample class which uses wrapper class.



WRAPPER GENERATION CONFIGURATION

Ant build file is used to generated wrapper class and contains the following configuration:

		<project name="GenJavaWrapper" default="generate" basedir="../">

			<taskdef name="openlgen" classname="org.openl.conf.ant.JavaWrapperAntTask"/>

			<target name="generate">
				<echo message="Generating template Wrapper"/>

				<openlgen openlName="org.openl.xls" userHome="." 
					srcFile="rules/TemplateRules.xls"
					targetClass="gen.template.TemplateJavaWrapper"
					displayName="New Project - template
					targetSrcDir="gen">
				</openlgen>
			</target>

		</project>

where:
	srcFile - path to file with rules
	targetClass - wrapper class which will be generated
	displayName - name of project (used by OpenL WebStudio)
	targetSrcDir - folder which used as target folder for generated classes
