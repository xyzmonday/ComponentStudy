package com.richfit.module_mcq.module_xxcx;

import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import com.richfit.common_lib.lib_adapter.SimpleAdapter;
import com.richfit.common_lib.utils.UiUtil;
import com.richfit.data.constant.Global;
import com.richfit.domain.bean.InventoryQueryParam;
import com.richfit.domain.bean.SimpleEntity;
import com.richfit.module_mcq.R;
import com.richfit.sdk_xxcx.inventory_query_n.header.InvNQueryHeaderFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by monday on 2017/8/28.
 */

public class MCQInvNHeadFragment extends InvNQueryHeaderFragment {

    //仓位
    EditText etLocation;

    @Override
    public void handleBarCodeScanResult(String type, String[] list) {
        if (list != null && list.length == 2 && isOpenLocationType) {
            String location = list[0];
            String locationType = list[Global.LOCATION_TYPE_POS];
            clearCommonUI(etLocation);
            etLocation.setText(location);
            UiUtil.setSelectionForSimpleSp(mLocationTypes, locationType, spLocationType);
        } else if (list != null && list.length == 1) {
            String location = list[0];
            clearCommonUI(etLocation);
            etLocation.setText(location);
        }
    }

    @Override
    public int getContentId() {
        return R.layout.mcq_fragment_invn_head;
    }


    @Override
    public void initView() {
        super.initView();
        llMaterialClass.setVisibility(View.GONE);
        llMaterialDesc.setVisibility(View.GONE);
        etLocation = mView.findViewById(R.id.et_location);
    }


    @Override
    public void _onPause() {
        super._onPause();
        if (mRefData != null) {
            mRefData.location = getString(etLocation);
        }
    }
}
