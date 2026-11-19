/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2020 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.app.campaign;

import static de.symeda.sormas.api.campaign.ExpressionProcessorUtils.refreshEvaluationContext;
import static de.symeda.sormas.api.utils.FieldConstraints.CHARACTER_LIMIT_DEFAULT;

import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.fragment.app.FragmentManager;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import de.symeda.sormas.api.campaign.data.CampaignFormDataEntry;
import de.symeda.sormas.api.campaign.form.CampaignFormElement;
import de.symeda.sormas.api.campaign.form.CampaignFormElementType;
import de.symeda.sormas.api.campaign.form.CampaignFormTranslations;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.utils.YesNoUnknown;
import de.symeda.sormas.app.R;
import de.symeda.sormas.app.backend.campaign.form.CampaignFormMeta;
import de.symeda.sormas.app.component.controls.ControlCheckBoxField;
import de.symeda.sormas.app.component.controls.ControlCheckBoxGroupField;
import de.symeda.sormas.app.component.controls.ControlDateField;
import de.symeda.sormas.app.component.controls.ControlDecimalEditField;
import de.symeda.sormas.app.component.controls.ControlImageEditField;
import de.symeda.sormas.app.component.controls.ControlPhoneField;
import de.symeda.sormas.app.component.controls.ControlPropertyField;
import de.symeda.sormas.app.component.controls.ControlSpinnerField;
import de.symeda.sormas.app.component.controls.ControlSwitchField;
import de.symeda.sormas.app.component.controls.ControlTextBoxField;
import de.symeda.sormas.app.component.controls.ControlTextEditField;
import de.symeda.sormas.app.component.controls.ControlTextEditFieldAllowZeroInput;
import de.symeda.sormas.app.component.controls.ControlTextEditFieldRange;
import de.symeda.sormas.app.component.controls.ControlTextReadField;
import de.symeda.sormas.app.component.controls.ControlTimeField;
import de.symeda.sormas.app.component.controls.ControlValidatedTextField;
import de.symeda.sormas.app.component.controls.ValueChangeListener;
import de.symeda.sormas.app.util.YesNo;

public class CampaignFormDataFragmentUtils {
    public static final int DEFAULT_MIN_LENGTH = 1;
    private static final DecimalFormat df = new DecimalFormat("0.00");
    private static boolean isExpressionEvaluationInProgress = false;

    private static final Pattern ID_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*");

    private static final Set<String> KEYWORDS = Set.of("true", "false", "null", "AND", "OR", "NOT");


    private ActivityResultLauncher<Intent> cameraLauncher;
    private CampaignFormDataFragmentUtils() {
    }

    private static void applyExpressionValue(
            CampaignFormElement formElement,
            List<CampaignFormDataEntry> formValues,
            Object rawValue) {

        CampaignFormDataEntry entry = getOrCreateCampaignFormDataEntry(formValues, formElement);

        if (rawValue == null || rawValue.toString().trim().isEmpty()) {
            entry.setValue(rawValue);
        } else {
            entry.setValue(rawValue);
        }
    }

    public static Set<String> getRangeMinMaxTriggerIds(List<CampaignFormElement> elements) {
        Set<String> ids = new LinkedHashSet<>();
        if (elements == null) return ids;

        for (CampaignFormElement e : elements) {
            if (e == null || !"range".equalsIgnoreCase(e.getType())) continue;
            if (e.getExpression() != null && !e.getExpression().trim().isEmpty()) continue;

            String[] constraints = e.getConstraints();
            if (constraints == null) continue;

            boolean hasMin = Arrays.stream(constraints)
                    .filter(c -> c != null)
                    .map(c -> c.trim().toLowerCase(Locale.ROOT))
                    .anyMatch(c -> c.startsWith("min="));

            boolean hasMax = Arrays.stream(constraints)
                    .filter(c -> c != null)
                    .map(c -> c.trim().toLowerCase(Locale.ROOT))
                    .anyMatch(c -> c.startsWith("max="));

            if (hasMin || hasMax) {
                ids.add(e.getId());
            }
        }
        return ids;
    }

    public static Set<String> extractIdsFromExpression(String expression) {
        Set<String> ids = new LinkedHashSet<>();
        if (expression == null || expression.trim().isEmpty()) return ids;

        Matcher matcher = ID_PATTERN.matcher(expression);
        while (matcher.find()) {
            String token = matcher.group();
            if (!KEYWORDS.contains(token)) {
                ids.add(token);
            }
        }
        return ids;
    }

    public static Set<String> getRangeExpressionEffectIds(
            List<CampaignFormElement> elements,
            Set<String> triggerIds) {
        Set<String> effectIds = new LinkedHashSet<>();
        if (elements == null || triggerIds == null || triggerIds.isEmpty()) return effectIds;

        for (CampaignFormElement e : elements) {
            if (e == null || !"range".equalsIgnoreCase(e.getType())) continue;
            if (e.getExpression() == null || e.getExpression().trim().isEmpty()) continue;

            Set<String> refs = extractIdsFromExpression(e.getExpression());
            for (String ref : refs) {
                if (triggerIds.contains(ref)) {
                    effectIds.add(e.getId());
                    break;
                }
            }
        }
        return effectIds;
    }

