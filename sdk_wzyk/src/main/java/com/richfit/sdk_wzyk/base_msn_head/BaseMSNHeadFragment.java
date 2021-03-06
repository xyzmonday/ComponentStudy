package com.richfit.sdk_wzyk.base_msn_head;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.jakewharton.rxbinding2.widget.RxAdapterView;
import com.richfit.common_lib.lib_adapter.InvAdapter;
import com.richfit.common_lib.lib_adapter.WorkAdapter;
import com.richfit.common_lib.lib_base_sdk.base_head.BaseHeadFragment;
import com.richfit.common_lib.utils.DateChooseHelper;
import com.richfit.common_lib.utils.SPrefUtil;
import com.richfit.common_lib.widget.RichEditText;
import com.richfit.data.constant.Global;
import com.richfit.data.helper.CommonUtil;
import com.richfit.domain.bean.InvEntity;
import com.richfit.domain.bean.ReferenceEntity;
import com.richfit.domain.bean.SimpleEntity;
import com.richfit.domain.bean.WorkEntity;
import com.richfit.sdk_wzyk.R;
import com.richfit.sdk_wzyk.R2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Created by monday on 2016/11/20.
 */

public abstract class BaseMSNHeadFragment<P extends IMSNHeadPresenter> extends BaseHeadFragment<P>
        implements IMSNHeadView {

    //发出工厂
    @BindView(R2.id.ll_send_work)
    protected LinearLayout llSendWork;
    @BindView(R2.id.tv_send_work_name)
    protected TextView tvSendWorkName;
    @BindView(R2.id.sp_send_work)
    protected Spinner spSendWork;

    //发出库位
    @BindView(R2.id.ll_send_inv)
    protected LinearLayout llSendInv;
    @BindView(R2.id.sp_send_inv)
    protected Spinner spSendInv;

    //接收工厂
    @BindView(R2.id.ll_rec_work)
    protected LinearLayout llRecWork;
    @BindView(R2.id.sp_rec_work)
    protected Spinner spRecWork;

    //接收库位
    @BindView(R2.id.ll_rec_inv)
    protected LinearLayout llRecInv;
    @BindView(R2.id.sp_rec_inv)
    protected Spinner spRecInv;

    @BindView(R2.id.et_transfer_date)
    protected RichEditText etTransferDate;

    /*发出工厂*/
    protected WorkAdapter mSendWorkAdapter;
    protected List<WorkEntity> mSendWorks;

    /*发出库位*/
    protected InvAdapter mSendInvAdapter;
    protected List<InvEntity> mSendInvs;

    /*接收工厂*/
    protected WorkAdapter mRecWorkAdpater;
    protected List<WorkEntity> mRecWorks;
    /*接收库位*/
    protected InvAdapter mRecInvAdapter;
    protected List<InvEntity> mRecInvs;

    @Override
    protected int getContentId() {
        return R.layout.wzyk_fragment_base_msn_header;
    }

    protected void initVariable(Bundle savedInstanceState) {
        super.initVariable(savedInstanceState);
        mSendWorks = new ArrayList<>();
        mSendInvs = new ArrayList<>();
        mRecWorks = new ArrayList<>();
        mRecInvs = new ArrayList<>();
    }


    /**
     * 注册点击事件
     */
    @Override
    protected void initEvent() {
        //过账日期
        etTransferDate.setOnRichEditTouchListener((view, text) ->
                DateChooseHelper.chooseDateForEditText(mActivity, etTransferDate, Global.GLOBAL_DATE_PATTERN_TYPE1));

        //发出工厂
        RxAdapterView.itemSelections(spSendWork)
                .filter(position -> position.intValue() > 0)
                .subscribe(position -> {
                    int recPosition = spRecWork.getSelectedItemPosition();
                    if (recPosition > 0 && position.intValue() == recPosition) {
                        showMessage("发出工厂不能与接收工厂一致,请重新选择");
                        spSendWork.setSelection(0);
                    } else {
                        mPresenter.getSendInvsByWorkId(mSendWorks.get(position.intValue()).workId, getOrgFlag());
                    }
                });

        //接收工厂
        RxAdapterView.itemSelections(spRecWork)
                .filter(aInteger -> {
                    int sendPosition = spSendWork.getSelectedItemPosition();
                    int recPosition = aInteger.intValue();
                    if (recPosition <= 0) {
                        return false;
                    }
                    if (sendPosition > 0 && recPosition > 0 && sendPosition == recPosition) {
                        showMessage("发出工厂不能与接收工厂一致,请重新选择");
                        spRecWork.setSelection(0);
                        return false;
                    }
                    return true;
                })
                .filter(position -> position.intValue() > 0)
                .subscribe(position -> mPresenter.getRecInvsByWorkId( mRecWorks.get(position.intValue()).workId, getOrgFlag()));
    }

    @Override
    protected void initData() {
        SPrefUtil.saveData(mBizType, "0");
        etTransferDate.setText(CommonUtil.getCurrentDate(Global.GLOBAL_DATE_PATTERN_TYPE1));
        //获取发出工厂列表
        mPresenter.getWorks(getOrgFlag());
        //如果是离线直接获取缓存，不能让用户删除缓存
        if (mUploadMsgEntity != null && mPresenter != null && mPresenter.isLocal()) {
            return;
        }
        //删除历史数据
        mPresenter.deleteCollectionData("", mBizType, Global.USER_ID, mCompanyCode);
    }

    @Override
    public void deleteCacheSuccess(String message) {
        showMessage(message);
    }

    @Override
    public void deleteCacheFail(String message) {
        showMessage(message);
    }

    @Override
    public void showWorks(List<WorkEntity> works) {
        mSendWorks.clear();
        mSendWorks.addAll(works);
        //绑定适配器
        if (mSendWorkAdapter == null) {
            mSendWorkAdapter = new WorkAdapter(mActivity, R.layout.item_simple_sp, mSendWorks);
            spSendWork.setAdapter(mSendWorkAdapter);
        } else {
            mSendWorkAdapter.notifyDataSetChanged();
        }

        if (llRecWork.getVisibility() != View.GONE) {
            mRecWorks.clear();
            mRecWorks.addAll(works);
            //绑定适配器
            if (mRecWorkAdpater == null) {
                mRecWorkAdpater = new WorkAdapter(mActivity, R.layout.item_simple_sp, mRecWorks);
                spRecWork.setAdapter(mRecWorkAdpater);
            } else {
                mRecWorkAdpater.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void loadWorksFail(String message) {
        showMessage(message);
    }

    /**
     * 发出工厂和接收工厂初始化完毕
     */
    @Override
    public void loadWorksComplete() {
        //处理离线情况
        if (mUploadMsgEntity != null && !TextUtils.isEmpty(mUploadMsgEntity.workId)) {
            selectedWork(mSendWorks, mUploadMsgEntity.workId, spSendWork);
            selectedWork(mRecWorks, mUploadMsgEntity.workId, spRecWork);
        }
    }

    private void selectedWork(List<WorkEntity> works, final String workId, Spinner sp) {
        if (works == null || works.size() == 0 || TextUtils.isEmpty(workId))
            return;
        int pos = -1;
        for (WorkEntity item : works) {
            ++pos;
            if (workId.equalsIgnoreCase(item.workId))
                break;
        }
        if (pos > 0) {
            sp.setSelection(pos);
            lockUIUnderEditState(sp);
        }
    }

    @Override
    public void _onPause() {
        super._onPause();
        if (mRefData == null)
            mRefData = new ReferenceEntity();

        //过账日期
        mRefData.voucherDate = getString(etTransferDate);
        mRefData.bizType = mBizType;
        mRefData.moveType = getMoveType();

        //发出工厂(工厂)
        if (mSendWorks != null && mSendWorks.size() > 0 && spSendWork != null && spSendWork.getAdapter() != null) {
            final int position = spSendWork.getSelectedItemPosition();
            mRefData.workCode = mSendWorks.get(position).workCode;
            mRefData.workName = mSendWorks.get(position).workName;
            mRefData.workId = mSendWorks.get(position).workId;
        }

        //发出库位
        if (mSendInvs != null && mSendInvs.size() > 0 && spSendInv != null && spSendInv.getAdapter() != null) {
            final int position = spSendInv.getSelectedItemPosition();
            mRefData.invCode = mSendInvs.get(position).invCode;
            mRefData.invName = mSendInvs.get(position).invName;
            mRefData.invId = mSendInvs.get(position).invId;
        }

        //接收工厂
        if (mRecWorks != null && mRecWorks.size() > 0 && spRecWork != null && spRecWork.getAdapter() != null) {
            final int position = spRecWork.getSelectedItemPosition();
            mRefData.recWorkName = mRecWorks.get(position).workName;
            mRefData.recWorkCode = mRecWorks.get(position).workCode;
            mRefData.recWorkId = mRecWorks.get(position).workId;
        }

        //接收库位
        if (mRecInvs != null && mRecInvs.size() > 0 && spRecInv != null && spRecInv.getAdapter() != null) {
            final int position = spRecInv.getSelectedItemPosition();
            mRefData.recInvCode = mRecInvs.get(position).invCode;
            mRefData.recInvName = mRecInvs.get(position).invName;
            mRefData.recInvId = mRecInvs.get(position).invId;
        }
    }

    @Override
    public void clearAllUI() {
        spSendWork.setSelection(0);
        spSendInv.setSelection(0);
        spRecWork.setSelection(0);
        spRecInv.setSelection(0);
    }

    @Override
    public void showProjectNums(Map<String, List<SimpleEntity>> map) {

    }

    @Override
    public void loadProjectNumsFail(String message) {

    }

    @Override
    public void clearAllUIAfterSubmitSuccess() {
        super.clearAllUIAfterSubmitSuccess();
    }

    protected abstract String getMoveType();

    /**
     * 返回组织机构flag，0表示ERP的组织机构；1表示二级单位的组织机构
     *
     * @return
     */
    protected abstract int getOrgFlag();
}
