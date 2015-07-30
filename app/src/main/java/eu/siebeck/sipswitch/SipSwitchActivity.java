package eu.siebeck.sipswitch;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import cyanogenmod.app.CMStatusBarManager;
import cyanogenmod.app.CustomTile;

/**
 * @author Robert G. Siebeck <robert@siebeck.org>
 *
 */
public class SipSwitchActivity extends AppWidgetProvider {
	private static final String
		ENABLE_SIP_ACTION = "eu.siebeck.sipswitch.ENABLE_SIP",
		CALL_MODE = "eu.siebeck.sipswitch.CALL_MODE",
		EXTRA_CALL_MODE = "eu.siebeck.sipswitch.EXTRA_CALL_MODE";
	private int SIP_SWITCH_TILE_ID = 1;
	/**
	 * Action string for the SIP call option configuration changed intent.
	 * This is used to communicate  change to the SIP call option, triggering re-registration of
	 * the SIP phone accounts.
	 */
	private static final String ACTION_SIP_CALL_OPTION_CHANGED =
			"com.android.phone.SIP_CALL_OPTION_CHANGED";
	private static final String LOG = SipSwitchActivity.class.getName();

	private static final String
		SIP_CALL_OPTIONS = "sip_call_options",
		SIP_ALWAYS = "SIP_ALWAYS",
		SIP_ADDRESS_ONLY = "SIP_ADDRESS_ONLY",
		SIP_ASK_ME_EACH_TIME = "SIP_ASK_ME_EACH_TIME";

	private static final Map<Integer,RemoteViews> remoteViewsMap = new HashMap<>();

	@Override
	public void onUpdate(final Context context,
			final AppWidgetManager appWidgetManager, final int[] widgetIds) {
//		Debug.waitForDebugger();

		final String callMode = Settings.System.getString(
				context.getContentResolver(),
				SIP_CALL_OPTIONS);
		if (callMode == null) {
			Log.w(LOG, "SIP_CALL_OPTIONS was null");
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
				setCallMode(context, SIP_ALWAYS);
			else
				setCallMode(context, SIP_ASK_ME_EACH_TIME);
		}

		final Intent enableSipClickIntent = new Intent(context, SipSwitchActivity.class);
		enableSipClickIntent.setAction(ENABLE_SIP_ACTION);
		final PendingIntent pendingSipClickIntent = PendingIntent.getBroadcast(
				context, 0, enableSipClickIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		final Intent callModeClickIntent = new Intent(context, SipSwitchActivity.class);
		callModeClickIntent.setAction(CALL_MODE);
		callModeClickIntent.putExtra(EXTRA_CALL_MODE, callMode);
		final PendingIntent pendingCallModeClickIntent = PendingIntent
				.getBroadcast(context, 0, callModeClickIntent,
						PendingIntent.FLAG_UPDATE_CURRENT);

		for (final int widgetId : widgetIds) {
			final RemoteViews views = getRemoteViews(context, widgetId);

			views.setImageViewResource(R.id.img_sip, R.mipmap.sip_settings);
			views.setImageViewResource(R.id.ind_mode, getModeIndicator(callMode));
			views.setImageViewResource(R.id.img_mode, getModeImage(callMode));

			views.setOnClickPendingIntent(R.id.sipButton, pendingSipClickIntent);
			views.setOnClickPendingIntent(R.id.callModeButton, pendingCallModeClickIntent);

			appWidgetManager.updateAppWidget(widgetId, views);
		}

		final Intent sipSettingsIntent = getSipSettingsIntent();

		final CustomTile mCustomTile = new CustomTile.Builder(context)
				.setOnClickIntent(pendingCallModeClickIntent)
				.setOnSettingsClickIntent(sipSettingsIntent)
				.setContentDescription(R.string.sip_settings)
				.setLabel(context.getString(getModeToast(callMode)))
				.setIcon(getModeImage(callMode))
				.build();
		CMStatusBarManager.getInstance(context)
				.publishTile(SIP_SWITCH_TILE_ID, mCustomTile);
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		for (final int widgetId : appWidgetIds)
			deleteRemoteViews(widgetId);

		super.onDeleted(context, appWidgetIds);
	}

	@Override
	public void onReceive(final Context context, final Intent intent) {
		final String action = intent.getAction();
		if (ENABLE_SIP_ACTION.equals(action)) {
			// Debug.waitForDebugger();
			final Intent sipSettingsIntent = getSipSettingsIntent();
			try {
				context.startActivity(sipSettingsIntent);
			} catch(final Exception e) {
				Log.e(LOG, "Error starting intent", e);
			}
		} else if (CALL_MODE.equals(action)) {
//			Debug.waitForDebugger();
			final String callMode = toggleCallMode(intent.getStringExtra(EXTRA_CALL_MODE));
			setCallMode(context, callMode);

			updateWidgetView(context);

			Toast.makeText(context, context.getString(R.string.toast,
						context.getString(R.string.sip),
						context.getString(getModeToast(callMode))),
					Toast.LENGTH_SHORT).show();
		} else if ("com.motorola.blur.home.ACTION_SET_WIDGET_SIZE".equals(action)) {
			final int spanX = intent.getExtras().getInt("spanX");
			final int spanY = intent.getExtras().getInt("spanY");
			final int appWidgetId = intent.getExtras().getInt("appWidgetId");
			Log.i(LOG, "Resized to " + spanX + " * " + spanY);
			int layoutId = spanX > 1 ? R.layout.widget_layout : R.layout.widget_layout_small;
			addRemoteViews(context, appWidgetId, layoutId);
			final RemoteViews views = getRemoteViews(context, appWidgetId);

			final AppWidgetManager appWidgetManager =
					AppWidgetManager.getInstance(context.getApplicationContext());
			updateWidget(context, appWidgetManager, appWidgetId, views);
		}
		super.onReceive(context, intent);
	}

	private Intent getSipSettingsIntent() {
		final Intent sipSettingsIntent = new Intent();
		final String componentName;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // XXX PhoneAccountSettingsActivity is not exported by phone app. We could only
            // call it with root access. For now, we simply call the parent activity
            // CallFeaturesSetting and let the user navigate to the SIP settings.
            // sipSettingsComponentName = "com.android.phone/.settings.PhoneAccountSettingsActivity";
            componentName = "com.android.phone/.CallFeaturesSetting";
        } else {
            componentName = "com.android.phone/.sip.SipSettings";
        }
		final ComponentName sipSettingsComponent = ComponentName.unflattenFromString(componentName);
		sipSettingsIntent.setComponent(sipSettingsComponent);
		sipSettingsIntent.setAction("android.intent.action.MAIN");
		sipSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		return sipSettingsIntent;
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public void onAppWidgetOptionsChanged(final Context context,
			final AppWidgetManager appWidgetManager,
			final int appWidgetId,
			final android.os.Bundle newOptions) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			final int width = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
			int layoutId = width < 100 ? R.layout.widget_layout_small : R.layout.widget_layout;
			addRemoteViews(context, appWidgetId, layoutId);
		}

