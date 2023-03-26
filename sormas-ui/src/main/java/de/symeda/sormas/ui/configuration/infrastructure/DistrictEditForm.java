/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/
package de.symeda.sormas.ui.configuration.infrastructure;

import static de.symeda.sormas.ui.utils.LayoutUtil.fluidRowLocs;

import com.vaadin.v7.data.validator.RegexpValidator;
import com.vaadin.v7.ui.ComboBox;
import com.vaadin.v7.ui.TextField;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.InfrastructureDataReferenceDto;
import de.symeda.sormas.api.campaign.CampaignDto;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.infrastructure.community.CommunityDto;
import de.symeda.sormas.api.infrastructure.district.DistrictDto;
import de.symeda.sormas.api.infrastructure.region.RegionDto;
import de.symeda.sormas.ui.utils.AbstractEditForm;

public class DistrictEditForm extends AbstractEditForm<DistrictDto> {

	private static final long serialVersionUID = 7573666294384000190L;

	private static final String HTML_LAYOUT =
		fluidRowLocs(DistrictDto.NAME, DistrictDto.EXTERNAL_ID_DUMMY)
		+ fluidRowLocs(DistrictDto.REGION, DistrictDto.RISK); // ,DistrictDto.GROWTH_RATE,+ fluidRowLocs(RegionDto.EXTERNAL_ID);

	private final boolean create;

	public DistrictEditForm(boolean create) {

		super(DistrictDto.class, DistrictDto.I18N_PREFIX, false);
		this.create = create;

		setWidth(540, Unit.PIXELS);

		if (create) {
			hideValidationUntilNextCommit();
		}
		addFields();
	}

	@Override
	protected void addFields() {

		addField(DistrictDto.NAME, TextField.class);
		
		TextField tfe = addField(DistrictDto.EXTERNAL_ID_DUMMY, TextField.class);
		tfe.addValidator( new RegexpValidator("^[0-9]\\d*$", "Not a valid input"));
		tfe.setCaption("DCode");
		//addField(DistrictDto.EPID_CODE, TextField.class);
		ComboBox region = addInfrastructureField(DistrictDto.REGION);
		
		ComboBox riskField = addRiskCombo(DistrictDto.RISK);
		
		//ComboBox riskField = addField(DistrictDto.RISK, ComboBox.class);
		riskField.addItem("Low Risk (LR)");
		riskField.addItem("Middle Risk (MR)");
		riskField.addItem("High Risk (HR)");
		riskField.setNullSelectionAllowed(true);
		
//		TextField growthRate = addField(DistrictDto.GROWTH_RATE, TextField.class); NID
//		growthRate.setConverter(new StringToFloatConverter());
//		growthRate.setConversionError(I18nProperties.getValidationError(Validations.onlyDecimalNumbersAllowed, growthRate.getCaption()));

		setRequired(true, DistrictDto.NAME, DistrictDto.EXTERNAL_ID_DUMMY, DistrictDto.REGION);

		region.addItems(FacadeProvider.getRegionFacade().getAllActiveAsReference());

		// TODO: Workaround until cases and other data is properly transfered when infrastructure data changes
		if (!create) {
			region.setEnabled(false);
		}
	}
	
	protected ComboBox addRiskCombo(String fieldId) {
		//System.out.println("=========================== "+fieldId);
		ComboBox field = addField(fieldId, ComboBox.class);
		// Make sure that the ComboBox still contains a pre-selected inactive infrastructure entity
		field.addValueChangeListener(e -> {
			String value = e.getProperty().getValue().toString();
			if (value != null && !field.containsId(value)) {
				field.addItem(value);
			}
		});
		return field;
	}

	@Override
	protected String createHtmlLayout() {
		return HTML_LAYOUT;
	}
}
