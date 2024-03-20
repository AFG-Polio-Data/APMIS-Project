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
package de.symeda.sormas.api.infrastructure.district;

import java.util.Date;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import de.symeda.sormas.api.EntityDto;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.FieldConstraints;

public class DistrictDto extends EntityDto {

	private static final long serialVersionUID = 8990957700033431836L;

	public static final String I18N_PREFIX = "District";

	public static final String NAME = "name";
	public static final String FA_AF = "fa_af";
	public static final String PS_AF = "ps_af";
	public static final String EPID_CODE = "epidCode";
	public static final String RISK = "risk";
	public static final String GROWTH_RATE = "growthRate";
	public static final String REGION = "region";
	public static final String EXTERNAL_ID = "externalId";
	public static final String EXTERNAL_ID_DUMMY = "externalIddummy";
//	public static final String DISTRICT_MODALITY = "districtModality";


	@Size(max = FieldConstraints.CHARACTER_LIMIT_SMALL, message = Validations.textTooLong)
	private String name;
	@Size(max = FieldConstraints.CHARACTER_LIMIT_SMALL, message = Validations.textTooLong)
	private String fa_af;
	@Size(max = FieldConstraints.CHARACTER_LIMIT_SMALL, message = Validations.textTooLong)
	private String ps_af;
	@Size(max = FieldConstraints.CHARACTER_LIMIT_SMALL, message = Validations.textTooLong)
	private String epidCode;
	private String risk;
	private Float growthRate;
	private RegionReferenceDto region;
	private boolean archived;
	
	private Long populationData;
	private Long campaignPopulationData;
	private String selectedPopulationData;
	private Long regionId;
	private String regionUuid_;
	private String uuid_;
	private String mapHasc;
	private String districtModality;
	private String districtStatus;
	private String ageGroup;



	//@Size(max = FieldConstraints.CHARACTER_LIMIT_DEFAULT, message = Validations.textTooLong)
	
	//@Min(2)
	private Long externalId;
	private String externalIddummy;

	public DistrictDto(
		Date creationDate,
		Date changeDate,
		String uuid,
		boolean archived,
		String name,
		String epidCode,
		String risk,
		Float growthRate,
		String regionUuid,
		String regionName,
		Long regionExternalId,
		Long externalId) {

		super(creationDate, changeDate, uuid);
		this.archived = archived;
		this.name = name;
		this.epidCode = epidCode;
		this.risk = risk;
		this.growthRate = growthRate;
		this.region = new RegionReferenceDto(regionUuid, regionName, regionExternalId);
		this.externalId = externalId;
	}
	
	public DistrictDto(String name, Long populationData, Long regionId, String regionUuid_, String uuid_, String selectedPopulationData) {
		this.name = name;
		this.populationData = populationData;
		this.regionId = regionId;
		this.regionUuid_ = regionUuid_;
		this.uuid_ = uuid_;
		this.selectedPopulationData = selectedPopulationData;
	};
	
//	public DistrictDto(String name, Long populationData, Long regionId, String regionUuid_, String uuid_, String selectedPopulationData, String districtModality) {
//		this.name = name;
//		this.populationData = populationData;
//		this.regionId = regionId;
//		this.regionUuid_ = regionUuid_;
//		this.uuid_ = uuid_;
//		this.selectedPopulationData = selectedPopulationData;
//		this.districtModality = districtModality;
//	};
	
//	public DistrictDto(String name, Long populationData, Long regionId, String regionUuid_, String uuid_, String selectedPopulationData, String districtModality, String districtStatus) {
//		this.name = name;
//		this.populationData = populationData;
//		this.regionId = regionId;
//		this.regionUuid_ = regionUuid_;
//		this.uuid_ = uuid_;
//		this.selectedPopulationData = selectedPopulationData;
//		this.districtModality = districtModality;
//		this.districtStatus = districtStatus;
//
//	};
	
