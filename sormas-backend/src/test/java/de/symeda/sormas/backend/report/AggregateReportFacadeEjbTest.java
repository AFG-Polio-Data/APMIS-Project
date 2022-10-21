/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2022 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.backend.report;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.facility.FacilityDto;
import de.symeda.sormas.api.infrastructure.facility.FacilityReferenceDto;
import de.symeda.sormas.api.infrastructure.pointofentry.PointOfEntryReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.report.AggregateCaseCountDto;
import de.symeda.sormas.api.report.AggregateReportCriteria;
import de.symeda.sormas.api.report.AggregateReportDto;
import de.symeda.sormas.api.report.AggregateReportGroupingLevel;
import de.symeda.sormas.api.user.DefaultUserRole;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.api.utils.EpiWeek;
import de.symeda.sormas.backend.AbstractBeanTest;
import de.symeda.sormas.backend.TestDataCreator;
import de.symeda.sormas.backend.infrastructure.district.District;
import de.symeda.sormas.backend.infrastructure.district.DistrictFacadeEjb;
import de.symeda.sormas.backend.infrastructure.region.Region;
import de.symeda.sormas.backend.infrastructure.region.RegionFacadeEjb;

public class AggregateReportFacadeEjbTest extends AbstractBeanTest {

	TestDataCreator.RDCF rdcf;
	FacilityDto facility2;
	Region region2;
	District district2;
	private UserDto officer;
	private UserDto informant1;
	private UserDto informant2;

	@Before
	public void setupData() {

		rdcf = creator.createRDCF("Region", "District", "Community", "Facility", "PointOfEntry");
		region2 = creator.createRegion("Region2");
		district2 = creator.createDistrict("District2", region2);
		facility2 = creator.createFacility("Facility2", rdcf.region, rdcf.district, rdcf.community);
		officer = creator.createUser(
			rdcf.region.getUuid(),
			rdcf.district.getUuid(),
			null,
			"Off",
			"One",
			creator.getUserRoleReference(DefaultUserRole.SURVEILLANCE_OFFICER));

		informant1 = creator.createUser(
			rdcf.region.getUuid(),
			rdcf.district.getUuid(),
			rdcf.facility.getUuid(),
			"Info",
			"One",
			creator.getUserRoleReference(DefaultUserRole.HOSPITAL_INFORMANT));
		informant1.setAssociatedOfficer(officer.toReference());
		getUserFacade().saveUser(informant1, false);

		informant2 = creator.createUser(
			rdcf.region.getUuid(),
			rdcf.district.getUuid(),
			rdcf.facility.getUuid(),
			"Info",
			"Two",
			creator.getUserRoleReference(DefaultUserRole.HOSPITAL_INFORMANT));
		informant2.setAssociatedOfficer(officer.toReference());
		getUserFacade().saveUser(informant2, false);

	}

	@Test
	public void testAggregateReportWithHospitalInformant() {
		loginWith(informant1);

		EpiWeek epiWeek = DateHelper.getEpiWeek(new Date());

		AggregateReportDto aggregateReportDto = AggregateReportDto.build();
		aggregateReportDto.setDisease(Disease.HIV);
		aggregateReportDto.setReportingUser(informant1.toReference());
		aggregateReportDto.setNewCases(1);
		aggregateReportDto.setDeaths(3);
		aggregateReportDto.setLabConfirmations(2);
		aggregateReportDto.setYear(epiWeek.getYear());
		aggregateReportDto.setEpiWeek(epiWeek.getWeek());
		aggregateReportDto.setRegion(rdcf.region);
		aggregateReportDto.setDistrict(rdcf.district);
		aggregateReportDto.setHealthFacility(rdcf.facility);
		getAggregateReportFacade().saveAggregateReport(aggregateReportDto);

		AggregateReportCriteria criteria = new AggregateReportCriteria().healthFacility(rdcf.facility);
		criteria.setShowZeroRows(true);
		criteria.epiWeekFrom(DateHelper.getEpiWeek(new Date())).epiWeekTo(DateHelper.getEpiWeek(new Date()));

		List<AggregateCaseCountDto> indexList = getAggregateReportFacade().getIndexList(criteria);
		int aggregatedDiseaseCount = getDiseaseConfigurationFacade().getAllDiseases(true, null, false, true).size();
		Assert.assertEquals(aggregatedDiseaseCount, indexList.size());
		Assert.assertEquals(1, indexList.stream().filter(aggregatedCaseCountDto -> aggregatedCaseCountDto.getDeaths() == 3).count());
	}

