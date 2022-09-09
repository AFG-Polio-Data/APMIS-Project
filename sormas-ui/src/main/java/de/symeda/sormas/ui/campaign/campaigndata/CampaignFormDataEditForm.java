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

import static de.symeda.sormas.ui.utils.LayoutUtil.fluidRowLocs;
import static de.symeda.sormas.ui.utils.LayoutUtil.loc;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.persistence.NoResultException;

import com.vaadin.server.Page;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.v7.data.Validator;
import com.vaadin.v7.data.util.converter.Converter;
import com.vaadin.v7.ui.ComboBox;
import com.vaadin.v7.ui.DateField;
import com.vaadin.v7.ui.TextField;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.campaign.CampaignDto;
import de.symeda.sormas.api.campaign.data.CampaignFormDataCriteria;
import de.symeda.sormas.api.campaign.data.CampaignFormDataDto;
import de.symeda.sormas.api.campaign.data.CampaignFormDataReferenceDto;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaDto;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.infrastructure.area.AreaReferenceDto;
import de.symeda.sormas.api.infrastructure.community.CommunityDto;
import de.symeda.sormas.api.infrastructure.community.CommunityReferenceDto;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UserProvider;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.campaign.expressions.ExpressionProcessor;
import de.symeda.sormas.ui.utils.AbstractEditForm;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.FieldHelper;

public class CampaignFormDataEditForm extends AbstractEditForm<CampaignFormDataDto> {

	public static final String CAMPAIGN_FORM_LOC = "campaignFormLoc";
	private AreaReferenceDto area;
	public static final String AREA = "area";

	private DateField dates;

	public AreaReferenceDto getArea() {
		return area;
	}

	public void setArea(AreaReferenceDto area) {
		this.area = area;
	}

	private static final String HTML_LAYOUT = fluidRowLocs(CampaignFormDataDto.FORM_DATE// ,
																						// CampaignFormDataDto.LATITUDE,
	// CampaignFormDataDto.LONGITUDE
	) + fluidRowLocs(CampaignFormDataEditForm.AREA, CampaignFormDataDto.REGION)// ,
																				// CampaignFormDataDto.FORM_TYPE)
			+ fluidRowLocs(CampaignFormDataDto.DISTRICT, CampaignFormDataDto.COMMUNITY)// ,
																						// CampaignFormDataDto.FORM_TYPE)
			+ loc(CAMPAIGN_FORM_LOC);

	private static final long serialVersionUID = -8974009722689546941L;

	private CampaignFormBuilder campaignFormBuilder;

