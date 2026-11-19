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

package de.symeda.sormas.app.campaign.read;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

import de.symeda.sormas.app.BaseActivity;
import de.symeda.sormas.app.BaseReadActivity;
import de.symeda.sormas.app.BaseReadFragment;
import de.symeda.sormas.app.R;
import de.symeda.sormas.app.backend.campaign.data.CampaignFormData;
import de.symeda.sormas.app.backend.campaign.form.CampaignFormMeta;
import de.symeda.sormas.app.backend.common.DaoException;
import de.symeda.sormas.app.backend.common.DatabaseHelper;
import de.symeda.sormas.app.campaign.edit.CampaignFormDataEditActivity;
import de.symeda.sormas.app.component.menu.PageMenuItem;
import de.symeda.sormas.app.core.notification.NotificationHelper;
import de.symeda.sormas.app.core.notification.NotificationType;
import de.symeda.sormas.app.util.NavigationHelper;

public class CampaignFormDataReadActivity extends BaseReadActivity<CampaignFormData> {

    private List<String> intraCampaignsCategories = List.of("ICM", "ADMIN", "EAG-ICM", "EAG-ADMIN");

    public static void startActivity(Context context, String rootUuid, boolean finishInsteadOfUpNav) {
        BaseActivity.startActivity(context, CampaignFormDataReadActivity.class, buildBundle(rootUuid, finishInsteadOfUpNav));
    }

    @Override
    protected CampaignFormData queryRootEntity(String recordUuid) {
        return DatabaseHelper.getCampaignFormDataDao().queryUuidWithEmbedded(recordUuid);
    }

    @Override
    public void goToEditView() {

        if (intraCampaignsCategories.contains(DatabaseHelper.getCampaignFormDataDao().queryUuid(getRootUuid()).getCampaignFormMeta().getFormCategory())) {
            if(DatabaseHelper.getCampaignFormDataDao().queryUuid(getRootUuid()).getIsvalidated()){
                showCustomDialog(getContext(),
                        "Data Entry Error",
                        "Validated Record. Please contact System Administrator to update record.");
            }else {
                CampaignFormDataEditActivity.startActivity(getContext(), getRootUuid());
            }
        }else{
            CampaignFormDataEditActivity.startActivity(getContext(), getRootUuid());
        }
    }

    @Override
    public void goToDeleteRecord() throws DaoException {
        List<String> uuidx = new ArrayList<>();
        uuidx.add(getRootUuid());

        if (intraCampaignsCategories.contains(DatabaseHelper.getCampaignFormDataDao().queryUuid(getRootUuid()).getCampaignFormMeta().getFormCategory())) {
            if(DatabaseHelper.getCampaignFormDataDao().queryUuid(getRootUuid()).getIsvalidated()){
                showCustomDialog(getContext(),
                        "Data Delete Error",
                        "Validated Record. Please contact System Administrator to delete this record.");
            }else {
                DatabaseHelper.getCampaignFormDataDao().delete(uuidx);
                NavigationHelper.navigateUpFrom(this);
                NotificationHelper.showNotification(this, NotificationType.INFO, "Record Deleted");
            }
        }else{
            DatabaseHelper.getCampaignFormDataDao().delete(uuidx);
            NavigationHelper.navigateUpFrom(this);
            NotificationHelper.showNotification(this, NotificationType.INFO, "Record Deleted");
        }

     //   DatabaseHelper.deleteCampaignRecord(getRootUuid());
     //   CampaignFormDataEditActivity.startActivity(getContext(), getRootUuid());
    }

    @Override
    protected BaseReadFragment buildReadFragment(PageMenuItem menuItem, CampaignFormData activityRootData) {
        return CampaignFormDataReadFragment.newInstance(activityRootData);
    }

    @Override
    public Enum getPageStatus() {
        return null;
    }

    @Override
    protected int getActivityTitle() {
        return R.string.heading_campaign_form_data_read;
    }

    private void showCustomDialog(Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(true);

        AlertDialog dialog = builder.create();
        dialog.show();
    }



}
