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

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
 
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.symeda.sormas.api.campaign.data.CampaignFormDataEntry;
import de.symeda.sormas.api.campaign.data.PlatformEnum;
import de.symeda.sormas.api.campaign.form.CampaignFormElement;
import de.symeda.sormas.api.campaign.form.CampaignFormElementType;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaGeographyLevel;
import de.symeda.sormas.api.utils.ValidationException;
import de.symeda.sormas.app.BaseActivity;
import de.symeda.sormas.app.BaseEditActivity;
import de.symeda.sormas.app.BaseEditFragment;
import de.symeda.sormas.app.R;
import de.symeda.sormas.app.backend.campaign.Campaign;
import de.symeda.sormas.app.backend.campaign.data.CampaignFormData;
import de.symeda.sormas.app.backend.campaign.data.CampaignFormDataCriteria;
import de.symeda.sormas.app.backend.campaign.form.CampaignFormMeta;
import de.symeda.sormas.app.backend.common.DaoException;
import de.symeda.sormas.app.backend.common.DatabaseHelper;
import de.symeda.sormas.app.campaign.CampaignFormDataFragmentUtils;
import de.symeda.sormas.app.component.controls.ControlPropertyField;
import de.symeda.sormas.app.component.menu.PageMenuItem;
import de.symeda.sormas.app.component.validation.FragmentValidator;
import de.symeda.sormas.app.core.async.AsyncTaskResult;
import de.symeda.sormas.app.core.async.SavingAsyncTask;
import de.symeda.sormas.app.core.async.TaskResultHolder;
import de.symeda.sormas.app.core.notification.NotificationHelper;
import de.symeda.sormas.app.util.ErrorReportingHelper;

