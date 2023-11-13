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
public abstract class ReferenceDto implements Serializable, HasUuid, Comparable<ReferenceDto> {

	public static final String CAPTION = "caption";

	@Required
	@Pattern(regexp = UUID_REGEX, message = Validations.uuidPatternNotMatching)
	private String uuid;
	private String caption;
	private String formType;
	private String dateExpired;
	private String ps_af;
	private String fa_af;
	
//	private Long externalId;

	public ReferenceDto() {

	}

	public ReferenceDto(String uuid) {
		this.uuid = uuid;
	}

	public ReferenceDto(String uuid, String caption) {
		this.uuid = uuid;
		this.caption = caption;
	}
	
	public ReferenceDto(String uuid, String caption, String type) {
		this.uuid = uuid;
		this.caption = caption;
		this.formType = type; 
	}
	public ReferenceDto(String uuid, String caption, String type, String dateExpired) {
		this.uuid = uuid;
		this.caption = caption;
		this.formType = type; 
		this.dateExpired = dateExpired;
		
	}
	

	
	public ReferenceDto(@Pattern(regexp = "^[0-9a-zA-Z-]*$", message = "uuidPatternNotMatching") String uuid,
			String caption, String formType, String dateExpired, String ps_af, String fa_af) {
		super();
		this.uuid = uuid;
		this.caption = caption;
		this.formType = formType;
		this.dateExpired = dateExpired;
		this.ps_af = ps_af;
		this.fa_af = fa_af;
	}

	/*	public ReferenceDto(String uuid, String caption, Long externalId) {
		this.uuid = uuid;
		this.caption = caption;
		this.externalId = externalId;
	}
	
	*/
	

	public String getDateExpired() {
		return dateExpired;
	}

	public void setDateExpired(String dateExpired) {
		this.dateExpired = dateExpired;
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
		return formType;
	}

//	public void setFormtype(String formType) {
//		this.formType = formType;
//	}

	/*public Long getExternalID() {
		return externalId;
	}

	public void setExternalID(Long externalId) {
		this.externalId = externalId;
	}
*/
	

	public void setFormType(String formType) {
		this.formType = formType;
	}

	public String getPs_af() {
		return ps_af;
	}

	public void setPs_af(String ps_af) {
		this.ps_af = ps_af;
	}

	public String getFa_af() {
		return fa_af;
	}

	public void setFa_af(String fa_af) {
		this.fa_af = fa_af;
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
