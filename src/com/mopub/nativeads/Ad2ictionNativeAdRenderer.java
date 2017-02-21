package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ad2iction.common.VisibleForTesting;
import com.ad2iction.nativeads.NativeResponse;
import com.mopub.common.Preconditions;

import java.util.Iterator;
import java.util.WeakHashMap;

public class Ad2ictionNativeAdRenderer
		implements MoPubAdRenderer<Ad2ictionCustomEventNative.Ad2ictionForwardingNativeAd> {

	private static final String LOG_TAG = Ad2ictionNativeAdRenderer.class.getSimpleName();

	@NonNull private final ViewBinder mViewBinder;
	@NonNull private final WeakHashMap<View, Ad2ictionNativeViewHolder> mViewHolderMap;

	public Ad2ictionNativeAdRenderer(@NonNull final ViewBinder viewBinder) {
		mViewBinder = viewBinder;
		mViewHolderMap = new WeakHashMap<>();
	}

	@Override
	@NonNull
	public View createAdView(@NonNull final Context context, final ViewGroup parent) {
		return LayoutInflater.from(context).inflate(this.mViewBinder.layoutId, parent, false);
	}

	@Override
	public void renderAdView(@NonNull final View view, @NonNull
	final Ad2ictionCustomEventNative.Ad2ictionForwardingNativeAd ad2ictionForwardingNativeAd) {
		Ad2ictionNativeViewHolder nativeViewHolder = mViewHolderMap.get(view);
		if (nativeViewHolder == null) {
			nativeViewHolder = Ad2ictionNativeViewHolder.fromViewBinder(view, this.mViewBinder);
			mViewHolderMap.put(view, nativeViewHolder);
		}
		nativeViewHolder.update(ad2ictionForwardingNativeAd.getNativeResponse());
		nativeViewHolder
				.updateExtras(view, ad2ictionForwardingNativeAd.getNativeResponse(), mViewBinder);
		setViewVisibility(nativeViewHolder, View.VISIBLE);
	}

	private void setViewVisibility(@NonNull final Ad2ictionNativeViewHolder viewHolder,
	                               final int visibility) {
		if (viewHolder.staticNativeViewHolder.mainView != null) {
			viewHolder.staticNativeViewHolder.mainView.setVisibility(visibility);
		}
	}

	@Override
	public boolean supports(@NonNull final BaseNativeAd nativeAd) {
		Preconditions.checkNotNull(nativeAd);
		return nativeAd instanceof Ad2ictionCustomEventNative.Ad2ictionForwardingNativeAd;
	}

	private static class Ad2ictionNativeViewHolder {

		private final StaticNativeViewHolder staticNativeViewHolder;
		@Nullable TextView titleView;
		@Nullable TextView textView;
		@Nullable TextView callToActionView;
		@Nullable ImageView mainImageView;
		@Nullable ImageView iconImageView;

		@VisibleForTesting

		private Ad2ictionNativeViewHolder(final StaticNativeViewHolder staticNativeViewHolder) {
			this.staticNativeViewHolder = staticNativeViewHolder;
		}

		static Ad2ictionNativeViewHolder fromViewBinder(final View view,
		                                                final ViewBinder viewBinder) {
			StaticNativeViewHolder staticNativeViewHolder =
					StaticNativeViewHolder.fromViewBinder(view, viewBinder);
			Ad2ictionNativeViewHolder nativeViewHolder =
					new Ad2ictionNativeViewHolder(staticNativeViewHolder);

			nativeViewHolder.titleView = staticNativeViewHolder.titleView;
			nativeViewHolder.textView = staticNativeViewHolder.textView;
			nativeViewHolder.callToActionView = staticNativeViewHolder.callToActionView;
			nativeViewHolder.mainImageView = staticNativeViewHolder.mainImageView;
			nativeViewHolder.iconImageView = staticNativeViewHolder.iconImageView;

			return nativeViewHolder;
		}

		void update(@NonNull NativeResponse nativeResponse) {
			this.addTextView(this.titleView, nativeResponse.getTitle());
			this.addTextView(this.textView, nativeResponse.getText());
			this.addTextView(this.callToActionView, nativeResponse.getCallToAction());
			nativeResponse.loadMainImage(this.mainImageView);
			nativeResponse.loadIconImage(this.iconImageView);
		}

		void updateExtras(@NonNull View outerView, @NonNull NativeResponse nativeResponse,
		                  @NonNull ViewBinder viewBinder) {
			Iterator i$ = viewBinder.extras.keySet().iterator();

			while (i$.hasNext()) {
				String key = (String) i$.next();
				int resourceId = viewBinder.extras.get(key);
				View view = outerView.findViewById(resourceId);
				Object content = nativeResponse.getExtra(key);
				if (view instanceof ImageView) {
					((ImageView) view).setImageDrawable(null);
					nativeResponse.loadExtrasImage(key, (ImageView) view);
				} else if (view instanceof TextView) {
					((TextView) view).setText(null);
					if (content instanceof String) {
						this.addTextView((TextView) view, (String) content);
					}
				} else {
					Log.d(LOG_TAG, "View bound to " + key +
							" should be an instance of TextView or ImageView.");
				}
			}

		}

		private void addTextView(@Nullable TextView textView, @Nullable String contents) {
			if (textView == null) {
				Log.d(LOG_TAG, "Attempted to add text (" + contents + ") to null TextView.");
			} else {
				textView.setText(null);
				if (contents == null) {
					Log.d(LOG_TAG, "Attempted to set TextView contents to null.");
				} else {
					textView.setText(contents);
				}

			}
		}
	}

}
