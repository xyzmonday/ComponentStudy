package com.richfit.sdk_cwtz.collect;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxAdapterView;
import com.jakewharton.rxbinding2.widget.RxAutoCompleteTextView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.richfit.common_lib.lib_adapter.SimpleAdapter;
import com.richfit.common_lib.lib_base_sdk.base_collect.BaseCollectFragment;
import com.richfit.common_lib.utils.UiUtil;
import com.richfit.common_lib.widget.RichAutoEditText;
import com.richfit.common_lib.widget.RichEditText;
import com.richfit.data.constant.Global;
import com.richfit.data.helper.CommonUtil;
import com.richfit.domain.bean.InventoryEntity;
import com.richfit.domain.bean.InventoryQueryParam;
import com.richfit.domain.bean.MaterialEntity;
import com.richfit.domain.bean.ResultEntity;
import com.richfit.domain.bean.SimpleEntity;
import com.richfit.sdk_cwtz.R;
import com.richfit.sdk_cwtz.R2;
import com.richfit.sdk_cwtz.adapter.SpecialInvAdapter;
import com.richfit.sdk_cwtz.collect.imp.LACollectPresenterImp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;


/**
 * 注意在获取库存的时候，如果启用了WM那么使用04从SAP获取有效库存，
 * 如果没有启用WM，那么使用03获取有效库存。
 * 仓位调整的整体步骤：
 * 1.获取物料信息，该接口与无参考获取物料信息一致；
 * 2.输入源仓位，获取该仓位的库存信息(注意这里与出库移库不同的是，仓位调整获取的是该仓位下的库存)；
 * 3.选择某一条源仓位库存，带出该库存；
 * 4.输入目标仓位
 * Created by monday on 2017/2/7.
 */

