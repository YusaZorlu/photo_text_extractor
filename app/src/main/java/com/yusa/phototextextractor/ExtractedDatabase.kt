package com.yusa.phototextextractor

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [ExtractedImage::class], version = 1)
abstract class ExtractedDatabase: RoomDatabase() {
    abstract fun extractedDao():ExtractedDao
    companion object{
        @Volatile
        private var INSTANCE :ExtractedDatabase? = null
        fun getDatabase(context: Context): ExtractedDatabase{
            val tempInstance = INSTANCE
            if (tempInstance != null){
                return tempInstance
            }
            synchronized(this){
                var instance = INSTANCE
                if (instance == null){
                    instance = Room.databaseBuilder(context.applicationContext,
                        ExtractedDatabase::class.java,
                        "jetpack").build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}