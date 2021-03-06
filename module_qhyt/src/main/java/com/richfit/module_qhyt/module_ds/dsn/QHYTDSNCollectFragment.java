package com.richfit.module_qhyt.module_ds.dsn;


import android.text.TextUtils;

import com.richfit.domain.bean.ResultEntity;
import com.richfit.sdk_wzck.base_dsn_collect.BaseDSNCollectFragment;
import com.richfit.sdk_wzck.base_dsn_collect.imp.DSNCollectPresenterImp;

/**
 * Created by monday on 2017/3/27.
 */

public class QHYTDSNCollectFragment extends BaseDSNCollectFragment<DSNCollectPresenterImp> {


    @Override
    public void initPresenter() {
        mPresenter = new DSNCollectPresenterImp(mActivity);
    }

    @Override
    protected void initView() {

    }

    @Override
    public void initData() {

    }

    @Override
    public void initDataLazily() {
        if (mRefData == null) {
            showMessage("请先在抬头界面选择工厂");
            return;
        }

        if ("26".equals(mBizType) && TextUtils.isEmpty(mRefData.costCenter)) {
            showMessage("请先在抬头界面输入成本中心");
            return;
        }

        if ("27".equals(mBizType) && TextUtils.isEmpty(mRefData.projectNum)) {
            showMessage("请现在抬头界面输入项目编号");
            return;
        }
        super.initDataLazily();
    }

    @Override
    public ResultEntity provideResult() {
        ResultEntity result = super.provideResult();
        result.zzzdy9 = mRefData.zzzdy9;
        result.zzzxlb = mRefData.zzzxlb;
        result.zzzxnr = mRefData.zzzxnr;
        return result;
    }

}
