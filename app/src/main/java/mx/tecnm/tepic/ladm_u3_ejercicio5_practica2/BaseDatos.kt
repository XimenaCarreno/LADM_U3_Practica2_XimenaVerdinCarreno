package mx.tecnm.tepic.ladm_u3_ejercicio5_practica2

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BaseDatos(
    context: Context?,
    name:String?,
    factory: SQLiteDatabase.CursorFactory?,
    version:Int
): SQLiteOpenHelper(context,name,factory,version){
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE CITA(ID INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, LUGAR VARCHAR(200),HORA VARCHAR(200), FECHA TEXT,DESCRIPCION VARCHAR (500))")
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
    }

}