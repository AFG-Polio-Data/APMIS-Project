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

import static de.symeda.sormas.ui.UiUtil.permitted;
import static de.symeda.sormas.ui.utils.FilteredGrid.EDIT_BTN_ID;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.vaadin.hene.popupbutton.PopupButton;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.server.StreamResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.components.grid.HeaderCell;
import com.vaadin.ui.components.grid.HeaderRow;
import com.vaadin.v7.ui.Grid;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.Language;
import de.symeda.sormas.api.campaign.CampaignReferenceDto;
import de.symeda.sormas.api.campaign.data.CampaignFormDataCriteria;
import de.symeda.sormas.api.campaign.data.CampaignFormDataIndexDto;
import de.symeda.sormas.api.campaign.data.translation.TranslationElement;
import de.symeda.sormas.api.campaign.form.CampaignFormElement;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaDto;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaReferenceDto;
import de.symeda.sormas.api.campaign.form.CampaignFormTranslations;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.infrastructure.area.AreaDto;
import de.symeda.sormas.api.infrastructure.area.AreaReferenceDto;
import de.symeda.sormas.api.infrastructure.community.CommunityReferenceDto;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.user.FormAccess;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.user.UserType;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UserProvider;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.campaign.AbstractCampaignView;
import de.symeda.sormas.ui.campaign.components.CampaignFormPhaseSelector;
import de.symeda.sormas.ui.campaign.components.CampaignSelector;
import de.symeda.sormas.ui.campaign.components.importancefilterswitcher.ImportanceFilterSwitcher;
import de.symeda.sormas.ui.campaign.importer.CampaignFormDataImportLayout;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.ExportEntityName;
import de.symeda.sormas.ui.utils.GridExportStreamResource;
import de.symeda.sormas.ui.utils.RowCount;
import de.symeda.sormas.ui.utils.ShowDetailsListener;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

@SuppressWarnings("serial")
@com.vaadin.annotations.JavaScript("jquerymini.js")
@CssImport("w3c.css")
public class CampaignDataView extends AbstractCampaignView {

	public static final String VIEW_NAME = ROOT_VIEW_NAME + "/campaigndata";

	private final CampaignSelector campaignSelector;
	private final CampaignFormDataCriteria criteria;
	private final CampaignDataGrid grid;
	private CampaignFormDataFilterForm filterForm;
	private ImportanceFilterSwitcher importanceFilterSwitcher;
	private CampaignFormPhaseSelector campaignFormPhaseSelector;
	private PopupButton newFormButton;
	private PopupButton importCampaignButton;
	private RowCount rowsCount;

