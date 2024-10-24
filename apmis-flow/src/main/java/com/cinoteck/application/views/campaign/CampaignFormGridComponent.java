package com.cinoteck.application.views.campaign;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.cinoteck.application.UserProvider;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.campaign.CampaignDto;
import de.symeda.sormas.api.campaign.diagram.CampaignDashboardElement;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaExpiryDto;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaReferenceDto;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaWithExpReferenceDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;

import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.function.ValueProvider;

public class CampaignFormGridComponent extends VerticalLayout {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5040277864446152755L;
	List<CampaignFormMetaReferenceDto> savedCampaignFormMetas;
	List<CampaignFormMetaReferenceDto> allCampaignFormMetas;
	Grid<CampaignFormMetaReferenceDto> grid = new Grid<>(CampaignFormMetaReferenceDto.class, false);
	CampaignDto capaingDto;
	private CampaignFormMetaReferenceDto formBeenEdited;
	private String campaignPhase;
	List<CampaignFormMetaReferenceDto> allElements;
	private UserProvider userProvider = new UserProvider();
	private String userLanguage;
	FormLayout formx = new FormLayout();

	public CampaignFormGridComponent(List<CampaignFormMetaReferenceDto> savedCampaignFormMetas,
			List<CampaignFormMetaReferenceDto> allCampaignFormMetas, CampaignDto capaingDto, String campaignPhase) {
		this.savedCampaignFormMetas = savedCampaignFormMetas;
		this.allCampaignFormMetas = allCampaignFormMetas;
		this.capaingDto = capaingDto;
		this.campaignPhase = campaignPhase;
		this.userLanguage = userLanguage;

		userLanguage = userProvider.getUser().getLanguage().toString();

		if (userLanguage.equalsIgnoreCase("pashto")) {
			grid.addColumn(CampaignFormMetaReferenceDto::getFormname_ps_af)
					.setHeader(I18nProperties.getCaption(Captions.formname) + "Pashto");
		} else if (userLanguage.equalsIgnoreCase("Dari")) {
			grid.addColumn(CampaignFormMetaReferenceDto::getFormname_fa_af)
					.setHeader(I18nProperties.getCaption(Captions.formname) + "Dari ");
		} else if (userLanguage.equalsIgnoreCase("english")) {
			grid.addColumn(CampaignFormMetaReferenceDto::getCaption)
					.setHeader(I18nProperties.getCaption(Captions.formname));
		}

//		grid.addColumn(CampaignFormMetaReferenceDto::getDaysExpired)
//				.setHeader(I18nProperties.getCaption(Captions.expiry) + " (default)");
		grid.addColumn(this::getDaysExpiredEditable)
				.setHeader(I18nProperties.getCaption(Captions.expiry) + " custom days (may not update until saved)");

		grid.setItems(savedCampaignFormMetas);
		addClassName("list-view");
		setSizeFull();
		add(getContent(capaingDto, savedCampaignFormMetas));

	}

	private int getDaysExpiredEditable(CampaignFormMetaReferenceDto item) {
		return FacadeProvider.getCampaignFacade().getCampaignFormExp(item.getUuid(), capaingDto.getUuid());
	}

	private Component getContent(CampaignDto capaingDto, List<CampaignFormMetaReferenceDto> savedCampaignFormMetas) {
//		System.out.println(capaingDto + "0------HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH");
		if(capaingDto != null) {
			VerticalLayout formx = editorForm(capaingDto, savedCampaignFormMetas);
			formx.getStyle().remove("width");
			HorizontalLayout content = new HorizontalLayout(grid, formx);
			content.setFlexGrow(4, grid);
			content.setFlexGrow(0, formx);
			content.addClassNames("content");
			content.setSizeFull();
			return content;	
		}else {
			VerticalLayout saveCampaignFirstLayout = new VerticalLayout();
			
			return saveCampaignFirstLayout;
		}
		
		
	}

