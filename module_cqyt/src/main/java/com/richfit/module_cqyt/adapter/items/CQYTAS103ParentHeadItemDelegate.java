package com.richfit.module_cqyt.adapter.items;

import com.richfit.common_lib.lib_adapter_rv.base.ItemViewDelegate;
import com.richfit.common_lib.lib_adapter_rv.base.ViewHolder;
import com.richfit.data.constant.Global;
import com.richfit.domain.bean.RefDetailEntity;
import com.richfit.module_cqyt.R;

/**
 * Created by monday on 2017/7/4.
 */

public class CQYTAS103ParentHeadItemDelegate implements ItemViewDelegate<RefDetailEntity> {

    @Override
    public int getItemViewLayoutId() {
        return R.layout.cqyt_item_as103_parent_head;
    }

    @Override
    public boolean isForViewType(RefDetailEntity item, int position) {
        return item.getViewType() == Global.PARENT_NODE_HEADER_TYPE;
    }

    @Override
    public void convert(ViewHolder holder, RefDetailEntity item, int position) {
        holder.setText(R.id.refLineNum, item.lineNum);
        holder.setText(R.id.materialNum, item.materialNum);
        holder.setText(R.id.materialDesc, item.materialDesc);
        holder.setText(R.id.materialGroup, item.materialGroup);
        holder.setText(R.id.materialUnit, item.unit);
        //应发数量
        holder.setText(R.id.actQuantity, item.actQuantity);
        //累计数量
        holder.setText(R.id.totalQuantity, item.totalQuantity);
        //累计到货数量
        holder.setText(R.id.totalArrivalQuantity, item.totalArrivalQuantity);
        //累计件数
        holder.setText(R.id.cqyt_tv_total_quantity_custom, item.totalQuantityCustom);
        //工厂
        holder.setText(R.id.work, item.workCode);
        //库存地点
        holder.setText(R.id.inv, item.invCode);
    }
}