    public static void handleExpressionSec(
            ExpressionParser expressionParser,
            List<CampaignFormDataEntry> formValues,
            CampaignFormElementType type,
            ControlPropertyField dynamicField,
            String expressionString,
            Boolean isDisIgnore,
            Object orginalValue) {
        try {
            if (expressionString != null && !expressionString.trim().isEmpty()) {
                final Object expressionValue = getExpressionValue(expressionParser, formValues, expressionString);
                String valuex = expressionValue + "";

                // VALIDATEDTEXT must accept an empty string because the outermost
                // ternary intentionally clears the calculated field when the row is blank.
                if (type == CampaignFormElementType.VALIDATEDTEXT) {
                    ((ControlValidatedTextField) dynamicField).setExpressionValue(expressionValue);
                } else if (!valuex.isEmpty() && !valuex.equals("") && expressionValue != null) {//&& !valuex.equals("0")

                    if (expressionValue != null) { //we need to see how to check and filter when its blank or empty
                        System.out.println(dynamicField.getCaption() + " : " + expressionString + " =====)))))))))))))))==== " + expressionValue);
                        if (type == CampaignFormElementType.YES_NO) {
                            ControlSwitchField.setValue((ControlSwitchField) dynamicField, expressionValue, true, YesNo.class, null);
                        } else if (type == CampaignFormElementType.RANGE) {
                            String valudex = valuex == null ? "" : valuex.endsWith(".0") ? valuex.replace(".0", "") : valuex;

                            if (orginalValue != null) {
                                if (!orginalValue.toString().equals(valudex)) {
                                    System.out.println(orginalValue + "from handlesec++++++++++++2222333++++++++++++++++ " + valudex);
                                    if (!(orginalValue.toString().isEmpty() && valudex != null)) {
                                        System.out.println("from handlesec++++++++++++333333333++++++++++++++++ " + valudex);
//                                        ControlTextEditFieldRange.setValue((ControlTextEditFieldRange) dynamicField, expressionValue.toString().equals("0") ? "0" : expressionValue.toString().endsWith(".0") ? expressionValue.toString().replace(".0", "") : expressionValue.toString());
                                        // Guard: only update if value changed
                                        ControlTextEditFieldRange rangeField = (ControlTextEditFieldRange) dynamicField;
                                        String newValue = expressionValue.toString().equals("0")
                                                ? "0"
                                                : expressionValue.toString().endsWith(".0")
                                                ? expressionValue.toString().replace(".0", "")
                                                : expressionValue.toString();
                                        String currentValue = rangeField.getValue();
                                        if (currentValue == null) {
                                            currentValue = "";
                                        }
                                        if (!currentValue.equals(newValue)) {
                                            ControlTextEditFieldRange.setValue(rangeField, newValue);
                                        }
                                    }
                                }
                            } else {
//                                if (valudex != null) {
//                                    ControlTextEditFieldRange.setValue((ControlTextEditFieldRange) dynamicField, expressionValue.toString().equals("0") ? "" : expressionValue.toString().endsWith(".0") ? expressionValue.toString().replace(".0", "") : expressionValue.toString());
//                                }
                                if (valudex != null) {
                                    // Guard: only update if value changed
                                    ControlTextEditFieldRange rangeField = (ControlTextEditFieldRange) dynamicField;
                                    String newValue = expressionValue.toString().equals("0")
                                            ? ""
                                            : expressionValue.toString().endsWith(".0")
                                            ? expressionValue.toString().replace(".0", "")
                                            : expressionValue.toString();
                                    String currentValue = rangeField.getValue();
                                    if (currentValue == null) {
                                        currentValue = "";
                                    }
                                    if (!currentValue.equals(newValue)) {
                                        ControlTextEditFieldRange.setValue(rangeField, newValue);
                                    }
                                }
                            }
                        } else if (type == CampaignFormElementType.DECIMAL && expressionValue != null) {

                            System.out.println(orginalValue + "from handlesec++++++++++++decimal2222333++++++++++++++++ " + expressionValue);
                            System.out.println(Boolean.FALSE.equals(expressionValue) + "from handlesecdecimal ++++++++++++decimal2222333++++++++++++++++ " + expressionValue);


                            if (expressionValue instanceof Boolean
                                    && Boolean.FALSE.equals(expressionValue)
                            ) {

                                // Clear UI field
//                                if (dynamicField instanceof ControlTextEditFieldRange) {
//                                    ControlTextEditFieldRange.setValue(
//                                            (ControlTextEditFieldRange) dynamicField, ""
//                                    );
//
//                                }

                                if (dynamicField instanceof ControlTextEditFieldRange) {
                                    ControlTextEditFieldRange rangeField = (ControlTextEditFieldRange) dynamicField;
                                    String currentValue = rangeField.getValue();
                                    if (currentValue == null) {
                                        currentValue = "";
                                    }
                                    // Only set if not already empty to avoid re-triggering listeners
                                    if (!currentValue.isEmpty()) {
                                        ControlTextEditFieldRange.setValue(rangeField, "");
                                    }
                                }


                                // Allow user to correct the value
                                dynamicField.setEnabled(true);
                                return;
                            }
                            if (expressionValue instanceof Number) {

                                String value = expressionValue.toString();


                                String currentFieldValue = ((ControlDecimalEditField) dynamicField).getValue();
                                if (currentFieldValue != null && currentFieldValue.endsWith(".")) {
                                    return;
                                }

                                if (orginalValue != null) {
                                    String currentValue = orginalValue.toString();
                                    String newValue = expressionValue.toString();

                                    if (newValue.contains(".") && !newValue.endsWith(".0")) {
                                        if (!currentValue.equals(newValue)) {
                                            if (!(currentValue.isEmpty() && newValue.equals("0"))) {
                                                ControlDecimalEditField.setValue((ControlDecimalEditField) dynamicField, newValue);
                                            }
                                        }
                                    } else {
                                        String valudex = value.equals("0") ? null : value.endsWith(".0") ? value.replace(".0", "") : value;
                                        if (!currentValue.equals(valudex)) {
                                            if (!(currentValue.isEmpty() && valudex == null)) {
                                                ControlDecimalEditField.setValue((ControlDecimalEditField) dynamicField, valudex);
                                            }
                                        }
                                    }
                                } else {
                                    String newValue = expressionValue.toString();

                                    if (newValue.contains(".") && (newValue.endsWith(".") || newValue.split("\\.")[1].length() < 2)) {
                                        ControlDecimalEditField.setValue((ControlDecimalEditField) dynamicField, newValue);
                                    } else {
                                        try {
                                            double num = Double.parseDouble(newValue);
                                            String valudex;
                                            if (num == 0) {
                                                valudex = String.valueOf((int) num);
                                            } else if (num == Math.floor(num)) {
                                                valudex = String.valueOf((int) num);
                                            } else {
                                                valudex = String.format("%.2f", num);
                                            }
                                            ControlDecimalEditField.setValue((ControlDecimalEditField) dynamicField, valudex);
                                        } catch (NumberFormatException e) {
                                            ControlDecimalEditField.setValue((ControlDecimalEditField) dynamicField, newValue);
                                        }
                                    }
                                }
                            }
                        } else if (type == CampaignFormElementType.NUMBER) {
                            if (expressionValue != null) {
                                System.out.println("Expression Value is not null =================");
                                String valudex = expressionValue.toString();
                                try {
                                    double num = Double.parseDouble(valuex);
                                    if (num != 0) {
                                        // If it's a whole number (like 15.0), convert to integer string
                                        if (num == Math.floor(num)) {
                                            valudex = String.valueOf((int) num);
                                        } else {
                                            // If it's a decimal, format to 2 decimal places
                                            valudex = String.format("%.2f", num);
                                        }
                                    } else {
                                        valudex = String.valueOf((int) num); // Whole number

                                    }
                                    // If num == 0, valudex stays null
                                } catch (NumberFormatException e) {
                                    valudex = valuex;
                                }

                                if (orginalValue != null) {
                                    System.out.println("orginalValue Value is not null =================");

                                    if (!orginalValue.toString().equals(valudex)) {
                                        System.out.println("orginalValue Value is not valudex =================");

                                        if (!(orginalValue.toString().isEmpty() && valudex == null)) {
                                            ControlTextEditField.setValue((ControlTextEditField) dynamicField, valudex);
                                        }
                                    }else{
                                        if(valudex!=null){
                                            ControlTextEditField.setValue((ControlTextEditField) dynamicField, valudex);
                                        }
                                    }
                                } else {
                                    System.out.println("orginalValue Value is  null =================");

                                    if (valudex != null) {
                                        ControlTextEditField.setValue((ControlTextEditField) dynamicField, valudex);
                                    }
                                }
                            }else{

                                System.out.println("Expression Value is null =================");

                                String valudex = null;
                                try {
                                    double num = Double.parseDouble(valuex);
                                    if (num != 0) {
                                        // If it's a whole number (like 15.0), convert to integer string
                                        if (num == Math.floor(num)) {
                                            valudex = String.valueOf((int) num);
                                        } else {
                                            // If it's a decimal, format to 2 decimal places
                                            valudex = String.format("%.2f", num);
                                        }
                                    } else {
                                        valudex = String.valueOf((int) num); // Whole number

                                    }
                                    // If num == 0, valudex stays null
                                } catch (NumberFormatException e) {
                                    valudex = valuex;
                                }

                                if (orginalValue != null) {
                                    System.out.println("orginalValue Value is not null =================expressionnul");

                                    if (!orginalValue.toString().equals(valudex)) {
                                        if (!(orginalValue.toString().isEmpty() && valudex == null)) {
                                            ControlTextEditField.setValue((ControlTextEditField) dynamicField, valudex);
                                        }
                                    }
                                } else {
                                    System.out.println("orginalValue Value is null =================expressionnul");

                                    if (valudex != null) {
                                        ControlTextEditField.setValue((ControlTextEditField) dynamicField, valudex);
                                    }
                                }
                            }
                        } else if (expressionValue.getClass().isAssignableFrom(Boolean.class)) {
                            ControlTextEditField.setValue((ControlTextEditField) dynamicField, (Double) (!Double.isFinite((double) expressionValue) ? 0 : expressionValue.toString().endsWith(".0") ? expressionValue.toString().replace(".0", "") : df.format((double) expressionValue)));
                        } else {
                            ControlTextEditField.setValue((ControlTextEditField) dynamicField, expressionValue == null ? null : expressionValue.toString());
                        }
                    }
                }


            } else {

            }
        } catch (SpelEvaluationException e) {
            Log.e("Error evaluating expression on field : " + dynamicField.getCaption(), e.getMessage());
        }catch (Exception ex){
            Log.e("Error evaluating expressionyyyyy on field : " + dynamicField.getCaption(), ex.getMessage());
        }
        if (type == CampaignFormElementType.RANGE || type == CampaignFormElementType.DECIMAL) {
            dynamicField.setEnabled(true);
        } else if (isDisIgnore) {
            dynamicField.setEnabled(true);
        } else {
            dynamicField.setEnabled(false);
        }

    }

