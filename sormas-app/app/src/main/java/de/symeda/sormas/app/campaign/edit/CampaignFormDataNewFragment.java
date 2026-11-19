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

import static androidx.databinding.DataBindingUtil.setContentView;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

import de.symeda.sormas.api.MapperUtil;
import de.symeda.sormas.api.campaign.form.CampaignFormTranslations;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.user.UserRole;
import de.symeda.sormas.api.utils.ValidationException;
import de.symeda.sormas.app.BaseEditFragment;
import de.symeda.sormas.app.backend.campaign.data.CampaignFormData;
import de.symeda.sormas.app.backend.campaign.form.CampaignFormMeta;
import de.symeda.sormas.app.backend.common.DatabaseHelper;
import de.symeda.sormas.app.backend.config.ConfigProvider;
import de.symeda.sormas.app.backend.region.Community;
import de.symeda.sormas.app.backend.region.District;
import de.symeda.sormas.app.backend.region.PopulationData;
import de.symeda.sormas.app.backend.user.User;
import de.symeda.sormas.app.campaign.CampaignFormDataFragmentUtils;
import de.symeda.sormas.app.component.controls.ControlCameraImageField;
import de.symeda.sormas.app.component.controls.ControlImageEditField;
import de.symeda.sormas.app.component.controls.ControlPropertyEditField;
import de.symeda.sormas.app.component.controls.ControlPropertyField;
import de.symeda.sormas.app.component.validation.FragmentValidator;
import de.symeda.sormas.app.component.validation.ValidationErrorInfo;
import de.symeda.sormas.app.util.DataUtils;
import de.symeda.sormas.app.util.InfrastructureDaoHelper;
import de.symeda.sormas.app.util.TextViewBindingAdapters;
import de.symeda.sormas.api.campaign.data.CampaignFormDataEntry;
import de.symeda.sormas.api.campaign.form.CampaignFormElement;
import de.symeda.sormas.api.campaign.form.CampaignFormElementOptions;
import de.symeda.sormas.api.campaign.form.CampaignFormElementType;
import de.symeda.sormas.app.R;
import de.symeda.sormas.app.component.Item;
import de.symeda.sormas.app.databinding.FragmentCampaignDataNewLayoutBinding;

