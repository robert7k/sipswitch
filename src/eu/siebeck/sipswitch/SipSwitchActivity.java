package eu.siebeck.sipswitch;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * @author Robert G. Siebeck <robert@siebeck.eu>
 *
 */
public class SipSwitchActivity extends AppWidgetProvider {
	private static final String
			EXTRA_RECEIVE_CALLS = "eu.siebeck.sipswitch.EXTRA_RECEIVE_CALLS",
			EXTRA_CALL_MODE = "eu.siebeck.sipswitch.EXTRA_CALL_MODE";
	private static final String LOG = SipSwitchActivity.class.getName();
	public static final String
			ENABLE_SIP_ACTION = "eu.siebeck.sipswitch.ENABLE_SIP",
			CALL_MODE = "eu.siebeck.sipswitch.CALL_MODE";

	@SuppressWarnings("deprecation")
	@Override
	public void onUpdate(final Context context,
			final AppWidgetManager appWidgetManager, final int[] widgetIds) {
//		Debug.waitForDebugger();

		int sipReceiveCalls = -1;
		String callMode = "";
		try {
			sipReceiveCalls = Settings.System.getInt(
					context.getContentResolver(),
					Settings.System.SIP_RECEIVE_CALLS);
			callMode = Settings.System.getString(
					context.getContentResolver(),
					Settings.System.SIP_CALL_OPTIONS);

		} catch (final SettingNotFoundException e) {
			Log.w(LOG, e);
			Toast.makeText(
					context,
					context.getResources().getString(
							R.string.setting_not_supported),
					Toast.LENGTH_LONG).show();
			return;
		}

		final RemoteViews views = new RemoteViews(
				context.getApplicationContext().getPackageName(),
				R.layout.widget_layout);

		views.setImageViewResource(R.id.img_sip,
				sipReceiveCalls == 1 ? R.drawable.sip_on : R.drawable.sip_off);
		views.setImageViewResource(R.id.ind_sip,
				sipReceiveCalls == 1 ? R.drawable.appwidget_settings_ind_on_l
						: R.drawable.appwidget_settings_ind_off_l);
		views.setImageViewResource(R.id.ind_mode, getModeIndicator(callMode));
		views.setImageViewResource(R.id.img_mode, getModeImage(callMode));

		final Intent enableSipClickIntent = new Intent(context,
				SipSwitchActivity.class);
		enableSipClickIntent.setAction(ENABLE_SIP_ACTION);
		enableSipClickIntent.putExtra(EXTRA_RECEIVE_CALLS, sipReceiveCalls);

		final PendingIntent pendingSipClickIntent = PendingIntent.getBroadcast(
				context, 0, enableSipClickIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.sipButton,
				pendingSipClickIntent);

		final Intent callModeClickIntent = new Intent(context,
				SipSwitchActivity.class);
		callModeClickIntent.setAction(CALL_MODE);
		callModeClickIntent.putExtra(EXTRA_CALL_MODE, callMode);

		final PendingIntent pendingCallModeClickIntent = PendingIntent
				.getBroadcast(context, 0, callModeClickIntent,
						PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.callModeButton,
				pendingCallModeClickIntent);

		for (final int widgetId : widgetIds) {
			appWidgetManager.updateAppWidget(widgetId, views);
		}
	}

	@Override
	public void onReceive(final Context context, final Intent intent) {
		final String action = intent.getAction();
		if (ENABLE_SIP_ACTION.equals(action)) {
//			Debug.waitForDebugger();
			final int receiveCalls = intent.getIntExtra(EXTRA_RECEIVE_CALLS, 0) ^ 1;
			Log.i(LOG, "Set receiveCalls to " + receiveCalls);
			Settings.System.putInt(context.getContentResolver(),
					Settings.System.SIP_RECEIVE_CALLS, receiveCalls);
			updateWidgetView(context);
		} else if (CALL_MODE.equals(action)) {
//			Debug.waitForDebugger();

			final String callMode = toggleCallMode(intent
					.getStringExtra(EXTRA_CALL_MODE));
			Log.i(LOG, "Setting callMode to " + callMode);
			Settings.System.putString(context.getContentResolver(),
					Settings.System.SIP_CALL_OPTIONS, callMode);

			updateWidgetView(context);

			Toast.makeText(context, getModeToast(callMode),
					Toast.LENGTH_SHORT).show();
		}
		super.onReceive(context, intent);
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
		if (Settings.System.SIP_ASK_ME_EACH_TIME.equals(callMode))
			return R.string.mode_ask;
		else if (Settings.System.SIP_ADDRESS_ONLY.equals(callMode))
			return R.string.mode_phone;
		else
			return R.string.mode_sip;
	}

	private String toggleCallMode(final String callMode) {
		if (Settings.System.SIP_ASK_ME_EACH_TIME.equals(callMode))
			return Settings.System.SIP_ADDRESS_ONLY;
		else if (Settings.System.SIP_ADDRESS_ONLY.equals(callMode))
			return Settings.System.SIP_ALWAYS;
		else
			return Settings.System.SIP_ASK_ME_EACH_TIME;
	}

	private int getModeIndicator(final String callMode) {
		if (Settings.System.SIP_ASK_ME_EACH_TIME.equals(callMode))
			return R.drawable.appwidget_settings_ind_mid_r;
		else if (Settings.System.SIP_ADDRESS_ONLY.equals(callMode))
			return R.drawable.appwidget_settings_ind_off_r;
		else
			return R.drawable.appwidget_settings_ind_on_r;
	}

	private int getModeImage(final String callMode) {
		if (Settings.System.SIP_ASK_ME_EACH_TIME.equals(callMode))
			return R.drawable.mode_ask;
		else if (Settings.System.SIP_ADDRESS_ONLY.equals(callMode))
			return R.drawable.mode_phone;
		else
			return R.drawable.mode_sip;
	}
}