package com.richfit.sdk_wzck.base_ds_collect;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.jakewharton.rxbinding2.widget.RxAdapterView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.richfit.common_lib.lib_adapter.InvAdapter;
import com.richfit.common_lib.lib_adapter.LocationAdapter;
import com.richfit.common_lib.lib_adapter.SimpleAdapter;
import com.richfit.common_lib.lib_base_sdk.base_collect.BaseCollectFragment;
import com.richfit.common_lib.utils.ArithUtil;
import com.richfit.common_lib.utils.L;
import com.richfit.common_lib.utils.SPrefUtil;
import com.richfit.common_lib.utils.UiUtil;
import com.richfit.common_lib.widget.RichEditText;
import com.richfit.data.constant.Global;
import com.richfit.data.helper.CommonUtil;
import com.richfit.data.helper.TransformerHelper;
import com.richfit.domain.bean.InvEntity;
import com.richfit.domain.bean.InventoryEntity;
import com.richfit.domain.bean.InventoryQueryParam;
import com.richfit.domain.bean.LocationInfoEntity;
import com.richfit.domain.bean.RefDetailEntity;
import com.richfit.domain.bean.ResultEntity;
import com.richfit.domain.bean.SimpleEntity;
import com.richfit.sdk_wzck.R;
import com.richfit.sdk_wzck.R2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import butterknife.BindView;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * 增加寄售转自有业务，将specialInvFlag和specialInvNum和location字段全部显示在仓位下拉里面，
 * 用户选择之后就确定了需要出库的库存类型。在服务端新增了一个字段是以上三个的组合，那么获取单条缓存，
 * 匹配单条缓存的逻辑不需要改变，但是在保存单条数据的时候，需要将specialInvFlag和specialInvNum和location
 * 三个字段从库存里面带出来回传给服务端。
 * Created by monday on 2016/11/19.
 */