import static de.symeda.sormas.app.campaign.CampaignFormDataFragmentUtils.handleDependingOn;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class CampaignFormDataNewFragment extends BaseEditFragment<FragmentCampaignDataNewLayoutBinding, CampaignFormData, CampaignFormData> {

    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private CampaignFormData record;
    private List<Item> initialCampaigns;
    private List<Item> initialAreas;
    private List<Item> initialRegions;
    private List<Item> initialDistricts;
    private List<Item> initialCommunities;

    private Date presentDate;

    private int min = 9;
    private int max = 9;

    private Map<String, String> optionsValues;
    private List<String> constraints;

    private boolean onError;
    private String errorMessage = "";

    // private List<CampaignFormTranslations> translationsOpt;
    private Map<String, String> userOptTranslations = null;

    private String currentCountryCode;
    private TextView countryLabel;
    private TextView helperText;
    private String country = "Afghanistan";
    private final Map<String, CountryDetails> mapvalue = new HashMap<>();
    List<String> preCampaignsCategories = List.of("FLW", "MODALITY_PRE", "TRAINING", "MONITORING");
    List<String> intraCampaignsCategories = List.of("ICM", "ADMIN", "EAG-ICM", "EAG-ADMIN");
    List<String> postCampaignsCategories = List.of("PCA", "FMS", "LQAS", "EAG-PCA", "EAG-FMS", "EAG-LQAS", "MODALITY_POST", "VALIDATION");

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    private ControlImageEditField currentImageField = null;

    private TabHost mTabHost;

    private boolean daywise;

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
        return newInstance(CampaignFormDataNewFragment.class, null, activityRootData);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register the camera launcher ONCE, before the view is created.
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Forward the result to the currently active image field
                    if (currentImageField != null) {
                        currentImageField.handleCameraResult(result.getResultCode());
                        currentImageField = null; // clear after use
                    }
                }
        );

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted && currentImageField != null) {
                        currentImageField.launchCamera();
                    } else if (!granted) {
                        Toast.makeText(requireContext(),
                                "Camera permission is required to capture an image",
                                Toast.LENGTH_SHORT).show();
                    }
                });
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
        Date expiryDate = DatabaseHelper.getCampaignFormMetaWithExpDao().getCampaignFormExpiryDateByCampaignIdAndFormId(record.getCampaign().getUuid(), campaignFormMeta.getUuid());
        LocalDate expiryLocalDate = expiryDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();


        record.setFormValues(new ArrayList<>());

        final List<CampaignFormDataEntry> formValues = record.getFormValues();
        final List<CampaignFormTranslations> translationsOpt = record.getCampaignFormMeta().getCampaignFormTranslations();

        final Map<String, ControlPropertyField> fieldMap = new HashMap<>();
        final Map<CampaignFormElement, ControlPropertyField> expressionMap = new LinkedHashMap<>();

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


        Resources res = getResources();
        mTabHost = (TabHost) view.findViewById(R.id.tabhostxxx);
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


    //                optionsValues = campaignFormElement.getOptions().stream().collect(Collectors.toMap(MapperUtil::getKey, MapperUtil::getCaption));  // .collect(Collectors.toList());

                    optionsValues = campaignFormElement.getOptions().stream().sorted(Comparator.comparing(
                            o -> {
                                if (o.getOrder() == null || o.getOrder().isEmpty()) {
                                    return Integer.MAX_VALUE;
                                }
                                try {
                                    return Integer.parseInt(o.getOrder());
                                } catch (NumberFormatException e) {
                                    return Integer.MAX_VALUE;
                                }
                            })).collect(Collectors.toMap(MapperUtil::getKey, MapperUtil::getCaption, (e1, e2) -> e1, LinkedHashMap::new));
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
    //            System.out.println(campaignFormElement.getErrormessage() + ")))))))))))))))))))))(((((((((((((((===");

                errorMessage = campaignFormElement.getErrormessage() != null ? campaignFormElement.getErrormessage() : "";

                onError = campaignFormElement.isWarnonerror();
                Boolean isRangeandExpression = false;

                if (daywise) {
                    if (type == CampaignFormElementType.DAYWISE) {
                        countr++;
                    } else if (countr == 1 && onlyDay == 1) {
                        final LinearLayout dynamicLayout = mTabHost.findViewById(R.id.tabSheet1);
                        if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL && type != CampaignFormElementType.LINEBREAK) {
                            ControlPropertyField dynamicField;
                            boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                            if (type == CampaignFormElementType.YES_NO) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlYesNoUnknownField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.TEXTBOX) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.CHECKBOXBASIC ) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlMultiSelectCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), optionsValues, campaignFormElement.isImportant());
                            }  else if (type == CampaignFormElementType.NUMBER) {

                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints, true, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.DECIMAL) {
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints, true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                    isRangeandExpression = true;
                                }
                            }  else if (type == CampaignFormElementType.EMAIL) {

                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                            }else if (type == CampaignFormElementType.RANGE) {
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage, onError, campaignFormMeta.getCampaignFormElements());
                                    isRangeandExpression = true;
                                }
    //campaignFormElement
                            } else if (type == CampaignFormElementType.DROPDOWN) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlSpinnerFieldEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints, optionsValues, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.DATE) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlDateEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(), campaignFormElement.isImportant());
                                if (record.getCampaign() != null) {
                                    de.symeda.sormas.app.backend.campaign.Campaign campaign = record.getCampaign();
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
                                        ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMinDate(minDate);
                                    }
                                    if (maxDate != null) {
                                        ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMaxDate(maxDate);
                                    }
                                }
                            } else if (type == CampaignFormElementType.IMAGE) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlImageEditField(
                                        campaignFormElement,
                                        requireContext(),
                                        CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta),
                                        userHints,
                                        campaignFormElement.isImportant(),     // not hardcoded true
                                        cameraLauncher,
                                        cameraPermissionLauncher
                                );

                                if (dynamicField instanceof ControlImageEditField) {
                                    ((ControlImageEditField) dynamicField).setCampaignFormDataUuid(record.getUuid());
                                    ((ControlImageEditField) dynamicField).setRecord(record);
                                    ((ControlImageEditField) dynamicField).setOnCaptureRequestListener(
                                            f -> currentImageField = f);
                                }
                            } else {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
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


                            if (type == CampaignFormElementType.DROPDOWN && "lotClusterNo" == campaignFormElement.getId()) {
                                System.out.println("++++++++_______________222__________lotClusterNo");

                            } else {
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
                            }

                            Object defaultValue = campaignFormElement.getDefaultvalue();
                            formValues.add(new CampaignFormDataEntry(campaignFormElement.getId(), defaultValue == null ? null : defaultValue));
                            dynamicField.setValue(defaultValue == null ? null : defaultValue);

                            final String dependingOn = campaignFormElement.getDependingOn();
                            if (dependingOn != null) {
                                handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);
                            }

                            final String expressionString = campaignFormElement.getExpression();
                            if (expressionString != null) {
                                CampaignFormDataFragmentUtils.handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
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
                        }  else if (type == CampaignFormElementType.LINEBREAK) {
    //                        System.out.println("Linke Break detetected in formelements ----------------");
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, "");
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        }
                    } else if (countr == 2 && onlyDay == 2) {
                        final LinearLayout dynamicLayout = mTabHost.findViewById(R.id.tabSheet2);
                        if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL && type != CampaignFormElementType.LINEBREAK) {
                            ControlPropertyField dynamicField;
                            boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                            if (type == CampaignFormElementType.YES_NO) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlYesNoUnknownField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.TEXTBOX) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.CHECKBOXBASIC ) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlMultiSelectCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), optionsValues, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.NUMBER) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                            }else if (type == CampaignFormElementType.DECIMAL) {
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                    isRangeandExpression = true;
                                }
                            } else if (type == CampaignFormElementType.EMAIL) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.RANGE) {
                                final boolean exprx = expressionx;

                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage, onError, campaignFormMeta.getCampaignFormElements());
                                    isRangeandExpression = true;
                                }

                            } else if (type == CampaignFormElementType.DROPDOWN) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlSpinnerFieldEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints, optionsValues, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.DATE) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlDateEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(), campaignFormElement.isImportant());
                                if (record.getCampaign() != null) {
                                    de.symeda.sormas.app.backend.campaign.Campaign campaign = record.getCampaign();
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
                                        ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMinDate(minDate);
                                    }
                                    if (maxDate != null) {
                                        ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMaxDate(maxDate);
                                    }
                                }
                            } else {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
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
    //                                expressionMap.forEach((formElement, controlPropertyField) ->
    //                                        CampaignFormDataFragmentUtils.handleExpressionSec(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));

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
                            Object defaultValue = campaignFormElement.getDefaultvalue();
                            formValues.add(new CampaignFormDataEntry(campaignFormElement.getId(), defaultValue == null ? null : defaultValue));
                            dynamicField.setValue(defaultValue == null ? null : defaultValue);
                            final String dependingOn = campaignFormElement.getDependingOn();
                            if (dependingOn != null) {
                                handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);
                            }

                            final String expressionString = campaignFormElement.getExpression();
                            if (expressionString != null) {
                                CampaignFormDataFragmentUtils.handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
                                expressionMap.put(campaignFormElement, dynamicField);
                            }
                        } else if (type == CampaignFormElementType.SECTION) {
                            if (campaignFormElement.getDependingOn() == null) {
                                ImageView dynamicField = new ImageView(requireContext(), null, R.style.LineBreak);
    //                            dynamicLayout.setDividerPadding(30);
                                dynamicLayout.addView(dynamicField, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        } else if (type == CampaignFormElementType.LABEL) {
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, CampaignFormDataFragmentUtils.getUserLanguageCaption(CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), campaignFormElement));
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        }  else if (type == CampaignFormElementType.LINEBREAK) {
    //                        System.out.println("Linke Break detetected in formelements ----------------");
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, "");
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        }
                    } else if (countr == 3 && onlyDay == 3) {
                        final LinearLayout dynamicLayout = mTabHost.findViewById(R.id.tabSheet3);
                        if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL && type != CampaignFormElementType.LINEBREAK) {
                            ControlPropertyField dynamicField;
                            boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                            if (type == CampaignFormElementType.YES_NO) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlYesNoUnknownField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.TEXTBOX) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.CHECKBOXBASIC ) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlMultiSelectCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), optionsValues, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.NUMBER) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.DECIMAL) {
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                    isRangeandExpression = true;
                                }
                            } else if (type == CampaignFormElementType.EMAIL) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.RANGE) {
                                final boolean exprx = expressionx;

                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage,onError, campaignFormMeta.getCampaignFormElements());

                                    isRangeandExpression = true;
                                }

                            } else if (type == CampaignFormElementType.DROPDOWN) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlSpinnerFieldEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints, optionsValues, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.DATE) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlDateEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(), campaignFormElement.isImportant());
                                if (record.getCampaign() != null) {
                                    de.symeda.sormas.app.backend.campaign.Campaign campaign = record.getCampaign();
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
                                        ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMinDate(minDate);
                                    }
                                    if (maxDate != null) {
                                        ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMaxDate(maxDate);
                                    }
                                }
                            } else {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
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
    //                                expressionMap.forEach((formElement, controlPropertyField) ->
    //                                        CampaignFormDataFragmentUtils.handleExpressionSec(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));

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
                            Object defaultValue = campaignFormElement.getDefaultvalue();
                            formValues.add(new CampaignFormDataEntry(campaignFormElement.getId(), defaultValue == null ? null : defaultValue));
                            dynamicField.setValue(defaultValue == null ? null : defaultValue);
                            final String dependingOn = campaignFormElement.getDependingOn();
                            if (dependingOn != null) {
                                handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);
                            }

                            final String expressionString = campaignFormElement.getExpression();
                            if (expressionString != null) {
                                CampaignFormDataFragmentUtils.handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
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
    //                        System.out.println("Linke Break detetected in formelements ----------------");
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, "");
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        }
                    } else if (countr == 4 && onlyDay == 4) {
                        final LinearLayout dynamicLayout = mTabHost.findViewById(R.id.tabSheet4);
                        if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL && type != CampaignFormElementType.LINEBREAK) {
                            ControlPropertyField dynamicField;
                            boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                            if (type == CampaignFormElementType.YES_NO) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlYesNoUnknownField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.TEXTBOX) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.CHECKBOXBASIC ) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlMultiSelectCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), optionsValues, campaignFormElement.isImportant());
                            }  else if (type == CampaignFormElementType.NUMBER ) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.DECIMAL) {
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                    isRangeandExpression = true;
                                }
                            } else if (type == CampaignFormElementType.EMAIL) {

                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.RANGE) {
                                final boolean exprx = expressionx;

                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage, onError, campaignFormMeta.getCampaignFormElements());
                                    isRangeandExpression = true;
                                }

                            } else if (type == CampaignFormElementType.DROPDOWN) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlSpinnerFieldEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints, optionsValues, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.DATE) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlDateEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(), campaignFormElement.isImportant());
                                if (record.getCampaign() != null) {
                                    de.symeda.sormas.app.backend.campaign.Campaign campaign = record.getCampaign();
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
                                        ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMinDate(minDate);
                                    }
                                    if (maxDate != null) {
                                        ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMaxDate(maxDate);
                                    }
                                }
                            } else {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
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
    //                                expressionMap.forEach((formElement, controlPropertyField) ->
    //                                        CampaignFormDataFragmentUtils.handleExpressionSec(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));

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
                            Object defaultValue = campaignFormElement.getDefaultvalue();
                            formValues.add(new CampaignFormDataEntry(campaignFormElement.getId(), defaultValue == null ? null : defaultValue));
                            dynamicField.setValue(defaultValue == null ? null : defaultValue);
                            final String dependingOn = campaignFormElement.getDependingOn();
                            if (dependingOn != null) {
                                handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);
                            }

                            final String expressionString = campaignFormElement.getExpression();
                            if (expressionString != null) {
                                CampaignFormDataFragmentUtils.handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
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
    //                        System.out.println("Linke Break detetected in formelements ----------------");
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, "");
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        }
                    } else if (countr == 5 && onlyDay == 5) {
                        final LinearLayout dynamicLayout = mTabHost.findViewById(R.id.tabSheet5);
                        if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL && type != CampaignFormElementType.LINEBREAK) {
                            ControlPropertyField dynamicField;
                            boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                            if (type == CampaignFormElementType.YES_NO) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlYesNoUnknownField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.TEXTBOX) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.CHECKBOXBASIC ) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlMultiSelectCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), optionsValues, campaignFormElement.isImportant());
                            }  else if (type == CampaignFormElementType.NUMBER) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.DECIMAL) {
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                    isRangeandExpression = true;
                                }
                            }  else if (type == CampaignFormElementType.EMAIL) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.RANGE) {
                                final boolean exprx = expressionx;

                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage, onError, campaignFormMeta.getCampaignFormElements());
                                    isRangeandExpression = true;
                                }

                            } else if (type == CampaignFormElementType.DROPDOWN) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlSpinnerFieldEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints, optionsValues, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.DATE) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlDateEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(), campaignFormElement.isImportant());
                                if (record.getCampaign() != null) {
                                    de.symeda.sormas.app.backend.campaign.Campaign campaign = record.getCampaign();
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
                                        ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMinDate(minDate);
                                    }
                                    if (maxDate != null) {
                                        ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMaxDate(maxDate);
                                    }
                                }
                            } else {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
                            }
                            fieldMap.put(campaignFormElement.getId(), dynamicField);
                            dynamicField.setShowCaption(true);
                            dynamicLayout.addView(dynamicField, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            Boolean finalIsRangeandExpression = isRangeandExpression;
                            final String dependingOnX = campaignFormElement.getDependingOn();

                            Boolean isdependingOn = false;
                            if (dependingOnX != null) {
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
    //                                expressionMap.forEach((formElement, controlPropertyField) ->
    //                                        CampaignFormDataFragmentUtils.handleExpressionSec(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));

                                    boolean changedFieldIsRangeTrigger =
                                            campaignFormElement != null
                                                    && "range".equalsIgnoreCase(campaignFormElement.getType())
                                                    && rangeTriggerIds.contains(campaignFormElement.getId());

                                    boolean userStillEditingThisField = field.hasFocus() || field.getFocusedChild() != null;

                                    expressionMap.forEach((formElement, controlPropertyField) -> {
                                        if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                        boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());

                                        if (!isRangeEffect) {
                                            CampaignFormDataFragmentUtils.handleExpressionSec(
                                                    expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()),
                                                    controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue());
                                        }
                                    });

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
                            Object defaultValue = campaignFormElement.getDefaultvalue();
                            formValues.add(new CampaignFormDataEntry(campaignFormElement.getId(), defaultValue == null ? null : defaultValue));
                            dynamicField.setValue(defaultValue == null ? null : defaultValue);
                            final String dependingOn = campaignFormElement.getDependingOn();
                            if (dependingOn != null) {
                                handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);
                            }

                            final String expressionString = campaignFormElement.getExpression();
                            if (expressionString != null) {
                                CampaignFormDataFragmentUtils.handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
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
    //                        System.out.println("Linke Break detetected in formelements ----------------");
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, "");
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        }
                    } else if (countr == 6 && onlyDay == 6) {
                        final LinearLayout dynamicLayout = mTabHost.findViewById(R.id.tabSheet6);
                        if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL && type != CampaignFormElementType.LINEBREAK) {
                            ControlPropertyField dynamicField;
                            boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                            if (type == CampaignFormElementType.YES_NO) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlYesNoUnknownField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.TEXTBOX) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.CHECKBOXBASIC ) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlMultiSelectCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), optionsValues, campaignFormElement.isImportant());
                            }  else if (type == CampaignFormElementType.NUMBER) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.DECIMAL) {
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                    isRangeandExpression = true;
                                }
                            } else if (type == CampaignFormElementType.EMAIL) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.RANGE) {
                                final boolean exprx = expressionx;

                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage, onError, campaignFormMeta.getCampaignFormElements());
                                    isRangeandExpression = true;
                                }

                            } else if (type == CampaignFormElementType.DROPDOWN) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlSpinnerFieldEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints, optionsValues, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.DATE) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlDateEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(), campaignFormElement.isImportant());
                                if (record.getCampaign() != null) {
                                    de.symeda.sormas.app.backend.campaign.Campaign campaign = record.getCampaign();
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
                                        ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMinDate(minDate);
                                    }
                                    if (maxDate != null) {
                                        ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMaxDate(maxDate);
                                    }
                                }
                            } else {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
                            }
                            fieldMap.put(campaignFormElement.getId(), dynamicField);
                            dynamicField.setShowCaption(true);
                            dynamicLayout.addView(dynamicField, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            Boolean finalIsRangeandExpression = isRangeandExpression;
                            final String dependingOxn = campaignFormElement.getDependingOn();

                            Boolean isdependingOn = false;
                            if (dependingOxn != null) {
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
    //                                expressionMap.forEach((formElement, controlPropertyField) ->
    //                                        CampaignFormDataFragmentUtils.handleExpressionSec(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));

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
                            Object defaultValue = campaignFormElement.getDefaultvalue();
                            formValues.add(new CampaignFormDataEntry(campaignFormElement.getId(), defaultValue == null ? null : defaultValue));
                            dynamicField.setValue(defaultValue == null ? null : defaultValue);
                            final String dependingOn = campaignFormElement.getDependingOn();
                            if (dependingOn != null) {
                                handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);
                            }

                            final String expressionString = campaignFormElement.getExpression();
                            if (expressionString != null) {
                                CampaignFormDataFragmentUtils.handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
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
    //                        System.out.println("Linke Break detetected in formelements ----------------");
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, "");
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        }
                    } else if (countr == 7 && onlyDay == 7) {
                        final LinearLayout dynamicLayout = mTabHost.findViewById(R.id.tabSheet7);
                        if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL && type != CampaignFormElementType.LINEBREAK) {
                            ControlPropertyField dynamicField;
                            boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                            if (type == CampaignFormElementType.YES_NO) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlYesNoUnknownField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.TEXTBOX) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.CHECKBOXBASIC ) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlMultiSelectCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), optionsValues, campaignFormElement.isImportant());
                            }  else if (type == CampaignFormElementType.NUMBER) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.DECIMAL) {
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                    isRangeandExpression = true;
                                }
                            } else if (type == CampaignFormElementType.EMAIL) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.RANGE) {
                                final boolean exprx = expressionx;

                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage, onError, campaignFormMeta.getCampaignFormElements());
                                    isRangeandExpression = true;
                                }

                            } else if (type == CampaignFormElementType.DROPDOWN) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlSpinnerFieldEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints, optionsValues, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.DATE) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlDateEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(), campaignFormElement.isImportant());
                                if (record.getCampaign() != null) {
                                    de.symeda.sormas.app.backend.campaign.Campaign campaign = record.getCampaign();
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
                                        ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMinDate(minDate);
                                    }
                                    if (maxDate != null) {
                                        ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMaxDate(maxDate);
                                    }
                                }
                            } else {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
                            }
                            fieldMap.put(campaignFormElement.getId(), dynamicField);
                            dynamicField.setShowCaption(true);
                            dynamicLayout.addView(dynamicField, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            Boolean finalIsRangeandExpression = isRangeandExpression;
                            final String dependingOxn = campaignFormElement.getDependingOn();

                            Boolean isdependingOn = false;
                            if (dependingOxn != null) {
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
    //                                expressionMap.forEach((formElement, controlPropertyField) ->
    //                                        CampaignFormDataFragmentUtils.handleExpressionSec(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));

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
                            Object defaultValue = campaignFormElement.getDefaultvalue();
                            formValues.add(new CampaignFormDataEntry(campaignFormElement.getId(), defaultValue == null ? null : defaultValue));
                            dynamicField.setValue(defaultValue == null ? null : defaultValue);
                            final String dependingOn = campaignFormElement.getDependingOn();
                            if (dependingOn != null) {
                                handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);
                            }

                            final String expressionString = campaignFormElement.getExpression();
                            if (expressionString != null) {
                                CampaignFormDataFragmentUtils.handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
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
    //                        System.out.println("Linke Break detetected in formelements ----------------");
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, "");
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        }
                    } else if (countr == 8 && onlyDay == 8) {
                        final LinearLayout dynamicLayout = mTabHost.findViewById(R.id.tabSheet8);
                        if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL && type != CampaignFormElementType.LINEBREAK) {
                            ControlPropertyField dynamicField;
                            boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                            if (type == CampaignFormElementType.YES_NO) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlYesNoUnknownField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.TEXTBOX) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.CHECKBOXBASIC ) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlMultiSelectCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), optionsValues, campaignFormElement.isImportant());
                            }  else if (type == CampaignFormElementType.NUMBER) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.DECIMAL) {
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                    isRangeandExpression = true;
                                }
                            }  else if (type == CampaignFormElementType.EMAIL) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.RANGE) {
                                final boolean exprx = expressionx;

                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage, onError, campaignFormMeta.getCampaignFormElements());
                                    isRangeandExpression = true;
                                }

                            } else if (type == CampaignFormElementType.DROPDOWN) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlSpinnerFieldEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints, optionsValues, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.DATE) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlDateEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(), campaignFormElement.isImportant());
                                if (record.getCampaign() != null) {
                                    de.symeda.sormas.app.backend.campaign.Campaign campaign = record.getCampaign();
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
                                        ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMinDate(minDate);
                                    }
                                    if (maxDate != null) {
                                        ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMaxDate(maxDate);
                                    }
                                }
                            } else {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
                            }
                            fieldMap.put(campaignFormElement.getId(), dynamicField);
                            dynamicField.setShowCaption(true);
                            dynamicLayout.addView(dynamicField, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            Boolean finalIsRangeandExpression = isRangeandExpression;
                            final String dependingOxn = campaignFormElement.getDependingOn();

                            Boolean isdependingOn = false;
                            if (dependingOxn != null) {
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
    //                                expressionMap.forEach((formElement, controlPropertyField) ->
    //                                        CampaignFormDataFragmentUtils.handleExpressionSec(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));

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

                            Object defaultValue = campaignFormElement.getDefaultvalue();
                            formValues.add(new CampaignFormDataEntry(campaignFormElement.getId(), defaultValue == null ? null : defaultValue));
                            dynamicField.setValue(defaultValue == null ? null : defaultValue);
                            final String dependingOn = campaignFormElement.getDependingOn();

                            if (dependingOn != null) {
                                handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);
                            }

                            final String expressionString = campaignFormElement.getExpression();
                            if (expressionString != null) {
                                CampaignFormDataFragmentUtils.handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
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
    //                        System.out.println("Linke Break detetected in formelements ----------------");
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, "");
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        }
                    } else if (countr == 9 && onlyDay == 9) {
                        final LinearLayout dynamicLayout = mTabHost.findViewById(R.id.tabSheet9);
                        if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL && type != CampaignFormElementType.LINEBREAK) {
                            ControlPropertyField dynamicField;
                            boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                            if (type == CampaignFormElementType.YES_NO) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlYesNoUnknownField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.TEXTBOX) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.CHECKBOXBASIC ) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlMultiSelectCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), optionsValues, campaignFormElement.isImportant());
                            }  else if (type == CampaignFormElementType.NUMBER) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.DECIMAL) {
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                    isRangeandExpression = true;
                                }
                            }  else if (type == CampaignFormElementType.EMAIL) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.RANGE) {
                                final boolean exprx = expressionx;

                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage, onError, campaignFormMeta.getCampaignFormElements());
                                    isRangeandExpression = true;
                                }

                            } else if (type == CampaignFormElementType.DROPDOWN) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlSpinnerFieldEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints, optionsValues, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.DATE) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlDateEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(), campaignFormElement.isImportant());
                                if (record.getCampaign() != null) {
                                    de.symeda.sormas.app.backend.campaign.Campaign campaign = record.getCampaign();
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
                                        ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMinDate(minDate);
                                    }
                                    if (maxDate != null) {
                                        ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMaxDate(maxDate);
                                    }
                                }
                            } else {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
                            }
                            fieldMap.put(campaignFormElement.getId(), dynamicField);
                            dynamicField.setShowCaption(true);
                            dynamicLayout.addView(dynamicField, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            Boolean finalIsRangeandExpression = isRangeandExpression;
                            final String dependingOxn = campaignFormElement.getDependingOn();

                            Boolean isdependingOn = false;
                            if (dependingOxn != null) {
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
    //                                expressionMap.forEach((formElement, controlPropertyField) ->
    //                                        CampaignFormDataFragmentUtils.handleExpressionSec(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));

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

                            Object defaultValue = campaignFormElement.getDefaultvalue();
                            formValues.add(new CampaignFormDataEntry(campaignFormElement.getId(), defaultValue == null ? null : defaultValue));
                            dynamicField.setValue(defaultValue == null ? null : defaultValue);
                            final String dependingOn = campaignFormElement.getDependingOn();

                            if (dependingOn != null) {
                                handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);
                            }

                            final String expressionString = campaignFormElement.getExpression();
                            if (expressionString != null) {
                                CampaignFormDataFragmentUtils.handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
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
    //                        System.out.println("Linke Break detetected in formelements ----------------");
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, "");
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        }
                    } else if (countr == 10 && onlyDay == 10) {
                        final LinearLayout dynamicLayout = mTabHost.findViewById(R.id.tabSheet10);
                        if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL && type != CampaignFormElementType.LINEBREAK) {
                            ControlPropertyField dynamicField;
                            boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                            if (type == CampaignFormElementType.YES_NO) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlYesNoUnknownField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.TEXTBOX) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.CHECKBOXBASIC ) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlMultiSelectCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), optionsValues, campaignFormElement.isImportant());
                            }  else if (type == CampaignFormElementType.NUMBER) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.DECIMAL) {
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                    isRangeandExpression = true;
                                }
                            }  else if (type == CampaignFormElementType.EMAIL) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.RANGE) {
                                final boolean exprx = expressionx;

                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage, onError, campaignFormMeta.getCampaignFormElements());
                                    isRangeandExpression = true;
                                }

                            } else if (type == CampaignFormElementType.DROPDOWN) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlSpinnerFieldEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints, optionsValues, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.DATE) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlDateEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(), campaignFormElement.isImportant());
                                if (record.getCampaign() != null) {
                                    de.symeda.sormas.app.backend.campaign.Campaign campaign = record.getCampaign();
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
                                        ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMinDate(minDate);
                                    }
                                    if (maxDate != null) {
                                        ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMaxDate(maxDate);
                                    }
                                }
                            } else {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
                            }
                            fieldMap.put(campaignFormElement.getId(), dynamicField);
                            dynamicField.setShowCaption(true);
                            dynamicLayout.addView(dynamicField, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            Boolean finalIsRangeandExpression = isRangeandExpression;
                            final String dependingOxn = campaignFormElement.getDependingOn();

                            Boolean isdependingOn = false;
                            if (dependingOxn != null) {
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
    //                                expressionMap.forEach((formElement, controlPropertyField) ->
    //                                        CampaignFormDataFragmentUtils.handleExpressionSec(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));

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

                            Object defaultValue = campaignFormElement.getDefaultvalue();
                            formValues.add(new CampaignFormDataEntry(campaignFormElement.getId(), defaultValue == null ? null : defaultValue));
                            dynamicField.setValue(defaultValue == null ? null : defaultValue);
                            final String dependingOn = campaignFormElement.getDependingOn();

                            if (dependingOn != null) {
                                handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);
                            }

                            final String expressionString = campaignFormElement.getExpression();
                            if (expressionString != null) {
                                CampaignFormDataFragmentUtils.handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
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
    //                        System.out.println("Linke Break detetected in formelements ----------------");
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, "");
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        }
                    } else if (countr == 11 && onlyDay == 11) {
                        final LinearLayout dynamicLayout = mTabHost.findViewById(R.id.tabSheet11);
                        if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL && type != CampaignFormElementType.LINEBREAK) {
                            ControlPropertyField dynamicField;
                            boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                            if (type == CampaignFormElementType.YES_NO) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlYesNoUnknownField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.TEXTBOX) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                            } else if (type == CampaignFormElementType.CHECKBOXBASIC ) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlMultiSelectCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), optionsValues, campaignFormElement.isImportant());
                            }  else if (type == CampaignFormElementType.NUMBER) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.DECIMAL) {
                                final boolean exprx = expressionx;
                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                    isRangeandExpression = true;
                                }
                            }  else if (type == CampaignFormElementType.EMAIL) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.RANGE) {
                                final boolean exprx = expressionx;

                                if (!exprx) {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                                } else {
                                    dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage, onError, campaignFormMeta.getCampaignFormElements());
                                    isRangeandExpression = true;
                                }

                            } else if (type == CampaignFormElementType.DROPDOWN) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlSpinnerFieldEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints, optionsValues, campaignFormElement.isImportant());
                            } else if (type == CampaignFormElementType.DATE) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlDateEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(), campaignFormElement.isImportant());
                                if (record.getCampaign() != null) {
                                    de.symeda.sormas.app.backend.campaign.Campaign campaign = record.getCampaign();
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
                                        ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMinDate(minDate);
                                    }
                                    if (maxDate != null) {
                                        ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMaxDate(maxDate);
                                    }
                                }
                            } else {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
                            }
                            fieldMap.put(campaignFormElement.getId(), dynamicField);
                            dynamicField.setShowCaption(true);
                            dynamicLayout.addView(dynamicField, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            Boolean finalIsRangeandExpression = isRangeandExpression;
                            final String dependingOxn = campaignFormElement.getDependingOn();

                            Boolean isdependingOn = false;
                            if (dependingOxn != null) {
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
    //                                expressionMap.forEach((formElement, controlPropertyField) ->
    //                                        CampaignFormDataFragmentUtils.handleExpressionSec(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));

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

                            Object defaultValue = campaignFormElement.getDefaultvalue();
                            formValues.add(new CampaignFormDataEntry(campaignFormElement.getId(), defaultValue == null ? null : defaultValue));
                            dynamicField.setValue(defaultValue == null ? null : defaultValue);
                            final String dependingOn = campaignFormElement.getDependingOn();

                            if (dependingOn != null) {
                                handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);
                            }

                            final String expressionString = campaignFormElement.getExpression();
                            if (expressionString != null) {
                                CampaignFormDataFragmentUtils.handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
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
    //                        System.out.println("Linke Break detetected in formelements ----------------");
                            if (campaignFormElement.getDependingOn() == null) {
                                TextView textView = new TextView(requireContext());
                                TextViewBindingAdapters.setHtmlValue(textView, "");
                                dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                        }
                    }
                } else {

                    final LinearLayout dynamicLayout = view.findViewById(R.id.dynamicLayout);
                    if (type != CampaignFormElementType.SECTION && type != CampaignFormElementType.LABEL  && type != CampaignFormElementType.LINEBREAK) {
                        ControlPropertyField dynamicField;

                        boolean ignoreDisable = campaignFormElement.isIgnoredisable();
                        if (type == CampaignFormElementType.YES_NO) {

                            dynamicField = CampaignFormDataFragmentUtils.createControlYesNoUnknownField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                        } else if (type == CampaignFormElementType.VALIDATEDTEXT) {
                            dynamicField = CampaignFormDataFragmentUtils.createControlValidatedTextField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                        } else if (type == CampaignFormElementType.TEXTBOX) {
                            dynamicField = CampaignFormDataFragmentUtils.createControlTextBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                        } else if (type == CampaignFormElementType.CHECKBOX || type == CampaignFormElementType.RADIO || type == CampaignFormElementType.RADIOBASIC) {
                            dynamicField = CampaignFormDataFragmentUtils.createControlCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints);
                        } else if (type == CampaignFormElementType.CHECKBOXBASIC ) {
                            dynamicField = CampaignFormDataFragmentUtils.createControlMultiSelectCheckBoxField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), optionsValues, campaignFormElement.isImportant());
                        } else if (type == CampaignFormElementType.NUMBER) {
                            if(campaignFormElement.getId().equalsIgnoreCase("villageCode")){
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldAllowStartingZero(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant());

                            }else{
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());

                            }
                        } else if (type == CampaignFormElementType.DECIMAL) {
                            final boolean exprx = expressionx;
                            if (!exprx) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimal(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError);
                            } else {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldDecimalExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage);
                                isRangeandExpression = true;
                            }
                        } else if (type == CampaignFormElementType.EMAIL) {
                            dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(
                                    campaignFormElement,
                                    requireContext(),
                                    CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta),
                                    userHints,false,
                                    campaignFormElement.isImportant()
                            );




                            dynamicField.addValueChangedListener(e-> {
                                if(e.getValue() != null ){
                                    String input = e.getValue() != null ? e.getValue().toString() : null;

                                    boolean isValid = android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches();
                                    if(campaignFormElement.isImportant() ){
                                        if (!isValid) {
                                            ((ControlPropertyEditField) e).enableErrorState("Please enter a valid email address");
                                        } else {
                                            ((ControlPropertyEditField) e).disableErrorState();
                                        }
                                    }else if(!campaignFormElement.isImportant() && (input != null || input != "")){
                                        if (!isValid) {
                                            ((ControlPropertyEditField) e).enableErrorState("Please enter a valid email address");
                                        } else {
                                            ((ControlPropertyEditField) e).disableErrorState();
                                        }
                                    }

                                }
                            });


                        }else if (type == CampaignFormElementType.PHONE) {
                            addMapValue();
    //                        // Create a label (caption)
                            countryLabel = new TextView(requireContext());
                            countryLabel.setText("Select Country");
                            countryLabel.setTextSize(16);
                            countryLabel.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
                            countryLabel.setPadding(24, 16, 24, 16);  // Add some padding for spacing

                            // Add the label to the dynamic layout
                            dynamicLayout.addView(countryLabel, new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                            Spinner countrySpinner = new Spinner(requireContext());

                            // Get country names from system locale
                            List<String> countryNames = new ArrayList<>();

                            for (Map.Entry<String, CountryDetails> countries : mapvalue.entrySet()) {
                                countryNames.add(countries.getKey().toString());
                            }
                            Collections.sort(countryNames);
                            currentCountryCode = countryNames.get(0);
                            // Create an ArrayAdapter
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, countryNames);
                            countrySpinner.setAdapter(adapter);


                            // Add spinner to dynamic layout
                            dynamicLayout.addView(countrySpinner, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                            dynamicField = CampaignFormDataFragmentUtils.createControlPhoneField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant());

                            helperText = new TextView(requireContext());
                            helperText.setText("Mobile number for " + country
                                    + " must be between " + min + " and " + max + " digits without the country code");
                            helperText.setTextSize(10);
                            helperText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
                            dynamicLayout.addView(helperText, new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            // Add listener to update countryLabel when a country is selected
                            countrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    String selectedCountry = countryNames.get(position);
                                    String countryCode = mapvalue.get(selectedCountry).getCode();  // Get country code from the map
                                    dynamicField.setValue("");
                                    dynamicField.setValue(countryCode);  // Update the EditText with the country code
                                    currentCountryCode = countryCode;
                                    helperText.setText("Mobile number for " + selectedCountry
                                            + " must be between " + mapvalue.get(selectedCountry).getMinLength() + " and " +
                                            mapvalue.get(selectedCountry).getMaxLength() + " digits without the country code");
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {}
                            });
                        } else if (type == CampaignFormElementType.RANGE) {
                            final boolean exprx = expressionx;
                            if (!exprx) {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangeOnly(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, campaignFormElement.isImportant(), minx, maxz, false, onError, campaignFormMeta.getCampaignFormElements());
                            } else {
                                dynamicField = CampaignFormDataFragmentUtils.createControlTextEditFieldRangexOnlyExpression(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), true, campaignFormElement.isImportant(), errorMessage, onError, campaignFormMeta.getCampaignFormElements());
                                isRangeandExpression = true;
                            }

                        } else if (type == CampaignFormElementType.DROPDOWN) {
                            dynamicField = CampaignFormDataFragmentUtils.createControlSpinnerFieldEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints, optionsValues, campaignFormElement.isImportant());
                        } else if (type == CampaignFormElementType.DATE) {
                            dynamicField = CampaignFormDataFragmentUtils.createControlDateEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,true, this.getFragmentManager(), campaignFormElement.isImportant());
                            if (record.getCampaign() != null) {
                                de.symeda.sormas.app.backend.campaign.Campaign campaign = record.getCampaign();
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
                                    ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMinDate(minDate);
                                }
                                if (maxDate != null) {
                                    ((de.symeda.sormas.app.component.controls.ControlDateField) dynamicField).setMaxDate(maxDate);
                                }
                            }

                        }else if (type == CampaignFormElementType.TIME) {
                            dynamicField = CampaignFormDataFragmentUtils.createControlTimeEditField(
                                    campaignFormElement,
                                    requireContext(),
                                    CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta),
                                    true,
                                    this.getFragmentManager(),
                                    campaignFormElement.isImportant());
                        } else if (type == CampaignFormElementType.IMAGE) {
                            dynamicField = CampaignFormDataFragmentUtils.createControlImageEditField(
                                    campaignFormElement,
                                    requireContext(),
                                    CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta),
                                    userHints,
                                    campaignFormElement.isImportant(),     // not hardcoded true
                                    cameraLauncher,
                                    cameraPermissionLauncher
                            );

                            if (dynamicField instanceof ControlImageEditField) {
                                ((ControlImageEditField) dynamicField).setCampaignFormDataUuid(record.getUuid());
                                ((ControlImageEditField) dynamicField).setRecord(record);
                                ((ControlImageEditField) dynamicField).setOnCaptureRequestListener(
                                        f -> currentImageField = f);
                            }
                        } else {
                            dynamicField = CampaignFormDataFragmentUtils.createControlTextEditField(campaignFormElement, requireContext(), CampaignFormDataFragmentUtils.getUserTranslations(campaignFormMeta), userHints,false, campaignFormElement.isImportant());
                        }

                        fieldMap.put(campaignFormElement.getId(), dynamicField);
                        dynamicField.setShowCaption(true);
                        dynamicLayout.addView(dynamicField, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        final String dependingOn = campaignFormElement.getDependingOn();
                        Boolean finalIsRangeandExpression = isRangeandExpression;
                        Boolean isdependingOn = false;
                        if (dependingOn != null) {
                            isdependingOn = true;
                        }

                        Boolean finalIsdependingOn = isdependingOn;
                        if (type == CampaignFormElementType.DROPDOWN && campaignFormElement.getId().equalsIgnoreCase("lotClusterNo")) {
                            dynamicField.addValueChangedListener(field -> {
                                baseEditActivity.setDataModified(true);
                                if (field.getValue() != null) {
                                    record.setLotClusterNo((String) field.getValue());
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
    //                                expressionMap.forEach((formElement, controlPropertyField) ->
    //                                        CampaignFormDataFragmentUtils.handleExpressionSec(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));

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
                        } else {
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
    //                                expressionMap.forEach((formElement, controlPropertyField) ->
    //                                        CampaignFormDataFragmentUtils.handleExpressionSec(expressionParser, formValues, CampaignFormElementType.fromString(formElement.getType()), controlPropertyField, formElement.getExpression(), ignoreDisable, field.getValue()));

                                    boolean changedFieldIsRangeTrigger =
                                            campaignFormElement != null
                                                    && "range".equalsIgnoreCase(campaignFormElement.getType())
                                                    && rangeEffectIds.contains(campaignFormElement.getId());

                                    boolean userStillEditingThisField = field.hasFocus() || field.getFocusedChild() != null;

                                    // pass 1: evaluate non-range effects immediately
                                    expressionMap.forEach((formElement, controlPropertyField) -> {
                                        if (formElement.getExpression() == null || formElement.getExpression().isEmpty()) return;
                                        boolean isRangeEffect = "range".equalsIgnoreCase(formElement.getType());
                                        boolean isDeferredRangeEffect = isRangeEffect && rangeEffectIds.contains(formElement.getId());

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
                                if (finalIsdependingOn && isRangeandExpressionx) {
                                    field.setVisibility(View.GONE);
                                }

                            });
                        }

                        if (type == CampaignFormElementType.NUMBER && campaignFormElement.getId().equalsIgnoreCase("villageCode")) {
                            dynamicField.addValueChangedListener(e->{
                                if (dynamicField.getValue().toString() != null && dynamicField.getValue().toString() != ""){
                                    if (record != null && record.getCommunity() != null) {


                                        if ( dynamicField.getValue().toString().length() == 3) {
                                            String inputValue = e.getValue().toString();
                                            if (inputValue.length() == 3) {
                                                handleVillageCodeValueGeneration(inputValue, dynamicField);
                                            }
                                        }
                                    }
                                }
                            });
                        }

                        if (type == CampaignFormElementType.TIME) {
                            dynamicField.addValueChangedListener(e->{
                                String value = dynamicField.getValue().toString();
                                if (dynamicField.getValue().toString() != null && dynamicField.getValue().toString() != ""){
                                    dynamicField.setValue(value);
                                }
                            });
                        }

                        if (type == CampaignFormElementType.PHONE && campaignFormElement.getId().equalsIgnoreCase("mobileNumber")) {
                            dynamicField.addValueChangedListener(e->{
                                String value = dynamicField.getValue().toString().replace(currentCountryCode, "");
                                if (dynamicField.getValue().toString() != null && dynamicField.getValue().toString() != ""){
                                    if (value != null && !value.matches("^[+]?[0-9]*$")) {
                                        dynamicField.setValue(value.replaceAll("[^0-9+]", ""));// Remove invalid characters
                                    }
                                }
                            });
                        }

                        if (type == CampaignFormElementType.TEXT && campaignFormElement.getId().equalsIgnoreCase("eTazkiraNo")) {
                            dynamicField.addValueChangedListener(e->{
                                if (dynamicField.getValue().toString() != null){
                                    if ( dynamicField.getValue().toString().length() == 13) {
                                        String inputValue = e.getValue().toString().replace("-", "").replace(".", "");
                                        if (inputValue.length() == 13) {
                                            handleETazkiraNoFormatting(inputValue, dynamicField);
                                        }
                                    }

                                }
                            });
                        }

                        Object defaultValue = campaignFormElement.getDefaultvalue();
                        formValues.add(new CampaignFormDataEntry(campaignFormElement.getId(), defaultValue == null ? null : defaultValue));
                        dynamicField.setValue(defaultValue == null ? null : defaultValue);
                        if (dependingOn != null && finalIsRangeandExpression) {
                            dynamicField.hideFieldOnly();
                        } else if (dependingOn != null) {
                            handleDependingOn(fieldMap, campaignFormElement, dynamicField, formValues);
                        }

                        final String expressionString = campaignFormElement.getExpression();
                        if (expressionString != null) {
                            CampaignFormDataFragmentUtils.handleExpression(expressionParser, formValues, type, dynamicField, expressionString, ignoreDisable);
                            expressionMap.put(campaignFormElement, dynamicField);
                        }
                    } else if (type == CampaignFormElementType.LINEBREAK) {
                        if (campaignFormElement.getDependingOn() == null) {
                            TextView textView = new TextView(requireContext());
                            TextViewBindingAdapters.setHtmlValue(textView, "");
                            dynamicLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        }
                    }
                    else if (type == CampaignFormElementType.SECTION) {
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
            System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++: " + daywiseDayCount);
            if (daywiseDayCount > 0) {
                System.out.println("COunt is more than 1  " +  daywiseDayCount);

                spec = mTabHost.newTabSpec("tab1").setIndicator("D1",//caption_1,
                                res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet1);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(0).getLayoutParams().width = 140;
            }
            if (daywiseDayCount > 1) {
                System.out.println("COunt is  2  daywise" +  daywiseDayCount);

                spec = mTabHost.newTabSpec("tab2").setIndicator("D2",//caption_1,
                                res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet2);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(1).getLayoutParams().width = 140;
            }
            if (daywiseDayCount > 2) {
                System.out.println("COunt is  3 daywise  " +  daywiseDayCount);

                spec = mTabHost.newTabSpec("tab3").setIndicator("D3",//caption_1,
                                res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet3);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(2).getLayoutParams().width = 140;
            }
            if (daywiseDayCount > 3) {
                System.out.println("COunt is  4 daywise " +  daywiseDayCount);

                spec = mTabHost.newTabSpec("tab4").setIndicator("D4",//caption_1,
                                res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet4);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(3).getLayoutParams().width = 140;
            }
            if (daywiseDayCount > 4) {
                System.out.println("COunt has toatal of 5 Daywise or more but is not 6 " +  daywiseDayCount);

                spec = mTabHost.newTabSpec("tab5").setIndicator("D5",//caption_1,
                                res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet5);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(4).getLayoutParams().width = 140;
                if(daywiseDayCount == 5){
                    mTabHost.getTabWidget().getChildAt(4).setVisibility(View.GONE);
                }

            }
            if (daywiseDayCount > 5) {

                System.out.println("COunt has toatal of 6 Daywise or more but is not 6 " +  daywiseDayCount);

                spec = mTabHost.newTabSpec("tab6").setIndicator("D6",//caption_1,
                                res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet6);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(5).getLayoutParams().width = 140;
                if(daywiseDayCount == 6){
                    mTabHost.getTabWidget().getChildAt(5).setVisibility(View.GONE);
                }

            }
            if (daywiseDayCount > 6) {
                System.out.println("COunt has toatal of 7 Daywise or more but is not 6 " +  daywiseDayCount);

                spec = mTabHost.newTabSpec("tab7").setIndicator("D7",//caption_1,
                                res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet7);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(6).getLayoutParams().width = 140;
                if(daywiseDayCount == 7){
                    mTabHost.getTabWidget().getChildAt(6).setVisibility(View.GONE);

                }

            }
            if (daywiseDayCount > 7) {
                System.out.println("COunt has toatal of 8 Daywise or more but is not 6 " +  daywiseDayCount);

                spec = mTabHost.newTabSpec("tab8").setIndicator("D8",//caption_1,
                                res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet8);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(7).getLayoutParams().width = 140;
                if(daywiseDayCount == 8) {

                    mTabHost.getTabWidget().getChildAt(7).setVisibility(View.GONE);
                }
            }
            if (daywiseDayCount > 8) {
                System.out.println("COunt has toatal of 8 Daywise or more but is not 6 " +  daywiseDayCount);

                spec = mTabHost.newTabSpec("tab9").setIndicator("D9",//caption_1,
                                res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet9);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(8).getLayoutParams().width = 140;
                if(daywiseDayCount == 9) {

                    mTabHost.getTabWidget().getChildAt(8).setVisibility(View.GONE);
                }

            }
            if (daywiseDayCount > 9) {
                System.out.println("COunt has toatal of 9 Daywise or more but is not 6 " +  daywiseDayCount);

                spec = mTabHost.newTabSpec("tab10").setIndicator("D10",//caption_1,
                                res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet10);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(9).getLayoutParams().width = 140;
                if(daywiseDayCount == 10) {

                    mTabHost.getTabWidget().getChildAt(9).setVisibility(View.GONE);
                }

            }
            if (daywiseDayCount > 10) {
                System.out.println("COunt has toatal of 10 Daywise or more but is not 6 " +  daywiseDayCount);

                spec = mTabHost.newTabSpec("tab11").setIndicator("D11",//caption_1,
                                res.getDrawable(R.drawable.ic_clear_black_24dp))
                        .setContent(R.id.tabSheet11);
                mTabHost.addTab(spec);
                mTabHost.getTabWidget().getChildAt(10).getLayoutParams().width = 160;
                if(daywiseDayCount == 11) {

                    mTabHost.getTabWidget().getChildAt(10).setVisibility(View.GONE);
                }

            }
//            if (daywiseDayCount > 11) {
//                System.out.println("COunt has toatal of 11 Daywise or more but is not 6 " +  daywiseDayCount);
//
//                spec = mTabHost.newTabSpec("tab12").setIndicator("D12",//caption_1,
//                                res.getDrawable(R.drawable.ic_clear_black_24dp))
//                        .setContent(R.id.tabSheet11);
//                mTabHost.addTab(spec);
//                mTabHost.getTabWidget().getChildAt(11).getLayoutParams().width = 140;
//                if(daywiseDayCount == 12) {
//
//                    mTabHost.getTabWidget().getChildAt(11).setVisibility(View.GONE);
//                }
//
//            }


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


    private void handleETazkiraNoFormatting(String inputValue, ControlPropertyField dynamicField) {
        String value = inputValue;

        String formattedTazkira = value.substring(0, 4) + "-"
                + value.substring(4, 8) + "-"
                + value.substring(8);

        dynamicField.setValue(formattedTazkira);

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
        int maxScroll = Math.max(0,
                scrollView.getChildAt(0).getWidth() - scrollView.getWidth());

        boolean canScrollRight = scrollX < maxScroll;
        boolean hasScrolled = scrollX > 0;

        int leftVisibility = (hasScrolled || !canScrollRight)
                ? View.VISIBLE
                : View.GONE;

        int rightVisibility = canScrollRight
                ? View.VISIBLE
                : View.GONE;

        leftArrow.setVisibility(leftVisibility);
        rightArrow.setVisibility(rightVisibility);

        Log.d("TABS",
                "scrollX=" + scrollX +
                        " max=" + maxScroll +
                        " left=" + (leftVisibility == View.VISIBLE ? "VISIBLE" : "GONE") +
                        " right=" + (rightVisibility == View.VISIBLE ? "VISIBLE" : "GONE"));
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

        scrollView.postDelayed(() -> {
            Log.d("TABS", "After scroll = " + scrollView.getScrollX());
        }, 500);

        scrollView.postDelayed(() ->{
            updateArrowVisibility(scrollView, leftArrow, rightArrow);
        }, 300);
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
                //since the village code most likelyh start with "0"

                String firstCharacterDelete = inputValue.substring(1);

                dynamicField.setValue(cCode + "" + firstCharacterDelete);
                break;
        }
    }

    @Override
    public int getEditLayout() {
        return R.layout.fragment_campaign_data_new_layout;
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

        List<Item> districtItemList = InfrastructureDaoHelper.loadAllDistricts();
        districtItemList.removeIf(d -> d == null || d.getValue() == null || d.getValue().toString().trim().isEmpty());

        List<Item> communityItemList = InfrastructureDaoHelper.loadAllCommunities();
        communityItemList.removeIf(d -> d == null || d.getValue() == null || d.getValue().toString().trim().isEmpty());


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

            if (initialDistricts.stream().noneMatch(item -> item.getValue() == null)) {
                initialDistricts.add(0, new Item<>("", null));
            }

        } else {

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

            List<String> userClusterUuids = new ArrayList<>();

            userClusterUuids = DatabaseHelper.getCommunityDao().getByDistrict(ConfigProvider.getUser().getDistrict()).stream()
                    .map(Community::getUuid)
                    .collect(Collectors.toList());

            System.out.println( "userClusterUuids).getUuid()------" + userClusterUuids);

            List<PopulationData> popDataSelectedClusters = DatabaseHelper.getPopulationDataDao()
                    .getSelectedClustersByMultipleUuids(userClusterUuids, record.getCampaign().getUuid());

            System.out.println( "popDataSelectedClusterssize------" + popDataSelectedClusters.size());

            Set<String> selectedClustersUuids = popDataSelectedClusters.stream()
                    .map(PopulationData::getCluster_id)
                    .collect(Collectors.toSet());

            System.out.println( "selectedClustersUuids)------" + selectedClustersUuids.size());

            List<Item> initialClustersFound = communityItemList.stream()
                    .filter(item -> selectedClustersUuids.contains(((Community) item.getValue()).getUuid()))
                    .collect(Collectors.toList());

            System.out.println( "initialClustersFound)------" + initialClustersFound.size());

            initialCommunities = initialClustersFound;

            if (initialCommunities.stream().noneMatch(item -> item.getValue() == null)) {
                initialCommunities.add(0, new Item<>("", null));
            }
        }

        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        presentDate = date;
    }

    @Override
    protected void onLayoutBinding(FragmentCampaignDataNewLayoutBinding contentBinding) {
        contentBinding.setData(record);


        Item campaignItem = record.getCampaign() != null ? DataUtils.toItem(record.getCampaign()) : null;

        if (campaignItem != null && !initialCampaigns.contains(campaignItem)) {
            initialCampaigns.add(campaignItem);
        }

        contentBinding.campaignFormDataCampaign.initializeSpinner(initialCampaigns, record.getCampaign());
        contentBinding.campaignFormDataCampaign.setEnabled(false);


        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();

        System.out.println("Initializing date field with: " + date);

        try {
            // Initialize the date field with fragment manager and initial date
            if (record.getCampaign() != null) {
                final CampaignFormMeta campaignFormMeta = DatabaseHelper.getCampaignFormMetaDao().queryForId(record.getCampaignFormMeta().getId());
                Date expiryDate = DatabaseHelper.getCampaignFormMetaWithExpDao().getCampaignFormExpiryDateByCampaignIdAndFormId(record.getCampaign().getUuid(), campaignFormMeta.getUuid());


                de.symeda.sormas.app.backend.campaign.Campaign campaign = record.getCampaign();
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
                record.getCommunity(), record.getCampaignFormMeta().getGeographylevel(),  false);


//        InfrastructureDaoHelper.initializeRegionAreaFields(
//                contentBinding.campaignFormDataArea,
//                initialAreas,
//                record.getArea(),
//                contentBinding.campaignFormDataRegion,
//                initialRegions,
//                record.getRegion(),
//                contentBinding.campaignFormDataDistrict,
//                initialDistricts,
//                record.getDistrict(),
//                contentBinding.campaignFormDataCommunity,
//                initialCommunities,
//                record.getCommunity(), false);
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
                String normalizedDateString = normalizeRawDateString(input); // this gives "03-08-2025"
                if (normalizedDateString != null) {
                    SimpleDateFormat fallbackFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
                    return fallbackFormat.parse(normalizedDateString);
                }
            }catch(Exception ee){
                Log.e(getClass().getName(), "Error parsing date: " + input, ee);
                return null;
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

    @Override
    protected void onAfterLayoutBinding(FragmentCampaignDataNewLayoutBinding contentBinding) {
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

    public void validateForSave(Context context) throws ValidationException {
        if (!daywise) {
            // Non-daywise form → validate everything
            FragmentValidator.validate(context, getContentBinding());
            return;
        }

        // Day-wise form → restricted validation
        validateDayWise(context);
    }

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
        System.out.println("DAYYYYYYYYYYYYYYYYYYYY " +day);
        switch (day) {
            case 1: return mTabHost.findViewById(R.id.tabSheet1);
            case 2: return mTabHost.findViewById(R.id.tabSheet2);
            case 3: return mTabHost.findViewById(R.id.tabSheet3);
            case 4: return mTabHost.findViewById(R.id.tabSheet4);
            case 5: return mTabHost.findViewById(R.id.tabSheet5);
            case 6: return mTabHost.findViewById(R.id.tabSheet6);
            case 7: return mTabHost.findViewById(R.id.tabSheet7);
            case 8: return mTabHost.findViewById(R.id.tabSheet8);

            case 9: return mTabHost.findViewById(R.id.tabSheet9);
            case 10: return mTabHost.findViewById(R.id.tabSheet10);
            case 11: return mTabHost.findViewById(R.id.tabSheet11);
            default: return null;
        }
    }

}