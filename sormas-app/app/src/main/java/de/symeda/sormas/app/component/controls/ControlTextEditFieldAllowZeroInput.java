/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.app.component.controls;

import static de.symeda.sormas.app.core.notification.NotificationType.WARNING;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;

import de.symeda.sormas.api.utils.FieldConstraints;
import de.symeda.sormas.app.R;
import de.symeda.sormas.app.backend.config.ConfigProvider;
import de.symeda.sormas.app.component.VisualState;
import de.symeda.sormas.app.component.VisualStateControlType;
import de.symeda.sormas.app.core.NotificationContext;
import de.symeda.sormas.app.core.notification.NotificationHelper;

public class ControlTextEditFieldAllowZeroInput extends ControlPropertyEditField<String> {

    // Views

    protected EditText input;

    // Attributes

    private boolean singleLine;
    private int maxLines;
    private int maxLength;
    private int minLength;
    private boolean textArea;
    private int inputType;
    boolean isInternalChange = false;


    private boolean enableZeroHandling = false;


    // Listeners

    protected InverseBindingListener inverseBindingListener;
    private OnClickListener onClickListener;

    // Constructors

    public ControlTextEditFieldAllowZeroInput(Context context) {
        super(context);
    }

    public ControlTextEditFieldAllowZeroInput(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ControlTextEditFieldAllowZeroInput(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    // Instance methods

    public void setCursorToRight() {
        input.setSelection(input.getText().length());
    }

    /**
     * Handles clicks on the buttons to switch to the next view.
     */
    private void setUpOnEditorActionListener() {
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                int definedActionId = v.getImeActionId();
                if (definedActionId == EditorInfo.IME_ACTION_NONE) {
                    return false;
                }

                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    int id = getNextFocusForwardId();
                    if (id != NO_ID) {
                        View nextView = v.getRootView().findViewById(id);
                        if (nextView != null && nextView.getVisibility() == VISIBLE) {
                            if (nextView instanceof ControlTextEditFieldAllowZeroInput) {
                                requestFocusForContentView(nextView);
                            } else if (nextView instanceof ControlPropertyField) {
                                ((ControlPropertyField) nextView).requestFocusForContentView(nextView);
                            } else {
                                nextView.requestFocus();
                            }
                        }
                    }

                    return true;
                }

                return false;
            }
        });
    }

    private void setUpOnFocusChangeListener() {
        input.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!v.isEnabled()) {
                    return;
                }

                showOrHideNotifications(hasFocus);

                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

                if (imm != null) {
                    if (hasFocus) {
                        changeVisualState(VisualState.FOCUSED);
                        imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
                        // Prevent the content from being automatically selected
                        input.setSelection(input.getText().length(), input.getText().length());
                        if (onClickListener != null) {
                            input.setOnClickListener(onClickListener);
                        }
                    } else {
                        if (hasError) {
                            changeVisualState(VisualState.ERROR);
                        } else {
                            changeVisualState(VisualState.NORMAL);
                        }
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        input.setOnClickListener(null);
                    }
                }
            }
        });
    }

    private void initializeOnClickListener() {
        if (onClickListener != null) {
            return;
        }

        onClickListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!v.isEnabled()) {
                    return;
                }

                showOrHideNotifications(v.hasFocus());

                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

                if (imm != null) {
                    if (v.hasFocus()) {
                        imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
                        //// Prevent the content from being automatically selected
                        //input.setSelection(input.getText().length(), input.getText().length());
                    } else {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        };
    }

    // Overrides

    @Override
    public String getValue() {
        return (String) super.getValue();
    }

    @Override
    protected String getFieldValue() {
        if (input.getText() == null) {
            return null;
        }
        return input.getText().toString();
    }

    @Override
    protected void setFieldValue(String value) {
//        System.out.println("SEtting fireld value i text edit field =============" + value);
        input.setText(value);
    }

