@file:OptIn(ExperimentalMaterial3Api::class)

package com.karmagbyte.tshongla

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

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
    val sellerId: String = "",
    val dzongkhag: String,
    val address: String,
    val stock: Int,
    val discountPercent: Int,
    val symbol: String,
    val colors: List<Color>,
    val description: String,
    val imageUrl: String = ""
) {
    val discountedPrice: Int
        get() = if (discountPercent <= 0) price else (price * (100 - discountPercent)) / 100
}

data class Booking(
    val product: Product,
    val quantity: Int,
    val pickupCode: String,
    val status: String = "Reserved",
    val customerId: String = "",
    val sellerId: String = ""
)

private val sampleProducts = listOf(
    Product(1, "Handwoven Yathra Bag", "Textiles", 1250, 4.9, "Pem's Handcraft", "demo", "Bumthang", "Chamkhar Town, near main market", 8, 10, "👜", listOf(Color(0xFFB95C4B), Color(0xFFE9A64A)), "A colourful handwoven shoulder bag made with traditional Bhutanese yathra patterns."),
    Product(2, "Natural Incense Set", "Wellness", 480, 4.8, "Druk Aromas", "demo", "Thimphu", "Norzin Lam, Thimphu", 15, 0, "🌿", listOf(Color(0xFF557A5D), Color(0xFFA7C48C)), "A calming selection of locally prepared incense using aromatic Himalayan herbs."),
    Product(3, "Carved Wooden Bowl", "Home", 890, 4.7, "Zorig Woodworks", "demo", "Trashigang", "Trashigang Town, upper market", 5, 5, "🥣", listOf(Color(0xFF8B5A3C), Color(0xFFD69B61)), "A smooth, food-safe wooden bowl individually carved and finished by a local artisan.")
)

