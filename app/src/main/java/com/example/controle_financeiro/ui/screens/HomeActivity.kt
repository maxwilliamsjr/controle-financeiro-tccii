package com.example.controle_financeiro.ui.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.os.Build
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.controle_financeiro.LoginActivity
import com.example.controle_financeiro.R
import android.graphics.Color
import android.view.View
import com.example.controle_financeiro.ui.categoria.MenuCategoriaActivity
import com.example.controle_financeiro.ui.despesa.MenuDespesasActivity
import com.example.controle_financeiro.ui.metodopagamento.MenuMetodoPagamentoActivity
import com.example.controle_financeiro.ui.planejamento.PlanejamentoActivity
import com.example.controle_financeiro.ui.renda.MenuRendaActivity
import com.example.controle_financeiro.PerfilActivity
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.util.*

class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var containerAnteriorOutros: LinearLayout
    private lateinit var containerAnteriorCartoes: LinearLayout
    private lateinit var txtTotalAnteriorValor: TextView

    private lateinit var containerAtualOutros: LinearLayout
    private lateinit var containerAtualCartoes: LinearLayout
    private lateinit var txtTotalAtualValor: TextView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        window.statusBarColor = android.graphics.Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }
        setContentView(R.layout.activity_home)

        initializeViews()
        setupToolbar()
        setupNavigation()
        setupRefreshListener()
        setupButtonListeners()
        loadUserData()
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        containerAnteriorOutros = findViewById(R.id.containerAnteriorOutros)
        containerAnteriorCartoes = findViewById(R.id.containerAnteriorCartoes)
        txtTotalAnteriorValor = findViewById(R.id.txtTotalAnteriorValor)

        containerAtualOutros = findViewById(R.id.containerAtualOutros)
        containerAtualCartoes = findViewById(R.id.containerAtualCartoes)
        txtTotalAtualValor = findViewById(R.id.txtTotalAtualValor)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun setupNavigation() {
        navigationView.setNavigationItemSelectedListener(this)
    }

    private fun setupRefreshListener() {
        swipeRefreshLayout.setOnRefreshListener {
            loadExpenseDataAnterior()
            loadExpenseDataAtual()
        }
    }

    private fun setupButtonListeners() {
        findViewById<TextView>(R.id.btnDespesas).setOnClickListener {
            startActivity(Intent(this, MenuDespesasActivity::class.java))
        }
        findViewById<TextView>(R.id.btnRendas).setOnClickListener {
            startActivity(Intent(this, MenuRendaActivity::class.java))
        }
        findViewById<TextView>(R.id.btnMetodos).setOnClickListener {
            startActivity(Intent(this, MenuMetodoPagamentoActivity::class.java))
        }
        findViewById<TextView>(R.id.btnCategorias).setOnClickListener {
            startActivity(Intent(this, MenuCategoriaActivity::class.java))
        }
        /*
        findViewById<TextView>(R.id.btnPlanejamento).setOnClickListener {
            startActivity(Intent(this, PlanejamentoActivity::class.java))
        }
        */
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("usuarios").document(userId).get()
            .addOnSuccessListener { doc ->
                doc.getString("nome")?.let { name ->
                    val firstName = name.split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: ""
                    supportActionBar?.title = "Olá, $firstName"
                }
                loadExpenseDataAnterior()
                loadExpenseDataAtual()
            }
            .addOnFailureListener {
                loadExpenseDataAnterior()
                loadExpenseDataAtual()
            }
    }

    private fun loadExpenseDataAnterior() {
        val userId = auth.currentUser?.uid ?: run {
            showAuthError()
            return
        }

        containerAnteriorOutros.removeAllViews()
        containerAnteriorCartoes.removeAllViews()
        swipeRefreshLayout.isRefreshing = true

        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val fixos = listOf("Pix", "Boleto", "Transferência", "Dinheiro", "Débito automático")

        var totalFixos = 0.0
        var totalDebito = 0.0

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        val inicioMesAnterior = calendar.apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val fimMesAnterior = calendar.apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time

        db.collection("despesas")
            .whereEqualTo("userId", userId)
            .whereIn("metodoPagamento", fixos)
            .whereGreaterThanOrEqualTo("data", inicioMesAnterior)
            .whereLessThanOrEqualTo("data", fimMesAnterior)
            .get()
            .addOnSuccessListener { snapFixos ->
                totalFixos = snapFixos.documents.sumOf { it.getDouble("valor") ?: 0.0 }
                Log.d("HomeActivity", "Anterior - Total despesas fixas: $totalFixos")

                // Cartões débito mês anterior
                db.collection("cartoes")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("tipo", "Débito")
                    .get()
                    .addOnSuccessListener { snapDebitoCartoes ->
                        val debitoIds = snapDebitoCartoes.documents.mapNotNull { it.getString("id") }
                        if (debitoIds.isEmpty()) {
                            loadCartoesCreditoAnterior(userId, totalFixos, formatter, totalFixos)
                        } else {
                            db.collection("despesas")
                                .whereEqualTo("userId", userId)
                                .whereIn("cartaoId", debitoIds)
                                .whereGreaterThanOrEqualTo("data", inicioMesAnterior)
                                .whereLessThanOrEqualTo("data", fimMesAnterior)
                                .get()
                                .addOnSuccessListener { snapDebitoDespesas ->
                                    totalDebito = snapDebitoDespesas.documents.sumOf { it.getDouble("valor") ?: 0.0 }
                                    Log.d("HomeActivity", "Anterior - Total despesas débito: $totalDebito")
                                    loadCartoesCreditoAnterior(userId, totalFixos + totalDebito, formatter, totalFixos + totalDebito)
                                }
                                .addOnFailureListener {
                                    loadCartoesCreditoAnterior(userId, totalFixos, formatter, totalFixos)
                                }
                        }
                    }
                    .addOnFailureListener {
                        loadCartoesCreditoAnterior(userId, totalFixos, formatter, totalFixos)
                    }
            }
            .addOnFailureListener {
                loadCartoesCreditoAnterior(userId, 0.0, formatter, 0.0)
            }
    }

    private fun loadCartoesCreditoAnterior(userId: String, totalOutros: Double, formatter: NumberFormat, totalGeralInicial: Double) {
        db.collection("cartoes")
            .whereEqualTo("userId", userId)
            .whereEqualTo("tipo", "Crédito")
            .get()
            .addOnSuccessListener { snapCreditoCartoes ->
                containerAnteriorCartoes.removeAllViews()

                if (snapCreditoCartoes.isEmpty) {
                    updateOutrosAnterior(totalOutros, formatter)
                    updateTotalGeralAnterior(totalGeralInicial, formatter)
                    swipeRefreshLayout.isRefreshing = false
                    return@addOnSuccessListener
                }

                val totaisFaturas = mutableListOf<Double>()
                var processed = 0

                fun done() {
                    processed++
                    if (processed == snapCreditoCartoes.size()) {
                        val totalCredito = totaisFaturas.sum()
                        val totalGeral = totalOutros + totalCredito
                        updateOutrosAnterior(totalOutros, formatter)
                        updateTotalGeralAnterior(totalGeral, formatter)
                        swipeRefreshLayout.isRefreshing = false
                    }
                }

                val hoje = Timestamp.now().toDate()

                snapCreditoCartoes.documents.forEach { cartaoDoc ->
                    val cartaoId = cartaoDoc.getString("id") ?: ""
                    val banco = cartaoDoc.getString("banco") ?: ""
                    val nome = cartaoDoc.getString("nome") ?: ""
                    val bandeira = cartaoDoc.getString("bandeira") ?: ""
                    val nomeCompleto = "$banco $nome $bandeira"

                    db.collection("cartoes").document(cartaoId)
                        .collection("fechamentos")
                        .whereLessThanOrEqualTo("dataFechamento", hoje)
                        .orderBy("dataFechamento", Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { snapFaturas ->
                            val docFatura = snapFaturas.documents.firstOrNull()
                            if (docFatura == null) {
                                addCartaoComValorAnterior(nomeCompleto, 0.0, formatter)
                                totaisFaturas.add(0.0)
                                done()
                            } else {
                                val dtIni = docFatura.getTimestamp("dataInicio")?.toDate()
                                val dtFim = docFatura.getTimestamp("dataFechamento")?.toDate()
                                if (dtIni == null || dtFim == null) {
                                    addCartaoComValorAnterior(nomeCompleto, 0.0, formatter)
                                    totaisFaturas.add(0.0)
                                    done()
                                } else {
                                    db.collection("despesas")
                                        .whereEqualTo("userId", userId)
                                        .whereEqualTo("cartaoId", cartaoId)
                                        .whereGreaterThanOrEqualTo("data", dtIni)
                                        .whereLessThanOrEqualTo("data", dtFim)
                                        .get()
                                        .addOnSuccessListener { snapDespesasFatura ->
                                            val somaFatura = snapDespesasFatura.documents.sumOf { it.getDouble("valor") ?: 0.0 }
                                            addCartaoComValorAnterior(nomeCompleto, somaFatura, formatter)
                                            totaisFaturas.add(somaFatura)
                                            done()
                                        }
                                        .addOnFailureListener {
                                            addCartaoComValorAnterior(nomeCompleto, 0.0, formatter)
                                            totaisFaturas.add(0.0)
                                            done()
                                        }
                                }
                            }
                        }
                        .addOnFailureListener {
                            addCartaoComValorAnterior(nomeCompleto, 0.0, formatter)
                            totaisFaturas.add(0.0)
                            done()
                        }
                }
            }
            .addOnFailureListener {
                updateOutrosAnterior(totalOutros, formatter)
                updateTotalGeralAnterior(totalGeralInicial, formatter)
                swipeRefreshLayout.isRefreshing = false
            }
    }

    private fun updateOutrosAnterior(total: Double, formatter: NumberFormat) {
        containerAnteriorOutros.removeAllViews()
        if (total > 0.0) addValue(containerAnteriorOutros, total, formatter)
    }

    private fun updateTotalGeralAnterior(total: Double, formatter: NumberFormat) {
        txtTotalAnteriorValor.text = formatter.format(total)
    }

    private fun addCartaoComValorAnterior(nome: String, valor: Double, formatter: NumberFormat) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 12)
        }
        layout.addView(TextView(this).apply {
            text = nome
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
        })
        layout.addView(TextView(this).apply {
            text = formatter.format(valor)
            textSize = 14f
            gravity = Gravity.CENTER_HORIZONTAL
        })
        containerAnteriorCartoes.addView(layout)
    }

    private fun loadExpenseDataAtual() {
        val userId = auth.currentUser?.uid ?: run {
            showAuthError()
            return
        }

        containerAtualOutros.removeAllViews()
        containerAtualCartoes.removeAllViews()
        swipeRefreshLayout.isRefreshing = true

        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val fixos = listOf("Pix", "Boleto", "Transferência", "Dinheiro", "Débito automático")

        var totalFixos = 0.0
        var totalDebito = 0.0

        val hoje = Calendar.getInstance()
        val inicioMesAtual = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val fimMesAtual = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time

        db.collection("despesas")
            .whereEqualTo("userId", userId)
            .whereIn("metodoPagamento", fixos)
            .whereGreaterThanOrEqualTo("data", inicioMesAtual)
            .whereLessThanOrEqualTo("data", fimMesAtual)
            .get()
            .addOnSuccessListener { snapFixos ->
                totalFixos = snapFixos.documents.sumOf { it.getDouble("valor") ?: 0.0 }
                Log.d("HomeActivity", "Atual - Total despesas fixas: $totalFixos")

                db.collection("cartoes")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("tipo", "Débito")
                    .get()
                    .addOnSuccessListener { snapDebitoCartoes ->
                        val debitoIds = snapDebitoCartoes.documents.mapNotNull { it.getString("id") }
                        if (debitoIds.isEmpty()) {
                            loadCartoesCreditoAtual(userId, totalFixos, formatter, totalFixos)
                        } else {
                            db.collection("despesas")
                                .whereEqualTo("userId", userId)
                                .whereIn("cartaoId", debitoIds)
                                .whereGreaterThanOrEqualTo("data", inicioMesAtual)
                                .whereLessThanOrEqualTo("data", fimMesAtual)
                                .get()
                                .addOnSuccessListener { snapDebitoDespesas ->
                                    totalDebito = snapDebitoDespesas.documents.sumOf { it.getDouble("valor") ?: 0.0 }
                                    Log.d("HomeActivity", "Atual - Total despesas débito: $totalDebito")
                                    loadCartoesCreditoAtual(userId, totalFixos + totalDebito, formatter, totalFixos + totalDebito)
                                }
                                .addOnFailureListener {
                                    loadCartoesCreditoAtual(userId, totalFixos, formatter, totalFixos)
                                }
                        }
                    }
                    .addOnFailureListener {
                        loadCartoesCreditoAtual(userId, totalFixos, formatter, totalFixos)
                    }
            }
            .addOnFailureListener {
                loadCartoesCreditoAtual(userId, 0.0, formatter, 0.0)
            }
    }

    private fun loadCartoesCreditoAtual(userId: String, totalOutros: Double, formatter: NumberFormat, totalGeralInicial: Double) {
        db.collection("cartoes")
            .whereEqualTo("userId", userId)
            .whereEqualTo("tipo", "Crédito")
            .get()
            .addOnSuccessListener { snapCreditoCartoes ->
                containerAtualCartoes.removeAllViews()

                Log.d("HomeActivity", "Cartões crédito encontrados: ${snapCreditoCartoes.size()}")

                if (snapCreditoCartoes.isEmpty) {
                    Log.d("HomeActivity", "Nenhum cartão de crédito encontrado.")
                    updateOutrosAtual(totalOutros, formatter)
                    updateTotalGeralAtual(totalGeralInicial, formatter)
                    swipeRefreshLayout.isRefreshing = false
                    return@addOnSuccessListener
                }

                val totaisFaturas = mutableListOf<Double>()
                var processed = 0

                fun done() {
                    processed++
                    if (processed == snapCreditoCartoes.size()) {
                        val totalCredito = totaisFaturas.sum()
                        val totalGeral = totalOutros + totalCredito
                        Log.d("HomeActivity", "Total crédito (soma das faturas atuais): $totalCredito")
                        Log.d("HomeActivity", "Total geral atualizado (outros + crédito): $totalGeral")
                        updateOutrosAtual(totalOutros, formatter)
                        updateTotalGeralAtual(totalGeral, formatter)
                        swipeRefreshLayout.isRefreshing = false
                    }
                }

                val hoje = Timestamp.now().toDate()

                fun isDateInInterval(date: Date, start: Date, end: Date): Boolean {
                    val dateOnly = date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    val startOnly = start.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    val endOnly = end.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    return (dateOnly.isEqual(startOnly) || dateOnly.isAfter(startOnly)) &&
                            (dateOnly.isEqual(endOnly) || dateOnly.isBefore(endOnly))
                }

                snapCreditoCartoes.documents.forEach { cartaoDoc ->
                    val cartaoId = cartaoDoc.getString("id") ?: ""
                    val banco = cartaoDoc.getString("banco") ?: ""
                    val nome = cartaoDoc.getString("nome") ?: ""
                    val bandeira = cartaoDoc.getString("bandeira") ?: ""
                    val nomeCompleto = "$banco $nome $bandeira"

                    Log.d("HomeActivity", "Buscando faturas para cartão: $nomeCompleto (ID: $cartaoId)")

                    db.collection("cartoes").document(cartaoId)
                        .collection("fechamentos")
                        .orderBy("dataFechamento", Query.Direction.DESCENDING)
                        .get()
                        .addOnSuccessListener { snapFaturas ->

                            Log.d("HomeActivity", "Faturas encontradas para cartão $nomeCompleto:")
                            snapFaturas.documents.forEach { doc ->
                                val dtIni = doc.getTimestamp("dataInicio")?.toDate()
                                val dtFim = doc.getTimestamp("dataFechamento")?.toDate()
                                Log.d("HomeActivity", " - Fatura: início=$dtIni, fim=$dtFim")
                            }

                            val docFaturaEmVigor = snapFaturas.documents.find { doc ->
                                val dtIni = doc.getTimestamp("dataInicio")?.toDate()
                                val dtFim = doc.getTimestamp("dataFechamento")?.toDate()
                                Log.d("HomeActivity", "Comparando data hoje=$hoje com fatura: inicio=$dtIni, fim=$dtFim")
                                dtIni != null && dtFim != null && isDateInInterval(hoje, dtIni, dtFim)
                            }

                            if (docFaturaEmVigor == null) {
                                Log.d("HomeActivity", "Nenhuma fatura em vigor encontrada para o cartão $nomeCompleto")
                                addCartaoComValorAtual(nomeCompleto, 0.0, formatter)
                                totaisFaturas.add(0.0)
                                done()
                            } else {
                                val dtIni = docFaturaEmVigor.getTimestamp("dataInicio")?.toDate()
                                val dtFim = docFaturaEmVigor.getTimestamp("dataFechamento")?.toDate()
                                Log.d("HomeActivity", "Fatura em vigor para $nomeCompleto: início=$dtIni, fim=$dtFim")

                                db.collection("despesas")
                                    .whereEqualTo("userId", userId)
                                    .whereEqualTo("cartaoId", cartaoId)
                                    .whereGreaterThanOrEqualTo("data", dtIni!!)
                                    .whereLessThanOrEqualTo("data", dtFim!!)
                                    .get()
                                    .addOnSuccessListener { snapDespesasFatura ->
                                        val somaFatura = snapDespesasFatura.documents.sumOf { it.getDouble("valor") ?: 0.0 }
                                        Log.d("HomeActivity", "Soma despesas fatura cartão $nomeCompleto: $somaFatura")
                                        addCartaoComValorAtual(nomeCompleto, somaFatura, formatter)
                                        totaisFaturas.add(somaFatura)
                                        done()
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("HomeActivity", "Erro ao buscar despesas da fatura do cartão $nomeCompleto", e)
                                        addCartaoComValorAtual(nomeCompleto, 0.0, formatter)
                                        totaisFaturas.add(0.0)
                                        done()
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("HomeActivity", "Erro ao buscar faturas para cartão $nomeCompleto", e)
                            addCartaoComValorAtual(nomeCompleto, 0.0, formatter)
                            totaisFaturas.add(0.0)
                            done()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("HomeActivity", "Erro ao buscar cartões de crédito", e)
                updateOutrosAtual(totalOutros, formatter)
                updateTotalGeralAtual(totalGeralInicial, formatter)
                swipeRefreshLayout.isRefreshing = false
            }
    }

    private fun updateOutrosAtual(total: Double, formatter: NumberFormat) {
        containerAtualOutros.removeAllViews()
        if (total > 0.0) addValue(containerAtualOutros, total, formatter)
    }

    private fun updateTotalGeralAtual(total: Double, formatter: NumberFormat) {
        txtTotalAtualValor.text = formatter.format(total)
    }

    private fun addCartaoComValorAtual(nome: String, valor: Double, formatter: NumberFormat) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 12)
        }
        layout.addView(TextView(this).apply {
            text = nome
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
        })
        layout.addView(TextView(this).apply {
            text = formatter.format(valor)
            textSize = 14f
            gravity = Gravity.CENTER_HORIZONTAL
        })
        containerAtualCartoes.addView(layout)
    }

    private fun addValue(container: LinearLayout, value: Double, formatter: NumberFormat) {
        container.addView(TextView(this).apply {
            text = formatter.format(value)
            textSize = 18f
            setPadding(0, 0, 0, 12)
            gravity = Gravity.CENTER_HORIZONTAL
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
    }

    private fun showAuthError() {
        Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_perfil -> {
                startActivity(Intent(this, PerfilActivity::class.java))
            }
            R.id.menu_sair -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }


}

//teste
