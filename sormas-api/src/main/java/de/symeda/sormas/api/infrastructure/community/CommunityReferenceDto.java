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
package de.symeda.sormas.api.infrastructure.community;

import java.util.Comparator;

import de.symeda.sormas.api.InfrastructureDataReferenceDto;
import de.symeda.sormas.api.statistics.StatisticsGroupingKey;

public class CommunityReferenceDto extends InfrastructureDataReferenceDto implements StatisticsGroupingKey {

	private static final long serialVersionUID = -8833267932522978860L;
	private Integer numberx;

	public CommunityReferenceDto() {
		super();
	}

	public CommunityReferenceDto(String uuid) {
		super(uuid);
	}

	public CommunityReferenceDto(String uuid, String caption, Long externalId) {
		super(uuid, caption, externalId);
	}

	public CommunityReferenceDto(String uuid, String caption, Long externalId, Integer clusterNumber) {
		super(uuid, caption, externalId);
		this.numberx = clusterNumber;
	}

	public Integer getNumber() {
		return numberx;
	}

	@Override
	public int keyCompareTo(StatisticsGroupingKey o) {
		if (o == null) {
			throw new NullPointerException("Can't compare to null.");
		}

		if (this.equals(o)) {
			return 0;
		}
		int captionComparison = this.getCaption().compareTo(((CommunityReferenceDto) o).getCaption());
		if (captionComparison != 0) {
			return captionComparison;
		} else {
			return this.getUuid().compareTo(((CommunityReferenceDto) o).getUuid());
		}
	}

	// Comparator for sorting the list by clusternumber /UtilDateFormatter
	public static Comparator<CommunityReferenceDto> clusternumber = new Comparator<CommunityReferenceDto>() {

		public int compare(CommunityReferenceDto comRef1, CommunityReferenceDto comRef2) {

			int clusterno1 = comRef1.getNumber();
			int clusterno2 = comRef2.getNumber();

			// For ascending order
			return clusterno1 - clusterno2;

			// For descending order
			// clusterno2-clusterno1;
		}
	}; 
}
