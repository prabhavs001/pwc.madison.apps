package com.pwc.madison.core.constants;

public final class WorkflowConstants {

	private WorkflowConstants() {
		super();
	}
	
	// Constants used for the Full Cycle and Simplified WF
	public static final String EMAIL_APPROVAL_BODY_TITLE = "The following document(s) are available for your approval:";
	public static final String EMAIL_ENGAGE_BODY_TITLE = "The following document(s) require additional revierwers/approvers. Please begin new workflow.";
	public static final String EMAIL_PUBLISH_BODY_TITLE = "The following document(s) are approved and available for publishing:";
	public static final String EMAIL_SITE_GENERATION_BODY_TITLE = "The following approved document(s) have been published:";
	public static final String EMAIL_REVIEW_BODY_TITLE = "The following document(s) are available for your review:";
	public static final String EMAIL_BACK_AUTHOR_BODY_TITLE = "The following document(s) require your review of comments and edits made:";
	public static final String EMAIL_AUHTOR_FINAL_BODY_TITLE = "The following approved document(s) have been published:";

	public static final String EMAIL_BACK_AUTHOR_SUBJECT = "PwC Madison Document has been sent back to you for comment review/ edits  -";
	public static final String EMAIL_REVIEW_SUBJECT = "PwC Madison Document ready for your review  -";
	public static final String EMAIL_BACK_REVIEWER_SUBJECT = "PwC Madison Document has been sent back to you for comment review/ edits    -";
	public static final String EMAIL_APPROVER_SUBJECT = "PwC Madison Document ready for your approval  -";
	public static final String EMAIL_ENGAGE_SUBJECT = "PwC Madison Document - please add additional reviewers/approvers for -";
	public static final String EMAIL_PUBLISHER_SUBJECT = "PwC Madison Document ready for publishing  -";
	public static final String EMAIL_AUHTOR_FINAL_SUBJECT = "PwC Madison Document has been published - ";

	public static final String SEND_AUTHOR_EMAIL_STEP = "Send completion email to author";
	public static final String GOTO_REVIEWER_STEP = "Goto reviewer";
	public static final String SEND_BACK_TO_AUTHOR_STEP = "Send back to author";

	public static final String WORKFLOW_ID = "workflowId";
}
