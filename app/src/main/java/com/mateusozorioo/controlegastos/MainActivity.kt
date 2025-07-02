package com.mateusozorioo.controlegastos

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mateusozorioo.controlegastos.ui.theme.ControleGastosTheme
import java.text.SimpleDateFormat
import java.util.*

// Classe que representa um gasto
data class Expense(
    val id: String,
    val valor: Double,
    val categoria: String,
    val descricao: String,
    val data: String
) {
    // Converte para string para salvar
    fun toSaveString(): String {
        return "$id|$valor|$categoria|$descricao|$data"
    }

    companion object {
        // Cria Expense a partir de string salva
        fun fromSaveString(saveString: String): Expense? {
            return try {
                val parts = saveString.split("|")
                if (parts.size == 5) {
                    Expense(
                        id = parts[0],
                        valor = parts[1].toDouble(),
                        categoria = parts[2],
                        descricao = parts[3],
                        data = parts[4]
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}

// Classe para gerenciar a persistência dos dados
class ExpenseRepository(private val context: Context) {
    //Usa SharedPreferences para salvar os dados localmente.
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)

    //saveExpenses(): salva uma lista como string.
    fun saveExpenses(expenses: List<Expense>) {
        val expensesString = expenses.joinToString(separator = ";;;") { it.toSaveString() }
        sharedPreferences.edit().putString("expenses", expensesString).apply()
    }

    //loadExpenses(): carrega e converte de volta em objetos.
    fun loadExpenses(): List<Expense> {
        val savedString = sharedPreferences.getString("expenses", null)
        return if (savedString != null && savedString.isNotEmpty()) {
            savedString.split(";;;").mapNotNull { Expense.fromSaveString(it) }
        } else {
            // Dados de exemplo apenas na primeira execução
            listOf(
                Expense("1", 25.50, "Alimentação", "Lanche", "01/07/2024"),
                Expense("2", 80.00, "Transporte", "Uber", "01/07/2024"),
                Expense("3", 150.00, "Compras", "Supermercado", "30/06/2024")
            )
        }
    }
}

// MainActivity é a atividade principal do app.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //Inicia a interface
        setContent {
            ControleGastosTheme {
                //Scaffold: estrutura de layout padrão do Compose
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ExpenseList(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
// A lógica principal do app está aqui.
@Composable
fun ExpenseList(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { ExpenseRepository(context) }

    // Estados para controlar a interface (se o diálogo está visível, valores dos inputs etc.)
    var showAddDialog by remember { mutableStateOf(false) }
    var novoValor by remember { mutableStateOf("") }
    var novaCategoria by remember { mutableStateOf("") }
    var novaDescricao by remember { mutableStateOf("") }
    //mutableStateListOf garante que, ao adicionar ou remover gastos, a UI se atualiza automaticamente.


    // Lista de categorias pré-definidas
    val categorias = listOf("Alimentação", "Transporte", "Compras", "Lazer", "Saúde", "Outros")
    var categoriaExpandida by remember { mutableStateOf(false) }

    // Lista de gastos com persistência
    val gastos = remember { mutableStateListOf<Expense>() }

    // Carregar gastos salvos no SharedPreferences ao iniciar
    LaunchedEffect(Unit) {
        val savedExpenses = repository.loadExpenses()
        gastos.clear()
        gastos.addAll(savedExpenses)
    }

    // Função para salvar gastos
    fun saveExpenses() {
        repository.saveExpenses(gastos.toList())
    }

    Column(modifier = modifier.padding(16.dp)) {
        // Cabeçalho
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            // Cabeçalho com o total
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "💰 Controle de Gastos",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Total: R$ ${"%.2f".format(gastos.sumOf { it.valor })}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${gastos.size} gastos registrados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botão para "Adicionar Novo Gasto"
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Adicionar gasto")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Adicionar Novo Gasto")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista de gastos
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(gastos) { gasto ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        //Cada gasto vira um cartão (Card) com descrição, categoria, valor e botão de deletar
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = gasto.descricao,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = gasto.categoria,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = gasto.data,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "R$ ${"%.2f".format(gasto.valor)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            IconButton(
                                onClick = {
                                    //Excluir gasto
                                    gastos.remove(gasto)
                                    saveExpenses() // Salvar após excluir
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Excluir gasto",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo para adicionar gasto
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Adicionar Gasto") },
            text = {
                Column {
                    //Valor
                    OutlinedTextField(
                        value = novoValor,
                        onValueChange = { novoValor = it },
                        label = { Text("Valor (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Dropdown para categoria
                    ExposedDropdownMenuBox(
                        expanded = categoriaExpandida,
                        onExpandedChange = { categoriaExpandida = !categoriaExpandida }
                    ) {
                        OutlinedTextField(
                            value = novaCategoria,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Categoria") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoriaExpandida) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoriaExpandida,
                            onDismissRequest = { categoriaExpandida = false }
                        ) {
                            categorias.forEach { categoria ->
                                DropdownMenuItem(
                                    text = { Text(categoria) },
                                    onClick = {
                                        novaCategoria = categoria
                                        categoriaExpandida = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    //Descrição
                    OutlinedTextField(
                        value = novaDescricao,
                        onValueChange = { novaDescricao = it },
                        label = { Text("Descrição") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },

            //Botão de confirmação para salvar os dados
            confirmButton = {
                TextButton(
                    onClick = {
                        val valor = novoValor.replace(",", ".").toDoubleOrNull()
                        if (valor != null && valor > 0 && novaCategoria.isNotEmpty() && novaDescricao.isNotEmpty()) {
                            gastos.add(
                                Expense(
                                    id = UUID.randomUUID().toString(),
                                    valor = valor,
                                    categoria = novaCategoria,
                                    descricao = novaDescricao,
                                    data = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                                )
                            )
                            saveExpenses() // Salvar após adicionar
                            // Limpar campos
                            novoValor = ""
                            novaCategoria = ""
                            novaDescricao = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Adicionar") // Ao clicar nesse botão você dá o comando para executar tudo que está acima
                }
            },
            //Botão para voltar a tela inicial
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}