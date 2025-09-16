package com.example.calculadora

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    private lateinit var tvDisplay: TextView

    private var currentInput: String = ""

    private var operand: Double? = null

    private var pendingOp: String? = null

    private lateinit var btnToggleTheme: MaterialButton

    private lateinit var prefs: SharedPreferences

    private val historyList = mutableListOf<String>()

    private val maxHistory = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializa SharedPreferences
        prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)

        // Verifica preferência salva e aplica antes de inflar a UI
        val isDarkMode = prefs.getBoolean("isDarkMode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // TextView de display
        tvDisplay = findViewById(R.id.txtResultado)

        // Botões de dígitos
        val digits = listOf(
            "0" to R.id.btn0,
            "1" to R.id.btn1,
            "2" to R.id.btn2,
            "3" to R.id.btn3,
            "4" to R.id.btn4,
            "5" to R.id.btn5,
            "6" to R.id.btn6,
            "7" to R.id.btn7,
            "8" to R.id.btn8,
            "9" to R.id.btn9,
            "." to R.id.btnPonto
        )
        digits.forEach { (digit, id) ->
            findViewById<Button>(id).setOnClickListener { appendDigit(digit) }
        }

        // Botões de operações
        val ops = listOf(
            "+" to R.id.btnSomar,
            "-" to R.id.btnSubtrair,
            "×" to R.id.btnMultiplicar,
            "÷" to R.id.btnDividir
        )
        ops.forEach { (op, id) ->
            findViewById<Button>(id).setOnClickListener { onOperator(op) }
        }

        // Botão igual
        findViewById<Button>(R.id.btnIgual).setOnClickListener { onEquals() }

        // Botão limpar tudo
        findViewById<Button>(R.id.btnClear).setOnClickListener { clearAll() }

        // Botão backspace
        findViewById<Button>(R.id.btnBackspace).setOnClickListener { backspace() }

        // Mapeia o botão
        btnToggleTheme = findViewById(R.id.btnToggleTheme)

        val btnHistory = findViewById<Button>(R.id.btnHistory)

        // Ajusta ícone inicial de acordo com o tema atual
        updateButtonIcon()

        // Clique do botão
        btnToggleTheme.setOnClickListener {
            if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                // Muda para claro
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                prefs.edit().putBoolean("isDarkMode", false).apply()
            } else {
                // Muda para escuro
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                prefs.edit().putBoolean("isDarkMode", true).apply()
            }
            // Atualiza ícone
            updateButtonIcon()
        }

        btnHistory.setOnClickListener {
            showHistoryPopup()
        }

        updateDisplay()
    }

    private fun appendDigit(d: String) {
        if (d == "." && currentInput.contains(".")) return
        currentInput = if (currentInput == "0") d else currentInput + d
        updateDisplay()
    }

    private fun onOperator(op: String) {
        if (currentInput.isNotEmpty()) {
            val value = currentInput.toDoubleOrNull()
            if (value != null) {
                if (operand == null) operand = value
                else operand = performOperation(operand!!, value, pendingOp)
            }
            currentInput = ""
        }
        pendingOp = op
        updateDisplay()
    }

    private fun onEquals() {
        if (operand != null && currentInput.isNotEmpty()) {
            val value = currentInput.toDoubleOrNull() ?: return
            val result = performOperation(operand!!, value, pendingOp)

            // Chama a invocação do histórico
            prepareHistory(operand, value, pendingOp, result)

            operand = null
            pendingOp = null
            currentInput = formatNumber(result)
            updateDisplay()
        }
    }

    private fun performOperation(a: Double, b: Double, op: String?): Double {
        return when (op) {
            "+" -> a + b
            "-" -> a - b
            "×" -> a * b
            "÷" -> if (b == 0.0) {
                Toast.makeText(this, "Divisão por zero", Toast.LENGTH_SHORT).show()
                a
            } else a / b
            else -> b
        }
    }

    private fun clearAll() {
        currentInput = ""
        operand = null
        pendingOp = null
        updateDisplay()
    }

    private fun backspace() {
        if (currentInput.isNotEmpty()) {
            currentInput = currentInput.dropLast(1)
            updateDisplay()
        }
    }

    private fun updateDisplay() {
        tvDisplay.text = if (currentInput.isNotEmpty()) currentInput else (operand?.toString() ?: "0")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentInput", currentInput)
        outState.putDouble("operand", operand ?: Double.NaN)
        outState.putString("pendingOp", pendingOp)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentInput = savedInstanceState.getString("currentInput", "")
        val opnd = savedInstanceState.getDouble("operand", Double.NaN)
        operand = if (opnd.isNaN()) null else opnd
        pendingOp = savedInstanceState.getString("pendingOp")
        updateDisplay()
    }

    private fun updateButtonIcon() {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            btnToggleTheme.text = "🌙"
        } else {
            btnToggleTheme.text = "☀️"
        }
    }

    private fun showHistoryPopup() {
        if (historyList.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Histórico de Operações")
                .setMessage("Nenhum histórico disponível")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // Inverte a lista e adiciona numeração
        val numberedHistory = historyList
            .asReversed()
            .mapIndexed { index, item -> "${index + 1}. $item" }

        // Cria adaptador para a lista já numerada
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, numberedHistory)

        // Cria o ListView dinamicamente
        val listView = ListView(this).apply {
            this.adapter = adapter
        }

        // Mostra o diálogo com o ListView
        AlertDialog.Builder(this)
            .setTitle("Histórico de Operações")
            .setView(listView)
            .setPositiveButton("Fechar", null)
            .show()
    }

    private fun prepareHistory(operand: Double?, value: Double, pendingOp: String?, result: Double) {
        if (operand == null || pendingOp == null) return

        val historyEntry = "${formatNumber(operand)} $pendingOp ${formatNumber(value)} = ${formatNumber(result)}"
        addToHistory(historyEntry)
    }

    private fun formatNumber(n: Double): String {
        return if (n % 1.0 == 0.0) {
            n.toInt().toString() // mostra como inteiro
        } else {
            n.toString()
        }
    }
    private fun addToHistory(operation: String) {
        if (historyList.size >= maxHistory) {
            historyList.removeAt(0) // remove o mais antigo
        }
        historyList.add(operation)
    }
}