	@SuppressWarnings("deprecation")
	public CampaignDataView() {
		super(VIEW_NAME);

		criteria = ViewModelProviders.of(getClass()).get(CampaignFormDataCriteria.class);

		campaignSelector = new CampaignSelector();
		criteria.setCampaign(campaignSelector.getValue());
		addHeaderComponent(campaignSelector);

		campaignFormPhaseSelector = new CampaignFormPhaseSelector();
		criteria.setFormType(campaignFormPhaseSelector.getValue().toString());
		addHeaderComponent(campaignFormPhaseSelector);

		grid = new CampaignDataGrid(criteria);
		rowsCount = new RowCount(Strings.labelNumberDataRows, grid.getItemCount());

		VerticalLayout mainLayout = new VerticalLayout();
		HorizontalLayout filtersLayout = new HorizontalLayout();

		filtersLayout.setWidthFull();
		filtersLayout.setMargin(false);
		filtersLayout.setSpacing(true);

		CampaignFormDataFilterForm filterBar = createFilterBar();
		filtersLayout.addComponent(filterBar);
		filtersLayout.setComponentAlignment(filterBar, Alignment.TOP_LEFT);
		filtersLayout.setExpandRatio(filterBar, 0.8f);

		importanceFilterSwitcher = new ImportanceFilterSwitcher();
		importanceFilterSwitcher.setVisible(false);
		filtersLayout.addComponent(importanceFilterSwitcher);
		filtersLayout.setComponentAlignment(importanceFilterSwitcher, Alignment.TOP_RIGHT);
		filtersLayout.setExpandRatio(importanceFilterSwitcher, 0.2f);

		mainLayout.addComponent(filtersLayout);

		filterForm.getField(CampaignFormDataCriteria.CAMPAIGN_FORM_META).addValueChangeListener(e -> {
			Object value = e.getProperty().getValue();
			importanceFilterSwitcher.setVisible(value != null);
			UI.getCurrent().getSession().getCurrent().setAttribute("lastcriteria", criteria.toUrlParams().toString());
			grid.reload();

			executeJavaScript();
		});

		importanceFilterSwitcher.addValueChangeListener(e -> {
			grid.reload();
			executeJavaScript();
			// navigateTo(criteria);
			createFormMetaChangedCallback().accept((CampaignFormMetaReferenceDto) filterForm
					.getField(CampaignFormDataCriteria.CAMPAIGN_FORM_META).getValue());
			rowsCount.update(grid.getItemCount());
		});

		mainLayout.addComponent(rowsCount);
		mainLayout.addComponent(grid);
		mainLayout.setMargin(true);
		mainLayout.setSpacing(false);
		mainLayout.setSizeFull();
		mainLayout.setExpandRatio(grid, 1);
		mainLayout.setStyleName("crud-main-layout");

		Panel newFormPanel = new Panel();
		{
			VerticalLayout newFormLayout = new VerticalLayout();
			newFormLayout.setSpacing(true);
			newFormLayout.setMargin(true);
			newFormLayout.addStyleName(CssStyles.LAYOUT_MINIMAL);
			newFormLayout.setWidth(350, Unit.PIXELS);

			newFormPanel.setContent(newFormLayout);
			fillNewFormDropdown(newFormPanel);

			newFormButton = ButtonHelper.createIconPopupButton(Captions.actionNewForm, VaadinIcons.PLUS_CIRCLE,
					newFormPanel);
			newFormButton.setId("new-form");
			if (permitted(FeatureType.CAMPAIGNS, UserRight.CAMPAIGN_FORM_DATA_EDIT)) {
				addHeaderComponent(newFormButton);
			}
		}

		Panel importFormPanel = new Panel();
		{
			VerticalLayout importFormLayout = new VerticalLayout();
			importFormLayout.setSpacing(true);
			importFormLayout.setMargin(true);
			importFormLayout.addStyleName(CssStyles.LAYOUT_MINIMAL);
			importFormLayout.setWidth(350, Unit.PIXELS);

			importFormPanel.setContent(importFormLayout);
			fillImportDropdown(importFormPanel);

			importCampaignButton = ButtonHelper.createIconPopupButton(Captions.actionImport, VaadinIcons.PLUS_CIRCLE,
					importFormPanel);
			importCampaignButton.setId("campaign-form-import");
			if (permitted(FeatureType.CAMPAIGNS, UserRight.CAMPAIGN_FORM_DATA_EDIT)) {
				addHeaderComponent(importCampaignButton);
			}
		}

		if (UserProvider.getCurrent().hasUserRight(UserRight.CAMPAIGN_FORM_DATA_EXPORT)) {
			VerticalLayout exportLayout = new VerticalLayout();
			{
				exportLayout.setSpacing(true);
				exportLayout.setMargin(true);
				exportLayout.addStyleName(CssStyles.LAYOUT_MINIMAL);
				exportLayout.setWidth(250, Unit.PIXELS);
			}

			PopupButton exportPopupButton = ButtonHelper.createIconPopupButton(Captions.export, VaadinIcons.DOWNLOAD,
					exportLayout);
			addHeaderComponent(exportPopupButton);

			{
				StreamResource streamResource = GridExportStreamResource.createStreamResource("", "", grid,
						ExportEntityName.CAMPAIGN_DATA, EDIT_BTN_ID);
				addExportButton(streamResource, exportPopupButton, exportLayout, VaadinIcons.TABLE, Captions.export,
						Strings.infoBasicExport);

			}

			exportPopupButton.addClickListener(e -> {
				exportLayout.removeAllComponents();
				String formNamee = criteria.getCampaignFormMeta() == null ? "All_Forms"
						: criteria.getCampaignFormMeta().getCaption();
				String camNamee = campaignSelector.getValue() == null ? "All_Campaigns"
						: campaignSelector.getValue().toString();
				StreamResource streamResourcex = GridExportStreamResource.createStreamResource(
						camNamee + "_" + formNamee, "APMIS", grid, ExportEntityName.CAMPAIGN_DATA, EDIT_BTN_ID);
				addExportButton(streamResourcex, exportPopupButton, exportLayout, VaadinIcons.TABLE, Captions.export,
						Strings.infoBasicExport);
			});

		}

		campaignSelector.addValueChangeListener(e -> {
			campaignFormPhaseSelector.clear();
			((VerticalLayout) importFormPanel.getContent()).removeAllComponents();
			if (!Objects.isNull(campaignSelector.getValue())) {
				fillImportDropdown(importFormPanel);
				fillNewFormDropdown(newFormPanel);
				importCampaignButton.setEnabled(true);
				newFormButton.setEnabled(true);
				UI.getCurrent().getSession().getCurrent().setAttribute("lastcriteria",
						criteria.toUrlParams().toString());
				// System.out.println("333333333333333333333 " +
				// criteria.toUrlParams().toString());
			} else {
				importCampaignButton.setEnabled(false);
				newFormButton.setEnabled(false);
				UI.getCurrent().getSession().getCurrent().setAttribute("lastcriteria",
						criteria.toUrlParams().toString());
				// System.out.println("44444444444444444 " + criteria.toUrlParams().toString());
			}
			criteria.setCampaignFormMeta(null);
			filterForm.setValue(criteria);
			UI.getCurrent().getSession().getCurrent().setAttribute("lastcriteria", criteria.toUrlParams().toString());
			executeJavaScript();

		});

		campaignFormPhaseSelector.addValueChangeListener(e -> {
			((VerticalLayout) newFormPanel.getContent()).removeAllComponents();
			if (!Objects.isNull(campaignSelector.getValue())) {
				fillNewFormDropdown(newFormPanel);
				newFormButton.setEnabled(true);
			} else {
				newFormButton.setEnabled(false);
			}
			criteria.setFormType(e.getValue().toString());
			filterForm.setPhaseFilterContent(e.getValue().toString());
			filterForm.setValue(criteria);
			UI.getCurrent().getSession().getCurrent().setAttribute("lastcriteria", criteria.toUrlParams().toString());
			// System.out.println("777777777777777777777 " +
			// criteria.toUrlParams().toString());

			// grid.reload();
			executeJavaScript();
			rowsCount.update(grid.getItemCount());
		});

		if (campaignSelector.getValue() == null) {
			importCampaignButton.setEnabled(false);
			newFormButton.setEnabled(false);
		}

		addComponent(mainLayout);
		executeJavaScript();
		rowsCount.update(grid.getItemCount());

		JavaScript js = Page.getCurrent().getJavaScript();
		js.execute("$(document).ready(function() {\n" + "	if ($(window).width() <= 825) {\n"
				+ "document.querySelector(\".v-label.v-widget.h1.v-label-h1.vspace-none.v-label-vspace-none.v-label-undef-w\").style.display='none';\n"

				+ "document.querySelector(\".v-horizontallayout-view-headerxxxx :nth-child(12)\").style.display='none';"
				+ "document.querySelector(\".v-horizontallayout-view-headerxxxx :nth-child(13)\").style.display='none';"
				+ "document.querySelector(\".v-horizontallayout-view-headerxxxx :nth-child(10)\").style.display='none';\n"
				+ "document.querySelector(\".v-horizontallayout-view-headerxxxx :nth-child(11)\").style.display='none';\n"
				+ "	}"
//				+ "document.querySelectorAll(\".v-grid-column-header-content\").forEach(function (elem) {\r\n"
//				+ "  if (parseFloat(window.getComputedStyle(elem).width) === parseFloat(window.getComputedStyle(elem.parentElement).width)) {\r\n"
//				+ "    elem.setAttribute(\"title\", elem.textContent);\r\n"
//				+ "  }\r\n"
//				+ "    elem.setAttribute(\"title\", elem.textContent);\r\n"
//
//				+ "});"
				+ "});");

		executeJavaScript();
//		

	}

