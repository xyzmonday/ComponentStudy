package com.richfit.sdk_xxcx.material_liaoqian.detail;


import com.richfit.common_lib.lib_base_sdk.base_detail.IBaseDetailView;
import com.richfit.domain.bean.InventoryEntity;

import java.util.List;

/**
 * Created by monday on 2017/3/16.
 */

public interface ILQDetailView extends IBaseDetailView<InventoryEntity> {

    /**
     * 显示库存
     * @param list
     */
    void showInventory(List<InventoryEntity> list);
}
