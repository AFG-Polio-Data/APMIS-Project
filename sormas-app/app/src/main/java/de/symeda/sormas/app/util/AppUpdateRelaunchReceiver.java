package de.symeda.sormas.app.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.symeda.sormas.app.login.LoginActivity;
import de.symeda.sormas.app.backend.config.ConfigProvider;

/**
 * Relaunches the app after a self-update has replaced the APK.
 *
 * The app process is killed while the package is being replaced, so
 * onActivityResult is not a reliable way to detect a successful install.
 * Instead we listen for MY_PACKAGE_REPLACED / PACKAGE_REPLACED.
 *
 * Only acts if ConfigProvider.isAppUpdatePending() is true, which is set
 * by AppUpdateController.installApp() right before launching the installer
 * intent. This prevents an unwanted relaunch after unrelated package
 * replacements (Play Store, adb, etc.).
 */
public class AppUpdateRelaunchReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        final String action = intent.getAction();
        final boolean isMyPackageReplaced =
                Intent.ACTION_MY_PACKAGE_REPLACED.equals(action);
        final boolean isPackageReplaced =
                Intent.ACTION_PACKAGE_REPLACED.equals(action);

        if (!isMyPackageReplaced && !isPackageReplaced) {
            return;
        }

        // PACKAGE_REPLACED is broadcast for every package; filter to our own.
        if (isPackageReplaced) {
            if (intent.getData() == null
                    || !context.getPackageName()
                    .equals(intent.getData().getSchemeSpecificPart())) {
                return;
            }
        }

        // Only relaunch if a self-update was actually in progress.
        if (!ConfigProvider.isAppUpdatePending()) {
            return;
        }
        ConfigProvider.setAppUpdatePending(false);

        Intent launchIntent = new Intent(context, LoginActivity.class);
        launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(launchIntent);
    }
}