    public static void handleExpression(
            ExpressionParser expressionParser,
            List<CampaignFormDataEntry> formValues,
            CampaignFormElementType type,
            ControlPropertyField dynamicField,
            String expressionString,
            Boolean isDisIgnore) {
        try {
            if (expressionString != null && !expressionString.trim().isEmpty()) {
                final Object expressionValue = getExpressionValue(expressionParser, formValues, expressionString);
                String valuex = expressionValue + "";
                ;
                System.out.println("second method ___))))))))))))   )))))))))))))))))))))))))))))))))-----= " + valuex);
                // Do this before the non-empty guard so a calculated blank can clear
                // a value that was produced by an earlier expression evaluation.
                if (type == CampaignFormElementType.VALIDATEDTEXT) {
                    ((ControlValidatedTextField) dynamicField).setExpressionValue(expressionValue);
                } else if (!valuex.isEmpty() && !valuex.equals("") && expressionValue != null) {
                    if (expressionValue != null) { //we need to see how to check and filter when its blank or empty

                        if (type == CampaignFormElementType.YES_NO) {
                            ControlSwitchField.setValue((ControlSwitchField) dynamicField, expressionValue, true, YesNo.class, null);
                        } else if (type == CampaignFormElementType.RANGE) {
                            String valudex = valuex;

                            try {
                                double num = Double.parseDouble(valuex);
                                if (num != 0) {
                                    if (num == Math.floor(num)) {
                                        valudex = String.valueOf((int) num); // Whole number
                                    } else {
                                        valudex = String.format("%.2f", num); // Decimal to 2 dp
                                    }
                                } else {
                                    valudex = String.valueOf((int) num); // Whole number
                                }
                            } catch (NumberFormatException e) {
                                valudex = valuex; // fallback for non-numeric input
                            }

                            if (valudex != null && !valudex.isEmpty() && !valudex.toString().equals("")) {
                                ControlTextEditFieldRange.setValue((ControlTextEditFieldRange) dynamicField, valudex);
                            }

                        } else if (type == CampaignFormElementType.NUMBER) {

                            String formatted;
                            try {
                                double num = Double.parseDouble(valuex);
                                if (num == Math.floor(num)) {
                                    formatted = String.valueOf((int) num);
                                } else {
                                    formatted = String.format("%.2f", num);
                                }
                            } catch (NumberFormatException e) {
                                formatted = valuex;
                            }
                            ControlTextEditField.setValue((ControlTextEditField) dynamicField, formatted);
                            //                            ControlTextEditField.setValue((ControlTextEditField) dynamicField, expressionValue.toString().equals("0") ? "0" : (expressionValue.toString().endsWith(".0") ? expressionValue.toString().replace(".0", "") : expressionValue.toString()));

                        } else if (type == CampaignFormElementType.DECIMAL) {

                            if (expressionValue instanceof Boolean
                                    && Boolean.FALSE.equals(expressionValue)
                            ) {

                                // Clear UI field
                                if (dynamicField instanceof ControlTextEditFieldRange) {
                                    ControlTextEditFieldRange.setValue(
                                            (ControlTextEditFieldRange) dynamicField, ""
                                    );

                                }
                                dynamicField.setEnabled(true);
                                return;
                            }


                            if (expressionValue instanceof Number) {

                                if (dynamicField instanceof ControlDecimalEditField) {
                                    String formattedValue;
                                    if (valuex != null) {
                                        double numValue;
                                        try {
                                            numValue = Double.parseDouble(valuex);
                                            if (numValue == Math.floor(numValue)) {
                                                // It's a whole number, remove decimal part
                                                formattedValue = String.format("%.0f", numValue);
                                            } else {
                                                // It has decimals, format to one decimal place
                                                formattedValue = String.format("%.1f", numValue);
                                            }
                                            // Handle zero case
                                            if (numValue == 0) {
                                                formattedValue = "0";
                                            }
                                        } catch (NumberFormatException e) {
                                            formattedValue = valuex;
                                        }
                                        ControlDecimalEditField.setValue((ControlDecimalEditField) dynamicField, formattedValue);
                                    }
                                }
                            }
                        } else if (expressionValue.getClass().isAssignableFrom(Boolean.class)) {
                            ControlTextEditField.setValue((ControlTextEditField) dynamicField, (Double) (!Double.isFinite((double) expressionValue) ? 0 : expressionValue.toString().endsWith(".0") ? expressionValue.toString().replace(".0", "") : df.format((double) expressionValue)));
                        } else {
                            ControlTextEditField.setValue((ControlTextEditField) dynamicField, expressionValue == null ? null : expressionValue.toString());
                        }
                    }
                }
            }
        } catch (SpelEvaluationException e) {
            Log.e("Error evaluating expression on field2 : " + dynamicField.getCaption(), e.getMessage());
        }catch (Exception ex){
            Log.e("Error evaluating expressionxxxxx on field2 : " + dynamicField.getCaption(), ex.getMessage());
        }
        if (type == CampaignFormElementType.RANGE || type == CampaignFormElementType.DECIMAL) {
            dynamicField.setEnabled(true);
        } else if (isDisIgnore) {
            dynamicField.setEnabled(true);
        } else {
            dynamicField.setEnabled(false);
        }

    }

    private static String getExpressionZeroToFalse(String expressionString) {
        String expressionZeroParser = expressionString.replaceAll(" ", "");
        String tmpString = "";

        boolean cleanProccessor = false;

        if (expressionZeroParser.contains(":0")) {
            tmpString = expressionZeroParser.replaceAll(":0", ":false");
            cleanProccessor = true;
        }

        if (tmpString.contains(":false0")) {
            tmpString = tmpString.replaceAll(":false0", ":0");
            cleanProccessor = true;
        }

        if (cleanProccessor) {
            return tmpString;
        } else {
            return expressionZeroParser;
        }
    }


    public static Object getExpressionValue(ExpressionParser expressionParser, List<CampaignFormDataEntry> formValues, String rawExpressionString) {
//        System.out.println("111111111 getExpressionValue" + rawExpressionString);
//        final String processedExpressionZeroString = getExpressionZeroToFalse(rawExpressionString);
//        String cleanedexpressionString = processedExpressionZeroString;

//        final String processedExpressionZeroString = getExpressionZeroToFalse(rawExpressionString);
        String cleanedexpressionString = rawExpressionString;

//        System.out.println("22222222222222 getExpressionValue" + cleanedexpressionString);

        // ControlSwitchField stores YES/NO as enum values on Android, while
        // JSON expressions for a yes-no element use true/false. Build a
        // temporary expression-only view so persistence keeps its original
        // enum values but SpEL receives platform-neutral booleans.
        final List<CampaignFormDataEntry> expressionFormValues = new ArrayList<>();
        for (CampaignFormDataEntry entry : formValues) {
            Object normalizedValue = normalizeExpressionValue(entry.getValue());
            expressionFormValues.add(new CampaignFormDataEntry(entry.getId(), normalizedValue));
        }

        final EvaluationContext context = refreshEvaluationContext(expressionFormValues);
        final Expression expression = expressionParser.parseExpression(cleanedexpressionString);
        final Class<?> valueType = expression.getValueType(context);
        final Object valueFin = expression.getValue(context, valueType);
        if (valueFin != null) {
            if (!valueFin.getClass().isAssignableFrom(valueType)) {
                System.out.println("EmptyStackException: >>>>>>-");
                throw new EmptyStackException();
            }
        } else {
            throw new SpelEvaluationException(SpelMessage.NOT_A_REAL);
        }
        return expression.getValue(context, valueType);
    }

    private static Object normalizeExpressionValue(Object value) {
        if (!(value instanceof Enum<?>)) {
            return value;
        }

        String enumName = ((Enum<?>) value).name();
        if ("YES".equalsIgnoreCase(enumName) || "Y".equalsIgnoreCase(enumName)) {
            return Boolean.TRUE;
        }
        if ("NO".equalsIgnoreCase(enumName) || "N".equalsIgnoreCase(enumName)) {
            return Boolean.FALSE;
        }

        return value;
    }
    public static void handleDependingOnSectionAndLabel(
            Map<String, ControlPropertyField> fieldMap,
            CampaignFormElement campaignFormElement,
            LinearLayout dynamicField) {
        final String dependingOn = campaignFormElement.getDependingOn();
        final String[] dependingOnValues = campaignFormElement.getDependingOnValues();

        List<String> constraints;
        String depenValuexd = null;
        if (dependingOnValues != null) {
            constraints = (List) Arrays.stream(dependingOnValues).collect(Collectors.toList());
            ListIterator<String> lstItemsx = constraints.listIterator();
            if (lstItemsx.hasNext()) {
                System.out.println("handleDependingOnSectionAndLabel@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@1111>>>>>>>> " + lstItemsx);

                depenValuexd = lstItemsx.next().toString();
            }
        }

        System.out.println(dependingOn + "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@22222>>>>>>>> " + depenValuexd);

        final String depenValuex = depenValuexd;

        if (dependingOn != null && depenValuex != null) {

            System.out.println(dependingOn + "111111Not nulll @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@22222>>>>>>>> " + depenValuexd);

            ControlPropertyField controlPropertyField = fieldMap.get(dependingOn);

            System.out.println(controlPropertyField + "111111------- @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@22222>>>>>>>> " + controlPropertyField.getValue());

            setVisibilityDependencyForSectionAndLabel(dynamicField, depenValuex, controlPropertyField.getValue());
        }
        ;
    }

