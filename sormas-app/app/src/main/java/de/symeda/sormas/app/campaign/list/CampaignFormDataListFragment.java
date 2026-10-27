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

package de.symeda.sormas.app.campaign.list;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.symeda.sormas.app.PagedBaseListFragment;
import de.symeda.sormas.app.R;
import de.symeda.sormas.app.backend.campaign.data.CampaignFormData;
import de.symeda.sormas.app.campaign.read.CampaignFormDataReadActivity;
import de.symeda.sormas.app.core.adapter.databinding.OnListItemClickListener;
import de.symeda.sormas.app.core.notification.NotificationHelper;
import de.symeda.sormas.app.core.notification.NotificationType;

public class CampaignFormDataListFragment extends PagedBaseListFragment<CampaignFormDataListAdapter> implements OnListItemClickListener {

    private LinearLayoutManager linearLayoutManager;
    private RecyclerView recyclerViewForList;

    private List<String> intraCampaignsCategories = List.of("ICM", "ADMIN", "EAG-ICM", "EAG-ADMIN");


    public static CampaignFormDataListFragment newInstance() {
        return newInstance(CampaignFormDataListFragment.class, null, null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        linearLayoutManager = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false);
        recyclerViewForList = view.findViewById(R.id.recyclerViewForList);
        //view.getId()
       // recyclerViewForList.setBackgroundColor(Color.parseColor("#00FF00"));

        return view;
    }

    @Override
    public CampaignFormDataListAdapter getNewListAdapter() {
        return (CampaignFormDataListAdapter) ((CampaignFormDataListActivity) getActivity()).getAdapter();
    }

    @Override
    public void onListItemClick(View view, int position, Object item) {
        CampaignFormData campaignFormData = (CampaignFormData) item;

        if (intraCampaignsCategories.contains(((CampaignFormData) item).getCampaignFormMeta().getFormCategory())) {
            if(((CampaignFormData) item).getIsvalidated()){
                showCustomDialog(getContext(),
                        "Data Entry Error",
                        "Validated Record. Please contact System Administrator to update record.");
            }else {
                CampaignFormDataReadActivity.startActivity(getContext(), campaignFormData.getUuid(), false);
            }
        }else{
            CampaignFormDataReadActivity.startActivity(getContext(), campaignFormData.getUuid(), false);
        }
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

    @Override
    protected boolean canAddToList() {
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        getSubHeadingHandler().updateSubHeadingTitle();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        recyclerViewForList.setAdapter(getListAdapter());
        recyclerViewForList.setLayoutManager(linearLayoutManager);
    }
}
