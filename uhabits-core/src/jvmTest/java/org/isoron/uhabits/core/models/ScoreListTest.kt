/*
 * Copyright (C) 2016-2021 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.core.models

import junit.framework.Assert.assertTrue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.number.IsCloseTo
import org.hamcrest.number.OrderingComparison
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.utils.DateUtils.Companion.getToday
import org.junit.Before
import org.junit.Test
import java.util.ArrayList

class ScoreListTest : BaseUnitTest() {
    private lateinit var habit: Habit
    private lateinit var today: Timestamp
    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        today = getToday()
        habit = fixtures.createEmptyHabit()
    }

    @Test
    fun test_getValue() {
        check(0, 20)
        val expectedValues = doubleArrayOf(
            0.655747,
            0.636894,
            0.617008,
            0.596033,
            0.573910,
            0.550574,
            0.525961,
            0.500000,
            0.472617,
            0.443734,
            0.413270,
            0.381137,
            0.347244,
            0.311495,
            0.273788,
            0.234017,
            0.192067,
            0.147820,
            0.101149,
            0.051922,
            0.000000,
            0.000000,
            0.000000
        )
        checkScoreValues(expectedValues)
    }

    @Test
    fun test_getValueWithSkip() {
        check(0, 20)
        addSkip(5)
        addSkip(10)
        addSkip(11)
        habit.recompute()
        val expectedValues = doubleArrayOf(
            0.596033,
            0.573910,
            0.550574,
            0.525961,
            0.500000,
            0.472617,
            0.472617,
            0.443734,
            0.413270,
            0.381137,
            0.347244,
            0.347244,
            0.347244,
            0.311495,
            0.273788,
            0.234017,
            0.192067,
            0.147820,
            0.101149,
            0.051922,
            0.000000,
            0.000000,
            0.000000
        )
        checkScoreValues(expectedValues)
    }

    @Test
    fun test_getValueWithSkip2() {
        check(5)
        addSkip(4)
        habit.recompute()
        val expectedValues = doubleArrayOf(
            0.041949,
            0.044247,
            0.046670,
            0.049226,
            0.051922,
            0.051922,
            0.0
        )
        checkScoreValues(expectedValues)
    }

    @Test
    fun test_withZeroTarget() {
        habit = fixtures.createNumericalHabit()
        habit.targetValue = 0.0
        habit.recompute()
        assertTrue(habit.scores[today].value.isFinite())
    }

    @Test
    fun test_imperfectNonDaily() {
        // If the habit should be performed 3 times per week and the user misses 1 repetition
        // each week, score should converge to 66%.
        habit.frequency = Frequency(3, 7)
        val values = ArrayList<Int>()
        for (k in 0..99) {
            values.add(Entry.YES_MANUAL)
            values.add(Entry.YES_MANUAL)
            values.add(Entry.NO)
            values.add(Entry.NO)
            values.add(Entry.NO)
            values.add(Entry.NO)
            values.add(Entry.NO)
        }
        check(values)
        assertThat(habit.scores[today].value, IsCloseTo.closeTo(2 / 3.0, E))

        // Missing 2 repetitions out of 4 per week, the score should converge to 50%
        habit.frequency = Frequency(4, 7)
        habit.recompute()
        assertThat(habit.scores[today].value, IsCloseTo.closeTo(0.5, E))
    }

    @Test
    fun test_irregularNonDaily() {
        // If the user performs habit perfectly each week, but on different weekdays,
        // score should still converge to 100%
        habit.frequency = Frequency(1, 7)
        val values = ArrayList<Int>()
        for (k in 0..99) {
            // Week 0
            values.add(Entry.YES_MANUAL)
            values.add(Entry.NO)
            values.add(Entry.NO)
            values.add(Entry.NO)
            values.add(Entry.NO)
            values.add(Entry.NO)
            values.add(Entry.NO)

            // Week 1
            values.add(Entry.NO)
            values.add(Entry.NO)
            values.add(Entry.NO)
            values.add(Entry.NO)
            values.add(Entry.NO)
            values.add(Entry.NO)
            values.add(Entry.YES_MANUAL)
        }
        check(values)
        assertThat(habit.scores[today].value, IsCloseTo.closeTo(1.0, 1e-3))
    }

    @Test
    fun shouldAchieveHighScoreInReasonableTime() {
        // Daily habits should achieve at least 99% in 3 months
        habit = fixtures.createEmptyHabit()
        habit.frequency = Frequency.DAILY
        for (i in 0..89) check(i)
        habit.recompute()
        assertThat(habit.scores[today].value, OrderingComparison.greaterThan(0.99))

        // Weekly habits should achieve at least 99% in 9 months
        habit = fixtures.createEmptyHabit()
        habit.frequency = Frequency.WEEKLY
        for (i in 0..38) check(7 * i)
        habit.recompute()
        assertThat(habit.scores[today].value, OrderingComparison.greaterThan(0.99))

        // Monthly habits should achieve at least 99% in 18 months
        habit.frequency = Frequency(1, 30)
        for (i in 0..17) check(30 * i)
        habit.recompute()
        assertThat(habit.scores[today].value, OrderingComparison.greaterThan(0.99))
    }

    @Test
    fun test_recompute() {
        assertThat(habit.scores[today].value, IsCloseTo.closeTo(0.0, E))
        check(0, 2)
        assertThat(habit.scores[today].value, IsCloseTo.closeTo(0.101149, E))
        habit.frequency = Frequency(1, 2)
        habit.recompute()
        assertThat(habit.scores[today].value, IsCloseTo.closeTo(0.054816, E))
    }

    @Test
    fun test_addThenRemove() {
        val habit = fixtures.createEmptyHabit()
        habit.recompute()
        assertThat(habit.scores[today].value, IsCloseTo.closeTo(0.0, E))
        habit.originalEntries.add(Entry(today, Entry.YES_MANUAL))
        habit.recompute()
        assertThat(habit.scores[today].value, IsCloseTo.closeTo(0.051922, E))
        habit.originalEntries.add(Entry(today, Entry.UNKNOWN))
        habit.recompute()
        assertThat(habit.scores[today].value, IsCloseTo.closeTo(0.0, E))
    }

    private fun check(offset: Int) {
        val entries = habit.originalEntries
        entries.add(Entry(today.minus(offset), Entry.YES_MANUAL))
    }

    private fun check(from: Int, to: Int) {
        val entries = habit.originalEntries
        for (i in from until to) entries.add(Entry(today.minus(i), Entry.YES_MANUAL))
        habit.recompute()
    }

    private fun check(values: ArrayList<Int>) {
        val entries = habit.originalEntries
        for (i in values.indices) if (values[i] == Entry.YES_MANUAL) entries.add(
            Entry(
                today.minus(i),
                Entry.YES_MANUAL
            )
        )
        habit.recompute()
    }

    private fun addSkip(day: Int) {
        val entries = habit.originalEntries
        entries.add(Entry(today.minus(day), Entry.SKIP))
    }

    private fun checkScoreValues(expectedValues: DoubleArray) {
        var current = today
        val scores = habit.scores
        for (expectedValue in expectedValues) {
            assertThat(scores[current].value, IsCloseTo.closeTo(expectedValue, E))
            current = current.minus(1)
        }
    }

    companion object {
        private const val E = 1e-6
    }
}
