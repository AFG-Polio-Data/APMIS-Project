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

package de.symeda.sormas.app.campaign.list;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.app.BaseActivity;
import de.symeda.sormas.app.BaseListActivity;
import de.symeda.sormas.app.PagedBaseListActivity;
import de.symeda.sormas.app.PagedBaseListFragment;
import de.symeda.sormas.app.R;
import de.symeda.sormas.app.backend.campaign.Campaign;
import de.symeda.sormas.app.backend.campaign.CampaignDao;
import de.symeda.sormas.app.backend.campaign.data.CampaignFormData;
import de.symeda.sormas.app.backend.campaign.data.CampaignFormDataCriteria;
import de.symeda.sormas.app.backend.campaign.form.CampaignFormMeta;
import de.symeda.sormas.app.backend.common.DatabaseHelper;
import de.symeda.sormas.app.backend.config.ConfigProvider;
import de.symeda.sormas.app.campaign.edit.CampaignFormDataNewActivity;
import de.symeda.sormas.app.campaign.edit.CampaignFormMetaDialog;
import de.symeda.sormas.app.component.Item;
import de.symeda.sormas.app.component.menu.PageMenuItem;
import de.symeda.sormas.app.databinding.FilterCampaignFormDataListLayoutBinding;
import de.symeda.sormas.app.util.Callback;

public class CampaignFormDataListActivity extends PagedBaseListActivity<CampaignFormData> {

    private CampaignFormDataListViewModel model;
    private CampaignDao campaignDao;
    private FilterCampaignFormDataListLayoutBinding filterBinding;
    //this resets active campaign to servers active campaign...
    public static void startActivity(Context context) {
        List<Campaign> activeCampaigns = DatabaseHelper.getCampaignDao().getAllActive();
        int pageMenuPosition = activeCampaigns.size() > 0 ? 1 : 0;
        BaseListActivity.startActivity(context, CampaignFormDataListActivity.class, buildBundle(pageMenuPosition));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showPreloader();
        adapter = new CampaignFormDataListAdapter();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                if (positionStart == 0) {
                    RecyclerView recyclerView = findViewById(R.id.recyclerViewForList);
                    if (recyclerView != null) {
                        recyclerView.scrollToPosition(0);
                    }
                }
            }

