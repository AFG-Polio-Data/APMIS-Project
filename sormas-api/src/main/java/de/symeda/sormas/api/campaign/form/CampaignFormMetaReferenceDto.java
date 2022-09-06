/*
 * ******************************************************************************
 * * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * * Copyright © 2016-2020 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 * *
 * * This program is free software: you can redistribute it and/or modify
 * * it under the terms of the GNU General Public License as published by
 * * the Free Software Foundation, either version 3 of the License, or
 * * (at your option) any later version.
 * *
 * * This program is distributed in the hope that it will be useful,
 * * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * * GNU General Public License for more details.
 * *
 * * You should have received a copy of the GNU General Public License
 * * along with this program. If not, see <https://www.gnu.org/licenses/>.
 * ******************************************************************************
 */

package de.symeda.sormas.api.campaign.form;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import de.symeda.sormas.api.ReferenceDto;
import de.symeda.sormas.api.user.FormAccess;

public class CampaignFormMetaReferenceDto extends ReferenceDto {

	@Enumerated(EnumType.STRING)
	private FormAccess formCategory;

	public CampaignFormMetaReferenceDto() {
		// System.out.println("00000000000000000000000000000000");
	}

	public CampaignFormMetaReferenceDto(String uuid) {
		// System.out.println("11111111111111");
		setUuid(uuid);
	}

	public CampaignFormMetaReferenceDto(String uuid, String caption) {
		// System.out.println("333333333333333333333333333333333");
		setUuid(uuid);
		setCaption(caption);
	}

	public CampaignFormMetaReferenceDto(String uuid, String caption, String type) {
		// System.out.println("44444444444444444444444444444444444");
		setUuid(uuid);
		setCaption(caption);
		setFormType(type);
	}
	
	public CampaignFormMetaReferenceDto(String uuid, String caption, String type, FormAccess access) {
		// System.out.println("555555555555555555555555555555555555555");
		setUuid(uuid);
		setCaption(caption);
		setFormType(type);
		this.setFormCategory(access);
	}

	public FormAccess getFormCategory() {
		return formCategory;
	}

	public void setFormCategory(FormAccess formCategory) {
		this.formCategory = formCategory;
	}
	
	

}
