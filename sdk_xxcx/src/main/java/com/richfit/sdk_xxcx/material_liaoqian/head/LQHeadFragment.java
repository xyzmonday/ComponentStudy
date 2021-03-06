package com.richfit.sdk_xxcx.material_liaoqian.head;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.richfit.common_lib.lib_mvp.BaseFragment;
import com.richfit.common_lib.widget.RichEditText;
import com.richfit.data.helper.CommonUtil;
import com.richfit.domain.bean.MaterialEntity;
import com.richfit.domain.bean.ReferenceEntity;
import com.richfit.sdk_xxcx.R;
import com.richfit.sdk_xxcx.R2;
import com.richfit.sdk_xxcx.material_liaoqian.head.imp.LQHeadPresenterImp;

import butterknife.BindView;

/**
 * 物资料签信息查询
 * Created by monday on 2017/3/16.
 */

public class LQHeadFragment extends BaseFragment<LQHeadPresenterImp>
        implements ILQHeadView {

    @BindView(R2.id.et_material_num)
    RichEditText etMaterialNum;
    @BindView(R2.id.tv_material_desc)
    TextView tvMaterialDesc;
    @BindView(R2.id.tv_material_group)
    TextView tvMaterialGroup;
    @BindView(R2.id.tv_material_unit)
    TextView tvMaterialUnit;
    @BindView(R2.id.tv_batch_flag)
    TextView tvBatchFlag;


    String workCode;
    String invCode;
    String location;
    String materialGroup;
    String materialNum;
    String materialUnit;
    String batchFlag;

    /**
     * 料签扫描响应。料签的规制是:
     * 1. 工厂
     * 2. 库存地点
     * 3. 仓位
     * 4. 物料组
     * 5. 物料编码
     * 6. 计量单位
     * 7. 批次
     *
     * @param type
     * @param list
     */
    @Override
    public void handleBarCodeScanResult(String type, String[] list) {
        if (list != null && list.length >= 7 && list.length <= 12) {
            workCode = list[0];
            invCode = list[1];
            location = list[2];
            materialGroup = list[3];
            materialNum = list[4];
            materialUnit = CommonUtil.toStringHex(list[5]);
            batchFlag = list[6];
            loadMaterialInfo(materialNum);
        }
    }

    @Override
    protected void initVariable(@Nullable Bundle savedInstanceState) {
        mRefData = null;
    }

    @Override
    protected void initView() {

    }

    @Override
    protected int getContentId() {
        return R.layout.xxcx_fragment_lq_header;
    }

    @Override
    protected void initPresenter() {
        mPresenter = new LQHeadPresenterImp(mActivity);
    }

    @Override
    protected void initEvent() {
        //手动输入物料编码查询物料信息
        etMaterialNum.setOnRichEditTouchListener((view, materialNum) -> {
            hideKeyboard(view);
            loadMaterialInfo(materialNum);
        });
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void initDataLazily() {

    }

    /**
     * 查询物料信息
     *
     * @param materialNum
     */
    private void loadMaterialInfo(String materialNum) {
        if (TextUtils.isEmpty(materialNum)) {
            showMessage("请输入物料条码");
            return;
        }
        clearAllUI();
        mPresenter.getMaterialInfo("01", materialNum);
    }

    @Override
    public void querySuccess(MaterialEntity entity) {
        if (entity != null) {
            etMaterialNum.setText(materialNum);
            tvMaterialDesc.setText(entity.materialDesc);
            tvMaterialGroup.setText(materialGroup);
            tvBatchFlag.setText(batchFlag);
            tvMaterialUnit.setText(entity.unit);
        }
    }

    @Override
    public void queryFail(String message) {
        showMessage(message);
    }

    private void clearAllUI() {
        clearCommonUI(tvMaterialDesc, tvMaterialGroup, tvMaterialUnit, tvBatchFlag);
    }

    @Override
    public void _onPause() {
        super._onPause();
        if (mRefData == null) {
            mRefData = new ReferenceEntity();
        }
        mRefData.workCode = workCode;
        mRefData.invCode = invCode;
        mRefData.location = location;
        mRefData.materialNum = materialNum;
        mRefData.materialGroup = materialGroup;
        mRefData.materialDesc = getString(tvMaterialDesc);
        mRefData.batchFlag = batchFlag;
    }

    @Override
    public void retry(String action) {
        loadMaterialInfo(getString(etMaterialNum));
        super.retry(action);
    }

    @Override
    public boolean isNeedShowFloatingButton() {
        return false;
    }
}
