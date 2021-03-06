package com.richfit.sdk_wzpd.blind.collect.imp;

import android.content.Context;
import android.text.TextUtils;

import com.richfit.common_lib.lib_base_sdk.base_collect.BaseCollectPresenterImp;
import com.richfit.common_lib.lib_rx.RxSubscriber;
import com.richfit.data.constant.Global;
import com.richfit.data.helper.TransformerHelper;
import com.richfit.domain.bean.MaterialEntity;
import com.richfit.domain.bean.ResultEntity;
import com.richfit.sdk_wzpd.blind.collect.IBlindCollectPresenter;
import com.richfit.sdk_wzpd.blind.collect.IBlindCollectView;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.Flowable;

/**
 * Created by monday on 2017/3/3.
 */

public class BlindCollectPresenterImp extends BaseCollectPresenterImp<IBlindCollectView>
        implements IBlindCollectPresenter {


    public BlindCollectPresenterImp(Context context) {
        super(context);
    }

    @Override
    public void getCheckTransferInfoSingle(final String checkId, String location, String queryType,
                                           String materialNum, String bizType) {
        mView = getView();

        RxSubscriber<MaterialEntity> subscriber =
                mRepository.getMaterialInfo(queryType, materialNum)
                        .flatMap(materialInfo -> {
                            if (materialInfo != null && !TextUtils.isEmpty(materialInfo.id)) {
                                return Flowable.just(materialInfo);
                            }
                            return Flowable.error(new Throwable("未获取到该物料信息"));
                        })
                        .compose(TransformerHelper.io2main())
                        .subscribeWith(new RxSubscriber<MaterialEntity>(mContext, "正在获取盘点库存信息...") {
                            @Override
                            public void _onNext(MaterialEntity data) {
                                if (mView != null) {
                                    mView.loadMaterialInfoSuccess(data);
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
                                    mView.loadMaterialInfoFail(message);
                                }
                            }

                            @Override
                            public void _onServerError(String code, String message) {
                                if (mView != null) {
                                    mView.loadMaterialInfoFail(message);
                                }
                            }

                            @Override
                            public void _onComplete() {

                            }
                        });
        addSubscriber(subscriber);
    }

    @Override
    public void uploadCheckDataSingle(ResultEntity result) {
        mView = getView();
        //如果是01仓位级别，需要检查仓位是否存在
        final String checkLevel = result.checkLevel;
        Flowable<String> flowable;
        if ("01".equals(checkLevel)) {
            Map<String,Object> extraMap = new HashMap<>();
            extraMap.put("locationType",result.locationType);
            flowable = Flowable.concat(mRepository.getLocationInfo("04", result.workId, result.invId, result.storageNum, result.location,extraMap),
                    mRepository.uploadCheckDataSingle(result));
        } else {
            flowable = mRepository.uploadCheckDataSingle(result);
        }

        RxSubscriber<String> subscriber = flowable.compose(TransformerHelper.io2main())
                .subscribeWith(new RxSubscriber<String>(mContext, "正在保存本次盘点数量...") {
                    @Override
                    public void _onNext(String s) {
                        if (mView != null) {
                            mView.saveCollectedDataSuccess(s);
                        }
                    }

                    @Override
                    public void _onNetWorkConnectError(String message) {
                        if (mView != null) {
                            mView.networkConnectError(Global.RETRY_TRANSFER_DATA_ACTION);
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
}