	@Test
	public void testAggregateReportsSorting() {
		loginWith(informant1);

		EpiWeek epiWeek = DateHelper.getEpiWeek(new Date());

		AggregateReportDto reportDto = createAggregateReport(epiWeek, "61Y");
		createAggregateReport(epiWeek, "41Y_60Y");
		createAggregateReport(epiWeek, "21Y_30Y");
		createAggregateReport(epiWeek, "5Y_15Y");
		createAggregateReport(epiWeek, "31Y_40Y");
		createAggregateReport(epiWeek, "60M_4Y");
		createAggregateReport(epiWeek, "0D_30D");
		createAggregateReport(epiWeek, "1M_59M");

		AggregateReportCriteria criteria = new AggregateReportCriteria().healthFacility(rdcf.facility);
		criteria.setShowZeroRows(true);
		criteria.epiWeekFrom(DateHelper.getEpiWeek(new Date())).epiWeekTo(DateHelper.getEpiWeek(new Date()));

		List<AggregateCaseCountDto> indexList = getAggregateReportFacade().getIndexList(criteria);

		int aggregatedDiseaseCount = getDiseaseConfigurationFacade().getAllDiseases(true, null, false, true).size();
		Assert.assertEquals(aggregatedDiseaseCount + 8 - 1, indexList.size());

		int index = 0;
		for (AggregateCaseCountDto caseCountDto : indexList) {
			if (caseCountDto.getDisease() == reportDto.getDisease())
				break;
			index++;
		}
		Assert.assertEquals("0D_30D", indexList.get(index++).getAgeGroup());
		Assert.assertEquals("1M_59M", indexList.get(index++).getAgeGroup());
		Assert.assertEquals("60M_4Y", indexList.get(index++).getAgeGroup());
		Assert.assertEquals("5Y_15Y", indexList.get(index++).getAgeGroup());
		Assert.assertEquals("21Y_30Y", indexList.get(index++).getAgeGroup());
		Assert.assertEquals("31Y_40Y", indexList.get(index++).getAgeGroup());
		Assert.assertEquals("41Y_60Y", indexList.get(index++).getAgeGroup());
		Assert.assertEquals("61Y", indexList.get(index++).getAgeGroup());
	}

	private AggregateReportDto createAggregateReport(EpiWeek epiWeek, String ageGroup) {
		return createAggregateReport(epiWeek, ageGroup, rdcf.region, rdcf.district, rdcf.facility, rdcf.pointOfEntry, 1, 3, 2);
	}

	private AggregateReportDto createAggregateReport(Disease disease, String ageGroup, Integer cases, Integer deaths, Integer labConfirmations) {
		return createAggregateReportDto(
			disease,
			DateHelper.getEpiWeek(new Date()),
			ageGroup,
			rdcf.region,
			rdcf.district,
			null,
			null,
			cases,
			deaths,
			labConfirmations);
	}

	private AggregateReportDto createAggregateReport(
		Integer cases,
		Integer deaths,
		Integer labConfirmations,
		RegionReferenceDto region,
		DistrictReferenceDto district,
		FacilityReferenceDto facility,
		PointOfEntryReferenceDto pointOfEntry) {
		return createAggregateReport(
			DateHelper.getEpiWeek(new Date()),
			"1Y_100Y",
			region,
			district,
			facility,
			pointOfEntry,
			cases,
			deaths,
			labConfirmations);
	}

	private AggregateReportDto createAggregateReport(
		EpiWeek epiWeek,
		String ageGroup,
		RegionReferenceDto region,
		DistrictReferenceDto district,
		FacilityReferenceDto facility,
		PointOfEntryReferenceDto pointOfEntry,
		int newCases,
		int deaths,
		int labConfirmations) {
		return createAggregateReportDto(Disease.HIV, epiWeek, ageGroup, region, district, facility, pointOfEntry, newCases, deaths, labConfirmations);
	}

