package com.tugraz.chronos

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import com.tugraz.chronos.model.database.ChronosDB
import com.tugraz.chronos.model.entities.Task
import com.tugraz.chronos.model.entities.TaskGroup
import com.tugraz.chronos.model.service.ChronosService
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private var db: ChronosDB = ChronosDB.getTestDB(ApplicationProvider.getApplicationContext())!!
    private var fdb = Firebase.database

    private lateinit var group: TaskGroup
    private lateinit var task: Task

    @Before
    fun setup() {
        group = TaskGroup("This is a Test Group")
        task = Task(0, "This is a Test Task", "Description", "")
    }

    @After
    fun teardown() = runBlocking {
        val taskList = db.taskDao().getAllTasks()
        val groupList = db.taskGroupDao().getAllGroups()

        for (groupEntry in groupList) {
            for (taskEntry in groupEntry.taskList) {
                taskEntry.groupId = 0
                db.taskDao().updateTask(taskEntry)
            }
            db.taskGroupDao().deleteGroup(groupEntry.taskGroup)
        }

        taskList.forEach {db.taskDao().deleteTask(it)}
    }

    @Test
    fun onlineDatabaseConnection()  {
        val testString = "This is a test"
        val myRef = fdb.getReference("Connection Test")
        myRef.setValue(testString)

        fdb.reference.child("Connection Test").get().addOnSuccessListener {
            Log.i("success", "${it.value}")
            assert(testString == it.value)
        }.addOnFailureListener{
            Log.e("failure", "Error getting data", it)
            assert(false) {"Did not retrieve a result!"}
        }
    }

    @Test
    fun insertGroupLocalDatabase() = runBlocking {
        val id = db.taskGroupDao().insertGroup(group)

        assert(db.taskGroupDao().getGroupByID(id).taskGroup.title == group.title){"The inserted and retrieved tasks are different."}
    }

    @Test
    fun modifyGroupLocalDatabase() = runBlocking {
        val id = db.taskGroupDao().insertGroup(group)

        val title = "This is another Test Group"
        val dbGroup = db.taskGroupDao().getGroupByID(id)
        dbGroup.taskGroup.title = title
        db.taskGroupDao().updateGroup(dbGroup.taskGroup)

        assert(db.taskGroupDao().getGroupByID(id).taskGroup.title == title) {"There is a difference in the title of the modified group."}
    }

    @Test
    fun deleteGroupLocalDatabase() = runBlocking {
        val id = db.taskGroupDao().insertGroup(group)
        val dbGroup = db.taskGroupDao().getGroupByID(id)

        val lengthBefore = db.taskGroupDao().getAllGroups().size
        for (taskEntry in dbGroup.taskList) {
            taskEntry.groupId = 0
            db.taskDao().updateTask(taskEntry)
        }
        db.taskGroupDao().deleteGroup(dbGroup.taskGroup)
        val lengthAfter = db.taskGroupDao().getAllGroups().size

        assert(lengthAfter == lengthBefore - 1) {"Deleting a Group from the database did not work."}
    }

    @Test
    fun insertTaskLocalDatabase() = runBlocking {
        val id = db.taskDao().insertTask(task)

        val dbTask = db.taskDao().getTaskByID(id)
        assert(dbTask.groupId == task.groupId) {"The inserted groupID title differs from the original one."}
        assert(dbTask.title == task.title) {"The inserted Task title differs from the original one."}
        assert(dbTask.description == task.description) {"The inserted Task description differs from the original one."}
        assert(dbTask.date == task.date) {"The inserted Task date differs from the original one."}
    }

    @Test
    fun modifyTaskLocalDatabase() = runBlocking {
        val id = db.taskDao().insertTask(task)
        val dbTask = db.taskDao().getTaskByID(id)

        val title = "This is another Test Task"
        val description = "Another Description"
        val date = ""
        dbTask.title = title
        dbTask.description = description
        dbTask.date = date
        db.taskDao().updateTask(dbTask)

        assert(dbTask.title == title) {"There is a difference in the title of the modified task."}
        assert(dbTask.description == description) {"There is a difference in the description of the modified task."}
        assert(dbTask.date == date) {"There is a difference in the date of the modified task."}
    }

    @Test
    fun deleteTaskLocalDatabase() = runBlocking {
        val id = db.taskDao().insertTask(task)
        val dbTask = db.taskDao().getTaskByID(id)

        val lengthBefore = db.taskDao().getAllTasks().size
        db.taskDao().deleteTask(dbTask)
        val lengthAfter = db.taskDao().getAllTasks().size

        assert(lengthAfter == lengthBefore - 1) {"Deleting a Group from the database did not work."}
    }

    @Test
    fun assignTaskToGroupLocalDatabase() = runBlocking {
        val groupId = db.taskGroupDao().insertGroup(group)
        task.groupId = groupId
        val taskId = db.taskDao().insertTask(task)

        val dbGroup = db.taskGroupDao().getGroupByID(groupId)
        val dbTask = db.taskDao().getTaskByID(taskId)

        assert(dbGroup.taskList.contains(dbTask)) {"Assigning a Task to a present group went wrong."}
    }

    @Test
    fun unassignTaskOfGroupLocalDatabase() = runBlocking {
        val groupId = db.taskGroupDao().insertGroup(group)
        task.groupId = groupId
        val taskId = db.taskDao().insertTask(task)

        var dbGroup = db.taskGroupDao().getGroupByID(groupId)
        val dbTask = db.taskDao().getTaskByID(taskId)

        assert(dbGroup.taskList.contains(dbTask)) {"Assigning a Task to a present group went wrong."}

        for (taskEntry in dbGroup.taskList) {
            taskEntry.groupId = task.taskId
            db.taskDao().updateTask(taskEntry)
        }
        dbGroup = db.taskGroupDao().getGroupByID(groupId)

        assert(dbGroup.taskList.isEmpty()) {"Removing a task from an existing group went wrong."}
    }

    @Test
    fun modifyAssignedTaskOfGroupLocalDatabase() = runBlocking {
        val groupId = db.taskGroupDao().insertGroup(group)
        task.groupId = groupId
        val taskId = db.taskDao().insertTask(task)

        var dbTaskOfGroup = task
        for (taskEntry in db.taskGroupDao().getGroupByID(groupId).taskList) {
            if (taskEntry.taskId == taskId) {
                dbTaskOfGroup = taskEntry
            }
        }
        assert(dbTaskOfGroup.taskId != task.taskId) {"Something went wrong when searching for an registered task for a group."}

        val title = "This is another Test Task"
        val description = "Another description"
        val date = ""
        dbTaskOfGroup.title = title
        dbTaskOfGroup.description = description
        dbTaskOfGroup.date = date
        db.taskDao().updateTask(dbTaskOfGroup)
        val modDbTask = db.taskDao().getTaskByID(taskId)

        assert(modDbTask == dbTaskOfGroup) {"Updating a task from inside a group went wrong."}
    }
}