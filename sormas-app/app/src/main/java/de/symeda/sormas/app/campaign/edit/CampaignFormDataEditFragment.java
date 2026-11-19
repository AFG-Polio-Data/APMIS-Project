/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2020 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,re or
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
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

import de.symeda.sormas.api.MapperUtil;
import de.symeda.sormas.api.campaign.data.CampaignFormDataEntry;
import de.symeda.sormas.api.campaign.form.CampaignFormElement;
import de.symeda.sormas.api.campaign.form.CampaignFormElementOptions;
import de.symeda.sormas.api.campaign.form.CampaignFormElementType;
import de.symeda.sormas.api.campaign.form.CampaignFormTranslations;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.user.UserRole;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.ValidationException;
import de.symeda.sormas.app.BaseEditFragment;
import de.symeda.sormas.app.R;
import de.symeda.sormas.app.backend.campaign.Campaign;
import de.symeda.sormas.app.backend.campaign.data.CampaignFormData;
import de.symeda.sormas.app.backend.campaign.data.CampaignFormDataCriteria;
import de.symeda.sormas.app.backend.campaign.form.CampaignFormMeta;
import de.symeda.sormas.app.backend.common.DatabaseHelper;
import de.symeda.sormas.app.backend.config.ConfigProvider;
import de.symeda.sormas.app.backend.region.District;
import de.symeda.sormas.app.backend.region.PopulationData;
import de.symeda.sormas.app.backend.user.User;
import de.symeda.sormas.app.campaign.CampaignFormDataFragmentUtils;
import de.symeda.sormas.app.component.Item;
import de.symeda.sormas.app.component.controls.ControlCheckBoxField;
import de.symeda.sormas.app.component.controls.ControlCheckBoxGroupField;
import de.symeda.sormas.app.component.controls.ControlDateField;
import de.symeda.sormas.app.component.controls.ControlDecimalEditField;
import de.symeda.sormas.app.component.controls.ControlImageEditField;
import de.symeda.sormas.app.component.controls.ControlPhoneField;
import de.symeda.sormas.app.component.controls.ControlPropertyEditField;
import de.symeda.sormas.app.component.controls.ControlPropertyField;
import de.symeda.sormas.app.component.controls.ControlSpinnerField;
import de.symeda.sormas.app.component.controls.ControlSwitchField;
import de.symeda.sormas.app.component.controls.ControlTextBoxField;
import de.symeda.sormas.app.component.controls.ControlTextEditField;
import de.symeda.sormas.app.component.controls.ControlTextEditFieldAllowZeroInput;
import de.symeda.sormas.app.component.controls.ControlTextEditFieldRange;
import de.symeda.sormas.app.component.controls.ControlTimeField;
import de.symeda.sormas.app.component.validation.FragmentValidator;
import de.symeda.sormas.app.component.validation.ValidationErrorInfo;
import de.symeda.sormas.app.databinding.FragmentCampaignDataEditLayoutBinding;
import de.symeda.sormas.app.util.DataUtils;
import de.symeda.sormas.app.util.InfrastructureDaoHelper;
import de.symeda.sormas.app.util.TextViewBindingAdapters;
import de.symeda.sormas.app.util.YesNo;

import static de.symeda.sormas.app.campaign.CampaignFormDataFragmentUtils.createControlMultiSelectCheckBoxEditField;
import static de.symeda.sormas.app.campaign.CampaignFormDataFragmentUtils.createControlCheckBoxField;
import static de.symeda.sormas.app.campaign.CampaignFormDataFragmentUtils.createControlSpinnerFieldEditField;
import static de.symeda.sormas.app.campaign.CampaignFormDataFragmentUtils.createControlDateEditField;
import static de.symeda.sormas.app.campaign.CampaignFormDataFragmentUtils.createControlTextEditField;
import static de.symeda.sormas.app.campaign.CampaignFormDataFragmentUtils.createControlTextEditFieldAllowStartingZero;
import static de.symeda.sormas.app.campaign.CampaignFormDataFragmentUtils.createControlYesNoUnknownField;
import static de.symeda.sormas.app.campaign.CampaignFormDataFragmentUtils.getUserTranslations;
import static de.symeda.sormas.app.campaign.CampaignFormDataFragmentUtils.handleDependingOn;
import static de.symeda.sormas.app.campaign.CampaignFormDataFragmentUtils.handleExpression;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;

public class CampaignFormDataEditFragment extends BaseEditFragment<FragmentCampaignDataEditLayoutBinding, CampaignFormData, CampaignFormData> {

    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private CampaignFormData record;
    private List<Item> initialCampaigns;
    private List<Item> initialAreas;
    private List<Item> initialRegions;
    private List<Item> initialDistricts;
    private List<Item> initialCommunities;
    private Map<String, String> optionsValues = null;
    private List<String> constraints;
    private boolean onError;
    private String errorMessage = "";

    private String caption_1 = "";
    private String caption_2 = "";
    private String caption_3 = "";
    private String caption_4 = "";
    private String caption_5 = "";
    private String caption_6 = "";
    private String caption_7 = "";
    private String caption_8 = "";

    //private List<CampaignFormTranslations> translationsOpt;
    private Map<String, String> userOptTranslations = null;
    private Campaign campaign;
    //    private CampaignFormMeta campaignFormMeta;
    private CampaignFormDataCriteria criteria = new CampaignFormDataCriteria();
    private String initialLotNo = "";
    private String lotChangedValue = "";
    private String lotClusterNoChangedValue = "";
    private String initialLotClusterNo = "";
    private boolean lotNoChanged = false;
    private boolean lotClusterNoChanged = false;
    private boolean validateChecker = true;

    private String countryCodeHolder = "";
    int doubleLotChecker = 0;

    private SimpleDateFormat dateFormat;

    private static final String STANDARD_DATE_FORMAT = "dd-MM-yyyy HH:mm:ss";
    private SimpleDateFormat standardDateFormat = new SimpleDateFormat(STANDARD_DATE_FORMAT, Locale.getDefault());

    private String currentCountryCode;
    private TextView countryLabel;
    private TextView helperText;

    private int min = 0;
    private int max = 0;
    private String country = "";
    private Map<String, CountryDetails> mapvalue = new HashMap<>();

    boolean isSpinnerInitialized = false;

    private ControlTextEditFieldRange errorSetterGlobal;
    List<String> preCampaignsCategories = List.of("FLW", "MODALITY_PRE", "TRAINING", "MONITORING");
    List<String> intraCampaignsCategories = List.of("ICM", "ADMIN", "EAG-ICM", "EAG-ADMIN");
    List<String> postCampaignsCategories = List.of("PCA", "FMS", "LQAS", "EAG-PCA", "EAG-FMS", "EAG-LQAS", "MODALITY_POST", "VALIDATION");

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private TabHost mTabHost;
    private boolean daywise = false;

    private IntConsumer buildDayFields;
    private int daywiseDayCount;
    private final Set<Integer> builtDays = new HashSet<>();

    public void addMapValue() {

        mapvalue.put("Afghanistan", new CountryDetails("+93", 9, 9));
        mapvalue.put("Albania", new CountryDetails("+355", 8, 9));
        mapvalue.put("Algeria", new CountryDetails("+213", 9, 9));
        mapvalue.put("Andorra", new CountryDetails("+376", 6, 6));
        mapvalue.put("Angola", new CountryDetails("+244", 9, 9));
        mapvalue.put("Argentina", new CountryDetails("+54", 10, 10));
        mapvalue.put("Armenia", new CountryDetails("+374", 8, 8));
        mapvalue.put("Australia", new CountryDetails("+61", 9, 9));
        mapvalue.put("Austria", new CountryDetails("+43", 10, 13));
        mapvalue.put("Azerbaijan", new CountryDetails("+994", 9, 9));
        mapvalue.put("Bahrain", new CountryDetails("+973", 8, 8));
        mapvalue.put("Bangladesh", new CountryDetails("+880", 10, 10));
        mapvalue.put("Belarus", new CountryDetails("+375", 9, 9));
        mapvalue.put("Belgium", new CountryDetails("+32", 8, 9));
        mapvalue.put("Bolivia", new CountryDetails("+591", 8, 8));
        mapvalue.put("Brazil", new CountryDetails("+55", 10, 11));
        mapvalue.put("Canada", new CountryDetails("+1", 10, 10));
        mapvalue.put("China", new CountryDetails("+86", 11, 11));
        mapvalue.put("Colombia", new CountryDetails("+57", 10, 10));
        mapvalue.put("Denmark", new CountryDetails("+45", 8, 8));
        mapvalue.put("Egypt", new CountryDetails("+20", 10, 10));
        mapvalue.put("France", new CountryDetails("+33", 9, 9));
        mapvalue.put("Germany", new CountryDetails("+49", 10, 11));
        mapvalue.put("India", new CountryDetails("+91", 10, 10));
        mapvalue.put("Indonesia", new CountryDetails("+62", 9, 11));
        mapvalue.put("Iran", new CountryDetails("+98", 10, 10));
        mapvalue.put("Iraq", new CountryDetails("+964", 10, 10));
        mapvalue.put("Italy", new CountryDetails("+39", 9, 10));
        mapvalue.put("Japan", new CountryDetails("+81", 10, 10));
        mapvalue.put("Kenya", new CountryDetails("+254", 9, 9));
        mapvalue.put("Mexico", new CountryDetails("+52", 10, 10));
        mapvalue.put("Netherlands", new CountryDetails("+31", 9, 9));
        mapvalue.put("Nigeria", new CountryDetails("+234", 7, 10));
        mapvalue.put("Pakistan", new CountryDetails("+92", 10, 10));
        mapvalue.put("Philippines", new CountryDetails("+63", 10, 10));
        mapvalue.put("Poland", new CountryDetails("+48", 9, 9));
        mapvalue.put("Portugal", new CountryDetails("+351", 9, 9));
        mapvalue.put("Russia", new CountryDetails("+7", 10, 10));
        mapvalue.put("Saudi Arabia", new CountryDetails("+966", 9, 9));
        mapvalue.put("South Africa", new CountryDetails("+27", 9, 9));
        mapvalue.put("South Korea", new CountryDetails("+82", 9, 10));
        mapvalue.put("Spain", new CountryDetails("+34", 9, 9));
        mapvalue.put("Sweden", new CountryDetails("+46", 7, 9));
        mapvalue.put("Switzerland", new CountryDetails("+41", 9, 9));
        mapvalue.put("Thailand", new CountryDetails("+66", 9, 9));
        mapvalue.put("Turkey", new CountryDetails("+90", 10, 10));
        mapvalue.put("Ukraine", new CountryDetails("+380", 9, 9));
        mapvalue.put("United Arab Emirates", new CountryDetails("+971", 9, 9));
        mapvalue.put("United Kingdom", new CountryDetails("+44", 9, 10));
        mapvalue.put("United States", new CountryDetails("+1", 10, 10));
        mapvalue.put("Vietnam", new CountryDetails("+84", 9, 10));
    }

    public static BaseEditFragment newInstance(CampaignFormData activityRootData) {
        return newInstance(CampaignFormDataEditFragment.class, null, activityRootData);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);

        final CampaignFormMeta campaignFormMeta = DatabaseHelper.getCampaignFormMetaDao().queryForId(record.getCampaignFormMeta().getId());

        final Set<String> rangeTriggerIds =
                CampaignFormDataFragmentUtils.getRangeMinMaxTriggerIds(campaignFormMeta.getCampaignFormElements());

        final Set<String> rangeEffectIds =
                CampaignFormDataFragmentUtils.getRangeExpressionEffectIds(
                        campaignFormMeta.getCampaignFormElements(), rangeTriggerIds);

        // Get all hint translation
        Map<String, String> userHints =
                CampaignFormDataFragmentUtils.getUserHints(campaignFormMeta);
        final List<CampaignFormDataEntry> formValues = record.getFormValues();
        final List<CampaignFormTranslations> translationsOpt = record.getCampaignFormMeta().getCampaignFormTranslations();
        campaign = DatabaseHelper.getCampaignDao().queryForId(record.getCampaign().getId());
        criteria.setCampaign(campaign);
        criteria.setCampaignFormMeta(campaignFormMeta);

        System.out.println(criteria.getCampaign().getUuid() + "campaign.getUuid()campaign.getUuid()campaign.getUuid()----------------");
        System.out.println(campaignFormMeta.getUuid() + "campaign.getUuid()campaign.getUuid()campaign.getUuid()----------------");

        Date expiryDate = DatabaseHelper.getCampaignFormMetaWithExpDao().getCampaignFormExpiryDateByCampaignIdAndFormId(campaign.getUuid(), campaignFormMeta.getUuid());
        LocalDate expiryLocalDate = expiryDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();


        final Map<String, String> formValuesMap = new HashMap<>();
        final Map<String, Object> rawFormValuesMap = new HashMap<>();
        formValues.forEach(campaignFormDataEntry -> {
            formValuesMap.put(campaignFormDataEntry.getId(), DataHelper.toStringNullable(campaignFormDataEntry.getValue()));
            rawFormValuesMap.put(campaignFormDataEntry.getId(), campaignFormDataEntry.getValue());
        });

        final Map<String, ControlPropertyField> fieldMap = new HashMap<>();
        final Map<CampaignFormElement, ControlPropertyField> expressionMap = new HashMap<>();

        daywise = false;

        int dayy = 0;
        for (CampaignFormElement campaignFormElement : campaignFormMeta.getCampaignFormElements()) {
            CampaignFormElementType type = CampaignFormElementType.fromString(campaignFormElement.getType());

            if (type == CampaignFormElementType.DAYWISE) {
                daywise = true;
                break;
            }
            if (daywise) {
                break;
            }

        }

        int accrd_count = 0;

        Resources res = getResources();
        mTabHost = (TabHost) view.findViewById(R.id.tabhostxxxxXEd);
        mTabHost.setup();

        TabHost.TabSpec spec;