	private AggregateReportDto createAggregateReportDto(
		Disease disease,
		EpiWeek epiWeek,
		String ageGroup,
		RegionReferenceDto region,
		DistrictReferenceDto district,
		FacilityReferenceDto facility,
		PointOfEntryReferenceDto pointOfEntry,
		int newCases,
		int deaths,
		int labConfirmations) {
		AggregateReportDto aggregateReportDto = AggregateReportDto.build();
		aggregateReportDto.setDisease(disease);
		aggregateReportDto.setReportingUser(informant1.toReference());
		aggregateReportDto.setNewCases(newCases);
		aggregateReportDto.setDeaths(deaths);
		aggregateReportDto.setLabConfirmations(labConfirmations);
		aggregateReportDto.setYear(epiWeek.getYear());
		aggregateReportDto.setEpiWeek(epiWeek.getWeek());
		aggregateReportDto.setRegion(region);
		aggregateReportDto.setDistrict(district);
		aggregateReportDto.setHealthFacility(facility);
		aggregateReportDto.setPointOfEntry(pointOfEntry);
		aggregateReportDto.setAgeGroup(ageGroup);
		return getAggregateReportFacade().saveAggregateReport(aggregateReportDto);
	}

	@Test
	public void testAggregateReportsSummarize() {
		loginWith(informant1);

		createAggregateReport(1, 1, 1, rdcf.region, rdcf.district, null, null);
		createAggregateReport(2, 2, 2, rdcf.region, rdcf.district, null, null);
		createAggregateReport(3, 3, 3, rdcf.region, rdcf.district, rdcf.facility, null);

		AggregateReportCriteria criteria = new AggregateReportCriteria();
		criteria.setShowZeroRows(false);
		criteria.epiWeekFrom(DateHelper.getEpiWeek(new Date())).epiWeekTo(DateHelper.getEpiWeek(new Date()));

		criteria.setAggregateReportGroupingLevel(AggregateReportGroupingLevel.DISTRICT);
		List<AggregateCaseCountDto> indexList = getAggregateReportFacade().getIndexList(criteria);
		Assert.assertEquals(1, indexList.size());
		Assert.assertEquals(2, indexList.get(0).getNewCases());
		Assert.assertEquals(informant1.toReference(), indexList.get(0).getReportingUser());

		criteria.setAggregateReportGroupingLevel(AggregateReportGroupingLevel.REGION);
		List<AggregateCaseCountDto> indexListRegionGrouping = getAggregateReportFacade().getIndexList(criteria);
		Assert.assertEquals(1, indexListRegionGrouping.size());
		Assert.assertEquals(2, indexListRegionGrouping.get(0).getNewCases());
		Assert.assertNull(indexListRegionGrouping.get(0).getReportingUser());
	}

	@Test
	public void testAggregateReportsSummarizeAllWhenNoGrouping() {
		loginWith(informant1);

		createAggregateReport(1, 1, 1, rdcf.region, rdcf.district, facility2.toReference(), null);
		createAggregateReport(2, 2, 2, rdcf.region, rdcf.district, rdcf.facility, null);
		createAggregateReport(3, 10, 3, rdcf.region, rdcf.district, null, rdcf.pointOfEntry);
		createAggregateReport(11, 10, 3, RegionFacadeEjb.toReferenceDto(region2), DistrictFacadeEjb.toReferenceDto(district2), null, null);

		AggregateReportCriteria criteria = new AggregateReportCriteria();
		criteria.setShowZeroRows(false);
		criteria.epiWeekFrom(DateHelper.getEpiWeek(new Date())).epiWeekTo(DateHelper.getEpiWeek(new Date()));

		criteria.setAggregateReportGroupingLevel(null);
		List<AggregateCaseCountDto> indexList = getAggregateReportFacade().getIndexList(criteria);
		Assert.assertEquals(1, indexList.size());
		Assert.assertEquals(17, indexList.get(0).getNewCases());
		Assert.assertNull(indexList.get(0).getReportingUser());
	}

