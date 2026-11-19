package com.cinoteck.application.views.user;

import com.vaadin.flow.component.formlayout.FormLayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.context.SessionScoped;

import com.cinoteck.application.UserProvider;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.exception.ZeroException;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.selection.SelectionEvent;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.validator.RegexpValidator;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;

import de.symeda.sormas.api.AuthProvider;
import de.symeda.sormas.api.CountryHelper;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.Language;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.infrastructure.area.AreaReferenceDto;
import de.symeda.sormas.api.infrastructure.area.AreaType;
import de.symeda.sormas.api.infrastructure.community.CommunityReferenceDto;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.user.FormAccess;
import de.symeda.sormas.api.user.JurisdictionLevel;
import de.symeda.sormas.api.user.UserActivitySummaryDto;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.user.UserHelper;
import de.symeda.sormas.api.user.UserRole;
import de.symeda.sormas.api.user.UserType;
import de.symeda.sormas.api.user.UserRole.UserRoleValidationException;

@Route(value = "/edit-user")
public class UserForm extends FormLayout {
	private static final long serialVersionUID = 6918900346481904170L;

	private boolean isDistrictMulti = false;
	Binder<UserDto> binder = new BeanValidationBinder<>(UserDto.class);

	List<AreaReferenceDto> regions;
	List<RegionReferenceDto> provinces;
	List<DistrictReferenceDto> districts;
	List<CommunityReferenceDto> communities;
	List<CommunityReferenceDto> communitiesx;

	TextField firstName = new TextField(I18nProperties.getCaption(Captions.firstName));
	TextField lastName = new TextField(I18nProperties.getCaption(Captions.lastName));

	TextField userEmail = new TextField(I18nProperties.getCaption(Captions.User_userEmail));
	TextField phone = new TextField(I18nProperties.getCaption(Captions.User_phone));
	TextField userPosition = new TextField(I18nProperties.getCaption(Captions.User_userPosition));
	TextField userOrganisation = new TextField(I18nProperties.getCaption(Captions.User_userOrganisation));

	ComboBox<AreaReferenceDto> region = new ComboBox<>(I18nProperties.getCaption(Captions.area));
	ComboBox<RegionReferenceDto> province = new ComboBox<>(I18nProperties.getCaption(Captions.region));
	ComboBox<DistrictReferenceDto> district = new ComboBox<>(I18nProperties.getCaption(Captions.district));

	TextField street = new TextField(I18nProperties.getCaption(Captions.Location_street));
	TextField houseNumber = new TextField(I18nProperties.getCaption(Captions.Location_houseNumber));
	TextField additionalInformation = new TextField(I18nProperties.getCaption(Captions.Location_additionalInformation));
	TextField postalCode = new TextField(I18nProperties.getCaption(Captions.Location_postalCode));
	ComboBox<AreaType> areaType = new ComboBox<>(I18nProperties.getCaption(Captions.Location_areaType));
	TextField city = new TextField(I18nProperties.getCaption(Captions.city));

	TextField userName = new TextField(I18nProperties.getCaption(Captions.User_userName));
	Checkbox activeCheck = new Checkbox();
	private boolean active = true;

	Checkbox commusr = new Checkbox(I18nProperties.getCaption(Captions.User_commonUser));
	private boolean isCommonUser = false;
	MultiSelectComboBox<UserRole> userRoles = new MultiSelectComboBox<>(
			I18nProperties.getCaption(Captions.User_userRoles));

	CheckboxGroup<FormAccess> formAccess = new CheckboxGroup<>();

	ComboBox<Language> language = new ComboBox<>(I18nProperties.getCaption(Captions.language));

	CheckboxGroup districtMulti = new CheckboxGroup<>();
	CheckboxGroup clusterNo = new CheckboxGroup<>();

	Button save = new Button(I18nProperties.getCaption(Captions.actionSave));
	Button delete = new Button(I18nProperties.getCaption(Captions.actionDelete));
	Button close = new Button(I18nProperties.getCaption(Captions.actionCancel));

	Button createPassword = new Button(I18nProperties.getCaption(Captions.userResetPassword));

	ConfirmDialog _dialog = new ConfirmDialog();

	Map<String, Component> map = new HashMap<>();
	RegexpValidator patternValidator = new RegexpValidator("^[A-Za-z]+$", "Only letters are allowed");
	EmailValidator emailVal = new EmailValidator(I18nProperties.getCaption(Captions.notaValidEmail));

	String initialLastNameValue = "";
	UserDto usr = new UserDto();
	static UserProvider currentUser = new UserProvider();
	Set<UserRole> roles = new HashSet<UserRole>();
	Set<FormAccess> formAccessesList = new LinkedHashSet<FormAccess>();

	private final UserProvider userProvider = new UserProvider();
	H2 pInfo = new H2(I18nProperties.getString(Strings.headingPersonData));
	H2 userData = new H2(I18nProperties.getString(Strings.headingUserData));

	boolean editmode = false;
	UserDto user;

	public UserForm(List<AreaReferenceDto> regions, List<RegionReferenceDto> provinces,
			List<DistrictReferenceDto> districts, UserDto user, boolean editmode) {

		this.user = user;
		

		
		addClassName("contact-form");
		HorizontalLayout hor = new HorizontalLayout();
		Icon vaadinIcon = new Icon(VaadinIcon.ARROW_CIRCLE_LEFT_O);
		Span prefixText = new Span(I18nProperties.getCaption(Captions.allUsers));
		prefixText.setClassName("backButtonText");
		HorizontalLayout layout = new HorizontalLayout(vaadinIcon, prefixText);
		vaadinIcon.setClassName("backButton");
		hor.setJustifyContentMode(JustifyContentMode.START);
		hor.add(layout);
		hor.setHeight("5px");
		hor.setId("backLayout");
		hor.getStyle().set("width", "none !important");
		layout.addClickListener(event -> {
		    VaadinSession.getCurrent().setAttribute("contact-form", null);
		    fireEvent(new CloseEvent(this));
		});
		add(hor);
		configureFields(user);
		System.out.println("____TRSTING LANGUAGE TRANSLATOR : " + I18nProperties.getUserLanguage());
		updatePasswordDialog();
	}

	public UserForm() {
	}

