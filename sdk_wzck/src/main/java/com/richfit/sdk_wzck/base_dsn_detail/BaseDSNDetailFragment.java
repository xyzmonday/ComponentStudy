package com.richfit.sdk_wzck.base_dsn_detail;

import android.text.TextUtils;
import android.view.View;

import com.richfit.common_lib.lib_base_sdk.base_detail.BaseDetailFragment;
import com.richfit.common_lib.lib_mvp.BaseFragment;
import com.richfit.data.constant.Global;
import com.richfit.domain.bean.BottomMenuEntity;
import com.richfit.domain.bean.RefDetailEntity;
import com.richfit.sdk_wzck.R;
import com.richfit.sdk_wzck.adapter.DSNDetailAdapter;

import java.util.ArrayList;
import java.util.List;

import static com.richfit.common_lib.utils.SPrefUtil.getData;

/**
 * Created by monday on 2017/2/23.
 */

public abstract class BaseDSNDetailFragment<P extends IDSNDetailPresenter> extends BaseDetailFragment<P, RefDetailEntity>
        implements IDSNDetailView<RefDetailEntity> {


    /*处理寄售转自有业务。主要的逻辑是用户点击过账按钮之后系统自动检查该缓存(子节点)中是否有特殊库存标识是否
    * 等于K而且特殊库存编号不为空。如果满足以上的条件，那么系统自动调用转自有的接口。如果转自有成功修改成员变量
    * isTurnSuccess为true。如果业务在上传数据的时候有第二步，那么需要检查该字段*/
    /*是否需要寄售转自有*/
    protected boolean isNeedTurn = false;
    /*转自有是否成功*/
    protected boolean isTurnSuccess = false;

    @Override
    protected int getContentId() {
        return R.layout.wzck_fragment_base_dsn_detail;
    }

    @Override
    protected void initDataLazily() {
        if (mRefData == null) {
            showMessage("请先在抬头界面选择工厂");
            return;
        }
        if (TextUtils.isEmpty(mRefData.workId)) {
            showMessage("请先在抬头界面选择工厂");
            return;
        }
        //这里先将寄售转自有的相关标记清空
        isNeedTurn = false;
        isTurnSuccess = false;
        startAutoRefresh();
    }


    @Override
    public void showNodes(List<RefDetailEntity> allNodes) {
        saveTransId(allNodes);
        saveTurnFlag(allNodes);
        if (mAdapter == null) {
            mAdapter = new DSNDetailAdapter(mActivity, R.layout.wzck_item_dsn_detail_parent_item, allNodes);
            mRecyclerView.setAdapter(mAdapter);
            mAdapter.setOnItemEditAndDeleteListener(this);
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
     * 保存缓存的抬头Id
     *
     * @param allNodes
     */
    protected void saveTransId(List<RefDetailEntity> allNodes) {
        for (RefDetailEntity node : allNodes) {
            if (!TextUtils.isEmpty(node.transId)) {
                mTransId = node.transId;
                break;
            }
        }
    }

    /**
     * 获取是否该明细是否需要转自有。
     *
     * @param nodes
     */
    private void saveTurnFlag(final List<RefDetailEntity> nodes) {
        for (RefDetailEntity node : nodes) {
            if ("K".equalsIgnoreCase(node.specialInvFlag) && !isEmpty(node.specialInvNum)) {
                isNeedTurn = true;
                break;
            }
        }
    }

    /**
     * 修改明细
     *
     * @param node
     * @param position
     */
    @Override
    public void editNode(final RefDetailEntity node, int position) {
        String state = (String) getData(mBizType, "0");
        if (!"0".equals(state)) {
            showMessage("已经过账,不允许修改");
            return;
        }
        //获取与该子节点的物料编码和发出库位一致的发出仓位和接收仓位列表
        if (mAdapter != null && DSNDetailAdapter.class.isInstance(mAdapter)) {
            DSNDetailAdapter adapter = (DSNDetailAdapter) mAdapter;
            ArrayList<String> sendLocations = adapter.getLocations(position, 0);
            mPresenter.editNode(sendLocations, null, null, node, mCompanyCode,
                    mBizType, mRefType, getSubFunName(), position);
        }
    }


    /**
     * 删除明细数据
     *
     * @param node
     * @param position
     */
    @Override
    public void deleteNode(final RefDetailEntity node, int position) {
        String state = (String) getData(mBizType, "0");
        if (!"0".equals(state)) {
            showMessage("已经过账,不允许删除");
            return;
        }
        mPresenter.deleteNode("N", node.transId, node.transLineId, node.locationId,
                mRefData.refType, mRefData.bizType,node.refLineId,Global.USER_ID, position, mCompanyCode);
    }

    /**
     * 删除成功后回到该方法，注意这里是无参考明细界面所以重写该方法
     *
     * @param position：节点在明细列表的位置
     */
    @Override
    public void deleteNodeSuccess(int position) {
        showMessage("删除成功");
        if (mAdapter != null) {
            mAdapter.removeItemByPosition(position);
        }
    }

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
    protected void submit2BarcodeSystem(String tranToSapFlag) {
        //如果需要寄售转自有但是没有成功过，都需要用户需要再次寄售转自有
        if (isNeedTurn && !isTurnSuccess) {
            startTurnOwnSupplies("07");
            return;
        }
        String transferFlag = (String) getData(mBizType, "0");
        if ("1".equals(transferFlag)) {
            showMessage(getString(R.string.msg_detail_off_location));
            return;
        }
        mExtraTansMap.clear();
        mExtraTansMap.put("centerCost", mRefData.costCenter);
        mExtraTansMap.put("projectNum", mRefData.projectNum);
        mPresenter.submitData2BarcodeSystem("", mTransId, mRefData.bizType, mRefType, Global.USER_ID,
                mRefData.voucherDate, tranToSapFlag, mExtraTansMap);
    }

    /**
     * 第一步过账成功后显示物料凭证
     */
    @Override
    public void submitBarcodeSystemSuccess() {
        showSuccessDialog(mShowMsg);
    }


    /**
     * 2.数据上传
     */
    protected void submit2SAP(String tranToSapFlag) {
        String state = (String) getData(mBizType, "0");
        if ("0".equals(state)) {
            showMessage("请先过账");
            return;
        }
        mPresenter.submitData2SAP(mTransId, mRefData.bizType, mRefType, Global.USER_ID,
                mRefData.voucherDate, tranToSapFlag, null);
    }

    /**
     * 第二步下架成功后跳转到抬头界面
     */
    @Override
    public void submitSAPSuccess() {
        setRefreshing(false, "下架成功");
        showSuccessDialog(mShowMsg);
        if (mAdapter != null) {
            mAdapter.removeAllVisibleNodes();
        }
        mRefData = null;
        mShowMsg.setLength(0);
        mTransId = "";
        mPresenter.showHeadFragmentByPosition(BaseFragment.HEADER_FRAGMENT_INDEX);
    }

    /**
     * 第三步转储入口
     * @param tranToSapFlag
     */
    @Override
    protected void sapUpAndDownLocation(String tranToSapFlag) {

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
    public List<BottomMenuEntity> provideDefaultBottomMenu() {
        List<BottomMenuEntity> tmp = super.provideDefaultBottomMenu();
        tmp.get(0).transToSapFlag = "01";
        tmp.get(2).transToSapFlag = "05";
        ArrayList<BottomMenuEntity> menus = new ArrayList<>();
        menus.add(tmp.get(0));
        menus.add(tmp.get(2));
        return menus;
    }

    @Override
    protected boolean checkTransStateBeforeRefresh() {
        String transferKey = (String) getData(mBizType, "0");
        if ("1".equals(transferKey)) {
            setRefreshing(false, getString(R.string.msg_detail_off_location));
            return false;
        }
        return true;
    }

    /*子类返回修改模块的名称*/
    protected abstract String getSubFunName();
}
