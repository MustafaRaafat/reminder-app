package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminderData: MutableList<ReminderDTO>? = mutableListOf()) :
    ReminderDataSource {

    private var shouldReturnError = false

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }
//    Create a fake data source to act as a double to the real data source

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
//        "return error"
        if (shouldReturnError) {
            return Result.Error("error getting reminders")
        }
//        "Return the reminders"
        reminderData?.let {
            return Result.Success(ArrayList(it))
        }
        return Result.Error("error")
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
//        "save the reminder"
        reminderData?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        //        "return error"
        if (shouldReturnError) {
            return Result.Error("error get the reminder")
        }
//        "return the reminder with the id"
        reminderData?.firstOrNull {
            it.id == id
        }?.let {
            return Result.Success(it)
        }
        return Result.Error("error")
    }

    override suspend fun deleteAllReminders() {
//        "delete all the reminders"
        reminderData?.clear()
    }


}