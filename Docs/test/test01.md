![](../OpenL60.png "")

<DIV markdown="1" style="background:LightCyan;">
	
<p style="color:red;">This is a red paragraph inside a blue div.</p>
<p>Unfortunately, Markdown elements cannot be put inside html as seen in the lines below</p>

added by TW
	
# Test01
	
## Test 02

End of div test
	
</DIV>


## AdrianTest1

<span style="background:LightCyan;">
more testing

*here is some markdown*
### And here
</span>
	
Break test



Break after cat <br />

| **Dogs**                   | **Cats**                   |
|----------------------------|----------------------------|
| ● Pluto <br />● Max<br />● Beethoven                      | 1. The one from Tabitha<br />2. The cat from outer space       |
| More things in Dogs column | More things in Cats column |


Some text just testing if the br tag works properly. Let me place one here <br> in the middle of the sentence.

## AdrianTest2

Now test blockquote with the guide info

> Release 5.26
>
> Document number: TP_OpenL_DG_3.6_LSh
> 
> Revised: 10-13-2022
> 
> OpenL Tablets Documentation is licensed under a Creative Commons Attribution 3.0 United States License.

And here is another pgf

Now lets see if it is better as code

```
Release 5.26

Document number: TP_OpenL_DG_3.6_LSh

Revised: 10-13-2022

OpenL Tablets Documentation is licensed under a Creative Commons Attribution 3.0 United States License.
```

And here is another pgf

Now test admonitions

!!! note
    Need to indent to make the note content.
    You should note that the title will be automatically capitalized.
    Can it have another line
    ### Can it contain formatting?
    **like this**
    *or this*

!!! danger "Don't try this at home"
    ...
    
!!! important ""
    This is an admonition box without a title.
    
    
 NOW SOME NUMBERED HEADING TESTS
 
 Image using markdown
 
![](../img/indexpage/indexpageimage00.png)
 
 
 
 
 Full width image using html
 
<img src="../../img/indexpage/indexpageimage00.png">


Reduced width image using html

<img src="../../img/indexpage/indexpageimage00.png" width = "400">


Reduced width and centered image using html and putting it inside a tag

<center>
<img src="../../img/indexpage/indexpageimage00.png" width = "400">
</center>

and again

<img src="../../img/indexpage/indexpageimage00.png" width = "50%" display:block margin-left:auto margin-right:auto>

# LIST TESTS FOLLOW


   **Standard numbered list**

1.  Monkey
2.  Chicken
3.  Weddings

    **Another numbered list**

4.  Monkey
5.  Chicken
6.  Weddings

    Note the lists above actually have two issues: i. they are built on bullet style not numbers ii. The text introducing the list has a list style applied to it.

    In contrast, the lists below are “pure”

**This list created by clicking numbers button. Stays “Normal”**

1.  Monkey
2.  Chicken
3.  Weddings

**This list created by clicking numbers button. Stays “Normal”**

1.  Hat
2.  Shoe
3.  Jumper
Now here is a list within a list

- Bullets
 - test
 - 
  - 2 spaces
    - 4 spaces
        - 8 spaces
- more
	
Conclusion (looking at RTD, not Github): 2 spaces does nothing. 4 spaces gives first indent. 8 spaces gives second indent

A new list but with tabs:

- Bullets
	- 1 tab
		- 2 tabs
       
            - 3 indents
                - 4 indents
- more

Conclusion (looking at RTD, not Github): 1 tab gives first indent, 2 tabs gives second indent and so on.

Now a numbered list with subnumbers

1. Apples
	1. Royal Gala. I typed 8 spaces to get this indend.t
	1. Other
    1. now with 4 spaces
    2. now again

1. Pears
2. 


More tests


8.  Dsfdfs
	-   Test
	-   test
1.  Sdfsdf
2.  Dfsfs


**Bullet list with sub-numbers**
    

-   Dfsf
	1.  Fsf
	2.  Dfs
	3.  
-   Dfsf
-   Dfs

# CODE TEST FOLLOWS

## Following style is Code Lines.

The following code fragment is an example of the rules project descriptor:

```
<project>
	<!-- Project name. -->
	<name>Project name</name>
	<!-- Optional. Comment string to project. -->
	<comment>comment</comment>

	<!-- OpenL project includes one or more rules modules.  -->
	<modules>

		<module>
			<name>MyModule1</name>
						
<!-- 
				Rules document which is usually an excel file in the project. 
			-->
			<rules-root path="MyModule1.xls"/>

		</module>	
		
		<module>
			<name>MyModule2</name>
			
<!-- 
				Rules document which is usually an excel file in the project. 
			-->
			<rules-root path="MyModule2.xls"/>
			<method-filter>
				<includes>
					<value> * </value>
				</includes>
			</method-filter>
		</module>	
	</modules>

<dependencies>
		<dependency>
			<name>projectName</name>
			<autoIncluded>false</autoIncluded>
		</dependency>
	</dependencies>
	<properties-file-name-pattern>{lob}</properties-file-name-pattern>
	<properties-file-name-processor>default.DefaultPropertiesFileNameProcessor</properties-file-name-processor>
	<!-- Project's classpath (list of all source dependencies). -->
	<classpath>
		<entry path="path1"/>
		<entry path="path2"/>
	</classpath>
	
</project>
```

## Following inline style is Code.

Resolving strategies are defined via `org.openl.rules.project.resolving.ResolvingStrategy` SPI.

1.  Select the **openl-simple-project-archetype** menu item.
2.  **Following style is Code**

As an alternative way is using the following command:  
`mvn archetype:generate 
–DarchetypeGroupId=org.openl.rules 
–DarchetypeArtifactId=openl-simple-project-archetype
\-DarchetypeVersion=5.X.X`

1.  Follow with the Maven creation wizard.

Proceed as follows:

1.  In the project `src` folder, create an interface as follows:

```
public interface Simple {
		void hello1(int i);
}
```

1.  Create a wrapper object as follows: (style is List Code)

```
import static java.lang.System.out;
import org.openl.rules.runtime.RulesEngineFactory;

public class Example {

		public static void main(String[] args) {
			//define the interface
			RulesEngineFactory<Simple > rulesFactory = 
				new RulesEngineFactory<Simple>("TemplateRules.xls", 
									Simple.class);

			Simple rules = (Simple) rulesFactory.newInstance();
			rules.hello1(12);
		
		}
}
```

## Following style is Code

When the class is run, it executes and displays **Good Afternoon, World!**

The following example illustrates using a wrapper with a generated interface in runtime: Style is Code.

```
public static void callRulesWithGeneratedInterface(){
	// Creates new instance of OpenL Rules Factory
	RulesEngineFactory<?> rulesFactory = 
new RulesEngineFactory<Object>("TemplateRules.xls");
				//Creates new instance of dynamic Java Wrapper for our lesson
Object rules = rulesFactory.newInstance();
        
       //Get current hour
	Calendar calendar = Calendar.getInstance();
	int hour = calendar.get(Calendar.HOUR_OF_DAY);

	Class<?> clazz = rulesFactory.getInterfaceClass();

try{
Method method = clazz.getMethod("hello1”, int.class);
out.println("* Executing OpenL rules...\n");
method.invoke(rules, hour);
}catch(NoSuchMethodException e){
}catch (InvocationTargetException e) {
}catch (IllegalAccessException e) {
}
}
```

Got a question? Drop us a line: <support@writage.com>.

