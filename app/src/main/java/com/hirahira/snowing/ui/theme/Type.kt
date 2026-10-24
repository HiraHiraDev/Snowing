package com.hirahira.snowing.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hirahira.snowing.R

// Fonts are bundled, cut down by docs/fonts/make-fonts.sh (ADR-0010): Literata pinned to
// weight 500 with its optical size axis kept, Hanken Grotesk 500-700 for the UI (owner's
// choice over the design's Manrope).

/**
 * Literata cut for one text size, as a browser does with `font-optical-sizing: auto`:
 * large names get the display drawing, small rows the text one.
 */
private fun literata(size: TextUnit, italic: Boolean = false) = FontFamily(
    Font(
        resId = if (italic) R.font.literata_italic else R.font.literata,
        weight = FontWeight.Medium,
        style = if (italic) FontStyle.Italic else FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.opticalSizing(size)),
    ),
)

private fun hanken(weight: FontWeight) = Font(
    resId = R.font.hanken,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private val HankenGrotesk = FontFamily(
    hanken(FontWeight.Medium),
    hanken(FontWeight.SemiBold),
    hanken(FontWeight.Bold),
)

// Leading split evenly above and below, like CSS line-height.
private val Base = TextStyle(
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

private fun serif(size: TextUnit, lineHeight: TextUnit = TextUnit.Unspecified, tracking: TextUnit = 0.em, italic: Boolean = false) =
    Base.copy(
        fontFamily = literata(size, italic),
        fontWeight = FontWeight.Medium,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        fontSize = size,
        lineHeight = lineHeight,
        letterSpacing = tracking,
    )

private fun sans(size: TextUnit, weight: FontWeight, tracking: TextUnit = 0.em) = Base.copy(
    fontFamily = HankenGrotesk,
    fontWeight = weight,
    fontSize = size,
    letterSpacing = tracking,
)

private fun underlined(style: TextStyle) = style.copy(textDecoration = TextDecoration.Underline)

internal val DefaultTypography = SnowingTypography(
    // Balanced lines like the design's text-wrap: balance, so a two-word name splits evenly.
    moodHero = serif(76.sp, lineHeight = 0.98.em, tracking = (-0.035).em).copy(lineBreak = LineBreak.Heading),
    statsHero = serif(80.sp, lineHeight = 1.25.em, tracking = (-0.04).em),
    ctaLabel = serif(30.sp, lineHeight = 1.05.em, tracking = (-0.02).em),
    pageTitle = serif(32.sp, lineHeight = 1.08.em, tracking = (-0.02).em).copy(lineBreak = LineBreak.Heading),
    stepLabel = sans(11.5.sp, FontWeight.Bold, tracking = 0.1.em),
    paragraph = sans(14.5.sp, FontWeight.Medium).copy(lineHeight = 1.5.em),
    stepText = sans(14.sp, FontWeight.SemiBold),
    stepNumber = sans(12.sp, FontWeight.Bold),
    itemTitle = sans(15.sp, FontWeight.Bold),
    itemBody = sans(13.5.sp, FontWeight.Medium).copy(lineHeight = 1.45.em),
    itemNote = sans(13.sp, FontWeight.SemiBold).copy(lineHeight = 1.45.em),
    inlineAction = sans(14.sp, FontWeight.Bold),
    quietAction = sans(15.sp, FontWeight.Medium),
    footnote = sans(12.5.sp, FontWeight.Medium).copy(lineHeight = 1.5.em),
    button = sans(16.sp, FontWeight.Bold),
    listItem = serif(26.sp, lineHeight = 1.15.em, tracking = (-0.02).em),
    sectionHeader = serif(22.sp, italic = true),
    valueLarge = serif(22.sp),
    milestone = serif(20.sp),
    valueMedium = serif(19.sp),
    rowTitle = serif(17.sp),
    eyebrow = sans(11.sp, FontWeight.Bold, tracking = 0.14.em),
    body = sans(14.sp, FontWeight.Medium),
    label = sans(13.sp, FontWeight.SemiBold),
    hint = sans(13.sp, FontWeight.Medium),
    note = sans(11.sp, FontWeight.Medium),
    axis = sans(11.sp, FontWeight.SemiBold),
    action = underlined(sans(13.sp, FontWeight.Bold)),
    meta = sans(12.sp, FontWeight.Medium),
    metaStrong = sans(12.sp, FontWeight.SemiBold),
    micro = sans(10.sp, FontWeight.Bold, tracking = 0.1.em),
)