	public DistrictDto(String name, Long populationData, Long regionId, String regionUuid_, String uuid_, String selectedPopulationData, String districtModality, String districtStatus, String ageGroup) {
		this.name = name;
		this.populationData = populationData;
		this.regionId = regionId;
		this.regionUuid_ = regionUuid_;
		this.uuid_ = uuid_;
		this.selectedPopulationData = selectedPopulationData;
		this.districtModality = districtModality;
		this.districtStatus = districtStatus;
		this.ageGroup = ageGroup;
	};
	
	

	public DistrictDto(@Size(max = 255, message = "textTooLong") String name, String mapHasc) {
		super();
		this.name = name;
		this.mapHasc = mapHasc;
	}

	public DistrictDto() {
		super();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEpidCode() {
		return epidCode;
	}
	
	public String getExternalIddummy() {
		
		if(externalId != null) {
			externalIddummy = externalId+"";
		}
		
		return externalIddummy;
	}

	public void setExternalIddummy(String externalIddummy) {
		
		if(externalIddummy != null) {
			this.externalId = Long.parseLong(externalIddummy);
		}
				
	}

	public void setEpidCode(String epidCode) {
		this.epidCode = epidCode;
	}

	public String getRisk() {
		return risk;
	}

	public void setRisk(String risk) {
		this.risk = risk;
	}

	public RegionReferenceDto getRegion() {
		return region;
	}

	public void setRegion(RegionReferenceDto region) {
		this.region = region;
	}

	@Override
	public String toString() {
		return getName();
	}

	public Float getGrowthRate() {
		return growthRate;
	}

	public void setGrowthRate(Float growthRate) {
		this.growthRate = growthRate;
	}

	public boolean isArchived() {
		return archived;
	}

	public void setArchived(boolean archived) {
		this.archived = archived;
	}

	public Long getExternalId() {
		return externalId;
	}

	public void setExternalId(Long externalId) {
		this.externalId = externalId;
	}

	public DistrictReferenceDto toReference() {
	//	return new DistrictReferenceDto(getUuid());
		return new DistrictReferenceDto(getUuid(), name, externalId);
	}

	public static DistrictDto build() {
		DistrictDto dto = new DistrictDto();
		dto.setUuid(DataHelper.createUuid());
		return dto;
	}

	public Long getPopulationData() {
		return populationData;
	}

	public void setPopulationData(Long populationData) {
		this.populationData = populationData;
	}

	public Long getRegionId() {
		return regionId;
	}

	public void setRegionId(Long regionId) {
		this.regionId = regionId;
	}

	public String getRegionUuid_() {
		return regionUuid_;
	}

	public void setRegionUuid_(String regionUuid_) {
		this.regionUuid_ = regionUuid_;
	}

	public String getUuid_() {
		return uuid_;
	}

	public void setUuid_(String uuid_) {
		this.uuid_ = uuid_;
	}

	public Long getCampaignPopulationData() {
		return campaignPopulationData;
	}

	public void setCampaignPopulationData(Long campaignPopulationData) {
		this.campaignPopulationData = campaignPopulationData;
	}

	public String getSelectedPopulationData() {
		return selectedPopulationData;
	}

	public void setSelectedPopulationData(String selectedPopulationData) {
		this.selectedPopulationData = selectedPopulationData;
	}

	public String getMapHasc() {
		return mapHasc;
	}

	public void setMapHasc(String mapHasc) {
		this.mapHasc = mapHasc;
	}

	public String getFa_af() {
		return fa_af;
	}

	public void setFa_af(String fa_af) {
		this.fa_af = fa_af;
	}

	public String getPs_af() {
		return ps_af;
	}

	public void setPs_af(String ps_af) {
		this.ps_af = ps_af;
	}

	public String getDistrictModality() {
		return districtModality;
	}

	public void setDistrictModality(String districtModality) {
		this.districtModality = districtModality;
	}

	public String getDistrictStatus() {
		return districtStatus;
	}

	public void setDistrictStatus(String districtStatus) {
		this.districtStatus = districtStatus;
	}

	public String getAgeGroup() {
		return ageGroup;
	}

	public void setAgeGroup(String ageGroup) {
		this.ageGroup = ageGroup;
	}
	
	
	
}
