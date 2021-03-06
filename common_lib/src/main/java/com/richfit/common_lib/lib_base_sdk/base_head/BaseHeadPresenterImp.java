package com.richfit.common_lib.lib_base_sdk.base_head;

import android.content.Context;
import android.util.Log;

import com.richfit.common_lib.lib_eventbus.Event;
import com.richfit.common_lib.lib_eventbus.EventBusUtil;
import com.richfit.common_lib.lib_eventbus.EventCode;
import com.richfit.common_lib.lib_mvp.BasePresenter;
import com.richfit.common_lib.lib_rx.RxSubscriber;
import com.richfit.data.constant.Global;
import com.richfit.data.helper.TransformerHelper;
import com.richfit.domain.bean.ResultEntity;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Created by monday on 2017/3/18.
 */

public class BaseHeadPresenterImp<V extends IBaseHeadView> extends BasePresenter<V>
        implements IBaseHeadPresenter<V> {

    protected V mView;

    public BaseHeadPresenterImp(Context context) {
        super(context);
        EventBusUtil.register(this);
    }


    @Override
    protected void onStart() {
        mView = getView();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onClearHeadUI(Event<Boolean> event) {
        if (event.getData() && event.getCode() == EventCode.EVENT_CLEARHEAUI) {
            mView = getView();
            if (mView != null) {
                mView.clearAllUIAfterSubmitSuccess();
            }
        }
    }

    @Override
    public void detachView() {
        EventBusUtil.unregister(this);
        super.detachView();
    }

    @Override
    public void uploadEditedHeadData(ResultEntity resultEntity) {
        mView = getView();
        if (resultEntity == null) {
            mView.uploadEditedHeadDataFail("请先获取修改的数据");
            return;
        }
        mRepository.uploadEditedHeadData(resultEntity)
                .compose(TransformerHelper.io2main())
                .subscribeWith(new RxSubscriber<String>(mContext, "正在保存抬头修改的数据") {
                    @Override
                    public void _onNext(String s) {

                    }

                    @Override
                    public void _onNetWorkConnectError(String message) {
                        if (mView != null) {
                            mView.networkConnectError(Global.RETRY_SAVE_COLLECTION_DATA_ACTION);
                        }
                    }

                    @Override
                    public void _onCommonError(String message) {
                        if (mView != null) {
                            mView.uploadEditedHeadDataFail(message);
                        }
                    }

                    @Override
                    public void _onServerError(String code, String message) {
                        if (mView != null) {
                            mView.uploadEditedHeadDataFail(message);
                        }
                    }

                    @Override
                    public void _onComplete() {
                        if (mView != null) {
                            mView.uploadEditedHeadComplete();
                        }
                    }
                });
    }


}
