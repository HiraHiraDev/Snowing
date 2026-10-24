package com.hirahira.snowing.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design tokens. Screens and components read these through [SnowingTheme]
 * and never use literal colors, sizes or text styles, so a new design
 * (or several themes) is a change of values, not of screens.
 */
@Immutable
data class SnowingColors(
    /** The background scene; it follows the time of day. */
    val sky: SkyPalette,
    /** Ink picked for contrast with the sky: dark on a light day sky, light otherwise. */
    val ink: Color,
    val inkSoft: Color,
    /** The quietest text, e.g. a footnote. */
    val inkFaint: Color,
    /** 1 dp lines that group content instead of cards. */
    val line: Color,
    /** Thin rings around icons. */
    val outline: Color,
    /** A faint fill that sets a small area apart, e.g. a preview of a system setting. */
    val well: Color,
    /** Unlit ticks of indicators and rulers. */
    val tickDim: Color,
    val accentText: Color,
    val accentFill: Color,
    /** Soft ring around a picked dot. */
    val accentHalo: Color,
    val ctaFill: Color,
    val ctaDot: Color,
    /** Light around the idle start button: amber on a dark sky, a soft shadow on a light one. */
    val ctaGlow: Color,
    /** Behind long content over the scene, at its strongest; it fades in from transparent above. */
    val scrim: Color,
    /**
     * A panel over the scene (onboarding, ADR-0011, design 6.2): translucent over a blurred
     * copy of the scene; [sheetSolid] where Android cannot blur (API < 31).
     */
    val sheet: Color,
    val sheetSolid: Color,
    /** Over the app while a panel is open. */
    val sheetScrim: Color,
    /** The panel's lit top edge. */
    val sheetHighlight: Color,
    /** The bar to drag a panel by. */
    val sheetHandle: Color,
    /** Text on the light snowdrift, whatever the sky. */
    val onDrift: Color,
    val onDriftSoft: Color,
    /** The distant forest behind the hills. */
    val haze: Color,
    /** Cabin walls, mullions and the dark pines beside it. */
    val cabin: Color,
    val gable: Color,
    /** Lines between the cabin's logs. */
    val logs: Color,
    /** Cabin windows unlit; [windowLight] shines over them as much as the sky allows. */
    val windowBase: Color,
    val windowLight: Color,
    /** Cat paw prints in the drift. */
    val paw: Color,
    val sunDisc: Color,
    val sunGlow: Color,
    val moonDisc: Color,
    val moonGlow: Color,
    val milestone: Color,
    val isLightSky: Boolean,
) {
    companion object {
        /** Above this luma of the sky's top the ink turns dark (design 6a). */
        private const val LIGHT_SKY_LUMA = 0.42f

        // Rec. 709 weights on the gamma-encoded values, as in the design's HTML. The handoff's
        // Palette.kt uses Color.luminance(), which is linear and keeps the day sky "dark".
        private fun Color.luma(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

        /**
         * The onboarding panel is dark whatever the sky (design 6.2): night ink and amber, with
         * fainter lines and a softer glow around the button.
         */
        fun forSheet(): SnowingColors = forSky(Sky.Night).copy(
            line = Fixed.InkLight.copy(alpha = 0.14f),
            ctaGlow = Fixed.Amber.copy(alpha = 0.35f),
        )

        fun forSky(sky: SkyPalette): SnowingColors {
            val light = sky.skyTop.luma() > LIGHT_SKY_LUMA
            val ink = if (light) Fixed.InkDark else Fixed.InkLight
            return SnowingColors(
                sky = sky,
                ink = ink,
                inkSoft = ink.copy(alpha = if (light) 0.72f else 0.78f),
                inkFaint = ink.copy(alpha = 0.66f),
                line = ink.copy(alpha = if (light) 0.20f else 0.22f),
                outline = ink.copy(alpha = 0.28f),
                well = ink.copy(alpha = 0.10f),
                tickDim = ink.copy(alpha = if (light) 0.35f else 0.40f),
                accentText = if (light) Fixed.AmberDeepText else Fixed.AmberText,
                accentFill = if (light) Fixed.AmberDeep else Fixed.Amber,
                accentHalo = Fixed.Amber.copy(alpha = 0.22f),
                ctaFill = if (light) Fixed.InkDark else Fixed.Amber,
                ctaDot = if (light) Fixed.Amber else Fixed.InkOnAmber,
                ctaGlow = if (light) Fixed.InkDark.copy(alpha = 0.3f) else Fixed.Amber.copy(alpha = 0.55f),
                scrim = if (light) Fixed.ScrimLight.copy(alpha = 0.94f) else Fixed.ScrimDark.copy(alpha = 0.90f),
                sheet = Fixed.Sheet.copy(alpha = 0.72f),
                sheetSolid = Fixed.Sheet.copy(alpha = 0.94f),
                sheetScrim = Fixed.SheetScrim.copy(alpha = 0.38f),
                sheetHighlight = Color.White.copy(alpha = 0.10f),
                sheetHandle = ink.copy(alpha = 0.32f),
                onDrift = Fixed.InkOnAmber,
                onDriftSoft = Fixed.InkOnDriftSoft,
                haze = mix(sky.skyLow, sky.hillFar, 0.55f),
                cabin = mix(sky.hillNear, Fixed.SceneInk, 0.45f),
                gable = mix(sky.hillNear, Fixed.SceneInk, 0.60f),
                logs = mix(sky.hillNear, sky.driftBottom, 0.25f).copy(alpha = 0.35f),
                windowBase = mix(sky.hillNear, Fixed.SceneInk, 0.70f),
                paw = mix(sky.driftBottom, sky.hillNear, 0.45f).copy(alpha = 0.6f),
                windowLight = Fixed.Amber,
                sunDisc = Fixed.SunDisc,
                sunGlow = Fixed.SunGlow,
                moonDisc = Fixed.MoonDisc,
                moonGlow = Fixed.MoonGlow,
                milestone = Fixed.Milestone,
                isLightSky = light,
            )
        }
    }
}

/** Design 6a spacing: no cards, groups are set apart by space and 1 dp lines. */
@Immutable
data class SnowingSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    /** Vertical padding of rows separated by lines. */
    val row: Dp = 11.dp,
    /** Between things side by side in a row, e.g. an indicator and its description. */
    val inline: Dp = 12.dp,
    /** Inside a group, tight. */
    val m: Dp = 14.dp,
    val l: Dp = 18.dp,
    /** Inside a group, loose. */
    val xl: Dp = 22.dp,
    /** Between groups. */
    val group: Dp = 36.dp,
    /** Horizontal padding of every screen. */
    val screen: Dp = 28.dp,
    val screenBottom: Dp = 40.dp,
    // Below the status bar: the page tabs, then where each page's content starts.
    val tabsTop: Dp = 14.dp,
    /** Where content scrolling up under the tabs starts to fade, just below the tab names. */
    val underTabsFadeStart: Dp = 32.dp,
    val pageTopStats: Dp = 76.dp,
    val pageTopSnow: Dp = 96.dp,
    val pageTopMoods: Dp = 56.dp,
    // A panel over the scene (design 6.2).
    val sheetSide: Dp = 26.dp,
    val sheetBottom: Dp = 34.dp,
    val sheetHandleTop: Dp = 12.dp,
    val sheetHandleGap: Dp = 22.dp,
    /** Between a heading's small label and its title. */
    val headingGap: Dp = 10.dp,
    /** Between paragraphs of running text. */
    val paragraph: Dp = 10.dp,
    /** Above a divider that ends a block, and below it. */
    val dividerAbove: Dp = 20.dp,
    val dividerBelow: Dp = 18.dp,
    /** Between numbered steps. */
    val stepGap: Dp = 12.dp,
    /** Vertical padding of rows divided by lines in a panel. */
    val itemRow: Dp = 16.dp,
    /** Above the panel's main button. */
    val sheetAction: Dp = 26.dp,
    /** A preview indented under a numbered step, and its inner padding. */
    val previewInset: Dp = 36.dp,
    val previewPaddingV: Dp = 9.dp,
    val previewPaddingH: Dp = 12.dp,
)

