package com.stevenschoen.emojiswitcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.stericson.RootTools.RootTools;
import com.stevenschoen.emojiswitcher.billing.IabHelper;
import com.stevenschoen.emojiswitcher.billing.IabResult;
import com.stevenschoen.emojiswitcher.billing.Inventory;
import com.stevenschoen.emojiswitcher.billing.Purchase;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SwitcherActivity extends Activity {

    private TextView textCurrentEmojiSet;
    private ImageButton buttonRefreshEmojiState;
    private Spinner spinnerInstallEmojis;
    private ArrayAdapter<EmojiSet> emojiSetsAdapter;

    private ArrayList<EmojiSet> emojiSets = new ArrayList<>();
    private EmojiSet currentSystemEmojiSet;

    EmojiSwitcherUtils emojiSwitcherUtils;

	private IabHelper billingHelper;
	private boolean purchasedRemoveAds;
	private AdView adView;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emojiswitcher);

        emojiSwitcherUtils = new EmojiSwitcherUtils();

        verifyRoot();

        textCurrentEmojiSet = (TextView) findViewById(R.id.text_currentemojisetdetected_is);

        buttonRefreshEmojiState = (ImageButton) findViewById(R.id.button_refreshemojistate);
        buttonRefreshEmojiState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchCurrentSystemEmojiSet();
            }
        });

        spinnerInstallEmojis = (Spinner) findViewById(R.id.spinner_pickemojiset);
        emojiSetsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, emojiSets);
        emojiSetsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerInstallEmojis.setAdapter(emojiSetsAdapter);

		final Button buttonReboot = (Button) findViewById(R.id.button_reboot);
		buttonReboot.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				RootTools.restartAndroid();
			}
		});

        Button buttonInstallEmojiSet = (Button) findViewById(R.id.button_installemojiset);
        buttonInstallEmojiSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emojiSwitcherUtils.installEmojiSet(SwitcherActivity.this, (EmojiSet) spinnerInstallEmojis.getSelectedItem());
                fetchCurrentSystemEmojiSet();
				AlertDialog.Builder builder = new AlertDialog.Builder(SwitcherActivity.this);
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
				builder.show();
            }
        });

        copyEmojiSetsToData();

        fetchCurrentSystemEmojiSet();

		setupBilling();
    }

    private void fetchCurrentSystemEmojiSet() {
        verifyRoot();

        new EmojiSwitcherUtils.GetCurrentEmojiSetTask() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                buttonRefreshEmojiState.setEnabled(false);
            }

            @Override
            protected void onPostExecute(EmojiSet emojiSet) {
                super.onPostExecute(emojiSet);
                SwitcherActivity.this.currentSystemEmojiSet = emojiSet;
                attemptShowCurrentSystemEmojiSet();
            }
        }.execute(this);
    }

    private void attemptShowCurrentSystemEmojiSet() {
        if (currentSystemEmojiSet != null && !emojiSets.isEmpty() && emojiSets.size() > 0) {
            EmojiSet[] setsToCompare = new EmojiSet[emojiSets.size() + 1];
            setsToCompare[0] = currentSystemEmojiSet;
            for (int i = 0; i < emojiSets.size(); i++) {
                setsToCompare[i + 1] = emojiSets.get(i);
            }
            new EmojiSwitcherUtils.CompareEmojiSetTask() {
                @Override
                protected void onPostExecute(EmojiSet emojiSet) {
                    super.onPostExecute(emojiSet);
                    buttonRefreshEmojiState.setEnabled(true);
                    if (emojiSet != null) {
                        textCurrentEmojiSet.setTextColor(getResources().getColor(R.color.current_emojis_good));
                        textCurrentEmojiSet.setText(emojiSet.getFriendlyName());
                    } else {
                        textCurrentEmojiSet.setTextColor(getResources().getColor(R.color.current_emojis_bad));
                        textCurrentEmojiSet.setText(R.string.unknown);
                    }
                }
            }.execute(setsToCompare);
        }
    }

    private void verifyRoot() {
        if (!EmojiSwitcherUtils.isRootReady()) {
            Intent checkRootIntent = new Intent(this, CheckRootActivity.class);
            startActivity(checkRootIntent);
            finish();
        }
    }

    private void copyEmojiSetsToData() {
        File filesDir = getFilesDir();
        final File doneCopyingFile = new File(filesDir + File.separator + "donecopying");
        int versionCode = 0;
        try {
            versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        try {
            boolean copy = (!doneCopyingFile.exists() || !FileUtils.readFileToString(doneCopyingFile).equals(String.valueOf(versionCode)));
            if (copy) {
                final int finalVersionCode = versionCode;
                new EmojiSwitcherUtils.CopyEmojiSetsFromAssetsTask() {
                    ProgressDialog loadingDialog;

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        loadingDialog = ProgressDialog.show(SwitcherActivity.this, "Please wait", "Preparing emoji sets", true, false);
                    }

                    @Override
                    protected void onPostExecute(ArrayList<EmojiSet> emojiSets) {
                        super.onPostExecute(emojiSets);
                        SwitcherActivity.this.emojiSets.clear();
                        SwitcherActivity.this.emojiSets.addAll(emojiSets);
                        emojiSetsAdapter.notifyDataSetChanged();
                        try {
                            FileUtils.write(doneCopyingFile, String.valueOf(finalVersionCode));
                        } catch (IOException e) {
                            Toast.makeText(SwitcherActivity.this, "Error: Writing DC", Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                        loadingDialog.dismiss();
                        attemptShowCurrentSystemEmojiSet();
                    }
                }.execute(this);
            } else {
                final int finalVersionCode = versionCode;
                new EmojiSwitcherUtils.GetEmojiSetsFromDataTask() {
                    @Override
                    protected void onPostExecute(ArrayList<EmojiSet> emojiSets) {
                        super.onPostExecute(emojiSets);
                        SwitcherActivity.this.emojiSets.clear();
                        SwitcherActivity.this.emojiSets.addAll(emojiSets);
                        emojiSetsAdapter.notifyDataSetChanged();
                        try {
                            FileUtils.write(doneCopyingFile, String.valueOf(finalVersionCode));
                        } catch (IOException e) {
                            Toast.makeText(SwitcherActivity.this, "Error: Writing DC", Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }
                }.execute(this);
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error: If copy", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

	private void setupBilling() {
		billingHelper = new IabHelper(this, EmojiSwitcherUtils.PLAY_LICENSING_KEY);
		billingHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			@Override
			public void onIabSetupFinished(IabResult result) {
				if (result.isSuccess()) {
					checkAds();
				} else {
					Log.d("asdf", "Problem setting up in-app billing: " + result);
				}
			}
		});
	}

	private void checkAds() {
		List additionalSkuList = new ArrayList();
		additionalSkuList.add(EmojiSwitcherUtils.SKU_REMOVEADS);
		billingHelper.queryInventoryAsync(true, additionalSkuList,
				new IabHelper.QueryInventoryFinishedListener() {
					@Override
					public void onQueryInventoryFinished(IabResult result, Inventory inv) {
						if (result.isSuccess()) {
							purchasedRemoveAds = inv.hasPurchase(EmojiSwitcherUtils.SKU_REMOVEADS);
							final View removeAdsButton = findViewById(R.id.button_removeads);
							if (!purchasedRemoveAds) {
								AdRequest adRequest = new AdRequest.Builder().build();

								adView = new AdView(SwitcherActivity.this);
								adView.setAdSize(AdSize.SMART_BANNER);
								adView.setAdUnitId(EmojiSwitcherUtils.GOOGLE_ADS_UNITID);
								FrameLayout adHolder = (FrameLayout) findViewById(R.id.holder_ad);
								adHolder.addView(adView);
								adView.loadAd(adRequest);

								removeAdsButton.setOnClickListener(new View.OnClickListener() {
									@Override
									public void onClick(View v) {
										billingHelper.launchPurchaseFlow(SwitcherActivity.this,
												EmojiSwitcherUtils.SKU_REMOVEADS, 0, new IabHelper.OnIabPurchaseFinishedListener() {
													@Override
													public void onIabPurchaseFinished(IabResult result, Purchase info) {
														checkAds();
													}
												});
									}
								});
								removeAdsButton.setAlpha(0);
								removeAdsButton.setVisibility(View.VISIBLE);
								removeAdsButton.animate().alpha(0.75f);
							} else {
								removeAdsButton.animate().alpha(0).withEndAction(new Runnable() {
									@Override
									public void run() {
										removeAdsButton.setVisibility(View.GONE);
									}
								});
							}
						}
					}
				});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (billingHelper != null) {
			billingHelper.handleActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.emojiswitcher, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

	@Override
	protected void onResume() {
		super.onResume();

		if (adView != null) adView.resume();
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (adView != null) adView.pause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (billingHelper != null) billingHelper.dispose();
		billingHelper = null;

		if (adView != null) adView.destroy();
	}
}