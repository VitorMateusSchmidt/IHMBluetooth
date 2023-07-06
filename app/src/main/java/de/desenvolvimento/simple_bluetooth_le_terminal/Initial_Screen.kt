package de.desenvolvimento.simple_bluetooth_le_terminal

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar

class Initial_Screen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initial_screen)
        val toolbar = findViewById<Toolbar>(R.id.toolbar) //cria objeto toolbar do componente toolbar
        setSupportActionBar(toolbar)
    }

    fun btn_save(view: android.view.View) {
        val text = R.string.toast_btnSave
        val duration = Toast.LENGTH_SHORT

        val toast = Toast.makeText(applicationContext, text, duration)
        toast.show()

    }

    fun btn_down(view: android.view.View) {
        val intent = Intent(this, MainActivity::class.java).apply {}
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val duration = Toast.LENGTH_SHORT

        if(id == R.id.site){
            abrirweb()
        }else if(id == R.id.congif){
//            val toast = Toast.makeText(applicationContext, "não há configurações", duration)
//            toast.show()
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Sobre o app")
            builder.setMessage("App usado para leitura de dados dos equipamentos fluxo:" + '\n' + "Pressione em Gráfico Salvo para ler dados já armazenados."
                                + '\n' + "Pressione em Baixar do Equipamento para conectar bluetooth e ler dados do equipamento.")
            builder.setNeutralButton("ok", null)
            val dialog: AlertDialog = builder.create()
            dialog.show()


        }else{

        }

        return super.onOptionsItemSelected(item)
    }

    fun abrirweb() {
        val uri = Uri.parse("http://fluxo.ind.br")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

//    private fun sqliteParaExcel() { //list<Pessoa> pessoas
//        //list<Pessoa> pessoas
//        val colunas = "\"NOME\",\"CPF\",\"NASCIMENTO\""
//        val root = Environment.getDataDirectory()
//        val pasta = File(root.absolutePath)
//        pasta.mkdirs()
//
//        val arquivoXLS = File(pasta, "ListaDeNomes.xls")
//
//        var streamDeSaida: FileOutputStream? = null
//
//        try {
//            streamDeSaida = FileOutputStream(arquivoXLS)
//        } catch (e: FileNotFoundException) {
//            e.printStackTrace()
//        }
//
//        try {
//            streamDeSaida!!.write(colunas.toByteArray())
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//    }
//
//    fun crc_check(msg: ByteArray?) {
//
//    }
}