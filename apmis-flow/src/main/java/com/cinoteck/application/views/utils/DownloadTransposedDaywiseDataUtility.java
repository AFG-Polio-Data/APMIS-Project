package com.cinoteck.application.views.utils;

import com.opencsv.CSVWriter;


import com.vaadin.flow.server.StreamResource;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.campaign.data.CampaignFormDataCriteria;
import de.symeda.sormas.api.campaign.data.CampaignFormDataEntry;
import de.symeda.sormas.api.campaign.data.CampaignFormDataIndexDto;
import de.symeda.sormas.api.campaign.form.CampaignFormElement;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaDto;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaIndexDto;
import de.symeda.sormas.api.utils.CSVUtils;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.DateHelper;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
public final class DownloadTransposedDaywiseDataUtility {

    CampaignFormDataCriteria criteria = new CampaignFormDataCriteria();

    public static StreamResource createTransposedDataFromIndexList(CampaignFormDataCriteria criteria, String formName,
            String campaignName) {

    	
//        String exportFileName = "APMIS_" + formName + "_" + campaignName + ".csv";
        
		String exportFileName = campaignName + "_" + formName + "_LONG_" +  new SimpleDateFormat("yyyyddMM").format(Calendar.getInstance().getTime())+ ".csv";// createFileNameWithCurrentDateandEntityNameString(formName+

//        + new SimpleDateFormat("yyyyddMM").format(Calendar.getInstance().getTime());
        List<CampaignFormDataIndexDto> formDatafromIndexList = FacadeProvider.getCampaignFormDataFacade()
                .getIndexList(criteria, null, null, null);

        Set<String> fieldIDWithoutDaySuffix = new HashSet<>();
        for (CampaignFormDataIndexDto formData : formDatafromIndexList) {
            if (formData.getFormValues() != null) {
                Set<String> fieldIDsFromFormValues = new HashSet<>();
                for (CampaignFormDataEntry formValues : formData.getFormValues()) {
                    fieldIDsFromFormValues.add(formValues.getId());
                }
                List<String> fieldIDsforColumnHeaders = new ArrayList<>(fieldIDsFromFormValues);
                for (String uniqueVariable : extractUniqueVariableParts(fieldIDsforColumnHeaders)) {
                    String variableID = uniqueVariable.replaceAll("(_day\\d+|_days\\d+)$", "");
                    fieldIDWithoutDaySuffix.add(variableID);
                }
            }
        }

        List<String> columnNames = new ArrayList<>();
        Map<String, String> fieldIdToCaptionMap = new HashMap<>();
        List<String> fieldCaptions = new ArrayList<>();
        CampaignFormMetaDto formMetaReference = FacadeProvider.getCampaignFormMetaFacade()
                .getCampaignFormMetaByUuid(criteria.getCampaignFormMeta().getUuid());
        if (formMetaReference != null) {
            List<CampaignFormElement> campaignFormElements = formMetaReference.getCampaignFormElements();
            fieldIdToCaptionMap = matchFieldsWithCaptions(fieldIDWithoutDaySuffix, campaignFormElements);
        }

        columnNames.addAll(Arrays.asList("Campaign", "Form", "Region", "RCode", "Province", "PCode", "District", "Dcode",
                "Cluster", "Cluster Number", "CCode", "Form Phase", "Source", "Creating User", "Day"));

        fieldCaptions.addAll(Arrays.asList("Campaign", "Form", "Region", "RCode", "Province", "PCode", "District", "Dcode",
                "Cluster", "Cluster Number", "CCode", "Form Phase", "Source", "Creating User", "Day"));

        Map<String, Integer> fieldIdPositions = new HashMap<>();
        int ageGroupIndex = 15;
        for (String fieldGroup : fieldIDWithoutDaySuffix) {
            columnNames.add(fieldGroup);
            fieldIdPositions.put(fieldGroup, ageGroupIndex);
            ageGroupIndex++;
        }

        for (String fieldId : fieldIDWithoutDaySuffix) {
            String caption = fieldIdToCaptionMap.getOrDefault(fieldId, fieldId);
            fieldCaptions.add(caption);
            fieldIdPositions.put(caption, ageGroupIndex);
            ageGroupIndex++;
        }

        Map<String, Map<String, String>> dayValueMap = new HashMap<>();
        for (CampaignFormDataIndexDto individualTransposedFormData : formDatafromIndexList) {
            if (individualTransposedFormData.getFormValues() != null) {
                Map<String, String> formDataMaxp = new HashMap<>();
                Set<String> fieldIDsFromFormValues = new HashSet<>();
                for (CampaignFormDataEntry formValues : individualTransposedFormData.getFormValues()) {
                    formDataMaxp.put(formValues.getId(), formValues.getValue().toString());
                    fieldIDsFromFormValues.add(formValues.getId());
                }

                List<String> fieldIDsforColumnHeaders = new ArrayList<>(fieldIDsFromFormValues);
                Set<String> uniqueVariablePartsWithoutDaySuffixForColumnHeader = new HashSet<>();
                for (String uniqueVariable : extractUniqueVariableParts(fieldIDsforColumnHeaders)) {
                    String variableID = uniqueVariable.replaceAll("(_day\\d+|_days\\d+)$", "");
                    uniqueVariablePartsWithoutDaySuffixForColumnHeader.add(variableID);
                }

                for (String day : extractUniqueDayValues(fieldIDsforColumnHeaders)) {
                    for (String variable : uniqueVariablePartsWithoutDaySuffixForColumnHeader) {
                        String key = variable + "_" + day;
                        if (formDataMaxp.containsKey(key)) {
                            String keyValue = formDataMaxp.get(key);
                            dayValueMap.computeIfAbsent(day, k -> new HashMap<>()).put(variable, keyValue);
                        }
                    }
                }
            }
        }

        return new StreamResource(exportFileName, () -> {
            try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
                byteStream.write(0xEF);
                byteStream.write(0xBB);
                byteStream.write(0xBF);

                try (CSVWriter writer = CSVUtils.createCSVWriter(new OutputStreamWriter(byteStream, StandardCharsets.UTF_8),
                        FacadeProvider.getConfigFacade().getCsvSeparator())) {

                    writer.writeNext(fieldCaptions.toArray(new String[0]));
                    writer.writeNext(columnNames.toArray(new String[0]));

                    for (CampaignFormDataIndexDto individualTransposedFormData : formDatafromIndexList) {
                        if (individualTransposedFormData.getFormValues() != null) {
                            Map<String, String> formDataMaxp = new LinkedHashMap<>();
                            Set<String> fieldIDsFromFormValues = new LinkedHashSet<>();
                            for (CampaignFormDataEntry formValues : individualTransposedFormData.getFormValues()) {
                                String value = formValues.getValue().toString();
                                formDataMaxp.put(formValues.getId(), value);
                                fieldIDsFromFormValues.add(formValues.getId());
                            }

                            List<String> fieldIDsforColumnHeaders = new ArrayList<>(fieldIDsFromFormValues);
                            Set<String> uniqueVariablePartsWithoutDaySuffixForColumnHeader = new LinkedHashSet<>();
                            for (String uniqueVariable : extractUniqueVariableParts(fieldIDsforColumnHeaders)) {
                                String variableID = uniqueVariable.replaceAll("(_day\\d+|_days\\d+)$", "");
                                uniqueVariablePartsWithoutDaySuffixForColumnHeader.add(variableID);
                            }

                            for (String day : extractUniqueDayValues(fieldIDsforColumnHeaders)) {
                                List<String> row = new ArrayList<>(Collections.nCopies(columnNames.size(), ""));
                                row.set(0, individualTransposedFormData.getCampaign().toString());
                                row.set(1, individualTransposedFormData.getForm().toString());
                                row.set(2, individualTransposedFormData.getArea().toString());
                                row.set(3, individualTransposedFormData.getRcode().toString());
                                row.set(4, individualTransposedFormData.getRegion().toString());
                                row.set(5, individualTransposedFormData.getPcode() + "");
                                row.set(6, individualTransposedFormData.getDistrict().toString());
                                row.set(7, individualTransposedFormData.getDcode() + "");
                                if (individualTransposedFormData.getCommunity() == null) {
                                    row.set(8, "");
                                    row.set(9, "");
                                    row.set(10, "");
                                } else {
                                    row.set(8, individualTransposedFormData.getCommunity().toString());
                                    row.set(9, individualTransposedFormData.getClusternumber().toString());
                                    row.set(10, individualTransposedFormData.getCcode().toString());
                                }
                                row.set(11, individualTransposedFormData.getFormType().toString());
                                row.set(12,
                                        individualTransposedFormData.getSource() != null
                                                ? individualTransposedFormData.getSource().toString()
                                                : "");
                                row.set(13,
                                        individualTransposedFormData.getCreatingUser() != null
                                                ? individualTransposedFormData.getCreatingUser().toString()
                                                : "");
                                row.set(14, day.split("_")[1]);

                                for (String variable : uniqueVariablePartsWithoutDaySuffixForColumnHeader) {
                                    String key = variable +  day;
                                    if (formDataMaxp.containsKey(key)) {
                                        String keyValue = formDataMaxp.get(key);
                                        int colIndex = columnNames.indexOf(variable);
                                        if (colIndex >= 0) {
                                            row.set(colIndex, keyValue);
                                        }
                                    }
                                }
                                writer.writeNext(row.toArray(new String[0]));
                            }
                        }
                    }
                }

                return new ByteArrayInputStream(byteStream.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException("Error generating CSV file", e);
            }
        });
    }
    

