package com.cinoteck.application.views.user;


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import org.slf4j.LoggerFactory;
import com.cinoteck.application.UserProvider;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.server.StreamResource;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.infrastructure.area.AreaReferenceDto;
import de.symeda.sormas.api.infrastructure.community.CommunityReferenceDto;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.user.FormAccess;
import de.symeda.sormas.api.user.JurisdictionLevel;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.user.UserRole;
import de.symeda.sormas.api.user.UserType;

public class UsersBulkEditDialog extends Dialog {

	private final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());

	Binder<UserDto> binder = new BeanValidationBinder<>(UserDto.class, false);
	CheckboxGroup<FormAccess> formAccess = new CheckboxGroup<>();

	Set<FormAccess> formAccessesList = new LinkedHashSet<FormAccess>();

//	VerticalLayout formAccessParentLayout = new VerticalLayout(formAccess);

	MultiSelectComboBox<UserRole> userRoles = new MultiSelectComboBox<>(
			I18nProperties.getCaption(Captions.User_userRoles));
	Set<UserRole> roles = new HashSet<UserRole>();
	ComboBox<AreaReferenceDto> region = new ComboBox<>(I18nProperties.getCaption(Captions.area));
	ComboBox<RegionReferenceDto> province = new ComboBox<>(I18nProperties.getCaption(Captions.region));
	ComboBox<DistrictReferenceDto> district = new ComboBox<>(I18nProperties.getCaption(Captions.district));
	CheckboxGroup<DistrictReferenceDto> districtMulti = new CheckboxGroup<DistrictReferenceDto>();
	CheckboxGroup<CommunityReferenceDto> clusterNo = new CheckboxGroup<>();
	List<AreaReferenceDto> regions;
	List<RegionReferenceDto> provinces;
	List<DistrictReferenceDto> districts;
	List<CommunityReferenceDto> communities;
	List<CommunityReferenceDto> communitiesx;
	Map<String, Component> map = new HashMap<>();
	private final UserProvider userProvider = new UserProvider();
	private boolean isDistrictMulti = false;
	ComboBox<String> editableFormFields = new ComboBox<String>();
	VerticalLayout dialog = new VerticalLayout();
	Set<UserDto> updatedItems = new HashSet<UserDto>();
	List<String> userList = new ArrayList<>();
	Grid<UserDto> reviewGrid = new Grid<UserDto>();
	Button startDataUpdate = new Button(I18nProperties.getCaption(Captions.actionBulkEditUserFormFields));
	public Button donloadErrorReport = new Button(I18nProperties.getCaption(Captions.reviewUpdatedData));

	private boolean callbackRunning = false;
	private Timer timer;
	private int pollCounter = 0;
	private File file_;

	Span anchorSpan = new Span();
	public Anchor downloadErrorReportButton;

	Span anchorSpanCredential = new Span();
	public Anchor downloadCredntialsReportButton;

	private UserDto globalUserDto = new UserDto();

	public UsersBulkEditDialog(Set<UserDto> selectedItems, UserDto userDtox, ConfigurableFilterDataProvider filterDataProvider) {

		this.setHeaderTitle("Bulk User Edit");
		// this.getStyle().set("color" , "#0D6938");

		Hr seperatorr = new Hr();
		seperatorr.getStyle().set("color", " #0D6938");

		UI.getCurrent().addPollListener(event -> {
			if (callbackRunning) {
				UI.getCurrent().access(this::pokeFlow);
			} else {
				stopPullers();
			}
		});
		
		

		H3 step2 = new H3();
		step2.add("Step 1: Select Desired User Edit Field");

		Label lblImportTemplateInfo1 = new Label(
				I18nProperties.getString("You have selected " + selectedItems.size() + " users to be updated."));
		Label lblImportTemplateInfo = new Label(I18nProperties.getString(Strings.selectFormField));

		editableFormFields.setItems("Form Access", "User Role");
		formAccess.setVisible(false);
//		formAccessParentLayout.setWidth("50%");

		userRoles.setVisible(false);
		userRoles.setWidth("50%");
		region.setVisible(false);
		region.setWidth("50%");
		province.setVisible(false);
		province.setWidth("50%");
		district.setVisible(false);
		district.setWidth("50%");
		districtMulti.setVisible(false);
		clusterNo.setVisible(false);

		formAccess.setLabel(I18nProperties.getCaption(Captions.formAccess));

		binder.forField(formAccess).bind(UserDto::getFormAccess, UserDto::setFormAccess);
		binder.forField(userRoles).withValidator(new UserRolesValidator())
				.asRequired(I18nProperties.getCaption(Captions.userRoleRequired))
				.bind(UserDto::getUserRoles, UserDto::setUserRoles);

		binder.forField(region).bind(UserDto::getArea, UserDto::setArea);
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
		    // 1. ALWAYS clear downstream components when Region changes to prevent stale data
		    province.clear();
		    district.clear();
		    districtMulti.clear();
		    clusterNo.clear();

		    // 2. Hide downstream components temporarily
		    province.setVisible(false);
		    district.setVisible(false);
		    districtMulti.setVisible(false);
		    clusterNo.setVisible(false);

		    // 3. If a new Region is selected, populate the Province dropdown
		    if (e.getValue() != null) {
		        provinces = FacadeProvider.getRegionFacade().getAllActiveByArea(e.getValue().getUuid());

		        if (userProvider.getUser().getLanguage().toString().equals("Pashto")) {
		            province.setItems(FacadeProvider.getRegionFacade().getAllActiveByAreaPashto(e.getValue().getUuid()));
		        } else if (userProvider.getUser().getLanguage().toString().equals("Dari")) {
		            province.setItems(FacadeProvider.getRegionFacade().getAllActiveByAreaDari(e.getValue().getUuid()));
		        } else {
		            province.setItems(provinces);
		        }

		        // 4. Re-evaluate Role Visibility (so Province shows immediately if needed)
		        if (userRoles.getValue() != null) {
		            JurisdictionLevel level = UserRole.getJurisdictionLevel(userRoles.getValue());
		            // If the role is not AREA or NATION level, they need to select a Province
		            if (level != JurisdictionLevel.AREA && level != JurisdictionLevel.NATION) {
		                province.setVisible(true);
		            }
		        }
		    }
		});
		

