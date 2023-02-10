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

import de.symeda.sormas.api.EntityDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.utils.DataHelper;

public class DistrictIndexDto extends EntityDto {

	private static final long serialVersionUID = -1445387465599056704L;
	public static final int CASE_INCIDENCE_DIVISOR = 100000;

	public static final String I18N_PREFIX = "District";

	public static final String NAME = "name";
	public static final String EPID_CODE = "epidCode";
	public static final String RISK = "risk";
	public static final String POPULATION = "population";
	public static final String GROWTH_RATE = "growthRate";
	public static final String REGION = "region";
	public static final String REGION_EXTERNALID = "regionexternalId";
	public static final String EXTERNAL_ID = "externalId";
	public static final String AREA_EXTERNAL_ID = "areaexternalId";
	public static final String AREA_NAME = "areaname";

	private String name;
	private String epidCode;
	private String risk;
	private Integer population;
	private Float growthRate;
	private RegionReferenceDto region;
	private Long externalId;
	private Long regionexternalId;
	private Long areaexternalId;
	private String areaname;

	public DistrictIndexDto() {
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

	public Integer getPopulation() {
		return population;
	}

	public void setPopulation(Integer population) {
		this.population = population;
	}

	public Float getGrowthRate() {
		return growthRate;
	}

	public void setGrowthRate(Float growthRate) {
		this.growthRate = growthRate;
	}

	public Long getExternalId() {
		return externalId;
	}
	
	public void setExternalId(Long externalId) {
		this.externalId = externalId;
	}

	
	
	public Long getRegionexternalId() {
		return regionexternalId;
	}

	public void setRegionexternalId(Long regionexternalId) {
		this.regionexternalId = regionexternalId;
	}

	public Long getAreaexternalId() {
		return areaexternalId;
	}

	public void setAreaexternalId(Long areaexternalId) {
		this.areaexternalId = areaexternalId;
	}

	public String getAreaname() {
		return areaname;
	}

	public void setAreaname(String areaname) {
		this.areaname = areaname;
	}

	public DistrictReferenceDto toReference() {
		return new DistrictReferenceDto(getUuid(), name, externalId);
	}

	public static DistrictIndexDto build() {
		DistrictIndexDto dto = new DistrictIndexDto();
		dto.setUuid(DataHelper.createUuid());
		return dto;
	}
}
