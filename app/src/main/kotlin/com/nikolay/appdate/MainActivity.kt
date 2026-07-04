package com.nikolay.appdate

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.content.pm.ApplicationInfo
import android.content.pm.InstallSourceInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private const val PLAY_PACKAGE = "com.android.vending"
private const val GALAXY_PACKAGE = "com.sec.android.app.samsungapps"
private const val FDROID_PACKAGE = "org.fdroid.fdroid"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppdateTheme {
                AppdateScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppdateScreen() {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var refreshToken by rememberSaveable { mutableIntStateOf(0) }
    var apps by remember { mutableStateOf<List<DisabledApp>>(emptyList()) }
    var availability by remember { mutableStateOf(StoreAvailability()) }
    var isScanning by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshToken) {
        isScanning = true
        errorText = null
        runCatching { scanDisabledApps(context.applicationContext) }
            .onSuccess { result ->
                apps = result.apps
                availability = result.storeAvailability
            }
            .onFailure { error ->
                errorText = error.localizedMessage ?: "Package scan failed."
            }
        isScanning = false
    }

    val filteredApps by remember(apps, query) {
        derivedStateOf {
            val normalizedQuery = query.trim().lowercase(Locale.US)
            if (normalizedQuery.isEmpty()) {
                apps
            } else {
                apps.filter { it.matches(normalizedQuery) }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Appdate",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isScanning) "Scanning disabled apps" else "${apps.size} disabled apps",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { refreshToken += 1 },
                        enabled = !isScanning
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .navigationBarsPadding()
                .imePadding()
        ) {
            SearchAndSummary(
                query = query,
                onQueryChange = { query = it },
                totalCount = apps.size,
                visibleCount = filteredApps.size,
                errorText = errorText
            )

            when {
                isScanning && apps.isEmpty() -> EmptyState("Scanning disabled apps...")
                errorText != null && apps.isEmpty() -> EmptyState(errorText ?: "Package scan failed.")
                filteredApps.isEmpty() -> EmptyState(
                    if (apps.isEmpty()) "No disabled apps found." else "No disabled apps match the filter."
                )
                else -> DisabledAppList(
                    apps = filteredApps,
                    availability = availability,
                    onOpenStore = { store, packageName -> context.openStore(store, packageName) },
                    onOpenAppInfo = { packageName -> context.openAppInfo(packageName) },
                    onUninstall = { packageName -> context.openUninstall(packageName) }
                )
            }
        }
    }
}

@Composable
private fun SearchAndSummary(
    query: String,
    onQueryChange: (String) -> Unit,
    totalCount: Int,
    visibleCount: Int,
    errorText: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search disabled apps") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (query.isBlank()) "$totalCount disabled apps" else "$visibleCount of $totalCount disabled apps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DisabledAppList(
    apps: List<DisabledApp>,
    availability: StoreAvailability,
    onOpenStore: (StoreKind, String) -> Unit,
    onOpenAppInfo: (String) -> Unit,
    onUninstall: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = apps,
            key = { it.packageName }
        ) { app ->
            DisabledAppCard(
                app = app,
                availability = availability,
                onOpenStore = onOpenStore,
                onOpenAppInfo = onOpenAppInfo,
                onUninstall = onUninstall
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DisabledAppCard(
    app: DisabledApp,
    availability: StoreAvailability,
    onOpenStore: (StoreKind, String) -> Unit,
    onOpenAppInfo: (String) -> Unit,
    onUninstall: (String) -> Unit
) {
    val storeAction = app.storeAction(availability)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppIcon(
                    packageName = app.packageName,
                    label = app.label
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusPill(app.enabledState, PillTone.Neutral)
                StatusPill("Update unknown", PillTone.Warning)
                StatusPill(app.sourceChipLabel, app.sourceTone)
            }

            Text(
                text = app.detailLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (storeAction != null) {
                    StoreButton(
                        store = storeAction,
                        onClick = { onOpenStore(storeAction, app.packageName) },
                        modifier = Modifier.weight(1.35f)
                    )
                } else {
                    Text(
                        text = "No Play/Galaxy store",
                        modifier = Modifier.weight(1.35f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = { onOpenAppInfo(app.packageName) },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "App info for ${app.label}"
                    )
                }
                IconButton(
                    onClick = { onUninstall(app.packageName) },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Uninstall ${app.label}"
                    )
                }
            }
        }
    }
}

@Composable
private fun AppIcon(packageName: String, label: String) {
    val context = LocalContext.current
    val iconSizePx = with(LocalDensity.current) { 44.dp.roundToPx() }
    val icon by produceState<ImageBitmap?>(initialValue = null, packageName, iconSizePx) {
        value = withContext(Dispatchers.IO) {
            context.packageManager.appIconBitmap(packageName, iconSizePx)
        }
    }

    if (icon != null) {
        Image(
            bitmap = icon!!,
            contentDescription = null,
            modifier = Modifier.size(44.dp)
        )
    } else {
        InitialAvatar(label)
    }
}

@Composable
private fun InitialAvatar(label: String) {
    val initial = label.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Surface(
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StoreButton(
    store: StoreKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Text(
            text = store.buttonLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusPill(label: String, tone: PillTone) {
    val colors = when (tone) {
        PillTone.Positive -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        PillTone.Warning -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        PillTone.Neutral -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = colors.first,
        contentColor = colors.second
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

@Composable
private fun AppdateTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> darkColorScheme(
            primary = Color(0xFF70DDB0),
            secondary = Color(0xFFB4CCBF),
            tertiary = Color(0xFFE3BE73),
            background = Color(0xFF101512),
            surface = Color(0xFF171D19)
        )
        else -> lightColorScheme(
            primary = Color(0xFF126B4A),
            secondary = Color(0xFF4F6358),
            tertiary = Color(0xFF725B19),
            background = Color(0xFFF7FAF7),
            surface = Color(0xFFFFFFFF)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

private suspend fun scanDisabledApps(context: Context): ScanResult = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val availability = StoreAvailability(
        playStore = pm.isPackageLaunchable(PLAY_PACKAGE),
        galaxyStore = pm.isPackageLaunchable(GALAXY_PACKAGE)
    )

    val apps = pm.getInstalledApplicationsCompat(PackageManager.MATCH_DISABLED_COMPONENTS)
        .asSequence()
        .filter { pm.isDisabled(it) }
        .map { appInfo ->
            val packageInfo = pm.getPackageInfoCompat(appInfo.packageName)
            val versionCode = packageInfo.versionCodeCompat()
            val versionName = packageInfo?.versionName ?: versionCode.toString()
            val installSource = pm.installSourceFor(appInfo.packageName)
            DisabledApp(
                label = pm.safeLabel(appInfo),
                packageName = appInfo.packageName,
                versionName = versionName,
                versionCode = versionCode,
                lastUpdatedAt = packageInfo?.lastUpdateTime ?: 0L,
                installSourceLabel = installSource.displayLine,
                installSourcePackage = installSource.packageName,
                enabledState = pm.enabledStateName(appInfo.packageName)
            )
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        .toList()

    ScanResult(apps, availability)
}

@Suppress("DEPRECATION")
private fun PackageManager.getInstalledApplicationsCompat(flags: Int): List<ApplicationInfo> {
    return if (Build.VERSION.SDK_INT >= 33) {
        getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags.toLong()))
    } else {
        getInstalledApplications(flags)
    }
}

@Suppress("DEPRECATION")
private fun PackageManager.getPackageInfoCompat(packageName: String): PackageInfo? {
    return try {
        val flags = PackageManager.MATCH_DISABLED_COMPONENTS
        if (Build.VERSION.SDK_INT >= 33) {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            getPackageInfo(packageName, flags)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}

@Suppress("DEPRECATION")
private fun PackageManager.getApplicationInfoCompat(packageName: String, flags: Int): ApplicationInfo? {
    return try {
        if (Build.VERSION.SDK_INT >= 33) {
            getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
        } else {
            getApplicationInfo(packageName, flags)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}

private fun PackageManager.isPackageLaunchable(packageName: String): Boolean {
    val appInfo = getApplicationInfoCompat(packageName, PackageManager.MATCH_DISABLED_COMPONENTS)
        ?: return false
    val state = runCatching { getApplicationEnabledSetting(packageName) }
        .getOrDefault(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
    return when (state) {
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED -> false
        else -> appInfo.enabled
    }
}

private fun PackageManager.isDisabled(appInfo: ApplicationInfo): Boolean {
    val state = runCatching { getApplicationEnabledSetting(appInfo.packageName) }
        .getOrDefault(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)

    return state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
        state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER ||
        state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED ||
        (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && !appInfo.enabled)
}

private fun PackageManager.enabledStateName(packageName: String): String {
    return when (
        runCatching { getApplicationEnabledSetting(packageName) }
            .getOrDefault(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
    ) {
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> "Disabled"
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER -> "Disabled by user"
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED -> "Disabled until used"
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> "Enabled override"
        else -> "Disabled by system/default"
    }
}

private fun PackageManager.safeLabel(appInfo: ApplicationInfo): String {
    val label = appInfo.loadLabel(this).toString().trim()
    return if (label.isNullOrEmpty()) appInfo.packageName else label
}

@Suppress("DEPRECATION")
private fun PackageManager.installSourceFor(packageName: String): AppInstallSource {
    return try {
        if (Build.VERSION.SDK_INT >= 30) {
            val info: InstallSourceInfo = getInstallSourceInfo(packageName)
            val updateOwner = if (Build.VERSION.SDK_INT >= 34) {
                info.updateOwnerPackageName
            } else {
                null
            }
            val installingPackage = info.installingPackageName
            when {
                !updateOwner.isNullOrBlank() -> AppInstallSource(
                    displayLine = "Update owner: ${friendlyStoreName(updateOwner)}",
                    packageName = updateOwner
                )
                !installingPackage.isNullOrBlank() -> AppInstallSource(
                    displayLine = "Installed by: ${friendlyStoreName(installingPackage)}",
                    packageName = installingPackage
                )
                else -> AppInstallSource()
            }
        } else {
            val installer = getInstallerPackageName(packageName)
            if (installer.isNullOrBlank()) {
                AppInstallSource()
            } else {
                AppInstallSource(
                    displayLine = "Installed by: ${friendlyStoreName(installer)}",
                    packageName = installer
                )
            }
        }
    } catch (_: IllegalArgumentException) {
        AppInstallSource()
    } catch (_: PackageManager.NameNotFoundException) {
        AppInstallSource()
    }
}

private fun friendlyStoreName(packageName: String): String {
    return when (packageName) {
        PLAY_PACKAGE -> "Play Store"
        GALAXY_PACKAGE -> "Galaxy Store"
        FDROID_PACKAGE -> "F-Droid"
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.samsung.android.packageinstaller" -> "Package installer"
        "com.sec.android.app.myfiles" -> "Samsung My Files"
        else -> packageName
    }
}

private fun PackageManager.appIconBitmap(packageName: String, sizePx: Int): ImageBitmap? {
    return runCatching {
        val appInfo = getApplicationInfoCompat(packageName, PackageManager.MATCH_DISABLED_COMPONENTS)
            ?: return null
        appInfo.loadIcon(this).toBitmap(sizePx).asImageBitmap()
    }.getOrNull()
}

private fun Drawable.toBitmap(sizePx: Int): Bitmap {
    if (this is BitmapDrawable && bitmap != null) {
        return Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true)
    }

    val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    setBounds(0, 0, sizePx, sizePx)
    draw(canvas)
    return output
}

@Suppress("DEPRECATION")
private fun PackageInfo?.versionCodeCompat(): Long {
    if (this == null) return 0L
    return if (Build.VERSION.SDK_INT >= 28) longVersionCode else versionCode.toLong()
}

private fun Context.openStore(store: StoreKind, packageName: String) {
    when (store) {
        StoreKind.Play -> openPlayStore(packageName)
        StoreKind.Galaxy -> openGalaxyStore(packageName)
    }
}

private fun Context.openPlayStore(packageName: String) {
    val playUri = Uri.parse("https://play.google.com/store/apps/details")
        .buildUpon()
        .appendQueryParameter("id", packageName)
        .build()
    val playIntent = Intent(Intent.ACTION_VIEW, playUri).setPackage(PLAY_PACKAGE)
    if (!startSafely(playIntent)) {
        startSafely(Intent(Intent.ACTION_VIEW, playUri))
    }
}

private fun Context.openGalaxyStore(packageName: String) {
    val galaxyIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("samsungapps://ProductDetail/$packageName")
    ).setPackage(GALAXY_PACKAGE)
    if (!startSafely(galaxyIntent)) {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://galaxystore.samsung.com/detail/$packageName")
        )
        if (!startSafely(webIntent)) {
            Toast.makeText(this, "Galaxy Store is not available.", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun Context.openAppInfo(packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.parse("package:$packageName"))
    if (!startSafely(intent)) {
        Toast.makeText(this, "Android could not open app info.", Toast.LENGTH_SHORT).show()
    }
}

private fun Context.openUninstall(packageName: String) {
    val intent = Intent(Intent.ACTION_DELETE)
        .setData(Uri.parse("package:$packageName"))
        .putExtra(Intent.EXTRA_RETURN_RESULT, true)
    if (!startSafely(intent)) {
        openAppInfo(packageName)
    }
}

private fun Context.startSafely(intent: Intent): Boolean {
    return try {
        if (this !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        Toast.makeText(this, "Android blocked that action.", Toast.LENGTH_SHORT).show()
        false
    }
}

private data class ScanResult(
    val apps: List<DisabledApp>,
    val storeAvailability: StoreAvailability
)

private data class StoreAvailability(
    val playStore: Boolean = false,
    val galaxyStore: Boolean = false
) {
    fun isAvailable(store: StoreKind): Boolean {
        return when (store) {
            StoreKind.Play -> playStore
            StoreKind.Galaxy -> galaxyStore
        }
    }
}

private data class AppInstallSource(
    val displayLine: String? = null,
    val packageName: String? = null
)

private data class DisabledApp(
    val label: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val lastUpdatedAt: Long,
    val installSourceLabel: String?,
    val installSourcePackage: String?,
    val enabledState: String
) {
    val sourceChipLabel: String
        get() = installSourcePackage?.let { "Source: ${friendlyStoreName(it)}" } ?: "Source unknown"

    val sourceTone: PillTone
        get() = when (installSourcePackage) {
            PLAY_PACKAGE, GALAXY_PACKAGE -> PillTone.Positive
            null -> PillTone.Neutral
            else -> PillTone.Neutral
        }

    val detailLine: String
        get() {
            val details = mutableListOf(
                "Version $versionName ($versionCode)"
            )
            if (lastUpdatedAt > 0L) {
                details += "Updated ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(lastUpdatedAt))}"
            }
            return details.joinToString(" · ")
        }

    fun storeAction(availability: StoreAvailability): StoreKind? {
        val preferred = when (installSourcePackage) {
            PLAY_PACKAGE -> StoreKind.Play
            GALAXY_PACKAGE -> StoreKind.Galaxy
            else -> null
        }
        return preferred?.takeIf { availability.isAvailable(it) }
    }

    fun matches(query: String): Boolean {
        if (query.isBlank()) return true
        return listOf(
            label,
            packageName,
            versionName,
            enabledState,
            installSourceLabel.orEmpty()
        ).joinToString(" ")
            .lowercase(Locale.US)
            .contains(query)
    }
}

private enum class StoreKind(val buttonLabel: String) {
    Play("Play Store"),
    Galaxy("Galaxy Store")
}

private enum class PillTone {
    Positive,
    Warning,
    Neutral
}
