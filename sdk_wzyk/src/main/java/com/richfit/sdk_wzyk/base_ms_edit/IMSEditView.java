package com.richfit.sdk_wzyk.base_ms_edit;

import com.richfit.common_lib.lib_base_sdk.base_edit.IBaseEditView;
import com.richfit.domain.bean.InventoryEntity;
import com.richfit.domain.bean.RefDetailEntity;
import com.richfit.domain.bean.SimpleEntity;

import java.util.List;
import java.util.Map;

/**
 * Created by monday on 2017/2/13.
 */

public interface IMSEditView extends IBaseEditView {
    /**
     * 显示库存
     *
     * @param list
     */
    void showInventory(List<InventoryEntity> list);

    void loadInventoryFail(String message);

    /**
     * 获取缓存成功
     *
     * @param cache
     * @param batchFlag
     * @param location
     */
    void onBindCache(RefDetailEntity cache, String batchFlag, String location);

    void loadCacheFail(String message);
}