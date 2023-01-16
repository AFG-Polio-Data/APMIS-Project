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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.renderers.DateRenderer;
import com.vaadin.ui.renderers.NumberRenderer;
import com.vaadin.ui.themes.ValoTheme;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.campaign.CampaignIndexDto;
import de.symeda.sormas.api.campaign.data.CampaignFormDataCriteria;
import de.symeda.sormas.api.campaign.data.CampaignFormDataEntry;
import de.symeda.sormas.api.campaign.data.CampaignFormDataIndexDto;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.api.utils.SortProperty;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.campaign.components.importancefilterswitcher.CriteriaPhase;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.FilteredGrid;
import de.symeda.sormas.ui.utils.ShowDetailsListener;

public class CampaignDataGrid extends FilteredGrid<CampaignFormDataIndexDto, CampaignFormDataCriteria> {

	private static final long serialVersionUID = 8045806100043073638L;

	NumberRenderer numberRenderer = new NumberRenderer();

	public CampaignDataGrid(CampaignFormDataCriteria criteria) {

		super(CampaignFormDataIndexDto.class);
		setSizeFull();

		setDataProvider();
		setCriteria(criteria);

		addDefaultColumns();
	}

	// Apply filter
	protected void addDefaultColumns() {
		// addEditColumn(e -> {
		// ControllerProvider.getCampaignController().navigateToFormDataView(e.getUuid());
		// });
		if (permitted(FeatureType.CAMPAIGNS, UserRight.CAMPAIGN_FORM_DATA_EDIT)) {
			addItemClickListener(new ShowDetailsListener<>(CampaignFormDataIndexDto.CAMPAIGN,
					e -> ControllerProvider.getCampaignController().navigateToFormDataView(e.getUuid())));
			addItemClickListener(new ShowDetailsListener<>(CampaignFormDataIndexDto.FORM,
					e -> ControllerProvider.getCampaignController().navigateToFormDataView(e.getUuid())));
			addItemClickListener(new ShowDetailsListener<>(CampaignFormDataIndexDto.AREA,
					e -> ControllerProvider.getCampaignController().navigateToFormDataView(e.getUuid())));
			addItemClickListener(new ShowDetailsListener<>(CampaignFormDataIndexDto.REGION,
					e -> ControllerProvider.getCampaignController().navigateToFormDataView(e.getUuid())));
			addItemClickListener(new ShowDetailsListener<>(CampaignFormDataIndexDto.DISTRICT,
					e -> ControllerProvider.getCampaignController().navigateToFormDataView(e.getUuid())));
			addItemClickListener(new ShowDetailsListener<>(CampaignFormDataIndexDto.COMMUNITY,
					e -> ControllerProvider.getCampaignController().navigateToFormDataView(e.getUuid())));
			addItemClickListener(new ShowDetailsListener<>(CampaignFormDataIndexDto.FORM_DATE,
					e -> ControllerProvider.getCampaignController().navigateToFormDataView(e.getUuid())));
			addItemClickListener(new ShowDetailsListener<>(CampaignFormDataIndexDto.FORM_TYPE,
					e -> ControllerProvider.getCampaignController().navigateToFormDataView(e.getUuid())));
		}
		setColumns(
				// EDIT_BTN_ID,
				CampaignFormDataIndexDto.CAMPAIGN, CampaignFormDataIndexDto.FORM, CampaignFormDataIndexDto.AREA,
				CampaignFormDataIndexDto.RCODE, CampaignFormDataIndexDto.REGION, CampaignFormDataIndexDto.PCODE,
				CampaignFormDataIndexDto.DISTRICT, CampaignFormDataIndexDto.DCODE, CampaignFormDataIndexDto.COMMUNITY,
				CampaignFormDataIndexDto.COMMUNITYNUMBER, CampaignFormDataIndexDto.CCODE,
				CampaignFormDataIndexDto.FORM_DATE, CampaignFormDataIndexDto.FORM_TYPE);
		// getColumn(EDIT_BTN_ID).setWidth(40).setStyleGenerator(item ->
		// CssStyles.GRID_CELL_LINK);

		((Column<CampaignFormDataIndexDto, Date>) getColumn(CampaignFormDataIndexDto.FORM_DATE))
				.setRenderer(new DateRenderer(DateHelper.getLocalDateFormat(I18nProperties.getUserLanguage())));

		for (Column<?, ?> column : getColumns()) {
			column.setCaption(I18nProperties.getPrefixCaption(CampaignFormDataIndexDto.I18N_PREFIX, column.getId(),
					column.getCaption()));// .setSortable(true);
			column.setDescriptionGenerator(CampaignFormDataIndexDto -> column.getCaption()); // set the description of
																								// default columns
																								// #94-iyanuu
			column.setSortable(true);

		}
	}

	public void reload() {
		getDataProvider().refreshAll();
	}

	public void setDataProvider() {
		DataProvider<CampaignFormDataIndexDto, CampaignFormDataCriteria> dataProvider = DataProvider
				.fromFilteringCallbacks(
						query -> FacadeProvider.getCampaignFormDataFacade()
								.getIndexList(query.getFilter().orElse(null), query.getOffset(), query.getLimit(),
										query.getSortOrders().stream()
												// .map(sortOrder -> new SortProperty(sortOrder.getSorted()))
												.map(sortOrder -> new SortProperty(sortOrder.getSorted(),
														sortOrder.getDirection() == SortDirection.ASCENDING))
												.collect(Collectors.toList()))
								.stream(),
						query -> (int) FacadeProvider.getCampaignFormDataFacade()
								.count(query.getFilter().orElse(null)));
		setDataProvider(dataProvider);
		setSelectionMode(SelectionMode.NONE);
	}

	public void addCustomColumn(String property, String caption) {
		if (!property.toString().contains("readonly")) {

			Column<CampaignFormDataIndexDto, Object> newColumn = addColumn(
					e -> e.getFormValues().stream().filter(v -> v.getId().equals(property)).findFirst().orElse(null));
			newColumn.setSortable(false);
			newColumn.setCaption(caption);
			newColumn.setId(property);
			newColumn.setWidth(240.0);
			newColumn.setDescriptionGenerator(CampaignFormDataIndexDto -> newColumn.getCaption());// set the
																									// description
																									// of default
																									// columns
																									// #94-iyanuu

		}

	}
}
