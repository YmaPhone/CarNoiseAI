package com.ymaphone.carnoiseai

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private val Navy = Color(0xFF071426)
private val Panel = Color(0xFF12243A)
private val Blue = Color(0xFF2864F0)
private val Ink = Color(0xFF111827)
private val Muted = Color(0xFF8995A7)
private val Green = Color(0xFF43D6A0)

class MainActivity : ComponentActivity() {
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (
            Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
        setContent { CarMuteApp() }
    }
}

@Composable
fun CarMuteApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("carnoise", Context.MODE_PRIVATE) }

    var page by remember { mutableIntStateOf(0) }
    var connected by remember { mutableStateOf(prefs.getBoolean("vehicle_connected", false)) }
    var profile by remember { mutableStateOf(prefs.getBoolean("profile_enabled", true)) }
    var vehicleName by remember {
        mutableStateOf(
            prefs.getString("vehicle_name", "הרכב שלי")
                ?.let { if (it == "My Car") "הרכב שלי" else it } ?: "הרכב שלי"
        )
    }
    var address by remember { mutableStateOf(prefs.getString("vehicle_address", null)) }
    var allowAlarms by remember { mutableStateOf(prefs.getBoolean("allow_alarms", true)) }
    var allowCalls by remember { mutableStateOf(prefs.getBoolean("allow_calls", false)) }
    var allowMessages by remember { mutableStateOf(prefs.getBoolean("allow_messages", false)) }
    var allowMedia by remember { mutableStateOf(prefs.getBoolean("allow_media", true)) }
    var restore by remember { mutableStateOf(prefs.getBoolean("restore_dnd", true)) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark_theme", true)) }

    LaunchedEffect(Unit) {
        while (true) {
            connected = prefs.getBoolean("vehicle_connected", false)
            delay(1000)
        }
    }

    val bg = if (dark) Navy else Color(0xFFF4F6FA)
    val fg = if (dark) Color.White else Ink
    val card = if (dark) Panel else Color.White

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = if (dark) {
                darkColorScheme(primary = Blue, background = bg, surface = card)
            } else {
                lightColorScheme(primary = Blue, background = bg, surface = card)
            }
        ) {
            Column(Modifier.fillMaxSize().background(bg)) {
                AppHeader(connected = connected, dark = dark, fg = fg)

                Box(Modifier.weight(1f)) {
                    Crossfade(
                        targetState = page,
                        animationSpec = tween(durationMillis = 190),
                        label = "מעבר בין מסכים"
                    ) { screen ->
                        when (screen) {
                            0 -> HomeScreen(
                                fg = fg,
                                card = card,
                                connected = connected,
                                car = vehicleName,
                                profile = profile,
                                onProfile = {
                                    profile = it
                                    prefs.edit().putBoolean("profile_enabled", it).apply()
                                    if (connected) {
                                        if (it) DrivingProfile.apply(context)
                                        else DrivingProfile.restore(context, force = true)
                                    }
                                },
                                onCars = { page = 1 },
                                onSettings = { page = 3 }
                            )

                            1 -> CarsScreen(
                                fg = fg,
                                card = card,
                                name = vehicleName,
                                address = address,
                                onRename = {
                                    vehicleName = it
                                    prefs.edit().putString("vehicle_name", it).apply()
                                },
                                onChoose = {
                                    vehicleName = it.first
                                    address = it.second
                                    prefs.edit()
                                        .putString("vehicle_name", it.first)
                                        .putString("vehicle_address", it.second)
                                        .apply()
                                    if (connected) DrivingProfile.restore(context, force = true)
                                    connected = false
                                    page = 0
                                }
                            )

                            2 -> ProfileScreen(
                                fg = fg,
                                card = card,
                                enabled = profile,
                                calls = allowCalls,
                                messages = allowMessages,
                                media = allowMedia,
                                alarms = allowAlarms,
                                restore = restore,
                                onProfile = {
                                    profile = it
                                    prefs.edit().putBoolean("profile_enabled", it).apply()
                                    if (connected) {
                                        if (it) DrivingProfile.apply(context)
                                        else DrivingProfile.restore(context, force = true)
                                    }
                                },
                                onCalls = {
                                    allowCalls = it
                                    prefs.edit().putBoolean("allow_calls", it).apply()
                                    if (connected) DrivingProfile.apply(context)
                                },
                                onMessages = {
                                    allowMessages = it
                                    prefs.edit().putBoolean("allow_messages", it).apply()
                                    if (connected) DrivingProfile.apply(context)
                                },
                                onMedia = {
                                    allowMedia = it
                                    prefs.edit().putBoolean("allow_media", it).apply()
                                    if (connected) DrivingProfile.apply(context)
                                },
                                onAlarms = {
                                    allowAlarms = it
                                    prefs.edit().putBoolean("allow_alarms", it).apply()
                                    if (connected) DrivingProfile.apply(context)
                                },
                                onRestore = {
                                    restore = it
                                    prefs.edit().putBoolean("restore_dnd", it).apply()
                                },
                                onPermission = {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                                }
                            )

                            else -> SettingsScreen(
                                fg = fg,
                                card = card,
                                dark = dark,
                                onDark = {
                                    dark = it
                                    prefs.edit().putBoolean("dark_theme", it).apply()
                                },
                                onDnd = {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                                },
                                onAuto = { openAndroidAuto(context) }
                            )
                        }
                    }
                }

                NavigationBar(
                    containerColor = if (dark) Color(0xFF06101E) else Color.White
                ) {
                    val labels = listOf("בית", "הרכב שלי", "פרופיל נהיגה", "הגדרות")
                    labels.forEachIndexed { index, label ->
                        val scale by animateFloatAsState(
                            targetValue = if (page == index) 1.07f else 1f,
                            animationSpec = tween(160),
                            label = "הדגשת סמל"
                        )
                        NavigationBarItem(
                            selected = page == index,
                            onClick = { page = index },
                            icon = {
                                Icon(
                                    imageVector = when (index) {
                                        0 -> Icons.Rounded.Home
                                        1 -> Icons.Rounded.DirectionsCar
                                        2 -> Icons.Rounded.Tune
                                        else -> Icons.Rounded.Settings
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                )
                            },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Blue,
                                selectedTextColor = Blue,
                                unselectedIconColor = if (dark) Color.LightGray else Color.Gray,
                                unselectedTextColor = if (dark) Color.LightGray else Color.Gray
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppHeader(connected: Boolean, dark: Boolean, fg: Color) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("CarMute", color = fg, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text(
                "שקט בדרך. שליטה בידיים שלך.",
                color = if (dark) Color.LightGray else Color.Gray,
                fontSize = 12.sp
            )
        }
        Surface(
            shape = CircleShape,
            color = if (connected) Color(0xFF174D43) else if (dark) Panel else Color(0xFFE8EDF6)
        ) {
            Icon(
                imageVector = if (connected) Icons.Rounded.BluetoothConnected else Icons.Rounded.DirectionsCar,
                contentDescription = if (connected) "מחובר לרכב" else "ממתין לחיבור",
                tint = if (connected) Green else if (dark) Color.White else Ink,
                modifier = Modifier.padding(12.dp).size(22.dp)
            )
        }
    }
}

@Composable
private fun HomeScreen(
    fg: Color,
    card: Color,
    connected: Boolean,
    car: String,
    profile: Boolean,
    onProfile: (Boolean) -> Unit,
    onCars: () -> Unit,
    onSettings: () -> Unit
) {
    var introVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { introVisible = true }

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnimatedVisibility(
                visible = introVisible,
                enter = fadeIn(tween(220)) + slideInVertically(
                    animationSpec = tween(220),
                    initialOffsetY = { it / 14 }
                )
            ) {
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF173C70), Color(0xFF09182B))
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column {
                        Crossfade(
                            targetState = connected,
                            animationSpec = tween(180),
                            label = "מצב חיבור"
                        ) { isConnected ->
                            Text(
                                if (isConnected) "הרכב מחובר" else "מוכנים לנסיעה?",
                                color = Color(0xFF9DBEFF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Crossfade(
                            targetState = connected,
                            animationSpec = tween(180),
                            label = "כותרת רכב"
                        ) { isConnected ->
                            Text(
                                if (isConnected) car else "ברוכים הבאים ל‑CarMute",
                                color = Color.White,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            if (connected) "פרופיל הנהיגה שלך פעיל"
                            else "מתחברים לרכב ומפעילים שקט באופן אוטומטי",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(22.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (connected) Color(0xFF1B725D) else Color(0xFF203B5C),
                                shape = CircleShape
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.Bluetooth,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(7.dp))
                                    Text(
                                        if (connected) "מחובר באמצעות Bluetooth" else "ממתין לחיבור לרכב",
                                        color = Color.White,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            val carTilt by animateFloatAsState(
                                targetValue = if (connected) -4f else 0f,
                                animationSpec = tween(260),
                                label = "תנועת סמל הרכב"
                            )
                            Icon(
                                Icons.Rounded.DirectionsCar,
                                contentDescription = "רכב",
                                tint = Color(0xFF8FB5FF),
                                modifier = Modifier.size(42.dp).graphicsLayer {
                                    rotationZ = carTilt
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("מצב נהיגה", color = fg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Switch(checked = profile, onCheckedChange = onProfile)
            }
            Surface(color = card, shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.DoNotDisturbOn,
                            contentDescription = null,
                            tint = Color(0xFF5B8CFF),
                            modifier = Modifier.size(29.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("נא לא להפריע", color = fg, fontWeight = FontWeight.SemiBold)
                            Text(
                                "השתקת שיחות והתראות בזמן הנהיגה",
                                color = Muted,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            if (profile) "פעיל" else "כבוי",
                            color = if (profile) Green else Muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    AnimatedVisibility(
                        visible = connected && profile,
                        enter = fadeIn(tween(180)),
                        exit = fadeOut(tween(140))
                    ) {
                        Text(
                            "הפרופיל יופעל כל עוד הרכב מחובר.",
                            color = Green,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "המוזיקה יכולה להמשיך להתנגן. אפשר לבחור אילו שיחות, הודעות והתראות יורשו בפרופיל הנהיגה.",
                        color = Muted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Text("קיצורי דרך", color = fg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickCard(
                    title = "בחירת רכב",
                    subtitle = "ניהול חיבור Bluetooth",
                    icon = Icons.Rounded.DirectionsCar,
                    card = card,
                    fg = fg,
                    modifier = Modifier.weight(1f),
                    click = onCars
                )
                QuickCard(
                    title = "Android Auto",
                    subtitle = "פתיחת הגדרות המערכת",
                    icon = Icons.Rounded.Android,
                    card = card,
                    fg = fg,
                    modifier = Modifier.weight(1f),
                    click = onSettings
                )
            }
        }
    }
}

@Composable
private fun QuickCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    card: Color,
    fg: Color,
    modifier: Modifier,
    click: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = click),
        color = card,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = Color(0xFF5B8CFF), modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun CarsScreen(
    fg: Color,
    card: Color,
    name: String,
    address: String?,
    onRename: (String) -> Unit,
    onChoose: (Pair<String, String>) -> Unit
) {
    val context = LocalContext.current
    var showRename by remember { mutableStateOf(false) }
    var editedName by remember(name) { mutableStateOf(name) }
    var devices by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter: BluetoothAdapter? = manager.adapter
            @Suppress("MissingPermission")
            devices = adapter?.bondedDevices
                ?.map { Pair(it.name ?: "התקן Bluetooth", it.address) }
                ?.sortedBy { it.first.lowercase() } ?: emptyList()
        } catch (_: SecurityException) {
            error = "כדי להציג מכשירים מצומדים, יש לאשר גישה למכשירים בסביבה."
        } catch (_: Exception) {
            error = "לא הצלחנו לקרוא את רשימת מכשירי ה‑Bluetooth."
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("הרכב שלי", color = fg, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("בחרו את חיבור ה‑Bluetooth של הרכב שלכם.", color = Muted, fontSize = 13.sp)
        }
        item {
            Surface(color = card, shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(17.dp)) {
                    Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = Color(0xFF5B8CFF), modifier = Modifier.size(34.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(name, color = fg, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    Text(
                        if (address == null) "עדיין לא נבחר רכב" else "הרכב שנבחר לניהול אוטומטי",
                        color = Muted,
                        fontSize = 12.sp
                    )
                    if (address != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "עריכת שם הרכב",
                            color = Color(0xFF6C9BFF),
                            fontSize = 12.sp,
                            modifier = Modifier.clickable {
                                editedName = name
                                showRename = true
                            }
                        )
                    }
                }
            }
        }
        item {
            Text("מכשירי Bluetooth מצומדים", color = fg, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            if (error != null) {
                Text(error!!, color = Color(0xFFE09A55), fontSize = 12.sp)
            }
            if (devices.isEmpty()) {
                Text(
                    "לא נמצאו מכשירים מצומדים. תחילה יש לצמד את הרכב דרך הגדרות ה‑Bluetooth בטלפון.",
                    color = Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Bluetooth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("פתיחת הגדרות Bluetooth")
                }
            }
        }
        items(devices, key = { it.second }) { device ->
            Surface(
                color = card,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().clickable { onChoose(device) }
            ) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Bluetooth, contentDescription = null, tint = Color(0xFF5B8CFF))
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(device.first, color = fg, fontWeight = FontWeight.Medium)
                        Text(device.second, color = Muted, fontSize = 10.sp)
                    }
                    Text(
                        if (device.second == address) "נבחר" else "בחירה",
                        color = if (device.second == address) Green else Color(0xFF6C9BFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("עריכת שם הרכב") },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    singleLine = true,
                    label = { Text("השם שיוצג באפליקציה") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editedName.isNotBlank()) onRename(editedName.trim())
                    showRename = false
                }) { Text("שמירה") }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text("ביטול") }
            }
        )
    }
}

@Composable
private fun ProfileScreen(
    fg: Color,
    card: Color,
    enabled: Boolean,
    calls: Boolean,
    messages: Boolean,
    media: Boolean,
    alarms: Boolean,
    restore: Boolean,
    onProfile: (Boolean) -> Unit,
    onCalls: (Boolean) -> Unit,
    onMessages: (Boolean) -> Unit,
    onMedia: (Boolean) -> Unit,
    onAlarms: (Boolean) -> Unit,
    onRestore: (Boolean) -> Unit,
    onPermission: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Text("פרופיל נהיגה", color = fg, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text(
            "כאן קובעים מה יקרה לטלפון כשהוא יתחבר לרכב.",
            color = Muted,
            fontSize = 13.sp
        )
        SettingRow("הפעלת פרופיל אוטומטית", "הפעלת מצב „נא לא להפריע” בחיבור לרכב", enabled, onProfile, fg, card)
        SettingRow("אפשר שיחות", "שיחות נכנסות יוכלו לצלצל", calls, onCalls, fg, card)
        SettingRow("אפשר התראות הודעות", "הצגת התראות על הודעות חדשות", messages, onMessages, fg, card)
        SettingRow("אפשר שמע ומוזיקה", "השארת שמע המדיה פעיל", media, onMedia, fg, card)
        SettingRow("אפשר שעונים מעוררים", "צלצול השעונים המעוררים יישאר פעיל", alarms, onAlarms, fg, card)
        SettingRow("שחזור לאחר ניתוק", "החזרת הגדרות הצליל שהיו לפני החיבור", restore, onRestore, fg, card)

        Surface(color = card, shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("הרשאת „נא לא להפריע”", color = fg, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "כדי ש‑CarMute תוכל להפעיל ולכבות את מצב הנהיגה בעצמה, צריך לאשר לה גישה מיוחדת בהגדרות הטלפון.",
                    color = Muted,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onPermission, modifier = Modifier.fillMaxWidth()) {
                    Text("פתיחת הגדרות ההרשאה")
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    fg: Color,
    card: Color
) {
    Surface(color = card, shape = RoundedCornerShape(18.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, color = Muted, fontSize = 11.sp)
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun SettingsScreen(
    fg: Color,
    card: Color,
    dark: Boolean,
    onDark: (Boolean) -> Unit,
    onDnd: () -> Unit,
    onAuto: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("הגדרות", color = fg, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        SettingRow("מראה כהה", "שימוש בעיצוב הכחול והכהה", dark, onDark, fg, card)

        ActionRow(
            title = "הרשאת „נא לא להפריע”",
            subtitle = "פתיחת הגדרות ההרשאה של Android",
            icon = Icons.Rounded.DoNotDisturbOn,
            fg = fg,
            card = card,
            onClick = onDnd
        )
        ActionRow(
            title = "Android Auto",
            subtitle = "פתיחת הגדרות Android Auto בטלפון",
            icon = Icons.Rounded.Android,
            fg = fg,
            card = card,
            onClick = onAuto
        )

        Surface(color = card, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("על CarMute", color = fg, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(5.dp))
                Text(
                    "CarMute עוזרת לנהל את השקט בטלפון בזמן שהרכב מחובר.",
                    color = Muted,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                Text("גרסה 1.0.2", color = Muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    fg: Color,
    card: Color,
    onClick: () -> Unit
) {
    Surface(
        color = card,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFF5B8CFF))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, color = Muted, fontSize = 11.sp)
            }
            Icon(Icons.Rounded.ChevronLeft, contentDescription = null, tint = Muted)
        }
    }
}

private fun openAndroidAuto(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:com.google.android.projection.gearhead")
    )
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}