	private void fillImportDropdown(Panel containerPanel) {

		CampaignReferenceDto campaignReferenceDto = campaignSelector.getValue();

		String phase = campaignFormPhaseSelector.getValue();
		Set<FormAccess> userFormAccess = UserProvider.getCurrent().getFormAccess();
		((VerticalLayout) containerPanel.getContent()).removeAllComponents();

		if (campaignReferenceDto != null) {
			List<CampaignFormMetaReferenceDto> campagaignFormReferences;
			if (UserProvider.getCurrent().getUser().getUsertype().equals(UserType.WHO_USER)) {
				campagaignFormReferences = FacadeProvider.getCampaignFormMetaFacade()
						.getCampaignFormMetasAsReferencesByCampaign(campaignReferenceDto.getUuid());
			} else {
				campagaignFormReferences = FacadeProvider.getCampaignFormMetaFacade()
						.getCampaignFormMetaAsReferencesByCampaignIntraCamapaign(campaignReferenceDto.getUuid());
				;
			}

			Collections.sort(campagaignFormReferences);
			for (CampaignFormMetaReferenceDto campaignForm : campagaignFormReferences) {
				Button campaignFormButton = ButtonHelper.createButton(campaignForm.toString(), e -> {
					importCampaignButton.setPopupVisible(false);
					try {
						Window popupWindow = VaadinUiUtil
								.showPopupWindow(new CampaignFormDataImportLayout(campaignForm, campaignReferenceDto));
						popupWindow.setCaption(I18nProperties.getString(Strings.headingImportCampaign));
						popupWindow.addCloseListener(c -> grid.reload());
					} catch (IOException ioException) {
						ioException.printStackTrace();
					}
				});
				campaignFormButton.setWidth(100, Unit.PERCENTAGE);
				campaignFormButton.setStyleName("nocapitalletter");
				campaignFormButton.removeStyleName("v-button");
				((VerticalLayout) containerPanel.getContent()).addComponent(campaignFormButton);
			}
			if (campagaignFormReferences.size() >= 10) {
				// setting a fixed height will enable a scrollbar. Increase width to accommodate
				// it
				containerPanel.setHeight(400, Unit.PIXELS);
				containerPanel.setWidth(containerPanel.getContent().getWidth() + 20.0f, Unit.PIXELS);
			} else {
				containerPanel.setHeightUndefined();
				containerPanel.setWidth(containerPanel.getContent().getWidth(), Unit.PIXELS);
			}
		}
	}

