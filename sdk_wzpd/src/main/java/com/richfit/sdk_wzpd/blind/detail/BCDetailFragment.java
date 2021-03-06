package com.richfit.sdk_wzpd.blind.detail;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.richfit.common_lib.lib_interface.IOnItemMove;
import com.richfit.common_lib.lib_mvp.BaseFragment;
import com.richfit.common_lib.lib_tree_rv.CommonTreeAdapter;
import com.richfit.common_lib.utils.UiUtil;
import com.richfit.common_lib.widget.AdvancedEditText;
import com.richfit.common_lib.widget.AutoSwipeRefreshLayout;
import com.richfit.data.constant.Global;
import com.richfit.domain.bean.InventoryEntity;
import com.richfit.domain.bean.ReferenceEntity;
import com.richfit.domain.bean.ResultEntity;
import com.richfit.sdk_wzpd.R;
import com.richfit.sdk_wzpd.R2;
import com.richfit.sdk_wzpd.adapter.BlindDetailAdapter;
import com.richfit.sdk_wzpd.blind.detail.imp.BlindDetailPresenterImp;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

/**
 * Created by monday on 2016/12/6.
 */

public class BCDetailFragment extends BaseFragment<BlindDetailPresenterImp>
        implements IBlindDetailView, SwipeRefreshLayout.OnRefreshListener, View.OnClickListener,
        IOnItemMove<InventoryEntity> {


    /*每一页展示的title个数*/
    private static final int TITLE_SIZE_IN_PAGE = 5;
    private static final int BOTTOM_BAR_HEIGHT = 60;
    private static final int PAGE_SIZE = 50;

    @BindView(R2.id.base_detail_horizontal_scroll)
    HorizontalScrollView mHScrollView;
    @BindView(R2.id.base_detail_recycler_view)
    protected RecyclerView mRecycleView;
    @BindView(R2.id.base_detail_swipe_refresh_layout)
    AutoSwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R2.id.root_id)
    LinearLayout mExtraContainer;
    @BindView(R2.id.pd_search_condition)
    LinearLayout llPdSearchCondition;
    @BindView(R2.id.id_material_condition)
    AdvancedEditText mMaterialCondition;
    @BindView(R2.id.id_location_condition)
    AdvancedEditText mLocationCondition;
    @BindView(R2.id.bottom_bar)
    LinearLayout mBottomBar;

    List<String> mTitles;
    SparseArray<TextView> mTextViews;
    SparseArray<View> mDividers;
    protected CommonTreeAdapter<InventoryEntity> mAdapter;
    int mCurrentPageNum = 0;
    String mTransNum;

    @Override
    public void handleBarCodeScanResult(String type, String[] list) {
        if (list != null && list.length >= 12) {
            final String materialNum = list[2];
            if (mMaterialCondition.isFocused()) {
                mMaterialCondition.setText(materialNum);
                startLoadInventory(mCurrentPageNum + 1);
            }
        } else if (list != null && list.length == 1) {
            final String location = list[0];
            mLocationCondition.setText(location);
            startLoadInventory(mCurrentPageNum + 1);
        }
    }


    @Override
    protected int getContentId() {
        return R.layout.wzpd_fragment_blind_detail;
    }

    @Override
    public void initPresenter() {
        mPresenter = new BlindDetailPresenterImp(mActivity);
    }

    @Override
    public void initVariable(@Nullable Bundle savedInstanceState) {
        mTitles = new ArrayList<>();
        mTextViews = new SparseArray<>();
        mDividers = new SparseArray<>();
    }

    @Override
    protected void initView() {
        // 刷新时，指示器旋转后变化的颜色
        mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_light, android.R.color.holo_red_light,
                android.R.color.holo_orange_light, android.R.color.holo_green_light);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        LinearLayoutManager lm = new LinearLayoutManager(mActivity, LinearLayoutManager.VERTICAL, false);
        mRecycleView.setLayoutManager(lm);
    }

    @Override
    public void initEvent() {
        mMaterialCondition.setOnRichEditTouchListener(new AdvancedEditText.OnRichEditTouchListener() {
            @Override
            public void onTouchSuffixIcon(View view, String text) {
                //清除
                mMaterialCondition.setText("");
            }

            @Override
            public void onTouchPrefixIcon(View view, String text) {
                //请求
                startLoadInventory(mCurrentPageNum + 1);
            }
        });

        mLocationCondition.setOnRichEditTouchListener(new AdvancedEditText.OnRichEditTouchListener() {
            @Override
            public void onTouchSuffixIcon(View view, String text) {
                //清除
                mLocationCondition.setText("");
            }

            @Override
            public void onTouchPrefixIcon(View view, String text) {
                //请求
                startLoadInventory(mCurrentPageNum + 1);
            }
        });
    }

    @Override
    public void initData() {

    }

    @Override
    public void initDataLazily() {
        if (mRefData == null) {
            showMessage("请先在盘点抬头界面初始化本次盘点");
            return;
        }
        if (isEmpty(mRefData.checkId)) {
            showMessage("请先在抬头界面初始化本次盘点");
            return;
        }

        if ("01".equals(mRefData.checkLevel) && TextUtils.isEmpty(mRefData.storageNum)) {
            showMessage("请先在抬头界面选择仓库号");
            return;
        }
        if ("02".equals(mRefData.checkLevel) && TextUtils.isEmpty(mRefData.workId)) {
            showMessage("请先在抬头界面选择工厂");
            return;
        }

        if ("02".equals(mRefData.checkLevel) && TextUtils.isEmpty(mRefData.invId)) {
            showMessage("请先在抬头界面选择库存");
            return;
        }

        if (isEmpty(mRefData.voucherDate)) {
            showMessage("请先在抬头界面选择过账日期");
            return;
        }
        startAutoRefresh();
    }

    private void startAutoRefresh() {
        mSwipeRefreshLayout.postDelayed((() -> {
            mHScrollView.scrollTo((int) (mExtraContainer.getWidth() / 2.0f - UiUtil.getScreenWidth(mActivity) / 2.0f), 0);
            mSwipeRefreshLayout.autoRefresh();
        }), 100);
    }

    @Override
    public void onRefresh() {
        startLoadInventory(mCurrentPageNum + 1);
    }

    /***
     * 初始化底部页码的标题。根据mTitles的个数初始化页码的个数
     */
    void setupBottomBar(int totalPage) {
        if (totalPage == 0)
            return;
        mBottomBar.removeAllViews();
        for (int i = 0; i < totalPage; i++) {
            //页码
            TextView tempTextView = mTextViews.get(i);
            if (tempTextView != null) {
                mBottomBar.addView(tempTextView);
            } else {
                tempTextView = createPageGuider(i);
                mTextViews.put(i, tempTextView);
                mBottomBar.addView(tempTextView);
            }
            // 分割线
            View tempDivider = mDividers.get(i);
            if (tempDivider != null) {
                mBottomBar.addView(tempDivider);
            } else {
                tempDivider = createDivider();
                mDividers.put(i, tempDivider);
                mBottomBar.addView(tempDivider);
            }
        }
        mTextViews.get(mCurrentPageNum)
                .setBackgroundColor(ContextCompat.getColor(mActivity, R.color.grey_300));
    }

    /**
     * 创建页码
     *
     * @param page
     * @return
     */
    private TextView createPageGuider(int page) {
        TextView textView = new TextView(mActivity);
        textView.setText(mTitles.get(page));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        textView.setTextColor(ContextCompat.getColor(mActivity, R.color.black));
        textView.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.grey_500));
        textView.setWidth((int) (UiUtil.getScreenWidth(mActivity) * 1.0 / TITLE_SIZE_IN_PAGE));
        textView.setHeight(BOTTOM_BAR_HEIGHT);
        textView.setGravity(Gravity.CENTER);
        textView.setId(page);
        textView.setOnClickListener(null);
        textView.setOnClickListener(this);
        return textView;
    }

    /**
     * 创建页码分割线
     */
    private View createDivider() {
        View view = new View(mActivity);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.width = 1;
        layoutParams.height = BOTTOM_BAR_HEIGHT;
        layoutParams.gravity = Gravity.CENTER;
        view.setLayoutParams(layoutParams);
        view.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.grey_500));
        return view;
    }

    @Override
    public void onClick(View view) {
        if (mTextViews != null && mTextViews.size() > 0) {
            int indexOf = mTextViews.indexOfValue((TextView) view);
            if (indexOf == mCurrentPageNum)
                return;
            jump(indexOf);
        }
    }

    /**
     * 点击改变颜色，并且跳转
     *
     * @param position
     */
    private void jump(int position) {
        int size = mTextViews.size();
        for (int i = 0; i < size; i++)
            mTextViews.get(i).setBackgroundColor(ContextCompat.getColor(mActivity, R.color.grey_500));
        mTextViews.get(position).setBackgroundColor(ContextCompat.getColor(mActivity, R.color.white));
        //自动滑动
        if (position % TITLE_SIZE_IN_PAGE == 0) {
            int firstTitleIndex = position / TITLE_SIZE_IN_PAGE;
            mHScrollView.scrollTo(firstTitleIndex * UiUtil.getScreenWidth(mActivity), 0);
        }

        //说明是向左滑动
        if (position < mCurrentPageNum) {
            //计算当前页码位于的区间的下限
            int lowLimit = (mCurrentPageNum / TITLE_SIZE_IN_PAGE) * TITLE_SIZE_IN_PAGE;
            if (position < lowLimit) {
                int firstTitleIndex = position / TITLE_SIZE_IN_PAGE;
                mHScrollView.scrollTo(firstTitleIndex * UiUtil.getScreenWidth(mActivity), 0);
            }
        }
        startLoadInventory(position + 1);
    }

    /**
     * 开始加载库存信息
     */
    private void startLoadInventory(int number) {
        String checkId = mRefData.checkId;
        String materialNum = getString(mMaterialCondition);
        String location = getString(mLocationCondition);
        int pageNum = number;
        mPresenter.getCheckTransferInfo(checkId, materialNum, location,
                "queryPage", pageNum, PAGE_SIZE, mBizType);
    }

    private void setRefreshing(boolean isSuccess) {
        mSwipeRefreshLayout.setRefreshing(false);
        mExtraContainer.setVisibility(isSuccess ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void showCheckInfo(ReferenceEntity refData, int pageNum) {
        //根据totalCount计算分页
        mCurrentPageNum = pageNum - 1;
        int totalCount = refData.totalCount;
        if (totalCount == 0)
            return;
        int tempTotalPage = totalCount % PAGE_SIZE == 0 ? totalCount / PAGE_SIZE :
                totalCount / PAGE_SIZE + 1;
        for (int i = 0; i < tempTotalPage; i++) {
            mTitles.add("第" + (i + 1) + "页");
        }
        setupBottomBar(tempTotalPage);
        setRefreshing(true);
        showNodes(refData.checkList);
    }

    @Override
    public void showNodes(List<InventoryEntity> allNodes) {
        if (allNodes == null || allNodes.size() <= 0)
            return;
        if (mAdapter == null) {
            mAdapter = new BlindDetailAdapter(mActivity, R.layout.wzpd_item_blind_detail, allNodes);
            mRecycleView.setAdapter(mAdapter);
            mAdapter.setOnItemEditAndDeleteListener(this);
        } else {
            mAdapter.addAll(allNodes);
        }
    }

    @Override
    public void loadCheckInfoFail(String message) {
        setRefreshing(false);
        showMessage(message);
        if (mAdapter != null) {
            mAdapter.removeAllVisibleNodes();
        }
    }

    @Override
    public void deleteNode(InventoryEntity node, int position) {
        if (!node.isChecked) {
            showMessage("该行还未盘点");
            return;
        }
        mPresenter.deleteNode(mRefData.checkId, node.checkLineId, Global.USER_ID, position, mBizType);
    }

    @Override
    public void editNode(InventoryEntity node, int position) {
        if (!node.isChecked) {
            showMessage("该行还未盘点");
            return;
        }
        mPresenter.editNode(node, mCompanyCode, mBizType, mRefType, "盲盘-无参考");
    }


    /**
     * 删除成功后将该行移除。
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
    public void deleteNodeFail(String message) {
        showMessage("删除失败" + message);
    }

    /**
     * 显示过账，数据上传等菜单对话框
     *
     * @param companyCode
     */
    @Override
    public void showOperationMenuOnDetail(final String companyCode) {
        if (mPresenter.isLocal()) {
            //如果是离线
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            builder.setTitle("温馨提示")
                    .setMessage("您是否要结束本次操作?")
                    .setPositiveButton("结束本次操作", (dialog, which) -> {
                        dialog.dismiss();
                        mPresenter.setTransFlag(mBizType, mRefData.checkId, "2");
                    }).setNegativeButton("取消", (dialog, which) -> dialog.dismiss()).show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            builder.setTitle("提示");
            builder.setMessage("您真的要过账本次盘点?");
            builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
            builder.setPositiveButton("确定", (dialog, which) -> {
                dialog.dismiss();
                transferCheckData();
            });
            builder.show();
        }
    }

    @Override
    public void setTransFlagFail(String message) {
        showMessage(message);
    }

    @Override
    public void setTransFlagsComplete() {
        showMessage("结束本次操作!");
        startAutoRefresh();
    }


    /**
     * 过账
     */
    private void transferCheckData() {
        if (!checkDataBeforeOperationOnDetail()) {
            return;
        }
        mTransNum = "";
        mPresenter.transferCheckData(mRefData.checkId, Global.USER_ID, mBizType);
    }

    @Override
    public void transferCheckDataSuccess() {
        showSuccessDialog(mTransNum);
        mPresenter.showHeadFragmentByPosition(BaseFragment.HEADER_FRAGMENT_INDEX);
    }

    @Override
    public void showTransferedNum(String transNum) {
        mTransNum = transNum;
    }

    @Override
    public void transferCheckDataFail(String message) {
        showMessage(message);
    }

    @Override
    public void retry(String retryAction) {
        switch (retryAction) {
            case Global.RETRY_TRANSFER_DATA_ACTION:
                mPresenter.transferCheckData(mRefData.checkId, Global.USER_ID, mBizType);
                break;
        }
        super.retry(retryAction);
    }

    @Override
    public void _onPause() {
        super._onPause();
    }
}
