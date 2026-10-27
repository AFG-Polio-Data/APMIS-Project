package com.cinoteck.application.views.campaigndata;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.validation.constraints.NotNull;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.LoggerFactory;

import com.cinoteck.application.UserProvider;
import com.cinoteck.application.UserProvider.HasUserProvider;
import com.cinoteck.application.ViewModelProviders;
import com.cinoteck.application.ViewModelProviders.HasViewModelProviders;
import com.cinoteck.application.views.MainLayout;
import com.cinoteck.application.views.utils.DownloadTransposedDaywiseDataUtility;
import com.cinoteck.application.views.utils.DownloadTransposedLqasDataUtility;
import com.cinoteck.application.views.utils.gridexporter.GridExporter;
import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.Grid.MultiSortPriority;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.data.selection.MultiSelect;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.Language;
import de.symeda.sormas.api.MapperUtil;
import de.symeda.sormas.api.campaign.CampaignDto;
import de.symeda.sormas.api.campaign.CampaignIndexDto;
import de.symeda.sormas.api.campaign.CampaignPhase;
import de.symeda.sormas.api.campaign.CampaignReferenceDto;
import de.symeda.sormas.api.campaign.data.CampaignFormDataCriteria;
import de.symeda.sormas.api.campaign.data.CampaignFormDataDto;
import de.symeda.sormas.api.campaign.data.CampaignFormDataEntry;
import de.symeda.sormas.api.campaign.data.CampaignFormDataIndexDto;
import de.symeda.sormas.api.campaign.data.CampaignFormImageNamingContext;
import de.symeda.sormas.api.campaign.data.CampaignFormImageValue;
import de.symeda.sormas.api.campaign.form.CampaignFormElement;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaDto;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaExpiryDto;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaGeographyLevel;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaHistoryExtractDto;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaReferenceDto;
import de.symeda.sormas.api.document.DocumentDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.Descriptions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.infrastructure.PopulationDataDto;
import de.symeda.sormas.api.infrastructure.area.AreaDto;
import de.symeda.sormas.api.infrastructure.area.AreaReferenceDto;
import de.symeda.sormas.api.infrastructure.community.CommunityReferenceDto;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.user.FormAccess;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.user.UserRole;
import de.symeda.sormas.api.user.UserType;
import de.symeda.sormas.api.utils.CSVUtils;
import de.symeda.sormas.api.utils.SortProperty;
import org.apache.commons.lang3.StringUtils;

@PageTitle("APMIS-Campaign Data")
@Route(value = "campaigndata", layout = MainLayout.class)
@CssImport(value = "./styles/custom-grid-styles.css", themeFor = "vaadin-grid")
@CssImport(value = "./styles/vaadin-text-field.css", themeFor = "vaadin-text-field")
@CssImport(value = "./styles/vaadin-number-field.css", themeFor = "vaadin-number-field")