	@Test
	public void testAggregateReportsSummarizeMultipleSubJurisdictions() {
		loginWith(informant1);

		createAggregateReport(1, 1, 1, rdcf.region, rdcf.district, facility2.toReference(), null);
		createAggregateReport(2, 2, 2, rdcf.region, rdcf.district, rdcf.facility, null);
		createAggregateReport(3, 10, 3, rdcf.region, rdcf.district, null, rdcf.pointOfEntry);

		AggregateReportCriteria criteria = new AggregateReportCriteria();
		criteria.setShowZeroRows(false);
		criteria.epiWeekFrom(DateHelper.getEpiWeek(new Date())).epiWeekTo(DateHelper.getEpiWeek(new Date()));

		criteria.setAggregateReportGroupingLevel(AggregateReportGroupingLevel.DISTRICT);
		List<AggregateCaseCountDto> indexList = getAggregateReportFacade().getIndexList(criteria);
		Assert.assertEquals(1, indexList.size());
		Assert.assertEquals(6, indexList.get(0).getNewCases());
		Assert.assertEquals(13, indexList.get(0).getDeaths());
		Assert.assertNull(indexList.get(0).getReportingUser());

		criteria.setAggregateReportGroupingLevel(AggregateReportGroupingLevel.HEALTH_FACILITY);
		List<AggregateCaseCountDto> indexListFacilityGrouping = getAggregateReportFacade().getIndexList(criteria);
		Assert.assertEquals(2, indexListFacilityGrouping.size());
		Assert.assertEquals(2, indexListFacilityGrouping.get(0).getNewCases());
		Assert.assertEquals(2, indexListFacilityGrouping.get(0).getDeaths());
		Assert.assertEquals(1, indexListFacilityGrouping.get(1).getNewCases());
		Assert.assertEquals(1, indexListFacilityGrouping.get(1).getDeaths());

		criteria.setAggregateReportGroupingLevel(AggregateReportGroupingLevel.POINT_OF_ENTRY);
		List<AggregateCaseCountDto> indexListPoeGrouping = getAggregateReportFacade().getIndexList(criteria);
		Assert.assertEquals(1, indexListPoeGrouping.size());
		Assert.assertEquals(3, indexListPoeGrouping.get(0).getNewCases());
		Assert.assertEquals(10, indexListPoeGrouping.get(0).getDeaths());

		criteria.setAggregateReportGroupingLevel(AggregateReportGroupingLevel.REGION);
		List<AggregateCaseCountDto> indexListRegionGrouping = getAggregateReportFacade().getIndexList(criteria);
		Assert.assertEquals(1, indexListRegionGrouping.size());
		Assert.assertEquals(6, indexListRegionGrouping.get(0).getNewCases());
		Assert.assertEquals(13, indexListRegionGrouping.get(0).getDeaths());
		Assert.assertNull(indexListRegionGrouping.get(0).getReportingUser());

		criteria.setAggregateReportGroupingLevel(null);
		List<AggregateCaseCountDto> indexListNullGrouping = getAggregateReportFacade().getIndexList(criteria);
		Assert.assertEquals(1, indexListNullGrouping.size());
		Assert.assertEquals(6, indexListNullGrouping.get(0).getNewCases());
		Assert.assertEquals(13, indexListNullGrouping.get(0).getDeaths());
		Assert.assertNull(indexListNullGrouping.get(0).getReportingUser());

		criteria.setAggregateReportGroupingLevel(AggregateReportGroupingLevel.DISTRICT);
		createAggregateReport(4, 4, 4, rdcf.region, rdcf.district, null, null);
		List<AggregateCaseCountDto> indexListDistrictGroupingWhenDistrictData = getAggregateReportFacade().getIndexList(criteria);
		Assert.assertEquals(1, indexListDistrictGroupingWhenDistrictData.size());
		Assert.assertEquals(4, indexListDistrictGroupingWhenDistrictData.get(0).getNewCases());
		Assert.assertEquals(4, indexListDistrictGroupingWhenDistrictData.get(0).getDeaths());
		Assert.assertEquals(informant1.toReference(), indexListDistrictGroupingWhenDistrictData.get(0).getReportingUser());
	}

	@Test
	public void testAggregatereportSummarizeConsidersUpperLevelData() {
		useNationalUserLogin();

		createAggregateReport(2, 2, 2, rdcf.region, rdcf.district, facility2.toReference(), null);
		createAggregateReport(4, 4, 4, rdcf.region, rdcf.district, null, rdcf.pointOfEntry);
		createAggregateReport(3, 3, 3, rdcf.region, rdcf.district, rdcf.facility, null);
		createAggregateReport(1, 1, 1, rdcf.region, rdcf.district, null, null);

		AggregateReportCriteria criteria = new AggregateReportCriteria();
		criteria.setShowZeroRows(false);
		criteria.epiWeekFrom(DateHelper.getEpiWeek(new Date())).epiWeekTo(DateHelper.getEpiWeek(new Date()));

		criteria.setAggregateReportGroupingLevel(AggregateReportGroupingLevel.DISTRICT);
		List<AggregateCaseCountDto> indexList = getAggregateReportFacade().getIndexList(criteria);
		Assert.assertEquals(1, indexList.size());
		Assert.assertEquals(1, indexList.get(0).getNewCases());
	}

