package com.richfit.module_qysh.module_ys.collect;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.jakewharton.rxbinding2.widget.RxAdapterView;

import com.richfit.common_lib.lib_adapter.BottomDialogMenuAdapter;
import com.richfit.common_lib.lib_adapter.InvAdapter;
import com.richfit.common_lib.lib_base_sdk.base_collect.BaseCollectFragment;
import com.richfit.common_lib.utils.ArithUtil;
import com.richfit.common_lib.utils.UiUtil;
import com.richfit.common_lib.widget.RichEditText;
import com.richfit.data.constant.Global;
import com.richfit.data.helper.CommonUtil;
import com.richfit.data.helper.TransformerHelper;
import com.richfit.domain.bean.BottomMenuEntity;
import com.richfit.domain.bean.InvEntity;
import com.richfit.domain.bean.RefDetailEntity;
import com.richfit.domain.bean.ResultEntity;
import com.richfit.module_qysh.R;
import com.richfit.module_qysh.R2;
import com.richfit.module_qysh.module_ys.collect.imp.QYSHAOPresenterImp;
import com.richfit.sdk_wzys.camera.TakephotoActivity;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;

/**
 * 其他验收。处理逻辑：
 * 扫描物料匹配单据的物料信息->调用getTransferInfoSingle方法获取缓存(获取库存地点和用户数量)
 * Created by monday on 2016/11/23.
 */

