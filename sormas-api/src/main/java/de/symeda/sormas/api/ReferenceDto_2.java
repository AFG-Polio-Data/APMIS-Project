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
package de.symeda.sormas.api;

import java.io.Serializable;

import javax.validation.constraints.Pattern;

import org.apache.commons.lang3.ObjectUtils;

import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.utils.Required;

@SuppressWarnings("serial")
public abstract class ReferenceDto_2 implements Serializable, HasUuid, Comparable<ReferenceDto> {

	public static final String CAPTION = "caption";

	@Required
	@Pattern(regexp = UUID_REGEX, message = Validations.uuidPatternNotMatching)
	private String uuid;
	private String caption;
	private String formtype;
	private Long externalId;

	public ReferenceDto_2() {

	}

	public ReferenceDto_2(String uuid) {
		this.uuid = uuid;
	}

	public ReferenceDto_2(String uuid, String caption) {
		this.uuid = uuid;
		this.caption = caption;
	}
	
	public ReferenceDto_2(String uuid, String caption, Long externalId) {
		//System.out.println(caption+" ddddddddd555555555555555555    externalId     dddddddddddddddddddd "+externalId);
		this.uuid = uuid;
		this.caption = caption;
		this.externalId = externalId;
	}
	
	public ReferenceDto_2(String uuid, String caption, String type) {
		
	//	System.out.println(caption+" ddddddddd5555555555555555555555555555555555555555555dddddddddddddddddddddddddddd "+type);
		this.uuid = uuid;
		this.caption = caption;
		this.formtype = type;
	}
	
	

	@Override
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}
	
	

	public String getFormType() {
		return formtype;
	}

	public void setFormType(String formtype) {
		this.formtype = formtype;
	}

	@Override
	public String toString() {
		return getCaption();
	}

	@Override
	public boolean equals(Object o) {

		if (this == o) {
			return true;
		}
		if (o == null) {
			return false;
		}

		if (getUuid() != null && o instanceof HasUuid && ((HasUuid) o).getUuid() != null) {
			// this works, because we are using UUIDs
			HasUuid ado = (HasUuid) o;
			return getUuid().equals(ado.getUuid());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {

		if (getUuid() != null) {
			return getUuid().hashCode();
		}
		return 0;
	}

	@Override
	public int compareTo(ReferenceDto o) {
		return ObjectUtils.compare(getCaption(), o.getCaption());
	}
}