	public CampaignFormDataEditForm(boolean create) {
		super(CampaignFormDataDto.class, CampaignFormDataDto.I18N_PREFIX);

		if (create) {
			hideValidationUntilNextCommit();
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void addFields() {
		ComboBox cbCampaign = addField(CampaignFormDataDto.CAMPAIGN, ComboBox.class);
		cbCampaign.addItems(FacadeProvider.getCampaignFacade().getAllActiveCampaignsAsReference());

		// addField(CampaignFormDataDto.LONGITUDE, TextField.class);
		// addField(CampaignFormDataDto.LATITUDE, TextField.class);
		ComboBox cbRegion = addInfrastructureField(CampaignFormDataDto.REGION);
		ComboBox cbArea = addInfrastructureField(CampaignFormDataDto.AREA);
		ComboBox cbDistrict = addInfrastructureField(CampaignFormDataDto.DISTRICT);
		ComboBox cbCommunity = addInfrastructureField(CampaignFormDataDto.COMMUNITY);

		
		
		// addField(CampaignFormDataDto.FORM_TYPE);

		/*
		 * ComboBox clusterfieldx = addField(CampaignFormDataDto.FORM_TYPE,
		 * ComboBox.class); clusterfieldx.addItem("Pre-Campaign");
		 * clusterfieldx.addItem("Intra-Campign");
		 * clusterfieldx.addItem("Post-Campaign");
		 */

		dates = addField(CampaignFormDataDto.FORM_DATE, DateField.class);

		// addField(CampaignFormDataDto.FORM_DATE, DateField.class);

		setRequired(true, CampaignFormDataDto.CAMPAIGN, CampaignFormDataDto.FORM_DATE, CampaignFormDataDto.REGION,
				CampaignFormDataDto.AREA, CampaignFormDataDto.DISTRICT, CampaignFormDataDto.COMMUNITY);
		// CampaignFormDataDto.FORM_TYPE);

		addInfrastructureListenerx(cbArea, cbRegion, cbDistrict, cbCommunity);
		cbArea.addItems(FacadeProvider.getAreaFacade().getAllActiveAsReference());

		final UserDto currentUser = UserProvider.getCurrent().getUser();
		final AreaReferenceDto currentUserArea = currentUser.getArea();
		// final RegionReferenceDto currentUserRegion = currentUser.getRegion();

		/*
		 * if
		 * (FacadeProvider.getFeatureConfigurationFacade().isFeatureEnabled(FeatureType.
		 * INFRASTRUCTURE_TYPE_AREA)) { cbArea =
		 * addCustomField(CampaignFormDataEditForm.AREA, AreaReferenceDto.class,
		 * ComboBox.class);
		 * cbArea.setCaption(I18nProperties.getCaption(Captions.CampaignFormData_area));
		 * 
		 * setRequired(true, CampaignFormDataEditForm.AREA);
		 * cbArea.addItems(FacadeProvider.getAreaFacade().getAllActiveAsReference());
		 * cbArea.addValueChangeListener(e -> { AreaReferenceDto area =
		 * (AreaReferenceDto) e.getProperty().getValue();
		 * 
		 * if (area == null) { cbRegion.setValue(null); }
		 * 
		 * FieldHelper.updateItems( cbRegion, area != null ?
		 * FacadeProvider.getRegionFacade().getAllActiveByArea(area.getUuid()) :
		 * FacadeProvider.getRegionFacade().getAllActiveByServerCountry()); });
		 * cbRegion.addValueChangeListener(e -> { RegionReferenceDto region =
		 * (RegionReferenceDto) e.getProperty().getValue(); if (Objects.nonNull(region))
		 * {
		 * cbArea.setValue(FacadeProvider.getRegionFacade().getByUuid(region.getUuid()).
		 * getArea()); } }); if (currentUserRegion != null) { final AreaReferenceDto
		 * area =
		 * FacadeProvider.getRegionFacade().getByUuid(currentUserRegion.getUuid()).
		 * getArea(); cbArea.setValue(area); if (currentUserRegion != null) {
		 * cbArea.setEnabled(false); } } }
		 */

		if (currentUserArea != null) {
			cbArea.setValue(currentUserArea);
			cbArea.setEnabled(false);
		}
		if (currentUser.getRegion() != null) {
			cbRegion.setValue(currentUser.getRegion());
			cbRegion.setEnabled(false);
		}
		if (currentUser.getDistrict() != null) {
			cbDistrict.setValue(currentUser.getDistrict());
			cbDistrict.setEnabled(false);
		}
		if (currentUser.getCommunity() != null) {
			//cbCommunity.setValue(currentUser.getCommunity());
			//cbCommunity.setEnabled(false);
		}
	}

	@SuppressWarnings("deprecation")
	private void addInfrastructureListeners(ComboBox cbRegion, ComboBox cbDistrict, ComboBox cbCommunity) {
		
		
		cbRegion.addValueChangeListener(e -> {
			RegionReferenceDto region = (RegionReferenceDto) e.getProperty().getValue();
			FieldHelper.updateItems(cbDistrict,
					region != null ? FacadeProvider.getDistrictFacade().getAllActiveByRegion(region.getUuid()) : null);
		});

		cbDistrict.addValueChangeListener(e -> {
			DistrictReferenceDto district = (DistrictReferenceDto) e.getProperty().getValue();
			
			
			FieldHelper.updateItems(cbCommunity,
					district != null ? FacadeProvider.getCommunityFacade().getAllActiveByDistrict(district.getUuid())
							: null);
			
		});
	}

	@SuppressWarnings("deprecation")
	private void addInfrastructureListenerx(ComboBox cbArea, ComboBox cbRegion, ComboBox cbDistrict,
			ComboBox cbCommunity) {
		cbArea.addValueChangeListener(e -> {
			AreaReferenceDto area = (AreaReferenceDto) e.getProperty().getValue();
			FieldHelper.updateItems(cbRegion,
					area != null ? FacadeProvider.getRegionFacade().getAllActiveByArea(area.getUuid()) : null);
			cbCommunity.clear();
		});

		cbRegion.addValueChangeListener(e -> {
			RegionReferenceDto region = (RegionReferenceDto) e.getProperty().getValue();
			FieldHelper.updateItems(cbDistrict,
					region != null ? FacadeProvider.getDistrictFacade().getAllActiveByRegion(region.getUuid()) : null);
			cbCommunity.clear();
		});

		cbDistrict.addValueChangeListener(e -> {
			DistrictReferenceDto district = (DistrictReferenceDto) e.getProperty().getValue();

			if (district != null) {
				List<CommunityReferenceDto> items = FacadeProvider.getCommunityFacade()
						.getAllActiveByDistrict(district.getUuid());
				for (CommunityReferenceDto item : items) {
					item.setCaption(item.getNumber() != null ? item.getNumber().toString() : item.getCaption());
				}
				FieldHelper.updateItems(cbCommunity, district != null ? items : null);

			}

			
			final UserDto currentUserx = UserProvider.getCurrent().getUser();
			if(currentUserx.getCommunity().size() > 0) {
				cbCommunity.clear();
				FieldHelper.updateItems(cbCommunity,
						  currentUserx.getCommunity().stream().collect(Collectors.toList())
							);
			}
			
//			FieldHelper.updateItems(cbCommunity, district != null ? items.stream().filter(ce -> ce.getCaption() != null).collect(Collectors.toList()) : null);
	

		});
		System.out.println(Page.getCurrent().getLocation());
		URI location = Page.getCurrent().getLocation();
		String uri = location.toString();
		if (uri.contains(",")) {

			// }

			cbCommunity.addValueChangeListener(e -> {

				if (cbCommunity.getValue() != null && cbDistrict.getValue() != null) {

					System.out.println(dates.getValue() + "||||||||||||||||||||||||||||"
							+ super.getValue().getCampaignFormMeta().getUuid() + "||||||||||||"
							+ super.getValue().getCampaign().getUuid());
					CampaignFormMetaDto campaignForm = FacadeProvider.getCampaignFormMetaFacade()
							.getCampaignFormMetaByUuid(super.getValue().getCampaignFormMeta().getUuid());

					CampaignDto campaign = FacadeProvider.getCampaignFacade()
							.getByUuid(super.getValue().getCampaign().getUuid());

					CommunityReferenceDto community = (CommunityReferenceDto) cbCommunity.getValue();

					System.out.println(community.getCaption() + "??????????????????>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> "
							+ campaignForm.getFormName() + campaign.getName());
					String formuuid = FacadeProvider.getCampaignFormDataFacade().getByClusterDropDown(community,
							campaignForm, campaign);

					if (!formuuid.equals("nul")) {
						// Page.getCurrent().get
						ControllerProvider.getCampaignController().navigateToFormDataView(formuuid);
					} else {
						System.out.println(
								">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
						Page.getCurrent().getJavaScript().execute("$(document).ready(function() {"
								// + "alert();"
								// + "document.querySelector(\".v-slot.v-align-right.v-align-bottom\").show();"
								// +
								// "$('.v-slot.v-align-right.v-align-bottom').toggleClass('v-align-center').addClass('v-align-right');"
								+ "$('.v-verticallayout.v-layout.v-vertical.v-widget.v-has-width.v-has-height.v-margin-top.v-margin-right.v-margin-bottom.v-margin-left').show();"
								+ "$('.v-verticallayout.v-layout.v-vertical.v-widget.v-has-width.v-has-height.v-margin-top.v-margin-right.v-margin-bottom.v-margin-left').show();"
						// +"$('#formidx').find('td:contains('Void')').parent('tr').hide();"
								+ "});");
					}
				}
			});

		}

		;

	}

	@Override
	public CampaignFormDataDto getValue() {
		CampaignFormDataDto value = super.getValue();

		if (campaignFormBuilder == null) {
			throw new RuntimeException("Campaign form builder has not been initialized");
		}

		value.setFormValues(campaignFormBuilder.getFormValues());

		return value;
	}

	@Override
	public void setValue(CampaignFormDataDto newFieldValue) throws ReadOnlyException, Converter.ConversionException {
		super.setValue(newFieldValue);

		buildCampaignForm(newFieldValue);
	}

	@Override
	public void validate() throws Validator.InvalidValueException {
		super.validate();

		if (campaignFormBuilder == null) {
			throw new RuntimeException("Campaign form builder has not been initialized");
		}

		campaignFormBuilder.validateFields();
	}

	public void resetFormValues() {
		campaignFormBuilder.resetFormValues();
	}

	private void buildCampaignForm(CampaignFormDataDto campaignFormData) {
		VerticalLayout campaignFormLayout = new VerticalLayout();
		campaignFormLayout.setWidthFull();
		campaignFormLayout.setHeightFull();
		// campaignFormLayout.setWidth(100f, Unit.PERCENTAGE);
		// CssStyles.style(campaignFormLayout, CssStyles.VSPACE_3);

		CampaignFormMetaDto campaignForm = FacadeProvider.getCampaignFormMetaFacade()
				.getCampaignFormMetaByUuid(campaignFormData.getCampaignFormMeta().getUuid());
		campaignFormBuilder = new CampaignFormBuilder(campaignForm.getCampaignFormElements(),
				campaignFormData.getFormValues(), campaignFormLayout, campaignForm.getCampaignFormTranslations());

		campaignFormBuilder.buildForm();

		final ExpressionProcessor expressionProcessor = new ExpressionProcessor(campaignFormBuilder);
		expressionProcessor.disableExpressionFieldsForEditing();
		expressionProcessor.configureExpressionFieldsWithTooltip();
		expressionProcessor.addExpressionListener();

		getContent().addComponent(campaignFormLayout, CAMPAIGN_FORM_LOC);
	}

	@Override
	protected String createHtmlLayout() {
		return HTML_LAYOUT;
	}
}
