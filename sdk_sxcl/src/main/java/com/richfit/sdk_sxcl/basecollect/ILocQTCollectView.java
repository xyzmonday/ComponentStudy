package com.richfit.sdk_sxcl.basecollect;

import android.support.annotation.NonNull;

import com.richfit.common_lib.lib_base_sdk.base_collect.IBaseCollectView;
import com.richfit.domain.bean.InventoryEntity;
import com.richfit.domain.bean.RefDetailEntity;
import com.richfit.domain.bean.SimpleEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by monday on 2017/5/26.
 */

public interface ILocQTCollectView extends IBaseCollectView {

    /**
     * 获取匹配的物料信息
     *
     * @param materialNum：物料号
     * @param batchFlag：批次
     */
    void loadMaterialInfo(@NonNull String materialNum, @NonNull String batchFlag);
    /**
     * 初始化单据行适配器
     */
    void setupRefLineAdapter(ArrayList<String> refLines);

    /**
     * 为数据采集界面的UI绑定数据
     */
    void bindCommonCollectUI();

    /**
     * 显示库存
     * @param list
     */
    void showInventory(List<InventoryEntity> list);
    void loadInventoryFail(String message);

    /**
     * 获取缓存成功
     * @param cache
     * @param batchFlag
     * @param location
     */
    void onBindCache(RefDetailEntity cache, String batchFlag, String location);
    void loadCacheSuccess();
    void loadCacheFail(String message);
    /**
     * 加载上架仓位
     */
    void loadLocationList();

    void checkLocationFail(String message);
    void checkLocationSuccess(String batchFlag, String location);

    /**
     * 显示库存
     * @param list
     */
    void showLocationList(List<String> list);
    void loadLocationListComplete();
    void loadLocationListFail(String message);
}
