@file:OptIn(ExperimentalMaterial3Api::class)

package com.karmagbyte.tshongla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Wine = Color(0xFF8F3035)
private val Gold = Color(0xFFE4A93B)
private val Cream = Color(0xFFFFF9F0)
private val Ink = Color(0xFF2C2523)
private val Sage = Color(0xFF59735C)
private val SoftRose = Color(0xFFF7E8E6)

private val dzongkhags = listOf(
    "Bumthang", "Chhukha", "Dagana", "Gasa", "Haa", "Lhuentse", "Mongar", "Paro", "Pemagatshel", "Punakha",
    "Samdrup Jongkhar", "Samtse", "Sarpang", "Thimphu", "Trashigang", "Trashiyangtse", "Trongsa", "Tsirang", "Wangdue Phodrang", "Zhemgang"
)

data class Product(
    val id: Int,
    val name: String,
    val category: String,
    val price: Int,
    val rating: Double,
    val seller: String,
    val dzongkhag: String,
    val address: String,
    val stock: Int,
    val discountPercent: Int,
    val symbol: String,
    val colors: List<Color>,
    val description: String
) {
    val discountedPrice: Int
        get() = if (discountPercent <= 0) price else (price * (100 - discountPercent)) / 100
}

data class Booking(
    val product: Product,
    val quantity: Int,
    val pickupCode: String,
    val status: String = "Reserved"
)

private val sampleProducts = listOf(
    Product(1, "Handwoven Yathra Bag", "Textiles", 1250, 4.9, "Pem's Handcraft", "Bumthang", "Chamkhar Town, near main market", 8, 10, "👜", listOf(Color(0xFFB95C4B), Color(0xFFE9A64A)), "A colourful handwoven shoulder bag made with traditional Bhutanese yathra patterns."),
    Product(2, "Natural Incense Set", "Wellness", 480, 4.8, "Druk Aromas", "Thimphu", "Norzin Lam, Thimphu", 15, 0, "🌿", listOf(Color(0xFF557A5D), Color(0xFFA7C48C)), "A calming selection of locally prepared incense using aromatic Himalayan herbs."),
    Product(3, "Carved Wooden Bowl", "Home", 890, 4.7, "Zorig Woodworks", "Trashigang", "Trashigang Town, upper market", 5, 5, "🥣", listOf(Color(0xFF8B5A3C), Color(0xFFD69B61)), "A smooth, food-safe wooden bowl individually carved and finished by a local artisan."),
    Product(4, "Bhutanese Woven Scarf", "Textiles", 1600, 4.9, "Weaves of Haa", "Haa", "Haa Town, near bus stand", 6, 0, "🧣", listOf(Color(0xFF7A3F65), Color(0xFFD787A6)), "A soft statement scarf woven by hand with a contemporary interpretation of traditional motifs."),
    Product(5, "Clay Butter Lamp", "Crafts", 350, 4.6, "Deki Pottery", "Paro", "Paro Town, Tshongdue", 20, 15, "🪔", listOf(Color(0xFFAA603E), Color(0xFFF0B36D)), "A handmade clay butter lamp inspired by Bhutanese homes and sacred spaces."),
    Product(6, "Wildflower Honey", "Food", 650, 4.8, "Mountain Harvest", "Trongsa", "Trongsa Town, below dzong road", 10, 0, "🍯", listOf(Color(0xFFE09A24), Color(0xFFF7D46B)), "Pure Bhutanese wildflower honey collected in small batches from mountain apiaries.")
)

enum class Screen {
    RoleSelect, CustomerLogin, ShopkeeperLogin, Home, Explore, Detail, Bookings,
    BookingSuccess, Shop, SellerReservations, AddProduct, EditProduct
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TshongLaTheme { TshongLaApp() } }
    }
}

@Composable
private fun TshongLaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Wine,
            onPrimary = Color.White,
            secondary = Gold,
            background = Cream,
            surface = Color.White,
            onSurface = Ink
        ),
        content = content
    )
}

