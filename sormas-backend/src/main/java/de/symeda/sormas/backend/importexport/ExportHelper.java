/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2021 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.symeda.sormas.backend.importexport;

import java.util.Arrays;
import java.util.Collections;

import de.symeda.sormas.api.importexport.ExportConfigurationDto;

public final class ExportHelper {

	private ExportHelper() {

	}

//	public static String[] getVaccinationExportProperties() {
//		return new String[] ;
//	}

	public static boolean shouldExportFields(ExportConfigurationDto exportConfiguration, String... fields) {
		return exportConfiguration == null || !Collections.disjoint(exportConfiguration.getProperties(), Arrays.asList(fields));
	}

}
