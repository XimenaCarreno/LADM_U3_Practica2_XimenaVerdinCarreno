package mx.tecnm.tepic.ladm_u3_ejercicio5_practica2

import android.content.Intent
import android.database.sqlite.SQLiteException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main3.*
import kotlinx.android.synthetic.main.activity_main3.listacitas

class MainActivity3 : AppCompatActivity() {
    var baseDatos=BaseDatos(this,"basedatos1",null,1)
    var listaID=ArrayList<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)

        button6.setOnClickListener {
            consultacion()
        }

        button7.setOnClickListener {
            eliminacion()
        }

        button8.setOnClickListener {
            var intent= Intent(this,MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun eliminacion() {
        try {

            var trans=baseDatos.writableDatabase
            var lugar=Lugar.text.toString()
            var fecha=Fecha.text.toString()
            var citasLocal=ArrayList<String>()
            var resultadoL=trans.delete("CITA","LUGAR=?",
                arrayOf(lugar))
            var resultadoF=trans.delete("CITA","FECHA=?",
                arrayOf(fecha))
            if (resultadoL==0 && resultadoF==0){
                mensaje("ERROR! No se pudo eliminar")
            }else{
                mensaje("Se eliminaron los registros con éxito")
            }
            listaID.clear()
            citasLocal.add("SIN CITAS")
            var adaptador = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,citasLocal)
            listacitas.adapter=adaptador
            this.registerForContextMenu(listacitas)
            trans.close()
        }catch (e:SQLiteException){
            mensaje(e.message!!)
        }
    }

    private fun consultacion() {
        try {
            var trans=baseDatos.readableDatabase
            var citasLocal=ArrayList<String>()
            var fecha=Fecha.text.toString()
            var lugar=Lugar.text.toString()
            var descripcion=Descripcion.text.toString()
            var respuestaL=trans.query("CITA", arrayOf("*"),
                "LUGAR=?", arrayOf(lugar),null,null,null)
            var respuestaF=trans.query("CITA", arrayOf("*"),
                "FECHA=?", arrayOf(fecha),null,null,null)
            var respuestaD=trans.query("CITA", arrayOf("*"),
                "DESCRIPCION=?", arrayOf(descripcion),null,null,null)
            listaID.clear()
            //BUSCAR POR LUGAR
            if (respuestaL.moveToFirst()){
                do{
                    var concatenacion="ID: ${respuestaL.getInt(0)}" +
                            "\nLUGAR: ${respuestaL.getString(1)}" +
                            "\nHORA: ${respuestaL.getString(2)}"+
                            "\nFECHA: ${respuestaL.getString(3)}"+
                            "\nDESCRIPCION: ${respuestaL.getString(4)}"
                    citasLocal.add(concatenacion)
                    listaID.add(respuestaL.getInt(0).toString())
                }while (respuestaL.moveToNext())
            }else{
                //SINO HAY RESULTADOS, BUSCAR POR FECHAS
                if (respuestaF.moveToFirst()) {
                    do {
                        var concatenacion = "ID: ${respuestaF.getInt(0)}" +
                                "\nLUGAR: ${respuestaF.getString(1)}" +
                                "\nHORA: ${respuestaF.getString(2)}" +
                                "\nFECHA: ${respuestaF.getString(3)}" +
                                "\nDESCRIPCION: ${respuestaF.getString(4)}"
                        citasLocal.add(concatenacion)
                        listaID.add(respuestaF.getInt(0).toString())
                    } while (respuestaF.moveToNext())
                }
                else
                {
                    //SI NO HAY RESULTADOS, BUSCAR POR DESCRIPCION
                    if (respuestaD.moveToFirst()) {
                        do {
                            var concatenacion = "ID: ${respuestaD.getInt(0)}" +
                                    "\nLUGAR: ${respuestaD.getString(1)}" +
                                    "\nHORA: ${respuestaD.getString(2)}" +
                                    "\nFECHA: ${respuestaD.getString(3)}" +
                                    "\nDESCRIPCION: ${respuestaD.getString(4)}"
                            citasLocal.add(concatenacion)
                            listaID.add(respuestaD.getInt(0).toString())
                        } while (respuestaD.moveToNext())
                    }
                    else
                    {
                        //SI NINGUNA BUSQUEDA ARROJÓ RESULTADOS
                        citasLocal.add("NO HAY COINCIDENCIAS")
                    }
                }
            }
            var adaptador = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,citasLocal)
            listacitas.adapter=adaptador
            this.registerForContextMenu(listacitas)
            trans.close()
        }catch (e: SQLiteException){
            mensaje(e.message!!)
        }
    }
    private fun mensaje(s:String){
        AlertDialog.Builder(this)
            .setTitle("ATENCIÓN")
            .setMessage(s)
            .setPositiveButton("OK"){d,i->d.dismiss()}
            .show()
    }
}