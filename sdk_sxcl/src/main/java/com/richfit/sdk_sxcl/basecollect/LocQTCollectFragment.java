package com.richfit.sdk_sxcl.basecollect;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxAdapterView;
import com.jakewharton.rxbinding2.widget.RxAutoCompleteTextView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.richfit.common_lib.lib_adapter.LocationAdapter;
import com.richfit.common_lib.lib_mvp.BaseFragment;
import com.richfit.common_lib.utils.L;
import com.richfit.common_lib.utils.SPrefUtil;
import com.richfit.common_lib.widget.RichAutoEditText;
import com.richfit.common_lib.widget.RichEditText;
import com.richfit.data.constant.Global;
import com.richfit.data.helper.CommonUtil;
import com.richfit.data.helper.TransformerHelper;
import com.richfit.domain.bean.InventoryEntity;
import com.richfit.domain.bean.LocationInfoEntity;
import com.richfit.domain.bean.RefDetailEntity;
import com.richfit.domain.bean.ResultEntity;
import com.richfit.sdk_sxcl.R;
import com.richfit.sdk_sxcl.R2;
import com.richfit.sdk_sxcl.basecollect.imp.LocQTCollectPresenterImp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;

/**
 * 这里需要根据是否是上架还是下架处理进行具体的业务分发。
 * 相同的地方在于获取物料明细(也就是确定当前操作的明细行)
 * Created by monday on 2017/5/26.
 */