        builtDays.clear();
        buildDayFields = onlyDay -> {
            int countr = 0;

            for (CampaignFormElement campaignFormElement : campaignFormMeta.getCampaignFormElements()) {
                CampaignFormElementType type = CampaignFormElementType.fromString(campaignFormElement.getType());

                int minx = 0;
                int maxz = 0;
                boolean expressionx = false;


                CampaignFormElementOptions campaignFormElementOptions = new CampaignFormElementOptions();
                if (campaignFormElement.getOptions() != null) {
                    final Locale locale = I18nProperties.getUserLanguage().getLocale();

                    if (locale != null) {
                        translationsOpt.stream().filter(t -> t.getLanguageCode().equals(locale.toString()))
                                .findFirst().ifPresent(filteredTranslations -> filteredTranslations.getTranslations().stream()
                                        .filter(cd -> cd.getOptions() != null)
                                        .findFirst().ifPresent(optionsList -> userOptTranslations = optionsList.getOptions().stream()
                                                .filter(c -> c.getCaption() != null).collect(Collectors.toMap(MapperUtil::getKey, MapperUtil::getCaption))));
                    }

                    optionsValues = campaignFormElement.getOptions().stream().collect(Collectors.toMap(MapperUtil::getKey, MapperUtil::getCaption));  // .collect(Collectors.toList());

                    if (userOptTranslations == null) {
                        campaignFormElementOptions.setOptionsListValues(optionsValues);
                        //get18nOptCaption(formElement.getId(), optionsValues));
                    } else {
                        campaignFormElementOptions.setOptionsListValues(userOptTranslations);

                    }

                } else {
                    optionsValues = new HashMap<String, String>();
                }


                if (campaignFormElement.getConstraints() != null && campaignFormElement.getExpression() == null) {

                    constraints = (List) Arrays.stream(campaignFormElement.getConstraints()).collect(Collectors.toList());
                    ListIterator<String> lstItemsx = constraints.listIterator();
                    int i = 1;
                    while (lstItemsx.hasNext()) {
                        String lss = lstItemsx.next().toString();
                        if (lss.toLowerCase().contains("max")) {
                            maxz = Integer.parseInt(lss.substring(lss.lastIndexOf("=") + 1));
                            //  campaignFormElementOptions.setMax(Integer.parseInt(lss.substring(lss.lastIndexOf("=") + 1)));
                        } else if (lss.toLowerCase().contains("min")) {
                            //  campaignFormElementOptions.setMin(Integer.parseInt(lss.substring(lss.lastIndexOf("=") + 1)));
                            minx = Integer.parseInt(lss.substring(lss.lastIndexOf("=") + 1));
                        }
                    }

                } else if (campaignFormElement.getConstraints() != null && campaignFormElement.getExpression() != null) {

                    expressionx = true;
                }

                //   System.out.println(campaignFormElement.getErrormessage() + ")))))))))))))))))))))(((((((((((((((===");

                errorMessage = campaignFormElement.getErrormessage() != null ? campaignFormElement.getErrormessage() : "";


                onError = campaignFormElement.isWarnonerror();
                Boolean isRangeandExpression = false;
                Boolean isNumberandExpression = false;

                if (daywise) {
                    if (type == CampaignFormElementType.DAYWISE) {
                        countr++;
                    } else if (countr == 1 && onlyDay == 1) {
                        final LinearLayout dynamicLayout = mTabHost.findViewById(R.id.tabSheet1);
                        if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL && type != CampaignFormElementType.LINEBREAK) {
                            String value = formValuesMap.get(campaignFormElement.getId());
    //                        value = value == null ? null : value.endsWith(".0") ?  value.replace(".0", "") : value;
                            if (value != null) {
                                try {
                                    double num = Double.parseDouble(value);
                                    if (num == Math.floor(num)) {
                                        value = String.valueOf((int) num); // whole number, no decimal
                                    } else {
                                        value = String.format("%.2f", num); // round to 2 decimal places
                                    }
                                } catch (NumberFormatException e) {
                                    // value is not a number, leave as-is
                                }
                            }

                            ControlPropertyField dynamicField;
                            boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                            if (type == CampaignFormElementType.YES_NO) {
                                dynamicField = createControlYesNoUnknownField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlSwitchField.setValue((ControlSwitchField) dynamicField, value, true, YesNo.class, null);
                            } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                                dynamicField = createControlCheckBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlCheckBoxField.setValue((ControlCheckBoxField) dynamicField, Boolean.valueOf(value));
                            } else if (type == CampaignFormElementType.CHECKBOXBASIC) {
                                List<String> selectedKeys = new ArrayList<>();
                                if (value != null && !value.trim().isEmpty()) {
                                    String str = value.trim();

                                    // Remove brackets if present
                                    if (str.startsWith("[") && str.endsWith("]")) {
                                        str = str.substring(1, str.length() - 1);
                                    }

                                    // Remove quotes if present
                                    str = str.replace("\"", "");

                                    // Split by comma
                                    String[] parts = str.split("\\s*,\\s*");

                                    for (String part : parts) {
                                        String cleanPart = part.trim();
                                        if (!cleanPart.isEmpty()) {
                                            selectedKeys.add(cleanPart);
                                        }
                                    }
                                }

                                // Create the field
                                dynamicField = createControlMultiSelectCheckBoxEditField(
                                        campaignFormElement,
                                        requireContext(),
                                        getUserTranslations(campaignFormMeta),
                                        optionsValues,
                                        selectedKeys,
                                        campaignFormElement.isImportant()
                                );

                                // Now set the value using the new method
                                if (dynamicField instanceof ControlCheckBoxGroupField) {
                                    ((ControlCheckBoxGroupField) dynamicField).setOptionsAndValue(optionsValues, selectedKeys);
                                }
                            } else if (type == CampaignFormElementType.NUMBER) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints, true, campaignFormElement.isImportant());
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    isNumberandExpression = true;
                                }
                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value);
                            } else if (type == CampaignFormElementType.DECIMAL) {
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints, true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                    isRangeandExpression = true;
                                }
                                ControlDecimalEditField.setValue((ControlDecimalEditField) dynamicField, value);
                            } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.RANGE) {
                                final boolean exprx = expressionx;
                                System.out.println( exprx + " exprxexprxexprxexprxexprx");
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                                } else {
                                     dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage, onError, campaignFormMeta.getCampaignFormElements());
                                    isRangeandExpression = true;
                                }

                                if (value != null) {
                                    try {
                                        double num = Double.parseDouble(value);
                                        if (num == Math.floor(num)) {
                                            value = String.valueOf((int) num); // whole number, no decimal
                                        } else {
                                            value = String.format("%.2f", num); // round to 2 decimal places
                                        }
                                    } catch (NumberFormatException e) {
                                        // value is not a number, leave as-is
                                    }
                                }

                                ControlTextEditFieldRange.setValue((ControlTextEditFieldRange) dynamicField, value);

                            } else if (type == CampaignFormElementType.DROPDOWN) {
                                dynamicField = createControlSpinnerFieldEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,optionsValues, campaignFormElement.isImportant());
                                ControlSpinnerField.setValue((ControlSpinnerField) dynamicField, value);
                            } else if (type == CampaignFormElementType.DATE) {
                                dynamicField = createControlDateEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(), campaignFormElement.isImportant());
                                System.out.println( getDateValue(value) + " getDateValue(value) getDateValue(value) getDateValue(value)" );


                                if (campaign != null) {
                                    Date minDate = null;
                                    Date maxDate = null;

                                    if (preCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getPreCampStartDate();
                                    }else if (intraCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getStartDate();
                                    }else if (postCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getPostCampStartDate();
                                     }
                                    maxDate = expiryDate;


                                    if (minDate != null) {
                                        ((ControlDateField) dynamicField).setMinDate(minDate);                            }
                                    if (maxDate != null) {
                                        ((ControlDateField) dynamicField).setMaxDate(maxDate);
                                    }
                                }

                                ControlDateField.setValue((ControlDateField) dynamicField, getDateValue(value));
                            } else if (type == CampaignFormElementType.TEXTBOX) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlTextBoxField.setValue((ControlTextBoxField) dynamicField, value);
                            }else if (type == CampaignFormElementType.IMAGE) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlImageEditField(
                                        campaignFormElement,
                                        requireContext(),
                                        CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta),
                                        userHints,
                                        campaignFormElement.isImportant(),     // not hardcoded true
                                        cameraLauncher,
                                        cameraPermissionLauncher
                                );
    //                            ((ControlImageEditField) dynamicField).setCampaignFormDataUuid(record.getUuid());
    //                            ((ControlImageEditField) dynamicField).setRecord(record);
                                ControlImageEditField.setValue((ControlImageEditField) dynamicField, rawFormValuesMap.get(campaignFormElement.getId()), record);
                            } else {
                                dynamicField = createControlTextEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value);
                            }

                            fieldMap.put(campaignFormElement.getId(), dynamicField);
                            dynamicField.setShowCaption(true);
                            dynamicLayout.addView(dynamicField, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                            Boolean finalIsRangeandExpression = isRangeandExpression;
                            Boolean finalIsNumberandExpression = isNumberandExpression;
                            final String dependingOnx = campaignFormElement.getDependingOn();

                            Boolean isdependingOn = false;
                            if (dependingOnx != null) {
                                isdependingOn = true;
                            }
                            Boolean finalIsdependingOn = isdependingOn;

                            dynamicField.addValueChangedListener(field -> {
                                baseEditActivity.setDataModified(true);
                                final Boolean isRangeandExpressionx = finalIsRangeandExpression;
                                Boolean okk = field.getFocusedChild() != null ? true : false;
                                final CampaignFormDataEntry campaignFormDataEntry = CampaignFormDataFragmentUtils.getOrCreateCampaignFormDataEntry(formValues, campaignFormElement);
                                campaignFormDataEntry.setValue(field.getValue());
                                if ((campaignFormElement.getExpression() == null && fieldMap.get(campaignFormElement.getId()) != null) || (okk && isRangeandExpressionx)) {
                                    for (CampaignFormDataEntry det : formValues) {
                                        if (det.getValue() != null) {
                                            if (det.getValue().toString().isEmpty()) {
                                                det.setValue(null);
                                            }
                                        }
                                    }
    //                                    expressionMap.forEach((formElement, controlPropertyField) ->
    //                                            CampaignFormDataFragmentUtils.handleExpressionSec(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));

                                    boolean changedFieldIsRangeTrigger =
                                            campaignFormElement != null
                                                    && "range".equalsIgnoreCase(campaignFormElement.getType())
                                                    && rangeEffectIds.contains(campaignFormElement.getId());

                                    boolean userStillEditingThisField = field.hasFocus() || field.getFocusedChild() != null;

                                    // pass 1: evaluate non-range effects immediately
                                    expressionMap.forEach((formElement, controlPropertyField) -> {
                                        if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                        boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());

                                        if (!isRangeEffect) {
                                            CampaignFormDataFragmentUtils.handleExpressionSec(
                                                    expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                    controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                        }
                                    });

                                    // pass 2: evaluate range effects, but defer while typing in range trigger
                                    if (!(changedFieldIsRangeTrigger && userStillEditingThisField)) {
                                        expressionMap.forEach((formElement, controlPropertyField) -> {
                                            if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                            boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());
                                            boolean isDeferredRangeEffect = isRangeEffect && rangeEffectIds.contains(formElement.getId());

                                            if (isRangeEffect) {
                                                // range-only effect side
                                                CampaignFormDataFragmentUtils.handleExpressionSec(
                                                        expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                        controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                            }
                                        });
                                    }
                                } else if (field.isFocused()) {
                                    System.out.println(">>>>>>>>>>>>>>>>>ONFOCUSSS>>>>>>>>>>>>>>>>>>>>" + fieldMap.get(campaignFormElement.getId()).getCaption());

                                }
                                if (finalIsdependingOn && isRangeandExpressionx) {
                                    field.setVisibility(View.GONE);
                                }

                            });

                            final String dependingOn = campaignFormElement.getDependingOn();
                            if (dependingOn != null) {
                                handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);
                            }

                            final String expressionString = campaignFormElement.getExpression();
                            if (expressionString != null) {
                            handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
                                expressionMap.put(campaignFormElement, dynamicField);
                            }
                        } else if (type == CampaignFormElementType.SECTION) {
                            if (campaignFormElement.getDependingOn() == null) {
                                ControlPropertyField dynamicField;
                                dynamicLayout.addView(new ImageView(requireContext(), null, R.style.FullHorizontalDividerStyle));
                            }
                        } else if (type == CampaignFormElementType.LABEL) {
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, CampaignFormDataFragmentUtils.getUserLanguageCaption(CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), campaignFormElement));
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        } else if (type == CampaignFormElementType.LINEBREAK) {
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, "");
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        }
                    } else if (countr == 2 && onlyDay == 2) {
                        final LinearLayout dynamicLayout = mTabHost.findViewById(R.id.tabSheet2);
                        if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL && type != CampaignFormElementType.LINEBREAK) {
                            String value = formValuesMap.get(campaignFormElement.getId());
    //                        value = value == null ? null : value.endsWith(".0") ?  value.replace(".0", "") : value;
                            if (value != null) {
                                try {
                                    double num = Double.parseDouble(value);
                                    if (num == Math.floor(num)) {
                                        value = String.valueOf((int) num); // whole number, no decimal
                                    } else {
                                        value = String.format("%.2f", num); // round to 2 decimal places
                                    }
                                } catch (NumberFormatException e) {
                                    // value is not a number, leave as-is
                                }
                            }
                            ControlPropertyField dynamicField;
                            boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                            if (type == CampaignFormElementType.YES_NO) {
                                dynamicField = createControlYesNoUnknownField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlSwitchField.setValue((ControlSwitchField) dynamicField, value, true, YesNo.class, null);
                            } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                                dynamicField = createControlCheckBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlCheckBoxField.setValue((ControlCheckBoxField) dynamicField, Boolean.valueOf(value));
                            } else if (type == CampaignFormElementType.CHECKBOXBASIC) {
                                List<String> selectedKeys = new ArrayList<>();
                                if (value != null && !value.trim().isEmpty()) {
                                    String str = value.trim();

                                    // Remove brackets if present
                                    if (str.startsWith("[") && str.endsWith("]")) {
                                        str = str.substring(1, str.length() - 1);
                                    }

                                    // Remove quotes if present
                                    str = str.replace("\"", "");

                                    // Split by comma
                                    String[] parts = str.split("\\s*,\\s*");

                                    for (String part : parts) {
                                        String cleanPart = part.trim();
                                        if (!cleanPart.isEmpty()) {
                                            selectedKeys.add(cleanPart);
                                        }
                                    }
                                }

                                // Create the field
                                dynamicField = createControlMultiSelectCheckBoxEditField(
                                        campaignFormElement,
                                        requireContext(),
                                        getUserTranslations(campaignFormMeta),
                                        optionsValues,
                                        selectedKeys,
                                        campaignFormElement.isImportant()
                                );

                                // Now set the value using the new method
                                if (dynamicField instanceof ControlCheckBoxGroupField) {
                                    ((ControlCheckBoxGroupField) dynamicField).setOptionsAndValue(optionsValues, selectedKeys);
                                }
                            } else if (type == CampaignFormElementType.NUMBER) {

                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    isNumberandExpression = true;
                                }
                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value);
                            } else if (type == CampaignFormElementType.DECIMAL) {
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints, true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                    isRangeandExpression = true;
                                }
                                ControlDecimalEditField.setValue((ControlDecimalEditField) dynamicField, value);
                            } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.RANGE) {
                                final boolean exprx = expressionx;

                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                                } else {
                                     dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage, onError, campaignFormMeta.getCampaignFormElements());
                                    isRangeandExpression = true;
                                }

                                if (value != null) {
                                    try {
                                        double num = Double.parseDouble(value);
                                        if (num == Math.floor(num)) {
                                            value = String.valueOf((int) num); // whole number, no decimal
                                        } else {
                                            value = String.format("%.2f", num); // round to 2 decimal places
                                        }
                                    } catch (NumberFormatException e) {
                                        // value is not a number, leave as-is
                                    }
                                }

                                ControlTextEditFieldRange.setValue((ControlTextEditFieldRange) dynamicField, value);

                            } else if (type == CampaignFormElementType.DROPDOWN) {
                                dynamicField = createControlSpinnerFieldEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints, optionsValues, campaignFormElement.isImportant());
                                ControlSpinnerField.setValue((ControlSpinnerField) dynamicField, value);
                            }else if (type == CampaignFormElementType.IMAGE) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlImageEditField(
                                        campaignFormElement,
                                        requireContext(),
                                        CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta),
                                        userHints,
                                        campaignFormElement.isImportant(),     // not hardcoded true
                                        cameraLauncher,
                                        cameraPermissionLauncher
                                );
    //                            ((ControlImageEditField) dynamicField).setCampaignFormDataUuid(record.getUuid());
    //                            ((ControlImageEditField) dynamicField).setRecord(record);
                                ControlImageEditField.setValue((ControlImageEditField) dynamicField, rawFormValuesMap.get(campaignFormElement.getId()), record);
                            } else if (type == CampaignFormElementType.DATE) {
                                dynamicField = createControlDateEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(), campaignFormElement.isImportant());


                                if (campaign != null) {
                                    Date minDate = null;
                                    Date maxDate = null;

                                    String campaignPhase = "";
                                    if (preCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getPreCampStartDate();
                                     }else if (intraCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getStartDate();
                                     }else if (postCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getPostCampStartDate();
                                     }
                                    maxDate = expiryDate;



                                    if (minDate != null) {
                                        ((ControlDateField) dynamicField).setMinDate(minDate);                            }
                                    if (maxDate != null) {
                                        ((ControlDateField) dynamicField).setMaxDate(maxDate);
                                    }
                                }

                                ControlDateField.setValue((ControlDateField) dynamicField, getDateValue(value));
                            }else if (type == CampaignFormElementType.TEXTBOX) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlTextBoxField.setValue((ControlTextBoxField) dynamicField, value);
                            } else {
                                dynamicField = createControlTextEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value);
                            }

                            fieldMap.put(campaignFormElement.getId(), dynamicField);
                            dynamicField.setShowCaption(true);
                            dynamicLayout.addView(dynamicField, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            Boolean finalIsRangeandExpression = isRangeandExpression;
                            Boolean finalIsNumberandExpression = isRangeandExpression;


                            final String dependingOnx = campaignFormElement.getDependingOn();

                            Boolean isdependingOn = false;
                            if (dependingOnx != null) {
                                isdependingOn = true;
                            }
                            Boolean finalIsdependingOn = isdependingOn;

                            dynamicField.addValueChangedListener(field -> {
                                final Boolean isRangeandExpressionx = finalIsRangeandExpression;
                                final Boolean isNumberandExpressionx = finalIsNumberandExpression;

                                Boolean okk = field.getFocusedChild() != null ? true : false;
                                final CampaignFormDataEntry campaignFormDataEntry = CampaignFormDataFragmentUtils.getOrCreateCampaignFormDataEntry(formValues, campaignFormElement);
                                campaignFormDataEntry.setValue(field.getValue());
                                if ((campaignFormElement.getExpression() == null && fieldMap.get(campaignFormElement.getId()) != null) || (okk && isRangeandExpressionx)) {
                                    for (CampaignFormDataEntry det : formValues) {
                                        if (det.getValue() != null) {
                                            if (det.getValue().toString().isEmpty()) {
                                                det.setValue(null);
                                            }
                                        }
                                    }
    //                                expressionMap.forEach((formElement, controlPropertyField) ->
    //                                CampaignFormDataFragmentUtils.handleExpressionSec(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));

                                    boolean changedFieldIsRangeTrigger =
                                            campaignFormElement != null
                                                    && "range".equalsIgnoreCase(campaignFormElement.getType())
                                                    && rangeEffectIds.contains(campaignFormElement.getId());

                                    boolean userStillEditingThisField = field.hasFocus() || field.getFocusedChild() != null;

                                    // pass 1: evaluate non-range effects immediately
                                    expressionMap.forEach((formElement, controlPropertyField) -> {
                                        if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                        boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());

                                        if (!isRangeEffect) {
                                            CampaignFormDataFragmentUtils.handleExpressionSec(
                                                    expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                    controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                        }
                                    });

                                    // pass 2: evaluate range effects, but defer while typing in range trigger
                                    if (!(changedFieldIsRangeTrigger && userStillEditingThisField)) {
                                        expressionMap.forEach((formElement, controlPropertyField) -> {
                                            if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                            boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());
                                            boolean isDeferredRangeEffect = isRangeEffect && rangeEffectIds.contains(formElement.getId());

                                            if (isRangeEffect) {
                                                // range-only effect side
                                                CampaignFormDataFragmentUtils.handleExpressionSec(
                                                        expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                        controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                            }
                                        });
                                    }
                                }
                            });

                            final String dependingOn = campaignFormElement.getDependingOn();
                            if (dependingOn != null) {
    //                            handleDependingOn(fieldMap, campaignFormElement, dynamicField);
                                handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);

                            }

                            final String expressionString = campaignFormElement.getExpression();
                            if (expressionString != null) {
                                handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
                                expressionMap.put(campaignFormElement, dynamicField);
                            }
                        } else if (type == CampaignFormElementType.SECTION) {
                            if (campaignFormElement.getDependingOn() == null) {
                                ControlPropertyField dynamicField;
                                dynamicLayout.addView(new ImageView(requireContext(), null, R.style.FullHorizontalDividerStyle));
                            }
                        } else if (type == CampaignFormElementType.LABEL) {
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, CampaignFormDataFragmentUtils.getUserLanguageCaption(CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), campaignFormElement));
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        } else if (type == CampaignFormElementType.LINEBREAK) {
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, "");
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        }
                    } else if (countr == 3 && onlyDay == 3) {
                        final LinearLayout dynamicLayout = mTabHost.findViewById(R.id.tabSheet3);
                        if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL && type != CampaignFormElementType.LINEBREAK) {
                            String value = formValuesMap.get(campaignFormElement.getId());
    //                        value = value == null ? null : value.endsWith(".0") ?  value.replace(".0", "") : value;
                            if (value != null) {
                                try {
                                    double num = Double.parseDouble(value);
                                    if (num == Math.floor(num)) {
                                        value = String.valueOf((int) num); // whole number, no decimal
                                    } else {
                                        value = String.format("%.2f", num); // round to 2 decimal places
                                    }
                                } catch (NumberFormatException e) {
                                    // value is not a number, leave as-is
                                }
                            }
                            ControlPropertyField dynamicField;
                            boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                            if (type == CampaignFormElementType.YES_NO) {
                                dynamicField = createControlYesNoUnknownField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlSwitchField.setValue((ControlSwitchField) dynamicField, value, true, YesNo.class, null);
                            } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                                dynamicField = createControlCheckBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlCheckBoxField.setValue((ControlCheckBoxField) dynamicField, Boolean.valueOf(value));
                            } else if (type == CampaignFormElementType.CHECKBOXBASIC) {
                                List<String> selectedKeys = new ArrayList<>();
                                if (value != null && !value.trim().isEmpty()) {
                                    String str = value.trim();

                                    // Remove brackets if present
                                    if (str.startsWith("[") && str.endsWith("]")) {
                                        str = str.substring(1, str.length() - 1);
                                    }

                                    // Remove quotes if present
                                    str = str.replace("\"", "");

                                    // Split by comma
                                    String[] parts = str.split("\\s*,\\s*");

                                    for (String part : parts) {
                                        String cleanPart = part.trim();
                                        if (!cleanPart.isEmpty()) {
                                            selectedKeys.add(cleanPart);
                                        }
                                    }
                                }

                                // Create the field
                                dynamicField = createControlMultiSelectCheckBoxEditField(
                                        campaignFormElement,
                                        requireContext(),
                                        getUserTranslations(campaignFormMeta),
                                        optionsValues,
                                        selectedKeys,
                                        campaignFormElement.isImportant()
                                );

                                // Now set the value using the new method
                                if (dynamicField instanceof ControlCheckBoxGroupField) {
                                    ((ControlCheckBoxGroupField) dynamicField).setOptionsAndValue(optionsValues, selectedKeys);
                                }
                            } else if (type == CampaignFormElementType.NUMBER) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value);
                            } else if (type == CampaignFormElementType.DECIMAL) {
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                    isRangeandExpression = true;
                                }
                                ControlDecimalEditField.setValue((ControlDecimalEditField) dynamicField, value);
                            } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.RANGE) {
                                final boolean exprx = expressionx;

                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                                } else {
                                     dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage, onError, campaignFormMeta.getCampaignFormElements());
                                    isRangeandExpression = true;
                                }


                                if (value != null) {
                                    try {
                                        double num = Double.parseDouble(value);
                                        if (num == Math.floor(num)) {
                                            value = String.valueOf((int) num); // whole number, no decimal
                                        } else {
                                            value = String.format("%.2f", num); // round to 2 decimal places
                                        }
                                    } catch (NumberFormatException e) {
                                        // value is not a number, leave as-is
                                    }
                                }
                                ControlTextEditFieldRange.setValue((ControlTextEditFieldRange) dynamicField, value);
                            } else if (type == CampaignFormElementType.DROPDOWN) {
                                dynamicField = createControlSpinnerFieldEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints, optionsValues, campaignFormElement.isImportant());
                                ControlSpinnerField.setValue((ControlSpinnerField) dynamicField, value);
                            } else if (type == CampaignFormElementType.DATE) {
                                dynamicField = createControlDateEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(),campaignFormElement.isImportant());


                                if (campaign != null) {
                                    Date minDate = null;
                                    Date maxDate = null;

                                    String campaignPhase = "";
                                    if (preCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getPreCampStartDate();
                                     }else if (intraCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getStartDate();
                                     }else if (postCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getPostCampStartDate();
                                     }

                                    maxDate = expiryDate;


                                    if (minDate != null) {
                                        ((ControlDateField) dynamicField).setMinDate(minDate);                            }
                                    if (maxDate != null) {
                                        ((ControlDateField) dynamicField).setMaxDate(maxDate);
                                    }
                                }

                                ControlDateField.setValue((ControlDateField) dynamicField, getDateValue(value));
                            }else if (type == CampaignFormElementType.TEXTBOX) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlTextBoxField.setValue((ControlTextBoxField) dynamicField, value);
                            }else if (type == CampaignFormElementType.IMAGE) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlImageEditField(
                                        campaignFormElement,
                                        requireContext(),
                                        CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta),
                                        userHints,
                                        campaignFormElement.isImportant(),     // not hardcoded true
                                        cameraLauncher,
                                        cameraPermissionLauncher
                                );
    //                            ((ControlImageEditField) dynamicField).setCampaignFormDataUuid(record.getUuid());
    //                            ((ControlImageEditField) dynamicField).setRecord(record);
                                ControlImageEditField.setValue((ControlImageEditField) dynamicField, rawFormValuesMap.get(campaignFormElement.getId()), record);
                            } else {
                                dynamicField = createControlTextEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value);
                            }

                            fieldMap.put(campaignFormElement.getId(), dynamicField);
                            dynamicField.setShowCaption(true);
                            dynamicLayout.addView(dynamicField, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                            Boolean finalIsRangeandExpression = isRangeandExpression;
                            final String dependingOnx = campaignFormElement.getDependingOn();

                            Boolean isdependingOn = false;
                            if (dependingOnx != null) {
                                isdependingOn = true;
                            }
                            Boolean finalIsdependingOn = isdependingOn;
                            dynamicField.addValueChangedListener(field -> {
                                final Boolean isRangeandExpressionx = finalIsRangeandExpression;
                                Boolean okk = field.getFocusedChild() != null ? true : false;
                                final CampaignFormDataEntry campaignFormDataEntry = CampaignFormDataFragmentUtils.getOrCreateCampaignFormDataEntry(formValues, campaignFormElement);
                                campaignFormDataEntry.setValue(field.getValue());
                                if ((campaignFormElement.getExpression() == null && fieldMap.get(campaignFormElement.getId()) != null) || (okk && isRangeandExpressionx)) {
                                    for (CampaignFormDataEntry det : formValues) {
                                        if (det.getValue() != null) {
                                            if (det.getValue().toString().isEmpty()) {
                                                det.setValue(null);
                                            }
                                        }
                                    }
    //                                expressionMap.forEach((formElement, controlPropertyField) ->
    ////                                                CampaignFormDataFragmentUtils.handleExpressionSecEdit(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));
    //
    //                                                                        CampaignFormDataFragmentUtils.handleExpressionSec(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));

                                    boolean changedFieldIsRangeTrigger =
                                            campaignFormElement != null
                                                    && "range".equalsIgnoreCase(campaignFormElement.getType())
                                                    && rangeEffectIds.contains(campaignFormElement.getId());

                                    boolean userStillEditingThisField = field.hasFocus() || field.getFocusedChild() != null;

                                    // pass 1: evaluate non-range effects immediately
                                    expressionMap.forEach((formElement, controlPropertyField) -> {
                                        if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                        boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());

                                        if (!isRangeEffect) {
                                            CampaignFormDataFragmentUtils.handleExpressionSec(
                                                    expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                    controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                        }
                                    });

                                    // pass 2: evaluate range effects, but defer while typing in range trigger
                                    if (!(changedFieldIsRangeTrigger && userStillEditingThisField)) {
                                        expressionMap.forEach((formElement, controlPropertyField) -> {
                                            if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                            boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());
                                            boolean isDeferredRangeEffect = isRangeEffect && rangeEffectIds.contains(formElement.getId());

                                            if (isRangeEffect) {
                                                // range-only effect side
                                                CampaignFormDataFragmentUtils.handleExpressionSec(
                                                        expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                        controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                            }
                                        });
                                    }
                                } else if (field.isFocused()) {
                                    System.out.println(">>>>>>>>>>>>>>>>>ONFOCUSSS>>>>>>>>>>>>>>>>>>>>" + fieldMap.get(campaignFormElement.getId()).getCaption());

                                }

                                if (finalIsdependingOn && isRangeandExpressionx) {
                                    field.setVisibility(View.GONE);
                                }

                            });

                            final String dependingOn = campaignFormElement.getDependingOn();
                            if (dependingOn != null) {
                                handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);
                            }

                            final String expressionString = campaignFormElement.getExpression();
                            if (expressionString != null) {
                                                      handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
                                expressionMap.put(campaignFormElement, dynamicField);
                            }
                        } else if (type == CampaignFormElementType.SECTION) {
                            if (campaignFormElement.getDependingOn() == null) {
                                ControlPropertyField dynamicField;
                                dynamicLayout.addView(new ImageView(requireContext(), null, R.style.FullHorizontalDividerStyle));
                            }
                        } else if (type == CampaignFormElementType.LABEL) {
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, CampaignFormDataFragmentUtils.getUserLanguageCaption(CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), campaignFormElement));
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        } else if (type == CampaignFormElementType.LINEBREAK) {
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, "");
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        }
                    } else if (countr == 4 && onlyDay == 4) {
                        final LinearLayout dynamicLayout = mTabHost.findViewById(R.id.tabSheet4);
                        if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL && type != CampaignFormElementType.LINEBREAK) {
                            String value = formValuesMap.get(campaignFormElement.getId());
    //                        value = value == null ? null : value.endsWith(".0") ?  value.replace(".0", "") : value;
                            if (value != null) {
                                try {
                                    double num = Double.parseDouble(value);
                                    if (num == Math.floor(num)) {
                                        value = String.valueOf((int) num); // whole number, no decimal
                                    } else {
                                        value = String.format("%.2f", num); // round to 2 decimal places
                                    }
                                } catch (NumberFormatException e) {
                                    // value is not a number, leave as-is
                                }
                            }
                            ControlPropertyField dynamicField;
                            boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                            if (type == CampaignFormElementType.YES_NO) {
                                dynamicField = createControlYesNoUnknownField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlSwitchField.setValue((ControlSwitchField) dynamicField, value, true, YesNo.class, null);
                            } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                                dynamicField = createControlCheckBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlCheckBoxField.setValue((ControlCheckBoxField) dynamicField, Boolean.valueOf(value));
                            } else if (type == CampaignFormElementType.CHECKBOXBASIC) {
                                List<String> selectedKeys = new ArrayList<>();
                                if (value != null && !value.trim().isEmpty()) {
                                    String str = value.trim();

                                    // Remove brackets if present
                                    if (str.startsWith("[") && str.endsWith("]")) {
                                        str = str.substring(1, str.length() - 1);
                                    }

                                    // Remove quotes if present
                                    str = str.replace("\"", "");

                                    // Split by comma
                                    String[] parts = str.split("\\s*,\\s*");

                                    for (String part : parts) {
                                        String cleanPart = part.trim();
                                        if (!cleanPart.isEmpty()) {
                                            selectedKeys.add(cleanPart);
                                        }
                                    }
                                }

                                // Create the field
                                dynamicField = createControlMultiSelectCheckBoxEditField(
                                        campaignFormElement,
                                        requireContext(),
                                        getUserTranslations(campaignFormMeta),
                                        optionsValues,
                                        selectedKeys,
                                        campaignFormElement.isImportant()
                                );

                                // Now set the value using the new method
                                if (dynamicField instanceof ControlCheckBoxGroupField) {
                                    ((ControlCheckBoxGroupField) dynamicField).setOptionsAndValue(optionsValues, selectedKeys);
                                }
                            } else if (type == CampaignFormElementType.NUMBER) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value);
                            } else if (type == CampaignFormElementType.DECIMAL) {
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                    isRangeandExpression = true;
                                }
                                ControlDecimalEditField.setValue((ControlDecimalEditField) dynamicField, value);
                            } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.RANGE) {
                                final boolean exprx = expressionx;

                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                                } else {
                                     dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage, onError, campaignFormMeta.getCampaignFormElements());
                                    isRangeandExpression = true;
                                }

                                if (value != null) {
                                    try {
                                        double num = Double.parseDouble(value);
                                        if (num == Math.floor(num)) {
                                            value = String.valueOf((int) num); // whole number, no decimal
                                        } else {
                                            value = String.format("%.2f", num); // round to 2 decimal places
                                        }
                                    } catch (NumberFormatException e) {
                                        // value is not a number, leave as-is
                                    }
                                }

    //                            if (value != null) {
    //                                // Check if the value is a String or a Decimal
    //                                if (value instanceof String ) {
    //                                    try {
    //                                        // Parse the value to a Double
    //                                        double numericValue = Double.parseDouble(value.toString());
    //
    //                                        // If it's a whole number (e.g., ends with .0), convert to integer
    //                                        if (numericValue % 1 == 0) {
    //                                            value = String.valueOf((int) numericValue); // Convert to whole number
    //                                        }
    //                                    } catch (NumberFormatException e) {
    //                                        // Handle cases where value is not a valid number
    //                                        System.err.println("Value is not a valid number: " + value);
    //                                    }
    //                                }
    //                                // Set the value to the field
    ////                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value);
    //                            }
                                ControlTextEditFieldRange.setValue((ControlTextEditFieldRange) dynamicField, value);
                            } else if (type == CampaignFormElementType.DROPDOWN) {
                                dynamicField = createControlSpinnerFieldEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints, optionsValues, campaignFormElement.isImportant());
                                ControlSpinnerField.setValue((ControlSpinnerField) dynamicField, value);
                            } else if (type == CampaignFormElementType.DATE) {
                                dynamicField = createControlDateEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(), campaignFormElement.isImportant());


                                if (campaign != null) {
                                    Date minDate = null;
                                    Date maxDate = null;

                                    String campaignPhase = "";
                                    if (preCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getPreCampStartDate();
                                     }else if (intraCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getStartDate();
                                     }else if (postCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getPostCampStartDate();
                                     }

                                    maxDate = expiryDate;


                                    if (minDate != null) {
                                        ((ControlDateField) dynamicField).setMinDate(minDate);                            }
                                    if (maxDate != null) {
                                        ((ControlDateField) dynamicField).setMaxDate(maxDate);
                                    }
                                }

                                ControlDateField.setValue((ControlDateField) dynamicField, getDateValue(value));
                            }else if (type == CampaignFormElementType.TEXTBOX) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlTextBoxField.setValue((ControlTextBoxField) dynamicField, value);
                            }else if (type == CampaignFormElementType.IMAGE) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlImageEditField(
                                        campaignFormElement,
                                        requireContext(),
                                        CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta),
                                        userHints,
                                        campaignFormElement.isImportant(),     // not hardcoded true
                                        cameraLauncher,
                                        cameraPermissionLauncher
                                );
    //                            ((ControlImageEditField) dynamicField).setCampaignFormDataUuid(record.getUuid());
    //                            ((ControlImageEditField) dynamicField).setRecord(record);
                                ControlImageEditField.setValue((ControlImageEditField) dynamicField, rawFormValuesMap.get(campaignFormElement.getId()), record);
                            } else {
                                dynamicField = createControlTextEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value);
                            }

                            fieldMap.put(campaignFormElement.getId(), dynamicField);
                            dynamicField.setShowCaption(true);
                            dynamicLayout.addView(dynamicField, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                            Boolean finalIsRangeandExpression = isRangeandExpression;
                            final String dependingOnx = campaignFormElement.getDependingOn();

                            Boolean isdependingOn = false;
                            if (dependingOnx != null) {
                                isdependingOn = true;
                            }
                            Boolean finalIsdependingOn = isdependingOn;
                            dynamicField.addValueChangedListener(field -> {
                                final Boolean isRangeandExpressionx = finalIsRangeandExpression;
                                Boolean okk = field.getFocusedChild() != null ? true : false;
                                final CampaignFormDataEntry campaignFormDataEntry = CampaignFormDataFragmentUtils.getOrCreateCampaignFormDataEntry(formValues, campaignFormElement);
                                campaignFormDataEntry.setValue(field.getValue());
                                if ((campaignFormElement.getExpression() == null && fieldMap.get(campaignFormElement.getId()) != null) || (okk && isRangeandExpressionx)) {
                                    for (CampaignFormDataEntry det : formValues) {
                                        if (det.getValue() != null) {
                                            if (det.getValue().toString().isEmpty()) {
                                                det.setValue(null);
                                            }
                                        }
                                    }
    //                                expressionMap.forEach((formElement, controlPropertyField) ->
    ////                                                CampaignFormDataFragmentUtils.handleExpressionSecEdit(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));
    //
    //                                                                                CampaignFormDataFragmentUtils.handleExpressionSec(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));

                                    boolean changedFieldIsRangeTrigger =
                                            campaignFormElement != null
                                                    && "range".equalsIgnoreCase(campaignFormElement.getType())
                                                    && rangeEffectIds.contains(campaignFormElement.getId());

                                    boolean userStillEditingThisField = field.hasFocus() || field.getFocusedChild() != null;

                                    // pass 1: evaluate non-range effects immediately
                                    expressionMap.forEach((formElement, controlPropertyField) -> {
                                        if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                        boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());

                                        if (!isRangeEffect) {
                                            CampaignFormDataFragmentUtils.handleExpressionSec(
                                                    expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                    controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                        }
                                    });

                                    // pass 2: evaluate range effects, but defer while typing in range trigger
                                    if (!(changedFieldIsRangeTrigger && userStillEditingThisField)) {
                                        expressionMap.forEach((formElement, controlPropertyField) -> {
                                            if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                            boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());
                                            boolean isDeferredRangeEffect = isRangeEffect && rangeEffectIds.contains(formElement.getId());

                                            if (isRangeEffect) {
                                                // range-only effect side
                                                CampaignFormDataFragmentUtils.handleExpressionSec(
                                                        expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                        controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                            }
                                        });
                                    }
                                } else if (field.isFocused()) {
                                    System.out.println(">>>>>>>>>>>>>>>>>ONFOCUSSS>>>>>>>>>>>>>>>>>>>>" + fieldMap.get(campaignFormElement.getId()).getCaption());

                                }
                                if (finalIsdependingOn && isRangeandExpressionx) {
                                    field.setVisibility(View.GONE);
                                }

                            });

                            final String dependingOn = campaignFormElement.getDependingOn();
                            if (dependingOn != null) {
                                handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);
                            }

                            final String expressionString = campaignFormElement.getExpression();
                            if (expressionString != null) {
                                                      handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
                                expressionMap.put(campaignFormElement, dynamicField);
                            }
                        } else if (type == CampaignFormElementType.SECTION) {
                            if (campaignFormElement.getDependingOn() == null) {
                                ControlPropertyField dynamicField;
                                dynamicLayout.addView(new ImageView(requireContext(), null, R.style.FullHorizontalDividerStyle));
                            }
                        } else if (type == CampaignFormElementType.LABEL) {
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, CampaignFormDataFragmentUtils.getUserLanguageCaption(CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), campaignFormElement));
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        } else if (type == CampaignFormElementType.LINEBREAK) {
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, "");
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        }
                    } else if (countr == 5 && onlyDay == 5) {
                        final LinearLayout dynamicLayout = mTabHost.findViewById(R.id.tabSheet5);
                        if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL && type != CampaignFormElementType.LINEBREAK) {
                            String value = formValuesMap.get(campaignFormElement.getId());
    //                        value = value == null ? null : value.endsWith(".0") ?  value.replace(".0", "") : value;
                            if (value != null) {
                                try {
                                    double num = Double.parseDouble(value);
                                    if (num == Math.floor(num)) {
                                        value = String.valueOf((int) num); // whole number, no decimal
                                    } else {
                                        value = String.format("%.2f", num); // round to 2 decimal places
                                    }
                                } catch (NumberFormatException e) {
                                    // value is not a number, leave as-is
                                }
                            }
                            ControlPropertyField dynamicField;
                            boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                            if (type == CampaignFormElementType.YES_NO) {
                                dynamicField = createControlYesNoUnknownField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlSwitchField.setValue((ControlSwitchField) dynamicField, value, true, YesNo.class, null);
                            } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                                dynamicField = createControlCheckBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlCheckBoxField.setValue((ControlCheckBoxField) dynamicField, Boolean.valueOf(value));
                            } else if (type == CampaignFormElementType.CHECKBOXBASIC) {
                                List<String> selectedKeys = new ArrayList<>();
                                if (value != null && !value.trim().isEmpty()) {
                                    String str = value.trim();

                                    // Remove brackets if present
                                    if (str.startsWith("[") && str.endsWith("]")) {
                                        str = str.substring(1, str.length() - 1);
                                    }

                                    // Remove quotes if present
                                    str = str.replace("\"", "");

                                    // Split by comma
                                    String[] parts = str.split("\\s*,\\s*");

                                    for (String part : parts) {
                                        String cleanPart = part.trim();
                                        if (!cleanPart.isEmpty()) {
                                            selectedKeys.add(cleanPart);
                                        }
                                    }
                                }

                                // Create the field
                                dynamicField = createControlMultiSelectCheckBoxEditField(
                                        campaignFormElement,
                                        requireContext(),
                                        getUserTranslations(campaignFormMeta),
                                        optionsValues,
                                        selectedKeys,
                                        campaignFormElement.isImportant()
                                );

                                // Now set the value using the new method
                                if (dynamicField instanceof ControlCheckBoxGroupField) {
                                    ((ControlCheckBoxGroupField) dynamicField).setOptionsAndValue(optionsValues, selectedKeys);
                                }
                            } else if (type == CampaignFormElementType.NUMBER) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value);
                            } else if (type == CampaignFormElementType.DECIMAL) {
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                    isRangeandExpression = true;
                                }
                                ControlDecimalEditField.setValue((ControlDecimalEditField) dynamicField, value);
                            } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.RANGE) {
                                final boolean exprx = expressionx;

                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                                } else {
                                     dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage, onError, campaignFormMeta.getCampaignFormElements());
                                    isRangeandExpression = true;
                                }

                                if (value != null) {
                                    try {
                                        double num = Double.parseDouble(value);
                                        if (num == Math.floor(num)) {
                                            value = String.valueOf((int) num); // whole number, no decimal
                                        } else {
                                            value = String.format("%.2f", num); // round to 2 decimal places
                                        }
                                    } catch (NumberFormatException e) {
                                        // value is not a number, leave as-is
                                    }
                                }
                                ControlTextEditFieldRange.setValue((ControlTextEditFieldRange) dynamicField, value);
                            } else if (type == CampaignFormElementType.DROPDOWN) {
                                dynamicField = createControlSpinnerFieldEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints, optionsValues, campaignFormElement.isImportant());
                                ControlSpinnerField.setValue((ControlSpinnerField) dynamicField, value);
                            } else if (type == CampaignFormElementType.DATE) {
                                dynamicField = createControlDateEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(), campaignFormElement.isImportant());


                                if (campaign != null) {
                                    Date minDate = null;
                                    Date maxDate = null;

                                    String campaignPhase = "";
                                    if (preCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getPreCampStartDate();
                                     }else if (intraCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getStartDate();
                                     }else if (postCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getPostCampStartDate();
                                     }

                                    maxDate = expiryDate;


                                    if (minDate != null) {
                                        ((ControlDateField) dynamicField).setMinDate(minDate);                            }
                                    if (maxDate != null) {
                                        ((ControlDateField) dynamicField).setMaxDate(maxDate);
                                    }
                                }

                                ControlDateField.setValue((ControlDateField) dynamicField, getDateValue(value));
                            }else if (type == CampaignFormElementType.TEXTBOX) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlTextBoxField.setValue((ControlTextBoxField) dynamicField, value);
                            }else if (type == CampaignFormElementType.IMAGE) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlImageEditField(
                                        campaignFormElement,
                                        requireContext(),
                                        CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta),
                                        userHints,
                                        campaignFormElement.isImportant(),     // not hardcoded true
                                        cameraLauncher,
                                        cameraPermissionLauncher
                                );
    //                            ((ControlImageEditField) dynamicField).setCampaignFormDataUuid(record.getUuid());
    //                            ((ControlImageEditField) dynamicField).setRecord(record);
                                ControlImageEditField.setValue((ControlImageEditField) dynamicField, rawFormValuesMap.get(campaignFormElement.getId()), record);
                            } else {
                                dynamicField = createControlTextEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value);
                            }

                            fieldMap.put(campaignFormElement.getId(), dynamicField);
                            dynamicField.setShowCaption(true);
                            dynamicLayout.addView(dynamicField, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            Boolean finalIsRangeandExpression = isRangeandExpression;
                            final String dependingOnx = campaignFormElement.getDependingOn();

                            Boolean isdependingOn = false;
                            if (dependingOnx != null) {
                                isdependingOn = true;
                            }
                            Boolean finalIsdependingOn = isdependingOn;
                            dynamicField.addValueChangedListener(field -> {
                                final Boolean isRangeandExpressionx = finalIsRangeandExpression;
                                Boolean okk = field.getFocusedChild() != null ? true : false;
                                final CampaignFormDataEntry campaignFormDataEntry = CampaignFormDataFragmentUtils.getOrCreateCampaignFormDataEntry(formValues, campaignFormElement);
                                campaignFormDataEntry.setValue(field.getValue());
                                if ((campaignFormElement.getExpression() == null && fieldMap.get(campaignFormElement.getId()) != null) || (okk && isRangeandExpressionx)) {
                                    for (CampaignFormDataEntry det : formValues) {
                                        if (det.getValue() != null) {
                                            if (det.getValue().toString().isEmpty()) {
                                                det.setValue(null);
                                            }
                                        }
                                    }
    //                                expressionMap.forEach((formElement, controlPropertyField) ->
    ////                                                CampaignFormDataFragmentUtils.handleExpressionSecEdit(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));
    //
    //                                                                        CampaignFormDataFragmentUtils.handleExpressionSec(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));

                                    boolean changedFieldIsRangeTrigger =
                                            campaignFormElement != null
                                                    && "range".equalsIgnoreCase(campaignFormElement.getType())
                                                    && rangeEffectIds.contains(campaignFormElement.getId());

                                    boolean userStillEditingThisField = field.hasFocus() || field.getFocusedChild() != null;

                                    // pass 1: evaluate non-range effects immediately
                                    expressionMap.forEach((formElement, controlPropertyField) -> {
                                        if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                        boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());

                                        if (!isRangeEffect) {
                                            CampaignFormDataFragmentUtils.handleExpressionSec(
                                                    expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                    controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                        }
                                    });

                                    // pass 2: evaluate range effects, but defer while typing in range trigger
                                    if (!(changedFieldIsRangeTrigger && userStillEditingThisField)) {
                                        expressionMap.forEach((formElement, controlPropertyField) -> {
                                            if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                            boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());
                                            boolean isDeferredRangeEffect = isRangeEffect && rangeEffectIds.contains(formElement.getId());

                                            if (isRangeEffect) {
                                                // range-only effect side
                                                CampaignFormDataFragmentUtils.handleExpressionSec(
                                                        expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                        controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                            }
                                        });
                                    }
                                } else if (field.isFocused()) {
                                    System.out.println(">>>>>>>>>>>>>>>>>ONFOCUSSS>>>>>>>>>>>>>>>>>>>>" + fieldMap.get(campaignFormElement.getId()).getCaption());

                                }
                                if (finalIsdependingOn && isRangeandExpressionx) {
                                    field.setVisibility(View.GONE);
                                }

                            });

                            final String dependingOn = campaignFormElement.getDependingOn();
                            if (dependingOn != null) {
                                handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);
                            }

                            final String expressionString = campaignFormElement.getExpression();
                            if (expressionString != null) {
                                                      handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
                                expressionMap.put(campaignFormElement, dynamicField);
                            }
                        } else if (type == CampaignFormElementType.SECTION) {
                            if (campaignFormElement.getDependingOn() == null) {
                                ControlPropertyField dynamicField;
                                dynamicLayout.addView(new ImageView(requireContext(), null, R.style.FullHorizontalDividerStyle));
                            }
                        } else if (type == CampaignFormElementType.LABEL) {
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, CampaignFormDataFragmentUtils.getUserLanguageCaption(CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), campaignFormElement));
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        } else if (type == CampaignFormElementType.LINEBREAK) {
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, "");
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        }
                    } else if (countr == 6 && onlyDay == 6) {
                        final LinearLayout dynamicLayout = mTabHost.findViewById(R.id.tabSheet6);
                        if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL && type != CampaignFormElementType.LINEBREAK) {
                            String value = formValuesMap.get(campaignFormElement.getId());
    //                        value = value == null ? null : value.endsWith(".0") ?  value.replace(".0", "") : value;
                            if (value != null) {
                                try {
                                    double num = Double.parseDouble(value);
                                    if (num == Math.floor(num)) {
                                        value = String.valueOf((int) num); // whole number, no decimal
                                    } else {
                                        value = String.format("%.2f", num); // round to 2 decimal places
                                    }
                                } catch (NumberFormatException e) {
                                    // value is not a number, leave as-is
                                }
                            }
                            ControlPropertyField dynamicField;
                            boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                            if (type == CampaignFormElementType.YES_NO) {
                                dynamicField = createControlYesNoUnknownField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlSwitchField.setValue((ControlSwitchField) dynamicField, value, true, YesNo.class, null);
                            } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                                dynamicField = createControlCheckBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlCheckBoxField.setValue((ControlCheckBoxField) dynamicField, Boolean.valueOf(value));
                            } else if (type == CampaignFormElementType.CHECKBOXBASIC) {
                                List<String> selectedKeys = new ArrayList<>();
                                if (value != null && !value.trim().isEmpty()) {
                                    String str = value.trim();

                                    // Remove brackets if present
                                    if (str.startsWith("[") && str.endsWith("]")) {
                                        str = str.substring(1, str.length() - 1);
                                    }

                                    // Remove quotes if present
                                    str = str.replace("\"", "");

                                    // Split by comma
                                    String[] parts = str.split("\\s*,\\s*");

                                    for (String part : parts) {
                                        String cleanPart = part.trim();
                                        if (!cleanPart.isEmpty()) {
                                            selectedKeys.add(cleanPart);
                                        }
                                    }
                                }

                                // Create the field
                                dynamicField = createControlMultiSelectCheckBoxEditField(
                                        campaignFormElement,
                                        requireContext(),
                                        getUserTranslations(campaignFormMeta),
                                        optionsValues,
                                        selectedKeys,
                                        campaignFormElement.isImportant()
                                );

                                // Now set the value using the new method
                                if (dynamicField instanceof ControlCheckBoxGroupField) {
                                    ((ControlCheckBoxGroupField) dynamicField).setOptionsAndValue(optionsValues, selectedKeys);
                                }
                            } else if (type == CampaignFormElementType.NUMBER) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value);
                            } else if (type == CampaignFormElementType.DECIMAL) {
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                    isRangeandExpression = true;
                                }
                                ControlDecimalEditField.setValue((ControlDecimalEditField) dynamicField, value);
                            } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.RANGE) {
                                final boolean exprx = expressionx;

                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                                } else {
                                     dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage, onError, campaignFormMeta.getCampaignFormElements());
                                    isRangeandExpression = true;
                                }

                                if (value != null) {
                                    try {
                                        double num = Double.parseDouble(value);
                                        if (num == Math.floor(num)) {
                                            value = String.valueOf((int) num); // whole number, no decimal
                                        } else {
                                            value = String.format("%.2f", num); // round to 2 decimal places
                                        }
                                    } catch (NumberFormatException e) {
                                        // value is not a number, leave as-is
                                    }
                                }
                                ControlTextEditFieldRange.setValue((ControlTextEditFieldRange) dynamicField, value);
                            } else if (type == CampaignFormElementType.DROPDOWN) {
                                dynamicField = createControlSpinnerFieldEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints, optionsValues, campaignFormElement.isImportant());
                                ControlSpinnerField.setValue((ControlSpinnerField) dynamicField, value);
                            } else if (type == CampaignFormElementType.DATE) {
                                dynamicField = createControlDateEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(), campaignFormElement.isImportant());

                                if (campaign != null) {
                                    Date minDate = null;
                                    Date maxDate = null;

                                    String campaignPhase = "";
                                    if (preCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getPreCampStartDate();
                                     }else if (intraCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getStartDate();
                                     }else if (postCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getPostCampStartDate();
                                     }

                                    maxDate = expiryDate;


                                    if (minDate != null) {
                                        ((ControlDateField) dynamicField).setMinDate(minDate);                            }
                                    if (maxDate != null) {
                                        ((ControlDateField) dynamicField).setMaxDate(maxDate);
                                    }
                                }
                                ControlDateField.setValue((ControlDateField) dynamicField, getDateValue(value));
                            }else if (type == CampaignFormElementType.TEXTBOX) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlTextBoxField.setValue((ControlTextBoxField) dynamicField, value);
                            }else if (type == CampaignFormElementType.IMAGE) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlImageEditField(
                                        campaignFormElement,
                                        requireContext(),
                                        CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta),
                                        userHints,
                                        campaignFormElement.isImportant(),     // not hardcoded true
                                        cameraLauncher,
                                        cameraPermissionLauncher
                                );
    //                            ((ControlImageEditField) dynamicField).setCampaignFormDataUuid(record.getUuid());
    //                            ((ControlImageEditField) dynamicField).setRecord(record);
                                ControlImageEditField.setValue((ControlImageEditField) dynamicField, rawFormValuesMap.get(campaignFormElement.getId()), record);
                            } else {
                                dynamicField = createControlTextEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value);
                            }

                            fieldMap.put(campaignFormElement.getId(), dynamicField);
                            dynamicField.setShowCaption(true);
                            dynamicLayout.addView(dynamicField, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            Boolean finalIsRangeandExpression = isRangeandExpression;
                            final String dependingOnx = campaignFormElement.getDependingOn();

                            Boolean isdependingOn = false;
                            if (dependingOnx != null) {
                                isdependingOn = true;
                            }
                            Boolean finalIsdependingOn = isdependingOn;
                            dynamicField.addValueChangedListener(field -> {
                                final Boolean isRangeandExpressionx = finalIsRangeandExpression;
                                Boolean okk = field.getFocusedChild() != null ? true : false;
                                final CampaignFormDataEntry campaignFormDataEntry = CampaignFormDataFragmentUtils.getOrCreateCampaignFormDataEntry(formValues, campaignFormElement);
                                campaignFormDataEntry.setValue(field.getValue());
                                if ((campaignFormElement.getExpression() == null && fieldMap.get(campaignFormElement.getId()) != null) || (okk && isRangeandExpressionx)) {
                                    for (CampaignFormDataEntry det : formValues) {
                                        if (det.getValue() != null) {
                                            if (det.getValue().toString().isEmpty()) {
                                                det.setValue(null);
                                            }
                                        }
                                    }
    //                                expressionMap.forEach((formElement, controlPropertyField) ->
    ////                                                CampaignFormDataFragmentUtils.handleExpressionSecEdit(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));
    //
    //                                                                        CampaignFormDataFragmentUtils.handleExpressionSec(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));

                                    boolean changedFieldIsRangeTrigger =
                                            campaignFormElement != null
                                                    && "range".equalsIgnoreCase(campaignFormElement.getType())
                                                    && rangeEffectIds.contains(campaignFormElement.getId());

                                    boolean userStillEditingThisField = field.hasFocus() || field.getFocusedChild() != null;

                                    // pass 1: evaluate non-range effects immediately
                                    expressionMap.forEach((formElement, controlPropertyField) -> {
                                        if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                        boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());

                                        if (!isRangeEffect) {
                                            CampaignFormDataFragmentUtils.handleExpressionSec(
                                                    expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                    controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                        }
                                    });

                                    // pass 2: evaluate range effects, but defer while typing in range trigger
                                    if (!(changedFieldIsRangeTrigger && userStillEditingThisField)) {
                                        expressionMap.forEach((formElement, controlPropertyField) -> {
                                            if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                            boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());
                                            boolean isDeferredRangeEffect = isRangeEffect && rangeEffectIds.contains(formElement.getId());

                                            if (isRangeEffect) {
                                                // range-only effect side
                                                CampaignFormDataFragmentUtils.handleExpressionSec(
                                                        expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                        controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                            }
                                        });
                                    }
                                } else if (field.isFocused()) {
                                    System.out.println(">>>>>>>>>>>>>>>>>ONFOCUSSS>>>>>>>>>>>>>>>>>>>>" + fieldMap.get(campaignFormElement.getId()).getCaption());

                                }
                                if (finalIsdependingOn && isRangeandExpressionx) {
                                    field.setVisibility(View.GONE);
                                }

                            });

                            final String dependingOn = campaignFormElement.getDependingOn();
                            if (dependingOn != null) {
                                handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);
                            }

                            final String expressionString = campaignFormElement.getExpression();
                            if (expressionString != null) {
                                                      handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
                                expressionMap.put(campaignFormElement, dynamicField);
                            }
                        } else if (type == CampaignFormElementType.SECTION) {
                            if (campaignFormElement.getDependingOn() == null) {
                                ControlPropertyField dynamicField;
                                dynamicLayout.addView(new ImageView(requireContext(), null, R.style.FullHorizontalDividerStyle));
                            }
                        } else if (type == CampaignFormElementType.LABEL) {
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, CampaignFormDataFragmentUtils.getUserLanguageCaption(CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), campaignFormElement));
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        } else if (type == CampaignFormElementType.LINEBREAK) {
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, "");
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        }
                    } else if (countr == 7 && onlyDay == 7) {
                        final LinearLayout dynamicLayout = mTabHost.findViewById(R.id.tabSheet7);
                        if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL && type != CampaignFormElementType.LINEBREAK) {
                            String value = formValuesMap.get(campaignFormElement.getId());
    //                        value = value == null ? null : value.endsWith(".0") ?  value.replace(".0", "") : value;
                            if (value != null) {
                                try {
                                    double num = Double.parseDouble(value);
                                    if (num == Math.floor(num)) {
                                        value = String.valueOf((int) num); // whole number, no decimal
                                    } else {
                                        value = String.format("%.2f", num); // round to 2 decimal places
                                    }
                                } catch (NumberFormatException e) {
                                    // value is not a number, leave as-is
                                }
                            }
                            ControlPropertyField dynamicField;
                            boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                            if (type == CampaignFormElementType.YES_NO) {
                                dynamicField = createControlYesNoUnknownField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlSwitchField.setValue((ControlSwitchField) dynamicField, value, true, YesNo.class, null);
                            } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                                dynamicField = createControlCheckBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlCheckBoxField.setValue((ControlCheckBoxField) dynamicField, Boolean.valueOf(value));
                            } else if (type == CampaignFormElementType.CHECKBOXBASIC) {
                                List<String> selectedKeys = new ArrayList<>();
                                if (value != null && !value.trim().isEmpty()) {
                                    String str = value.trim();

                                    // Remove brackets if present
                                    if (str.startsWith("[") && str.endsWith("]")) {
                                        str = str.substring(1, str.length() - 1);
                                    }

                                    // Remove quotes if present
                                    str = str.replace("\"", "");

                                    // Split by comma
                                    String[] parts = str.split("\\s*,\\s*");

                                    for (String part : parts) {
                                        String cleanPart = part.trim();
                                        if (!cleanPart.isEmpty()) {
                                            selectedKeys.add(cleanPart);
                                        }
                                    }
                                }

                                // Create the field
                                dynamicField = createControlMultiSelectCheckBoxEditField(
                                        campaignFormElement,
                                        requireContext(),
                                        getUserTranslations(campaignFormMeta),
                                        optionsValues,
                                        selectedKeys,
                                        campaignFormElement.isImportant()
                                );

                                // Now set the value using the new method
                                if (dynamicField instanceof ControlCheckBoxGroupField) {
                                    ((ControlCheckBoxGroupField) dynamicField).setOptionsAndValue(optionsValues, selectedKeys);
                                }
                            } else if (type == CampaignFormElementType.NUMBER) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value);
                            } else if (type == CampaignFormElementType.DECIMAL) {
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                    isRangeandExpression = true;
                                }
                                ControlDecimalEditField.setValue((ControlDecimalEditField) dynamicField, value);
                            } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.RANGE) {
                                final boolean exprx = expressionx;

                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                                } else {
                                     dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage, onError, campaignFormMeta.getCampaignFormElements());
                                    isRangeandExpression = true;
                                }

                                if (value != null) {
                                    try {
                                        double num = Double.parseDouble(value);
                                        if (num == Math.floor(num)) {
                                            value = String.valueOf((int) num); // whole number, no decimal
                                        } else {
                                            value = String.format("%.2f", num); // round to 2 decimal places
                                        }
                                    } catch (NumberFormatException e) {
                                        // value is not a number, leave as-is
                                    }
                                }
                                ControlTextEditFieldRange.setValue((ControlTextEditFieldRange) dynamicField, value);
                            } else if (type == CampaignFormElementType.DROPDOWN) {
                                dynamicField = createControlSpinnerFieldEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints, optionsValues, campaignFormElement.isImportant());
                                ControlSpinnerField.setValue((ControlSpinnerField) dynamicField, value);
                            } else if (type == CampaignFormElementType.DATE) {
                                dynamicField = createControlDateEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(), campaignFormElement.isImportant());

                                if (campaign != null) {
                                    Date minDate = null;
                                    Date maxDate = null;

                                    String campaignPhase = "";
                                    if (preCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getPreCampStartDate();
                                     }else if (intraCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getStartDate();
                                     }else if (postCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getPostCampStartDate();
                                     }

                                    maxDate = expiryDate;


                                    if (minDate != null) {
                                        ((ControlDateField) dynamicField).setMinDate(minDate);                            }
                                    if (maxDate != null) {
                                        ((ControlDateField) dynamicField).setMaxDate(maxDate);
                                    }
                                }
                                ControlDateField.setValue((ControlDateField) dynamicField, getDateValue(value));
                            }else if (type == CampaignFormElementType.TEXTBOX) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlTextBoxField.setValue((ControlTextBoxField) dynamicField, value);
                            }else if (type == CampaignFormElementType.IMAGE) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlImageEditField(
                                        campaignFormElement,
                                        requireContext(),
                                        CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta),
                                        userHints,
                                        campaignFormElement.isImportant(),     // not hardcoded true
                                        cameraLauncher,
                                        cameraPermissionLauncher
                                );
    //                            ((ControlImageEditField) dynamicField).setCampaignFormDataUuid(record.getUuid());
    //                            ((ControlImageEditField) dynamicField).setRecord(record);
                                ControlImageEditField.setValue((ControlImageEditField) dynamicField, rawFormValuesMap.get(campaignFormElement.getId()), record);
                            } else {
                                dynamicField = createControlTextEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value);
                            }

                            fieldMap.put(campaignFormElement.getId(), dynamicField);
                            dynamicField.setShowCaption(true);
                            dynamicLayout.addView(dynamicField, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            Boolean finalIsRangeandExpression = isRangeandExpression;
                            final String dependingOnx = campaignFormElement.getDependingOn();

                            Boolean isdependingOn = false;
                            if (dependingOnx != null) {
                                isdependingOn = true;
                            }
                            Boolean finalIsdependingOn = isdependingOn;
                            dynamicField.addValueChangedListener(field -> {
                                final Boolean isRangeandExpressionx = finalIsRangeandExpression;
                                Boolean okk = field.getFocusedChild() != null ? true : false;
                                final CampaignFormDataEntry campaignFormDataEntry = CampaignFormDataFragmentUtils.getOrCreateCampaignFormDataEntry(formValues, campaignFormElement);
                                campaignFormDataEntry.setValue(field.getValue());
                                if ((campaignFormElement.getExpression() == null && fieldMap.get(campaignFormElement.getId()) != null) || (okk && isRangeandExpressionx)) {
                                    for (CampaignFormDataEntry det : formValues) {
                                        if (det.getValue() != null) {
                                            if (det.getValue().toString().isEmpty()) {
                                                det.setValue(null);
                                            }
                                        }
                                    }
    //                                expressionMap.forEach((formElement, controlPropertyField) ->{
    ////                                                CampaignFormDataFragmentUtils.handleExpressionSecEdit(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));
    //
    //                                CampaignFormDataFragmentUtils.handleExpressionSec(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
    //                            });

                                    boolean changedFieldIsRangeTrigger =
                                            campaignFormElement != null
                                                    && "range".equalsIgnoreCase(campaignFormElement.getType())
                                                    && rangeEffectIds.contains(campaignFormElement.getId());

                                    boolean userStillEditingThisField = field.hasFocus() || field.getFocusedChild() != null;

                                    // pass 1: evaluate non-range effects immediately
                                    expressionMap.forEach((formElement, controlPropertyField) -> {
                                        if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                        boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());

                                        if (!isRangeEffect) {
                                            CampaignFormDataFragmentUtils.handleExpressionSec(
                                                    expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                    controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                        }
                                    });

                                    // pass 2: evaluate range effects, but defer while typing in range trigger
                                    if (!(changedFieldIsRangeTrigger && userStillEditingThisField)) {
                                        expressionMap.forEach((formElement, controlPropertyField) -> {
                                            if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                            boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());
                                            boolean isDeferredRangeEffect = isRangeEffect && rangeEffectIds.contains(formElement.getId());

                                            if (isRangeEffect) {
                                                // range-only effect side
                                                CampaignFormDataFragmentUtils.handleExpressionSec(
                                                        expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                        controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                            }
                                        });
                                    }
                                } else if (field.isFocused()) {
                                    System.out.println(">>>>>>>>>>>>>>>>>ONFOCUSSS>>>>>>>>>>>>>>>>>>>>" + fieldMap.get(campaignFormElement.getId()).getCaption());

                                }
                                if (finalIsdependingOn && isRangeandExpressionx) {
                                    field.setVisibility(View.GONE);
                                }

                            });

                            final String dependingOn = campaignFormElement.getDependingOn();
                            if (dependingOn != null) {
                                handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);
                            }

                            final String expressionString = campaignFormElement.getExpression();
                            if (expressionString != null) {
                                                      handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
                                expressionMap.put(campaignFormElement, dynamicField);
                            }
                        } else if (type == CampaignFormElementType.SECTION) {
                            if (campaignFormElement.getDependingOn() == null) {
                                ControlPropertyField dynamicField;
                                dynamicLayout.addView(new ImageView(requireContext(), null, R.style.FullHorizontalDividerStyle));
                            }
                        } else if (type == CampaignFormElementType.LABEL) {
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, CampaignFormDataFragmentUtils.getUserLanguageCaption(CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), campaignFormElement));
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        } else if (type == CampaignFormElementType.LINEBREAK) {
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, "");
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        }
                    } else if (countr == 8 && onlyDay == 8) {
                        final LinearLayout dynamicLayout = mTabHost.findViewById(R.id.tabSheet8);
                        if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL && type != CampaignFormElementType.LINEBREAK) {
                            String value = formValuesMap.get(campaignFormElement.getId());
    //                        value = value == null ? null : value.endsWith(".0") ?  value.replace(".0", "") : value;
                            if (value != null) {
                                try {
                                    double num = Double.parseDouble(value);
                                    if (num == Math.floor(num)) {
                                        value = String.valueOf((int) num); // whole number, no decimal
                                    } else {
                                        value = String.format("%.2f", num); // round to 2 decimal places
                                    }
                                } catch (NumberFormatException e) {
                                    // value is not a number, leave as-is
                                }
                            }
                            ControlPropertyField dynamicField;
                            boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                            if (type == CampaignFormElementType.YES_NO) {
                                dynamicField = createControlYesNoUnknownField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlSwitchField.setValue((ControlSwitchField) dynamicField, value, true, YesNo.class, null);
                            } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                                dynamicField = createControlCheckBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlCheckBoxField.setValue((ControlCheckBoxField) dynamicField, Boolean.valueOf(value));
                            } else if (type == CampaignFormElementType.CHECKBOXBASIC) {
                                List<String> selectedKeys = new ArrayList<>();
                                if (value != null && !value.trim().isEmpty()) {
                                    String str = value.trim();

                                    // Remove brackets if present
                                    if (str.startsWith("[") && str.endsWith("]")) {
                                        str = str.substring(1, str.length() - 1);
                                    }

                                    // Remove quotes if present
                                    str = str.replace("\"", "");

                                    // Split by comma
                                    String[] parts = str.split("\\s*,\\s*");

                                    for (String part : parts) {
                                        String cleanPart = part.trim();
                                        if (!cleanPart.isEmpty()) {
                                            selectedKeys.add(cleanPart);
                                        }
                                    }
                                }

                                // Create the field
                                dynamicField = createControlMultiSelectCheckBoxEditField(
                                        campaignFormElement,
                                        requireContext(),
                                        getUserTranslations(campaignFormMeta),
                                        optionsValues,
                                        selectedKeys,
                                        campaignFormElement.isImportant()
                                );

                                // Now set the value using the new method
                                if (dynamicField instanceof ControlCheckBoxGroupField) {
                                    ((ControlCheckBoxGroupField) dynamicField).setOptionsAndValue(optionsValues, selectedKeys);
                                }
                            } else if (type == CampaignFormElementType.NUMBER) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value);
                            } else if (type == CampaignFormElementType.DECIMAL) {
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                    isRangeandExpression = true;
                                }
                                ControlDecimalEditField.setValue((ControlDecimalEditField) dynamicField, value);
                            } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.RANGE) {
                                final boolean exprx = expressionx;

                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                                } else {
                                     dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage, onError, campaignFormMeta.getCampaignFormElements());
                                    isRangeandExpression = true;
                                }

                                if (value != null) {
                                    try {
                                        double num = Double.parseDouble(value);
                                        if (num == Math.floor(num)) {
                                            value = String.valueOf((int) num); // whole number, no decimal
                                        } else {
                                            value = String.format("%.2f", num); // round to 2 decimal places
                                        }
                                    } catch (NumberFormatException e) {
                                        // value is not a number, leave as-is
                                    }
                                }
                                ControlTextEditFieldRange.setValue((ControlTextEditFieldRange) dynamicField, value);
                            } else if (type == CampaignFormElementType.DROPDOWN) {
                                dynamicField = createControlSpinnerFieldEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints, optionsValues, campaignFormElement.isImportant());
                                ControlSpinnerField.setValue((ControlSpinnerField) dynamicField, value);
                            } else if (type == CampaignFormElementType.DATE) {
                                dynamicField = createControlDateEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(), campaignFormElement.isImportant());

                                if (campaign != null) {
                                    Date minDate = null;
                                    Date maxDate = null;

                                    String campaignPhase = "";
                                    if (preCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getPreCampStartDate();
                                     }else if (intraCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getStartDate();
                                     }else if (postCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getPostCampStartDate();
                                     }

                                    maxDate = expiryDate;


                                    if (minDate != null) {
                                        ((ControlDateField) dynamicField).setMinDate(minDate);                            }
                                    if (maxDate != null) {
                                        ((ControlDateField) dynamicField).setMaxDate(maxDate);
                                    }
                                }
                                ControlDateField.setValue((ControlDateField) dynamicField, getDateValue(value));
                            }else if (type == CampaignFormElementType.TEXTBOX) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlTextBoxField.setValue((ControlTextBoxField) dynamicField, value);
                            }else if (type == CampaignFormElementType.IMAGE) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlImageEditField(
                                        campaignFormElement,
                                        requireContext(),
                                        CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta),
                                        userHints,
                                        campaignFormElement.isImportant(),     // not hardcoded true
                                        cameraLauncher,
                                        cameraPermissionLauncher
                                );
    //                            ((ControlImageEditField) dynamicField).setCampaignFormDataUuid(record.getUuid());
    //                            ((ControlImageEditField) dynamicField).setRecord(record);
                                ControlImageEditField.setValue((ControlImageEditField) dynamicField, rawFormValuesMap.get(campaignFormElement.getId()), record);
                            } else {
                                dynamicField = createControlTextEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value);
                            }

                            fieldMap.put(campaignFormElement.getId(), dynamicField);
                            dynamicField.setShowCaption(true);
                            dynamicLayout.addView(dynamicField, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            Boolean finalIsRangeandExpression = isRangeandExpression;
                            final String dependingOnx = campaignFormElement.getDependingOn();

                            Boolean isdependingOn = false;
                            if (dependingOnx != null) {
                                isdependingOn = true;
                            }
                            Boolean finalIsdependingOn = isdependingOn;
                            dynamicField.addValueChangedListener(field -> {
                                final Boolean isRangeandExpressionx = finalIsRangeandExpression;
                                Boolean okk = field.getFocusedChild() != null ? true : false;
                                final CampaignFormDataEntry campaignFormDataEntry = CampaignFormDataFragmentUtils.getOrCreateCampaignFormDataEntry(formValues, campaignFormElement);
                                campaignFormDataEntry.setValue(field.getValue());
                                if ((campaignFormElement.getExpression() == null && fieldMap.get(campaignFormElement.getId()) != null) || (okk && isRangeandExpressionx)) {
                                    for (CampaignFormDataEntry det : formValues) {
                                        if (det.getValue() != null) {
                                            if (det.getValue().toString().isEmpty()) {
                                                det.setValue(null);
                                            }
                                        }
                                    }
                                    boolean changedFieldIsRangeTrigger =
                                            campaignFormElement != null
                                                    && "range".equalsIgnoreCase(campaignFormElement.getType())
                                                    && rangeEffectIds.contains(campaignFormElement.getId());

                                    boolean userStillEditingThisField = field.hasFocus() || field.getFocusedChild() != null;

                                    // pass 1: evaluate non-range effects immediately
                                    expressionMap.forEach((formElement, controlPropertyField) -> {
                                        if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                        boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());

                                        if (!isRangeEffect) {
                                            CampaignFormDataFragmentUtils.handleExpressionSec(
                                                    expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                    controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                        }
                                    });

                                    // pass 2: evaluate range effects, but defer while typing in range trigger
                                    if (!(changedFieldIsRangeTrigger && userStillEditingThisField)) {
                                        expressionMap.forEach((formElement, controlPropertyField) -> {
                                            if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                            boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());
                                            boolean isDeferredRangeEffect = isRangeEffect && rangeEffectIds.contains(formElement.getId());

                                            if (isRangeEffect) {
                                                // range-only effect side
                                                CampaignFormDataFragmentUtils.handleExpressionSec(
                                                        expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                        controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                            }
                                        });
                                    }
                                } else if (field.isFocused()) {
                                    System.out.println(">>>>>>>>>>>>>>>>>ONFOCUSSS>>>>>>>>>>>>>>>>>>>>" + fieldMap.get(campaignFormElement.getId()).getCaption());

                                }
                                if (finalIsdependingOn && isRangeandExpressionx) {
                                    field.setVisibility(View.GONE);
                                }

                            });


                            final String dependingOn = campaignFormElement.getDependingOn();
                            if (dependingOn != null) {
                                handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);
                            }

                            final String expressionString = campaignFormElement.getExpression();
                            if (expressionString != null) {
                                                      handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
                                expressionMap.put(campaignFormElement, dynamicField);
                            }


                        } else if (type == CampaignFormElementType.SECTION) {
                            if (campaignFormElement.getDependingOn() == null) {
                                ControlPropertyField dynamicField;
                                dynamicLayout.addView(new ImageView(requireContext(), null, R.style.FullHorizontalDividerStyle));
                            }
                        } else if (type == CampaignFormElementType.LABEL) {
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, CampaignFormDataFragmentUtils.getUserLanguageCaption(CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), campaignFormElement));
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        } else if (type == CampaignFormElementType.LINEBREAK) {
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, "");
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        }
                    }

                    else if (countr == 9 && onlyDay == 9) {
                        final LinearLayout dynamicLayout = mTabHost.findViewById(R.id.tabSheet9);
                        if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL && type != CampaignFormElementType.LINEBREAK) {
                            String value = formValuesMap.get(campaignFormElement.getId());
    //                        value = value == null ? null : value.endsWith(".0") ?  value.replace(".0", "") : value;
                            if (value != null) {
                                try {
                                    double num = Double.parseDouble(value);
                                    if (num == Math.floor(num)) {
                                        value = String.valueOf((int) num); // whole number, no decimal
                                    } else {
                                        value = String.format("%.2f", num); // round to 2 decimal places
                                    }
                                } catch (NumberFormatException e) {
                                    // value is not a number, leave as-is
                                }
                            }
                            ControlPropertyField dynamicField;
                            boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                            if (type == CampaignFormElementType.YES_NO) {
                                dynamicField = createControlYesNoUnknownField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlSwitchField.setValue((ControlSwitchField) dynamicField, value, true, YesNo.class, null);
                            } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                                dynamicField = createControlCheckBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlCheckBoxField.setValue((ControlCheckBoxField) dynamicField, Boolean.valueOf(value));
                            } else if (type == CampaignFormElementType.CHECKBOXBASIC) {
                                List<String> selectedKeys = new ArrayList<>();
                                if (value != null && !value.trim().isEmpty()) {
                                    String str = value.trim();

                                    // Remove brackets if present
                                    if (str.startsWith("[") && str.endsWith("]")) {
                                        str = str.substring(1, str.length() - 1);
                                    }

                                    // Remove quotes if present
                                    str = str.replace("\"", "");

                                    // Split by comma
                                    String[] parts = str.split("\\s*,\\s*");

                                    for (String part : parts) {
                                        String cleanPart = part.trim();
                                        if (!cleanPart.isEmpty()) {
                                            selectedKeys.add(cleanPart);
                                        }
                                    }
                                }

                                // Create the field
                                dynamicField = createControlMultiSelectCheckBoxEditField(
                                        campaignFormElement,
                                        requireContext(),
                                        getUserTranslations(campaignFormMeta),
                                        optionsValues,
                                        selectedKeys,
                                        campaignFormElement.isImportant()
                                );

                                // Now set the value using the new method
                                if (dynamicField instanceof ControlCheckBoxGroupField) {
                                    ((ControlCheckBoxGroupField) dynamicField).setOptionsAndValue(optionsValues, selectedKeys);
                                }
                            } else if (type == CampaignFormElementType.NUMBER) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value);
                            } else if (type == CampaignFormElementType.DECIMAL) {
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                    isRangeandExpression = true;
                                }
                                ControlDecimalEditField.setValue((ControlDecimalEditField) dynamicField, value);
                            } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.RANGE) {
                                final boolean exprx = expressionx;

                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage, onError, campaignFormMeta.getCampaignFormElements());
                                    isRangeandExpression = true;
                                }

                                if (value != null) {
                                    try {
                                        double num = Double.parseDouble(value);
                                        if (num == Math.floor(num)) {
                                            value = String.valueOf((int) num); // whole number, no decimal
                                        } else {
                                            value = String.format("%.2f", num); // round to 2 decimal places
                                        }
                                    } catch (NumberFormatException e) {
                                        // value is not a number, leave as-is
                                    }
                                }
                                ControlTextEditFieldRange.setValue((ControlTextEditFieldRange) dynamicField, value);
                            } else if (type == CampaignFormElementType.DROPDOWN) {
                                dynamicField = createControlSpinnerFieldEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints, optionsValues, campaignFormElement.isImportant());
                                ControlSpinnerField.setValue((ControlSpinnerField) dynamicField, value);
                            } else if (type == CampaignFormElementType.DATE) {
                                dynamicField = createControlDateEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(), campaignFormElement.isImportant());

                                if (campaign != null) {
                                    Date minDate = null;
                                    Date maxDate = null;

                                    String campaignPhase = "";
                                    if (preCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getPreCampStartDate();
                                    }else if (intraCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getStartDate();
                                    }else if (postCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getPostCampStartDate();
                                    }

                                    maxDate = expiryDate;


                                    if (minDate != null) {
                                        ((ControlDateField) dynamicField).setMinDate(minDate);                            }
                                    if (maxDate != null) {
                                        ((ControlDateField) dynamicField).setMaxDate(maxDate);
                                    }
                                }
                                ControlDateField.setValue((ControlDateField) dynamicField, getDateValue(value));
                            }else if (type == CampaignFormElementType.TEXTBOX) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlTextBoxField.setValue((ControlTextBoxField) dynamicField, value);
                            }else if (type == CampaignFormElementType.IMAGE) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlImageEditField(
                                        campaignFormElement,
                                        requireContext(),
                                        CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta),
                                        userHints,
                                        campaignFormElement.isImportant(),     // not hardcoded true
                                        cameraLauncher,
                                        cameraPermissionLauncher
                                );
    //                            ((ControlImageEditField) dynamicField).setCampaignFormDataUuid(record.getUuid());
    //                            ((ControlImageEditField) dynamicField).setRecord(record);
                                ControlImageEditField.setValue((ControlImageEditField) dynamicField, rawFormValuesMap.get(campaignFormElement.getId()), record);
                            } else {
                                dynamicField = createControlTextEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value);
                            }

                            fieldMap.put(campaignFormElement.getId(), dynamicField);
                            dynamicField.setShowCaption(true);
                            dynamicLayout.addView(dynamicField, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            Boolean finalIsRangeandExpression = isRangeandExpression;
                            final String dependingOnx = campaignFormElement.getDependingOn();

                            Boolean isdependingOn = false;
                            if (dependingOnx != null) {
                                isdependingOn = true;
                            }
                            Boolean finalIsdependingOn = isdependingOn;
                            dynamicField.addValueChangedListener(field -> {
                                final Boolean isRangeandExpressionx = finalIsRangeandExpression;
                                Boolean okk = field.getFocusedChild() != null ? true : false;
                                final CampaignFormDataEntry campaignFormDataEntry = CampaignFormDataFragmentUtils.getOrCreateCampaignFormDataEntry(formValues, campaignFormElement);
                                campaignFormDataEntry.setValue(field.getValue());
                                if ((campaignFormElement.getExpression() == null && fieldMap.get(campaignFormElement.getId()) != null) || (okk && isRangeandExpressionx)) {
                                    for (CampaignFormDataEntry det : formValues) {
                                        if (det.getValue() != null) {
                                            if (det.getValue().toString().isEmpty()) {
                                                det.setValue(null);
                                            }
                                        }
                                    }
                                    boolean changedFieldIsRangeTrigger =
                                            campaignFormElement != null
                                                    && "range".equalsIgnoreCase(campaignFormElement.getType())
                                                    && rangeEffectIds.contains(campaignFormElement.getId());

                                    boolean userStillEditingThisField = field.hasFocus() || field.getFocusedChild() != null;

                                    // pass 1: evaluate non-range effects immediately
                                    expressionMap.forEach((formElement, controlPropertyField) -> {
                                        if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                        boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());

                                        if (!isRangeEffect) {
                                            CampaignFormDataFragmentUtils.handleExpressionSec(
                                                    expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                    controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                        }
                                    });

                                    // pass 2: evaluate range effects, but defer while typing in range trigger
                                    if (!(changedFieldIsRangeTrigger && userStillEditingThisField)) {
                                        expressionMap.forEach((formElement, controlPropertyField) -> {
                                            if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                            boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());
                                            boolean isDeferredRangeEffect = isRangeEffect && rangeEffectIds.contains(formElement.getId());

                                            if (isRangeEffect) {
                                                // range-only effect side
                                                CampaignFormDataFragmentUtils.handleExpressionSec(
                                                        expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                        controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                            }
                                        });
                                    }
                                } else if (field.isFocused()) {
                                    System.out.println(">>>>>>>>>>>>>>>>>ONFOCUSSS>>>>>>>>>>>>>>>>>>>>" + fieldMap.get(campaignFormElement.getId()).getCaption());

                                }
                                if (finalIsdependingOn && isRangeandExpressionx) {
                                    field.setVisibility(View.GONE);
                                }

                            });


                            final String dependingOn = campaignFormElement.getDependingOn();
                            if (dependingOn != null) {
                                handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);
                            }

                            final String expressionString = campaignFormElement.getExpression();
                            if (expressionString != null) {
                                handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
                                expressionMap.put(campaignFormElement, dynamicField);
                            }


                        } else if (type == CampaignFormElementType.SECTION) {
                            if (campaignFormElement.getDependingOn() == null) {
                                ControlPropertyField dynamicField;
                                dynamicLayout.addView(new ImageView(requireContext(), null, R.style.FullHorizontalDividerStyle));
                            }
                        } else if (type == CampaignFormElementType.LABEL) {
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, CampaignFormDataFragmentUtils.getUserLanguageCaption(CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), campaignFormElement));
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        } else if (type == CampaignFormElementType.LINEBREAK) {
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, "");
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        }
                    }

                    else if (countr == 10 && onlyDay == 10) {
                        final LinearLayout dynamicLayout = mTabHost.findViewById(R.id.tabSheet10);
                        if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL && type != CampaignFormElementType.LINEBREAK) {
                            String value = formValuesMap.get(campaignFormElement.getId());
    //                        value = value == null ? null : value.endsWith(".0") ?  value.replace(".0", "") : value;
                            if (value != null) {
                                try {
                                    double num = Double.parseDouble(value);
                                    if (num == Math.floor(num)) {
                                        value = String.valueOf((int) num); // whole number, no decimal
                                    } else {
                                        value = String.format("%.2f", num); // round to 2 decimal places
                                    }
                                } catch (NumberFormatException e) {
                                    // value is not a number, leave as-is
                                }
                            }
                            ControlPropertyField dynamicField;
                            boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                            if (type == CampaignFormElementType.YES_NO) {
                                dynamicField = createControlYesNoUnknownField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlSwitchField.setValue((ControlSwitchField) dynamicField, value, true, YesNo.class, null);
                            } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                                dynamicField = createControlCheckBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlCheckBoxField.setValue((ControlCheckBoxField) dynamicField, Boolean.valueOf(value));
                            } else if (type == CampaignFormElementType.CHECKBOXBASIC) {
                                List<String> selectedKeys = new ArrayList<>();
                                if (value != null && !value.trim().isEmpty()) {
                                    String str = value.trim();

                                    // Remove brackets if present
                                    if (str.startsWith("[") && str.endsWith("]")) {
                                        str = str.substring(1, str.length() - 1);
                                    }

                                    // Remove quotes if present
                                    str = str.replace("\"", "");

                                    // Split by comma
                                    String[] parts = str.split("\\s*,\\s*");

                                    for (String part : parts) {
                                        String cleanPart = part.trim();
                                        if (!cleanPart.isEmpty()) {
                                            selectedKeys.add(cleanPart);
                                        }
                                    }
                                }

                                // Create the field
                                dynamicField = createControlMultiSelectCheckBoxEditField(
                                        campaignFormElement,
                                        requireContext(),
                                        getUserTranslations(campaignFormMeta),
                                        optionsValues,
                                        selectedKeys,
                                        campaignFormElement.isImportant()
                                );

                                // Now set the value using the new method
                                if (dynamicField instanceof ControlCheckBoxGroupField) {
                                    ((ControlCheckBoxGroupField) dynamicField).setOptionsAndValue(optionsValues, selectedKeys);
                                }
                            } else if (type == CampaignFormElementType.NUMBER) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value);
                            } else if (type == CampaignFormElementType.DECIMAL) {
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                    isRangeandExpression = true;
                                }
                                ControlDecimalEditField.setValue((ControlDecimalEditField) dynamicField, value);
                            } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.RANGE) {
                                final boolean exprx = expressionx;

                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage, onError, campaignFormMeta.getCampaignFormElements());
                                    isRangeandExpression = true;
                                }

                                if (value != null) {
                                    try {
                                        double num = Double.parseDouble(value);
                                        if (num == Math.floor(num)) {
                                            value = String.valueOf((int) num); // whole number, no decimal
                                        } else {
                                            value = String.format("%.2f", num); // round to 2 decimal places
                                        }
                                    } catch (NumberFormatException e) {
                                        // value is not a number, leave as-is
                                    }
                                }
                                ControlTextEditFieldRange.setValue((ControlTextEditFieldRange) dynamicField, value);
                            } else if (type == CampaignFormElementType.DROPDOWN) {
                                dynamicField = createControlSpinnerFieldEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints, optionsValues, campaignFormElement.isImportant());
                                ControlSpinnerField.setValue((ControlSpinnerField) dynamicField, value);
                            } else if (type == CampaignFormElementType.DATE) {
                                dynamicField = createControlDateEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(), campaignFormElement.isImportant());

                                if (campaign != null) {
                                    Date minDate = null;
                                    Date maxDate = null;

                                    String campaignPhase = "";
                                    if (preCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getPreCampStartDate();
                                    }else if (intraCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getStartDate();
                                    }else if (postCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getPostCampStartDate();
                                    }

                                    maxDate = expiryDate;


                                    if (minDate != null) {
                                        ((ControlDateField) dynamicField).setMinDate(minDate);                            }
                                    if (maxDate != null) {
                                        ((ControlDateField) dynamicField).setMaxDate(maxDate);
                                    }
                                }
                                ControlDateField.setValue((ControlDateField) dynamicField, getDateValue(value));
                            }else if (type == CampaignFormElementType.TEXTBOX) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlTextBoxField.setValue((ControlTextBoxField) dynamicField, value);
                            }else if (type == CampaignFormElementType.IMAGE) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlImageEditField(
                                        campaignFormElement,
                                        requireContext(),
                                        CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta),
                                        userHints,
                                        campaignFormElement.isImportant(),     // not hardcoded true
                                        cameraLauncher,
                                        cameraPermissionLauncher
                                );
                                ControlImageEditField.setValue((ControlImageEditField) dynamicField, rawFormValuesMap.get(campaignFormElement.getId()), record);
                            } else {
                                dynamicField = createControlTextEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value);
                            }

                            fieldMap.put(campaignFormElement.getId(), dynamicField);
                            dynamicField.setShowCaption(true);
                            dynamicLayout.addView(dynamicField, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            Boolean finalIsRangeandExpression = isRangeandExpression;
                            final String dependingOnx = campaignFormElement.getDependingOn();

                            Boolean isdependingOn = false;
                            if (dependingOnx != null) {
                                isdependingOn = true;
                            }
                            Boolean finalIsdependingOn = isdependingOn;
                            dynamicField.addValueChangedListener(field -> {
                                final Boolean isRangeandExpressionx = finalIsRangeandExpression;
                                Boolean okk = field.getFocusedChild() != null ? true : false;
                                final CampaignFormDataEntry campaignFormDataEntry = CampaignFormDataFragmentUtils.getOrCreateCampaignFormDataEntry(formValues, campaignFormElement);
                                campaignFormDataEntry.setValue(field.getValue());
                                if ((campaignFormElement.getExpression() == null && fieldMap.get(campaignFormElement.getId()) != null) || (okk && isRangeandExpressionx)) {
                                    for (CampaignFormDataEntry det : formValues) {
                                        if (det.getValue() != null) {
                                            if (det.getValue().toString().isEmpty()) {
                                                det.setValue(null);
                                            }
                                        }
                                    }
                                    boolean changedFieldIsRangeTrigger =
                                            campaignFormElement != null
                                                    && "range".equalsIgnoreCase(campaignFormElement.getType())
                                                    && rangeEffectIds.contains(campaignFormElement.getId());

                                    boolean userStillEditingThisField = field.hasFocus() || field.getFocusedChild() != null;

                                    // pass 1: evaluate non-range effects immediately
                                    expressionMap.forEach((formElement, controlPropertyField) -> {
                                        if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                        boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());

                                        if (!isRangeEffect) {
                                            CampaignFormDataFragmentUtils.handleExpressionSec(
                                                    expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                    controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                        }
                                    });

                                    // pass 2: evaluate range effects, but defer while typing in range trigger
                                    if (!(changedFieldIsRangeTrigger && userStillEditingThisField)) {
                                        expressionMap.forEach((formElement, controlPropertyField) -> {
                                            if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                            boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());
                                            boolean isDeferredRangeEffect = isRangeEffect && rangeEffectIds.contains(formElement.getId());

                                            if (isRangeEffect) {
                                                // range-only effect side
                                                CampaignFormDataFragmentUtils.handleExpressionSec(
                                                        expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                        controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                            }
                                        });
                                    }
                                } else if (field.isFocused()) {
                                    System.out.println(">>>>>>>>>>>>>>>>>ONFOCUSSS>>>>>>>>>>>>>>>>>>>>" + fieldMap.get(campaignFormElement.getId()).getCaption());

                                }
                                if (finalIsdependingOn && isRangeandExpressionx) {
                                    field.setVisibility(View.GONE);
                                }

                            });


                            final String dependingOn = campaignFormElement.getDependingOn();
                            if (dependingOn != null) {
                                handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);
                            }

                            final String expressionString = campaignFormElement.getExpression();
                            if (expressionString != null) {
                                handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
                                expressionMap.put(campaignFormElement, dynamicField);
                            }


                        } else if (type == CampaignFormElementType.SECTION) {
                            if (campaignFormElement.getDependingOn() == null) {
                                ControlPropertyField dynamicField;
                                dynamicLayout.addView(new ImageView(requireContext(), null, R.style.FullHorizontalDividerStyle));
                            }
                        } else if (type == CampaignFormElementType.LABEL) {
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, CampaignFormDataFragmentUtils.getUserLanguageCaption(CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), campaignFormElement));
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        } else if (type == CampaignFormElementType.LINEBREAK) {
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, "");
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        }
                    }
                    else if (countr == 11 && onlyDay == 11) {
                        final LinearLayout dynamicLayout = mTabHost.findViewById(R.id.tabSheet11);
                        if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL && type != CampaignFormElementType.LINEBREAK) {
                            String value = formValuesMap.get(campaignFormElement.getId());
    //                        value = value == null ? null : value.endsWith(".0") ?  value.replace(".0", "") : value;
                            if (value != null) {
                                try {
                                    double num = Double.parseDouble(value);
                                    if (num == Math.floor(num)) {
                                        value = String.valueOf((int) num); // whole number, no decimal
                                    } else {
                                        value = String.format("%.2f", num); // round to 2 decimal places
                                    }
                                } catch (NumberFormatException e) {
                                    // value is not a number, leave as-is
                                }
                            }
                            ControlPropertyField dynamicField;
                            boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                            if (type == CampaignFormElementType.YES_NO) {
                                dynamicField = createControlYesNoUnknownField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlSwitchField.setValue((ControlSwitchField) dynamicField, value, true, YesNo.class, null);
                            } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                                dynamicField = createControlCheckBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlCheckBoxField.setValue((ControlCheckBoxField) dynamicField, Boolean.valueOf(value));
                            } else if (type == CampaignFormElementType.CHECKBOXBASIC) {
                                List<String> selectedKeys = new ArrayList<>();
                                if (value != null && !value.trim().isEmpty()) {
                                    String str = value.trim();

                                    // Remove brackets if present
                                    if (str.startsWith("[") && str.endsWith("]")) {
                                        str = str.substring(1, str.length() - 1);
                                    }

                                    // Remove quotes if present
                                    str = str.replace("\"", "");

                                    // Split by comma
                                    String[] parts = str.split("\\s*,\\s*");

                                    for (String part : parts) {
                                        String cleanPart = part.trim();
                                        if (!cleanPart.isEmpty()) {
                                            selectedKeys.add(cleanPart);
                                        }
                                    }
                                }

                                // Create the field
                                dynamicField = createControlMultiSelectCheckBoxEditField(
                                        campaignFormElement,
                                        requireContext(),
                                        getUserTranslations(campaignFormMeta),
                                        optionsValues,
                                        selectedKeys,
                                        campaignFormElement.isImportant()
                                );

                                // Now set the value using the new method
                                if (dynamicField instanceof ControlCheckBoxGroupField) {
                                    ((ControlCheckBoxGroupField) dynamicField).setOptionsAndValue(optionsValues, selectedKeys);
                                }
                            } else if (type == CampaignFormElementType.NUMBER) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value);
                            } else if (type == CampaignFormElementType.DECIMAL) {
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                    isRangeandExpression = true;
                                }
                                ControlDecimalEditField.setValue((ControlDecimalEditField) dynamicField, value);
                            } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.RANGE) {
                                final boolean exprx = expressionx;

                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage, onError, campaignFormMeta.getCampaignFormElements());
                                    isRangeandExpression = true;
                                }

                                if (value != null) {
                                    try {
                                        double num = Double.parseDouble(value);
                                        if (num == Math.floor(num)) {
                                            value = String.valueOf((int) num); // whole number, no decimal
                                        } else {
                                            value = String.format("%.2f", num); // round to 2 decimal places
                                        }
                                    } catch (NumberFormatException e) {
                                        // value is not a number, leave as-is
                                    }
                                }
                                ControlTextEditFieldRange.setValue((ControlTextEditFieldRange) dynamicField, value);
                            } else if (type == CampaignFormElementType.DROPDOWN) {
                                dynamicField = createControlSpinnerFieldEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints, optionsValues, campaignFormElement.isImportant());
                                ControlSpinnerField.setValue((ControlSpinnerField) dynamicField, value);
                            } else if (type == CampaignFormElementType.DATE) {
                                dynamicField = createControlDateEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(), campaignFormElement.isImportant());

                                if (campaign != null) {
                                    Date minDate = null;
                                    Date maxDate = null;

                                    String campaignPhase = "";
                                    if (preCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getPreCampStartDate();
                                    }else if (intraCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getStartDate();
                                    }else if (postCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                        minDate = campaign.getPostCampStartDate();
                                    }

                                    maxDate = expiryDate;


                                    if (minDate != null) {
                                        ((ControlDateField) dynamicField).setMinDate(minDate);                            }
                                    if (maxDate != null) {
                                        ((ControlDateField) dynamicField).setMaxDate(maxDate);
                                    }
                                }
                                ControlDateField.setValue((ControlDateField) dynamicField, getDateValue(value));
                            }else if (type == CampaignFormElementType.TEXTBOX) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                                ControlTextBoxField.setValue((ControlTextBoxField) dynamicField, value);
                            }  else if (type == CampaignFormElementType.IMAGE) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlImageEditField(
                                        campaignFormElement,
                                        requireContext(),
                                        CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta),
                                        userHints,
                                        campaignFormElement.isImportant(),     // not hardcoded true
                                        cameraLauncher,
                                        cameraPermissionLauncher
                                );
    //                            ((ControlImageEditField) dynamicField).setCampaignFormDataUuid(record.getUuid());
    //                            ((ControlImageEditField) dynamicField).setRecord(record);
                                ControlImageEditField.setValue((ControlImageEditField) dynamicField, rawFormValuesMap.get(campaignFormElement.getId()), record);
                            } else {
                                dynamicField = createControlTextEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value);
                            }

                            fieldMap.put(campaignFormElement.getId(), dynamicField);
                            dynamicField.setShowCaption(true);
                            dynamicLayout.addView(dynamicField, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            Boolean finalIsRangeandExpression = isRangeandExpression;
                            final String dependingOnx = campaignFormElement.getDependingOn();

                            Boolean isdependingOn = false;
                            if (dependingOnx != null) {
                                isdependingOn = true;
                            }
                            Boolean finalIsdependingOn = isdependingOn;
                            dynamicField.addValueChangedListener(field -> {
                                final Boolean isRangeandExpressionx = finalIsRangeandExpression;
                                Boolean okk = field.getFocusedChild() != null ? true : false;
                                final CampaignFormDataEntry campaignFormDataEntry = CampaignFormDataFragmentUtils.getOrCreateCampaignFormDataEntry(formValues, campaignFormElement);
                                campaignFormDataEntry.setValue(field.getValue());
                                if ((campaignFormElement.getExpression() == null && fieldMap.get(campaignFormElement.getId()) != null) || (okk && isRangeandExpressionx)) {
                                    for (CampaignFormDataEntry det : formValues) {
                                        if (det.getValue() != null) {
                                            if (det.getValue().toString().isEmpty()) {
                                                det.setValue(null);
                                            }
                                        }
                                    }
                                    boolean changedFieldIsRangeTrigger =
                                            campaignFormElement != null
                                                    && "range".equalsIgnoreCase(campaignFormElement.getType())
                                                    && rangeEffectIds.contains(campaignFormElement.getId());

                                    boolean userStillEditingThisField = field.hasFocus() || field.getFocusedChild() != null;

                                    // pass 1: evaluate non-range effects immediately
                                    expressionMap.forEach((formElement, controlPropertyField) -> {
                                        if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                        boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());

                                        if (!isRangeEffect) {
                                            CampaignFormDataFragmentUtils.handleExpressionSec(
                                                    expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                    controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                        }
                                    });

                                    // pass 2: evaluate range effects, but defer while typing in range trigger
                                    if (!(changedFieldIsRangeTrigger && userStillEditingThisField)) {
                                        expressionMap.forEach((formElement, controlPropertyField) -> {
                                            if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                            boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());
                                            boolean isDeferredRangeEffect = isRangeEffect && rangeEffectIds.contains(formElement.getId());

                                            if (isRangeEffect) {
                                                // range-only effect side
                                                CampaignFormDataFragmentUtils.handleExpressionSec(
                                                        expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                        controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                            }
                                        });
                                    }
                                } else if (field.isFocused()) {
                                    System.out.println(">>>>>>>>>>>>>>>>>ONFOCUSSS>>>>>>>>>>>>>>>>>>>>" + fieldMap.get(campaignFormElement.getId()).getCaption());

                                }
                                if (finalIsdependingOn && isRangeandExpressionx) {
                                    field.setVisibility(View.GONE);
                                }

                            });


                            final String dependingOn = campaignFormElement.getDependingOn();
                            if (dependingOn != null) {
                                handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);
                            }

                            final String expressionString = campaignFormElement.getExpression();
                            if (expressionString != null) {
                                handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
                                expressionMap.put(campaignFormElement, dynamicField);
                            }


                        } else if (type == CampaignFormElementType.SECTION) {
                            if (campaignFormElement.getDependingOn() == null) {
                                ControlPropertyField dynamicField;
                                dynamicLayout.addView(new ImageView(requireContext(), null, R.style.FullHorizontalDividerStyle));
                            }
                        } else if (type == CampaignFormElementType.LABEL) {
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, CampaignFormDataFragmentUtils.getUserLanguageCaption(CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), campaignFormElement));
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        } else if (type == CampaignFormElementType.LINEBREAK) {
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, "");
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        }
                    }
                } else {
                    final LinearLayout dynamicLayout = view.findViewById(R.id.dynamicLayoutxXEd);
                    if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL && type != CampaignFormElementType.LINEBREAK) {
                        String value = formValuesMap.get(campaignFormElement.getId());
    //                    value = value == null ? null : value.endsWith(".0") ?  value.replace(".0", "") : value;
                        String yes_no = "";
    //                    if (value != null) {
    //                        try {
    //                            double num = Double.parseDouble(value);
    //                            if (num == Math.floor(num)) {
    //                                value = String.valueOf((int) num); // whole number, no decimal
    //                            } else {
    //                                value = String.format("%.2f", num); // round to 2 decimal places
    //                            }
    //                        } catch (NumberFormatException e) {
    //                            // value is not a number, leave as-is
    //                        }
    //                    }
                        ControlPropertyField dynamicField;
                        boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                        if (type == CampaignFormElementType.YES_NO) {
                            dynamicField = createControlYesNoUnknownField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                            ControlSwitchField.setValue((ControlSwitchField) dynamicField, value, true, YesNo.class, null);
                        } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                            dynamicField = createControlCheckBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                            ControlCheckBoxField.setValue((ControlCheckBoxField) dynamicField, Boolean.valueOf(value));
                        }

                        else if (type == CampaignFormElementType.CHECKBOXBASIC) {
                            List<String> selectedKeys = new ArrayList<>();
                            if (value != null && !value.trim().isEmpty()) {
                                String str = value.trim();

                                // Remove brackets if present
                                if (str.startsWith("[") && str.endsWith("]")) {
                                    str = str.substring(1, str.length() - 1);
                                }

                                // Remove quotes if present
                                str = str.replace("\"", "");

                                // Split by comma
                                String[] parts = str.split("\\s*,\\s*");

                                for (String part : parts) {
                                    String cleanPart = part.trim();
                                    if (!cleanPart.isEmpty()) {
                                        selectedKeys.add(cleanPart);
                                    }
                                }
                            }

                            // Create the field
                            dynamicField = createControlMultiSelectCheckBoxEditField(
                                    campaignFormElement,
                                    requireContext(),
                                    getUserTranslations(campaignFormMeta),
                                    optionsValues,
                                    selectedKeys,
                                    campaignFormElement.isImportant()
                            );

                            // Now set the value using the new method
                            if (dynamicField instanceof ControlCheckBoxGroupField) {
                                ((ControlCheckBoxGroupField) dynamicField).setOptionsAndValue(optionsValues, selectedKeys);
                            }
                        }
                        else if (type == CampaignFormElementType.DECIMAL) {
                            final boolean exprx = expressionx;
                            if (!exprx) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                            } else {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                isRangeandExpression = true;
                            }
                            ControlDecimalEditField.setValue((ControlDecimalEditField) dynamicField, value);
                        } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                            dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                        } else if (type == CampaignFormElementType.RANGE) {
                            final boolean exprx = expressionx;
                            if (!exprx) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                            } else {
                                 dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage, onError, campaignFormMeta.getCampaignFormElements());
                                isRangeandExpression = true;
                            }

                            if (value != null) {
                                try {
                                    double num = Double.parseDouble(value);
                                    if (num == Math.floor(num)) {
                                        value = String.valueOf((int) num); // whole number, no decimal
                                    } else {
                                        value = String.format("%.2f", num); // round to 2 decimal places
                                    }
                                } catch (NumberFormatException e) {
                                    // value is not a number, leave as-is
                                }
                            }
                            ControlTextEditFieldRange.setValue((ControlTextEditFieldRange) dynamicField, value);
                        } else if (type == CampaignFormElementType.DROPDOWN) {
                            dynamicField = createControlSpinnerFieldEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints, optionsValues, campaignFormElement.isImportant());
                            System.out.println("setting dropdownvalue ===" +  value +  "formelementid " + campaignFormElement.getId());
                            ControlSpinnerField.setValue((ControlSpinnerField) dynamicField, value);

                        } else if (type == CampaignFormElementType.DATE) {
                            dynamicField = createControlDateEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(), campaignFormElement.isImportant());


                            if (campaign != null) {
                                Date minDate = null;
                                Date maxDate = null;

                                String campaignPhase = "";
                                if (preCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                    minDate = campaign.getPreCampStartDate();
                                }else if (intraCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                    minDate = campaign.getStartDate();
                                }else if (postCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                                    minDate = campaign.getPostCampStartDate();
                                }

                                maxDate = expiryDate;


                                if (minDate != null) {
                                    ((ControlDateField) dynamicField).setMinDate(minDate);                            }
                                if (maxDate != null) {
                                    ((ControlDateField) dynamicField).setMaxDate(maxDate);
                                }
                            }
                            ControlDateField.setValue((ControlDateField) dynamicField, getDateValue(value));
                        } else if (type == CampaignFormElementType.PHONE) {
                            addMapValue();
                            dynamicField = CampaignFormDataFragmentUtils.createControlPhoneField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                            ControlPhoneField.setValue((ControlPhoneField) dynamicField, value);
                        } else if (type == CampaignFormElementType.TIME) {
                            dynamicField = CampaignFormDataFragmentUtils.createControlTimeEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, this.getFragmentManager(), campaignFormElement.isImportant());
                            ControlTimeField.setValue((ControlTimeField) dynamicField, value);
                        }else if (type == CampaignFormElementType.TEXTBOX) {
                            dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints);
                            ControlTextBoxField.setValue((ControlTextBoxField) dynamicField, value);
                        }  else if (type == CampaignFormElementType.IMAGE) {
                            dynamicField = CampaignFormDataFragmentUtils.createControlImageEditField(
                                    campaignFormElement,
                                    requireContext(),
                                    CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta),
                                    userHints,
                                    campaignFormElement.isImportant(),     // not hardcoded true
                                    cameraLauncher,
                                    cameraPermissionLauncher
                            );
                            ((ControlImageEditField) dynamicField).setCampaignFormDataUuid(record.getUuid());
                            ((ControlImageEditField) dynamicField).setRecord(record);
                            ControlImageEditField.setValue((ControlImageEditField) dynamicField, rawFormValuesMap.get(campaignFormElement.getId()));
                        }else {
                            if(campaignFormElement.getId().equalsIgnoreCase("villageCode")){
                                dynamicField = createControlTextEditFieldAllowStartingZero(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), false, campaignFormElement.isImportant());
                                ControlTextEditFieldAllowZeroInput.setValue((ControlTextEditFieldAllowZeroInput) dynamicField, value, "");
                            }else {
                                dynamicField = createControlTextEditField(campaignFormElement, requireContext(), getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
                                ControlTextEditField.setValue((ControlTextEditField) dynamicField, value, "");
                            }
                        }

                        fieldMap.put(campaignFormElement.getId(), dynamicField);
                        dynamicField.setShowCaption(true);
                        dynamicLayout.addView(dynamicField, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        Boolean finalIsRangeandExpression = isRangeandExpression;
                        final String dependingOnx = campaignFormElement.getDependingOn();

                        Boolean isdependingOn = false;
                        if (dependingOnx != null) {
                            isdependingOn = true;
                        }
                        Boolean finalIsdependingOn = isdependingOn;
                        if (type == CampaignFormElementType.RANGE && campaignFormElement.getId().equalsIgnoreCase("LotNo")) {
                            initialLotNo = formValuesMap.get(campaignFormElement.getId());
                            lotChangedValue = formValuesMap.get(campaignFormElement.getId());

                            errorSetterGlobal = (ControlTextEditFieldRange) dynamicField;
                            criteria.setCommunity(null);
                            List<CampaignFormData> lotchecker = DatabaseHelper.getCampaignFormDataDao().queryByCriteriaa(criteria, 0, 100);
                            List<String> listLotNo = new ArrayList();
                            List<String> listLotClusterNo = new ArrayList();

                            dynamicField.addValueChangedListener(field -> {

                                if (field.getValue() != null && !field.getValue().toString().isEmpty()) {
                                    if (initialLotNo != null  && !initialLotNo.isEmpty()) {
                                        double initialLotNoValueHelper = Double.parseDouble(initialLotNo);
                                        Long initialLotNoValueHelperUsed = (long) initialLotNoValueHelper;
                                        if ((Long.parseLong(field.getValue().toString()) - Long.parseLong(initialLotNo.toString()) != 0)
                                            // (Long.parseLong(field.getValue().toString()) -
                                            //     initialLotNoValueHelperUsed != 0)
                                        ) {
                                            lotNoChanged = true;
                                            lotChangedValue = field.getValue().toString();
                                        } else {
                                            lotNoChanged = false;
                                        }
                                    }

                                    if (lotNoChanged || lotClusterNoChanged) {
                                        CampaignFormDataEntry lotNo = new CampaignFormDataEntry();
                                        lotNo.setId("LotNo");
                                        lotNo.setValue(lotChangedValue);
                                        CampaignFormDataEntry lotClusterNo = new CampaignFormDataEntry();
                                        lotClusterNo.setId("LotClusterNo");
                                        lotClusterNo.setValue(lotClusterNoChangedValue);
                                        if (lotchecker.size() > 0) {
                                            for (CampaignFormData campaignFormDataData : lotchecker) {
                                                List<CampaignFormDataEntry> lotOwnSec = campaignFormDataData.getFormValues();

                                                for (CampaignFormDataEntry campaignFormDataEntry : lotOwnSec) {
                                                    if (campaignFormDataEntry.getId().equalsIgnoreCase(lotNo.getId().toString())) {
                                                        listLotNo.add(campaignFormDataEntry.getValue().toString());
                                                    }
                                                    if (campaignFormDataEntry.getId().equalsIgnoreCase(lotClusterNo.getId().toString())) {
                                                        listLotClusterNo.add(campaignFormDataEntry.getValue().toString());
                                                    }
                                                }
                                            }
                                        }

                                        for (String string : listLotClusterNo) {
                                            int index = listLotClusterNo.indexOf(string);
                                            if (listLotNo.size() > 0) {
                                                double lotChangedValueHelper = Double.parseDouble(lotChangedValue);
                                                Long lotChangedValueHelperUsed = (long) lotChangedValueHelper;

                                                double initialLotNoValueHelper = Double.parseDouble(initialLotNo);
                                                Long initialLotNoValueHelperUsed = (long) initialLotNoValueHelper;

                                                double initialLotClusterNoValueHelper = Double.parseDouble(initialLotClusterNo);
                                                Long initialLotClusterNoHelperUsed = (long) initialLotClusterNoValueHelper;
                                                if ((Long.parseLong(string) - Long.parseLong(lotClusterNo.getValue().toString()) == 0)
                                                        && (Long.parseLong(listLotNo.get(index))
                                                        - lotChangedValueHelperUsed == 0)
                                                ) {
                                                    if ((Long.parseLong(string) - initialLotClusterNoHelperUsed == 0)
                                                            && (Long.parseLong(listLotNo.get(index))
                                                            - initialLotNoValueHelperUsed == 0)) {
                                                        validateChecker = true;
                                                    } else {
                                                        validateChecker = false;
                                                    }
                                                }
                                            }
                                        }

                                        if (!validateChecker) {
                                            validateChecker = true;
                                            showValidationError("Lot Cluster Number Already Exist for this Lot Number");
                                            errorSetterGlobal.enableErrorState("Lot Cluster Number Already Exist for this Lot Number");
                                            errorSetterGlobal.setValidationCallback(() -> { return true;});
                                        } else {
                                            errorSetterGlobal.disableErrorState();
                                            errorSetterGlobal.setValidationCallback(() -> { return false;});
                                        }
                                    }
                                }
                            });
                        }

                        if (type == CampaignFormElementType.TIME) {
                            dynamicField.addValueChangedListener(e -> {
                                String timeValue = dynamicField.getValue().toString();
                                if (dynamicField.getValue().toString() != null && dynamicField.getValue().toString() != "") {
                                    dynamicField.setValue(timeValue);
                                }
                            });
                        }

                        if (type == CampaignFormElementType.PHONE && campaignFormElement.getId().equalsIgnoreCase("mobileNumber")) {
                            dynamicField.addValueChangedListener(e -> {
                                String values = dynamicField.getValue().toString();
                                if (dynamicField.getValue().toString() != null && dynamicField.getValue().toString() != "") {
                                    if (values != null && !values.matches("^[+]?[0-9]*$")) {
                                        dynamicField.setValue(values.replaceAll("[^0-9+]", ""));// Remove invalid characters
                                    }
                                }
                            });
                        }
                        if (type == CampaignFormElementType.EMAIL && campaignFormElement.getId().equalsIgnoreCase("email")) {
    //                        initialLotNo = formValuesMap.get(campaignFormElement.getId());
    //                        lotChangedValue = formValuesMap.get(campaignFormElement.getId());
                            dynamicField.addValueChangedListener(field -> {
                                String email = field.getValue() != null ? field.getValue().toString().trim() : "";

                                if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                    field.setTooltipText("Error : Enter a valid email address.");
                                    // Display validation error if email is invalid
    //                                Notification.show("Invalid email address. Please enter a valid email.", 3000, Notification.Position.MIDDLE);
    //                                field.clear(); // Optionally clear the invalid input
                                }
                            });
                        }

                        if (type == CampaignFormElementType.DROPDOWN && campaignFormElement.getId().equalsIgnoreCase("LotClusterNo")) {
                            initialLotClusterNo = formValuesMap.get(campaignFormElement.getId());
                            lotClusterNoChangedValue = formValuesMap.get(campaignFormElement.getId());
                            dynamicField.addValueChangedListener(field -> {
                                ControlSpinnerField errorSetter = (ControlSpinnerField) dynamicField;
                                ControlPropertyEditField errorSetterkyc = (ControlPropertyEditField) dynamicField;
    //                            criteria.setCommunity(record.getCommunity());
                                criteria.setCommunity(null);
                                List<CampaignFormData> lotchecker = DatabaseHelper.getCampaignFormDataDao().queryByCriteriaa(criteria, 0, 100);

                                List<String> listLotNo = new ArrayList();
                                List<String> listLotClusterNo = new ArrayList();

                                if (field.getValue() != null && !field.getValue().toString().isEmpty()) {

                                    if (initialLotClusterNo != null && !initialLotClusterNo.isEmpty()) {
                                        if ((Long.parseLong(field.getValue().toString()) - Long.parseLong(initialLotClusterNo.toString()) != 0)) {
                                            lotClusterNoChanged = true;
                                            lotClusterNoChangedValue = field.getValue().toString();
                                        }
                                    } else {
    //                                    if ((Long.parseLong(field.getValue().toString()) - Long.parseLong(initialLotClusterNo.toString()) != 0)) {
                                        lotClusterNoChanged = false;
    //                                    }
                                    }
                                    if (lotNoChanged || lotClusterNoChanged) {
                                        CampaignFormDataEntry lotNo = new CampaignFormDataEntry();
                                        lotNo.setId("LotNo");
                                        lotNo.setValue(lotChangedValue);
                                        CampaignFormDataEntry lotClusterNo = new CampaignFormDataEntry();
                                        lotClusterNo.setId("LotClusterNo");
                                        lotClusterNo.setValue(field.getValue().toString());
                                        if (lotchecker.size() > 0) {
                                            for (CampaignFormData campaignFormDataData : lotchecker) {
                                                List<CampaignFormDataEntry> lotOwnSec = campaignFormDataData.getFormValues();

                                                for (CampaignFormDataEntry campaignFormDataEntry : lotOwnSec) {
                                                    if (campaignFormDataEntry.getId().equalsIgnoreCase(lotNo.getId().toString())) {
                                                        listLotNo.add(campaignFormDataEntry.getValue().toString());
                                                        ;
                                                    }
                                                    if (campaignFormDataEntry.getId().equalsIgnoreCase(lotClusterNo.getId().toString())) {
                                                        listLotClusterNo.add(campaignFormDataEntry.getValue().toString());
                                                    }
                                                }
                                            }
                                        }

                                        for (String string : listLotClusterNo) {
                                            int index = listLotClusterNo.indexOf(string);
                                            if (listLotNo.size() > 0) {
                                                double lotChangedValueHelper = Double.parseDouble(lotChangedValue);
                                                Long lotChangedValueHelperUsed = (long) lotChangedValueHelper;

                                                double initialLotNoValueHelper = Double.parseDouble(initialLotNo);
                                                Long initialLotNoValueHelperUsed = (long) initialLotNoValueHelper;

                                                double initialLotClusterNoValueHelper = Double.parseDouble(initialLotClusterNo);
                                                Long initialLotClusterNoHelperUsed = (long) initialLotClusterNoValueHelper;
                                                if ((Long.parseLong(string) - Long.parseLong(lotClusterNo.getValue().toString()) == 0)
                                                        && (Long.parseLong(listLotNo.get(index))
                                                        - lotChangedValueHelperUsed == 0)
                                                ) {
                                                    if ((Long.parseLong(string) - initialLotClusterNoHelperUsed == 0)
                                                            && (Long.parseLong(listLotNo.get(index))
                                                            - initialLotNoValueHelperUsed == 0)) {
                                                        validateChecker = true;
                                                    } else {
                                                        validateChecker = false;
                                                    }
                                                }
                                            }
                                        }

                                        if (!validateChecker) {
                                            validateChecker = true;
                                            showValidationError("Lot Cluster Number Already Exist for this Lot Number");
                                            errorSetter.enableErrorState("Lot Cluster Number Already Exist for this Lot Number");
                                            errorSetterkyc.setValidationCallback(() -> { return true;});
                                        } else {
                                            errorSetter.disableErrorState();
                                            errorSetterkyc.setValidationCallback(() -> { return false;});
                                        }
                                    }
                                }
                                final Boolean isRangeandExpressionx = finalIsRangeandExpression;
                                Boolean okk = field.getFocusedChild() != null ? true : false;
                                final CampaignFormDataEntry campaignFormDataEntry = CampaignFormDataFragmentUtils.getOrCreateCampaignFormDataEntry(formValues, campaignFormElement);
                                campaignFormDataEntry.setValue(field.getValue());
                                if ((campaignFormElement.getExpression() == null && fieldMap.get(campaignFormElement.getId()) != null) || (okk && isRangeandExpressionx)) {
                                    for (CampaignFormDataEntry det : formValues) {
                                        if (det.getValue() != null) {
                                            if (det.getValue().toString().isEmpty()) {
                                                det.setValue(null);
                                            }
                                        }
                                    }
                                    expressionMap.forEach((formElement, controlPropertyField) ->
                                                                                    CampaignFormDataFragmentUtils.handleExpressionSec(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));
    //                                        CampaignFormDataFragmentUtils.handleExpressionSecEdit(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));

                                } else if (field.isFocused()) {
                                    System.out.println(">>>>>>>>>>>>>>>>>ONFOCUSSS>>>>>>>>>>>>>>>>>>>>" + fieldMap.get(campaignFormElement.getId()).getCaption());

                                }

                                if (finalIsdependingOn && isRangeandExpressionx) {
                                    field.setVisibility(View.GONE);
                                }

                            });

    //                        dynamicField.setValidationCallback(() -> {
    //                            boolean hasError = !lotClusterValid.get();
    //                            if (hasError) {
    //                                dynamicField.enableErrorState("Lot Cluster Number Already Exist for this Lot Number");
    //                            } else {
    //                                dynamicField.disableErrorState();
    //                            }
    //                            return hasError; // IMPORTANT: true => error; false => ok
    //                        });
                        } else {
                            dynamicField.addValueChangedListener(field -> {
                                final Boolean isRangeandExpressionx = finalIsRangeandExpression;
                                Boolean okk = field.getFocusedChild() != null ? true : false;
                                final CampaignFormDataEntry campaignFormDataEntry = CampaignFormDataFragmentUtils.getOrCreateCampaignFormDataEntry(formValues, campaignFormElement);
                                campaignFormDataEntry.setValue(field.getValue());
                                if ((campaignFormElement.getExpression() == null && fieldMap.get(campaignFormElement.getId()) != null) || (okk && isRangeandExpressionx)) {
                                    for (CampaignFormDataEntry det : formValues) {
                                        if (det.getValue() != null) {
                                            if (det.getValue().toString().isEmpty()) {
                                                det.setValue(null);
                                            }
                                        }
                                    }
    //                                expressionMap.forEach((formElement, controlPropertyField) ->
    //                                        CampaignFormDataFragmentUtils.handleExpressionSecEdit(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));

    //                                expressionMap.forEach((formElement, controlPropertyField) ->
    //                                                                                CampaignFormDataFragmentUtils.handleExpressionSec(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));
    ////


                                    boolean changedFieldIsRangeTrigger =
                                            campaignFormElement != null
                                                    && "range".equalsIgnoreCase(campaignFormElement.getType())
                                                    && rangeEffectIds.contains(campaignFormElement.getId());

                                    boolean userStillEditingThisField = field.hasFocus() || field.getFocusedChild() != null;

                                    // pass 1: evaluate non-range effects immediately
                                    expressionMap.forEach((formElement, controlPropertyField) -> {
                                        if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                        boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());

                                        if (!isRangeEffect) {
                                            CampaignFormDataFragmentUtils.handleExpressionSec(
                                                    expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                    controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                        }
                                    });

                                    // pass 2: evaluate range effects, but defer while typing in range trigger
                                    if (!(changedFieldIsRangeTrigger && userStillEditingThisField)) {
                                        expressionMap.forEach((formElement, controlPropertyField) -> {
                                            if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                            boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());
                                            boolean isDeferredRangeEffect = isRangeEffect && rangeEffectIds.contains(formElement.getId());

                                            if (isRangeEffect) {
                                                // range-only effect side
                                                CampaignFormDataFragmentUtils.handleExpressionSec(
                                                        expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                        controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                            }
                                        });
                                    }
                                } else if (field.isFocused()) {
                                    System.out.println(">>>>>>>>>>>>>>>>>ONFOCUSSS>>>>>>>>>>>>>>>>>>>>" + fieldMap.get(campaignFormElement.getId()).getCaption());

                                }
                                if (finalIsdependingOn && isRangeandExpressionx) {
                                    field.setVisibility(View.GONE);
                                }

                            });
                        }

                        if (type == CampaignFormElementType.NUMBER && campaignFormElement.getId().equalsIgnoreCase("villageCode")) {
                            dynamicField.addValueChangedListener(e -> {
                                if (dynamicField.getValue().toString() != null && dynamicField.getValue().toString() != "") {
                                    if (record != null && record.getCommunity() != null) {
                                        if (dynamicField.getValue().toString().length() == 3) {
                                            String inputValue = e.getValue().toString();
                                            if (inputValue.length() == 3) {
                                                handleVillageCodeValueGeneration(inputValue, dynamicField);
                                            }
                                        }
                                    }
                                }
                            });
                        }

                        if (type == CampaignFormElementType.TEXT && campaignFormElement.getId().equalsIgnoreCase("eTazkiraNo")) {
                            dynamicField.addValueChangedListener(e -> {
                                if (dynamicField.getValue().toString() != null) {
                                    if (dynamicField.getValue().toString().length() == 13) {
                                        String inputValue = e.getValue().toString().replace("-", "").replace(".", "");
    //                                    if (inputValue.length() == 13) {
                                        handleETazkiraNoFormatting(inputValue, dynamicField);
    //                                    }
                                    }
                                }
                            });
                        }


                        if(type == CampaignFormElementType.CHECKBOXBASIC){
                            dynamicField.addValueChangedListener(field -> {
                                final CampaignFormDataEntry campaignFormDataEntry = CampaignFormDataFragmentUtils.getOrCreateCampaignFormDataEntry(formValues, campaignFormElement);

                                // Get the current value from the field
                                Object fieldValue = field.getValue();
                                String serializedValue = "";

                                if (fieldValue instanceof List) {
                                    List<String> selectedList = (List<String>) fieldValue;
                                    if (!selectedList.isEmpty()) {
                                        // Serialize as JSON array-like string
                                        serializedValue = "[" + String.join(",", selectedList) + "]";

                                        System.out.println(serializedValue + "serializedValueserializedValueserializedValueserializedValue");
                                    }
                                }


                                campaignFormDataEntry.setValue(serializedValue);
                                System.out.println("DEBUG - Field " + campaignFormElement.getId() + " set to: " + serializedValue);
                            });
                        }

                        final String dependingOn = campaignFormElement.getDependingOn();
                        if (dependingOn != null) {
                            handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);
                        }

                        final String expressionString = campaignFormElement.getExpression();
                        if (expressionString != null) {
                            handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
                            expressionMap.put(campaignFormElement, dynamicField);
                        }
                    } else if (type == CampaignFormElementType.SECTION) {
                        if (campaignFormElement.getDependingOn() == null) {
                            ControlPropertyField dynamicField;
                            dynamicLayout.addView(new ImageView(requireContext(), null, R.style.FullHorizontalDividerStyle));
                        }
                    } else if (type == CampaignFormElementType.LABEL) {
                        if (campaignFormElement.getDependingOn() == null) {
                            TextView textView = new TextView(requireContext());
                            TextViewBindingAdapters.setHtmlValue(textView, CampaignFormDataFragmentUtils.getUserLanguageCaption(CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), campaignFormElement));
                            dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        }
                    } else if (type == CampaignFormElementType.LINEBREAK) {
                        if (campaignFormElement.getDependingOn() == null) {
                            TextView textView = new TextView(requireContext());
                            TextViewBindingAdapters.setHtmlValue(textView, "");
                            dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        }
                    }

                }

            }
            daywiseDayCount = countr;
        };
        buildDayFields.accept(daywise ? 1 : -1);
        if (daywise) {
            builtDays.add(1);
        }


        if (daywise) {

            if (daywiseDayCount > 0) {
                spec = mTabHost.newTabSpec("tab1").setIndicator("D1",//caption_1,
                                res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet1);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(0).getLayoutParams().width = 140;
            }
            if (daywiseDayCount > 1) {
                spec = mTabHost.newTabSpec("tab2").setIndicator("D2",//caption_1,
                                res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet2);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(1).getLayoutParams().width = 140;
            }
            if (daywiseDayCount > 2) {
                spec = mTabHost.newTabSpec("tab3").setIndicator("D3",//caption_1,
                                res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet3);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(2).getLayoutParams().width = 140;
            }
            if (daywiseDayCount > 3) {
                spec = mTabHost.newTabSpec("tab4").setIndicator("D4",//caption_1,
                                res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet4);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(3).getLayoutParams().width = 140;
            }

            if (daywiseDayCount == 6) { //>5 (four day form with summary
                spec = mTabHost.newTabSpec("tab5").setIndicator(" ",//caption_1,
                                res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet5);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(4).getLayoutParams().width = 140;
                mTabHost.getTabWidget().getChildAt(4).setVisibility(View.GONE);
            }

            if (daywiseDayCount > 4 && daywiseDayCount != 6) {
                spec = mTabHost.newTabSpec("tab5").setIndicator("D5",//caption_1,
                                res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet5);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(4).getLayoutParams().width = 140;
            }

            if (daywiseDayCount > 5 && daywiseDayCount != 6) {
                spec = mTabHost.newTabSpec("tab6").setIndicator("D6",//caption_1,
                                res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet6);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(5).getLayoutParams().width = 140;
            }

            if (daywiseDayCount > 6) {
                spec = mTabHost.newTabSpec("tab7").setIndicator("D7",//caption_1,
                                res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet7);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(6).getLayoutParams().width = 140;
            }

            if (daywiseDayCount > 7) {
                spec = mTabHost.newTabSpec("tab8").setIndicator("D8",//caption_1,
                                res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet8);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(7).getLayoutParams().width = 140;
//                mTabHost.getTabWidget().getChildAt(7).setVisibility(View.GONE);
            }

            if (daywiseDayCount > 8) {
                spec = mTabHost.newTabSpec("tab9").setIndicator("D9",//caption_1,
                                res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet9);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(8).getLayoutParams().width = 140;
//                mTabHost.getTabWidget().getChildAt(8).setVisibility(View.GONE);
            }


            if (daywiseDayCount > 9) {
                spec = mTabHost.newTabSpec("tab10").setIndicator("D10",//caption_1,
                                res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet10);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(9).getLayoutParams().width = 140;
//                mTabHost.getTabWidget().getChildAt(8).setVisibility(View.GONE);
            }

            if (daywiseDayCount > 10) {
                spec = mTabHost.newTabSpec("tab11").setIndicator("D11",//caption_1,
                                res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet11);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(10).getLayoutParams().width = 160;
                mTabHost.getTabWidget().getChildAt(10).setVisibility(View.GONE);
            }



           /*  if (dayy > 5) {
                spec = mTabHost.newTabSpec("tab6").setIndicator("D6",//caption_1,
                        res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet6);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(5).getLayoutParams().width = 140;
            }
            if (dayy > 6) {
                spec = mTabHost.newTabSpec("tab7").setIndicator("D7",//caption_1,
                        res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet7);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(6).getLayoutParams().width = 140;
            }
            if (dayy > 7) {
                spec = mTabHost.newTabSpec("tab8").setIndicator("D8",//caption_1,
                        res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet8);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(7).getLayoutParams().width = 140;
            }

*/

        }
        if (daywise) {
            // day sections without a visible tab cannot be opened from the tab bar, so build them up front
            int tabbedDays = mTabHost.getTabWidget().getChildCount();
            for (int day = 1; day <= daywiseDayCount; day++) {
                boolean reachable = day <= tabbedDays
                        && mTabHost.getTabWidget().getChildAt(day - 1).getVisibility() == View.VISIBLE;
                if (!reachable && builtDays.add(day)) {
                    buildDayFields.accept(day);
                }
            }
            mTabHost.setOnTabChangedListener(tabId -> {
                int currentDay = mTabHost.getCurrentTab() + 1;
                for (int day = 1; day <= currentDay && day <= daywiseDayCount; day++) {
                    if (builtDays.add(day)) {
                        buildDayFields.accept(day);
                    }
                }
            });
            setupTabScrolling(view);
        }
        return view;
    }


    @Override
    protected String getSubHeadingTitle() {
        if (record != null && record.getCampaign().getName() != null) {
            return record.getCampaign().getName() + " | " + record.getCampaignFormMeta().getFormName();
        }
        return super.getSubHeadingTitle();
    }

    private void setupTabScrolling(View view) {
        try {
            HorizontalScrollView tabsScroll = view.findViewById(R.id.tabs_scroll);
            ImageView leftArrow = view.findViewById(R.id.tabs_left_arrow);
            ImageView rightArrow = view.findViewById(R.id.tabs_right_arrow);

            if (tabsScroll == null || leftArrow == null || rightArrow == null) {
                return;
            }

            // Post the initial visibility update to ensure layout is measured
            tabsScroll.post(() -> updateArrowVisibility(tabsScroll, leftArrow, rightArrow));

            tabsScroll.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                updateArrowVisibility(tabsScroll, leftArrow, rightArrow);
            });

            leftArrow.setOnClickListener(v -> scrollTabs(tabsScroll, leftArrow, rightArrow, -200));
            rightArrow.setOnClickListener(v -> scrollTabs(tabsScroll, leftArrow, rightArrow, 200));
        } catch (Exception e) {
            Log.e("TabScrolling", "Error setting up tab scrolling", e);
        }
    }

    private void updateArrowVisibility(HorizontalScrollView scrollView,
                                       ImageView leftArrow,
                                       ImageView rightArrow) {

        if (scrollView.getChildCount() == 0) {
            return;
        }

        int scrollX = scrollView.getScrollX();
        int maxScroll = Math.max(0, scrollView.getChildAt(0).getWidth() - scrollView.getWidth());

        boolean canScrollRight = scrollX < maxScroll;
        boolean hasScrolled = scrollX > 0;

        // Right arrow
        rightArrow.setVisibility(canScrollRight ? View.VISIBLE : View.GONE);

        // Left arrow:
        // Show if we've scrolled OR if the right arrow is no longer visible.
        leftArrow.setVisibility((hasScrolled || !canScrollRight) ? View.VISIBLE : View.GONE);
    }

    private void scrollTabs(HorizontalScrollView scrollView,
                            ImageView leftArrow,
                            ImageView rightArrow,
                            int distance) {

        if (distance > 0) {
            // User clicked the right arrow
            leftArrow.setVisibility(View.VISIBLE);
        }

        scrollView.smoothScrollBy(distance, 0);

        scrollView.postDelayed(() ->
                updateArrowVisibility(scrollView, leftArrow, rightArrow), 300);
    }


    private List<String> parseCheckboxValue(String value) {
        List<String> selectedKeys = new ArrayList<>();

        if (value != null && !value.trim().isEmpty()) {
            String str = value.trim();

            // Remove brackets if present
            if (str.startsWith("[") && str.endsWith("]")) {
                str = str.substring(1, str.length() - 1);
            }

            // Remove quotes if present
            str = str.replace("\"", "");

            // Split by comma
            String[] parts = str.split("\\s*,\\s*");

            for (String part : parts) {
                String cleanPart = part.trim();
                if (!cleanPart.isEmpty()) {
                    selectedKeys.add(cleanPart);
                }
            }
        }

        return selectedKeys;
    }


    private void handleETazkiraNoFormatting(String inputValue, ControlPropertyField dynamicField) {
        String value = inputValue;

        String formattedTazkira = value.substring(0, 4) + "-"
                + value.substring(4, 8) + "-"
                + value.substring(8);

        dynamicField.setValue(formattedTazkira);

    }




    private void handleVillageCodeValueGeneration(String inputValue, ControlPropertyField dynamicField) {
        String cCode = record.getCommunity().getExternalid().toString();
        switch (cCode.length()) {
            case 4:
                dynamicField.setValue(cCode + "000" + inputValue);
                break;
            case 5:
                dynamicField.setValue(cCode + "00" + inputValue);
                break;
            case 6:
                dynamicField.setValue(cCode + "0" + inputValue);
                break;
            case 7:
                dynamicField.setValue(cCode + "" + inputValue);
                break;
            case 8:
                // when the length of the ccode is 8 what we're doing here is to
                //delete the first charcter and return a new bvalue
                String firstCharacterDelete = inputValue.substring(1);

                dynamicField.setValue(cCode + "" + firstCharacterDelete);
                break;
        }
    }


    public long getMilliFromDate(String dateFormat) {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy h:mm:ss a");

        try {
            date = formatter.parse(dateFormat);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date.getTime();
    }



    protected Date getDateValue(String input) {
        if (StringUtils.isEmpty(input)) {
            return null;
        }

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            Date parsedDate = dateFormat.parse(input);

            // Clear time components
            Calendar cal = Calendar.getInstance();
            cal.setTime(parsedDate);
            return cal.getTime();
        } catch (ParseException e) {
//            Log.e(getClass().getName(), "Error parsing date: " + input, e);
//            return null;
            try{
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date parsedDate = dateFormat.parse(input);

                // Clear time components
                Calendar cal = Calendar.getInstance();
                cal.setTime(parsedDate);
                return cal.getTime();
            }catch(Exception ee){

                try{
                    String normalizedDateString = normalizeRawDateString(input); // this gives "03-08-2025"
                    if (normalizedDateString != null) {
                        SimpleDateFormat fallbackFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
                        return fallbackFormat.parse(normalizedDateString);
                    }
                }catch(Exception eex){
                    Log.e(getClass().getName(), "Error parsing date: " + input, eex);

                }
            }

            return null;

        }
    }

    public static String normalizeRawDateString(String rawDateStr) {
        try {
            // First parse the raw string
            SimpleDateFormat inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
            Date date = inputFormat.parse(rawDateStr);

            // Then format it to dd-MM-yyyy
            return formatDateToDdMMyyyy(date);
        } catch (ParseException e) {
            Log.e("DateParse", "Could not parse date: " + rawDateStr, e);
            return null;
        }
    }
    public static String formatDateToDdMMyyyy(Date date) {
        if (date == null) return null;

        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
        return formatter.format(date);
    }