public abstract class BaseDSCollectFragment<P extends IDSCollectPresenter> extends BaseCollectFragment<P>
        implements IDSCollectView {

    @BindView(R2.id.sp_ref_line_num)
    protected Spinner spRefLine;
    @BindView(R2.id.et_material_num)
    protected RichEditText etMaterialNum;
    @BindView(R2.id.tv_material_desc)
    TextView tvMaterialDesc;
    @BindView(R2.id.tv_material_unit)
    TextView tvMaterialUnit;
    @BindView(R2.id.tv_work)
    TextView tvWork;
    @BindView(R2.id.act_quantity_name)
    protected TextView tvActQuantityName;
    @BindView(R2.id.tv_act_quantity)
    protected TextView tvActQuantity;
    @BindView(R2.id.et_batch_flag)
    protected EditText etBatchFlag;
    @BindView(R2.id.sp_inv)
    protected Spinner spInv;
    @BindView(R2.id.sp_location)
    protected Spinner spLocation;
    @BindView(R2.id.tv_inv_quantity)
    protected TextView tvInvQuantity;
    @BindView(R2.id.tv_location_quantity)
    protected TextView tvLocQuantity;
    @BindView(R2.id.quantity_name)
    protected TextView tvQuantityName;
    @BindView(R2.id.et_quantity)
    protected EditText etQuantity;
    @BindView(R2.id.cb_single)
    protected CheckBox cbSingle;
    @BindView(R2.id.tv_total_quantity)
    protected TextView tvTotalQuantity;
    //增加仓储类型
    @BindView(R2.id.ll_location_type)
    LinearLayout llLocationType;
    @BindView(R2.id.sp_location_type)
    protected Spinner spLocationType;

    /*仓储类型*/
    protected List<SimpleEntity> mLocationTypes;
    /*单据行选项*/
    protected List<String> mRefLines;
    ArrayAdapter<String> mRefLineAdapter;
    /*库存地点*/
    protected List<InvEntity> mInvDatas;
    private InvAdapter mInvAdapter;
    /*库存信息*/
    protected List<InventoryEntity> mInventoryDatas;
    protected LocationAdapter mLocationAdapter;
    /*当前操作的明细行号*/
    protected String mSelectedRefLineNum;
    /*批次一致性检查*/
    protected boolean isBatchValidate = true;
    /*批次拆分。默认是不进行批次拆分*/
    protected boolean isSplitBatchFlag = false;
    protected boolean isLocationChecked = false;
    protected String mCachedBatchFlag;
    //当扫描下架仓位+仓储类型时必须先通过仓储类型去加载库存，将下架仓位保存
    protected String mAutoLocation;


    @Override
    public void handleBarCodeScanResult(String type, String[] list) {
        if (!etMaterialNum.isEnabled()) {
            showMessage("请先在抬头界面获取相关数据");
            return;
        }
        super.handleBarCodeScanResult(type, list);
        if (list != null && list.length > 12) {
            if (!etMaterialNum.isEnabled()) {
                showMessage("请先在抬头界面获取相关数据");
                return;
            }
            final String materialNum = list[Global.MATERIAL_POS];
            final String batchFlag = list[Global.BATCHFALG_POS];
            if (cbSingle.isChecked() && materialNum.equalsIgnoreCase(getString(etMaterialNum))) {
                getTransferSingle(spLocation.getSelectedItemPosition());
            } else {
                //在非单品模式下，扫描不同的物料。注意这里必须用新的物料和批次更新UI
                loadMaterialInfo(materialNum, batchFlag);
            }
        } else if (list != null && list.length == 1 && !cbSingle.isChecked()) {
            //仓位处理
            if (mInventoryDatas != null && spLocation.getAdapter() != null) {
                final String location = list[0];
                UiUtil.setSelectionForLocation(mInventoryDatas, location, spLocation);
            }
        } else if (list != null && list.length == 2 && !cbSingle.isChecked() && isOpenLocationType) {
            mAutoLocation = null;
            mAutoLocation = list[Global.LOCATION_POS];
            String locationType = list[Global.LOCATION_TYPE_POS];
            if (mLocationTypes != null && mLocationTypes.size() > 0 && spLocationType.getAdapter() != null) {
                String oldLocationType = mLocationTypes.get(spLocationType.getSelectedItemPosition()).code;
                if (locationType.equals(oldLocationType)) {
                    if (mInventoryDatas == null || mInventoryDatas.size() == 0) {
                        showMessage("请先获取库存");
                        return;
                    }
                    //如果当前仓储类型一致,那么直接获取单条缓存
                    UiUtil.setSelectionForLocation(mInventoryDatas, mAutoLocation, spLocation);
                    return;
                }
            }
            //如果仓储类型不一致
            UiUtil.setSelectionForSimpleSp(mLocationTypes, locationType, spLocationType);
            return;
        }
    }

    @Override
    protected int getContentId() {
        return R.layout.wzck_fragment_base_dsy_collect;
    }

    @Override
    protected void initView() {
        llLocationType.setVisibility(isOpenLocationType ? View.VISIBLE : View.GONE);
    }

    /**
     * 注册所有UI事件
     */
    @Override
    protected void initEvent() {
        //扫描后者手动输入物资条码
        etMaterialNum.setOnRichEditTouchListener((view, materialNum) -> {
            hideKeyboard(etMaterialNum);
            loadMaterialInfo(materialNum, getString(etBatchFlag));
        });

        //监听物料恢复批次状态
        RxTextView.textChanges(etMaterialNum)
                .filter(str -> !TextUtils.isEmpty(str))
                .subscribe(e -> {
                    isOpenBatchManager = true;
                    etBatchFlag.setEnabled(true);
                });

        //监测批次修改，如果修改了批次那么需要重新刷新库存信息和用户已经输入的信息.
        //这里需要注意的是，如果库存地点没有初始化完毕，修改批次不刷新UI
       /* RxTextView.textChanges(etBatchFlag)
                .filter(str -> !TextUtils.isEmpty(str) && spInv.getAdapter() != null)
                .subscribe(batch -> resetCommonUIPartly());*/

        //监听单据行
        RxAdapterView.itemSelections(spRefLine)
                .filter(position -> position > 0)
                .subscribe(position -> bindCommonCollectUI());

        //库存地点，选择库存地点加载库存数据
        RxAdapterView.itemSelections(spInv)
                .filter(a -> spInv.getSelectedItemPosition() > 0)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                //注意工厂和库存地点必须使用行里面的
                .subscribe(position -> {
                    if (isOpenLocationType) {
                        mPresenter.getDictionaryData("locationType");
                    } else {
                        loadInventory(position);
                    }
                });


        //选择仓储类型加载库存(这里不增加过来>0条件的目标是当用户从>0切回<=0时需要清除一些字段)
        RxAdapterView.itemSelections(spLocationType)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .filter(pos -> spInv.getAdapter() != null && spInv.getSelectedItemPosition() > 0)
                //注意工厂和库存地点必须使用行里面的
                .subscribe(position -> loadInventory(spInv.getSelectedItemPosition()));

        //下架仓位,选择下架仓位刷新库存数量，并且获取缓存。注意这里必须检查库存是否加载
        RxAdapterView
                .itemSelections(spLocation)
                .filter(position -> (mInventoryDatas != null && mInventoryDatas.size() > 0 && position.intValue() <= mInventoryDatas.size() - 1))
                .filter(a -> spInv.getAdapter() != null)
                .subscribe(position -> getTransferSingle(position));

        //单品(注意单品仅仅控制实收数量，累计数量是由行信息里面控制)
        cbSingle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            etQuantity.setText(isChecked ? "1" : "");
            etQuantity.setEnabled(!isChecked);
        });
    }



    /**
     * 检查抬头界面的必要的字段是否已经赋值
     */
    @Override
    protected void initDataLazily() {
        etMaterialNum.setEnabled(false);
        if (mRefData == null) {
            showMessage("请先在抬头界面获取单据数据");
            return;
        }

        if (isEmpty(mRefData.bizType)) {
            showMessage("未获取到业务类型");
            return;
        }

        if (isEmpty(mRefData.moveType)) {
            showMessage("未获取到移动类型");
            return;
        }

        if (isEmpty(mRefData.refType)) {
            showMessage("请先在抬头界面获取单据数据");
            return;
        }
        if (isEmpty(mRefData.voucherDate)) {
            showMessage("请先在抬头界面选择过账日期");
            return;
        }
        String transferKey = (String) SPrefUtil.getData(mBizType + mRefType, "0");
        if ("1".equals(transferKey)) {
            showMessage("本次采集的数据已经上传,请先到数据明细界面进行过账等操作");
            return;
        }
        etMaterialNum.setEnabled(true);
        isOpenBatchManager = true;
        etBatchFlag.setEnabled(true);
    }

    @Override
    public void loadMaterialInfo(String materialNum, String batchFlag) {
        if (!etMaterialNum.isEnabled()) {
            return;
        }
        if (TextUtils.isEmpty(materialNum)) {
            showMessage("请输入物资条码");
            return;
        }
        clearAllUI();
        etMaterialNum.setText(materialNum);
        etBatchFlag.setText(batchFlag);
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
        if (mRefLines == null) {
            mRefLines = new ArrayList<>();
        }
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
        //再次复位批次管理标识
        isOpenBatchManager = true;
        etBatchFlag.setEnabled(true);
        manageBatchFlagStatus(etBatchFlag, lineData.batchManagerStatus);
        etQuantity.setText("");
        //物资描述
        tvMaterialDesc.setText(lineData.materialDesc);
        //计量单位
        tvMaterialUnit.setText(lineData.unit);
        //工厂
        tvWork.setText(lineData.workName);
        //应收数量
        tvActQuantity.setText(lineData.actQuantity);
        //批次
        if (isOpenBatchManager && TextUtils.isEmpty(getString(etBatchFlag))) {
            etBatchFlag.setText(lineData.batchFlag);
        }
        etBatchFlag.setEnabled(isOpenBatchManager);
        //先将库存地点选择器打开，获取缓存后在判断是否需要锁定
        spInv.setEnabled(true);
        if (!cbSingle.isChecked())
            mPresenter.getInvsByWorkId(lineData.workId, getOrgFlag());
    }

    @Override
    public void loadInvFail(String message) {
        showMessage(message);
        spInv.setSelection(0);
    }

    @Override
    public void showInvs(ArrayList<InvEntity> list) {
        if (mInvDatas == null) {
            mInvDatas = new ArrayList<>();
        }
        //初始化库存地点
        mInvDatas.clear();
        mInvDatas.addAll(list);
        if (mInvAdapter == null) {
            mInvAdapter = new InvAdapter(mActivity, R.layout.item_simple_sp, mInvDatas);
            spInv.setAdapter(mInvAdapter);
        } else {
            mInvAdapter.notifyDataSetChanged();
        }
        //默认选择第一个
        spInv.setSelection(0);
    }


    @Override
    public void loadInvComplete() {
        RefDetailEntity lineData = getLineData(mSelectedRefLineNum);
        if (mInvDatas != null && mInvAdapter != null && !TextUtils.isEmpty(lineData.invCode)) {
            UiUtil.setSelectionForInv(mInvDatas, lineData.invCode, spInv);
        }
    }


    /**
     * 选择库存地点，加载库存
     *
     * @param position:库存地点的位置
     */
    protected void loadInventory(int position) {
        tvInvQuantity.setText("");
        tvLocQuantity.setText("");
        tvTotalQuantity.setText("");
        if (mLocationAdapter != null) {
            mInventoryDatas.clear();
            mLocationAdapter.notifyDataSetChanged();
        }
        if (position <= 0) {
            return;
        }

        if (isOpenBatchManager && TextUtils.isEmpty(getString(etBatchFlag))) {
            showMessage("请输入批次");
            return;
        }
        RefDetailEntity lineData = getLineData(mSelectedRefLineNum);
        InvEntity invEntity = mInvDatas.get(position);

        InventoryQueryParam param = provideInventoryQueryParam();
        mPresenter.getInventoryInfo(param.queryType, lineData.workId,
                invEntity.invId, lineData.workCode, invEntity.invCode, "", getString(etMaterialNum),
                lineData.materialId, "", getString(etBatchFlag), lineData.specialInvFlag,
                lineData.specialInvNum, param.invType, param.extraMap);
    }

    /**
     * 加载库存成功。
     * location + specialInvFlag + specialInvNum
     *
     * @param list
     */
    @Override
    public void showInventory(List<InventoryEntity> list) {
        if (mInventoryDatas == null) {
            mInventoryDatas = new ArrayList<>();
        }
        mInventoryDatas.clear();
        InventoryEntity tmp = new InventoryEntity();
        tmp.locationCombine = "请选择";
        mInventoryDatas.add(tmp);
        mInventoryDatas.addAll(list);
        if (mLocationAdapter == null) {
            mLocationAdapter = new LocationAdapter(mActivity, R.layout.item_simple_sp, mInventoryDatas);
            spLocation.setAdapter(mLocationAdapter);
        } else {
            mLocationAdapter.notifyDataSetChanged();
        }
        spLocation.setSelection(0);
    }

    @Override
    public void loadInventoryFail(String message) {
        showMessage(message);
    }

    /**
     * 如果获取库存成功，可以获取建议仓位
     * 如果打开了仓储类型那么这里的逻辑上，扫描仓储类型先匹配仓储类型->触发下载最新库存->匹配下架仓位->触发单条缓存匹配
     */
    @Override
    public void loadInventoryComplete() {
        if (isOpenLocationType && TextUtils.isEmpty(mAutoLocation)) {
            return;
        }
        //自动匹配下架仓位，并获取缓存
        UiUtil.setSelectionForLocation(mInventoryDatas, mAutoLocation, spLocation);
    }

    /**
     * 获取单条缓存。注意这必须先确定好批次和仓位是否输入，因为系统将用批次和仓位匹配缓存信息。
     *
     * @param position:下架仓位的位置
     */
    protected void getTransferSingle(int position) {
        //检测是否选择了发出库位
        if (position <= 0) {
            spLocation.setSelection(0);
            tvInvQuantity.setText("");
            tvLocQuantity.setText("");
            tvTotalQuantity.setText("");
            return;
        }

        //这里给出的是location+specialInvFlag+specialInvNum的组合字段
        final String location = mInventoryDatas.get(position).locationCombine;
        final String batchFlag = getString(etBatchFlag);

        //从新计算库存数量
        RefDetailEntity data = getLineData(mSelectedRefLineNum);
        InventoryEntity invData = mInventoryDatas.get(position);
        String invQuantity = calQuantityByUnitRate(invData.invQuantity, data.recordUnit, data.unitRate);
        invData.invQuantity = invQuantity;
        tvInvQuantity.setText(invData.invQuantity);

        if (spRefLine.getSelectedItemPosition() <= 0) {
            showMessage("请先选择单据行");
            return;
        }
        //检验是否选择了库存地点
        if (spInv.getSelectedItemPosition() <= 0) {
            showMessage("请先选择库存地点");
            return;
        }

        if (isOpenBatchManager && TextUtils.isEmpty(batchFlag)) {
            showMessage("请先输入批次");
            return;
        }

        if (TextUtils.isEmpty(location)) {
            showMessage("请先输入下架仓位");
            tvInvQuantity.setText("");
            tvLocQuantity.setText("");
            tvTotalQuantity.setText("");
            return;
        }

        tvInvQuantity.setText(invQuantity);
        final RefDetailEntity lineData = getLineData(mSelectedRefLineNum);
        final String refCodeId = mRefData.refCodeId;
        final String refType = mRefData.refType;
        final String bizType = mRefData.bizType;
        final String refLineId = lineData.refLineId;
        isBatchValidate = true;
        mPresenter.getTransferInfoSingle(refCodeId, refType, bizType, refLineId,
                getString(etMaterialNum), batchFlag, location, lineData.refDoc,
                CommonUtil.convertToInt(lineData.refDocItem), Global.USER_ID);
    }


    /**
     * 加载该仓位的仓位数量（注意如果可以请求仓位历史数据时，那么说明不需要考虑在物料是否是质检。）
     * 是否上架的标志位必须在refresUI方法中应确定好,这里必须检查批次是否一致。
     * <p>
     * 这里为了增加寄售转自有的功能，搜索缓存的关键字变成了批次和仓位以及specialInvFlag,specialInvNum
     *
     * @param batchFlag
     * @param locationCombine
     */
    @Override
    public void onBindCache(RefDetailEntity cache, String batchFlag, String locationCombine) {
        if (cache != null) {
            tvTotalQuantity.setText(cache.totalQuantity);
            //查询该行的locationInfo
            List<LocationInfoEntity> locationInfos = cache.locationList;
            if (locationInfos == null || locationInfos.size() == 0) {
                //没有缓存
                tvLocQuantity.setText("0");
                return;
            }
            //如果有缓存，但是可能匹配不上
            tvLocQuantity.setText("0");
            //匹配每一个缓存
            for (LocationInfoEntity cachedItem : locationInfos) {
                if ("barcode".equalsIgnoreCase(cachedItem.location)) {
                    //不显示该仓位的值
                    return;
                }
                //缓存和输入的都为空或者都不为空而且相等
                boolean isMatch = false;

                isBatchValidate = !isOpenBatchManager ? true : ((TextUtils.isEmpty(cachedItem.batchFlag) && TextUtils.isEmpty(batchFlag)) ||
                        (!TextUtils.isEmpty(cachedItem.batchFlag) && !TextUtils.isEmpty(batchFlag) &&
                                batchFlag.equalsIgnoreCase(cachedItem.batchFlag)));
                String locationType = "";
                if(isOpenLocationType) {
                    locationType = mLocationTypes.get(spLocationType.getSelectedItemPosition()).code;
                }
                //这里匹配的逻辑是，如果打开了匹配管理，那么如果输入了批次通过批次和仓位匹配，而且如果批次没有输入，那么通过仓位匹配。
                //如果没有打开批次管理，那么直接通过仓位匹配
                if (!isOpenBatchManager) {
                    isMatch = isOpenLocationType ? locationCombine.equalsIgnoreCase(cachedItem.locationCombine)
                            && locationType.equalsIgnoreCase(cachedItem.locationType) : locationCombine.equalsIgnoreCase(cachedItem.locationCombine);
                } else {
                    if (TextUtils.isEmpty(cachedItem.batchFlag) && TextUtils.isEmpty(batchFlag)) {
                        isMatch = isOpenLocationType ? locationCombine.equalsIgnoreCase(cachedItem.locationCombine)
                                && locationType.equalsIgnoreCase(cachedItem.locationType) : locationCombine.equalsIgnoreCase(cachedItem.locationCombine);
                    } else if (!TextUtils.isEmpty(cachedItem.batchFlag) && !TextUtils.isEmpty(batchFlag)) {
                        isMatch = isOpenLocationType ? locationCombine.equalsIgnoreCase(cachedItem.locationCombine) && batchFlag.equalsIgnoreCase(cachedItem.batchFlag)
                                && locationType.equalsIgnoreCase(cachedItem.locationType) : locationCombine.equalsIgnoreCase(cachedItem.locationCombine) && batchFlag.equalsIgnoreCase(cachedItem.batchFlag);
                    }
                }

                L.e("isBatchValidate = " + isBatchValidate + "; isMatch = " + isMatch);

                //注意它没有匹配成功，可能是批次没有匹配也可能是仓位没有匹配。
                if (isMatch) {
                    tvLocQuantity.setText(cachedItem.quantity);
                    break;
                }
            }
            //2017年07月19日增加批次拆分标识。如果不进行批次拆分那么批次必须保持一致。
            if (!isSplitBatchFlag && !isBatchValidate) {
                showMessage("批次输入有误，请检查批次是否与缓存批次输入一致");
            }
            //锁定库存地点
            if (cache != null) {
                lockInv(cache.invId);
            }
        }
    }

    /**
     * 锁住库存地点
     *
     * @param cachedInvId:缓存的库存地点
     */
    protected void lockInv(String cachedInvId) {
        //锁定库存地点
        if (!TextUtils.isEmpty(cachedInvId)) {
            int pos = -1;
            for (InvEntity data : mInvDatas) {
                pos++;
                if (cachedInvId.equals(data.invId))
                    break;
            }
            spInv.setEnabled(false);
            spInv.setSelection(pos);
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
        spInv.setEnabled(true);
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
    protected void clearAllUI() {
        clearCommonUI(tvMaterialDesc, tvWork, tvActQuantity, tvLocQuantity, etQuantity, tvLocQuantity, tvInvQuantity,
                tvTotalQuantity, cbSingle, etMaterialNum, etBatchFlag, tvMaterialUnit);
        //单据行
        if (mRefLineAdapter != null) {
            mRefLines.clear();
            mRefLineAdapter.notifyDataSetChanged();
            spRefLine.setBackgroundColor(0);
        }
        //库存地点
        if (mInvAdapter != null) {
            mInvDatas.clear();
            mInvAdapter.notifyDataSetChanged();
        }
        //下架仓位
        if (mLocationAdapter != null) {
            mInventoryDatas.clear();
            mLocationAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 用户修改批次后重置部分UI
     */
    private void resetCommonUIPartly() {
        //如果没有打开批次，那么用户不能输入批次，这里再次拦截
        if (!isOpenBatchManager)
            return;
        //库存地点
        if (spInv.getAdapter() != null) {
            spInv.setEnabled(true);
            spInv.setSelection(0);
        }
        //下架仓位
        if (mInventoryDatas != null && mInventoryDatas.size() > 0 &&
                spLocation.getAdapter() != null) {
            mInventoryDatas.clear();
            mLocationAdapter.notifyDataSetChanged();
        }
        //库存数量
        tvInvQuantity.setText("");
        //历史仓位数量
        tvLocQuantity.setText("");
        //实收数量
        etQuantity.setText("");
        //累计数量
        tvTotalQuantity.setText("");
    }

    /**
     * 检查数量是否合理。第一：实发数量+累计数量<=应发数量;第二：该仓位已经录入的数量 + 实发数量 <= 库存数量
     */
    protected boolean refreshQuantity(final String quantity) {
        if (Float.valueOf(quantity) <= 0.0f) {
            showMessage("输入数量不合理");
            return false;
        }

        float totalQuantityV = 0.0f;
        //累计数量
        totalQuantityV += CommonUtil.convertToFloat(getString(tvTotalQuantity), 0.0f);
        //应该数量
        final float actQuantityV = CommonUtil.convertToFloat(getString(tvActQuantity), 0.0f);
        //本次出库数量
        final float quantityV = CommonUtil.convertToFloat(quantity, 0.0f);
        if (Float.compare(quantityV + totalQuantityV, actQuantityV) > 0.0f) {
            showMessage("输入数量有误，请重新输入");
            etQuantity.setText("");
            return false;
        }
        //该仓位的历史出库数量
        final float historyQuantityV = CommonUtil.convertToFloat(getString(tvLocQuantity), 0.0f);
        //该仓位的库存数量
        float inventoryQuantity = CommonUtil.convertToFloat(getString(tvInvQuantity), 0.0f);

        if (Float.compare(quantityV + historyQuantityV, inventoryQuantity) > 0.0f) {
            showMessage("输入数量有误，请重新输入");
            etQuantity.setText("");
            return false;
        }
        return true;
    }

    /**
     * 检查数据是否合格
     *
     * @return
     */
    @Override
    public boolean checkCollectedDataBeforeSave() {

        if (!etMaterialNum.isEnabled()) {
            showMessage("请先获取物料信息");
            return false;
        }

        //检查数据是否可以保存
        if (spRefLine.getSelectedItemPosition() <= 0) {
            showMessage("请先选择单据行");
            return false;
        }
        //库存地点
        if (spInv.getSelectedItemPosition() <= 0) {
            showMessage("请先选择库存地点");
            return false;
        }

        if (TextUtils.isEmpty(getString(tvInvQuantity))) {
            showMessage("请先获取库存");
            return false;
        }

        //物资条码
        if (isEmpty(getString(etMaterialNum))) {
            showMessage("请先输入物料条码");
            return false;
        }

        //批次
        if (!isSplitBatchFlag && isOpenBatchManager && !isBatchValidate) {
            showMessage("批次输入有误，请检查批次是否与缓存批次输入一致");
            return false;
        }
        //实发数量
        if (!cbSingle.isChecked() && TextUtils.isEmpty(getString(etQuantity))) {
            showMessage("请先输入数量");
            return false;
        }

        if (!refreshQuantity(cbSingle.isChecked() ? "1" : getString(etQuantity))) {
            return false;
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
    public ResultEntity provideResult() {
        RefDetailEntity lineData = getLineData(mSelectedRefLineNum);
        ResultEntity result = new ResultEntity();
        InventoryQueryParam param = provideInventoryQueryParam();
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
        result.invId = mInvDatas.get(spInv.getSelectedItemPosition()).invId;
        result.materialId = lineData.materialId;
        result.batchFlag = !isOpenBatchManager ? Global.DEFAULT_BATCHFLAG : getString(etBatchFlag);
        result.quantity = getString(etQuantity);
        result.unit = TextUtils.isEmpty(lineData.recordUnit) ? lineData.materialUnit : lineData.recordUnit;
        result.unitRate = Float.compare(lineData.unitRate, 0.0f) == 0 ? 1.f : lineData.unitRate;
        //库存相关的字段回传
        int locationPos = spLocation.getSelectedItemPosition();
        if (mInventoryDatas != null && mInventoryDatas.size() > 0) {
            result.location = mInventoryDatas.get(locationPos).location;
            result.specialInvFlag = mInventoryDatas.get(locationPos).specialInvFlag;
            result.specialInvNum = mInventoryDatas.get(locationPos).specialInvNum;
            //寄售转自有的标识
            result.specialConvert = (!TextUtils.isEmpty(result.specialInvFlag) && "k".equalsIgnoreCase(result.specialInvFlag)
                    && !TextUtils.isEmpty(result.specialInvNum)) ?
                    "Y" : "N";
        }
        result.supplierNum = mRefData.supplierNum;
        result.invType = param.invType;
        if (isOpenLocationType)
            result.locationType = mLocationTypes.get(spLocationType.getSelectedItemPosition()).code;
        result.modifyFlag = "N";
        return result;
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
    public void getSuggestedLocationSuccess(InventoryEntity suggestedInventory) {
        if (suggestedInventory != null && !TextUtils.isEmpty(suggestedInventory.suggestLocation) && mInventoryDatas != null) {
            int pos = -1;
            for (InventoryEntity data : mInventoryDatas) {
                pos++;
                if (suggestedInventory.suggestLocation.equalsIgnoreCase(data.locationCombine)) {
                    break;
                }
            }
            if (pos >= 0) {
                spLocation.setSelection(pos);
            }
        }
    }

    @Override
    public void getSuggestedLocationFail(String message) {
        showMessage(message);
    }

    @Override
    public void getSuggestedLocationComplete() {
    }

    @Override
    public void checkLocationFail(String message) {
        showMessage(message);
    }

    @Override
    public void checkLocationSuccess(String batchFlag, String location) {

    }

    @Override
    public void saveCollectedDataSuccess(String message) {
        showMessage(message);
        tvTotalQuantity.setText(String.valueOf(ArithUtil.add(getString(etQuantity), getString(tvTotalQuantity))));
        tvLocQuantity.setText(String.valueOf(ArithUtil.add(getString(etQuantity), getString(tvLocQuantity))));
        if (!cbSingle.isChecked()) {
            etQuantity.setText("");
        }
    }

    @Override
    public void saveCollectedDataFail(String message) {
        showMessage("保存数据失败;" + message);
    }


    @Override
    protected InventoryQueryParam provideInventoryQueryParam() {
        InventoryQueryParam queryParam = super.provideInventoryQueryParam();
        if (mLocationTypes != null && isOpenLocationType) {
            queryParam.extraMap = new HashMap<>();
            String locationType = mLocationTypes.get(spLocationType.getSelectedItemPosition()).code;
            queryParam.extraMap.put("locationType", locationType);
        }
        return queryParam;
    }

    @Override
    public void _onPause() {
        super._onPause();
        //将仓储类型回到原始位置
        if(spLocationType.getAdapter() != null) {
            spLocationType.setSelection(0);
        }
        clearAllUI();
    }

    @Override
    public void retry(String retryAction) {
        switch (retryAction) {
            //获取单条缓存失败
            case Global.RETRY_LOAD_SINGLE_CACHE_ACTION:
                getTransferSingle(spLocation.getSelectedItemPosition());
                break;
        }
        super.retry(retryAction);
    }

    protected abstract int getOrgFlag();

}
