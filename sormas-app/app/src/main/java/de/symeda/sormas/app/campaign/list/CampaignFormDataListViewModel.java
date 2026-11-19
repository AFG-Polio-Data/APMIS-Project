package de.symeda.sormas.app.campaign.list;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.DataSource;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.paging.PositionalDataSource;

import java.util.List;

import de.symeda.sormas.app.backend.campaign.data.CampaignFormDataCriteria;
import de.symeda.sormas.app.backend.campaign.data.CampaignFormData;
import de.symeda.sormas.app.campaign.list.CampaignFormDataListViewModel.CampaignFormDataDataSource;
import de.symeda.sormas.app.backend.common.DatabaseHelper;

public class CampaignFormDataListViewModel extends ViewModel {

    private LiveData<PagedList<CampaignFormData>> campaignFormDataList;
    private CampaignFormDataFactory campaignFormDataFactory;
    private MutableLiveData<Integer> rowCount = new MutableLiveData<>();




    public CampaignFormDataListViewModel() {
        campaignFormDataFactory = new CampaignFormDataFactory();
        CampaignFormDataCriteria campaignFormDataCriteria = new CampaignFormDataCriteria();
        campaignFormDataFactory.setCampaignFormDataCriteria(campaignFormDataCriteria);
        PagedList.Config config = new PagedList.Config.Builder().setEnablePlaceholders(false).setInitialLoadSizeHint(32).setPageSize(16).build();

        LivePagedListBuilder<CampaignFormData, CampaignFormData> campaignsListBuilder =
                new LivePagedListBuilder<>(campaignFormDataFactory, config);

//        LivePagedListBuilder campaignsListBuilder = new LivePagedListBuilder(campaignFormDataFactory, config);
        campaignFormDataList = campaignsListBuilder.build();

        // Observe the data changes to update row count
        campaignFormDataList.observeForever(pagedList -> {
            if (pagedList != null) {
                updateRowCount();
            }
        });
    }

    public LiveData<Integer> getRowCount() {
        return rowCount;
    }

    public void updateRowCount() {
        new Thread(() -> {
            long count = DatabaseHelper.getCampaignFormDataDao().countByCriteria(
                    campaignFormDataFactory.getCampaignFormDataCriteria()
                    );
            rowCount.postValue(Integer.parseInt(count+""));
        }).start();
    }


    public LiveData<PagedList<CampaignFormData>> getCampaignFormDataList() {
        return campaignFormDataList;
    }

    void notifyCriteriaUpdated() {
        if (campaignFormDataList.getValue() != null) {
            campaignFormDataList.getValue().getDataSource().invalidate();
            if (!campaignFormDataList.getValue().isEmpty()) {
                campaignFormDataList.getValue().loadAround(0);
            }
        }
    }

    public CampaignFormDataCriteria getCriteria() {
        return campaignFormDataFactory.getCampaignFormDataCriteria();
    }

    public static class CampaignFormDataDataSource extends PositionalDataSource<CampaignFormData> {

        private CampaignFormDataCriteria campaignFormDataCriteria;

        CampaignFormDataDataSource(CampaignFormDataCriteria campaignFormDataCriteria) {
            this.campaignFormDataCriteria = campaignFormDataCriteria;
        }


        @Override
        public void loadInitial(@NonNull LoadInitialParams params,
                                @NonNull LoadInitialCallback<CampaignFormData> callback) {

            long totalCount = DatabaseHelper.getCampaignFormDataDao()
                    .countByCriteria(campaignFormDataCriteria);

            int loadSize = params.requestedLoadSize;  // can be any number now
            int startPosition = 0; // Always start from the beginning for initial load

            List<CampaignFormData> formDataList =
                    DatabaseHelper.getCampaignFormDataDao()
                            .queryByCriteria(campaignFormDataCriteria, startPosition, loadSize);

            // With placeholders disabled, use this form of onResult:
            callback.onResult(formDataList, startPosition);
        }


        @Override
        public void loadRange(@NonNull LoadRangeParams params, @NonNull LoadRangeCallback<CampaignFormData> callback) {
            List<CampaignFormData> formDataList = DatabaseHelper.getCampaignFormDataDao().queryByCriteria(campaignFormDataCriteria, params.startPosition, params.loadSize);
            callback.onResult(formDataList);
        }
    }




        public static class CampaignFormDataFactory extends DataSource.Factory {

        private MutableLiveData<CampaignFormDataDataSource> mutableDataSource;
        private CampaignFormDataDataSource campaignFormDataDataSource;
        private CampaignFormDataCriteria campaignFormDataCriteria;

        CampaignFormDataFactory() {
            this.mutableDataSource = new MutableLiveData<>();
        }


            @NonNull
            @Override
            public DataSource<Integer, CampaignFormData> create() {
                            mutableDataSource.postValue(campaignFormDataDataSource);

                return new CampaignFormDataDataSource(campaignFormDataCriteria);
            }

        public CampaignFormDataCriteria getCampaignFormDataCriteria() {
            return campaignFormDataCriteria;
        }

        public void setCampaignFormDataCriteria(CampaignFormDataCriteria campaignFormDataCriteria) {
            this.campaignFormDataCriteria = campaignFormDataCriteria;
        }


    }
}