@Composable
private fun TshongLaApp() {
    var screen by remember { mutableStateOf(Screen.RoleSelect) }
    val products = remember { mutableStateListOf<Product>() }
    val bookings = remember { mutableStateListOf<Booking>() }
    var selected by remember { mutableStateOf(sampleProducts.first()) }
    var latestBooking by remember { mutableStateOf<Booking?>(null) }
    var dataMessage by remember { mutableStateOf<String?>(null) }
    var loadingProducts by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    DisposableEffect(Unit) {
        TshongLaFirestore.seedProductsIfEmpty(sampleProducts)
        val productListener = TshongLaFirestore.listenToProducts(
            onChange = { latest ->
                products.clear()
                products.addAll(latest)
                loadingProducts = false
                val refreshedSelected = latest.firstOrNull { it.id == selected.id }
                if (refreshedSelected != null) selected = refreshedSelected
            },
            onError = { dataMessage = "Firestore: $it" }
        )
        val bookingListener = TshongLaFirestore.listenToBookings(
            onChange = { latest ->
                bookings.clear()
                bookings.addAll(latest)
            },
            onError = { dataMessage = "Firestore: $it" }
        )
        onDispose {
            productListener.remove()
            bookingListener.remove()
        }
    }

    LaunchedEffect(dataMessage) {
        dataMessage?.let {
            snackbarHostState.showSnackbar(it)
            dataMessage = null
        }
    }

    fun openProduct(product: Product) {
        selected = product
        screen = Screen.Detail
    }

    Scaffold(
        containerColor = Cream,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (screen in listOf(Screen.Home, Screen.Explore, Screen.Bookings)) {
                CustomerBottomBar(screen, bookings.size) { screen = it }
            }
            if (screen in listOf(Screen.Shop, Screen.SellerReservations)) {
                SellerBottomBar(screen) { screen = it }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (screen) {
                Screen.RoleSelect -> RoleSelectionScreen(
                    customer = { screen = Screen.CustomerLogin },
                    shopkeeper = { screen = Screen.ShopkeeperLogin }
                )
                Screen.CustomerLogin -> LoginScreen(
                    title = "Customer login",
                    subtitle = "Discover local handmade products and reserve them for pickup.",
                    icon = Icons.Outlined.Person,
                    back = { screen = Screen.RoleSelect },
                    login = { screen = Screen.Home }
                )
                Screen.ShopkeeperLogin -> LoginScreen(
                    title = "Shopkeeper login",
                    subtitle = "Manage products, prices, stock, discounts and reservations.",
                    icon = Icons.Outlined.Storefront,
                    back = { screen = Screen.RoleSelect },
                    login = { screen = Screen.Shop }
                )
                Screen.Home -> HomeScreen(products, loadingProducts, ::openProduct) { screen = Screen.Explore }
                Screen.Explore -> ExploreScreen(products, loadingProducts, ::openProduct)
                Screen.Detail -> ProductDetailScreen(
                    product = selected,
                    back = { screen = Screen.Home },
                    book = { quantity ->
                        TshongLaFirestore.createBooking(
                            product = selected,
                            quantity = quantity,
                            onSuccess = { booking ->
                                latestBooking = booking
                                screen = Screen.BookingSuccess
                            },
                            onError = { dataMessage = it }
                        )
                    }
                )
                Screen.Bookings -> BookingsScreen(bookings) { screen = Screen.Explore }
                Screen.BookingSuccess -> BookingSuccessScreen(
                    booking = latestBooking,
                    viewBookings = { screen = Screen.Bookings },
                    continueShopping = { screen = Screen.Home }
                )
                Screen.Shop -> ShopScreen(
                    products = products,
                    bookings = bookings,
                    openReservations = { screen = Screen.SellerReservations },
                    addProduct = { screen = Screen.AddProduct },
                    editProduct = { product -> selected = product; screen = Screen.EditProduct },
                    deleteProduct = { product ->
                        TshongLaFirestore.deleteProduct(product, onError = { dataMessage = it })
                    }
                )
                Screen.SellerReservations -> SellerReservationsScreen(
                    bookings = bookings,
                    back = { screen = Screen.Shop },
                    updateStatus = { booking, newStatus ->
                        TshongLaFirestore.updateBookingStatus(
                            booking,
                            newStatus,
                            onError = { dataMessage = it }
                        )
                    }
                )
                Screen.AddProduct -> AddProductScreen(
                    nextId = (products.maxOfOrNull { it.id } ?: 0) + 1,
                    save = { product ->
                        TshongLaFirestore.saveProduct(
                            product,
                            onSuccess = { screen = Screen.Shop },
                            onError = { dataMessage = it }
                        )
                    },
                    back = { screen = Screen.Shop }
                )
                Screen.EditProduct -> EditProductScreen(
                    product = selected,
                    save = { product ->
                        TshongLaFirestore.saveProduct(
                            product,
                            onSuccess = { selected = product; screen = Screen.Shop },
                            onError = { dataMessage = it }
                        )
                    },
                    back = { screen = Screen.Shop }
                )
            }
        }
    }
}

