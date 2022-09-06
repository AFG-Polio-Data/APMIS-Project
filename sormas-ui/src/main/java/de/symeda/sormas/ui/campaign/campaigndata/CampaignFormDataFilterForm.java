/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2020 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.symeda.sormas.ui.campaign.campaigndata;

import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.text.WordUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.ui.ComboBox;
import com.vaadin.v7.ui.Field;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.ReferenceDto;
import de.symeda.sormas.api.campaign.CampaignPhase;
import de.symeda.sormas.api.campaign.data.CampaignFormDataCriteria;
import de.symeda.sormas.api.campaign.data.CampaignFormDataDto;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaReferenceDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.infrastructure.area.AreaReferenceDto;
import de.symeda.sormas.api.infrastructure.community.CommunityReferenceDto;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.user.UserType;
import de.symeda.sormas.ui.UserProvider;
import de.symeda.sormas.ui.campaign.components.importancefilterswitcher.CriteriaPhase;
import de.symeda.sormas.ui.utils.AbstractFilterForm;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.FieldConfiguration;
import de.symeda.sormas.ui.utils.FieldHelper;

public class CampaignFormDataFilterForm extends AbstractFilterForm<CampaignFormDataCriteria> {

	private static final long serialVersionUID = 718816470397296272L;

	private Consumer<CampaignFormMetaReferenceDto> formMetaChangedCallback;
	private ComboBox cbCampaignForm;
	private ComboBox areaFilter;
	private ComboBox regionFilter;
	private ComboBox districtFilter;
	private ComboBox communityFilter;

	public String phaseFilterContent;

	protected CampaignFormDataFilterForm() {

		super(CampaignFormDataCriteria.class, CampaignFormDataDto.I18N_PREFIX);
		formActionButtonsComponent.style(CssStyles.FORCE_CAPTION);
		formActionButtonsComponent.setSpacing(false);
		formActionButtonsComponent.setSizeFull();
		formActionButtonsComponent.setMargin(new MarginInfo(false, false, false, true)); ///campaigndata
	}

	public String getPhaseFilterContent() {
		return phaseFilterContent;
	}

	public void setPhaseFilterContent(String phaseFilterContent) {
		this.phaseFilterContent = phaseFilterContent;
	} 
	@Override
	protected String[] getMainFilterLocators() {
		return new String[] {
				// CampaignFormDataCriteria.FORM_PHASE,
				CampaignFormDataCriteria.CAMPAIGN_FORM_META, CampaignFormDataCriteria.AREA,
				CampaignFormDataCriteria.REGION, CampaignFormDataCriteria.DISTRICT,
				CampaignFormDataCriteria.COMMUNITY };
	}

