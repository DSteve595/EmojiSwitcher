package com.stevenschoen.emojiswitcher;

import android.app.Activity;

import com.stevenschoen.emojiswitcher.billing.IabException;
import com.stevenschoen.emojiswitcher.billing.IabHelper;
import com.stevenschoen.emojiswitcher.network.EmojiSetListing;

import java.util.Collections;
import java.util.List;

public class SetListingUtils {
    public static boolean userOwnsSet(EmojiSetListing listing, IabHelper billingHelper) {
        if (listing.googlePlaySku == null || listing.free) {
            return true;
        } else {
            List<String> skuList = Collections.singletonList(listing.googlePlaySku);
            try {
                return billingHelper.queryInventory(true, skuList, null).hasPurchase(listing.googlePlaySku);
            } catch (IabException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public static void purchaseSet(Activity activity, EmojiSetListing listing, IabHelper billingHelper, IabHelper.OnIabPurchaseFinishedListener listener) {
        billingHelper.launchPurchaseFlow(activity, listing.googlePlaySku, 0, listener);
    }
}