//		region.setItemLabelGenerator(AreaReferenceDto::getCaption);
//		region.addValueChangeListener(e -> {
//
//			if (e.getValue() != null) {
//				districtMulti.setVisible(false);
//				clusterNo.setVisible(false);
//				provinces = FacadeProvider.getRegionFacade().getAllActiveByArea(e.getValue().getUuid());
//
//				if (userProvider.getUser().getLanguage().toString().equals("Pashto")) {
//					province.setItems(
//							FacadeProvider.getRegionFacade().getAllActiveByAreaPashto(e.getValue().getUuid()));
//				} else if (userProvider.getUser().getLanguage().toString().equals("Dari")) {
//					province.setItems(FacadeProvider.getRegionFacade().getAllActiveByAreaDari(e.getValue().getUuid()));
//				} else {
//					province.setItems(provinces);
//				}
//			}
//		});

//		province.setItemLabelGenerator(RegionReferenceDto::getCaption);
//		province.addValueChangeListener(e -> {
//
//			if (e.getValue() != null && userRoles.getValue() != null) {
//
//				final JurisdictionLevel jurisdictionLevel = UserRole.getJurisdictionLevel(userRoles.getValue());
//				System.out.println((jurisdictionLevel == JurisdictionLevel.DISTRICT) + " +++___________111"
//						+ userRoles.getValue());
//				if (jurisdictionLevel == JurisdictionLevel.DISTRICT) {
//
//					districtMulti.setVisible(true);
//					district.setVisible(false);
//					clusterNo.setVisible(false);
//					// this.setColspan(districtMulti, 2);
//					districts = FacadeProvider.getDistrictFacade().getAllActiveByRegion(e.getValue().getUuid());
//
//					districtMulti.setLabel(I18nProperties.getCaption(Captions.district));
//
//					for (DistrictReferenceDto item : districts) {
//						System.out.println((jurisdictionLevel == JurisdictionLevel.DISTRICT) + " +++___________222: "
//								+ item.getCaption());
//
//						if (item.getCaption() == null) {
//
//							Notification notification = new Notification();
//							notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
//							notification.setPosition(Position.MIDDLE);
//							Button closeButton = new Button(new Icon("lumo", "cross"));
//							closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
//							closeButton.getElement().setAttribute("aria-label", "Close");
//							closeButton.addClickListener(event -> {
//								notification.close();
//							});
//
//							Paragraph text = new Paragraph("District cannot be empty, please contact support");
//
//							HorizontalLayout layout = new HorizontalLayout(text, closeButton);
//							layout.setAlignItems(Alignment.CENTER);
//
//							notification.add(layout);
//							notification.open();
//						}
//
//					}
//
//					districtMulti.setItems(districts);
//					district.setItems(districts);
//					isDistrictMulti = true;
//					districtMulti.getChildren().forEach(checkbox -> {
//						// checkbox.getElement().setProperty("id", "checkbox-" + checkbox.getLabel());
//						checkbox.getElement().getClassList().add("custom-checkbox-class");
//
//					});
//					//
//
//				} else {
//
//					districts = FacadeProvider.getDistrictFacade().getAllActiveByRegion(e.getValue().getUuid());
//					System.out.println(" +++___________333333: ");
//					if (userProvider.getUser().getLanguage().toString().equals("Pashto")) {
//						district.setItems(
//								FacadeProvider.getDistrictFacade().getAllActiveByRegionPashto(e.getValue().getUuid()));
//					} else if (userProvider.getUser().getLanguage().toString().equals("Dari")) {
//						district.setItems(
//								FacadeProvider.getDistrictFacade().getAllActiveByRegionDari(e.getValue().getUuid()));
//					} else {
//						district.setItems(districts);
//					}
//				}
//			}
//
//		});
		
		province.setItemLabelGenerator(RegionReferenceDto::getCaption);
		province.addValueChangeListener(e -> {
		    if (e.getValue() != null && userRoles.getValue() != null) {
		        final JurisdictionLevel jurisdictionLevel = UserRole.getJurisdictionLevel(userRoles.getValue());
		        
		        if (jurisdictionLevel == JurisdictionLevel.DISTRICT && userRoles.getValue().contains(UserRole.SURVEILLANCE_OFFICER)) {
		            // Surveillance Officer -> Multi-select District
		            districtMulti.setVisible(true);
		            district.setVisible(false);
		            clusterNo.setVisible(false);
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

		        } else if (jurisdictionLevel == JurisdictionLevel.DISTRICT && !userRoles.getValue().contains(UserRole.SURVEILLANCE_OFFICER)) {
		            // Regular District -> Single-select District
		            districts = FacadeProvider.getDistrictFacade().getAllActiveByRegion(e.getValue().getUuid());
		            if (userProvider.getUser().getLanguage().toString().equals("Pashto")) {
		                district.setItems(FacadeProvider.getDistrictFacade().getAllActiveByRegionPashto(e.getValue().getUuid()));
		            } else if (userProvider.getUser().getLanguage().toString().equals("Dari")) {
		                district.setItems(FacadeProvider.getDistrictFacade().getAllActiveByRegionDari(e.getValue().getUuid()));
		            } else {
		                district.setItems(districts);
		            }
		            isDistrictMulti = false;
		            districtMulti.setVisible(false);
		            district.setVisible(true);
		            clusterNo.setVisible(false);

		        } else if (jurisdictionLevel == JurisdictionLevel.REGION) {
		            // Region -> Populate District but hide it
		            districts = FacadeProvider.getDistrictFacade().getAllActiveByRegion(e.getValue().getUuid());
		            if (userProvider.getUser().getLanguage().toString().equals("Pashto")) {
		                district.setItems(FacadeProvider.getDistrictFacade().getAllActiveByRegionPashto(e.getValue().getUuid()));
		            } else if (userProvider.getUser().getLanguage().toString().equals("Dari")) {
		                district.setItems(FacadeProvider.getDistrictFacade().getAllActiveByRegionDari(e.getValue().getUuid()));
		            } else {
		                district.setItems(districts);
		            }
		            isDistrictMulti = false;
		            districtMulti.setVisible(false);
		            district.setVisible(false);
		            clusterNo.setVisible(false);

		        } else {
		            // Community Level
		            districts = FacadeProvider.getDistrictFacade().getAllActiveByRegion(e.getValue().getUuid());
		            if (userProvider.getUser().getLanguage().toString().equals("Pashto")) {
		                district.setItems(FacadeProvider.getDistrictFacade().getAllActiveByRegionPashto(e.getValue().getUuid()));
		            } else if (userProvider.getUser().getLanguage().toString().equals("Dari")) {
		                district.setItems(FacadeProvider.getDistrictFacade().getAllActiveByRegionDari(e.getValue().getUuid()));
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
		    if (!isDistrictMulti) {
		        DistrictReferenceDto districtDto = (DistrictReferenceDto) e.getValue();
		        if (e.getValue() != null) {
		            clusterNo.setVisible(true);
		            communities = FacadeProvider.getCommunityFacade().getAllActiveByDistrict(e.getValue().getUuid());
		            clusterNo.setLabel(I18nProperties.getCaption(Captions.clusterNumber));

		            if (districtDto != null) {
		                List<CommunityReferenceDto> items = FacadeProvider.getCommunityFacade().getAllActiveByDistrict(districtDto.getUuid());
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
		        } else {
		            clusterNo.clear();
		            clusterNo.setVisible(false);
		        }
		    } else {
		        clusterNo.clear();
		        clusterNo.setVisible(false);
		    }
		});
		
//		district.setItemLabelGenerator(DistrictReferenceDto::getCaption);
//		district.addValueChangeListener(e -> {
//			if (!isDistrictMulti) {
//				DistrictReferenceDto districtDto = (DistrictReferenceDto) e.getValue();
//				System.out.println(districtDto + " vvvvvvvddddddDISTRICT CHANGES!!ssssssssssefasdfa:" + e.getValue());
//
//				if (e.getValue() != null) {
//					clusterNo.setVisible(true);
//					// this.setColspan(clusterNo, 2);
//					communities = FacadeProvider.getCommunityFacade().getAllActiveByDistrict(e.getValue().getUuid());
//
//					clusterNo.setLabel(I18nProperties.getCaption(Captions.clusterNumber));
//
//					if (districtDto != null) {
//
//						List<CommunityReferenceDto> items = FacadeProvider.getCommunityFacade()
//								.getAllActiveByDistrict(districtDto.getUuid());
//						for (CommunityReferenceDto item : items) {
//							if (item.getNumber() == null) {
//
//								Notification notification = new Notification();
//								notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
//								notification.setPosition(Position.MIDDLE);
//								Button closeButton = new Button(new Icon("lumo", "cross"));
//								closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
//								closeButton.getElement().setAttribute("aria-label", "Close");
//								closeButton.addClickListener(event -> {
//									notification.close();
//								});
//
//								Paragraph text = new Paragraph(
//										"Cluster Number cannot be empty, please contact support");
//
//								HorizontalLayout layout = new HorizontalLayout(text, closeButton);
//								layout.setAlignItems(Alignment.CENTER);
//
//								notification.add(layout);
//								notification.open();
//							}
//
//							item.setCaption(item.getNumber() != null ? item.getNumber().toString() : null);
//						}
//						Collections.sort(items, CommunityReferenceDto.clusternumber);
//
//						clusterNo.setItems(items);
//						clusterNo.getChildren().forEach(checkbox -> {
//							checkbox.getElement().getClassList().add("custom-checkbox-class");
//
//						});
//						//
//					}
//				}
//			} else {
//				district.clear();
//				district.setVisible(false);
//				clusterNo.clear();
//				clusterNo.setVisible(false);
//			}
//		});

		binder.forField(province).bind(UserDto::getRegion, UserDto::setRegion);

		binder.forField(district).bind(UserDto::getDistrict, UserDto::setDistrict);

		binder.forField(districtMulti);

		binder.bind(districtMulti, UserDto::getDistricts, UserDto::setDistricts);
		districtMulti.setVisible(true);

		binder.forField(clusterNo);

		binder.bind(clusterNo, UserDto::getCommunity, UserDto::setCommunity);

		roles = FacadeProvider.getUserRoleConfigFacade().getEnabledUserRoles();
		roles.remove(UserRole.BAG_USER);

		List<UserRole> rolesz = new ArrayList<>(roles); // Convert Set to List
		roles.remove(UserRole.BAG_USER);

		// Sorting the user roles usng comprtor
		Collections.sort(rolesz, new UserRoleCustomComparator());

		// then i'm converting back to a set for facade to handle save properly.
		Set<UserRole> sortedUserRoles = new TreeSet<>(rolesz);

		userRoles.setItems(sortedUserRoles);

		userRoles.addValueChangeListener(e -> {
			updateFieldsByUserRole(e.getValue());
			validateUserRoles();
		});

		formAccess.setLabel(I18nProperties.getCaption(Captions.formAccess));
		formAccess.setItemLabelGenerator(FormAccess::getDisplayName);
		if (userProvider.getUser().getUsertype() == UserType.WHO_USER) {
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
			
//			formAccessesList.add(FormAccess.ARCHIVE);
//			formAccessesList.add(FormAccess.FLW);
//			formAccessesList.add(FormAccess.TRAINING);
//			formAccessesList.add(FormAccess.ICM);
//			formAccessesList.add(FormAccess.ADMIN);
//			formAccessesList.add(FormAccess.EAG_ICM);
//			formAccessesList.add(FormAccess.EAG_ADMIN);
//			formAccessesList.add(FormAccess.PCA);
//			formAccessesList.add(FormAccess.FMS);
//			formAccessesList.add(FormAccess.LQAS);
//			formAccessesList.add(FormAccess.EAG_PCA);
//			formAccessesList.add(FormAccess.EAG_FMS);
//			formAccessesList.add(FormAccess.EAG_LQAS);
			formAccess.setItems(formAccessesList);
		} else {
			formAccessesList.remove(FormAccess.FLW);
			formAccessesList.remove(FormAccess.TRAINING);
			formAccessesList.remove(FormAccess.PCA);
			formAccessesList.remove(FormAccess.LQAS);
			formAccessesList.remove(FormAccess.FMS);

			formAccessesList.add(FormAccess.ARCHIVE);
			
			formAccessesList.add(FormAccess.ICM);
			formAccessesList.add(FormAccess.ADMIN);
			formAccessesList.add(FormAccess.EAG_ICM);
			formAccessesList.add(FormAccess.EAG_ADMIN);

			
			formAccess.setItems(formAccessesList);
		}
//		formAccess.setItems(FormAccess.values());
		
		Div formAccessCheckers = new Div();

		formAccessCheckers.add(formAccess);
		formAccess.setId("formaccesschkid");
		
		VerticalLayout formAccessParentLayout = new VerticalLayout(formAccess);//, preCampformAccess, intraCampformAccess,


		editableFormFields.addValueChangeListener(e -> {
			if (e.getValue().equalsIgnoreCase("Form Access")) {
				// refreshPage();
				close();

				open();

				editableFormFields.setValue("Form Access");
				
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
//				String checkBoxDivider =
//						"var parentDiv = document.getElementById(\"formaccesschkid\");\n"
//						+ "if (parentDiv) {\n"
//						+ "var children = parentDiv.querySelectorAll(\"vaadin-checkbox\");\n"
//						+ "for (var i = 0; i < children.length; i++) {\n"
//						+ "console.log(children[i]);\n"
//						+ "if (i === 0 && !children[i].classList.contains(\"formaccesschkbox\")) {\n"
//						+ "var newContentL = '<hr><b class=\"formaccesschkbox\">Pre-Campaign</b><hr>';\n"
//						+ "children[i].insertAdjacentHTML('afterend', newContentL);\n"
//						+ "}\n"
//						
//						+ "if (i === 2 && !children[i].classList.contains(\"formaccesschkbox\")) {\n"
//						+ "var newContentL = '<hr><b class=\"formaccesschkbox\">Intra-Campaign</b><hr>';\n"
//						+ "children[i].insertAdjacentHTML('afterend', newContentL);\n"
//						+ "}\n"
//									
//										
//						+ "if (i === 6 && !children[i].classList.contains(\"formaccesschkbox\")) {\n"
//						+ "var newContentL = '<hr><b class=\"formaccesschkbox\">Post-Campaign</b><hr>';\n"
//						+ "children[i].insertAdjacentHTML('afterend', newContentL);\n"
//						+ "}\n"
//						
//						+"}}";
				
				
				UI.getCurrent().getPage().executeJs(checkBoxDivider);
				
				userRoles.setVisible(false);
				if (region.isVisible()) {
					region.setVisible(false);
				}
				if (province.isVisible()) {
					province.setVisible(false);
				}
				if (district.isVisible()) {
					district.setVisible(false);
				}
				if (districtMulti.isVisible()) {
					districtMulti.setVisible(false);
				}
				if (clusterNo.isVisible()) {
					clusterNo.setVisible(false);
				}
				formAccess.setVisible(true);
				formAccessParentLayout.setVisible(true);

			} else if (e.getValue().equalsIgnoreCase("User Role")) {
				
				String checkBoxDividerRemover =	"var parentDiv = document.getElementById(\"formaccesschkid\");\r\n"
						+ "				if (parentDiv) {\r\n"
						+ "				    var children = parentDiv.querySelectorAll(\".formaccesschkbox\");\r\n"
						+ "				    for (var i = 0; i < children.length; i++) {\r\n"
						+ "				        children[i].parentNode.removeChild(children[i]);\r\n"
						+ "				    }\r\n"
						+ "				}";
				
				UI.getCurrent().getPage().executeJs(checkBoxDividerRemover);
				
				if(formAccess.isVisible()) {
					formAccessParentLayout.setVisible(false);
				}
				
				close();

				open();

				editableFormFields.setValue("User Role");
//				formAccess.setVisible(false);
				
				userRoles.setVisible(true);
			} else {
				close();

				open();
				formAccess.setVisible(false);
				userRoles.setVisible(false);
				if (region.isVisible()) {
					region.setVisible(false);
				}
				if (province.isVisible()) {
					province.setVisible(false);
				}
				if (district.isVisible()) {
					district.setVisible(false);
				}
				if (districtMulti.isVisible()) {
					districtMulti.setVisible(false);
				}
				if (clusterNo.isVisible()) {
					clusterNo.setVisible(false);
				}

			}

			if (donloadErrorReport.isVisible()) {
				donloadErrorReport.setVisible(false);
				dialog.remove(reviewGrid);
			}
		});

		H3 step3 = new H3();
		step3.add("Step 2: Fill form fields");
		Label lblImportCsvFile = new Label(I18nProperties.getString(Strings.updateUserFormField));

		Label sd = new Label("Upload");

		Icon startImportButtonIcon = new Icon(VaadinIcon.UPLOAD);
		startDataUpdate.setIcon(startImportButtonIcon);
		startDataUpdate.setVisible(true);

		UserProvider usr = new UserProvider();
		UserDto userDto = usr.getUser();

		startDataUpdate.addClickListener(ed -> {

			ConfirmDialog confirmUpdateDialog = new ConfirmDialog();
			confirmUpdateDialog.setHeader("Update " + selectedItems.size() + " User Data");
			confirmUpdateDialog.setText("You are about to update the " + editableFormFields.getValue() + "'s of "
					+ selectedItems.size() + " users. Are you sure you want to continue with this action ?");
			confirmUpdateDialog.setCancelable(false);

			confirmUpdateDialog.setRejectable(true);
			confirmUpdateDialog.setRejectText(I18nProperties.getCaption(Captions.actionNo));
			confirmUpdateDialog.setConfirmText(I18nProperties.getCaption(Captions.actionYes));
			confirmUpdateDialog.addRejectListener(e -> confirmUpdateDialog.close());
			confirmUpdateDialog.open();
			confirmUpdateDialog.addConfirmListener(e -> {

				try {
					if (editableFormFields.getValue().equalsIgnoreCase("form access")) {

						for (UserDto user : selectedItems) {

							System.out.println(user.getUserName() + " Username of user being updated");
							userList.add(user.getUuid());

							Set<FormAccess> acceses = new HashSet<>();
							acceses.addAll(formAccess.getValue());

							user.setFormAccess(acceses);
							updatedItems.add(user);

							globalUserDto = user;
							FacadeProvider.getUserFacade().updateFormAccessUsers(userList, acceses);

						}
					} else if (editableFormFields.getValue().equalsIgnoreCase("User Role")) {

						for (UserDto user : selectedItems) {
							userList.add(user.getUuid());
							user.setUserRoles(userRoles.getValue());
							user.setArea(region.getValue());
							user.setRegion(province.getValue());
							user.setDistrict(district.getValue());
							user.setDistricts(districtMulti.getValue());
//							Set<UserRole> checkUserRoles = new HashSet<UserRole>();
//							checkUserRoles.addAll(userRoles.getValue());
//							
//							if (clusterNo.getSelectedItems().size() > 0) {
//								System.out.println( (checkUserRoles.contains(UserRole._REST_USER)) + "uu"+ (checkUserRoles.contains("Mobile User")) + "User Role contain mobile and cluster flw " +clusterNo.getSelectedItems());
//								
								user.setCommunity(clusterNo.getSelectedItems());
								
//							} else {
//								
//								Set<CommunityReferenceDto> emptyClusterList = new HashSet<CommunityReferenceDto>();
//								System.out.println((checkUserRoles.contains(UserRole._REST_USER)) + "uu"+ (checkUserRoles.contains("Mobile User")) + "User Role Does not contain mobile and cluster flw " + clusterNo.getSelectedItems());
////
//								user.setCommunity(emptyClusterList);
////								user.setCommunitynos(null);
//							}

							updatedItems.add(user);

							globalUserDto = user;
//							System.out.println(
//									updatedItems.size() + "+++++++++++++++++++++++++++++++++++++" + updatedItems);
							FacadeProvider.getUserFacade().bulkUpdateUserRoles(userList, user);

						}

					}
				} catch (Exception exception) {
					logger.debug(exception.toString());
				} finally {
					donloadErrorReport.setVisible(true);
					donloadErrorReport.addClickListener(ex -> {
						close();

						open();
						System.out.println(updatedItems.size() + "+++++++++++++in listener ++++++++++++++++++++++++"
								+ updatedItems);
//						donloadErrorReport.setVisible(false);
						configureUpdateReviewGrid(updatedItems, globalUserDto);

						scrollToBottom(dialog);

					});
				}

			});
		});

		H3 step4 = new H3();
		step4.add("Step 3: Review Updated Users");
		donloadErrorReport.setVisible(false);

		startIntervalCallback();

		dialog.add(step2, lblImportTemplateInfo1, lblImportTemplateInfo, editableFormFields, step3, lblImportCsvFile,
				formAccessParentLayout, userRoles, region, province, district, districtMulti, clusterNo, startDataUpdate, step4,
				donloadErrorReport);
		


		Button doneButton = new Button("Done", e -> {
			close();
			stopIntervalCallback();
			filterDataProvider.refreshAll();

		});
		Icon doneButtonIcon = new Icon(VaadinIcon.CHECK_CIRCLE_O);

		doneButton.setIcon(doneButtonIcon);
		getFooter().add(doneButton);

		add(dialog);
		setWidth("80%");
		setCloseOnEsc(false);
		setCloseOnOutsideClick(false);
		

	}

	private void scrollToBottom(VerticalLayout layout) {
		layout.getElement().executeJs("arguments[0].scrollTop = arguments[0].scrollHeight;", layout.getElement());
	}

	public static abstract class BulkUsersEditDataDialogEvent extends ComponentEvent<UsersBulkEditDialog> {
		private UserDto user;

		protected BulkUsersEditDataDialogEvent(UsersBulkEditDialog source, UserDto user) {
			super(source, false);
			this.user = user;
		}

		public UserDto getContact() {
			return user;
		}
	}

	public static class SaveEvent extends BulkUsersEditDataDialogEvent {
		SaveEvent(UsersBulkEditDialog source, UserDto user) {
			super(source, user);
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
				} else {

				}
			}
		});
	}

	class UserRoleCustomComparator implements Comparator<UserRole> {
		private final String[] customOrder = { "Admin", "National Data Manager", "National Officer",
				"National Observer / Partner", "Regional Observer", "Regional Data Manager", "Regional Officer",
				"Provincial Observer", "Provincial Data Clerk", "Provincial Officer", "District Officer",
				"District Observer" };

		@Override
		public int compare(UserRole role1, UserRole role2) {
			// Get the indexes of the roles in the custom order
			int index1 = indexOfRole(role1);
			int index2 = indexOfRole(role2);

			// Compare based on their indexes in the custom order
			return Integer.compare(index1, index2);
		}

		private int indexOfRole(UserRole role) {
			for (int i = 0; i < customOrder.length; i++) {
				if (customOrder[i].equals(role.name())) {
					return i;
				}
			}
			return customOrder.length; // Role not found, place it at the end
		}
	}
	
	protected void updateFieldsByUserRole(Set<UserRole> userRoles) {
	    if (userRoles == null || userRoles.isEmpty()) {
	        clusterNo.clear(); clusterNo.setVisible(false);
	        district.clear(); district.setVisible(false);
	        districtMulti.clear(); districtMulti.setVisible(false);
	        province.clear(); province.setVisible(false);
	        region.clear(); region.setVisible(false);
	        return;
	    }

	    final JurisdictionLevel jurisdictionLevel = UserRole.getJurisdictionLevel(userRoles);

	    boolean showRegion = false;
	    boolean showProvince = false;
	    boolean showDistrict = false;
	    boolean showDistrictMulti = false;
	    boolean showCluster = false;

	    if (jurisdictionLevel == JurisdictionLevel.COMMUNITY) {
	        showRegion = true; showProvince = true; showDistrict = true; showCluster = true;
	        isDistrictMulti = false;
	    } else if (jurisdictionLevel == JurisdictionLevel.DISTRICT) {
	        showRegion = true; showProvince = true;
	        if (userRoles.contains(UserRole.SURVEILLANCE_OFFICER)) {
	            showDistrictMulti = true; isDistrictMulti = true;
	        } else {
	            showDistrict = true; isDistrictMulti = false;
	        }
	    } else if (jurisdictionLevel == JurisdictionLevel.REGION) {
	        showRegion = true; showProvince = true; isDistrictMulti = false;
	    } else if (jurisdictionLevel == JurisdictionLevel.AREA || jurisdictionLevel == JurisdictionLevel.NATION) {
	        showRegion = true; isDistrictMulti = false;
	    }

	    // 1. Apply visibility and clear hidden fields
	    region.setVisible(showRegion);
	    if (!showRegion) region.clear();

	    province.setVisible(showProvince);
	    if (!showProvince) province.clear();

	    district.setVisible(showDistrict);
	    if (!showDistrict) district.clear();

	    districtMulti.setVisible(showDistrictMulti);
	    if (!showDistrictMulti) districtMulti.clear();

	    clusterNo.setVisible(showCluster);
	    if (!showCluster) clusterNo.clear();

	    // 2. Re-populate if Province was already selected
	    if (province.getValue() != null) {
	        String provUuid = province.getValue().getUuid();
	        if (showDistrict) {
	            districts = FacadeProvider.getDistrictFacade().getAllActiveByRegion(provUuid);
	            district.setItems(districts);
	        } else if (showDistrictMulti) {
	            districts = FacadeProvider.getDistrictFacade().getAllActiveByRegion(provUuid);
	            districtMulti.setItems(districts);
	        }
	    }
	}
	