	@SuppressWarnings("unchecked")
	public void configureFields(UserDto user) {
		JurisdictionLevel currentJurisdiction = UserRole.getJurisdictionLevel(userProvider.getUser().getUserRoles());

		

		pInfo = new H2(I18nProperties.getString(Strings.headingPersonData));
		this.setColspan(pInfo, 2);

		userData = new H2(I18nProperties.getString(Strings.headingUserData));
		this.setColspan(userData, 2);

		firstName = new TextField(I18nProperties.getCaption(Captions.firstName));

		this.binder.forField(this.firstName).asRequired(I18nProperties.getCaption("firstNameRequired"))
				.bind(userx -> userx.getFirstName(), (userx, firstName) -> userx.setFirstName(firstName));
		this.binder.forField(this.lastName).asRequired(I18nProperties.getCaption("lastNameRequired"))
				.bind(userx -> userx.getLastName(), (userx, lastName) -> userx.setLastName(lastName));
		this.binder.forField(this.userEmail).bind(userx -> userx.getUserEmail(),
				(userx, userEmail) -> userx.setUserEmail(userEmail));
		this.binder.forField(this.phone).bind(userx -> userx.getPhone(), (userx, phone) -> userx.setPhone(phone));
		this.binder.forField(this.userPosition).bind(userx -> userx.getUserPosition(),
				(userx, userPosition) -> userx.setUserPosition(userPosition));
		this.binder.forField(this.userOrganisation).bind(userx -> userx.getUserOrganisation(),
				(userx, userOrganisation) -> userx.setUserOrganisation(userOrganisation));
		this.binder.forField(this.userName).asRequired("Please Fill Out a First and Last Name")
				.bind(userx -> userx.getUserName(), (userx, userOrganisation) -> userx.setUserName(userOrganisation));

		this.setColspan(activeCheck, 2);
		activeCheck.setLabel("Active ?");
		activeCheck.setValue(active);

		this.binder.forField(this.activeCheck).bind(userx -> userx.isActive(),
				(userx, userOrganisation) -> userx.setActive(userOrganisation));

		this.binder.forField(this.commusr).bind(userx -> userx.isCommomUser(),
				(userx, userOrganisation) -> userx.setCommomUser(userOrganisation));

		formAccess.setLabel(I18nProperties.getCaption(Captions.formAccess));

		this.binder.forField(this.formAccess).bind(userx -> userx.getFormAccess(),
				(userx, userOrganisation) -> userx.setFormAccess(userOrganisation));

		this.binder.forField(this.language).bind(userx -> userx.getLanguage(),
				(userx, userOrganisation) -> userx.setLanguage(userOrganisation));

		this.binder.forField(this.userRoles).withValidator(new UserRolesValidator())
				.asRequired(I18nProperties.getCaption(Captions.userRoleRequired))
				.bind(userx -> userx.getUserRoles(), (userx, userOrganisation) -> userx.setUserRoles(userOrganisation));

		this.binder.forField(this.region).bind(userx -> userx.getArea(),
				(userx, userOrganisation) -> userx.setArea(userOrganisation));

		this.binder.forField(this.province).bind(userx -> userx.getRegion(),
				(userx, userOrganisation) -> userx.setRegion(userOrganisation));

		this.binder.forField(this.district).bind(userx -> userx.getDistrict(),
				(userx, userOrganisation) -> userx.setDistrict(userOrganisation));

		this.binder.forField(this.districtMulti);
		binder.bind(districtMulti, userx -> userx.getDistricts(),
				(userx, userOrganisation) -> userx.setDistricts(userOrganisation));

		districtMulti.setVisible(false);

		this.binder.forField(this.clusterNo);
		binder.bind(clusterNo, userx -> userx.getCommunity(),
				(userx, userOrganisation) -> userx.setCommunity(userOrganisation));

		// Sync the selected cluster numbers onto the bean's communitynos (registered once)
		clusterNo.addValueChangeListener(ex -> {
			@SuppressWarnings("unchecked")
			Set<CommunityReferenceDto> selected = (Set<CommunityReferenceDto>) ex.getValue();
			if (selected != null && !selected.isEmpty()) {
				Set<String> selectedClusterNumbers = new HashSet<>();
				for (CommunityReferenceDto community : selected) {
					if (community.getNumber() != null) {
						selectedClusterNumbers.add(community.getNumber().toString());
					}
				}
				user.setCommunitynos(selectedClusterNumbers);
			} else {
				user.setCommunitynos(new HashSet<>());
			}
		});

		roles = FacadeProvider.getUserRoleConfigFacade().getEnabledUserRoles();
		roles.remove(UserRole.BAG_USER);

		if (userProvider.getUser().getUsertype() == UserType.WHO_USER) {
			if (!roles.contains(UserRole.PUBLISH_USER)) {
				roles.add(UserRole.PUBLISH_USER);
			}
			if (!roles.contains(UserRole.EDITOR_USER)) {
				roles.add(UserRole.EDITOR_USER);
			}
		} else {
			if (roles.contains(UserRole.PUBLISH_USER)) {
				roles.remove(UserRole.PUBLISH_USER);
			}
			if (!roles.contains(UserRole.EDITOR_USER)) {
				roles.add(UserRole.EDITOR_USER);
			}
		}

		Set<UserRole> loggedInRoles = null;
		try {
			loggedInRoles = UserProvider.getCurrent().getUserRoles();
		} catch (Exception e) {
			loggedInRoles = userProvider.getUser().getUserRoles();
		}

		roles = getHierarchicalManageableRoles(loggedInRoles, user != null ? user.getUserRoles() : null, roles);

		List<UserRole> rolesz = new ArrayList<>(roles);
		rolesz.remove(UserRole.BAG_USER);

		Collections.sort(rolesz, new UserRoleCustomComparator());

		Set<UserRole> sortedUserRoles = new LinkedHashSet<>(rolesz);
		userRoles.setItems(sortedUserRoles);

		this.setColspan(userRoles, 1);
		userRoles.addValueChangeListener(e -> {
			updateFieldsByUserRole(e.getValue());
			validateUserRoles();
			
		});

		ComboBox<UserType> userTypes = new ComboBox<UserType>();
		userTypes.setItems(UserType.values());

		language.setItemLabelGenerator(Language::toString);
		language.setItems(Language.getAssignableLanguages());

		binder.forField(language).asRequired(I18nProperties.getString(Strings.languageRequired))
				.bind(UserDto::getLanguage, UserDto::setLanguage);

		regions = FacadeProvider.getAreaFacade().getAllActiveAsReference();
		if (userProvider.getUser().getLanguage().toString().equals("Pashto")) {
			region.setItems(FacadeProvider.getAreaFacade().getAllActiveAsReferencePashto());
		} else if (userProvider.getUser().getLanguage().toString().equals("Dari")) {
			region.setItems(FacadeProvider.getAreaFacade().getAllActiveAsReferenceDari());
		} else {
			region.setItems(regions);
		}

		region.setItemLabelGenerator(AreaReferenceDto::getCaption);
		region.addValueChangeListener(e -> {

			if (e.getValue() != null) {
				districtMulti.setVisible(false);
				clusterNo.setVisible(false);
				provinces = FacadeProvider.getRegionFacade().getAllActiveByArea(e.getValue().getUuid());

				if (userProvider.getUser().getLanguage().toString().equals("Pashto")) {
					province.setItems(
							FacadeProvider.getRegionFacade().getAllActiveByAreaPashto(e.getValue().getUuid()));
				} else if (userProvider.getUser().getLanguage().toString().equals("Dari")) {
					province.setItems(FacadeProvider.getRegionFacade().getAllActiveByAreaDari(e.getValue().getUuid()));
				} else {
					province.setItems(provinces);
				}
			}
		});
		
	

		province.setItemLabelGenerator(RegionReferenceDto::getCaption);
		province.addValueChangeListener(e -> {

			if (e.getValue() != null && userRoles.getValue() != null) {
				final JurisdictionLevel jurisdictionLevel = UserRole.getJurisdictionLevel(userRoles.getValue());
				if (jurisdictionLevel == JurisdictionLevel.DISTRICT
						&& userRoles.getValue().contains(UserRole.SURVEILLANCE_OFFICER)) {

					districtMulti.setVisible(true);
					district.setVisible(false);
					clusterNo.setVisible(false);
					this.setColspan(districtMulti, 2);
					districts = FacadeProvider.getDistrictFacade().getAllActiveByRegion(e.getValue().getUuid());

					districtMulti.setLabel(I18nProperties.getCaption(Captions.district));

					for (DistrictReferenceDto item : districts) {
						if (item.getCaption() == null) {
							Notification notification = new Notification();
							notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
							notification.setPosition(Position.MIDDLE);
							Button closeButton = new Button(new Icon("lumo", "cross"));
							closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
							closeButton.getElement().setAttribute("aria-label", "Close");
							closeButton.addClickListener(event -> notification.close());
							Paragraph text = new Paragraph("District cannot be empty, please contact support");
							HorizontalLayout layout = new HorizontalLayout(text, closeButton);
							layout.setAlignItems(Alignment.CENTER);
							notification.add(layout);
							notification.open();
						}
					}

					districtMulti.setItems(districts);
					district.setItems(districts);
					isDistrictMulti = true;
					districtMulti.getChildren().forEach(checkbox -> {
						checkbox.getElement().getClassList().add("custom-checkbox-class");
					});

				} else if (jurisdictionLevel == JurisdictionLevel.DISTRICT
						&& !userRoles.getValue().contains(UserRole.SURVEILLANCE_OFFICER)) {

					districts = FacadeProvider.getDistrictFacade().getAllActiveByRegion(e.getValue().getUuid());

					if (userProvider.getUser().getLanguage().toString().equals("Pashto")) {
						district.setItems(
								FacadeProvider.getDistrictFacade().getAllActiveByRegionPashto(e.getValue().getUuid()));
					} else if (userProvider.getUser().getLanguage().toString().equals("Dari")) {
						district.setItems(
								FacadeProvider.getDistrictFacade().getAllActiveByRegionDari(e.getValue().getUuid()));
					} else {
						district.setItems(districts);
					}

					isDistrictMulti = false;
					districtMulti.setVisible(false);
					district.setVisible(true);
					clusterNo.setVisible(false);

				} else if (jurisdictionLevel == JurisdictionLevel.REGION) {

					districts = FacadeProvider.getDistrictFacade().getAllActiveByRegion(e.getValue().getUuid());

					if (userProvider.getUser().getLanguage().toString().equals("Pashto")) {
						district.setItems(
								FacadeProvider.getDistrictFacade().getAllActiveByRegionPashto(e.getValue().getUuid()));
					} else if (userProvider.getUser().getLanguage().toString().equals("Dari")) {
						district.setItems(
								FacadeProvider.getDistrictFacade().getAllActiveByRegionDari(e.getValue().getUuid()));
					} else {
						district.setItems(districts);
					}

					isDistrictMulti = false;
					districtMulti.setVisible(false);
					district.setVisible(false);
					clusterNo.setVisible(false);

				} else {

					districts = FacadeProvider.getDistrictFacade().getAllActiveByRegion(e.getValue().getUuid());
					if (userProvider.getUser().getLanguage().toString().equals("Pashto")) {
						district.setItems(
								FacadeProvider.getDistrictFacade().getAllActiveByRegionPashto(e.getValue().getUuid()));
					} else if (userProvider.getUser().getLanguage().toString().equals("Dari")) {
						district.setItems(
								FacadeProvider.getDistrictFacade().getAllActiveByRegionDari(e.getValue().getUuid()));
					} else {
						district.setItems(districts);
					}

					isDistrictMulti = false;
					districtMulti.setVisible(false);
					district.setVisible(true);
					clusterNo.setVisible(false);
				}
			}

		});

		district.setItemLabelGenerator(DistrictReferenceDto::getCaption);
		district.addValueChangeListener(e -> {
			Set<UserRole> selectedRoles = userRoles.getValue();
			boolean isObserver = selectedRoles != null && selectedRoles.contains(UserRole.DISTRICT_OBSERVER);
			JurisdictionLevel selectedLevel = (selectedRoles == null || selectedRoles.isEmpty()) ? null
					: UserRole.getJurisdictionLevel(selectedRoles);

			if (!isDistrictMulti && !isObserver) {
				DistrictReferenceDto districtDto = (DistrictReferenceDto) e.getValue();

				if (e.getValue() != null) {
					if (selectedLevel == JurisdictionLevel.COMMUNITY) {
						populateClusterOptions(districtDto);
					} else {
						clusterNo.clear();
						clusterNo.setVisible(false);
					}
				}
			} else if (!isDistrictMulti && isObserver) {
				if (e.getValue() != null) {
					clusterNo.clear();
					clusterNo.setVisible(false);
				}
			} else {
				district.clear();
				district.setVisible(false);
				clusterNo.clear();
				clusterNo.setVisible(false);
			}
		});


		commusr.addValueChangeListener(e -> {
			if ((boolean) e.getValue() == true) {
				userTypes.setValue(UserType.COMMON_USER);

				// Create a temporary copy so we don't permanently destroy the admin's role list
				Set<UserRole> filteredCommonRoles = new LinkedHashSet<>(sortedUserRoles);
				filteredCommonRoles.remove(UserRole.ADMIN);
				filteredCommonRoles.remove(UserRole.COMMUNITY_INFORMANT);
				filteredCommonRoles.remove(UserRole.AREA_ADMIN_SUPERVISOR);
				filteredCommonRoles.remove(UserRole.ADMIN_SUPERVISOR);
				filteredCommonRoles.remove(UserRole.BAG_USER);
				filteredCommonRoles.remove(UserRole.PUBLISH_USER);
				filteredCommonRoles.remove(UserRole.EDITOR_USER);

				userRoles.setItems(filteredCommonRoles);
			}

			if ((boolean) e.getValue() == false) {
				// Restore the original sorted list cleanly
				userRoles.setItems(sortedUserRoles);
			}
		});

		formAccess.setLabel(I18nProperties.getCaption(Captions.formAccess));
		formAccess.setItemLabelGenerator(FormAccess::getDisplayName);

		this.setColspan(activeCheck, 2);
		activeCheck.setLabel(I18nProperties.getCaption(Captions.User_active));
		activeCheck.setValue(active);
		binder.forField(activeCheck).bind(UserDto::isActive, UserDto::setActive);

		formAccessesList.add(FormAccess.ARCHIVE);
		
		formAccessesList.add(FormAccess.DISTRICT_READINESS);
		formAccessesList.add(FormAccess.TF_REPORT);
		formAccessesList.add(FormAccess.EVENING_REVIEW);
		formAccessesList.add(FormAccess.MICROPLANNING);

		
		formAccessesList.add(FormAccess.FLW);
		formAccessesList.add(FormAccess.TRAINING);
		formAccessesList.add(FormAccess.MONITORING);
		formAccessesList.add(FormAccess.ICM);
		formAccessesList.add(FormAccess.ADMIN);
		formAccessesList.add(FormAccess.EAG_ICM);
		formAccessesList.add(FormAccess.EAG_ADMIN);
		formAccessesList.add(FormAccess.PCA);
		formAccessesList.add(FormAccess.FMS);
		formAccessesList.add(FormAccess.LQAS);
		formAccessesList.add(FormAccess.EAG_PCA);
		formAccessesList.add(FormAccess.EAG_FMS);
		formAccessesList.add(FormAccess.EAG_LQAS);
		formAccessesList.add(FormAccess.VALIDATION);

		formAccess.setItems(formAccessesList);

		Div formAccessCheckers = new Div();
		formAccessCheckers.add(formAccess);

		language.setWidthFull();
		region.setWidthFull();
		province.setWidthFull();
		district.setWidthFull();
		
//		applyGeographicScope(currentJurisdiction);

		VerticalLayout otherFormField = new VerticalLayout(language, region, province, district, districtMulti);
		formAccess.setId("formaccesschkid");
		VerticalLayout formAccessParentLayout = new VerticalLayout(formAccess);
		formAccessParentLayout.setPadding(false);
		formAccessParentLayout.setSpacing(false);
		HorizontalLayout allFieldLayout = new HorizontalLayout(formAccessParentLayout, otherFormField);
		this.setColspan(allFieldLayout, 2);

		// Initial state: all geographic fields are hidden until a role is chosen.
		// Role-driven visibility is managed by updateFieldsByUserRole().
		region.setVisible(false);
		province.setVisible(false);
		district.setVisible(false);
		districtMulti.setVisible(false);
		clusterNo.setVisible(false);

		add(pInfo, firstName, lastName, userEmail, phone, userPosition, userOrganisation, userData, userName,
				activeCheck, commusr, userRoles, allFieldLayout, clusterNo);

		createButtonsLayout();
		
		String checkBoxDivider = "var parentDiv = document.getElementById(\"formaccesschkid\");\n"
				+ "if (parentDiv) {\n" 
				+ "var children = parentDiv.querySelectorAll(\"vaadin-checkbox\");\n"
				+ "for (var i = 0; i < children.length; i++) {\n" 
				+ "console.log(children[i]);\n" 
				+ "if (i === 0) {\n"
				+ "var newContentL = '<hr><b class=\"formaccesschkbox\">Report</b><hr>';\n"
				+ "children[i].insertAdjacentHTML('afterend', newContentL);\n" 
				+ "}\n"
				
				+ "if (i === 4) {\n"
				+ "var newContentL = '<hr><b class=\"formaccesschkbox\">Pre-Campaign</b><hr>';\n"
				+ "children[i].insertAdjacentHTML('afterend', newContentL);\n" 
				+ "}\n"
				

				+ "if (i === 7) {\n" 
				+ "var newContentL = '<hr><b class=\"formaccesschkbox\">Intra-Campaign</b><hr>';\n"
				+ "children[i].insertAdjacentHTML('afterend', newContentL);\n" 
				+ "}\n"

				+ "if (i === 11) {\n" 
				+ "var newContentL = '<hr><b class=\"formaccesschkbox\">Post-Campaign</b><hr>';\n"
				+ "children[i].insertAdjacentHTML('afterend', newContentL);\n" 
				+ "}\n"

				+ "}}";
		
		

		UI.getCurrent().getPage().executeJs(checkBoxDivider);

	}