public class LACollectFragment extends BaseCollectFragment<LACollectPresenterImp>
        implements ILACollectView {

    @BindView(R2.id.et_material_num)
    protected RichEditText etMaterialNum;
    @BindView(R2.id.tv_material_desc)
    TextView tvMaterialDesc;
    @BindView(R2.id.tv_material_group)
    TextView tvMaterialGroup;
    @BindView(R2.id.tv_material_unit)
    TextView tvMaterialUnit;
    @BindView(R2.id.et_batch_flag)
    protected EditText etBatchFlag;
    @BindView(R2.id.et_send_location)
    protected RichAutoEditText etSendLocation;
    @BindView(R2.id.tv_send_inv_quantity)
    protected TextView tvSendInvQuantity;
    @BindView(R2.id.et_rec_location)
    protected AppCompatAutoCompleteTextView etRecLocation;
    @BindView(R2.id.et_adjust_quantity)
    protected EditText etRecQuantity;
    //增加特殊库存
    @BindView(R2.id.sp_special_inv)
    protected Spinner spSpecialInv;
    //增加源仓位仓储类型
    @BindView(R2.id.ll_location_type)
    LinearLayout llLocationType;
    //增加目标仓位仓储类型
    @BindView(R2.id.ll_rec_location_type)
    LinearLayout llRecLocationType;
    @BindView(R2.id.sp_location_type)
    Spinner spLocationType;
    @BindView(R2.id.sp_rec_location_type)
    Spinner spRecLocationType;

    /*仓储类型*/
    protected List<SimpleEntity> mLocationTypes;
    protected List<SimpleEntity> mRecLocationTypes;
    protected SpecialInvAdapter mAdapter;
    protected List<InventoryEntity> mInventoryDatas;
    private MaterialEntity mHistoryData;
    /*发出仓位列表适配器*/
    protected List<String> mSendLocationList;
    protected List<String> mRecLocationList;

    @Override
    public void handleBarCodeScanResult(String type, String[] list) {
        super.handleBarCodeScanResult(type, list);
        if (list != null && list.length > 2) {
            final String materialNum = list[Global.MATERIAL_POS];
            final String batchFlag = list[Global.BATCHFALG_POS];
            loadMaterialInfo(materialNum, batchFlag);
        } else if (list != null && list.length == 1) {
            final String location = list[Global.LOCATION_POS];
            //目标仓位
            if (etRecLocation.hasFocus() && etRecLocation.isFocused()) {
                clearCommonUI(etRecLocation);
                etRecLocation.setText(location);
            } else {
                //源仓位
                clearCommonUI(etSendLocation);
                etSendLocation.setText(location);
                //获取库存
                loadInventoryInfo(location);
            }
        } else if (list != null && list.length == 2 && isOpenLocationType) {
            String location = list[Global.LOCATION_POS];
            String locationType = list[Global.LOCATION_TYPE_POS];
            //目标仓位
            if (etRecLocation.hasFocus() && etRecLocation.isFocused()) {
                clearCommonUI(etRecLocation);
                etRecLocation.setText(location);
            } else {
                //源仓位
                clearCommonUI(etSendLocation);
                etSendLocation.setText(location);
                //自动选择仓储类型
                UiUtil.setSelectionForSimpleSp(mLocationTypes, locationType, spLocationType);
                //加载库存
                loadInventoryInfo(location);
            }
        }
    }

    @Override
    protected void initVariable(Bundle savedInstanceState) {
        super.initVariable(savedInstanceState);
        mSendLocationList = new ArrayList<>();
        mRecLocationList = new ArrayList<>();
    }

    @Override
    protected int getContentId() {
        return R.layout.cwtz_fragment_la_collect;
    }

    @Override
    protected void initPresenter() {
        mPresenter = new LACollectPresenterImp(mActivity);
    }

    @Override
    protected void initDataLazily() {
        etMaterialNum.setEnabled(false);
        if (mRefData == null) {
            showMessage("在先在抬头界面选择相关的信息");
            return;
        }

        if (TextUtils.isEmpty(mRefData.workCode)) {
            showMessage("请现在抬头界面选择工厂");
            return;
        }

        if (TextUtils.isEmpty(mRefData.invCode)) {
            showMessage("请先在抬头界面选择库存地点");
            return;
        }
        if ("Y".equalsIgnoreCase(Global.WMFLAG) && TextUtils.isEmpty(mRefData.storageNum)) {
            showMessage("未获取到仓位号,请重新在太头界面合适的工厂和库存地点");
            return;
        }
        etMaterialNum.setEnabled(true);
        isOpenBatchManager = true;
        etBatchFlag.setEnabled(true);
    }

    @Override
    protected void initEvent() {

        //获取物料
        etMaterialNum.setOnRichEditTouchListener((view, materialNum) -> loadMaterialInfo(materialNum, getString(etBatchFlag)));

        //监听物料恢复批次状态
        RxTextView.textChanges(etMaterialNum)
                .filter(str -> !TextUtils.isEmpty(str))
                .subscribe(e -> {
                    isOpenBatchManager = true;
                    etBatchFlag.setEnabled(true);
                });

        //获取仓位的库存
        etSendLocation.setOnRichAutoEditTouchListener((view, location) -> {
            hideKeyboard(view);
            loadInventoryInfo(location);
        });

        //选择下拉，带出该库存
        RxAdapterView.itemSelections(spSpecialInv)
                .filter(pos -> pos > 0)
                .subscribe(pos -> tvSendInvQuantity.setText(mInventoryDatas.get(pos).invQuantity));

        //增加仓储类型的选择获取提示库存
        RxAdapterView.itemSelections(spLocationType)
                .filter(a -> isOpenLocationType && spLocationType.getAdapter() != null && mLocationTypes != null
                        && mLocationTypes.size() > 0)
                .subscribe(position -> loadTipInventory());

        //提示库存处理
        //点击自动提示控件，显示默认列表
        RxView.clicks(etSendLocation)
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .filter(a -> mSendLocationList != null && mSendLocationList.size() > 0)
                .subscribe(a -> showAutoCompleteConfig(etSendLocation));

        RxView.clicks(etRecLocation)
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .filter(a -> mRecLocationList != null && mRecLocationList.size() > 0)
                .subscribe(a -> showAutoCompleteConfig(etRecLocation));

        //选择发出仓位获取该仓位上的库存
        RxAutoCompleteTextView.itemClickEvents(etSendLocation)
                .subscribe(a -> {
                    hideKeyboard(etSendLocation);
                    loadInventoryInfo(getString(etSendLocation));
                });

        RxAutoCompleteTextView.itemClickEvents(etRecLocation)
                .subscribe(a -> hideKeyboard(etRecLocation));
    }

    @Override
    protected void initView() {
        llLocationType.setVisibility(isOpenLocationType ? View.VISIBLE : View.GONE);
        llRecLocationType.setVisibility(isOpenRecLocationType ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void initData() {

    }

    /**
     * 获取物料信息入口
     *
     * @param materialNum
     * @param batchFlag
     */
    protected void loadMaterialInfo(String materialNum, String batchFlag) {
        if (TextUtils.isEmpty(materialNum)) {
            showMessage("物料编码为空,请重新输入");
            return;
        }
        clearAllUI();
        etMaterialNum.setText(materialNum);
        etBatchFlag.setText(batchFlag);
        mHistoryData = null;
        isOpenBatchManager = true;
        etBatchFlag.setEnabled(true);
        mPresenter.getMaterialInfo("01", materialNum, mRefData.workId);
    }

    @Override
    public void getMaterialInfoSuccess(MaterialEntity data) {
        etMaterialNum.setTag(data.id);
        tvMaterialDesc.setText(data.materialDesc);
        tvMaterialGroup.setText(data.materialGroup);
        tvMaterialUnit.setText(data.unit);
        manageBatchFlagStatus(etBatchFlag, data.batchManagerStatus);
        if (isOpenBatchManager && TextUtils.isEmpty(getString(etBatchFlag))) {
            etBatchFlag.setText(data.batchFlag);
        }
        mHistoryData = data;
    }

    @Override
    public void getMaterialInfoComplete() {
        if (isOpenLocationType) {
            mPresenter.getDictionaryData("locationType");
        } else {
            //获取提示库存
            loadTipInventory();
        }
    }

    @Override
    public void getMaterialInfoFail(String message) {
        showMessage(message);
    }

    //获取提示库存
    protected void loadTipInventory() {
        if (mSendLocationList != null) {
            mSendLocationList.clear();
        }

        if (mRecLocationList != null) {
            mRecLocationList.clear();
        }


        if (TextUtils.isEmpty(mRefData.workId)) {
            showMessage("请先在抬头界面选择工厂");
            return;
        }

        if (TextUtils.isEmpty(mRefData.invId)) {
            showMessage("请现在抬头界面选择库存地点");
        }

        Object tag = etMaterialNum.getTag();
        if (tag == null || TextUtils.isEmpty(tag.toString())) {
            showMessage("请先获取物料信息");
            return;
        }

        //清空库存，使得用户必须获取新仓储类型下的库存
        if (mInventoryDatas != null && mAdapter != null) {
            mInventoryDatas.clear();
            mAdapter.notifyDataSetChanged();
            tvSendInvQuantity.setText("");
        }

        InventoryQueryParam param = provideInventoryQueryParam();
        mPresenter.getTipInventoryInfo(param.queryType, mRefData.workId, mRefData.invId, mRefData.workCode,
                mRefData.invCode, mRefData.storageNum, getString(etMaterialNum), tag.toString(),
                "", "", getString(etBatchFlag), "", "", "", param.invType, param.extraMap);
    }

    /**
     * 获取库存信息
     *
     * @param location
     */
    protected void loadInventoryInfo(String location) {
        if (mAdapter != null) {
            mInventoryDatas.clear();
            mAdapter.notifyDataSetChanged();
        }
        tvSendInvQuantity.setText("");

        Object tag = etMaterialNum.getTag();
        if (tag == null || TextUtils.isEmpty(tag.toString())) {
            showMessage("请先获取物料信息");
            return;
        }

        if (TextUtils.isEmpty(location)) {
            showMessage("先输入目标仓位");
            return;
        }
        InventoryQueryParam param = provideInventoryQueryParam();
        mPresenter.getInventoryInfo(param.queryType, mRefData.workId, mRefData.invId, mRefData.workCode,
                mRefData.invCode, mRefData.storageNum, getString(etMaterialNum), tag.toString(),
                "", "", getString(etBatchFlag), location, "", "", param.invType, param.extraMap);
    }

    @Override
    public void showInventory(List<InventoryEntity> invs) {
        if (mInventoryDatas == null) {
            mInventoryDatas = new ArrayList<>();
        }
        mInventoryDatas.clear();
        InventoryEntity tmp = new InventoryEntity();
        tmp.location = "请选择";
        mInventoryDatas.add(tmp);
        mInventoryDatas.addAll(invs);
        //初始化特殊库存标识下拉列表
        if (mAdapter == null) {
            mAdapter = new SpecialInvAdapter(mActivity, R.layout.item_simple_sp, mInventoryDatas);
            spSpecialInv.setAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void loadInventoryFail(String message) {
        showMessage(message);
    }

    @Override
    public void getDeviceInfoSuccess(ResultEntity result) {

    }

    @Override
    public void getDeviceInfoFail(String message) {
        showMessage(message);
    }

    @Override
    public void getDeviceInfoComplete() {

    }

    //加载提示库存成功
    @Override
    public void showTipInventory(List<String> list) {
        mSendLocationList.addAll(list);
        ArrayAdapter<String> sendLocationAdapter = new ArrayAdapter<>(mActivity,
                android.R.layout.simple_dropdown_item_1line, mSendLocationList);
        etSendLocation.setAdapter(sendLocationAdapter);
        setAutoCompleteConfig(etSendLocation);

        //目标仓位
        mRecLocationList.addAll(list);
        ArrayAdapter<String> recLocationAdapter = new ArrayAdapter<>(mActivity,
                android.R.layout.simple_dropdown_item_1line, mRecLocationList);
        etRecLocation.setAdapter(recLocationAdapter);
        setAutoCompleteConfig(etRecLocation);

    }

    @Override
    public void loadITipInventoryComplete() {

    }

    @Override
    public void loadTipInventoryFail(String message) {
        showMessage(message);
        etSendLocation.setAdapter(null);
        etRecLocation.setAdapter(null);
    }

    @Override
    public void loadDictionaryDataFail(String message) {
        showMessage(message);
    }

    @Override
    public void showOperationMenuOnCollection(final String companyCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle("提示");
        builder.setMessage("您真的需要保存数据吗?点击确定将保存数据.");
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        builder.setPositiveButton("确定", (dialog, which) -> {
            dialog.dismiss();
            saveCollectedData();
        });
        builder.show();
    }

    protected boolean refreshQuantity(final String quantity) {
        if (Float.valueOf(quantity) <= 0.0f) {
            showMessage("输入数量不合理");
            return false;
        }
        //发出仓位的库存
        final float sendInvQuantityV = CommonUtil.convertToFloat(getString(tvSendInvQuantity), 0.0f);
        final float quantityV = CommonUtil.convertToFloat(quantity, 0.0f);
        if (Float.compare(quantityV, sendInvQuantityV) > 0.0f) {
            showMessage("输入数量有误，请重新输入");
            return false;
        }
        return true;
    }

    @Override
    public boolean checkCollectedDataBeforeSave() {

        if (!etMaterialNum.isEnabled()) {
            showMessage("请先获取物料信息");
            return false;
        }

        if (TextUtils.isEmpty(mRefData.bizType)) {
            showMessage("业务类型为空");
            return false;
        }

        if (TextUtils.isEmpty(mRefData.workId)) {
            showMessage("请先在抬头界面选择工厂");
            return false;
        }

        if (TextUtils.isEmpty(mRefData.invId)) {
            showMessage("请先在抬头界面选择库存地点");
            return false;
        }

        if (spSpecialInv.getSelectedItemPosition() <= 0) {
            showMessage("请先选择特殊库存标识");
            return false;
        }

        Object tag = etMaterialNum.getTag();
        if (tag == null || TextUtils.isEmpty(tag.toString())) {
            showMessage("请先获取物料信息");
            return false;
        }

        final String location = getString(etSendLocation);
        if (TextUtils.isEmpty(location) || location.length() > 10) {
            showMessage("源仓位不合理");
            return false;
        }

        final String recLocation = getString(etRecLocation);
        if (TextUtils.isEmpty(recLocation) || recLocation.length() > 10) {
            showMessage("目标仓位不合理");
            return false;
        }

        if (TextUtils.isEmpty(getString(tvSendInvQuantity))) {
            showMessage("请先获取有效库存");
            return false;
        }

        //必须先判断调整数据是否输入
        if (TextUtils.isEmpty(getString(etRecQuantity))) {
            showMessage("调整数量有误");
            return false;
        }

        if (!refreshQuantity(getString(etRecQuantity))) {
            showMessage("调整数量有误");
            return false;
        }
        return true;
    }

    @Override
    public ResultEntity provideResult() {
        ResultEntity result = new ResultEntity();
        result.businessType = mRefData.bizType;
        int position = spSpecialInv.getSelectedItemPosition();
        //如果没有打开，那么返回服务给出的默认批次
        result.batchFlag = !isOpenBatchManager ? CommonUtil.toUpperCase(mInventoryDatas.get(position).batchFlag)
                : CommonUtil.toUpperCase(getString(etBatchFlag));
        result.workId = mRefData.workId;
        result.invId = mRefData.invId;
        result.materialId = CommonUtil.Obj2String(etMaterialNum.getTag());
        result.location = CommonUtil.toUpperCase(getString(etSendLocation));
        result.recLocation = CommonUtil.toUpperCase(getString(etRecLocation));
        result.quantity = getString(etRecQuantity);
        result.userId = Global.USER_ID;
        result.invType = mInventoryDatas.get(position).invType;
        result.invFlag = mInventoryDatas.get(position).invFlag;
        result.specialInvFlag = mInventoryDatas.get(position).specialInvFlag;
        result.specialInvNum = mInventoryDatas.get(position).specialInvNum;
        if (isOpenLocationType) {
            result.locationType = mLocationTypes.get(spLocationType.getSelectedItemPosition()).code;
        }
        if (isOpenRecLocationType) {
            result.recLocationType = mRecLocationTypes.get(spRecLocationType.getSelectedItemPosition()).code;
        }
        InventoryQueryParam queryParam = provideInventoryQueryParam();
        result.queryType = queryParam.queryType;
        return result;
    }

    @Override
    public void saveCollectedDataSuccess(String message) {
        showSuccessDialog(message);
        clearAllUI();
        clearCommonUI(etMaterialNum, etBatchFlag);
    }

    @Override
    public void saveCollectedDataFail(String message) {
        showErrorDialog(message);
    }

    protected void clearAllUI() {
        clearCommonUI(tvMaterialDesc, tvMaterialGroup, tvMaterialUnit, etSendLocation,
                tvSendInvQuantity, etRecLocation, etRecQuantity);
        if (mAdapter != null) {
            mInventoryDatas.clear();
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void retry(String action) {
        switch (action) {
            case Global.RETRY_QUERY_MATERIAL_INFO:
                loadMaterialInfo(getString(etMaterialNum), getString(etBatchFlag));
                break;
            case Global.RETRY_LOAD_INVENTORY_ACTION:
                loadInventoryInfo(getString(etRecLocation));
                break;
        }
        super.retry(action);
    }

    @Override
    public void loadDictionaryDataSuccess(Map<String, List<SimpleEntity>> data) {
        List<SimpleEntity> locationTypes = data.get("locationType");
        if (locationTypes != null) {
            if (mLocationTypes == null) {
                mLocationTypes = new ArrayList<>();
            }
            if (mRecLocationTypes == null) {
                mRecLocationTypes = new ArrayList<>();
            }
            mLocationTypes.clear();
            mRecLocationTypes.clear();
            mLocationTypes.addAll(locationTypes);
            mRecLocationTypes.addAll(locationTypes);
            //发出仓储类型
            SimpleAdapter adapter = new SimpleAdapter(mActivity, R.layout.item_simple_sp,
                    mLocationTypes, false);
            spLocationType.setAdapter(adapter);

            //接收仓储类型
            SimpleAdapter recAdapter = new SimpleAdapter(mActivity, R.layout.item_simple_sp,
                    mRecLocationTypes, false);
            spRecLocationType.setAdapter(recAdapter);
        }
    }


    @Override
    public void _onPause() {
        super._onPause();
        clearAllUI();
        clearCommonUI(etMaterialNum, etBatchFlag);
    }

    @Override
    protected InventoryQueryParam provideInventoryQueryParam() {
        InventoryQueryParam queryParam = super.provideInventoryQueryParam();
        if (mLocationTypes != null && isOpenLocationType) {
            queryParam.extraMap = new HashMap<>();
            String locationType = mLocationTypes.get(spLocationType.getSelectedItemPosition()).code;
            queryParam.extraMap.put("locationType", locationType);
        }
        if (mRecLocationTypes != null && isOpenRecLocationType) {
            if (queryParam.extraMap == null)
                queryParam.extraMap = new HashMap<>();
            String recLocationType = mRecLocationTypes.get(spRecLocationType.getSelectedItemPosition()).code;
            queryParam.extraMap.put("recLocationType", recLocationType);
        }
        return queryParam;
    }
}