//	protected void updateFieldsByUserRole(Set<UserRole> userRoles) {
//		final JurisdictionLevel jurisdictionLevel = UserRole.getJurisdictionLevel(userRoles);
//		final boolean useCommunity = jurisdictionLevel == JurisdictionLevel.COMMUNITY;
//		final boolean useDistrictOnly = jurisdictionLevel == JurisdictionLevel.DISTRICT;
//		final boolean useDistrict = jurisdictionLevel == JurisdictionLevel.DISTRICT || useCommunity;
//		final boolean useRegion = jurisdictionLevel == JurisdictionLevel.REGION || useDistrict;
//		final boolean useArea = jurisdictionLevel == JurisdictionLevel.AREA || useRegion;
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
////			district.clear();
//			district.setVisible(false);
//			districtMulti.setVisible(true);
//			province.setVisible(true);
//			region.setVisible(true);
//		} else if (useDistrict) {
//			clusterNo.clear();
//			clusterNo.setVisible(false);
//			district.clear();
//			district.setVisible(false);
//			districtMulti.setVisible(true);
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
//
//	}

	private void pokeFlow() {
		// Notification.show("dialog detected... User wont logout");
	}

	private void startIntervalCallback() {
		UI.getCurrent().setPollInterval(5000);
		if (!callbackRunning) {
			timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					stopIntervalCallback();
				}
			}, 15000); // 10 minutes

			callbackRunning = true;
		}
	}

	private void stopIntervalCallback() {
		if (callbackRunning) {
			callbackRunning = false;
			if (timer != null) {
				timer.cancel();
				timer.purge();
			}

		}
	}

	private void stopPullers() {
		UI.getCurrent().setPollInterval(-1);
	}

	private void refreshPage() {
		// Get the current UI
		UI ui = UI.getCurrent();

		// Get the current page and reload it
		Page page = ui.getPage();
		page.reload();
	}

	protected void resetDownloadErrorReportButton() {
		downloadErrorReportButton.removeAll();
		downloadErrorReportButton.setVisible(false);
	}

	public void extendDownloadErrorReportButton(StreamResource streamResource) {
		anchorSpan.remove(downloadErrorReportButton);
		donloadErrorReport.setVisible(true);

		downloadErrorReportButton = new Anchor(streamResource, ".");// ,
																	// I18nProperties.getCaption(Captions.downloadErrorReport));
																	// I18nProperties.getCaption(Captions.importDownloadErrorReport)
		downloadErrorReportButton.setHref(streamResource);
		downloadErrorReportButton.setClassName("vaadin-button");

		anchorSpan.add(downloadErrorReportButton);

	}

	public void extendDownloadCredentialsReportButton(StreamResource streamResource) {
		anchorSpanCredential.remove(downloadCredntialsReportButton);

		downloadCredntialsReportButton = new Anchor(streamResource, ".");// ,
																			// I18nProperties.getCaption(Captions.downloadErrorReport));
																			// I18nProperties.getCaption(Captions.importDownloadErrorReport)
		downloadCredntialsReportButton.setHref(streamResource);
		downloadCredntialsReportButton.setClassName("vaadin-button");

		anchorSpanCredential.add(downloadCredntialsReportButton);

	}

	public void configureUpdateReviewGrid(Set<UserDto> selectedItems, UserDto userDto) {
		Column<UserDto> usernameColumn = reviewGrid.addColumn(UserDto::getUserName)
				.setHeader(I18nProperties.getCaption(Captions.User_userName)).setSortable(true).setAutoWidth(true)
				.setResizable(true);

		if (editableFormFields.getValue().equalsIgnoreCase("form access")) {
			Column<UserDto> FormAccessColumn = reviewGrid.addColumn(UserDto::getFormAccess)
					.setHeader(I18nProperties.getCaption(Captions.formAccess)).setSortable(true).setAutoWidth(true)
					.setResizable(true);

		} else if (editableFormFields.getValue().equalsIgnoreCase("user role")) {

			Column<UserDto> userRolesColumn = reviewGrid.addColumn(UserDto::getUserRoles)
					.setHeader(I18nProperties.getCaption(Captions.User_userRoles)).setSortable(true).setAutoWidth(true)
					.setResizable(true);

//			Set<UserDto> userDto =  selectedItems; // {
			if (userDto.getArea() != null) {

				Column<UserDto> userRegionColumn = reviewGrid.addColumn(UserDto::getArea)
						.setHeader(I18nProperties.getCaption(Captions.area)).setSortable(true).setAutoWidth(true)
						.setResizable(true);

			}

			if (userDto.getRegion() != null) {

				Column<UserDto> userProvinceColumn = reviewGrid.addColumn(UserDto::getRegion)
						.setHeader(I18nProperties.getCaption(Captions.region)).setSortable(true).setAutoWidth(true)
						.setResizable(true);

			}

			if (userDto.getDistrict() != null) {

				Column<UserDto> userDistrictColumn = reviewGrid.addColumn(UserDto::getDistrict)
						.setHeader(I18nProperties.getCaption(Captions.district)).setSortable(true).setAutoWidth(true)
						.setResizable(true);
			}

			if (userDto.getDistricts().size() > 0) {

				Column<UserDto> userDistrictColumn = reviewGrid.addColumn(UserDto::getDistricts)
						.setHeader(I18nProperties.getCaption(Captions.district)).setSortable(true).setAutoWidth(true)
						.setResizable(true);
			}

			if ( userDto.getCommunity().size() > 0 ) {

				Column<UserDto> userCommunityColumn = reviewGrid.addColumn(UserDto::getCommunity)
						.setHeader(I18nProperties.getCaption(Captions.clusterNumber)).setSortable(true)
						.setAutoWidth(true).setResizable(true);
			}

//			}

		}

		ListDataProvider<UserDto> listed = new ListDataProvider<UserDto>(selectedItems);

		System.out.println(listed + "items in grid " + "ccccccccccccccccc");
		reviewGrid.setDataProvider(listed);

		dialog.add(reviewGrid);
	}

}