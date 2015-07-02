package com.stevenschoen.emojiswitcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.widget.Toast;

import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootShell.execution.Shell;
import com.stericson.RootTools.RootTools;
import com.stevenschoen.emojiswitcher.network.EmojiSetListing;
import com.stevenschoen.emojiswitcher.network.NetworkInterface;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

import retrofit.RestAdapter;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

public class EmojiSwitcherUtils {
	public static final String GOOGLE_ADS_UNITID = "ca-app-pub-9259165898539273/5321636233";
	public static final String PLAY_LICENSING_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsV" +
			"ARwtabK8aSCIf5Pel+rFPZWgDMbKeiWAuKHex7T7PWme78lcFCnadcj94sCpBeYx65fVO6LPeLMD+8nR1WlX" +
			"1fUiVbCG1efGka+OGpKJ1/BnVRwE4wC88ijXu7UwP2Lp0lP4Z/7mUs9oR/5VbYsNTwukzyg8U8VOBa2PA4/e" +
			"uSo8YHTY4toD8lvyzqRzmGGW+yoWdKNbKx/6yZE3O7cH/S8rLbTMeESwuJ0h+E8A9moKOkPFEwPVXx9hMXIU" +
			"XyPGOw/f05Bn0GIOhXcVwh7cp2KjhRHzwTPBUqzZymBY7QzKl8bw8yXr/Q6U4yCdQajHhh3g64PM4YB13peo" +
			"amCQIDAQAB";
	public static final String SKU_REMOVEADS = "emojiswitcher_removeads";

    private InstallEmojiSetTask currentInstallTask;

    private static final String systemFontsPath = "/system/fonts/";
    private static final String systemEmojiFilePath = systemFontsPath + "NotoColorEmoji.ttf";
    private static final String htcFilePath = systemFontsPath + "AndroidEmoji-htc.ttf";
    private static final String htcBackupFilePath = htcFilePath + ".bak";

    private NetworkInterface networkInterface;

    public EmojiSwitcherUtils() {
        networkInterface = new RestAdapter.Builder()
                .setEndpoint(NetworkInterface.URL)
                .build().create(NetworkInterface.class);
    }

    public NetworkInterface getNetworkInterface() {
        return networkInterface;
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

	public void installEmojiSet(Context context, EmojiSet emojiSet) {
        if (isHtc()) {
            applyHtcFix();
        }

        if (currentInstallTask != null) {
            currentInstallTask.cancel(true);
            currentInstallTask = null;
            installEmojiSet(context, emojiSet);
        } else {
            currentInstallTask = new InstallEmojiSetTask();
            currentInstallTask.execute(context, emojiSet);
        }
    }

    public static void applyPermissions(final Activity activity, String permissions, String path) {
        try {
            RootTools.remount(path, "RW");
            Shell shell = RootTools.getShell(true);
            Command commandPermission = new Command(0, "chmod " + permissions + " " + path);
            shell.add(commandPermission);
            shell.close();
        } catch (TimeoutException e) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, "Error: Timeout", Toast.LENGTH_LONG).show();
                }
            });
            e.printStackTrace();
        } catch (RootDeniedException e) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, "Error: Root denied", Toast.LENGTH_LONG).show();
                }
            });
            e.printStackTrace();
        } catch (IOException e) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, "Error: IOException", Toast.LENGTH_LONG).show();
                }
            });
            e.printStackTrace();
        }
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

    private static class InstallEmojiSetTask extends AsyncTask<Object, Void, Void> {
        @Override
        protected Void doInBackground(Object... params) {
            final Activity activity = (Activity) params[0];
            EmojiSet emojiSet = (EmojiSet) params[1];

            File systemEmojiSetFile = new File(systemEmojiFilePath);
            File backupFile = new File(systemEmojiBackupFilePath(activity));
            if (backupFile.length() == 0) {
                RootTools.copyFile(systemEmojiSetFile.getAbsolutePath(),
                        backupFile.getAbsolutePath(), true, false);
            }

            File emojiSetFile = emojiSet.path;
            RootTools.copyFile(emojiSetFile.getAbsolutePath(),
                    systemEmojiSetFile.getAbsolutePath(), true, false);
            applyPermissions(activity, "644", systemEmojiFilePath);

            return null;
        }
    }

    public static Observable<EmojiSetListing> currentEmojiSet(final Context context, final List<EmojiSetListing> sets) {
        return Observable.create(new Observable.OnSubscribe<EmojiSetListing>() {
            @Override
            public void call(Subscriber<? super EmojiSetListing> subscriber) {
                File emojiSetDestinationFile = new File(context.getFilesDir() + File.separator + "systemcurrent.ttf");
                RootTools.copyFile(systemEmojiFilePath, emojiSetDestinationFile.getAbsolutePath(), true, false);
                applyPermissions((Activity) context, "777", emojiSetDestinationFile.getAbsolutePath());
                EmojiSet systemSet = new EmojiSet(emojiSetDestinationFile);

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
            applyPermissions(activity[0], "644", systemEmojiFilePath);

            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            makeRebootDialog(activity).show();
        }
    }
}