enum class Screen {
    RoleSelect, CustomerLogin, ShopkeeperLogin, CustomerRegister, ShopkeeperRegister,
    Home, Explore, Detail, Bookings, BookingSuccess, Shop, SellerReservations, AddProduct, EditProduct
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
    var currentUser by remember { mutableStateOf<TshongLaUser?>(null) }
    val products = remember { mutableStateListOf<Product>() }
    val bookings = remember { mutableStateListOf<Booking>() }
    var selectedProduct by remember { mutableStateOf(sampleProducts.first()) }
    var latestBooking by remember { mutableStateOf<Booking?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var loadingProducts by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    DisposableEffect(currentUser?.uid, currentUser?.role) {
        val user = currentUser
        if (user == null) {
            onDispose { }
        } else {
            loadingProducts = true
            TshongLaFirestore.seedProductsIfEmpty(sampleProducts)
            val productListener = TshongLaFirestore.listenToProducts(
                onChange = { latest ->
                    products.clear(); products.addAll(latest); loadingProducts = false
                    latest.firstOrNull { it.id == selectedProduct.id }?.let { selectedProduct = it }
                },
                onError = { message = it; loadingProducts = false }
            )
            val bookingListener = if (user.role == "shopkeeper") {
                TshongLaFirestore.listenToBookingsForSeller(user.uid,
                    onChange = { latest -> bookings.clear(); bookings.addAll(latest) },
                    onError = { message = it })
            } else {
                TshongLaFirestore.listenToBookingsForCustomer(user.uid,
                    onChange = { latest -> bookings.clear(); bookings.addAll(latest) },
                    onError = { message = it })
            }
            onDispose { productListener.remove(); bookingListener.remove() }
        }
    }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); message = null }
    }

    fun signOut() {
        TshongLaAuth.signOut()
        currentUser = null
        products.clear(); bookings.clear()
        screen = Screen.RoleSelect
    }

    Scaffold(
        containerColor = Cream,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (screen in listOf(Screen.Home, Screen.Explore, Screen.Bookings)) {
                CustomerBottomBar(screen, bookings.size) { screen = it }
            }
            if (screen in listOf(Screen.Shop, Screen.SellerReservations)) {
                SellerBottomBar(screen, { screen = it }, ::signOut)
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
                    subtitle = "Sign in to browse and reserve handmade products.",
                    icon = Icons.Outlined.Person,
                    back = { screen = Screen.RoleSelect },
                    login = { email, password ->
                        TshongLaAuth.login(email, password, "customer",
                            onSuccess = { currentUser = it; screen = Screen.Home },
                            onError = { message = it })
                    },
                    createAccount = { screen = Screen.CustomerRegister }
                )
                Screen.ShopkeeperLogin -> LoginScreen(
                    title = "Shopkeeper login",
                    subtitle = "Sign in to manage products, stock, discounts and reservations.",
                    icon = Icons.Outlined.Storefront,
                    back = { screen = Screen.RoleSelect },
                    login = { email, password ->
                        TshongLaAuth.login(email, password, "shopkeeper",
                            onSuccess = { currentUser = it; screen = Screen.Shop },
                            onError = { message = it })
                    },
                    createAccount = { screen = Screen.ShopkeeperRegister }
                )
                Screen.CustomerRegister -> RegisterScreen(
                    role = "customer",
                    back = { screen = Screen.CustomerLogin },
                    register = { name, email, password, _, _, _ ->
                        TshongLaAuth.register(name, email, password, "customer",
                            onSuccess = { currentUser = it; message = "Account created"; screen = Screen.Home },
                            onError = { message = it })
                    }
                )
                Screen.ShopkeeperRegister -> RegisterScreen(
                    role = "shopkeeper",
                    back = { screen = Screen.ShopkeeperLogin },
                    register = { name, email, password, shopName, dzongkhag, address ->
                        TshongLaAuth.register(name, email, password, "shopkeeper", shopName, dzongkhag, address,
                            onSuccess = { currentUser = it; message = "Shopkeeper account created"; screen = Screen.Shop },
                            onError = { message = it })
                    }
                )
                Screen.Home -> HomeScreen(products, loadingProducts,
                    openProduct = { selectedProduct = it; screen = Screen.Detail },
                    explore = { screen = Screen.Explore })
                Screen.Explore -> ExploreScreen(products, loadingProducts) { selectedProduct = it; screen = Screen.Detail }
                Screen.Detail -> ProductDetailScreen(selectedProduct,
                    back = { screen = Screen.Home },
                    book = { quantity ->
                        val uid = currentUser?.uid.orEmpty()
                        TshongLaFirestore.createBooking(selectedProduct, uid, quantity,
                            onSuccess = { latestBooking = it; screen = Screen.BookingSuccess },
                            onError = { message = it })
                    })
                Screen.Bookings -> BookingsScreen(bookings) { screen = Screen.Explore }
                Screen.BookingSuccess -> BookingSuccessScreen(latestBooking,
                    viewBookings = { screen = Screen.Bookings },
                    continueShopping = { screen = Screen.Home })
                Screen.Shop -> {
                    val user = currentUser
                    val mine = products.filter { it.sellerId == user?.uid }
                    ShopScreen(
                        products = mine,
                        bookings = bookings,
                        shopName = user?.shopName?.ifBlank { user.name } ?: "My Shop",
                        openReservations = { screen = Screen.SellerReservations },
                        addProduct = { screen = Screen.AddProduct },
                        editProduct = { selectedProduct = it; screen = Screen.EditProduct },
                        deleteProduct = { TshongLaFirestore.deleteProduct(it, onError = { error -> message = error }) }
                    )
                }
                Screen.SellerReservations -> SellerReservationsScreen(bookings,
                    back = { screen = Screen.Shop },
                    updateStatus = { booking, status ->
                        TshongLaFirestore.updateBookingStatus(booking, status, onError = { message = it })
                    })
                Screen.AddProduct -> {
                    val user = currentUser ?: return@Box
                    AddProductScreen(
                        nextId = (products.maxOfOrNull { it.id } ?: 0) + 1,
                        sellerId = user.uid,
                        sellerName = user.shopName.ifBlank { user.name },
                        defaultDzongkhag = user.dzongkhag.ifBlank { "Thimphu" },
                        defaultAddress = user.address,
                        save = { product, imageUri ->
                            saveProductWithOptionalImage(product, imageUri,
                                success = { screen = Screen.Shop },
                                error = { message = it })
                        },
                        back = { screen = Screen.Shop }
                    )
                }
                Screen.EditProduct -> EditProductScreen(
                    product = selectedProduct,
                    save = { product, imageUri ->
                        saveProductWithOptionalImage(product, imageUri,
                            success = { selectedProduct = product; screen = Screen.Shop },
                            error = { message = it })
                    },
                    back = { screen = Screen.Shop }
                )
            }
        }
    }
}

