package de.symeda.sormas.api.campaign.data;

import java.io.IOException;
import java.util.List;

import javax.ejb.Remote;

@Remote
public interface CampaignFormImageFacade {

	String generateImageFileName(CampaignFormImageNamingContext namingContext);

	CampaignFormImageValue normalizeImageValue(CampaignFormImageValue imageValue,
			CampaignFormImageNamingContext namingContext);

	List<CampaignFormImageValue> normalizeImageValues(List<CampaignFormImageValue> imageValues,
			CampaignFormImageNamingContext namingContext, Integer maxCount);

	CampaignFormImageValue uploadImage(String campaignFormDataUuid, CampaignFormImageValue imageValue, byte[] imageContent)
			throws IOException;

	byte[] readImage(String imageId) throws IOException;

	void deleteImage(String imageId);

	List<CampaignFormImageValue> getImages(String campaignFormDataUuid);
	
	String resolvePreviewUrl(String imageId);

	CampaignFormImageValidationDto validateImageContent(byte[] imageContent);

}
