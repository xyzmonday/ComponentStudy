package com.richfit.sdk_wzyk.base_ms_collect.imp;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.richfit.common_lib.lib_base_sdk.base_collect.BaseCollectPresenterImp;
import com.richfit.common_lib.lib_rx.RxSubscriber;
import com.richfit.data.constant.Global;
import com.richfit.data.helper.TransformerHelper;
import com.richfit.domain.bean.InvEntity;
import com.richfit.domain.bean.InventoryEntity;
import com.richfit.domain.bean.RefDetailEntity;
import com.richfit.domain.bean.ResultEntity;
import com.richfit.domain.bean.SimpleEntity;
import com.richfit.sdk_wzyk.base_ms_collect.IMSCollectPresenter;
import com.richfit.sdk_wzyk.base_ms_collect.IMSCollectView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Flowable;
import io.reactivex.subscribers.ResourceSubscriber;

/**
 * Created by monday on 2017/2/10.
 */

public class MSCollectPresenterImp extends BaseCollectPresenterImp<IMSCollectView>
        implements IMSCollectPresenter {

    public MSCollectPresenterImp(Context context) {
        super(context);
    }

    /**
     * 单条数据上传，增加接收仓位的检查
     *
     * @param result:用户采集的数据(json格式)
     */
    @Override
    public void uploadCollectionDataSingle(ResultEntity result) {
        mView = getView();
        Flowable<String> flowable;
        if (mRepository.isLocal()) {
            flowable = mRepository.uploadCollectionDataSingle(result);
        } else if (!TextUtils.isEmpty(result.recLocation) && !"barcode".equalsIgnoreCase(result.recLocation)) {
            //检查接收仓位
            Map<String, Object> extraMap = new HashMap<>();
            extraMap.put("locationType", result.locationType);
            flowable = Flowable.zip(mRepository.getLocationInfo(result.queryType, result.recWorkId, result.recInvId, "",
                    result.recLocation, extraMap),
                    mRepository.uploadCollectionDataSingle(result), (s, s2) -> s + ";" + s2);
        } else {
            //意味着不上架
            flowable = mRepository.uploadCollectionDataSingle(result);
        }

        ResourceSubscriber<String> subscriber =
                flowable.compose(TransformerHelper.io2main())
                        .subscribeWith(new RxSubscriber<String>(mContext) {
                            @Override
                            public void _onNext(String s) {
                                if (mView != null) {
                                    mView.saveCollectedDataSuccess(s);
                                }
                            }

                            @Override
                            public void _onNetWorkConnectError(String message) {
                                if (mView != null) {
                                    mView.networkConnectError(Global.RETRY_SAVE_COLLECTION_DATA_ACTION);
                                }
                            }

                            @Override
                            public void _onCommonError(String message) {
                                if (mView != null) {
                                    mView.saveCollectedDataFail(message);
                                }
                            }

                            @Override
                            public void _onServerError(String code, String message) {
                                if (mView != null) {
                                    mView.saveCollectedDataFail(message);
                                }
                            }

                            @Override
                            public void _onComplete() {

                            }
                        });
        addSubscriber(subscriber);
    }

    @Override
    public void getInvsByWorkId(String workId, int flag) {
        mView = getView();
        ResourceSubscriber<ArrayList<InvEntity>> subscriber =
                mRepository.getInvsByWorkId(workId, flag)
                        .compose(TransformerHelper.io2main())
                        .subscribeWith(new ResourceSubscriber<ArrayList<InvEntity>>() {
                            @Override
                            public void onNext(ArrayList<InvEntity> list) {
                                if (mView != null) {
                                    mView.showInvs(list);
                                }
                            }

                            @Override
                            public void onError(Throwable t) {
                                if (mView != null) {
                                    mView.loadInvFail(t.getMessage());
                                }
                            }

                            @Override
                            public void onComplete() {
                                if (mView != null) {
                                    mView.loadInvComplete();
                                }
                            }
                        });
        addSubscriber(subscriber);
    }

    @Override
    public void checkLocation(String queryType, String workId, String invId, String batchFlag,
                              String location, Map<String, Object> extraMap) {
        mView = getView();
        if (TextUtils.isEmpty(workId) && mView != null) {
            mView.checkLocationFail("工厂为空");
            return;
        }

        if (TextUtils.isEmpty(invId) && mView != null) {
            mView.checkLocationFail("库存地点为空");
            return;
        }

        ResourceSubscriber<String> subscriber =
                mRepository.getLocationInfo(queryType, workId, invId, "", location, extraMap)
                        .compose(TransformerHelper.io2main())
                        .subscribeWith(new ResourceSubscriber<String>() {
                            @Override
                            public void onNext(String s) {

                            }

                            @Override
                            public void onError(Throwable t) {
                                if (mView != null) {
                                    mView.checkLocationFail(t.getMessage());
                                }
                            }

                            @Override
                            public void onComplete() {
                                if (mView != null) {
                                    mView.checkLocationSuccess(batchFlag, location);
                                }
                            }
                        });
        addSubscriber(subscriber);
    }

    @Override
    public void getInventoryInfo(String queryType, String workId, String invId, String workCode, String invCode, String storageNum,
                                 String materialNum, String materialId, String location, String batchFlag,
                                 String specialInvFlag, String specialInvNum, String invType, Map<String, Object> extraMap) {
        mView = getView();
        RxSubscriber<List<InventoryEntity>> subscriber = null;
        if ("04".equals(queryType)) {
            subscriber = mRepository.getStorageNum(workId, workCode, invId, invCode)
                    .filter(num -> !TextUtils.isEmpty(num))
                    .flatMap(num -> mRepository.getInventoryInfo(queryType, workId, invId,
                            workCode, invCode, num, materialNum, materialId, "", "", batchFlag, location,
                            specialInvFlag, specialInvNum, invType, extraMap))
                    .compose(TransformerHelper.io2main())
                    .subscribeWith(new InventorySubscriber(mContext, "正在获取库存"));

        } else {
            subscriber = mRepository.getInventoryInfo(queryType, workId, invId,
                    workCode, invCode, storageNum, materialNum, materialId, "", "", batchFlag,
                    location, specialInvFlag, specialInvNum, invType, extraMap)
                    .compose(TransformerHelper.io2main())
                    .subscribeWith(new InventorySubscriber(mContext, "正在获取库存"));
        }
        addSubscriber(subscriber);
    }

    class InventorySubscriber extends RxSubscriber<List<InventoryEntity>> {

        public InventorySubscriber(Context context, String msg) {
            super(context, msg);
        }

        @Override
        public void _onNext(List<InventoryEntity> list) {
            if (mView != null) {
                mView.showInventory(list);
            }
        }

        @Override
        public void _onNetWorkConnectError(String message) {
            if (mView != null) {
                mView.networkConnectError(Global.RETRY_LOAD_INVENTORY_ACTION);
            }
        }

        @Override
        public void _onCommonError(String message) {
            if (mView != null) {
                mView.loadInventoryFail(message);
            }
        }

        @Override
        public void _onServerError(String code, String message) {
            if (mView != null) {
                mView.loadInventoryFail(message);
            }
        }

        @Override
        public void _onComplete() {
            if (mView != null) {
                mView.loadInventoryComplete();
            }
        }
    }

    @Override
    public void getTransferInfoSingle(String refCodeId, String refType, String bizType, String refLineId,
                                      String batchFlag, String location, String refDoc, int refDocItem, String userId) {
        mView = getView();
        RxSubscriber<RefDetailEntity> subscriber =
                mRepository.getTransferInfoSingle(refCodeId, refType, bizType, refLineId,
                        "", "", "", "", "", batchFlag, location, refDoc, refDocItem, userId)
                        .filter(refData -> refData != null && refData.billDetailList != null)
                        .flatMap(refData -> getMatchedLineData(refLineId, refData))
                        .compose(TransformerHelper.io2main())
                        .subscribeWith(new RxSubscriber<RefDetailEntity>(mContext) {
                            @Override
                            public void _onNext(RefDetailEntity cache) {
                                //获取缓存数据
                                if (mView != null) {
                                    mView.onBindCache(cache, batchFlag, location);
                                }
                            }

                            @Override
                            public void _onNetWorkConnectError(String message) {
                                if (mView != null) {
                                    mView.networkConnectError(Global.RETRY_LOAD_SINGLE_CACHE_ACTION);
                                }
                            }

                            @Override
                            public void _onCommonError(String message) {
                                if (mView != null) {
                                    mView.loadCacheFail(message);
                                }
                            }

                            @Override
                            public void _onServerError(String code, String message) {
                                if (mView != null) {
                                    mView.loadCacheFail(message);
                                }
                            }

                            @Override
                            public void _onComplete() {
                                if (mView != null) {
                                    mView.loadCacheSuccess();
                                }
                            }
                        });
        addSubscriber(subscriber);
    }

    @Override
    public void getSuggestInventoryInfo(String queryTyp, String workCode, String invCode, String materialNum,
                                        Map<String, Object> extraMap) {
        mView = getView();
        mRepository.getSuggestInventoryInfo(workCode, invCode, materialNum, queryTyp, extraMap)
                .compose(TransformerHelper.io2main())
                .subscribeWith(new ResourceSubscriber<List<InventoryEntity>>() {
                    @Override
                    public void onNext(List<InventoryEntity> list) {
                        if (mView != null) {
                            mView.getSuggestedLocationSuccess(list.get(0));
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        if (mView != null) {
                            mView.getSuggestedLocationFail(t.getMessage());
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (mView != null) {
                            mView.getSuggestedLocationComplete();
                        }
                    }
                });
    }

    @Override
    public void checkRecLocation(String queryTyp, String workCode, String invCode, String materialNum, Map<String, Object> extraMap) {
        mView = getView();
        mRepository.getSuggestInventoryInfo(workCode, invCode, materialNum, queryTyp, extraMap)
                .compose(TransformerHelper.io2main())
                .subscribeWith(new ResourceSubscriber<List<InventoryEntity>>() {
                    @Override
                    public void onNext(List<InventoryEntity> list) {
                        if (mView != null) {
                            mView.getActLocationSuccess(list.get(0));
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        if (mView != null) {
                            mView.getActLocationFail(t.getMessage());
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }
}

