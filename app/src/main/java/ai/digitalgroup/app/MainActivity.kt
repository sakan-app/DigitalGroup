package ai.digitalgroup.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AColor
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import org.json.JSONArray
import org.json.JSONObject

private const val PARENT_URL = "https://www.digital-future.ai"
private val gold = Color(0xFFD4AF37)
private val black = Color(0xFF0A0A0A)
private val surface = Color(0xFF1B1B1B)
private data class Store(val id: String, val label: String, val url: String)
private val stores = listOf(
    Store("sakan", "SAKAN", "https://www.sakanapp.net"),
    Store("nexora", "NEXORA", "https://www.nexoraonline.de"),
    Store("helpmey", "HELPMEY", "https://www.helpmey.net")
)
private data class Department(val name: String, val ar: String, val icon: String)
private val departments = listOf(
    Department("Sneakers", "أحذية رياضية", "👟"), Department("Smartphones", "هواتف ذكية", "📱"),
    Department("Computers", "كمبيوتر", "💻"), Department("Bluetooth Devices", "بلوتوث", "🎧"),
    Department("Women's Clothing", "ملابس حريمي", "👗"), Department("Innerwear", "داخلية", "◈"),
    Department("Evening Wear", "سهرة", "✨"), Department("Men's Suits", "بدل رجالي", "🤵"),
    Department("Luxury Perfumes", "عطور فاخرة", "❖"), Department("Watches", "ساعات", "⌚"),
    Department("Women's Bags", "شنط حريمي", "👜"), Department("Sportswear", "ملابس رياضية", "🏃")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DigitalGroupRoot() }
    }
}

private class CartPersistence(context: Context) {
    private val prefs = context.getSharedPreferences("digitalgroup_cart_v1", Context.MODE_PRIVATE)
    fun read(): JSONArray = try { JSONArray(prefs.getString("cart", "[]")) } catch (_: Exception) { JSONArray() }
    fun write(value: JSONArray) { prefs.edit().putString("cart", value.toString()).apply() }
}

