package com.mulesoft.services.tools.validation;

/**
 * Common constants and enums used by the validation/rules subsystem.
 *
 * <p>This interface centralizes:
 * <ul>
 *   <li>rule applicability scopes (file/application/project)</li>
 *   <li>severity and type identifiers</li>
 *   <li>supported ruleset names</li>
 * </ul>
 *
 * @author franco.parma
 * @version 1.1.0
 * @since 1.1.0
 */
public interface Constants {

	interface Applicability {
		/**
		 * Scope value indicating the rule applies to each individual file.
		 */
		public String FILE = "file";
		/**
		 * Scope value indicating the rule applies at the application level.
		 */
		public String APPLICATION = "application";
		/**
		 * Scope value indicating the rule applies at the project metadata level.
		 */
		public String PROJECT = "project";
	}

	interface Severity {
		/**
		 * Highest severity level.
		 */
		public String BLOCKER = "BLOCKER";
		/**
		 * Critical severity level.
		 */
		public String CRITICAL = "CRITICAL";

		/**
		 * Major severity level.
		 */
		public String MAJOR = "MAJOR";
		/**
		 * Minor severity level.
		 */
		public String MINOR = "MINOR";
		/**
		 * Informational severity level.
		 */
		public String INFO = "INFO";
	}

	interface Type {
		/**
		 * Code smell rule type identifier.
		 */
		public String CODE_SMELL = "code_smell";
		/**
		 * Bug rule type identifier.
		 */
		public String BUG = "bug";
		/**
		 * Vulnerability rule type identifier.
		 */
		public String VULNERABILITY = "vulnerability";
	}

	interface Ruleset {
		/**
		 * Mule 3 ruleset identifier.
		 */
		public String MULE3 = "mule3";
		/**
		 * Mule 4 ruleset identifier.
		 */
		public String MULE4 = "mule4";
	}

	/**
	 * Strongly-typed rule types used by rule definition code.
	 */
	public enum Types {
		CODE_SMELL, BUG, VULNERABILITY
	}

	/**
	 * Strongly-typed severities used by rule definition code.
	 */
	public enum Severities {
		BLOCKER, CRITICAL, MAJOR, MINOR, INFO
	}

}
