package com.richfit.barcodesystemproduct.home;

import com.richfit.common_lib.lib_mvp.BaseView;
import com.richfit.common_lib.lib_mvp.IPresenter;
import com.richfit.domain.bean.MenuNode;

import java.util.ArrayList;

/**
 * Created by monday on 2016/11/7.
 */

public interface HomeContract {

    interface View extends BaseView {
        void initModulesSuccess(ArrayList<MenuNode> modules);
        void initModelsFail(String message);

    }

    interface Presenter extends IPresenter<View> {
        /*初始化每一个模块的基本配置*/
        void setupModule(String loginId);
        void changeMode(String loginId, int mode);

    }
}