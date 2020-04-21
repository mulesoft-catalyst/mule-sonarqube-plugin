package com.mulesoft.services.tools.validation;

/**
 * Rules Severity
 * 
 * @author franco.parma
 *
 */
public interface Constants {

	interface Applicability {
		public String FILE = "file";
		public String APPLICATION = "application";
	}

	interface Severity {
		public String BLOCKER = "BLOCKER";
		public String CRITICAL = "CRITICAL";

		public String MAJOR = "MAJOR";
		public String MINOR = "MINOR";
		public String INFO = "INFO";
	}

	interface Type {
		public String CODE_SMELL = "code_smell";
		public String BUG = "bug";
		public String VULNERABILITY = "vulnerability";
	}

	interface Ruleset {
		public String MULE3 = "mule3";
		public String MULE4 = "mule4";
	}

	public enum Types {
		CODE_SMELL, BUG, VULNERABILITY
	}

	public enum Severities {
		BLOCKER, CRITICAL, MAJOR, MINOR, INFO
	}

}