	public static List<String> extractUniqueVariableParts(List<String> formEntriesID) {
	Set<String> variablePartsSet = new HashSet<>();
	Pattern pattern = Pattern.compile("(^(.+)day\\d+|^(.+)days\\d+|_day\\d+|_days\\d+)$");

	for (String id : formEntriesID) {
		Matcher matcher = pattern.matcher(id);
		if (matcher.find()) {
			variablePartsSet.add(matcher.group(1));
		}
	}
	return new ArrayList<>(variablePartsSet);
}
private static List<String> extractUniqueDayValues(List<String> formEntriesID) {
	Set<String> dayValuesSet = new HashSet<>();
	Pattern pattern = Pattern.compile("(day\\d+|days\\d+|_day\\d+|_days\\d+)$");

	for (String id : formEntriesID) {
		Matcher matcher1 = pattern.matcher(id);
		if (matcher1.find()) {
			dayValuesSet.add(matcher1.group());
		}
	}
	return new ArrayList<>(dayValuesSet);
}

public static String extractDay(String id) {
	Pattern pattern = Pattern.compile("_day\\d+$");
	Matcher matcher = pattern.matcher(id);
	if (matcher.find()) {
		return matcher.group();
	}
	return null;
}
 

    private static Map<String, String> matchFieldsWithCaptions(Set<String> fieldIDWithoutDaySuffix,
            List<CampaignFormElement> formElements) {
        Map<String, String> fieldIdToCaptionMap = new HashMap<>();
        for (CampaignFormElement element : formElements) {
            String fieldId = element.getId();
            if (fieldIDWithoutDaySuffix.contains(fieldId)) {
                fieldIdToCaptionMap.put(fieldId, element.getCaption());
            }
        }
        return fieldIdToCaptionMap;
    }
    
