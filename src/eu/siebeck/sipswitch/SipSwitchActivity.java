package eu.siebeck.sipswitch;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * @author Robert G. Siebeck <robert@siebeck.org>
 *
 */
public class SipSwitchActivity extends AppWidgetProvider {
	private static final String EXTRA_CALL_MODE = "eu.siebeck.sipswitch.EXTRA_CALL_MODE";
	private static final String LOG = SipSwitchActivity.class.getName();
	public static final String
			ENABLE_SIP_ACTION = "eu.siebeck.sipswitch.ENABLE_SIP",
			CALL_MODE = "eu.siebeck.sipswitch.CALL_MODE";

	private static final String
		SIP_CALL_OPTIONS = "sip_call_options",
		SIP_ALWAYS = "SIP_ALWAYS",
		SIP_ADDRESS_ONLY = "SIP_ADDRESS_ONLY",
		SIP_ASK_ME_EACH_TIME = "SIP_ASK_ME_EACH_TIME";

	@Override
	public void onUpdate(final Context context,
			final AppWidgetManager appWidgetManager, final int[] widgetIds) {
//		Debug.waitForDebugger();

		final String callMode = Settings.System.getString(
					context.getContentResolver(),
					SIP_CALL_OPTIONS);
		if (callMode == null) {
			Log.w(LOG, "SIP_CALL_OPTIONS was null");
			setCallMode(context, SIP_ASK_ME_EACH_TIME);
		}

		for (final int widgetId : widgetIds) {
			final RemoteViews views = getRemoteViews(context, appWidgetManager, widgetId);

			views.setImageViewResource(R.id.img_sip, R.drawable.sip_on);
			views.setImageViewResource(R.id.ind_mode, getModeIndicator(callMode));
			views.setImageViewResource(R.id.img_mode, getModeImage(callMode));

			final Intent enableSipClickIntent = new Intent(context, SipSwitchActivity.class);
			enableSipClickIntent.setAction(ENABLE_SIP_ACTION);

			final PendingIntent pendingSipClickIntent = PendingIntent.getBroadcast(
					context, 0, enableSipClickIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			views.setOnClickPendingIntent(R.id.sipButton, pendingSipClickIntent);

			final Intent callModeClickIntent = new Intent(context, SipSwitchActivity.class);
			callModeClickIntent.setAction(CALL_MODE);
			callModeClickIntent.putExtra(EXTRA_CALL_MODE, callMode);

			final PendingIntent pendingCallModeClickIntent = PendingIntent
					.getBroadcast(context, 0, callModeClickIntent,
							PendingIntent.FLAG_UPDATE_CURRENT);
			views.setOnClickPendingIntent(R.id.callModeButton, pendingCallModeClickIntent);

			appWidgetManager.updateAppWidget(widgetId, views);
		}
	}

	@Override
	public void onReceive(final Context context, final Intent intent) {
		final String action = intent.getAction();
		if (ENABLE_SIP_ACTION.equals(action)) {
			final Intent sipSettingsIntent = new Intent();
			final ComponentName SipSettingsComponent =
					ComponentName.unflattenFromString("com.android.phone/.sip.SipSettings");
			sipSettingsIntent.setComponent(SipSettingsComponent);
			sipSettingsIntent.setAction("android.intent.action.MAIN");
			sipSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(sipSettingsIntent);
		} else if (CALL_MODE.equals(action)) {
			final String callMode = toggleCallMode(intent.getStringExtra(EXTRA_CALL_MODE));
			setCallMode(context, callMode);

			updateWidgetView(context);

			Toast.makeText(context, getModeToast(callMode), Toast.LENGTH_SHORT).show();
		}
		super.onReceive(context, intent);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public void onAppWidgetOptionsChanged(final Context context,
			final AppWidgetManager appWidgetManager,
			final int appWidgetId,
			final android.os.Bundle newOptions) {
		final RemoteViews views = getRemoteViews(context, appWidgetManager, appWidgetId);
		appWidgetManager.updateAppWidget(appWidgetId, views);

		onUpdate(context, appWidgetManager, new int[] {appWidgetId});

		super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private RemoteViews getRemoteViews (final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId) {
		final int width;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			width = 150;
		} else {
			final Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
			width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
		}
		final int layoutId = width < 100 ? R.layout.widget_layout_small : R.layout.widget_layout;
		final RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);
		return views;
	}

	private void setCallMode(final Context context, final String callMode) {
		Log.i(LOG, "Setting callMode to " + callMode);
		Settings.System.putString(context.getContentResolver(),
				SIP_CALL_OPTIONS, callMode);
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
			return R.string.mode_ask;
		else if (SIP_ADDRESS_ONLY.equals(callMode))
			return R.string.mode_phone;
		else
			return R.string.mode_sip;
	}

	private String toggleCallMode(final String callMode) {
		if (SIP_ASK_ME_EACH_TIME.equals(callMode))
			return SIP_ADDRESS_ONLY;
		else if (SIP_ADDRESS_ONLY.equals(callMode))
			return SIP_ALWAYS;
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
			return R.drawable.mode_ask;
		else if (SIP_ADDRESS_ONLY.equals(callMode))
			return R.drawable.mode_phone;
		else
			return R.drawable.mode_sip;
	}
}