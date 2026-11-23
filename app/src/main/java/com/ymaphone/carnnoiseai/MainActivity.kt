package com.ymaphone.carnoiseai

import android.Manifest
import android.app.NotificationManager
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private val Navy = Color(0xFF071426)
private val Panel = Color(0xFF12243A)
private val Blue = Color(0xFF2864F0)
private val Ink = Color(0xFF111827)

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        setContent { CarNoiseApp() }
    }
}

@Composable
fun CarNoiseApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("carnoise", Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf(0) }
    var connected by remember { mutableStateOf(prefs.getBoolean("vehicle_connected", false)) }
    var profile by remember { mutableStateOf(prefs.getBoolean("profile_enabled", true)) }
    var vehicleName by remember { mutableStateOf(prefs.getString("vehicle_name", "My Car") ?: "My Car") }
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

    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary=Blue, background=bg, surface=card) else lightColorScheme(primary=Blue, background=bg, surface=card)) {
        Column(Modifier.fillMaxSize().background(bg)) {
            Row(Modifier.fillMaxWidth().padding(horizontal=22.dp, vertical=18.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween) {
                Column {
                    Text("CarNoiseAI", color=fg, fontSize=24.sp, fontWeight=FontWeight.Bold)
                    Text("Your drive, your rules", color=if(dark) Color.LightGray else Color.Gray, fontSize=12.sp)
                }
                Surface(shape=CircleShape, color=if(connected) Color(0xFF174D43) else card) {
                    Icon(if(connected) Icons.Rounded.BluetoothConnected else Icons.Rounded.Bluetooth, null, tint=if(connected) Color(0xFF48D7A0) else fg, modifier=Modifier.padding(12.dp))
                }
            }
            Box(Modifier.weight(1f)) {
                when(page) {
                    0 -> HomeScreen(fg, card, connected, vehicleName, profile, address != null,
                        onProfile={ profile=it; prefs.edit().putBoolean("profile_enabled",it).apply(); if(connected) { if(it) DrivingProfile.apply(context) else DrivingProfile.restore(context, force = true) } },
                        onCars={page=1}, onSettings={page=3})
                    1 -> CarsScreen(fg, card, vehicleName, address, onRename={ vehicleName=it; prefs.edit().putString("vehicle_name",it).apply() }, onChoose={
                        vehicleName=it.first; address=it.second
                        prefs.edit().putString("vehicle_name",it.first).putString("vehicle_address",it.second).apply()
                        if (connected) DrivingProfile.restore(context, force = true)
                        connected=false
                        page=0
                    })
                    2 -> ProfileScreen(fg, card, profile, allowCalls, allowMessages, allowMedia, allowAlarms, restore,
                        onProfile={profile=it;prefs.edit().putBoolean("profile_enabled",it).apply(); if(connected) { if(it) DrivingProfile.apply(context) else DrivingProfile.restore(context, force = true) }},
                        onCalls={allowCalls=it;prefs.edit().putBoolean("allow_calls",it).apply(); if(connected) DrivingProfile.apply(context)},
                        onMessages={allowMessages=it;prefs.edit().putBoolean("allow_messages",it).apply(); if(connected) DrivingProfile.apply(context)},
                        onMedia={allowMedia=it;prefs.edit().putBoolean("allow_media",it).apply(); if(connected) DrivingProfile.apply(context)},
                        onAlarms={allowAlarms=it;prefs.edit().putBoolean("allow_alarms",it).apply(); if(connected) DrivingProfile.apply(context)},
                        onRestore={restore=it;prefs.edit().putBoolean("restore_dnd",it).apply()},
                        onPermission={context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))})
                    else -> SettingsScreen(fg, card, dark, onDark={dark=it;prefs.edit().putBoolean("dark_theme",it).apply()},
                        onDnd={context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))},
                        onAuto={openAndroidAuto(context)})
                }
            }
            NavigationBar(containerColor=if(dark) Color(0xFF06101E) else Color.White) {
                listOf("Home","Cars","Profile","Settings").forEachIndexed { index, label ->
                    NavigationBarItem(selected=page==index, onClick={page=index}, icon={Icon(when(index){0->Icons.Rounded.Home;1->Icons.Rounded.DirectionsCar;2->Icons.Rounded.Tune;else->Icons.Rounded.Settings},null)}, label={Text(label)}, colors=NavigationBarItemDefaults.colors(selectedIconColor=Blue, selectedTextColor=Blue, unselectedIconColor=if(dark) Color.LightGray else Color.Gray, unselectedTextColor=if(dark) Color.LightGray else Color.Gray))
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(fg:Color, card:Color, connected:Boolean, car:String, profile:Boolean, hasCar:Boolean, onProfile:(Boolean)->Unit, onCars:()->Unit, onSettings:()->Unit) {
    LazyColumn(contentPadding=PaddingValues(20.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {
            Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF173C70),Color(0xFF09182B))), RoundedCornerShape(28.dp)).padding(22.dp)) {
                Column {
                    Text(if(connected) "CONNECTED" else "READY WHEN YOU ARE", color=Color(0xFF9DBEFF), fontSize=11.sp, fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text(if(connected) car else "Your car companion", color=Color.White, fontSize=25.sp, fontWeight=FontWeight.Bold)
                    Text(if(connected) "Driving profile is active" else "Connect to your vehicle Bluetooth", color=Color.LightGray, fontSize=13.sp)
                    Spacer(Modifier.height(22.dp))
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Surface(color=if(connected) Color(0xFF1B725D) else Color(0xFF203B5C), shape=CircleShape) {
                            Row(Modifier.padding(horizontal=13.dp, vertical=8.dp), verticalAlignment=Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Bluetooth, null, tint=Color.White, modifier=Modifier.size(16.dp)); Spacer(Modifier.width(7.dp))
                                Text(if(connected) "Bluetooth connected" else "Waiting for car", color=Color.White, fontSize=12.sp)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Rounded.DirectionsCar, null, tint=Color(0xFF8FB5FF), modifier=Modifier.size(36.dp))
                    }
                }
            }
        }
        item {
            Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween, modifier=Modifier.fillMaxWidth()) {
                Text("Driving profile", color=fg, fontSize=18.sp, fontWeight=FontWeight.Bold)
                Switch(checked=profile,onCheckedChange=onProfile)
            }
            Surface(color=card, shape=RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Rounded.DoNotDisturbOn, null, tint=Color(0xFF5B8CFF), modifier=Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text("Do Not Disturb",color=fg,fontWeight=FontWeight.SemiBold); Text("Silence calls and interruptions while driving",color=Color.Gray,fontSize=12.sp) }
                        Text(if(profile) "ON" else "OFF",color=if(profile) Color(0xFF41C99A) else Color.Gray,fontSize=11.sp,fontWeight=FontWeight.Bold)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Media playback stays available. Fine-tune allowed interruptions in Profile.",color=Color.Gray,fontSize=12.sp)
                }
            }
        }
        item {
            Text("Quick actions",color=fg,fontSize=18.sp,fontWeight=FontWeight.Bold)
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                QuickCard("My vehicle",Icons.Rounded.DirectionsCar,card,fg,Modifier.weight(1f),onCars)
                QuickCard("Android Auto",Icons.Rounded.Android,card,fg,Modifier.weight(1f),onSettings)
            }
        }
    }
}