		final RemoteViews views = getRemoteViews(context, appWidgetId);
		updateWidget(context, appWidgetManager, appWidgetId, views);

		super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
	}

	private void updateWidget(final Context context, final AppWidgetManager appWidgetManager,
			final int appWidgetId, final RemoteViews views) {
		appWidgetManager.updateAppWidget(appWidgetId, views);
		onUpdate(context, appWidgetManager, new int[]{appWidgetId});
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private RemoteViews getRemoteViews (final Context context, final int appWidgetId) {
		if (!remoteViewsMap.containsKey(appWidgetId))
			addRemoteViews(context, appWidgetId, R.layout.widget_layout);
		return remoteViewsMap.get(appWidgetId);
	}

	private void addRemoteViews(final Context context, final int appWidgetId, final int layoutId) {
		RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);
		remoteViewsMap.put(appWidgetId, views);
	}

	private void deleteRemoteViews(final int appWidgetId) {
		remoteViewsMap.remove(appWidgetId);
	}

	private void setCallMode(final Context context, final String callMode) {
		Log.i(LOG, "Setting callMode to " + callMode);
		Settings.System.putString(context.getContentResolver(),
				SIP_CALL_OPTIONS, callMode);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			// Notify SipAccountRegistry in the telephony layer that the configuration has changed.
			Intent intent = new Intent(ACTION_SIP_CALL_OPTION_CHANGED);
			context.sendBroadcast(intent);
		}
	}

	private void updateWidgetView(final Context context) {
		final AppWidgetManager appWidgetManager =
				AppWidgetManager.getInstance(context.getApplicationContext());
		final ComponentName thisWidget = new ComponentName(context,
				SipSwitchActivity.class);
		final int[] widgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
		final Intent update = new Intent(context, SipSwitchActivity.class);
		update.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
		context.sendBroadcast(update);
	}

	private int getModeToast(final String callMode) {
		if (SIP_ASK_ME_EACH_TIME.equals(callMode))
			return R.string.sip_call_options_entry_3;
		else if (SIP_ADDRESS_ONLY.equals(callMode))
			return R.string.sip_call_options_entry_2;
		else
			return R.string.sip_call_options_wifi_only_entry_1;
	}

	private String toggleCallMode(final String callMode) {
		if (SIP_ASK_ME_EACH_TIME.equals(callMode))
			return SIP_ADDRESS_ONLY;
		else if (SIP_ADDRESS_ONLY.equals(callMode))
			return SIP_ALWAYS;
		else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			return SIP_ADDRESS_ONLY;
		else
			return SIP_ASK_ME_EACH_TIME;
	}

	private int getModeIndicator(final String callMode) {
		if (SIP_ASK_ME_EACH_TIME.equals(callMode))
			return R.drawable.appwidget_settings_ind_mid_r;
		else if (SIP_ADDRESS_ONLY.equals(callMode))
			return R.drawable.appwidget_settings_ind_off_r;
		else
			return R.drawable.appwidget_settings_ind_on_r;
	}

	private int getModeImage(final String callMode) {
		if (SIP_ASK_ME_EACH_TIME.equals(callMode))
			return R.mipmap.mode_ask;
		else if (SIP_ADDRESS_ONLY.equals(callMode))
			return R.mipmap.mode_phone;
		else
			return R.mipmap.mode_sip;
	}
}
