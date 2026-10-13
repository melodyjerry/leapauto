package com.leapauto.app.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leapauto.app.ControlWidget
import com.leapauto.app.LeapmotorApi
import com.leapauto.app.R
import com.leapauto.app.ThemeManager
import com.leapauto.app.VehicleTheme
import com.leapauto.app.ui.theme.LocalAppDarkTheme
import com.leapauto.app.ui.theme.statusGood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectorBottomSheet(
    vin: String,
    currentThemeId: String,
    themes: List<VehicleTheme>,
    api: LeapmotorApi?,
    onDismiss: () -> Unit,
    onThemeApplied: (String) -> Unit
) {
    val context = LocalContext.current
    val isDark = LocalAppDarkTheme.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var downloadingThemeId by remember { mutableStateOf<String?>(null) }
    var activeThemeId by remember { mutableStateOf(currentThemeId) }

    val pageBgColor = if (isDark) Color(0xFF161A23) else Color(0xFFF7F8FA)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = pageBgColor,
        tonalElevation = 0.dp,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // 顶部标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "3D 展台与小组件主题",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "选中的主题将同时应用于爱车 3D 展台背景与桌面小组件皮肤",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_phosphor_x),
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 主题列表
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(themes, key = { it.themeId }) { theme ->
                    val isSelected = activeThemeId == theme.themeId
                    val isReady = ThemeManager.isThemeReady(context, theme.themeId)
                    val isDownloading = downloadingThemeId == theme.themeId

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !isDownloading) {
                                if (isSelected) return@clickable
                                if (isReady || theme.isDefault || theme.themeId == ThemeManager.DEFAULT_THEME_ID) {
                                    ThemeManager.setActiveThemeId(context, vin, theme.themeId)
                                    activeThemeId = theme.themeId
                                    ControlWidget.refreshData(context)
                                    onThemeApplied(theme.themeId)
                                    Toast.makeText(context, "已应用主题：${theme.name}", Toast.LENGTH_SHORT).show()
                                } else {
                                    downloadingThemeId = theme.themeId
                                    coroutineScope.launch(Dispatchers.IO) {
                                        // 1. 优先预拉取单张 3D 展台实景大图，秒级就绪
                                        val bgReady = ThemeManager.ensure3dBgDownloaded(context, theme)
                                        if (bgReady) {
                                            withContext(Dispatchers.Main) {
                                                ThemeManager.setActiveThemeId(context, vin, theme.themeId)
                                                activeThemeId = theme.themeId
                                                ControlWidget.refreshData(context)
                                                onThemeApplied(theme.themeId)
                                            }
                                        }
                                        // 2. 后台同步下载并解压全量主题包 (包含小组件等皮肤)
                                        val fullReady = if (api != null) {
                                            ThemeManager.syncThemePackage(context, api, theme)
                                        } else bgReady

                                        withContext(Dispatchers.Main) {
                                            downloadingThemeId = null
                                            if (fullReady || bgReady) {
                                                ThemeManager.setActiveThemeId(context, vin, theme.themeId)
                                                activeThemeId = theme.themeId
                                                ControlWidget.refreshData(context)
                                                onThemeApplied(theme.themeId)
                                                Toast.makeText(context, "已应用主题：${theme.name}", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "主题包下载失败，请检查网络", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) {
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            else Color(0xFF202634).copy(alpha = 0.90f)
                        } else {
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else Color.White
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 0.8.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else if (isDark) Color.White.copy(alpha = 0.10f)
                            else Color.Black.copy(alpha = 0.06f)
                        ),
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = theme.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (theme.tags.isNotEmpty()) {
                                        theme.tags.forEach { tag ->
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = MaterialTheme.statusGood.copy(alpha = 0.16f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = tag,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.statusGood
                                                )
                                            }
                                        }
                                    }
                                    if (theme.isDefault) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "默认",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(5.dp))

                                Text(
                                    text = theme.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // 状态标记 / 下载提示 / 选中指示
                            when {
                                isDownloading -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                isSelected -> {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_phosphor_check),
                                            contentDescription = "已选用",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                !isReady -> {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.40f))
                                    ) {
                                        Text(
                                            text = "下载并应用",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f))
                                    ) {
                                        Text(
                                            text = "应用",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