//
//    public void setFieldAsError(String errorMessage) {
//        enableErrorState(errorMessage);
////        setErrorIfEmptyRange();
//    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled); // this has to be called first
        input.setEnabled(enabled);
        label.setEnabled(enabled);
    }

    @Override
    public void setHint(String hint) {
        this.hint = hint;
        input.setHint(hint);
    }

    @Override
    protected void initialize(Context context, AttributeSet attrs, int defStyle) {
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ControlTextEditField, 0, 0);

            try {
                singleLine = a.getBoolean(R.styleable.ControlTextEditField_singleLine, true);
                maxLines = a.getInt(R.styleable.ControlTextEditField_maxLines, 1);
                textArea = a.getBoolean(R.styleable.ControlTextEditField_textArea, false);
                maxLength = a.getInt(
                        R.styleable.ControlTextEditField_maxLength,
                        textArea ? FieldConstraints.CHARACTER_LIMIT_BIG : FieldConstraints.CHARACTER_LIMIT_UUID_MIN);

                minLength = a.getInt(
                        R.styleable.ControlTextEditField_minLength,
                        textArea ? FieldConstraints.CHARACTER_LIMIT_BIG : FieldConstraints.CHARACTER_LIMIT_DEFAULT);
                inputType = a.getInt(R.styleable.ControlTextEditField_inputType, InputType.TYPE_CLASS_TEXT);
            } finally {
                a.recycle();
            }
        }
    }

    @Override
    protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (inflater != null) {
            if (textArea) {
                inflater.inflate(R.layout.control_textfield_edit_multi_row_layout, this);
            } else if (isSlim()) {
                inflater.inflate(R.layout.control_textfield_edit_slim_layout, this);
            } else {
                inflater.inflate(R.layout.control_textfield_edit_layout, this);
            }
        } else {
            throw new RuntimeException("Unable to inflate layout in " + getClass().getName());
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        initInput(false, false, false, null, null, false, false);
    }




    protected void initInput(boolean isIntegerFlag, boolean isRequired, boolean isRange, Integer minValue, Integer maxValue, Boolean isExpression, Boolean warnOnError) {

        input = (EditText) this.findViewById(R.id.text_input);
        input.setTextAlignment(getTextAlignment());
        if (getTextAlignment() == View.TEXT_ALIGNMENT_GRAVITY) {
            input.setGravity(getGravity());
        }
        if (isIntegerFlag) {
            input.setInputType(InputType.TYPE_CLASS_NUMBER |
                    InputType.TYPE_NUMBER_FLAG_DECIMAL |
                    InputType.TYPE_NUMBER_FLAG_SIGNED);
        } else {

            input.setInputType(inputType);
        }
        setSingleLine(singleLine);
        if (getMaxLength() >= 0) {
            input.setFilters(
                    new InputFilter[]{
                            new InputFilter.LengthFilter(240)}
            );
        }
//// After setting LengthFilter
        InputFilter[] existing = input.getFilters();
        boolean isNumeric = (input.getInputType() & InputType.TYPE_CLASS_NUMBER) == InputType.TYPE_CLASS_NUMBER;
        boolean allowDecimal = (input.getInputType() & InputType.TYPE_NUMBER_FLAG_DECIMAL) != 0;

        if (isNumeric) {
            InputFilter symbolBlocker = new InputFilter() {
                @Override
                public CharSequence filter(CharSequence source, int start, int end,
                                           Spanned dest, int dstart, int dend) {
                    if (start == end) return null;

                    StringBuilder sb = new StringBuilder(dest);
                    sb.replace(dstart, dend, source.subSequence(start, end).toString());
                    String newText = sb.toString();

                    if (newText.isEmpty()) return null;

//                    if (!allowDecimal) {
//                        if (!newText.matches("\\d*")) {
//                            return "";
//                        }
//                        if (newText.length() > 1 && newText.startsWith("0")) {
//                            return "";
//                        }
//                        return null;
//                    }
                    // Decimal: digits, optional single dot, optional digits; no lone dots
                    // Valid examples: "1", "0", "12.", "12.3", "0.45"
                    // Invalid: ".", "..", "1..2", "1.2.3", "abc"
                    if (!newText.matches("\\d+(?:\\.\\d*)?")) {
                        return "";
                    }
                    //Reject "09", "0123", etc. but allow "0.xxx"
//                    if (newText.length() > 1 && newText.startsWith("0") && !newText.startsWith("0.")) {
//                        return "";
//                    }
                    return null;                }
            };
            InputFilter[] merged = new InputFilter[existing.length + 1];
            System.arraycopy(existing, 0, merged, 0, existing.length);
            merged[existing.length] = symbolBlocker;
            input.setFilters(merged);
        }
        required = isRequired;

        CharSequence valx = input.getText();
        if (valx == null && required) {
            //  setSoftRequired(true);

            //   input.setError("!");
            return;
        }

        input.addTextChangedListener(new TextWatcher() {
String beforeData = "";
String onChangeData = "";
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                beforeData = charSequence+"";
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                onChangeData = charSequence+"";
            }

//            @Override
//            public void afterTextChanged(Editable editablex) {
//                System.out.println("===================================================== " + editablex.toString());
//                System.out.println("===================================================== " + input.getText());
//
//
//
//
//                String text = editablex.toString();
//
////                handleZeroReplacement(text);
//
//                if (inverseBindingListener != null) {
//                    inverseBindingListener.onChange();
//                }
//
//                onValueChanged();
//
//
//                if (isRange && minValue != null && maxValue != null) {
//                    //        System.out.println(minValue + "--------%%%11%%%------------" + maxValue);
//                    if (minValue != null && maxValue != null && input.getText() != null) {
//                        //      System.out.println(minValue + "--------%%%22%%%------------" + maxValue);
//                        if (!input.getText().toString().equals("") || !input.getText().toString().isEmpty()) {
//                            //         System.out.println(minValue + "--------%%%333%%%------------" + maxValue);
//                            int valxx = Integer.parseInt(input.getText().toString());
//                            if (valxx >= minValue && valxx <= maxValue) {
//                                //           System.out.println(minValue + "--------%%%444%%%------------" + maxValue);
//                            } else if (warnOnError) {
//                                //          System.out.println(minValue + "--------%%555%%%%------------" + maxValue);
//                                NotificationHelper.showNotification((NotificationContext) input.getContext(), WARNING, "Number not in provided range! i.e min: " + minValue + " and max: " + maxValue);
//                            } else {
//                                //       System.out.println(minValue + "Number not in provided range!" + maxValue);
//                                input.setError("Number not in provided range! i.e min: " + minValue + " and max: " + maxValue);
//                                setErrorIfEmptyRange();
//                                enableErrorState("Number not in provided range! i.e min: " + minValue + " and max: " + maxValue);
//
//                            }
//                        }
//
//                    }
//
//                }
//
//
//                if (isRange && isExpression && isRequired) {
//                    System.out.println("is range111111111111111111111111111111111-==================");
//                    System.out.println(beforeData + "beforeDatarange111111111111111111111111111111111" + onChangeData);                    System.out.println(beforeData + "beforeDatarange111111111111111111111111111111111" + onChangeData);
//                    System.out.println(beforeData.length() + "lengthbeforeDatarange111111111111111111111111111111111" + onChangeData.length());
////                    System.out.println(beforeDatax.length() + "beforeXDatarange111111111111111111111111111111111" + onChangeData.length());
//
//                    try {
//                        if (beforeData.length() > 0 || onChangeData.length() > 0) {
//                            if (beforeData.toString() != "" && onChangeData.toString() != "") {
//                                int beforeDatavalxx = Integer.parseInt(beforeData.toString());
//                                int onChangeDatavalxx = Integer.parseInt(onChangeData.toString());
//
//                                if (beforeData.length() > 0 && onChangeData.length() == 0) {
//                                    input.setError("Number not in provided range!");
//                                    enableErrorState("Number not in provided range!");
//                                }
//                            }
//                        }
//                    } catch (NumberFormatException ec) {
//                        input.setError("Please Enter a valid Number");
//                        enableErrorState("Please Enter a valid Number");
//                    }
//
//                    System.out.println("111111111111111111111111111111111-==================cccccc");
//                } else if (isRange && isExpression && !isRequired) {
//
//                    System.out.println("is rangejnotrequired111111111111111111111111111111111-==================");
//                    System.out.println(beforeData + "beforeDatarange111111111111111111111111111111111" + onChangeData);
//
//                    try {
//                        if (beforeData.length() > 0 || onChangeData.length() > 0) {
//                            if (beforeData.toString() != "" && onChangeData.toString() != "") {
//
//                                int beforeDatavalxx = Integer.parseInt(beforeData.toString());
//                                int onChangeDatavalxx = Integer.parseInt(onChangeData.toString());
//                                System.out.println(beforeDatavalxx + "elsevalxx111111111111111111111111111111111-==================" + onChangeDatavalxx);
//                            }
//                        }
//                    } catch (NumberFormatException e) {
//                        input.setError("Please enter a valid number.");
//                        enableErrorState("else1111111111111111111111-=====Number not in provided range! i.e min:------------------");
//                    }
//
//                }
//
//                }


            @Override
            public void afterTextChanged(Editable editablex) {
                System.out.println("===================================================== " + editablex.toString());
                System.out.println("===================================================== " + input.getText());

                String text = editablex.toString();

                if (inverseBindingListener != null) {
                    inverseBindingListener.onChange();
                }

                onValueChanged();

                if (isRange && minValue != null && maxValue != null) {
                    if (minValue != null && maxValue != null && input.getText() != null) {
                        if (beforeData.length() == 0 && onChangeData.length() == 0) {
                            System.out.println("lenght before and after is 0 ===== no user input ==");
                            disableErrorState();
                        }

                        if (!input.getText().toString().equals("") || !input.getText().toString().isEmpty()) {
                            try{
                                int valxx = Integer.parseInt(input.getText().toString());
                                if (valxx >= minValue && valxx <= maxValue) {
                                    // Valid range
                                } else if (warnOnError) {
                                    NotificationHelper.showNotification((NotificationContext) input.getContext(), WARNING, "Number not in provided range! i.e min: " + minValue + " and max: " + maxValue);
                                } else {
                                    input.setError("Number not in provided range! i.e min: " + minValue + " and max: " + maxValue);
                                    setErrorIfEmptyRange();
                                    enableErrorState("Number not in provided range! i.e min: " + minValue + " and max: " + maxValue);
                                }

                            }catch(NumberFormatException e ){
                                if(warnOnError){
                                    NotificationHelper.showNotification((NotificationContext) input.getContext(), WARNING, "Number not in provided range! i.e min: " + minValue + " and max: " + maxValue);
                                }else{
                                    input.setError("Number not in provided range! i.e min: " + minValue + " and max: " + maxValue);
                                    setErrorIfEmptyRange();
                                    enableErrorState("Number not in provided range! i.e min: " + minValue + " and max: " + maxValue);
                                }
                            }

                        }
                    }
                } else if (isRange && isExpression && isRequired){
 
                    try {
                        if(beforeData.length() > 0 || onChangeData.length() > 0 ) {
                            int beforeDatavalxx = Integer.parseInt(beforeData.toString());
                            int onChangeDatavalxx = Integer.parseInt(onChangeData.toString());
                            System.out.println(beforeDatavalxx + "valxx111111111111111111111111111111111-==================" + onChangeDatavalxx);
                            if (beforeData.length() > 0 && onChangeData.length() == 0) {
                                enableErrorState("Number not in provided range!");
                            }
                        }
                    }catch (NumberFormatException e){
                        if(beforeData.length() > 0 && onChangeData.length() == 0){
                            input.setError("Please enter a valid number");
                            enableErrorState("Invalid number");
                        }else if (beforeData.length() > 0 &&  onChangeData.length() > 0) {
                            try {
                                Integer.parseInt(text);
                                input.setError(null);
                                disableErrorState();
                            } catch (NumberFormatException eX) {
                                input.setError("Please enter a valid number");
                                enableErrorState("Invalid number");
                            }
                        }
 
            }
                     System.out.println("111111111111111111111111111111111-==================cccccc");
                }else if(isRange && isExpression && !isRequired){
                    System.out.println("elselrange but not expressiom alxx111111111111111111111111111111111-==================" );
                    try {
                        if(beforeData.length() > 0 || onChangeData.length() > 0 ) {
                            int beforeDatavalxx = Integer.parseInt(beforeData.toString());
                            int onChangeDatavalxx = Integer.parseInt(onChangeData.toString());
                            System.out.println(beforeDatavalxx +  "elsevalxx111111111111111111111111111111111-==================" + onChangeDatavalxx);
                        }
                    }catch (NumberFormatException e){
                        if (!text.isEmpty()) {
                            try {
                                Integer.parseInt(text);
                                // ✅ If parsing works, clear error
                                input.setError(null);
                                disableErrorState();
                            } catch (NumberFormatException ecc) {
                                input.setError("Please enter a valid number");
                                enableErrorState("Invalid number");
                            }
                        }
 
                   }
                }
            }
        });

       addValueChangedListener(new ValueChangeListener() {


          @Override
          public void onChange(ControlPropertyField field) {
              System.out.println(isLiveValidationDisabled() + " vaue changes isLiveValidationDisabled()----------TEXTEDIT");
                      if (!isLiveValidationDisabled()) {
                      ((ControlTextEditFieldAllowZeroInput) field).setErrorIfEmptyRange();
              }
          }
      });




        setUpOnEditorActionListener();
        setUpOnFocusChangeListener();
        initializeOnClickListener();


    }


    private void NumberNumericValueValidator(String errorMessage, String minValue, String maxValue) {
        Integer minValuex = 0;
        Integer maxValuex = 0;
        boolean decimalAllowed;
        boolean onError;


    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (getHint() == null) {
            setHint(getPrefixCaption());
        }
    }

    @Override
    protected void requestFocusForContentView(View nextView) {
        if (nextView != null) {
            ((ControlTextEditFieldAllowZeroInput) nextView).input.requestFocus();
            ((ControlTextEditFieldAllowZeroInput) nextView).setCursorToRight();
        }
    }

    @Override
    protected void changeVisualState(VisualState state) {
        if (getUserEditRight() != null && !ConfigProvider.hasUserRight(getUserEditRight())) {
            state = VisualState.DISABLED;
        }

        if (this.visualState == state) {
            return;
        }

        visualState = state;

        int labelColor = getResources().getColor(state.getLabelColor());
        Drawable drawable = getResources().getDrawable(state.getBackground(VisualStateControlType.TEXT_FIELD));
        int textColor = getResources().getColor(state.getTextColor());
        int hintColor = getResources().getColor(state.getHintColor());

        if (drawable != null) {
            drawable = drawable.mutate();
        }

        label.setTextColor(labelColor);
        setBackground(drawable);

        if (state != VisualState.ERROR) {
            input.setTextColor(textColor);
            input.setHintTextColor(hintColor);
        }

        setEnabled(state != VisualState.DISABLED);
    }

    @Override
    public void setBackgroundResource(int resId) {
        setBackgroundResourceFor(input, resId);
    }

    @Override
    public void setBackground(Drawable background) {
        setBackgroundFor(input, background);
    }

    // Data binding, getters & setters

    @BindingAdapter("value")
    public static void setValue(ControlTextEditFieldAllowZeroInput view, String text) {
        // If text is null or blank, keep it empty
        if (text == null || text.trim().isEmpty() || text == "" || text.equalsIgnoreCase("NaN")) {
            view.setFieldValue("");
            return;
        }else{
            try {
                double num = Double.parseDouble(text);
                if (num == Math.floor(num)) {
                    // Whole number, no decimals
                    text = String.valueOf((int) num);
                } else {
                    // Show with 2 decimal places
                    text = String.format("%.2f", num);
                }
            } catch (NumberFormatException e) {
                // If not a number, leave as-is
            }
            view.setFieldValue(text);
            return;

        }
    }


    @BindingAdapter("value")
    public static void setValue(ControlTextEditFieldAllowZeroInput view, String text, String errorMessage) {
        // If text is null or blank, keep it empty
        if (text == null || text.trim().isEmpty() || text == "") {
            view.setFieldValue("");
            return;
        }else{
            view.setFieldValue(text);
        }
    }

    @BindingAdapter("value")
    public static void setValue(ControlTextEditFieldAllowZeroInput view, Integer integerValue) {
        if (integerValue != null) {
            view.setFieldValue(String.valueOf(integerValue));
        } else {
            view.setFieldValue("");
        }
    }

    @BindingAdapter("value")
    public static void setValue(ControlTextEditFieldAllowZeroInput view, Float floatValue) {
        if (floatValue != null) {
            view.setFieldValue(String.valueOf(floatValue));
        } else {
            view.setFieldValue(null);
        }
    }

    @BindingAdapter("value")
    public static void setValue(ControlTextEditFieldAllowZeroInput view, Double doubleValue) {
        if (doubleValue != null) {
            view.setFieldValue(String.valueOf(doubleValue));
        } else {
            view.setFieldValue(null);
        }
    }



    public void setDoubleValue(Double doubleValue) {
        setValue(this, doubleValue);
    }

    public void setFloatValue(Float floatValue) {
        setValue(this, floatValue);
    }

    @InverseBindingAdapter(attribute = "value", event = "valueAttrChanged")
    public static String getValue(ControlTextEditFieldAllowZeroInput view) {
        return view.getFieldValue();
    }

    @InverseBindingAdapter(attribute = "value", event = "valueAttrChanged")
    public static Integer getIntegerValue(ControlTextEditFieldAllowZeroInput view) {
        if (view.getFieldValue() != null && !view.getFieldValue().isEmpty()) {
            return Integer.valueOf(view.getFieldValue());
        } else {
            return null;
        }
    }

    @InverseBindingAdapter(attribute = "value", event = "valueAttrChanged")
    public static Float getFloatValue(ControlTextEditFieldAllowZeroInput view) {
        if (view.getFieldValue() != null && !view.getFieldValue().isEmpty()) {
            return Float.valueOf(view.getFieldValue());
        } else {
            return null;
        }
    }

    @InverseBindingAdapter(attribute = "value", event = "valueAttrChanged")
    public static Double getDoubleValue(ControlTextEditFieldAllowZeroInput view) {
        if (view.getFieldValue() != null && !view.getFieldValue().isEmpty()) {
            return Double.valueOf(view.getFieldValue());
        } else {
            return null;
        }
    }

    @BindingAdapter("valueAttrChanged")
    public static void setListener(ControlTextEditFieldAllowZeroInput view, InverseBindingListener listener) {
        view.inverseBindingListener = listener;
    }

    public String getHint() {
        if (input.getHint() == null) {
            return null;
        }

        return input.getHint().toString();
    }

    public boolean isSingleLine() {
        return singleLine;
    }

    public void setSingleLine(boolean singleLine) {
        this.singleLine = singleLine;

        if (this.singleLine) {
            input.setMaxLines(1);
            input.setVerticalScrollBarEnabled(false);
        } else {
            input.setMaxLines(getMaxLines());
            input.setVerticalScrollBarEnabled(true);
            if (textArea) {
                input.setLines(getMaxLines());
            }
        }
    }

    public int getInputType() {
        return inputType;
    }

    public void setInputType(int inputType) {
        this.inputType = inputType;
        if (input != null)
            input.setInputType(inputType);
    }

    public int getMaxLines() {
        return maxLines;
    }

    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(int minLength) {
        this.minLength = minLength;
    }

    public EditText getInput() {
        return input;
    }

    public void setInput(EditText input) {
        this.input = input;
    }
}