	private VerticalLayout editorForm(CampaignDto capaingDto,
			List<CampaignFormMetaReferenceDto> savedCampaignFormMetas) {

		VerticalLayout vert = new VerticalLayout();

		Button plusButton = new Button(new Icon(VaadinIcon.PLUS));
		plusButton.addThemeVariants(ButtonVariant.LUMO_ICON);
		plusButton.setTooltipText(I18nProperties.getCaption(Captions.addNewForm));

		Button deleteButton = new Button(new Icon(VaadinIcon.DEL_A));
		deleteButton.addThemeVariants(ButtonVariant.LUMO_ICON);
		deleteButton.getStyle().set("background-color", "red!important");
		deleteButton.setTooltipText(I18nProperties.getCaption(Captions.removeThisForm));

		Button saveButton = new Button(I18nProperties.getCaption(Captions.actionAdd), new Icon(VaadinIcon.CHECK));

		Button cacleButton = new Button(I18nProperties.getCaption(Captions.actionCancel), new Icon(VaadinIcon.REFRESH));

		ComboBox<CampaignFormMetaReferenceDto> forms = new ComboBox<CampaignFormMetaReferenceDto>();
		forms.setLabel(I18nProperties.getCaption(Captions.campaignCampaignForm));
		forms.setItems(allCampaignFormMetas);
		// if its a clicked action set the value from the item....TODO

		IntegerField daysExpire = new IntegerField();
		daysExpire.setLabel(I18nProperties.getCaption(Captions.daysTOExpiry));
		String datd = "";

		if (capaingDto == null) {
			forms.addValueChangeListener(e -> {
				daysExpire.clear();
				daysExpire
						.setValue(FacadeProvider.getCampaignFacade().getDefaultCampaignFormExp(e.getValue().getUuid()));

			});
		} else {

			forms.addValueChangeListener(e -> {
				boolean isFormUUidInList = false;
				for (CampaignFormMetaReferenceDto formName : savedCampaignFormMetas) {
					if (formName.getUuid().equalsIgnoreCase(e.getValue().getUuid())) {
						isFormUUidInList = true;
						break;
					}
				}

				if (isFormUUidInList) {
					daysExpire.clear();
					daysExpire.setValue(
							FacadeProvider.getCampaignFacade().getCampaignFormExp(e.getValue().getUuid(),capaingDto.getUuid() ));
				} else {
					daysExpire.clear();
					daysExpire.setValue(
							FacadeProvider.getCampaignFacade().getDefaultCampaignFormExp(e.getValue().getUuid()));
				}

			});

		}
		;

		if (capaingDto != null && capaingDto.getStartDate() != null) {
			datd = capaingDto.getStartDate().toLocaleString();
		} else if (capaingDto != null && capaingDto.getStartDate() == null) {

		}
		daysExpire.setHelperText(I18nProperties.getString(Strings.max60DaysFromStartDate) + " (" + datd + ")");
		daysExpire.setMin(1);
		daysExpire.setMax(60);
		daysExpire.setStepButtonsVisible(true);
		// if its a clicked action set the value from the item....TODO

		HorizontalLayout buttonLay = new HorizontalLayout(plusButton, deleteButton);

		// buttonLay.setEnabled(false);

		HorizontalLayout buttonAfterLay = new HorizontalLayout(saveButton, cacleButton);
		buttonAfterLay.getStyle().set("flex-wrap", "wrap");
		buttonAfterLay.setJustifyContentMode(JustifyContentMode.END);
		buttonLay.setSpacing(true);


		deleteButton.addClickListener(dex -> {
			if (formBeenEdited == null) {
				Notification.show(I18nProperties.getString(Strings.pleaseSelectFormFirst));
			} else {

				capaingDto.getCampaignFormMetas().remove(formBeenEdited);
				// FacadeProvider.getCampaignFacade().saveCampaign(capdto);
				Notification.show(formBeenEdited + I18nProperties.getString(Strings.wasRemovedFromCampaign));
				grid.setItems(capaingDto.getCampaignFormMetas());
			}
			grid.setItems(capaingDto.getCampaignFormMetas(campaignPhase));
		});

		plusButton.addClickListener(ce -> {
			CampaignFormMetaReferenceDto newcampform = new CampaignFormMetaReferenceDto();

			formx.setVisible(true);
			buttonAfterLay.setVisible(true);

			try {
				forms.setValue(newcampform);
			} finally {
				saveButton.setText(I18nProperties.getCaption(Captions.actionAdd));
				daysExpire.setValue(5);
			}
			grid.setItems(capaingDto.getCampaignFormMetas(campaignPhase));
			grid.setHeight("auto !important");

		});

		cacleButton.addClickListener(ees -> {
			CampaignFormMetaReferenceDto newcampform = new CampaignFormMetaReferenceDto();

			formx.setVisible(false);
			buttonAfterLay.setVisible(false);

			forms.setValue(newcampform);
			saveButton.setText(I18nProperties.getCaption(Captions.actionSave));
			daysExpire.setValue(0);
			grid.setItems(capaingDto.getCampaignFormMetas(campaignPhase));
			grid.setHeight("");
		});

		saveButton.addClickListener(e -> {

			if (((Button) e.getSource()).getText().equalsIgnoreCase("Add")
					|| ((Button) e.getSource()).getText().equalsIgnoreCase("Add?")) {
				CampaignFormMetaReferenceDto newCampForm = forms.getValue();

				UUID uuid = UUID.randomUUID();
				String fullUUID = uuid.toString(); // Get full UUID with dashes
				String truncatedUUID = fullUUID.substring(0, Math.min(fullUUID.length(), 36));

				String uuidWithHyphens = truncatedUUID.toString();

				Date startDate = capaingDto.getStartDate();
				long differenceInMilliseconds = startDate.getTime();
				long millisecondsInOneDay = 24 * 60 * 60 * 1000; // 24 hours * 60 minutes * 60 seconds * 1000
																	// milliseconds
				long daysexpiredinMilliseconds = daysExpire.getValue().longValue() * millisecondsInOneDay;
				Date newDate = new Date(startDate.getTime() + differenceInMilliseconds);

				CampaignFormMetaWithExpReferenceDto camFormExp = new CampaignFormMetaWithExpReferenceDto();// capaingDto.getUuid(),
																											// forms.getValue().getUuid(),
//						daysExpire.getValue().longValue(), newDate , uuidWithHyphens.toUpperCase());
				camFormExp.setCampaignId(capaingDto.getUuid());
				camFormExp.setFormId(forms.getValue().getUuid());
				camFormExp.setDaysExpired(daysExpire.getValue().longValue());
				camFormExp.setDate(newDate);
				camFormExp.setUuid(uuidWithHyphens.toUpperCase());

				newCampForm.setCaption(forms.getValue().toString());
				newCampForm.setDaysExpired(daysExpire.getValue());

				capaingDto.getCampaignFormMetas().add(newCampForm);
				capaingDto.getCampaignFormMetaExpiry().add(camFormExp);
				capaingDto.setCampaignFormMetaExpiryDto(capaingDto.getCampaignFormMetaExpiry());

				allCampaignFormMetas.removeAll(capaingDto.getCampaignFormMetas());

				forms.setItems(allCampaignFormMetas);

				Notification.show(I18nProperties.getString(Strings.newFormAddedSucces));
				grid.setItems(capaingDto.getCampaignFormMetas(campaignPhase));

			} else {
				System.out.println(((Button) e.getSource()).getText()
						+ " Text buttton when addd ELSEEEEEEEEEEEEEEEEEEE IFFFFFFFFFF");
				if (formBeenEdited != null) {
					CampaignFormMetaReferenceDto newCampForm = forms.getValue();
					capaingDto.getCampaignFormMetas().remove(formBeenEdited);
					capaingDto.getCampaignFormMetas().add(newCampForm);

					List<CampaignFormMetaWithExpReferenceDto> camFormExp_ = new ArrayList<>();
					CampaignFormMetaReferenceDto newCampFormxx = formBeenEdited;
					String getFormCaption = newCampFormxx.getUuid();

					camFormExp_ = capaingDto.getCampaignFormMetaExpiry().stream()
							.filter(ew -> ew.getUuid() == getFormCaption).collect(Collectors.toList());

//					CampaignFormMetaWithExpReferenceDto camFormExp_i = new CampaignFormMetaWithExpReferenceDto(capaingDto.getUuid(), forms.getValue().getUuid(),
//							daysExpire.getValue().longValue());
//									String uuidWithHyphens = uuid.toString();
					UUID uuid = UUID.randomUUID();
					String fullUUID = uuid.toString(); // Get full UUID with dashes
					String truncatedUUID = fullUUID.substring(0, Math.min(fullUUID.length(), 36));

					String uuidWithHyphens = truncatedUUID.toString();

					Date startDate = capaingDto.getStartDate();
					long differenceInMilliseconds = startDate.getTime();
					long millisecondsInOneDay = 24 * 60 * 60 * 1000; // 24 hours * 60 minutes * 60 seconds * 1000
																		// milliseconds
					long daysexpiredinMilliseconds = daysExpire.getValue().longValue() * millisecondsInOneDay;
					Date newDate = new Date(startDate.getTime() + differenceInMilliseconds);

					CampaignFormMetaWithExpReferenceDto camFormExp_i = new CampaignFormMetaWithExpReferenceDto();// capaingDto.getUuid(),
																													// forms.getValue().getUuid(),
//					daysExpire.getValue().longValue(), newDate , uuidWithHyphens.toUpperCase());
					camFormExp_i.setCampaignId(capaingDto.getUuid());
					camFormExp_i.setFormId(forms.getValue().getUuid());
					camFormExp_i.setDaysExpired(daysExpire.getValue().longValue());
					camFormExp_i.setDate(newDate);
					camFormExp_i.setUuid(uuidWithHyphens.toUpperCase());
//					CampaignFormMetaWithExpReferenceDto camFormExp_i = new CampaignFormMetaWithExpReferenceDto(capaingDto.getUuid(), forms.getValue().getUuid(),
//							daysExpire.getValue().longValue(),null , null);

//					CampaignFormMetaWithExpReferenceDto camFormExp_i = new CampaignFormMetaWithExpReferenceDto(capaingDto.getUuid(), forms.getValue().getUuid(),
//							daysExpire.getValue().longValue(), null , null);

					if (camFormExp_.size() > 0)
						capaingDto.getCampaignFormMetaExpiry().remove(camFormExp_.get(0));

					capaingDto.getCampaignFormMetaExpiry().add(camFormExp_i);
					capaingDto.setCampaignFormMetaExpiryDto(capaingDto.getCampaignFormMetaExpiry());

					grid.setItems(capaingDto.getCampaignFormMetas(campaignPhase));
					getSavedElements();

					Notification.show(I18nProperties.getString(Strings.campaignUpdated));
				} else {

					System.out.println(
							((Button) e.getSource()).getText() + " Text buttton when addd ELSEEEEEEEEEEEEEEEEEEE");
					CampaignFormMetaReferenceDto newCampForm = forms.getValue();
					UUID uuid = UUID.randomUUID();
					String fullUUID = uuid.toString(); // Get full UUID with dashes
					String truncatedUUID = fullUUID.substring(0, Math.min(fullUUID.length(), 36));

					String uuidWithHyphens = truncatedUUID.toString();
					Date startDate = capaingDto.getStartDate();
					long differenceInMilliseconds = startDate.getTime();
					long millisecondsInOneDay = 24 * 60 * 60 * 1000; // 24 hours * 60 minutes * 60 seconds * 1000
																		// milliseconds
					long daysexpiredinMilliseconds = daysExpire.getValue().longValue() * millisecondsInOneDay;
					Date newDate = new Date(startDate.getTime() + differenceInMilliseconds);
//

					CampaignFormMetaWithExpReferenceDto camFormExp = new CampaignFormMetaWithExpReferenceDto();// capaingDto.getUuid(),
					// forms.getValue().getUuid(),
//daysExpire.getValue().longValue(), newDate , uuidWithHyphens.toUpperCase());
					camFormExp.setCampaignId(capaingDto.getUuid());
					camFormExp.setFormId(forms.getValue().getUuid());
					camFormExp.setDaysExpired(daysExpire.getValue().longValue());
					camFormExp.setDate(newDate);
					camFormExp.setUuid(uuidWithHyphens.toUpperCase());
//					CampaignFormMetaWithExpReferenceDto camFormExp = new CampaignFormMetaWithExpReferenceDto(
//							capaingDto.getUuid(), forms.getValue().getUuid(), daysExpire.getValue().longValue(), null,
//							null);

//					CampaignFormMetaWithExpReferenceDto camFormExp = new CampaignFormMetaWithExpReferenceDto(capaingDto.getUuid(), forms.getValue().getUuid(),
//							daysExpire.getValue().longValue(), null , null);
//					
//					CampaignFormMetaWithExpReferenceDto camFormExp = new CampaignFormMetaWithExpReferenceDto(capaingDto.getUuid(), forms.getValue().getUuid(),
//							daysExpire.getValue().longValue());

					newCampForm.setCaption(forms.getValue().toString());
					newCampForm.setDaysExpired(daysExpire.getValue());

					capaingDto.getCampaignFormMetas().add(newCampForm);
					capaingDto.getCampaignFormMetaExpiry().add(camFormExp);
					capaingDto.setCampaignFormMetaExpiryDto(capaingDto.getCampaignFormMetaExpiry());
					allCampaignFormMetas.removeAll(capaingDto.getCampaignFormMetas());

					forms.setItems(allCampaignFormMetas);

					Notification.show(I18nProperties.getString(Strings.newFormAddedSucces));
					grid.setItems(capaingDto.getCampaignFormMetas(campaignPhase));
//					Notification.show(I18nProperties.getString(Strings.pleaseSelectaFormBeforeUpdate));
				}
			}
			grid.setHeight("");
		});

		formx.add(forms, daysExpire);
		formx.setColspan(forms, 1);
		formx.setColspan(daysExpire, 1);
		formx.setVisible(false);
		buttonAfterLay.setVisible(false);

		vert.add(buttonLay, formx, buttonAfterLay);
		
		grid.addSelectionListener(ee -> {

			int size = ee.getAllSelectedItems().size();
			if (size > 0) {
				CampaignFormMetaReferenceDto selectedCamp = ee.getFirstSelectedItem().get();
				formBeenEdited = selectedCamp;
				boolean isSingleSelection = size == 1;
				buttonLay.setEnabled(isSingleSelection);
				buttonAfterLay.setEnabled(isSingleSelection);
				formx.setVisible(true);
				buttonAfterLay.setVisible(true);

				// delete.setEnabled(size != 0);
				forms.setValue(selectedCamp);
				saveButton.setText(I18nProperties.getCaption(Captions.update));
				int dayz = getDaysExpiredEditable(selectedCamp);
				
				daysExpire.clear();
				daysExpire.setPlaceholder(String.valueOf(dayz));
				daysExpire.setValue(daysExpire.getValue() == null ? dayz : selectedCamp.getDaysExpired());

			} else {
				formBeenEdited = new CampaignFormMetaReferenceDto();
			}
		});


		return vert;
	}

	public List<CampaignFormMetaReferenceDto> getSavedElements() {
		return grid.getDataProvider().fetch(new Query<>()).collect(Collectors.toList());
	}

}