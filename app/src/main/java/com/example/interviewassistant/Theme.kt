package com.example.interviewassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Colours sampled from the Figma screenshots. */
object C {
    val Bg = Color(0xFF07091A)
    val Card = Color(0xFF11152D)
    val CardSelected = Color(0xFF171739)
    val Border = Color(0xFF181D3A)
    val Field = Color(0xFF0E1228)
    val FieldBorder = Color(0xFF1E2545)
    val Primary = Color(0xFF685FFC)
    val PrimaryDeep = Color(0xFF5249E8)
    val PrimaryBorder = Color(0xFF32307B)
    val AccentText = Color(0xFF6C63FF)
    val Text = Color(0xFFE8ECFF)
    val Body = Color(0xFFC8D5E0)
    val Muted = Color(0xFF8890B5)
    val Hint = Color(0xFF7B7F93)
    val Green = Color(0xFF00E5B0)
    val GreenBg = Color(0xFF071623)
    val GreenBorder = Color(0xFF063236)
    val Amber = Color(0xFFF59E0B)
    val AmberBg = Color(0xFF161219)
    val AmberBorder = Color(0xFF241F27)
    val Red = Color(0xFFEF4444)
    val Disabled = Color(0xFF1E2545)
    val RingTrack = Color(0xFF1E2545)
}

val PrimaryBrush: Brush = Brush.horizontalGradient(listOf(C.Primary, C.PrimaryDeep))

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.Primary,
            background = C.Bg,
            surface = C.Card,
            onSurface = C.Text,
            onBackground = C.Text,
            error = C.Red
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = C.Bg,
            contentColor = C.Text,
            content = content
        )
    }
}

/** Dark background with the two soft glows seen in the design. */
@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(C.Bg)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x33202A6B), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.05f),
                        radius = size.width * 0.9f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x2208343A), Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.95f),
                        radius = size.width * 0.9f
                    )
                )
            },
        content = content
    )
}

/** Text with the sizes/weights used across the design. */
@Composable
fun Txt(
    text: String,
    size: Int = 14,
    weight: FontWeight = FontWeight.Normal,
    color: Color = C.Text,
    modifier: Modifier = Modifier,
    align: TextAlign? = null,
    mono: Boolean = false,
    spacing: Float = 0f,
    lineHeight: Int? = null
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size.sp,
        fontWeight = weight,
        textAlign = align,
        fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
        letterSpacing = spacing.sp,
        lineHeight = (lineHeight ?: (size * 1.4).toInt()).sp
    )
}

/** Small uppercase monospace label, e.g. EMAIL, LIVE INTERVIEW. */
@Composable
fun MonoLabel(text: String, color: Color = C.Muted, modifier: Modifier = Modifier) {
    Txt(text, 11, FontWeight.Medium, color, modifier, mono = true, spacing = 1.5f)
}

fun Modifier.card(
    radius: Dp = 18.dp,
    bg: Color = C.Card,
    border: Color = C.Border
): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .background(bg, shape)
        .border(1.dp, border, shape)
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(16.dp)
    val base = modifier.fillMaxWidth().height(54.dp)
    val styled = if (enabled) {
        base
            .shadow(18.dp, shape, ambientColor = C.Primary, spotColor = C.Primary)
            .clip(shape)
            .background(PrimaryBrush, shape)
    } else {
        base.clip(shape).background(C.Disabled, shape)
    }
    Box(
        modifier = styled.clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Txt(text, 15, FontWeight.Bold, if (enabled) Color.White else C.Hint)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .card(radius = 16.dp, bg = C.Field, border = C.Border)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Txt(text, 15, FontWeight.Bold, C.Muted)
    }
}

/** Compact button used in the live-interview bottom bar. */
@Composable
fun SmallButton(text: String, primary: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    val styled = if (primary) {
        modifier.clip(shape).background(PrimaryBrush, shape)
    } else {
        modifier.card(radius = 14.dp, bg = C.Field, border = C.Border)
    }
    Box(
        modifier = styled.clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Txt(text, 14, FontWeight.Bold, if (primary) Color.White else C.Muted)
    }
}

@Composable
fun BackRow(onBack: () -> Unit) {
    Row(
        modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Txt("‹", 26, color = C.Muted)
        Spacer(Modifier.width(10.dp))
        Txt("Back", 15, color = C.Muted)
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(16.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        textStyle = TextStyle(color = C.Text, fontSize = 15.sp),
        cursorBrush = SolidColor(C.Primary),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .clip(shape)
                    .background(C.Field, shape)
                    .border(1.dp, C.FieldBorder, shape)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.width(12.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Txt(placeholder, 15, color = C.Hint)
                    inner()
                }
                if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    trailing()
                }
            }
        }
    )
}

@Composable
fun BrandName(size: Int) {
    Row {
        Txt("Interview", size, FontWeight.Bold)
        Txt("AI", size, FontWeight.Bold, C.AccentText)
    }
}

@Composable
fun Logo() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(PrimaryBrush),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Person, null, modifier = Modifier.size(22.dp), tint = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        BrandName(20)
    }
}

/** Purple circle with a soft glow (welcome screen and the live-interview avatar). */
@Composable
fun AvatarCircle(size: Dp) {
    Box(Modifier.size(size * 1.6f), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size * 1.6f)
                .background(Brush.radialGradient(listOf(Color(0x336C5CFF), Color.Transparent)), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(size)
                .shadow(24.dp, CircleShape, ambientColor = C.Primary, spotColor = C.Primary)
                .clip(CircleShape)
                .background(PrimaryBrush, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Person, null, modifier = Modifier.size(size * 0.5f), tint = Color.White)
        }
    }
}

@Composable
fun Pill(text: String, color: Color = C.Green, bg: Color = C.GreenBg, border: Color = C.GreenBorder) {
    Box(
        modifier = Modifier.card(radius = 20.dp, bg = bg, border = border).padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Txt(text, 12, FontWeight.Medium, color)
    }
}

@Composable
fun SectionTitle(text: String, barColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height(14.dp).clip(RoundedCornerShape(2.dp)).background(barColor))
        Spacer(Modifier.width(8.dp))
        Txt(text, 14, FontWeight.Bold)
    }
}

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(C.Border)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(4.dp)
                .background(Brush.horizontalGradient(listOf(C.Primary, C.Green)))
        )
    }
}
