package de.symeda.sormas.ui.utils;

import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.vaadin.ui.CheckBoxGroup;
import com.vaadin.ui.Flash;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.v7.data.Item;
import com.vaadin.v7.data.fieldgroup.DefaultFieldGroupFieldFactory;
import com.vaadin.v7.shared.ui.combobox.FilteringMode;
import com.vaadin.v7.ui.AbstractSelect;
import com.vaadin.v7.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.v7.ui.AbstractTextField;
import com.vaadin.v7.ui.CheckBox;
import com.vaadin.v7.ui.ComboBox;
import com.vaadin.v7.ui.DateField;
import com.vaadin.v7.ui.Field;
import com.vaadin.v7.ui.OptionGroup;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.ReferenceDto;
import de.symeda.sormas.api.campaign.form.CampaignFormElementEnumOptions;
import de.symeda.sormas.api.campaign.form.CampaignFormElementOptions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.symptoms.SymptomState;
import de.symeda.sormas.api.utils.DateFormatHelper;
import de.symeda.sormas.api.utils.FieldConstraints;
import de.symeda.sormas.api.utils.YesNoUnknown;
import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;
import de.symeda.sormas.api.utils.fieldvisibility.FieldVisibilityCheckers;
import de.symeda.sormas.ui.ActivityAsCase.ActivityAsCaseField;
import de.symeda.sormas.ui.campaign.jsonHelpers.BasicRadioGroupHelper;
import de.symeda.sormas.ui.campaign.jsonHelpers.BasicCheckboxHelper;
import de.symeda.sormas.ui.campaign.jsonHelpers.CheckboxBasicGroup;
import de.symeda.sormas.ui.campaign.jsonHelpers.RadioBasicGroup;
import de.symeda.sormas.ui.clinicalcourse.HealthConditionsForm;
import de.symeda.sormas.ui.exposure.ExposuresField;
import de.symeda.sormas.ui.hospitalization.PreviousHospitalizationsField;
import de.symeda.sormas.ui.location.LocationEditForm;
import de.symeda.sormas.ui.person.LocationsField;
import de.symeda.sormas.ui.vaccination.VaccinationsField;

public class SormasFieldGroupFieldFactory extends DefaultFieldGroupFieldFactory {

	private static final long serialVersionUID = 471700572643936674L;

	public static final int TEXT_AREA_MAX_LENGTH = FieldConstraints.CHARACTER_LIMIT_BIG;
	public static final int TEXT_FIELD_MAX_LENGTH = FieldConstraints.CHARACTER_LIMIT_DEFAULT;

	private final FieldVisibilityCheckers fieldVisibilityCheckers;
	private final UiFieldAccessCheckers fieldAccessCheckers;