public class LocQTCollectFragment extends BaseFragment<LocQTCollectPresenterImp>
        implements ILocQTCollectView {


    @BindView(R2.id.sp_ref_line_num)
    Spinner spRefLine;
    @BindView(R2.id.et_material_num)
    RichEditText etMaterialNum;
    @BindView(R2.id.tv_material_desc)
    TextView tvMaterialDesc;
    @BindView(R2.id.tv_special_inv_flag)
    TextView tvSpecialInvFlag;
    @BindView(R2.id.tv_work_name)
    TextView tvWorkName;
    @BindView(R2.id.tv_work)
    TextView tvWork;
    //注意库存地点是显示
    @BindView(R2.id.tv_inv)
    TextView tvInv;
    @BindView(R2.id.tv_act_quantity)
    TextView tvActQuantity;
    @BindView(R2.id.tv_inv_type)
    TextView tvInvType;
    @BindView(R2.id.tv_batch_flag)
    TextView tvBatchFlag;
    //下架仓位
    @BindView(R2.id.sp_x_loc)
    Spinner spXLoc;
    //库存数量
    @BindView(R2.id.tv_inv_quantity)
    TextView tvInvQuantity;
    //上架仓位
    @BindView(R2.id.et_s_location)
    RichAutoEditText etSLocation;
    //仓位数量
    @BindView(R2.id.tv_location_quantity)
    TextView tvLocQuantity;
    @BindView(R2.id.et_quantity)
    EditText etQuantity;
    @BindView(R2.id.cb_single)
    CheckBox cbSingle;
    @BindView(R2.id.tv_total_quantity)
    TextView tvTotalQuantity;

    /*当前匹配的行明细（行号）*/
    protected ArrayList<String> mRefLines;
    /*单据行适配器*/
    ArrayAdapter<String> mRefLineAdapter;
    /*库存信息*/
    private List<InventoryEntity> mInventoryDatas;
    private LocationAdapter mXLocAdapter;
    /*当前选中的单据行*/
    protected String mSelectedRefLineNum;
    /*校验仓位是否存在，如果false表示校验该仓位不存在或者没有校验该仓位，不允许保存数据*/
    protected boolean isLocationChecked = false;
    /*批次一致性检查*/
    protected boolean isBatchValidate = true;
    /*上架仓位列表适配器*/
    ArrayAdapter<String> mSLocationAdapter;
    List<String> mSLocationList;
    /*上下架处理标识,S标识上架,H表示下架*/
    private String mSHFlag;

    @Override
    public void handleBarCodeScanResult(String type, String[] list) {
        if (list != null && list.length > 12) {
            if (!etMaterialNum.isEnabled()) {
                showMessage("请先在抬头界面获取相关数据");
                return;
            }
            final String materialNum = list[Global.MATERIAL_POS];
            final String batchFlag = list[Global.BATCHFALG_POS];
            if (cbSingle.isChecked() && materialNum.equalsIgnoreCase(getString(etMaterialNum))) {
                if ("S".equalsIgnoreCase(mSHFlag)) {
                    getTransferSingle(batchFlag, getString(etSLocation));
                } else {
                    getTransferSingle(spXLoc.getSelectedItemPosition());
                }
            } else {
                //在非单品模式下，扫描不同的物料。注意这里必须用新的物料和批次更新UI
                etMaterialNum.setText(materialNum);
                tvBatchFlag.setText(batchFlag);
                loadMaterialInfo(materialNum, batchFlag);
            }
        }
    }

    @Override
    protected int getContentId() {
        return R.layout.sxcl_fragment_locqt_collect;
    }

    @Override
    public void initPresenter() {
        mPresenter = new LocQTCollectPresenterImp(mActivity);
    }


    @Override
    protected void initVariable(@Nullable Bundle savedInstanceState) {
        mRefLines = new ArrayList<>();
        mInventoryDatas = new ArrayList<>();
        mSLocationList = new ArrayList<>();
    }

    @Override
    protected void initView() {

    }

    /**
     * 绑定公共事件，子类自己根据是否上架，是否需要检查上架是否存在
     * 重写上架仓位监听
     */
    @Override
    public void initEvent() {
        //扫描后者手动输入物资条码
        etMaterialNum.setOnRichEditTouchListener((view, materialNum) -> {
            hideKeyboard(etMaterialNum);
            //手动输入没有批次
            loadMaterialInfo(materialNum, getString(tvBatchFlag));
        });

        //选择单据行
        RxAdapterView
                .itemSelections(spRefLine)
                .filter(position -> position > 0)
                .subscribe(position -> bindCommonCollectUI());

        //单品(注意单品仅仅控制实收数量，累计数量是由行信息里面控制)
        cbSingle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            etQuantity.setText(isChecked ? "1" : "");
            etQuantity.setEnabled(!isChecked);
        });

        //下架仓位,选择下架仓位刷新库存数量，并且获取缓存
        RxAdapterView
                .itemSelections(spXLoc)
                .filter(position -> (mInventoryDatas != null && mInventoryDatas.size() > 0 &&
                        position.intValue() <= mInventoryDatas.size() - 1))
                .subscribe(position -> {
                    RefDetailEntity data = getLineData(mSelectedRefLineNum);
                    InventoryEntity invData = mInventoryDatas.get(position);
                    String invQuantity = calQuantityByUnitRate(invData.invQuantity, data.recordUnit, data.unitRate);
                    invData.invQuantity = invQuantity;
                    tvInvQuantity.setText(invData.invQuantity);
                    getTransferSingle(position);
                });

        //点击上架仓位获取缓存
        etSLocation.setOnRichAutoEditTouchListener((view, location) -> getTransferSingle(getString(tvBatchFlag), location));

        //监听输入的关键字
        RxTextView.textChanges(etSLocation)
                .debounce(100, TimeUnit.MILLISECONDS)
                .filter(str -> !TextUtils.isEmpty(str) && mSLocationList != null &&
                        mSLocationList.size() > 0 && !filterKeyWord(str))
                .subscribe(a -> loadLocationList(getString(etSLocation), true));

        //选中上架仓位列表的item，关闭输入法,并且直接匹配出仓位数量
        RxAutoCompleteTextView.itemClickEvents(etSLocation)
                .subscribe(a -> {
                    hideKeyboard(etSLocation);
                    getTransferSingle(getString(tvBatchFlag), getString(etSLocation));
                });

        //点击自动提示控件，显示默认列表
        RxView.clicks(etSLocation)
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .filter(a -> mSLocationList != null && mSLocationList.size() > 0)
                .subscribe(a -> showAutoCompleteConfig(etSLocation));
    }

    @Override
    public void initData() {

    }


    /**
     * 检查抬头界面的必要的字段是否已经赋值
     */
    @Override
    public void initDataLazily() {
        etMaterialNum.setEnabled(false);
        if (mRefData == null) {
            showMessage("请先在抬头界面获取单据数据");
            return;
        }
        if (TextUtils.isEmpty(mRefData.bizType)) {
            showMessage("未获取到业务类型");
            return;
        }

        if (TextUtils.isEmpty(mRefData.refType)) {
            showMessage("请先在抬头界面获取单据数据");
            return;
        }

        String transferKey = (String) SPrefUtil.getData(mBizType + mRefType, "0");
        if ("1".equals(transferKey)) {
            showMessage("本次采集已经过账,请先到数据明细界面进行数据上传操作");
            return;
        }
        etMaterialNum.setEnabled(true);

    }

    /**
     * 输入或者扫描物料条码后系统自动去匹配单据行明细，并且初始化默认选择的明细数据.
     * 由于在初始化单据行下拉列表的同时也需要出发页面刷星，所以页面刷新统一延迟到选择单据
     * 行列表的item之后。
     * 具体流程loadMaterialInfo->setupRefLineAdapter->bindCommonCollectUI
     */
    @Override
    public void loadMaterialInfo(@NonNull String materialNum, @NonNull String batchFlag) {
        if (!etMaterialNum.isEnabled()) {
            return;
        }
        if (TextUtils.isEmpty(materialNum)) {
            showMessage("请输入物资条码");
            return;
        }
        clearAllUI();
        //这里先将上下架表示清空
        mSHFlag = "";
        //刷新界面(在单据行明细查询是否有该物料条码，如果有那么刷新界面)
        matchMaterialInfo(materialNum, batchFlag)
                .compose(TransformerHelper.io2main())
                .subscribe(details -> setupRefLineAdapter(details), e -> showMessage(e.getMessage()));
    }


    /**
     * 设置单据行
     *
     * @param refLines
     */
    @Override
    public void setupRefLineAdapter(ArrayList<String> refLines) {
        mRefLines.clear();
        mRefLines.add(getString(R.string.default_choose_item));
        if (refLines != null)
            mRefLines.addAll(refLines);

        //如果未查询到提示用户
        if (mRefLines.size() == 1) {
            showMessage("该单据中未查询到该物料,请检查物资编码或者批次是否正确");
            spRefLine.setSelection(0);
            return;
        }

        //初始化单据行适配器
        if (mRefLineAdapter == null) {
            mRefLineAdapter = new ArrayAdapter<>(mActivity, R.layout.item_simple_sp, mRefLines);
            spRefLine.setAdapter(mRefLineAdapter);

        } else {
            mRefLineAdapter.notifyDataSetChanged();
        }
        //如果多行设置颜色
        spRefLine.setBackgroundColor(ContextCompat.getColor(mActivity, mRefLines.size() >= 3 ?
                R.color.colorPrimary : R.color.white));
        //默认选择第一个
        spRefLine.setSelection(1);
    }

    /**
     * 绑定UI。
     */
    @Override
    public void bindCommonCollectUI() {
        mSelectedRefLineNum = mRefLines.get(spRefLine.getSelectedItemPosition());
        RefDetailEntity lineData = getLineData(mSelectedRefLineNum);
        etQuantity.setText("");
        //物资描述
        tvMaterialDesc.setText(lineData.materialDesc);
        //特殊库存标识
        tvSpecialInvFlag.setText(lineData.specialInvFlag);
        //工厂
        tvWork.setText(lineData.workName);
        //库存地点
        tvInv.setText(lineData.invName);
        //库存类型
        tvInvType.setText(lineData.invType);
        //允许进行上下架的数量(应收数量)
        tvActQuantity.setText(lineData.actQuantity);
        //批次处理
        tvBatchFlag.setText(lineData.batchFlag);
        mSHFlag = lineData.shkzg;
        if ("S".equalsIgnoreCase(mSHFlag) && "H".equalsIgnoreCase(mSHFlag)) {
            showMessage("未获取到上下架标识");
            return;
        }
        //这里需要根据具体是上架还是下架进行接下来的业务分发，如果是上架那么不做处理了
        if ("S".equalsIgnoreCase(mSHFlag)) {
            //如果是上架，那么获取上架仓位参考列表
            tvLocQuantity.setText("");
            tvTotalQuantity.setText("");
        } else {
            //如果是下架
            loadInventory();
        }
    }

    /**
     * 获取上架仓位参考列表
     *
     * @param keyWord
     * @param isDropDown
     */
    @Override
    public void loadLocationList(String keyWord, boolean isDropDown) {
        tvLocQuantity.setText("");
        tvTotalQuantity.setText("");
        RefDetailEntity lineData = getLineData(mSelectedRefLineNum);
        mPresenter.getLocationList(lineData.workId, lineData.workCode, lineData.invId,
                lineData.invCode, keyWord, 100, 0, isDropDown);
    }

    /**
     * 如果用户输入的关键字在mLocationList存在，那么不在进行数据查询.
     *
     * @param keyWord
     * @return
     */
    private boolean filterKeyWord(CharSequence keyWord) {
        Pattern pattern = Pattern.compile("^" + keyWord.toString().toUpperCase());
        for (String item : mSLocationList) {
            Matcher matcher = pattern.matcher(item);
            while (matcher.find()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void getLocationListFail(String message) {
        showMessage(message);
    }

    @Override
    public void getLocationListSuccess(List<String> list, boolean isDropDown) {
        mSLocationList.clear();
        mSLocationList.addAll(list);
        if (mSLocationAdapter == null) {
            mSLocationAdapter = new ArrayAdapter<>(mActivity,
                    android.R.layout.simple_dropdown_item_1line, mSLocationList);
            etSLocation.setAdapter(mSLocationAdapter);
            setAutoCompleteConfig(etSLocation);
        } else {
            mSLocationAdapter.notifyDataSetChanged();
        }
        if (isDropDown) {
            showAutoCompleteConfig(etSLocation);
        }
    }

    /**
     * 下架处理时加载库存
     */
    private void loadInventory() {
        tvInvQuantity.setText("");
        tvLocQuantity.setText("");
        tvTotalQuantity.setText("");
        if (mXLocAdapter != null) {
            mInventoryDatas.clear();
            mXLocAdapter.notifyDataSetChanged();
        }
        if (isOpenBatchManager && TextUtils.isEmpty(getString(tvBatchFlag))) {
            showMessage("请输入批次");
            return;
        }
        final RefDetailEntity lineData = getLineData(mSelectedRefLineNum);

        //需要确定库存类型
        mPresenter.getInventoryInfo("04", lineData.workId,
                lineData.invId, lineData.workCode, lineData.invCode, "", getString(etMaterialNum),
                lineData.materialId, "", getString(tvBatchFlag), "", "", lineData.invType, "");
    }

    /**
     * 加载库存成功
     *
     * @param list
     */
    @Override
    public void showInventory(List<InventoryEntity> list) {
        mInventoryDatas.clear();
        InventoryEntity tmp = new InventoryEntity();
        tmp.locationCombine = "请选择";
        mInventoryDatas.add(tmp);
        mInventoryDatas.addAll(list);
        if (mXLocAdapter == null) {
            mXLocAdapter = new LocationAdapter(mActivity, R.layout.item_simple_sp, mInventoryDatas);
            spXLoc.setAdapter(mXLocAdapter);
        } else {
            mXLocAdapter.notifyDataSetChanged();
        }
        spXLoc.setSelection(0);
    }

    /**
     * 加载库存失败
     *
     * @param message
     */
    @Override
    public void loadInventoryFail(String message) {
        showMessage(message);
    }


    /**
     * 上架处理时获取单条缓存
     **/
    private void getTransferSingle(String batchFlag, String location) {

        if (spRefLine.getSelectedItemPosition() <= 0) {
            showMessage("请先选择单据行");
            return;
        }
        final RefDetailEntity lineData = getLineData(mSelectedRefLineNum);

        //检查库存地点
        if (TextUtils.isEmpty(lineData.invId)) {
            showMessage("未获取到库存点");
            return;
        }

        //批次处理。打开了批次管理而且必须输入，那么检查是否输入了批次
        if (isOpenBatchManager && tvBatchFlag.isEnabled())
            if (TextUtils.isEmpty(batchFlag)) {
                showMessage("请先输入批次");
                return;
            }
        if (TextUtils.isEmpty(location)) {
            showMessage("请先输入上架仓位");
            return;
        }
        isBatchValidate = false;
        //这里不考虑是否上架
        mPresenter.checkLocation("04", lineData.workId, lineData.invId, batchFlag, location);
    }

    @Override
    public void checkLocationFail(String message) {
        showMessage(message);
        isLocationChecked = false;
    }

    @Override
    public void checkLocationSuccess(String batchFlag, String location) {
        isLocationChecked = true;
        final RefDetailEntity lineData = getLineData(mSelectedRefLineNum);
        final String refCodeId = mRefData.refCodeId;
        final String refLineId = lineData.refLineId;
        final String refType = mRefData.refType;
        final String bizType = mRefData.bizType;
        mPresenter.getTransferInfoSingle(refCodeId, refType, bizType, refLineId,
                getString(etMaterialNum), batchFlag, location, lineData.refDoc, CommonUtil.convertToInt(lineData.refDocItem), Global.USER_ID);
    }

    /**
     * 下架处理时获取单条缓存
     */
    private void getTransferSingle(int position) {
        final String invQuantity = mInventoryDatas.get(position).invQuantity;
        //这里给出的是location+specialInvFlag+specialInvNum的组合字段
        final String location = mInventoryDatas.get(position).locationCombine;
        final String batchFlag = getString(tvBatchFlag);

        if (position <= 0) {
            resetLocation();
            return;
        }
        if (spRefLine.getSelectedItemPosition() <= 0) {
            showMessage("请先选择单据行");
            return;
        }

        final RefDetailEntity lineData = getLineData(mSelectedRefLineNum);

        //检查库存地点
        if (TextUtils.isEmpty(lineData.invId)) {
            showMessage("未获取到库存点");
            return;
        }

        if (isOpenBatchManager && TextUtils.isEmpty(batchFlag)) {
            showMessage("请先输入批次");
            return;
        }

        if (TextUtils.isEmpty(location)) {
            showMessage("请先输入下架仓位");
            resetLocation();
            return;
        }

        tvInvQuantity.setText(invQuantity);

        final String refCodeId = mRefData.refCodeId;
        final String refType = mRefData.refType;
        final String bizType = mRefData.bizType;
        final String refLineId = lineData.refLineId;
        isBatchValidate = true;
        mPresenter.getTransferInfoSingle(refCodeId, refType, bizType, refLineId,
                getString(etMaterialNum), batchFlag, location, lineData.refDoc,
                CommonUtil.convertToInt(lineData.refDocItem),
                Global.USER_ID);
    }


    private void resetLocation() {
        spXLoc.setSelection(0, true);
        tvInvQuantity.setText("");
        tvLocQuantity.setText("");
        tvTotalQuantity.setText("");
    }


    @Override
    public void onBindCache(RefDetailEntity cache, String batchFlag, String location) {
        if (cache != null) {
            tvTotalQuantity.setText(cache.totalQuantity);
            //匹配缓存
            List<LocationInfoEntity> locationInfos = cache.locationList;
            if (locationInfos == null || locationInfos.size() == 0) {
                //没有缓存
                tvLocQuantity.setText("0");
                return;
            }
            tvLocQuantity.setText("0");
            for (LocationInfoEntity cachedItem : locationInfos) {
                //缓存和输入的都为空或者都不为空而且相等,那么系统默认批次匹配
                boolean isMatch;

                isBatchValidate = isOpenBatchManager && ((TextUtils.isEmpty(cachedItem.batchFlag) && TextUtils.isEmpty(batchFlag)) ||
                        (!TextUtils.isEmpty(cachedItem.batchFlag) && !TextUtils.isEmpty(batchFlag) && batchFlag.equalsIgnoreCase(cachedItem.batchFlag)));

                isMatch = isOpenBatchManager ? (TextUtils.isEmpty(cachedItem.batchFlag) && TextUtils.isEmpty(batchFlag) &&
                        location.equalsIgnoreCase(cachedItem.location)) || (
                        !TextUtils.isEmpty(cachedItem.batchFlag) && !TextUtils.isEmpty(batchFlag) &&
                                location.equalsIgnoreCase(cachedItem.location))
                        : location.equalsIgnoreCase(cachedItem.location);
                L.e("isBatchValidate = " + isBatchValidate + "; isMatch = " + isMatch);
                //注意它没有匹配次成功可能是批次页可能是仓位。
                if (isMatch) {
                    tvLocQuantity.setText(cachedItem.quantity);
                    break;
                }
            }
            if (!isBatchValidate) {
                showMessage("批次输入有误，请检查批次是否与缓存批次输入一致");
            }
        }
    }


    @Override
    public void loadCacheSuccess() {
        if (cbSingle.isChecked() && checkCollectedDataBeforeSave()) {
            saveCollectedData();
        }
    }

    @Override
    public void loadCacheFail(String message) {
        showMessage(message);
        isBatchValidate = true;
        //如果没有获取到任何缓存
        tvLocQuantity.setText("0");
        tvTotalQuantity.setText("0");
        if (cbSingle.isChecked() && checkCollectedDataBeforeSave()) {
            saveCollectedData();
        }
    }


    /**
     * 不论扫描的是否是同一个物料，都清除控件的信息。
     */
    private void clearAllUI() {
        clearCommonUI(tvMaterialDesc, tvWork, tvActQuantity, tvInv, tvInvType, tvLocQuantity,
                etQuantity, tvLocQuantity, tvSpecialInvFlag, tvInvQuantity, tvTotalQuantity, cbSingle);

        //单据行
        if (mRefLineAdapter != null) {
            mRefLines.clear();
            mRefLineAdapter.notifyDataSetChanged();
            spRefLine.setBackgroundColor(0);
        }

        //上架仓位
        if (mSLocationAdapter != null) {
            mSLocationList.clear();
            mSLocationAdapter.notifyDataSetChanged();
        }

        //下架仓位
        if (mXLocAdapter != null) {
            mInventoryDatas.clear();
            mXLocAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean checkCollectedDataBeforeSave() {
        //检查数据是否可以保存
        if (spRefLine.getSelectedItemPosition() <= 0) {
            showMessage("请先选择单据行");
            return false;
        }


        //物资条码
        if (isEmpty(getString(etMaterialNum))) {
            showMessage("请先输入物料条码");
            return false;
        }
        //批次
        if (isOpenBatchManager && !isBatchValidate) {
            showMessage("批次输入有误，请检查批次是否与缓存批次输入一致");
            return false;
        }
        //实发数量
        if (!cbSingle.isChecked() && TextUtils.isEmpty(getString(etQuantity))) {
            showMessage("请先输入数量");
            return false;
        }

        //处理库存数量
        if ("H".equalsIgnoreCase(mSHFlag) && TextUtils.isEmpty(getString(tvInvQuantity))) {
            showMessage("请先获取库存");
            return false;
        }
        //处理下架仓位
        if ("H".equalsIgnoreCase(mSHFlag) && spXLoc.getSelectedItemPosition() <= 0) {
            showMessage("请先选择下架仓位");
            return false;
        }

        //处理上架仓位
        if ("S".equalsIgnoreCase(mSHFlag) && TextUtils.isEmpty(getString(etSLocation))) {
            showMessage("请先输入上架仓位");
            return false;
        }

        if ("S".equalsIgnoreCase(mSHFlag) && !isLocationChecked) {
            showMessage("您输入的仓位不存在");
            return false;
        }
        if (!refreshQuantity(cbSingle.isChecked() ? "1" : getString(etQuantity))) {
            return false;
        }
        return true;
    }

    /**
     * 处理输入实收数量和累计数量
     * 父节点记录了前一次累计数量，所以这里仅仅将当前的入库数量与前一次的累计数量相加即可。
     */
    private boolean refreshQuantity(final String quantity) {
        //将已经录入的所有的子节点的仓位数量累加
        final float totalQuantityV = CommonUtil.convertToFloat(getString(tvTotalQuantity), 0.0f);
        final float actQuantityV = CommonUtil.convertToFloat(getString(tvActQuantity), 0.0f);
        final float quantityV = CommonUtil.convertToFloat(quantity, 0.0f);
        if (Float.compare(quantityV, 0.0f) <= 0.0f) {
            showMessage("输入数量不合理");
            return false;
        }
        //如果上架
        if ("S".equalsIgnoreCase(mSHFlag)) {
            if (Float.compare(quantityV + totalQuantityV, actQuantityV) > 0.0f) {
                showMessage("输入数量有误，请出现输入");
                if (!cbSingle.isChecked())
                    etQuantity.setText("");
                return false;
            }
        } else {
            //该仓位的历史出库数量
            final float historyQuantityV = CommonUtil.convertToFloat(getString(tvLocQuantity), 0.0f);
            //该仓位的库存数量
            float inventoryQuantity = CommonUtil.convertToFloat(getString(tvInvQuantity), 0.0f);
            if (Float.compare(quantityV + historyQuantityV, inventoryQuantity) > 0.0f) {
                showMessage("输入数量有误，请重新输入");
                etQuantity.setText("");
                return false;
            }
        }
        return true;
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

    @Override
    public void saveCollectedData() {
        if (!checkCollectedDataBeforeSave()) {
            return;
        }
        Flowable.create((FlowableOnSubscribe<ResultEntity>) emitter -> {
            RefDetailEntity lineData = getLineData(mSelectedRefLineNum);
            ResultEntity result = new ResultEntity();
            result.businessType = mRefData.bizType;
            result.refCodeId = mRefData.refCodeId;
            result.refCode = mRefData.recordNum;
            result.refLineNum = lineData.lineNum;
            result.voucherDate = mRefData.voucherDate;
            result.refType = mRefData.refType;
            result.moveType = mRefData.moveType;
            result.userId = Global.USER_ID;
            result.refLineId = lineData.refLineId;
            result.workId = lineData.workId;
            result.unit = TextUtils.isEmpty(lineData.recordUnit) ? lineData.materialUnit : lineData.recordUnit;
            result.unitRate = Float.compare(lineData.unitRate, 0.0f) == 0 ? 1.f : lineData.unitRate;
            result.invId = lineData.invId;
            result.materialId = lineData.materialId;
            if ("S".equalsIgnoreCase(mSHFlag)) {
                //上架仓位
                result.location = getString(etSLocation);
            } else {
                //下架仓位
                result.location = mInventoryDatas.get(spXLoc.getSelectedItemPosition()).location;
            }
            result.batchFlag = getString(tvBatchFlag);
            result.quantity = getString(etQuantity);
            result.modifyFlag = "N";
            result.specialConvert = "N";
            result.refDoc = lineData.refDoc;
            result.refDocItem = lineData.refDocItem;
            result.supplierNum = mRefData.supplierNum;
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.BUFFER).compose(TransformerHelper.io2main())
                .subscribe(result -> mPresenter.uploadCollectionDataSingle(result));
    }

    @Override
    public void saveCollectedDataSuccess() {
        showMessage("保存数据成功");
        final float quantityV = CommonUtil.convertToFloat(getString(etQuantity), 0.0f);
        final float locQuantityV = CommonUtil.convertToFloat(getString(tvLocQuantity), 0.0f);
        final float totalQuantity = CommonUtil.convertToFloat(getString(tvTotalQuantity), 0.0f);
        tvLocQuantity.setText(String.valueOf(quantityV + locQuantityV));
        tvTotalQuantity.setText(String.valueOf(totalQuantity + quantityV));
        if (!cbSingle.isChecked()) {
            etQuantity.setText("");
        }
    }

    @Override
    public void saveCollectedDataFail(String message) {
        showMessage("保存数据失败;" + message);
    }

    @Override
    public void _onPause() {
        super._onPause();
        clearAllUI();
        clearCommonUI(etMaterialNum, tvBatchFlag);
    }

    @Override
    public void retry(String retryAction) {
        switch (retryAction) {
            //获取单条缓存失败
            case Global.RETRY_LOAD_SINGLE_CACHE_ACTION:
                if ("S".equalsIgnoreCase(mSHFlag)) {
                    getTransferSingle(getString(tvBatchFlag), getString(etSLocation));
                } else if ("H".equalsIgnoreCase(mSHFlag)) {
                    getTransferSingle(spXLoc.getSelectedItemPosition());
                }
                break;
        }
        super.retry(retryAction);
    }


}
