package com.richfit.sdk_wzrk.base_asn_head;


import com.richfit.common_lib.lib_base_sdk.base_head.IBaseHeadView;
import com.richfit.domain.bean.SimpleEntity;
import com.richfit.domain.bean.WorkEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by monday on 2016/11/16.
 */

public interface IASNHeadView extends IBaseHeadView {
    /**
     * 显示工厂列表
     */
    void showWorks(ArrayList<WorkEntity> works);

    /**
     * 获取工厂列表失败
     */
    void loadWorksFail(String message);

    /**
     * 获取工厂完成
     */
    void loadWorksComplete();

    /**
     * 显示移动类型列表
     */
    void showMoveTypes(ArrayList<String> moveTypes);

    /**
     * 获取移动类型失败
     */
    void loadMoveTypesFail(String message);

    /**
     * 显示供应商列表
     */
    void showSuppliers(Map<String,List<SimpleEntity>> map);

    /**
     * 获取供应商失败
     */
    void loadSuppliersFail(String message);
    /**
     * 删除缓存
     * @param message
     */
    void deleteCacheSuccess(String message);
    void deleteCacheFail(String message);
}