	public void updatePasswordDialog() {
		_dialog.setHeader("Update Password");
		_dialog.setCloseOnEsc(false);
		_dialog.setCancelable(true);
		_dialog.addCancelListener(e -> _dialog.close());

		_dialog.setRejectable(true);
		_dialog.setRejectText("Cancel");
		_dialog.addRejectListener(e -> _dialog.close());

		_dialog.setConfirmText("Really Update Password");
		_dialog.addConfirmListener(e -> makeNewPassword(binder.getBean().getUuid(), binder.getBean().getUserEmail(),
				binder.getBean().getUserName()));
	}

	public void makeNewPassword(String userUuid, String userEmail, String userName) {
		String newPassword = FacadeProvider.getUserFacade().resetPassword(userUuid);

		Dialog newUserPop = new Dialog();
		newUserPop.setClassName("passwordsDialog");
		VerticalLayout infoLayout = new VerticalLayout();

		Paragraph infoText = new Paragraph("Please , copy this password, it is shown only once.");
		newUserPop.setHeaderTitle("Password Updated");

		H3 username = new H3(I18nProperties.getCaption(Captions.Login_username) + " : " + userName);
		username.getStyle().set("color", "#0D6938");

		H3 password = new H3(I18nProperties.getCaption(Captions.Login_password) + " : " + newPassword);
		password.getStyle().set("color", "#0D6938");

		infoLayout.add(username, password);
		newUserPop.add(infoLayout);
		newUserPop.setOpened(true);
	}

