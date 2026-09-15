package com.example.rotinapp

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.rotinapp.databinding.ActivityListaGridBinding

class ListaGridActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListaGridBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListaGridBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Dados de exemplo para o ListView
        val categorias = listOf("Estudos", "Trabalho", "Saúde", "Lazer", "Casa")
        val listAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, categorias)
        binding.listViewCategorias.adapter = listAdapter

        // Dados de exemplo para o GridView
        val itensGrid = listOf("🏠", "📚", "💪", "🎮", "🧹", "💼", "🛒", "🎯", "⏰")
        val gridAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, itensGrid)
        binding.gridViewIcones.adapter = gridAdapter

        binding.btnVoltarListaGrid.setOnClickListener {
            finish()
        }
    }
}