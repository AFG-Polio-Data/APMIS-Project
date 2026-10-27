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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;


import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.user.UserRole;
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
import de.symeda.sormas.app.backend.campaign.form.CampaignFormMetaRegion;
import de.symeda.sormas.app.backend.common.DatabaseHelper;
import de.symeda.sormas.app.backend.config.ConfigProvider;
import de.symeda.sormas.app.backend.region.Community;
import de.symeda.sormas.app.backend.region.District;
import de.symeda.sormas.app.backend.region.DistrictDao;
import de.symeda.sormas.app.backend.region.PopulationData;
import de.symeda.sormas.app.backend.user.User;
import de.symeda.sormas.app.campaign.edit.CampaignFormDataNewActivity;
import de.symeda.sormas.app.campaign.edit.CampaignFormMetaDialog;
import de.symeda.sormas.app.component.Item;
import de.symeda.sormas.app.component.menu.PageMenuItem;
import de.symeda.sormas.app.databinding.FilterCampaignFormDataListLayoutBinding;
import de.symeda.sormas.app.util.Callback;
import de.symeda.sormas.app.util.DataUtils;
import de.symeda.sormas.app.util.ErrorReportingHelper;
import de.symeda.sormas.app.util.InfrastructureDaoHelper;

public class CampaignFormDataListActivity extends PagedBaseListActivity<CampaignFormData> {

    private CampaignFormDataListViewModel model;
    private CampaignDao campaignDao;
    private FilterCampaignFormDataListLayoutBinding filterBinding;
    private BroadcastReceiver refreshReceiver;