	public void suggestUserName(boolean editMode) {
		if (editMode) {
			lastName.addValueChangeListener(e -> {
				if (userName.isEmpty()) {
					userName.setValue(UserHelper.getSuggestedUsername(firstName.getValue(), lastName.getValue()));
				}
			});
		}
	}

	private void createButtonsLayout() {
		createPassword.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
		close.addThemeVariants(ButtonVariant.LUMO_ERROR);
		close.addClickShortcut(Key.ESCAPE);

		delete.addClickListener(event -> fireEvent(new DeleteEvent(this, binder.getBean())));
		close.setEnabled(true);
		close.addClickListener(event -> {
			VaadinSession.getCurrent().setAttribute("contact-form", null);
			fireEvent(new CloseEvent(this));
		});

		createPassword.addClickListener(event -> {
			_dialog.open();
		});

		HorizontalLayout horizontallayout = new HorizontalLayout();

		createPassword.getStyle().set("margin-right", "auto");
		save.getStyle().set("margin-left", "auto");

		horizontallayout.add(createPassword, save, close);
		add(horizontallayout);
		this.setColspan(horizontallayout, 2);
	}

	public boolean checkUnitAssignmentByJurisdictionLevel() {
		if (userRoles.getValue() == null || userRoles.getValue().isEmpty()) {
			return true;
		}
		for (UserRole role : userRoles.getValue()) {
			if (role.getJurisdictionLevel() == JurisdictionLevel.COMMUNITY) {
				if (clusterNo.getSelectedItems().size() == 0) {
					clusterNo.setInvalid(true);
					clusterNo.setErrorMessage("One or More Clusters Must be selected for Cluster Level Users.");
					return false;
				}
			} else if (role.getJurisdictionLevel() == JurisdictionLevel.DISTRICT) {
				if (role == UserRole.SURVEILLANCE_OFFICER) {
					if (districtMulti.getSelectedItems().size() == 0) {
						districtMulti.setInvalid(true);
						districtMulti.setErrorMessage("One or More Districts Must be selected for District Officers.");
						return false;
					}
				} else {
					if (district.getValue() == null) {
						district.setInvalid(true);
						district.setErrorMessage("One or More Districts Must be selected for District Level Users.");
						district.focus();
						return false;
					}
				}
			} else if (role.getJurisdictionLevel() == JurisdictionLevel.REGION) {
				if (province.getValue() == null) {
					province.setInvalid(true);
					province.setErrorMessage("One or More Province(s) Must be selected for Provincial Level Users.");
					province.focus();
					return false;
				}
			} else if (role.getJurisdictionLevel() == JurisdictionLevel.AREA) {
				if (region.getValue() == null) {
					region.setInvalid(true);
					region.setErrorMessage("One or More Region(s) Must be selected for Regional Level Users.");
					region.focus();
					return false;
				}
			}
		}
		return true;
	}