public class CampaignDataView extends VerticalLayout
		implements HasUserProvider, HasViewModelProviders, BeforeEnterObserver {

	private static final long serialVersionUID = 7588118851062483372L;

	private Grid<CampaignFormDataIndexDto> grid = new Grid<>(CampaignFormDataIndexDto.class, false);
	private CampaignFormDataCriteria criteria;
	private CampaignFormMetaDto formMetaReference;
	private CampaignFormDataDto campaignFormDatadto;

	ComboBox<String> campaignYear = new ComboBox<>();
	ComboBox<CampaignReferenceDto> campaignz = new ComboBox<>();
	ComboBox<CampaignPhase> campaignPhase = new ComboBox<>();
	ComboBox<CampaignFormMetaReferenceDto> campaignFormCombo = new ComboBox<>();
//	ComboBox<AreaReferenceDto> regionCombo = new ComboBox<>();
	MultiSelectComboBox<AreaReferenceDto> regionCombo = new MultiSelectComboBox<>();
//	ComboBox<RegionReferenceDto> provinceCombo = new ComboBox<>();
	MultiSelectComboBox<RegionReferenceDto> provinceCombo = new MultiSelectComboBox<>();

//	ComboBox<DistrictReferenceDto> districtCombo = new ComboBox<>();
	MultiSelectComboBox<DistrictReferenceDto> districtCombo = new MultiSelectComboBox<>();

	ComboBox<CommunityReferenceDto> clusterCombo = new ComboBox<>();
	ComboBox<String> verifiedStatusCombo = new ComboBox<>();
	ComboBox<String> publishedStatusCombo = new ComboBox<>();

	ComboBox<CampaignFormElementImportance> importanceSwitcher = new ComboBox<>();
	ComboBox<String> timelinessFilter = new ComboBox<>();
	Button resetHandler = new Button();
//	Button applyHandler = new Button();
	List<AreaReferenceDto> regions;
	List<AreaReferenceDto> regionsx;
	List<RegionReferenceDto> provinces;
	List<DistrictReferenceDto> districts;
	List<CommunityReferenceDto> communities;
	List<CampaignFormMetaReferenceDto> campaignForms;
	Anchor anchor = new Anchor("", I18nProperties.getCaption(Captions.export));
	Anchor transposdeDataAnchor = new Anchor("", "Export Transposed Data");
	Anchor transposdeDataDictionaryAnchor = new Anchor("", "Export Transposed Data Guide");

	private List<PopulationDataDto> popDto = new ArrayList<PopulationDataDto>();

	CampaignFormDataCriteria transposedDataCriteriaListener = new CampaignFormDataCriteria();

	Icon icon = VaadinIcon.UPLOAD_ALT.create();
	Paragraph countRowItems;
	UserProvider userProvider = new UserProvider();

	CampaignFormDataEditForm campaignFormDataEditForm;

	Button enterBulkEdit = new Button(I18nProperties.getCaption(Captions.actionEnterBulkEditMode));
	Button leaveBulkEdit = new Button(I18nProperties.getCaption(Captions.actionLeaveBulkEditMode));
	MenuBar dropdownBulkOperations = new MenuBar();
	ConfirmDialog confirmationDialog;
	Checkbox checkboxx = new Checkbox();
	Button selectAllButton = new Button();
	Button selectAllButtonpLACEHOLDER = new Button();
	private String exportFileName;
	private GridExporter<CampaignFormDataIndexDto> exporter;
	private Map<String, String> exportImageFileNames;

	private DataProvider<CampaignFormDataIndexDto, CampaignFormDataCriteria> dataProvider;

	NumberFormat arabicFormat = NumberFormat.getInstance();

	Column<CampaignFormDataIndexDto> provinceNameColumn, pCodeColumn, districtNameColumn, dCodeColumn,
			clusterNumberColumn, ccodeColumn, formNameColumn, clusterNameColumn, publishedColumn, verifiedColumn;

	ComboBox<CampaignFormMetaReferenceDto> newForm = new ComboBox<>();
	ComboBox<CampaignFormMetaReferenceDto> importFormData = new ComboBox<>();

	HorizontalLayout actionButtonlayout = new HorizontalLayout();
	Button exportTransposedDataButton = new Button();

	GridMultiSelectionModel<CampaignFormDataIndexDto> selectionModel;
	private Set<CampaignFormDataIndexDto> selectedItems = new HashSet<>();

	// Counters for tracking creation
	private int transposdeDataAnchorCreationCount = 0;
	private int exportTransposedDataButtonCreationCount = 0;
	// Initialize a flag to check if the click listener is already attached
	boolean isClickListenerAttached;
	private boolean callbackRunning = false;
	private Timer timer;

	private boolean isGridInMultiselectMode = false;

	protected final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());

	public CampaignDataView() {
		languageHandler();
		setSizeFull();
		setSpacing(false);
		criteria = new CampaignFormDataCriteria();
		criteria.setUsertype(userProvider.getUser().getUsertype().toString());
		criteria.setUsertypeEnum(userProvider.getUser().getUsertype());
		createCampaignDataFilter();

		configureGrid(criteria);
//		configureGridDataRestricttion(criteria);
		configureColumnStyles(criteria);
	}

	private String getLabelForEnum(CampaignPhase campaignPhase) {
		if (campaignPhase != null) {
			switch (campaignPhase) {
			case PRE:
				return "Pre-Campaign";

			case POST:
				return "Post-Campaign";

			case INTRA:
				return "Intra-Campaign";

			default:
				return campaignPhase.toString();
			}
		} else {
			return "Intra-Campaign";
		}

	}

	private void createCampaignDataFilter() {

		setMargin(true);

		newForm.setLabel(I18nProperties.getCaption(Captions.actionNewForm));
		newForm.setPlaceholder(I18nProperties.getCaption(Captions.dataEntry));
		newForm.setTooltipText(I18nProperties.getDescription(Descriptions.campaign_dataEntry));
		newForm.setClearButtonVisible(true);
		newForm.getStyle().set("padding-top", "0px !important");

		importFormData.setLabel(I18nProperties.getCaption(Captions.actionImport));
		importFormData.setPlaceholder(I18nProperties.getCaption(Captions.dataImport));
		importFormData.setTooltipText(I18nProperties.getDescription(Descriptions.campaign_dataImport));
		importFormData.setClearButtonVisible(true);
		importFormData.getStyle().set("padding-top", "0px !important");

		exportTransposedDataButton = new Button("Export Transposed Data");
//		exporttransposeDataDictionary = new Button("Export Transposed Data Guide");

		VerticalLayout filterBlock = new VerticalLayout();
		filterBlock.setSpacing(true);
		filterBlock.setMargin(true);
		filterBlock.setClassName("campaignDataFilterParent");

		HorizontalLayout layout = new HorizontalLayout();
		layout.setAlignItems(Alignment.END);
		I18nProperties.setDefaultLanguage(userProvider.getUser().getLanguage());
		Button displayFilters = new Button(I18nProperties.getCaption(Captions.hideFilters),
				new Icon(VaadinIcon.SLIDERS));

		actionButtonlayout.setClassName("row pl-3");
		actionButtonlayout.setVisible(true);
		actionButtonlayout.setAlignItems(Alignment.END);

		Button exportButton = new Button(I18nProperties.getCaption(Captions.export));
		exportButton.setIcon(new Icon(VaadinIcon.UPLOAD));

		Button exportImagesButton = new Button("Export with Images (ZIP)");
		exportImagesButton.setIcon(new Icon(VaadinIcon.DOWNLOAD));

		exportTransposedDataButton.setIcon(new Icon(VaadinIcon.UPLOAD));
//		exporttransposeDataDictionary.setIcon(new Icon(VaadinIcon.DOWNLOAD));

		exportButton.addClickListener(e -> {

			if (checkIfActiveFormContainsImageField(campaignFormCombo.getValue().getUuid())) {
				anchor.setHref(createZipExportResource());
				anchor.getElement().callJsFunction("click");

			} else {

				anchor.getElement().callJsFunction("click");

			}
		});

		actionButtonlayout.add(campaignYear, campaignz, campaignPhase, newForm, importFormData, exportButton, anchor);
		anchor.getStyle().set("display", "none");

		HorizontalLayout level1Filters = new HorizontalLayout();
		level1Filters.setPadding(false);
		level1Filters.setVisible(true);
		level1Filters.setWidth("98%");
		level1Filters.setAlignItems(Alignment.END);
		level1Filters.setClassName("row pl-3");

		publishedStatusCombo.setVisible(false);
		verifiedStatusCombo.setVisible(false);

		HorizontalLayout rightFloat = new HorizontalLayout();
		rightFloat.setWidth("100%");
		rightFloat.setJustifyContentMode(JustifyContentMode.END);
		level1Filters.add(campaignFormCombo, regionCombo, provinceCombo, districtCombo, clusterCombo,
				verifiedStatusCombo, publishedStatusCombo, importanceSwitcher, resetHandler, rightFloat);

		displayFilters.addClickListener(e -> {
			I18nProperties.setUserLanguage(userProvider.getUser().getLanguage());
			if (!level1Filters.isVisible()) {
				actionButtonlayout.setVisible(true);
				level1Filters.setVisible(true);
				displayFilters.setText(I18nProperties.getCaption(Captions.hideFilters));
			} else {
				actionButtonlayout.setVisible(false);
				level1Filters.setVisible(false);
				displayFilters.setText(I18nProperties.getCaption(Captions.showFilters));
			}
		});

		TextArea resultField = new TextArea();
		resultField.setWidth("100%");

		campaignYear.setLabel(I18nProperties.getCaption(Captions.campaignYear));
		campaignYear.getStyle().set("padding-top", "0px !important");
		campaignYear.setClassName("col-sm-6, col-xs-6");

		campaignz.setLabel(I18nProperties.getCaption(Captions.Campaigns));
		campaignz.getStyle().set("padding-top", "0px !important");
		campaignz.setClassName("col-sm-6, col-xs-6");

		campaignPhase.setLabel(I18nProperties.getCaption(Captions.Campaign_phase));
		campaignPhase.getStyle().set("padding-top", "0px !important");
		campaignz.setClassName("col-sm-6, col-xs-6");

		campaignFormCombo.setLabel(I18nProperties.getCaption(Captions.campaignCampaignForm));
		campaignFormCombo.getStyle().set("padding-top", "0px !important");
		campaignFormCombo.getStyle().set("--vaadin-combo-box-overlay-width", "350px");
		campaignFormCombo.setClassName("col-sm-6, col-xs-6");

//		campaignFormComboVersions.setLabel(I18nProperties.getCaption(Captions.campaignCampaignForm + "Versions"));
//		campaignFormComboVersions.getStyle().set("padding-top", "0px !important");
//		campaignFormComboVersions.getStyle().set("--vaadin-combo-box-overlay-width", "350px");
//		campaignFormComboVersions.setClassName("col-sm-6, col-xs-6");

		regionCombo.setLabel(I18nProperties.getCaption(Captions.area));
		regionCombo.getStyle().set("padding-top", "0px !important");
		regionCombo.setClassName("col-sm-6, col-xs-6");
		regionCombo.setPlaceholder(I18nProperties.getCaption(Captions.area));

		if (userProvider.getUser().getLanguage().toString().equals("Pashto")) {
			regionsx = FacadeProvider.getAreaFacade().getAllActiveAsReferencePashto();
			regionCombo.setItems(regionsx);
		} else if (userProvider.getUser().getLanguage().toString().equals("Dari")) {
			regionsx = FacadeProvider.getAreaFacade().getAllActiveAsReferenceDari();
			regionCombo.setItems(regionsx);
		} else {
			regions = FacadeProvider.getAreaFacade().getAllActiveAsReference();
			regionCombo.setItems(regions);
		}

		if (userProvider.getUser().getArea() != null) {
			if (userProvider.getUser().getLanguage().toString().equals("Pashto")) {
				AreaReferenceDto singleArea = userProvider.getUser().getArea();
				AreaDto hgsghsag = FacadeProvider.getAreaFacade().getByUuid(singleArea.getUuid());
				AreaReferenceDto singleAreatw0 = new AreaReferenceDto(hgsghsag.getUuid(), hgsghsag.getFa_af());
				regionCombo.setValue(singleAreatw0);
			} else if (userProvider.getUser().getLanguage().toString().equals("Dari")) {
				regionCombo.setValue(userProvider.getUser().getArea());
			} else {
				regionCombo.setValue(userProvider.getUser().getArea());// rda56kGbCAja
			}
			regionCombo.setEnabled(false);
		}

		provinceCombo.setLabel(I18nProperties.getCaption(Captions.region));
		provinceCombo.getStyle().set("padding-top", "0px !important");
		provinceCombo.setClassName("col-sm-6, col-xs-6");
		provinceCombo.setPlaceholder(I18nProperties.getCaption(Captions.region));

		provinceCombo.setEnabled(false);
		districtCombo.setEnabled(false);
		clusterCombo.setEnabled(false);

		List<DistrictReferenceDto> userDistricts = new ArrayList<>();

		if (userProvider.getUser().getUserRoles().contains(UserRole.AREA_SURVEILLANCE_SUPERVISOR)
				&& userProvider.getUser().getArea() != null) {

			regionCombo.setValue(userProvider.getUser().getArea());
			regionCombo.setEnabled(false);
			if (regionCombo.getValue() != null) {
				provinces = FacadeProvider.getRegionFacade()
						.getAllActiveByArea(userProvider.getUser().getArea().getUuid());
				provinceCombo.setItems(provinces);
				provinceCombo.setEnabled(true);
			}
		}

		else if (!userProvider.getUser().getUserRoles().contains(UserRole.AREA_SURVEILLANCE_SUPERVISOR)
				&& userProvider.getUser().getArea() != null) {

			regionCombo.setValue(userProvider.getUser().getArea());
			regionCombo.setEnabled(false);

			if (userProvider.getUser().getRegion() != null
					&& userProvider.getUser().getUserRoles().contains(UserRole.SURVEILLANCE_SUPERVISOR)) {

				provinces = FacadeProvider.getRegionFacade()
						.getAllActiveByArea(userProvider.getUser().getArea().getUuid());
				provinceCombo.setItems(provinces);
				provinceCombo.setValue(userProvider.getUser().getRegion());
				provinceCombo.setEnabled(false);

				districts = FacadeProvider.getDistrictFacade()
						.getAllActiveByRegion(userProvider.getUser().getRegion().getUuid());

				// If user has multiple districts
				if (userProvider.getUser().getDistricts() != null && !userProvider.getUser().getDistricts().isEmpty()) {

					System.out.println(userProvider.getUser().getDistricts()
							+ "districts11114444488888888888888888888888888888888userDistrict");

					userDistricts = userProvider.getUser().getDistricts().stream().filter(districts::contains)
							.collect(Collectors.toList());

					districtCombo.setItems(userDistricts);

				}
				// If user has single district
				else if (userProvider.getUser().getDistrict() != null) {

					DistrictReferenceDto userDistrict = userProvider.getUser().getDistrict();

					if (districts.contains(userDistrict)) {
						userDistricts.add(userDistrict);
					}
					districtCombo.setItems(userDistricts);

				}
				// If user does not have district

				else {
					districtCombo.setItems(districts);

				}

				districtCombo.setEnabled(true);

			} else if (userProvider.getUser().getRegion() != null
					&& !userProvider.getUser().getUserRoles().contains(UserRole.SURVEILLANCE_SUPERVISOR)) {

				provinces = FacadeProvider.getRegionFacade()
						.getAllActiveByArea(userProvider.getUser().getArea().getUuid());
				provinceCombo.setItems(provinces);
				provinceCombo.setValue(userProvider.getUser().getRegion());
				provinceCombo.setEnabled(false);

				if (userProvider.getUser().getDistrict() != null
						&& userProvider.getUser().getUserRoles().contains(UserRole.SURVEILLANCE_OFFICER)) {

					districts = FacadeProvider.getDistrictFacade()
							.getAllActiveByRegion(userProvider.getUser().getRegion().getUuid());

					if (userProvider.getUser().getDistricts() != null
							&& !userProvider.getUser().getDistricts().isEmpty()) {

						userDistricts = userProvider.getUser().getDistricts().stream().filter(districts::contains)
								.collect(Collectors.toList());
					}
					// If user has single district
					else if (userProvider.getUser().getDistrict() != null) {

						DistrictReferenceDto userDistrict = userProvider.getUser().getDistrict();

						if (districts.contains(userDistrict)) {
							userDistricts.add(userDistrict);
						}
					}

					System.out.println("5555588888888888888888888888888888888");

					districtCombo.setItems(userDistricts);
//					districtCombo.setItems(districts);
					districtCombo.setValue(userProvider.getUser().getDistrict());
					districtCombo.setEnabled(false);
					if (districtCombo.getValue() != null) {
						List<CommunityReferenceDto> allClusters = new ArrayList<>();

						for (DistrictReferenceDto selectedUUID : districtCombo.getValue()) {
							String uuid = selectedUUID.getUuid();
							communities = FacadeProvider.getCommunityFacade().getAllActiveByDistrict(uuid);
							allClusters.addAll(communities);
						}

//						communities = FacadeProvider.getCommunityFacade().getAllActiveByDistrict(e.getValue().getUuid());
						clusterCombo.setItemLabelGenerator(itm -> {
							CommunityReferenceDto dcfv = (CommunityReferenceDto) itm;
							return dcfv.getNumber() + " | " + dcfv.getCaption();
						});

//						communities = FacadeProvider.getCommunityFacade()
//								.getAllActiveByDistrict(districtCombo.getValue().getUuid());
						clusterCombo.clear();
//						clusterCombo.setItems(communities);
						allClusters.sort(Comparator.comparing(CommunityReferenceDto::getNumber));
						clusterCombo.setItems(allClusters);
						clusterCombo.setEnabled(true);
					}
				} else if (userProvider.getUser().getDistrict() != null
						&& !userProvider.getUser().getUserRoles().contains(UserRole.SURVEILLANCE_OFFICER)) {

					provinces = FacadeProvider.getRegionFacade()
							.getAllActiveByArea(userProvider.getUser().getArea().getUuid());
					provinceCombo.setItems(provinces);
					provinceCombo.setValue(userProvider.getUser().getRegion());
					provinceCombo.setEnabled(false);

					districts = FacadeProvider.getDistrictFacade()
							.getAllActiveByRegion(userProvider.getUser().getRegion().getUuid());

					if (userProvider.getUser().getDistricts() != null
							&& !userProvider.getUser().getDistricts().isEmpty()) {

						userDistricts = userProvider.getUser().getDistricts().stream().filter(districts::contains)
								.collect(Collectors.toList());

						districtCombo.setItems(userDistricts);

					}
					// If user has single district
					else if (userProvider.getUser().getDistrict() != null) {

						DistrictReferenceDto userDistrict = userProvider.getUser().getDistrict();

						if (districts.contains(userDistrict)) {
							userDistricts.add(userDistrict);
						}

						districtCombo.setItems(districts);

					} else {
						districtCombo.setItems(userDistricts);

					}

					System.out.println("666688888888888888888888888888888888");

//					districtCombo.setItems(districts);
					districtCombo.setValue(userProvider.getUser().getDistrict());
					districtCombo.setEnabled(false);

					communities = FacadeProvider.getCommunityFacade()
							.getAllActiveByDistrict(userProvider.getUser().getDistrict().getUuid());
					communities.sort(Comparator.comparing(CommunityReferenceDto::getNumber));
					clusterCombo.setItems(communities);
					clusterCombo.setEnabled(true);

					if (userProvider.getUser().getCommunity() != null
							&& userProvider.getUser().getUserRoles().contains(UserRole.COMMUNITY_OFFICER)) {

						communities = FacadeProvider.getCommunityFacade()
								.getAllActiveByDistrict(userProvider.getUser().getDistrict().getUuid());
						communities.sort(Comparator.comparing(CommunityReferenceDto::getNumber));
						clusterCombo.setItems(communities);
//					clusterCombo.setValue(userProvider.getUser().getCommunity());
						clusterCombo.setEnabled(false);

					} else if (userProvider.getUser().getCommunity() != null
							&& !userProvider.getUser().getUserRoles().contains(UserRole.COMMUNITY_OFFICER)) {

						communities = FacadeProvider.getCommunityFacade()
								.getAllActiveByDistrict(userProvider.getUser().getDistrict().getUuid());
						communities.sort(Comparator.comparing(CommunityReferenceDto::getNumber));
						clusterCombo.setItems(communities);
//					clusterCombo.setValue(userProvider.getUser().getCommunity());
						clusterCombo.setEnabled(false);
					} else {

						communities = FacadeProvider.getCommunityFacade()
								.getAllActiveByDistrict(userProvider.getUser().getDistrict().getUuid());
						communities.sort(Comparator.comparing(CommunityReferenceDto::getNumber));
						clusterCombo.setItems(communities);
						clusterCombo.setEnabled(true);
					}

				} else {

					System.out.println("777788888888888888888888888888888888");

					districts = FacadeProvider.getDistrictFacade()
							.getAllActiveByRegion(userProvider.getUser().getRegion().getUuid());

					if (userProvider.getUser().getDistricts() != null
							&& !userProvider.getUser().getDistricts().isEmpty()) {

						userDistricts = userProvider.getUser().getDistricts().stream().filter(districts::contains)
								.collect(Collectors.toList());

						districtCombo.setItems(userDistricts);

					}
					// If user has single district
					else if (userProvider.getUser().getDistrict() != null) {

						DistrictReferenceDto userDistrict = userProvider.getUser().getDistrict();

						if (districts.contains(userDistrict)) {
							userDistricts.add(userDistrict);
						}

						districtCombo.setItems(userDistricts);

					}

					// If user does not have district

					else {
						districtCombo.setItems(districts);

					}

//					districtCombo.setItems(userDistricts);
//					districtCombo.setItems(districts);
					districtCombo.setEnabled(true);

				}

			} else {

				System.out.println("88888888888888888888888888888888");

				provinces = FacadeProvider.getRegionFacade()
						.getAllActiveByArea(userProvider.getUser().getArea().getUuid());
				provinceCombo.setItems(provinces);
				provinceCombo.setEnabled(true);
			}
		}

		else {

			provinces = FacadeProvider.getRegionFacade().getAllActiveAsReference();
			provinceCombo.setItems(provinces);
		}

		dropdownBulkOperations = new MenuBar();
		MenuItem bulkActionsItem = dropdownBulkOperations.addItem(I18nProperties.getCaption(Captions.bulkActions));
		SubMenu subMenu = bulkActionsItem.getSubMenu();

		final MenuItem deleteBulkItem = subMenu.addItem(I18nProperties.getCaption(Captions.actionDelete),
				e -> handleDeleteAction());
		final MenuItem verifyDataBulkItem = subMenu.addItem(I18nProperties.getCaption(Captions.actionVerify),
				e -> handleDataVerificationAction());

		final MenuItem publishDataBulkItem = subMenu.addItem(I18nProperties.getCaption(Captions.actionPublishData),
				e -> handleDataPublishingAction());

		final MenuItem validateDataBulkItem = subMenu.addItem(I18nProperties.getCaption(Captions.actionPublishData),
				e -> handleDataValidateAction());

		final MenuItem unVerifyDataBulkItem = subMenu.addItem(I18nProperties.getCaption("Unverify"),
				e -> handleDataUnVerificationAction());

		final MenuItem unPublishDataBulkItem = subMenu.addItem(I18nProperties.getCaption(Captions.actionUnpublish),
				e -> handleDataUnPublishingAction());

		final MenuItem unValidateDataBulkItem = subMenu.addItem(I18nProperties.getCaption(Captions.actionUnpublish),
				e -> handleDataUnValidateAction());

		if (userProvider.hasUserRight(UserRight.PERFORM_BULK_OPERATIONS)) {
			enterBulkEdit = new Button(I18nProperties.getCaption(Captions.actionEnterBulkEditMode));
			leaveBulkEdit = new Button(I18nProperties.getCaption(Captions.actionLeaveBulkEditMode));

			deleteBulkItem.setVisible(true);

			if (userProvider.getUser().getUsertype() == UserType.WHO_USER && campaignPhase.getValue() != null
					&& userProvider.getUser().getUserRoles().contains(UserRole.PUBLISH_USER)) {
				if (campaignPhase.getValue().toString().equalsIgnoreCase("post-campaign")) {
					verifyDataBulkItem.setVisible(true);
					publishDataBulkItem.setVisible(true);
					unVerifyDataBulkItem.setVisible(true);
					unPublishDataBulkItem.setVisible(true);
				} else {
					verifyDataBulkItem.setVisible(false);
					publishDataBulkItem.setVisible(false);
					unVerifyDataBulkItem.setVisible(false);
					unPublishDataBulkItem.setVisible(false);

				}
			} else {
				verifyDataBulkItem.setVisible(false);
				publishDataBulkItem.setVisible(false);
				unVerifyDataBulkItem.setVisible(false);
				unPublishDataBulkItem.setVisible(false);

			}

		}

		provinceCombo.getStyle().set("padding-top", "0px");
		provinceCombo.setClassName("col-sm-6, col-xs-6");

		districtCombo.setLabel(I18nProperties.getCaption(Captions.district));
		districtCombo.getStyle().set("padding-top", "0px !important");
		districtCombo.setClassName("col-sm-6, col-xs-6");
		districtCombo.setPlaceholder(I18nProperties.getCaption(Captions.district));

		clusterCombo.setLabel(I18nProperties.getCaption(Captions.community));
		clusterCombo.getStyle().set("padding-top", "0px !important");
		clusterCombo.setPlaceholder(I18nProperties.getCaption(Captions.community));
		clusterCombo.setClassName("col-sm-6, col-xs-6");
		clusterCombo.getStyle().set("--vaadin-combo-box-overlay-width", "350px");

		verifiedStatusCombo.setLabel(I18nProperties.getCaption("Verified Status"));
		verifiedStatusCombo.getStyle().set("padding-top", "0px !important");
		verifiedStatusCombo.setPlaceholder(I18nProperties.getCaption("All Data"));
		verifiedStatusCombo.setClassName("col-sm-6, col-xs-6");
		verifiedStatusCombo.getStyle().set("--vaadin-combo-box-overlay-width", "350px");
		verifiedStatusCombo.setItems("All Data", "Verified", "Unverified");

		publishedStatusCombo.setLabel(I18nProperties.getCaption("Publish Status"));
		publishedStatusCombo.getStyle().set("padding-top", "0px !important");
		publishedStatusCombo.setPlaceholder(I18nProperties.getCaption("All Data"));
		publishedStatusCombo.setClassName("col-sm-6, col-xs-6");
		publishedStatusCombo.getStyle().set("--vaadin-combo-box-overlay-width", "350px");
		publishedStatusCombo.setItems("All Data", "Published", "Unpublished");

		Button transposeLqasButton = new Button("Transpose LQAS Data");

		transposeLqasButton.addClickListener(e -> {
			buildLqasTransposedData(criteria);
		});

		// Initialize Item lists
		List<CampaignReferenceDto> campaigns = FacadeProvider.getCampaignFacade().getAllCampaignByStartDate();
		CampaignReferenceDto lastStarted = FacadeProvider.getCampaignFacade().getLastStartedCampaign();
		List<String> camYearList = campaigns.stream().map(CampaignReferenceDto::getCampaignYear).distinct()
				.collect(Collectors.toList());

		campaignYear.setItems(camYearList);
		campaignYear.setItemLabelGenerator(item -> {

			switch (userProvider.getUser().getLanguage().toString()) {
			case "Pashto":
				arabicFormat = NumberFormat.getInstance(new Locale("ps"));
				return String.valueOf(arabicFormat.format(Long.parseLong(item))).replace(",", "");
			case "Dari":
				arabicFormat = NumberFormat.getInstance(new Locale("fa"));
				return String.valueOf(arabicFormat.format(Long.parseLong(item))).replace(",", "");
			default:
				arabicFormat = NumberFormat.getInstance(new Locale("en"));
				return String.valueOf(arabicFormat.format(Long.parseLong(item))).replace(",", "");
			}
		});
		campaignYear.setValue(lastStarted.getCampaignYear());

		List<CampaignReferenceDto> allCampaigns = campaigns.stream()
				.filter(c -> c.getCampaignYear().equals(campaignYear.getValue())).collect(Collectors.toList());

		campaignz.setItems(allCampaigns);
		campaignz.setValue(lastStarted);

		campaignPhase.setItemLabelGenerator(this::getLabelForEnum);
		if (userProvider.getUser().getUsertype() == UserType.EOC_USER) {
			campaignPhase.setItems(CampaignPhase.values());
			campaignPhase.setValue(CampaignPhase.INTRA);
		} else {
			campaignPhase.setItems(CampaignPhase.values());
			campaignPhase.setValue(CampaignPhase.PRE);
		}

		configureFormNameTranslations();

		campaignFormCombo.getStyle().set("--vaadin-combo-box-overlay-width", "350px");
		if (campaignForms.size() > 0) {
			campaignFormCombo.setValue(campaignForms.get(0));

		}

		criteria.campaign(lastStarted);
		criteria.setFormType(campaignPhase.getValue().toString());
		criteria.setCampaignFormMeta(campaignFormCombo.getValue());

		int numberOfRows = (int) FacadeProvider.getCampaignFormDataFacade().count(criteria);
		countRowItems = new Paragraph(I18nProperties.getCaption(Captions.rows) + numberOfRows);
		countRowItems.setId("rowCount");
		rightFloat.add(countRowItems);

		// Configure Comboboxes Value Change Listeners
		campaignYear.addValueChangeListener(e -> {
			campaignz.clear();
			List<CampaignReferenceDto> allCampaigns_ = campaigns.stream()
					.filter(c -> c.getCampaignYear().equals(campaignYear.getValue())).collect(Collectors.toList());
			campaignz.setItems(allCampaigns_);
			campaignz.setValue(allCampaigns_.get(0));
			importanceSwitcher.setReadOnly(false);

		});

		campaignz.addValueChangeListener(e -> {
			if (e.getValue() != null) {
				campaignFormCombo.clear();
				newForm.clear();
				importFormData.clear();
				configureFormNameTranslations();
				campaignFormCombo.setValue(campaignForms.get(0));
				exportFileName = e.getValue().toString() + "_"
						+ campaignFormCombo.getValue().toString().replaceAll("[^a-zA-Z0-9]+", " ") + "_"
						+ new SimpleDateFormat("yyyyddMM").format(Calendar.getInstance().getTime());
				exporter.setFileName(exportFileName);
				anchor.setHref(exporter.getCsvStreamResource());
				importanceSwitcher.clear();
				importanceSwitcher.setReadOnly(false);
				configureColumnStyles(criteria);
				reload();
				updateRowCount();

			}
		});

		campaignPhase.addValueChangeListener(e -> {

			System.out.println(e.getValue() + " form phase vaue ");
			importanceSwitcher.clear();
			importanceSwitcher.setReadOnly(false);
			if (e.getValue() != null) {

				if (e.getValue().toString().equalsIgnoreCase("post-campaign")) {
					verifiedStatusCombo.setVisible(true);
					publishedStatusCombo.setVisible(true);

//					System.out.println("post campaign selected ");

					if (userProvider.getUser().getUsertype() == UserType.WHO_USER) {
						if (userProvider.hasUserRight(UserRight.PERFORM_BULK_OPERATIONS)) {
							if (userProvider.getUser().getUserRoles().contains(UserRole.PUBLISH_USER)) {
								verifyDataBulkItem.setVisible(true);
								publishDataBulkItem.setVisible(true);
								unVerifyDataBulkItem.setVisible(true);
								unPublishDataBulkItem.setVisible(true);
								System.out.println("user is a publish user ");
							} else {
								verifyDataBulkItem.setVisible(false);
								publishDataBulkItem.setVisible(false);
								unVerifyDataBulkItem.setVisible(false);
								unPublishDataBulkItem.setVisible(false);
								System.out.println("user is NOT a publish user ");
							}
//							System.out.println("user ca n do bulk peration an is who  2");
						} else {
							verifyDataBulkItem.setVisible(false);
							publishDataBulkItem.setVisible(false);
							unVerifyDataBulkItem.setVisible(false);
							unPublishDataBulkItem.setVisible(false);
//							System.out.println("can either not don bvulk and  is  who   ");
						}

						verifiedColumn.setVisible(true);
						publishedColumn.setVisible(true);
						leaveBulkEdit.setVisible(false);
						bulkActionsItem.setVisible(false);
						enterBulkEdit.setVisible(true);
//						System.out.println("user ca n do bulk peration an is who  ");
					} else if (userProvider.getUser().getUsertype() == UserType.EOC_USER
							&& campaignPhase.getValue() != null
							&& userProvider.getUser().getUserRoles().contains(UserRole.EDITOR_USER)) {

						enterBulkEdit.setVisible(false);
					} else {
						verifiedStatusCombo.setVisible(false);
						publishedStatusCombo.setVisible(false);
						if (verifiedColumn != null || publishedColumn != null) {
							verifiedColumn.setVisible(false);
							publishedColumn.setVisible(false);
						}
						verifyDataBulkItem.setVisible(false);
						publishDataBulkItem.setVisible(false);
						unVerifyDataBulkItem.setVisible(false);
						unPublishDataBulkItem.setVisible(false);
						leaveBulkEdit.setVisible(false);
						bulkActionsItem.setVisible(false);
						enterBulkEdit.setVisible(true);
//						System.out.println("can either not don bvulk or is not who   ");
					}

				} else {
					verifiedStatusCombo.setVisible(false);
					publishedStatusCombo.setVisible(false);
					if (verifiedColumn != null || publishedColumn != null) {
						verifiedColumn.setVisible(false);
						publishedColumn.setVisible(false);
					}
//					verifiedColumn.setVisible(false);
//					publishedColumn.setVisible(false);
					verifyDataBulkItem.setVisible(false);
					publishDataBulkItem.setVisible(false);
					unVerifyDataBulkItem.setVisible(false);
					unPublishDataBulkItem.setVisible(false);
					leaveBulkEdit.setVisible(false);
					bulkActionsItem.setVisible(false);
					enterBulkEdit.setVisible(true);
//					System.out.println("non - post campaign selected ");

				}

			}

			campaignFormCombo.clear();
			newForm.clear();
			importFormData.clear();
			configureFormNameTranslations();
			campaignFormCombo.setValue(campaignForms.get(0));
			remove(grid);
			configureGrid(criteria);
			configureColumnStyles(criteria);
			updateRowCount();

		});

		campaignFormCombo.addValueChangeListener(e -> {
			if (e.getValue() != null) {
				formMetaReference = FacadeProvider.getCampaignFormMetaFacade()
						.getCampaignFormMetaByUuid(e.getValue().getUuid());

				exportFileName = campaignz.getValue().toString() + "_"
						+ campaignFormCombo.getValue().toString().replaceAll("[^a-zA-Z0-9]+", " ") + "_"
						+ new SimpleDateFormat("yyyyddMM").format(Calendar.getInstance().getTime());
				exporter.setFileName(exportFileName);
				anchor.setHref(exporter.getCsvStreamResource());
				importanceSwitcher.clear();
				importanceSwitcher.setReadOnly(false);

				reload();
				configureColumnStyles(criteria);

				// Remove the exportTransposedDataButton and transposdeDataAnchor if they exist
				if (actionButtonlayout.getChildren()
						.anyMatch(component -> component.equals(exportTransposedDataButton))) {
					actionButtonlayout.remove(exportTransposedDataButton);
				}
				if (actionButtonlayout.getChildren().anyMatch(component -> component.equals(transposdeDataAnchor))) {
					actionButtonlayout.remove(transposdeDataAnchor);
					actionButtonlayout.remove(transposdeDataDictionaryAnchor);
				}

				// Check if the value contains "Day 1" and add new components
				if (e.getValue().toString().contains("Day 1") || e.getValue().toString().contains("LQAS")) {
					generateTransposeDataFunctions(actionButtonlayout, campaignFormCombo.getValue().toString(),
							criteria);
				}

				checkIfActiveFormContainsImageField(campaignFormCombo.getValue().getUuid());
			} else {
				importanceSwitcher.clear();
				importanceSwitcher.setReadOnly(true);
			}
			updateRowCount();
			configureColumnStyles(criteria);
		});

		regionCombo.setClearButtonVisible(true);
		regionCombo.addValueChangeListener(e -> {
			if (e.getValue() != null) {
				List<RegionReferenceDto> allProvinces = new ArrayList<>();
				if (userProvider.getUser().getLanguage().toString().equals("Pashto")) {
					for (AreaReferenceDto selectedUUID : e.getValue()) {
						String uuid = selectedUUID.getUuid();
						provinces = FacadeProvider.getRegionFacade().getAllActiveByAreaPashto(uuid);
						allProvinces.addAll(provinces);
					}
//						provinces = FacadeProvider.getRegionFacade().getAllActiveByAreaPashto(e.getValue().getUuid());
					provinceCombo.setItems(allProvinces);
				} else if (userProvider.getUser().getLanguage().toString().equals("Dari")) {
					for (AreaReferenceDto selectedUUID : e.getValue()) {
						String uuid = selectedUUID.getUuid();
						provinces = FacadeProvider.getRegionFacade().getAllActiveByAreaDari(uuid);
						allProvinces.addAll(provinces);
					}
//						provinces = FacadeProvider.getRegionFacade().getAllActiveByAreaDari(e.getValue().getUuid());
					provinceCombo.setItems(allProvinces);
				} else {
					for (AreaReferenceDto selectedUUID : e.getValue()) {
						String uuid = selectedUUID.getUuid();
						provinces = FacadeProvider.getRegionFacade().getAllActiveByArea(uuid);
						allProvinces.addAll(provinces);
					}
//						provinces = FacadeProvider.getRegionFacade().getAllActiveByArea(e.getValue().getUuid());
					provinceCombo.setItems(allProvinces);
				}
				provinceCombo.setEnabled(true);
			} else {
				if (provinceCombo.getValue() != null) {
					provinceCombo.clear();
				}
				provinceCombo.setEnabled(false);
			}
			reload();
			updateRowCount();

			if (e.getValue() != null) {
				if (campaignFormCombo.getValue() != null && (campaignFormCombo.getValue().toString().contains("Day 1")
						|| campaignFormCombo.getValue().toString().contains("LQAS"))) {
					generateTransposeDataFunctions(actionButtonlayout, campaignFormCombo.getValue().toString(),
							criteria);
				}
			}
		});

		provinceCombo.setClearButtonVisible(true);

		provinceCombo.addValueChangeListener(e -> {
			if (e.getValue() != null) {
				List<DistrictReferenceDto> allDistricts = new ArrayList<>();
				List<DistrictReferenceDto> userDistrictsz = new ArrayList<>();

				if (userProvider.getUser().getLanguage().toString().equals("Pashto")) {
					for (RegionReferenceDto selectedUUID : e.getValue()) {
						String uuid = selectedUUID.getUuid();
						districts = FacadeProvider.getDistrictFacade().getAllActiveByRegionPashto(uuid);
						if (userProvider.getUser().getUserRoles().contains(UserRole.SURVEILLANCE_OFFICER)) {
							if (userProvider.getUser().getDistricts() != null
									&& !userProvider.getUser().getDistricts().isEmpty()) {
								// if the user selected district is amonths the active distgricts add them
								for (DistrictReferenceDto userDistrict : userProvider.getUser().getDistricts()) {
									if (districts.contains(userDistrict)) {
										userDistrictsz.add(userDistrict);
									}
								}
								allDistricts.addAll(userDistrictsz);
							}
						} else if (!userProvider.getUser().getUserRoles().contains(UserRole.SURVEILLANCE_OFFICER)
								&& userProvider.getUser().getDistrict() != null) {
							DistrictReferenceDto userDistrict = userProvider.getUser().getDistrict();
							if (districts.contains(userDistrict)) {
								userDistrictsz.add(userDistrict);
							}
							allDistricts.addAll(userDistrictsz);
						} else {
							allDistricts.addAll(districts);
						}
					}
					districtCombo.setItems(allDistricts);
				} else if (userProvider.getUser().getLanguage().toString().equals("Dari")) {
					for (RegionReferenceDto selectedUUID : e.getValue()) {
						String uuid = selectedUUID.getUuid();
						districts = FacadeProvider.getDistrictFacade().getAllActiveByRegionDari(uuid);
						if (userProvider.getUser().getUserRoles().contains(UserRole.SURVEILLANCE_OFFICER)) {
							if (userProvider.getUser().getDistricts() != null
									&& !userProvider.getUser().getDistricts().isEmpty()) {
								// if the user selected district is amonths the active distgricts add them
								for (DistrictReferenceDto userDistrict : userProvider.getUser().getDistricts()) {
									if (districts.contains(userDistrict)) {
										userDistrictsz.add(userDistrict);
									}
								}
								allDistricts.addAll(userDistrictsz);
							}
						} else if (!userProvider.getUser().getUserRoles().contains(UserRole.SURVEILLANCE_OFFICER)
								&& userProvider.getUser().getDistrict() != null) {
							DistrictReferenceDto userDistrict = userProvider.getUser().getDistrict();
							if (districts.contains(userDistrict)) {
								userDistrictsz.add(userDistrict);
							}
							allDistricts.addAll(userDistrictsz);
						} else {
							allDistricts.addAll(districts);
						}
					}
					districtCombo.setItems(allDistricts);
				} else {
					for (RegionReferenceDto selectedUUID : e.getValue()) {
						String uuid = selectedUUID.getUuid();
						districts = FacadeProvider.getDistrictFacade().getAllActiveByRegion(uuid);
						if (userProvider.getUser().getUserRoles().contains(UserRole.SURVEILLANCE_OFFICER)) {
							if (userProvider.getUser().getDistricts() != null
									&& !userProvider.getUser().getDistricts().isEmpty()) {
								// if the user selected district is amonths the active distgricts add them
								for (DistrictReferenceDto userDistrict : userProvider.getUser().getDistricts()) {
									if (districts.contains(userDistrict)) {
										userDistrictsz.add(userDistrict);
									}
								}
								allDistricts.addAll(userDistrictsz);
							}
						} else if (!userProvider.getUser().getUserRoles().contains(UserRole.SURVEILLANCE_OFFICER)
								&& userProvider.getUser().getDistrict() != null) {
							DistrictReferenceDto userDistrict = userProvider.getUser().getDistrict();
							if (districts.contains(userDistrict)) {
								userDistrictsz.add(userDistrict);
							}
							allDistricts.addAll(userDistrictsz);
						} else {
							allDistricts.addAll(districts);
						}
					}
					districtCombo.setItems(allDistricts);
				}
				districtCombo.setEnabled(true);
			} else {
				if (districtCombo.getValue() != null) {
					districtCombo.clear();
				}
				districtCombo.setEnabled(false);
			}
			reload();
			updateRowCount();

			if (e.getValue() != null) {
				if (campaignFormCombo.getValue() != null && (campaignFormCombo.getValue().toString().contains("Day 1")
						|| campaignFormCombo.getValue().toString().contains("LQAS"))) {
					generateTransposeDataFunctions(actionButtonlayout, campaignFormCombo.getValue().toString(),
							criteria);
				}
			}
		});

		districtCombo.setClearButtonVisible(true);

		districtCombo.addValueChangeListener(e -> {
			if (e.getValue() != null) {
				List<CommunityReferenceDto> allClusters = new ArrayList<>();

				for (DistrictReferenceDto selectedUUID : e.getValue()) {
					String uuid = selectedUUID.getUuid();
					communities = FacadeProvider.getCommunityFacade().getAllActiveByDistrict(uuid);
					allClusters.addAll(communities);
				}

//				communities = FacadeProvider.getCommunityFacade().getAllActiveByDistrict(e.getValue().getUuid());
				clusterCombo.setItemLabelGenerator(itm -> {
					CommunityReferenceDto dcfv = (CommunityReferenceDto) itm;
					return dcfv.getNumber() + " | " + dcfv.getCaption();
				});
				allClusters.sort(Comparator.comparing(CommunityReferenceDto::getNumber));
				clusterCombo.setItems(allClusters);

				clusterCombo.setEnabled(true);

			} else {
				if (clusterCombo.getValue() != null) {
					clusterCombo.clear();
				}
				clusterCombo.setEnabled(false);
			}

			reload();
			updateRowCount();

			if (e.getValue() != null) {
				if (campaignFormCombo.getValue() != null && (campaignFormCombo.getValue().toString().contains("Day 1")
						|| campaignFormCombo.getValue().toString().contains("LQAS"))) {
					generateTransposeDataFunctions(actionButtonlayout, campaignFormCombo.getValue().toString(),
							criteria);
				}
			}
		});

		clusterCombo.setClearButtonVisible(true);
		clusterCombo.addValueChangeListener(e -> {
			reload();
			updateRowCount();

			if (campaignFormCombo.getValue() != null && (campaignFormCombo.getValue().toString().contains("Day 1")
					|| campaignFormCombo.getValue().toString().contains("LQAS"))) {
				generateTransposeDataFunctions(actionButtonlayout, campaignFormCombo.getValue().toString(), criteria);
			}

		});

		newForm.addValueChangeListener(e -> {
			if (e.getValue() != null && campaignz != null) {
				boolean isCampaignClosed = FacadeProvider.getCampaignFacade().isClosedd(campaignz.getValue().getUuid());

				if (!isCampaignClosed) {

					if (campaignPhase.getValue() != null) {

						if (FacadeProvider.getCampaignFormMetaWithExpFacade()
								.getFormsWithExpiryByCampaignUuidAndFormUuid(campaignz.getValue().getUuid(),
										e.getValue().getUuid())
								.size() > 0) {

							CampaignFormMetaExpiryDto expiryDto = FacadeProvider.getCampaignFormMetaWithExpFacade()
									.getFormsWithExpiryByCampaignUuidAndFormUuid(campaignz.getValue().getUuid(),
											e.getValue().getUuid())
									.get(0);

							CampaignDto campaign = FacadeProvider.getCampaignFacade()
									.getByUuid(campaignz.getValue().getUuid());

							if (checkFormValidityByPhase(campaignPhase.getValue().toString().toLowerCase(), campaign,
									expiryDto)) {
								CampaignFormMetaDto formDatax = FacadeProvider.getCampaignFormMetaFacade()
										.getCampaignFormMetaByUuid(e.getValue().getUuid());

								CampaignFormMetaGeographyLevel formGeographyLevel = formDatax.getGeographyLevel();

								List districtuuids = new ArrayList<>();

								if (userProvider.getUser().getDistrict() != null) {
									districtuuids.add(userProvider.getUser().getDistrict().getUuid());
									popDto = FacadeProvider.getPopulationDataFacade()
											.fetchPopulationDataSelectionByUserDistricts(districtuuids);

								} else if (userProvider.getUser().getDistricts().size() > 0) {
									for (DistrictReferenceDto districts : userProvider.getUser().getDistricts()) {
										districtuuids.add(districts.getUuid());
									}
									popDto = FacadeProvider.getPopulationDataFacade()
											.fetchPopulationDataSelectionByUserDistricts(districtuuids);

								} else {
									popDto = FacadeProvider.getPopulationDataFacade()
											.fetchPopulationDataSelectionByCampaign(campaignz.getValue().getUuid());
								}

								CampaignFormDataEditForm cam = new CampaignFormDataEditForm(e.getValue(),
										campaignz.getValue(), false, null, grid, formGeographyLevel, campaign,
										expiryDto, popDto);
								// add(cam);

								newForm.setValue(null);
							}
						} else {
							Notification notification = new Notification();
							notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
							notification.setPosition(Position.TOP_END);
							Div text = new Div(new Text(
									"This Form is not available for Data Entry. Please contact System Administrator to Specify Entry Period."),
									new HtmlComponent("br"),
									new Text("Close this warning to continue working in APMIS."));

							Button closeButton = new Button(new Icon("lumo", "cross"));
							closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
							closeButton.addClickListener(event -> {
								notification.close();
							});

							HorizontalLayout layoutx = new HorizontalLayout(text, closeButton);
							layoutx.setAlignItems(Alignment.CENTER);

							notification.add(layoutx);
							notification.open();

						}
					}

				} else {
					Notification notification = new Notification();
					notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
					notification.setPosition(Position.TOP_END);
					Div text = new Div(new Text(
							"This Campaign has been Closed for Data Entry. Please contact System Administrator."),
							new HtmlComponent("br"), new Text("Close this warning to continue working in APMIS."));

					Button closeButton = new Button(new Icon("lumo", "cross"));
					closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
					closeButton.addClickListener(event -> {
						notification.close();
					});

					HorizontalLayout layoutx = new HorizontalLayout(text, closeButton);
					layoutx.setAlignItems(Alignment.CENTER);

					notification.add(layoutx);
					notification.open();
				}

			}
		});

		importFormData.addValueChangeListener(e -> {
			CampaignDto campaignUuid = FacadeProvider.getCampaignFacade().getByUuid(campaignz.getValue().getUuid());
			boolean isCampaignClosed = FacadeProvider.getCampaignFacade().isClosedd(campaignz.getValue().getUuid());

			if (!isCampaignClosed) {
				if (importFormData.getValue() != null) {

					if (e.getValue() != null) {

						if (FacadeProvider.getCampaignFormMetaWithExpFacade()
								.getFormsWithExpiryByCampaignUuidAndFormUuid(campaignUuid.getUuid(),
										e.getValue().getUuid())
								.size() > 0) {

							CampaignFormMetaExpiryDto expiryDto = FacadeProvider.getCampaignFormMetaWithExpFacade()
									.getFormsWithExpiryByCampaignUuidAndFormUuid(campaignUuid.getUuid(),
											e.getValue().getUuid())
									.get(0);

							if (checkFormValidityByPhase(e.getValue().getFormType().toString().toLowerCase(),
									campaignUuid, expiryDto)) {
								ImportCampaignsFormDataDialog dialogx = new ImportCampaignsFormDataDialog(
										campaignz.getValue(), importFormData.getValue(), campaignUuid);
								dialogx.open();

							}

						} else {
							Notification notification = new Notification();
							notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
							notification.setPosition(Position.TOP_END);
							Div text = new Div(new Text(
									"This Form is not available for Data Import. Please contact System Administrator to Specify Entry Period."),
									new HtmlComponent("br"),
									new Text("Close this warning to continue working in APMIS."));

							Button closeButton = new Button(new Icon("lumo", "cross"));
							closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
//				closeButton.setAriaLabel("Close");
							closeButton.addClickListener(event -> {
								notification.close();
							});

							HorizontalLayout layoutx = new HorizontalLayout(text, closeButton);
							layoutx.setAlignItems(Alignment.CENTER);

							notification.add(layoutx);
							notification.open();

						}

					}

				}
			} else {
				Notification notification = new Notification();
				notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
				notification.setPosition(Position.TOP_END);
				Div text = new Div(
						new Text("This Campaign has been Closed for Data Import. Please contact System Administrator."),
						new HtmlComponent("br"), new Text("Close this warning to continue working in APMIS."));

				Button closeButton = new Button(new Icon("lumo", "cross"));
				closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
//		closeButton.setAriaLabel("Close");
				closeButton.addClickListener(event -> {
					notification.close();
				});

				HorizontalLayout layoutx = new HorizontalLayout(text, closeButton);
				layoutx.setAlignItems(Alignment.CENTER);

				notification.add(layoutx);
				notification.open();
			}
		});

		// TODO Importance filter switcher should be visible only on the change of form

		importanceSwitcher.setPlaceholder(I18nProperties.getCaption(Captions.importance));
		importanceSwitcher.setItems(CampaignFormElementImportance.values());
		importanceSwitcher.getStyle().set("padding-top", "0px !important");
		importanceSwitcher.setClassName("col-sm-6, col-xs-6");
		importanceSwitcher.setClearButtonVisible(true);
		importanceSwitcher.setTooltipText(I18nProperties.getDescription(Descriptions.campaign_importance));
		importanceSwitcher.addValueChangeListener(e -> {

			formMetaReference = FacadeProvider.getCampaignFormMetaFacade()
					.getCampaignFormMetaByUuid(campaignFormCombo.getValue().getUuid());

			reload();
			if (formMetaReference != null) {
				remove(grid);

				configureGrid(criteria);

				final boolean allAndImportantFormElements = e.getValue() == CampaignFormElementImportance.ALL;
				final boolean onlyImportantFormElements = e.getValue() == CampaignFormElementImportance.IMPORTANT;

				final List<CampaignFormElement> campaignFormElements = formMetaReference.getCampaignFormElements();

				for (CampaignFormElement element : campaignFormElements) {
					List<MapperUtil> options = new ArrayList<MapperUtil>();
					if (element.getOptions() != null) {
						options = element.getOptions();
					}
					String fieldsType = element.getType();

					if (element.isImportant() && onlyImportantFormElements) {
						String caption = null;
						if (caption == null) {
							caption = element.getCaption();
						}

						if (caption != null) {
							addCustomColumn(element.getId(), caption, fieldsType, options);
						}
					} else if (allAndImportantFormElements) {
						String caption = null;
						if (caption == null) {
							caption = element.getCaption();
						}
						if (caption != null) {
							addCustomColumn(element.getId(), caption, fieldsType, options);
						}
					}
				}
			}

			configureColumnStyles(criteria);

			if (leaveBulkEdit.isVisible()) {
				leaveBulkEdit.setVisible(false);
//				bulkActionsItem.setVisible(false);
				dropdownBulkOperations.setVisible(false);

				enterBulkEdit.setVisible(true);
			}

//			if (isGridInMultiselectMode) {
//				configureGridMultiSelect();
//			}

		});

		verifiedStatusCombo.addValueChangeListener(e -> {
			if (e.getValue() != null) {
				if (e.getValue().equalsIgnoreCase("unverified")) {
					criteria.setIsVerified(false);

				} else if (e.getValue().equalsIgnoreCase("verified")) {
					criteria.setIsVerified(true);

				} else {
					criteria.setIsVerified(null);

				}

			} else {
				criteria.setIsVerified(null);
			}

			reload();
			updateRowCount();
		});

		publishedStatusCombo.addValueChangeListener(e -> {
			if (e.getValue() != null) {
				if (e.getValue().equalsIgnoreCase("unpublished")) {
					criteria.setIsPublished(false);

				} else if (e.getValue().equalsIgnoreCase("published")) {
					criteria.setIsPublished(true);

				} else {
					criteria.setIsPublished(null);

				}

			} else {
				criteria.setIsPublished(null);
			}

			reload();
			updateRowCount();
		});

		enterBulkEdit.getStyle().set("margin-top", "5px");

		enterBulkEdit.addClassName("bulkActionButton");
		Icon bulkModeButtonnIcon = new Icon(VaadinIcon.CLIPBOARD_CHECK);
		enterBulkEdit.setIcon(bulkModeButtonnIcon);

		if (userProvider.hasUserRight(UserRight.PERFORM_BULK_OPERATIONS)) {

			actionButtonlayout.add(enterBulkEdit);

		}

		enterBulkEdit.addClickListener(e -> {
			isGridInMultiselectMode = true;
			dropdownBulkOperations.setVisible(true);
			configureGridMultiSelect();
			enterBulkEdit.setVisible(false);
			leaveBulkEdit.setVisible(true);
			bulkActionsItem.setVisible(true);

		});

		selectAllButtonpLACEHOLDER.addClickListener(e -> {
			selectAllButton.click();
		});

		leaveBulkEdit.setText(I18nProperties.getCaption(Captions.actionLeaveBulkEditMode));
		leaveBulkEdit.addClassName("leaveBulkActionButton");
		leaveBulkEdit.setVisible(false);
		Icon leaveBulkModeButtonnIcon = new Icon(VaadinIcon.CLIPBOARD_CHECK);
		leaveBulkEdit.setIcon(leaveBulkModeButtonnIcon);
		actionButtonlayout.add(leaveBulkEdit);

		leaveBulkEdit.addClickListener(e -> {
			grid.setSelectionMode(Grid.SelectionMode.SINGLE);
			enterBulkEdit.setVisible(true);
			leaveBulkEdit.setVisible(false);
			selectAllButtonpLACEHOLDER.setVisible(false);

			dropdownBulkOperations.setVisible(false);
		});
		dropdownBulkOperations.setVisible(false);
		actionButtonlayout.add(dropdownBulkOperations);
		selectAllButtonpLACEHOLDER.setText("Select All");
		selectAllButtonpLACEHOLDER.setVisible(false);
		actionButtonlayout.add(selectAllButtonpLACEHOLDER);

		resetHandler.setText(I18nProperties.getCaption(Captions.resetFilters));
		resetHandler.addClickListener(e -> {
			UI.getCurrent().getPage().reload();
			criteria.campaign(lastStarted);
			criteria.setFormType(campaignPhase.getValue().toString());
			updateRowCount();
		});

		layout.add(displayFilters, actionButtonlayout);

		filterBlock.add(layout, level1Filters);

		add(filterBlock);
	}

	private StreamResource createZipExportResource() {
		return new StreamResource(exportFileName + ".zip", () -> {
			try {
				List<CampaignFormDataIndexDto> exportRows = fetchExportRows();
				exportImageFileNames = buildExportImageFileNames(exportRows);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try (ZipOutputStream zos = new ZipOutputStream(baos)) {

					zos.putNextEntry(new ZipEntry(exportFileName + ".csv"));
					zos.write(readCsvBytes());
					zos.closeEntry();

					for (Map.Entry<String, byte[]> img : collectImagesFromGrid(exportRows).entrySet()) {
						zos.putNextEntry(new ZipEntry("images/" + img.getKey()));
						zos.write(img.getValue());
						zos.closeEntry();
					}
				}
				return new ByteArrayInputStream(baos.toByteArray());
			} catch (IOException e) {
				logger.error("Failed to generate ZIP export", e);
				return new ByteArrayInputStream(new byte[0]);
			}
		});
	}

	private byte[] readCsvBytes() throws IOException {
		try (InputStream in = exporter.getCsvInputStream()) {
			return in.readAllBytes();
		}
	}

	private Map<String, byte[]> collectImagesFromGrid(List<CampaignFormDataIndexDto> exportRows) {
		Map<String, byte[]> images = new LinkedHashMap<>();

		for (CampaignFormDataIndexDto item : exportRows) {
			if (item.getFormValues() == null) {
				continue;
			}
			for (CampaignFormDataEntry entry : item.getFormValues()) {
				for (CampaignFormImageValue img : extractImageValues(entry.getValue())) {
					if (!img.isUploaded() || StringUtils.isBlank(img.getImageId())) {
						continue;
					}
					String exportName = exportImageFileName(img, entry.getId());
					if (images.containsKey(exportName)) {
						continue;
					}
					try {
						images.put(exportName, FacadeProvider.getCampaignFormImageFacade().readImage(img.getImageId()));
					} catch (IOException e) {
						logger.error("Could not read image " + img.getImageId(), e);
					}
				}
			}
		}
		return images;
	}

	/**
	 * Fetches the rows in the very same order the grid exporter writes them, so the
	 * row numbers stamped on the exported images line up with the exported CSV.
	 */
	private List<CampaignFormDataIndexDto> fetchExportRows() {
		Query<CampaignFormDataIndexDto, CampaignFormDataCriteria> query = new Query<>(0,
				dataProvider.size(new Query<>()), grid.getDataCommunicator().getBackEndSorting(),
				grid.getDataCommunicator().getInMemorySorting(), null);
		return dataProvider.fetch(query).collect(Collectors.toList());
	}

	/**
	 * Maps every exported image to a unique file name following the geographic
	 * naming convention (region_province_district_clusterNumber_clusterName, see
	 * the image upload naming) extended with the image field id and the 1-based
	 * export row number, so images can be matched to their CSV row and to the field
	 * they belong to. A trailing counter is added only when a field holds several
	 * images for the same row. The CSV image column uses this exact same name, so
	 * it maps directly onto the ZIP entry.
	 */
	private Map<String, String> buildExportImageFileNames(List<CampaignFormDataIndexDto> exportRows) {
		Map<String, String> fileNames = new LinkedHashMap<>();
		Map<String, String> geoBaseNames = new HashMap<>();
		int rowNumber = 0;

		for (CampaignFormDataIndexDto item : exportRows) {
			rowNumber++;
			Map<String, Integer> occurrences = new HashMap<>();
			if (item.getFormValues() == null) {
				continue;
			}
			for (CampaignFormDataEntry entry : item.getFormValues()) {
				String fieldId = sanitizeFileSegment(entry.getId());
				for (CampaignFormImageValue img : extractImageValues(entry.getValue())) {
					if (!img.isUploaded() || StringUtils.isBlank(img.getImageId())) {
						continue;
					}
					String key = imageExportKey(img.getImageId(), fieldId);
					if (fileNames.containsKey(key)) {
						continue;
					}
					String geoBaseName = geoBaseNames.computeIfAbsent(geoKey(item), k -> imageGeoBaseName(item));
					StringBuilder name = new StringBuilder(geoBaseName).append('_').append(fieldId).append('_')
							.append(rowNumber);
					int occurrence = occurrences.merge(fieldId, 1, Integer::sum);
					if (occurrence > 1) {
						name.append('_').append(occurrence);
					}
					fileNames.put(key, name.append('.').append(imageExtension(img)).toString());
				}
			}
		}
		return fileNames;
	}

	private String exportImageFileName(CampaignFormImageValue img, String fieldId) {
		if (exportImageFileNames != null && StringUtils.isNotBlank(img.getImageId())) {
			String mapped = exportImageFileNames.get(imageExportKey(img.getImageId(), sanitizeFileSegment(fieldId)));
			if (StringUtils.isNotBlank(mapped)) {
				return mapped;
			}
		}
		return imageDisplayName(img);
	}

	private String imageExportKey(String imageId, String fieldId) {
		return imageId + "|" + fieldId;
	}

	private String geoKey(CampaignFormDataIndexDto item) {
		return String.join("|", StringUtils.defaultString(item.getArea()), StringUtils.defaultString(item.getRegion()),
				StringUtils.defaultString(item.getDistrict()),
				item.getClusternumber() == null ? "" : String.valueOf(item.getClusternumber()),
				StringUtils.defaultString(item.getCommunity()));
	}

	/**
	 * Builds the geographic part of the file name using the shared image naming
	 * convention.
	 */
	private String imageGeoBaseName(CampaignFormDataIndexDto item) {
		CampaignFormImageNamingContext context = new CampaignFormImageNamingContext();
		context.setRegion(item.getArea());
		context.setProvince(item.getRegion());
		context.setDistrict(item.getDistrict());
		context.setClusterName(item.getCommunity());
		context.setClusterNumber(item.getClusternumber() == null ? null : String.valueOf(item.getClusternumber()));
		return FacadeProvider.getCampaignFormImageFacade().generateImageFileName(context);
	}

	private String imageExtension(CampaignFormImageValue img) {
		for (String candidate : new String[] { img.getGeneratedFileName(), img.getOriginalFileName() }) {
			if (StringUtils.isNotBlank(candidate)) {
				int dot = candidate.lastIndexOf('.');
				if (dot >= 0 && dot < candidate.length() - 1) {
					String extension = candidate.substring(dot + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]",
							"");
					if (StringUtils.isNotBlank(extension)) {
						return "jpeg".equals(extension) ? "jpg" : extension;
					}
				}
			}
		}
		String mimeType = img.getMimeType();
		if (StringUtils.isNotBlank(mimeType) && mimeType.trim().toLowerCase(Locale.ROOT).startsWith("image/")) {
			String extension = mimeType.trim().toLowerCase(Locale.ROOT).substring("image/".length());
			if ("jpeg".equals(extension)) {
				return "jpg";
			}
			if (StringUtils.isNotBlank(extension)) {
				return extension;
			}
		}
		return "jpg";
	}

	private String sanitizeFileSegment(String input) {
		if (StringUtils.isBlank(input)) {
			return "NA";
		}
		String normalized = input.trim().replaceAll("\\s+", "-").replaceAll("[^A-Za-z0-9\\-]", "");
		normalized = normalized.replaceAll("-+", "-");
		normalized = StringUtils.strip(normalized, "-");
		return StringUtils.isBlank(normalized) ? "NA" : normalized;
	}

	public void buildLqasTransposedData(CampaignFormDataCriteria criteria) {
//		FacadeProvider.getCampaignFormDataFacade().getTransposedCampaignFormDataLQAS(criteria);
	};

	private boolean checkFormValidityByPhase(String campaignPhase, CampaignDto campaignDto,
			CampaignFormMetaExpiryDto expiryDto) {
		String phase = campaignPhase;
		Date today = new Date();

		// TODO Check if the current date is before the start date

		switch (phase.toLowerCase()) {
		case "pre-campaign":
			if (today.before(campaignDto.getPreCampStartDate())) {
				showErrorNotification("This Form is not yet open for Data Entry. Please contact System Administrator.");
				return false;
			}

			break;

		case "intra-campaign":
			if (today.before(campaignDto.getStartDate())) {
				showErrorNotification("This Form is not yet open for Data Entry. Please contact System Administrator.");
				return false;
			}
			break;

		case "post-campaign":
			if (today.before(campaignDto.getPostCampStartDate())) {
				showErrorNotification("This Form is not yet open for Data Entry. Please contact System Administrator.");
				return false;
			}
			break;

		default:
			// Optional: handle unexpected phases
			break;
		}

		// 🔹 Check if form expired
		if (expiryDto.getEnddate() != null && expiryDto.getEnddate().before(today)) {
			showErrorNotification("This Form has been Closed for Data Entry. Please contact System Administrator.");
			return false;
		}

		return true;
	}

	private void showErrorNotification(String message) {
		Notification notification = new Notification();
		notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
		notification.setPosition(Notification.Position.TOP_END);

		Div text = new Div(new Text(message), new HtmlComponent("br"),
				new Text("Close this warning to continue working in APMIS."));

		Button closeButton = new Button(new Icon("lumo", "cross"));
		closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
		closeButton.addClickListener(event -> notification.close());

		HorizontalLayout layout = new HorizontalLayout(text, closeButton);
		layout.setAlignItems(Alignment.CENTER);

		notification.add(layout);
		notification.open();
	}

	private void configureGridMultiSelect() {
		selectionModel = (GridMultiSelectionModel<CampaignFormDataIndexDto>) grid
				.setSelectionMode(Grid.SelectionMode.MULTI);

		selectionModel.setSelectAllCheckboxVisibility(GridMultiSelectionModel.SelectAllCheckboxVisibility.VISIBLE);

		selectionModel.addSelectionListener(event -> {
			if (event.getAllSelectedItems().isEmpty()) {
				selectedItems.clear();
			} else if (event.getAllSelectedItems().size() == getDataProviderSize()) {
				selectedItems.addAll(fetchAllItems());
			}
			grid.getDataProvider().refreshAll();
		});

		ComponentRenderer<Checkbox, CampaignFormDataIndexDto> checkboxRenderer = new ComponentRenderer<>(item -> {
			Checkbox checkboxx = new Checkbox();
			checkboxx.setValue(grid.getSelectedItems().contains(item)); // Set the initial value
			checkboxx.addValueChangeListener(event -> {
				if (event.getValue()) {
					grid.select(item);
				} else {
					grid.deselect(item);
				}
			});
			return checkboxx;
		});

		grid.addColumn(checkboxRenderer).setHeader(selectAllButton).setSortable(false).setResizable(true)
				.setAutoWidth(true).setVisible(false);
	}

	private int getDataProviderSize() {
		return dataProvider.size(new Query<>());
	}

	private Set<CampaignFormDataIndexDto> fetchAllItems() {
		Stream<CampaignFormDataIndexDto> stream = dataProvider.fetch(new Query<>());
		Set<CampaignFormDataIndexDto> allItems = new HashSet<>();
		stream.forEach(allItems::add);
		return allItems;
	}

	// Method to get currently selected items
	public Set<CampaignFormDataIndexDto> getSelectedItems() {

		return new HashSet<>(selectedItems);
	}

	public void generateTransposeDataFunctions(HorizontalLayout actionButionLayout, String formName,
			CampaignFormDataCriteria criteria) {

		if (exportTransposedDataButton.isVisible()) {
			actionButionLayout.remove(exportTransposedDataButton, transposdeDataAnchor);
		}

		transposdeDataAnchor = new Anchor("", "TransposeDaywiseData");
		transposdeDataAnchor.getStyle().set("display", "none");

		transposdeDataDictionaryAnchor = new Anchor("", "TransposeDaywiseData");
		transposdeDataDictionaryAnchor.getStyle().set("display", "none");

		exportTransposedDataButton = new Button("Export Transposed Data");

		if (transposdeDataAnchor.getElement().getAttribute("href") != "") {
			transposdeDataAnchor.setHref("");
			transposdeDataDictionaryAnchor.setHref("");
		}

		CampaignFormDataCriteria transposedDataCriteria = new CampaignFormDataCriteria();
		transposedDataCriteria = criteria;

		actionButionLayout.add(exportTransposedDataButton, transposdeDataAnchor, transposdeDataDictionaryAnchor);

		if (formName.toString().contains("Day 1")) {

			exportTransposedDataButton.addClickListener(e -> {
				DownloadTransposedDaywiseDataUtility downloadTransposedDaywiseICMDataUtility = new DownloadTransposedDaywiseDataUtility();
				transposdeDataAnchor.setHref(downloadTransposedDaywiseICMDataUtility
						.createTransposedDataFromIndexList(criteria, formName, campaignz.getValue().toString()));
				transposdeDataDictionaryAnchor
						.setHref(downloadTransposedDaywiseICMDataUtility.createTransposedDataFormExpressions(criteria));

				transposdeDataAnchor.getElement().setAttribute("download", true);
				transposdeDataAnchor.getElement().callJsFunction("click");
			});

		} else if (formName.toString().contains("LQAS")) {
			exportTransposedDataButton.addClickListener(e -> {
				DownloadTransposedLqasDataUtility downloadTransposedLqasDaywiseDataUtility = new DownloadTransposedLqasDataUtility();
				transposdeDataAnchor.setHref(downloadTransposedLqasDaywiseDataUtility
						.createTransposedLqasDataFromIndexList(criteria, formName, campaignz.getValue().toString()));
				transposdeDataDictionaryAnchor.setHref(
						downloadTransposedLqasDaywiseDataUtility.createTransposedDataFormExpressions(criteria));

				transposdeDataAnchor.getElement().setAttribute("download", true);
				transposdeDataAnchor.getElement().callJsFunction("click");
			});

		}
	}

	private boolean checkIfActiveFormContainsImageField(String campaignFormMetaUuid) {
		if (campaignFormMetaUuid == null) {
			return false;
		}

		List<CampaignFormElement> campaignFormElements = FacadeProvider.getCampaignFormMetaFacade()
				.getCampaignFormMetaByUuid(campaignFormMetaUuid).getCampaignFormElements();
		if (campaignFormElements == null) {
			return false;
		}

		for (CampaignFormElement element : campaignFormElements) {
			if ("image".equalsIgnoreCase(element.getType())) {
				return true;
			}
		}

		return false;
	}

	public void removeColumnsSelectionn() {
		grid.setSelectionMode(SelectionMode.NONE);
	}

	public void setgridVisibility() {
		grid.getColumnByKey(CampaignFormDataIndexDto.ISVERIFIED).setVisible(true);
	}

	public void setgridVisibilityx() {
		grid.getColumnByKey(CampaignFormDataIndexDto.ISPUBLISHED).setVisible(true);
	}

	public void configureFormNameTranslations() {

		String language = userProvider.getUser().getLanguage().toString();

		Set<FormAccess> xx = new HashSet<FormAccess>();
		xx = userProvider.getUser().getFormAccess();

		List<CampaignFormMetaReferenceDto> filterdList = new ArrayList<>();

		switch (language) {
		case "Pashto":
			if (userProvider.getUser().getArea() != null) {
				campaignForms = FacadeProvider.getCampaignFormMetaFacade()
						.getCampaignFormMetasAsReferencesByCampaignandRoundAndPashtox(
								campaignPhase.getValue().toString().toLowerCase(), campaignz.getValue().getUuid(),
								userProvider.getUser().getArea());
			} else {
				campaignForms = FacadeProvider.getCampaignFormMetaFacade()
						.getCampaignFormMetasAsReferencesByCampaignandRoundAndPashto(
								campaignPhase.getValue().toString().toLowerCase(), campaignz.getValue().getUuid());
			}

			for (FormAccess n : xx) {
				boolean yn = campaignForms.stream().filter(e -> !e.getFormCategory().equals(null))
						.filter(ee -> ee.getFormCategory().equals(n)).collect(Collectors.toList()).size() > 0;
				if (yn) {
					filterdList.addAll(campaignForms.stream().filter(e -> !e.getFormCategory().equals(null))
							.filter(ee -> ee.getFormCategory().equals(n)).collect(Collectors.toList()));
				}
			}

			filterdList.sort(Comparator.comparing(CampaignFormMetaReferenceDto::getCaption));

			newForm.setItems(filterdList);
			importFormData.setItems(filterdList);
			campaignFormCombo.setItems(filterdList);

			break;

		case "Dari":

			if (userProvider.getUser().getArea() != null) {
				campaignForms = FacadeProvider.getCampaignFormMetaFacade()
						.getAllCampaignFormMetasAsReferencesByRoundandCampaignRoundAndDarix(
								campaignPhase.getValue().toString().toLowerCase(), campaignz.getValue().getUuid(),
								userProvider.getUser().getArea());
			} else {
				campaignForms = FacadeProvider.getCampaignFormMetaFacade()
						.getAllCampaignFormMetasAsReferencesByRoundandCampaignRoundAndDari(
								campaignPhase.getValue().toString().toLowerCase(), campaignz.getValue().getUuid());
				System.out.println("444444444444444444444444444444444444444444");
			}

			for (FormAccess n : xx) {
				boolean yn = campaignForms.stream().filter(e -> !e.getFormCategory().equals(null))
						.filter(ee -> ee.getFormCategory().equals(n)).collect(Collectors.toList()).size() > 0;
				if (yn) {
					filterdList.addAll(campaignForms.stream().filter(e -> !e.getFormCategory().equals(null))
							.filter(ee -> ee.getFormCategory().equals(n)).collect(Collectors.toList()));
				}
			}

			filterdList.sort(Comparator.comparing(CampaignFormMetaReferenceDto::getCaption));

			newForm.setItems(filterdList);
			importFormData.setItems(filterdList);
			campaignFormCombo.setItems(filterdList);

			break;

		default:
			if (userProvider.getUser().getArea() != null) {
				campaignForms = FacadeProvider.getCampaignFormMetaFacade()
						.getAllCampaignFormMetasAsReferencesByRoundandCampaignx(
								campaignPhase.getValue().toString().toLowerCase(), campaignz.getValue().getUuid(),
								userProvider.getUser().getArea());
				System.out.println("555555555555555555555555555555555555555555555555");
			} else {
				campaignForms = FacadeProvider.getCampaignFormMetaFacade()
						.getAllCampaignFormMetasAsReferencesByRoundandCampaign(
								campaignPhase.getValue().toString().toLowerCase(), campaignz.getValue().getUuid());
				System.out.println("666666666666666666666666666666666666666666666666");
			}

			campaignForms.removeIf(e -> e.getFormCategory() == null);

			for (FormAccess n : xx) {
				boolean yn = campaignForms.stream().filter(e -> !e.getFormCategory().equals(null))
						.filter(ee -> ee.getFormCategory().equals(n)).collect(Collectors.toList()).size() > 0;
				if (yn) {
					filterdList.addAll(campaignForms.stream().filter(e -> !e.getFormCategory().equals(null))
							.filter(ee -> ee.getFormCategory().equals(n)).collect(Collectors.toList()));
				}
			}

			filterdList.sort(Comparator.comparing(CampaignFormMetaReferenceDto::getCaption));

//			campaignForms = FacadeProvider.getCampaignFormMetaFacade()
//					.getAllCampaignFormMetasAsReferencesByRoundUserLanguageCampaignandForm(
//							campaignPhase.getValue().toString().toLowerCase(), campaignz.getValue().getUuid(),
//							userProvider.getUser().getFormAccess(), "Dari");
//			campaignForms.sort(Comparator.comparing(CampaignFormMetaReferenceDto::getCaption));

			newForm.setItems(filterdList);
			importFormData.setItems(filterdList);
			campaignFormCombo.setItems(filterdList);
//		        	newForm.setItems(campaignForms);
			break;

		}

	}

	private void updateGridSelectionMode() {
		if (userProvider.getUser().getUsertype() == UserType.EOC_USER) {
			boolean isPublished = FacadeProvider.getCampaignFacade().isPublished(criteria.getCampaign().getUuid());

			if (isPublished && criteria.getFormType().toString().equals("POST-CAMPAIGN")) {
				grid.setSelectionMode(SelectionMode.SINGLE);
			}
		}
	}

	private void selectAllItems(ListDataProvider<CampaignFormDataIndexDto> dataProvider) {
		MultiSelect<Grid<CampaignFormDataIndexDto>, CampaignFormDataIndexDto> multiSelect = grid.asMultiSelect();
		dataProvider.getItems().forEach(multiSelect::select);
	}

	private <T> void selectAllItems(Grid<T> grid, DataProvider<T, ?> dataProvider) {
		MultiSelect<Grid<T>, T> multiSelect = grid.asMultiSelect();
		dataProvider.fetch(new Query<>()).forEach(multiSelect::select);

		grid.addSelectionListener(event -> {
			for (T item : event.getAllSelectedItems()) {
				grid.select(item);
			}
		});
	}

	private void setBulkModeLoader() {
		grid.setItems(
				query -> FacadeProvider.getCampaignFormDataFacade()
						.getIndexList(criteria, query.getOffset(), query.getLimit(),
								query.getSortOrders().stream()
										.map(sortOrder -> new SortProperty(sortOrder.getSorted(),
												sortOrder.getDirection() == SortDirection.ASCENDING))
										.collect(Collectors.toList()))
						.stream(),
				query -> (int) FacadeProvider.getCampaignFormDataFacade().count(criteria));

	}

	private Stream<CampaignFormDataIndexDto> fetchCampaignFormData(
			Query<CampaignFormDataIndexDto, CampaignFormDataCriteria> query) {
		if (userProvider.getUser().getLanguage().toString().equals("Pashto")) {
			return FacadeProvider.getCampaignFormDataFacade()
					.getIndexListPashto(criteria, query.getOffset(), query.getLimit(), query.getSortOrders().stream()
							.map(sortOrder -> new SortProperty(sortOrder.getSorted(),
									sortOrder.getDirection() == SortDirection.ASCENDING))
							.collect(Collectors.toList()))
					.stream();
		} else if (userProvider.getUser().getLanguage().toString().equals("Dari")) {

//			System.out.println("Darrrrrrrrrrrrrrrrrrrrrrr");

			return FacadeProvider.getCampaignFormDataFacade()
					.getIndexListDari(criteria, query.getOffset(), query.getLimit(), query.getSortOrders().stream()
							.map(sortOrder -> new SortProperty(sortOrder.getSorted(),
									sortOrder.getDirection() == SortDirection.ASCENDING))
							.collect(Collectors.toList()))
					.stream();
		} else {
//			System.out.println("Enggggggggggggggggggggggg");
			return FacadeProvider.getCampaignFormDataFacade()
					.getIndexList(criteria, query.getOffset(), query.getLimit(), query.getSortOrders().stream()
							.map(sortOrder -> new SortProperty(sortOrder.getSorted(),
									sortOrder.getDirection() == SortDirection.ASCENDING))
							.collect(Collectors.toList()))
					.stream();
		}
	}

	private int countCampaignFormData(Query<CampaignFormDataIndexDto, CampaignFormDataCriteria> query) {
		return (int) FacadeProvider.getCampaignFormDataFacade().count(criteria);
	}

	private void handleDeleteAction() {

		deleteAllSelectedItems(grid.getSelectedItems());

	}

	private void handleDataVerificationAction() {

		verifyAllSelectedItems(grid.getSelectedItems());

	}

	private void handleDataUnVerificationAction() {
		unVerifyAllSelectedItems(grid.getSelectedItems());
	}

	private void handleDataUnPublishingAction() {
		unPublishAllSelectedItems(grid.getSelectedItems());
	}

	private void handleDataPublishingAction() {
		publishAllSelectedItems(grid.getSelectedItems());
	}

	private void handleDataUnValidateAction() {
		unValidateAllSelectedItems(grid.getSelectedItems());
	}

	private void handleDataValidateAction() {
		validateAllSelectedItems(grid.getSelectedItems());
	}

	public void deleteAllSelectedItems(Collection<CampaignFormDataIndexDto> selectedRows) {
		confirmationDialog = new ConfirmDialog();

		if (selectedRows.size() == 0) {
			confirmationDialog.setCancelable(false);
			confirmationDialog.setRejectable(false);
			confirmationDialog.addCancelListener(e -> confirmationDialog.close());
			confirmationDialog.setConfirmText(I18nProperties.getCaption(Captions.actionOkay));

			confirmationDialog.setText("You have not selected any data to be deleted.");
			confirmationDialog.setHeader("Error Deleting Campaign Data");
			confirmationDialog.open();

		} else {
			confirmationDialog.setCancelable(true);
			confirmationDialog.setRejectable(true);
			confirmationDialog.setRejectText(I18nProperties.getCaption(Captions.actionNo));
			confirmationDialog.setConfirmText(I18nProperties.getCaption(Captions.actionYes));
			confirmationDialog.addCancelListener(e -> confirmationDialog.close());
			confirmationDialog.addRejectListener(e -> confirmationDialog.close());
			confirmationDialog.open();
			confirmationDialog.setHeader("Delete Campaign Data");
//TODO: Language

			confirmationDialog
					.setText("Are you sure you want to Delete " + selectedRows.size() + " selected Campaign Data?");

			confirmationDialog.addConfirmListener(e -> {
				List<String> uuids = selectedRows.stream().map(CampaignFormDataIndexDto::getUuid)
						.collect(Collectors.toList());
				FacadeProvider.getCampaignFormDataFacade().deleteCampaignData(uuids);

				reload();
				if (leaveBulkEdit.isVisible()) {
					leaveBulkEdit.setVisible(false);
					enterBulkEdit.setVisible(true);
					grid.setSelectionMode(Grid.SelectionMode.SINGLE);

					dropdownBulkOperations.setVisible(false);
					selectAllButtonpLACEHOLDER.setVisible(false);

				}
			});

		}
	}

	public void publishAllSelectedItems(Collection<CampaignFormDataIndexDto> selectedRows) {
		confirmationDialog = new ConfirmDialog();
		boolean isDataDirty = false;
		for (CampaignFormDataIndexDto selectedItem : selectedRows) {

			System.out.println(selectedRows.size() + "<<<Size of selected items " + selectedItem.isIsverified()
					+ ">>>> Dirty data " + isDataDirty);

			if (selectedItem.isIsverified() == false) {
				isDataDirty = true;
			}

//			System.out.println(selectedRows.size() + "<<<2222222Size of selected items " + selectedItem.isIsverified()
//					+ "2222222222>>>> Dirty data " + isDataDirty);

		}
		if (selectedRows.size() == 0 || isDataDirty) {
			confirmationDialog.setCancelable(false);
			confirmationDialog.setRejectable(false);
			confirmationDialog.addCancelListener(e -> confirmationDialog.close());
			confirmationDialog.setConfirmText(I18nProperties.getCaption(Captions.actionOkay));
			if (selectedRows.size() == 0 && (isDataDirty || !isDataDirty)) {
				confirmationDialog.setText("You have not selected any data to be published.");

			} else if (selectedRows.size() > 0 && isDataDirty) {
				confirmationDialog.setText(
						"You have selected 1 or more unverified records to publish. Please verify any records you wish to publish first.");

			}
			confirmationDialog.setHeader("Error Publishing Campaign Data");
			confirmationDialog.open();

		} else {
			confirmationDialog.setCancelable(true);
			confirmationDialog.setRejectable(true);
			confirmationDialog.setRejectText(I18nProperties.getCaption(Captions.actionNo));
			confirmationDialog.setConfirmText(I18nProperties.getCaption(Captions.actionYes));
			confirmationDialog.addCancelListener(e -> confirmationDialog.close());
			confirmationDialog.addRejectListener(e -> confirmationDialog.close());
			confirmationDialog.open();
			confirmationDialog.setHeader("Publish Campaign Data");
//TODO: Language

			confirmationDialog
					.setText("Are you sure you want to Publish " + selectedRows.size() + " selected Campaign Data?");

			confirmationDialog.addConfirmListener(e -> {
				List<String> uuids = selectedRows.stream().map(CampaignFormDataIndexDto::getUuid)
						.collect(Collectors.toList());
				FacadeProvider.getCampaignFormDataFacade().publishCampaignData(uuids, false);
				reload();

				if (leaveBulkEdit.isVisible()) {
					leaveBulkEdit.setVisible(false);
					enterBulkEdit.setVisible(true);
					grid.setSelectionMode(Grid.SelectionMode.SINGLE);
					dropdownBulkOperations.setVisible(false);
					selectAllButtonpLACEHOLDER.setVisible(false);

				}
			});

		}
	}

	public void unPublishAllSelectedItems(Collection<CampaignFormDataIndexDto> selectedRows) {
		confirmationDialog = new ConfirmDialog();
		boolean isDataDirty = false;
		for (CampaignFormDataIndexDto selectedItem : selectedRows) {

			System.out.println(selectedRows.size() + "<<unpublish<Size of selected items " + selectedItem.isIsverified()
					+ ">>>> Dirty data " + isDataDirty);

			if (selectedItem.isIsverified() == false) {
				isDataDirty = true;
			}

//			System.out.println(selectedRows.size() + "<<<2222222Size of selected items " + selectedItem.isIsverified()
//					+ "2222222222>>>> Dirty data " + isDataDirty);

		}
		if (selectedRows.size() == 0 || isDataDirty) {
			confirmationDialog.setCancelable(false);
			confirmationDialog.setRejectable(false);
			confirmationDialog.addCancelListener(e -> confirmationDialog.close());
			confirmationDialog.setConfirmText(I18nProperties.getCaption(Captions.actionOkay));
			if (selectedRows.size() == 0 && (isDataDirty || !isDataDirty)) {
				confirmationDialog.setText("You have not selected any data to be unpublished.");

			} else if (selectedRows.size() > 0 && isDataDirty) {
				confirmationDialog.setText(
						"You have selected 1 or more unverified records to publish. Please verify any records you wish to unpublish first.");

			}
			confirmationDialog.setHeader("Error Unpublishing Campaign Data");
			confirmationDialog.open();

		} else {
			confirmationDialog.setCancelable(true);
			confirmationDialog.setRejectable(true);
			confirmationDialog.setRejectText(I18nProperties.getCaption(Captions.actionNo));
			confirmationDialog.setConfirmText(I18nProperties.getCaption(Captions.actionYes));
			confirmationDialog.addCancelListener(e -> confirmationDialog.close());
			confirmationDialog.addRejectListener(e -> confirmationDialog.close());
			confirmationDialog.open();
			confirmationDialog.setHeader("Unpublish Campaign Data");
//TODO: Language

			confirmationDialog
					.setText("Are you sure you want to Unpublish " + selectedRows.size() + " selected Campaign Data?");

			confirmationDialog.addConfirmListener(e -> {
				List<String> uuids = selectedRows.stream().map(CampaignFormDataIndexDto::getUuid)
						.collect(Collectors.toList());

				FacadeProvider.getCampaignFormDataFacade().publishCampaignData(uuids, true);
				reload();
				if (leaveBulkEdit.isVisible()) {
					leaveBulkEdit.setVisible(false);
					enterBulkEdit.setVisible(true);
					grid.setSelectionMode(Grid.SelectionMode.SINGLE);

					dropdownBulkOperations.setVisible(false);
					selectAllButtonpLACEHOLDER.setVisible(false);

				}
			});

		}
	}

	public void verifyAllSelectedItems(Collection<CampaignFormDataIndexDto> selectedRows) {
		confirmationDialog = new ConfirmDialog();

		if (selectedRows.size() == 0) {
			confirmationDialog.setCancelable(false);
			confirmationDialog.setRejectable(false);
			confirmationDialog.addCancelListener(e -> confirmationDialog.close());
			confirmationDialog.setConfirmText(I18nProperties.getCaption(Captions.actionOkay));

			confirmationDialog.setText("You have not selected any data to be verified.");
			confirmationDialog.setHeader("Error Verifying Campaign Data");
			confirmationDialog.open();

		} else {
			confirmationDialog.setCancelable(true);
			confirmationDialog.setRejectable(true);
			confirmationDialog.setRejectText(I18nProperties.getCaption(Captions.actionNo));
			confirmationDialog.setConfirmText(I18nProperties.getCaption(Captions.actionYes));
			confirmationDialog.addCancelListener(e -> confirmationDialog.close());
			confirmationDialog.addRejectListener(e -> confirmationDialog.close());
			confirmationDialog.open();
			confirmationDialog.setHeader("Verify Campaign Data");
//TODO: Language

			confirmationDialog
					.setText("Are you sure you want to Verify " + selectedRows.size() + " selected Campaign Data?");

			confirmationDialog.addConfirmListener(e -> {
				List<String> uuids = selectedRows.stream().map(CampaignFormDataIndexDto::getUuid)
						.collect(Collectors.toList());

				System.err.println(" verification kicke from frontend");

//			FacadeProvider.getCampaignFormDataFacade().verifyCampaignData(event.getCampaign().getUuid(), true);

				FacadeProvider.getCampaignFormDataFacade().verifyCampaignData(uuids, false);
//				 Notification.show("Camapaign Dayta Deleted ");
				reload();
				if (leaveBulkEdit.isVisible()) {
					leaveBulkEdit.setVisible(false);
					enterBulkEdit.setVisible(true);
					grid.setSelectionMode(Grid.SelectionMode.SINGLE);

					dropdownBulkOperations.setVisible(false);
					selectAllButtonpLACEHOLDER.setVisible(false);

				}
			});

		}
	}

	public void unVerifyAllSelectedItems(Collection<CampaignFormDataIndexDto> selectedRows) {
		confirmationDialog = new ConfirmDialog();

		if (selectedRows.size() == 0) {
			confirmationDialog.setCancelable(false);
			confirmationDialog.setRejectable(false);
			confirmationDialog.addCancelListener(e -> confirmationDialog.close());
			confirmationDialog.setConfirmText(I18nProperties.getCaption(Captions.actionOkay));

			confirmationDialog.setText("You have not selected any data to be unverified.");
			confirmationDialog.setHeader("Error Unverifying Campaign Data");
			confirmationDialog.open();

		} else {
			confirmationDialog.setCancelable(true);
			confirmationDialog.setRejectable(true);
			confirmationDialog.setRejectText(I18nProperties.getCaption(Captions.actionNo));
			confirmationDialog.setConfirmText(I18nProperties.getCaption(Captions.actionYes));
			confirmationDialog.addCancelListener(e -> confirmationDialog.close());
			confirmationDialog.addRejectListener(e -> confirmationDialog.close());
			confirmationDialog.open();
			confirmationDialog.setHeader("Unverify Campaign Data");
//TODO: Language

			confirmationDialog
					.setText("Are you sure you want to Unverify " + selectedRows.size() + " selected Campaign Data?");

			confirmationDialog.addConfirmListener(e -> {
				List<String> uuids = selectedRows.stream().map(CampaignFormDataIndexDto::getUuid)
						.collect(Collectors.toList());

				System.err.println(" unverification kicke from frontend");

//			FacadeProvider.getCampaignFormDataFacade().verifyCampaignData(event.getCampaign().getUuid(), true);

				FacadeProvider.getCampaignFormDataFacade().verifyCampaignData(uuids, true);
//				 Notification.show("Camapaign Dayta Deleted ");
				reload();
				if (leaveBulkEdit.isVisible()) {
					leaveBulkEdit.setVisible(false);
					enterBulkEdit.setVisible(true);
					grid.setSelectionMode(Grid.SelectionMode.SINGLE);

					dropdownBulkOperations.setVisible(false);
					selectAllButtonpLACEHOLDER.setVisible(false);

				}
			});

		}
	}

	public void validateAllSelectedItems(Collection<CampaignFormDataIndexDto> selectedRows) {
		confirmationDialog = new ConfirmDialog();
		boolean isDataDirty = false;
		for (CampaignFormDataIndexDto selectedItem : selectedRows) {

			System.out.println(selectedRows.size() + "<<<Size of selected items " + selectedItem.isIsverified()
					+ ">>>> Dirty data " + isDataDirty);

			if (selectedItem.isIsverified() == false) {
				isDataDirty = true;
			}

//			System.out.println(selectedRows.size() + "<<<2222222Size of selected items " + selectedItem.isIsverified()
//					+ "2222222222>>>> Dirty data " + isDataDirty);

		}
		if (selectedRows.size() == 0 || isDataDirty) {
			confirmationDialog.setCancelable(false);
			confirmationDialog.setRejectable(false);
			confirmationDialog.addCancelListener(e -> confirmationDialog.close());
			confirmationDialog.setConfirmText(I18nProperties.getCaption(Captions.actionOkay));
			if (selectedRows.size() == 0 && (isDataDirty || !isDataDirty)) {
				confirmationDialog.setText("You have not selected any data to be published.");

			} else if (selectedRows.size() > 0 && isDataDirty) {
				confirmationDialog.setText(
						"You have selected 1 or more unverified records to publish. Please verify any records you wish to publish first.");

			}
			confirmationDialog.setHeader("Error Publishing Campaign Data");
			confirmationDialog.open();

		} else {
			confirmationDialog.setCancelable(true);
			confirmationDialog.setRejectable(true);
			confirmationDialog.setRejectText(I18nProperties.getCaption(Captions.actionNo));
			confirmationDialog.setConfirmText(I18nProperties.getCaption(Captions.actionYes));
			confirmationDialog.addCancelListener(e -> confirmationDialog.close());
			confirmationDialog.addRejectListener(e -> confirmationDialog.close());
			confirmationDialog.open();
			confirmationDialog.setHeader("Publish Campaign Data");
//TODO: Language

			confirmationDialog
					.setText("Are you sure you want to Publish " + selectedRows.size() + " selected Campaign Data?");

			confirmationDialog.addConfirmListener(e -> {
				List<String> uuids = selectedRows.stream().map(CampaignFormDataIndexDto::getUuid)
						.collect(Collectors.toList());
				FacadeProvider.getCampaignFormDataFacade().publishCampaignData(uuids, false);
				reload();

				if (leaveBulkEdit.isVisible()) {
					leaveBulkEdit.setVisible(false);
					enterBulkEdit.setVisible(true);
					grid.setSelectionMode(Grid.SelectionMode.SINGLE);
					dropdownBulkOperations.setVisible(false);
					selectAllButtonpLACEHOLDER.setVisible(false);

				}
			});

		}
	}

	public void unValidateAllSelectedItems(Collection<CampaignFormDataIndexDto> selectedRows) {
		confirmationDialog = new ConfirmDialog();
		boolean isDataDirty = false;
		for (CampaignFormDataIndexDto selectedItem : selectedRows) {

			System.out.println(selectedRows.size() + "<<unpublish<Size of selected items " + selectedItem.isIsverified()
					+ ">>>> Dirty data " + isDataDirty);

			if (selectedItem.isIsverified() == false) {
				isDataDirty = true;
			}

//			System.out.println(selectedRows.size() + "<<<2222222Size of selected items " + selectedItem.isIsverified()
//					+ "2222222222>>>> Dirty data " + isDataDirty);

		}
		if (selectedRows.size() == 0 || isDataDirty) {
			confirmationDialog.setCancelable(false);
			confirmationDialog.setRejectable(false);
			confirmationDialog.addCancelListener(e -> confirmationDialog.close());
			confirmationDialog.setConfirmText(I18nProperties.getCaption(Captions.actionOkay));
			if (selectedRows.size() == 0 && (isDataDirty || !isDataDirty)) {
				confirmationDialog.setText("You have not selected any data to be unpublished.");

			} else if (selectedRows.size() > 0 && isDataDirty) {
				confirmationDialog.setText(
						"You have selected 1 or more unverified records to publish. Please verify any records you wish to unpublish first.");

			}
			confirmationDialog.setHeader("Error Unpublishing Campaign Data");
			confirmationDialog.open();

		} else {
			confirmationDialog.setCancelable(true);
			confirmationDialog.setRejectable(true);
			confirmationDialog.setRejectText(I18nProperties.getCaption(Captions.actionNo));
			confirmationDialog.setConfirmText(I18nProperties.getCaption(Captions.actionYes));
			confirmationDialog.addCancelListener(e -> confirmationDialog.close());
			confirmationDialog.addRejectListener(e -> confirmationDialog.close());
			confirmationDialog.open();
			confirmationDialog.setHeader("Unpublish Campaign Data");
//TODO: Language

			confirmationDialog
					.setText("Are you sure you want to Unpublish " + selectedRows.size() + " selected Campaign Data?");

			confirmationDialog.addConfirmListener(e -> {
				List<String> uuids = selectedRows.stream().map(CampaignFormDataIndexDto::getUuid)
						.collect(Collectors.toList());

				FacadeProvider.getCampaignFormDataFacade().publishCampaignData(uuids, true);
				reload();
				if (leaveBulkEdit.isVisible()) {
					leaveBulkEdit.setVisible(false);
					enterBulkEdit.setVisible(true);
					grid.setSelectionMode(Grid.SelectionMode.SINGLE);

					dropdownBulkOperations.setVisible(false);
					selectAllButtonpLACEHOLDER.setVisible(false);

				}
			});

		}
	}

	public void reload() {

		criteria.campaign(campaignz.getValue());
		criteria.setFormType(campaignPhase.getValue().toString());
		criteria.setCampaignFormMeta(campaignFormCombo.getValue());
//		criteria.area(regionCombo.getValue());
		criteria.areaSet(regionCombo.getValue());
//		criteria.region(provinceCombo.getValue());
		criteria.regionSet(provinceCombo.getValue());
//		criteria.district(districtCombo.getValue());
		criteria.districtSet(districtCombo.getValue());
		criteria.community(clusterCombo.getValue());

		grid.getDataProvider().refreshAll();
	}

	@SuppressWarnings("deprecation")
	private void configureGrid(CampaignFormDataCriteria criteria) {
		System.out.println("Configure grid calllllleddddddddddddd");
		languageHandler();
		setMargin(false);

		grid = new Grid<>(CampaignFormDataIndexDto.class, false);
//		grid.setSelectionMode(SelectionMode.SINGLE);
		grid.setColumnReorderingAllowed(true);

		ComponentRenderer<Span, CampaignFormDataIndexDto> rCodeRender = new ComponentRenderer<>(input -> {
			NumberFormat arabicFormat = NumberFormat.getInstance();
			if (userProvider.getUser().getLanguage().toString().equals("Pashto")) {
				arabicFormat = NumberFormat.getInstance(new Locale("ps"));
			} else if (userProvider.getUser().getLanguage().toString().equals("Dari")) {
				arabicFormat = NumberFormat.getInstance(new Locale("fa"));
			}

			String value = String.valueOf(arabicFormat.format(input.getRcode()));
			Span label = new Span(value);
			label.getStyle().set("color", "var(--lumo-body-text-color) !important");
			return label;
		});

		ComponentRenderer<Span, CampaignFormDataIndexDto> pCodeRender = new ComponentRenderer<>(input -> {

			NumberFormat arabicFormat = NumberFormat.getInstance();
			if (userProvider.getUser().getLanguage().toString().equals("Pashto")) {
				arabicFormat = NumberFormat.getInstance(new Locale("ps"));
			} else if (userProvider.getUser().getLanguage().toString().equals("Dari")) {
				arabicFormat = NumberFormat.getInstance(new Locale("fa"));
			}

			String value = String.valueOf(arabicFormat.format(input.getPcode()));
			Span label = new Span(value);
			label.getStyle().set("color", "var(--lumo-body-text-color) !important");
			return label;
		});

		ComponentRenderer<Span, CampaignFormDataIndexDto> dCodeRender = new ComponentRenderer<>(input -> {

			NumberFormat arabicFormat = NumberFormat.getInstance();
			if (userProvider.getUser().getLanguage().toString().equals("Pashto")) {
				arabicFormat = NumberFormat.getInstance(new Locale("ps"));
			} else if (userProvider.getUser().getLanguage().toString().equals("Dari")) {
				arabicFormat = NumberFormat.getInstance(new Locale("fa"));
			}

			String value = String.valueOf(arabicFormat.format(input.getDcode()));
			Span label = new Span(value);
			label.getStyle().set("color", "var(--lumo-body-text-color) !important");
			return label;
		});

		ComponentRenderer<Span, CampaignFormDataIndexDto> cCodeRender = new ComponentRenderer<>(input -> {

			NumberFormat arabicFormat = NumberFormat.getInstance();
			if (userProvider.getUser().getLanguage().toString().equals("Pashto")) {
				arabicFormat = NumberFormat.getInstance(new Locale("ps"));
			} else if (userProvider.getUser().getLanguage().toString().equals("Dari")) {
				arabicFormat = NumberFormat.getInstance(new Locale("fa"));
			}

			String value = "";
			if (input.getCcode() != null) {
				value = String.valueOf(arabicFormat.format(input.getCcode() != null ? input.getCcode() : ""));

			}

			Span label = new Span(value);
			label.getStyle().set("color", "var(--lumo-body-text-color) !important");
			return label;
		});

		ComponentRenderer<Span, CampaignFormDataIndexDto> clusterNumberRender = new ComponentRenderer<>(input -> {

			NumberFormat arabicFormat = NumberFormat.getInstance();
			if (userProvider.getUser().getLanguage().toString().equals("Pashto")) {
				arabicFormat = NumberFormat.getInstance(new Locale("ps"));
			} else if (userProvider.getUser().getLanguage().toString().equals("Dari")) {
				arabicFormat = NumberFormat.getInstance(new Locale("fa"));
			}

			String value = "";
			if (input.getClusternumber() != null) {
				value = String
						.valueOf(arabicFormat.format(input.getClusternumber() != null ? input.getClusternumber() : ""));

			}
			Span label = new Span(value);
			label.getStyle().set("color", "var(--lumo-body-text-color) !important");
			return label;
		});

		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

		if (userProvider.getUser().getLanguage().toString().equals("Pashto")) {

			grid.addColumn(CampaignFormDataIndexDto.CAMPAIGN).setHeader(I18nProperties.getCaption(Captions.Campaigns))
					.setSortable(true).setResizable(true).setAutoWidth(true).setTooltipGenerator(e -> e.getCampaign());
			formNameColumn = grid.addColumn(CampaignFormDataIndexDto.FORM)
					.setHeader(I18nProperties.getCaption(Captions.campaignCampaignForm)).setSortable(true)
					.setResizable(true).setAutoWidth(true).setTooltipGenerator(e -> e.getForm());

			grid.addColumn(e -> e.getFormDate() != null
					? e.getFormDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
					: "").setHeader(I18nProperties.getCaption(Captions.date)).setKey("date")
//			createHeaderComponent(I18nProperties.getCaption(Captions.area), I18nProperties.getCaption(Captions.area)))
					.setSortable(true).setResizable(true).setAutoWidth(true)
					.setTooltipGenerator(e -> e.getFormDate() != null ? e.getFormDate().toInstant()
							.atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter) : "")
					.setFooter(I18nProperties.getCaption(Captions.date).toLowerCase());

			grid.addColumn(e -> e.getChangeDate() != null
					? e.getChangeDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
					: "").setHeader("Last Modified").setKey("lastmodified").setSortable(true).setResizable(true)
					.setAutoWidth(true)
					.setTooltipGenerator(e -> e.getChangeDate() != null ? e.getChangeDate().toInstant()
							.atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter) : "")
					.setFooter("Last Modified");

			grid.addColumn(CampaignFormDataIndexDto.AREA).setHeader(I18nProperties.getCaption(Captions.area))
					.setSortable(true).setResizable(true).setAutoWidth(true).setTooltipGenerator(e -> e.getArea());
			grid.addColumn(rCodeRender).setHeader(I18nProperties.getCaption(Captions.Area_externalId)).setSortable(true)
					.setResizable(true).setAutoWidth(true)
					.setTooltipGenerator(e -> I18nProperties.getCaption(Captions.Area_externalId));
			provinceNameColumn = grid.addColumn(CampaignFormDataIndexDto.REGION)
					.setHeader(I18nProperties.getCaption(Captions.region)).setSortable(true).setResizable(true)
					.setAutoWidth(true).setTooltipGenerator(e -> e.getRegion());
			pCodeColumn = grid.addColumn(pCodeRender).setHeader(I18nProperties.getCaption(Captions.Region_externalID))
					.setSortable(true).setResizable(true).setAutoWidth(true)
					.setTooltipGenerator(e -> I18nProperties.getCaption(Captions.Region_externalID));
			districtNameColumn = grid.addColumn(CampaignFormDataIndexDto.DISTRICT)
					.setHeader(I18nProperties.getCaption(Captions.district)).setSortable(true).setResizable(true)
					.setAutoWidth(true).setTooltipGenerator(e -> e.getDistrict());
			dCodeColumn = grid.addColumn(dCodeRender).setHeader(I18nProperties.getCaption(Captions.District_externalID))
					.setSortable(true).setResizable(true).setAutoWidth(true)
					.setTooltipGenerator(e -> I18nProperties.getCaption(Captions.District_externalID));

