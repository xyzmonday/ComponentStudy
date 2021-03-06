package com.richfit.sdk_wzyk.base_ms_head;

import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.richfit.common_lib.lib_base_sdk.base_head.BaseHeadFragment;
import com.richfit.common_lib.utils.DateChooseHelper;
import com.richfit.common_lib.utils.SPrefUtil;
import com.richfit.common_lib.widget.RichEditText;
import com.richfit.data.constant.Global;
import com.richfit.data.helper.CommonUtil;
import com.richfit.domain.bean.ReferenceEntity;
import com.richfit.sdk_wzyk.R;
import com.richfit.sdk_wzyk.R2;

import butterknife.BindView;

/**
 * 发出
 * Created by monday on 2017/2/9.
 */

public abstract class BaseMSHeadFragment<P extends IMSHeadPresenter> extends BaseHeadFragment<P>
        implements IMSHeadView {


    @BindView(R2.id.et_ref_num)
    RichEditText etRefNum;
    @BindView(R2.id.tv_ref_num)
    TextView tvRefNum;

    //发出工厂
    @BindView(R2.id.ll_send_work)
    protected LinearLayout llSendWork;
    @BindView(R2.id.tv_send_work)
    protected TextView tvSendWork;

    //接收工厂
    @BindView(R2.id.ll_rec_work)
    protected LinearLayout llRecWork;
    @BindView(R2.id.tv_rec_work)
    protected TextView tvRecWork;

    //库存地点
    @BindView(R2.id.ll_inv)
    protected LinearLayout llInv;
    @BindView(R2.id.tv_inv_name)
    protected TextView tvInvName;
    @BindView(R2.id.tv_inv)
    protected TextView tvInv;
    @BindView(R2.id.et_transfer_date)
    RichEditText etTransferDate;

    @Override
    public void handleBarCodeScanResult(String type, String[] list) {
        if (list != null && list.length == 1) {
            getRefData(list[0]);
        }
    }

    @Override
    protected int getContentId() {
        return R.layout.wzyk_fragment_base_msy_header;
    }

    /**
     * 注册点击事件
     */
    @Override
    protected void initEvent() {
        /*点击单号加载单据数据*/
        etRefNum.setOnRichEditTouchListener((view, refNum) -> {
            hideKeyboard(view);
            getRefData(refNum);
        });

        /*选择日期*/
        etTransferDate.setOnRichEditTouchListener((view, text) ->
                DateChooseHelper.chooseDateForEditText(mActivity, etTransferDate, Global.GLOBAL_DATE_PATTERN_TYPE1));
    }

    @Override
    protected void initView() {

    }

    @Override
    public void initData() {
        etTransferDate.setText(CommonUtil.getCurrentDate(Global.GLOBAL_DATE_PATTERN_TYPE1));
        if (mUploadMsgEntity != null && mPresenter != null && mPresenter.isLocal() &&
                !TextUtils.isEmpty(mUploadMsgEntity.transId) && !TextUtils.isEmpty(mUploadMsgEntity.refNum)) {
            etRefNum.setText(mUploadMsgEntity.refNum);
            getRefData(mUploadMsgEntity.refNum);
            //如果是离线那么锁定控件
            lockUIUnderEditState(etRefNum);
        }
    }

    protected void getRefData(String refNum) {
        mRefData = null;
        clearAllUI();
        mPresenter.getReference(refNum, mRefType, mBizType, getMoveType(), "", Global.USER_ID);
    }


    /**
     * 删除整单缓存数据成功
     */
    @Override
    public void deleteCacheSuccess() {
        showMessage("缓存删除成功");
        bindCommonHeaderUI();
    }

    /**
     * 删除整单缓存数据失败
     *
     * @param message
     */
    @Override
    public void deleteCacheFail(String message) {
        showMessage(message);
        bindCommonHeaderUI();
    }

    @Override
    public void getTransferInfoFail(String message) {
        showMessage(message);
    }

    /**
     * 为公共控件绑定数据
     */
    @Override
    public void bindCommonHeaderUI() {
        if (mRefData != null) {
            //单据号
            tvRefNum.setText(mRefData.recordNum);
            //发出工厂
            tvSendWork.setText(mRefData.workCode);
            //接收工厂
            tvRecWork.setText(mRefData.recWorkCode);
            //发出库位
            tvInv.setText(mRefData.invCode);
        }
    }

    /**
     * 获取单据数据成功
     *
     * @param refData
     */
    @Override
    public void getReferenceSuccess(ReferenceEntity refData) {
        //将过账标识重置
        //过账标识，如果已经过账，那么不允许在明细刷新数据，也不运行在采集界面采集数据
        SPrefUtil.saveData(mBizType + mRefType, "0");
        refData.bizType = mBizType;
        refData.moveType = getMoveType();
        refData.refType = mRefType;
        mRefData = refData;
        cacheProcessor(mRefData.transId, mRefData.transId, mRefData.recordNum,
                mRefData.refCodeId, mRefData.refType, mRefData.bizType);
    }

    /**
     * 获取单据数据失败
     *
     * @param message
     */
    @Override
    public void getReferenceFail(String message) {
        showMessage(message);
        mRefData = null;
        //清除所有控件绑定的数据
        clearAllUI();
    }


    /**
     * /**
     * 检查数据库是否存在该历史数据.
     * 如果有缓存提示用户是否删除缓存。
     * 如果用户点击确定删除那么删除缓存，并刷新界面；
     * 如果用户点击取消删除那么直接刷新界面，在采集界面和明细界面会重新获取缓存
     *
     * @param cacheFlag：缓存标志。有可能是Y,N或者TransId标识
     * @param transId：缓存id,用于删除缓存
     * @param refNum：单据号
     * @param bizType：业务类型
     */
    @Override
    public void cacheProcessor(String cacheFlag, String transId, String refNum,
                               String refCodeId, String refType, String bizType) {
        if (!TextUtils.isEmpty(cacheFlag)) {
            android.support.v7.app.AlertDialog.Builder dialog = new android.support.v7.app.AlertDialog.Builder(mActivity);
            dialog.setTitle("提示");
            dialog.setIcon(R.mipmap.icon_tips);
            dialog.setMessage(getString(R.string.msg_has_history));
            dialog.setPositiveButton("确定", (dia, which) -> {
                dia.dismiss();
                mPresenter.deleteCollectionData(refNum, transId, refCodeId, refType, bizType,
                        Global.USER_ID, mCompanyCode);
            });
            dialog.setNegativeButton("取消", (dia, which) -> {
                dia.dismiss();
                mPresenter.getTransferInfo(mRefData, refCodeId, bizType, refType);
            });
            dialog.show();
        } else {
            bindCommonHeaderUI();
        }
    }

    @Override
    public void clearAllUI() {
        clearCommonUI(etRefNum, tvRefNum, tvSendWork, tvSendWork, tvRecWork, tvInv);
    }

    @Override
    public void clearAllUIAfterSubmitSuccess() {
        super.clearAllUIAfterSubmitSuccess();
        clearCommonUI(etRefNum, tvRefNum, tvSendWork, tvRecWork, tvInv);
        mRefData = null;
    }


    @Override
    public void _onPause() {
        //再次检查用户是否输入的额外字段而且必须输入的字段（情景是用户请求单据之前没有输入该字段，回来填上后，但是没有请求单据而是直接）
        //切换了页面
        if (mRefData != null) {
            mRefData.voucherDate = getString(etTransferDate);
        }
    }

    @Override
    public void retry(String retryAction) {
        switch (retryAction) {
            case Global.RETRY_LOAD_REFERENCE_ACTION:
                mPresenter.getReference(getString(etRefNum), mRefType, mBizType, getMoveType(), "", Global.LOGIN_ID);
                break;
        }
        super.retry(retryAction);
    }

    /*返回移动类型*/
    @CheckResult
    @NonNull
    protected abstract String getMoveType();
}
