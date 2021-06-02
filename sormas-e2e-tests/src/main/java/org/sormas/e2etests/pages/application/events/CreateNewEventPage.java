/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2021 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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
 */

package org.sormas.e2etests.pages.application.events;

import org.openqa.selenium.By;

public class CreateNewEventPage {
  public static final By TITLE_INPUT = By.cssSelector(".popupContent #eventTitle");
  public static final By SAVE_BUTTON = By.id("commit");
  public static final By DISCARD_BUTTON = By.id("discard");
  public static final By EVENT_STATUS_OPTIONS =
      By.cssSelector(".popupContent #eventStatus .v-select-option label");
  public static final By RISK_LEVEL_COMBOBOX = By.cssSelector(".popupContent #riskLevel div");
  public static final By EVENT_MANAGEMENT_STATUS_OPTIONS =
      By.cssSelector(".popupContent #eventManagementStatus .v-select-option label");
  public static final By  START_DATA_INPUT = By.cssSelector(".popupContent #startDate input");
  public static final By SIGNAL_EVOLUTION_DATE_INPUT =
      By.cssSelector(".popupContent #evolutionDate input");
  public static final By EVENT_INVESTIGATION_STATUS_OPTIONS =
      By.cssSelector(".popupContent #eventInvestigationStatus label");
  public static final By DISEASE_INPUT = By.cssSelector(".popupContent #disease div");
  public static final By EXTERNAL_ID_INPUT = By.cssSelector(".popupContent #externalId");
  public static final By INTERNAL_ID_INPUT = By.cssSelector(".popupContent #internalId");
  public static final By EXTERNAL_TOKEN_INPUT = By.cssSelector(".popupContent #externalToken");
  public static final By DESCRIPTION_INPUT = By.cssSelector(".popupContent #eventDesc");
  public static final By SOURCE_TYPE_COMBOBOX = By.cssSelector(".popupContent #srcType div");
  public static final By TYPE_OF_PLACE_COMBOBOX = By.cssSelector(".popupContent #typeOfPlace div");
  public static final By COUNTRY_COMBOBOX = By.cssSelector(".popupContent #country div");
  public static final By REGION_COMBOBOX = By.cssSelector(".popupContent #region div");
  public static final By DISTRICT_COMBOBOX = By.cssSelector(".popupContent #district div");
  public static final By COMMUNITY_COMBOBOX = By.cssSelector(".popupContent #community div");
  public static final By STREET_INPUT = By.cssSelector(".popupContent #street");
  public static final By HOUSE_NUMBER_INPUT = By.cssSelector(".popupContent #houseNumber");
  public static final By ADDITIONAL_INFORMATION_INPUT =
      By.cssSelector(".popupContent #additionalInformation");
  public static final By POSTAL_CODE_INPUT = By.cssSelector(".popupContent #postalCode");
  public static final By AREA_TYPE_COMBOBOX = By.cssSelector(".popupContent #areaType div");
  public static final By COMMUNITY_CONTACT_PERSON_COMBOBOX =
      By.cssSelector(".popupContent #details");
  public static final By CITY_INPUT = By.cssSelector(".popupContent #city");
  public static final By GPS_LATITUDE_INPUT = By.cssSelector(".popupContent #latitude");
  public static final By GPS_LONGITUDE_INPUT = By.cssSelector(".popupContent #longitude");
  public static final By PRIMARY_MODE_OF_TRANSMISSION =
      By.cssSelector(".popupContent #diseaseTransmissionMode");
  public static final By NOSOCOMIAL = By.cssSelector(".popupContent #nosocomial");
  public static final By EVENT_INVESTIGATION_START_DATE =
      By.cssSelector(".popupContent #eventInvestigationStartDate");
  public static final By EVENT_INVESTIGATION_END_DATE =
      By.cssSelector(".popupContent #eventInvestigationEndDate");
  public static final By SOURCE_MEDIA_WEBSITE = By.cssSelector(".popupContent #srcMediaWebsite");
  public static final By SOURCE_MEDIA_NAME = By.cssSelector(".popupContent #srcMediaName");
  public static final By SOURCE_MEDIA_DETAILS = By.cssSelector(".popupContent #srcMediaDetails");
  public static final By SOURCE_FIRST_NAME = By.cssSelector(".popupContent #srcFirstName");
  public static final By SOURCE_LAST_NAME = By.cssSelector(".popupContent #srcLastName");
  public static final By SOURCE_TEL_NO = By.cssSelector(".popupContent #srcTelNo");
  public static final By SOURCE_EMAIL = By.cssSelector(".popupContent #srcEmail");
  public static final By SOURCE_INSTITUTIONAL_PARTNER =
      By.cssSelector(".popupContent #srcInstitutionalPartnerType div");
  public static final By SOURCE_INSTITUTIONAL_PARTNER_DETAILS =
      By.cssSelector(".popupContent #srcInstitutionalPartnerTypeDetails");
  public static final By SPECIFY_OTHER_EVENT_PLACE =
      By.cssSelector(".popupContent #typeOfPlaceText");
  public static final By MEANS_OF_TRANSPORT = By.cssSelector(".popupContent #meansOfTransport div");
  public static final By CONNECTION_NUMBER = By.cssSelector(".popupContent #connectionNumber");
  public static final By TRAVEL_DATE = By.cssSelector(".popupContent #travelDate");
  public static final By FACILITY_CATEGORY = By.cssSelector(".popupContent #typeGroup div");
  public static final By FACILITY_TYPE = By.cssSelector(".popupContent #facilityType div");
  public static final By FACILITY = By.cssSelector(".popupContent #facility div");
  public static final By MEANS_OF_TRANSPORT_DETAILS =
      By.cssSelector(".popupContent #meansOfTransportDetails");
  public static final By DISEASE_NAME = By.cssSelector(".popupContent #diseaseDetails");
  public static final By REPORT_DATE = By.cssSelector(".popupContent #reportDateTime input");
  public static final By NEW_EVENT_CREATED_MESSAGE =
      By.xpath("//*[contains(text(),'New event created')]");
}
