/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2020 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.symeda.sormas.app.campaign.edit;

import static android.view.View.GONE;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;

import java.util.Calendar;
 import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.symeda.sormas.api.campaign.data.CampaignFormDataDto;
import de.symeda.sormas.api.campaign.data.CampaignFormDataEntry;
import de.symeda.sormas.api.campaign.data.CampaignFormDataIndexDto;
import de.symeda.sormas.api.campaign.form.CampaignFormElement;
import de.symeda.sormas.api.campaign.form.CampaignFormElementType;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaGeographyLevel;
import de.symeda.sormas.api.user.UserRole;
import de.symeda.sormas.api.utils.ValidationException;
import de.symeda.sormas.app.BaseEditActivity;
import de.symeda.sormas.app.BaseEditFragment;
import de.symeda.sormas.app.R;
import de.symeda.sormas.app.backend.campaign.Campaign;
import de.symeda.sormas.app.backend.campaign.data.CampaignFormData;
import de.symeda.sormas.app.backend.campaign.data.CampaignFormDataCriteria;
import de.symeda.sormas.app.backend.campaign.form.CampaignFormMeta;
import de.symeda.sormas.app.backend.common.DaoException;
import de.symeda.sormas.app.backend.common.DatabaseHelper;
import de.symeda.sormas.app.backend.config.ConfigProvider;

import de.symeda.sormas.app.campaign.CampaignFormDataFragmentUtils;
import de.symeda.sormas.app.component.Item;
import de.symeda.sormas.app.component.controls.ControlPropertyField;
import de.symeda.sormas.app.component.menu.PageMenuItem;
import de.symeda.sormas.app.component.validation.FragmentValidator;
import de.symeda.sormas.app.core.async.AsyncTaskResult;
import de.symeda.sormas.app.core.async.SavingAsyncTask;
import de.symeda.sormas.app.core.async.TaskResultHolder;
import de.symeda.sormas.app.core.notification.NotificationHelper;
import de.symeda.sormas.app.util.Bundler;