    public static void handleDependingOn(
            Map<String, ControlPropertyField> fieldMap,
            CampaignFormElement campaignFormElement,
            ControlPropertyField dynamicField,
            List<CampaignFormDataEntry> formValues) {
        final String dependingOn = campaignFormElement.getDependingOn();
        final String[] dependingOnValues = campaignFormElement.getDependingOnValues();

        List<String> constraints;
        String depenValuexd = null;
        if (dependingOnValues != null) {
            constraints = (List) Arrays.stream(dependingOnValues).collect(Collectors.toList());
            ListIterator<String> lstItemsx = constraints.listIterator();
            while (lstItemsx.hasNext()) {
                System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@1111>>>>>>>> " + lstItemsx);

                depenValuexd = lstItemsx.next().toString();
            }
        }

        System.out.println(dependingOn + "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@22222>>>>>>>> " + depenValuexd);

        final String depenValuex = depenValuexd;

        if (dependingOn != null && depenValuex != null) {
            ControlPropertyField controlPropertyField = fieldMap.get(dependingOn);
            setVisibilityDependency(dynamicField, depenValuex, controlPropertyField.getValue(), fieldMap, formValues, campaignFormElement);
            final ControlPropertyField finalDynamicField = dynamicField;
            controlPropertyField.addValueChangedListener(field -> {
                setVisibilityDependency(dynamicField, depenValuex, field.getValue(), fieldMap, formValues, campaignFormElement);
            });
        }
        ;
    }

    public static void setVisibilityDependency(ControlPropertyField field, String dependingOnValues, Object dependingOnFieldValue, Map<String, ControlPropertyField> fieldMap, List<CampaignFormDataEntry> formValues, CampaignFormElement campaignFormElement) {

        System.out.println(dependingOnValues + " =1111 static   @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@22>>>>>>>> dynamic = " + dependingOnFieldValue + "BBBTT" + field.getValue());

        String parsedDependingOnFieldValue = dependingOnFieldValue == null
                ? ""
                : dependingOnFieldValue instanceof Boolean
                ? YesNoUnknown.valueOf(((Boolean) dependingOnFieldValue).booleanValue()).name()
                : dependingOnFieldValue.toString().equalsIgnoreCase("Yes") ? "true" : dependingOnFieldValue.toString().equalsIgnoreCase("No") ? "false" : dependingOnFieldValue.toString();


        System.out.println(parsedDependingOnFieldValue + " parsedDependingOnFieldValue ========" + dependingOnValues.equalsIgnoreCase(parsedDependingOnFieldValue));
        System.out.println(parsedDependingOnFieldValue + " 22222parsedDependingOnFieldValue ========" + dependingOnValues.contains(parsedDependingOnFieldValue));


        if (dependingOnValues.contains("!")) {
            dependingOnValues = dependingOnValues.replace("!", "");
            if (dependingOnValues.contains(parsedDependingOnFieldValue)) {

                if (field.getValue() != null) {
                    clearFormValue(campaignFormElement.getId(), formValues);

                    field.setValue(null);
                }
                field.setVisibility(View.GONE);
            } else {
//                if (field.getValue() != null) {
//                    clearFormValue(campaignFormElement.getId(), formValues);
//                    field.setValue(null);
//                }

                field.setVisibility(View.VISIBLE);
            }
        } else {
            if (dependingOnValues.equalsIgnoreCase(parsedDependingOnFieldValue)) {

//                if (field.getValue() != null) {
//                    clearFormValue(campaignFormElement.getId(), formValues);
//                    field.setValue(null);
//                }

                field.setVisibility(View.VISIBLE);
            } else {
                if (field.getValue() != null) {
                    clearFormValue(campaignFormElement.getId(), formValues);

                    field.setValue(null);
                }
                field.setVisibility(View.GONE);
            }
        }
    }

    public static void setVisibilityDependency(ControlPropertyField field, String dependingOnValues, Object dependingOnFieldValue, Map<String, ControlPropertyField> fieldMap) {


        System.out.println(dependingOnValues + " =22222 static   @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@22>>>>>>>> dynamic = " + dependingOnFieldValue + "BBBTT" + field.getValue());

        String parsedDependingOnFieldValue = dependingOnFieldValue == null
                ? ""
                : dependingOnFieldValue instanceof Boolean
                ? YesNoUnknown.valueOf(((Boolean) dependingOnFieldValue).booleanValue()).name()
                : dependingOnFieldValue.toString().equalsIgnoreCase("Yes") ? "true" : dependingOnFieldValue.toString().equalsIgnoreCase("No") ? "false" : dependingOnFieldValue.toString();

        if (dependingOnValues.contains("!")) {
            dependingOnValues = dependingOnValues.replace("!", "");
            if (dependingOnValues.contains(parsedDependingOnFieldValue)) {
                field.setVisibility(View.GONE);
            } else {
                field.setVisibility(View.VISIBLE);
            }
        } else {
            if (dependingOnValues.equalsIgnoreCase(parsedDependingOnFieldValue)) {
                field.setVisibility(View.VISIBLE);
            } else {

                field.setVisibility(View.GONE);

            }
        }
    }

    public static void setVisibilityDependencyForSectionAndLabel(LinearLayout field, String dependingOnValues, Object dependingOnFieldValue) {
        System.out.println(dependingOnValues + " = static --  @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@22>>>>>>>> dynamic = " + dependingOnFieldValue);

        String parsedDependingOnFieldValue = dependingOnFieldValue == null
                ? ""
                : dependingOnFieldValue instanceof Boolean
                ? YesNoUnknown.valueOf(((Boolean) dependingOnFieldValue).booleanValue()).name()
                : dependingOnFieldValue.toString().equalsIgnoreCase("Yes") ? "true" : dependingOnFieldValue.toString().equalsIgnoreCase("No") ? "false" : dependingOnFieldValue.toString();

        System.out.println(" = dddddddddddddddddddddddddddddddd   @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@22>>>>>>>> dynamic = " + parsedDependingOnFieldValue);
        if (dependingOnValues.contains("!")) {
            dependingOnValues = dependingOnValues.replace("!", "");
            if (dependingOnValues.contains(parsedDependingOnFieldValue)) {
                field.setVisibility(View.GONE);
            } else {
                field.setVisibility(View.VISIBLE);
            }
        } else {
            if (dependingOnValues.equalsIgnoreCase(parsedDependingOnFieldValue)) {
                field.setVisibility(View.VISIBLE);
            } else {
                field.setVisibility(View.GONE);
            }
        }
    }
    public static void clearFormValue(String fieldId, List<CampaignFormDataEntry> formValues) {
        if (formValues != null) {
            formValues.removeIf(entry -> fieldId.equals(entry.getId()));
        }
    }

    private static boolean containsIgnoreCase(List<String> list, String soughtFor) {
        for (String current : list) {
            if (current.equalsIgnoreCase(soughtFor)) {
                return true;
            }
        }
        return false;
    }

    public static void recalculateAllExpressions(
            ExpressionParser expressionParser,
            List<CampaignFormDataEntry> formValues,
            List<CampaignFormElement> formElements,
            int maxPasses) {

        if (formElements == null || formElements.isEmpty()) {
            return;
        }

        for (int pass = 0; pass < Math.max(1, maxPasses); pass++) {
            boolean changed = false;

            for (CampaignFormElement element : formElements) {
                if (element == null || element.getExpression() == null || element.getExpression().trim().isEmpty()) {
                    continue;
                }

                CampaignFormDataEntry beforeEntry = getOrCreateCampaignFormDataEntry(formValues, element);
                Object before = beforeEntry.getValue();

                applyExpressionToEntry(expressionParser, formValues, element);

                Object after = beforeEntry.getValue();
                if ((before == null && after != null) || (before != null && !before.equals(after))) {
                    changed = true;
                }
            }

            if (!changed) {
                break;
            }
        }
    }