//			if(!isActiveFormDistrictLevel) {

			clusterNameColumn = grid.addColumn(CampaignFormDataIndexDto.COMMUNITY)
					.setHeader(I18nProperties.getCaption(Captions.community)).setSortable(true).setResizable(true)
					.setAutoWidth(true).setTooltipGenerator(e -> I18nProperties.getCaption(Captions.community));
			clusterNumberColumn = grid.addColumn(clusterNumberRender).setKey("clusterNumber")
					.setHeader(I18nProperties.getCaption(Captions.clusterNumber)).setSortable(true).setResizable(true)
					.setAutoWidth(true).setTooltipGenerator(e -> I18nProperties.getCaption(Captions.clusterNumber));
			ccodeColumn = grid.addColumn(cCodeRender)
					.setHeader(I18nProperties.getCaption(Captions.Community_externalID)).setSortable(true)
					.setResizable(true).setAutoWidth(true)
					.setTooltipGenerator(e -> I18nProperties.getCaption(Captions.Community_externalID));

//			}

			grid.addColumn(CampaignFormDataIndexDto.FORM_TYPE).setHeader(I18nProperties.getCaption(Captions.formPhase))
					.setSortable(true).setResizable(true).setAutoWidth(true)
					.setTooltipGenerator(e -> I18nProperties.getCaption(Captions.formPhase));
			grid.addColumn(CampaignFormDataIndexDto.SOURCE).setHeader("Source")// I18nProperties.getCaption(Captions.formPhase))
					.setSortable(true).setResizable(true).setAutoWidth(true)
					.setTooltipGenerator(e -> "Source:" + e.getSource());
			grid.addColumn(CampaignFormDataIndexDto.CREATED_BY)
					.setHeader(I18nProperties.getCaption(Captions.Campaign_creatingUser)).setSortable(true)
					.setResizable(true).setAutoWidth(true).setTooltipGenerator(
							e -> I18nProperties.getCaption(Captions.Campaign_creatingUser) + e.getSource());

		} else if (userProvider.getUser().getLanguage().toString().equals("Dari")) {

			grid.addColumn(CampaignFormDataIndexDto.CAMPAIGN).setHeader(I18nProperties.getCaption(Captions.Campaigns))
					.setSortable(true).setResizable(true).setAutoWidth(true).setTooltipGenerator(e -> e.getCampaign());
			formNameColumn = grid.addColumn(CampaignFormDataIndexDto.FORM)
					.setHeader(I18nProperties.getCaption(Captions.campaignCampaignForm)).setSortable(true)
					.setResizable(true).setAutoWidth(true).setTooltipGenerator(e -> e.getForm());

			grid.addColumn(e -> e.getFormDate() != null
					? e.getFormDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
					: "").setHeader(I18nProperties.getCaption(Captions.date)).setKey("date")
//			createHeaderComponent(I18nProperties.getCaption(Captions.area), I18nProperties.getCaption(Captions.area)))
					.setComparator(Comparator.comparing(CampaignFormDataIndexDto::getFormDate)).setSortable(true)
					.setResizable(true).setAutoWidth(true)
					.setTooltipGenerator(e -> e.getFormDate() != null ? e.getFormDate().toInstant()
							.atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter) : "")
					.setFooter(I18nProperties.getCaption(Captions.date).toLowerCase());

			grid.addColumn(e -> e.getChangeDate() != null
					? e.getChangeDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
					: "").setHeader("Last Modified").setKey("lastmodified").setSortable(true).setResizable(true)
					.setAutoWidth(true)
					.setTooltipGenerator(e -> e.getChangeDate() != null ? e.getChangeDate().toInstant()
							.atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter) : "")
					.setFooter("Last Modified");

			grid.addColumn(CampaignFormDataIndexDto.AREA).setHeader(I18nProperties.getCaption(Captions.area))
					.setSortable(true).setResizable(true).setAutoWidth(true).setTooltipGenerator(e -> e.getArea());
			grid.addColumn(rCodeRender).setHeader(I18nProperties.getCaption(Captions.Area_externalId)).setSortable(true)
					.setResizable(true).setAutoWidth(true)
					.setTooltipGenerator(e -> I18nProperties.getCaption(Captions.Area_externalId));
			provinceNameColumn = grid.addColumn(CampaignFormDataIndexDto.REGION)
					.setHeader(I18nProperties.getCaption(Captions.region)).setSortable(true).setResizable(true)
					.setAutoWidth(true).setTooltipGenerator(e -> e.getRegion());
			pCodeColumn = grid.addColumn(pCodeRender).setHeader(I18nProperties.getCaption(Captions.Region_externalID))
					.setSortable(true).setResizable(true).setAutoWidth(true)
					.setTooltipGenerator(e -> I18nProperties.getCaption(Captions.Region_externalID));
			districtNameColumn = grid.addColumn(CampaignFormDataIndexDto.DISTRICT)
					.setHeader(I18nProperties.getCaption(Captions.district)).setSortable(true).setResizable(true)
					.setAutoWidth(true).setTooltipGenerator(e -> e.getDistrict());
			dCodeColumn = grid.addColumn(dCodeRender).setHeader(I18nProperties.getCaption(Captions.District_externalID))
					.setSortable(true).setResizable(true).setAutoWidth(true)
					.setTooltipGenerator(e -> I18nProperties.getCaption(Captions.District_externalID));

