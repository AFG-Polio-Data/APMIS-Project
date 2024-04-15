package de.symeda.sormas.api.infrastructure;

import java.util.List;
import java.util.Set;

import javax.ejb.Remote;
import javax.validation.Valid;

import de.symeda.sormas.api.AgeGroup;
import de.symeda.sormas.api.statistics.StatisticsCaseCriteria;
import de.symeda.sormas.api.utils.ValidationRuntimeException;

@Remote
public interface PopulationDataFacade {

	Integer getDistrictPopulation(String districtUuid, PopulationDataCriteria critariax);
	
	Integer getDistrictPopulationByType(String districtUuid, String campaignUuid, AgeGroup ageGroup);
	
	List<PopulationDataDto> getDistrictPopulationByTypeUsingUUIDs(String districtUuid, String campaignUuid, AgeGroup ageGroup);
	
	List<PopulationDataDto> getDistrictModalityByUUIDsandCampaignUUIdAndAgeGroup(String districtUuid, String campaignUuid, AgeGroup ageGroup);
	
//	List<PopulationDataDto> getDistrictStatusByDistrictUuidandCampaignUUIdAndAgeGroup(String districtUuid, String campaignUuid, AgeGroup ageGroup);

	
	Integer getDistrictPopulationByUuidAndAgeGroup(String districtUuid, String campaignUuid, String ageGroup);
	
	String getDistrictModalityByUuidAndCampaignAndAgeGroup(String districtUuid, String campaignUuid,  String ageGroup);
	
	String getDistrictStatusByCampaign(String districtUuid, String campaignUuid, String ageGroup);
	/**
	 * Returns the population of the district, projected to the current point in time based on its growth rate
	 */
	Integer getProjectedDistrictPopulation(String districtUuid, PopulationDataCriteria critariax);

	Integer getRegionPopulation(String regionUuid, PopulationDataCriteria critariax);

	/**
	 * Returns the population of the region, projected to the current point in time based on its growth rate
	 */
	Integer getProjectedRegionPopulation(String regionUuid, PopulationDataCriteria critariax);

	void savePopulationData(@Valid List<PopulationDataDto> populationDataList) throws ValidationRuntimeException;
	
	List<PopulationDataDto> getPopulationData(PopulationDataCriteria criteria);
	
	List<PopulationDataDto> getAllPopulationData();

	List<Object[]> getPopulationDataForExport(String campaignUuid);
	
	
	void savePopulationList(Set<PopulationDataDto> savePopulationList);

	/**
	 * Checks whether there is general population data available for all regions and districts
	 */
	List<Long> getMissingPopulationDataForStatistics(
		StatisticsCaseCriteria criteria,
		boolean groupByRegion,
		boolean groupByDistrict,
		boolean groupBySex,
		boolean groupByAgeGroup);

	List<PopulationDataDto> getPopulationDataImportChecker(PopulationDataCriteria criteria);

	List<PopulationDataDto> getPopulationDataWithCriteria(String criteria);

	void savePopulationDatax(@Valid List<PopulationDataDto> populationDataList,
			@Valid List<PopulationDataFauxDto> fauxPopulationDataList, boolean isFauxData) throws ValidationRuntimeException;
}
