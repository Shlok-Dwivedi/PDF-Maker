package com.nothing.pdfmaker.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nothing.pdfmaker.ui.theme.NothingBlack
import com.nothing.pdfmaker.ui.theme.NothingBorder
import com.nothing.pdfmaker.ui.theme.NothingDarkGray
import com.nothing.pdfmaker.ui.theme.NothingGray
import com.nothing.pdfmaker.ui.theme.NothingRed
import com.nothing.pdfmaker.ui.theme.NothingSurface
import com.nothing.pdfmaker.ui.theme.NothingWhite
import com.nothing.pdfmaker.util.DocumentFilter
import com.nothing.pdfmaker.util.PageItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageStudioModal(
    selectedUris: List<Uri>,
    onDismiss: () -> Unit,
    onGeneratePdf: (pages: List<PageItem>, fitToA4: Boolean, docTitle: String) -> Unit,
    isGenerating: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val pages = remember {
        mutableStateListOf<PageItem>().apply {
            addAll(selectedUris.map { PageItem(uri = it) })
        }
    }

    var selectedFilter by remember { mutableStateOf(DocumentFilter.ORIGINAL) }
    var fitToA4 by remember { mutableStateOf(true) }
    var docTitle by remember { mutableStateOf("Scanned_Document") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NothingBlack,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PDF CONVERTER STUDIO",
                        color = NothingWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${pages.size} images selected",
                        color = NothingGray,
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = NothingWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Document Title Input
            OutlinedTextField(
                value = docTitle,
                onValueChange = { docTitle = it },
                label = { Text("Document Name", color = NothingGray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NothingWhite,
                    unfocusedTextColor = NothingWhite,
                    focusedBorderColor = NothingWhite,
                    unfocusedBorderColor = NothingBorder,
                    cursorColor = NothingRed
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Horizontal Page Preview & Rotation
            Text(
                text = "PAGES & ORIENTATION",
                color = NothingGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(pages) { index, pageItem ->
                    Box(
                        modifier = Modifier
                            .size(110.dp, 150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NothingSurface)
                    ) {
                        AsyncImage(
                            model = pageItem.uri,
                            contentDescription = "Page ${index + 1}",
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(pageItem.rotationDegrees),
                            contentScale = ContentScale.Crop
                        )

                        // Page number badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                                .background(NothingBlack.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = NothingWhite,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Rotate button
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                                .size(28.dp)
                                .background(NothingBlack.copy(alpha = 0.8f), CircleShape)
                                .clickable {
                                    pageItem.rotationDegrees = (pageItem.rotationDegrees + 90f) % 360f
                                    pages[index] = pageItem.copy(rotationDegrees = pageItem.rotationDegrees)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RotateRight,
                                contentDescription = "Rotate",
                                tint = NothingWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Remove button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(28.dp)
                                .background(NothingBlack.copy(alpha = 0.8f), CircleShape)
                                .clickable {
                                    pages.removeAt(index)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove",
                                tint = NothingRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Document Enhancement Filters (Office Lens Style)
            Text(
                text = "DOCUMENT FILTERS",
                color = NothingGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    DocumentFilter.ORIGINAL to "Natural (Raw)",
                    DocumentFilter.NATURAL_CLEAR to "Clear Tone",
                    DocumentFilter.BLACK_AND_WHITE to "B&W Doc",
                    DocumentFilter.GRAYSCALE to "Grayscale"
                ).forEach { (filter, label) ->
                    val selected = selectedFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedFilter = filter
                            pages.forEach { it.filter = filter }
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = NothingSurface,
                            labelColor = NothingWhite,
                            selectedContainerColor = NothingWhite,
                            selectedLabelColor = NothingBlack
                        ),
                        border = BorderStroke(1.dp, if (selected) NothingWhite else NothingBorder),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Options: Fit to A4
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NothingSurface, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Standard A4 Page Sizing",
                        color = NothingWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (fitToA4) "Pages fit standard printable A4" else "Fit to exact image aspect ratio",
                        color = NothingGray,
                        fontSize = 12.sp
                    )
                }

                Switch(
                    checked = fitToA4,
                    onCheckedChange = { fitToA4 = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NothingWhite,
                        checkedTrackColor = NothingRed,
                        uncheckedThumbColor = NothingGray,
                        uncheckedTrackColor = NothingDarkGray
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Generate Button
            Button(
                onClick = {
                    onGeneratePdf(pages, fitToA4, docTitle)
                },
                enabled = pages.isNotEmpty() && !isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NothingWhite,
                    contentColor = NothingBlack,
                    disabledContainerColor = NothingDarkGray,
                    disabledContentColor = NothingGray
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = NothingBlack,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "GENERATING PDF...",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "CREATE PDF (${pages.size} PAGES)",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