	public SormasFieldGroupFieldFactory(FieldVisibilityCheckers fieldVisibilityCheckers,
			UiFieldAccessCheckers fieldAccessCheckers) {
		this.fieldVisibilityCheckers = fieldVisibilityCheckers;
		this.fieldAccessCheckers = fieldAccessCheckers;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <T extends Field> T createField(Class<?> type, Class<T> fieldType) {
		if (type.isEnum()) {
			if (fieldType.isAssignableFrom(Field.class) // no specific fieldType defined?
					&& (SymptomState.class.isAssignableFrom(type) || YesNoUnknown.class.isAssignableFrom(type))) {
				NullableOptionGroup field = new NullableOptionGroup();
				field.setImmediate(true);
				populateWithEnumData(field, (Class<? extends Enum>) type);
				CssStyles.style(field, ValoTheme.OPTIONGROUP_HORIZONTAL, CssStyles.OPTIONGROUP_CAPTION_INLINE);
				return (T) field;
				// remember that the class used here are just dummies-hack
			} else if ((RadioBasicGroup.class.isAssignableFrom(fieldType)
					&& CampaignFormElementEnumOptions.class.isAssignableFrom(type))
					|| (CheckboxBasicGroup.class.isAssignableFrom(fieldType)
							&& CampaignFormElementEnumOptions.class.isAssignableFrom(type))
					|| (BasicRadioGroupHelper.class.isAssignableFrom(fieldType)
							&& CampaignFormElementEnumOptions.class.isAssignableFrom(type))
					|| (BasicCheckboxHelper.class.isAssignableFrom(fieldType)
							&& CampaignFormElementEnumOptions.class.isAssignableFrom(type))) {
				// Flash class is only use as a placeholder
				Boolean swt = false;
				if (CheckboxBasicGroup.class.isAssignableFrom(fieldType)
						|| BasicCheckboxHelper.class.isAssignableFrom(fieldType)) {
					swt = true;
				}
				// fieldType = (Class<T>) RadioButtonGroup.class;

				OptionGroup field = new OptionGroup();

				CampaignFormElementOptions campaignFormElementOptions = new CampaignFormElementOptions();
				Map data = campaignFormElementOptions.getOptionsListValues();

				field.addItems(data);
				field.setItemCaptionMode(ItemCaptionMode.ITEM);
				field.setNullSelectionAllowed(false);

				if (swt) {
					field.setMultiSelect(true);
					CssStyles.style(field, "width: 50%");
				}

				CssStyles.style(field, CssStyles.OPTIONGROUP_CAPTION_INLINE, CssStyles.FLOAT_RIGHT);

				return (T) field;
			} else if (DateField.class.isAssignableFrom(fieldType)
					&& CampaignFormElementEnumOptions.class.isAssignableFrom(type)) {
				
				System.out.println("sssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss");
				
				DateField field = new DateField();
				field.setDateFormat(DateFormatHelper.getDateFormatPattern());
				field.setLenient(true);
				field.setImmediate(true);
				field.setConverter(new SormasDefaultConverterFactory().createDateConverter(Date.class));
				return (T) field;
				
				
			} else {
				if (Disease.class.isAssignableFrom(type)) {
					fieldType = (Class<T>) ComboBox.class;
					ComboBox field = ComboBoxHelper.createComboBoxV7();
					field.setImmediate(true);
					field.setNullSelectionAllowed(true);
					populateWithDiseaseData(field);
					return (T) field;
				} else {

					if (!AbstractSelect.class.isAssignableFrom(fieldType)) {
						fieldType = (Class<T>) ComboBox.class;
					}
					T field = super.createField(type, fieldType);
					if (field instanceof OptionGroup) {
						CssStyles.style(field, ValoTheme.OPTIONGROUP_HORIZONTAL);
					} else if (fieldType.isAssignableFrom(NullableOptionGroup.class)) {
						NullableOptionGroup select = new NullableOptionGroup();
						select.setImmediate(true);
						populateWithEnumData(select, (Class<? extends Enum>) type);
						CssStyles.style(select, ValoTheme.OPTIONGROUP_HORIZONTAL);
						field = (T) select;
					} else if (field instanceof ComboBox) {
						((ComboBox) field).setFilteringMode(FilteringMode.CONTAINS);
						((ComboBox) field).setNullSelectionAllowed(true);
					}
					return field;
				}

			}
		} else if (CampaignFormElementOptions.class.isAssignableFrom(type)
				&& ComboBox.class.isAssignableFrom(fieldType)) {

			T field = super.createField(type, fieldType);

			ComboBox select = new ComboBox();

			CampaignFormElementOptions campaignFormElementOptions = new CampaignFormElementOptions();
			HashMap<String, String> data = (HashMap<String, String>) campaignFormElementOptions.getOptionsListValues();
			//select.addItems(data);
			 for(Entry<String, String> e : data.entrySet()){ 
			select.addItem(e.getKey());
			select.setItemCaption(e.getKey(), e.getValue()); 
			 }
			
			//((AbstractSelect) field).setItemCaptionMode(ItemCaptionMode.ITEM);
			CssStyles.style(select, ValoTheme.OPTIONGROUP_HORIZONTAL);
			field = (T) select;

			((ComboBox) field).setFilteringMode(FilteringMode.CONTAINS);
			((ComboBox) field).setNullSelectionAllowed(true);

			return field;

		} else if (Boolean.class.isAssignableFrom(type)) {
			fieldType = CheckBox.class.isAssignableFrom(fieldType) ? (Class<T>) CheckBox.class
					: (Class<T>) NullableOptionGroup.class;

			return createBooleanField(fieldType);
		} else if (ComboBox.class.isAssignableFrom(fieldType)
				|| ComboBoxWithPlaceholder.class.isAssignableFrom(fieldType)) {
			ComboBoxWithPlaceholder combo = new ComboBoxWithPlaceholder();
			combo.setImmediate(true);

			return (T) combo;
		} else if (AbstractSelect.class.isAssignableFrom(fieldType)) {
			AbstractSelect field = createCompatibleSelect((Class<? extends AbstractSelect>) fieldType);
			field.setNullSelectionAllowed(true);
			return (T) field;
		} else if (LocationEditForm.class.isAssignableFrom(fieldType)) {
			return (T) new LocationEditForm(fieldVisibilityCheckers, fieldAccessCheckers);
		} else if (HealthConditionsForm.class.isAssignableFrom(fieldType)) {
			return (T) new HealthConditionsForm(fieldVisibilityCheckers, fieldAccessCheckers);
		} else if (DateTimeField.class.isAssignableFrom(fieldType)) {
			DateTimeField field = new DateTimeField();
			field.setConverter(new SormasDefaultConverterFactory().createDateConverter(Date.class));
			return (T) field;
		} else if (DateField.class.isAssignableFrom(fieldType)) {
			DateField field = super.createField(type, DateField.class);
			field.setDateFormat(DateFormatHelper.getDateFormatPattern());
			field.setLenient(true);
			field.setConverter(new SormasDefaultConverterFactory().createDateConverter(Date.class));
			return (T) field;
		} else if (PreviousHospitalizationsField.class.isAssignableFrom(fieldType)) {
			return (T) new PreviousHospitalizationsField(fieldVisibilityCheckers, fieldAccessCheckers);
		} else if (ExposuresField.class.isAssignableFrom(fieldType)) {
			return (T) new ExposuresField(fieldVisibilityCheckers, fieldAccessCheckers);
		} else if (ActivityAsCaseField.class.isAssignableFrom(fieldType)) {
			return (T) new ActivityAsCaseField(fieldVisibilityCheckers, fieldAccessCheckers);
		} else if (LocationsField.class.isAssignableFrom(fieldType)) {
			return (T) new LocationsField(fieldVisibilityCheckers, fieldAccessCheckers);
		} else if (VaccinationsField.class.isAssignableFrom(fieldType)) {
			return (T) new VaccinationsField(fieldVisibilityCheckers, fieldAccessCheckers);
		} else if (fieldType.equals(Field.class)) {
			// no specific field type defined -> fallbacks
			if (Date.class.isAssignableFrom(type)) {
				DateField field = super.createField(type, DateField.class);
				field.setDateFormat(DateFormatHelper.getDateFormatPattern());
				field.setLenient(true);
				field.setConverter(new SormasDefaultConverterFactory().createDateConverter(Date.class));
				return (T) field;
			} else if (ReferenceDto.class.isAssignableFrom(type)) {
				return (T) ComboBoxHelper.createComboBoxV7();
			}
		}
		return super.createField(type, fieldType);
	}

	@Override
	protected AbstractSelect createCompatibleSelect(Class<? extends AbstractSelect> fieldType) {
		if (NullableOptionGroup.class.isAssignableFrom(fieldType)) {
			return new NullableOptionGroup();
		}
		return super.createCompatibleSelect(fieldType);
	}

	@Override
	protected <T extends AbstractTextField> T createAbstractTextField(Class<T> fieldType) {
		T textField = super.createAbstractTextField(fieldType);
		textField.setNullRepresentation("");
		return textField;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected <T extends Field> T createBooleanField(Class<T> fieldType) {
		if (NullableOptionGroup.class.isAssignableFrom(fieldType)) {
			final AbstractSelect s = new NullableOptionGroup();
			;
			s.addItem(Boolean.TRUE);
			s.setItemCaption(Boolean.TRUE, I18nProperties.getEnumCaption(YesNoUnknown.YES));
			s.addItem(Boolean.FALSE);
			s.setItemCaption(Boolean.FALSE, I18nProperties.getEnumCaption(YesNoUnknown.NO));

			CssStyles.style(s, ValoTheme.OPTIONGROUP_HORIZONTAL);

			return (T) s;
		} else {
			return super.createBooleanField(fieldType);
		}
	}

	@SuppressWarnings("unchecked")
	protected void populateWithDiseaseData(ComboBox diseaseField) {

		diseaseField.removeAllItems();
		for (Object p : diseaseField.getContainerPropertyIds()) {
			diseaseField.removeContainerProperty(p);
		}
		diseaseField.addContainerProperty(CAPTION_PROPERTY_ID, String.class, "");
		diseaseField.setItemCaptionPropertyId(CAPTION_PROPERTY_ID);
		List<Disease> diseases = FacadeProvider.getDiseaseConfigurationFacade().getAllDiseases(true, true, true);
		for (Object r : diseases) {
			Item newItem = diseaseField.addItem(r);
			newItem.getItemProperty(CAPTION_PROPERTY_ID).setValue(r.toString());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void populateWithEnumData(AbstractSelect select, Class<? extends Enum> enumClass) {

		select.removeAllItems();
		for (Object p : select.getContainerPropertyIds()) {
			select.removeContainerProperty(p);
		}
		select.addContainerProperty(CAPTION_PROPERTY_ID, String.class, "");
		select.setItemCaptionPropertyId(CAPTION_PROPERTY_ID);
		EnumSet<?> enumSet = EnumSet.allOf(enumClass);
		for (Object r : enumSet) {
			boolean visible = true;
			if (fieldVisibilityCheckers != null) {
				visible = fieldVisibilityCheckers.isVisible(enumClass, ((Enum<?>) r).name());
			}
			if (visible) {
				Item newItem = select.addItem(r);
				newItem.getItemProperty(CAPTION_PROPERTY_ID).setValue(r.toString());
			}
		}
	}

}
