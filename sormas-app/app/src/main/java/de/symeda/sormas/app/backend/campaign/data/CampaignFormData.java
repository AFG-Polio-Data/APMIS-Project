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

package de.symeda.sormas.app.backend.campaign.data;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

import de.symeda.sormas.api.campaign.data.CampaignFormDataEntry;
import de.symeda.sormas.api.campaign.data.PlatformEnum;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.app.backend.campaign.Campaign;
import de.symeda.sormas.app.backend.campaign.form.CampaignFormMeta;
import de.symeda.sormas.app.backend.common.PseudonymizableAdo;
import de.symeda.sormas.app.backend.region.Area;
import de.symeda.sormas.app.backend.region.Community;
import de.symeda.sormas.app.backend.region.District;
import de.symeda.sormas.app.backend.region.Region;
import de.symeda.sormas.app.backend.user.User;

@Entity(name = CampaignFormData.TABLE_NAME)
@DatabaseTable(tableName = CampaignFormData.TABLE_NAME)
public class CampaignFormData extends PseudonymizableAdo {

    public static final String TABLE_NAME = "campaignFormData";
    public static final String I18N_PREFIX = "CampaignFormData";

    public static final String CAMPAIGN = "campaign";
    public static final String FORM_DATE = "formDate";
	public static final String CAMPAIGN_FORM_META = "campaignformmeta";

    public static final String FORM_CATEGORY = "formCategory";
    public static final String SOURCE = "source";

    public static final String LOTCLUSTERNO = "lotClusterNo";

    public static final String COMMUNITY = "community";

    @Column(name = "formValues")
    private String formValuesJson;
    private List<CampaignFormDataEntry> formValues;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private Campaign campaign;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private CampaignFormMeta campaignFormMeta;

    @DatabaseField(dataType = DataType.DATE_LONG)//, canBeNull = true)
    private Date formDate;

  //  @Transient
 //   private Area area;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private Area area;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private Region region;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private District district;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private Community community;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private User creatingUser;

    @DatabaseField
    private boolean archived;

    @Column(name = "formCategory")
    private String formCategory;

    @Column(name = "source")
    private PlatformEnum soruce;

    @Column(name = "lotclusterno")
    private String lotClusterNo;

    @Column(name = "lotno")
    private String lotNo;

    /**
     * JsonRawValue annotation is used to handle this differently when merging data
     */
    @JsonRawValue
    public String getFormValuesJson() {
        return formValuesJson;
    }

    public void setFormValuesJson(String formValuesJson) {
        this.formValuesJson = formValuesJson;
        this.formValues = null;
    }

    @Transient
    public List<CampaignFormDataEntry> getFormValues() {
        if (formValues == null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<CampaignFormDataEntry>>() {
            }.getType();
            formValues = gson.fromJson(formValuesJson, type);
            if (formValues == null) {
                formValues = new ArrayList<>();
            }
        }
        return formValues;
    }

    public void setFormValues(List<CampaignFormDataEntry> formValues) {

        this.formValues = formValues;
        Gson gson = new Gson();
        formValuesJson = gson.toJson(formValues);

    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public CampaignFormMeta getCampaignFormMeta() {
        return campaignFormMeta;
    }

    public void setCampaignFormMeta(CampaignFormMeta campaignFormMeta) {
        this.campaignFormMeta = campaignFormMeta;
    }

    public Date getFormDate() {
        return formDate;
    }

    public void setFormDate(Date formDate) {
        this.formDate = formDate;
    }

    @Transient
    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {

        this.region = region;
        if (region != null) {
            setArea(region.getArea());
        }
    }

    public District getDistrict() {
        return district;
    }

    public void setDistrict(District district) {
        this.district = district;
    }

    public Community getCommunity() {
        return community;
    }

    public void setCommunity(Community community) {
        this.community = community;
    }

    public User getCreatingUser() {
        return creatingUser;
    }

    public void setCreatingUser(User creatingUser) {
        this.creatingUser = creatingUser;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public PlatformEnum getSoruce() {
        return soruce;
    }

    public void setSoruce(PlatformEnum soruce) {
        this.soruce = soruce;
    }

    public String getFormCategory() {
        if (campaignFormMeta != null) {
            return campaignFormMeta.getFormCategory();
        } else{
            return formCategory;
        }
    }

    public String getLotClusterNo() {
        return lotClusterNo;
    }

    public void setLotClusterNo(String lotClusterNo) {
        this.lotClusterNo = lotClusterNo;
    }

    public String getLotNo() {
        return lotNo;
    }

    public void setLotNo(String lotNo) {
        this.lotNo = lotNo;
    }

    //    public String getFormCategory() {
//        return campaignFormMeta.getFormCategory();
//    }

    public void setFormCategory(String formCategory) {
        if (campaignFormMeta != null) {
            formCategory = campaignFormMeta.getFormCategory();
        }
        this.formCategory = formCategory;
    }

    @Override
    public String getI18nPrefix() {
        return I18N_PREFIX;
    }

    @Override
    public String toString() {
        return DataHelper.toStringNullable(getCampaign()) + " - "
                + DataHelper.toStringNullable(getCampaignFormMeta())
                + " - " + DataHelper.toStringNullable((getCommunity()))
                + " " + DateHelper.formatShortDate(getFormDate());
    }
}
