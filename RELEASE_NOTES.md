## Release 1.0.7

This release focuses on making the plugin easier to install on SonarQube 9.x+ deployments (including Helm/Kubernetes), improving rule visibility/loading reliability, improving issue locations, and tightening dependencies.

### Fixes mapped to upstream GitHub issues

- **#70 – How to add namespaces to the plugin?**
  - Added project property `sonar.mule.namespace.properties` to load an additional namespaces `.properties` file (supports `file:...`, `http(s):...`, and `classpath:...`). Namespaces are merged with the built-in Mule 3/4 defaults so rules can reference additional module prefixes.

- **#69 – Version incompatible for sonarqube 9.x and higher**
  - Hardened rule loading so rules are reliably available after restarts:
    - First tries `file:extensions/plugins/rules-3.xml` / `rules-4.xml` (for runtime customization)
    - Falls back to embedded `classpath:rules-3.xml` / `rules-4.xml` (works out-of-the-box)

- **#67 – Compatibility with Sonarqube 9.9.3 LTS?** and **#60 – Not compatible with Sonarqube 9.9 LTS**
  - Addressed the most common 9.9 LTS symptoms (plugin installs but rules/profiles are empty, or server fails due to missing rules files) by embedding default rule stores in the JAR and providing a safe external override mechanism (see #69).

- **#59 – jackson-databind 2.12.6.1 vulnerability**
  - Updated `jackson-databind` dependency to `2.17.2`.

- **#57 – Treat NO coverage Information as 0% coverage**
  - When no MUnit coverage JSON report is found, the plugin now publishes **0% coverage** (writes `lineHits(..., 0)` for each Mule config file line) so Quality Gates can fail as intended instead of passing due to a missing metric.

- **#56 – Only certain files are being validated against rules**
  - Changed scanning to be independent of Sonar “language detection” for `.xml`:
    - Added `sonar.mule.scan.file.suffixes` (default `.xml`) which controls what this plugin scans.
    - Mule file scanning now uses `MuleFilePredicate` (suffix + Mule core namespace) without requiring global Sonar XML language suffix changes.

- **#55 – Sonarqube dce 9.8.0 not installing (rules-3.xml missing / HTML error)**
  - Eliminated the hard dependency on external `extensions/plugins/rules-*.xml` by embedding defaults inside the JAR.
  - Still supports external overrides if you place valid XML rule stores on disk (the plugin will prefer disk rules when present).

- **#54 – Rules/profiles not visible after installing on SonarQube 9.9 LTS**
  - The embedded default rule stores ensure rules and quality profiles are always definable even when the server filesystem does not contain `extensions/plugins/rules-*.xml`.

- **#50 – Plugin not installing with Helm Chart on EKS**
  - Helm installs frequently cannot mount/copy extra `rules-*.xml` files into the SonarQube container. Embedding rule stores in the JAR removes that requirement; external rules are now optional.

- **#31 – XML file replacement (removing .xml suffix from Sonar XML language)**
  - Updated configuration guidance so you do **not** need to remove `.xml` from SonarQube’s XML analyzer.
  - The Mule plugin scans Mule XML files by Mule namespace + `sonar.mule.scan.file.suffixes`, reducing conflicts with the built-in XML analyzer.

- **#30 – Minimum Sonarqube version not documented**
  - Documented **minimum SonarQube version 7.7** (based on `sonar-plugin-api` 7.7).

- **#29 – matches function inside locationHint**
  - Improved `locationHint` evaluation:
    - Added namespace-aware XPath evaluation for location hints.
    - Added a fallback to support simple top-level `matches(nodeSetExpr, 'regex')` style location hints even though the standard JDK XPath engine is XPath 1.0.

- **#25 – Mule code doesn't generate coverage with java class**
  - Improved coverage matching between the JSON report and analyzed files by supporting both normalized relative paths and basenames, reducing “coverage not applied” cases when reports include paths or different path separators.

- **#19 – Switching off .xml extension for XML language disables other XML checks**
  - Avoids forcing users to disable `.xml` for the XML analyzer; Mule scanning is now independent and configurable via `sonar.mule.scan.file.suffixes`.

- **#17 – Quality Gate used but is not working** and **#58 / #33 – PR vs branch analysis differences**
  - Quality Gate / PR decoration behavior is largely governed by SonarQube’s PR analysis model (often limited to changed lines/new code). This release improves issue anchoring (at least line 1 when no hint is available) and stabilizes “application-scope” issues, but PR-vs-branch parity may still require SonarQube configuration (New Code definition, PR decoration settings, etc.).

- **#15 – Line numbers not displayed on issues**
  - Improved issue locations:
    - If a `locationHint` is provided, it’s evaluated with namespace support and used to attach the issue to a precise XML range.
    - If no hint is available, issues are anchored at line 1 to ensure SonarQube can display a line number instead of “file-level only”.

- **#66 – Update Unlicense URL**
  - Updated `UNLICENSE.md` link to `https://unlicense.org/`.

- **#65 – Fix Plugin URL**
  - Updated `pom.xml` project URL to `https://github.com/mulesoft-catalyst/mule-sonarqube-plugin`.

### Notes / behavior changes
- **Rules packaging**: `rules-3.xml` and `rules-4.xml` are now embedded in the plugin JAR by default. If you want to customize rules without rebuilding, place rule files at `extensions/plugins/rules-3.xml` and/or `extensions/plugins/rules-4.xml` on the SonarQube server.
- **Scanning property**: configure scanned suffixes with `sonar.mule.scan.file.suffixes` (default `.xml`). This is separate from SonarQube language detection.

## Release 1.0.8

This release focuses on security hardening for safer use in CI/PR analysis environments.

### Security fixes

- **XXE hardening for all XML parsing**
  - All JDOM XML parsing now uses a hardened parser configuration that:
    - disallows DOCTYPE declarations
    - disables external general/parameter entities
    - disables external DTD loading
    - disables entity expansion
  - This mitigates XXE-driven **SSRF**, local file disclosure, and entity-expansion DoS when scanning untrusted Mule XML.

- **Restrict `sonar.mule.namespace.properties` to local-only**
  - Namespace extension specs now support only:
    - `classpath:...`
    - `file:...`
    - plain filesystem paths
  - `http(s)` URLs are rejected to prevent SSRF via project configuration.

## Release 1.0.9

This release adds a project naming convention rule for Mule 4 and the supporting `project` scope in the rule engine.

### Rules
- **Project naming convention (Mule 4)**
  - Added a new Mule 4 ruleset `category="project"` with a rule that validates the Mule artifact project name (`/mule:mule-artifact/project`) ends with one of:
    - `-api`, `-sapi`, `-xapi`, `-papi`, `-impl`, `-mcp`, `-agent`
  - Implemented `applies="project"` scope so rules can validate SonarQube project metadata when needed (separate from the Mule XML-based rule above).

## Release 1.0.10

This release updates the Mule 4 project naming convention rule to validate `mule-artifact/project` via XPath (instead of SonarQube project metadata), ensuring enforcement is driven by the Mule artifact descriptor committed to source control.

## Release 1.0.11

This release expands the built-in Mule 4 namespace mappings to cover more commonly used modules/connectors, improving rule and metric evaluation for projects using those namespaces.

## Release 1.1.0

This release introduces **v1.1 rules** backed by the standard `javax.xml` (JAXP) XPath engine, enabling **node-based rules** with accurate issue locations and multiple issues per rule per file.

### Credits

- Thanks to [@trentbowman](https://github.com/trentbowman) for originally proposing the move to `javax.xml` (JAXP) XPath evaluation and providing the fixes required to make it work reliably.

### Fixes / improvements

- **Build/packaging: generate proper Sonar plugin manifest**
  - Updated `pom.xml` packaging to `sonar-plugin` so the built artifact includes required plugin manifest headers (e.g., `Plugin-Key`, `Plugin-Class`, `Plugin-Version`) and can be loaded by SonarQube.
  - Removed the unused `log4j-slf4j-impl` dependency to satisfy `sonar-packaging-maven-plugin` dependency checks and avoid shipping an unnecessary logging backend inside the plugin.

- **#23 – Enable better issue location reporting**
  - Added **node-scope** rules for v1.1 that return the exact violating nodes (elements/attributes). SonarQube issues are now anchored directly to those nodes without requiring a separate `locationHint`.

- **#15 – Line numbers not displayed on issues**
  - v1.1 rules attach issues to concrete XML ranges (element name / attribute value) when using node-scope, improving line number visibility and enabling multiple violations per file.

### Major behavior / format changes

- **Rule versioning (`pluginVersion`)**
  - Existing rules continue to work as **v1.0** (JDOM2 path) by default.
  - Rules (or rulesets) that set `pluginVersion="1.1"` use the new `javax.xml` XPath path.

- **New rule scope: `node` (v1.1)**
  - **application**: must evaluate `true` for at least one file, else an issue is raised.
  - **file**: must evaluate `true` per file, else a file-level issue is raised.
  - **node**: selects violating nodes; an issue is raised for **each** selected node.
  - For v1.1 rules, omitting `applies=...` defaults to **node** behavior.

- **Additional built-in rules file**
  - The plugin now loads `rules-4-custom.xml` in addition to `rules-4.xml` (supports both filesystem override under `extensions/plugins/` and embedded classpath defaults).