//			if(!isActiveFormDistrictLevel) {

			clusterNameColumn = grid.addColumn(CampaignFormDataIndexDto.COMMUNITY)
					.setHeader(I18nProperties.getCaption(Captions.community)).setSortable(true).setResizable(true)
					.setAutoWidth(true).setTooltipGenerator(e -> I18nProperties.getCaption(Captions.community));
			clusterNumberColumn = grid.addColumn(clusterNumberRender).setKey("clusterNumber")
					.setHeader(I18nProperties.getCaption(Captions.clusterNumber)).setSortable(true).setResizable(true)
					.setAutoWidth(true).setTooltipGenerator(e -> I18nProperties.getCaption(Captions.clusterNumber));
			ccodeColumn = grid.addColumn(cCodeRender)
					.setHeader(I18nProperties.getCaption(Captions.Community_externalID)).setSortable(true)
					.setResizable(true).setAutoWidth(true)
					.setTooltipGenerator(e -> I18nProperties.getCaption(Captions.Community_externalID));
//			}

			grid.addColumn(CampaignFormDataIndexDto.FORM_TYPE).setHeader(I18nProperties.getCaption(Captions.formPhase))
					.setSortable(true).setResizable(true).setAutoWidth(true)
					.setTooltipGenerator(e -> I18nProperties.getCaption(Captions.formPhase));
			grid.addColumn(CampaignFormDataIndexDto.SOURCE).setHeader("Source")// I18nProperties.getCaption(Captions.formPhase))
					.setSortable(true).setResizable(true).setAutoWidth(true)
					.setTooltipGenerator(e -> "Source:" + e.getSource());
			grid.addColumn(CampaignFormDataIndexDto.CREATED_BY)
					.setHeader(I18nProperties.getCaption(Captions.Campaign_creatingUser)).setSortable(true)
					.setResizable(true).setAutoWidth(true).setTooltipGenerator(
							e -> I18nProperties.getCaption(Captions.Campaign_creatingUser) + e.getSource());

		} else {

			grid.addColumn(CampaignFormDataIndexDto.CAMPAIGN).setHeader(I18nProperties.getCaption(Captions.Campaigns))
//					createHeaderComponent(I18nProperties.getCaption(Captions.Campaigns),I18nProperties.getCaption(Captions.Campaigns)))
					.setSortable(true).setResizable(true).setAutoWidth(true).setTooltipGenerator(e -> e.getCampaign())
					.setFooter(CampaignFormDataIndexDto.CAMPAIGN);

			formNameColumn = grid.addColumn(CampaignFormDataIndexDto.FORM)
					.setHeader(I18nProperties.getCaption(Captions.campaignCampaignForm))
//							createHeaderComponent(I18nProperties.getCaption(Captions.campaignCampaignForm),I18nProperties.getCaption(Captions.campaignCampaignForm)))
					.setSortable(true).setResizable(true).setAutoWidth(true).setTooltipGenerator(e -> e.getForm())
					.setFooter(CampaignFormDataIndexDto.FORM);

			grid.addColumn(e -> e.getFormDate() != null
					? e.getFormDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
					: "").setHeader(I18nProperties.getCaption(Captions.date)).setKey("date")
//			createHeaderComponent(I18nProperties.getCaption(Captions.area), I18nProperties.getCaption(Captions.area)))
					.setSortable(true).setResizable(true).setAutoWidth(true)
					.setTooltipGenerator(e -> e.getFormDate() != null ? e.getFormDate().toInstant()
							.atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter) : "")
					.setFooter(I18nProperties.getCaption(Captions.date).toLowerCase());

			grid.addColumn(e -> e.getChangeDate() != null
					? e.getChangeDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
					: "").setHeader("Last Modified").setKey("lastmodified").setSortable(true).setResizable(true)
					.setAutoWidth(true)
					.setTooltipGenerator(e -> e.getChangeDate() != null ? e.getChangeDate().toInstant()
							.atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter) : "")
					.setFooter("Last Modified");

			grid.addColumn(CampaignFormDataIndexDto.AREA).setHeader(I18nProperties.getCaption(Captions.area))
