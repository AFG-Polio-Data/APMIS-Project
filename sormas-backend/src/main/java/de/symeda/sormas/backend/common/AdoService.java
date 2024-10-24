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
package de.symeda.sormas.backend.common;

import java.util.List;

import javax.persistence.EntityExistsException;
import javax.validation.constraints.NotNull;

import de.symeda.sormas.backend.campaign.Campaign;

public interface AdoService<ADO extends AbstractDomainObject> {

	List<ADO> getAll();
	
	List<ADO> getByRound(String round);
	
	List<ADO> getByRoundAndCampaign(String round, String uuid);
	
	List<Campaign> getAllCampaignByStartDate();

	ADO getById(long id);

	ADO getByUuid(String uuid);

	Boolean exists(@NotNull String uuid);

	/**
	 * <b>DELETES</b> an entity from the database!
	 * 
	 * @param deleteme
	 */
	void delete(ADO deleteme);

	/**
	 * Speichert ein neues Objekt in der Datenbank.
	 * Es darf vorher keine id haben und ist danch attacht.
	 */
	void persist(ADO persistme);

	/**
	 * Zum Speichern, wenn das ado neu oder direkt aus der Datenbank ist.<br/>
	 * Ruft persist() für neue Entities auf,
	 * bei attachten wird nichts gemacht.
	 * <br/>
	 * Das ado ist nach dem Aufruf attacht.
	 * 
	 * @param ado
	 * @throws EntityExistsException
	 *             wenn das ado detacht ist
	 */
	void ensurePersisted(ADO ado) throws EntityExistsException;

	/**
	 * JPA-Session flushen
	 */
	void doFlush();

//	List<ADO> getByRoundAndExpiryDate(String round);

	

	
}
