package com.richfit.common_lib.lib_tree_rv;


import android.content.Context;
import android.os.Handler;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.richfit.common_lib.R;
import com.richfit.common_lib.lib_adapter_rv.base.ItemViewDelegate;
import com.richfit.common_lib.lib_adapter_rv.base.ItemViewDelegateManager;
import com.richfit.common_lib.lib_adapter_rv.base.ViewHolder;
import com.richfit.common_lib.lib_interface.IAdapterState;
import com.richfit.common_lib.lib_interface.IOnItemMove;
import com.richfit.data.constant.Global;
import com.richfit.domain.bean.InventoryEntity;
import com.richfit.domain.bean.RefDetailEntity;
import com.richfit.domain.bean.TreeNode;

import java.util.List;

public abstract class MultiItemTypeTreeAdapter<T extends TreeNode> extends RecyclerView.Adapter<ViewHolder> {
    protected Context mContext;

    protected List<T> mVisibleNodes;
    protected List<T> mAllNodes;


    protected ItemViewDelegateManager mItemViewDelegateManager;
    protected OnItemClickListener mOnItemClickListener;
    protected IOnItemMove<T> mOnItemMove;
    protected IAdapterState mAdapterState;


    public MultiItemTypeTreeAdapter(Context context, List<T> allNodes) {
        mContext = context;
        mAllNodes = allNodes;
        mVisibleNodes = RecycleTreeViewHelper.filterVisibleNodes(allNodes);
        mItemViewDelegateManager = new ItemViewDelegateManager();
    }

    @Override
    public int getItemViewType(int position) {
        if (!useItemViewDelegateManager()) return super.getItemViewType(position);
        return mItemViewDelegateManager.getItemViewType(mVisibleNodes.get(position), position);
    }

