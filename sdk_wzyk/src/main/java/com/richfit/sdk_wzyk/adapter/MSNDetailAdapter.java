package com.richfit.sdk_wzyk.adapter;

import android.content.Context;

import com.richfit.common_lib.lib_adapter_rv.base.ViewHolder;
import com.richfit.common_lib.lib_tree_rv.CommonTreeAdapter;
import com.richfit.data.constant.Global;
import com.richfit.domain.bean.RefDetailEntity;
import com.richfit.sdk_wzyk.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by monday on 2017/3/19.
 */

public class MSNDetailAdapter extends CommonTreeAdapter<RefDetailEntity> {

    public MSNDetailAdapter(Context context, int layoutId, List<RefDetailEntity> allNodes) {
        super(context, layoutId, allNodes);
    }

    @Override
    protected void convert(ViewHolder holder, RefDetailEntity item, int position) {
        holder.setText(R.id.rowNum, String.valueOf(position + 1));
        holder.setText(R.id.materialNum, item.materialNum);
        holder.setText(R.id.materialDesc, item.materialDesc);
        holder.setText(R.id.materialGroup, item.materialGroup);
        holder.setText(R.id.materialUnit, item.unit);
        //发出库位
        holder.setText(R.id.sendInv, item.invCode);
        //发出仓位
        holder.setText(R.id.sendLoc, item.location);
        //发出批次
        holder.setText(R.id.sendBatchFlag, Global.DEFAULT_BATCHFLAG.equalsIgnoreCase(item.batchFlag) ? "" : item.batchFlag);
        //移库数量
        holder.setText(R.id.quantity, item.quantity);
        //接收仓位
        holder.setText(R.id.recLoc, item.recLocation);
        //接收批次
        holder.setText(R.id.recBatchFlag, Global.DEFAULT_BATCHFLAG.equalsIgnoreCase(item.recBatchFlag) ? "" : item.recBatchFlag);
        //特殊库存
        holder.setText(R.id.specialInvFlag, item.specialInvFlag)
                .setText(R.id.specialInvNum, item.specialInvNum)
                .setText(R.id.tv_location_type, item.locationType)
                .setText(R.id.tv_rec_location_type,item.recLocationType);
    }

    /**
     * 获取发出仓位和接收仓位列表
     *
     * @param materialNum:物料编码
     * @param invId:库存地点id
     * @param position:需要修改的子节点的位置
     * @param flag:0表示获取发出仓位,1:表示获取接收仓位
     * @return
     */
    public ArrayList<String> getLocations(String materialNum, String invId, int position, int flag) {
        ArrayList<String> locations = new ArrayList<>();
        for (int i = 0; i < mVisibleNodes.size(); i++) {
            if (i == position)
                continue;
            RefDetailEntity node = mVisibleNodes.get(i);
            if (!node.materialNum.equals(materialNum) && !node.invId.equals(invId)) {
                locations.add(flag == 0 ? node.locationCombine : node.recLocation);
            }
        }
        return locations;
    }
}