import static de.symeda.sormas.app.backend.common.DatabaseHelper.getString;
import static de.symeda.sormas.app.campaign.CampaignFormDataFragmentUtils.createControlTextEditField;
import static de.symeda.sormas.app.campaign.CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression;
import static de.symeda.sormas.app.campaign.CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly;
import static de.symeda.sormas.app.core.notification.NotificationType.ERROR;
import static de.symeda.sormas.app.core.notification.NotificationType.WARNING;
import static de.symeda.sormas.app.util.DataUtils.toItems;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class CampaignFormDataNewActivity extends BaseEditActivity<CampaignFormData> {

    private AsyncTask saveTask;
    private Campaign campaign;
    private CampaignFormMeta campaignFormMetaX;
    private CampaignFormMeta campaignFormMeta;
    private Locale currentLocale;

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    private CampaignFormDataCriteria criteria = new CampaignFormDataCriteria();

    List<String> preCampaignsCategories = List.of("FLW", "MODALITY_PRE", "TRAINING", "MONITORING");
    List<String> intraCampaignsCategories = List.of("ICM", "ADMIN", "EAG-ICM", "EAG-ADMIN");
    List<String> postCampaignsCategories = List.of("PCA", "FMS", "LQAS", "EAG-PCA", "EAG-FMS", "EAG-LQAS", "MODALITY_POST", "VALIDATION");
    LocalDate minDate = null;
    LocalDate maxDate = null;


    public static void startActivity(Context context, String campaignUUID, String campaignFormMetaUUID) {
        BaseEditActivity.startActivity(context, CampaignFormDataNewActivity.class,
                BaseEditActivity.buildBundle(null).setCampaignUuid(campaignUUID).setCampaignFormMetaUuid(campaignFormMetaUUID));
    }

    @Override
    public void onCreateInner(@Nullable Bundle savedInstanceState) {
        super.onCreateInner(savedInstanceState);
        campaign = DatabaseHelper.getCampaignDao().queryUuid(new Bundler(savedInstanceState).getCampaignUuid());
        campaignFormMeta = DatabaseHelper.getCampaignFormMetaDao().queryUuid(new Bundler(savedInstanceState).getCampaignFormMetaUuid());
        campaignFormMetaX = DatabaseHelper.getCampaignFormMetaDao().queryUuid(new Bundler(savedInstanceState).getCampaignFormMetaUuid());
    }

    @Override
    protected CampaignFormData queryRootEntity(String recordUuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected CampaignFormData buildRootEntity() {
        CampaignFormData campaignFormData = DatabaseHelper.getCampaignFormDataDao().build();
        return campaignFormData;
    }

    @Override
    protected BaseEditFragment buildEditFragment(PageMenuItem menuItem, CampaignFormData activityRootData) {
        activityRootData.setCampaign(campaign);
        activityRootData.setCampaignFormMeta(campaignFormMeta);
        BaseEditFragment campaignFormDataNewFragment = CampaignFormDataNewFragment.newInstance(activityRootData);
        campaignFormDataNewFragment.setLiveValidationDisabled(true);
        return campaignFormDataNewFragment;
    }




    @Override
    public void saveData() {
        if (saveTask != null) {
            NotificationHelper.showNotification(this, WARNING, getString(R.string.message_already_saving));
            return; // don't save multiple times
        }

        final CampaignFormData campaignFormDataToSaveX = getStoredRootEntity();


        final CampaignFormData campaignFormDataToSave = getStoredRootEntity();
        boolean saveChecker = true;
        criteria.setCampaign(campaign);
        criteria.setCampaignFormMeta(campaignFormMeta);
        criteria.setCommunity(null);
        List<CampaignFormData> lotchecker = DatabaseHelper.getCampaignFormDataDao().queryByCriteria(criteria, 0, 100);

        CampaignFormDataFragmentUtils.recalculateAllExpressions(
                expressionParser,
                campaignFormDataToSave.getFormValues(),
                campaignFormMeta.getCampaignFormElements(),
                3
        );

        if(!ConfigProvider.getUser().getUserRoles().contains(UserRole.SURVEILLANCE_OFFICER)) { // District Officer
            criteria.setCommunity(campaignFormDataToSave.getCommunity());
        }
        campaignFormDataToSave.setFormCategory(campaignFormDataToSave.getCampaignFormMeta().getFormCategory());
        campaignFormDataToSave.setIsvalidated(false);

        CampaignFormDataNewFragment fragment = (CampaignFormDataNewFragment) getActiveFragment();

        try {
            fragment.validateForSave(getContext());
        } catch (ValidationException e) {
            NotificationHelper.showNotification(this, ERROR, e.getMessage());
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            currentLocale = getResources().getConfiguration().getLocales().get(0);
        } else {
            currentLocale = getResources().getConfiguration().locale;
        }
        String language = currentLocale.getLanguage();

        List<CampaignFormDataEntry> cleanedFormValues = new ArrayList<>(campaignFormDataToSave.getFormValues().size());
        List<CampaignFormDataEntry> formValues = new ArrayList<>();
        if (!language.equalsIgnoreCase("en")) {

            for (CampaignFormDataEntry entry : campaignFormDataToSave.getFormValues()) {
                if ("time".equalsIgnoreCase(entry.getId())) {
                    String convertedTime = convertToEnglishNumbers(String.valueOf(entry.getValue()));
                    if (!convertedTime.equals(entry.getValue())) {
                        CampaignFormDataEntry timeEntry = new CampaignFormDataEntry();
                        timeEntry.setId(entry.getId());
                        timeEntry.setValue(convertedTime);
                        cleanedFormValues.add(timeEntry);
                    } else {
                        cleanedFormValues.add(entry);
                    }
                } else if ("date".equalsIgnoreCase(entry.getId())) {
                    String convertedDate = convertToEnglishNumbers(String.valueOf(entry.getValue()));
                    if (!convertedDate.equals(entry.getValue())) {
                        CampaignFormDataEntry timeEntry = new CampaignFormDataEntry();
                        timeEntry.setId(entry.getId());
                        timeEntry.setValue(convertedDate);
                        cleanedFormValues.add(timeEntry);
                    } else {
                        cleanedFormValues.add(entry);
                    }
                } else {
                    cleanedFormValues.add(entry);
                }
            }
            formValues = cleanedFormValues;
        } else {
            formValues = campaignFormDataToSave.getFormValues();
        }        final List<CampaignFormDataEntry> filledFormValues = new ArrayList<>();

        CampaignFormDataEntry lotNo = new CampaignFormDataEntry();
        CampaignFormDataEntry lotClusterNo = new CampaignFormDataEntry();

//        formValues.forEach(campaignFormDataEntry ->
        for(CampaignFormDataEntry campaignFormDataEntry : formValues) {
            if (campaignFormDataEntry.getId() != null && campaignFormDataEntry.getValue() != null) {

                Object rawValue = campaignFormDataEntry.getValue();
                if (rawValue instanceof Map || rawValue instanceof List) {
                    filledFormValues.add(campaignFormDataEntry);
                    continue;
                }


                String value = rawValue.toString();

//                String value = campaignFormDataEntry.getValue().toString();

                System.out.println(campaignFormDataEntry.getId() + "Village code Value detected -------" + value);

                if(campaignFormDataEntry.getId().toString().equalsIgnoreCase("villageCode")
               || campaignFormDataEntry.getId().toString().equalsIgnoreCase("tazkiraNo")
               || campaignFormDataEntry.getId().toString().equalsIgnoreCase("phone")){
                   System.out.println("Village code Value detected -------");
               }else{
                   try {
                       double num = Double.parseDouble(value);
                       if (num == Math.floor(num)) { // means it's a whole number
                           value = String.valueOf((int) num); // convert to int string
                       } else {
                           value = String.valueOf(num); // keep original decimal
                       }
                   } catch (NumberFormatException e) {
                       // not a number, leave value as is
                   }
               }

                System.out.println(campaignFormDataEntry.getId() + "before setting v Village code Value detected -------" + value);

                campaignFormDataEntry.setValue(value);
                filledFormValues.add(campaignFormDataEntry);
                if (campaignFormDataEntry.getId().equalsIgnoreCase("LotNo")) {
                    lotNo = campaignFormDataEntry;
                }
                if (campaignFormDataEntry.getId().equalsIgnoreCase("LotClusterNo")) {
                    lotClusterNo = campaignFormDataEntry;
                }
            }
        };

        for (CampaignFormElement campaignFormElement : campaignFormMeta.getCampaignFormElements()) {
                if (CampaignFormElementType.fromString(campaignFormElement.getType()) == CampaignFormElementType.DATE) {
                    String idtoBeUpdated = campaignFormElement.getId();
                    for(CampaignFormDataEntry campaignFormDataEntry : formValues) {
                        if (idtoBeUpdated.equalsIgnoreCase(campaignFormDataEntry.getId())) {
                            campaignFormDataEntry.setValue(dateFormatterLongAndMobile(campaignFormDataEntry.getValue()));;
                        }
                    }

                }
        }

        if (campaignFormDataToSave.getFormDate() != null) {
            Date date = campaignFormDataToSave.getFormDate();

            Calendar cal = Calendar.getInstance();

            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int minute = cal.get(Calendar.MINUTE);
            int second = cal.get(Calendar.SECOND);
            int milli = cal.get(Calendar.MILLISECOND);

// now apply that to your date
            cal.setTime(date);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, second);
            cal.set(Calendar.MILLISECOND, milli);

            campaignFormDataToSave.setFormDate(cal.getTime());
        }

        List<String> listLotNo = new ArrayList();
        List<String> listLotClusterNo = new ArrayList();

        if (lotchecker.size() > 0) {
            for (CampaignFormData campaignFormDataData : lotchecker) {
                List<CampaignFormDataEntry> lotOwnSec = campaignFormDataData.getFormValues();
                if (lotOwnSec.contains(lotNo)) {
                    listLotNo.add(lotOwnSec.get(lotOwnSec.indexOf(lotNo)).getValue().toString());
                }

                if (lotOwnSec.contains(lotClusterNo) && lotOwnSec.contains(lotNo)) {
                    listLotClusterNo.add(lotOwnSec.get(lotOwnSec.indexOf(lotClusterNo)).getValue().toString());
                }
            }
        }

        for (String string : listLotClusterNo) {
            if (listLotNo.size() > 0) {
            if ((Long.parseLong(string) - Long.parseLong(lotClusterNo.getValue().toString()) == 0)
                        && (Long.parseLong(listLotNo.get(0))
                        - Long.parseLong(lotNo.getValue().toString()) == 0)
            ) {
                saveChecker = false;
//                generateErroMessageByLevel(0);
                NotificationHelper.showNotification(this, ERROR, generateErroMessageByLevel(7));

                break;
            }
            }
        }


        if (preCampaignsCategories.contains(campaignFormDataToSave.getCampaignFormMeta().getFormCategory()) ||
                intraCampaignsCategories.contains(campaignFormDataToSave.getCampaignFormMeta().getFormCategory())) {
            campaignFormDataToSave.setIsverified(true);
            campaignFormDataToSave.setIspublished(true);
        } else if (postCampaignsCategories.contains(campaignFormDataToSave.getCampaignFormMeta().getFormCategory())) {
            campaignFormDataToSave.setIsverified(false);
            campaignFormDataToSave.setIspublished(false);
        }



        campaignFormDataToSave.setFormValues(filledFormValues);


        CampaignFormDataNewFragment activeFragment = (CampaignFormDataNewFragment) getActiveFragment();
        activeFragment.setLiveValidationDisabled(false);

        if(campaignFormDataToSave.getFormDate() == null){
            saveChecker = false;
        }else{
            String geographyLevel = campaignFormMeta.getGeographylevel();
            if (CampaignFormMetaGeographyLevel.REGION.toString().equals(geographyLevel)) {
                if (campaignFormDataToSave.getArea() == null) {
                    saveChecker = false;
//                    generateErroMessageByLevel(2);

                }
            } else if (CampaignFormMetaGeographyLevel.PROVINCE.toString().equals(geographyLevel)) {
                if (campaignFormDataToSave.getRegion() == null) {
                    saveChecker = false;
//                    generateErroMessageByLevel(3);

                }
            } else if (CampaignFormMetaGeographyLevel.DISTRICT.toString().equals(geographyLevel)) {
                if (campaignFormDataToSave.getDistrict() == null) {
                    saveChecker = false;
//                    generateErroMessageByLevel(4);

                }
            } else {
                if (campaignFormDataToSave.getCommunity() == null) {
                    saveChecker = false;
//                    generateErroMessageByLevel(5);

                }
            }

            if(campaignFormDataToSave.getFormDate() == null) {
                try {
                    Date expiryDate = DatabaseHelper.getCampaignFormMetaWithExpDao().getCampaignFormExpiryDateByCampaignIdAndFormId(campaignFormDataToSave.getCampaign().getUuid(), campaignFormDataToSave.getCampaignFormMeta().getUuid());

                    if (preCampaignsCategories.contains(campaignFormDataToSave.getCampaignFormMeta().getFormCategory())) {
                        minDate = campaign.getPreCampStartDate().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                    } else if (intraCampaignsCategories.contains(campaignFormDataToSave.getCampaignFormMeta().getFormCategory())) {
                        minDate = campaign.getStartDate().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                    } else if (postCampaignsCategories.contains(campaignFormDataToSave.getCampaignFormMeta().getFormCategory())) {
                        minDate = campaign.getPostCampStartDate().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                    }

                    maxDate = expiryDate.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();

                    LocalDate formDate = campaignFormDataToSave.getFormDate().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();


                    FragmentValidator.validate(getContext(), getActiveFragment().getContentBinding(), minDate, maxDate, formDate);
                }catch (ValidationException e) {
//                    System.out.println("About to fetch validation error from activity ------");
//                    System.out.println(" meaage validation error from activity ------" + e.getMessage());

                    NotificationHelper.showNotification(this, ERROR, e.getMessage());
                        return;
                }
            }
        }



        if (saveChecker) {
            saveTask = new SavingAsyncTask(getRootView(), campaignFormDataToSave) {

                @Override
                public void doInBackground(TaskResultHolder resultHolder) throws DaoException {
                	campaignFormDataToSave.setRecordversion(campaignFormDataToSave.getRecordversion() == null ? 1L : campaignFormDataToSave.getRecordversion());
//
//                    DatabaseHelper.getCampaignFormDataDao().saveAndSnapshot(campaignFormDataToSave);
                  try{
                  DatabaseHelper.getCampaignFormDataDao().saveAndSnapshot(campaignFormDataToSave);
                  } catch (DaoException exx) {
                    Log.e("CampaignFormDataNewActivity", "Failed to save campaign form data------", exx);
                      throw exx;   // rethrow so the async task still reports failure
                  }
                }

                @Override
                protected void onPostExecute(AsyncTaskResult<TaskResultHolder> taskResult) {
                    hidePreloader();
                    super.onPostExecute(taskResult);
                    if (taskResult.getResultStatus().isSuccess()) {
                        finish();
                        CampaignFormDataEditActivity.startActivity(getContext(), campaignFormDataToSave.getUuid());
                    }
                    saveTask = null;
                }
            }.executeOnThreadPool();
        } else {
            if(campaignFormDataToSave.getFormDate() == null){
//                generateErroMessageByLevel(1);
                NotificationHelper.showNotification(this, ERROR, generateErroMessageByLevel(1) );

            }else if(campaignFormDataToSave.getCommunity() == null){
                    String geoLevel = campaignFormDataToSave.getCampaignFormMeta().getGeographylevel();
                    switch (geoLevel == null ? "CLUSTER" : geoLevel) {
                        case "REGION":
                            if (campaignFormDataToSave.getArea() == null) {
                                NotificationHelper.showNotification(this, ERROR, generateErroMessageByLevel(2));
                            }
                            break;
                        case "PROVINCE":
                            if (campaignFormDataToSave.getRegion() == null) {
                                NotificationHelper.showNotification(this, ERROR, generateErroMessageByLevel(3));
                            }
                            break;
                        case "DISTRICT":
                            if (campaignFormDataToSave.getDistrict() == null) {
                                NotificationHelper.showNotification(this, ERROR, generateErroMessageByLevel(4));
                            }
                            break;
                        case "CLUSTER":
                        default:
                            if (campaignFormDataToSave.getCommunity() == null) {
                                NotificationHelper.showNotification(this, ERROR, generateErroMessageByLevel(5));
                            }
                            break;
                    }

            }else if(campaignFormDataToSave.getFormDate() != null ){
                LocalDate formDatetoCheck = campaignFormDataToSave.getFormDate().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                if ((minDate != null && formDatetoCheck.isBefore(minDate)) ||
                        (maxDate != null && formDatetoCheck.isAfter(maxDate))) {
                    // Handle the case when date is outside the allowed range

                    NotificationHelper.showNotification(this, ERROR, generateErroMessageByLevel(6));
                }

            } else{
                NotificationHelper.showNotification(this, WARNING, generateErroMessageByLevel(7));
            }
            return;
        }
    }

    public String generateErroMessageByLevel(int level) {
        String message = "";
        switch (level) {
            case 0 :
                return message = "Lot Number Already Exist";
            case 1 :
                return message = "Form Date cannot be left Empty.";
            case 2 :
                return message = "Region cannot be left Empty. Please select a region to proceed.";
            case 3 :
                return message = "Province cannot be left Empty. Please select a province to proceed.";
            case 4 :
                return message = "District cannot be left Empty. Please select a district to proceed.";
            case 5 :
                return message = "Cluster cannot be left Empty. Please select a cluster to proceed.";
            case 6 :
                return message = "Form Date is Outside Validity Period.";
            case 7 :
                return message = "Lot Cluster Number Already Exist for this Lot Number.";
        }
        return message;
    }

    public String dateFormatterLongAndMobile(Object value) {
        if (value == null) return null;

        String dateStr = String.valueOf(value);
        System.out.println("Date in question: " + dateStr);

        // Standard output format
        DateFormat outputFormatter = new SimpleDateFormat("dd-MM-yyyy");

        try {
            // First try parsing with the standard format
            Date parsedDate = outputFormatter.parse(dateStr);
            return outputFormatter.format(parsedDate);
        } catch (ParseException e) {
            // If standard format fails, try other formats
            String[] inputFormats = {
                    "yyyy-MM-dd",                   // e.g., 2025-06-25
                    "MMM dd, yyyy HH:mm:ss a",      // e.g., Jun 25, 2025 10:30:00 AM
                    "MMM d, yyyy HH:mm:ss",         // e.g., Jun 5, 2025 10:30:00
                    "MMM d, yyyy HH:mm:ss a",       // e.g., Jun 5, 2025 10:30:00 AM
                    "dd/MM/yyyy",                   // e.g., 25/06/2025
                    "EEE MMM dd HH:mm:ss z yyyy" ,   // e.g., Wed Jun 25 10:30:00 GMT 2025
                    "EEE MMM dd HH:mm:ss zzz yyyy"

            };

            for (String formatString : inputFormats) {
                try {
                    DateFormat inputFormatter = new SimpleDateFormat(formatString);
                    Date parsedDate = inputFormatter.parse(dateStr);
                    return outputFormatter.format(parsedDate);
                } catch (ParseException e2) {
                    // Continue to next format
                }
            }
        }

        System.out.println("Could not parse date: " + dateStr);
        return dateStr; // Return original if we can't parse it
    }


    @Override
    public Enum getPageStatus() {
        return null;
    }

    @Override
    protected int getActivityTitle() {
        return R.string.heading_campaign_form_data_new;
    }

    public static String convertToEnglishNumbers(String input) {
        if (input == null) return null;
        final char[] persianDigits = {'\u06F0','\u06F1','\u06F2','\u06F3','\u06F4','\u06F5','\u06F6','\u06F7','\u06F8','\u06F9'};
        final char[] arabicDigits = {'\u0660','\u0661','\u0662','\u0663','\u0664','\u0665','\u0666','\u0667','\u0668','\u0669'};
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            boolean found = false;
            for (int j = 0; j < 10; j++) {
                if (ch == persianDigits[j] || ch == arabicDigits[j]) {
                    output.append((char) ('0' + j));
                    found = true;
                    break;
                }
            }
            if (!found) {
                output.append(ch);
            }
        }
        return output.toString();
    }

    public static Object getFormValueById(List<CampaignFormDataEntry> formValues, String id) {
        if (formValues == null || id == null || id.trim().isEmpty()) {
            return null;
        }

        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        for (CampaignFormDataEntry entry : formValues) {
            if (entry != null && entry.getId() != null && entry.getId().equalsIgnoreCase(id)) {
                System.out.println("entry.getValue()entry.getValue()entry.getValue()entry.getValue() " + entry.getValue());
                return entry.getValue();
            }
        }

        return null;
    }

    private ControlPropertyField createControlPropertyFieldFromElement(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations,
            Map<String, String> userHints) {

        if (campaignFormElement == null) {
            return null;
        }

        CampaignFormElementType type = CampaignFormElementType.fromString(campaignFormElement.getType());

        switch (type) {
            case NUMBER:
                return createControlTextEditField(
                        campaignFormElement,
                        context,
                        userTranslations,
                        userHints,
                        false,
                        campaignFormElement.isImportant());

            case DECIMAL:
                return createControlTextEditFieldDecimalExpression(
                        campaignFormElement,
                        context,
                        userTranslations,
                        true,
                        campaignFormElement.isImportant(),
                        campaignFormElement.getErrormessage());

            case RANGE:
                return createControlTextEditFieldRangeOnly(
                        campaignFormElement,
                        context,
                        userTranslations,
                        userHints,
                        false,
                        campaignFormElement.isImportant(),
                        null,
                        null,
                        false,
                        null,
                        new ArrayList<>());

            default:
                return createControlTextEditField(
                        campaignFormElement,
                        context,
                        userTranslations,
                        userHints,
                        true,
                        campaignFormElement.isImportant());
        }
    }

}
