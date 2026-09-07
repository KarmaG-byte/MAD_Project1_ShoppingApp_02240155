package com.karmagbyte.tshongla

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * Keeps the opening-screen brand mark consistent with the launcher icon.
 *
 * MainActivity's original logo placeholder is the single text glyph "T" with
 * color, fontSize and fontWeight supplied. This package-level overload is more
 * specific than the Material3 star import for that call and renders the real
 * TshongLa launcher emblem instead. Other four-argument Text calls are delegated
 * unchanged to Material3 Text.
 */
@Composable
fun Text(
    text: String,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight
) {
    if (text == "T") {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher),
            contentDescription = "TshongLa logo",
            modifier = Modifier.size(62.dp)
        )
    } else {
        androidx.compose.material3.Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight
        )
    }
}