//    protected Date getDateValue(String input) {
//        if (StringUtils.isEmpty(input)) {
//            return null;
//        }
//
//        try {
//            try {
//                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
//                Date parsedDate = dateFormat.parse(input);
//
//                // Add current time to date-only input
//                Calendar cal = Calendar.getInstance();
//                Calendar parsedCal = Calendar.getInstance();
//                parsedCal.setTime(parsedDate);
//
//                cal.set(Calendar.YEAR, parsedCal.get(Calendar.YEAR));
//                cal.set(Calendar.MONTH, parsedCal.get(Calendar.MONTH));
//                cal.set(Calendar.DAY_OF_MONTH, parsedCal.get(Calendar.DAY_OF_MONTH));
//
//                return cal.getTime();
//            } catch (ParseException e1) {
//                // Continue to next format
//
//                try {
//                    SimpleDateFormat dateTimeFormatAmPm = new SimpleDateFormat("dd-MM-yyyy h:mm:ss a", Locale.getDefault());
//                    return dateTimeFormatAmPm.parse(input);
//                } catch (ParseException e3) {
//                    // Continue to next format
//
//                    try {
//                        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
//                        return dateTimeFormat.parse(input);
//                    } catch (ParseException e2) {
//                        // Continue to next format
//
//                        try {
//                            SimpleDateFormat monthNameFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.US);
//                            return monthNameFormat.parse(input);
//                        } catch (ParseException e4) {
//                            // Continue to next format
//
//                            try {
//                                SimpleDateFormat rfc1123Format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
//                                return rfc1123Format.parse(input);
//                            } catch (ParseException e5) {
//                                Log.e(getClass().getName(), "Error parsing date: " + input, e5);
//                                return null;
//                            }
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            Log.e(getClass().getName(), "Error parsing date: " + input, e);
//            return null;
//        }
//    }