private fun saveProductWithOptionalImage(
    product: Product,
    imageUri: Uri?,
    success: () -> Unit,
    error: (String) -> Unit
) {
    if (imageUri == null) {
        TshongLaFirestore.saveProduct(product, onSuccess = success, onError = error)
    } else {
        TshongLaStorage.uploadProductImage(product.id, imageUri,
            onSuccess = { url -> TshongLaFirestore.saveProduct(product.copy(imageUrl = url), onSuccess = success, onError = error) },
            onError = error)
    }
}

@Composable
private fun RoleSelectionScreen(customer: () -> Unit, shopkeeper: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFFF3E4), Cream, Color.White))).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(42.dp))
        Surface(shape = CircleShape, color = Wine, modifier = Modifier.size(76.dp)) {
            Box(contentAlignment = Alignment.Center) { Text("T", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold) }
        }
        Spacer(Modifier.height(18.dp))
        Text("TshongLa", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
        Text("Handmade Marketplace", color = Wine, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(34.dp))
        Text("How would you like to continue?", fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        RoleCard("I'm a Customer", "Browse local handmade products and reserve them for pickup.", Icons.Outlined.ShoppingBag, customer)
        Spacer(Modifier.height(16.dp))
        RoleCard("I'm a Shopkeeper", "Register products, add photos and manage your shop.", Icons.Outlined.Storefront, shopkeeper)
    }
}

@Composable
private fun RoleCard(title: String, text: String, icon: ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(18.dp), color = SoftRose, modifier = Modifier.size(62.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Wine, modifier = Modifier.size(30.dp)) }
            }
            Column(Modifier.padding(horizontal = 14.dp).weight(1f)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text, color = Color.Gray, fontSize = 13.sp)
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