	private void fillNewFormDropdown(Panel containerPanel) {

		CampaignReferenceDto campaignReferenceDtx = campaignSelector.getValue();
		String phase = campaignFormPhaseSelector.getValue();
		Set<FormAccess> userFormAccess = UserProvider.getCurrent().getFormAccess();

		((VerticalLayout) containerPanel.getContent()).removeAllComponents();

		// System.out.println(phase + " #############################");

		if (phase != null && campaignReferenceDtx != null) {
			List<CampaignFormMetaReferenceDto> campagaignFormReferences = FacadeProvider.getCampaignFormMetaFacade()
					.getAllCampaignFormMetasAsReferencesByRoundandCampaignandForm(phase.toLowerCase(),
							campaignReferenceDtx.getUuid(), userFormAccess);

			Collections.sort(campagaignFormReferences);

			for (CampaignFormMetaReferenceDto campaignForm : campagaignFormReferences) {
				Button campaignFormButton = ButtonHelper.createButton(campaignForm.toString(), el -> {

//					System.out.println(campaignReferenceDtx.getUuid()
//							+ " ####################################################################"
//							+ campaignForm.getUuid());

					ControllerProvider.getCampaignController().navigateToFormDataView(campaignReferenceDtx.getUuid(),
							campaignForm.getUuid());

					newFormButton.setPopupVisible(false);
				});

				campaignFormButton.setWidth(100, Unit.PERCENTAGE);
				// campaignFormButton.removeStyleName(VIEW_NAME);
				campaignFormButton.removeStyleName("v-button");
				campaignFormButton.setStyleName("nocapitalletter");
				((VerticalLayout) containerPanel.getContent()).addComponent(campaignFormButton);
			}
			if (campagaignFormReferences.size() >= 10) {
				// setting a fixed height will enable a scrollbar. Increase width to accommodate
				// it
				containerPanel.setHeight(400, Unit.PIXELS);
				containerPanel.setWidth(containerPanel.getContent().getWidth() + 20.0f, Unit.PIXELS);
			} else {
				containerPanel.setHeightUndefined();
				containerPanel.setWidth(containerPanel.getContent().getWidth(), Unit.PIXELS);
			}
		}
	}