	@Test
	public void testAggregateReportGetEditData() {
		useNationalUserLogin();
		Disease disease = Disease.ACUTE_VIRAL_HEPATITIS;
		creator.updateDiseaseConfiguration(disease, false, false, false, true, null);

		createAggregateReport(disease, null, 1, 1, 1);
		AggregateReportDto selectedAggregateReport = createAggregateReport(disease, null, 2, 2, 2);

		List<AggregateReportDto> similarAggregateReports = getAggregateReportFacade().getSimilarAggregateReports(selectedAggregateReport);

		Assert.assertEquals(1, similarAggregateReports.size());
		Assert.assertEquals(2, similarAggregateReports.get(0).getNewCases().intValue());
		Assert.assertFalse(similarAggregateReports.get(0).isExpiredAgeGroup());

		creator.updateDiseaseConfiguration(disease, false, false, false, true, Arrays.asList("0D_28D", "1M_12M", "1Y_5Y", "6Y"));

		createAggregateReport(disease, "0D_28D", 3, 3, 3);
		createAggregateReport(disease, "1M_12M", 4, 4, 4);
		createAggregateReport(disease, "1Y_5Y", 5, 5, 5);
		createAggregateReport(disease, "6Y", 6, 6, 6);

		createAggregateReport(disease, "0D_28D", 31, 31, 31);
		AggregateReportDto selectedAggregateReport2 = createAggregateReport(disease, "1M_12M", 41, 41, 41);
		createAggregateReport(disease, "1Y_5Y", 51, 51, 51);
		createAggregateReport(disease, "6Y", 61, 61, 61);

		List<AggregateReportDto> similarAggregateReports2 = getAggregateReportFacade().getSimilarAggregateReports(selectedAggregateReport);
		Assert.assertEquals(5, similarAggregateReports2.size());
		Assert.assertTrue(similarAggregateReports2.get(similarAggregateReports2.indexOf(selectedAggregateReport)).isExpiredAgeGroup());
		Assert.assertFalse(similarAggregateReports2.get(similarAggregateReports2.indexOf(selectedAggregateReport2)).isExpiredAgeGroup());
		Assert.assertEquals(41, similarAggregateReports2.get(similarAggregateReports2.indexOf(selectedAggregateReport2)).getNewCases().intValue());

		creator.updateDiseaseConfiguration(disease, false, false, false, true, Arrays.asList("0D_2Y", "3Y_10Y", "11Y"));
		createAggregateReport(disease, "0D_2Y", 7, 7, 7);
		AggregateReportDto selectedAggregateReport3 = createAggregateReport(disease, "3Y_10Y", 8, 8, 8);
		createAggregateReport(disease, "11Y", 9, 9, 9);

		List<AggregateReportDto> similarAggregateReports3 = getAggregateReportFacade().getSimilarAggregateReports(selectedAggregateReport);
		Assert.assertEquals(8, similarAggregateReports3.size());
		Assert.assertTrue(similarAggregateReports3.get(similarAggregateReports3.indexOf(selectedAggregateReport)).isExpiredAgeGroup());
		Assert.assertTrue(similarAggregateReports3.get(similarAggregateReports3.indexOf(selectedAggregateReport2)).isExpiredAgeGroup());
		Assert.assertFalse(similarAggregateReports3.get(similarAggregateReports3.indexOf(selectedAggregateReport3)).isExpiredAgeGroup());

		creator.updateDiseaseConfiguration(disease, false, false, false, true, null);
		List<AggregateReportDto> similarAggregateReports4 = getAggregateReportFacade().getSimilarAggregateReports(selectedAggregateReport);
		Assert.assertEquals(8, similarAggregateReports3.size());
		Assert.assertFalse(similarAggregateReports4.get(similarAggregateReports4.indexOf(selectedAggregateReport)).isExpiredAgeGroup());
		Assert.assertTrue(similarAggregateReports4.get(similarAggregateReports4.indexOf(selectedAggregateReport2)).isExpiredAgeGroup());
		Assert.assertTrue(similarAggregateReports4.get(similarAggregateReports4.indexOf(selectedAggregateReport3)).isExpiredAgeGroup());
	}
}