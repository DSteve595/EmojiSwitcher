package com.stevenschoen.emojiswitcher;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.stericson.RootTools.RootTools;
import com.stevenschoen.emojiswitcher.billing.IabHelper;
import com.stevenschoen.emojiswitcher.billing.IabResult;
import com.stevenschoen.emojiswitcher.billing.Inventory;
import com.stevenschoen.emojiswitcher.billing.Purchase;
import com.stevenschoen.emojiswitcher.network.EmojiSetListing;
import com.stevenschoen.emojiswitcher.network.EmojiSetsResponse;

import java.util.ArrayList;
import java.util.List;

import rx.android.lifecycle.LifecycleObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;

public class SwitcherActivity extends RxAppCompatActivity {

    private TextView textCurrentEmojiSet;
    private ImageButton buttonRefreshEmojiState;
    private Spinner spinnerInstallEmojis;
    private int lastSelectedSetPosition;
    private ArrayAdapter<EmojiSetListing> emojiSetsAdapter;

    private BehaviorSubject<EmojiSetsResponse> emojiSetsResponseObservable = BehaviorSubject.create();
    private ArrayList<EmojiSetListing> emojiSetListings = new ArrayList<>();

    private EmojiSwitcherUtils emojiSwitcherUtils;

    private IabHelper billingHelper;
    private boolean purchasedRemoveAds;
    private AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emojiswitcher);

        verifyRoot();

        if (EmojiSwitcherUtils.isRootReady()) {
            init();
        }
    }

    private void init() {
        emojiSwitcherUtils = new EmojiSwitcherUtils();
        LifecycleObservable.bindActivityLifecycle(lifecycle(),
                EmojiSwitcherUtils.getNetworkInterface(this).getEmojiSets())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<EmojiSetsResponse>() {
                    @Override
                    public void call(EmojiSetsResponse emojiSetsResponse) {
                        emojiSetListings.clear();
                        emojiSetListings.addAll(emojiSetsResponse.emojiSets);
                        emojiSetsAdapter.notifyDataSetChanged();
                        if (spinnerInstallEmojis.getSelectedItemPosition() == AdapterView.INVALID_POSITION) {
                            for (int i = 0; i < emojiSetListings.size(); i++) {
                                if (emojiSetListings.get(i).selectByDefault) {
                                    spinnerInstallEmojis.setSelection(i);
                                    break;
                                }
                            }
                        }
                        emojiSetsResponseObservable.onNext(emojiSetsResponse);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });

        textCurrentEmojiSet = (TextView) findViewById(R.id.text_currentemojisetdetected_is);

        buttonRefreshEmojiState = (ImageButton) findViewById(R.id.button_refreshemojistate);
        buttonRefreshEmojiState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshCurrentSystemEmojiSet();
            }
        });

        spinnerInstallEmojis = (Spinner) findViewById(R.id.spinner_pickemojiset);
        emojiSetsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, emojiSetListings);
        emojiSetsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerInstallEmojis.setAdapter(emojiSetsAdapter);

        spinnerInstallEmojis.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                EmojiSetListing listing = (EmojiSetListing) parent.getItemAtPosition(position);
                if (SetListingUtils.userOwnsSet(listing, billingHelper)) {
                    lastSelectedSetPosition = position;
                } else {
                    parent.setSelection(lastSelectedSetPosition);
//                    buy
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        Button buttonReboot = (Button) findViewById(R.id.button_reboot);
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
                EmojiSetListing listing = (EmojiSetListing) spinnerInstallEmojis.getSelectedItem();
                if (SetListingUtils.userOwnsSet(listing, billingHelper)) {
                    emojiSwitcherUtils.installEmojiSet(SwitcherActivity.this, (EmojiSet) spinnerInstallEmojis.getSelectedItem());
                    refreshCurrentSystemEmojiSet();
                    EmojiSwitcherUtils.makeRebootDialog(SwitcherActivity.this).show();
                } else {
//                    buy
                }
            }
        });

        refreshCurrentSystemEmojiSet();

        setupBilling();
    }

    private void refreshCurrentSystemEmojiSet() {
        verifyRoot();

        buttonRefreshEmojiState.setEnabled(false);

        LifecycleObservable.bindActivityLifecycle(lifecycle(),
                emojiSetsResponseObservable)
                .subscribe(new Action1<EmojiSetsResponse>() {
                    @Override
                    public void call(EmojiSetsResponse emojiSetsResponse) {
                        EmojiSwitcherUtils.currentEmojiSet(SwitcherActivity.this, emojiSetListings)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Action1<EmojiSetListing>() {
                                    @Override
                                    public void call(EmojiSetListing emojiSetListing) {
                                        if (emojiSetListing != null) {
                                            textCurrentEmojiSet.setTextColor(getResources().getColor(R.color.current_emojis_good));
                                            textCurrentEmojiSet.setText(emojiSetListing.name);
                                        } else {
                                            textCurrentEmojiSet.setTextColor(getResources().getColor(R.color.current_emojis_bad));
                                            textCurrentEmojiSet.setText(R.string.unknown);
                                        }
                                        buttonRefreshEmojiState.setEnabled(true);
                                    }
                                }, new Action1<Throwable>() {
                                    @Override
                                    public void call(Throwable throwable) {
                                        throwable.printStackTrace();
                                    }
                                });
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
    }

    private void verifyRoot() {
        if (!EmojiSwitcherUtils.isRootReady()) {
            Intent checkRootIntent = new Intent(this, CheckRootActivity.class);
            startActivity(checkRootIntent);
            finish();
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
        List<String> additionalSkuList = new ArrayList<>();
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
        switch (id) {
            case R.id.action_restore:
                new EmojiSwitcherUtils.RestoreSystemEmojiTask() {
                    @Override
                    protected void onPostExecute(Void nothing) {
                        super.onPostExecute(nothing);

                        refreshCurrentSystemEmojiSet();
                    }
                }.execute(this);
                return true;
            case R.id.action_manage_downloads:
                ManageDownloadsFragment fragment = (ManageDownloadsFragment) Fragment.instantiate(this, ManageDownloadsFragment.class.getName());
                fragment.show(getSupportFragmentManager(), "managedownloads");

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