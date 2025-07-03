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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mateusozorioo.controlegastos.ui.theme.ControleGastosTheme
import java.text.SimpleDateFormat
import java.util.*

// Classe que representa um gasto individual
data class Expense(
    val id: String,          // ID único para identificar cada gasto
    val valor: Double,       // Valor em reais do gasto
    val categoria: String,   // Categoria do gasto (Alimentação, Transporte, etc.)
    val descricao: String,   // Descrição detalhada do gasto
    val data: String         // Data do gasto no formato dd/MM/yyyy
) {
    // Método para converter o objeto Expense em uma string para salvar no SharedPreferences
    // Usa o separador "|" para dividir os campos
    fun toSaveString(): String {
        return "$id|$valor|$categoria|$descricao|$data"
    }

    companion object {
        // Método estático para criar um objeto Expense a partir de uma string salva
        // Desfaz o processo do toSaveString()
        fun fromSaveString(saveString: String): Expense? {
            return try {
                val parts = saveString.split("|")  // Divide a string pelos separadores "|"
                if (parts.size == 5) {             // Verifica se tem todos os 5 campos necessários
                    Expense(
                        id = parts[0],
                        valor = parts[1].toDouble(),
                        categoria = parts[2],
                        descricao = parts[3],
                        data = parts[4]
                    )
                } else null  // Retorna null se a string estiver malformada
            } catch (e: Exception) {
                null  // Retorna null se houver erro na conversão
            }
        }
    }
}

// Classe responsável por gerenciar a persistência dos dados no dispositivo
class ExpenseRepository(private val context: Context) {
    // Usa SharedPreferences para salvar os dados localmente no dispositivo
    // É como um "banco de dados" simples que persiste entre execuções do app
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)

    // Método para salvar uma lista de gastos no SharedPreferences
    // Converte a lista em uma única string usando ";;;" como separador entre gastos
    fun saveExpenses(expenses: List<Expense>) {
        val expensesString = expenses.joinToString(separator = ";;;") { it.toSaveString() }
        sharedPreferences.edit().putString("expenses", expensesString).apply()
    }

    // Método para carregar os gastos salvos do SharedPreferences
    // Converte a string salva de volta em uma lista de objetos Expense
    fun loadExpenses(): List<Expense> {
        val savedString = sharedPreferences.getString("expenses", null)
        return if (savedString != null && savedString.isNotEmpty()) {
            // Se há dados salvos, converte cada parte em um objeto Expense
            savedString.split(";;;").mapNotNull { Expense.fromSaveString(it) }
        } else {
            // Se não há dados salvos, retorna uma lista com dados de exemplo
            // Isso acontece apenas na primeira execução do app
            listOf(
                Expense("1", 25.50, "Alimentação", "Lanche", "01/07/2024"),
                Expense("2", 80.00, "Transporte", "Uber", "01/07/2024"),
                Expense("3", 150.00, "Compras", "Supermercado", "30/06/2024")
            )
        }
    }
}

// Enum para controlar qual tela está sendo exibida
// Isso permite navegar entre a tela principal e a tela de categorias
enum class Screen {
    MAIN,           // Tela principal com lista de gastos
    TOP_CATEGORY    // Tela que mostra a categoria mais gastada
}

