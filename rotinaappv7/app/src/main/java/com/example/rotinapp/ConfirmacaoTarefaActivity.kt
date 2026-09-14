package com.example.rotinapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ConfirmacaoTarefaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOME_TAREFA = "EXTRA_NOME_TAREFA"
        const val EXTRA_HORARIO = "EXTRA_HORARIO"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmacao_tarefa)

        // Recupera os valores enviados via Intent
        val nomeTarefa = intent.getStringExtra(EXTRA_NOME_TAREFA) ?: "Tarefa sem nome"
        val horario = intent.getStringExtra(EXTRA_HORARIO) ?: "--:--"

        // Uso do findViewById
        val tvNomeTarefa: TextView = findViewById(R.id.tvNomeTarefa)
        val tvHorario: TextView = findViewById(R.id.tvHorario)
        val etObservacao: EditText = findViewById(R.id.etObservacao)
        val tvObservacaoSalva: TextView = findViewById(R.id.tvObservacaoSalva)
        val btnSalvarObservacao: Button = findViewById(R.id.btnSalvarObservacao)
        val btnVerListaGrid: Button = findViewById(R.id.btnVerListaGrid)
        val btnVoltar: Button = findViewById(R.id.btnVoltar)

        tvNomeTarefa.text = "Nome: $nomeTarefa"
        tvHorario.text = "Horário: $horario"

        // Uso do EditText: lê o texto digitado pelo usuário
        btnSalvarObservacao.setOnClickListener {
            val textoDigitado = etObservacao.text.toString()
            if (textoDigitado.isNotBlank()) {
                tvObservacaoSalva.text = "Observação salva: $textoDigitado"
                Toast.makeText(this, "Observação salva!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Digite algo antes de salvar", Toast.LENGTH_SHORT).show()
            }
        }

        btnVerListaGrid.setOnClickListener {
            startActivity(Intent(this, ListaGridActivity::class.java))
        }

        btnVoltar.setOnClickListener {
            finish()
        }
    }
}