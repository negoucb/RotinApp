package com.example.rotinapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.GridView
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class ListaGridActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_grid)

        val listView: ListView = findViewById(R.id.listViewCategorias)
        val gridView: GridView = findViewById(R.id.gridViewIcones)
        val btnVoltar: Button = findViewById(R.id.btnVoltarListaGrid)

        // Dados de exemplo para o ListView
        val categorias = listOf("Estudos", "Trabalho", "Saúde", "Lazer", "Casa")
        val listAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, categorias)
        listView.adapter = listAdapter

        // Dados de exemplo para o GridView
        val itensGrid = listOf("🏠", "📚", "💪", "🎮", "🧹", "💼", "🛒", "🎯", "⏰")
        val gridAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, itensGrid)
        gridView.adapter = gridAdapter

        btnVoltar.setOnClickListener {
            finish()
        }
    }
}