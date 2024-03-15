package com.pwc.madison.core.userreg.models.response;

/**
 * 
 * Model to represent the response data from Viewpoint edit profile.
 *
 */
public class EditProfileAndChangePasswordResponse {

    private int editProfileStatus;
    private EditProfileResponse editProfileResponse;
    private CompleteProfileAnalyticResponse completeProfileAnalyticResponse;

    public int getEditProfileStatus() {
        return editProfileStatus;
    }

    public void setEditProfileStatus(int editProfileStatus) {
        this.editProfileStatus = editProfileStatus;
    }

    public EditProfileResponse getEditProfileResponse() {
        return editProfileResponse;
    }

    public void setEditProfileResponse(EditProfileResponse editProfileResponse) {
        this.editProfileResponse = editProfileResponse;
    }

    public CompleteProfileAnalyticResponse getCompleteProfileAnalyticResponse() {
		return completeProfileAnalyticResponse;
	}

	public void setCompleteProfileAnalyticResponse(CompleteProfileAnalyticResponse completeProfileAnalyticResponse) {
		this.completeProfileAnalyticResponse = completeProfileAnalyticResponse;
	}

	public EditProfileAndChangePasswordResponse(int editProfileStatus, EditProfileResponse editProfileResponse) {
        super();
        this.editProfileStatus = editProfileStatus;
        this.editProfileResponse = editProfileResponse;
    }

    public EditProfileAndChangePasswordResponse() {
    }

    @Override
    public String toString() {
        return "EditProfileAndChangePasswordResponse [editProfileStatus=" + editProfileStatus
                 + ", editProfileResponse=" + editProfileResponse + "]";
    }

}
