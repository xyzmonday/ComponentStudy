package com.richfit.sdk_wzpd.checkn.head;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxAdapterView;
import com.jakewharton.rxbinding2.widget.RxCompoundButton;
import com.richfit.common_lib.lib_adapter.BottomDialogMenuAdapter;
import com.richfit.common_lib.lib_adapter.InvAdapter;
import com.richfit.common_lib.lib_adapter.SimpleAdapter;
import com.richfit.common_lib.lib_adapter.WorkAdapter;
import com.richfit.common_lib.lib_base_sdk.base_head.BaseHeadFragment;
import com.richfit.common_lib.utils.DateChooseHelper;
import com.richfit.common_lib.utils.SPrefUtil;
import com.richfit.common_lib.widget.RichEditText;
import com.richfit.data.constant.Global;
import com.richfit.data.helper.CommonUtil;
import com.richfit.domain.bean.BottomMenuEntity;
import com.richfit.domain.bean.InvEntity;
import com.richfit.domain.bean.ReferenceEntity;
import com.richfit.domain.bean.SimpleEntity;
import com.richfit.domain.bean.WorkEntity;
import com.richfit.sdk_wzpd.R;
import com.richfit.sdk_wzpd.R2;
import com.richfit.sdk_wzpd.checkn.head.imp.CNHeadPresenterImp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;

import static com.richfit.data.constant.Global.USER_ID;


/**
 * 青海无参考明盘抬头界面。仓库级选择工厂和库存地点;
 * 仓位级选择库存号。checkLevel表示盘点的级别，01表示仓位级;02表示库存级
 * <p>
 * Created by monday on 2017/3/3.
 */