    /**
     * 创建ViewHolder
     *
     * @param parent
     * @param viewType
     * @return
     */
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemViewDelegate itemViewDelegate = mItemViewDelegateManager.getItemViewDelegate(viewType);
        int layoutId = itemViewDelegate.getItemViewLayoutId();
        ViewHolder holder = ViewHolder.createViewHolder(mContext, parent, layoutId);
        //设置子节点的margin
        if (viewType == Global.CHILD_NODE_ITEM_TYPE || viewType == Global.CHILD_NODE_HEADER_TYPE) {
            setChildNodeMargin(holder.getConvertView());
        }
        setListener(parent, holder, viewType);
        return holder;
    }

    /**
     * 设置item的点击和长按监听
     *
     * @param parent
     * @param viewHolder
     * @param viewType
     */
    protected void setListener(final ViewGroup parent, final ViewHolder viewHolder, final int viewType) {

        viewHolder.getConvertView().setOnClickListener(v -> {
            final int position = viewHolder.getAdapterPosition();
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(v, viewHolder, position);
            }
        });

        viewHolder.getConvertView().setOnLongClickListener(v -> {
            if (mOnItemClickListener != null) {
                int position = viewHolder.getAdapterPosition();
                return mOnItemClickListener.onItemLongClick(v, viewHolder, position);
            }
            return false;
        });

        if (viewType == Global.PARENT_NODE_HEADER_TYPE || viewType == Global.PARENT_NODE_ITEM_TYPE) {
            //仅仅针对父节点
            viewHolder.setOnCheckedChangeListener(R.id.cb_choose, (buttonView, isChecked) -> {
                if (mOnItemMove != null) {
                    int position = viewHolder.getAdapterPosition();
                    T item = mVisibleNodes.get(position);
                    if(item instanceof RefDetailEntity) {
                        RefDetailEntity node = (RefDetailEntity) item;
                        node.isPatchTransfer = isChecked;
                    }
                }
            });
        }

        //注意这里不需要判断viewType类型，因为这里是先通过findViewById针对某一个view设置setOnClickListener
        setItemNodeEditAndDeleteListener(viewHolder);
    }

    /**
     * 绑定该ViewHolder的数据，由ItemDelegate去实现
     *
     * @param viewHolder
     * @param position
     */
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        onViewHolderBindInternal(viewHolder, getItemViewType(position));
        convert(viewHolder, mVisibleNodes.get(position));
        viewHolder.getConvertView().setOnClickListener(v -> {
            if (getItemViewType(position) == Global.PARENT_NODE_HEADER_TYPE) {
                expandOrCollapse(position);
            }
        });
    }


    public void convert(ViewHolder holder, T item) {
        mItemViewDelegateManager.convert(holder, item, holder.getAdapterPosition());
    }

    protected void onViewHolderBindInternal(ViewHolder holder, int viewType) {
        //不能修改子节点的数据明细，只能修改父节点和子节点的抬头
        if (mAdapterState != null) {
            mAdapterState.onBindViewHolder(holder, viewType);
        }
    }

    @Override
    public int getItemCount() {
        int itemCount = mVisibleNodes.size();
        return itemCount;
    }

    public void removeAllVisibleNodes() {
        if (mAllNodes != null && mVisibleNodes != null) {
            mAllNodes.clear();
            mVisibleNodes.clear();
            notifyDataSetChanged();
        }
    }


    public void addAll(List<T> data) {
        if (data == null || data.size() == 0)
            return;
        mAllNodes = data;
        mVisibleNodes = RecycleTreeViewHelper.filterVisibleNodes(data);
        notifyDataSetChanged();
    }

    public T getItem(int position) {
        if (mVisibleNodes != null && position >= 0 && position < mVisibleNodes.size()) {
            return mVisibleNodes.get(position);
        }
        return null;
    }

    public List<T> getDatas() {
        return mVisibleNodes;
    }

    public MultiItemTypeTreeAdapter addItemViewDelegate(ItemViewDelegate<T> itemViewDelegate) {
        mItemViewDelegateManager.addDelegate(itemViewDelegate);
        return this;
    }

    public MultiItemTypeTreeAdapter addItemViewDelegate(int viewType, ItemViewDelegate<T> itemViewDelegate) {
        mItemViewDelegateManager.addDelegate(viewType, itemViewDelegate);
        return this;
    }

    protected boolean useItemViewDelegateManager() {
        return mItemViewDelegateManager.getItemViewDelegateCount() > 0;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, RecyclerView.ViewHolder holder, int position);

        boolean onItemLongClick(View view, RecyclerView.ViewHolder holder, int position);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    public void setOnItemEditAndDeleteListener(IOnItemMove<T> listener) {
        mOnItemMove = listener;
    }

    public void setAdapterStateListener(IAdapterState listener) {
        mAdapterState = listener;
    }

    /**
     * 点击搜索或者展开
     *
     * @param position
     */
    protected void expandOrCollapse(int position) {
        if (position < 0 || position > mVisibleNodes.size() - 1)
            return;
        T n = mVisibleNodes.get(position);
        if (n != null) {
            if (n.isLeaf())
                return;
            n.setExpand(!n.isExpand());
            mVisibleNodes = RecycleTreeViewHelper.filterVisibleNodes(mAllNodes);
            notifyDataSetChanged();
        }
    }


    /**
     * 设置子节点的margin
     */
    private void setChildNodeMargin(View itemView) {
        RecyclerView.LayoutParams itemLayoutParams = (RecyclerView.LayoutParams) itemView.getLayoutParams();
        itemLayoutParams.setMargins((int) mContext.getResources().getDimension(R.dimen.child_left_padding), 0, 0, 0);
    }

    /**
     * 可编辑节点的删除和修改监听
     *
     * @param holder
     */
    private void setItemNodeEditAndDeleteListener(ViewHolder holder) {
        //设置点击和删除监听
        holder.setOnClickListener(R.id.item_edit, view -> {
            if (mOnItemMove != null) {
                int position = holder.getAdapterPosition();
                mOnItemMove.editNode(mVisibleNodes.get(position), position);
            }
        });
        holder.setOnClickListener(R.id.item_delete, view -> {
            int position = holder.getAdapterPosition();
            showDialogForNodeDelete(mVisibleNodes.get(position), position);
        });
    }

    /**
     * 提示用户是否删除该子节点
     *
     * @param node：将要删除的子节点
     * @param position：将要删除的子节点在显示列表的位置
     */
    protected void showDialogForNodeDelete(final T node, final int position) {
        Builder builder = new Builder(mContext);
        builder.setTitle("警告");
        builder.setIcon(R.mipmap.icon_warning);
        builder.setMessage("您真的要删除该条数据?点击确定删除.");
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        builder.setPositiveButton("确定", (dialog, which) -> {
            if (node != null) {
                if (mOnItemMove != null) {
                    mOnItemMove.deleteNode(node, position);
                }
            }
        });
        builder.show();
    }

    /**
     * 对于无参考的模块，直接删除该节点
     *
     * @param position:该节点在明细界面的位置
     */
    public void removeItemByPosition(int position) {
        if (mVisibleNodes != null && position >= 0 && position < mVisibleNodes.size()) {
            mAllNodes.remove(position);
            mVisibleNodes.remove(position);
            notifyItemRemoved(position);
        }
    }

    /**
     * 对于父子节点结构的明细，当户删除子节点后，如果需要修改父节点的某些字段信息。
     * 如果删除的子节点是最后一个子节点，那么需要自动删除子节点的头节点
     *
     * @param position:需要删除的节点位置。注意这里对于父子节点的明细只有子节点能够删除。 对于无参考的业务(包括有参考但是非父子节点的明细界面)，
     *                                                   节点删除有两类：第一类是直接删除该节点(对于这种情况直接调用removeItem方法即可)；
     *                                                   第二类是修改该节点的某些字段(比如说验收)。
     */
    public void removeNodeByPosition(final int position) {
        final TreeNode node = mVisibleNodes.get(position);
        final TreeNode parentNode = node.getParent();
        if (parentNode != null) {
            if (parentNode.getChildren().size() == 2 && parentNode.getChildren().get(0).getViewType() == Global.CHILD_NODE_HEADER_TYPE) {
                TreeNode childNode = parentNode.getChildren().get(0);
                int indexOf = mVisibleNodes.indexOf(childNode);
                mAllNodes.remove(childNode);
                mVisibleNodes.remove(childNode);
                notifyItemRemoved(indexOf);

            }
            //移除需要删除的节点
            mAllNodes.remove(node);
            mVisibleNodes.remove(node);
            //刷新子节点删除
            notifyItemRemoved(position);
        }
    }

    public void checkAllNodes(boolean isChecked) {
        for (T item : mVisibleNodes) {
            if (item.getViewType() == Global.PARENT_NODE_HEADER_TYPE ||
                    item.getViewType() == Global.PARENT_NODE_ITEM_TYPE) {
                //父节点
                if (item instanceof RefDetailEntity) {
                    RefDetailEntity node = (RefDetailEntity) item;
                    node.isPatchTransfer = isChecked;
                }
            }
        }
        notifyDataSetChanged();
    }

}
