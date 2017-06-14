package com.richfit.sdk_wzck.base_ds_head;

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
import com.richfit.sdk_wzck.R;
import com.richfit.sdk_wzck.R2;

import butterknife.BindView;

/**
 * 物资出库抬头界面基类
 * Created by monday on 2016/11/19.
 */

public abstract class BaseDSHeadFragment<P extends IDSHeadPresenter> extends BaseHeadFragment<P>
        implements IDSHeadView {

    @BindView(R2.id.et_ref_num)
    protected RichEditText etRefNum;
    @BindView(R2.id.tv_ref_num)
    TextView tvRefNum;
    @BindView(R2.id.et_transfer_date)
    RichEditText etTransferDate;
    @BindView(R2.id.tv_creator)
    TextView tvCreator;
    @BindView(R2.id.ll_supplier)
    protected LinearLayout llSuppier;
    @BindView(R2.id.ll_customer)
    protected LinearLayout llCustomer;
    @BindView(R2.id.tv_customer)
    TextView tvCustomer;
    @BindView(R2.id.tv_supplier)
    TextView tvSupplier;
    @BindView(R2.id.ll_creator)
    protected LinearLayout llCreator;

    @Override
    public void handleBarCodeScanResult(String type, String[] list) {
        if (list != null && list.length >= 1) {
            getRefData(list[0]);
        }
    }

    @Override
    public void initVariable(Bundle savedInstanceState) {
        mRefData = null;
    }


    @Override
    protected int getContentId() {
        return R.layout.wzck_fragment_base_dsy_header;
    }

    /**
     * 注册点击事件
     */
    @Override
    public void initEvent() {
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
        etTransferDate.setText(CommonUtil.getCurrentDate(Global.GLOBAL_DATE_PATTERN_TYPE1));
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
        bindCommonHeaderUI();
    }

    /**
     * 为公共控件绑定数据
     */
    @Override
    public void bindCommonHeaderUI() {
        if (mRefData != null) {
            //单据号
            tvRefNum.setText(mRefData.recordNum);
            //创建人
            tvCreator.setText(mRefData.recordCreator);
            //供应商
            tvSupplier.setText(mRefData.supplierNum + "_" + mRefData.supplierDesc);
            //客户
            tvCustomer.setText(mRefData.customer);
            //过账日期
            if (!TextUtils.isEmpty(mRefData.voucherDate))
                etTransferDate.setText(mRefData.voucherDate);
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
            //如果是离线直接获取缓存，不能让用户删除缓存
            if (mUploadMsgEntity != null && mPresenter != null && mPresenter.isLocal()) {
                mPresenter.getTransferInfo(mRefData, refCodeId, bizType, refType);
                return;
            }
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
        clearCommonUI(tvRefNum, tvSupplier, tvCreator);
    }

    @Override
    public void clearAllUIAfterSubmitSuccess() {
        clearCommonUI(etRefNum, tvRefNum, tvSupplier, tvCreator);
        mRefData = null;
    }


    @Override
    public void _onPause() {
        super._onPause();
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

    @Override
    public boolean isNeedShowFloatingButton() {
        return false;
    }

    /*返回移动类型*/
    @CheckResult
    @NonNull
    protected abstract String getMoveType();


}