	@SuppressWarnings("deprecation")
	public CampaignFormDataFilterForm createFilterBar() {
		final UserDto user = UserProvider.getCurrent().getUser();
		criteria.setArea(user.getArea());
		criteria.setRegion(user.getRegion());
		criteria.setDistrict(user.getDistrict());
		criteria.setCommunity(null); // set to null for the initial filter bar
		filterForm = new CampaignFormDataFilterForm();

		if (filterForm.hasFilter()) {
			executeJavaScript();
			criteria.setArea(criteria.getArea());
			criteria.setRegion(criteria.getRegion());
			criteria.setDistrict(criteria.getDistrict());
			criteria.setCommunity(criteria.getCommunity());

		}

		filterForm.addResetHandler(e -> {
			ViewModelProviders.of(CampaignDataView.class).remove(CampaignFormDataCriteria.class);
			UI.getCurrent().getSession().getCurrent().setAttribute("lastcriteria", "");
			navigateTo(null, true);
			executeJavaScript();
			rowsCount.update(grid.getItemCount());
		});

		campaignSelector.addValueChangeListener(e -> {
			criteria.setCampaign(campaignSelector.getValue());
			grid.reload();
			UI.getCurrent().getSession().getCurrent().setAttribute("lastcriteria", criteria.toUrlParams().toString());
			executeJavaScript();
			rowsCount.update(grid.getItemCount());
		});

		campaignFormPhaseSelector.addValueChangeListener(e -> {
			criteria.setFormType(e.getValue().toString());
			grid.reload();
			UI.getCurrent().getSession().getCurrent().setAttribute("lastcriteria",
					Page.getCurrent().getLocation().toString());
			executeJavaScript();
			rowsCount.update(grid.getItemCount());
		});

		// apply button action
		filterForm.addApplyHandler(e -> {
			criteria.setCampaign(campaignSelector.getValue());
			criteria.setFormType(campaignFormPhaseSelector.getValue().toString());
			UI.getCurrent().getSession().getCurrent().setAttribute("lastcriteria", criteria.toUrlParams().toString());
			System.out.println(UI.getCurrent().getSession().getCurrent().getAttribute("lastcriteria"));
			grid.reload();
			executeJavaScript();
			rowsCount.update(grid.getItemCount());

		});
		filterForm.addValueChangeListener(e -> {
			if (!filterForm.hasFilter() && campaignSelector == null) {
				navigateTo(null);
				executeJavaScript();
			} else if (filterForm.hasFilter() && campaignSelector != null) {
				UI.getCurrent().getSession().getCurrent().setAttribute("lastcriteria",
						criteria.toUrlParams().toString());
				rowsCount.update(grid.getItemCount());
			}
		});

		callBackFormData();

		return filterForm;

	}