//					createHeaderComponent(I18nProperties.getCaption(Captions.area), I18nProperties.getCaption(Captions.area)))
					.setSortable(true).setResizable(true).setAutoWidth(true).setTooltipGenerator(e -> e.getArea())
					.setFooter(I18nProperties.getCaption(Captions.area).toLowerCase());// .setFooter(CampaignFormDataIndexDto.AREA);
			grid.addColumn(CampaignFormDataIndexDto.RCODE)
					.setHeader(I18nProperties.getCaption(Captions.Area_externalId))
//							createHeaderComponent(I18nProperties.getCaption(Captions.Area_externalId), I18nProperties.getCaption(Captions.Area_externalId)))
					.setSortable(true).setResizable(true).setAutoWidth(true)
					.setTooltipGenerator(e -> e.getRcode().toString()).setFooter(CampaignFormDataIndexDto.RCODE);

			provinceNameColumn = grid.addColumn(CampaignFormDataIndexDto.REGION)
					.setHeader(I18nProperties.getCaption(Captions.region))
//					createHeaderComponent(I18nProperties.getCaption(Captions.region), I18nProperties.getCaption(Captions.region)))
					.setSortable(true).setResizable(true).setAutoWidth(true).setTooltipGenerator(e -> e.getRegion())
					.setFooter(I18nProperties.getCaption(Captions.region).toLowerCase());

			pCodeColumn = grid.addColumn(CampaignFormDataIndexDto.PCODE)
					.setHeader(I18nProperties.getCaption(Captions.Region_externalID))