@Composable
private fun RoleSelectionScreen(customer: () -> Unit, shopkeeper: () -> Unit) {
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF3E4), Cream, Color.White)))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(42.dp))
        Surface(shape = CircleShape, color = Wine, modifier = Modifier.size(76.dp), shadowElevation = 4.dp) {
            Box(contentAlignment = Alignment.Center) { Text("T", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold) }
        }
        Spacer(Modifier.height(18.dp))
        Text("TshongLa", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text("Handmade Marketplace", color = Wine, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(34.dp))
        Text("How would you like to continue?", fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("Choose your role to get the right experience.", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(26.dp))
        RoleCard("I'm a Customer", "Browse handmade products and reserve items for personal pickup.", Icons.Outlined.ShoppingBag, customer)
        Spacer(Modifier.height(16.dp))
        RoleCard("I'm a Shopkeeper", "Register products and manage prices, stock, discounts and reservations.", Icons.Outlined.Storefront, shopkeeper)
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CloudDone, null, tint = Sage, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Connected marketplace data", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun RoleCard(title: String, text: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(18.dp), color = SoftRose, modifier = Modifier.size(64.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Wine, modifier = Modifier.size(32.dp)) }
            }
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(text, color = Color.Gray, fontSize = 13.sp, lineHeight = 18.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Wine)
        }
    }
}

@Composable
private fun LoginScreen(title: String, subtitle: String, icon: ImageVector, back: () -> Unit, login: () -> Unit) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Back") }
            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(22.dp), color = SoftRose, modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Wine, modifier = Modifier.size(36.dp)) }
            }
            Spacer(Modifier.height(18.dp))
            Text(title, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = Color.Gray, lineHeight = 21.sp)
        }
        item {
            OutlinedTextField(
                phone, { phone = it; showError = false }, Modifier.fillMaxWidth(),
                label = { Text("Phone number") }, leadingIcon = { Icon(Icons.Outlined.Phone, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(16.dp), singleLine = true
            )
        }
        item {
            OutlinedTextField(
                password, { password = it; showError = false }, Modifier.fillMaxWidth(),
                label = { Text("Password") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(16.dp), singleLine = true
            )
        }
        item {
            Button(
                onClick = { if (phone.isNotBlank() && password.isNotBlank()) login() else showError = true },
                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)
            ) { Text("Login", fontWeight = FontWeight.Bold) }
            if (showError) Text("Please enter your phone number and password.", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(16.dp)) {
                Text("Create an account")
            }
        }
    }
}

@Composable
private fun CustomerBottomBar(current: Screen, bookingCount: Int, navigate: (Screen) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        val destinations = listOf(
            Triple(Screen.Home, Icons.Outlined.Home, "Home"),
            Triple(Screen.Explore, Icons.Outlined.Search, "Explore"),
            Triple(Screen.Bookings, Icons.Outlined.ConfirmationNumber, "Bookings")
        )
        destinations.forEach { (screen, icon, label) ->
            NavigationBarItem(
                selected = current == screen,
                onClick = { navigate(screen) },
                icon = {
                    BadgedBox(badge = { if (screen == Screen.Bookings && bookingCount > 0) Badge { Text(bookingCount.toString()) } }) {
                        Icon(icon, label)
                    }
                },
                label = { Text(label, fontSize = 11.sp) }
            )
        }
    }
}