@Composable
private fun QuickCard(title:String, icon:androidx.compose.ui.graphics.vector.ImageVector, card:Color, fg:Color, modifier:Modifier, click:()->Unit) {
    Surface(modifier=modifier.clickable(onClick=click), color=card, shape=RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(17.dp)) { Icon(icon,null,tint=Color(0xFF5B8CFF),modifier=Modifier.size(27.dp)); Spacer(Modifier.height(12.dp)); Text(title,color=fg,fontWeight=FontWeight.SemiBold,fontSize=13.sp) }
    }
}

@Composable
private fun CarsScreen(fg:Color, card:Color, name:String, address:String?, onRename:(String)->Unit, onChoose:(Pair<String,String>)->Unit) {
    val context= LocalContext.current
    var showRename by remember { mutableStateOf(false) }
    var editedName by remember(name) { mutableStateOf(name) }
    var devices by remember { mutableStateOf(listOf<Pair<String,String>>()) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            val manager=context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter:BluetoothAdapter?=manager.adapter
            @Suppress("MissingPermission")
            devices=adapter?.bondedDevices?.map { Pair(it.name ?: "Bluetooth device",it.address) } ?: emptyList()
        } catch (_:SecurityException) { error="Allow Nearby devices permission to list paired vehicles." }
    }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Your vehicles",color=fg,fontSize=25.sp,fontWeight=FontWeight.Bold)
        Text("Choose the Bluetooth device used by your car.",color=Color.Gray,fontSize=13.sp)
        Spacer(Modifier.height(18.dp))
        Surface(color=card,shape=RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(16.dp)) {
                Icon(Icons.Rounded.DirectionsCar,null,tint=Color(0xFF5B8CFF),modifier=Modifier.size(32.dp))
                Spacer(Modifier.height(8.dp))
                Text(name,color=fg,fontWeight=FontWeight.Bold,fontSize=18.sp)
                Text(if(address==null) "No vehicle selected" else "Selected Bluetooth device",color=Color.Gray,fontSize=12.sp)
                if(address!=null) {
                    Spacer(Modifier.height(10.dp))
                    Text("Edit vehicle name",color=Color(0xFF6C9BFF),fontSize=12.sp,modifier=Modifier.clickable { editedName=name; showRename=true })
                }
            }
        }
        if(showRename) {
            AlertDialog(
                onDismissRequest={showRename=false},
                title={Text("Vehicle name")},
                text={ OutlinedTextField(value=editedName,onValueChange={editedName=it},singleLine=true,label={Text("Name shown in CarNoiseAI")}) },
                confirmButton={ TextButton(onClick={ if(editedName.isNotBlank()) onRename(editedName.trim()); showRename=false }) { Text("Save") } },
                dismissButton={ TextButton(onClick={showRename=false}) { Text("Cancel") } }
            )
        }
        Spacer(Modifier.height(18.dp))
        Text("Paired Bluetooth devices",color=fg,fontWeight=FontWeight.SemiBold)
        if(error!=null) Text(error!!,color=Color(0xFFE09A55))
        if(devices.isEmpty()) Text("No paired devices found. Pair your car in Android Bluetooth settings first.",color=Color.Gray,modifier=Modifier.padding(top=12.dp))
        devices.forEach { device ->
            Surface(color=card,shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth().padding(top=9.dp).clickable { onChoose(device) }) {
                Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Bluetooth,null,tint=Color(0xFF5B8CFF))
                    Column(Modifier.weight(1f).padding(start=12.dp)) { Text(device.first,color=fg,fontWeight=FontWeight.Medium); Text(device.second,color=Color.Gray,fontSize=11.sp) }
                    Text(if(device.second==address) "SELECTED" else "SELECT",color=Color(0xFF6C9BFF),fontSize=11.sp,fontWeight=FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(fg:Color,card:Color,enabled:Boolean,calls:Boolean,messages:Boolean,media:Boolean,alarms:Boolean,restore:Boolean,onProfile:(Boolean)->Unit,onCalls:(Boolean)->Unit,onMessages:(Boolean)->Unit,onMedia:(Boolean)->Unit,onAlarms:(Boolean)->Unit,onRestore:(Boolean)->Unit,onPermission:()->Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("Driving profile",color=fg,fontSize=25.sp,fontWeight=FontWeight.Bold)
        Text("Choose what happens automatically when your car connects.",color=Color.Gray,fontSize=13.sp)
        SettingRow("Automatic profile","Enable DND when the selected car connects",enabled,onProfile,fg,card)
        SettingRow("Allow calls","Let incoming calls ring while driving",calls,onCalls,fg,card)
        SettingRow("Allow messages","Allow message alerts while driving",messages,onMessages,fg,card)
        SettingRow("Allow media","Keep music and media audio playing",media,onMedia,fg,card)
        SettingRow("Allow alarms","Alarm notifications can still sound",alarms,onAlarms,fg,card)
        SettingRow("Restore after disconnect","Turn off DND when the car disconnects",restore,onRestore,fg,card)
        Surface(color=card,shape=RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Sound behavior",color=fg,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Default setup: calls and messages are silenced, while music and media audio remain available. Change each rule above. Android and the phone manufacturer may apply additional DND behavior.",color=Color.Gray,fontSize=12.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick=onPermission,modifier=Modifier.fillMaxWidth()) { Text("Grant Do Not Disturb access") }
            }
        }
    }
}