	public void callBackFormData() {
		filterForm.setFormMetaChangedCallback(createFormMetaChangedCallback());
		grid.reload();
		executeJavaScript();
	}

	private Consumer<CampaignFormMetaReferenceDto> createFormMetaChangedCallback() {
		return formMetaReference -> {
			grid.removeAllColumns();
			grid.addDefaultColumns();
			executeJavaScript();
			if (formMetaReference != null) {

				CampaignFormMetaDto formMeta = FacadeProvider.getCampaignFormMetaFacade()
						.getCampaignFormMetaByUuid(formMetaReference.getUuid());
				Language userLanguage = UserProvider.getCurrent().getUser().getLanguage();
				CampaignFormTranslations translations = null;
				if (userLanguage != null) {
					translations = formMeta.getCampaignFormTranslations().stream()
							.filter(t -> t.getLanguageCode().equals(userLanguage.getLocale().toString())).findFirst()
							.orElse(null);
				}
				final boolean onlyImportantFormElements = importanceFilterSwitcher.isImportantSelected();
				final List<CampaignFormElement> campaignFormElements = formMeta.getCampaignFormElements();
				for (CampaignFormElement element : campaignFormElements) {
					if (element.isImportant() || !onlyImportantFormElements) {
						String caption = null;
						if (translations != null) {
							caption = translations.getTranslations().stream()
									.filter(t -> t.getElementId().equals(element.getId()))
									.map(TranslationElement::getCaption).findFirst().orElse(null);
						}
						if (caption == null) {
							caption = element.getCaption();
						}

						if (caption != null) {
							grid.addCustomColumn(element.getId(), caption);
							executeJavaScript();
						}
					}
				}
				executeJavaScript();
			}
			executeJavaScript();
		};

	}

	private Consumer<CampaignFormMetaReferenceDto> createFormMetaChangedCallbackPhase() {

		return formMetaReference -> {
			grid.removeAllColumns();
			grid.addDefaultColumns();
			executeJavaScript();
			if (formMetaReference != null) {
				CampaignFormMetaDto formMeta = FacadeProvider.getCampaignFormMetaFacade()
						.getCampaignFormMetaByUuid(formMetaReference.getUuid());
				Language userLanguage = UserProvider.getCurrent().getUser().getLanguage();
				CampaignFormTranslations translations = null;
				if (userLanguage != null) {
					translations = formMeta.getCampaignFormTranslations().stream()
							.filter(t -> t.getLanguageCode().equals(userLanguage.getLocale().toString())).findFirst()
							.orElse(null);
				}
				final boolean onlyImportantFormElements = importanceFilterSwitcher.isImportantSelected();
				final List<CampaignFormElement> campaignFormElements = formMeta.getCampaignFormElements();
				for (CampaignFormElement element : campaignFormElements) {
					if (element.isImportant() || !onlyImportantFormElements) {
						String caption = null;
						if (translations != null) {
							caption = translations.getTranslations().stream()
									.filter(t -> t.getElementId().equals(element.getId()))
									.map(TranslationElement::getCaption).findFirst().orElse(null);
						}
						if (caption == null) {
							caption = element.getCaption();
							executeJavaScript();
						}

						if (caption != null) {
							grid.addCustomColumn(element.getId(), caption);
							executeJavaScript();
							rowsCount.update(grid.getItemCount());
						}
					}
				}
			}
		};
	}

