package com.aura.finance

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.compose.ui.tooling.preview.Preview




object Aura {
    val Obsidian = Color(0xFF0D0E12)
    val Surface = Color(0xFF16181F)
    val Emerald = Color(0xFF00E676)
    val Loss = Color(0xFFFF5252)
    val Muted = Color(0xFF9AA0AE)
    val Border = Brush.linearGradient(listOf(Emerald.copy(alpha = .55f), Color.White.copy(alpha = .08f)))
    val Palette = listOf(Emerald, Color(0xFF40C4FF), Color(0xFFFFD740), Color(0xFFB388FF), Color(0xFFFF8A65))
}

private val AuraDark = darkColorScheme(
    primary = Aura.Emerald, onPrimary = Aura.Obsidian,
    background = Aura.Obsidian, onBackground = Color.White,
    surface = Aura.Surface, onSurface = Color.White,
    surfaceVariant = Color(0xFF1E212B), onSurfaceVariant = Aura.Muted,
    error = Aura.Loss
)

@Composable
fun AuraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AuraDark, content = content) // dark-first; swap for dynamicDarkColorScheme on API 31+
}



data class Slice(val label: String, val value: Float, val color: Color)
data class Candle(val open: Float, val high: Float, val low: Float, val close: Float)

@Composable
fun AllocationPie(slices: List<Slice>, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(1f, tween(900, easing = FastOutSlowInEasing), label = "pie")
    val total = slices.sumOf { it.value.toDouble() }.toFloat()
    val desc = slices.joinToString { "${it.label} ${(it.value / total * 100).toInt()} percent" }
    Row(modifier.semantics { contentDescription = "Asset allocation: $desc" }, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(132.dp)) {
            var start = -90f
            slices.forEach {
                val sweep = it.value / total * 360f * progress
                drawArc(it.color, start, sweep - 2f, false, style = Stroke(26.dp.toPx(), cap = StrokeCap.Butt))
                start += sweep
            }
        }
        Spacer(Modifier.width(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            slices.forEach {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(10.dp)) { drawCircle(it.color) }
                    Spacer(Modifier.width(8.dp))
                    Text("${it.label}  ${(it.value / total * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun RunwayLine(points: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(120.dp).semantics { contentDescription = "Cash runway projection line chart" }) {
        val max = points.max(); val min = points.min()
        val step = size.width / (points.size - 1)
        val path = Path()
        points.forEachIndexed { i, v ->
            val p = Offset(i * step, size.height - (v - min) / (max - min) * size.height)
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        val fill = Path().apply { addPath(path); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
        drawPath(fill, Brush.verticalGradient(listOf(Aura.Emerald.copy(alpha = .28f), Color.Transparent)))
        drawPath(path, Aura.Emerald, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun CandlestickChart(candles: List<Candle>, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(200.dp).semantics { contentDescription = "Candlestick chart, ${candles.size} sessions" }) {
        val hi = candles.maxOf { it.high }; val lo = candles.minOf { it.low }
        fun y(v: Float) = size.height - (v - lo) / (hi - lo) * size.height
        val slot = size.width / candles.size
        for (g in 0..3) drawLine(Color.White.copy(alpha = .06f), Offset(0f, size.height * g / 3), Offset(size.width, size.height * g / 3))
        candles.forEachIndexed { i, c ->
            val color = if (c.close >= c.open) Aura.Emerald else Aura.Loss
            val cx = slot * i + slot / 2
            drawLine(color, Offset(cx, y(c.high)), Offset(cx, y(c.low)), 2.dp.toPx())
            val top = y(maxOf(c.open, c.close)); val bottom = y(minOf(c.open, c.close))
            drawRoundRect(color, Offset(cx - slot * .3f, top), Size(slot * .6f, (bottom - top).coerceAtLeast(2f)), CornerRadius(2.dp.toPx()))
        }
    }
}


/** Single source of truth: 7 modules, 50 screens. Add a screen = add one line. */
enum class Module(val title: String) {
    AUTH("Authentication"), DASH("Dashboard"), PORTFOLIO("Portfolio"),
    BUDGET("Budget & AI"), TAX("Tax Vault"), SHARED("Shared Wealth"), DEV("Developer")
}

enum class Screen(val module: Module, val title: String) {
    // 1 Auth (6)
    BiometricOverlay(Module.AUTH, "Biometric Unlock"), BrokerSync(Module.AUTH, "Broker API Sync"),
    RiskIntro(Module.AUTH, "Risk Wizard · Intro"), RiskQuestions(Module.AUTH, "Risk Wizard · Questions"),
    Login(Module.AUTH, "Credentials Login"), OnboardingDone(Module.AUTH, "Onboarding Success"),
    // 2 Dashboard (8)
    NetWorth(Module.DASH, "Net Worth"), Allocation(Module.DASH, "Asset Allocation"), Runway(Module.DASH, "Cash-Flow Runway"),
    Transactions(Module.DASH, "Smart Transaction Log"), TxDetail(Module.DASH, "Transaction Detail"),
    Currencies(Module.DASH, "Multi-Currency"), Vault(Module.DASH, "Private Vault"), Insights(Module.DASH, "Insights Feed"),
    // 3 Portfolio (9)
    Equities(Module.PORTFOLIO, "Equities"), Candles(Module.PORTFOLIO, "Candlestick Detail"), CostBasis(Module.PORTFOLIO, "Cost Basis"),
    Crypto(Module.PORTFOLIO, "Crypto"), ColdWallet(Module.PORTFOLIO, "Cold Storage"), RealEstate(Module.PORTFOLIO, "Real Estate Equity"),
    Tokenized(Module.PORTFOLIO, "Tokenized Assets"), Rebalance(Module.PORTFOLIO, "Rebalancing"), Watchlist(Module.PORTFOLIO, "Watchlist"),
    // 4 Budget (8)
    ZeroBased(Module.BUDGET, "Zero-Based Matrix"), BucketEdit(Module.BUDGET, "Bucket Editor"), Subscriptions(Module.BUDGET, "Subscriptions"),
    UpcomingBills(Module.BUDGET, "Predicted Bills"), Splurge(Module.BUDGET, "Splurge Safety"), Milestones(Module.BUDGET, "Milestones"),
    Fire(Module.BUDGET, "FIRE Calculator"), Forecast(Module.BUDGET, "Forecast"),
    // 5 Tax (7)
    TaxEstimate(Module.TAX, "Tax Liability"), CapGains(Module.TAX, "Capital Gains"), Scanner(Module.TAX, "Receipt Scanner"),
    OcrPreview(Module.TAX, "OCR Preview"), Deductions(Module.TAX, "Deductible Expenses"), ExportHub(Module.TAX, "Export Hub"),
    ExportHistory(Module.TAX, "Export History"),
    // 6 Shared (6)
    Household(Module.SHARED, "Household Splits"), SplitDetail(Module.SHARED, "Split Detail"), Iou(Module.SHARED, "IOU Manager"),
    IouInterest(Module.SHARED, "Interest Calculator"), Allowance(Module.SHARED, "Allowance Board"), ChildDetail(Module.SHARED, "Dependent Detail"),
    // 7 Developer (6)
    Settings(Module.DEV, "Settings"), Profile(Module.DEV, "Profile"), ThemeEngine(Module.DEV, "Theme Engine"),
    MockSandbox(Module.DEV, "Mock API Sandbox"), Readme(Module.DEV, "README Sync"), About(Module.DEV, "About")
}



@Composable
fun AuraCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Surface(modifier.fillMaxWidth(), RoundedCornerShape(16.dp), Aura.Surface, border = BorderStroke(1.dp, Aura.Border)) {
        Column(Modifier.padding(16.dp), content = content)
    }

@Composable
fun Delta(pct: Double) {
    val up = pct >= 0; val c = if (up) Aura.Emerald else Aura.Loss
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(if (up) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, if (up) "Up" else "Down", tint = c)
        Text("%.2f%%".format(kotlin.math.abs(pct)), color = c, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun DashboardScreen() {
    var hidden by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
        item {
            AuraCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Net worth", Modifier.weight(1f), color = Aura.Muted)
                    IconButton({ hidden = !hidden }) { Icon(if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (hidden) "Show balance" else "Hide balance") }
                }
                Text(if (hidden) "₹ ••••••••" else "₹ 1,84,52,300", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) { Delta(3.42); Text("this month", color = Aura.Muted, style = MaterialTheme.typography.bodySmall) }
            }
        }
        item {
            AuraCard {
                Text("Allocation", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(12.dp))
                AllocationPie(listOf(Slice("Equities", 42f, Aura.Palette[0]), Slice("Crypto", 18f, Aura.Palette[1]),
                    Slice("Real estate", 25f, Aura.Palette[2]), Slice("Cash", 15f, Aura.Palette[3])))
            }
        }
        item {
            AuraCard {
                Text("Cash runway", style = MaterialTheme.typography.titleMedium)
                Text("212 days", style = MaterialTheme.typography.headlineSmall, color = Aura.Emerald, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                RunwayLine(listOf(9f, 8.6f, 8.9f, 7.8f, 7.2f, 7.5f, 6.4f, 5.9f, 5.1f, 4.8f))
            }
        }
        item {
            AuraCard {
                Text("Recent activity", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp))
                listOf(Triple("Swiggy · Food", "-₹ 642", "Settled"), Triple("Salary · Acme Corp", "+₹ 2,40,000", "Settled"), Triple("Zerodha SIP", "-₹ 25,000", "Pending"))
                    .forEach { (n, a, s) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(n); SuggestionChip({}, { Text(s) }) }
                            Text(a, color = if (a.startsWith("+")) Aura.Emerald else Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
            }
        }
    }
}

@Composable
fun EquitiesScreen() {
    val candles = remember { listOf(
        Candle(100f, 108f, 98f, 106f), Candle(106f, 111f, 104f, 109f), Candle(109f, 110f, 101f, 103f), Candle(103f, 107f, 100f, 106f),
        Candle(106f, 115f, 105f, 114f), Candle(114f, 118f, 112f, 113f), Candle(113f, 121f, 112f, 120f), Candle(120f, 124f, 117f, 118f),
        Candle(118f, 126f, 116f, 125f), Candle(125f, 130f, 123f, 129f)) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
        item {
            AuraCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("RELIANCE", fontWeight = FontWeight.Bold); Text("NSE · ₹ 2,945.10", color = Aura.Muted) }
                    Delta(5.18)
                }
                Spacer(Modifier.height(12.dp)); CandlestickChart(candles)
                Spacer(Modifier.height(12.dp))
                Row { Text("Cost basis ₹ 2,610", Modifier.weight(1f), color = Aura.Muted); Text("P&L +₹ 33,510", color = Aura.Emerald) }
            }
        }
    }
}



private data class Tab(val module: Module, val icon: ImageVector)
private val tabs = listOf(
    Tab(Module.DASH, Icons.Default.Dashboard), Tab(Module.PORTFOLIO, Icons.Default.ShowChart),
    Tab(Module.BUDGET, Icons.Default.AutoAwesome), Tab(Module.TAX, Icons.Default.ReceiptLong),
    Tab(Module.SHARED, Icons.Default.Groups)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraApp(nav: NavHostController = rememberNavController()) = AuraTheme {
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val current = Screen.entries.firstOrNull { it.name == route }
    val onHub = route?.startsWith("hub/") == true || route == null
    Scaffold(
        containerColor = Aura.Obsidian,
        topBar = {
            TopAppBar(
                title = { Text(current?.title ?: "AuraFinance", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { if (current != null) IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { IconButton({ nav.navigate("hub/${Module.DEV.name}") }) { Icon(Icons.Default.Settings, "Developer settings") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Aura.Obsidian, titleContentColor = androidx.compose.ui.graphics.Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Aura.Surface) {
                tabs.forEach { t ->
                    val selected = route == "hub/${t.module.name}" || current?.module == t.module
                    NavigationBarItem(selected, { nav.navigate("hub/${t.module.name}") { popUpTo("hub/DASH"); launchSingleTop = true } },
                        icon = { Icon(t.icon, null) }, label = { Text(t.module.title.substringBefore(' '), maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Aura.Emerald.copy(alpha = .18f), selectedIconColor = Aura.Emerald))
                }
            }
        }
    ) { pad ->
        NavHost(nav, "hub/DASH", Modifier.padding(pad),
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 12 } },
            exitTransition = { fadeOut(tween(140)) }) {
            composable("hub/{m}") { b -> ModuleHub(Module.valueOf(b.arguments!!.getString("m")!!)) { nav.navigate(it.name) } }
            Screen.entries.forEach { s -> composable(s.name) { ScreenHost(s) } }
        }
    }
}

/** Route each Screen to its implementation; unfinished ones fall back to a titled shell. */
@Composable
private fun ScreenHost(s: Screen) = when (s) {
    Screen.NetWorth -> DashboardScreen()
    Screen.Candles, Screen.Equities -> EquitiesScreen()
    else -> ScreenShell(s)
}

@Composable
private fun ModuleHub(module: Module, open: (Screen) -> Unit) {
    val items = Screen.entries.filter { it.module == module }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
        item { Text(module.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        items(items) { s ->
            Surface(Modifier.fillMaxWidth().clickable { open(s) }, RoundedCornerShape(16.dp), Aura.Surface, border = BorderStroke(1.dp, Aura.Border)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Icon(Icons.Default.ChevronRight, null, tint = Aura.Muted)
                }
            }
        }
    }
}

@Composable
private fun ScreenShell(s: Screen) = Box(Modifier.fillMaxSize().padding(16.dp), Alignment.Center) {
    Text("${s.title}\n${s.module.title} module", color = Aura.Muted, style = MaterialTheme.typography.titleMedium)
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0E12, widthDp = 390, heightDp = 844)
@Composable
fun AuraPreview() = AuraApp()