@Composable
private fun LoginScreen(
    title: String,
    subtitle: String,
    icon: ImageVector,
    back: () -> Unit,
    login: (String, String) -> Unit,
    createAccount: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Back") }
            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = SoftRose, modifier = Modifier.size(68.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Wine, modifier = Modifier.size(34.dp)) }
            }
            Spacer(Modifier.height(16.dp))
            Text(title, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = Color.Gray)
        }
        item {
            OutlinedTextField(email, { email = it; error = false }, Modifier.fillMaxWidth(),
                label = { Text("Email") }, leadingIcon = { Icon(Icons.Outlined.Email, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
        }
        item {
            OutlinedTextField(password, { password = it; error = false }, Modifier.fillMaxWidth(),
                label = { Text("Password") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                visualTransformation = PasswordVisualTransformation(), singleLine = true)
        }
        item {
            Button(onClick = { if (email.isNotBlank() && password.isNotBlank()) login(email, password) else error = true },
                modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Login", fontWeight = FontWeight.Bold) }
            if (error) Text("Enter your email and password.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            OutlinedButton(onClick = createAccount, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("Create an account") }
        }
    }
}

@Composable
private fun RegisterScreen(
    role: String,
    back: () -> Unit,
    register: (String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var shopName by remember { mutableStateOf("") }
    var dzongkhag by remember { mutableStateOf("Thimphu") }
    var address by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val shopkeeper = role == "shopkeeper"

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Back") }
                Column {
                    Text(if (shopkeeper) "Create shopkeeper account" else "Create customer account", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Your account is secured with Firebase Authentication.", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
        item { FormField("Full name", name, { name = it }, "Your name") }
        if (shopkeeper) item { FormField("Shop name", shopName, { shopName = it }, "e.g. Pema Crafts") }
        item { FormField("Email", email, { email = it }, "name@example.com", KeyboardType.Email) }
        item {
            OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") },
                supportingText = { Text("Use at least 6 characters") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
        }
        item {
            OutlinedTextField(confirm, { confirm = it }, Modifier.fillMaxWidth(), label = { Text("Confirm password") },
                visualTransformation = PasswordVisualTransformation(), singleLine = true)
        }
        if (shopkeeper) {
            item {
                ExposedDropdownMenuBox(expanded, { expanded = it }) {
                    OutlinedTextField(dzongkhag, {}, readOnly = true, label = { Text("Dzongkhag") },
                        leadingIcon = { Icon(Icons.Outlined.LocationOn, null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                    ExposedDropdownMenu(expanded, { expanded = false }) {
                        dzongkhags.forEach { d -> DropdownMenuItem({ Text(d) }, { dzongkhag = d; expanded = false }) }
                    }
                }
            }
            item { FormField("Shop / pickup address", address, { address = it }, "Town, street or landmark", minLines = 2) }
        }
        item {
            Button(onClick = {
                error = when {
                    name.isBlank() || email.isBlank() || password.isBlank() -> "Complete all required fields."
                    shopkeeper && (shopName.isBlank() || address.isBlank()) -> "Enter your shop name and pickup address."
                    password.length < 6 -> "Password must contain at least 6 characters."
                    password != confirm -> "Passwords do not match."
                    else -> ""
                }
                if (error.isBlank()) register(name, email, password, shopName, dzongkhag, address)
            }, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Create account", fontWeight = FontWeight.Bold) }
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CustomerBottomBar(current: Screen, bookingCount: Int, navigate: (Screen) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        listOf(
            Triple(Screen.Home, Icons.Outlined.Home, "Home"),
            Triple(Screen.Explore, Icons.Outlined.Search, "Explore"),
            Triple(Screen.Bookings, Icons.Outlined.ConfirmationNumber, "Bookings")
        ).forEach { (screen, icon, label) ->
            NavigationBarItem(selected = current == screen, onClick = { navigate(screen) }, icon = {
                BadgedBox(badge = { if (screen == Screen.Bookings && bookingCount > 0) Badge { Text(bookingCount.toString()) } }) { Icon(icon, label) }
            }, label = { Text(label) })
        }
    }
}

@Composable
private fun SellerBottomBar(current: Screen, navigate: (Screen) -> Unit, signOut: () -> Unit) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(current == Screen.Shop, { navigate(Screen.Shop) }, { Icon(Icons.Outlined.Storefront, "Shop") }, label = { Text("My Shop") })
        NavigationBarItem(current == Screen.SellerReservations, { navigate(Screen.SellerReservations) }, { Icon(Icons.Outlined.ReceiptLong, "Reservations") }, label = { Text("Reservations") })
        NavigationBarItem(false, signOut, { Icon(Icons.Outlined.Logout, "Sign out") }, label = { Text("Sign out") })
    }
}

@Composable
private fun HomeScreen(products: List<Product>, loading: Boolean, openProduct: (Product) -> Unit, explore: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
        item {
            Column(Modifier.background(Brush.verticalGradient(listOf(Color(0xFFFFF4E2), Cream))).padding(20.dp)) {
                Text("Kuzu zangpo!", color = Wine, fontWeight = FontWeight.SemiBold)
                Text("Find something\nmade with meaning.", fontSize = 29.sp, lineHeight = 33.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(16.dp)); SearchBar(readOnly = true, onClick = explore)
            }
        }
        item { SectionHeader("Featured for you", "Explore all", explore) }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) }
        items(products.take(6).chunked(2)) { row ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { product -> Box(Modifier.weight(1f)) { ProductCard(product) { openProduct(product) } } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String, click: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        TextButton(onClick = click) { Text(action) }
    }
}

@Composable
private fun SearchBar(readOnly: Boolean = false, onClick: (() -> Unit)? = null, value: String = "", onValueChange: (String) -> Unit = {}) {
    OutlinedTextField(value, onValueChange, Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        enabled = !readOnly, readOnly = readOnly, placeholder = { Text("Search handmade products") }, leadingIcon = { Icon(Icons.Default.Search, null) }, shape = RoundedCornerShape(18.dp))
}

@Composable
private fun ProductArtwork(product: Product, modifier: Modifier, symbolSize: Int) {
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(product.colors)), contentAlignment = Alignment.Center) {
        if (product.imageUrl.isNotBlank()) {
            AsyncImage(model = product.imageUrl, contentDescription = product.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else Text(product.symbol, fontSize = symbolSize.sp)
    }
}

@Composable
private fun ProductCard(product: Product, click: () -> Unit) {
    Card(onClick = click, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column {
            ProductArtwork(product, Modifier.fillMaxWidth().height(130.dp), 46)
            Column(Modifier.padding(12.dp)) {
                if (product.discountPercent > 0) Text("${product.discountPercent}% OFF", color = Wine, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(product.name, fontWeight = FontWeight.Bold, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Nu. ${product.discountedPrice}", color = Wine, fontWeight = FontWeight.ExtraBold)
                Text("📍 ${product.dzongkhag}", color = Color.Gray, fontSize = 11.sp)
                Text(if (product.stock > 0) "${product.stock} available" else "Out of stock", color = if (product.stock > 0) Sage else MaterialTheme.colorScheme.error, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ExploreScreen(products: List<Product>, loading: Boolean, openProduct: (Product) -> Unit) {
    var query by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("All Bhutan") }
    var expanded by remember { mutableStateOf(false) }
    val filtered = products.filter { it.name.contains(query, true) && (place == "All Bhutan" || it.dzongkhag == place) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Explore products", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold) }
        item { SearchBar(value = query, onValueChange = { query = it }) }
        item {
            ExposedDropdownMenuBox(expanded, { expanded = it }) {
                OutlinedTextField(place, {}, readOnly = true, label = { Text("Shop by Dzongkhag") },
                    leadingIcon = { Icon(Icons.Outlined.LocationOn, null) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                ExposedDropdownMenu(expanded, { expanded = false }) {
                    (listOf("All Bhutan") + dzongkhags).forEach { d -> DropdownMenuItem({ Text(d) }, { place = d; expanded = false }) }
                }
            }
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        items(filtered.chunked(2)) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { p -> Box(Modifier.weight(1f)) { ProductCard(p) { openProduct(p) } } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProductDetailScreen(product: Product, back: () -> Unit, book: (Int) -> Unit) {
    var quantity by remember(product.id) { mutableIntStateOf(1) }
    LazyColumn {
        item {
            Box(Modifier.fillMaxWidth().height(300.dp)) {
                ProductArtwork(product, Modifier.fillMaxSize().clip(RoundedCornerShape(0.dp)), 90)
                Surface(Modifier.padding(16.dp), shape = CircleShape, color = Color.White) { IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Back") } }
            }
        }
        item {
            Column(Modifier.padding(20.dp)) {
                Text(product.category.uppercase(), color = Wine, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(product.name, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                Text("Nu. ${product.discountedPrice}", color = Wine, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                if (product.discountPercent > 0) Text("Regular Nu. ${product.price} • ${product.discountPercent}% off", color = Color.Gray)
                HorizontalDivider(Modifier.padding(vertical = 16.dp))
                Text(product.seller, fontWeight = FontWeight.Bold)
                Text("📍 ${product.address}, ${product.dzongkhag}", color = Color.Gray)
                Spacer(Modifier.height(14.dp)); Text(product.description)
                Spacer(Modifier.height(18.dp)); Text("Available: ${product.stock}", color = if (product.stock > 0) Sage else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedIconButton({ if (quantity > 1) quantity-- }) { Icon(Icons.Default.Remove, null) }
                    Text(quantity.toString(), Modifier.padding(horizontal = 18.dp), fontWeight = FontWeight.Bold)
                    OutlinedIconButton({ if (quantity < product.stock) quantity++ }) { Icon(Icons.Default.Add, null) }
                }
                Spacer(Modifier.height(16.dp))
                Button({ book(quantity) }, enabled = product.stock > 0, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Book for pickup • Nu. ${product.discountedPrice * quantity}") }
            }
        }
    }
}

@Composable
private fun BookingSuccessScreen(booking: Booking?, viewBookings: () -> Unit, continueShopping: () -> Unit) {
    if (booking == null) return
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.CheckCircle, null, tint = Sage, modifier = Modifier.size(72.dp))
        Text("Item reserved!", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(16.dp))
        Text(booking.product.name, fontWeight = FontWeight.Bold)
        Text("Pickup code: ${booking.pickupCode}", color = Wine, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text("${booking.product.address}, ${booking.product.dzongkhag}", color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp)); Button(viewBookings, Modifier.fillMaxWidth()) { Text("View my bookings") }
        OutlinedButton(continueShopping, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Continue shopping") }
    }
}

@Composable
private fun BookingsScreen(bookings: List<Booking>, explore: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("My bookings", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold) }
        if (bookings.isEmpty()) item { Button(explore) { Text("No bookings yet — explore products") } }
        items(bookings, key = { it.pickupCode }) { b ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProductArtwork(b.product, Modifier.size(72.dp), 28)
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(b.product.name, fontWeight = FontWeight.Bold)
                        Text("Qty ${b.quantity} • ${b.status}", color = Wine)
                        Text("Pickup: ${b.pickupCode}", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopScreen(
    products: List<Product>, bookings: List<Booking>, shopName: String,
    openReservations: () -> Unit, addProduct: () -> Unit, editProduct: (Product) -> Unit, deleteProduct: (Product) -> Unit
) {
    var deleteTarget by remember { mutableStateOf<Product?>(null) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("My Shop", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold); Text(shopName, color = Wine) }
                Button(addProduct) { Icon(Icons.Default.Add, null); Text(" Product") }
            }
        }
        item {
            Card(onClick = openReservations, colors = CardDefaults.cardColors(containerColor = SoftRose)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ReceiptLong, null, tint = Wine)
                    Text("  ${bookings.count { it.status == "Reserved" || it.status == "Ready for pickup" }} active reservations", modifier = Modifier.weight(1f))
                    Text("View", color = Wine)
                }
            }
        }
        item { Text("Your products", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        items(products, key = { it.id }) { p ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProductArtwork(p, Modifier.size(82.dp), 30)
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(p.name, fontWeight = FontWeight.Bold)
                            Text("Nu. ${p.discountedPrice}", color = Wine)
                            Text("Stock ${p.stock} • ${p.discountPercent}% discount", fontSize = 12.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ editProduct(p) }, Modifier.weight(1f)) { Text("Edit") }
                        OutlinedButton({ deleteTarget = p }, Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
                    }
                }
            }
        }
    }
    deleteTarget?.let { p ->
        AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Delete product?") }, text = { Text(p.name) },
            confirmButton = { TextButton({ deleteProduct(p); deleteTarget = null }) { Text("Delete") } },
            dismissButton = { TextButton({ deleteTarget = null }) { Text("Cancel") } })
    }
}

@Composable
private fun SellerReservationsScreen(bookings: List<Booking>, back: () -> Unit, updateStatus: (Booking, String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(back) { Icon(Icons.Default.ArrowBack, null) }; Text("Reservations", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold) } }
        items(bookings, key = { it.pickupCode }) { b ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(14.dp)) {
                    Text(b.product.name, fontWeight = FontWeight.Bold)
                    Text("Qty ${b.quantity} • ${b.pickupCode}", color = Wine)
                    Text(b.status, fontWeight = FontWeight.SemiBold)
                    when (b.status) {
                        "Reserved" -> {
                            Button({ updateStatus(b, "Ready for pickup") }, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Mark ready for pickup") }
                            TextButton({ updateStatus(b, "Cancelled") }, Modifier.fillMaxWidth()) { Text("Cancel") }
                        }
                        "Ready for pickup" -> Button({ updateStatus(b, "Collected") }, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Mark as collected") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddProductScreen(
    nextId: Int, sellerId: String, sellerName: String, defaultDzongkhag: String, defaultAddress: String,
    save: (Product, Uri?) -> Unit, back: () -> Unit
) {
    ProductFormScreen("Register a product", null, nextId, sellerId, sellerName, defaultDzongkhag, defaultAddress, save, back)
}

@Composable
private fun EditProductScreen(product: Product, save: (Product, Uri?) -> Unit, back: () -> Unit) {
    ProductFormScreen("Edit product", product, product.id, product.sellerId, product.seller, product.dzongkhag, product.address, save, back)
}

@Composable
private fun ProductFormScreen(
    title: String,
    initial: Product?,
    nextId: Int,
    sellerId: String,
    sellerName: String,
    defaultDzongkhag: String,
    defaultAddress: String,
    save: (Product, Uri?) -> Unit,
    back: () -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var price by remember(initial) { mutableStateOf(initial?.price?.toString() ?: "") }
    var stock by remember(initial) { mutableStateOf(initial?.stock?.toString() ?: "") }
    var discount by remember(initial) { mutableStateOf(initial?.discountPercent?.toString() ?: "0") }
    var description by remember(initial) { mutableStateOf(initial?.description ?: "") }
    var category by remember(initial) { mutableStateOf(initial?.category ?: "Crafts") }
    var dzongkhag by remember(initial) { mutableStateOf(initial?.dzongkhag ?: defaultDzongkhag) }
    var address by remember(initial) { mutableStateOf(initial?.address ?: defaultAddress) }
    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var locationExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> selectedImage = uri }
    val productId = initial?.id ?: nextId

    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(back) { Icon(Icons.Default.ArrowBack, null) }; Text(title, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold) } }
        item {
            Card(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth().height(190.dp), colors = CardDefaults.cardColors(containerColor = SoftRose)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val preview: Any? = selectedImage ?: initial?.imageUrl?.takeIf { it.isNotBlank() }
                    if (preview != null) {
                        AsyncImage(model = preview, contentDescription = "Product image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Surface(Modifier.align(Alignment.BottomCenter).padding(10.dp), color = Color.White.copy(alpha = .9f), shape = RoundedCornerShape(14.dp)) {
                            Text("Tap to change photo", Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = Wine, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.AddPhotoAlternate, null, tint = Wine, modifier = Modifier.size(42.dp))
                            Text("Add product photo", fontWeight = FontWeight.Bold)
                            Text("Choose an image from your phone", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        item { FormField("Product name", name, { name = it }, "e.g. Handmade basket") }
        item {
            ExposedDropdownMenuBox(categoryExpanded, { categoryExpanded = it }) {
                OutlinedTextField(category, {}, readOnly = true, label = { Text("Category") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                ExposedDropdownMenu(categoryExpanded, { categoryExpanded = false }) {
                    listOf("Textiles", "Crafts", "Home", "Food", "Wellness").forEach { c -> DropdownMenuItem({ Text(c) }, { category = c; categoryExpanded = false }) }
                }
            }
        }
        item { FormField("Regular price (Nu.)", price, { price = it.filter(Char::isDigit) }, "0", KeyboardType.Number) }
        item { FormField("Available stock", stock, { stock = it.filter(Char::isDigit) }, "10", KeyboardType.Number) }
        item { FormField("Discount (%)", discount, { discount = it.filter(Char::isDigit).take(2) }, "0", KeyboardType.Number) }
        item {
            ExposedDropdownMenuBox(locationExpanded, { locationExpanded = it }) {
                OutlinedTextField(dzongkhag, {}, readOnly = true, label = { Text("Dzongkhag") }, leadingIcon = { Icon(Icons.Outlined.LocationOn, null) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(locationExpanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                ExposedDropdownMenu(locationExpanded, { locationExpanded = false }) {
                    dzongkhags.forEach { d -> DropdownMenuItem({ Text(d) }, { dzongkhag = d; locationExpanded = false }) }
                }
            }
        }
        item { FormField("Pickup address", address, { address = it }, "Town, street or landmark", minLines = 2) }
        item { FormField("Description", description, { description = it }, "Describe the product", minLines = 4) }
        item {
            Button(onClick = {
                val p = price.toIntOrNull(); val s = stock.toIntOrNull(); val d = discount.toIntOrNull()
                error = when {
                    name.isBlank() || p == null || s == null || address.isBlank() -> "Complete all required fields."
                    d == null || d !in 0..90 -> "Discount must be between 0 and 90%."
                    else -> ""
                }
                if (error.isBlank()) {
                    save(Product(productId, name.trim(), category, p!!, initial?.rating ?: 0.0, sellerName, sellerId, dzongkhag, address.trim(), s!!, d!!,
                        initial?.symbol ?: "🎁", initial?.colors ?: listOf(Color(0xFFB95C4B), Color(0xFFE9A64A)), description.trim(), initial?.imageUrl ?: ""), selectedImage)
                }
            }, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text(if (initial == null) "Register product" else "Save changes", fontWeight = FontWeight.Bold) }
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
    }
}

@Composable
private fun FormField(label: String, value: String, change: (String) -> Unit, placeholder: String, type: KeyboardType = KeyboardType.Text, minLines: Int = 1) {
    OutlinedTextField(value, change, Modifier.fillMaxWidth(), label = { Text(label) }, placeholder = { Text(placeholder) }, keyboardOptions = KeyboardOptions(keyboardType = type), minLines = minLines)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RolePreview() { TshongLaTheme { RoleSelectionScreen({}, {}) } }

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProductPreview() { TshongLaTheme { ProductDetailScreen(sampleProducts.first(), {}, {}) } }