public class CNHeadFragment extends BaseHeadFragment<CNHeadPresenterImp>
        implements ICNHeadView {


    @BindView(R2.id.ll_warehouse_level)
    LinearLayout llWarehouseLevel;
    @BindView(R2.id.ll_storage_num_level)
    protected LinearLayout llStorageNumLevel;

	 //库存级
    @BindView(R2.id.btn_warehouse_level)
    protected   RadioButton rbWarehouseLevel;

    //仓位级
    @BindView(R2.id.btn_storage_num_level)
    protected RadioButton rbStorageNumLevel;

    @BindView(R2.id.sp_work)
    protected  Spinner spWork;
    @BindView(R2.id.sp_inv)
    protected  Spinner spInv;
    @BindView(R2.id.sp_storage_num)
    protected Spinner spStorageNum;
    @BindView(R2.id.tv_checker)
    TextView tvChecker;
    @BindView(R2.id.et_check_date)
    protected RichEditText etTransferDate;
    @BindView(R2.id.cb_special_flag)
    CheckBox cbSpecialFlag;
    //增加仓储类型
    @BindView(R2.id.ll_location_type)
    LinearLayout llLocationType;
    @BindView(R2.id.sp_location_type)
    protected Spinner spLocationType;

    /*工厂列表*/
    protected   List<WorkEntity>  mWorkDatas;
    /*库存地点列表以及适配器*/
    InvAdapter mInvAdapter;
    protected List<InvEntity> mInvDatas;
    /*库存号列表*/
    protected  List<String> mStorageNums;
    protected String mSpecialFlag;
    /*仓储类型*/
    protected List<SimpleEntity> mLocationTypes;

    @Override
    protected int getContentId() {
        return R.layout.wzpd_fragment_cn_header;
    }

    @Override
    public void initPresenter() {
        mPresenter = new CNHeadPresenterImp(mActivity);
    }

    @Override
    public void initVariable(Bundle savedInstanceState) {
        super.initVariable(savedInstanceState);
        mInvDatas = new ArrayList<>();
        mWorkDatas = new ArrayList<>();
        mStorageNums = new ArrayList<>();
    }

    @Override
    protected void initView() {
        if(isOpenLocationType) {
            llLocationType.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 注册点击事件
     */
    @Override
    public void initEvent() {
        /*选择盘点日期*/
        etTransferDate.setOnRichEditTouchListener((view, text) ->
                DateChooseHelper.chooseDateForEditText(mActivity, etTransferDate, Global.GLOBAL_DATE_PATTERN_TYPE1));


        /*监听工厂，获取库存地点*/
        RxAdapterView.itemSelections(spWork)
                .filter(position -> position.intValue() > 0)
                .subscribe(position -> mPresenter.getInvsByWorkId(mWorkDatas.get(position).workId, 0));

        /*如果选择仓库级，那么初始化工厂和库存地点*/
        RxView.clicks(rbWarehouseLevel)
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .filter(a -> rbWarehouseLevel.isEnabled())
                .subscribe(a -> {
                    resetUI();
                    mRefData = null;
                    rbWarehouseLevel.setChecked(true);
                    llWarehouseLevel.setVisibility(View.VISIBLE);
                    if (spWork.getAdapter() == null) {
                        mPresenter.getWorks(0);
                    }
                });


        /*如果选择仓位级，那么初始化仓库号*/
        RxView.clicks(rbStorageNumLevel)
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .filter(a -> rbStorageNumLevel.isEnabled())
                .subscribe(a -> {
                    resetUI();
                    mRefData = null;
                    rbStorageNumLevel.setChecked(true);
                    llStorageNumLevel.setVisibility(View.VISIBLE);
                    if (spStorageNum.getAdapter() == null) {
                        mPresenter.getStorageNums(0);
                    }
                });

        RxCompoundButton.checkedChanges(cbSpecialFlag)
                .subscribe(a -> mSpecialFlag = a.booleanValue() ? "Y" : "N");

    }

    /**
     * 默认情况下，选择的是仓库级别，那么系统应该自动初始化工厂。但是如果
     * 子类重写该方法后，修改rbWarehouseLevel的checked属性，应该禁止加载工厂
     */
    @Override
    public void initData() {
        etTransferDate.setText(CommonUtil.getCurrentDate(Global.GLOBAL_DATE_PATTERN_TYPE1));
        tvChecker.setText(Global.LOGIN_ID);
        mSpecialFlag = cbSpecialFlag.isChecked() ? "Y" : "N";
        //因为默认是选择仓库级的，所以在先初始化工厂
        if (rbWarehouseLevel.isChecked() && spWork.getAdapter() == null) {
            //如果工厂还未初始化
            mPresenter.getWorks(0);
        }
    }

    @Override
    public void initDataLazily() {

    }

    /**
     * 重置所有的Button背景
     */
    protected void resetUI() {
        rbStorageNumLevel.setChecked(false);
        rbWarehouseLevel.setChecked(false);
        llWarehouseLevel.setVisibility(View.INVISIBLE);
        llStorageNumLevel.setVisibility(View.INVISIBLE);
    }

    /**
     * 显示工厂
     *
     * @param works
     */
    @Override
    public void showWorks(List<WorkEntity> works) {
        mWorkDatas.clear();
        mWorkDatas.addAll(works);
        WorkAdapter adapter = new WorkAdapter(mActivity, android.R.layout.simple_list_item_1, mWorkDatas);
        spWork.setAdapter(adapter);
    }

    @Override
    public void loadWorksFail(String message) {
        showMessage(message);
        SpinnerAdapter adapter = spWork.getAdapter();
        if (adapter != null && WorkAdapter.class.isInstance(adapter)) {
            mWorkDatas.clear();
            WorkAdapter workAdapter = (WorkAdapter) adapter;
            workAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 显示库存地点
     *
     * @param invs
     */
    @Override
    public void showInvs(List<InvEntity> invs) {
        mInvDatas.clear();
        mInvDatas.addAll(invs);
        if (mInvAdapter == null) {
            mInvAdapter = new InvAdapter(mActivity, android.R.layout.simple_list_item_1,
                    mInvDatas);
            spInv.setAdapter(mInvAdapter);
        } else {
            mInvAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void loadInvsFail(String message) {
        showMessage(message);
        if (mInvAdapter != null) {
            mInvDatas.clear();
            mInvAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void loadInvsComplete() {
        if(isOpenLocationType) {
            //库存地点加载完毕
            mPresenter.getDictionaryData("locationType");
        }
    }

    @Override
    public void showStorageNums(List<String> storageNums) {
        mStorageNums.clear();
        mStorageNums.addAll(storageNums);
        ArrayAdapter<String> adapter = new ArrayAdapter(mActivity, android.R.layout.simple_list_item_1,
                mStorageNums);
        spStorageNum.setAdapter(adapter);
    }

    @Override
    public void loadStorageNumFail(String message) {
        showMessage(message);
        SpinnerAdapter adapter = spWork.getAdapter();
        if (adapter != null && ArrayAdapter.class.isInstance(adapter)) {
            mWorkDatas.clear();
            ArrayAdapter workAdapter = (ArrayAdapter) adapter;
            workAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void loadStorageNumComplete() {
        if(isOpenLocationType) {
            mPresenter.getDictionaryData("locationType");
        }
    }

    /**
     * 用户初始化盘点之前进行必要的检查
     *
     * @return
     */
    @Override
    public boolean checkDataBeforeOperationOnHeader() {
        if (rbWarehouseLevel.isChecked()) {
            //如果仓库级选中，那么检查工厂和库存地点
            if (spWork.getSelectedItemPosition() == 0) {
                showMessage("请检查工厂");
                return false;
            }

            if (spInv.getSelectedItemPosition() == 0) {
                showMessage("请选择库存地点");
                return false;
            }
            return true;
        } else if (rbStorageNumLevel.isChecked()) {
            //如果仓位级选中，那么检查仓库号
            if (spStorageNum.getSelectedItemPosition() == 0) {
                showMessage("请选择仓库号");
                return false;
            }
            return true;
        }
        return true;
    }

    /**
     * 显示开始盘点和重新盘点两个菜单
     *
     * @param companyCode
     */
    @Override
    public void operationOnHeader(String companyCode) {

        View rootView = LayoutInflater.from(mActivity).inflate(R.layout.dialog_bottom_menu, null);
        GridView menu = (GridView) rootView.findViewById(R.id.gd_menus);
        BottomDialogMenuAdapter adapter = new BottomDialogMenuAdapter(mActivity, R.layout.item_dialog_bottom_menu, provideDefaultBottomMenu());
        menu.setAdapter(adapter);

        final Dialog dialog = new Dialog(mActivity, R.style.MaterialDialogSheet);
        dialog.setContentView(rootView);
        dialog.setCancelable(true);
        dialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.show();

        menu.setOnItemClickListener((adapterView, view, position, id) -> {
            switch (position) {
                case 0:
                    //1.开始盘点
                    startCheck();
                    break;
                case 1:
                    //2.重新盘点
                    restartCheck();
                    break;
            }
            dialog.dismiss();
        });

    }

    /**
     * 开始盘点
     */
    protected void startCheck() {
        //请求抬头信息
        mRefData = null;
        Map<String, Object> extraMap = null;
        if(isOpenLocationType) {
            extraMap = new HashMap<>();
            extraMap.put("locationType", mLocationTypes.get(spLocationType.getSelectedItemPosition()).code);
        }
        if (rbStorageNumLevel.isChecked()) {
            //库位级盘点
            if (mStorageNums == null || mStorageNums.size() <= 0) {
                showMessage("仓库号未初始化");
                return;
            }
            mPresenter.getCheckInfo(Global.USER_ID, mBizType, "01",
                    mSpecialFlag, mStorageNums.get(spStorageNum.getSelectedItemPosition()),
                    "", "", getString(etTransferDate), extraMap);
        } else if (rbWarehouseLevel.isChecked()) {
            //库存级
            if (mWorkDatas == null || mWorkDatas.size() == 0) {
                showMessage("工厂未初始化");
                return;
            }
            if (mInvDatas == null || mInvDatas.size() == 0) {
                showMessage("库存地点未初始化");
            }
            mPresenter.getCheckInfo(Global.USER_ID, mBizType, "02",
                    mSpecialFlag, "",
                    mWorkDatas.get(spWork.getSelectedItemPosition()).workId,
                    mInvDatas.get(spInv.getSelectedItemPosition()).invId, getString(etTransferDate), extraMap);
        }
    }

    /**
     * 重新开始盘点。先调用删除整单的接口删除历史的盘点数据，然后调用getCheckInfo获取新的盘点数据
     */
    private void restartCheck() {

        if (mRefData == null) {
            showMessage("请先点击开始盘点");
            return;
        }
        if (TextUtils.isEmpty(mRefData.checkId)) {
            showMessage("请先点击开始盘点");
            return;
        }

        if (rbStorageNumLevel.isChecked()) {
            //仓位级别
            mPresenter.deleteCheckData(mStorageNums.get(spStorageNum.getSelectedItemPosition()),
                    "", "", mRefData.checkId, USER_ID, mBizType);
        } else if (rbWarehouseLevel.isChecked()) {
            mPresenter.deleteCheckData("", mWorkDatas.get(spWork.getSelectedItemPosition()).workId,
                    mInvDatas.get(spInv.getSelectedItemPosition()).invId,
                    mRefData.checkId, USER_ID, mBizType);
        }
    }

    @Override
    public List<BottomMenuEntity> provideDefaultBottomMenu() {
        ArrayList<BottomMenuEntity> menus = new ArrayList<>();
        BottomMenuEntity menu = new BottomMenuEntity();
        menu.menuName = "开始盘点";
        menu.menuImageRes = R.mipmap.icon_start_check;
        menus.add(menu);

        menu = new BottomMenuEntity();
        menu.menuName = "重新盘点";
        menu.menuImageRes = R.mipmap.icon_restart_check;
        menus.add(menu);
        return menus;
    }

    @Override
    public void getCheckInfoSuccess(ReferenceEntity refData) {
        SPrefUtil.saveData(mBizType, "0");
        refData.bizType = mBizType;
        refData.refType = mRefType;
        mRefData = refData;
        showMessage("成功获取到盘点列表");

    }

    @Override
    public void getCheckInfoFail(String message) {
        mRefData = null;
        showMessage(message);
    }

    @Override
    public void loadDictionaryDataSuccess(Map<String, List<SimpleEntity>> data) {
        List<SimpleEntity> locationTypes = data.get("locationType");
        if (locationTypes != null) {
            if (mLocationTypes == null) {
                mLocationTypes = new ArrayList<>();
            }
            mLocationTypes.clear();
            mLocationTypes.addAll(locationTypes);
            SimpleAdapter adapter = new SimpleAdapter(mActivity, R.layout.item_simple_sp, mLocationTypes, false);
            spLocationType.setAdapter(adapter);
        }
    }

    @Override
    public void loadDictionaryDataFail(String message) {
        showMessage(message);
    }

    @Override
    public void bindCommonHeaderUI() {
        if (mRefData != null) {
        }
    }

    @Override
    public void deleteCacheSuccess() {
        showMessage("删除盘点历史成功");
        startCheck();
    }

    @Override
    public void deleteCacheFail(String message) {
        showMessage(message);
    }

    @Override
    public void _onPause() {
        super._onPause();
        //再次检查用户是否输入的额外字段而且必须输入的字段（情景是用户请求单据之前没有输入该字段，回来填上后，但是没有请求单据而是直接）
        //切换了页面
        if (mRefData != null) {
            mRefData.voucherDate = getString(etTransferDate);
            if (rbStorageNumLevel.isChecked()) {
                mRefData.storageNum = mStorageNums.get(spStorageNum.getSelectedItemPosition());
                mRefData.checkLevel = "01";
            } else if (rbWarehouseLevel.isChecked()) {
                final int selectedWorkPosition = spWork.getSelectedItemPosition();
                if (selectedWorkPosition > 0) {
                    mRefData.workId = mWorkDatas.get(selectedWorkPosition).workId;
                    mRefData.workCode = mWorkDatas.get(selectedWorkPosition).workCode;
                }
                final int selectedInvPosition = spInv.getSelectedItemPosition();
                if (selectedInvPosition > 0) {
                    mRefData.invId = mInvDatas.get(selectedInvPosition).invId;
                    mRefData.invCode = mInvDatas.get(selectedInvPosition).invCode;
                }
                mRefData.checkLevel = "02";
            }
            mRefData.specialFlag = mSpecialFlag;
        }
    }

    @Override
    public void clearAllUIAfterSubmitSuccess() {
        super.clearAllUIAfterSubmitSuccess();
    }

    @Override
    public boolean isNeedShowFloatingButton() {
        return true;
    }


    @Override
    public void retry(String action) {
        switch (action) {
            case Global.RETRY_LOAD_REFERENCE_ACTION:
                startCheck();
                break;
            case Global.RETRY_DELETE_TRANSFERED_CACHE_ACTION:
                restartCheck();
                break;
        }
        super.retry(action);
    }
}