@Composable
private fun SettingRow(title:String,subtitle:String,checked:Boolean,onChange:(Boolean)->Unit,fg:Color,card:Color) {
    Surface(color=card,shape=RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title,color=fg,fontWeight=FontWeight.SemiBold); Text(subtitle,color=Color.Gray,fontSize=11.sp) }
            Switch(checked=checked,onCheckedChange=onChange)
        }
    }
}

@Composable
private fun SettingsScreen(fg:Color,card:Color,dark:Boolean,onDark:(Boolean)->Unit,onDnd:()->Unit,onAuto:()->Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("Settings",color=fg,fontSize=25.sp,fontWeight=FontWeight.Bold)
        SettingRow("Dark appearance","Use the midnight blue interface",dark,onDark,fg,card)
        Surface(color=card,shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth().clickable(onClick=onDnd)) {
            Row(Modifier.padding(17.dp),verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Rounded.DoNotDisturbOn,null,tint=Color(0xFF5B8CFF)); Column(Modifier.weight(1f).padding(start=12.dp)) { Text("DND permissions",color=fg,fontWeight=FontWeight.SemiBold); Text("Open Android access settings",color=Color.Gray,fontSize=12.sp) }; Icon(Icons.Rounded.ChevronRight,null,tint=Color.Gray) }
        }
        Surface(color=card,shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth().clickable(onClick=onAuto)) {
            Row(Modifier.padding(17.dp),verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Rounded.Android,null,tint=Color(0xFF5B8CFF)); Column(Modifier.weight(1f).padding(start=12.dp)) { Text("Android Auto",color=fg,fontWeight=FontWeight.SemiBold); Text("Open Android Auto app settings",color=Color.Gray,fontSize=12.sp) }; Icon(Icons.Rounded.ChevronRight,null,tint=Color.Gray) }
        }
        Text("CarNoiseAI  •  Version 1.0.1",color=Color.Gray,fontSize=11.sp)
    }
}

private fun openAndroidAuto(context:Context) {
    val intent=Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:com.google.android.projection.gearhead"))
    try { context.startActivity(intent) } catch (_:Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
}