@Immutable
data class SnowingSizes(
    val touchTarget: Dp = 48.dp,
    val line: Dp = 1.dp,
    /** The read-only intensity indicator: thin ticks, lit ones taller. */
    val levelMarkWidth: Dp = 2.dp,
    val levelMarkHeight: Dp = 14.dp,
    val levelMarkDimHeight: Dp = 7.dp,
    val levelMarkGap: Dp = 4.dp,
    /** The round start / stop button and what is drawn inside it. */
    val ctaButton: Dp = 88.dp,
    val ctaDot: Dp = 14.dp,
    val ctaStop: Dp = 18.dp,
    val ctaOutline: Dp = 2.dp,
    val ctaGlow: Dp = 48.dp,
    /** Between the start / stop button and its label. */
    val ctaGap: Dp = 20.dp,
    /** Intensity ruler: a tall lit mark per level reached, a short dim one above. */
    val rulerLitWidth: Dp = 3.dp,
    val rulerLitHeight: Dp = 28.dp,
    val rulerDimWidth: Dp = 2.dp,
    val rulerDimHeight: Dp = 12.dp,
    val rulerBottomPadding: Dp = 8.dp,
    /** How far the ruler's cells reach past the screen margin, so the end marks line up with text. */
    val rulerOverhang: Dp = 6.dp,
    /** Wind dial: a ring with four ticks, a needle with a round head, a hub in the middle. */
    val dial: Dp = 88.dp,
    val dialRing: Dp = 1.5.dp,
    val dialTick: Dp = 7.dp,
    val dialTickInset: Dp = 5.dp,
    val needleWidth: Dp = 2.dp,
    val needleLength: Dp = 33.dp,
    val needleHead: Dp = 10.dp,
    val dialHub: Dp = 8.dp,
    val directionArrow: Dp = 18.dp,
    /** Under the picked option of a row of options. */
    val optionUnderline: Dp = 2.dp,
    /** Flake size dots, small to large, and the ring around the picked one. */
    val dotSmall: Dp = 8.dp,
    val dotMedium: Dp = 13.dp,
    val dotLarge: Dp = 20.dp,
    val dotHalo: Dp = 5.dp,
    /** Bar chart: thin rounded bars in a fixed-height row; an empty day keeps a stub. */
    val chartHeight: Dp = 64.dp,
    val barWidth: Dp = 3.dp,
    val barStub: Dp = 3.dp,
    val chartLabelGap: Dp = 10.dp,
    /** Width of each bar's column, so a bar and its label share one centre. */
    val labelColumn: Dp = 14.dp,
    /** The scrim behind the Moods page, measured up from the bottom; it fades in over all of it. */
    val moodsScrim: Dp = 640.dp,
    /** Marks the active page tab. */
    val tabDot: Dp = 5.dp,
    /** Content scrolling up under the tabs fades out over this distance. */
    val underTabsFade: Dp = 28.dp,
    /** The filled main button and the soft glow around it. */
    val button: Dp = 56.dp,
    val buttonGlow: Dp = 36.dp,
    /** The panel's drag handle. */
    val handleWidth: Dp = 36.dp,
    val handleHeight: Dp = 4.dp,
    /** Blur of the scene seen through a panel (API 31+). */
    val sheetBlur: Dp = 22.dp,
    /** How far down the panel's lit top edge fades out, about its corner radius. */
    val sheetEdgeFade: Dp = 28.dp,
    /** Rings: around a step's number, around a row's icon, around the footnote's "i". */
    val stepRing: Dp = 24.dp,
    val iconRing: Dp = 40.dp,
    val infoRing: Dp = 16.dp,
    val ringStroke: Dp = 1.5.dp,
    /** Line icons drawn in code: a row's icon, and the small check of a done state. */
    val glyph: Dp = 20.dp,
    val checkGlyph: Dp = 14.dp,
    /** The app's icon in a preview of a system setting. */
    val appIcon: Dp = 22.dp,
)