// MainActivity é a atividade principal do app - ponto de entrada
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()  // Habilita layout de borda a borda (usa toda a tela)

        // setContent define o conteúdo da interface usando Jetpack Compose
        setContent {
            ControleGastosTheme {  // Aplica o tema personalizado do app
                // Scaffold fornece a estrutura básica de layout do Material Design
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Chama o componente principal da aplicação
                    ExpenseApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Componente principal que gerencia a navegação entre telas
@Composable
fun ExpenseApp(modifier: Modifier = Modifier) {
    // Estado que controla qual tela está sendo exibida
    // remember garante que o estado persista durante recomposições
    var currentScreen by remember { mutableStateOf(Screen.MAIN) }

    // Decide qual tela mostrar baseado no estado atual
    when (currentScreen) {
        Screen.MAIN -> {
            // Mostra a tela principal e passa uma função para navegar para a tela de categoria
            ExpenseList(
                modifier = modifier,
                onNavigateToTopCategory = { currentScreen = Screen.TOP_CATEGORY }
            )
        }
        Screen.TOP_CATEGORY -> {
            // Mostra a tela de categoria mais gastada e passa uma função para voltar
            TopCategoryScreen(
                modifier = modifier,
                onNavigateBack = { currentScreen = Screen.MAIN }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
// Tela principal que exibe a lista de gastos e permite adicionar novos
@Composable
fun ExpenseList(
    modifier: Modifier = Modifier,
    onNavigateToTopCategory: () -> Unit  // Função callback para navegar para a tela de categoria
) {
    val context = LocalContext.current
    val repository = remember { ExpenseRepository(context) }

    // Estados para controlar a interface do usuário
    var showAddDialog by remember { mutableStateOf(false) }  // Se o diálogo de adicionar está visível
    var novoValor by remember { mutableStateOf("") }         // Valor digitado pelo usuário
    var novaCategoria by remember { mutableStateOf("") }     // Categoria selecionada
    var novaDescricao by remember { mutableStateOf("") }     // Descrição digitada

    // Lista de categorias disponíveis para seleção
    val categorias = listOf("Alimentação", "Transporte", "Compras", "Lazer", "Saúde", "Outros")
    var categoriaExpandida by remember { mutableStateOf(false) }  // Se o dropdown está aberto

    // Lista reativa de gastos - mutableStateListOf garante que mudanças na lista
    // automaticamente disparem recomposições da UI
    val gastos = remember { mutableStateListOf<Expense>() }

    // LaunchedEffect executa código apenas uma vez quando o componente é criado
    // Usado aqui para carregar os dados salvos quando a tela é iniciada
    LaunchedEffect(Unit) {
        val savedExpenses = repository.loadExpenses()
        gastos.clear()          // Limpa a lista atual
        gastos.addAll(savedExpenses)  // Adiciona todos os gastos carregados
    }

    // Função local para salvar os gastos no SharedPreferences
    // É chamada sempre que a lista é modificada (adicionar ou remover)
    fun saveExpenses() {
        repository.saveExpenses(gastos.toList())
    }

    // Layout principal da tela organizado em coluna vertical
    Column(modifier = modifier.padding(16.dp)) {
        // Card do cabeçalho com informações resumidas
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Título do app
                Text(
                    text = "💰 Controle de Gastos",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                // Total de gastos - calcula a soma de todos os valores
                Text(
                    text = "Total: R$ ${"%.2f".format(gastos.sumOf { it.valor })}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                // Contador de gastos registrados
                Text(
                    text = "${gastos.size} gastos registrados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botão para adicionar novo gasto
        Button(
            onClick = { showAddDialog = true },  // Abre o diálogo de adicionar
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Adicionar gasto")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Adicionar Novo Gasto")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // NOVO: Botão para navegar para a tela de categoria mais gastada
        Button(
            onClick = onNavigateToTopCategory,  // Chama a função de navegação passada como parâmetro
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.Info, contentDescription = "Ver categoria mais gastada")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Veja qual é a sua categoria!")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista rolável de gastos usando LazyColumn (similar ao RecyclerView)
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)  // Espaçamento entre itens
        ) {
            // items() cria um item da lista para cada gasto
            items(gastos) { gasto ->
                // Card individual para cada gasto
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    // Layout horizontal com informações do gasto e botão de deletar
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Coluna com as informações do gasto (lado esquerdo)
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
                        // Coluna com valor e botão de deletar (lado direito)
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "R$ ${"%.2f".format(gasto.valor)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            IconButton(
                                onClick = {
                                    gastos.remove(gasto)  // Remove o gasto da lista
                                    saveExpenses()        // Salva a lista atualizada
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

    // Diálogo modal para adicionar novo gasto
    // Só é exibido quando showAddDialog é true
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },  // Fecha ao clicar fora
            title = { Text("Adicionar Gasto") },
            text = {
                // Conteúdo do diálogo com campos de entrada
                Column {
                    // Campo para digitar o valor
                    OutlinedTextField(
                        value = novoValor,
                        onValueChange = { novoValor = it },
                        label = { Text("Valor (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Dropdown para selecionar categoria
                    ExposedDropdownMenuBox(
                        expanded = categoriaExpandida,
                        onExpandedChange = { categoriaExpandida = !categoriaExpandida }
                    ) {
                        OutlinedTextField(
                            value = novaCategoria,
                            onValueChange = { },
                            readOnly = true,  // Campo somente leitura (só funciona via dropdown)
                            label = { Text("Categoria") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoriaExpandida) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        // Menu dropdown com as opções de categoria
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

                    // Campo para digitar a descrição
                    OutlinedTextField(
                        value = novaDescricao,
                        onValueChange = { novaDescricao = it },
                        label = { Text("Descrição") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            // Botão de confirmação para salvar o novo gasto
            confirmButton = {
                TextButton(
                    onClick = {
                        // Valida e processa os dados digitados
                        val valor = novoValor.replace(",", ".").toDoubleOrNull()

                        // Verifica se todos os campos estão preenchidos corretamente
                        if (valor != null && valor > 0 && novaCategoria.isNotEmpty() && novaDescricao.isNotEmpty()) {
                            // Cria novo gasto e adiciona à lista
                            gastos.add(
                                Expense(
                                    id = UUID.randomUUID().toString(),  // Gera ID único
                                    valor = valor,
                                    categoria = novaCategoria,
                                    descricao = novaDescricao,
                                    data = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                                )
                            )
                            saveExpenses()  // Salva a lista atualizada

                            // Limpa os campos do formulário
                            novoValor = ""
                            novaCategoria = ""
                            novaDescricao = ""
                            showAddDialog = false  // Fecha o diálogo
                        }
                    }
                ) {
                    Text("Adicionar")
                }
            },
            // Botão para cancelar e fechar o diálogo
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// NOVA TELA: Componente que exibe a categoria onde mais foi gasto dinheiro
@Composable
fun TopCategoryScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit  // Função callback para voltar à tela principal
) {
    val context = LocalContext.current
    val repository = remember { ExpenseRepository(context) }

    // Estado para armazenar a lista de gastos carregados
    var gastos by remember { mutableStateOf<List<Expense>>(emptyList()) }

    // Carrega os gastos quando a tela é criada
    LaunchedEffect(Unit) {
        gastos = repository.loadExpenses()
    }

    // Calcula qual categoria teve maior gasto total
    // groupBy agrupa os gastos por categoria, sumOf soma os valores de cada grupo
    val topCategory = gastos
        .groupBy { it.categoria }  // Agrupa gastos pela categoria
        .mapValues { (_, expenses) -> expenses.sumOf { it.valor } }  // Soma valores por categoria
        .maxByOrNull { it.value }  // Encontra a categoria com maior valor total

    // Layout principal da tela
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,  // Centraliza horizontalmente
        verticalArrangement = Arrangement.Center             // Centraliza verticalmente
    ) {
        // Card principal com o resultado
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)  // Sombra mais pronunciada
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Título da tela
                Text(
                    text = "📊 Análise de Gastos",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Texto explicativo
                Text(
                    text = "A categoria que você mais gastou dinheiro foi:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Exibe a categoria ou mensagem de erro
                if (topCategory != null) {
                    // Nome da categoria em destaque (fonte grande)
                    Text(
                        text = topCategory.key.uppercase(),  // Categoria em maiúsculas
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        fontSize = 48.sp  // Fonte bem grande para destaque
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Valor total gasto nesta categoria
                    Text(
                        text = "Total: R$ ${"%.2f".format(topCategory.value)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error,  // Cor vermelha para destacar o valor
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    // Mensagem quando não há gastos registrados
                    Text(
                        text = "Nenhum gasto registrado ainda",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Botão para voltar à tela principal
        Button(
            onClick = onNavigateBack,  // Chama a função de navegação passada como parâmetro
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Voltar")
        }
    }
}