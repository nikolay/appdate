package com.nikolay.appdate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private LinearLayout listContainer;
    private TextView summaryText;
    private TextView emptyText;
    private TextView storeText;
    private ProgressBar progressBar;
    private int primary;
    private int primaryOn;
    private int headerBg;
    private int headerTitle;
    private int headerSubtitle;
    private int amber;
    private int textColor;
    private int mutedColor;
    private int surfaceColor;
    private int subtleSurfaceColor;
    private int pageBg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadThemeColors();
        buildUi();
        refreshDisabledApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (listContainer != null) {
            refreshDisabledApps();
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(pageBg);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(22), dp(20), dp(18));
        header.setBackgroundColor(headerBg);
        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText(getString(R.string.app_name));
        title.setTextColor(headerTitle);
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(getString(R.string.home_subtitle));
        subtitle.setTextColor(headerSubtitle);
        subtitle.setTextSize(15);
        subtitle.setLineSpacing(dp(2), 1.0f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(6);
        header.addView(subtitle, subtitleParams);

        summaryText = new TextView(this);
        summaryText.setTextColor(headerTitle);
        summaryText.setTextSize(14);
        summaryText.setTypeface(Typeface.DEFAULT_BOLD);
        summaryText.setPadding(0, dp(14), 0, 0);
        header.addView(summaryText);

        Button refreshButton = button("Refresh");
        refreshButton.setOnClickListener(v -> refreshDisabledApps());
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(44));
        refreshParams.topMargin = dp(14);
        header.addView(refreshButton, refreshParams);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        root.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(28));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        storeText = new TextView(this);
        storeText.setTextColor(mutedColor);
        storeText.setTextSize(13);
        storeText.setLineSpacing(dp(2), 1.0f);
        storeText.setText(getString(R.string.store_check_hint));
        content.addView(storeText, cardParams());

        progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        progressParams.gravity = Gravity.CENTER_HORIZONTAL;
        progressParams.topMargin = dp(20);
        content.addView(progressBar, progressParams);

        emptyText = new TextView(this);
        emptyText.setTextColor(mutedColor);
        emptyText.setTextSize(16);
        emptyText.setGravity(Gravity.CENTER);
        emptyText.setPadding(dp(18), dp(36), dp(18), dp(36));
        emptyText.setText(getString(R.string.empty_disabled_apps));
        content.addView(emptyText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(listContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    private void refreshDisabledApps() {
        progressBar.setVisibility(View.VISIBLE);
        listContainer.removeAllViews();

        List<DisabledApp> apps = scanDisabledApps();
        Collections.sort(apps, new DisabledAppComparator());

        summaryText.setText(String.format(Locale.US,
                "%d disabled apps · store checks available",
                apps.size()));
        storeText.setText(getString(R.string.store_check_hint));

        emptyText.setVisibility(apps.isEmpty() ? View.VISIBLE : View.GONE);
        for (DisabledApp app : apps) {
            listContainer.addView(rowForApp(app), cardParams());
        }

        progressBar.setVisibility(View.GONE);
    }

    private List<DisabledApp> scanDisabledApps() {
        PackageManager pm = getPackageManager();
        List<DisabledApp> disabledApps = new ArrayList<>();
        List<ApplicationInfo> installedApps = getInstalledApplicationsCompat(pm);

        for (ApplicationInfo appInfo : installedApps) {
            if (!isDisabled(pm, appInfo)) {
                continue;
            }

            String packageName = appInfo.packageName;
            String label = safeLabel(pm, appInfo);
            PackageInfo packageInfo = getPackageInfoCompat(pm, packageName);
            long installedVersion = versionCode(packageInfo);
            String versionName = packageInfo != null && packageInfo.versionName != null
                    ? packageInfo.versionName
                    : String.valueOf(installedVersion);
            long updateTime = packageInfo != null ? packageInfo.lastUpdateTime : 0;
            String installSource = installSourceLine(pm, packageName);

            disabledApps.add(new DisabledApp(
                    label,
                    packageName,
                    versionName,
                    installedVersion,
                    updateTime,
                    installSource,
                    enabledStateName(pm, packageName),
                    safeIcon(pm, appInfo)));
        }

        return disabledApps;
    }

    @SuppressWarnings("deprecation")
    private List<ApplicationInfo> getInstalledApplicationsCompat(PackageManager pm) {
        int flags = PackageManager.MATCH_DISABLED_COMPONENTS;
        if (Build.VERSION.SDK_INT >= 33) {
            return pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags));
        }
        return pm.getInstalledApplications(flags);
    }

    @SuppressWarnings("deprecation")
    private PackageInfo getPackageInfoCompat(PackageManager pm, String packageName) {
        try {
            int flags = PackageManager.MATCH_DISABLED_COMPONENTS;
            if (Build.VERSION.SDK_INT >= 33) {
                return pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags));
            }
            return pm.getPackageInfo(packageName, flags);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private boolean isDisabled(PackageManager pm, ApplicationInfo appInfo) {
        int state;
        try {
            state = pm.getApplicationEnabledSetting(appInfo.packageName);
        } catch (IllegalArgumentException e) {
            state = PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
        }

        return state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED
                || (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && !appInfo.enabled);
    }

    private String enabledStateName(PackageManager pm, String packageName) {
        try {
            switch (pm.getApplicationEnabledSetting(packageName)) {
                case PackageManager.COMPONENT_ENABLED_STATE_DISABLED:
                    return "Disabled";
                case PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER:
                    return "Disabled by user";
                case PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED:
                    return "Disabled until used";
                case PackageManager.COMPONENT_ENABLED_STATE_ENABLED:
                    return "Enabled override";
                default:
                    return "Disabled by system/default";
            }
        } catch (IllegalArgumentException e) {
            return "Disabled";
        }
    }

    private String safeLabel(PackageManager pm, ApplicationInfo appInfo) {
        CharSequence label = appInfo.loadLabel(pm);
        if (label == null || TextUtils.isEmpty(label.toString().trim())) {
            return appInfo.packageName;
        }
        return label.toString();
    }

    private Drawable safeIcon(PackageManager pm, ApplicationInfo appInfo) {
        try {
            return appInfo.loadIcon(pm);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private long versionCode(PackageInfo packageInfo) {
        if (packageInfo == null) {
            return 0;
        }
        if (Build.VERSION.SDK_INT >= 28) {
            return packageInfo.getLongVersionCode();
        }
        return packageInfo.versionCode;
    }

    @SuppressWarnings("deprecation")
    private String installSourceLine(PackageManager pm, String packageName) {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                InstallSourceInfo info = pm.getInstallSourceInfo(packageName);
                String updateOwner = null;
                if (Build.VERSION.SDK_INT >= 34) {
                    updateOwner = info.getUpdateOwnerPackageName();
                }
                String installing = info.getInstallingPackageName();
                if (!TextUtils.isEmpty(updateOwner)) {
                    return "Update owner: " + friendlyStoreName(updateOwner);
                }
                if (!TextUtils.isEmpty(installing)) {
                    return "Installed by: " + friendlyStoreName(installing);
                }
            }

            String installer = pm.getInstallerPackageName(packageName);
            if (!TextUtils.isEmpty(installer)) {
                return "Installed by: " + friendlyStoreName(installer);
            }
        } catch (PackageManager.NameNotFoundException | IllegalArgumentException ignored) {
            return null;
        }
        return null;
    }

    private String friendlyStoreName(String packageName) {
        if ("com.android.vending".equals(packageName)) {
            return "Google Play";
        }
        if ("com.sec.android.app.samsungapps".equals(packageName)) {
            return "Galaxy Store";
        }
        return packageName;
    }

    private View rowForApp(DisabledApp app) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(14), dp(14), dp(14), dp(14));
        row.setBackgroundColor(surfaceColor);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(top);

        ImageView icon = new ImageView(this);
        if (app.icon != null) {
            icon.setImageDrawable(app.icon);
        }
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        iconParams.rightMargin = dp(12);
        top.addView(icon, iconParams);

        LinearLayout textGroup = new LinearLayout(this);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        top.addView(textGroup, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView name = new TextView(this);
        name.setText(app.label);
        name.setTextColor(textColor);
        name.setTextSize(17);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        textGroup.addView(name);

        TextView packageName = new TextView(this);
        packageName.setText(app.packageName);
        packageName.setTextColor(mutedColor);
        packageName.setTextSize(12);
        packageName.setSingleLine(true);
        packageName.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        textGroup.addView(packageName);

        TextView status = new TextView(this);
        status.setText(app.statusLine());
        status.setTextColor(primary);
        status.setTextSize(14);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = dp(12);
        row.addView(status, statusParams);

        TextView details = new TextView(this);
        details.setText(app.detailLine());
        details.setTextColor(mutedColor);
        details.setTextSize(13);
        details.setLineSpacing(dp(2), 1.0f);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        detailParams.topMargin = dp(4);
        row.addView(details, detailParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        actionsParams.topMargin = dp(12);
        row.addView(actions, actionsParams);

        Button update = button("Check update");
        update.setOnClickListener(v -> showUpdateChoices(app));
        actions.addView(update, new LinearLayout.LayoutParams(0, dp(44), 1));

        Space spacer = new Space(this);
        actions.addView(spacer, new LinearLayout.LayoutParams(dp(8), 1));

        Button uninstall = secondaryButton("Uninstall");
        uninstall.setOnClickListener(v -> confirmUninstall(app));
        actions.addView(uninstall, new LinearLayout.LayoutParams(0, dp(44), 1));

        return row;
    }

    private void showUpdateChoices(DisabledApp app) {
        String[] choices = new String[] {
                "Open Google Play",
                "Open Galaxy Store",
                "Open app info"
        };

        new AlertDialog.Builder(this)
                .setTitle("Check " + app.label)
                .setMessage("Appdate keeps this app disabled. Google Play or Galaxy Store will show whether an update is available and handle the update flow.")
                .setItems(choices, (dialog, which) -> {
                    if (which == 0) {
                        openPlayStore(app.packageName);
                    } else if (which == 1) {
                        openGalaxyStore(app.packageName);
                    } else {
                        openAppInfo(app.packageName);
                    }
                })
                .show();
    }

    private void confirmUninstall(DisabledApp app) {
        new AlertDialog.Builder(this)
                .setTitle("Uninstall " + app.label + "?")
                .setMessage("Android will ask for confirmation. System apps may only allow uninstalling updates or opening app info.")
                .setPositiveButton("Continue", (dialog, which) -> openUninstall(app.packageName))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openPlayStore(String packageName) {
        Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
        market.setPackage("com.android.vending");
        if (!startSafely(market)) {
            openUri("https://play.google.com/store/apps/details?id=" + packageName);
        }
    }

    private void openGalaxyStore(String packageName) {
        Intent galaxy = new Intent(Intent.ACTION_VIEW, Uri.parse("samsungapps://ProductDetail/" + packageName));
        galaxy.setPackage("com.sec.android.app.samsungapps");
        if (!startSafely(galaxy)) {
            Toast.makeText(this, "Galaxy Store is not available on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAppInfo(String packageName) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + packageName));
        startSafely(intent);
    }

    private void openUninstall(String packageName) {
        Intent intent = new Intent(Intent.ACTION_DELETE);
        intent.setData(Uri.parse("package:" + packageName));
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        if (!startSafely(intent)) {
            openAppInfo(packageName);
        }
    }

    private void openUri(String uri) {
        startSafely(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
    }

    private boolean startSafely(Intent intent) {
        try {
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        } catch (SecurityException e) {
            Toast.makeText(this, "Android blocked that action.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(13);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(false);
        button.setTextColor(primaryOn);
        button.setBackgroundColor(primary);
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = button(text);
        button.setTextColor(textColor);
        button.setBackgroundColor(subtleSurfaceColor);
        return button;
    }

    private void loadThemeColors() {
        primary = color(R.color.app_primary);
        primaryOn = color(R.color.app_primary_on);
        headerBg = color(R.color.app_header_bg);
        headerTitle = color(R.color.app_header_title);
        headerSubtitle = color(R.color.app_header_subtitle);
        amber = color(R.color.app_amber);
        textColor = color(R.color.app_text);
        mutedColor = color(R.color.app_muted);
        surfaceColor = color(R.color.app_surface);
        subtleSurfaceColor = color(R.color.app_surface_subtle);
        pageBg = color(R.color.app_bg);
    }

    private int color(int resourceId) {
        return getResources().getColor(resourceId, getTheme());
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(12);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class DisabledApp {
        final String label;
        final String packageName;
        final String versionName;
        final long installedVersion;
        final long lastUpdatedAt;
        final String installSource;
        final String enabledState;
        final Drawable icon;

        DisabledApp(
                String label,
                String packageName,
                String versionName,
                long installedVersion,
                long lastUpdatedAt,
                String installSource,
                String enabledState,
                Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.versionName = versionName;
            this.installedVersion = installedVersion;
            this.lastUpdatedAt = lastUpdatedAt;
            this.installSource = installSource;
            this.enabledState = enabledState;
            this.icon = icon;
        }

        String statusLine() {
            return "Disabled · check Play or Galaxy Store";
        }

        String detailLine() {
            StringBuilder builder = new StringBuilder();
            builder.append(enabledState)
                    .append(" · Installed ")
                    .append(versionName)
                    .append(" (")
                    .append(installedVersion)
                    .append(")");

            if (!TextUtils.isEmpty(installSource)) {
                builder.append(" · ")
                        .append(installSource);
            }

            if (lastUpdatedAt > 0) {
                builder.append(" · Updated ")
                        .append(DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(lastUpdatedAt)));
            }

            return builder.toString();
        }
    }

    private static final class DisabledAppComparator implements Comparator<DisabledApp> {
        @Override
        public int compare(DisabledApp left, DisabledApp right) {
            return left.label.compareToIgnoreCase(right.label);
        }
    }
}
