package com.richfit.module_qysh.module_sxcl;

import com.richfit.sdk_sxcl.basehead.LocQTHeadFragment;

/**
 * Created by monday on 2017/9/22.
 */

public class QYSHLocHeadFragment extends LocQTHeadFragment {


    @Override
    public void _onPause() {
        super._onPause();
        if(mRefData != null) {
            mRefData.specialInvFlag = "K";
        }
    }
}
