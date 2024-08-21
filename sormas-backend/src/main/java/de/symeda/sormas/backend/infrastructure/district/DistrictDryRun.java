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
package de.symeda.sormas.backend.infrastructure.district;

import static de.symeda.sormas.api.utils.FieldConstraints.CHARACTER_LIMIT_DEFAULT;

import java.util.List;
import java.util.ListIterator;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import de.symeda.sormas.backend.common.InfrastructureAdo;
import de.symeda.sormas.backend.infrastructure.community.Community;
import de.symeda.sormas.backend.infrastructure.region.Region;

@Entity
public class DistrictDryRun extends InfrastructureAdo {

	private static final long serialVersionUID = -6057113756091470463L;

	public static final String TABLE_NAME = "districtdryrun";

	public static final String NAME = "name";
	public static final String FA_AF = "fa_af";
	public static final String PS_AF = "ps_af";
	public static final String REGION = "region";
	public static final String EPID_CODE = "epidCode";
	public static final String RISK = "risk";
	public static final String COMMUNITIES = "communities";
	public static final String GROWTH_RATE = "growthRate";
	public static final String EXTERNAL_ID = "externalId";

	private String name;
	private String fa_af;
	private String ps_af;
	private Region region;
	private String epidCode;
	private String risk;
	private List<Community> communities;
	private Float growthRate;
	private Long externalId;

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

	@ManyToOne(cascade = CascadeType.REFRESH, optional = false)
	@JoinColumn(nullable = false)
	public Region getRegion() {
		return region;
	}

	public void setRegion(Region region) {
		this.region = region;
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

	@OneToMany(mappedBy = Community.DISTRICT, cascade = {}, fetch = FetchType.LAZY)
	@OrderBy(DistrictDryRun.NAME)
	public List<Community> getCommunities() {
		return communities;
	}

	public void setCommunities(List<Community> communities) {
		this.communities = communities;
	}

	public Float getGrowthRate() {
		return growthRate;
	}

	public void setGrowthRate(Float growthRate) {
		this.growthRate = growthRate;
	}

	// @Column(length = CHARACTER_LIMIT_DEFAULT)
	public Long getExternalId() {
		return externalId;
	}

	public void setExternalId(Long externalId) {
		this.externalId = externalId;
	}

	@Override
	public String toString() {
		return getName();
	}
}
