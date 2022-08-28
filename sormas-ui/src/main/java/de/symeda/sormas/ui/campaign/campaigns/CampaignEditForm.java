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

package de.symeda.sormas.ui.campaign.campaigns;

import static de.symeda.sormas.ui.utils.CssStyles.H3;
import static de.symeda.sormas.ui.utils.LayoutUtil.fluidRowLocs;
import static de.symeda.sormas.ui.utils.LayoutUtil.loc;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.UserProvider;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.v7.data.util.converter.Converter;
import com.vaadin.v7.data.util.converter.StringToIntegerConverter;
import com.vaadin.v7.ui.ComboBox;
import com.vaadin.v7.ui.DateField;
import com.vaadin.v7.ui.OptionGroup;
import com.vaadin.v7.ui.TextArea;
import com.vaadin.v7.ui.TextField;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.campaign.CampaignDto;
import de.symeda.sormas.api.campaign.diagram.CampaignDashboardElement;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaReferenceDto;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.ui.utils.AbstractEditForm;
import de.symeda.sormas.ui.utils.AbstractEditableGrid;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateComparisonValidator;
import de.symeda.sormas.ui.utils.FieldHelper;

public class CampaignEditForm extends AbstractEditForm<CampaignDto> {

	private static final long serialVersionUID = 7762204114905664597L;

	private static final String STATUS_CHANGE = "statusChange";
	private static final String CAMPAIGN_BASIC_HEADING_LOC = "campaignBasicHeadingLoc";
	private static final String USAGE_INFO = "usageInfo";
	private static final String ROUND_COMPONETS = "roundComponet";
	private static final String CAMPAIGN_TYPE_LOC = "typeLocation";
	private static final String CAMPAIGN_TYPE_SEARCH = "typeLocationSearch";
	private static final String CAMPAIGN_DATA_LOC = "campaignDataLoc";
	private static final String CAMPAIGN_DASHBOARD_LOC = "campaignDashboardLoc";
	private static final String SPACE_LOC = "spaceLoc";
	private static final String SPACE_LOCX = "spaceLocx";
	
	private static final String PRE_CAMPAIGN = "pre-campaign";
	private static final String INTRA_CAMPAIGN = "intra-campaign";
	private static final String POST_CAMPAIGN = "post-campaign";
	
	private OptionGroup clusterfieldx;

	private static final String HTML_LAYOUT = loc(CAMPAIGN_BASIC_HEADING_LOC)
		+ fluidRowLocs(CampaignDto.UUID, CampaignDto.CREATING_USER, CampaignDto.CAMPAIGN_YEAR)
		+ fluidRowLocs(CampaignDto.NAME, CampaignDto.ROUND) 
		+ fluidRowLocs(CampaignDto.START_DATE, CampaignDto.END_DATE)
		+ fluidRowLocs(CampaignDto.DESCRIPTION)
		+ fluidRowLocs(SPACE_LOCX)
		+ fluidRowLocs(CampaignDto.CAMPAIGN_TYPES)
		+ fluidRowLocs(USAGE_INFO)
		+ fluidRowLocs(CAMPAIGN_TYPE_LOC)
		+ fluidRowLocs(CAMPAIGN_TYPE_SEARCH)
		+ fluidRowLocs(ROUND_COMPONETS)
		+ fluidRowLocs(CAMPAIGN_DATA_LOC)
		+ fluidRowLocs(CAMPAIGN_DASHBOARD_LOC)
		+ fluidRowLocs(SPACE_LOC);

	private final VerticalLayout statusChangeLayout;
	private Boolean isCreateForm = null;
	private CampaignDto campaignDto;
	
	
	private CampaignFormsGridComponent campaignFormsGridComponent;
	private CampaignFormsGridComponent campaignFormsGridComponent_1;
	private CampaignFormsGridComponent campaignFormsGridComponent_2;
	
	private CampaignDashboardElementsGridComponent campaignDashboardGridComponent;
	private CampaignDashboardElementsGridComponent campaignDashboardGridComponent_1;
	private CampaignDashboardElementsGridComponent campaignDashboardGridComponent_2;