//					createHeaderComponent(I18nProperties.getCaption(Captions.Region_externalID), I18nProperties.getCaption(Captions.Region_externalID)))
					.setSortable(true).setResizable(true).setAutoWidth(true).setTooltipGenerator(e -> {
						Integer pcode = e.getPcode();
						return pcode == null ? "" : String.valueOf(pcode);

					}).setFooter(CampaignFormDataIndexDto.PCODE);
			districtNameColumn = grid.addColumn(CampaignFormDataIndexDto.DISTRICT)
					.setHeader(I18nProperties.getCaption(Captions.district))
//			createHeaderComponent(I18nProperties.getCaption(Captions.district),I18nProperties.getCaption(Captions.district)))
					.setSortable(true).setResizable(true).setAutoWidth(true).setTooltipGenerator(e -> e.getDistrict())
					.setFooter(CampaignFormDataIndexDto.DISTRICT);
			dCodeColumn = grid.addColumn(CampaignFormDataIndexDto.DCODE)
					.setHeader(I18nProperties.getCaption(Captions.District_externalID))
//					.setHeaderText("Your Tooltip Text")
//					createHeaderComponent(I18nProperties.getCaption(Captions.District_externalID),I18nProperties.getCaption(Captions.District_externalID)))
					.setSortable(true).setResizable(true).setAutoWidth(true).setFooter(CampaignFormDataIndexDto.DCODE)
					.setTooltipGenerator(e -> {
//						.setTooltipGenerator(e -> {
						Integer dcode = e.getDcode();
						return dcode == null ? "" : String.valueOf(dcode);
//						});

					});