    public static void applyExpressionToEntry(
            ExpressionParser expressionParser,
            List<CampaignFormDataEntry> formValues,
            CampaignFormElement formElement) {

        if (formElement == null || formElement.getExpression() == null || formElement.getExpression().trim().isEmpty()) {
            return;
        }

        try {
            Object expressionValue = getExpressionValue(expressionParser, formValues, formElement.getExpression());
            CampaignFormElementType type = CampaignFormElementType.fromString(formElement.getType());
            CampaignFormDataEntry entry = getOrCreateCampaignFormDataEntry(formValues, formElement);

            if (expressionValue == null) {
                entry.setValue(null);
                return;
            }

            if (type == CampaignFormElementType.RANGE || type == CampaignFormElementType.NUMBER || type == CampaignFormElementType.DECIMAL) {
                if (expressionValue instanceof Boolean) {
                    Boolean b = (Boolean) expressionValue;
                    if (!b) {
                        entry.setValue(null);
                        return;
                    } else {
                        entry.setValue(null);
                        return;
                    }
                }
            }

            String raw = String.valueOf(expressionValue);
            String normalized = raw;

            switch (type) {
                case NUMBER:
                    try {
                        double num = Double.parseDouble(raw);
                        if (num == Math.floor(num)) {
                            normalized = String.valueOf((int) num);
                        } else {
                            normalized = String.format(Locale.US, "%.2f", num);
                        }
                    } catch (NumberFormatException ignored) {
                        normalized = raw;
                    }
                    break;

                case DECIMAL:
                    if (expressionValue instanceof Number) {
                        double num = ((Number) expressionValue).doubleValue();
                        if (num == Math.floor(num)) {
                            normalized = String.valueOf((int) num);
                        } else {
                            normalized = String.format(Locale.US, "%.2f", num);
                        }
                    } else {
                        normalized = raw;
                    }
                    break;

                case RANGE:
                    try {
                        double num = Double.parseDouble(raw);
                        if (num == Math.floor(num)) {
                            normalized = String.valueOf((int) num);
                        } else {
                            normalized = String.format(Locale.US, "%.2f", num);
                        }
                    } catch (NumberFormatException ignored) {
                        normalized = raw;
                    }
                    break;
                case YES_NO:
                    normalized = raw;
                    break;
                default:
                    normalized = raw;
                    break;
            }

            entry.setValue(normalized);

        } catch (Exception e) {
            Log.e("ExpressionApply", "Failed to apply expression for field " + formElement.getId(), e);
        }
    }

    public static CampaignFormDataEntry getOrCreateCampaignFormDataEntry(
            List<CampaignFormDataEntry> formValues,
            CampaignFormElement campaignFormElement) {
        for (CampaignFormDataEntry campaignFormDataEntry : formValues) {
            if (campaignFormDataEntry.getId().equals(campaignFormElement.getId())) {
                return campaignFormDataEntry;
            }
        }
        final CampaignFormDataEntry newCampaignFomDataEntry = new CampaignFormDataEntry(campaignFormElement.getId(), null);
        formValues.add(newCampaignFomDataEntry);
        return newCampaignFomDataEntry;
    }

    public static Map<String, String> getUserTranslations(CampaignFormMeta campaignFormMeta) {
        final Map<String, String> userTranslations = new HashMap<>();

        final Locale locale = I18nProperties.getUserLanguage().getLocale();
        if (locale != null) {
            final List<CampaignFormTranslations> campaignFormTranslations = campaignFormMeta.getCampaignFormTranslations();
            campaignFormTranslations.forEach(cft -> {
                if (cft.getLanguageCode().equalsIgnoreCase(locale.toString())) {
                    cft.getTranslations()
                            .forEach(translationElement -> userTranslations.put(translationElement.getElementId(), translationElement.getCaption()));
                }
            });
        }
        return userTranslations;
    }

    public static String getUserLanguageCaption(Map<String, String> userTranslations, CampaignFormElement campaignFormElement) {
        if (userTranslations != null && userTranslations.containsKey(campaignFormElement.getId())) {
            return userTranslations.get(campaignFormElement.getId());
        } else {
            return campaignFormElement.getCaption();
        }
    }

    public static String getUserLanguageHint(Map<String, String> userHints, CampaignFormElement campaignFormElement) {
        if (userHints != null && userHints.containsKey(campaignFormElement.getId())) {
            return userHints.get(campaignFormElement.getId());
        } else {
            return campaignFormElement.getHint();
        }
    }

    public static Map<String, String> getUserHints(CampaignFormMeta campaignFormMeta) {
        Map<String, String> userHints = new HashMap<>();

        System.out.println("Getting User Hintssssssssssssssssssssssssssssssssss");
        List<CampaignFormTranslations> campaignFormTranslations = campaignFormMeta.getCampaignFormTranslations();
        Locale locale = I18nProperties.getUserLanguage().getLocale();

        if (campaignFormTranslations != null && locale != null) {
            campaignFormTranslations.forEach(cft -> {
                if (cft.getLanguageCode().equalsIgnoreCase(locale.toString())) {
                    cft.getTranslations()
                            .stream()
                            .filter(translationElement -> translationElement != null)
                            .forEach(translationElement -> userHints.put(translationElement.getElementId(), translationElement.getHint() != null ? translationElement.getHint() : ""));
                }
            });
        }
        return userHints;

    }

