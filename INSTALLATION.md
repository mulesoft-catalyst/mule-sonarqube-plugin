# Mule SonarQube Plugin Installation and Build Guide

This document explains how to:
- Build the plugin jar
- Install it into SonarQube
- Configure SonarQube and projects to use Mule rules
- Keep default SonarQube XML checks enabled

## 1. Prerequisites

You need one of the following build environments:

1. Local build tools:
- Java 21+
- Maven 3.x

2. Container-based build tools (no local Java/Maven required):
- Docker

You also need:
- A running SonarQube server
- Access to SonarQube server filesystem (or container volume) to copy plugin files

## 2. Build the Plugin Jar

### Option A: Build locally

From repository root:

```bash
mvn clean package sonar-packaging:sonar-plugin -Dlanguage=mule
```

### Option B: Build with Docker (recommended if local Java/Maven are unavailable)

From repository root:

```bash
docker run --rm -v "$PWD":/workspace -w /workspace maven:3.9.9-eclipse-temurin-21 \
  mvn clean package sonar-packaging:sonar-plugin -Dlanguage=mule
```

### Build output

The generated plugin jar is created in:

```text
target/mule-validation-sonarqube-plugin-<version>-mule.jar
```

## 3. Install Files into SonarQube

Copy these files to SonarQube plugin directory:

1. Plugin jar:
- `target/mule-validation-sonarqube-plugin-<version>-mule.jar`

2. Rule definitions (required by this plugin at runtime):
- `src/test/resources/rules-3.xml`
- `src/test/resources/rules-4.xml`

Destination directory on SonarQube host:

```text
<SONARQUBE_HOME>/extensions/plugins/
```

Notes:
- The plugin loads rules from `file:extensions/plugins/rules-3.xml` and `file:extensions/plugins/rules-4.xml`.
- Filenames must remain exactly `rules-3.xml` and `rules-4.xml`.

After copying files, restart SonarQube.

## 4. SonarQube Configuration

### 4.1 XML analyzer coexistence

This plugin is configured to work on the `xml` language so built-in SonarQube XML checks can run together with Mule rules.

Do not remove `.xml` from SonarQube XML settings.

### 4.2 Quality profiles

After restart, check Quality Profiles and activate the profile appropriate for your Mule runtime:

- MuleSoft Rules for Mule 3.x
- MuleSoft Rules for Mule 4.x

Because profiles are attached to XML language, assign the selected profile to the target project as needed.

### 4.3 Optional project properties

You can tune rule category filtering:

```properties
sonar.mule.ruleset.categories=flows,configuration,application
```

If omitted, all active rules are considered.

## 5. Run Analysis on a Project

Example scan:

```bash
mvn sonar:sonar \
  -Dsonar.host.url=http://<your-sonarqube-host>:9000 \
  -Dsonar.sources=src/
```

Optional additional property:

```bash
-Dsonar.mule.ruleset.categories=flows,configuration
```

## 6. Verify Installation

After first analysis, validate:

1. SonarQube shows issues from Mule repositories (`mule3-repository` or `mule4-repository`).
2. SonarQube also reports built-in XML issues for the same project.
3. Server logs do not show plugin load errors.

## 7. Troubleshooting

### Plugin not visible in SonarQube

- Confirm jar is in `<SONARQUBE_HOME>/extensions/plugins/`.
- Confirm SonarQube was restarted.
- Check SonarQube logs for plugin loading errors.

### No Mule rules reported

- Confirm `rules-3.xml` and `rules-4.xml` are in `<SONARQUBE_HOME>/extensions/plugins/`.
- Confirm filenames are exact.
- Confirm the correct Mule profile is active for the project.

### XML rules disappeared

- Confirm `.xml` is still configured under SonarQube XML settings.
- Confirm no other custom plugin is overriding XML language ownership.
