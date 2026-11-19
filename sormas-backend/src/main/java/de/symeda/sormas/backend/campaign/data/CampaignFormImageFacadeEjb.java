package de.symeda.sormas.backend.campaign.data;

import java.io.IOException;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.lang3.StringUtils;

import de.symeda.sormas.api.campaign.data.CampaignFormImageFacade;
import de.symeda.sormas.api.campaign.data.CampaignFormImageNamingContext;
import de.symeda.sormas.api.campaign.data.CampaignFormImageValidationDto;
import de.symeda.sormas.api.campaign.data.CampaignFormImageValue;
import de.symeda.sormas.api.document.DocumentDto;
import de.symeda.sormas.api.document.DocumentRelatedEntityType;
import de.symeda.sormas.api.utils.ValidationRuntimeException;
import de.symeda.sormas.backend.document.DocumentFacadeEjb;
import de.symeda.sormas.backend.user.UserFacadeEjb;
import de.symeda.sormas.backend.user.UserService;

@Stateless(name = "CampaignFormImageFacade")
public class CampaignFormImageFacadeEjb implements CampaignFormImageFacade {

	private static final String NA = "NA";
	private static final String DEFAULT_EXTENSION = "jpg";

	@EJB
	private DocumentFacadeEjb.DocumentFacadeEjbLocal documentFacade;

	@EJB
	private UserService userService;

	@EJB
	private YoloImageValidationService yoloValidationService;

	@Override
	public String generateImageFileName(CampaignFormImageNamingContext namingContext) {
		if (namingContext == null) {
			return String.join("_", NA, NA, NA, NA, NA);
		}

		return String.join("_", sanitizeSegment(namingContext.getRegion()), sanitizeSegment(namingContext.getProvince()),
				sanitizeSegment(namingContext.getDistrict()), sanitizeSegment(namingContext.getClusterNumber()),
				sanitizeSegment(namingContext.getClusterName()));
	}

	@Override
	public CampaignFormImageValue normalizeImageValue(CampaignFormImageValue imageValue,
			CampaignFormImageNamingContext namingContext) {
		if (imageValue == null) {
			return null;
		}

		if (!imageValue.hasIdentifier()) {
			throw new ValidationRuntimeException(
					"Image metadata must provide either imageId (uploaded) or localId (pending upload).");
		}

		if (StringUtils.isBlank(imageValue.getGeneratedFileName())) {
			String baseName = generateImageFileName(namingContext);
			String extension = detectExtension(imageValue);
			imageValue.setGeneratedFileName(baseName + "." + extension);
		}

		if (imageValue.getCapturedAt() == null) {
			imageValue.setCapturedAt(System.currentTimeMillis());
		}

		return imageValue;
	}

	@Override
	public List<CampaignFormImageValue> normalizeImageValues(List<CampaignFormImageValue> imageValues,
			CampaignFormImageNamingContext namingContext, Integer maxCount) {
		if (imageValues == null) {
			return null;
		}

		if (maxCount != null && maxCount > 0 && imageValues.size() > maxCount) {
			throw new ValidationRuntimeException("Image count exceeds configured maximum of " + maxCount + ".");
		}

		List<CampaignFormImageValue> normalized = new ArrayList<>(imageValues.size());
		for (CampaignFormImageValue imageValue : imageValues) {
			normalized.add(normalizeImageValue(imageValue, namingContext));
		}

		return normalized;
	}

	@Override
	public CampaignFormImageValue uploadImage(String campaignFormDataUuid, CampaignFormImageValue imageValue,
			byte[] imageContent) throws IOException {
		if (StringUtils.isBlank(campaignFormDataUuid)) {
			throw new ValidationRuntimeException("Campaign form data UUID must be set for image upload.");
		}
		if (imageContent == null || imageContent.length == 0) {
			throw new ValidationRuntimeException("Image content must not be empty.");
		}

		CampaignFormImageValidationDto validation = validateImageContent(imageContent);
		if (!validation.isValid()) {
			throw new ValidationRuntimeException(validation.getMessage());
		}

		CampaignFormImageValue metadata = imageValue == null ? new CampaignFormImageValue() : imageValue;
		if (StringUtils.isBlank(metadata.getLocalId())) {
			metadata.setLocalId("upload-" + System.currentTimeMillis());
		}
		metadata = normalizeImageValue(metadata, null);

		DocumentDto documentDto = DocumentDto.build();
		documentDto.setUploadingUser(UserFacadeEjb.toReferenceDto(userService.getCurrentUser()));
		documentDto.setRelatedEntityType(DocumentRelatedEntityType.CAMPAIGN_FORM_DATA);
		documentDto.setRelatedEntityUuid(campaignFormDataUuid);
		documentDto.setName(resolveDocumentName(metadata));
		documentDto.setMimeType(resolveMimeType(metadata));
		documentDto.setSize(imageContent.length);

		DocumentDto storedDocument = documentFacade.saveDocument(documentDto, imageContent);
		metadata.setImageId(storedDocument.getUuid());
		metadata.setMimeType(storedDocument.getMimeType());
		metadata.setCompressedSizeBytes(storedDocument.getSize());
		if (StringUtils.isBlank(metadata.getOriginalFileName())) {
			metadata.setOriginalFileName(storedDocument.getName());
		}
		if (StringUtils.isBlank(metadata.getGeneratedFileName())) {
			metadata.setGeneratedFileName(storedDocument.getName());
		}

		return metadata;
	}

