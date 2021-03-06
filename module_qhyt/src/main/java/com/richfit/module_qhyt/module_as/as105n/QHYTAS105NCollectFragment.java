package com.richfit.module_qhyt.module_as.as105n;

import android.text.TextUtils;

import com.richfit.domain.bean.RefDetailEntity;
import com.richfit.module_qhyt.R;
import com.richfit.sdk_wzrk.base_as_collect.BaseASCollectFragment;
import com.richfit.sdk_wzrk.base_as_collect.imp.ASCollectPresenterImp;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;

/**
 * 青海105物资入库数据采集界面。对于必检的物资不能使用105非必检入库。
 * 注意批次的enable=false,表示不检查批次输入，也就是即使在打开了批次管理的情况。
 * 下也不检查是否输入了批次
 * Created by monday on 2017/2/20.
 */

public class  QHYTAS105NCollectFragment extends BaseASCollectFragment<ASCollectPresenterImp> {

    @Override
    public void initPresenter() {
        mPresenter = new ASCollectPresenterImp(mActivity);
    }

    @Override
    public void initData() {

    }

    @Override
    public void initDataLazily() {
        super.initDataLazily();
        //这里让系统不能让用户手动输入批次，必须通过扫码的方式获取批次
        etBatchFlag.setEnabled(false);
    }

    /**
     * 绑定UI。注意重写的目的是判断必检物资不能做105非必检入库
     */
    @Override
    public void bindCommonCollectUI() {
        mSelectedRefLineNum = mRefLines.get(spRefLine.getSelectedItemPosition());
        RefDetailEntity lineData = getLineData(mSelectedRefLineNum);
        //如果是质检，那么不允许进行下面的操作
        isQmFlag = false;
        if (lineData != null && !TextUtils.isEmpty(lineData.qmFlag) && "X".equalsIgnoreCase(lineData.qmFlag)) {
            isQmFlag = true;
            showMessage("该物料是必检物资不能做105非必检入库");
            return;
        }
        super.bindCommonCollectUI();
        etBatchFlag.setEnabled(false);
    }

    @Override
    public boolean checkCollectedDataBeforeSave() {
        if (!isNLocation) {
            final String location = getString(etLocation);
            if (TextUtils.isEmpty(location)) {
                showMessage("请输入上架仓位");
                return false;
            }

            if (location.length() > 10) {
                showMessage("您输入的上架不合理");
                return false;
            }
        }
        if (isQmFlag) {
            showMessage("该物料是必检物资不能做105非必检入库");
            return false;
        }
        return super.checkCollectedDataBeforeSave();
    }


    @Override
    public void saveCollectedDataSuccess(String message) {
        super.saveCollectedDataSuccess(message);
        //强制修改enable
        etBatchFlag.setEnabled(false);
    }

    @Override
    protected int getOrgFlag() {
        return getInteger(R.integer.orgNorm);
    }


    /**
     * 通过物料编码和批次匹配单据明细的行。这里我们返回的所有行的insLot集合
     *
     * @param materialNum
     * @param batchFlag
     * @return
     */
    @Override
    protected Flowable<ArrayList<String>> matchMaterialInfo(final String materialNum, final String batchFlag) {
        if (mRefData == null || mRefData.billDetailList == null ||
                mRefData.billDetailList.size() == 0 || TextUtils.isEmpty(materialNum)) {
            return Flowable.error(new Throwable("请先获取单据信息"));
        }
        ArrayList<String> lineNums = new ArrayList<>();
        List<RefDetailEntity> list = mRefData.billDetailList;
        for (RefDetailEntity entity : list) {
            if (entity.batchManagerStatus) {
                final String lineNum105 = entity.lineNum105;
                //如果打开了批次，那么在看明细中是否有批次
                if (!TextUtils.isEmpty(entity.batchFlag) && !TextUtils.isEmpty(batchFlag)) {
                    if (materialNum.equalsIgnoreCase(entity.materialNum) &&
                            batchFlag.equalsIgnoreCase(entity.batchFlag) &&
                            !TextUtils.isEmpty(lineNum105))

                        lineNums.add(lineNum105);
                } else {
                    if (materialNum.equalsIgnoreCase(entity.materialNum) &&
                            !TextUtils.isEmpty(lineNum105))
                        lineNums.add(lineNum105);
                }
            } else {
                final String lineNum105 = entity.lineNum105;
                //如果明细中没有打开了批次管理,那么只匹配物料编码
                if (materialNum.equalsIgnoreCase(entity.materialNum) && !TextUtils.isEmpty(lineNum105))
                    lineNums.add(entity.lineNum105);

            }
        }
        if (lineNums.size() == 0) {
            return Flowable.error(new Throwable("未获取到匹配的物料"));
        }
        return Flowable.just(lineNums);
    }


    /**
     * 通过单据行的检验批得到该行在单据明细列表中的位置
     *
     * @param lineNum105:单据行的行号
     * @return 返回该行号对应的行明细在明细列表的索引
     */
    @Override
    protected int getIndexByLineNum(String lineNum105) {
        int index = -1;
        if (TextUtils.isEmpty(lineNum105))
            return index;

        if (mRefData == null || mRefData.billDetailList == null
                || mRefData.billDetailList.size() == 0)
            return index;

        for (RefDetailEntity detailEntity : mRefData.billDetailList) {
            index++;
            if (lineNum105.equalsIgnoreCase(detailEntity.lineNum105))
                break;

        }
        return index;
    }
}
