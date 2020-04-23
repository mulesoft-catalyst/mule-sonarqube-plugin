# Mule Sonarqube Validation Plugin

Sonarqube plugin:

* Ensure the consistency of Mule Applications by a set of predefined rules focused on specific quality attributes like: _Conceptual Integrity_, _Maintainability_, _Reusability_, _Security_, _Traceability_ and _Availability_.

**This module requires Java 8**.

## Prerequisites

## Plugin Generation

1. Download the module source code.
2. Open a terminal window and browse to module root folder.
3. Build the mule plugin for Mule rules running `mvn clean package sonar-packaging:sonar-plugin -Dlanguage=mule`.

## Maven Configuration
1. In your settings file add sonar profile.
 ```
 <profile>
           <id>sonar</id>
           <activation>
               <activeByDefault>true</activeByDefault>
           </activation>
           <properties>
               <!-- Optional URL to server. Default value is http://localhost:9000 -->
               <sonar.host.url>
                 http://localhost:9000
               </sonar.host.url>
           </properties>
   </profile>
 ```


## Sonar Configuration
1. Copy the generated file, mule-validation-sonarqube-plugin-{version}-mule.jar to *sonar-home*/extensions/plugins
2. Copy rules [Mule 3 Rules](https://github.com/mulesoft-consulting/mule-validation-toolkit/blob/master/mule-validation-xpath-core/src/main/resources/rules-3.xml) or [Mule 4 Rules](https://github.com/mulesoft-consulting/mule-validation-toolkit/blob/master/mule-validation-xpath-core/src/main/resources/rules-4.xml) to *sonar-home*/extensions/plugins
3. Start the server

## Project Configuration
### Alternative 1
1. Modify project's pom.xml file to add language and location of the source files to analysis.
```
<properties>
		...
		<sonar.sources>src</sonar.sources>
		...	
</properties>
```
2. Analyze the project executing mvn sonar:sonar

### Alternative 2
1. Analyze the project executing mvn sonar:sonar -Dsonar.sources=src

## Release Notes

#### 1.0.1
##### Changes
	- Refactor - One single project. mule-validation-xpath-core dependency is not longer needed

#### 1.0.0
##### Changes
	- Added Line Number processing in Coverage Sensor

#### 0.0.11
##### Changes
	- Added Rule Template. 
	  You could create new custom rules from SonarQube Console.

#### 0.0.10
##### Changes
	- Added Rule description. 
	  Now you could add an extended description to the rules. The description supports HTML.

#### 0.0.9
##### Changes
	- Added property sonar.mule.ruleset.categories. It allows to filter ruleset categories to apply in the project. 
	  Value should be a string separated by commas. 
	  For example run, `mvn sonar:sonar -Dsonar.mule.ruleset.categories=flows` to only apply the flows category ruleset
	
#### 0.0.8
##### Changes
	- Bug Fixes
	- Added Metric LOC
	- Added Support for SonarQube 7.2.1
#### 0.0.7
##### Changes
    - Added new rules + http namespace   
#### 0.0.6
##### Changes
     - Added Munit Coverage and Minor Improvements    
#### 0.0.5
##### Changes
	- Externalized Rules to $SONARQUBE_HOME/extensions/plugins/[rules-3.xml|rules-4.xml]. 
  Basic set of rules for [mule3](https://github.com/mulesoft-consulting/mule-validation-toolkit/blob/master/mule-validation-xpath-core/src/main/resources/rules-3.xml), [mule4](https://github.com/mulesoft-consulting/mule-validation-toolkit/blob/master/mule-validation-xpath-core/src/main/resources/rules-4.xml)
  
  **Adding new rules: if it is needed to add a new namespace you will have to added to [mule3-namespaces](https://github.com/mulesoft-consulting/mule-validation-toolkit/blob/master/mule-validation-xpath-core/src/main/resources/namespace-3.properties) or to [mule4-namespaces](https://github.com/mulesoft-consulting/mule-validation-toolkit/blob/master/mule-validation-xpath-core/src/main/resources/namespace-4.properties) and regenerate all plugins**
  
  **Updating existing rule: if you need to update an existing rule, you will also have to update its id number**
	
#### 0.0.4
##### Changes
	- Added Measures
		- Number of Flows
		- Number of SubFlows	
		- Number of DW Transformations
		- Application Complexity

## Final Note
Enjoy and provide feedback / contribute :)
