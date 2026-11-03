package com.nothing.pdfmaker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.pdfmaker.ui.theme.NothingBlack
import com.nothing.pdfmaker.ui.theme.NothingBorder
import com.nothing.pdfmaker.ui.theme.NothingGray
import com.nothing.pdfmaker.ui.theme.NothingRed
import com.nothing.pdfmaker.ui.theme.NothingSurface
import com.nothing.pdfmaker.ui.theme.NothingWhite

enum class ScanColorMode {
    NATURAL_FAST,   // SCANNER_MODE_BASE: 0 saturation boost, ultra-fast capture & crop
    DOCUMENT_FULL   // SCANNER_MODE_FULL: full editing & paper filters
}

@Composable
fun ScannerCards(
    currentMode: ScanColorMode,
    onModeChange: (ScanColorMode) -> Unit,
    onStartScan: () -> Unit,
    onStart8PointCrop: () -> Unit,
    onImportImages: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mode Selector: Natural / Fast (No Over-saturation) vs Full Filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141518), RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Natural / Fast Pill
            val isNatural = currentMode == ScanColorMode.NATURAL_FAST
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isNatural) NothingWhite else Color.Transparent)
                    .clickable { onModeChange(ScanColorMode.NATURAL_FAST) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = if (isNatural) NothingBlack else NothingGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "NATURAL & FAST",
                        color = if (isNatural) NothingBlack else NothingGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Document Full Mode Pill
            val isFull = currentMode == ScanColorMode.DOCUMENT_FULL
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isFull) NothingWhite else Color.Transparent)
                    .clickable { onModeChange(ScanColorMode.DOCUMENT_FULL) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = if (isFull) NothingBlack else NothingGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "DOCUMENT FILTERS",
                        color = if (isFull) NothingBlack else NothingGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Main Scan Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable { onStartScan() },
            colors = CardDefaults.cardColors(containerColor = NothingSurface),
            border = BorderStroke(1.dp, NothingBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFF202227), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Scan Document",
                            tint = NothingWhite,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Badge indicating mode
                    Box(
                        modifier = Modifier
                            .background(
                                if (currentMode == ScanColorMode.NATURAL_FAST) Color(0xFF102615) else Color(0xFF261012),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (currentMode == ScanColorMode.NATURAL_FAST) Color(0xFF4CAF50) else NothingRed,
                                        CircleShape
                                    )
                            )
                            Text(
                                text = if (currentMode == ScanColorMode.NATURAL_FAST) "TRUE COLOR • INSTANT" else "AUTO-CROP • FILTERS",
                                color = if (currentMode == ScanColorMode.NATURAL_FAST) Color(0xFF81C784) else NothingRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "SCAN DOCUMENT",
                    color = NothingWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (currentMode == ScanColorMode.NATURAL_FAST) {
                        "Fast capture & auto-crop with 100% natural camera colors (no artificial saturation or delay)."
                    } else {
                        "Auto-crop with Google's full document enhancement filters and page tools."
                    },
                    color = NothingGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // 8-Point Curved Crop Studio Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable { onStart8PointCrop() },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181A20)),
            border = BorderStroke(1.dp, Color(0xFF382025))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF2E151A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CropFree,
                        contentDescription = "8-Point Crop",
                        tint = NothingRed,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "8-POINT CURVED CROP",
                            color = NothingWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Straighten curved book pages with 4 corners + 4 edge handles",
                        color = NothingGray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Secondary Card: Import from Gallery
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable { onImportImages() },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141518)),
            border = BorderStroke(1.dp, NothingBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF1F2025), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Import Photos",
                        tint = NothingWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "IMPORT FROM GALLERY",
                        color = NothingWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Convert gallery photos into clean PDF without oversaturating",
                        color = NothingGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
