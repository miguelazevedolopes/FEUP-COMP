# Compilers Project

## GROUP: comp2022-2b



(Names, numbers, self assessment, and contribution of the members of the group to the project according to:)


NAME1: Ana Rita Antunes Ramada, NR1: 201904565, GRADE1: 18, CONTRIBUTION1: 25% <br>
NAME2: Margarida Nazaré Pereira dos Santos, NR2: 201908209, GRADE2: 18, CONTRIBUTION2: 25% <br>
NAME3: Maria Sofia Diogo Figueiredo, NR3: 201904675, GRADE3: 18, CONTRIBUTION3: 25% <br>
NAME4: Miguel Azevedo Lopes, NR4: 201704590, GRADE4: 18, CONTRIBUTION4: 25% <br>


GLOBAL Grade of the project: 18


**SUMMARY**: The tool compiles Jmm code into JVM code. It parses the code, analyses it semantically, generates intermediate ollir code 
and finally converts it into jasmin.





**SEMANTIC ANALYSIS**: Our compiler verifies all the semantic rules listed in the project's specification. This includes:

***Type Verification***

- Verifies if variable names used in the code have a corresponding declaration, either as a local variable, a method parameter or a field of the class (if applicable). 

- Checks if operands types are compatible with the operation (e.g. int + boolean is an error because + expects two integers.)   

- Checks that arrays aren't used in arithmetic operations (e.g. array1 + array2 is an error) 

- Checks if array access is done over an array 	  

- Checks if array access index is an expression of type integer	  

- Checks if type of the assignee is compatible with the assigned (an_int = a_bool is an error)  

- Checks if expressions in conditions return a boolean (if(2+3) is an error)

***Function Verification***

- When calling methods of the class declared in the code, verifies if the types of arguments of the call are compatible with the types in the method declaration  

- In case the method does not exist, verifies if the class extends another class and reports an error if it does not. If the class extends another class, it assumes the method exists in one of the super classes, and that it is being correctly called 

- When calling methods that belong to other classes other than the class declared in the code, verifies if the classes are being imported 




**CODE GENERATION**: 
Everything was implemented as planned, excluding optimizations.<br>
Ollir code is generated using a strategy that saves prefix code and actual code for each instruction.
This facilitates the conversion to 3 address Ollir instructions.<br>
Some optimizations are done while generating Ollir, such as constant folding and some optimization in "if else" statements.




**PROS**:
- The compiler is robust
- Constant folding (with -o option)
- Some dead code elimination on "if else" statements (with -o option)


**CONS**: 
- Not many optimizations
- Ollir does not have minimal number of instructions for some cases



For this project, you need to install [Java](https://jdk.java.net/), [Gradle](https://gradle.org/install/), and [Git](https://git-scm.com/downloads/) (and optionally, a [Git GUI client](https://git-scm.com/downloads/guis), such as TortoiseGit or GitHub Desktop). Please check the [compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html) for Java and Gradle versions.

## Project setup

There are three important subfolders inside the main folder. First, inside the subfolder named ``javacc`` you will find the initial grammar definition. Then, inside the subfolder named ``src`` you will find the entry point of the application. Finally, the subfolder named ``tutorial`` contains code solutions for each step of the tutorial. JavaCC21 will generate code inside the subfolder ``generated``.

## Compile and Running

To compile and install the program, run ``gradle installDist``. This will compile your classes and create a launcher script in the folder ``./build/install/comp2022-00/bin``. For convenience, there are two script files, one for Windows (``comp2022-00.bat``) and another for Linux (``comp2022-00``), in the root folder, that call tihs launcher script.

After compilation, a series of tests will be automatically executed. The build will stop if any test fails. Whenever you want to ignore the tests and build the program anyway, you can call Gradle with the flag ``-x test``.

## Test

To test the program, run ``gradle test``. This will execute the build, and run the JUnit tests in the ``test`` folder. If you want to see output printed during the tests, use the flag ``-i`` (i.e., ``gradle test -i``).
You can also see a test report by opening ``./build/reports/tests/test/index.html``.

## Checkpoint 1
For the first checkpoint the following is required:

1. Convert the provided e-BNF grammar into JavaCC grammar format in a .jj file
2. Resolve grammar conflicts, preferably with lookaheads no greater than 2
3. Include missing information in nodes (i.e. tree annotation). E.g. include the operation type in the operation node.
4. Generate a JSON from the AST

### JavaCC to JSON
To help converting the JavaCC nodes into a JSON format, we included in this project the JmmNode interface, which can be seen in ``src-lib/pt/up/fe/comp/jmm/ast/JmmNode.java``. The idea is for you to use this interface along with the Node class that is automatically generated by JavaCC (which can be seen in ``generated``). Then, one can easily convert the JmmNode into a JSON string by invoking the method JmmNode.toJson().

Please check the JavaCC tutorial to see an example of how the interface can be implemented.

### Reports
We also included in this project the class ``src-lib/pt/up/fe/comp/jmm/report/Report.java``. This class is used to generate important reports, including error and warning messages, but also can be used to include debugging and logging information. E.g. When you want to generate an error, create a new Report with the ``Error`` type and provide the stage in which the error occurred.


### Parser Interface

We have included the interface ``src-lib/pt/up/fe/comp/jmm/parser/JmmParser.java``, which you should implement in a class that has a constructor with no parameters (please check ``src/pt/up/fe/comp/CalculatorParser.java`` for an example). This class will be used to test your parser. The interface has a single method, ``parse``, which receives a String with the code to parse, and returns a JmmParserResult instance. This instance contains the root node of your AST, as well as a List of Report instances that you collected during parsing.

To configure the name of the class that implements the JmmParser interface, use the file ``config.properties``.

### Compilation Stages 

The project is divided in four compilation stages, that you will be developing during the semester. The stages are Parser, Analysis, Optimization and Backend, and for each of these stages there is a corresponding Java interface that you will have to implement (e.g. for the Parser stage, you have to implement the interface JmmParser).


### config.properties

The testing framework, which uses the class TestUtils located in ``src-lib/pt/up/fe/comp``, has methods to test each of the four compilation stages (e.g., ``TestUtils.parse()`` for testing the Parser stage). 

In order for the test class to find your implementations for the stages, it uses the file ``config.properties`` that is in root of your repository. It has four fields, one for each stage (i.e. ``ParserClass``, ``AnalysisClass``, ``OptimizationClass``, ``BackendClass``), and initially it only has one value, ``pt.up.fe.comp.SimpleParser``, associated with the first stage.

During the development of your compiler you will update this file in order to setup the classes that implement each of the compilation stages.