	public static StreamResource createTransposedDataFormExpressions(CampaignFormDataCriteria criteria) {
	return new StreamResource(criteria.getCampaignFormMeta().getCaption() + ".csv", () -> {
		try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
				OutputStreamWriter writer = new OutputStreamWriter(byteStream, StandardCharsets.UTF_8)) {
			writer.write("\uFEFF");
			System.out.println(criteria.getCampaignFormMeta().getUuid()
					+ "criteria.getCampaignFormMeta().getUuid(criteria.getCampaignFormMeta().getUuid(");
			List<CampaignFormMetaIndexDto> data = FacadeProvider.getCampaignFormMetaFacade()
					.getFormExpressions(criteria.getCampaignFormMeta().getUuid());
			writer.write("Variable Name,Format,Variable Caption,Description\n");
			for (CampaignFormMetaIndexDto dto : data) {
				writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\"\n", escapeCsv(dto.getFieldid()),
						escapeCsv(dto.getFieldtype()), escapeCsv(dto.getFieldcaption()),
						escapeCsv(dto.getFieldexpression())));
			}
			writer.flush();
			return new ByteArrayInputStream(byteStream.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	});
}
private static String escapeCsv(String value) {
	if (value == null) {
		return "";
	}
	String escaped = value.replace("\"", "\"\"");
	if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
		return "\"" + escaped + "\"";
	}
	return escaped;
}
}
 