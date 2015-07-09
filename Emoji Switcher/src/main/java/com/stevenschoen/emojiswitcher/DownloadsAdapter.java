package com.stevenschoen.emojiswitcher;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

public class DownloadsAdapter extends RecyclerView.Adapter<DownloadsAdapter.DownloadHolder> {

    private List<EmojiSet> sets;

    private Callbacks callbacks;

    public DownloadsAdapter(List<EmojiSet> sets) {
        this.sets = sets;
    }

    @Override
    public DownloadHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.manage_downloads_listitem, parent, false);
        return new DownloadHolder(view);
    }

    @Override
    public void onBindViewHolder(DownloadHolder holder, final int position) {
        final EmojiSet set = getSet(position);

        holder.name.setText(set.name);
        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callbacks != null) {
                    callbacks.onClickDelete(set);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return sets.size();
    }

    public EmojiSet getSet(int position) {
        return sets.get(position);
    }

    public void setCallbacks(Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    public static class DownloadHolder extends RecyclerView.ViewHolder {
        public View root;
        public TextView name;
        public ImageButton delete;

        public DownloadHolder(View itemView) {
            super(itemView);
            root = itemView;
            name = (TextView) itemView.findViewById(R.id.manage_downloads_listitem_name);
            delete = (ImageButton) itemView.findViewById(R.id.manage_downloads_listitem_delete);
        }
    }

    public interface Callbacks {
        void onClickDelete(EmojiSet set);
    }
}