	@Override
	public byte[] readImage(String imageId) throws IOException {
		if (StringUtils.isBlank(imageId)) {
			throw new ValidationRuntimeException("Image ID must not be empty.");
		}

		return documentFacade.read(imageId);
	}

	@Override
	public void deleteImage(String imageId) {
		if (StringUtils.isBlank(imageId)) {
			throw new ValidationRuntimeException("Image ID must not be empty.");
		}

		documentFacade.deleteDocument(imageId);
	}

	@Override
	public List<CampaignFormImageValue> getImages(String campaignFormDataUuid) {
		if (StringUtils.isBlank(campaignFormDataUuid)) {
			throw new ValidationRuntimeException("Campaign form data UUID must not be empty.");
		}

		List<DocumentDto> documents = documentFacade
				.getDocumentsRelatedToEntity(DocumentRelatedEntityType.CAMPAIGN_FORM_DATA, campaignFormDataUuid);
		List<CampaignFormImageValue> imageValues = new ArrayList<>(documents.size());
		for (DocumentDto document : documents) {
			CampaignFormImageValue imageValue = new CampaignFormImageValue();
			imageValue.setImageId(document.getUuid());
			imageValue.setOriginalFileName(document.getName());
			imageValue.setGeneratedFileName(document.getName());
			imageValue.setMimeType(document.getMimeType());
			imageValue.setCompressedSizeBytes(document.getSize());
			imageValues.add(imageValue);
		}

		return imageValues;
	}

	@Override
	public String resolvePreviewUrl(String imageId) {
		if (StringUtils.isBlank(imageId)) {
			return null;
		}

//		try {
//			byte[] imageBytes = readImage(imageId);
//			if (imageBytes == null || imageBytes.length == 0) {
//				return null;
//			}
//
//			DocumentDto documentDto = documentFacade.getDocumentByUuid(imageId);
//			String mimeType = documentDto != null && StringUtils.isNotBlank(documentDto.getMimeType())
//					? documentDto.getMimeType()
//					: "image/jpeg";
//
//			return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
//		} 
		  try {
		        byte[] imageBytes = readImage(imageId);
		        if (imageBytes == null || imageBytes.length == 0) {
		        	System.out.println("Image {} has zero bytes." +  imageId);
//		            System.out.print("Image {} has zero bytes.", imageId);
		            return null;
		        }
		        DocumentDto doc = documentFacade.getDocumentByUuid(imageId);
		        String mime = doc != null ? doc.getMimeType() : "image/jpeg";
		        // Optionally detect MIME from bytes as fallback
		        return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
		    }catch (IOException e) {
			throw new ValidationRuntimeException("Unable to resolve preview URL for image " + imageId + ".", e);
		}
	}

	private String sanitizeSegment(String input) {
		if (StringUtils.isBlank(input)) {
			return NA;
		}

		String normalized = input.trim();
		normalized = normalized.replaceAll("\\s+", "-");
		normalized = normalized.replaceAll("[^A-Za-z0-9\\-]", "");
		normalized = normalized.replaceAll("-+", "-");
		normalized = StringUtils.strip(normalized, "-");
		return StringUtils.isBlank(normalized) ? NA : normalized;
	}

	private String detectExtension(CampaignFormImageValue imageValue) {
		String fromOriginal = extensionFromFilename(imageValue.getOriginalFileName());
		if (fromOriginal != null) {
			return fromOriginal;
		}

		if (StringUtils.isNotBlank(imageValue.getMimeType())) {
			String mimeType = imageValue.getMimeType().trim().toLowerCase(Locale.ROOT);
			if (mimeType.startsWith("image/")) {
				String extension = mimeType.substring("image/".length());
				if ("jpeg".equals(extension)) {
					return "jpg";
				}
				if (StringUtils.isNotBlank(extension)) {
					return extension;
				}
			}
		}

		return DEFAULT_EXTENSION;
	}

	private String extensionFromFilename(String fileName) {
		if (StringUtils.isBlank(fileName)) {
			return null;
		}

		String trimmed = fileName.trim();
		int dotIndex = trimmed.lastIndexOf('.');
		if (dotIndex < 0 || dotIndex == trimmed.length() - 1) {
			return null;
		}

		String extension = trimmed.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
		if ("jpeg".equals(extension)) {
			return "jpg";
		}
		return extension.replaceAll("[^a-z0-9]", "");
	}

	private String resolveMimeType(CampaignFormImageValue imageValue) {
		if (StringUtils.isNotBlank(imageValue.getMimeType())) {
			return imageValue.getMimeType();
		}

		String extension = detectExtension(imageValue);
		if ("png".equals(extension)) {
			return "image/png";
		}
		if ("webp".equals(extension)) {
			return "image/webp";
		}
		return "image/jpeg";
	}

	private String resolveDocumentName(CampaignFormImageValue imageValue) {
		if (StringUtils.isNotBlank(imageValue.getGeneratedFileName())) {
			return imageValue.getGeneratedFileName();
		}
		if (StringUtils.isNotBlank(imageValue.getOriginalFileName())) {
			return imageValue.getOriginalFileName();
		}

		return generateImageFileName(null) + "." + detectExtension(imageValue);
	}

	@Override
	public CampaignFormImageValidationDto validateImageContent(byte[] imageContent) {
		if (yoloValidationService != null) {
			return yoloValidationService.validate(imageContent);
		}
		return CampaignFormImageValidationDto.valid();
	}
}
