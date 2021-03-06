package com.richfit.sdk_wzck.base_dsn_head;


import com.richfit.common_lib.lib_base_sdk.base_head.IBaseHeadPresenter;

import java.util.Map;

/**
 * Created by monday on 2017/2/23.
 */

public interface IDSNHeadPresenter extends IBaseHeadPresenter<IDSNHeadView> {

    /**
     * 获取工厂列表
     * @param flag
     */
    void getWorks(int flag);

    void getAutoComList(String workCode, Map<String,Object> extraMap, String keyWord,
                        int defaultItemNum, int flag, String...keys);
    /**
     * 删除整单缓存
     * @param bizType：业务类型
     * @param userId:用户id
     */
    void deleteCollectionData(String refType, String bizType, String userId,
                              String companyCode);

    void getDictionaryData(String... codes);
}