	public void validateAndSaveEdit(UserDto originalUser, String preceedingUsername) {
		List<FormAccess> formAccesses = new ArrayList<>(binder.getBean().getFormAccess());

		if (formAccesses.size() == 0 || formAccesses.size() < 1) {
			Notification notification = new Notification();
			notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
			notification.setPosition(Position.MIDDLE);
			Button closeButton = new Button(new Icon("lumo", "cross"));
			closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
			closeButton.getElement().setAttribute("aria-label", "Close");
			closeButton.addClickListener(eventx -> {
				notification.close();
			});
			Paragraph text = new Paragraph("Error : Form Access is Required, Please Fill Out a FormAccess to proceed.");
			HorizontalLayout layout = new HorizontalLayout(text, closeButton);
			layout.setAlignItems(Alignment.CENTER);
			notification.add(layout);
			notification.open();

		} else {
			if (!checkUnitAssignmentByJurisdictionLevel()) {
				return;
			}

			if (binder.validate().isOk()) {
				boolean isErrored = false;

				if (binder.getBean().getUserEmail() != null || binder.getBean().getUserEmail() != "") {
					UserDto anyEmailFromDb = FacadeProvider.getUserFacade().getByEmail(binder.getBean().getUserEmail());
					if (anyEmailFromDb == null) {
						isErrored = false;
					} else {
						if (anyEmailFromDb.getUserName().trim().equals(originalUser.getUserName().trim())
								&& !originalUser.getUserName().isEmpty()) {
							isErrored = false;
						} else {
							isErrored = true;
							Notification notification = new Notification();
							notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
							notification.setPosition(Position.MIDDLE);
							Button closeButton = new Button(new Icon("lumo", "cross"));
							closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
							closeButton.getElement().setAttribute("aria-label", "Close");
							closeButton.addClickListener(event -> notification.close());
							Paragraph text = new Paragraph("Error : Email already in the system.");
							HorizontalLayout layout = new HorizontalLayout(text, closeButton);
							layout.setAlignItems(Alignment.CENTER);
							notification.add(layout);
							notification.open();
							return;
						}
					}
				}

				if (binder.getBean().getUserName().contains(" ")) {
					Notification notification = new Notification();
					notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
					notification.setPosition(Position.MIDDLE);
					Button closeButton = new Button(new Icon("lumo", "cross"));
					closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
					closeButton.getElement().setAttribute("aria-label", "Close");
					closeButton.addClickListener(event -> notification.close());
					Paragraph text = new Paragraph("Error : Username cannot contain white space");
					HorizontalLayout layout = new HorizontalLayout(text, closeButton);
					layout.setAlignItems(Alignment.CENTER);
					notification.add(layout);
					notification.open();
					isErrored = true;
					return;
				} else {
					if (binder.getBean().getUserName() != null || userName != null) {
						UserDto retrieveBinderUserFromDb = FacadeProvider.getUserFacade()
								.getByUserName(binder.getBean().getUserName());
						String originalUserx = originalUser == null ? binder.getBean().getUserName()
								: originalUser.getUserName().trim();
						if (retrieveBinderUserFromDb == null) {
							if (binder.getBean().getUserName().contains(" ")) {
								// Handled above
							} else if (!preceedingUsername.equals(originalUserx)) {
								fireEvent(new SaveEvent(this, binder.getBean()));
							} else {
								isErrored = true;
								return;
							}
						} else if (retrieveBinderUserFromDb.getUserName().trim().equalsIgnoreCase(originalUserx)
								&& !originalUserx.isEmpty() && !isErrored) {
							if (preceedingUsername.equals(originalUserx)) {
								fireEvent(new SaveEvent(this, binder.getBean()));
							} else {
								UserDto checkNewusernamefromDB = FacadeProvider.getUserFacade()
										.getByUserName(binder.getBean().getUserName());
								if (checkNewusernamefromDB == null) {
									fireEvent(new SaveEvent(this, binder.getBean()));
								} else {
									Notification notification = new Notification();
									notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
									notification.setPosition(Position.MIDDLE);
									Button closeButton = new Button(new Icon("lumo", "cross"));
									closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
									closeButton.getElement().setAttribute("aria-label", "Close");
									closeButton.addClickListener(event -> notification.close());
									Paragraph text = new Paragraph("Error : Username not unique");
									HorizontalLayout layout = new HorizontalLayout(text, closeButton);
									layout.setAlignItems(Alignment.CENTER);
									notification.add(layout);
									notification.open();
									return;
								}
							}
						} else {
							if (FacadeProvider.getUserFacade().getByUserName(binder.getBean().getUserName()) != null) {
								Notification notification = new Notification();
								notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
								notification.setPosition(Position.MIDDLE);
								Button closeButton = new Button(new Icon("lumo", "cross"));
								closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
								closeButton.getElement().setAttribute("aria-label", "Close");
								closeButton.addClickListener(event -> notification.close());
								Paragraph text = new Paragraph("Error : Username not unique");
								HorizontalLayout layout = new HorizontalLayout(text, closeButton);
								layout.setAlignItems(Alignment.CENTER);
								notification.add(layout);
								notification.open();
								return;
							}
						}
					}
				}
			} else {
				StringBuilder errorMessages = new StringBuilder();
				binder.validate().getFieldValidationErrors().forEach(error -> {
					if (errorMessages.length() > 0) {
						errorMessages.append("\n");
					}
					errorMessages.append("• ").append(error.getMessage().orElse("Validation error"));
				});

				if (errorMessages.length() == 0) {
					errorMessages.append("Please correct the validation errors and try again.");
				}

				Notification notification = new Notification();
				notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
				notification.setPosition(Position.MIDDLE);
				Button closeButton = new Button(new Icon("lumo", "cross"));
				closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
				closeButton.getElement().setAttribute("aria-label", "Close");
				closeButton.addClickListener(event -> notification.close());
				Paragraph text = new Paragraph("Validation Error(s): " + errorMessages.toString());
				HorizontalLayout layout = new HorizontalLayout(text, closeButton);
				layout.setAlignItems(Alignment.CENTER);
				notification.add(layout);
				notification.open();
				return;
			}
		}
	}

	public void validateAndSaveNew() {
		boolean isErrored = false;
		List<FormAccess> formAccesses = new ArrayList<>(binder.getBean().getFormAccess());

		if (binder.validate().isOk()) {
			if (formAccesses.size() == 0 || formAccesses.size() < 1) {
				Notification notification = new Notification();
				notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
				notification.setPosition(Position.MIDDLE);
				Button closeButton = new Button(new Icon("lumo", "cross"));
				closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
				closeButton.getElement().setAttribute("aria-label", "Close");
				closeButton.addClickListener(eventx -> notification.close());
				Paragraph text = new Paragraph(
						"Error : Form Access is Required, Please Fill Out a FormAccess to proceed.");
				HorizontalLayout layout = new HorizontalLayout(text, closeButton);
				layout.setAlignItems(Alignment.CENTER);
				notification.add(layout);
				notification.open();

			} else {
				if (!checkUnitAssignmentByJurisdictionLevel()) {
					return;
				}

				if (binder.validate().isOk()) {
					if (binder.getBean().getUserEmail() != null) {
						UserDto binderEmailValidation = FacadeProvider.getUserFacade()
								.getByEmail(binder.getBean().getUserEmail());

						if (binderEmailValidation != null) {
							if (!binderEmailValidation.getUserName().trim()
									.equals(binder.getBean().getUserName().trim())) {
								Notification notification = new Notification();
								notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
								notification.setPosition(Position.MIDDLE);
								Button closeButton = new Button(new Icon("lumo", "cross"));
								closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
								closeButton.getElement().setAttribute("aria-label", "Close");
								closeButton.addClickListener(event -> notification.close());
								Paragraph text = new Paragraph("Error : Email already in the system...");
								HorizontalLayout layout = new HorizontalLayout(text, closeButton);
								layout.setAlignItems(Alignment.CENTER);
								notification.add(layout);
								notification.open();
								isErrored = true;
								return;
							}
						}
					}

					if (FacadeProvider.getUserFacade().getByUserName(binder.getBean().getUserName()) != null) {
						Notification notification = new Notification();
						notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
						notification.setPosition(Position.MIDDLE);
						Button closeButton = new Button(new Icon("lumo", "cross"));
						closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
						closeButton.getElement().setAttribute("aria-label", "Close");
						closeButton.addClickListener(event -> notification.close());
						Paragraph text = new Paragraph("Error : Username not unique");
						HorizontalLayout layout = new HorizontalLayout(text, closeButton);
						layout.setAlignItems(Alignment.CENTER);
						notification.add(layout);
						notification.open();
						isErrored = true;
						return;
					} else if (binder.getBean().getUserName().contains(" ")) {
						Notification notification = new Notification();
						notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
						notification.setPosition(Position.MIDDLE);
						Button closeButton = new Button(new Icon("lumo", "cross"));
						closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
						closeButton.getElement().setAttribute("aria-label", "Close");
						closeButton.addClickListener(event -> notification.close());
						Paragraph text = new Paragraph("Error : Username cannot contain white space");
						HorizontalLayout layout = new HorizontalLayout(text, closeButton);
						layout.setAlignItems(Alignment.CENTER);
						notification.add(layout);
						notification.open();
						isErrored = true;
						return;
					} else {
						if (!isErrored) {
							fireEvent(new SaveEvent(this, binder.getBean()));

							UserActivitySummaryDto userActivitySummaryDto = new UserActivitySummaryDto();
							userActivitySummaryDto.setActionModule("Users");
							userActivitySummaryDto.setAction("Created User: " + binder.getBean().getUserName());
							userActivitySummaryDto.setCreatingUser_string(userProvider.getUser().getUserName());
							FacadeProvider.getUserFacade().saveUserActivitySummary(userActivitySummaryDto);
						}
					}
				}
			}
		} else {
			StringBuilder errorMessages = new StringBuilder();
			binder.validate().getFieldValidationErrors().forEach(error -> {
				if (errorMessages.length() > 0) {
					errorMessages.append("\n");
				}
				errorMessages.append("• ").append(error.getMessage().orElse("Validation error"));
			});

			if (errorMessages.length() == 0) {
				errorMessages.append("Please correct the validation errors and try again.");
			}

			Notification notification = new Notification();
			notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
			notification.setPosition(Position.MIDDLE);
			Button closeButton = new Button(new Icon("lumo", "cross"));
			closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
			closeButton.getElement().setAttribute("aria-label", "Close");
			closeButton.addClickListener(event -> notification.close());
			Paragraph text = new Paragraph("Validation Error(s): " + errorMessages.toString());
			HorizontalLayout layout = new HorizontalLayout(text, closeButton);
			layout.setAlignItems(Alignment.CENTER);
			notification.add(layout);
			notification.open();
			return;
		}
	}

//	class UserRoleCustomComparator implements Comparator<UserRole> {
//		private final String[] customOrder = { "Admin", "National Data Manager", "National Officer",
//				"National Observer / Partner", "Regional Observer", "Regional Data Manager", "Regional Officer",
//				"Provincial Observer", "Provincial Data Clerk", "Provincial Officer", "District Officer",
//				"District Observer" };
//
//		@Override
//		public int compare(UserRole role1, UserRole role2) {
//			int index1 = indexOfRole(role1);
//			int index2 = indexOfRole(role2);
//			return Integer.compare(index1, index2);
//		}
//
//		private int indexOfRole(UserRole role) {
//			for (int i = 0; i < customOrder.length; i++) {
//				if (customOrder[i].equals(role.name())) {
//					return i;
//				}
//			}
//			return customOrder.length;
//		}
//	}

