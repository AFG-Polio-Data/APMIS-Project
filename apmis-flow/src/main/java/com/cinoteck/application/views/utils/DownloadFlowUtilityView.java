package com.cinoteck.application.views.utils;



import com.cinoteck.application.views.utils.ExportEntityName;
import com.opencsv.CSVWriter;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.StreamResource;

import de.symeda.sormas.api.AgeGroup;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.infrastructure.PopulationDataDto;
import de.symeda.sormas.api.person.Sex;
import de.symeda.sormas.api.utils.CSVUtils;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.DateHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DownloadFlowUtilityView{

  

    public static StreamResource createPopulationDataExportResource(String campaignUuid) {
        String exportFileName = createFileNameWithCurrentDate(ExportEntityName.POPULATION_DATA, ".csv");

        return new StreamResource(exportFileName, () -> {
            try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
                try (CSVWriter writer = CSVUtils.createCSVWriter(
                		new OutputStreamWriter(byteStream, StandardCharsets.UTF_8.name()),
						FacadeProvider.getConfigFacade().getCsvSeparator())) {
						// Generate and write columns to CSV writer
						List<String> columnNames = new ArrayList<>();
						columnNames.add(I18nProperties.getPrefixCaption(PopulationDataDto.I18N_PREFIX, PopulationDataDto.REGION));
						columnNames.add(I18nProperties.getPrefixCaption(PopulationDataDto.I18N_PREFIX, PopulationDataDto.DISTRICT));
						columnNames.add(I18nProperties.getPrefixCaption(PopulationDataDto.I18N_PREFIX, PopulationDataDto.COMMUNITY));
						//add campaign uuid to the last row
						columnNames.add(I18nProperties.getCaption(Captions.Campaign));
						columnNames.add(I18nProperties.getString(Strings.total));
						columnNames.add(I18nProperties.getCaption(Captions.populationDataMaleTotal));
						columnNames.add(I18nProperties.getCaption(Captions.populationDataFemaleTotal));
						

						Map<AgeGroup, Integer> ageGroupPositions = new HashMap<>();
						int ageGroupIndex = 6;
						for (AgeGroup ageGroup : AgeGroup.values()) {
							columnNames.add(DataHelper.getSexAndAgeGroupString(ageGroup, null));
							columnNames.add(DataHelper.getSexAndAgeGroupString(ageGroup, Sex.MALE));
							columnNames.add(DataHelper.getSexAndAgeGroupString(ageGroup, Sex.FEMALE));
							columnNames.add(DataHelper.getSexAndAgeGroupString(ageGroup, Sex.OTHER));
							ageGroupPositions.put(ageGroup, ageGroupIndex);
							ageGroupIndex += 4;
						}
						

						writer.writeNext(columnNames.toArray(new String[columnNames.size()]));

						List<Object[]> populationExportDataList = FacadeProvider.getPopulationDataFacade().getPopulationDataForExport(campaignUuid);

						String[] exportLine = new String[columnNames.size()];
						String regionName = "";
						String districtName = "";
						String communityName = "";
						String campaignName = "";
						for (Object[] populationExportData : populationExportDataList) {
							String dataRegionName = (String) populationExportData[0];
							String dataDistrictName = populationExportData[1] == null ? "" : (String) populationExportData[1];
							String dataCommunityName = populationExportData[2] == null ? "" : (String) populationExportData[2];
							String dataCampaignName = populationExportData[3] == null ? "" : (String) populationExportData[3];
							if (exportLine[0] != null
								&& (!dataRegionName.equals(regionName)
									|| !dataDistrictName.equals(districtName)
									|| !dataCampaignName.equals(campaignName)
									|| !dataCommunityName.equals(communityName))) {
								// New region or district reached; write line to CSV
								writer.writeNext(exportLine);
								exportLine = new String[columnNames.size()];
							}
							regionName = dataRegionName;
							districtName = dataDistrictName;
							communityName = dataCommunityName;
							campaignName = dataCampaignName;

							// Region
							if (exportLine[0] == null) {
								exportLine[0] = (String) populationExportData[0];
							}
							// District
							if (exportLine[1] == null) {
								exportLine[1] = (String) populationExportData[1];
							}
							// Community
							if (exportLine[2] == null) {
								exportLine[2] = (String) populationExportData[2];
							}
							
							//campaign
							if (exportLine[3] == null) {
								exportLine[3] = (String) populationExportData[3];
							}

							if (populationExportData[4] == null) {
								// Total population
								String sexString = (String) populationExportData[5];
								if (Sex.MALE.getName().equals(sexString)) {
									exportLine[5] = String.valueOf((int) populationExportData[6]);
								} else if (Sex.FEMALE.getName().equals(sexString)) {
									exportLine[6] = String.valueOf((int) populationExportData[6]);
								} else if (Sex.OTHER.getName().equals(sexString)) {
									exportLine[7] = String.valueOf((int) populationExportData[6]);
								} else {
									exportLine[4] = String.valueOf((int) populationExportData[6]);
								}
							} else {
								// Population based on age group position and sex
								Integer ageGroupPosition = ageGroupPositions.get(AgeGroup.valueOf((String) populationExportData[4]));
								String sexString = (String) populationExportData[5];
								if (Sex.MALE.getName().equals(sexString)) {
									ageGroupPosition += 1;
								} else if (Sex.FEMALE.getName().equals(sexString)) {
									ageGroupPosition += 2;
								} else if (Sex.OTHER.getName().equals(sexString)) {
									ageGroupPosition += 3;
								}
								exportLine[ageGroupPosition] = String.valueOf((int) populationExportData[6]);
							}
							
							columnNames.add(I18nProperties.getCaption(Captions.Campaign));
						}

						// Write last line to CSV
						writer.writeNext(exportLine);
						writer.flush();
					
                }
                return new ByteArrayInputStream(byteStream.toByteArray());
            } catch (IOException e) {
                // Handle exceptions and show a notification if needed
                return null;
            }
        });
    }

    public static String createFileNameWithCurrentDate(ExportEntityName entityName, String fileExtension) {
		String instanceName = FacadeProvider.getConfigFacade().getSormasInstanceName().toLowerCase(); //The export is being prepared
		String processedInstanceName = DataHelper.cleanStringForFileName(instanceName);
		String processedEntityName = DataHelper.cleanStringForFileName(entityName.getLocalizedNameInSystemLanguage());
		String exportDate = DateHelper.formatDateForExport(new Date());
		return String.join("_", processedInstanceName, processedEntityName, exportDate, fileExtension);
	}
}