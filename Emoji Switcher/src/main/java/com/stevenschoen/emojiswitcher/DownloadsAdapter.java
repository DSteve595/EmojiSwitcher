package com.stevenschoen.emojiswitcher;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.stevenschoen.emojiswitcher.network.EmojiSetListing;

import java.util.List;

public class DownloadsAdapter extends RecyclerView.Adapter<DownloadsAdapter.DownloadHolder> {

    private List<EmojiSetListing> listings;

    private Callbacks callbacks;

    public DownloadsAdapter(List<EmojiSetListing> listings) {
        this.listings = listings;
    }

    @Override
    public DownloadHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.manage_downloads_listitem, parent, false);
        return new DownloadHolder(view);
    }

    @Override
    public void onBindViewHolder(DownloadHolder holder, final int position) {
        EmojiSetListing listing = getListing(position);

        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callbacks != null) {
                    callbacks.onClick(position);
                }
            }
        });
        holder.name.setText(listing.name);
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    public EmojiSetListing getListing(int position) {
        return listings.get(position);
    }

    public void setCallbacks(Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    public static class DownloadHolder extends RecyclerView.ViewHolder {
        public View root;
        public TextView name;

        public DownloadHolder(View itemView) {
            super(itemView);
            root = itemView;
            name = (TextView) itemView.findViewById(R.id.manage_downloads_listitem_name);
        }
    }

    public interface Callbacks {
        void onClick(int position);
    }
}
