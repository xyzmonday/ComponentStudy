package com.richfit.barcodesystemproduct.setting;


import com.richfit.common_lib.lib_mvp.BaseView;
import com.richfit.domain.bean.UpdateEntity;

import zlc.season.rxdownload2.entity.DownloadStatus;

/**
 * Created by monday on 2016/11/29.
 */

public interface ISettingView extends BaseView {
    /**
     * 检查是否需要更新
     * @param info：服务器返回的更新信息
     */
    void checkAppVersion(UpdateEntity info);
    void getUpdateInfoFail(String message);

    //下载最新的app
    void prepareLoadApp();
    void loadLatestAppFail(String message);
    void showLoadAppProgress(DownloadStatus status);
    void loadAppComplete();

    //下载基础数据
    void onStartLoadBasicData(int maxProgress);
    void loadBasicDataProgress(float progress);
    void loadBasicDataFail(String message);
    void loadBasicDataComplete();

    void resetPasswordFail(String message);
    void resetPasswordComplete();

}
