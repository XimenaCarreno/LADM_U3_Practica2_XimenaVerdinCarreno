package mx.tecnm.tepic.ladm_u3_ejercicio5_practica2

import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    //Base de datos local
    var baseDatos=BaseDatos(this,"basedatos1",null,1)
    var listaID=ArrayList<String>()
    var idSeleccionadoEnLista=-1

    //Base de datos remota
    var baseRemota = FirebaseFirestore.getInstance()
    var datos = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {
            insertar()
        }

        button2.setOnClickListener {
            sincronizar()
        }

        button3.setOnClickListener {
            finish()
        }
        cargarCitas()
    }

    //Para la base de datos local
    private fun insertar(){
        try {
            var trans=baseDatos.writableDatabase
            var variables= ContentValues()
            variables.put("LUGAR",editText.text.toString())
            variables.put("HORA",editText2.text.toString())
            variables.put("FECHA",editTextDate.text.toString())
            variables.put("DESCRIPCION",editText4.text.toString())

            var respuesta =trans.insert("CITA",null,variables)
            if(respuesta==-1L){
                mensaje("ERROR NO SE PUDO INSERTAR")

            }else{
                mensaje("SE INSERTO CON EXITO")
                limpiarCampos()
            }
            trans.close()
        }catch (e: SQLiteException){
            mensaje(e.message!!)
        }
        cargarCitas()

    }

    private fun mensaje(s:String){
        AlertDialog.Builder(this)
            .setTitle("ATENCIÓN")
            .setMessage(s)
            .setPositiveButton("OK"){d,i->d.dismiss()}
            .show()
    }

    private fun limpiarCampos(){
        editText.setText("")
        editText2.setText("")
        editText4.setText("")
        editTextDate.setText("")
    }

    public fun cargarCitas(){
        try{
            var trans=baseDatos.readableDatabase
            var citasLocal=ArrayList<String>()
            var respuesta=trans.query("CITA", arrayOf("*"),null,null,null,null,null)
            listaID.clear()
            if (respuesta.moveToFirst()){
                do{
                    var concatenacion="ID: ${respuesta.getInt(0)}" +
                            "\nLUGAR: ${respuesta.getString(1)}" +
                            "\nHORA: ${respuesta.getString(2)}"+
                            "\nFECHA: ${respuesta.getString(3)}"+
                            "\nDESCRIPCION: ${respuesta.getString(4)}"
                    citasLocal.add(concatenacion)
                    listaID.add(respuesta.getInt(0).toString())
                }while (respuesta.moveToNext())

            }else{
                citasLocal.add("NO HAY CITAS INSERTADAS")
            }

            var adaptador = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,citasLocal)
            listacitas.adapter=adaptador
            this.registerForContextMenu(listacitas)
            listacitas.setOnItemClickListener { adapterView, view, i, l ->  idSeleccionadoEnLista=i
                Toast.makeText(this,"Se selecciono el elemento "+idSeleccionadoEnLista, Toast.LENGTH_LONG).show()}
            trans.close()
        }catch (e:SQLiteException)
        {
            mensaje("ERROR: "+e.message!!)
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        var inflaterOB=menuInflater
        inflaterOB.inflate(R.menu.menudes,menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if(idSeleccionadoEnLista==-1)
        {
            mensaje("ERROR! Debes dar clic primero en una cita para actualizarla o cancelarla")
            return true
        }
        when(item.itemId)
        {
            R.id.itemactualizar->{
                llamarVentanaAcualizar(listaID.get(idSeleccionadoEnLista))
            }

            R.id.itemeliminar->{
                var idEliminar=listaID.get(idSeleccionadoEnLista)
                AlertDialog.Builder(this)
                    .setTitle("ATENCIÓN")
                    .setMessage("¿Estas seguro de que deseas cancelar la cita:"+idEliminar+"?")
                    .setPositiveButton("ELIMINAR"){d,i->eliminar(idEliminar)}
                    .setNeutralButton("NO"){d,i->}
                    .show()
            }
            R.id.itemsalir-> {
            }
        }
        idSeleccionadoEnLista=-1
        return true
    }

    private fun eliminar(idEliminar: String) {
        try{
            var trans=baseDatos.writableDatabase
            var resultado=trans.delete("CITA","ID=?", arrayOf(idEliminar))
            if(resultado==0)
            {
                mensaje("ERROR! Hubo un problema y no se logró eliminar ")
            }
            else
            {
                mensaje("Se logró eliminar con éxito la cita")
                cargarCitas()
            }
            trans.close()
        }
        catch(e:SQLiteException)
        {
            mensaje(e.message!!)
        }

    }

    private fun llamarVentanaAcualizar(idLista: String) {
        var ventana= Intent(this,MainActivity2::class.java)
        ventana.putExtra("idactualizar",idLista)
        mensaje("ACTUALIZANDO EL EVENTO "+idLista)
        startActivity(ventana)
        finish()
    }

    private fun sincronizar(){
        datos.clear()/*Abre la base de datos de la nube para poder compararlos con los datos locales*/
        baseRemota.collection("cita").addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            if(firebaseFirestoreException !=null)
            {
                mensaje("¡ERROR! No se pudo recuperar data desde nube")
                return@addSnapshotListener
            }

            var cadena= ""
            for(registro in querySnapshot!!)
            {
                cadena=registro.id //Para el id del registro
                datos.add(cadena)
            }

            /*Insertar o Actualizar los registros en la base en cloud*/
            try {
                var trans=baseDatos.readableDatabase
                var respuesta=trans.query("CITA", arrayOf("*"),null,null,null,null,null)
                if(respuesta.moveToFirst())
                {
                    do {
                        if(datos.contains(respuesta.getString(0))) //Si ya existe el registro con ID=? en Firebase
                        {
                            actualizarRemoto(respuesta)
                        }
                        else
                        {
                            //Si no existe, insertamos el registro en el cloud
                            insertarRemoto(respuesta)
                        }
                    }while(respuesta.moveToNext())
                }
                else{
                    datos.add("NO HAY CITAS INSERTADAS")
                }
                trans.close()
            } catch (e: SQLiteException) {
                mensaje("ERROR: " + e.message!!)
            }
            /*Eliminar los registros que están en cloud, pero no en la base local
            (es decir, las que fueron eliminadas anteriormente)*/
            eliminandoRemoto()
        }
        mensaje("¡La sincronización fue exitosa!")
    }

    private fun eliminandoRemoto() {
        var eliminado= datos.subtract(listaID)
        if(eliminado.isEmpty())
        {
            /*Si continua existiendo el registro, no hagas nada*/
        }
        else
        {
            /*Si ya no existe, hay que eliminarlo*/
            eliminado.forEach(){ //Se hace un forEach para recorrer la base en cloud y encontrar el registro en cuestion
                baseRemota.collection("cita")
                    .document(it)
                    .delete()
                    .addOnSuccessListener {
                        //Aquí se hacía un Toast en el Ejercicio 3, pero aquí no se necesita
                    }
                    .addOnFailureListener{
                        mensaje("ALGO FALLO EN LA ELIMINACION\n${it.message!!}")
                    }
            }
        }
    }

    private fun actualizarRemoto(respuesta: Cursor?) {
        //Hay que actualizar el registro para que coincidan los datos locales
        // (porque no se actualiza a la par que se actualizan los locales. Por eso es sincronizacion opcional
        baseRemota.collection("cita")
            .document(respuesta!!.getString(0))
            .update(
                "LUGAR",respuesta!!.getString(1),
                "HORA",respuesta!!.getString(2),
                "FECHA",respuesta!!.getString(3),
                "DESCRIPCION", respuesta!!.getString(4)
            )
            .addOnSuccessListener {
                /*No hacer nada, no hay problema. En el ejercicio 3, aqui mostrabamos un Toast*/
            }.addOnFailureListener {
                AlertDialog.Builder(this)
                    .setTitle("ERROR")
                    .setMessage("ALGO FALLO EN LA ACTUALIZACION\n${it.message!!}")
                    .setPositiveButton("OK"){d,i->}
                    .show()
            }

    }

    private fun insertarRemoto(respuesta: Cursor?) {
        var datosInsertar = hashMapOf(
            "LUGAR" to respuesta!!.getString(1),
            "HORA" to respuesta!!.getString(2),
            "FECHA" to respuesta!!.getString(3),
            "DESCRIPCION" to respuesta!!.getString(4)
        )
        baseRemota.collection("cita").document("${respuesta!!.getString(0)}")//Asignarle el id que coincida con el local
            .set(datosInsertar as Any)
            .addOnSuccessListener {
                /*Aquí tampoco hacemos nada. En el ejercicio 3, se mostraba un Toast
                * que indicaba una transaccion exitosa*/
            }
            .addOnFailureListener{
                mensaje("ALGO FALLO EN LA CONEXIÓN\n${it.message!!}")
            }
    }
}