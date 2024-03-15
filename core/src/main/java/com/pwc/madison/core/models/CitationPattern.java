package com.pwc.madison.core.models;

/**
 * Model to represent Citation Pattern elements
 */
public class CitationPattern {

	private String id;
	private String routineId;
	private String routineName;
	private String name;
	private String scope;
	private String regex;

	public CitationPattern(final String id, final String routineId, final String routineName, final String name, final String scope, final String regex) {
		super();
		this.id = id;
		this.routineId = routineId;
		this.routineName = routineName;
		this.name = name;
		this.scope = scope;
		this.regex = regex;
		
	}

	public CitationPattern() {
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public String getRoutineId() {
		return routineId;
	}

	public void setRoutineId(final String routineId) {
		this.routineId = routineId;
	}

	public String getRoutineName() {
		return routineName;
	}

	public void setRoutineName(final String routineName) {
		this.routineName = routineName;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(final String scope) {
		this.scope = scope;
	}

	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

}