@Composable
private fun SellerBottomBar(current: Screen, navigate: (Screen) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(current == Screen.Shop, { navigate(Screen.Shop) }, { Icon(Icons.Outlined.Storefront, "Shop") }, label = { Text("My Shop") })
        NavigationBarItem(current == Screen.SellerReservations, { navigate(Screen.SellerReservations) }, { Icon(Icons.Outlined.ReceiptLong, "Reservations") }, label = { Text("Reservations") })
        NavigationBarItem(false, {}, { Icon(Icons.Outlined.Person, "Profile") }, label = { Text("Profile") })
    }
}

@Composable
private fun HomeScreen(products: List<Product>, loading: Boolean, openProduct: (Product) -> Unit, explore: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFFFFF4E2), Cream))).padding(20.dp)) {
                Text("Kuzu zangpo!", color = Wine, fontWeight = FontWeight.SemiBold)
                Text("Find something\nmade with meaning.", fontSize = 29.sp, lineHeight = 33.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Spacer(Modifier.height(18.dp))
                SearchBar(readOnly = true, onClick = explore)
                Spacer(Modifier.height(18.dp))
                HeroBanner(explore)
            }
        }
        item { SectionHeader("Shop by category", "Explore all", explore) }
        item { CategoryRow() }
        item { SectionHeader("Featured for you", "See all", explore) }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) }
        items(products.take(4).chunked(2)) { row ->
            Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { product -> Box(Modifier.weight(1f)) { ProductCard(product) { openProduct(product) } } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun HeroBanner(onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(24.dp), color = Wine) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("LOCAL • HANDMADE • UNIQUE", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("See it. Love it.\nReserve it locally.", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                AssistChip(onClick = onClick, label = { Text("Explore products") })
            }
            Text("🧺", fontSize = 68.sp)
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 22.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) { Text(action) }
    }
}