	public CampaignEditForm(CampaignDto campaignDto) {

		super(CampaignDto.class, CampaignDto.I18N_PREFIX);
		setWidth(1280, Unit.PIXELS);

		this.campaignDto = campaignDto;
		isCreateForm = campaignDto == null;
		if (isCreateForm) {
			hideValidationUntilNextCommit();
		}
		statusChangeLayout = new VerticalLayout();
		statusChangeLayout.setSpacing(false);
		statusChangeLayout.setMargin(false);
		getContent().addComponent(statusChangeLayout, STATUS_CHANGE);

		addFields();
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void addFields() {

		if (isCreateForm == null) {
			System.out.print("siCreateForm=null");
			return;
		}
		
		System.out.print("isCreateForm=true");

		Label campaignBasicHeadingLabel = new Label(I18nProperties.getString(Strings.headingCampaignBasics));
		campaignBasicHeadingLabel.addStyleName(H3);
		getContent().addComponent(campaignBasicHeadingLabel, CAMPAIGN_BASIC_HEADING_LOC);

		addField(CampaignDto.UUID);
		addField(CampaignDto.CREATING_USER);

		DateField startDate = addField(CampaignDto.START_DATE, DateField.class);
		startDate.removeAllValidators();
		
		DateField endDate = addField(CampaignDto.END_DATE, DateField.class);
		endDate.removeAllValidators();
		
		TextField textField = addField(CampaignDto.CAMPAIGN_YEAR);
		textField.setReadOnly(true);

		startDate.addValueChangeListener(e -> {
			textField.setReadOnly(false);
			textField.setValue(DateGetYear(startDate.getValue())+" ");
			textField.setReadOnly(true);		
		});
		
		
		startDate.addValidator(
			new DateComparisonValidator(
				startDate,
				endDate,
				true,
				true,
				I18nProperties.getValidationError(Validations.beforeDate, startDate.getCaption(), endDate.getCaption())));
		endDate.addValidator(
			new DateComparisonValidator(
				endDate,
				startDate,
				false,
				true,
				I18nProperties.getValidationError(Validations.afterDate, endDate.getCaption(), startDate.getCaption())));

		addField(CampaignDto.NAME);
		
		ComboBox clusterfield = addField(CampaignDto.ROUND, ComboBox.class);
		clusterfield.addItem("NID");
		clusterfield.addItem("SNID");
		clusterfield.addItem("Case Respond");
		clusterfield.addItem("Mopping-Up");
		
		TextArea description = addField(CampaignDto.DESCRIPTION, TextArea.class);
		description.setRows(6);
		
		
		
		final Label spacerx = new Label();
		spacerx.setHeight("10%");
		getContent().addComponent(spacerx, SPACE_LOCX);

		setReadOnly(true, CampaignDto.UUID, CampaignDto.CREATING_USER);
		setVisible(!isCreateForm, CampaignDto.UUID, CampaignDto.CREATING_USER);

		setRequired(true, CampaignDto.UUID, CampaignDto.NAME, CampaignDto.CREATING_USER, CampaignDto.START_DATE, CampaignDto.END_DATE, CampaignDto.ROUND, CampaignDto.CAMPAIGN_YEAR);

		FieldHelper.addSoftRequiredStyle(description);
		final HorizontalLayout usageLayout = new HorizontalLayout();
		usageLayout.setWidthFull();
		Label usageLabel = new Label(
				VaadinIcons.INFO_CIRCLE.getHtml() + " " + I18nProperties.getString(Strings.infoUsageOfEditableCampaignGrids),
				ContentMode.HTML);
		usageLabel.setWidthFull();
		usageLayout.addComponent(usageLabel);
		usageLayout.setSpacing(true);
		usageLayout.setMargin(new MarginInfo(true, false, true, false));
		getContent().addComponent(usageLayout, USAGE_INFO);
		
		
		final HorizontalLayout layoutParent = new HorizontalLayout();
		layoutParent.setWidthFull();
		
		
		TabSheet tabsheetParent = new TabSheet();
		layoutParent.addComponent(tabsheetParent);
		
		
		VerticalLayout parentTab1 = new VerticalLayout();
		final HorizontalLayout layout = new HorizontalLayout();
		layout.setWidthFull();
		
		
		TabSheet tabsheet = new TabSheet();
		layout.addComponent(tabsheet);

		// Create the first tab
		VerticalLayout tab1 = new VerticalLayout();
		
		campaignFormsGridComponent = new CampaignFormsGridComponent(
				this.campaignDto == null ? Collections.EMPTY_LIST : new ArrayList<>(campaignDto.getCampaignFormMetas("pre-campaign")),
				FacadeProvider.getCampaignFormMetaFacade().getAllCampaignFormMetasAsReferencesByRound(PRE_CAMPAIGN));
			getContent().addComponent(campaignFormsGridComponent, CAMPAIGN_DATA_LOC);
		tab1.addComponent(campaignFormsGridComponent);
		tab1.setCaption("Pre Campaign Forms");
		tabsheet.addTab(tab1);
		
		
	//	campaignFormsGridComponent.ListnerCampaignFilter(event);
		

		// This tab gets its caption from the component caption
		VerticalLayout tab2 = new VerticalLayout();
		
		//To Do: Check why set this to nulll in the first place
		final List<CampaignDashboardElement> campaignDashboardElements = FacadeProvider.getCampaignFacade().getCampaignDashboardElements(null, PRE_CAMPAIGN);
		campaignDashboardGridComponent = new CampaignDashboardElementsGridComponent(
			this.campaignDto == null
				? Collections.EMPTY_LIST
				: FacadeProvider.getCampaignFacade().getCampaignDashboardElements(campaignDto.getUuid(), PRE_CAMPAIGN),
			campaignDashboardElements);
		getContent().addComponent(campaignDashboardGridComponent, CAMPAIGN_DASHBOARD_LOC);
		tab2.addComponent(campaignDashboardGridComponent);
		tab2.setCaption("Pre Campaign Dashboard");
		tabsheet.addTab(tab2);
		

		


		
		getContent().addComponent(layout, ROUND_COMPONETS);
		
		
		
		
		parentTab1.addComponent(layout);
		parentTab1.setCaption("Pre-Campaign Phase");
		tabsheetParent.addTab(parentTab1);
		
		

		//start of a child campaign
		
		
		VerticalLayout parentTab3 = new VerticalLayout();
		
		final HorizontalLayout layoutPost = new HorizontalLayout();
		layoutPost.setWidthFull();
		
		
		TabSheet tabsheetPost = new TabSheet();
		layoutPost.addComponent(tabsheetPost);

		// Create the first tab
		VerticalLayout tab1Post = new VerticalLayout();
		
		campaignFormsGridComponent_1 = new CampaignFormsGridComponent(
				this.campaignDto == null ? Collections.EMPTY_LIST : new ArrayList<>(campaignDto.getCampaignFormMetas("intra-campaign")),
				FacadeProvider.getCampaignFormMetaFacade().getAllCampaignFormMetasAsReferencesByRound(INTRA_CAMPAIGN));
			getContent().addComponent(campaignFormsGridComponent_1, CAMPAIGN_DATA_LOC);
			tab1Post.addComponent(campaignFormsGridComponent_1);
			tab1Post.setCaption("Intra Campaign Forms");
			tabsheetPost.addTab(tab1Post);
		

		// This tab gets its caption from the component caption
		VerticalLayout tab2Post = new VerticalLayout();
		final List<CampaignDashboardElement> campaignDashboardElementsxx = FacadeProvider.getCampaignFacade().getCampaignDashboardElements(null, INTRA_CAMPAIGN);
		campaignDashboardGridComponent_1 = new CampaignDashboardElementsGridComponent(
			this.campaignDto == null
				? Collections.EMPTY_LIST
				: FacadeProvider.getCampaignFacade().getCampaignDashboardElements(campaignDto.getUuid(), INTRA_CAMPAIGN),
			campaignDashboardElementsxx);
		getContent().addComponent(campaignDashboardGridComponent_1, CAMPAIGN_DASHBOARD_LOC);
		tab2Post.addComponent(campaignDashboardGridComponent_1);
		tab2Post.setCaption("Intra Campaign Dashboard");
		tabsheetPost.addTab(tab2Post);
		
		tabsheetPost.addSelectedTabChangeListener(event -> campaignFormsGridComponent.ListnerCampaignFilter(event));
	
		getContent().addComponent(layoutPost, ROUND_COMPONETS);
		
		
		
		
		parentTab3.addComponent(layoutPost);
		parentTab3.setCaption("Intra-Campaign Phase");
		tabsheetParent.addTab(parentTab3);
		tabsheetParent.addSelectedTabChangeListener(event -> campaignFormsGridComponent.ListnerCampaignFilter(event));
		//stop
		


		//start of a child campaign
		
		
		VerticalLayout parentTab2 = new VerticalLayout();
		
		final HorizontalLayout layoutIntra = new HorizontalLayout();
		layoutIntra.setWidthFull();
		
		
		TabSheet tabsheetIntra = new TabSheet();
		layoutIntra.addComponent(tabsheetIntra);

		// Create the first tab
		VerticalLayout tab1Intra = new VerticalLayout();
		
		campaignFormsGridComponent_2 = new CampaignFormsGridComponent(
				this.campaignDto == null ? Collections.EMPTY_LIST : new ArrayList<>(campaignDto.getCampaignFormMetas("post-campaign")),
						FacadeProvider.getCampaignFormMetaFacade().getAllCampaignFormMetasAsReferencesByRound(POST_CAMPAIGN));
				//FacadeProvider.getCampaignFormMetaFacade().getAllCampaignFormMetasAsReferences());
			getContent().addComponent(campaignFormsGridComponent_2, CAMPAIGN_DATA_LOC);
			tab1Intra.addComponent(campaignFormsGridComponent_2);
			tab1Intra.setCaption("Post Campaign Forms");
		tabsheetIntra.addTab(tab1Intra);
		

		// This tab gets its caption from the component caption
		VerticalLayout tab2Intra = new VerticalLayout();
		final List<CampaignDashboardElement> campaignDashboardElementsx = FacadeProvider.getCampaignFacade().getCampaignDashboardElements(null, POST_CAMPAIGN);
		campaignDashboardGridComponent_2 = new CampaignDashboardElementsGridComponent(
			this.campaignDto == null
				? Collections.EMPTY_LIST
				: FacadeProvider.getCampaignFacade().getCampaignDashboardElements(campaignDto.getUuid(), POST_CAMPAIGN),
			campaignDashboardElementsx);
		getContent().addComponent(campaignDashboardGridComponent_2, CAMPAIGN_DASHBOARD_LOC);
		tab2Intra.addComponent(campaignDashboardGridComponent_2);
		tab2Intra.setCaption("Post Campaign Dashboard");
		tabsheetIntra.addTab(tab2Intra);
		
		tabsheetIntra.addSelectedTabChangeListener(event -> campaignFormsGridComponent.ListnerCampaignFilter(event));

		getContent().addComponent(layoutIntra, ROUND_COMPONETS);
		
		
		
		
		parentTab2.addComponent(layoutIntra);
		parentTab2.setCaption("Post-Campaign Phase");
		tabsheetParent.addTab(parentTab2);
		
		//stop
		
		//style todo
		tabsheetParent.setPrimaryStyleName("view-header"); 
		

		getContent().addComponent(layoutParent, CAMPAIGN_TYPE_LOC);
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		/*
	
		clusterfieldx = new OptionGroup(); 
		clusterfieldx = addField(CampaignDto.CAMPAIGN_TYPES, OptionGroup.class);
		CssStyles.style(clusterfieldx, ValoTheme.OPTIONGROUP_HORIZONTAL, CssStyles.OPTIONGROUP_HORIZONTAL_PRIMARY);
		clusterfieldx.addItem("Pre-Campaign");
		clusterfieldx.addItem("Intra-Campaign");
		clusterfieldx.addItem("Post-Campaign");
		clusterfieldx.setDescription("Campaign Switch");
		clusterfieldx.setEnabled(true);
		
		clusterfieldx.addValueChangeListener(new ValueChangeListener() {
			
			@Override
			public void valueChange(com.vaadin.v7.data.Property.ValueChangeEvent event) {
				// TODO Auto-generated method stub
				//Notification.show("DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDd"+ event.getProperty().getValue());
				//System.out.println("dddddddddddddddddddddddddlkjhguijhghj " + clusterfieldx.getCaption());
				
				
				Page.getCurrent().getJavaScript().execute("documentGetElementById('formidx').display = 'none'; alert();");
				//	//"$('#formidx').find('td:contains('Void')').parent('tr').hide();\n"
				//'alert();'
				//);
				
				campaignFormsGridComponent.ListnerCampaignFilter(event);
				
				
				
				//campaignFormsGridComponent.addItemClickListener
				
				
				
				//final ArrayList<CampaignFormMetaReferenceDto> gridItems = wired.getItems();
				
				//System.out.println("ddddddddddddddddddddddddddddddddddddddddddddddddddlkjhguijhghj " + gridItems);
				
				//gridItems.add(new CampaignFormMetaReferenceDto(null, " --Please select--"));
				
			}
			});
		
		
		
		//getContent().addComponent(clusterfieldx, CAMPAIGN_TYPE_SEARCH);
		
		
		
		campaignFormsGridComponent = new CampaignFormsGridComponent(
			this.campaignDto == null ? Collections.EMPTY_LIST : new ArrayList<>(campaignDto.getCampaignFormMetas()),
			FacadeProvider.getCampaignFormMetaFacade().getAllCampaignFormMetasAsReferencesByRound("intra-campaign")
			);
		System.out.println("+++++++++++++++++++++++++++++++++  ");	
		
		
		getContent().addComponent(campaignFormsGridComponent, CAMPAIGN_DATA_LOC);

		
		
		final List<CampaignDashboardElement> campaignDashboardElements = FacadeProvider.getCampaignFacade().getCampaignDashboardElements(null);
		campaignDashboardGridComponent = new CampaignDashboardElementsGridComponent(
			this.campaignDto == null
				? Collections.EMPTY_LIST
				: FacadeProvider.getCampaignFacade().getCampaignDashboardElements(campaignDto.getUuid()),
			campaignDashboardElements);
		getContent().addComponent(campaignDashboardGridComponent, CAMPAIGN_DASHBOARD_LOC);

		
		*/
		
		
		final Label spacer = new Label();
		getContent().addComponent(spacer, SPACE_LOC);
	}

	@Override
	public CampaignDto getValue() {
		final CampaignDto campaignDto = super.getValue();
		 HashSet<CampaignFormMetaReferenceDto> set = new HashSet<>();
		 set.addAll(campaignFormsGridComponent.getItems());
		 set.addAll(campaignFormsGridComponent_1.getItems());
		 set.addAll(campaignFormsGridComponent_2.getItems());
		 
		campaignDto.setCampaignFormMetas(new HashSet<>(set));
		
		
		 List<CampaignDashboardElement> setDashboard = new ArrayList<>();
		 setDashboard.addAll(campaignDashboardGridComponent.getItems());
		 setDashboard.addAll(campaignDashboardGridComponent_1.getItems());
		 setDashboard.addAll(campaignDashboardGridComponent_2.getItems());
		 
		 
		campaignDto.setCampaignDashboardElements(setDashboard);
		return campaignDto;
	}

	@Override
	public void setValue(CampaignDto newFieldValue) throws ReadOnlyException, Converter.ConversionException {
		
	//	newFieldValue.getCampaignFormMetas().removeIf(n -> (n.getCaption().contains("ICM")));
		
		
		System.out.println("+++++++");
		
		
		super.setValue(newFieldValue);
		campaignFormsGridComponent.setSavedItems(
				newFieldValue.getCampaignFormMetas() != null ? new ArrayList<>(newFieldValue.getCampaignFormMetas("pre-campaign")) : new ArrayList<>()
						);
		
		campaignFormsGridComponent_1.setSavedItems(
				newFieldValue.getCampaignFormMetas() != null ? new ArrayList<>(newFieldValue.getCampaignFormMetas("intra-campaign")) : new ArrayList<>()
						);
		
		campaignFormsGridComponent_2.setSavedItems(
				newFieldValue.getCampaignFormMetas() != null ? new ArrayList<>(newFieldValue.getCampaignFormMetas("post-campaign")) : new ArrayList<>()
						);

		if (CollectionUtils.isNotEmpty(newFieldValue.getCampaignDashboardElements())) {
			
			campaignDashboardGridComponent.setSavedItems(
				newFieldValue.getCampaignDashboardElements().stream().filter(e -> e.getPhase().equals("pre-campaign")).collect(Collectors.toList())
					.stream()
					.sorted(Comparator.comparingInt(CampaignDashboardElement::getOrder))
					.collect(Collectors.toList()));
			
			
			campaignDashboardGridComponent_1.setSavedItems(
					newFieldValue.getCampaignDashboardElements().stream().filter(e -> e.getPhase().equals("intra-campaign")).collect(Collectors.toList())
						.stream()
						.sorted(Comparator.comparingInt(CampaignDashboardElement::getOrder))
						.collect(Collectors.toList()));
			
			campaignDashboardGridComponent_2.setSavedItems(
					newFieldValue.getCampaignDashboardElements().stream().filter(e -> e.getPhase().equals("post-campaign")).collect(Collectors.toList()));
					/*
						.stream()
						.sorted(Comparator.comparingInt(CampaignDashboardElement::getOrder))
						.collect(Collectors.toList()));*/
		}
	}

	@Override
	public void discard() throws SourceException {
		super.discard();
		campaignFormsGridComponent.discardGrid();
		campaignFormsGridComponent_1.discardGrid();
		campaignFormsGridComponent_2.discardGrid();
		
		campaignDashboardGridComponent.discardGrid();
		campaignDashboardGridComponent_1.discardGrid();
		campaignDashboardGridComponent_2.discardGrid();
	}

	@Override
	protected String createHtmlLayout() {
		return HTML_LAYOUT;
	}
	
	public String DateGetYear(Date dates) {
	   
		 SimpleDateFormat getYearFormat = new SimpleDateFormat("yyyy");
	        String currentYear = getYearFormat.format(dates);
	        return currentYear;
	}
	
	
}
