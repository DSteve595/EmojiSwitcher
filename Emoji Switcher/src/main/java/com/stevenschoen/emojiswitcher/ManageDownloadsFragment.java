package com.stevenschoen.emojiswitcher;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.stevenschoen.emojiswitcher.network.EmojiSetListing;
import com.stevenschoen.emojiswitcher.network.EmojiSetsResponse;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import org.solovyev.android.views.llm.LinearLayoutManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.app.support.RxDialogFragment;
import rx.android.lifecycle.LifecycleObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;

public class ManageDownloadsFragment extends RxDialogFragment {

    private BehaviorSubject<Void> downloadedEmojiSetsSubject = BehaviorSubject.create();
    private ArrayList<EmojiSet> downloadedEmojiSets = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LifecycleObservable.bindFragmentLifecycle(lifecycle(),
                EmojiSwitcherUtils.getNetworkInterface(getActivity()).getEmojiSets())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<EmojiSetsResponse>() {
                    @Override
                    public void call(final EmojiSetsResponse emojiSetsResponse) {
                        Observable.create(new Observable.OnSubscribe<Void>() {
                            @Override
                            public void call(Subscriber<? super Void> subscriber) {
                                File setsFolder = new File(getActivity().getFilesDir() + File.separator + "emojisets");
                                File[] setFiles = setsFolder.listFiles();
                                List<String> fileMd5s = new ArrayList<>();
                                if (setFiles != null) {
                                    for (File file : setFiles) {
                                        try {
                                            fileMd5s.add(Files.hash(file, Hashing.md5()).toString());
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    for (EmojiSetListing listing : emojiSetsResponse.emojiSets) {
                                        for (int i = 0; i < fileMd5s.size(); i++) {
                                            if (fileMd5s.get(i).equals(listing.md5)) {
                                                downloadedEmojiSets.add(new EmojiSet(listing, setFiles[i]));
                                                break;
                                            }
                                        }
                                    }
                                }

                                downloadedEmojiSetsSubject.onNext(null);
                            }
                        }).subscribe();
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
        downloadsView.setLayoutManager(new LinearLayoutManager(downloadsView, android.support.v7.widget.LinearLayoutManager.VERTICAL, false));
        downloadsView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getActivity())
                .marginResId(R.dimen.abc_list_item_padding_horizontal_material)
                .build());
        final DownloadsAdapter downloadsAdapter = new DownloadsAdapter(downloadedEmojiSets);
        downloadsAdapter.setCallbacks(new DownloadsAdapter.Callbacks() {
            @Override
            public void onClickDelete(EmojiSet set) {
                set.path.delete();
                downloadedEmojiSets.remove(set);
                downloadsAdapter.notifyDataSetChanged();
            }
        });
        downloadsView.setAdapter(downloadsAdapter);

        LifecycleObservable.bindFragmentLifecycle(lifecycle(),
                downloadedEmojiSetsSubject)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Void>() {
                    @Override
                    public void call(Void nothing) {
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
                .setTitle(R.string.downloaded_emoji)
                .setPositiveButton("OK", null)
                .create();
    }
}
