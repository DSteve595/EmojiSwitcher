package com.stevenschoen.emojiswitcher;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.stericson.RootTools.RootTools;
import com.stevenschoen.emojiswitcher.network.EmojiSetListing;
import com.trello.rxlifecycle.components.support.RxDialogFragment;

import org.solovyev.android.views.llm.LinearLayoutManager;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class InstallEmojiFragment extends RxDialogFragment {

    private Callbacks callbacks;

    private EmojiSetListing listing;

    private EmojiSwitcherUtils emojiSwitcherUtils = new EmojiSwitcherUtils();
    private Observable<EmojiSwitcherUtils.InstallProgress> installProgress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        listing = getArguments().getParcelable("listing");

        installProgress = emojiSwitcherUtils.installEmojiSet(getActivity(), listing);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.install_emoji, null);

        RecyclerView stagesView = (RecyclerView) view.findViewById(R.id.install_emoji_stages_list);
        stagesView.setLayoutManager(new LinearLayoutManager(stagesView, android.support.v7.widget.LinearLayoutManager.VERTICAL, false));
        EmojiSet emojiSet = new EmojiSet(listing, new File(EmojiSwitcherUtils.filePath(getActivity(), listing)));
        final InstallStageAdapter stageAdapter = new InstallStageAdapter(
                EmojiSwitcherUtils.InstallProgress.hasHtcStage(),
                EmojiSwitcherUtils.InstallProgress.hasDownloadStage(emojiSet),
                EmojiSwitcherUtils.InstallProgress.hasBackupStage(getActivity()));
        stagesView.setAdapter(stageAdapter);

        final View rebootHolder = view.findViewById(R.id.install_emoji_reboot_holder);
        rebootHolder.setVisibility(View.GONE);

        Button rebootView = (Button) rebootHolder.findViewById(R.id.install_emoji_reboot_reboot);
        rebootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RootTools.restartAndroid();
            }
        });

        Button laterView = (Button) rebootHolder.findViewById(R.id.install_emoji_reboot_later);

        installProgress
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(this.<EmojiSwitcherUtils.InstallProgress>bindToLifecycle())
                .subscribe(new Observer<EmojiSwitcherUtils.InstallProgress>() {
                    @Override
                    public void onCompleted() {
                        if (callbacks != null) {
                            callbacks.refreshSystemEmoji();
                        }
                        rebootHolder.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                        if (callbacks != null) {
                            callbacks.refreshSystemEmoji();
                        }
                    }

                    @Override
                    public void onNext(EmojiSwitcherUtils.InstallProgress installProgress) {
                        stageAdapter.updateProgress(installProgress);
                    }
                });

        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setTitle(getString(R.string.installing_x_emoji, listing.name))
                .setCancelable(false)
                .create();
        dialog.setCanceledOnTouchOutside(false);

        laterView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        return dialog;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        callbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        callbacks = null;

        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    public interface Callbacks {
        void refreshSystemEmoji();
    }

    private class InstallStageAdapter extends RecyclerView.Adapter<InstallStageAdapter.StageHolder> {

        private List<EmojiSwitcherUtils.InstallProgress.Stage> stages = new LinkedList<>(Arrays.asList(EmojiSwitcherUtils.InstallProgress.Stage.values()));

        private EmojiSwitcherUtils.InstallProgress installProgress;

        public InstallStageAdapter(boolean hasHtcStage, boolean hasDownloadStage, boolean hasBackupStage) {
            if (!hasHtcStage) stages.remove(EmojiSwitcherUtils.InstallProgress.Stage.HtcFix);
            if (!hasDownloadStage) stages.remove(EmojiSwitcherUtils.InstallProgress.Stage.Download);
            if (!hasBackupStage) stages.remove(EmojiSwitcherUtils.InstallProgress.Stage.Backup);
			setHasStableIds(true);
        }

        public void updateProgress(EmojiSwitcherUtils.InstallProgress installProgress) {
            EmojiSwitcherUtils.InstallProgress oldProgress = this.installProgress;
            this.installProgress = installProgress;
            if (oldProgress == null ||
                    oldProgress.currentStage != installProgress.currentStage ||
                    oldProgress.currentStageProgress != installProgress.currentStageProgress) {
				notifyDataSetChanged();
            }
        }

        @Override
        public StageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.install_emoji_stages_listitem, parent, false);
            return new StageHolder(view);
        }

        @Override
        public void onBindViewHolder(StageHolder holder, int position) {
			EmojiSwitcherUtils.InstallProgress.Stage stage = stages.get(position);

            holder.title.setText(String.format("%d. %s", (position + 1), stage.getTitle()));
            if (stage == installProgress.currentStage) {
                holder.title.setEnabled(true);
                if (stage == EmojiSwitcherUtils.InstallProgress.Stage.Done) {
                    holder.loading.setVisibility(View.INVISIBLE);
                } else {
					if (installProgress.currentStage.hasPercentProgress()) {
						holder.loading.setVisibility(View.GONE);
						holder.percent.setVisibility(View.VISIBLE);
						holder.percent.setText(String.format("%d%%", installProgress.currentStageProgress));
					} else {
						holder.percent.setVisibility(View.INVISIBLE);
						holder.loading.setVisibility(View.VISIBLE);
					}
                }
            } else {
                holder.title.setEnabled(false);
                holder.loading.setVisibility(View.INVISIBLE);
				holder.percent.setVisibility(View.INVISIBLE);
            }
        }

		@Override
		public long getItemId(int position) {
			return stages.get(position).ordinal();
		}

		@Override
        public int getItemCount() {
            if (installProgress == null) {
                return 0;
            }

            return stages.size();
        }

        class StageHolder extends RecyclerView.ViewHolder {
            private View root;
            private TextView title;
            private ProgressBar loading;
            private TextView percent;

            public StageHolder(View itemView) {
                super(itemView);
                root = itemView;
                title = (TextView) itemView.findViewById(R.id.install_emoji_stages_listitem_title);
                loading = (ProgressBar) itemView.findViewById(R.id.install_emoji_stages_listitem_loading);
                percent = (TextView) itemView.findViewById(R.id.install_emoji_stages_listitem_percent);
            }
        }
    }
}
