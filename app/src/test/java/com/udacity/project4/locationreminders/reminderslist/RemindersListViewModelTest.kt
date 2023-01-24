package com.udacity.project4.locationreminders.reminderslist

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {
    private lateinit var viewModel: RemindersListViewModel
    private var data = FakeDataSource()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setUp() {
        viewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), data)
    }

    //    provide testing to the RemindersListViewModel and its live data objects
    @Test
    fun loadReminders_withOnlyOne() {
        mainCoroutineRule.runBlockingTest {
            data.deleteAllReminders()
            var r = ReminderDTO("hello", "mustafa", "at home", 1.0, 1.0, "id")
            data.saveReminder(r)
            viewModel.loadReminders()
            assertEquals(viewModel.remindersList.value!!.size, 1)
        }
    }

    @Test
    fun loadReminders_snackBarErrorMessage() {
        mainCoroutineRule.runBlockingTest {
            data.deleteAllReminders()
            viewModel.loadReminders()
            assertEquals(viewModel.showNoData.value, true)
        }
    }
}