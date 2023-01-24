package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {
    private lateinit var viewmodel: SaveReminderViewModel
    private var data = FakeDataSource()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setUp() {
        viewmodel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), data)
    }

//    provide testing to the SaveReminderView and its live data objects
    @Test
    fun isValidetData() {
        mainCoroutineRule.runBlockingTest {
            val reminder = ReminderDataItem("title", "des", "loc", 1.0, 2.0, "fd")
            val k = viewmodel.validateEnteredData(reminder)
            Assert.assertEquals(true, k)
        }
    }

    @Test
    fun showToast_oneReminder(){
        val reminder = ReminderDataItem("title", "des", "loc", 1.0, 2.0, "fd")
        viewmodel.validateAndSaveReminder(reminder)
        assertEquals(viewmodel.showToast.value,"Reminder Saved !")
    }
}