	class UserRoleCustomComparator implements Comparator<UserRole> {
		private final String[] customOrder = { "Admin", "National Data Manager", "National Officer",
				"National Observer / Partner", "Regional Observer", "Regional Data Manager", "Regional Officer",
				"Provincial Observer", "Provincial Data Clerk", "Provincial Officer", "District Officer",
				"District Observer" };

		@Override
		public int compare(UserRole role1, UserRole role2) {
			int index1 = indexOfRole(role1);
			int index2 = indexOfRole(role2);

			// If neither role is explicitly listed in the customOrder array, sort them
			// alphabetically
			if (index1 == customOrder.length && index2 == customOrder.length) {
				return role1.toString().compareToIgnoreCase(role2.toString());
			}
			return Integer.compare(index1, index2);
		}

		private int indexOfRole(UserRole role) {
			String caption = role.toString(); // Use the localized UI string to match customOrder
			for (int i = 0; i < customOrder.length; i++) {
				if (customOrder[i].equalsIgnoreCase(caption)) {
					return i;
				}
			}
			return customOrder.length;
		}
	}

	protected void validateUserRoles() {
		map.forEach((key, value) -> {
			Component formField = value;
			if (formField instanceof MultiSelectComboBox) {
				MultiSelectComboBox<UserRole> formFieldxx = (MultiSelectComboBox<UserRole>) formField;
				UserRolesValidator userRolesValidator = new UserRolesValidator();
				ValidationResult validation = userRolesValidator.apply(formFieldxx.getValue(),
						new ValueContext(formFieldxx));

				if (validation.isError()) {
					formFieldxx.setInvalid(true);
					formFieldxx.setErrorMessage(validation.getErrorMessage());
					formFieldxx.setTooltipText(validation.getErrorMessage());
				}
			}
		});
	}

	private void resetpassword() {
		fireEvent(new ResetPasswordEvent(this, binder.getBean()));
	}

	public void setUser(UserDto user) {
		this.user = user;
		binder.setBean(user);
	    updateFieldsByUserRole(user.getUserRoles());

//	    JurisdictionLevel currentJurisdiction = UserRole.getJurisdictionLevel(userProvider.getUser().getUserRoles());
//	    applyGeographicScope(currentJurisdiction);
	    
	}

	public static abstract class UserFormEvent extends ComponentEvent<UserForm> {
		private UserDto user;

		protected UserFormEvent(UserForm source, UserDto user) {
			super(source, false);
			this.user = user;
		}

		public UserDto getContact() {
			return user;
		}
	}

	public static class SaveEvent extends UserFormEvent {
		SaveEvent(UserForm source, UserDto user) {
			super(source, user);
		}
	}

	public static class UserFieldValueChangeEvent extends UserFormEvent {
		UserFieldValueChangeEvent(UserForm source, UserDto user) {
			super(source, user);
		}
	}

	public static class DeleteEvent extends UserFormEvent {
		DeleteEvent(UserForm source, UserDto user) {
			super(source, user);
		}
	}

	public static class CloseEvent extends UserFormEvent {
		CloseEvent(UserForm source) {
			super(source, new UserDto());
		}
	}

	public static class ResetPasswordEvent extends UserFormEvent {
		ResetPasswordEvent(UserForm source, UserDto user) {
			super(source, new UserDto());
		}
	}

	public Registration addResetPasswordListener(ComponentEventListener<ResetPasswordEvent> listener) {
		return addListener(ResetPasswordEvent.class, listener);
	}

	public Registration addDeleteListener(ComponentEventListener<DeleteEvent> listener) {
		return addListener(DeleteEvent.class, listener);
	}

	public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
		return addListener(SaveEvent.class, listener);
	}

	public Registration addCloseListener(ComponentEventListener<CloseEvent> listener) {
		return addListener(CloseEvent.class, listener);
	}

	public Registration addUserFieldValueChangeEventListener(
			ComponentEventListener<UserFieldValueChangeEvent> listener) {
		return addListener(UserFieldValueChangeEvent.class, listener);
	}

	
	private void populateClusterOptions(DistrictReferenceDto districtDto) {
		if (districtDto == null) {
			return;
		}

		clusterNo.setVisible(true);
		this.setColspan(clusterNo, 2);
		communities = FacadeProvider.getCommunityFacade().getAllActiveByDistrict(districtDto.getUuid());
		clusterNo.setLabel(I18nProperties.getCaption(Captions.clusterNumber));

		List<CommunityReferenceDto> items = FacadeProvider.getCommunityFacade()
				.getAllActiveByDistrict(districtDto.getUuid());
		for (CommunityReferenceDto item : items) {
			if (item.getNumber() == null) {
				Notification notification = new Notification();
				notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
				notification.setPosition(Position.MIDDLE);
				Button closeButton = new Button(new Icon("lumo", "cross"));
				closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
				closeButton.getElement().setAttribute("aria-label", "Close");
				closeButton.addClickListener(event -> notification.close());

				Paragraph text = new Paragraph("Cluster Number cannot be empty, please contact support");
				HorizontalLayout layout = new HorizontalLayout(text, closeButton);
				layout.setAlignItems(Alignment.CENTER);
				notification.add(layout);
				notification.open();
			}

			item.setCaption(item.getNumber() != null ? item.getNumber().toString() : null);
		}
		Collections.sort(items, CommunityReferenceDto.clusternumber);

		clusterNo.setItems(items);
		clusterNo.getChildren().forEach(checkbox -> {
			checkbox.getElement().getClassList().add("custom-checkbox-class");
		});
	}
	
	protected void updateFieldsByUserRole(Set<UserRole> userRoles) {

	    /* ---------- 1. Closed / empty state: reset everything ---------- */
	    if (userRoles == null || userRoles.isEmpty()) {
	        // Hide all geographic widgets
	        clusterNo.clear();      clusterNo.setVisible(false);
	        district.clear();       district.setVisible(false);
	        districtMulti.clear();  districtMulti.setVisible(false);
	        province.clear();       province.setVisible(false);
	        region.clear();         region.setVisible(false);

	        // Release any scope locks so the next edit starts clean
	        region.setReadOnly(false);
	        province.setReadOnly(false);
	        district.setReadOnly(false);
	        districtMulti.setReadOnly(false);
	        clusterNo.setReadOnly(false);
	        return;
	    }

	    /* ---------- 2. Re-apply the editor's geographic scope ---------- */
	    // The editor's role set can be null during early form construction
	    // (UserView.closeEditor() -> setUser(new UserDto())), so guard it.
	    UserDto editor = userProvider != null ? userProvider.getUser() : null;
	    Set<UserRole> editorRoles = editor != null ? editor.getUserRoles() : null;

	    if (editorRoles != null && !editorRoles.isEmpty()) {
	        applyGeographicScope(UserRole.getJurisdictionLevel(editorRoles));
	    } else {
	        // No editor scope available -> make sure nothing stays locked
	        region.setReadOnly(false);
	        province.setReadOnly(false);
	        district.setReadOnly(false);
	        districtMulti.setReadOnly(false);
	        clusterNo.setReadOnly(false);
	    }

	    /* ---------- 3. Original visibility logic ---------- */
	    final JurisdictionLevel jurisdictionLevel = UserRole.getJurisdictionLevel(userRoles);

	    boolean showRegion = false;
	    boolean showProvince = false;
	    boolean showDistrict = false;
	    boolean showDistrictMulti = false;
	    boolean showCluster = false;

	    switch (jurisdictionLevel) {
	        case COMMUNITY:
	            showRegion = true; showProvince = true;
	            showDistrict = true; showCluster = true;
	            break;
	        case DISTRICT:
	            showRegion = true; showProvince = true;
	            if (userRoles.contains(UserRole.SURVEILLANCE_OFFICER)) {
	                showDistrictMulti = true;
	            } else {
	                showDistrict = true;
	            }
	            break;
	        case REGION:
	            showRegion = true; showProvince = true;
	            break;
	        case AREA:
	        case NATION:
	            showRegion = true;
	            break;
	        default:
	            break;
	    }

	    region.setVisible(showRegion);           if (!showRegion)        region.clear();
	    province.setVisible(showProvince);       if (!showProvince)      province.clear();
	    district.setVisible(showDistrict);       if (!showDistrict)      district.clear();
	    districtMulti.setVisible(showDistrictMulti); if (!showDistrictMulti) districtMulti.clear();
	    clusterNo.setVisible(showCluster);       if (!showCluster)       clusterNo.clear();

	    if (province.getValue() != null) {
	        List<DistrictReferenceDto> districtOptions = FacadeProvider.getDistrictFacade()
	                .getAllActiveByRegion(province.getValue().getUuid());
	        if (showDistrict)      district.setItems(districtOptions);
	        if (showDistrictMulti) districtMulti.setItems(districtOptions);
	    }

	    if (showCluster && district.getValue() != null) {
	        populateClusterOptions(district.getValue());
	    }
	}
	