    public static ControlTextEditField createControlTextEditField(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations,
            Map<String, String> userHints,
            Boolean isIntegerField,
            Boolean isRequired) {
        return new ControlTextEditField(context) {

            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getHelpText() {
                return getUserLanguageHint(userHints, campaignFormElement);
            }

            @Override
            public int getTextAlignment() {
                return View.TEXT_ALIGNMENT_VIEW_START;
            }

            @Override
            public int getGravity() {
                return Gravity.CENTER_VERTICAL;
            }

            @Override
            public int getMaxLines() {
                return 1;
            }

            @Override
            public int getMaxLength() {
                return CHARACTER_LIMIT_DEFAULT;
            }

            //	@Override
            //	public int getMinLength() {
            //		return DEFAULT_MIN_LENGTH;
            //	}
//
            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);
                initLabel();
                initLabelAndValidationListeners();
                setLiveValidationDisabled(true);
                initInput(isIntegerField, isRequired, false, null, null, false, false);
                displayHelpText();
                if (isRequired) {
                    setRequired(true);
                }
            }
        };
    }

    public static ControlValidatedTextField createControlValidatedTextField(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations,
            Map<String, String> userHints) {
        return new ControlValidatedTextField(context) {

            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getHelpText() {
                return getUserLanguageHint(userHints, campaignFormElement);
            }

            @Override
            public int getTextAlignment() {
                return View.TEXT_ALIGNMENT_VIEW_START;
            }

            @Override
            public int getGravity() {
                return Gravity.CENTER_VERTICAL;
            }

            @Override
            public int getMaxLines() {
                return 1;
            }

            @Override
            public int getMaxLength() {
                return CHARACTER_LIMIT_DEFAULT;
            }

            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);
                initLabel();
                initLabelAndValidationListeners();
                setLiveValidationDisabled(true);
                initInput(false, false, false, null, null, false, false);
                displayHelpText();
                setEnabled(false);
            }
        };
    }

    public static ControlTextBoxField createControlTextBoxField(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations,
            Map<String, String> userHints) {
        return new ControlTextBoxField(context) {

            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getHelpText() {
                return getUserLanguageHint(userHints, campaignFormElement);
            }

            @Override
            public int getTextAlignment() {
                return View.TEXT_ALIGNMENT_VIEW_START;
            }

            @Override
            public int getGravity() {
                return Gravity.CENTER_VERTICAL;
            }

            @Override
            public int getMaxLines() {
                return Integer.MAX_VALUE;
            }

            @Override
            public int getMaxLength() {
                return CHARACTER_LIMIT_DEFAULT;
            }

            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);
                initLabel();
                initLabelAndValidationListeners();
                setLiveValidationDisabled(true);
                initInput(campaignFormElement.isImportant());
//                initInput(false, false, false, null, null, false, false);
                displayHelpText();
                setEnabled(true);
            }
        };
    }

    public static ControlTextBoxField createControlTextBoxReadField(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations) {
        return new ControlTextBoxField(context) {

            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

//            @Override
//            protected String getHelpText() {
//                return getUserLanguageHint(userHints, campaignFormElement);
//            }

            @Override
            public int getTextAlignment() {
                return View.TEXT_ALIGNMENT_VIEW_START;
            }

            @Override
            public int getGravity() {
                return Gravity.CENTER_VERTICAL;
            }

            @Override
            public int getMaxLines() {
                return Integer.MAX_VALUE;
            }

            @Override
            public int getMaxLength() {
                return CHARACTER_LIMIT_DEFAULT;
            }

            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);
                initLabel();
                initLabelAndValidationListeners();
                setLiveValidationDisabled(true);
                initInput(campaignFormElement.isImportant());
                setEnabled(false);
            }
        };
    }

    public static ControlTextEditFieldAllowZeroInput createControlTextEditFieldAllowStartingZero(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations,
            Boolean isIntegerField,
            Boolean isRequired) {
        return new ControlTextEditFieldAllowZeroInput(context) {

            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            public int getTextAlignment() {
                return View.TEXT_ALIGNMENT_VIEW_START;
            }

            @Override
            public int getGravity() {
                return Gravity.CENTER_VERTICAL;
            }

            @Override
            public int getMaxLines() {
                return 1;
            }

            @Override
            public int getMaxLength() {
                return CHARACTER_LIMIT_DEFAULT;
            }

            //
            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);
                initLabel();
                initLabelAndValidationListeners();
                setLiveValidationDisabled(true);
                initInput(isIntegerField, isRequired, false, null, null, false, false);
                displayHelpText();
                if (isRequired) {
                    setRequired(true);
                }
            }
        };
    }


    public static ControlPhoneField createControlPhoneField(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations,
            Map<String, String> userHints,
            Boolean isIntegerField,
            Boolean isRequired) {
        return new ControlPhoneField(context) {

            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getHelpText() {
                return getUserLanguageHint(userHints, campaignFormElement);
            }

            @Override
            public int getTextAlignment() {
                return View.TEXT_ALIGNMENT_VIEW_START;
            }

            @Override
            public int getGravity() {
                return Gravity.CENTER_VERTICAL;
            }

            @Override
            public int getMaxLines() {
                return 1;
            }

            @Override
            public int getMaxLength() {
                return CHARACTER_LIMIT_DEFAULT;
            }

            //	@Override
            //	public int getMinLength() {
            //		return DEFAULT_MIN_LENGTH;
            //	}
//
            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);
                initLabel();
                initLabelAndValidationListeners();
                setLiveValidationDisabled(true);
                initInput(isIntegerField, isRequired, false, null, null, false, false);
                displayHelpText();
                if (isRequired) {
                    setRequired(true);
                }
            }
        };
    }


    public static ControlTextEditField createControlTextEditFieldWithErrorMessage(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations,
            Boolean isIntegerField,
            Boolean isRequired,
            String errorMsg) {
        return new ControlTextEditField(context) {

            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            public int getTextAlignment() {
                return View.TEXT_ALIGNMENT_VIEW_START;
            }

            @Override
            public int getGravity() {
                return Gravity.CENTER_VERTICAL;
            }

            @Override
            public int getMaxLines() {
                return 1;
            }

            @Override
            public int getMaxLength() {
                return CHARACTER_LIMIT_DEFAULT;
            }

            //	@Override
            //	public int getMinLength() {
            //		return DEFAULT_MIN_LENGTH;
            //	}

            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);
                initLabel();
                initLabelAndValidationListenersErrorMsg(errorMsg);
                setLiveValidationDisabled(false);
                initInput(isIntegerField, isRequired, false, null, null, false, false);
                displayHelpText();
                if (isRequired) {
                    setRequired(true);
                }
            }
        };
    }


    public static ControlTextEditField createControlTextEditFieldRangex(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations,
            Boolean isIntegerField,
            Boolean isRequired,
            String errorMsg,
            Boolean warnOnError) {
        return new ControlTextEditField(context) {

            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            public int getTextAlignment() {
                return View.TEXT_ALIGNMENT_VIEW_START;
            }

            @Override
            public int getGravity() {
                return Gravity.CENTER_VERTICAL;
            }

            @Override
            public int getMaxLines() {
                return 1;
            }

            @Override
            public int getMaxLength() {
                return CHARACTER_LIMIT_DEFAULT;
            }


            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);
                initLabel();
//                initLabelAndValidationListeners();
                initLabelAndValidationListenersErrorMsg(errorMsg);
                setLiveValidationDisabled(true);
                initInput(isIntegerField, isRequired, true, null, null, true, false);
                displayHelpText();
            }
        };
    }


    public static ControlTextEditField createControlTextEditFieldRangex(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations,
            Boolean isIntegerField,
            Boolean isRequired,
            String errorMsg) {
        return new ControlTextEditField(context) {

            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            public int getTextAlignment() {
                return View.TEXT_ALIGNMENT_VIEW_START;
            }

            @Override
            public int getGravity() {
                return Gravity.CENTER_VERTICAL;
            }

            @Override
            public int getMaxLines() {
                return 1;
            }

            @Override
            public int getMaxLength() {
                return CHARACTER_LIMIT_DEFAULT;
            }


            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);
                initLabel();
                initLabelAndValidationListeners();
//                initLabelAndValidationListenersErrorMsg(errorMsg);
                setLiveValidationDisabled(true);
                initInput(isIntegerField, isRequired, true, null, null, true, false);
                displayHelpText();
            }
        };
    }