//    protected Date getDateValue(String input) {
//        if (StringUtils.isEmpty(input)) {
//            return null;
//        }
//
//        try {
//            // Try parsing as full datetime (dd-MM-yyyy HH:mm:ss)
//            try {
//                SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
//                return dateTimeFormat.parse(input);
//            } catch (ParseException e1) {
//                // Try parsing as full datetime with AM/PM (dd-MM-yyyy h:mm:ss a)
//                try {
//                    SimpleDateFormat dateTimeFormatAmPm = new SimpleDateFormat("dd-MM-yyyy h:mm:ss a", Locale.getDefault());
//                    return dateTimeFormatAmPm.parse(input);
//                } catch (ParseException e2) {
//                    // Try parsing as "MMM dd, yyyy hh:mm:ss a" (e.g. "Jun 21, 2025 12:00:00 AM")
//                    try {
//                        SimpleDateFormat monthNameFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault());
//                        return monthNameFormat.parse(input);
//                    } catch (ParseException e3) {
//                        // If that fails, try date-only format (dd-MM-yyyy)
//                        try {
//                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
//                            Date parsedDate = dateFormat.parse(input);
//
//                            // Add current time to the date-only input
//                            Calendar cal = Calendar.getInstance();
//                            Calendar parsedCal = Calendar.getInstance();
//                            parsedCal.setTime(parsedDate);
//
//                            cal.set(Calendar.YEAR, parsedCal.get(Calendar.YEAR));
//                            cal.set(Calendar.MONTH, parsedCal.get(Calendar.MONTH));
//                            cal.set(Calendar.DAY_OF_MONTH, parsedCal.get(Calendar.DAY_OF_MONTH));
//                            // Keep current time components
//
//                            return cal.getTime();
//                        } catch (ParseException e4) {
//                            Log.e(getClass().getName(), "Error parsing datee4: " + input, e4);
//                            return null;
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            Log.e(getClass().getName(), "Error parsing date: " + input, e);
//            return null;
//        }
//    }
//    private Date getDateValue(String input) {
//        if (StringUtils.isEmpty(input)) {
//            return null;
//        }
//
//        // getMilliFromDate(input);
//
//        dateFormat = new SimpleDateFormat("MMM dd, yyyy h:mm:ss a");
//        //   Date date = DateHelper.parseDate(input, dateFormat);
//        Calendar dateCalendar = Calendar.getInstance();
//        Calendar cachedCalendar = Calendar.getInstance();
//
//        //    System.err.println(date);
//        Date date = new Date(getMilliFromDate(input));
//        dateCalendar.setTime(date);
//
//        cachedCalendar.set(Calendar.YEAR, dateCalendar.get(Calendar.YEAR));
//        cachedCalendar.set(Calendar.MONTH, dateCalendar.get(Calendar.MONTH));
//        cachedCalendar.set(Calendar.DAY_OF_MONTH, dateCalendar.get(Calendar.DAY_OF_MONTH));
//        date = cachedCalendar.getTime();
//
//
//        return date;
//    }
//

    @Override
    public int getEditLayout() {
        return R.layout.fragment_campaign_data_edit_layout;
    }

    @Override
    public CampaignFormData getPrimaryData() {
        return record;
    }

    @Override
    protected void prepareFragmentData() {
        record = getActivityRootData();

        initialCampaigns = DataUtils.toItems(DatabaseHelper.getCampaignDao().queryActiveForAll());

        initialAreas = InfrastructureDaoHelper.loadAreas();
        initialRegions = InfrastructureDaoHelper.loadRegionsByServerCountry();
//        initialDistricts = InfrastructureDaoHelper.loadDistricts(record.getRegion());

         List<Item> districtItemList = InfrastructureDaoHelper.loadDistricts(record.getRegion());
        districtItemList.removeIf(d -> d == null || d.getValue() == null || d.getValue().toString().trim().isEmpty());

        if (ConfigProvider.getUser().getUserRoles().contains(UserRole.SURVEILLANCE_OFFICER)) {
            User user = ConfigProvider.getUser();
            List<String> userDistrictUuids = new ArrayList<>();

            if (user.getRegion() != null) {
                userDistrictUuids = DatabaseHelper.getDistrictDao().getByRegion(user.getRegion()).stream()
                        .map(District::getUuid)
                        .collect(Collectors.toList());
            } else if (user.getDistrict() != null) {
                userDistrictUuids.add(user.getDistrict().getUuid());
            }
            //Fetch population data for this campaign & selected = true
            if (!userDistrictUuids.isEmpty()) {
                List<PopulationData> popDataSelectedDistrict = DatabaseHelper.getPopulationDataDao()
                        .getSelectedDistrictsByMultipleUuids(userDistrictUuids, record.getCampaign().getUuid());

                //Collect UUIDs of districts that actually exist in the population data
                Set<String> selectedDistrictUuids = popDataSelectedDistrict.stream()
                        .map(PopulationData::getDistrict_id)
                        .collect(Collectors.toSet());

                initialDistricts = districtItemList.stream()
                        .filter(item -> item.getValue() instanceof District && selectedDistrictUuids.contains(((District) item.getValue()).getUuid()))
                        .collect(Collectors.toList());
            } else {
                initialDistricts = new ArrayList<>();
            }
            for(Item item : initialDistricts){
                System.out.println(((District) item.getValue()).getUuid() +  "UUIDS OF INITIAL DISTRICTS ---------------------------");
            }


        }else {

            System.out.println(ConfigProvider.getUser().getDistrict().getUuid() + "User role is not  surv Officer --------------------" + record.getCampaign().getUuid());

            //Fetch population data for this campaign & selected = true
            List<PopulationData> popDataSelectedDistrict = DatabaseHelper.getPopulationDataDao()
                    .getSelectedDistrictByUsersDistrict(ConfigProvider.getUser().getDistrict().getUuid(), record.getCampaign().getUuid());

            //Collect UUIDs of districts that actually exist in the population data
            Set<String> selectedDistrictUuids = popDataSelectedDistrict.stream()
                    .map(PopulationData::getDistrict_id)
                    .collect(Collectors.toSet());

            List<Item> initialDistrictsFound = districtItemList.stream()
                    .filter(item -> selectedDistrictUuids.contains(item.getValue().toString()))
                    .collect(Collectors.toList());

            initialDistricts = initialDistrictsFound;

        }

        if (ConfigProvider.getUser().getUserRoles().contains(UserRole.SURVEILLANCE_OFFICER)) {

        }else{
            initialCommunities = InfrastructureDaoHelper.loadCommunities(record.getDistrict());

            System.out.println("initialCommunities size " +  initialCommunities.size());

        }
    }

    @Override
    protected void onLayoutBinding(FragmentCampaignDataEditLayoutBinding contentBinding) {
//        System.out.println(record.getArea() +"ecord.getarea().getArea()ecord.getRegion().getRegionArea()");

        record.setArea(record.getArea() != null ? record.getArea() : record.getRegion().getArea());
        contentBinding.setData(record);

        Item campaignItem = record.getCampaign() != null ? DataUtils.toItem(record.getCampaign()) : null;
        if (campaignItem != null && !initialCampaigns.contains(campaignItem)) {
            initialCampaigns.add(campaignItem);
        }
        contentBinding.campaignFormDataCampaign.initializeSpinner(initialCampaigns, record.getCampaign());
        contentBinding.campaignFormDataCampaign.setEnabled(false);

        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        try {
            // Initialize the date field with fragment manager and initial date
            if (record.getCampaign() != null) {
                final CampaignFormMeta campaignFormMeta = DatabaseHelper.getCampaignFormMetaDao().queryForId(record.getCampaignFormMeta().getId());
                Date expiryDate = DatabaseHelper.getCampaignFormMetaWithExpDao().getCampaignFormExpiryDateByCampaignIdAndFormId(record.getCampaign().getUuid(), campaignFormMeta.getUuid());
                Date minDate = null;
                Date maxDate = null;

                if (preCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                    minDate = record.getCampaign().getPreCampStartDate();
                }else if (intraCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                    minDate = record.getCampaign().getStartDate();
                }else if (postCampaignsCategories.contains(campaignFormMeta.getFormCategory())) {
                    minDate = record.getCampaign().getPostCampStartDate();
                }
                maxDate = expiryDate;

                if (minDate != null) {
                    ((de.symeda.sormas.app.component.controls.ControlDateField) contentBinding.campaignFormDataFormDate).setMinDate(minDate);
                }
                if (maxDate != null) {
                    ((de.symeda.sormas.app.component.controls.ControlDateField) contentBinding.campaignFormDataFormDate).setMaxDate(maxDate);
                }
            }
            contentBinding.campaignFormDataFormDate.initializeDateField(getFragmentManager(), getDateValue(date.toString()));
            contentBinding.campaignFormDataFormDate.setValue(getDateValue(date.toString()));
            // Verify the value was set correctly
            Date retrievedValue = contentBinding.campaignFormDataFormDate.getValue();
            System.out.println("Date field value after initialization: " + retrievedValue);

            // Only disable after successful initialization
            contentBinding.campaignFormDataFormDate.setFieldValue(getDateValue(date.toString()));
            System.out.println("Date field initialization completed successfully" +  contentBinding.campaignFormDataFormDate.getValue());

        } catch (Exception exception) {
            exception.printStackTrace();
            System.out.println("Date field Error during date field initialization: " + exception.getMessage());

            // Fallback: try basic initialization
            try {
                contentBinding.campaignFormDataFormDate.initializeDateField(getFragmentManager());
                contentBinding.campaignFormDataFormDate.setFieldValue(date);
                contentBinding.campaignFormDataFormDate.setEnabled(false);
                System.out.println("Date field Fallback initialization completed");
            } catch (Exception fallbackException) {
                System.out.println("Date field Fallback initialization also failed: " + fallbackException.getMessage());
            }
        }

        InfrastructureDaoHelper.initializeRegionAreaFieldsByGeographyLevel(
                contentBinding.campaignFormDataArea,
                initialAreas,
                record.getArea(),
                contentBinding.campaignFormDataRegion,
                initialRegions,
                record.getRegion(),
                contentBinding.campaignFormDataDistrict,
                initialDistricts,
                record.getDistrict(),
                contentBinding.campaignFormDataCommunity,
                initialCommunities,
                record.getCommunity(),
                record.getCampaignFormMeta().getGeographylevel(), true);
    }

    @Override
    protected void onAfterLayoutBinding(FragmentCampaignDataEditLayoutBinding contentBinding) {
        super.onAfterLayoutBinding(contentBinding);

        contentBinding.campaignFormDataFormDate.initializeDateField(getFragmentManager());

        User user = ConfigProvider.getUser();

        if (user.getRegion() != null) {
            contentBinding.campaignFormDataArea.setEnabled(false);
            contentBinding.campaignFormDataRegion.setEnabled(false);
        }
        if (user.getDistrict() != null) {
            contentBinding.campaignFormDataDistrict.setEnabled(false);
        }
        if (user.getCommunity() != null) {
            contentBinding.campaignFormDataCommunity.setEnabled(false);
        }
    }

    private void showValidationError(String message) {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
//                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setTextColor(Color.RED);
        });

        dialog.show();
    }

    public void validateForSave(Context context) throws ValidationException {
        if (!daywise) {
            // Non-daywise form → validate everything
            FragmentValidator.validate(context, getContentBinding());
            return;
        }

        // Day-wise form → restricted validation
        validateDayWise(context);
    }