public class QYSHAOCollectFragment extends BaseCollectFragment<QYSHAOPresenterImp>
        implements IQYSHAOView {

    @BindView(R2.id.sp_ref_line_num)
    Spinner spRefLine;
    @BindView(R2.id.et_material_num)
    RichEditText etMaterialNum;
    @BindView(R2.id.tv_material_desc)
    TextView tvMaterialDesc;
    @BindView(R2.id.tv_act_quantity)
    TextView tvActQuantity;
    @BindView(R2.id.tv_work)
    TextView tvWork;
    @BindView(R2.id.sp_inv)
    protected Spinner spInv;
    @BindView(R2.id.et_batch_flag)
    EditText etBatchFlag;
    /*合同数量(单据数量)*/
    @BindView(R2.id.tv_order_quantity)
    protected TextView tvOrderQuantity;
    /*到货数量*/
    @BindView(R2.id.et_quantity)
    protected EditText etQuantity;

    /*结算数量*/
    @BindView(R2.id.qysh_tv_balance_quantity)
    protected TextView tvBalanceQuantity;

    /*/匹配的检验批行明细*/
    List<String> mRefLines;
    ArrayAdapter<String> mRefLineAdapter;
    /*当前正在操作的单据行号*/
    String mSelectedRefLineNum;
    /*缓存的到货数量*/
    String mCacheQuantity;
    /*缓存的库存地点*/
    String mCacheInvCode;
    /*库存地点*/
    List<InvEntity> mInvDatas;
    InvAdapter mInvAdapter;

    @Override
    public void handleBarCodeScanResult(String type, String[] list) {
        if (list != null && list.length > 12) {
            final String materialNum = list[Global.MATERIAL_POS];
            final String batchFlag = list[Global.BATCHFALG_POS];
            loadMaterialInfo(materialNum, batchFlag);
        }
    }

    @Override
    protected int getContentId() {
        return R.layout.qysh_fragment_ao_collect;
    }

    @Override
    public void initPresenter() {
        mPresenter = new QYSHAOPresenterImp(mActivity);
    }


    @Override
    public void initVariable(Bundle savedInstanceState) {
        super.initVariable(savedInstanceState);
        mRefLines = new ArrayList<>();
        mInvDatas = new ArrayList<>();
    }

    @Override
    protected void initView() {

    }

    @Override
    public void initEvent() {
        /*扫描后者手动输入物资条码*/
        etMaterialNum.setOnRichEditTouchListener((view, materialNum) -> {
            hideKeyboard(etMaterialNum);
            //手动输入没有批次
            loadMaterialInfo(materialNum, getString(etBatchFlag));
        });

        /*单据行*/
        RxAdapterView
                .itemSelections(spRefLine)
                .filter(position -> position > 0)
                .subscribe(position -> getTransferInfoSingle(position.intValue()));
    }

    @Override
    public void initData() {

    }

    @Override
    public void initDataLazily() {
        etMaterialNum.setEnabled(false);
        if (mRefData == null) {
            showMessage("请先在抬头界面获取单据数据");
            return;
        }
        if (TextUtils.isEmpty(mRefData.refType)) {
            showMessage("请先在抬头界面获取单据数据");
            return;
        }
        if (TextUtils.isEmpty(mRefData.voucherDate)) {
            showMessage("请先在抬头界面选择过账日期");
            return;
        }
        etMaterialNum.setEnabled(true);
        etBatchFlag.setEnabled(isOpenBatchManager);
    }

    /**
     * 加载物料明细信息。
     *
     * @param materialNum:物资条码
     */
    @Override
    public void loadMaterialInfo(final String materialNum, final String batchFlag) {
        if (!etMaterialNum.isEnabled())
            return;

        if (TextUtils.isEmpty(materialNum)) {
            showMessage("请输入物资条码");
            return;
        }
        clearAllUI();
        etBatchFlag.setText(batchFlag);
        //刷新界面(在单据行明细查询是否有该物料条码，如果有那么刷新界面)
        matchMaterialInfo(materialNum, batchFlag)
                .compose(TransformerHelper.io2main())
                .subscribe(details -> setupRefLineAdapter(details), e -> showMessage(e.getMessage()));
    }

    /**
     * 设置单据行适配器
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
            showMessage("该单据中未查询到该物料,请检查物资编码是否正确");
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
     * 获取验收缓存
     *
     * @param index
     */
    private void getTransferInfoSingle(int index) {
        spRefLine.setSelection(index);
        mSelectedRefLineNum = mRefLines.get(spRefLine.getSelectedItemPosition());
        final RefDetailEntity lineData = getLineData(mSelectedRefLineNum);
        if (lineData == null)
            return;
        final String refCodeId = mRefData.refCodeId;
        final String refType = mRefData.refType;
        final String bizType = mRefData.bizType;
        final String refLineId = lineData.refLineId;
        mPresenter.getTransferInfoSingle(refCodeId, refType, bizType, refLineId, getString(etBatchFlag), "",
                lineData.refDoc, CommonUtil.convertToInt(lineData.refDocItem), Global.USER_ID);
    }

    @Override
    public void onBindCache(RefDetailEntity cache, String batchFlag, String location) {
        if (cache != null) {
            mCacheQuantity = cache.totalQuantity;
            mCacheInvCode = cache.invCode;
        }
    }

    @Override
    public void loadCacheFail(String message) {
        showMessage(message);
        clearAllUI();
    }

    /**
     * 绑定UI。
     */
    @Override
    public void bindCommonCollectUI() {
        etQuantity.setText("");
        mSelectedRefLineNum = mRefLines.get(spRefLine.getSelectedItemPosition());
        RefDetailEntity lineData = getLineData(mSelectedRefLineNum);
        //物资描述
        tvMaterialDesc.setText(lineData.materialDesc);
        //工厂
        tvWork.setText(lineData.workName);
        //单据数量
        tvOrderQuantity.setText(lineData.orderQuantity);
        //应收数量
        tvActQuantity.setText(lineData.actQuantity);
        //到货数量(用户上传输入的数量)
        etQuantity.setText(mCacheQuantity);
        //批次
        if (TextUtils.isEmpty(getString(etBatchFlag))) {
            etBatchFlag.setText(isOpenBatchManager ? lineData.batchFlag : "");
        }
        //获取库存地点
        mPresenter.getInvsByWorkId(lineData.workId, 0);
    }

    @Override
    public void showInvs(List<InvEntity> invs) {
        //初始化库存地点
        mInvDatas.clear();
        mInvDatas.addAll(invs);
        if (mInvAdapter == null) {
            mInvAdapter = new InvAdapter(mActivity, R.layout.item_simple_sp, mInvDatas);
            spInv.setAdapter(mInvAdapter);
        } else {
            mInvAdapter.notifyDataSetChanged();
        }
        int position = -1;
        if (!TextUtils.isEmpty(mCacheInvCode)) {
            //如果有缓存，那么系统自动选择缓存
            for (InvEntity data : mInvDatas) {
                position++;
                if (mCacheInvCode.equals(data.invCode)) {
                    break;
                }
            }
        }
        //默认选择第一个
        spInv.setSelection(position == -1 ? 0 : position);
    }

    @Override
    public void loadInvsFail(String message) {
        showMessage(message);
        SpinnerAdapter adapter = spInv.getAdapter();
        if (adapter != null && InvAdapter.class.isInstance(adapter)) {
            mInvDatas.clear();
            InvAdapter invAdapter = (InvAdapter) adapter;
            invAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 清除所有的UI字段
     */
    private void clearAllUI() {
        clearCommonUI(tvMaterialDesc, tvWork, etBatchFlag, tvActQuantity, tvOrderQuantity, tvBalanceQuantity, etQuantity);

        //库存地点
        if (spInv.getAdapter() != null) {
            spInv.setSelection(0);
        }

        //单据
        if (mRefLineAdapter != null) {
            mRefLines.clear();
            mRefLineAdapter.notifyDataSetChanged();
            spRefLine.setBackgroundColor(0);
        }
    }

    /**
     * 检查用户输入的到货数量是否合理，必须保证到货数量小于等于应收数量而且大于等于0
     *
     * @param quantity
     * @return
     */
    private boolean refreshQuantity(final String quantity) {

        //检验批数量
        final float actQuantityV = CommonUtil.convertToFloat(getString(tvOrderQuantity), 0.0f);
        final float quantityV = CommonUtil.convertToFloat(quantity, 0.0f);

        if (Float.compare(quantityV, 0.0f) <= 0.0f) {
            showMessage("输入实收数量有误,请出现输入");
            etQuantity.setText("");
            return false;
        }
        if (Float.compare(quantityV, actQuantityV) > 0.0f) {
            showMessage("输入实收数量有误,请出现输入");
            etQuantity.setText("");
            return false;
        }
        return true;
    }

    @Override
    public boolean checkCollectedDataBeforeSave() {
        if (!etMaterialNum.isEnabled()) {
            showMessage("请先获取单据数据");
            return false;
        }
        //检查数据是否可以保存
        if (spRefLine.getSelectedItemPosition() == 0) {
            showMessage("请选选择单据行");
            return false;
        }
        if (TextUtils.isEmpty(mSelectedRefLineNum)) {
            showMessage("请选选择单据行");
            return false;
        }

        if (spInv.getSelectedItemPosition() == 0) {
            showMessage("请选择库存点");
            return false;
        }

        if (!refreshQuantity(getString(tvActQuantity))) {
            return false;
        }

        //批次信息必须与单据中的一样，这里批次在单据中一定有，但是防止用户修改
        if (isOpenBatchManager) {
            RefDetailEntity lineData = getLineData(mSelectedRefLineNum);
            final String batchFlag = lineData.batchFlag;
            if (!TextUtils.isEmpty(batchFlag) &&
                    !batchFlag.equalsIgnoreCase(getString(etBatchFlag))) {
                showMessage("批次与单据中的批次不一致");
                return false;
            }
        }

        return true;
    }

    @Override
    public void showOperationMenuOnCollection(final String companyCode) {
        View rootView = LayoutInflater.from(mActivity).inflate(R.layout.dialog_bottom_menu, null);
        GridView menu = (GridView) rootView.findViewById(R.id.gd_menus);
        final List<BottomMenuEntity> menus = provideDefaultBottomMenu();
        BottomDialogMenuAdapter adapter = new BottomDialogMenuAdapter(mActivity, R.layout.item_dialog_bottom_menu, menus);
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
                    //1.保存数据
                    saveCollectedData();
                    break;
                case 1:
                case 2:
                case 3:
                case 4:
                    //2.拍照
                    toTakePhoto(menus.get(position).menuName, menus.get(position).takePhotoType);
                    break;
            }
            dialog.dismiss();
        });
    }

    @Override
    public List<BottomMenuEntity> provideDefaultBottomMenu() {
        List<BottomMenuEntity> menus = new ArrayList<>();
        BottomMenuEntity menu = new BottomMenuEntity();
        menu.menuName = "保存数据";
        menu.menuImageRes = R.mipmap.icon_save_data;
        menus.add(menu);

        menu = new BottomMenuEntity();
        menu.menuName = "质量证明文件";
        menu.menuImageRes = R.mipmap.icon_take_photo1;
        menu.takePhotoType = 1;
        menus.add(menu);

        menu = new BottomMenuEntity();
        menu.menuName = "技术附件";
        menu.menuImageRes = R.mipmap.icon_take_photo2;
        menu.takePhotoType = 2;
        menus.add(menu);

        menu = new BottomMenuEntity();
        menu.menuName = "外观照片";
        menu.menuImageRes = R.mipmap.icon_take_photo3;
        menu.takePhotoType = 3;
        menus.add(menu);

        menu = new BottomMenuEntity();
        menu.menuName = "其他";
        menu.menuImageRes = R.mipmap.icon_take_photo4;
        menu.takePhotoType = 4;
        menus.add(menu);
        return menus;
    }


    @Override
    public ResultEntity provideResult() {
        RefDetailEntity lineData = getLineData(mSelectedRefLineNum);
        ResultEntity result = new ResultEntity();
        result.refCodeId = mRefData.refCodeId;
        result.refCode = mRefData.recordNum;
        result.refLineId = lineData.refLineId;
        result.businessType = mRefData.bizType;
        result.companyCode = Global.COMPANY_CODE;
        result.refType = mRefData.refType;
        result.moveType = mRefData.moveType;
        result.inspectionType = mRefData.inspectionType;
        result.inspectionPerson = Global.USER_ID;
        result.userId = Global.USER_ID;
        result.invId = mInvDatas.get(spInv.getSelectedItemPosition()).invId;
        result.quantity = getString(etQuantity);
        result.modifyFlag = "N";
        return result;
    }

    @Override
    public void saveCollectedDataSuccess(String message) {
        showMessage("数据保存成功");
        //结算数量为累计数量的逻辑
        //本次输入的数量
        float quantityV = CommonUtil.convertToFloat(getString(etQuantity), 0.0f);
        //历史累计数量
        float totalQuantityV = CommonUtil.convertToFloat(getString(tvBalanceQuantity), 0.0f);
        float newQuantityV = ArithUtil.add(quantityV, totalQuantityV);
        tvBalanceQuantity.setText(String.valueOf(newQuantityV));
        clearCommonUI(etQuantity);
    }

    @Override
    public void saveCollectedDataFail(String message) {
        showMessage(message);
        final float quantityV = CommonUtil.convertToFloat(getString(etQuantity), 0.0f);
        final float totalQuantity = CommonUtil.convertToFloat(getString(tvBalanceQuantity), 0.0f);
        tvBalanceQuantity.setText(String.valueOf(totalQuantity + quantityV));
        etQuantity.setText("");

    }

    /**
     * 拍照之前做必要的检查
     *
     * @return
     */
    protected boolean checkBeforeTakePhoto() {
        if (!etMaterialNum.isEnabled()) {
            showMessage("请先获取单据数据");
            return false;
        }
        if (TextUtils.isEmpty(mSelectedRefLineNum)) {
            showMessage("请先选择单据行");
            return false;
        }
        return true;
    }

    protected void toTakePhoto(String menuName, int takePhotoType) {
        if (!checkBeforeTakePhoto()) {
            return;
        }
        Intent intent = new Intent(mActivity, TakephotoActivity.class);
        Bundle bundle = new Bundle();
        //入库的子菜单的名称
        bundle.putString(Global.EXTRA_TITLE_KEY, "物资验收拍照-" + menuName);
        //拍照类型
        bundle.putInt(Global.EXTRA_TAKE_PHOTO_TYPE, takePhotoType);
        //单据号
        bundle.putString(Global.EXTRA_REF_NUM_KEY, mRefData.recordNum);

        bundle.putString(Global.EXTRA_BIZ_TYPE_KEY, mBizType);
        bundle.putString(Global.EXTRA_REF_TYPE_KEY, mRefType);
        //单据行号
        final int selectedLineNum = getIndexByLineNum(mSelectedRefLineNum);
        final RefDetailEntity lineData = getLineData(mSelectedRefLineNum);
        //行号
        bundle.putString(Global.EXTRA_REF_LINE_NUM_KEY, lineData.lineNum);
        //行id
        bundle.putString(Global.EXTRA_REF_LINE_ID_KEY, lineData.refLineId);
        bundle.putInt(Global.EXTRA_POSITION_KEY, selectedLineNum);
        bundle.putBoolean(Global.EXTRA_IS_LOCAL_KEY, false);
        intent.putExtras(bundle);
        mActivity.startActivity(intent);
    }

    @Override
    public void _onPause() {
        super._onPause();
        clearAllUI();
    }

    @Override
    public void retry(String retryAction) {
        switch (retryAction) {
            case Global.RETRY_SAVE_COLLECTION_DATA_ACTION:
                saveCollectedData();
                break;
        }
        super.retry(retryAction);
    }
}