@Composable
private fun CategoryRow() {
    val categories = listOf("🧵" to "Textiles", "🏺" to "Crafts", "🏠" to "Home", "🍯" to "Food", "🌿" to "Wellness")
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(categories) { (symbol, title) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 2.dp, modifier = Modifier.size(68.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(symbol, fontSize = 30.sp) }
                }
                Spacer(Modifier.height(6.dp))
                Text(title, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SearchBar(readOnly: Boolean = false, onClick: (() -> Unit)? = null, value: String = "", onValueChange: (String) -> Unit = {}) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        enabled = !readOnly,
        readOnly = readOnly,
        placeholder = { Text("Search handmade products") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(disabledContainerColor = Color.White, disabledBorderColor = Color.Transparent)
    )
}

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column {
            ProductArtwork(product, Modifier.fillMaxWidth().height(128.dp), 48)
            Column(Modifier.padding(12.dp)) {
                if (product.discountPercent > 0) Text("${product.discountPercent}% OFF", color = Wine, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(product.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
                Text("★ ${product.rating}", color = Gold, fontSize = 12.sp)
                Text("Nu. ${product.discountedPrice}", color = Wine, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                if (product.discountPercent > 0) Text("Regular Nu. ${product.price}", color = Color.Gray, fontSize = 11.sp)
                Text("📍 ${product.dzongkhag}", color = Color.Gray, fontSize = 11.sp)
                Text(if (product.stock > 0) "${product.stock} available" else "Out of stock", color = if (product.stock > 0) Sage else MaterialTheme.colorScheme.error, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ProductArtwork(product: Product, modifier: Modifier, symbolSize: Int) {
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(product.colors)), contentAlignment = Alignment.Center) {
        Text(product.symbol, fontSize = symbolSize.sp)
    }
}

@Composable
private fun ExploreScreen(products: List<Product>, loading: Boolean, openProduct: (Product) -> Unit) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedDzongkhag by remember { mutableStateOf("All Bhutan") }
    var locationExpanded by remember { mutableStateOf(false) }
    val filtered = products.filter {
        (selectedCategory == "All" || it.category == selectedCategory) &&
            (selectedDzongkhag == "All Bhutan" || it.dzongkhag == selectedDzongkhag) &&
            it.name.contains(query, true)
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Explore products", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold); Text("Browse by category and Dzongkhag.", color = Color.Gray) }
        item { SearchBar(value = query, onValueChange = { query = it }) }
        item {
            ExposedDropdownMenuBox(expanded = locationExpanded, onExpandedChange = { locationExpanded = it }) {
                OutlinedTextField(
                    selectedDzongkhag, {}, readOnly = true,
                    label = { Text("Shop by Dzongkhag") },
                    leadingIcon = { Icon(Icons.Outlined.LocationOn, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(locationExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                ExposedDropdownMenu(locationExpanded, { locationExpanded = false }) {
                    (listOf("All Bhutan") + dzongkhags).forEach { place ->
                        DropdownMenuItem({ Text(place) }, { selectedDzongkhag = place; locationExpanded = false })
                    }
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Textiles", "Crafts", "Home", "Food", "Wellness")) { category ->
                    FilterChip(selectedCategory == category, { selectedCategory = category }, { Text(category) })
                }
            }
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        item { Text("${filtered.size} products available", fontWeight = FontWeight.Bold) }
        items(filtered.chunked(2)) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { product -> Box(Modifier.weight(1f)) { ProductCard(product) { openProduct(product) } } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProductDetailScreen(product: Product, back: () -> Unit, book: (Int) -> Unit) {
    var quantity by remember(product.id) { mutableIntStateOf(1) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 18.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(300.dp)) {
                ProductArtwork(product, Modifier.fillMaxSize().clip(RoundedCornerShape(0.dp)), 96)
                Surface(Modifier.align(Alignment.TopStart).padding(16.dp), shape = CircleShape, color = Color.White) {
                    IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            }
        }
        item {
            Column(Modifier.padding(20.dp)) {
                Text(product.category.uppercase(), color = Wine, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(product.name, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(8.dp))
                Text("★ ${product.rating} • Verified local seller", color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                Text("Nu. ${product.discountedPrice}", color = Wine, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                if (product.discountPercent > 0) Text("Regular price: Nu. ${product.price} • ${product.discountPercent}% off", color = Color.Gray)
                HorizontalDivider(Modifier.padding(vertical = 18.dp))
                Text(product.seller, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("📍 ${product.address}, ${product.dzongkhag}", color = Color.Gray)
                Spacer(Modifier.height(14.dp))
                Text(if (product.stock > 0) "${product.stock} item(s) currently available" else "Currently out of stock", color = if (product.stock > 0) Sage else MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(18.dp))
                Text("About this product", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(product.description, color = Color.DarkGray, lineHeight = 22.sp)
                Spacer(Modifier.height(20.dp))
                Card(colors = CardDefaults.cardColors(containerColor = SoftRose), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Pickup from seller", fontWeight = FontWeight.Bold)
                        Text("Reserve now and collect it from ${product.address}, ${product.dzongkhag}.", color = Color.DarkGray, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("Quantity", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedIconButton({ if (quantity > 1) quantity-- }) { Icon(Icons.Default.Remove, "Decrease") }
                    Text(quantity.toString(), Modifier.padding(horizontal = 22.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    OutlinedIconButton({ if (quantity < product.stock) quantity++ }, enabled = product.stock > 0) { Icon(Icons.Default.Add, "Increase") }
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { book(quantity) }, enabled = product.stock > 0 && quantity <= product.stock,
                    modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(17.dp)
                ) {
                    Text(if (product.stock > 0) "Book for pickup • Nu. ${product.discountedPrice * quantity}" else "Out of stock", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BookingSuccessScreen(booking: Booking?, viewBookings: () -> Unit, continueShopping: () -> Unit) {
    if (booking == null) return
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.CheckCircle, null, tint = Sage, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(18.dp))
        Text("Item reserved!", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text("Your reservation is saved in Firestore.", color = Color.Gray)
        Spacer(Modifier.height(20.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(20.dp).fillMaxWidth()) {
                Text(booking.product.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("${booking.quantity} item(s) • Nu. ${booking.product.discountedPrice * booking.quantity}", color = Wine, fontWeight = FontWeight.Bold)
                HorizontalDivider(Modifier.padding(vertical = 14.dp))
                Text("Pickup location", color = Color.Gray, fontSize = 12.sp)
                Text("${booking.product.address}, ${booking.product.dzongkhag}", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(14.dp))
                Text("Pickup code", color = Color.Gray, fontSize = 12.sp)
                Text(booking.pickupCode, color = Wine, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(viewBookings, Modifier.fillMaxWidth()) { Text("View my bookings") }
        OutlinedButton(continueShopping, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Continue shopping") }
    }
}

@Composable
private fun BookingsScreen(bookings: List<Booking>, explore: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("My bookings", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold); Text("Saved pickup reservations.", color = Color.Gray) }
        if (bookings.isEmpty()) {
            item {
                Column(Modifier.fillParentMaxHeight(.6f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("🎟️", fontSize = 72.sp)
                    Text("No bookings yet", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Button(explore) { Text("Explore products") }
                }
            }
        } else {
            items(bookings, key = { it.pickupCode }) { booking -> BookingCard(booking) }
        }
    }
}

@Composable
private fun BookingCard(booking: Booking) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(booking.product.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("Qty ${booking.quantity} • Nu. ${booking.product.discountedPrice * booking.quantity}", color = Wine, fontWeight = FontWeight.SemiBold)
            Text("📍 ${booking.product.dzongkhag}", color = Color.Gray)
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BookingStatusChip(booking.status)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("Pickup code", color = Color.Gray, fontSize = 10.sp)
                    Text(booking.pickupCode, color = Wine, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun BookingStatusChip(status: String) {
    AssistChip(onClick = {}, label = { Text(status) })
}

@Composable
private fun ShopScreen(
    products: List<Product>, bookings: List<Booking>, openReservations: () -> Unit,
    addProduct: () -> Unit, editProduct: (Product) -> Unit, deleteProduct: (Product) -> Unit
) {
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    val activeReservations = bookings.count { it.status == "Reserved" || it.status == "Ready for pickup" }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("My Shop", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Pem's Handcraft", color = Wine, fontWeight = FontWeight.SemiBold)
                }
                Button(addProduct, shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Add, null); Text(" Product") }
            }
        }
        item {
            Card(onClick = openReservations, colors = CardDefaults.cardColors(containerColor = SoftRose), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ConfirmationNumber, null, tint = Wine, modifier = Modifier.size(32.dp))
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("Pickup reservations", fontWeight = FontWeight.Bold)
                        Text("$activeReservations active reservation(s)", color = Color.Gray, fontSize = 12.sp)
                    }
                    TextButton(openReservations) { Text("View") }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(products.size.toString(), "Products", Modifier.weight(1f))
                MetricCard(products.sumOf { it.stock }.toString(), "In stock", Modifier.weight(1f))
                MetricCard(products.count { it.discountPercent > 0 }.toString(), "Discounts", Modifier.weight(1f))
            }
        }
        item { Text("Your products", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        items(products, key = { it.id }) { product ->
            SellerProductCard(product, { editProduct(product) }, { productToDelete = product })
        }
    }

    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Delete product?") },
            text = { Text("${product.name} will also be removed from Firestore.") },
            confirmButton = { TextButton({ deleteProduct(product); productToDelete = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ productToDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
            Text(label, color = Color.Gray, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SellerProductCard(product: Product, edit: () -> Unit, delete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductArtwork(product, Modifier.size(84.dp), 31)
                Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
                    Text(product.name, fontWeight = FontWeight.Bold)
                    Text("Nu. ${product.discountedPrice}", color = Wine, fontWeight = FontWeight.ExtraBold)
                    if (product.discountPercent > 0) Text("${product.discountPercent}% discount", color = Gold, fontSize = 12.sp)
                    Text("Stock: ${product.stock}", color = if (product.stock > 0) Sage else MaterialTheme.colorScheme.error)
                    Text("📍 ${product.dzongkhag}", color = Color.Gray, fontSize = 12.sp)
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(edit, Modifier.weight(1f)) { Text("Edit") }
                OutlinedButton(delete, Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun SellerReservationsScreen(bookings: List<Booking>, back: () -> Unit, updateStatus: (Booking, String) -> Unit) {
    var selectedFilter by remember { mutableStateOf("Active") }
    val visible = when (selectedFilter) {
        "Active" -> bookings.filter { it.status == "Reserved" || it.status == "Ready for pickup" }
        "Completed" -> bookings.filter { it.status == "Collected" }
        "Cancelled" -> bookings.filter { it.status == "Cancelled" }
        else -> bookings
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(back) { Icon(Icons.Default.ArrowBack, "Back") }
                Column { Text("Reservations", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold); Text("Manage customer pickup bookings.", color = Color.Gray) }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Active", "Completed", "Cancelled", "All")) { filter ->
                    FilterChip(selectedFilter == filter, { selectedFilter = filter }, { Text(filter) })
                }
            }
        }
        if (visible.isEmpty()) {
            item { Text("No $selectedFilter reservations", color = Color.Gray, modifier = Modifier.padding(24.dp)) }
        } else {
            items(visible, key = { it.pickupCode }) { booking ->
                SellerReservationCard(booking) { newStatus -> updateStatus(booking, newStatus) }
            }
        }
    }
}

@Composable
private fun SellerReservationCard(booking: Booking, updateStatus: (String) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(booking.product.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("Qty ${booking.quantity} • Nu. ${booking.product.discountedPrice * booking.quantity}", color = Wine, fontWeight = FontWeight.SemiBold)
            Text("📍 ${booking.product.address}, ${booking.product.dzongkhag}", color = Color.Gray, fontSize = 12.sp)
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Pickup code", color = Color.Gray, fontSize = 11.sp); Text(booking.pickupCode, color = Wine, fontWeight = FontWeight.ExtraBold) }
                BookingStatusChip(booking.status)
            }
            when (booking.status) {
                "Reserved" -> {
                    Spacer(Modifier.height(12.dp))
                    Button({ updateStatus("Ready for pickup") }, Modifier.fillMaxWidth()) { Text("Mark ready for pickup") }
                    TextButton({ updateStatus("Cancelled") }, Modifier.fillMaxWidth()) { Text("Cancel reservation", color = MaterialTheme.colorScheme.error) }
                }
                "Ready for pickup" -> {
                    Spacer(Modifier.height(12.dp))
                    Button({ updateStatus("Collected") }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Sage)) { Text("Mark as collected") }
                    TextButton({ updateStatus("Cancelled") }, Modifier.fillMaxWidth()) { Text("Cancel reservation", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun AddProductScreen(nextId: Int, save: (Product) -> Unit, back: () -> Unit) {
    ProductFormScreen("Register a product", null, nextId, save, back)
}

@Composable
private fun EditProductScreen(product: Product, save: (Product) -> Unit, back: () -> Unit) {
    ProductFormScreen("Edit product", product, product.id, save, back)
}

@Composable
private fun ProductFormScreen(title: String, initial: Product?, nextId: Int, save: (Product) -> Unit, back: () -> Unit) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var price by remember(initial) { mutableStateOf(initial?.price?.toString() ?: "") }
    var stock by remember(initial) { mutableStateOf(initial?.stock?.toString() ?: "") }
    var discount by remember(initial) { mutableStateOf(initial?.discountPercent?.toString() ?: "0") }
    var description by remember(initial) { mutableStateOf(initial?.description ?: "") }
    var category by remember(initial) { mutableStateOf(initial?.category ?: "Textiles") }
    var dzongkhag by remember(initial) { mutableStateOf(initial?.dzongkhag ?: "Thimphu") }
    var address by remember(initial) { mutableStateOf(initial?.address ?: "") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var locationExpanded by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    val valid = name.isNotBlank() && price.toIntOrNull() != null && stock.toIntOrNull() != null && address.isNotBlank() && (discount.toIntOrNull() ?: -1) in 0..90

    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(back) { Icon(Icons.Default.ArrowBack, "Back") }; Text(title, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold) } }
        item { FormField("Product name", name, { name = it }, "e.g., Handwoven Yathra Bag") }
        item {
            ExposedDropdownMenuBox(categoryExpanded, { categoryExpanded = it }) {
                OutlinedTextField(category, {}, readOnly = true, label = { Text("Category") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                ExposedDropdownMenu(categoryExpanded, { categoryExpanded = false }) {
                    listOf("Textiles", "Crafts", "Home", "Food", "Wellness").forEach { option -> DropdownMenuItem({ Text(option) }, { category = option; categoryExpanded = false }) }
                }
            }
        }
        item { FormField("Regular price (Nu.)", price, { price = it.filter(Char::isDigit) }, "0", KeyboardType.Number) }
        item { FormField("Available stock", stock, { stock = it.filter(Char::isDigit) }, "10", KeyboardType.Number) }
        item { FormField("Discount (%)", discount, { discount = it.filter(Char::isDigit).take(2) }, "0", KeyboardType.Number) }
        item { Text("Shop location", fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("GPS is not required.", color = Color.Gray, fontSize = 12.sp) }
        item {
            ExposedDropdownMenuBox(locationExpanded, { locationExpanded = it }) {
                OutlinedTextField(dzongkhag, {}, readOnly = true, label = { Text("Dzongkhag") }, leadingIcon = { Icon(Icons.Outlined.LocationOn, null) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(locationExpanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                ExposedDropdownMenu(locationExpanded, { locationExpanded = false }) {
                    dzongkhags.forEach { place -> DropdownMenuItem({ Text(place) }, { dzongkhag = place; locationExpanded = false }) }
                }
            }
        }
        item { FormField("Pickup address", address, { address = it }, "Town, street, landmark or shop address", minLines = 2) }
        item { FormField("Description", description, { description = it }, "Tell buyers what makes this special", minLines = 4) }
        item {
            Button(
                onClick = {
                    if (valid) {
                        save(
                            Product(
                                id = initial?.id ?: nextId,
                                name = name.trim(), category = category, price = price.toInt(), rating = initial?.rating ?: 0.0,
                                seller = initial?.seller ?: "Pem's Handcraft", dzongkhag = dzongkhag, address = address.trim(),
                                stock = stock.toInt(), discountPercent = discount.toIntOrNull() ?: 0,
                                symbol = initial?.symbol ?: "🎁", colors = initial?.colors ?: listOf(Color(0xFFB95C4B), Color(0xFFE9A64A)),
                                description = description.trim()
                            )
                        )
                    } else showError = true
                },
                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)
            ) { Text(if (initial == null) "Register product" else "Save changes", fontWeight = FontWeight.Bold) }
            if (showError && !valid) Text("Complete all required fields. Discount must be 0–90%.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
    }
}

@Composable
private fun FormField(label: String, value: String, change: (String) -> Unit, placeholder: String, type: KeyboardType = KeyboardType.Text, minLines: Int = 1) {
    OutlinedTextField(value, change, Modifier.fillMaxWidth(), label = { Text(label) }, placeholder = { Text(placeholder) }, keyboardOptions = KeyboardOptions(keyboardType = type), minLines = minLines, shape = RoundedCornerShape(15.dp))
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RoleSelectionPreview() { TshongLaTheme { RoleSelectionScreen({}, {}) } }

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProductDetailPreview() { TshongLaTheme { ProductDetailScreen(sampleProducts.first(), {}, {}) } }

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ShopPreview() { TshongLaTheme { ShopScreen(sampleProducts, emptyList(), {}, {}, {}, {}) } }
