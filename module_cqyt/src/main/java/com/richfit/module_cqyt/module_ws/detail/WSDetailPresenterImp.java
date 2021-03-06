package com.richfit.module_cqyt.module_ws.detail;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.richfit.common_lib.lib_base_sdk.base_detail.BaseDetailPresenterImp;
import com.richfit.common_lib.lib_base_sdk.edit.EditActivity;
import com.richfit.common_lib.lib_rx.RxSubscriber;
import com.richfit.common_lib.utils.SPrefUtil;
import com.richfit.data.constant.Global;
import com.richfit.data.helper.TransformerHelper;
import com.richfit.domain.bean.LocationInfoEntity;
import com.richfit.domain.bean.RefDetailEntity;
import com.richfit.domain.bean.ReferenceEntity;
import com.richfit.module_cqyt.module_ws.edit.WSEditFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.subscribers.ResourceSubscriber;

/**
 * Created by monday on 2017/9/21.
 */

public class WSDetailPresenterImp extends BaseDetailPresenterImp<IWSDetailView>
        implements IWDDetailPresenter {

    public WSDetailPresenterImp(Context context) {
        super(context);
    }

    /**
     * 注意这里无参考获取整单缓存，refData单据数据为null
     *
     * @param refData：抬头界面获取的单据数据
     * @param refCodeId：单据id
     * @param bizType:业务类型
     * @param refType：单据类型
     * @param userId
     * @param workId
     * @param invId
     * @param recInvId
     */
    @Override
    public void getTransferInfo(ReferenceEntity refData, String refCodeId, String bizType, String refType, String userId, String workId,
                                String invId, String recWorkId, String recInvId,Map<String,Object> extraMap) {
        mView = getView();
        ResourceSubscriber<List<RefDetailEntity>> subscriber =
                mRepository.getTransferInfo("", refCodeId, bizType, refType, userId, workId, invId,
                        recWorkId, recInvId,extraMap)
                        .map(data -> trans2Detail(data))
                        .compose(TransformerHelper.io2main())
                        .subscribeWith(new ResourceSubscriber<List<RefDetailEntity>>() {
                            @Override
                            public void onNext(List<RefDetailEntity> list) {
                                if (mView != null) {
                                    mView.showNodes(list);
                                }
                            }

                            @Override
                            public void onError(Throwable t) {
                                if (mView != null) {
                                    mView.setRefreshing(false, t.getMessage());
                                }
                            }

                            @Override
                            public void onComplete() {
                                if (mView != null) {
                                    mView.setRefreshing(true, "获取明细缓存成功");
                                }
                            }
                        });
        addSubscriber(subscriber);
    }

    @Override
    public void deleteNode(String lineDeleteFlag, String transId, String transLineId, String locationId,
                           String refType, String bizType, String refLineId, String userId, int position, String companyCode) {
        RxSubscriber<String> subscriber = mRepository.deleteCollectionDataSingle(lineDeleteFlag, transId, transLineId,
                locationId, refType, bizType, refLineId, userId, position, companyCode)
                .compose(TransformerHelper.io2main())
                .subscribeWith(new RxSubscriber<String>(mContext) {
                    @Override
                    public void _onNext(String s) {

                    }

                    @Override
                    public void _onNetWorkConnectError(String message) {

                    }

                    @Override
                    public void _onCommonError(String message) {
                        if (mView != null) {
                            mView.deleteNodeFail(message);
                        }
                    }

                    @Override
                    public void _onServerError(String code, String message) {
                        if (mView != null) {
                            mView.deleteNodeFail(message);
                        }
                    }

                    @Override
                    public void _onComplete() {
                        if (mView != null) {
                            mView.deleteNodeSuccess(position);
                        }
                    }
                });
        addSubscriber(subscriber);
    }

    @Override
    public void editNode(ArrayList<String> sendLocations, ArrayList<String> recLocations,
                         ReferenceEntity refData, RefDetailEntity node,
                         String companyCode, String bizType, String refType, String subFunName, int position) {
        Intent intent = new Intent(mContext, EditActivity.class);
        Bundle bundle = new Bundle();

        //物料
        bundle.putString(Global.EXTRA_MATERIAL_NUM_KEY, node.materialNum);
        bundle.putString(Global.EXTRA_MATERIAL_ID_KEY, node.materialId);
        //入库子菜单类型
        bundle.putString(Global.EXTRA_BIZ_TYPE_KEY, bizType);
        bundle.putString(Global.EXTRA_REF_TYPE_KEY, refType);
        bundle.putString(Global.EXTRA_TITLE_KEY, subFunName + "-明细修改");
        bundle.putString(Global.EXTRA_QUANTITY_KEY, node.totalQuantity);
        //批次
        bundle.putString(Global.EXTRA_BATCH_FLAG_KEY, node.batchFlag);
        //报检单
        bundle.putString(WSEditFragment.EXTRA_DECLARATION_REF_KEY, node.inspectionNum);
        //备注
        bundle.putString(Global.EXTRA_REMARK_KEY, node.remark);

        //增加transLineId作为删除条件，然后修改
        bundle.putString(WSEditFragment.EXTRA_TRANS_LINE_ID_KEY, node.transLineId);

        intent.putExtras(bundle);

        Activity activity = (Activity) mContext;
        activity.startActivity(intent);
    }

    @Override
    public void submitData2BarcodeSystem(String refCodeId, String transId, String bizType, String refType, String userId, String voucherDate,
                                         String transToSapFlag, Map<String, Object> extraHeaderMap) {
        mView = getView();
        RxSubscriber<String> subscriber =
                mRepository.uploadCollectionData(refCodeId, transId, bizType, refType, -1, voucherDate, "", "", extraHeaderMap)
                        .doOnError(e -> SPrefUtil.saveData(bizType, "0"))
                        .doOnComplete(() -> SPrefUtil.saveData(bizType, "1"))
                        .compose(TransformerHelper.io2main())
                        .subscribeWith(new RxSubscriber<String>(mContext, "正在过账数据...") {
                            @Override
                            public void _onNext(String s) {
                                if (mView != null) {
                                    mView.saveMsgFowShow(s);
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
                                    mView.submitBarcodeSystemFail(message);
                                }
                            }

                            @Override
                            public void _onServerError(String code, String message) {
                                if (mView != null) {
                                    mView.submitBarcodeSystemFail(message);
                                }
                            }

                            @Override
                            public void _onComplete() {
                                if (mView != null) {
                                    mView.submitBarcodeSystemSuccess();
                                }
                            }
                        });
        addSubscriber(subscriber);
    }

    /**
     * 将服务器返回的三层结构的单据数据，转换成父节点的明细数据
     *
     * @return
     */
    private List<RefDetailEntity> trans2Detail(ReferenceEntity refData) {
        List<RefDetailEntity> list = refData.billDetailList;
        for (RefDetailEntity item : list) {
            item.transId = refData.transId;
        }
        return list;
    }

}
