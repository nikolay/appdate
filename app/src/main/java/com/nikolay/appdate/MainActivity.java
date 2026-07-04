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
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
    private EditText filterInput;
    private ProgressBar progressBar;
    private final List<DisabledApp> allDisabledApps = new ArrayList<>();
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

        filterInput = new EditText(this);
        filterInput.setSingleLine(true);
        filterInput.setHint("Filter by app or package");
        filterInput.setTextColor(textColor);
        filterInput.setHintTextColor(mutedColor);
        filterInput.setTextSize(15);
        filterInput.setPadding(dp(14), 0, dp(14), 0);
        filterInput.setBackgroundColor(surfaceColor);
        filterInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No work needed.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderDisabledApps();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No work needed.
            }
        });
        content.addView(filterInput, cardParams());

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
        allDisabledApps.clear();
        allDisabledApps.addAll(apps);

        storeText.setText(getString(R.string.store_check_hint));
        renderDisabledApps();

        progressBar.setVisibility(View.GONE);
    }

    private void renderDisabledApps() {
        if (listContainer == null || emptyText == null || summaryText == null) {
            return;
        }

        listContainer.removeAllViews();

        String query = filterInput != null
                ? filterInput.getText().toString().trim().toLowerCase(Locale.US)
                : "";
        List<DisabledApp> visibleApps = new ArrayList<>();
        for (DisabledApp app : allDisabledApps) {
            if (app.matches(query)) {
                visibleApps.add(app);
            }
        }

        if (TextUtils.isEmpty(query)) {
            summaryText.setText(String.format(Locale.US,
                    "%d disabled apps · open store or uninstall",
                    allDisabledApps.size()));
        } else {
            summaryText.setText(String.format(Locale.US,
                    "%d of %d disabled apps",
                    visibleApps.size(),
                    allDisabledApps.size()));
        }

        emptyText.setText(allDisabledApps.isEmpty()
                ? getString(R.string.empty_disabled_apps)
                : "No disabled apps match the filter.");
        emptyText.setVisibility(visibleApps.isEmpty() ? View.VISIBLE : View.GONE);
        for (DisabledApp app : visibleApps) {
            listContainer.addView(rowForApp(app), cardParams());
        }
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
            AppInstallSource installSource = installSourceFor(pm, packageName);

            disabledApps.add(new DisabledApp(
                    label,
                    packageName,
                    versionName,
                    installedVersion,
                    updateTime,
                    installSource.displayLine,
                    installSource.packageName,
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
    private AppInstallSource installSourceFor(PackageManager pm, String packageName) {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                InstallSourceInfo info = pm.getInstallSourceInfo(packageName);
                String updateOwner = null;
                if (Build.VERSION.SDK_INT >= 34) {
                    updateOwner = info.getUpdateOwnerPackageName();
                }
                String installing = info.getInstallingPackageName();
                if (!TextUtils.isEmpty(updateOwner)) {
                    return new AppInstallSource(
                            "Update owner: " + friendlyStoreName(updateOwner),
                            updateOwner);
                }
                if (!TextUtils.isEmpty(installing)) {
                    return new AppInstallSource(
                            "Installed by: " + friendlyStoreName(installing),
                            installing);
                }
            }

            String installer = pm.getInstallerPackageName(packageName);
            if (!TextUtils.isEmpty(installer)) {
                return new AppInstallSource(
                        "Installed by: " + friendlyStoreName(installer),
                        installer);
            }
        } catch (PackageManager.NameNotFoundException | IllegalArgumentException ignored) {
            return AppInstallSource.unknown();
        }
        return AppInstallSource.unknown();
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

        LinearLayout storeActions = actionRow();
        row.addView(storeActions);

        Button playStore = app.prefersGalaxyStore()
                ? secondaryButton("Google Play")
                : button("Google Play");
        playStore.setOnClickListener(v -> openPlayStore(app));
        storeActions.addView(playStore, new LinearLayout.LayoutParams(0, dp(44), 1));

        storeActions.addView(actionSpacer());

        Button galaxyStore = app.prefersGalaxyStore()
                ? button("Galaxy Store")
                : secondaryButton("Galaxy Store");
        galaxyStore.setOnClickListener(v -> openGalaxyStore(app));
        storeActions.addView(galaxyStore, new LinearLayout.LayoutParams(0, dp(44), 1));

        LinearLayout systemActions = actionRow();
        systemActions.setPadding(0, dp(8), 0, 0);
        row.addView(systemActions);

        Button appInfo = secondaryButton("App info");
        appInfo.setOnClickListener(v -> openAppInfo(app.packageName));
        systemActions.addView(appInfo, new LinearLayout.LayoutParams(0, dp(44), 1));

        systemActions.addView(actionSpacer());

        Button uninstall = warningButton("Uninstall");
        uninstall.setOnClickListener(v -> confirmUninstall(app));
        systemActions.addView(uninstall, new LinearLayout.LayoutParams(0, dp(44), 1));

        return row;
    }

    private LinearLayout actionRow() {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        actionsParams.topMargin = dp(12);
        actions.setLayoutParams(actionsParams);
        return actions;
    }

    private Space actionSpacer() {
        Space spacer = new Space(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(dp(8), 1));
        return spacer;
    }

    private void confirmUninstall(DisabledApp app) {
        new AlertDialog.Builder(this)
                .setTitle("Open Android uninstall?")
                .setMessage("Continue only opens Android's uninstall screen for " + app.label
                        + " (" + app.packageName + "). Nothing is removed unless you confirm on that Android screen. System apps may only allow app info or uninstalling updates.")
                .setPositiveButton("Open Android screen", (dialog, which) -> openUninstall(app.packageName))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openPlayStore(DisabledApp app) {
        Toast.makeText(this, "Opening Google Play for " + app.label, Toast.LENGTH_SHORT).show();
        openPlayStore(app.packageName);
    }

    private void openPlayStore(String packageName) {
        Uri playUri = Uri.parse("https://play.google.com/store/apps/details")
                .buildUpon()
                .appendQueryParameter("id", packageName)
                .build();
        Intent market = new Intent(Intent.ACTION_VIEW, playUri);
        market.setPackage("com.android.vending");
        if (!startSafely(market)) {
            openUri(playUri.toString());
        }
    }

    private void openGalaxyStore(DisabledApp app) {
        Toast.makeText(this, "Opening Galaxy Store for " + app.label, Toast.LENGTH_SHORT).show();
        openGalaxyStore(app.packageName);
    }

    private void openGalaxyStore(String packageName) {
        Intent galaxy = new Intent(Intent.ACTION_VIEW, Uri.parse("samsungapps://ProductDetail/" + packageName));
        galaxy.setPackage("com.sec.android.app.samsungapps");
        if (!startSafely(galaxy)) {
            if (!openUri("https://galaxystore.samsung.com/detail/" + packageName)) {
                Toast.makeText(this, "Galaxy Store is not available on this device.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openAppInfo(String packageName) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + packageName));
        if (!startSafely(intent)) {
            Toast.makeText(this, "Android could not open app info.", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressWarnings("deprecation")
    private void openUninstall(String packageName) {
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
        intent.setData(Uri.parse("package:" + packageName));
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        if (!startSafely(intent)) {
            openAppInfo(packageName);
        }
    }

    private boolean openUri(String uri) {
        return startSafely(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
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

    private Button warningButton(String text) {
        Button button = secondaryButton(text);
        button.setTextColor(amber);
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
        final String installSourcePackage;
        final String enabledState;
        final Drawable icon;

        DisabledApp(
                String label,
                String packageName,
                String versionName,
                long installedVersion,
                long lastUpdatedAt,
                String installSource,
                String installSourcePackage,
                String enabledState,
                Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.versionName = versionName;
            this.installedVersion = installedVersion;
            this.lastUpdatedAt = lastUpdatedAt;
            this.installSource = installSource;
            this.installSourcePackage = installSourcePackage;
            this.enabledState = enabledState;
            this.icon = icon;
        }

        String statusLine() {
            return "Disabled · open store to check update";
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

        boolean prefersGalaxyStore() {
            return "com.sec.android.app.samsungapps".equals(installSourcePackage);
        }

        boolean matches(String query) {
            if (TextUtils.isEmpty(query)) {
                return true;
            }

            String searchable = (label + " "
                    + packageName + " "
                    + versionName + " "
                    + enabledState + " "
                    + (installSource == null ? "" : installSource)).toLowerCase(Locale.US);
            return searchable.contains(query);
        }
    }

    private static final class AppInstallSource {
        final String displayLine;
        final String packageName;

        AppInstallSource(String displayLine, String packageName) {
            this.displayLine = displayLine;
            this.packageName = packageName;
        }

        static AppInstallSource unknown() {
            return new AppInstallSource(null, null);
        }
    }

    private static final class DisabledAppComparator implements Comparator<DisabledApp> {
        @Override
        public int compare(DisabledApp left, DisabledApp right) {
            return left.label.compareToIgnoreCase(right.label);
        }
    }
}