	@Override
	protected void addFields() {
		/*
		 * phaseFilter = addField( FieldConfiguration.withCaptionAndPixelSized(
		 * CampaignFormDataCriteria.FORM_PHASE,
		 * //I18nProperties.getPrefixCaption(CampaignFormDataDto.I18N_PREFIX,
		 * "form phase"), //I18nProperties.getPrefixDescription(CampaignDto.I18N_PREFIX,
		 * "campaignPhase") intra "Form Phase", 200));
		 * phaseFilter.addItems(CampaignPhase.values());
		 * //phaseFilter.addItems(FacadeProvider.getCampaignFormMetaFacade().
		 * getAllCampaignFormMetasAsReferencesByRound("post-campaign"));
		 * CampaignFormMetaReferenceDto
		 * 
		 * // phaseFilter.addItems(FacadeProvider.getCampaignFormMetaFacade().
		 * getAllCampaignFormMetasAsReferencesByRound("post-campaign"));
		 * //CampaignFormMetaDataDto
		 * 
		 * 
		 * phaseFilter.addValueChangeListener(e -> {
		 * 
		 * skipChangeEvents= true; System.out.println(
		 * "___________+++++++++++++++++++++++++++++++++++++++++++++_____________________"
		 * +e.getProperty().getValue()); cbCampaignForm.clear(); //
		 * cbCampaignForm.addItems(FacadeProvider.getCampaignFormMetaFacade().
		 * getAllCampaignFormMetasAsReferencesByRound((String)
		 * e.getProperty().getValue()));
		 * //formMetaChangedCallback.accept((CampaignFormMetaReferenceDto)
		 * e.getProperty().getValue()); });
		 * 
		 * 
		 */

		cbCampaignForm = addField(
				FieldConfiguration.withCaptionAndPixelSized(CampaignFormDataCriteria.CAMPAIGN_FORM_META, I18nProperties
						.getPrefixCaption(CampaignFormDataDto.I18N_PREFIX, CampaignFormDataDto.CAMPAIGN_FORM_META),
						200));
		// add filter based on user type
		if (UserProvider.getCurrent().hasUserType(UserType.WHO_USER)) {
			System.out.println("11111111111111111111111111111111111111");
			cbCampaignForm.addItems(FacadeProvider.getCampaignFormMetaFacade().getAllCampaignFormMetasAsReferences());
		}
		
		
		FieldHelper.addSoftRequiredStyle(cbCampaignForm);

		if (formMetaChangedCallback != null) {
			cbCampaignForm.addValueChangeListener(e -> {
				formMetaChangedCallback.accept((CampaignFormMetaReferenceDto) e.getProperty().getValue());
			});
		}

		areaFilter = addField(FieldConfiguration.withCaptionAndPixelSized(CampaignFormDataCriteria.AREA,
				I18nProperties.getCaption(Captions.Campaign_area), 200));
		areaFilter.setInputPrompt(I18nProperties.getString(Strings.promptAllAreas));
		areaFilter.addItems(FacadeProvider.getAreaFacade().getAllActiveAsReference());

		regionFilter = addField(FieldConfiguration.withCaptionAndPixelSized(CampaignFormDataCriteria.REGION,
				I18nProperties.getCaption(Captions.Campaign_region), 200));
		regionFilter.setInputPrompt(I18nProperties.getString(Strings.promptAllRegions));
		// regionFilter.addItems(FacadeProvider.getRegionFacade().getAllActiveByServerCountry());

		districtFilter = addField(FieldConfiguration.withCaptionAndPixelSized(CampaignFormDataCriteria.DISTRICT,
				I18nProperties.getCaption(Captions.Campaign_district), 200));
		districtFilter.setInputPrompt(I18nProperties.getString(Strings.promptAllDistricts));

		communityFilter = addField(FieldConfiguration.withCaptionAndPixelSized(CampaignFormDataCriteria.COMMUNITY,
				I18nProperties.getCaption(Captions.Campaign_community), 200));
		communityFilter.setInputPrompt(I18nProperties.getString(Strings.promptAllCommunities));

		UserDto user = currentUserDto();
		final AreaReferenceDto userArea = user.getArea();
		final RegionReferenceDto userRegion = user.getRegion();
		final DistrictReferenceDto userDistrict = user.getDistrict();
		final CommunityReferenceDto userCommunity = null; //set to null since users can have more than one community
		// final ReferenceDto formType;

		if (userArea != null) {
			areaFilter.setEnabled(false);
			regionFilter.addItems(FacadeProvider.getRegionFacade().getAllActiveByArea(userArea.getUuid()));
			if (userRegion != null) {
				regionFilter.setEnabled(false);
				districtFilter.addItems(FacadeProvider.getDistrictFacade().getAllActiveByRegion(userRegion.getUuid()));
				if (userDistrict != null) {
					districtFilter.setEnabled(false);
					// can I set the caption of all the items here before adding
					// List<CommunityReferenceDto> items =
					// FacadeProvider.getCommunityFacade().getAllActiveByDistrict(userDistrict.getUuid());
					// for(CommunityReferenceDto item : items) {
					// item.setCaption(item.getNumber().toString());
					// }
					communityFilter.addItems(
							FacadeProvider.getCommunityFacade().getAllActiveByDistrict(userDistrict.getUuid()));

					if (userCommunity != null) {
						communityFilter.setEnabled(false);
					}
				}
			}
		}
	}

	@Override
	protected void applyFieldConfiguration(FieldConfiguration configuration, Field field) {
		super.applyFieldConfiguration(configuration, field);
		if (configuration.getCaption() != null) {
			field.setCaption(configuration.getCaption());
		}
	}

