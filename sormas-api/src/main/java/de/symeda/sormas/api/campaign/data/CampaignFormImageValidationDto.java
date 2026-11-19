package de.symeda.sormas.api.campaign.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CampaignFormImageValidationDto implements Serializable {

	private static final long serialVersionUID = 1L;

	private boolean valid;
	private List<String> prohibitedLabelsDetected;
	private String message;

	public CampaignFormImageValidationDto() {
		this.valid = true;
		this.prohibitedLabelsDetected = new ArrayList<>();
		this.message = null;
	}

	public CampaignFormImageValidationDto(boolean valid, List<String> prohibitedLabelsDetected, String message) {
		this.valid = valid;
		this.prohibitedLabelsDetected = prohibitedLabelsDetected != null ? prohibitedLabelsDetected : new ArrayList<>();
		this.message = message;
	}

	public static CampaignFormImageValidationDto valid() {
		return new CampaignFormImageValidationDto(true, Collections.emptyList(), null);
	}

	public static CampaignFormImageValidationDto invalid(List<String> labels, String message) {
		return new CampaignFormImageValidationDto(false, labels, message);
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public List<String> getProhibitedLabelsDetected() {
		if (prohibitedLabelsDetected == null) {
			prohibitedLabelsDetected = new ArrayList<>();
		}
		return prohibitedLabelsDetected;
	}

	public void setProhibitedLabelsDetected(List<String> prohibitedLabelsDetected) {
		this.prohibitedLabelsDetected = prohibitedLabelsDetected;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
