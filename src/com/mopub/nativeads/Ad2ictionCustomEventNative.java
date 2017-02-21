package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.ad2iction.nativeads.Ad2ictionNative;
import com.ad2iction.nativeads.NativeResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mopub.nativeads.NativeImageHelper.preCacheImages;

public class Ad2ictionCustomEventNative extends CustomEventNative {

	private static final String LOG_TAG = Ad2ictionCustomEventNative.class.getSimpleName();

	protected void loadNativeAd(@NonNull Context context,
	                            @NonNull CustomEventNativeListener customEventNativeListener,
	                            @NonNull Map<String, Object> localExtras,
	                            @NonNull Map<String, String> serverExtras) {
		try {
			Ad2ictionForwardingNativeAd ad2ictionForwardingNativeAd =
					new Ad2ictionForwardingNativeAd(context, serverExtras.get("response_body_key"),
							customEventNativeListener);
			ad2ictionForwardingNativeAd.loadAd();
		} catch (IllegalArgumentException e) {
			customEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
			Log.i(LOG_TAG,
					"Failed Native AdFetch: Missing required server extras [response_body_key].");
		}
	}

	public static class Ad2ictionForwardingNativeAd extends BaseNativeAd
			implements Ad2ictionNative.Ad2ictionNativeNetworkListener,
			Ad2ictionNative.Ad2ictionNativeEventListener {

		private final Context mContext;
		private final String mKey;
		private final Ad2ictionNative mAd;
		private final CustomEventNativeListener mCustomEventNativeListener;
		private NativeResponse mResponse;

		private Ad2ictionForwardingNativeAd(Context context, String key,
		                                    CustomEventNativeListener customEventNativeListener)
				throws IllegalArgumentException {
			mContext = context.getApplicationContext();
			mKey = key;
			if (TextUtils.isEmpty(mKey)) {
				throw new IllegalArgumentException("Key cannot be null");
			}
			mAd = new Ad2ictionNative(context, key, "native", this);
			mCustomEventNativeListener = customEventNativeListener;
		}

		private void loadAd() {
			mAd.setNativeEventListener(this);
			mAd.makeRequest();
		}

		@Override
		public void onNativeLoad(NativeResponse nativeResponse) {
			mResponse = nativeResponse;

			final List<String> imageUrls = new ArrayList<>(2);
			final String mainImageUrl = nativeResponse.getMainImageUrl();
			if (mainImageUrl != null) {
				imageUrls.add(mainImageUrl);
				Log.d(LOG_TAG, "Ad2iction Native Ad main image found.");
			}

			final String iconUrl = nativeResponse.getIconImageUrl();
			if (iconUrl != null) {
				imageUrls.add(iconUrl);
				Log.d(LOG_TAG, "Ad2iction Native Ad icon image found.");
			}

			preCacheImages(mContext, imageUrls, new NativeImageHelper.ImageListener() {

				public void onImagesCached() {
					mCustomEventNativeListener.onNativeAdLoaded(Ad2ictionForwardingNativeAd.this);
				}

				public void onImagesFailedToCache(NativeErrorCode errorCode) {
					mCustomEventNativeListener.onNativeAdFailed(errorCode);
				}
			});
		}

		@Override
		public void onNativeFail(com.ad2iction.nativeads.NativeErrorCode nativeErrorCode) {
			if (nativeErrorCode == null) {
				mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
			} else if (nativeErrorCode == com.ad2iction.nativeads.NativeErrorCode.NETWORK_NO_FILL) {
				mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_NO_FILL);
			} else if (nativeErrorCode ==
					com.ad2iction.nativeads.NativeErrorCode.NETWORK_INVALID_STATE) {
				mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_INVALID_STATE);
			} else {
				mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
			}
		}

		@Override
		public void onNativeImpression(View view) {
			Log.d(LOG_TAG, "onNativeImpression: Ad2iction native ad impression logged");
			notifyAdImpressed();
		}

		@Override
		public void onNativeClick(View view) {
			Log.d(LOG_TAG, "onNativeClick: Ad2iction native ad clicked");
			notifyAdClicked();
		}

		@Override
		public void prepare(@NonNull View view) {
			mResponse.prepare(view);
			Log.d(LOG_TAG, "prepare(" + mResponse.toString() + " " + view.toString() + ")");
		}

		@Override
		public void clear(@NonNull View view) {
			mResponse.clear(view);
			Log.d(LOG_TAG, "clear(" + mResponse.toString() + ")");
		}

		@Override
		public void destroy() {
			mResponse.destroy();
			Log.d(LOG_TAG, "destroy(" + mResponse.toString() + ") started.");
		}

		NativeResponse getNativeResponse() {
			return mResponse;
		}
	}
}