@Composable
private fun DigitalGroupRoot() {
    val context = LocalContext.current
    val persistence = remember { CartPersistence(context) }
    var cart by remember { mutableStateOf(persistence.read()) }
    var store by remember { mutableStateOf(stores.first()) }
    var department by remember { mutableStateOf(departments.first()) }
    var page by remember { mutableStateOf("hub") }
    var size by remember { mutableStateOf("M") }
    var color by remember { mutableStateOf("Black") }
    var quantity by remember { mutableIntStateOf(1) }
    var locale by remember { mutableStateOf(deviceLanguage(context)) }
    var webUrl by remember { mutableStateOf<String?>(null) }
    val tr: (String, String, String) -> String = { de, en, ar -> when(locale) { "en" -> en; "ar" -> ar; else -> de } }
    val cartCount = remember(cart.toString()) { (0 until cart.length()).sumOf { cart.optJSONObject(it)?.optInt("qty", 0) ?: 0 } }

    MaterialTheme(colorScheme = darkColorScheme(primary = gold, background = black, surface = surface,
        onBackground = Color.White, onSurface = Color.White, onPrimary = black)) {
        Scaffold(containerColor = black, topBar = {
            Column(Modifier.background(black)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.clickable { page = "hub"; webUrl = null }) {
                        Text("DIGITALGROUP", color = gold, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("DIGITAL FUTURE", color = Color.LightGray, fontSize = 9.sp, letterSpacing = 3.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        QrCode(PARENT_URL)
                        IconButton(onClick = { page = "cart"; webUrl = null }) {
                            BadgedBox(badge = { if (cartCount > 0) Badge { Text(cartCount.toString()) } }) {
                                Icon(Icons.Default.ShoppingBag, contentDescription = "Cart", tint = gold)
                            }
                        }
                        TextButton(onClick = { locale = when(locale) { "de" -> "en"; "en" -> "ar"; else -> "de" } }) {
                            Text(locale.uppercase(), color = gold)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    stores.forEach { item ->
                        FilterChip(selected = store.id == item.id, onClick = { store = item; page = "hub"; webUrl = null },
                            label = { Text(item.label) })
                    }
                }
                HorizontalDivider(color = gold.copy(alpha = .45f))
            }
        }) { insets ->
            Column(Modifier.fillMaxSize().padding(insets)) {
                if (webUrl != null) {
                    StoreBrowser(webUrl!!, Modifier.weight(1f))
                    TextButton(onClick = { webUrl = null }) { Text(tr("Zurück","Back","عودة"), color = gold) }
                } else when(page) {
                    "product" -> ProductPage(store, department, size, { size = it }, color, { color = it },
                        quantity, { quantity = it }, { page = "summary" }, tr)
                    "summary" -> SummaryPage(store, department, size, color, quantity,
                        { page = "checkout" }, { page = "product" }, tr)
                    "checkout" -> CheckoutPage(store, { webUrl = store.url }, tr)
                    "cart" -> CartPage(cart, { cart = JSONArray(); persistence.write(cart) }, tr)
                    else -> HubPage(store, department, { department = it }, tr, { page = "product" },
                        { webUrl = store.url }, { dep ->
                            cart.put(JSONObject().put("store", store.id).put("department", dep.name)
                                .put("size", "M").put("color", "Black").put("qty", 1))
                            persistence.write(cart)
                        })
                }
            }
        }
    }
}

private fun deviceLanguage(context: Context): String = when (context.resources.configuration.locales[0].language.lowercase()) {
    "en" -> "en"
    "ar" -> "ar"
    else -> "de"
}

@Composable
private fun QrCode(value: String) {
    val bitmap = remember(value) {
        try {
            val matrix = MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, 144, 144)
            Bitmap.createBitmap(144, 144, Bitmap.Config.ARGB_8888).also { image ->
                for (x in 0 until 144) for (y in 0 until 144)
                    image.setPixel(x, y, if (matrix[x, y]) AColor.BLACK else AColor.WHITE)
            }
        } catch (_: Exception) { null }
    }
    bitmap?.let { Image(it.asImageBitmap(), "Digital Future QR code",
        Modifier.size(42.dp).background(Color.White, RoundedCornerShape(4.dp)).padding(2.dp)) }
}

@Composable
private fun HubPage(store: Store, active: Department, choose: (Department) -> Unit,
                    tr: (String, String, String) -> String, openProduct: () -> Unit,
                    openStore: () -> Unit, addToCart: (Department) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
        Text(tr("Willkommen bei","Welcome to","مرحباً بك في"), color = Color.LightGray)
        Text(store.label, color = gold, fontSize = 29.sp, letterSpacing = 3.sp)
        OutlinedButton(onClick = openStore, border = BorderStroke(1.dp, gold)) {
            Text(tr("Offiziellen Shop öffnen","Open official store","افتح المتجر الرسمي"), color = gold)
        }
        Spacer(Modifier.height(12.dp))
        Text(tr("KOLLEKTIONEN","COLLECTIONS","الأقسام"), color = gold, fontWeight = FontWeight.Bold)
        departments.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { dep ->
                    Card(Modifier.weight(1f).padding(vertical = 3.dp).clickable { choose(dep) },
                        colors = CardDefaults.cardColors(containerColor = if (dep == active) gold.copy(alpha = .16f) else surface),
                        border = BorderStroke(1.dp, if (dep == active) gold else gold.copy(alpha = .35f))) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(dep.icon, fontSize = 21.sp)
                            Text(dep.name.take(12), color = Color.White, fontSize = 9.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(active.name.uppercase(), color = gold, fontWeight = FontWeight.Bold)
            Text("20 ITEMS", color = Color.LightGray, fontSize = 10.sp)
        }
        repeat(20) { index ->
            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { openProduct() },
                colors = CardDefaults.cardColors(containerColor = surface),
                border = BorderStroke(1.dp, gold.copy(alpha = .55f))) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(66.dp).background(Color(0xFF292929), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center) { Icon(Icons.Default.Diamond, null, tint = gold, modifier = Modifier.size(28.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(active.name + " · " + (index + 1), color = Color.White)
                        Spacer(Modifier.height(7.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Text("Size: —", color = Color.LightGray, fontSize = 10.sp)
                            Text("Color: —", color = Color.LightGray, fontSize = 10.sp)
                            Text("Price: —", color = gold, fontSize = 10.sp)
                        }
                    }
                    IconButton(onClick = { addToCart(active) }) { Icon(Icons.Default.AddShoppingCart, null, tint = gold) }
                }
            }
        }
    }
}

@Composable
private fun ProductPage(store: Store, dep: Department, size: String, setSize: (String) -> Unit,
                        color: String, setColor: (String) -> Unit, qty: Int, setQty: (Int) -> Unit,
                        proceed: () -> Unit, tr: (String, String, String) -> String) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text(dep.name.uppercase(), color = gold, letterSpacing = 2.sp)
        Card(Modifier.fillMaxWidth().height(180.dp).padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = surface), border = BorderStroke(1.dp, gold)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Diamond, null, tint = gold, modifier = Modifier.size(64.dp))
            }
        }
        Text(tr("Produktdetails","Product details","تفاصيل المنتج"), color = Color.White, fontSize = 22.sp)
        Text(store.label + " · " + dep.name, color = Color.LightGray)
        Spacer(Modifier.height(12.dp))
        Text(tr("Preis","Price","السعر") + ": " + tr("wird im offiziellen Shop bestätigt","confirmed on official store","يؤكد في المتجر الرسمي"), color = gold)
        Text(tr("Farbe","Color","اللون"), color = gold)
        Row { listOf("Black","White","Gold").forEach { FilterChip(selected = color == it, onClick = { setColor(it) }, label = { Text(it) }) } }
        Text(tr("Größe","Size","المقاس"), color = gold)
        Row { listOf("S","M","L","XL").forEach { FilterChip(selected = size == it, onClick = { setSize(it) }, label = { Text(it) }) } }
        Text(tr("Menge","Quantity","الكمية"), color = gold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { setQty((qty - 1).coerceAtLeast(1)) }) { Icon(Icons.Default.Remove, null, tint = gold) }
            Text(qty.toString(), color = Color.White)
            IconButton(onClick = { setQty((qty + 1).coerceAtMost(99)) }) { Icon(Icons.Default.Add, null, tint = gold) }
        }
        Button(onClick = proceed, modifier = Modifier.fillMaxWidth()) { Text(tr("Weiter","Continue","متابعة")) }
    }
}