//			if(!isActiveFormDistrictLevel) {
			clusterNameColumn = grid.addColumn(CampaignFormDataIndexDto.COMMUNITY)
					.setHeader(I18nProperties.getCaption(Captions.community))
//						createHeaderComponent(I18nProperties.getCaption(Captions.community),I18nProperties.getCaption(Captions.community)))
					.setSortable(true).setResizable(true).setAutoWidth(true).setTooltipGenerator(e -> e.getCommunity())
					.setFooter(I18nProperties.getCaption(Captions.community));

			clusterNumberColumn = grid.addColumn(CampaignFormDataIndexDto.COMMUNITYNUMBER)
					.setHeader(I18nProperties.getCaption(Captions.clusterNumber))
//								createHeaderComponent(I18nProperties.getCaption(Captions.clusterNumber),I18nProperties.getCaption(Captions.clusterNumber)))
					.setSortable(true).setResizable(true).setAutoWidth(true)
					.setTooltipGenerator(e -> e.getClusternumber() == null ? "" : e.getClusternumber().toString())
					.setFooter(CampaignFormDataIndexDto.COMMUNITYNUMBER);
			ccodeColumn = grid.addColumn(CampaignFormDataIndexDto.CCODE)
					.setHeader(I18nProperties.getCaption(Captions.Community_externalID)).setSortable(true)
					.setResizable(true).setAutoWidth(true)
					.setTooltipGenerator(e -> e.getCcode() == null ? "" : e.getCcode().toString())
					.setFooter(CampaignFormDataIndexDto.CCODE);
//			}

			grid.addColumn(CampaignFormDataIndexDto.FORM_TYPE).setHeader(I18nProperties.getCaption(Captions.formPhase))
//					createHeaderComponent(I18nProperties.getCaption(Captions.formPhase),I18nProperties.getCaption(Captions.formPhase)))
					.setSortable(true).setResizable(true).setAutoWidth(true).setTooltipGenerator(e -> e.getFormType())
					.setFooter(CampaignFormDataIndexDto.FORM_TYPE);

			grid.addColumn(CampaignFormDataIndexDto.SOURCE).setHeader("Source")
//					createHeaderComponent("Source","Source"))// I18nProperties.getCaption(Captions.formPhase))
					.setSortable(true).setResizable(true).setAutoWidth(true)
					.setTooltipGenerator(e -> "Source:" + e.getSource()).setFooter(CampaignFormDataIndexDto.SOURCE);

			grid.addColumn(CampaignFormDataIndexDto.CREATED_BY)
					.setHeader(I18nProperties.getCaption(Captions.Campaign_creatingUser))
