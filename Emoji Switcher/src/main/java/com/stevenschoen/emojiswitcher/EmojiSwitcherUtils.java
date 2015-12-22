package com.stevenschoen.emojiswitcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootShell.execution.Shell;
import com.stericson.RootTools.RootTools;
import com.stevenschoen.emojiswitcher.network.EmojiSetListing;
import com.stevenschoen.emojiswitcher.network.NetworkInterface;
import com.thin.downloadmanager.DownloadRequest;
import com.thin.downloadmanager.DownloadStatusListener;
import com.thin.downloadmanager.ThinDownloadManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class EmojiSwitcherUtils {
	public static final String GOOGLE_ADS_UNITID = "ca-app-pub-9259165898539273/5321636233";
	public static final String PLAY_LICENSING_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsV" +
			"ARwtabK8aSCIf5Pel+rFPZWgDMbKeiWAuKHex7T7PWme78lcFCnadcj94sCpBeYx65fVO6LPeLMD+8nR1WlX" +
			"1fUiVbCG1efGka+OGpKJ1/BnVRwE4wC88ijXu7UwP2Lp0lP4Z/7mUs9oR/5VbYsNTwukzyg8U8VOBa2PA4/e" +
			"uSo8YHTY4toD8lvyzqRzmGGW+yoWdKNbKx/6yZE3O7cH/S8rLbTMeESwuJ0h+E8A9moKOkPFEwPVXx9hMXIU" +
			"XyPGOw/f05Bn0GIOhXcVwh7cp2KjhRHzwTPBUqzZymBY7QzKl8bw8yXr/Q6U4yCdQajHhh3g64PM4YB13peo" +
			"amCQIDAQAB";
	public static final String SKU_REMOVEADS = "emojiswitcher_removeads";

    private static final String systemFontsPath = "/system/fonts/";
    private static final String systemEmojiFilePath = systemFontsPath + "NotoColorEmoji.ttf";
    private static final String htcFilePath = systemFontsPath + "AndroidEmoji-htc.ttf";
    private static final String htcBackupFilePath = htcFilePath + ".bak";

    public static NetworkInterface getNetworkInterface(Context context) {
        return ((EmojiSwitcherApplication) context.getApplicationContext()).getNetworkInterface();
    }

    public static String systemEmojiBackupFilePath(Context context) {
        return context.getFilesDir() + File.separator + "backup.ttf";
    }

	public static boolean isRootReady() {
		return (RootTools.isRootAvailable() && RootTools.isAccessGiven());
	}

    public static boolean isHtc() {
        return Build.MANUFACTURER.toLowerCase(Locale.ENGLISH).equals("htc");
    }

    public static void applyHtcFix() {
        RootTools.copyFile(htcFilePath, htcBackupFilePath, true, true);
        RootTools.deleteFileOrDirectory(htcFilePath, false);
    }

    public static void undoHtcFix() {
        RootTools.copyFile(htcBackupFilePath, htcFilePath, true, true);
        RootTools.deleteFileOrDirectory(htcBackupFilePath, false);
    }

	public Observable<InstallProgress> installEmojiSet(final Context context, final EmojiSetListing listing) {
        return Observable.create(new Observable.OnSubscribe<InstallProgress>() {
            @Override
            public void call(final Subscriber<? super InstallProgress> subscriber) {
                try {
                    final EmojiSet emojiSet = new EmojiSet(listing, new File(filePath(context, listing)));

                    if (InstallProgress.hasHtcStage()) {
                        InstallProgress progress = new InstallProgress();
                        progress.currentStage = InstallProgress.Stage.HtcFix;
                        progress.currentStageProgress = 0;
                        subscriber.onNext(progress);
                        applyHtcFix();
                        progress.currentStageProgress = 100;
                        subscriber.onNext(progress);
                    }

                    final PublishSubject<Boolean> fileReadySubject = PublishSubject.create();
                    fileReadySubject.observeOn(Schedulers.io()).subscribe(new Subscriber<Boolean>() {
                        @Override
                        public void onCompleted() { }

                        @Override
                        public void onError(Throwable e) {
                            subscriber.onError(e);
                        }

                        @Override
                        public void onNext(Boolean ready) {
                            if (ready) {
                                if (InstallProgress.hasBackupStage(context)) {
                                    InstallProgress progress = new InstallProgress();
                                    progress.currentStage = InstallProgress.Stage.Backup;
                                    subscriber.onNext(progress);
                                    File systemEmojiSetFile = new File(systemEmojiFilePath);
                                    File backupFile = new File(systemEmojiBackupFilePath(context));
                                    RootTools.copyFile(systemEmojiSetFile.getAbsolutePath(),
                                            backupFile.getAbsolutePath(), true, false);
                                }

                                InstallProgress progress = new InstallProgress();
                                progress.currentStage = InstallProgress.Stage.Install;
                                subscriber.onNext(progress);

                                RootTools.copyFile(emojiSet.path.getAbsolutePath(),
                                        new File(systemEmojiFilePath).getAbsolutePath(), true, false);
                                try {
                                    applyPermissions("644", systemEmojiFilePath);
                                } catch (Exception e) {
                                    subscriber.onError(e);
                                }

                                progress = new InstallProgress();
                                progress.currentStage = InstallProgress.Stage.Done;
                                subscriber.onNext(progress);
                                subscriber.onCompleted();
                                unsubscribe();
                            }
                        }
                    });

                    if (InstallProgress.hasDownloadStage(emojiSet)) {
                        InstallProgress progress = new InstallProgress();
                        progress.currentStage = InstallProgress.Stage.Download;
                        subscriber.onNext(progress);

                        String path = emojiSet.path.getAbsolutePath();
                        emojiSet.path = new File(path);
                        FileUtils.deleteQuietly(emojiSet.path);
                        emojiSet.path.mkdirs();

                        ThinDownloadManager downloadManager = new ThinDownloadManager();
                        DownloadRequest request = new DownloadRequest(Uri.parse(listing.url));
                        request.setDestinationURI(Uri.parse(path));
                        final PublishSubject<Integer> downloadProgressSubject = PublishSubject.create();
                        downloadProgressSubject.throttleFirst(250, TimeUnit.MILLISECONDS)
                                .subscribe(new Action1<Integer>() {
                                    @Override
                                    public void call(Integer percent) {
                                        InstallProgress progress = new InstallProgress();
                                        progress.currentStage = InstallProgress.Stage.Download;
                                        progress.currentStageProgress = percent;
                                        subscriber.onNext(progress);
                                    }
                                }, new Action1<Throwable>() {
                                    @Override
                                    public void call(Throwable throwable) {
                                        subscriber.onError(throwable);
                                    }
                                });
                        request.setDownloadListener(new DownloadStatusListener() {
                            @Override
                            public void onDownloadComplete(int id) {
                                fileReadySubject.onNext(true);
                            }

                            @Override
                            public void onDownloadFailed(int id, int errorCode, String errorMessage) {
                                subscriber.onError(new Exception("Download failed, code " + errorMessage + ": " + errorMessage));
                            }

                            @Override
                            public void onProgress(int id, long l, int percent) {
                                downloadProgressSubject.onNext(percent);
                            }
                        });
                        downloadManager.add(request);
                    } else {
                        fileReadySubject.onNext(true);
                    }
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public static class InstallProgress {
        public enum Stage {
            HtcFix {
                @Override
                public String getTitle() {
                    return "HTC override";
                }
                @Override
                public boolean hasPercentProgress() {
                    return false;
                }
            }, Download {
                @Override
                public String getTitle() {
                    return "Download new emoji";
                }
                @Override
                public boolean hasPercentProgress() {
                    return true;
                }
            }, Backup {
                @Override
                public String getTitle() {
                    return "Backup old emoji";
                }
                @Override
                public boolean hasPercentProgress() {
                    return false;
                }
            }, Install {
                @Override
                public String getTitle() {
                    return "Install new emoji";
                }
                @Override
                public boolean hasPercentProgress() {
                    return false;
                }
            }, Done {
                @Override
                public String getTitle() {
                    return "Done!";
                }
                @Override
                public boolean hasPercentProgress() {
                    return false;
                }
            };

            public abstract String getTitle();
            public abstract boolean hasPercentProgress();
        }

        static boolean hasHtcStage() {
            return isHtc();
        }

        static boolean hasDownloadStage(EmojiSet emojiSet) {
            try {
                return !isDownloaded(emojiSet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        }

        static boolean hasBackupStage(Context context) {
            File backupFile = new File(systemEmojiBackupFilePath(context));
            return (backupFile.length() == 0);
        }

        public Stage currentStage;
        public int currentStageProgress = 0;
    }

    public static boolean isDownloaded(EmojiSet emojiSet) throws IOException {
        return (emojiSet.path != null && emojiSet.isIntact());
    }

    private static String filenameFromUrl(String url) {
        String filename = FilenameUtils.getName(url);
        int indexOfParam = filename.indexOf('?');
        if (indexOfParam != -1) {
            return filename.substring(0, indexOfParam);
        } else {
            return filename;
        }
    }

    public static String filePath(Context context, EmojiSetListing listing) {
        return context.getFilesDir() + File.separator + "emojisets" + File.separator + filenameFromUrl(listing.url);
    }

    public static void applyPermissions(String permissions, String path) throws TimeoutException, RootDeniedException, IOException {
        RootTools.remount(path, "RW");
        Shell shell = RootTools.getShell(true);
        Command commandPermission = new Command(0, "chmod " + permissions + " " + path);
        shell.add(commandPermission);
        shell.close();
    }

    public static Dialog makeRebootDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Reboot now?");
        builder.setMessage("Most apps require a reboot for new emojis to be recognized.");
        builder.setPositiveButton("Reboot", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                RootTools.restartAndroid();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        return builder.create();
    }

    public static Observable<EmojiSetListing> currentEmojiSet(final Context context, final List<EmojiSetListing> sets) {
        return Observable.create(new Observable.OnSubscribe<EmojiSetListing>() {
            @Override
            public void call(Subscriber<? super EmojiSetListing> subscriber) {
                File emojiSetDestinationFile = new File(context.getFilesDir() + File.separator + "systemcurrent.ttf");
                RootTools.copyFile(systemEmojiFilePath, emojiSetDestinationFile.getAbsolutePath(), true, false);
                try {
                    applyPermissions("777", emojiSetDestinationFile.getAbsolutePath());
                } catch (Exception e) {
                    subscriber.onError(e);
                }
                EmojiSet systemSet = new EmojiSet(new EmojiSetListing(), emojiSetDestinationFile);

                boolean found = false;
                for (EmojiSetListing set : sets) {
                    try {
                        if (systemSet.getFileMd5().equals(set.md5)) {
                            found = true;
                            subscriber.onNext(set);
                            break;
                        }
                    } catch (IOException e) {
                        subscriber.onError(e);
                    }
                }
                if (!found) {
                    subscriber.onNext(null);
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    public static class RestoreSystemEmojiTask extends AsyncTask<Activity, Void, Void> {
        private Activity activity;

        @Override
        protected Void doInBackground(Activity... activity) {
            this.activity = activity[0];

            if (isHtc()) {
                undoHtcFix();
            }

            RootTools.copyFile(systemEmojiBackupFilePath(activity[0]), systemEmojiFilePath, true, true);
            try {
                applyPermissions("644", systemEmojiFilePath);
            } catch (TimeoutException | RootDeniedException | IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            makeRebootDialog(activity).show();
        }
    }
}