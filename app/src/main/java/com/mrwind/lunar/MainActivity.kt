@Composable
private fun WidgetPreviewBox(
    config: AppConfig,
    clockText: String,
    accentColor: Color,
    tick: Int
) {
    val is4x4 = config.widgetSize == "4x4"
    val boxHeight = if (is4x4) 172.dp else 118.dp

    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))
    val solarText = LunarCalendar.formatSolarLine(cal)
    val lunarText = LunarCalendar.formatLunarLine(cal)

    val todoText = if (config.todos.isNotEmpty()) {
        val idx = ((tick / 60) % config.todos.size)
        val item = config.todos[idx]
        "${item.time} • ${item.title}"
    } else {
        ""
    }

    val bgBrush = when (config.widgetStyle) {
        "TRANSPARENT" -> Brush.linearGradient(listOf(Color(0x18FFFFFF), Color(0x08FFFFFF)))
        "FROSTED_LIGHT" -> Brush.linearGradient(listOf(Color(0xE6FFFFFF), Color(0xCCB4C2D4)))
        "THEME_COLOR" -> Brush.linearGradient(listOf(accentColor.copy(alpha = 0.85f), accentColor.copy(alpha = 0.65f)))
        else -> Brush.linearGradient(listOf(Color(0xCC242632), Color(0xD915161D)))
    }

    val textColor = if (config.widgetStyle == "FROSTED_LIGHT") Color(0xFF111827) else Color.White
    val subTextColor = if (config.widgetStyle == "FROSTED_LIGHT") Color(0xFF374151) else Color(0xFFDCE2EC)
    val emojiPrefix = if (config.showWeatherEffect) "${WidgetRenderer.weatherEmoji(config.weatherDescDisplay)} " else ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(boxHeight)
            .clip(RoundedCornerShape(18.dp))
            .background(bgBrush)
            .border(
                width = 1.dp,
                color = if (config.widgetStyle == "FROSTED_LIGHT") Color(0x88000000) else Color(0x33FFFFFF),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Top
        ) {
            // Hàng 1: Thứ + Âm lịch (trái) & Nhiệt độ (phải)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1.35f),
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = solarText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = lunarText,
                        fontSize = config.lunarFontSizeSp.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = subTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "$emojiPrefix${config.tempDisplay}",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = textColor,
                    maxLines = 1,
                    modifier = Modifier.weight(0.85f)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Hàng 2: Đồng hồ (trái) ngang hàng Tên địa điểm + Mô tả thời tiết (phải)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = clockText,
                    fontSize = if (config.showClockSeconds) 26.sp else 30.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = textColor,
                    maxLines = 1,
                    modifier = Modifier.weight(1.25f)
                )

                Column(
                    modifier = Modifier.weight(0.95f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = config.locationName.ifBlank { "Nghị Đức" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (config.showWeatherEffect) {
                        Text(
                            text = config.weatherDescDisplay,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = subTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (todoText.isNotBlank() || config.signature.isNotBlank()) {
                Spacer(modifier = Modifier.height(5.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = todoText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif,
                        color = textColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1.35f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = config.signature,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.SansSerif,
                        color = subTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(0.85f)
                    )
                }
            }
        }
    }
}
