package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
//import com.udacity.project4.locationreminders.data.dto.ReminderDTO
//import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {
    private lateinit var repo: RemindersLocalRepository
    private lateinit var dao: RemindersDao
    private lateinit var database: RemindersDatabase

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
        dao = database.reminderDao()
        repo = RemindersLocalRepository(dao, Dispatchers.Main)
    }

    @After
    fun close() {
        database.close()
    }

//    Add testing implementation to the RemindersLocalRepository.kt
    @Test
    fun saveTo_getFrom_localRepo() {
        var reminder = ReminderDTO("milk", "get Milk", "store", 1.0, 1.0, "id")
        runBlocking {
            repo.saveReminder(reminder)

            assertThat((repo.getReminder("id")as Result.Success).data.title,`is`("milk"))
        }
    }
}