	@Override
	public void enter(ViewChangeEvent event) {

		UserDto user = UserProvider.getCurrent().getUser();
		if (event.getParameters() != null) {
			// split at "&"
			String[] queryParameters = event.getParameters().split("&");
			for (String queryParameter : queryParameters) {
				String[] innerSplit = queryParameter.split("=");
				if (queryParameter.contains("area")) {
					AreaDto uu = FacadeProvider.getAreaFacade().getByUuid(innerSplit[1]);
					AreaReferenceDto areas = uu.toReference();
					criteria.setArea(areas);
				}
				if (queryParameter.contains("region")) {
					RegionReferenceDto regions = FacadeProvider.getRegionFacade()
							.getRegionReferenceByUuid(innerSplit[1]);
					criteria.setRegion(regions);
				}
				if (queryParameter.contains("district")) {
					DistrictReferenceDto districts = FacadeProvider.getDistrictFacade()
							.getDistrictReferenceByUuid(innerSplit[1]);
					criteria.setDistrict(districts);
				}
				if (queryParameter.contains("community")) {
					CommunityReferenceDto communitys = FacadeProvider.getCommunityFacade()
							.getCommunityReferenceByUuid(innerSplit[1]);
					criteria.setCommunity(communitys);
				}
				if (queryParameter.contains("formType")) {
					criteria.setFormType(innerSplit[1]);
					executeJavaScript();
				}
				if (queryParameter.contains("campaign")) {
					CampaignReferenceDto campaign = FacadeProvider.getCampaignFacade()
							.getReferenceByUuid(innerSplit[1]);
					criteria.setCampaign(campaign);

					// campaignSelector.setValue(criteria.getCampaign());

				}
				if (queryParameter.contains("campaignFormMeta")) {
					CampaignFormMetaReferenceDto campaignsmeta = FacadeProvider.getCampaignFormMetaFacade()
							.getCampaignFormMetaReferenceByUuid(innerSplit[1]);
					criteria.setCampaignFormMeta(campaignsmeta);
					filterForm.cbCampaignForm.setValue(campaignsmeta);
					filterForm.cbCampaignForm.setInputPrompt(campaignsmeta.getCaption());
					executeJavaScript();
				}
			}
		}

		applyingCriteria = true;
		executeJavaScript();
		filterForm.setValue(criteria);
		executeJavaScript();
		applyingCriteria = false;

		grid.reload();
		executeJavaScript();
		rowsCount.update(grid.getItemCount());

		super.enter(event);
	}

	public void executeJavaScript() {

		JavaScript jss = Page.getCurrent().getJavaScript();
		jss.execute("$(document).ready(function() {\n"
				+ "document.querySelectorAll(\".v-grid-column-header-content.v-grid-column-default-header-content\").forEach(function (elem) {\r\n"
				+ "  if (parseFloat(window.getComputedStyle(elem).width) === parseFloat(window.getComputedStyle(elem.parentElement).width)) {\r\n"
				+ "    elem.setAttribute(\"title\", elem.textContent);\r\n" + "  }\r\n"
				+ "    elem.setAttribute(\"title\", elem.textContent);\r\n"

				+ "});" + "});");

		grid.addColumnReorderListener(e -> {
			JavaScript jsss = Page.getCurrent().getJavaScript();
			jsss.execute(
					"document.querySelectorAll(\".v-grid-column-header-content.v-grid-column-default-header-content\").forEach(function (elem) {\r\n"
							+ "  if (parseFloat(window.getComputedStyle(elem).width) === parseFloat(window.getComputedStyle(elem.parentElement).width)) {\r\n"
							+ "    elem.setAttribute(\"title\", elem.textContent);\r\n" + "  }\r\n"
							+ "    elem.setAttribute(\"title\", elem.textContent);\r\n" + "});");
		});
	}

}