            @Override
            public void onItemRangeMoved(int positionStart, int toPosition, int itemCount) {
                RecyclerView recyclerView = findViewById(R.id.recyclerViewForList);
                if (recyclerView != null) {
                    recyclerView.scrollToPosition(0);
                }
            }
        });

        model = ViewModelProviders.of(this).get(CampaignFormDataListViewModel.class);
        model.getCriteria().setCampaign(DatabaseHelper.getCampaignDao().getLastStartedCampaign());
        model.getCampaignFormDataList().observe(this, campaignFormDataPagedList -> {
            adapter.submitList(campaignFormDataPagedList);
            setSetSubHeadingTitleForCampaign(model.getCriteria().getCampaign());
            hidePreloader();
        });

        filterBinding.setCriteria(model.getCriteria());

        setOpenPageCallback(p -> {
            showPreloader();
            model.notifyCriteriaUpdated();
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        getIntent().putExtra("refreshOnResume", true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getIntent().getBooleanExtra("refreshOnResume", false)) {
            showPreloader();
            if (model.getCampaignFormDataList().getValue() != null) {
                model.getCampaignFormDataList().getValue().getDataSource().invalidate();
            }
        }
    }

    @Override
    protected Callback getSynchronizeResultCallback() {
        // Reload the list after a synchronization has been done
        return () -> {
            showPreloader();
            model.getCampaignFormDataList().getValue().getDataSource().invalidate();
        };
    }

    @Override
    public int onNotificationCountChangingAsync(AdapterView parent, PageMenuItem menuItem, int position) {
        return 0;
    }

    @Override
    protected PagedBaseListFragment buildListFragment(PageMenuItem menuItem) {
        return CampaignFormDataListFragment.newInstance();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.dashboard_action_menu, menu);

        super.onCreateOptionsMenu(menu);
        getNewMenu().setTitle(R.string.action_new_campaign_form_data);
        return true;
    }

    @Override
    protected int getActivityTitle() {
        return R.string.heading_campaigns_list;
    }

    @Override
    public void goToNewView() {

        final CampaignFormDataCriteria criteria = model.getCriteria();
        final CampaignFormMetaDialog campaignFormMetaDialog = new CampaignFormMetaDialog(BaseActivity.getActiveActivity(), criteria.getCampaign());
        campaignFormMetaDialog.setPositiveCallback(() -> CampaignFormDataNewActivity.startActivity(getContext(), criteria.getCampaign().getUuid(), campaignFormMetaDialog.getCampaignFormMeta().getUuid()));
        campaignFormMetaDialog.show();
        campaignFormMetaDialog.setLiveValidationDisabled(true);
    }



    @Override
    public boolean isEntryCreateAllowed() {
        return model.getCriteria().getCampaign() != null && ConfigProvider.hasUserRight(UserRight.CAMPAIGN_FORM_DATA_EDIT);
    }

    @Override
    public void addFiltersToPageMenu() {
        View campaignsFormDataListFilterView = getLayoutInflater().inflate(R.layout.filter_campaign_form_data_list_layout, null);
        filterBinding = DataBindingUtil.bind(campaignsFormDataListFilterView);

        List<Item> campaigns = campaignsToItems(DatabaseHelper.getCampaignDao().getAllActive());
        filterBinding.campaignFilter.initializeSpinner(campaigns);
        filterBinding.campaignFilter.addValueChangedListener(e -> {
            Campaign campaign = (Campaign) e.getValue();
            if (campaign != null) {
                if(campaign.getCampaignFormMetas() != null){
                    List<Item> forms = campaignFormMetasToItems(campaign.getCampaignFormMetas());


                    forms.stream().filter(ee -> ee.getValue() != null)
                            .sorted(Comparator.comparing(item -> ((CampaignFormMeta)item.getValue()).getFormName()))
                            .collect(Collectors.toList());
                    filterBinding.campaignFormFilter.initializeSpinner(forms);
                    setSubHeadingTitle(campaign != null ? campaign.getName() : I18nProperties.getCaption(Captions.all));
                    if (getNewMenu() != null) {
                        getNewMenu().setVisible(isEntryCreateAllowed());
                    }
                }
            }
        });

        pageMenu.addFilter(campaignsFormDataListFilterView);




        filterBinding.applyFilters.setOnClickListener(e -> {

            showPreloader();
            pageMenu.hideAll();
           // System.out.println("-----------------------"+model.getCriteria().getCampaign().getUuid());
            if(model.getCriteria().getCampaign() != null){
                DatabaseHelper.getCampaignDao().updateCampaignLastOpenedDate(model.getCriteria().getCampaign().getUuid());
                // campaignDao.updateCampaignLastOpenedDate(model.getCriteria().getCampaign().getUuid());
                setSetSubHeadingTitleForCampaign(model.getCriteria().getCampaign());
            }else{
                Context context = getApplicationContext();
                CharSequence text = "You did not select any Campaign!";
                int duration = Toast.LENGTH_LONG;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
            model.notifyCriteriaUpdated();
        });

        filterBinding.resetFilters.setOnClickListener(e -> {
            showPreloader();
            pageMenu.hideAll();
            Campaign lastCampaign = DatabaseHelper.getCampaignDao().getLastStartedCampaign();
            model.getCriteria().setCampaign(lastCampaign);
            model.getCriteria().setCampaignFormMeta(null);
            filterBinding.invalidateAll();
            filterBinding.executePendingBindings();
            setSetSubHeadingTitleForCampaign(lastCampaign);
            if (getNewMenu() != null) {
                getNewMenu().setVisible(false);
            }
            model.notifyCriteriaUpdated();
        });
    }

    private List<Item> campaignsToItems(List<Campaign> campaigns) {
        List<Item> listOut = new ArrayList<>();
        listOut.add(new Item<Integer>("", null));
        for (Campaign campaign : campaigns) {
            listOut.add(new Item<>(campaign.getName(), campaign));
        }
        return listOut;
    }

    private List<Item> campaignFormMetasToItems(List<CampaignFormMeta> campaignFormMetas) {
        List<Item> listOut = new ArrayList<>();
        listOut.add(new Item<Integer>("", null));
        for (CampaignFormMeta campaignFormMeta : campaignFormMetas) {
            if(campaignFormMeta != null) {

                listOut.add(new Item<>(campaignFormMeta.getFormName(), campaignFormMeta));

            }
        }
//        listOut.stream().filter(ee -> ee.getValue() != null).sorted(Comparator.comparing(CampaignFormMetaReferenceDto::getCaption)).collect(Collectors.toList());
//        listOut.sort(Comparator.comparing(Item::getKey));
//        listOut.sort(Comparator.comparing(CampaignFormMetaReferenceDto::getCaption));
// Sorting the list alphabetically based on the form names
        listOut = listOut.stream()
                .filter(ee -> ee.getValue() != null)
                .sorted(Comparator.comparing(item -> ((CampaignFormMeta)item.getValue()).getFormName()))
                .collect(Collectors.toList());

        return listOut;


//        return listOut;
    }

    private void setSetSubHeadingTitleForCampaign(Campaign campaign) {
        setSubHeadingTitle(campaign != null ? campaign.getName() : I18nProperties.getCaption(Captions.all));
    }
}