/**
 * No rounded containers in design 6a: only circles, pills and the caps of ticks. The
 * onboarding panel is ours, outside the design (ADR-0011).
 */
@Immutable
data class SnowingShapes(
    val circle: Shape = CircleShape,
    val pill: Shape = RoundedCornerShape(percent = 50),
    val tick: Shape = RoundedCornerShape(2.dp),
    val sheet: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    /** A small filled area, e.g. a preview of a system setting. */
    val well: Shape = RoundedCornerShape(12.dp),
    val appIcon: Shape = RoundedCornerShape(6.dp),
)

/**
 * Text roles of design 6a. Literata for names and numbers, Hanken Grotesk for UI.
 * [eyebrow] and [micro] are meant to be shown in upper case.
 */
@Immutable
data class SnowingTypography(
    /** The mood's name on the Snow screen. */
    val moodHero: TextStyle,
    /** The big number on the Stats screen. */
    val statsHero: TextStyle,
    val ctaLabel: TextStyle,
    /** A sentence-long title of a screen without a hero, e.g. an onboarding step. */
    val pageTitle: TextStyle,
    /** Small capitals in the accent above a title, e.g. "Step 1 of 2". */
    val stepLabel: TextStyle,
    /** Running text of a panel. */
    val paragraph: TextStyle,
    /** A numbered step's text and its number. */
    val stepText: TextStyle,
    val stepNumber: TextStyle,
    /** A row of a list in a panel: its title, its explanation, and a short note in full ink. */
    val itemTitle: TextStyle,
    val itemBody: TextStyle,
    val itemNote: TextStyle,
    /** An action shown as accent text under a row. */
    val inlineAction: TextStyle,
    /** A quiet secondary action, e.g. "Not now". */
    val quietAction: TextStyle,
    val footnote: TextStyle,
    /** The label of a filled button. */
    val button: TextStyle,
    val listItem: TextStyle,
    val sectionHeader: TextStyle,
    /** Wind value. */
    val valueLarge: TextStyle,
    val milestone: TextStyle,
    /** Schedule value. */
    val valueMedium: TextStyle,
    val rowTitle: TextStyle,
    val eyebrow: TextStyle,
    val body: TextStyle,
    /** Names of controls, options and tabs. */
    val label: TextStyle,
    /** A quiet tip on how to use a screen. */
    val hint: TextStyle,
    /** The smallest explanation, e.g. "drag the needle". */
    val note: TextStyle,
    /** Labels along a chart, e.g. the days under the week's bars. */
    val axis: TextStyle,
    /** Actions shown as text, e.g. "Restore". */
    val action: TextStyle,
    val meta: TextStyle,
    val metaStrong: TextStyle,
    val micro: TextStyle,
)
