package com.rahmadarifan.android.custombottomsheetbehavior;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Ifan on 2/5/2018.
 */

public class ContentTabAdapter extends RecyclerView.Adapter<ContentTabAdapter.ContentTabHolder> {

    private List<String> exampleTexts;

    class ContentTabHolder extends RecyclerView.ViewHolder {

        private TextView tvContent;

        private ContentTabHolder(View view) {
            super(view);
            tvContent = view.findViewById(R.id.tv_content);
        }
    }

    ContentTabAdapter(List<String> exampleTexts) {
        this.exampleTexts = exampleTexts;
    }

    @Override
    public ContentTabHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_content_tab, parent, false);
        return new ContentTabHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ContentTabHolder holder, int position) {
        holder.tvContent.setText(exampleTexts.get(position));
    }

    @Override
    public int getItemCount() {
        return exampleTexts.size();
    }
}