//    private void validateDayWise(Context context) throws ValidationException {
//
//        int currentDay = mTabHost.getCurrentTab() + 1;
//        ValidationErrorInfo errorInfo = new ValidationErrorInfo(context);
//
//        // Always validate Day-1
//        ViewGroup day1 = getDayContainer(1);
//        FragmentValidator.validatePropertyEditFields(day1, errorInfo);
//
//        // Validate current day if different
//        if (currentDay != 1) {
//            System.out.println("NOTDAYONEVALIDATIONNNNNNNNNNNNNNNNNNNNNNNNNNNNEDITTTTTTTTTTTTTTT");
//            ViewGroup current = getDayContainer(currentDay);
//            System.out.println(current.getChildCount());
//            FragmentValidator.validatePropertyEditFields(current, errorInfo);
//        }
//
//        if (errorInfo.hasError()) {
//            throw new ValidationException(errorInfo.toString());
//        }
//    }

    private void validateDayWise(Context context) throws ValidationException {
        int currentDay = mTabHost.getCurrentTab() + 1;
        ValidationErrorInfo errorInfo = new ValidationErrorInfo(context);

        // Validate all days from day 1 up to and including the current day
        // e.g. currentDay = 3 -> validates day1, day2, day3
        System.out.println("CURRENTDAYYYYYYYYYYYYYYYYYYYYYYYYYY " + currentDay);
        for (int day = 1; day <= currentDay; day++) {
            System.out.println("VALIDATING DAY: " + day);
            ViewGroup dayContainer = getDayContainer(day);
            if (dayContainer != null) {
                System.out.println("Day " + day + " child count: " + dayContainer.getChildCount());
                FragmentValidator.validatePropertyEditFields(dayContainer, errorInfo);
            } else {
                System.out.println("WARNING: No container found for day " + day);
            }
        }

        if (errorInfo.hasError()) {
            throw new ValidationException(errorInfo.toString());
        }
    }

    private ViewGroup getDayContainer(int day) {
        switch (day) {
            case 1: return mTabHost.findViewById(R.id.tabSheet1);
            case 2: return mTabHost.findViewById(R.id.tabSheet2);
            case 3: return mTabHost.findViewById(R.id.tabSheet3);
            case 4: return mTabHost.findViewById(R.id.tabSheet4);
            case 5: return mTabHost.findViewById(R.id.tabSheet5);
            case 6: return mTabHost.findViewById(R.id.tabSheet6);
            case 7: return mTabHost.findViewById(R.id.tabSheet7);
            case 8: return mTabHost.findViewById(R.id.tabSheet8);
            default: return null;
        }
    }

}