package com.hirahira.snowing.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class MotionMathTest {

    @Test
    fun `turns take the short way round`() {
        assertEquals(20, shortestTurn(350, 10))
        assertEquals(-20, shortestTurn(10, 350))
        assertEquals(90, shortestTurn(0, 90))
        assertEquals(180, shortestTurn(0, 180))
        assertEquals(0, shortestTurn(45, 45))
    }

    @Test
    fun `the needle keeps turning forward across north`() {
        val unwrapper = AngleUnwrapper(340)
        assertEquals(340f, unwrapper.follow(340), 0f)
        assertEquals(370f, unwrapper.follow(10), 0f)
        assertEquals(400f, unwrapper.follow(40), 0f)
        assertEquals(355f, unwrapper.follow(355), 0f)
    }

    @Test
    fun `following the same angle twice changes nothing`() {
        val unwrapper = AngleUnwrapper(0)
        unwrapper.follow(300)
        assertEquals(-60f, unwrapper.follow(300), 0f)
    }

    @Test
    fun `the tab dot sits between tabs and runs past the ends`() {
        val lefts = floatArrayOf(10f, 100f, 250f)
        assertEquals(10f, positionBetween(0f, lefts), 1e-4f)
        assertEquals(55f, positionBetween(0.5f, lefts), 1e-4f)
        assertEquals(175f, positionBetween(1.5f, lefts), 1e-4f)
        assertEquals(250f, positionBetween(2f, lefts), 1e-4f)
        assertEquals(1f, positionBetween(-0.1f, lefts), 1e-4f)
        assertEquals(265f, positionBetween(2.1f, lefts), 1e-4f)
    }
}
