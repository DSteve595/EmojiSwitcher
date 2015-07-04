package com.stevenschoen.emojiswitcher;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.stevenschoen.emojiswitcher.network.EmojiSetListing;
import com.stevenschoen.emojiswitcher.network.EmojiSetsResponse;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import org.solovyev.android.views.llm.LinearLayoutManager;

import java.util.ArrayList;

import rx.android.app.support.RxDialogFragment;
import rx.android.lifecycle.LifecycleObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;

public class ManageDownloadsFragment extends RxDialogFragment {

    private BehaviorSubject<EmojiSetsResponse> emojiSetsResponseObservable = BehaviorSubject.create();
    private ArrayList<EmojiSetListing> emojiSetListings = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LifecycleObservable.bindFragmentLifecycle(lifecycle(),
                EmojiSwitcherUtils.getNetworkInterface(getActivity()).getEmojiSets())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<EmojiSetsResponse>() {
                    @Override
                    public void call(EmojiSetsResponse emojiSetsResponse) {
                        emojiSetsResponseObservable.onNext(emojiSetsResponse);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.manage_downloads, null);

        RecyclerView downloadsView = (RecyclerView) view.findViewById(R.id.manage_downloads_list);
        downloadsView.setLayoutManager(new LinearLayoutManager(getActivity(), android.support.v7.widget.LinearLayoutManager.VERTICAL, false));
        downloadsView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getActivity())
                .marginResId(R.dimen.abc_list_item_padding_horizontal_material)
                .build());
        final DownloadsAdapter downloadsAdapter = new DownloadsAdapter(emojiSetListings);
        downloadsView.setAdapter(downloadsAdapter);

        LifecycleObservable.bindFragmentLifecycle(lifecycle(),
                emojiSetsResponseObservable)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<EmojiSetsResponse>() {
                    @Override
                    public void call(EmojiSetsResponse emojiSetsResponse) {
                        emojiSetListings.clear();
                        emojiSetListings.addAll(emojiSetsResponse.emojiSets);
                        downloadsAdapter.notifyDataSetChanged();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setTitle(R.string.manage_downloads)
                .setPositiveButton("OK", null)
                .create();
    }
}
