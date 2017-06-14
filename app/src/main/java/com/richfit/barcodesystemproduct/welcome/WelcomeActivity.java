package com.richfit.barcodesystemproduct.welcome;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.jakewharton.rxbinding2.view.RxView;
import com.richfit.barcodesystemproduct.BuildConfig;
import com.richfit.barcodesystemproduct.R;
import com.richfit.common_lib.lib_mvp.BaseActivity;
import com.richfit.common_lib.utils.AppCompat;
import com.richfit.common_lib.utils.UiUtil;
import com.richfit.data.constant.Global;

import butterknife.BindView;

/**
 * Created by monday on 2016/11/8.
 */

public class WelcomeActivity extends BaseActivity<WelcomePresenterImp> implements WelcomeContract.View {

    @BindView(R.id.btn_online_mode)
    Button btnOnlineMode;
    @BindView(R.id.btn_offline_mode)
    Button btnOfflineMode;
    @BindView(R.id.reveal_view)
    View revealView;

    int mode;

    @Override
    protected int getContentId() {
        return R.layout.activity_welcome;
    }

    @Override
    protected void initPresenter() {
        mPresenter = new WelcomePresenterImp(this);
    }


    @Override
    protected void initVariables() {

    }

    @Override
    protected void initViews() {

    }

    @Override
    protected void initData(Bundle savedInstanceState) {

    }

    @Override
    public void initEvent() {
        switch (BuildConfig.APP_NAME) {
            case Global.QINGYANG:
                btnOfflineMode.setVisibility(View.GONE);
                break;
        }
        RxView.clicks(btnOnlineMode)
                .subscribe(a -> {
                    mode = Global.ONLINE_MODE;
                    mPresenter.loadFragmentConfig(Global.COMPANY_ID, BuildConfig.CONFIG_FILE_NAME);
                });

        RxView.clicks(btnOfflineMode)
                .subscribe(a -> {
                    mode = Global.OFFLINE_MODE;
                    mPresenter.loadFragmentConfig(Global.COMPANY_ID, BuildConfig.CONFIG_FILE_NAME);
                });
    }


    @Override
    public void loadFragmentConfigSuccess() {
        toHome();
    }

    @Override
    public void loadFragmentConfigFail(String message){
        showMessage(message);
        toHome();
    }


    /**
     * 显示动画，在动画结束后直接跳转到home页面
     */
    private void toHome() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final View view = Global.ONLINE_MODE == mode ? btnOnlineMode : btnOfflineMode;
            int primaryColor = AppCompat.getColor(R.color.colorPrimary, this);
            int[] location = new int[2];
            revealView.setBackgroundColor(primaryColor);
            view.getLocationOnScreen(location);
            int cx = (location[0] + (view.getWidth() / 2));
            int cy = location[1] + (UiUtil.getStatusBarHeight(this) / 2);
            hideNavigationStatus();
            UiUtil.showRevealEffect(revealView, cx, cy, new RevealAnimationListener());
        } else {
            mPresenter.toHome(mode);
        }
    }

    private void hideNavigationStatus() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);
    }

    protected class RevealAnimationListener extends AnimatorListenerAdapter {
        @Override
        public void onAnimationEnd(Animator animation) {
            mPresenter.toHome(mode);
        }
    }
}
