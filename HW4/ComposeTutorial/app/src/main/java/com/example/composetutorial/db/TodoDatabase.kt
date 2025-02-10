package com.example.composetutorial.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.composetutorial.Todo


@Database(entities =  [Todo::class], version = 1)
@TypeConverters(Converters::class)
abstract class TodoDatabase : RoomDatabase(){

    companion object {
        const val NAME = "Tobo_DB"
        const val PROFILEPICTURE = "Profile_Info"
    }

    abstract fun getTodoDao() : TodoDao

}
