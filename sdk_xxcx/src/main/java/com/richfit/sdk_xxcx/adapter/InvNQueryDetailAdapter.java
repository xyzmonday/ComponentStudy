package com.richfit.sdk_xxcx.adapter;

import android.content.Context;

import com.richfit.common_lib.lib_adapter_rv.base.ViewHolder;
import com.richfit.common_lib.lib_tree_rv.CommonTreeAdapter;
import com.richfit.domain.bean.InventoryEntity;
import com.richfit.sdk_xxcx.R;

import java.util.List;

/**
 * Created by monday on 2017/5/25.
 */

public class InvNQueryDetailAdapter extends CommonTreeAdapter<InventoryEntity> {

    public InvNQueryDetailAdapter(Context context, int layoutId, List<InventoryEntity> allNodes) {
        super(context, layoutId, allNodes);
    }

    @Override
    protected void convert(ViewHolder holder, InventoryEntity item, int position) {
        holder.setText(R.id.materialNum,item.materialNum)
                .setText(R.id.materialGroup,item.materialGroup)
                .setText(R.id.materialDesc,item.materialDesc)
                .setText(R.id.materialUnit,item.unit)
                .setText(R.id.batchFlag,item.batchFlag)
                .setText(R.id.work,item.workCode)
                .setText(R.id.inv,item.invCode)
                .setText(R.id.location,item.location)
                .setText(R.id.invQuantity,item.invQuantity);
    }
}