//	protected void updateFieldsByUserRole(Set<UserRole> userRoles) {
//	    final JurisdictionLevel jurisdictionLevel = UserRole.getJurisdictionLevel(userRoles);
//	    applyGeographicScope(jurisdictionLevel);
//
//	    
//	    
//	    if (userRoles == null || userRoles.isEmpty()) {
//	        // Hide and clear everything if no roles are selected
//	        clusterNo.clear(); clusterNo.setVisible(false);
//	        district.clear(); district.setVisible(false);
//	        districtMulti.clear(); districtMulti.setVisible(false);
//	        province.clear(); province.setVisible(false);
//	        region.clear(); region.setVisible(false);
//	        return;
//	    }
//
//
//	    // 1. Determine visibility based on the role's jurisdiction level
//	    boolean showRegion = false;
//	    boolean showProvince = false;
//	    boolean showDistrict = false;
//	    boolean showDistrictMulti = false;
//	    boolean showCluster = false;
//
//	    switch (jurisdictionLevel) {
//	    case COMMUNITY:
//	        showRegion = true;
//	        showProvince = true;
//	        showDistrict = true;
//	        showCluster = true;
//	        break;
//	    case DISTRICT:
//	        showRegion = true;
//	        showProvince = true;
//	        if (userRoles.contains(UserRole.SURVEILLANCE_OFFICER)) {
//	            showDistrictMulti = true;
//	        } else {
//	            showDistrict = true;
//	        }
//	        break;
//	    case REGION:
//	        showRegion = true;
//	        showProvince = true;
//	        break;
//	    case AREA:
//	    case NATION:
//	        showRegion = true;
//	        break;
//	    default:
//	        // NONE, HEALTH_FACILITY, LABORATORY, POINT_OF_ENTRY, ... -> no geographic assignment
//	        break;
//	    }
//
//	    // 2. Apply visibility and clear hidden fields to prevent saving hidden data
//	    region.setVisible(showRegion);
//	    if (!showRegion) region.clear();
//
//	    province.setVisible(showProvince);
//	    if (!showProvince) province.clear();
//
//	    district.setVisible(showDistrict);
//	    if (!showDistrict) district.clear();
//
//	    districtMulti.setVisible(showDistrictMulti);
//	    if (!showDistrictMulti) districtMulti.clear();
//
//	    clusterNo.setVisible(showCluster);
//	    if (!showCluster) clusterNo.clear();
//
//	    // 3. Re-populate dropdowns if a Province is already selected but the role changed
//	    if (province.getValue() != null) {
//	        List<DistrictReferenceDto> districtOptions = FacadeProvider.getDistrictFacade()
//	                .getAllActiveByRegion(province.getValue().getUuid());
//	        if (showDistrict) {
//	            district.setItems(districtOptions);
//	        }
//	        if (showDistrictMulti) {
//	            districtMulti.setItems(districtOptions);
//	        }
//	    }
//
//	    // 4. Re-populate cluster options if a district is already selected but the role changed
//	    if (showCluster && district.getValue() != null) {
//	        populateClusterOptions(district.getValue());
//	    }
//	}
	