//					createHeaderComponent(I18nProperties.getCaption(Captions.Campaign_creatingUser),I18nProperties.getCaption(Captions.Campaign_creatingUser)))//I18nProperties.getCaption(Captions.Campaign_creatingUser))
					.setSortable(true).setResizable(true).setAutoWidth(true)
					.setTooltipGenerator(e -> I18nProperties.getCaption(Captions.Campaign_creatingUser) + e.getSource())
					.setFooter(CampaignFormDataIndexDto.CREATED_BY);

			if (userProvider.getUser().getUsertype() == UserType.WHO_USER) {
				verifiedColumn = grid.addColumn(CampaignFormDataIndexDto::getVerifiedStringValue)
						.setHeader(I18nProperties.getCaption("Verified Status")).setSortable(true).setResizable(true)
						.setAutoWidth(true)
						.setTooltipGenerator(
								e -> I18nProperties.getCaption("Verified Status: ") + e.getVerifiedStringValue())
						.setFooter(CampaignFormDataIndexDto.ISVERIFIED);

				publishedColumn = grid.addColumn(CampaignFormDataIndexDto::getPublishedStringValue)
						.setHeader(I18nProperties.getCaption("Published Status")).setSortable(true).setResizable(true)
						.setAutoWidth(true)
						.setTooltipGenerator(
								e -> I18nProperties.getCaption("Published Status: ") + e.getPublishedStringValue())
						.setFooter(CampaignFormDataIndexDto.ISPUBLISHED);

				if (campaignPhase.getValue() != null
						&& campaignPhase.getValue().toString().equalsIgnoreCase("post-campaign")) {
					publishedColumn.setVisible(true);
					verifiedColumn.setVisible(true);
				} else {
					publishedColumn.setVisible(false);
					verifiedColumn.setVisible(false);
				}
			}
		}

		grid.setVisible(true);
		grid.setWidthFull();
		grid.setAllRowsVisible(false);

		dataProvider = DataProvider.fromFilteringCallbacks(this::fetchCampaignFormData, this::countCampaignFormData);
		grid.setDataProvider(dataProvider);
		grid.setMultiSort(true);
		grid.setDefaultMultiSortPriority(MultiSortPriority.APPEND);

		if (userProvider.getUser().getUsertype() == UserType.EOC_USER) {
			boolean isPublished = FacadeProvider.getCampaignFacade().isPublished(campaignz.getValue().getUuid());
			if (campaignPhase.getValue().toString().equalsIgnoreCase("POST-CAMPAIGN")) {
				if (isPublished) {
					grid.setSelectionMode(Grid.SelectionMode.NONE);
				}
			} else {
				grid.setSelectionMode(Grid.SelectionMode.SINGLE);
				grid.asSingleSelect().addValueChangeListener(e -> {
					if (e.getValue() != null) {
						if (FacadeProvider.getCampaignFormMetaWithExpFacade()
								.getFormsWithExpiryByCampaignUuidAndFormUuid(criteria.getCampaign().getUuid(),
										criteria.getCampaignFormMeta().getUuid())
								.size() > 0) {
							
								CampaignFormMetaExpiryDto expiryDto = FacadeProvider.getCampaignFormMetaWithExpFacade()
										.getFormsWithExpiryByCampaignUuidAndFormUuid(criteria.getCampaign().getUuid(),
												criteria.getCampaignFormMeta().getUuid())
										.get(0);
								CampaignDto campaign = FacadeProvider.getCampaignFacade()
										.getByUuid(criteria.getCampaign().getUuid()); // campaignz.getValue().getUuid());

								if (checkFormValidityByPhase(e.getValue().getFormType().toString().toLowerCase(), campaign,
										expiryDto)) {
									
									if(e.getValue().getFormType().toString().toLowerCase().equalsIgnoreCase("intra-campaign")) {
										
										if(userProvider.getUser().getUserRoles().contains(UserRole.PUBLISH_USER) 
												|| userProvider.getUser().getUserRoles().contains(UserRole.EDITOR_USER)
												 || userProvider.getUser().getUserRoles().contains(UserRole.ADMIN)) {
												CampaignFormDataDto formData = FacadeProvider.getCampaignFormDataFacade()
														.getCampaignFormDataByUuid(e.getValue().getUuid());

												CampaignFormMetaDto formMeta = FacadeProvider.getCampaignFormMetaFacade()
														.getCampaignFormMetaByUuid(campaignFormCombo.getValue().getUuid());

												CampaignFormDataEditForm cam = new CampaignFormDataEditForm(formData.getCampaignFormMeta(),
														campaignz.getValue(), true, formData.getUuid(), grid, formMeta.getGeographyLevel(),
														campaign, expiryDto, popDto);
											}else {
												if(!e.getValue().isIsvalidated()) {
													CampaignFormDataDto formData = FacadeProvider.getCampaignFormDataFacade()
															.getCampaignFormDataByUuid(e.getValue().getUuid());

													CampaignFormMetaDto formMeta = FacadeProvider.getCampaignFormMetaFacade()
															.getCampaignFormMetaByUuid(campaignFormCombo.getValue().getUuid());

													CampaignFormDataEditForm cam = new CampaignFormDataEditForm(formData.getCampaignFormMeta(),
															campaignz.getValue(), true, formData.getUuid(), grid, formMeta.getGeographyLevel(),
															campaign, expiryDto, popDto);	
												}else {
											
												Notification notification = new Notification();
												notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
												notification.setPosition(Position.TOP_END);
												Div text = new Div(new Text(
														"This Form has been Validated. Please contact System Administrator to update the record."),
														new HtmlComponent("br"),
														new Text("Close this warning to continue working in APMIS."));

												Button closeButton = new Button(new Icon("lumo", "cross"));
												closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
												//							closeButton.setAriaLabel("Close");
												closeButton.addClickListener(event -> {
													notification.close();
												});
												}
												
											}
										}else {
									
									

									CampaignFormDataDto formData = FacadeProvider.getCampaignFormDataFacade()
											.getCampaignFormDataByUuid(e.getValue().getUuid());

									CampaignFormMetaDto formMeta = FacadeProvider.getCampaignFormMetaFacade()
											.getCampaignFormMetaByUuid(campaignFormCombo.getValue().getUuid());

									CampaignFormDataEditForm cam = new CampaignFormDataEditForm(
											formData.getCampaignFormMeta(), campaignz.getValue(), true, formData.getUuid(),
											grid, formMeta.getGeographyLevel(), campaign, expiryDto, popDto);
								}
								
								}

						} else {
							Notification notification = new Notification();
							notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
							notification.setPosition(Position.TOP_END);
							Div text = new Div(new Text(
									"This Form is not available for Data Entry. Please contact System Administrator to Specify Entry Period."),
									new HtmlComponent("br"),
									new Text("Close this warning to continue working in APMIS."));

							Button closeButton = new Button(new Icon("lumo", "cross"));
							closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
//						closeButton.setAriaLabel("Close");
							closeButton.addClickListener(event -> {
								notification.close();
							});

							HorizontalLayout layoutx = new HorizontalLayout(text, closeButton);
							layoutx.setAlignItems(Alignment.CENTER);

							notification.add(layoutx);
							notification.open();
						}
					}

				});
			}
		} else {
			
			grid.setSelectionMode(Grid.SelectionMode.SINGLE);

			grid.asSingleSelect().addValueChangeListener(e -> {

			    CampaignFormDataIndexDto selected = e.getValue();

			    // Ignore deselection events triggered by grid.deselectAll().
			    if (selected == null) {
			        return;
			    }

			    if (FacadeProvider.getCampaignFormMetaWithExpFacade()
			            .getFormsWithExpiryByCampaignUuidAndFormUuid(
			                    criteria.getCampaign().getUuid(),
			                    criteria.getCampaignFormMeta().getUuid())
			            .size() > 0) {

			        CampaignFormMetaExpiryDto expiryDto =
			                FacadeProvider.getCampaignFormMetaWithExpFacade()
			                        .getFormsWithExpiryByCampaignUuidAndFormUuid(
			                                criteria.getCampaign().getUuid(),
			                                criteria.getCampaignFormMeta().getUuid())
			                        .get(0);

			        CampaignDto campaign = FacadeProvider.getCampaignFacade()
			                .getByUuid(criteria.getCampaign().getUuid());

			        String formType = selected.getFormType()
			                .toString().toLowerCase();

			        if (checkFormValidityByPhase(formType, campaign, expiryDto)) {
			        	
			        	System.out.println("Form type selected =====================" + formType);

			            if (formType.equalsIgnoreCase("intra-campaign")) {
			            	
				        	System.out.println("intra campaign form  =====================" + formType);
//

			                boolean canEditValidated = userProvider.getUser()
			                        .getUserRoles().contains(UserRole.PUBLISH_USER)
			                        || userProvider.getUser().getUserRoles()
			                                .contains(UserRole.EDITOR_USER)
			                        || userProvider.getUser().getUserRoles()
			                                .contains(UserRole.ADMIN);

				        	System.out.println(canEditValidated + "canEditValidatedintra campaign form  =====================selected.isIsvalidated()" + selected.isIsvalidated());

			                if (canEditValidated && selected.isIsvalidated()||
			                		canEditValidated && !selected.isIsvalidated() || 
			                			!canEditValidated && !selected.isIsvalidated()
			                		) {
			                	
					        	System.out.println(canEditValidated + "canEditValidatedintra campaign form  =====================selected.isIsvalidated()" + selected.isIsvalidated());


			                    CampaignFormDataDto formData =
			                            FacadeProvider.getCampaignFormDataFacade()
			                                    .getCampaignFormDataByUuid(
			                                            selected.getUuid());

			                    CampaignFormMetaDto formMeta =
			                            FacadeProvider.getCampaignFormMetaFacade()
			                                    .getCampaignFormMetaByUuid(
			                                            campaignFormCombo.getValue()
			                                                    .getUuid());

			                    new CampaignFormDataEditForm(
			                            formData.getCampaignFormMeta(),
			                            campaignz.getValue(),
			                            true,
			                            formData.getUuid(),
			                            grid,
			                            formMeta.getGeographyLevel(),
			                            campaign,
			                            expiryDto,
			                            popDto);

			                } else {
			                	
					        	System.out.println(canEditValidated + "invalid for entry canEditValidatedintra campaign form  =====================selected.isIsvalidated()" + selected.isIsvalidated());


					        	Notification notification = Notification.show(
					        	        "This Form has been Validated. Please contact "
					        	                + "System Administrator to update the record.",
					        	        5000,
					        	        Notification.Position.TOP_END);

					        	notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
			                }

			            } else {

			                CampaignFormDataDto formData =
			                        FacadeProvider.getCampaignFormDataFacade()
			                                .getCampaignFormDataByUuid(
			                                        selected.getUuid());

			                CampaignFormMetaDto formMeta =
			                        FacadeProvider.getCampaignFormMetaFacade()
			                                .getCampaignFormMetaByUuid(
			                                        campaignFormCombo.getValue()
			                                                .getUuid());

			                new CampaignFormDataEditForm(
			                        formData.getCampaignFormMeta(),
			                        campaignz.getValue(),
			                        true,
			                        formData.getUuid(),
			                        grid,
			                        formMeta.getGeographyLevel(),
			                        campaign,
			                        expiryDto,
			                        popDto);
			            }

			        } else {

			            Notification notification = new Notification();
			            notification.addThemeVariants(
			                    NotificationVariant.LUMO_ERROR);
			            notification.setPosition(Notification.Position.TOP_END);

			            Div text = new Div(
			                    new Text("This Form is not available for Data Entry. "
			                            + "Please contact System Administrator to "
			                            + "Specify Entry Period."),
			                    new HtmlComponent("br"),
			                    new Text("Close this warning to continue working "
			                            + "in APMIS."));

			            Button closeButton = new Button(new Icon("lumo", "cross"));
			            closeButton.addThemeVariants(
			                    ButtonVariant.LUMO_TERTIARY_INLINE);
			            closeButton.addClickListener(event -> notification.close());

			            HorizontalLayout layoutx =
			                    new HorizontalLayout(text, closeButton);
			            layoutx.setAlignItems(Alignment.CENTER);

			            notification.add(layoutx);
			            notification.open();
			        }
			    }

			    grid.deselectAll();
			});
//
//			grid.setSelectionMode(Grid.SelectionMode.SINGLE);
//			grid.asSingleSelect().addValueChangeListener(e -> {
//
//				if (FacadeProvider.getCampaignFormMetaWithExpFacade().getFormsWithExpiryByCampaignUuidAndFormUuid(
//						criteria.getCampaign().getUuid(), criteria.getCampaignFormMeta().getUuid()).size() > 0) {
//					CampaignFormMetaExpiryDto expiryDto = FacadeProvider.getCampaignFormMetaWithExpFacade()
//							.getFormsWithExpiryByCampaignUuidAndFormUuid(criteria.getCampaign().getUuid(),
//									criteria.getCampaignFormMeta().getUuid())
//							.get(0);
//					CampaignDto campaign = FacadeProvider.getCampaignFacade()
//							.getByUuid(criteria.getCampaign().getUuid()); // campaignz.getValue().getUuid());
////					System.out.println(e.getSource().getSelectionModel().getFirstSelectedItem().get().getFormType()
////							+ "=========================================");
//					
//					if (checkFormValidityByPhase(e.getValue().getFormType().toString().toLowerCase(), campaign,
//							expiryDto)) {
//						
//						if(e.getValue().getFormType().toString().toLowerCase().equalsIgnoreCase("intra-campaign")) {
//							
//						if(userProvider.getUser().getUserRoles().contains(UserRole.PUBLISH_USER) 
//								|| userProvider.getUser().getUserRoles().contains(UserRole.EDITOR_USER)
//								 || userProvider.getUser().getUserRoles().contains(UserRole.ADMIN)) {
//							
//							System.out.println("who user with publish editor or admin access =====================" + e.getValue().isIsvalidated());
//
//								CampaignFormDataDto formData = FacadeProvider.getCampaignFormDataFacade()
//										.getCampaignFormDataByUuid(e.getValue().getUuid());
//
//								CampaignFormMetaDto formMeta = FacadeProvider.getCampaignFormMetaFacade()
//										.getCampaignFormMetaByUuid(campaignFormCombo.getValue().getUuid());
//
//								CampaignFormDataEditForm cam = new CampaignFormDataEditForm(formData.getCampaignFormMeta(),
//										campaignz.getValue(), true, formData.getUuid(), grid, formMeta.getGeographyLevel(),
//										campaign, expiryDto, popDto);
//							}else {
//								System.out.println("who user without publish editor or admin access =====================" + e.getValue().isIsvalidated());
//								if(!e.getValue().isIsvalidated()) {
//									CampaignFormDataDto formData = FacadeProvider.getCampaignFormDataFacade()
//											.getCampaignFormDataByUuid(e.getValue().getUuid());
//
//									CampaignFormMetaDto formMeta = FacadeProvider.getCampaignFormMetaFacade()
//											.getCampaignFormMetaByUuid(campaignFormCombo.getValue().getUuid());
//
//									CampaignFormDataEditForm cam = new CampaignFormDataEditForm(formData.getCampaignFormMeta(),
//											campaignz.getValue(), true, formData.getUuid(), grid, formMeta.getGeographyLevel(),
//											campaign, expiryDto, popDto);	
//								}else {
//							
//								Notification notification = new Notification();
//								notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
//								notification.setPosition(Position.TOP_END);
//								Div text = new Div(new Text(
//										"This Form has been Validated. Please contact System Administrator to update the record."),
//										new HtmlComponent("br"),
//										new Text("Close this warning to continue working in APMIS."));
//
//								Button closeButton = new Button(new Icon("lumo", "cross"));
//								closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
//								//							closeButton.setAriaLabel("Close");
//								closeButton.addClickListener(event -> {
//									notification.close();
//								});
//								}
//								
//							}
//						}else {
//
//							CampaignFormDataDto formData = FacadeProvider.getCampaignFormDataFacade()
//									.getCampaignFormDataByUuid(e.getValue().getUuid());
//
//							CampaignFormMetaDto formMeta = FacadeProvider.getCampaignFormMetaFacade()
//									.getCampaignFormMetaByUuid(campaignFormCombo.getValue().getUuid());
//
//							CampaignFormDataEditForm cam = new CampaignFormDataEditForm(formData.getCampaignFormMeta(),
//									campaignz.getValue(), true, formData.getUuid(), grid, formMeta.getGeographyLevel(),
//									campaign, expiryDto, popDto);	
//						}
//
//
//					} else {
//						Notification notification = new Notification();
//						notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
//						notification.setPosition(Position.TOP_END);
//						Div text = new Div(new Text(
//								"This Form is not available for Data Entry. Please contact System Administrator to Specify Entry Period."),
//								new HtmlComponent("br"), new Text("Close this warning to continue working in APMIS."));
//
//						Button closeButton = new Button(new Icon("lumo", "cross"));
//						closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
////				closeButton.setAriaLabel("Close");
//						closeButton.addClickListener(event -> {
//							notification.close();
//						});
//
//						HorizontalLayout layoutx = new HorizontalLayout(text, closeButton);
//						layoutx.setAlignItems(Alignment.CENTER);
//
//						notification.add(layoutx);
//						notification.open();
//
//					}
//
//				}
//
//				grid.deselectAll();
//
//			});
		}

		exporter = GridExporter.createFor(grid);
		exporter.setAutoAttachExportButtons(false);
		exporter.setTitle(I18nProperties.getCaption(Captions.campaignDataInformation));
		exportFileName = campaignz.getValue().toString() + "_"
				+ campaignFormCombo.getValue().toString().replaceAll("[^a-zA-Z0-9]+", " ") + "_"
				+ new SimpleDateFormat("yyyyddMM").format(Calendar.getInstance().getTime());
		exporter.setFileName(exportFileName);
		anchor.setHref(exporter.getCsvStreamResource());
		anchor.getElement().setAttribute("download", true);
		anchor.setClassName("exportJsonGLoss");
		anchor.setId("campDatAnchor");
		anchor.getStyle().set("width", "100px");

		icon.getStyle().set("margin-right", "8px");
		icon.getStyle().set("font-size", "10px");
		;
		anchor.getElement().insertChild(0, icon.getElement());

		add(grid);
	}
	
	private void checkValidatedStatusAndUserPermissions(CampaignFormDataCriteria criteria) {
		if (userProvider.getUser().getUsertype() == UserType.WHO_USER) {
			if (userProvider.getUser().getUserRoles().contains(UserRole.PUBLISH_USER) || userProvider.getUser().getUserRoles().contains(UserRole.EDITOR_USER) ) {
				
			}else {
				
			}

		}
	}

	private void configureColumnStyles(CampaignFormDataCriteria criteria) {
		Date formExpiryDate = FacadeProvider.getCampaignFormMetaFacade().formExpiryDate(criteria);
		// TODO Auto-generated();
		grid.setClassNameGenerator((v) -> {

			if (v.getCreatingUser() != null) {
				List<CampaignFormDataIndexDto> creatingUserType = FacadeProvider.getCampaignFormDataFacade()
						.getCreatingUsersUserType(v.getCreatingUser().toString());

				for (CampaignFormDataIndexDto xx : creatingUserType) {
					return "WHODATA";
				}
			}

			if (formExpiryDate != null) {
				if (v.getFormDate() != null && v.getFormDate().after((Date) formExpiryDate)) {

					return "lateData";
				} else {
					return "";
				}
			} else {
				return "";
			}

		});

		if (campaignFormCombo.getValue() != null) {

			CampaignFormMetaDto formData = FacadeProvider.getCampaignFormMetaFacade()
					.getCampaignFormMetaByUuid(campaignFormCombo.getValue().getUuid());

			CampaignFormMetaGeographyLevel formGeographyLevel = formData.getGeographyLevel();

			if (formGeographyLevel.equals(CampaignFormMetaGeographyLevel.REGION)) {

//				if (clusterNameColumn != null) {
				clusterNameColumn.setVisible(false);
				clusterNumberColumn.setVisible(false);
				ccodeColumn.setVisible(false);
//				}

//				if (districtNameColumn != null) {
				districtNameColumn.setVisible(false);
				dCodeColumn.setVisible(false);
//				}

				if (provinceNameColumn != null) {
					provinceNameColumn.setVisible(false);
					pCodeColumn.setVisible(false);
				}

			} else if (formGeographyLevel.equals(CampaignFormMetaGeographyLevel.PROVINCE)) {
				if (clusterNameColumn != null) {
					clusterNameColumn.setVisible(false);
					clusterNumberColumn.setVisible(false);
					ccodeColumn.setVisible(false);
				}

				if (districtNameColumn != null) {
					districtNameColumn.setVisible(false);
					dCodeColumn.setVisible(false);
				}

			} else if (formGeographyLevel.equals(CampaignFormMetaGeographyLevel.DISTRICT)) {
				if (clusterNameColumn != null) {
					clusterNameColumn.setVisible(false);
					clusterNumberColumn.setVisible(false);
					ccodeColumn.setVisible(false);
				}
			} else {
				clusterNameColumn.setVisible(true);
				clusterNumberColumn.setVisible(true);
				ccodeColumn.setVisible(true);
			}
		}

	}

	public void addCustomColumn(String property, String caption, String fieldsType, List<MapperUtil> options) {
		if (!property.toString().contains("readonly")) {
			if (fieldsType != null && fieldsType.equalsIgnoreCase("image")) {
				Column<CampaignFormDataIndexDto> imageColumn = grid.addColumn(new ComponentRenderer<>(item -> {
					CampaignFormDataEntry formValue = item.getFormValues().stream()
							.filter(v -> property.equals(v.getId())).findFirst().orElse(null);

					if (formValue == null || formValue.getValue() == null) {
						return new Span("No image");
					}

					Object val = formValue.getValue();
					String imageId = null;

					// Single image (Map)
					if (val instanceof Map) {
						Map<?, ?> map = (Map<?, ?>) val;
						Object id = map.get("imageId");
						if (id != null) {
							imageId = id.toString();
						}
					}
					// Multiple images (List) — use the first one for the thumbnail
					else if (val instanceof List) {
						List<?> list = (List<?>) val;
						if (!list.isEmpty()) {
							Object first = list.get(0);
							if (first instanceof Map) {
								Map<?, ?> map = (Map<?, ?>) first;
								Object id = map.get("imageId");
								if (id != null) {
									imageId = id.toString();
								}
							}
						}
					}

					if (imageId == null || imageId.isEmpty()) {
						return new Span("No image");
					}

					StreamResource resource = buildImageResource(imageId);
					if (resource == null) {
						return new Span("No image");
					}

					Image thumbnail = new Image(resource, "Thumbnail"); // ✅ uses the StreamResource
					thumbnail.setWidth("40px");
					thumbnail.setHeight("40px");
					thumbnail.getStyle().set("object-fit", "cover");
					thumbnail.getStyle().set("cursor", "pointer");
					return thumbnail;
				})).setHeader(caption).setSortable(false).setResizable(true).setAutoWidth(true)
						.setTooltipGenerator(e -> "Image: " + caption);

				if (exporter != null) {
					exporter.setExportValue(imageColumn, item -> exportImageNames(item, property));
				}

				return;
			}

			Grid.Column<CampaignFormDataIndexDto> column = grid.addColumn(e -> {
				CampaignFormDataEntry formValue = e.getFormValues().stream().filter(v -> v.getId().equals(property))
						.findFirst().orElse(null);
				if (property.equalsIgnoreCase("villagecode")) {
					return formValue.getValue().toString();
				} else if (fieldsType != null && (fieldsType.equalsIgnoreCase("number"))) {
					if (formValue != null) {
						return removeTrailingDecimalFromString(formValue.getValue().toString()); // v.getId().equals(property)).findFirst().orElse(null));
					} else {
						return "";
					}
				} else if (fieldsType != null && (fieldsType.equalsIgnoreCase("yes_no"))) {
					if (formValue != null) {
						if (formValue.toString().equalsIgnoreCase("Yes")
								|| formValue.toString().equalsIgnoreCase("True")) {
							return removeTrailingDecimalFromString("Yes");// (e.getFormValues().stream().filter(v ->
																			// v.getId().equals(property)).findFirst().orElse(null));
						} else if (formValue.toString().equalsIgnoreCase("No")
								|| formValue.toString().equalsIgnoreCase("False")) {
							return removeTrailingDecimalFromString("No");// (e.getFormValues().stream().filter(v ->
																			// v.getId().equals(property)).findFirst().orElse(null));
						} else {
							return "";// (e.getFormValues().stream().filter(v ->
										// v.getId().equals(property)).findFirst().orElse(null));
						}
					} else {
						return "";
					}
				} else if (fieldsType != null && (fieldsType.equalsIgnoreCase("dropdown"))) {
					String formValueCaption = "";
					for (MapperUtil valueCaption : options) {
						if (formValue != null
								&& formValue.getValue().toString().equalsIgnoreCase(valueCaption.getKey())) {
							formValueCaption = valueCaption.getCaption();
						}
					}
					return formValueCaption;

				} else if (fieldsType != null && fieldsType.equalsIgnoreCase("image")) {

					if (formValue == null || formValue.getValue() == null) {
						return new Span("No image");
					}

					Object val = formValue.getValue();
					String imageId = null;

					// Single image (Map)
					if (val instanceof Map) {
						Map<?, ?> map = (Map<?, ?>) val;
						Object id = map.get("imageId");
						if (id != null) {
							imageId = id.toString();
						}
					}
					// Multiple images (List) — use the first for the thumbnail
					else if (val instanceof List) {
						List<?> list = (List<?>) val;
						if (!list.isEmpty()) {
							Object first = list.get(0);
							if (first instanceof Map) {
								Map<?, ?> map = (Map<?, ?>) first;
								Object id = map.get("imageId");
								if (id != null) {
									imageId = id.toString();
								}
							}
						}
					}

					if (imageId == null || imageId.isEmpty()) {
						return new Span("No image");
					}

					StreamResource resource = buildImageResource(imageId);
					if (resource == null) {
						return new Span("No image");
					}

					Image thumbnail = new Image(resource, "Image thumbnail"); // ✅ fixed: use the StreamResource
					thumbnail.setWidth("40px");
					thumbnail.setHeight("40px");
					thumbnail.getStyle().set("object-fit", "cover");
					thumbnail.getElement().addEventListener("click", ex -> {
						// optional: open a larger preview here
					});
					return thumbnail;

				} else {
					return removeTrailingDecimal(e.getFormValues().stream().filter(v -> v.getId().equals(property))
							.findFirst().orElse(null));
				}
			}).setHeader(caption).setFooter(property).setSortProperty(property).setSortable(false).setResizable(true)
					.setAutoWidth(true).setTooltipGenerator(e -> {
						CampaignFormDataEntry formValue = e.getFormValues().stream()
								.filter(v -> v.getId().equals(property)).findFirst().orElse(null);
						String value;

						if (fieldsType != null && (fieldsType.equalsIgnoreCase("number"))) {
							if (formValue != null) {
								System.out.println(formValue.getValue().toString().toLowerCase()
										+ "  ----------------formValue.getValue().toString().toLowerCase() ");
								return value = removeTrailingDecimalFromString(formValue.getValue().toString());// (e.getFormValues().stream().filter(v
																												// ->
																												// v.getId().equals(property)).findFirst().orElse(null));
							} else {
								return value = "";
							}
						} else {
							value = removeTrailingDecimal(e.getFormValues().stream()
									.filter(v -> v.getId().equals(property)).findFirst().orElse(null));
						}
						return caption + " : " + value;
					}).setClassNameGenerator(item -> "full-width-column");

			if (fieldsType != null && fieldsType.equalsIgnoreCase("textbox")) {
				column.setAutoWidth(false);
				column.setFlexGrow(0);
				int width = Math.max(80, caption.length() * 8 + 30);
				column.setWidth(width + "px");
//			    column.getStyle().set("white-space", "nowrap");
			}
		}

	}

	private StreamResource buildImageResource(String imageId) {
		if (StringUtils.isBlank(imageId)) {
			return null;
		}

		StreamResource resource = new StreamResource("image_" + imageId, () -> {
			try {
				byte[] bytes = FacadeProvider.getCampaignFormImageFacade().readImage(imageId);
				if (bytes == null || bytes.length == 0) {
					return new ByteArrayInputStream(new byte[0]);
				}
				return new ByteArrayInputStream(bytes);
			} catch (IOException e) {
				return new ByteArrayInputStream(new byte[0]);
			}
		});

		try {
			DocumentDto doc = FacadeProvider.getDocumentFacade().getDocumentByUuid(imageId);
			if (doc != null && StringUtils.isNotBlank(doc.getMimeType())) {
				resource.setContentType(doc.getMimeType());
			} else {
				resource.setContentType("image/jpeg");
			}
		} catch (Exception e) {
			resource.setContentType("image/jpeg");
		}

		return resource;
	}

	private String exportImageNames(CampaignFormDataIndexDto item, String property) {
		CampaignFormDataEntry formValue = item.getFormValues().stream().filter(v -> property.equals(v.getId()))
				.findFirst().orElse(null);
		if (formValue == null || formValue.getValue() == null) {
			return "";
		}
		return extractImageValues(formValue.getValue()).stream().map(img -> exportImageFileName(img, property))
				.filter(StringUtils::isNotBlank).collect(Collectors.joining(", "));
	}

	private String imageDisplayName(CampaignFormImageValue img) {
		return StringUtils.firstNonBlank(img.getGeneratedFileName(), img.getOriginalFileName(), img.getImageId());
	}

	private List<CampaignFormImageValue> extractImageValues(Object val) {
		List<CampaignFormImageValue> result = new ArrayList<>();
		if (val instanceof Map) {
			CampaignFormImageValue v = CampaignFormImageValue.fromMap((Map<?, ?>) val);
			if (v != null)
				result.add(v);
		} else if (val instanceof List) {
			for (Object o : (List<?>) val) {
				if (o instanceof Map) {
					CampaignFormImageValue v = CampaignFormImageValue.fromMap((Map<?, ?>) o);
					if (v != null)
						result.add(v);
				}
			}
		}
		return result;
	}

	public void addCustomColumn(String property, String caption, String fieldsType) {
		if (!property.toString().contains("readonly")) {
			grid.addColumn(e -> {
				CampaignFormDataEntry formValue = e.getFormValues().stream().filter(v -> v.getId().equals(property))
						.findFirst().orElse(null);
				if (fieldsType != null && (fieldsType.equalsIgnoreCase("number"))) {
					if (formValue != null) {
						return removeTrailingDecimalFromString(formValue.getValue().toString());// (e.getFormValues().stream().filter(v
																								// ->
																								// v.getId().equals(property)).findFirst().orElse(null));
					} else {
						return "";
					}
				} else if (fieldsType != null && (fieldsType.equalsIgnoreCase("yes_no"))) {
					if (formValue != null) {
						if (formValue.toString().equalsIgnoreCase("Yes")
								|| formValue.toString().equalsIgnoreCase("True")) {
							return removeTrailingDecimalFromString("Yes");// (e.getFormValues().stream().filter(v ->
																			// v.getId().equals(property)).findFirst().orElse(null));
						} else if (formValue.toString().equalsIgnoreCase("No")
								|| formValue.toString().equalsIgnoreCase("False")) {
							return removeTrailingDecimalFromString("No");// (e.getFormValues().stream().filter(v ->
																			// v.getId().equals(property)).findFirst().orElse(null));
						} else {
							return "";// (e.getFormValues().stream().filter(v ->
										// v.getId().equals(property)).findFirst().orElse(null));
						}
					} else {
						return "";
					}
				} else if (fieldsType != null && (fieldsType.equalsIgnoreCase("dropdown"))) {

					System.out.println(fieldsType + "fieldsType" + property + "property" + caption);
					return "";
				} else {
					return removeTrailingDecimal(e.getFormValues().stream().filter(v -> v.getId().equals(property))
							.findFirst().orElse(null));

				}
			}).setHeader(caption).setFooter(property).setSortProperty(property).setSortable(false).setResizable(true)
					.setAutoWidth(true).setTooltipGenerator(e -> {
						CampaignFormDataEntry formValue = e.getFormValues().stream()
								.filter(v -> v.getId().equals(property)).findFirst().orElse(null);
						String value;

						if (fieldsType != null && (fieldsType.equalsIgnoreCase("number"))) {
							if (formValue != null) {
								System.out.println(formValue.getValue().toString().toLowerCase()
										+ "  ----------------formValue.getValue().toString().toLowerCase() ");
								return value = removeTrailingDecimalFromString(formValue.getValue().toString());// (e.getFormValues().stream().filter(v
																												// ->
																												// v.getId().equals(property)).findFirst().orElse(null));
							} else {
								return value = "";
							}
						} else {
							value = removeTrailingDecimal(e.getFormValues().stream()
									.filter(v -> v.getId().equals(property)).findFirst().orElse(null));
						}
						return caption + " : " + value;
					}).setClassNameGenerator(item -> "full-width-column");
		}

	}

	private String removeTrailingDecimal(CampaignFormDataEntry value) {
		String valueCleaned = value == null ? null : value.toString();
		if (valueCleaned != null && valueCleaned.endsWith(".0")) {
			return valueCleaned.substring(0, valueCleaned.length() - 2);
		}
		return valueCleaned;
	}

	private String removeTrailingDecimalFromString(String value) {
		String valueCleaned = value == null ? null : value.toString();
		if (valueCleaned != null && valueCleaned.endsWith(".0")) {
			return valueCleaned.substring(0, valueCleaned.length() - 2);
		}
		return valueCleaned;
	}

	public void updateRowCount() {
		languageHandler();
		int numberOfRows = (int) FacadeProvider.getCampaignFormDataFacade().count(criteria);
		String newText = I18nProperties.getCaption(Captions.rows) + numberOfRows;

		countRowItems.setText(newText);
		countRowItems.setId("rowCount");
	}

	public void languageHandler() {

		if (I18nProperties.getUserLanguage() == null) {

			I18nProperties.setUserLanguage(Language.EN);
		} else {

			I18nProperties.setUserLanguage(userProvider.getUser().getLanguage());
			I18nProperties.getUserLanguage();
		}
		FacadeProvider.getI18nFacade().setUserLanguage(userProvider.getUser().getLanguage());
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public @NotNull ViewModelProviders getViewModelProviders() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserProvider getUserProvider() {
		// TODO Auto-generated method stub
		return null;
	}

}