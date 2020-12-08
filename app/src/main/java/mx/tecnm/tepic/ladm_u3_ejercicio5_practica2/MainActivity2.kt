package mx.tecnm.tepic.ladm_u3_ejercicio5_practica2

import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_main2.*

class MainActivity2 : AppCompatActivity() {
    var baseDatos=BaseDatos(this,"basedatos1",null,1)
    var id=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        var extra=intent.extras
        id=extra!!.getString("idactualizar")!!
        textView.setText(textView.text.toString()+" ${id}")
        try {
            var base=baseDatos.readableDatabase
            var respuesta=base.query("CITA", arrayOf("LUGAR","HORA","FECHA","DESCRIPCION"),"ID=?",
                arrayOf(id),null,null,null,null)
            if(respuesta.moveToFirst()){
                editLugar.setText(respuesta.getString(0))
                editHora.setText(respuesta.getString(1))
                editFecha.setText(respuesta.getString(2))
                editDescripcion.setText(respuesta.getString(3))
            }else{
                mensaje("ERROR! no se encontro ID")
            }
            base.close()
        }catch (e: SQLiteException){
            mensaje(e.message!!)
        }

        button4.setOnClickListener {
            actualizar(id)
        }

        button5.setOnClickListener {
            var intent= Intent(this,MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    private fun mensaje(s:String){
        AlertDialog.Builder(this)
            .setTitle("ATENCIÓN")
            .setMessage(s)
            .setPositiveButton("OK"){d,i->d.dismiss()}
            .show()
    }
    private fun actualizar (id:String){
        try {
            var trans=baseDatos.writableDatabase
            var valores= ContentValues()
            valores.put("LUGAR",editLugar.text.toString())
            valores.put("HORA",editHora.text.toString())
            valores.put("FECHA",editFecha.text.toString())
            valores.put("DESCRIPCION",editDescripcion.text.toString())
            var res=trans.update("CITA",valores,"ID=?", arrayOf(id))
            if(res>0){
                mensaje("Se actualizo correctamente el evento "+editDescripcion.text)
                /*VOLVER A CARGAR LA VENTANA 1 PARA QUE SE ACTUALIZE LA LISTA,
                    PORQUE EL ****** MÉTODO DE deferNotifyDataSetChanged()*/
                /*var intent= Intent(this,MainActivity::class.java)
                startActivity(intent)
                finish()*/
            }else{
                mensaje("No se pudo actualizar")
            }
        }catch (e: SQLiteException){
            mensaje(e.message!!)
        }

    }
}