//	protected void updateFieldsByUserRole(Set<UserRole> userRoles) {
//		final JurisdictionLevel jurisdictionLevel = UserRole.getJurisdictionLevel(userRoles);
//		final boolean useCommunity = jurisdictionLevel == JurisdictionLevel.COMMUNITY;
//		final boolean useDistrictOnly = jurisdictionLevel == JurisdictionLevel.DISTRICT
//				&& userRoles.contains(UserRole.SURVEILLANCE_OFFICER);
//		final boolean useDistrict = jurisdictionLevel == JurisdictionLevel.DISTRICT
//				&& !userRoles.contains(UserRole.SURVEILLANCE_OFFICER);
//		final boolean useRegion = jurisdictionLevel == JurisdictionLevel.REGION || useDistrict;
//		final boolean useArea = jurisdictionLevel == JurisdictionLevel.AREA || useRegion;
//
//		if (useCommunity) {
//			clusterNo.setVisible(true);
//			district.setVisible(true);
//			districtMulti.clear();
//			districtMulti.setVisible(false);
//			province.setVisible(true);
//			region.setVisible(true);
//		} else if (useDistrictOnly) {
//			clusterNo.clear();
//			clusterNo.setVisible(false);
//			district.setVisible(false);
//			districtMulti.setVisible(true);
//			province.setVisible(true);
//			region.setVisible(true);
//		} else if (useDistrict) {
//			clusterNo.clear();
//			clusterNo.setVisible(false);
//			district.clear();
//			district.setVisible(false);
//			districtMulti.setVisible(false);
//			province.setVisible(true);
//			region.setVisible(true);
//		} else if (useRegion) {
//			clusterNo.clear();
//			clusterNo.setVisible(false);
//			district.clear();
//			district.setVisible(false);
//			districtMulti.clear();
//			districtMulti.setVisible(false);
//			province.setVisible(true);
//			region.setVisible(true);
//		} else if (useArea) {
//			clusterNo.clear();
//			clusterNo.setVisible(false);
//			district.clear();
//			district.setVisible(false);
//			districtMulti.clear();
//			districtMulti.setVisible(false);
//			province.clear();
//			province.setVisible(false);
//			region.setVisible(true);
//		} else {
//			clusterNo.clear();
//			clusterNo.setVisible(false);
//			district.clear();
//			district.setVisible(false);
//			districtMulti.clear();
//			districtMulti.setVisible(false);
//			province.clear();
//			province.setVisible(false);
//			region.clear();
//			region.setVisible(false);
//		}
//	}

	public static void updateItems(CheckboxGroup select, Set<?> items) {
		Set<?> value = select.getSelectedItems();
		boolean readOnly = select.isReadOnly();
		select.setReadOnly(false);
		select.removeAll();
		if (items != null) {
			select.setItems(items);
		}
		select.setValue(value);
		select.setReadOnly(readOnly);
	}

	public static Set<UserRole> getAssignableRoles(Set<UserRole> assignedUserRoles) {
		final Set<UserRole> assignedRoles = assignedUserRoles == null ? Collections.emptySet() : assignedUserRoles;

		Set<UserRole> allRoles = UserRole.getAssignableRoles(UserProvider.getCurrent().getUserRoles());

		if (!FacadeProvider.getConfigFacade().isConfiguredCountry(CountryHelper.COUNTRY_CODE_SWITZERLAND)) {
			allRoles.remove(UserRole.BAG_USER);
		}

		Set<UserRole> enabledUserRoles = FacadeProvider.getUserRoleConfigFacade().getEnabledUserRoles();

		allRoles.removeIf(userRole -> !enabledUserRoles.contains(userRole) && !assignedRoles.contains(userRole));

		return allRoles;
	}
	
	/**
	 * Reusable utility to enforce downward hierarchical control on User Roles.
	 */
	public static Set<UserRole> getHierarchicalManageableRoles(Set<UserRole> loggedInUserRoles,
			Set<UserRole> targetUserRoles, Set<UserRole> availableRoles) {
		Set<UserRole> assignableRoles = new HashSet<>(availableRoles);

		if (loggedInUserRoles == null || loggedInUserRoles.isEmpty()) {
			return assignableRoles;
		}

		// Bypass hierarchy restrictions if the user is a system Admin
		if (loggedInUserRoles.contains(UserRole.ADMIN)) {
			assignableRoles.add(UserRole.ADMIN);
			return assignableRoles;
		}

		// Calculate the highest jurisdiction rank among the logged-in user's roles
		int maxRank = -1;
		for (UserRole role : loggedInUserRoles) {
			int rank = getRoleRank(role);
			if (rank > maxRank) {
				maxRank = rank;
			}
		}

		final int userRank = maxRank;

		assignableRoles.removeIf(targetRole -> {
			// ALWAYS preserve the edited user's existing roles so Vaadin binding doesn't
			// crash the form
			if (targetUserRoles != null && targetUserRoles.contains(targetRole)) {
				return false;
			}

			if (targetRole == UserRole.ADMIN) {
				return true; // Non-Admins cannot assign the Admin role
			}
			
			if (targetRole == UserRole.PUBLISH_USER || targetRole == UserRole.EDITOR_USER) {
	            return true;
	        }

			int targetRank = getRoleRank(targetRole);

			// Retain only if the target role's rank is strictly lower (<) than the logged
			// in user's rank
			return targetRank >= userRank;
		});

		return assignableRoles;
	}
	
	/**
	 * Maps Roles to a comparable integer rank using explicit UserRole Enum
	 * matching.
	 */
	private static int getRoleRank(UserRole role) {
		if (role == null)
			return 0;

		switch (role) {
		case ADMIN:
			return 60;

		// --- NATIONAL LEVEL (50) ---
		case COMMUNITY_INFORMANT: // Repurposed as National Data Manager
		case NATIONAL_USER:
		case NATIONAL_OBSERVER:
		case NATIONAL_CLINICIAN:
		case POE_NATIONAL_USER:
			return 50;

		// --- REGIONAL LEVEL (40) ---
		case AREA_ADMIN_SUPERVISOR: // Repurposed as Regional Data Manager
		case AREA_SURVEILLANCE_SUPERVISOR: // Repurposed as Regional Officer
		case AREA_STATE_OBSERVER: // Repurposed as Regional Observer
			return 40;

		// --- PROVINCIAL LEVEL (30) ---
		case ADMIN_SUPERVISOR: // Repurposed as Provincial Data Clerk
		case SURVEILLANCE_SUPERVISOR: // Repurposed as Provincial Officer
		case STATE_OBSERVER: // Repurposed as Provincial Observer
		case CASE_SUPERVISOR:
		case CONTACT_SUPERVISOR:
		case EVENT_OFFICER:
		case POE_SUPERVISOR:
			return 30;

		// --- DISTRICT LEVEL (20) ---
		case SURVEILLANCE_OFFICER:
		case DISTRICT_OBSERVER:
		case CASE_OFFICER:
		case CONTACT_OFFICER:
			return 20;

		// --- CLUSTER / COMMUNITY LEVEL (10) ---
		case COMMUNITY_OFFICER: // Repurposed as Cluster FLW
			return 10;

		// --- BASELINE / OTHER (0) ---
		case REST_USER:
		case PUBLISH_USER:
		case EDITOR_USER:
		case BAG_USER:
		case LAB_USER:
		case EXTERNAL_LAB_USER:
		case HOSPITAL_INFORMANT:
		case POE_INFORMANT:
			return 0;

		default:
			if (role.getJurisdictionLevel() != null) {
				switch (role.getJurisdictionLevel()) {
				case NATION:
					return 50;
				case AREA:
					return 40;
				case REGION:
					return 30;
				case DISTRICT:
					return 20;
				case COMMUNITY:
					return 10;
				default:
					return 0;
				}
			}
			return 0;
		}
	}
	
	
	private void applyGeographicScope(JurisdictionLevel jurisdiction) {
	    UserDto current = userProvider.getUser();
	    
	    region.setReadOnly(false);
	    province.setReadOnly(false);
	    district.setReadOnly(false);
	    districtMulti.setReadOnly(false);
	    clusterNo.setReadOnly(false);
	    

	    if (jurisdiction == JurisdictionLevel.AREA && current.getArea() != null) {
	        region.setReadOnly(true);
	        if (region.getValue() == null) {
	            region.setValue(current.getArea());
	        }
	    } else if (jurisdiction == JurisdictionLevel.REGION && current.getRegion() != null) {
	        region.setReadOnly(true);
	        province.setReadOnly(true);
	        if (region.getValue() == null) {
	            region.setValue(current.getArea());
	        }
	        if (province.getValue() == null) {
	            province.setValue(current.getRegion());
	        }
	    } else if (jurisdiction == JurisdictionLevel.DISTRICT
	            && (current.getDistrict() != null || (current.getDistricts() != null && !current.getDistricts().isEmpty()))) {
	        region.setReadOnly(true);
	        province.setReadOnly(true);
	        if (region.getValue() == null) {
	            region.setValue(current.getArea());
	        }
	        if (province.getValue() == null) {
	            province.setValue(current.getRegion());
	        }
	        if (current.getDistrict() != null) {
	            district.setReadOnly(true);
	            if (district.getValue() == null) {
	                district.setValue(current.getDistrict());
	            }
	            // Pre-select the creator's district for multi-district (surveillance officer) users as well
	            @SuppressWarnings("unchecked")
	            Set<DistrictReferenceDto> selectedMulti = (Set<DistrictReferenceDto>) districtMulti.getValue();
	            if (selectedMulti == null || selectedMulti.isEmpty()) {
	                districtMulti.setValue(Collections.singleton(current.getDistrict()));
	            }
	        } else {
	            districtMulti.setReadOnly(true);
	            @SuppressWarnings("unchecked")
	            Set<DistrictReferenceDto> selectedMulti = (Set<DistrictReferenceDto>) districtMulti.getValue();
	            if (selectedMulti == null || selectedMulti.isEmpty()) {
	                districtMulti.setValue(current.getDistricts());
	            }
	        }
	    } else if (jurisdiction == JurisdictionLevel.COMMUNITY && current.getCommunity() != null
	            && !current.getCommunity().isEmpty()) {
	        region.setReadOnly(true);
	        province.setReadOnly(true);
	        district.setReadOnly(true);
	        clusterNo.setReadOnly(true);
	        if (region.getValue() == null) {
	            region.setValue(current.getArea());
	        }
	        if (province.getValue() == null) {
	            province.setValue(current.getRegion());
	        }
	        if (district.getValue() == null) {
	            district.setValue(current.getDistrict());
	        }
	        @SuppressWarnings("unchecked")
	        Set<CommunityReferenceDto> selectedClusters = (Set<CommunityReferenceDto>) clusterNo.getValue();
	        if (selectedClusters == null || selectedClusters.isEmpty()) {
	            clusterNo.setValue(current.getCommunity());
	        }
	    }
	    // NATION and other levels: no restriction
	}
	

}