    //this resets active campaign to servers active campaign...
    public static void startActivity(Context context) {
        List<Campaign> activeCampaigns = DatabaseHelper.getCampaignDao().getAllActive();
        int pageMenuPosition = activeCampaigns.size() > 0 ? 1 : 0;
        BaseListActivity.startActivity(context, CampaignFormDataListActivity.class, buildBundle(pageMenuPosition));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        System.out.println(savedInstanceState + "savedInstanceStatesavedInstanceStatesavedInstanceState");
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

        model.getRowCount().observe(this, count -> {
            setRowCount(count);
        });
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

        refreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("REFRESH_ROW_COUNT".equals(intent.getAction())) {
                    model.updateRowCount();
                }
            }
        };
        registerReceiver(refreshReceiver, new IntentFilter("REFRESH_ROW_COUNT"));
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
        List<PopulationData> list = new ArrayList<>();

        if (ConfigProvider.getUser().getUserRoles().contains(UserRole.SURVEILLANCE_OFFICER)) {
            // Get all districts the user has access to
            List<District> userDistricts = new ArrayList<>();
            List<Item> districtItemList = InfrastructureDaoHelper.loadAllDistricts();
            List<String> districtUuids = new ArrayList<>();

            // Convert district items to District objects and collect UUIDs
            districtItemList.forEach(item -> {
                List<District> districts = DatabaseHelper.getDistrictDao().getByName(item.toString());
                userDistricts.addAll(districts);
            });

            userDistricts.forEach(district -> {
                districtUuids.add(district.getUuid());
            });
            System.out.println("User role contains surv Officer --------------------"
                    + list);
            // Option 1: Pass the list of UUIDs directly to a new DAO method
            list = DatabaseHelper.getPopulationDataDao().getSelectedDistrictsByMultipleUuids(
                    districtUuids, criteria.getCampaign().getUuid());


            System.out.println("after user role contains surv Officer --------------------"
                    + list);
        } else if (ConfigProvider.getUser().getUserRoles().contains(UserRole.COMMUNITY_OFFICER)) {
            // Get all districts the user has access to
            List<Community> userClusters = new ArrayList<>();
            User user = ConfigProvider.getUser();
            List<Community> clustersItemList = DatabaseHelper.getCommunityDao()
                    .getByDistrict(user.getDistrict());
            List<String> clustersUuids = new ArrayList<>();

            // Convert district items to District objects and collect UUIDs
            clustersItemList.forEach(item -> {
                if(item != null) {

                    System.out.println("User role contains cluster flw  item--------------------"
                            + item);


                    System.out.println("User role contains cluster flw  item--------------------"
                            + item.getName());
                    List<Community> clusters = DatabaseHelper.getCommunityDao().getByClusterName(item.getName());

                    System.out.println("User role contains cluster flw  clusters--------------------"
                            + clusters);
                    userClusters.addAll(clusters);

                    clustersUuids.add(item.getUuid().toString());
                }
            });

            System.out.println("User role contains cluster flw  clustersItemList--------------------"
                    + clustersItemList);

            System.out.println("User role contains cluster flw  userClusters--------------------"
                    + userClusters);

            System.out.println("User role contains cluster flw  clustersUuids--------------------"
                    + clustersUuids);


//
//            userClusters.forEach(cluster -> {
//                clustersUuids.add(cluster.getUuid());
//            });
            System.out.println("User role contains cluster flw  --------------------"
                    + list);
            // Option 1: Pass the list of UUIDs directly to a new DAO method
            list = DatabaseHelper.getPopulationDataDao().getSelectedClustersByMultipleUuids(
                    clustersUuids, criteria.getCampaign().getUuid());

            System.out.println("after user role contains cluster flw  --------------------"
                    + list);
        }else {

            System.out.println(ConfigProvider.getUser().getDistrict().getUuid() + "User role is not  surv Officer --------------------" + criteria.getCampaign().getUuid());

                    // Original logic for other user roles
            list = DatabaseHelper.getPopulationDataDao().getSelectedDistrictByUsersDistrict(
                    ConfigProvider.getUser().getDistrict().getUuid(), criteria.getCampaign().getUuid());
        }

        if (list.size() > 0) {
            final CampaignFormMetaDialog campaignFormMetaDialog = new CampaignFormMetaDialog(BaseActivity.getActiveActivity(), criteria.getCampaign());
            campaignFormMetaDialog.setPositiveCallback(() -> {
                //This try and catch block is supposed to logg possible error to the db
                try {
                    CampaignFormDataNewActivity.startActivity(getContext(), criteria.getCampaign().getUuid(), campaignFormMetaDialog.getCampaignFormMeta().getUuid());
                }catch(Exception e){

                    System.out.println("LIST ACTIVITY  Fragment Error Logged--------------------");

                    ErrorReportingHelper.logAndStoreDeviceError("New Form" , e);
                }
            });
            campaignFormMetaDialog.show();
            campaignFormMetaDialog.setLiveValidationDisabled(true);
        } else {
            showCustomDialog(this,
                    "Data Entry Error",
                    "Users distcrict is not selected for data entry in this campaign.");
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
            Campaign campaign = new Campaign();
            campaign = (Campaign) e.getValue();
            if (campaign != null) {
                if (campaign.getCampaignFormMetas() != null) {
                    List<Item> forms = new ArrayList<Item>();

                    List<CampaignFormMeta> allUnexpiredFormsForCampaign = new ArrayList<>();

                    for (CampaignFormMeta campaignFormMeta : campaign.getCampaignFormMetas()) {
                        Date expiryDate = DatabaseHelper.getCampaignFormMetaWithExpDao().getCampaignFormExpiryDateByCampaignIdAndFormId(campaign.getUuid(), campaignFormMeta.getUuid());
                        LocalDate currentDate = LocalDate.now();
                        if (expiryDate != null) {
                            LocalDate expiryLocalDate = expiryDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                            if (currentDate.isBefore(expiryLocalDate) || expiryLocalDate.isEqual(currentDate)) {
                                User user = ConfigProvider.getUser();
                                List<CampaignFormMetaRegion> formsSelectedForCampaign = DatabaseHelper.getCampaignFormMetaRegionDao().getSelectedFormsByRegion(campaignFormMeta.getUuid(), user.getRegion().getArea().getUuid());
                                if (formsSelectedForCampaign.size() > 0) {
                                    allUnexpiredFormsForCampaign.add(campaignFormMeta);
                                }
                            }
                        } else {
                            System.out.println("This form does not have an expiry date set  " + campaignFormMeta.getFormName());
                        }
                    }
                    Collections.sort(allUnexpiredFormsForCampaign, Comparator.comparing(CampaignFormMeta::getFormName));
                    forms = campaignFormMetasToItems(allUnexpiredFormsForCampaign);

                    System.out.println("--------cccccbb---------------" + forms);
                    forms.stream().filter(ee -> ee.getValue() != null)
                            .sorted(Comparator.comparing(item -> ((CampaignFormMeta) item.getValue()).getFormName()))
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
            if (model.getCriteria().getCampaign() != null) {
                DatabaseHelper.getCampaignDao().updateCampaignLastOpenedDate(model.getCriteria().getCampaign().getUuid());
                // campaignDao.updateCampaignLastOpenedDate(model.getCriteria().getCampaign().getUuid());
                setSetSubHeadingTitleForCampaign(model.getCriteria().getCampaign());
//                model.getCriteria().setCampaignFormMeta(null);
            } else {
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
            if (campaignFormMeta != null) {
                listOut.add(new Item<>(campaignFormMeta.getFormName(), campaignFormMeta));
                System.out.println("-----------ddd-----------" + listOut);
            }
        }
        listOut.stream()
                .filter(ee -> ee.getValue() != null)
                .sorted(Comparator.comparing(item -> ((CampaignFormMeta) item.getValue()).getFormName()))
                .collect(Collectors.toList());
        System.out.println("-----------ddd-----------" + listOut);

        return listOut;
    }

    private void setSetSubHeadingTitleForCampaign(Campaign campaign) {
        setSubHeadingTitle(campaign != null ? campaign.getName() : I18nProperties.getCaption(Captions.all));
    }
}
