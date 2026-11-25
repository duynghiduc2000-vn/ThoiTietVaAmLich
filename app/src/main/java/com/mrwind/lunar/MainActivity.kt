package com.mrwind.lunar

import android.Manifest
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mrwind.lunar.data.AppConfig
import com.mrwind.lunar.data.AppPrefs
import com.mrwind.lunar.data.LunarCalendar
import com.mrwind.lunar.data.TodoItem
import com.mrwind.lunar.data.WeatherFetcher
import com.mrwind.lunar.widget.LunarWeatherWidget4x2
import com.mrwind.lunar.widget.LunarWeatherWidget4x4
import com.mrwind.lunar.widget.WidgetRenderer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class ThemeOption(
    val key: String,
    val label: String,
    val color: Color
)

private val THEME_OPTIONS = listOf(
    ThemeOption("SKY_LIGHT", "Xanh trời sáng", Color(0xFF0284C7)),
    ThemeOption("MINT_LIGHT", "Bạc hà sáng", Color(0xFF10B981)),
    ThemeOption("LEAF_GREEN", "Xanh lá non", Color(0xFF22C55E)),
    ThemeOption("SUN_YELLOW", "Vàng nắng", Color(0xFFEAB308)),
    ThemeOption("PEACH_ORANGE", "Cam đào", Color(0xFFF97316)),
    ThemeOption("PINK_ROSE", "Hồng phấn", Color(0xFFEC4899))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var config by remember { mutableStateOf(AppPrefs.loadConfig(context)) }

            val isDark = when (config.appThemeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            val accent = THEME_OPTIONS.firstOrNull { it.key == config.colorThemeKey }?.color
                ?: Color(0xFF0284C7)

            val colorScheme = if (isDark) {
                darkColorScheme(
                    primary = accent,
                    background = Color(0xFF111014),
                    surface = Color(0xFF1A1820)
                )
            } else {
                lightColorScheme(
                    primary = accent,
                    background = Color(0xFFF4F5F8),
                    surface = Color(0xFFFFFFFF)
                )
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LunarWeatherScreen(
                        config = config,
                        onConfigChange = { updated ->
                            config = updated
                            AppPrefs.saveConfig(context, updated)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LunarWeatherScreen(
    config: AppConfig,
    onConfigChange: (AppConfig) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nowClock by remember { mutableStateOf(currentClockStr(config.showClockSeconds)) }
    var todoTime by remember { mutableStateOf("08:00") }
    var todoInput by remember { mutableStateOf("") }
    var previewTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(config.showClockSeconds, config.todos) {
        while (true) {
            nowClock = currentClockStr(config.showClockSeconds)
            previewTick++
            delay(1000L)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            val loc = getLastKnownCoords(context)
            if (loc != null) {
                onConfigChange(
                    config.copy(
                        latitude = String.format(Locale.US, "%.4f", loc.first),
                        longitude = String.format(Locale.US, "%.4f", loc.second)
                    )
                )
                Toast.makeText(context, "Đã cập nhật tọa độ GPS", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Không lấy được GPS, hãy bật Vị trí", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Bạn chưa cấp quyền Vị trí", Toast.LENGTH_SHORT).show()
        }
    }

    val scrollState = rememberScrollState()
    val cardBg = MaterialTheme.colorScheme.surface
    val accentColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text(
                text = "Thời tiết và Âm lịch",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Tùy chỉnh giao diện, vị trí và hiệu ứng cho widget",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        // 1. Khung Xem trước widget
        SectionCard(cardBg = cardBg) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Xem trước widget",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Mô phỏng nhanh màu nền, chữ và lớp hiệu ứng thời tiết",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
                OutlinedButton(
                    onClick = {
                        val nextSize = if (config.widgetSize == "4x2") "4x4" else "4x2"
                        onConfigChange(config.copy(widgetSize = nextSize))
                    }
                ) {
                    Text(text = "Khung ${config.widgetSize}")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            WidgetPreviewBox(
                config = config,
                clockText = nowClock,
                accentColor = accentColor,
                tick = previewTick
            )
        }

        // 2. Nền ứng dụng & Màu chủ đề
        SectionCard(cardBg = cardBg) {
            Text(
                text = "Nền ứng dụng",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val modes = listOf("SYSTEM" to "Hệ thống", "LIGHT" to "Nền sáng", "DARK" to "Nền tối")
                modes.forEach { (key, label) ->
                    val selected = config.appThemeMode == key
                    OutlinedButton(
                        onClick = { onConfigChange(config.copy(appThemeMode = key)) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) accentColor.copy(alpha = 0.18f) else Color.Transparent
                        )
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Màu chủ đề",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Vuốt ngang để xem thêm các màu sáng và màu gốc",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                THEME_OPTIONS.forEach { item ->
                    val active = config.colorThemeKey == item.key
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (active) accentColor.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                            )
                            .border(
                                width = if (active) 1.5.dp else 0.5.dp,
                                color = if (active) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onConfigChange(config.copy(colorThemeKey = item.key)) }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(item.color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.label,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 3. Vị trí & Chữ ký
        SectionCard(cardBg = cardBg) {
            Text(
                text = "Vị trí & Chữ ký",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Lấy vị trí GPS hiện tại", color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = config.locationName,
                onValueChange = { onConfigChange(config.copy(locationName = it)) },
                label = { Text("Tên địa điểm") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = config.latitude,
                    onValueChange = { onConfigChange(config.copy(latitude = it)) },
                    label = { Text("Vĩ độ (Lat)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = config.longitude,
                    onValueChange = { onConfigChange(config.copy(longitude = it)) },
                    label = { Text("Kinh độ (Lon)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Chữ ký cá nhân",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Hiển thị ở góc phải dưới widget",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = config.signature,
                onValueChange = { onConfigChange(config.copy(signature = it)) },
                placeholder = { Text("Nhập chữ ký của bạn...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 4. Cỡ chữ Âm lịch trên widget
        SectionCard(cardBg = cardBg) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cỡ chữ Âm lịch trên widget",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${config.lunarFontSizeSp.toInt()}sp",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            Text(
                text = "Kéo thanh trượt để tăng cỡ chữ giúp người lớn tuổi dễ đọc",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
            Slider(
                value = config.lunarFontSizeSp,
                onValueChange = { onConfigChange(config.copy(lunarFontSizeSp = it)) },
                valueRange = 12f..28f,
                steps = 15
            )
        }

        // 5. Việc cần làm trong ngày (tối đa 10 việc)
        SectionCard(cardBg = cardBg) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Việc cần làm trong ngày",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${config.todos.size}/10",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Text(
                text = "Hiển thị lần lượt mỗi 1 phút trên widget • Tự xóa khi sang ngày mới",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                todoTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            true
                        ).show()
                    }
                ) {
                    Text(text = todoTime, color = MaterialTheme.colorScheme.onSurface)
                }

                OutlinedTextField(
                    value = todoInput,
                    onValueChange = { todoInput = it },
                    placeholder = { Text("Nhập việc cần làm...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        if (config.todos.size >= 10) {
                            Toast.makeText(context, "Tối đa 10 việc mỗi ngày", Toast.LENGTH_SHORT).show()
                        } else if (todoInput.isNotBlank()) {
                            val newList = config.todos + TodoItem(
                                id = System.currentTimeMillis(),
                                time = todoTime,
                                title = todoInput.trim()
                            )
                            todoInput = ""
                            onConfigChange(config.copy(todos = newList))
                        }
                    }
                ) {
                    Text("Thêm")
                }
            }

            if (config.todos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                config.todos.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                            .clickable {
                                onConfigChange(config.copy(todos = config.todos.filterNot { it.id == item.id }))
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.time} • ${item.title}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Xóa",
                            fontSize = 12.sp,
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 6. Kiểu hiển thị widget & Tùy chọn hiển thị
        SectionCard(cardBg = cardBg) {
            Text(
                text = "Kiểu hiển thị widget",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))

            val styles = listOf(
                "TRANSPARENT" to "Trong suốt hoàn toàn",
                "LIQUID_DARK" to "Kính lỏng tối (Liquid Glass)",
                "FROSTED_LIGHT" to "Kính mờ sáng",
                "THEME_COLOR" to "Màu chủ đề của app"
            )
            styles.forEach { (key, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onConfigChange(config.copy(widgetStyle = key)) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = config.widgetStyle == key,
                        onClick = { onConfigChange(config.copy(widgetStyle = key)) }
                    )
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tùy chọn hiển thị",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hiển thị giây trên đồng hồ (nhảy giây liên tục)",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = config.showClockSeconds,
                    onCheckedChange = { onConfigChange(config.copy(showClockSeconds = it)) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hiển thị hiệu ứng thời tiết trên widget",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = config.showWeatherEffect,
                    onCheckedChange = { onConfigChange(config.copy(showWeatherEffect = it)) }
                )
            }
        }

        // 7. Ghim widget ra màn hình chính & Lưu cập nhật
        SectionCard(cardBg = cardBg) {
            Text(
                text = "Ghim widget ra màn hình chính",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                listOf("4x2", "4x4").forEach { sizeKey ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onConfigChange(config.copy(widgetSize = sizeKey)) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = config.widgetSize == sizeKey,
                            onClick = { onConfigChange(config.copy(widgetSize = sizeKey)) }
                        )
                        Text(text = sizeKey.replace("x", " x "), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = { pinSelectedWidget(context, config.widgetSize) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ghim Widget ra màn hình chính", color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        val weather = WeatherFetcher.fetchWeather(config.latitude, config.longitude)
                        val updated = if (weather != null) {
                            config.copy(
                                tempDisplay = weather.tempText,
                                weatherDescDisplay = weather.descText
                            )
                        } else {
                            config
                        }
                        AppPrefs.saveConfig(context, updated)
                        onConfigChange(updated)
                        WidgetRenderer.updateAllWidgets(context)
                        Toast.makeText(context, "Đã lưu & cập nhật Widget", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Lưu & Cập nhật Widget")
            }
        }

        Text(
            text = "Được tạo ra bởi Mr.Wind - Xiaomi Community Việt Nam",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun SectionCard(
    cardBg: Color,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            content()
        }
    }
}

@Composable
private fun WidgetPreviewBox(
    config: AppConfig,
    clockText: String,
    accentColor: Color,
    tick: Int
) {
    val is4x4 = config.widgetSize == "4x4"
    val boxHeight = if (is4x4) 240.dp else 132.dp

    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))
    val solarText = LunarCalendar.formatSolarLine(cal)
    val lunarText = LunarCalendar.formatLunarLine(cal)

    val line2Text = if (config.todos.isNotEmpty()) {
        val idx = ((tick / 60) % config.todos.size)
        val item = config.todos[idx]
        "Việc (${idx + 1}/${config.todos.size}) • ${item.time}: ${item.title}"
    } else {
        lunarText
    }

    val bgBrush = when (config.widgetStyle) {
        "TRANSPARENT" -> Brush.linearGradient(listOf(Color(0x18FFFFFF), Color(0x08FFFFFF)))
        "FROSTED_LIGHT" -> Brush.linearGradient(listOf(Color(0xE6FFFFFF), Color(0xCCB4C2D4)))
        "THEME_COLOR" -> Brush.linearGradient(listOf(accentColor.copy(alpha = 0.85f), accentColor.copy(alpha = 0.65f)))
        else -> Brush.linearGradient(listOf(Color(0xCC242632), Color(0xD915161D)))
    }

    val textColor = if (config.widgetStyle == "FROSTED_LIGHT") Color(0xFF111827) else Color.White
    val subTextColor = if (config.widgetStyle == "FROSTED_LIGHT") Color(0xFF374151) else Color(0xFFDCE2EC)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(boxHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(bgBrush)
            .border(
                width = 1.dp,
                color = if (config.widgetStyle == "FROSTED_LIGHT") Color(0x88000000) else Color(0x33FFFFFF),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1.3f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = solarText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = line2Text,
                    fontSize = config.lunarFontSizeSp.sp,
                    color = subTextColor,
                    maxLines = if (is4x4) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = clockText,
                    fontSize = if (config.showClockSeconds) 28.sp else 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1
                )
            }

            Column(
                modifier = Modifier.weight(0.8f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = config.tempDisplay,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = config.locationName.ifBlank { "Nghị Đức" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (config.showWeatherEffect) {
                    Text(
                        text = config.weatherDescDisplay,
                        fontSize = 11.sp,
                        color = subTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (config.signature.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = config.signature,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = subTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun currentClockStr(showSeconds: Boolean): String {
    val pattern = if (showSeconds) "HH:mm:ss" else "HH:mm"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
}

private fun getLastKnownCoords(context: Context): Pair<Double, Double>? {
    val fineOk = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarseOk = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fineOk && !coarseOk) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    return loc?.let { it.latitude to it.longitude }
}

private fun pinSelectedWidget(context: Context, sizeKey: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val mgr = AppWidgetManager.getInstance(context)
        if (mgr.isRequestPinAppWidgetSupported) {
            val provider = if (sizeKey == "4x4") {
                ComponentName(context, LunarWeatherWidget4x4::class.java)
            } else {
                ComponentName(context, LunarWeatherWidget4x2::class.java)
            }
            mgr.requestPinAppWidget(provider, null, null)
        } else {
            Toast.makeText(context, "Trình khởi chạy không hỗ trợ ghim trực tiếp, hãy thêm thủ công", Toast.LENGTH_LONG).show()
        }
    } else {
        Toast.makeText(context, "Hãy giữ màn hình chính để thêm Widget", Toast.LENGTH_LONG).show()
    }
}
