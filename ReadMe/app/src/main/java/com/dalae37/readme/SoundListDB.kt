package com.dalae37.readme

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SoundListDB(context : Context) : SQLiteOpenHelper(context, DATABASE_NAME,null,DATABASE_VERSION) {
    companion object{
        //DB
        private val DATABASE_VERSION = 1
        private val DATABASE_NAME = "soundList.db"

        //Table
        private val TABLE_NAME = "Sound"
        private val COOL_NAME = "SoundName"
        private val COOL_DIR = "SoundDir"
    }
    override fun onCreate(p0: SQLiteDatabase?) {
        val create_sql = "CREATE TABLE $TABLE_NAME ($COOL_NAME TEXT PRIMARY KEY"

    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
        if(p0 != null){
            p0.execSQL("DROP TABLE IF EXISTS $DATABASE_NAME")
            onCreate(p0)
        }
    }

    fun addSound(sound : Sound){
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COOL_NAME, sound.soundName)
        values.put(COOL_DIR, sound.soundDir)

        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    fun deleteSound(sound : Sound){
        val db = this.writableDatabase

        db.delete(TABLE_NAME,"$COOL_NAME=?", arrayOf(sound.soundName.toString()))
        db.close()
    }
}