//    public static ControlTextEditField createControlTextEditFieldRange(
//            CampaignFormElement campaignFormElement,
//            Context context,
//            Map<String, String> userTranslations,
//            Boolean isIntegerField,
//            Boolean isRequired,
//            Integer minVal,
//            Integer maxVal,
//            Boolean isExpression,
//            Boolean warnOnError) {
//
//        System.out.println(context + " --------------------- running range stage 1 : " + isExpression);
//        final boolean isExpressionx = isExpression;
//        return new ControlTextEditField(context) {
//
//
//            @Override
//            protected String getPrefixDescription() {
//                return getUserLanguageCaption(userTranslations, campaignFormElement);
//            }
//
//            @Override
//            protected String getPrefixCaption() {
//                return getUserLanguageCaption(userTranslations, campaignFormElement);
//            }
//
//            @Override
//            public int getTextAlignment() {
//                return View.TEXT_ALIGNMENT_VIEW_START;
//            }
//
//            @Override
//            public int getGravity() {
//                return Gravity.CENTER_VERTICAL;
//            }
//
//            @Override
//            public int getMaxLines() {
//                return 1;
//            }
//
//            @Override
//            public int getMaxLength() {
//                return 8;
//            }
//
//            @Override
//            public int getMinLength() {
//                return 1;
//            }
//
//            //
//            @Override
//            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
//                super.inflateView(context, attrs, defStyle);
//                initLabel();
//                initLabelAndValidationListeners();
//                setLiveValidationDisabled(true);
//                initInput(isIntegerField, isRequired, true, minVal, maxVal, isExpressionx, warnOnError);
//                displayHelpText();
//            }
//        };
//    }


    public static ControlTextEditFieldRange createControlTextEditFieldRangexOnlyExpression(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations,
            Boolean isIntegerField,
            Boolean isRequired,
            String errorMsg,
            Boolean warnOnError,
            List<CampaignFormElement> campaignFormElements) {
        return new ControlTextEditFieldRange(context) {

            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            public int getTextAlignment() {
                return View.TEXT_ALIGNMENT_VIEW_START;
            }

            @Override
            public int getGravity() {
                return Gravity.CENTER_VERTICAL;
            }

            @Override
            public int getMaxLines() {
                return 1;
            }

            @Override
            public int getMaxLength() {
                return CHARACTER_LIMIT_DEFAULT;
            }


            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);
                initLabel();
                initLabelAndValidationListeners();
//                initLabelAndValidationListenersErrorMsg(errorMsg);
                setLiveValidationDisabled(true);
//                initInput(isIntegerField, isRequired, true, null, null, true, false);

                initInput(true, isRequired, true, null, null, true, warnOnError, campaignFormElements);
                displayHelpText();
                if (isRequired) {
                    setRequired(true);
                }
            }
        };
    }

    public static ControlTextEditFieldRange createControlTextEditFieldRangeOnly(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations,
            Map<String, String> userHints,
            Boolean isIntegerField,
            Boolean isRequired,
            Integer minVal,
            Integer maxVal,
            Boolean isExpression,
            Boolean warnOnError,
            List<CampaignFormElement> campaignFormElements) {

        System.out.println(context + " --------------------- running range stage 1 : " + isExpression);
        final boolean isExpressionx = isExpression;
        return new ControlTextEditFieldRange(context) {


            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getHelpText() {
                return getUserLanguageHint(userHints, campaignFormElement);
            }

            @Override
            public int getTextAlignment() {
                return View.TEXT_ALIGNMENT_VIEW_START;
            }

            @Override
            public int getGravity() {
                return Gravity.CENTER_VERTICAL;
            }

            @Override
            public int getMaxLines() {
                return 1;
            }

            @Override
            public int getMaxLength() {
                return 8;
            }

            @Override
            public int getMinLength() {
                return 1;
            }

            //
            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);
                initLabel();
                initLabelAndValidationListeners();
                setLiveValidationDisabled(true);
//                initInput(isIntegerField, isRequired, true, minVal, maxVal, isExpressionx, warnOnError);
                initInput(true, isRequired, true, minVal, maxVal, isExpressionx, warnOnError, campaignFormElements);
                displayHelpText();
                if (isRequired) {
                    setRequired(true);
                }
            }
        };
    }


    public static ControlDecimalEditField createControlTextEditFieldDecimal(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations,
            Map<String, String> userHints,
            Boolean isDecimalField,
            Boolean isRequired,
            Integer minVal,
            Integer maxVal,
            Boolean isExpression,
            Boolean warnOnError) {
        final boolean isExpressionx = isExpression;
        return new ControlDecimalEditField(context) {
            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getHelpText() {
                return getUserLanguageHint(userHints, campaignFormElement);
            }

            @Override
            public int getTextAlignment() {
                return View.TEXT_ALIGNMENT_VIEW_START;
            }

            @Override
            public int getGravity() {
                return Gravity.CENTER_VERTICAL;
            }

            @Override
            public int getMaxLines() {
                return 1;
            }

            @Override
            public int getMaxLength() {
                return 8;
            }

            @Override
            public int getMinLength() {
                return 1;
            }


            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);
                initLabel();
                initLabelAndValidationListeners();
                setLiveValidationDisabled(true);
                initInput(isDecimalField, isRequired, true, minVal, maxVal, isExpressionx, warnOnError);
//                setVisibility(GONE);
                displayHelpText();
                if (isRequired) {
                    setRequired(true);
                }
            }
        };
    }

    public static ControlDecimalEditField createControlTextEditFieldDecimalExpression(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations,
            Boolean isDecimalField,
            Boolean isRequired,
            String errorMsg) {

        System.out.println(context + " --------------------- running decimal stage 1 : " + isDecimalField);
        return new ControlDecimalEditField(context) {

            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            public int getTextAlignment() {
                return View.TEXT_ALIGNMENT_VIEW_START;
            }

            @Override
            public int getGravity() {
                return Gravity.CENTER_VERTICAL;
            }

            @Override
            public int getMaxLines() {
                return 1;
            }

            @Override
            public int getMaxLength() {
                return CHARACTER_LIMIT_DEFAULT;
            }

            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);
                initLabel();
//                initLabelAndValidationListeners();
                initLabelAndValidationListenersErrorMsg(errorMsg);
                setLiveValidationDisabled(true);
                initInput(isDecimalField, isRequired, true, null, null, true, false);
                displayHelpText();
                if (isRequired) {
                    setRequired(true);
                }
            }
        };
    }


    public static ControlImageEditField createControlImageEditField(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations,
            Map<String, String> userHints,
            boolean isRequired,
            ActivityResultLauncher<Intent> launcher,
            ActivityResultLauncher<String> permissionLauncher) {

        return new ControlImageEditField(context) {
            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getHelpText() {
                return getUserLanguageHint(userHints, campaignFormElement);
            }

            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);
                initLabel();
                initLabelAndValidationListeners();
                setLiveValidationDisabled(true);
                configureForImageCapture(campaignFormElement, launcher, permissionLauncher);
                displayHelpText();
                if (isRequired) {
                    setRequired(true);
                }
            }
        };
    }

    public static ControlImageEditField createControlImageReadField(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations) {

        return new ControlImageEditField(context) {
            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);
                initLabel();
                initLabelAndValidationListeners();
                setLiveValidationDisabled(true);
                setReadOnly(true);
            }
        };
    }


    //to fix required for dropdown
    public static ControlSpinnerField createControlSpinnerFieldEditField(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations,
            Map<String, String> userHints,
            Map<String, String> isIntegerField,
            boolean isRequired) {
        return new ControlSpinnerField(context) {

            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getHelpText() {
                return getUserLanguageHint(userHints, campaignFormElement);
            }

            @Override
            public int getTextAlignment() {
                return View.TEXT_ALIGNMENT_VIEW_START;
            }

            @Override
            public int getGravity() {
                return Gravity.CENTER_VERTICAL;
            }

            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);
                initLabel();
                initLabelAndValidationListeners();
                setLiveValidationDisabled(true);
                initInput(isIntegerField, isRequired);
                displayHelpText();
                if (isRequired) {
                    setRequired(true);
                }
            }
        };
    }


    public static ControlSpinnerField createControlSpinnerFieldEditField(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations,
            Map<String, String> userHints,
//            Map<String, String> isIntegerField) {
            Map<String, String> optionsList) {


        return new ControlSpinnerField(context) {

            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getHelpText() {
                return getUserLanguageHint(userHints, campaignFormElement);
            }

            @Override
            public int getTextAlignment() {
                return View.TEXT_ALIGNMENT_VIEW_START;
            }

            @Override
            public int getGravity() {
                return Gravity.CENTER_VERTICAL;
            }

            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);
                initLabel();
                initLabelAndValidationListeners();
                setLiveValidationDisabled(true);
//                initInput(isIntegerField);

                initInput(optionsList);
                displayHelpText();
            }
        };
    }


    public static ControlDateField createControlDateEditField(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations,
            Map<String, String> userHints,
            Boolean isIntegerField,
            FragmentManager fm, boolean isRequired) {
        return new ControlDateField(context) {

            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getHelpText() {
                return getUserLanguageHint(userHints, campaignFormElement);
            }

            @Override
            public int getTextAlignment() {
                return View.TEXT_ALIGNMENT_VIEW_START;
            }

            @Override
            public int getGravity() {
                return Gravity.CENTER_VERTICAL;
            }


            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);
                initLabel();
                initLabelAndValidationListeners();
                setLiveValidationDisabled(true);
                initializeDateField(fm);
                initInput(true, isRequired);
                displayHelpText();
                if (isRequired) {
                    setRequired(true);
                }
            }
        };
    }



    public static ControlCheckBoxGroupField createControlMultiSelectCheckBoxEditField(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations,
            Map<String, String> optionValues,
            List<?> selectedKeys,
            boolean isRequired) {

        return new ControlCheckBoxGroupField(context) {
            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            public int getTextAlignment() {
                return View.TEXT_ALIGNMENT_VIEW_START;
            }

            @Override
            public int getGravity() {
                return Gravity.CENTER_VERTICAL;
            }

            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);

                // Initialize the parent field components
                initLabel();
                initLabelAndValidationListeners();
                setLiveValidationDisabled(false);

                // Set the group label text from campaign form element
                String labelText = getUserLanguageCaption(userTranslations, campaignFormElement);
                if (labelText != null && !labelText.isEmpty()) {
                    setGroupLabel(labelText);
                }

                if (isRequired) {
                    setRequired(true);
                }

                // Convert selectedKeys to proper format
                List<String> finalSelectedKeys = new ArrayList<>();
                if (selectedKeys != null && !selectedKeys.isEmpty()) {
                    for (Object key : selectedKeys) {
                        if (key != null) {
                            String strKey = key.toString().trim();
                            if (!strKey.isEmpty()) {
                                finalSelectedKeys.add(strKey);
                            }
                        }
                    }
                }

                // Set up the checkbox options with values
                if (optionValues != null && !optionValues.isEmpty()) {
                    System.out.println("DEBUG - Setting options with selected keys: " + finalSelectedKeys);
                    setOptionsAndValue(optionValues, finalSelectedKeys);
                }
                displayHelpText();
            }
        };
    }