@Composable
private fun SummaryPage(store: Store, dep: Department, size: String, color: String, qty: Int,
                        confirm: () -> Unit, back: () -> Unit, tr: (String, String, String) -> String) {
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.Center) {
        Text(tr("Bestellübersicht","Order summary","ملخص الطلب"), color = gold, fontSize = 26.sp)
        Card(Modifier.fillMaxWidth().padding(vertical = 16.dp), colors = CardDefaults.cardColors(containerColor = surface),
            border = BorderStroke(1.dp, gold)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(store.label, color = gold); Text(dep.name, color = Color.White, fontSize = 20.sp)
                Text(tr("Menge","Quantity","الكمية") + ": " + qty, color = Color.White)
                Text(tr("Artikelanzahl","Item count","عدد العناصر") + ": " + qty, color = Color.White)
                Text(tr("Größe","Size","المقاس") + ": " + size, color = Color.White)
                Text(tr("Farbe","Color","اللون") + ": " + color, color = Color.White)
            }
        }
        Button(onClick = confirm, modifier = Modifier.fillMaxWidth()) { Text(tr("Bestätigen & Checkout","Confirm & checkout","تأكيد والدفع")) }
        TextButton(onClick = back) { Text(tr("Zurück","Back","عودة"), color = gold) }
    }
}

@Composable
private fun CheckoutPage(store: Store, openOfficialCheckout: () -> Unit, tr: (String, String, String) -> String) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Lock, null, tint = gold, modifier = Modifier.size(46.dp))
        Text(tr("Sicherer Checkout","Secure checkout","دفع آمن"), color = gold, fontSize = 25.sp)
        Text(tr("Die Zahlung wird auf der offiziellen Shop-Domain abgeschlossen.",
            "Payment is completed on the official store domain.","يتم الدفع عبر نطاق المتجر الرسمي."),
            color = Color.White, modifier = Modifier.padding(vertical = 16.dp))
        Button(onClick = openOfficialCheckout) { Text(tr("Zum offiziellen Checkout","Continue to official checkout","المتابعة للدفع")) }
    }
}

@Composable
private fun CartPage(cart: JSONArray, clear: () -> Unit, tr: (String, String, String) -> String) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text(tr("Warenkorb","Shopping cart","سلة التسوق"), color = gold, fontSize = 25.sp)
        if (cart.length() == 0) Text(tr("Ihr Warenkorb ist leer.","Your cart is empty.","سلة التسوق فارغة"), color = Color.White)
        else {
            repeat(cart.length()) { i ->
                val item = cart.optJSONObject(i) ?: JSONObject()
                Text(item.optString("store") + " · " + item.optString("department") + " × " + item.optInt("qty"),
                    color = Color.White, modifier = Modifier.padding(vertical = 8.dp))
            }
            TextButton(onClick = clear) { Text(tr("Warenkorb leeren","Clear cart","إفراغ السلة"), color = gold) }
        }
    }
}

@Composable
private fun StoreBrowser(url: String, modifier: Modifier = Modifier) {
    AndroidView(modifier = modifier, factory = { context ->
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val host = request.url.host?.lowercase() ?: return true
                    val allowed = setOf("digital-future.ai", "www.digital-future.ai", "sakanapp.net", "www.sakanapp.net",
                        "nexoraonline.de", "www.nexoraonline.de", "helpmey.net", "www.helpmey.net")
                    if (host in allowed) return false
                    view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(request.url.toString())))
                    return true
                }
            }
            loadUrl(url)
        }
    }, update = { view -> if (view.url != url) view.loadUrl(url) })
}
