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

package de.symeda.sormas.api.campaign.data;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.ejb.Remote;
import javax.validation.Valid;

import de.symeda.sormas.api.campaign.CampaignDto;
import de.symeda.sormas.api.campaign.diagram.CampaignDiagramCriteria;
import de.symeda.sormas.api.campaign.diagram.CampaignDiagramDataDto;
import de.symeda.sormas.api.campaign.diagram.CampaignDiagramSeries;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaDto;
import de.symeda.sormas.api.infrastructure.community.CommunityReferenceDto;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.report.CampaignDataExtractDto;
import de.symeda.sormas.api.report.JsonDictionaryReportModelDto;
import de.symeda.sormas.api.user.FormAccess;
import de.symeda.sormas.api.utils.SortProperty;



@Remote
public interface CampaignFormDataFacade {

	CampaignFormDataDto saveCampaignFormData(@Valid CampaignFormDataDto dto);
	
	CampaignFormDataDto saveCampaignFormDataMobile(@Valid CampaignFormDataDto dto);

	List<CampaignFormDataDto> getByUuids(List<String> uuids);

	CampaignFormDataDto getCampaignFormDataByUuid(String campaignFormDataUuid);
	
	List<CampaignFormDataIndexDto> getCampaignFormDataByCreatingUser(String creatingUser);

	void deleteCampaignFormData(String campaignFormDataUuid);

	boolean isArchived(String campaignFormDataUuid);

	boolean exists(String uuid);

	CampaignFormDataReferenceDto getReferenceByUuid(String uuid);

	List<CampaignFormDataIndexDto> getIndexList(CampaignFormDataCriteria criteria, Integer first, Integer max, List<SortProperty> sortProperties);
  
	List<CampaignFormDataIndexDto> getIndexListPashto(CampaignFormDataCriteria criteria, Integer first, Integer max, List<SortProperty> sortProperties);

	List<CampaignFormDataIndexDto> getIndexListDari(CampaignFormDataCriteria criteria, Integer first, Integer max, List<SortProperty> sortProperties);

	List<CampaignFormDataIndexDto> getByCompletionAnalysis(CampaignFormDataCriteria criteria, Integer first, Integer max, List<SortProperty> sortProperties, FormAccess frm);

	List<CampaignFormDataIndexDto> getByTimelinessAnalysis(CampaignFormDataCriteria criteria, Integer first, Integer max, List<SortProperty> sortProperties, FormAccess frm);

	
	String getByCompletionAnalysisCount(CampaignFormDataCriteria criteria, Integer first, Integer max, List<SortProperty> sortProperties, FormAccess frm);
	
	List<CampaignFormDataIndexDto> getByCompletionAnalysisAdmin(CampaignFormDataCriteria criteria, Integer first, Integer max, List<SortProperty> sortProperties, FormAccess frm);
	
	String getByCompletionAnalysisCountAdmin(CampaignFormDataCriteria criteria, Integer first, Integer max, List<SortProperty> sortProperties, FormAccess frm);

	CampaignFormDataDto getExistingData(CampaignFormDataCriteria criteria);

	long count(CampaignFormDataCriteria criteria);

	List<CampaignDiagramDataDto> getDiagramData(List<CampaignDiagramSeries> diagramSeries, CampaignDiagramCriteria campaignDiagramCriteria);
	List<CampaignDiagramDataDto> getDiagramDataCard(List<CampaignDiagramSeries> diagramSeries, CampaignDiagramCriteria campaignDiagramCriteria);
	List<CampaignDiagramDataDto> getDiagramDataByGroups(List<CampaignDiagramSeries> diagramSeries, CampaignDiagramCriteria campaignDiagramCriteria);
	
	List<CampaignDiagramDataDto> getDiagramDataFlow(List<CampaignDiagramSeries> diagramSeries, CampaignDiagramCriteria campaignDiagramCriteria);
	List<CampaignDiagramDataDto> getDiagramDataCardFlow(List<CampaignDiagramSeries> diagramSeries, CampaignDiagramCriteria campaignDiagramCriteria);
	List<CampaignDiagramDataDto> getDiagramDataByGroupsFlow(List<CampaignDiagramSeries> diagramSeries, CampaignDiagramCriteria campaignDiagramCriteria);

	List<CampaignDiagramDataDto> getDiagramDataByAgeGroup(
		CampaignDiagramSeries diagramSeriesTotal,
		CampaignDiagramSeries diagramSeries,
		CampaignDiagramCriteria campaignDiagramCriteria);
	
	List<CampaignDiagramDataDto> getDiagramDataByAgeGroupCard(
			CampaignDiagramSeries diagramSeriesTotal,
			CampaignDiagramSeries diagramSeries,
			CampaignDiagramCriteria campaignDiagramCriteria);

	List<String> getAllActiveUuids();

	List<CampaignFormDataDto> getAllActiveAfter(Date date);
	
	List<CampaignFormDataDto> getAllActive();
	
	List<CampaignFormDataDto> getAllActiveData();
	List<CampaignFormDataDto> getAllActiveRef();

	void overwriteCampaignFormData(CampaignFormDataDto existingData, CampaignFormDataDto newData);
	
	List<CampaignFormDataDto> getCampaignFormData(String campaignformuuid, String formuuid);
	
	List<CampaignAggregateDataDto> getCampaignFormDataAggregatetoCSV(String campaignformuuid);
	
	List<MapCampaignDataDto> getCampaignDataforMaps();
	
	String getByClusterDropDown(CommunityReferenceDto community, CampaignFormMetaDto campaignForm, CampaignDto campaign);
	
	void deleteCampaignData(List<String> uuids);
	
	void verifyCampaignData(List<String> uuids);

	List<CampaignFormDataIndexDto> getByCompletionAnalysisNew(CampaignFormDataCriteria criteria, List<SortProperty> sortProperties, FormAccess frms);

	List<JsonDictionaryReportModelDto> getByJsonFormDefinitonToCSV();
	
	String getByJsonFormDefinitonToCSVCount();

	int prepareAllCompletionAnalysis();

	List<CampaignDataExtractDto> getCampaignFormDataExtractApi(String campaignformuuid, String formuuid);

	List<CampaignDataExtractDto> getCampaignFormDataPivotExtractApi();

	List<CampaignFormDataIndexDto> getFlwDuplicateErrorAnalysis(CampaignFormDataCriteria criteria, Integer first,
			Integer max, List<SortProperty> sortProperties);

	int getFlwDuplicateErrorAnalysisCount(CampaignFormDataCriteria criteria, Integer first, Integer max,
			List<SortProperty> sortProperties);

	String getByTimelinessAnalysisCount(CampaignFormDataCriteria criteria, Integer first, Integer max,
			List<SortProperty> sortProperties, FormAccess frms);
	
	List<CampaignFormDataIndexDto> getCreatingUsersUserType(String username);

	boolean getVerifiedStatus(String uuid);
	

}
