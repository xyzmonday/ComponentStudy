package com.richfit.sdk_wzck.base_ds_detail;

import android.text.TextUtils;
import android.widget.TextView;

import com.richfit.common_lib.lib_base_sdk.base_detail.BaseDetailFragment;
import com.richfit.common_lib.lib_mvp.BaseFragment;
import com.richfit.common_lib.utils.SPrefUtil;
import com.richfit.data.constant.Global;
import com.richfit.domain.bean.RefDetailEntity;
import com.richfit.domain.bean.TreeNode;
import com.richfit.sdk_wzck.R;
import com.richfit.sdk_wzck.R2;
import com.richfit.sdk_wzck.adapter.DSYDetailAdapter;

import java.util.List;

import butterknife.BindView;

import static com.richfit.common_lib.utils.SPrefUtil.getData;

/**
 * Created by monday on 2016/11/20.
 */

public abstract class BaseDSDetailFragment<P extends IDSDetailPresenter> extends BaseDetailFragment<P, RefDetailEntity>
        implements IDSDetailView<RefDetailEntity> {

    /**
     * 处理寄售转自有业务。主要的逻辑是用户点击过账按钮之后系统自动检查该缓存(子节点)中是否有特殊库存标识是否
     * 等于K而且特殊库存编号不为空。如果满足以上的条件，那么系统自动调用转自有的接口。如果转自有成功修改成员变量
     * isTurnSuccess为true。如果业务在上传数据的时候有第二步，那么需要检查该字段
     */
    /*是否需要寄售转自有*/
    protected boolean isNeedTurn = false;
    /*转自有是否成功*/
    protected boolean isTurnSuccess = false;


    @Override
    protected int getContentId() {
        return R.layout.wzck_fragment_base_dsy_detail;
    }

    @Override
    protected void initDataLazily() {
        if (mRefData == null) {
            showMessage("请现在抬头界面获取单据数据");
            return;
        }

        if (isEmpty(mRefData.recordNum)) {
            showMessage("请现在抬头界面输入参考单号");
            return;
        }

        if (isEmpty(mRefData.bizType)) {
            showMessage("未获取到业务类型");
            return;
        }

        if (isEmpty(mRefData.refType)) {
            showMessage("未获取到单据类型");
            return;
        }
        //这里先将寄售转自有的相关标记清空
        isNeedTurn = false;
        isTurnSuccess = false;
        startAutoRefresh();
    }

    /**
     * 如果不是标准的出库，需要重写该方法。
     */
    @Override
    public void showNodes(List<RefDetailEntity> allNodes) {
        saveTransId(allNodes);
        saveTurnFlag(allNodes);
        if (mAdapter == null) {
            mAdapter = new DSYDetailAdapter(mActivity, allNodes);
            mRecyclerView.setAdapter(mAdapter);
            mAdapter.setOnItemEditAndDeleteListener(this);
            mAdapter.setAdapterStateListener(this);
        } else {
            mAdapter.addAll(allNodes);
        }
    }

    /**
     * 刷新界面结束。注意如果用户切换界面(修改仓位等),那么系统不再自动过账
     */
    @Override
    public void refreshComplete() {
        super.refreshComplete();
        if (!isNeedTurn && isTurnSuccess) {
            //如果寄售转自有成功后，系统自动去过账。
            submit2BarcodeSystem(mBottomMenus.get(0).transToSapFlag);
        }
    }

    /**
     * 获取是否该明细是否需要转自有。
     *
     * @param nodes
     */
    protected void saveTurnFlag(final List<RefDetailEntity> nodes) {
        //仅仅检查子节点
        for (RefDetailEntity node : nodes) {
            if (Global.CHILD_NODE_ITEM_TYPE == node.getViewType() &&
                    "Y".equalsIgnoreCase(node.specialConvert)) {
                isNeedTurn = true;
                break;
            }
        }
    }

    /**
     * 修改明细里面的子节点
     *
     * @param node
     * @param position
     */
    @Override
    public void editNode(final RefDetailEntity node, int position) {
        String state = (String) getData(mBizType + mRefType, "0");
        if (!"0".equals(state)) {
            showMessage("已经过账,不允许修改");
            return;
        }
        mPresenter.editNode(null, null, mRefData, node, mCompanyCode, mBizType, mRefType,
                getSubFunName(), -1);
    }

    @Override
    public void deleteNode(final RefDetailEntity node, int position) {
        String state = (String) getData(mBizType + mRefType, "0");
        if (!"0".equals(state)) {
            showMessage("已经过账,不允许删除");
            return;
        }
        if (TextUtils.isEmpty(node.transLineId)) {
            showMessage("该行还未进行数据采集");
            return;
        }
        TreeNode parentNode = node.getParent();
        String lineDeleteFlag;
        if (parentNode == null) {
            lineDeleteFlag = "N";
        } else {
            lineDeleteFlag = parentNode.getChildren().size() > 1 ? "N" : "Y";
        }

        mPresenter.deleteNode(lineDeleteFlag, node.transId, node.transLineId,
                node.locationId, mRefData.refType, mRefData.bizType, node.refLineId, Global.USER_ID,
                position, mCompanyCode);
    }

    /**
     * 这里默认给出的是具有父子节点结构时，删除子节点情况下的回调。
     */
    @Override
    public void deleteNodeSuccess(int position) {
        showMessage("删除成功");
        if (mAdapter != null) {
            mAdapter.removeNodeByPosition(position);
        }
        startAutoRefresh();
    }

    /**
     * 显示过账，上传等底部菜单之前进行必要的检查。注意子类可以根据自己的需求
     * 自行添加检查的字段。父类仅仅做了最基本的检查。
     *
     * @return
     */
    @Override
    public boolean checkDataBeforeOperationOnDetail() {
        if (mRefData == null) {
            showMessage("请先获取单据数据");
            return false;
        }
        if (TextUtils.isEmpty(mTransId)) {
            showMessage("未获取缓存标识");
            return false;
        }

        if (TextUtils.isEmpty(mRefData.voucherDate)) {
            showMessage("请先选择过账日期");
            return false;
        }
        return true;
    }

    /**
     * 1.过账
     */
    @Override
    protected void submit2BarcodeSystem(String transToSapFlag) {
        //如果需要寄售转自有但是没有成功过，都需要用户需要再次寄售转自有
        if (isNeedTurn && !isTurnSuccess) {
            startTurnOwnSupplies("07");
            return;
        }
        String state = (String) SPrefUtil.getData(mBizType + mRefType, "0");
        if (!"0".equals(state)) {
            showMessage(getString(R.string.msg_detail_off_location));
            return;
        }
        mShowMsg.setLength(0);
        mExtraTansMap.clear();
        mPresenter.submitData2BarcodeSystem(mRefData.refCodeId, mTransId, mBizType, mRefType, Global.USER_ID,
                mRefData.voucherDate, transToSapFlag, mExtraTansMap);
    }

    /**
     * 第一步过账成功显示物料凭证
     */
    @Override
    public void submitBarcodeSystemSuccess() {
        showSuccessDialog(mShowMsg);
    }

    /**
     * 2.数据上传
     */
    protected void submit2SAP(String transToSapFlag) {
        String state = (String) getData(mBizType + mRefType, "0");
        if ("0".equals(state)) {
            showMessage("请先过账");
            return;
        }
        mShowMsg.setLength(0);
        mExtraTansMap.clear();
        mPresenter.submitData2SAP(mTransId, mRefData.bizType, mRefType, Global.USER_ID,
                mRefData.voucherDate, transToSapFlag, null);
    }

    /**
     * 第二步数据上传成功
     */
    @Override
    public void submitSAPSuccess() {
        setRefreshing(false, "下架成功");
        showSuccessDialog(mShowMsg);
        if (mAdapter != null) {
            mAdapter.removeAllVisibleNodes();
        }
        //注意这里必须清除单据数据
        mRefData = null;
        mShowMsg.setLength(0);
        mTransId = "";
        //两步成功后将寄售转自有标识清空
        isNeedTurn = false;
        isTurnSuccess = false;
        mPresenter.showHeadFragmentByPosition(BaseFragment.HEADER_FRAGMENT_INDEX);
    }

    /**
     * 3. 如果submitFlag:2那么分三步进行转储处理
     */
    protected void sapUpAndDownLocation(String transToSapFlag) {

    }


    @Override
    public void upAndDownLocationSuccess() {

    }

    /**
     * 开始寄售转自有
     *
     * @param transToSapFlag
     */
    protected void startTurnOwnSupplies(String transToSapFlag) {
        if (isEmpty(mTransId)) {
            showMessage("未获取到缓存,请先获取采集数据");
            return;
        }
        mShowMsg.setLength(0);
        mPresenter.turnOwnSupplies(mTransId, mRefData.bizType, mRefType, Global.USER_ID,
                mRefData.voucherDate, transToSapFlag, null, -1);
    }

    /**
     * 寄售转自有成功
     */
    @Override
    public void turnOwnSuppliesSuccess() {
        isTurnSuccess = true;
        isNeedTurn = false;
        //注意这里寄售转自有成功后，先刷新数据
        startAutoRefresh();
    }

    /**
     * 寄售转自有失败
     */
    @Override
    public void turnOwnSuppliesFail(String message) {
        showErrorDialog(TextUtils.isEmpty(message) ? "寄售转自有失败" : message);
        isTurnSuccess = false;
        isNeedTurn = true;
    }


    @Override
    protected boolean checkTransStateBeforeRefresh() {
        String transferFlag = (String) getData(mBizType + mRefType, "0");
        if ("1".equals(transferFlag)) {
            setRefreshing(false, getString(R.string.msg_detail_off_location));
            return false;
        }
        return true;
    }

    /*子类返回修改模块的名称*/
    protected abstract String getSubFunName();
}