//    public static ControlCheckBoxGroupField createControlMultiSelectCheckBoxEditField(
//            CampaignFormElement campaignFormElement,
//            Context context,
//            Map<String, String> userTranslations,
//            Map<String, String> optionValues,
//            List<?> selectedKeys,
//            boolean isRequired) {
//
//        return new ControlCheckBoxGroupField(context) {
//            protected String getPrefixDescription() {
//                return getUserLanguageCaption(userTranslations, campaignFormElement);
//            }
//
//            @Override
//            protected String getPrefixCaption() {
//                return getUserLanguageCaption(userTranslations, campaignFormElement);
//            }
//
//            @Override
//            public int getTextAlignment() {
//                return View.TEXT_ALIGNMENT_VIEW_START;
//            }
//
//            @Override
//            public int getGravity() {
//                return Gravity.CENTER_VERTICAL;
//            }
//
//            @Override
//            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
//                super.inflateView(context, attrs, defStyle);
//
//                // Add debug logging
//                System.out.println("DEBUG Creating checkbox field for: " + campaignFormElement.getId());
//                System.out.println("DEBUG Selected keys passed: " + selectedKeys);
//                System.out.println("DEBUG Options available: " + optionValues);
//
//                // Initialize the parent field components
//                initLabel();
//                initLabelAndValidationListeners();
//                setLiveValidationDisabled(false);
//
//                // Set the group label text from campaign form element
//                String labelText = getUserLanguageCaption(userTranslations, campaignFormElement);
//                if (labelText != null && !labelText.isEmpty()) {
//                    setGroupLabel(labelText);
//                }
//
//                if (isRequired) {
//                    setRequired(true);
//                }
//
//                // Set up the checkbox options
//                if (optionValues != null && !optionValues.isEmpty()) {
//                    // Convert selectedKeys to proper format for the field
//                    List<String> finalSelectedKeys = new ArrayList<>();
//                    if (selectedKeys != null && !selectedKeys.isEmpty()) {
//                        for (Object key : selectedKeys) {
//                            if (key != null) {
//                                String strKey = key.toString().trim();
//                                if (!strKey.isEmpty()) {
//                                    finalSelectedKeys.add(strKey);
//                                }
//                            }
//                        }
//                    }
//
//                    System.out.println("DEBUG - Setting options with selected keys: " + finalSelectedKeys);
    ////                    setOptionsAndValue(optionValues, finalSelectedKeys);
//
//                }
//            }
//        };
//    }


    public static ControlCheckBoxGroupField createControlMultiSelectCheckBoxField(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations,
            Map<String, String> optionValues,
            boolean isRequired) {

        return new ControlCheckBoxGroupField(context) {

            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            public int getTextAlignment() {
                return View.TEXT_ALIGNMENT_VIEW_START;
            }

            @Override
            public int getGravity() {
                return Gravity.CENTER_VERTICAL;
            }

            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);

                // Initialize the parent field components
                initLabel();
                initLabelAndValidationListeners();
                setLiveValidationDisabled(false);

                // Set the group label text from campaign form element
                String labelText = getUserLanguageCaption(userTranslations, campaignFormElement);
                if (labelText != null && !labelText.isEmpty()) {
//                    setGroupLabel(labelText);
                }

                // Set up the checkbox options
                if (optionValues != null && !optionValues.isEmpty()) {
                    setOptions(optionValues);
                }

//                 Configure required field if needed
                if (isRequired) {
                    System.out.println("DEBUG createControlMultiSelectCheckBoxField - calling setRequired(true)");
                    setRequired(true);
                    System.out.println("DEBUG createControlMultiSelectCheckBoxField - after setRequired, required field is: " + required);
                }
                displayHelpText();
            }

        };
    }


    public static ControlCheckBoxField createControlCheckBoxField(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations,
            Map<String, String> userHints) {
        return new ControlCheckBoxField(context) {

            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getHelpText() {
                return getUserLanguageHint(userHints, campaignFormElement);
            }

            @Override
            public int getTextAlignment() {
                return View.TEXT_ALIGNMENT_VIEW_START;
            }

            @Override
            public int getGravity() {
                return Gravity.CENTER_VERTICAL;
            }

            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);
                initLabel();
                initLabelAndValidationListeners();
                setLiveValidationDisabled(true);
                //required = true;

                initInput();
                displayHelpText();
            }
        };
    }


    public static ControlSwitchField createControlYesNoUnknownField(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations,
            Map<String, String> userHints) {
        return new ControlSwitchField(context) {


            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getHelpText() {
                return getUserLanguageHint(userHints, campaignFormElement);
            }

            @Override
            public int getTextAlignment() {
                return View.TEXT_ALIGNMENT_VIEW_START;
            }

            @Override
            public int getGravity() {
                return Gravity.CENTER_VERTICAL;
            }

            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);
                initLabel();
                initLabelAndValidationListeners();
                required = true;

                // initialize();
                setLiveValidationDisabled(true);
                initInputFirst();
                displayHelpText();
            }
        };
    }

    public static ControlTextReadField createControlTextReadField(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations) {
        return new ControlTextReadField(context) {

            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            public int getTextAlignment() {
                return View.TEXT_ALIGNMENT_VIEW_START;
            }

            @Override
            public int getGravity() {
                return Gravity.CENTER_VERTICAL;
            }

            @Override
            public int getMaxLines() {
                return 5;
            }

            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);
                initLabel();
                initTextView();
                displayHelpText();
            }
        };
    }

    public static ControlTimeField createControlTimeEditField(
            CampaignFormElement campaignFormElement,
            Context context,
            Map<String, String> userTranslations,
            Boolean isIntegerField,
            FragmentManager fm,
            boolean isRequired) {

        return new ControlTimeField(context) {

            @Override
            protected String getPrefixDescription() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            protected String getPrefixCaption() {
                return getUserLanguageCaption(userTranslations, campaignFormElement);
            }

            @Override
            public int getTextAlignment() {
                return View.TEXT_ALIGNMENT_VIEW_START;
            }

            @Override
            public int getGravity() {
                return Gravity.CENTER_VERTICAL;
            }

            @Override
            protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
                super.inflateView(context, attrs, defStyle);
                initLabel();
                initLabelAndValidationListeners();
                setLiveValidationDisabled(true);
                initializeTimeField(fm);
                initInput(false, isRequired, false, 0, 1000, false, false);
                displayHelpText();
                if (isRequired) {
                    setRequired(true);
                }
            }
        };
    }

    public class ControlEmailEditField extends ControlTextEditField {
        private String errorMessage;
        private boolean isRequired;

        public ControlEmailEditField(Context context, String errorMessage, boolean isRequired) {
            super(context);
            this.errorMessage = errorMessage;
            this.isRequired = isRequired;
            initEmailField();
        }

        private void initEmailField() {
            // Access the EditText through the appropriate method from parent class
            // Assuming your parent class has a method to get the input view
            TextView editText = getInputView(); // Or whatever method provides the input field

            if (editText != null) {
                editText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

                addValueChangedListener(new ValueChangeListener() {
                    @Override
                    public void onChange(ControlPropertyField field) {
                        validateEmail(field);
                    }
                });
            }
        }

        private void validateEmail(ControlPropertyField field) {
            String input = field.getValue() != null ? field.getValue().toString() : "";

            if (input.isEmpty() && !isRequired) {
                disableErrorState();
                return;
            }

            boolean isValid = android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches();

            if (!isValid) {
                enableErrorState(errorMessage);
            } else {
                disableErrorState();
            }
        }

        // Helper method to get the input view
        // This should match whatever method your parent class uses
        private EditText getInputView() {
            // Check if your parent class has a method like getEditText()
            // If not, you may need to find the view by ID
            try {
                return findViewById(R.id.date_input); // Use your actual EditText ID
            } catch (Exception e) {
                return null;
            }
        }
    }

}