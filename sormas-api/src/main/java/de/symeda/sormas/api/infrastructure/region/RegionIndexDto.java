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
package de.symeda.sormas.api.infrastructure.region;

import de.symeda.sormas.api.EntityDto;
import de.symeda.sormas.api.infrastructure.area.AreaReferenceDto;
import de.symeda.sormas.api.infrastructure.country.CountryReferenceDto;
import de.symeda.sormas.api.utils.DataHelper;

public class RegionIndexDto extends EntityDto {

	private static final long serialVersionUID = -199144233786408125L;

	public static final String I18N_PREFIX = "Region";
	public static final String NAME = "name";
	public static final String FA_AF = "fa_af";
	public static final String PS_AF = "ps_af";
	public static final String EPID_CODE = "epidCode";
	public static final String POPULATION = "population";
	public static final String GROWTH_RATE = "growthRate";
	public static final String EXTERNAL_ID = "externalId";
	public static final String REGION_EXTERNAL_ID = "areaexternalId";
	public static final String AREA = "area";
	public static final String COUNTRY = "country"; //AREA

	private String name;
	private String fa_af;
	private String ps_af;
	private String epidCode;
	private Integer population;
	private Float growthRate;
	private Long externalId;
	private Long areaexternalId;
	private AreaReferenceDto area;
	private CountryReferenceDto country;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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
	
	public String getEpidCode() {
		return epidCode;
	}

	public void setEpidCode(String epidCode) {
		this.epidCode = epidCode;
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

	public AreaReferenceDto getArea() {
		return area;
	}

	public void setArea(AreaReferenceDto area) {
		this.area = area;
	}
	
	public Long getAreaexternalId() {
		return areaexternalId;
	}
	
	public void setAreaexternalId(Long areaexternalId) {
		this.areaexternalId = areaexternalId;
	}

	public CountryReferenceDto getCountry() {
		return country;
	}

	public void setCountry(CountryReferenceDto country) {
		this.country = country;
	}

	public RegionReferenceDto toReference() {
		return new RegionReferenceDto(getUuid(), name, externalId);
	}

	public static RegionIndexDto build() {

		RegionIndexDto dto = new RegionIndexDto();
		dto.setUuid(DataHelper.createUuid());
		return dto;
	}
}
