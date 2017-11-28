package com.coder.baidubook;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by YandZD on 2017/11/28.
 */

public class ChaptersAdapter extends RecyclerView.Adapter {


    private ArrayList<ChaptersBean> mBeans;
    private ClickCallBack mClickCallBack;

    public ChaptersAdapter(ClickCallBack clickCallBack) {
        mClickCallBack = clickCallBack;
    }

    public void setData(ArrayList<ChaptersBean> beans) {
        mBeans = new ArrayList<>();
        mBeans.addAll(beans);
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chapters, parent, false);

        return new ViewHolder(rootView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((ViewHolder) holder).setUI(position);
    }

    @Override
    public int getItemCount() {
        if (mBeans == null) return 0;
        return mBeans.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView tv;

        public ViewHolder(View itemView) {
            super(itemView);
            tv = (TextView) itemView;
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = (int) v.getTag();
                    mClickCallBack.onClickCallBack(mBeans.get(position));
                }
            });
        }

        public void setUI(int position) {
            tv.setText(mBeans.get(position).getTitle());
            tv.setTag(position);
        }
    }

    interface ClickCallBack {
        void onClickCallBack(ChaptersBean bean);
    }


}