	@Override
	protected <T1 extends Field> void formatField(T1 field, String propertyId) {
		super.formatField(field, propertyId);
		field.addStyleName(CssStyles.CAPTION_ON_TOP);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void applyDependenciesOnFieldChange(String propertyId, Property.ValueChangeEvent event) {
		super.applyDependenciesOnFieldChange(propertyId, event);

		switch (propertyId) {
		case CampaignFormDataDto.AREA:
			System.out.println("saaaaaaaaaaaaaaaassssssssssssssssssssssssssssssss");
			AreaReferenceDto area = (AreaReferenceDto) event.getProperty().getValue();
			if (area != null) {
				regionFilter.removeAllItems();
				regionFilter.addItems(FacadeProvider.getRegionFacade().getAllActiveByArea(area.getUuid()));
			} else {
				regionFilter.removeAllItems();
				regionFilter.clear();
			}
			break;
		case CampaignFormDataDto.REGION:
			System.out.println("ssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss");
			RegionReferenceDto region = (RegionReferenceDto) event.getProperty().getValue();
			if (region != null) {
				districtFilter.removeAllItems();
				districtFilter.addItems(FacadeProvider.getDistrictFacade().getAllActiveByRegion(region.getUuid()));
			} else {
				districtFilter.removeAllItems();
				districtFilter.clear();
			}
			break;
		case CampaignFormDataDto.DISTRICT:
			DistrictReferenceDto district = (DistrictReferenceDto) event.getProperty().getValue();

			if (district != null) {
				communityFilter.removeAllItems();
				communityFilter
						.addItems(FacadeProvider.getCommunityFacade().getAllActiveByDistrict(district.getUuid()));
				System.out.println(">>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< + " + communityFilter);
			} else {
				communityFilter.removeAllItems();
				communityFilter.clear();
			}
			break;
		}
	}

	@Override
	protected void applyDependenciesOnNewValue(CampaignFormDataCriteria criteria) {
		cbCampaignForm.removeAllItems();

		if (criteria.getCampaign() != null && getPhaseFilterContent() == null) { // set the value of the campaign form
																					// dropdown
			if (UserProvider.getCurrent().hasUserType(UserType.EOC_USER)) {
				cbCampaignForm.addItems(FacadeProvider.getCampaignFormMetaFacade()
						.getCampaignFormMetaAsReferencesByCampaignIntraCamapaign(criteria.getCampaign().getUuid()));
			} else {
				cbCampaignForm.addItems(FacadeProvider.getCampaignFormMetaFacade()
						.getCampaignFormMetasAsReferencesByCampaign(criteria.getCampaign().getUuid()));
			}
		} else if (criteria.getCampaign() != null && getPhaseFilterContent() != null
				&& !"ALL PHASES".equals(getPhaseFilterContent())) {
			if (UserProvider.getCurrent().hasUserType(UserType.EOC_USER)) {

				cbCampaignForm.addItems(FacadeProvider.getCampaignFormMetaFacade()
						.getAllCampaignFormMetasAsReferencesByRoundandCampaign(getPhaseFilterContent().toLowerCase(),
								criteria.getCampaign().getUuid()));
			}
		} else if (getPhaseFilterContent() != null && criteria.getCampaign() == null) {
			if (UserProvider.getCurrent().hasUserType(UserType.EOC_USER)) {

				cbCampaignForm.addItems(FacadeProvider.getCampaignFormMetaFacade()
						.getAllCampaignFormMetasAsReferencesByRound(getPhaseFilterContent().toLowerCase()));
			}
		} else {
			if (UserProvider.getCurrent().hasUserType(UserType.EOC_USER)) {
				cbCampaignForm
						.addItems(FacadeProvider.getCampaignFormMetaFacade().getAllCampaignFormMetasAsReferences());
			}
		}
	}

	public void setFormMetaChangedCallback(Consumer<CampaignFormMetaReferenceDto> formMetaChangedCallback) {
		this.formMetaChangedCallback = formMetaChangedCallback;

		if (cbCampaignForm != null) {
			cbCampaignForm.addValueChangeListener(
					e -> formMetaChangedCallback.accept((CampaignFormMetaReferenceDto) e.getProperty().getValue()));
		}
	}

}