import static de.symeda.sormas.app.campaign.CampaignFormDataFragmentUtils.createControlTextEditField;
import static de.symeda.sormas.app.campaign.CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression;
import static de.symeda.sormas.app.campaign.CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly;
import static de.symeda.sormas.app.core.notification.NotificationType.ERROR;
import static de.symeda.sormas.app.core.notification.NotificationType.WARNING;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class CampaignFormDataEditActivity extends BaseEditActivity<CampaignFormData> {

    private AsyncTask saveTask;
    private Campaign campaign;
    private CampaignFormMeta campaignFormMeta;
    private CampaignFormMeta campaignFormMetaX;
    private CampaignFormDataCriteria criteria = new CampaignFormDataCriteria();
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    List<String> preCampaignsCategories = List.of("FLW", "MODALITY_PRE", "TRAINING", "MONITORING");
    List<String> intraCampaignsCategories = List.of("ICM", "ADMIN", "EAG-ICM", "EAG-ADMIN");
    List<String> postCampaignsCategories = List.of("PCA", "FMS", "LQAS", "EAG-PCA", "EAG-FMS", "EAG-LQAS", "MODALITY_POST", "VALIDATION");

    private Locale currentLocale;

    public static void startActivity(Context context, String rootUuid) {
        try {

            BaseActivity.startActivity(context, CampaignFormDataEditActivity.class, buildBundle(rootUuid));

        } catch(Exception e ){
            System.out.println("eDIT STARTACTIVITY Fragment Error Logged--------------------");

            ErrorReportingHelper.logAndStoreDeviceError( "Edit Form : " + e.getMessage(), e); // replaced sendCaughtException
        }
    }

//

    @Override
    protected CampaignFormData queryRootEntity(String recordUuid) {
        return DatabaseHelper.getCampaignFormDataDao().queryUuidWithEmbedded(recordUuid);
    }

    @Override
    protected CampaignFormData buildRootEntity() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected BaseEditFragment buildEditFragment(PageMenuItem menuItem, CampaignFormData activityRootData) {
        return CampaignFormDataEditFragment.newInstance(activityRootData);
    }

    @Override
    public void saveData() {

        if (saveTask != null) {
            NotificationHelper.showNotification(this, WARNING, getString(R.string.message_already_saving));
            return; // don't save multiple times
        }

        boolean saveChecker = true;

        final CampaignFormData campaignFormDataToSave = getStoredRootEntity();

        campaign = DatabaseHelper.getCampaignDao().queryUuid(campaignFormDataToSave.getCampaign().getUuid());
        campaignFormMeta = DatabaseHelper.getCampaignFormMetaDao().queryUuid(campaignFormDataToSave.getCampaignFormMeta().getUuid());
        campaignFormDataToSave.setFormCategory(campaignFormDataToSave.getCampaignFormMeta().getFormCategory());
        if(!campaignFormDataToSave.getIsvalidated()){
            campaignFormDataToSave.setIsvalidated(false);
        }else{
//            campaignFormDataToSave.setIsvalidated(true);
        }

        final CampaignFormData campaignFormDataToSaveX = getStoredRootEntity();

//        campaign = DatabaseHelper.getCampaignDao().queryUuid(campaignFormDataToSave.getCampaign().getUuid());
//        campaignFormMeta = DatabaseHelper.getCampaignFormMetaDao().queryUuid(campaignFormDataToSave.getCampaignFormMeta().getUuid());

        System.out.println(campaignFormDataToSave.getCampaignFormMeta().getFormCategory()+">>>>>edit>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>__");
        //true is returned when the form is yet to be synchronized with the server, so we only increment teh record version when
        //this form has been subimmted and synchronized with server
        //in return none synced changes wouldn't increment record version
//        if(!campaignFormDataToSave.isModifiedOrChildModified()){
//            campaignFormDataToSave.setRecordversion(campaignFormDataToSave.getRecordversion() + 1L);
//        }
//        campaignFormDataToSave.setFormCategory(campaignFormDataToSave.getCampaignFormMeta().getFormCategory());

        CampaignFormDataFragmentUtils.recalculateAllExpressions(
                expressionParser,
                campaignFormDataToSave.getFormValues(),
                campaignFormMeta.getCampaignFormElements(),
                3
        );



//        try {
//            FragmentValidator.validate(getContext(), getActiveFragment().getContentBinding());
//        } catch (ValidationException e) {
//            NotificationHelper.showNotification(this, ERROR, e.getMessage());
//            return;
//        }

        CampaignFormDataEditFragment fragment =
                (CampaignFormDataEditFragment) getActiveFragment();

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
                } else {
                    cleanedFormValues.add(entry);
                }
            }
            formValues = cleanedFormValues;
        } else {
            formValues = campaignFormDataToSave.getFormValues();
        }
        final List<CampaignFormDataEntry> filledFormValues = new ArrayList<>();


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
        for(CampaignFormDataEntry campaignFormDataEntry : formValues) {
            if (campaignFormDataEntry.getId() != null && campaignFormDataEntry.getValue() != null) {
                filledFormValues.add(campaignFormDataEntry);
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

        campaignFormDataToSave.setFormValues(filledFormValues);
        campaignFormDataToSave.setSoruce(PlatformEnum.MOBILE);


        if (preCampaignsCategories.contains(campaignFormDataToSave.getCampaignFormMeta().getFormCategory()) ||
                intraCampaignsCategories.contains(campaignFormDataToSave.getCampaignFormMeta().getFormCategory())) {
            campaignFormDataToSave.setIsverified(true);
            campaignFormDataToSave.setIspublished(true);
        } else if (postCampaignsCategories.contains(campaignFormDataToSave.getCampaignFormMeta().getFormCategory())) {
            if(campaignFormDataToSave.getIsverified()) {
                campaignFormDataToSave.setIsverified(false);
            }
            if(campaignFormDataToSave.getIspublished()) {
                campaignFormDataToSave.setIspublished(false);
            }
        }


        if(campaignFormDataToSave.getFormDate() == null){
            saveChecker = false;
        }else{
            String geographyLevel = campaignFormMeta.getGeographylevel();
            if (CampaignFormMetaGeographyLevel.REGION.toString().equals(geographyLevel)) {
                if (campaignFormDataToSave.getArea() == null) {
                    saveChecker = false;
                }
            } else if (CampaignFormMetaGeographyLevel.PROVINCE.toString().equals(geographyLevel)) {
                if (campaignFormDataToSave.getRegion() == null) {
                    saveChecker = false;
                }
            } else if (CampaignFormMetaGeographyLevel.DISTRICT.toString().equals(geographyLevel)) {
                if (campaignFormDataToSave.getDistrict() == null) {
                    saveChecker = false;
                }
            } else {
                if (campaignFormDataToSave.getCommunity() == null) {
                    saveChecker = false;
                }
            }
        }

        if (saveChecker) {
            saveTask = new SavingAsyncTask(getRootView(), campaignFormDataToSave) {

                @Override
                public void doInBackground(TaskResultHolder resultHolder) throws DaoException {

                    if(!campaignFormDataToSave.isModifiedOrChildModified()){
                        campaignFormDataToSave.setRecordversion(campaignFormDataToSave.getRecordversion()== null ? 1l  : campaignFormDataToSave.getRecordversion() + 1L);
                    }

//                campaignFormDataToSave.setRecordversion(campaignFormDataToSave.getRecordversion() == null ? 1L : campaignFormDataToSave.getRecordversion());

                    DatabaseHelper.getCampaignFormDataDao().saveAndSnapshot(campaignFormDataToSave);
                }

                @Override
                protected void onPostExecute(AsyncTaskResult<TaskResultHolder> taskResult) {
                    super.onPostExecute(taskResult);

                    if (taskResult.getResultStatus().isSuccess()) {
                        Intent intent = new Intent();
                        intent.setAction("REFRESH_ROW_COUNT");
                        sendBroadcast(intent);

                        finish();
                    } else {
                        //   onResume(); // reload data
                    }
                    saveTask = null;
                }
            }.executeOnThreadPool();

        }else {
            if(campaignFormDataToSave.getFormDate() == null){
                NotificationHelper.showNotification(this, ERROR, "Form Date cannot be left Empty.");
            }else if(campaignFormDataToSave.getCommunity() == null){
                String geoLevel = campaignFormMeta.getGeographylevel();
                switch (geoLevel == null ? "CLUSTER" : geoLevel) {
                    case "REGION":
                        if (campaignFormDataToSave.getArea() == null) {
                            NotificationHelper.showNotification(this, ERROR, "Region cannot be left Empty. Please select a region to proceed.");
                        }
                        break;
                    case "PROVINCE":
                        if (campaignFormDataToSave.getRegion() == null) {
                            NotificationHelper.showNotification(this, ERROR, "Province cannot be left Empty. Please select a province to proceed.");
                        }
                        break;
                    case "DISTRICT":
                        if (campaignFormDataToSave.getDistrict() == null) {
                            NotificationHelper.showNotification(this, ERROR, "District cannot be left Empty. Please select a district to proceed.");
                        }
                        break;
                    case "CLUSTER":
                    default:
                        if (campaignFormDataToSave.getCommunity() == null) {
                            NotificationHelper.showNotification(this, ERROR, "Cluster cannot be left Empty. Please select a cluster to proceed.");
                        }
                        break;
                }
            }

            }
    }

    // Inside CampaignFormDataEditActivity.java

    @Override
    protected void onResume() {
        super.onResume();
        CampaignFormData campaignFormData = getStoredRootEntity();
        if (campaignFormData != null) {
            campaign = DatabaseHelper.getCampaignDao().queryUuid(campaignFormData.getCampaign().getUuid());
            campaignFormMeta = DatabaseHelper.getCampaignFormMetaDao().queryUuid(campaignFormData.getCampaignFormMeta().getUuid());
            setSetSubHeadingTitleForCampaignAndFormName(campaign, campaignFormMeta);
        }
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
                    "EEE MMM dd HH:mm:ss z yyyy",   // e.g., Wed Jun 25 10:30:00 GMT 2025
                    "EEE MMM dd HH:mm:ss zzz yyyy"        // slight variation
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
        return R.string.heading_campaign_form_data_edit;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (saveTask != null && !saveTask.isCancelled())
            saveTask.cancel(true);
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


    private void setSetSubHeadingTitleForCampaignAndFormName(Campaign campaign, CampaignFormMeta campaignFormMeta) {
        setSubHeadingTitle(campaign != null ?  campaignFormMeta != null ? campaign.getName() + " | " + campaignFormMeta.getFormName() : "" :  "");
    }

}
