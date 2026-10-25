package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ScreenTab
import com.example.ui.theme.*

data class NavTabItem(
    val tab: ScreenTab,
    val labelBn: String,
    val labelEn: String,
    val icon: ImageVector
)

@Composable
fun AppBottomNav(
    activeTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit,
    language: String,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavTabItem(ScreenTab.SAVER, "সেভার", "Saver", Icons.Default.Download),
        NavTabItem(ScreenTab.HISTORY, "হিস্ট্রি", "History", Icons.Default.History),
        NavTabItem(ScreenTab.PWA_HUB, "PWA/ওয়েব", "PWA Hub", Icons.Default.Language),
        NavTabItem(ScreenTab.GUIDE, "গাইড", "Guide", Icons.Default.HelpOutline),
        NavTabItem(ScreenTab.TESTS, "টেস্ট", "Tests", Icons.Default.Science),
        NavTabItem(ScreenTab.SETTINGS, "সেটিংস", "Settings", Icons.Default.Settings)
    )

    Surface(
        color = DarkSurface.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = activeTab == item.tab
                val label = if (language == "bn") item.labelBn else item.labelEn

                Column(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 52.dp, minHeight = 52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(item.tab) }
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                        .testTag("nav_${item.tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) BrandCyan.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent
                            )
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = label,
                            tint = if (isSelected) BrandCyan else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) BrandCyan else TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
