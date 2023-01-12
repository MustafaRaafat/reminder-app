package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : AutoCloseKoinTest() {
    private lateinit var appContext: Application
    private lateinit var reminderTest: ReminderDataSource

    @Before
    fun setupTest() {
        stopKoin()
        appContext = ApplicationProvider.getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(appContext, get() as ReminderDataSource)
            }
            single {
                SaveReminderViewModel(get(), get() as ReminderDataSource)
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        startKoin {
            androidContext(appContext)
            modules(listOf(myModule))
        }
        reminderTest = get()
        runBlocking { reminderTest.deleteAllReminders() }
    }

//    test the displayed data on the UI.
    @Test
    fun showReminder_oneReminder_visibable() {
        var f = ReminderDTO("hello", "new reminder", "home", 1.0, 1.0, "id")
        runBlocking {
            reminderTest.saveReminder(f)
            launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

            onView(withText(f.title)).check(
                matches(isDisplayed())
            )
        }
    }

    //    test the navigation of the fragments.
    @Test
    fun navigateTo_SaveReminderScreen() {
        runBlocking {
            val s = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
            val con = mock(NavController::class.java)
            s.onFragment {
                Navigation.setViewNavController(it.view!!, con)
            }
            onView(withId(R.id.addReminderFAB)).perform(click())

            verify(con).navigate(ReminderListFragmentDirections.toSaveReminder())
        }
    }

//    add testing for the error messages.
    @Test
    fun reminder_noData() {
            launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
            onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }
}