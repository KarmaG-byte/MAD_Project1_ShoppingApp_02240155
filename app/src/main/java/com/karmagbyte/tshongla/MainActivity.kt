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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
    val discountedPrice: Int get() = if (discountPercent <= 0) price else (price * (100 - discountPercent)) / 100
}

data class Booking(
    val product: Product,
    val quantity: Int,
    val pickupCode: String,
    val status: String = "Reserved",
    val customerId: String = "",
    val sellerId: String = ""
)

data class RegistrationPayload(
    val name: String,
    val email: String,
    val password: String,
    val phone: String,
    val phoneVerified: Boolean,
    val shopName: String = "",
    val dzongkhag: String = "",
    val address: String = "",
    val cidNumber: String = "",
    val cidImageUri: String = ""
)

private val sampleProducts = listOf(
    Product(1, "Handwoven Yathra Bag", "Textiles", 1250, 4.9, "Pem's Handcraft", "demo", "Bumthang", "Chamkhar Town, near main market", 8, 10, "👜", listOf(Color(0xFFB95C4B), Color(0xFFE9A64A)), "A colourful handwoven shoulder bag made with traditional Bhutanese yathra patterns."),
    Product(2, "Natural Incense Set", "Wellness", 480, 4.8, "Druk Aromas", "demo", "Thimphu", "Norzin Lam, Thimphu", 15, 0, "🌿", listOf(Color(0xFF557A5D), Color(0xFFA7C48C)), "A calming selection of locally prepared incense using aromatic Himalayan herbs."),
    Product(3, "Carved Wooden Bowl", "Home", 890, 4.7, "Zorig Woodworks", "demo", "Trashigang", "Trashigang Town, upper market", 5, 5, "🥣", listOf(Color(0xFF8B5A3C), Color(0xFFD69B61)), "A smooth, food-safe wooden bowl individually carved and finished by a local artisan.")
)

enum class Screen {
    RoleSelect, CustomerLogin, ShopkeeperLogin, CustomerRegister, ShopkeeperRegister,
    Home, Explore, Detail, Bookings, BookingSuccess, Shop, SellerReservations,
    AddProduct, EditProduct, Review, Verify
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
        colorScheme = lightColorScheme(primary = Wine, onPrimary = Color.White, secondary = Gold, background = Cream, surface = Color.White, onSurface = Ink),
        content = content
    )
}

@Composable
private fun TshongLaApp() {
    var screen by remember { mutableStateOf(Screen.RoleSelect) }
    var currentUser by remember { mutableStateOf<TshongLaUser?>(null) }
    val products = remember { mutableStateListOf<Product>() }
    val bookings = remember { mutableStateListOf<Booking>() }
    val reviews = remember { mutableStateListOf<Review>() }
    var selectedProduct by remember { mutableStateOf(sampleProducts.first()) }
    var latestBooking by remember { mutableStateOf<Booking?>(null) }
    var reviewBooking by remember { mutableStateOf<Booking?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var loadingProducts by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    DisposableEffect(currentUser?.uid, currentUser?.role) {
        val user = currentUser
        if (user == null) onDispose { }
        else {
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
                TshongLaFirestore.listenToBookingsForSeller(user.uid, { latest -> bookings.clear(); bookings.addAll(latest) }, { message = it })
            } else {
                TshongLaFirestore.listenToBookingsForCustomer(user.uid, { latest -> bookings.clear(); bookings.addAll(latest) }, { message = it })
            }
            onDispose { productListener.remove(); bookingListener.remove() }
        }
    }

    DisposableEffect(currentUser?.uid, selectedProduct.id) {
        if (currentUser == null) onDispose { }
        else {
            val listener = TshongLaReviews.listenToProductReviews(selectedProduct.id,
                onChange = { latest -> reviews.clear(); reviews.addAll(latest) },
                onError = { message = it })
            onDispose { listener.remove() }
        }
    }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); message = null }
    }

    fun signOut() {
        TshongLaAuth.signOut(); currentUser = null; products.clear(); bookings.clear(); reviews.clear(); screen = Screen.RoleSelect
    }

    fun openProduct(product: Product) { selectedProduct = product; screen = Screen.Detail }

    Scaffold(
        containerColor = Cream,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (screen in listOf(Screen.Home, Screen.Explore, Screen.Bookings)) CustomerBottomBar(screen, bookings.size) { screen = it }
            if (screen in listOf(Screen.Shop, Screen.SellerReservations)) SellerBottomBar(screen, { screen = it }, { screen = Screen.Verify }, ::signOut)
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (screen) {
                Screen.RoleSelect -> RoleSelectionScreen({ screen = Screen.CustomerLogin }, { screen = Screen.ShopkeeperLogin })
                Screen.CustomerLogin -> LoginScreen("Customer login", "Sign in to browse and reserve handmade products.", Icons.Outlined.Person,
                    { screen = Screen.RoleSelect },
                    { email, password -> TshongLaAuth.login(email, password, "customer", { currentUser = it; screen = Screen.Home }, { message = it }) },
                    { screen = Screen.CustomerRegister })
                Screen.ShopkeeperLogin -> LoginScreen("Shopkeeper login", "Sign in to manage products, stock, discounts and reservations.", Icons.Outlined.Storefront,
                    { screen = Screen.RoleSelect },
                    { email, password -> TshongLaAuth.login(email, password, "shopkeeper", { currentUser = it; screen = Screen.Shop }, { message = it }) },
                    { screen = Screen.ShopkeeperRegister })
                Screen.CustomerRegister -> RegisterScreen("customer", { screen = Screen.CustomerLogin }) { p ->
                    TshongLaAuth.register(p.name, p.email, p.password, "customer", p.phone, p.phoneVerified,
                        onSuccess = { currentUser = it; message = "Account created and phone verified"; screen = Screen.Home }, onError = { message = it })
                }
                Screen.ShopkeeperRegister -> RegisterScreen("shopkeeper", { screen = Screen.ShopkeeperLogin }) { p ->
                    TshongLaAuth.register(p.name, p.email, p.password, "shopkeeper", p.phone, p.phoneVerified, p.shopName, p.dzongkhag, p.address, p.cidNumber, p.cidImageUri,
                        onSuccess = { currentUser = it; message = "Shopkeeper verification submitted"; screen = Screen.Shop }, onError = { message = it })
                }
                Screen.Home -> HomeScreen(products, loadingProducts, ::openProduct) { screen = Screen.Explore }
                Screen.Explore -> ExploreScreen(products, loadingProducts, ::openProduct)
                Screen.Detail -> ProductDetailScreen(selectedProduct, reviews, { screen = Screen.Home }) { quantity ->
                    val uid = currentUser?.uid.orEmpty()
                    TshongLaFirestore.createBooking(selectedProduct, uid, quantity,
                        onSuccess = { latestBooking = it; screen = Screen.BookingSuccess }, onError = { message = it })
                }
                Screen.Bookings -> BookingsScreen(bookings, { screen = Screen.Explore }) { booking ->
                    reviewBooking = booking; selectedProduct = booking.product; screen = Screen.Review
                }
                Screen.BookingSuccess -> BookingSuccessScreen(latestBooking, { screen = Screen.Bookings }, { screen = Screen.Home })
                Screen.Shop -> {
                    val user = currentUser
                    ShopScreen(
                        products = products.filter { it.sellerId == user?.uid },
                        bookings = bookings,
                        shopName = user?.shopName?.ifBlank { user.name } ?: "My Shop",
                        verified = user?.canPublishProducts == true,
                        openReservations = { screen = Screen.SellerReservations },
                        verify = { screen = Screen.Verify },
                        addProduct = {
                            if (user?.canPublishProducts == true) screen = Screen.AddProduct else { message = "Complete phone and CID verification before publishing products."; screen = Screen.Verify }
                        },
                        editProduct = { selectedProduct = it; screen = Screen.EditProduct },
                        deleteProduct = { TshongLaFirestore.deleteProduct(it, onError = { error -> message = error }) }
                    )
                }
                Screen.SellerReservations -> SellerReservationsScreen(bookings, { screen = Screen.Shop }) { booking, status ->
                    TshongLaFirestore.updateBookingStatus(booking, status, onError = { message = it })
                }
                Screen.AddProduct -> {
                    val user = currentUser ?: return@Box
                    ProductFormScreen("Register a product", null, (products.maxOfOrNull { it.id } ?: 0) + 1, user.uid, user.shopName.ifBlank { user.name }, user.dzongkhag.ifBlank { "Thimphu" }, user.address,
                        save = { product, image -> saveProductWithOptionalImage(product, image, { screen = Screen.Shop }, { message = it }) },
                        back = { screen = Screen.Shop })
                }
                Screen.EditProduct -> ProductFormScreen("Edit product", selectedProduct, selectedProduct.id, selectedProduct.sellerId, selectedProduct.seller, selectedProduct.dzongkhag, selectedProduct.address,
                    save = { product, image -> saveProductWithOptionalImage(product, image, { selectedProduct = product; screen = Screen.Shop }, { message = it }) },
                    back = { screen = Screen.Shop })
                Screen.Review -> {
                    val booking = reviewBooking
                    val user = currentUser
                    if (booking == null || user == null) { screen = Screen.Bookings }
                    else ReviewScreen(booking, user.name, { screen = Screen.Bookings }, { productRating, businessRating, comment, imageUri ->
                        TshongLaReviews.submitReview(booking, user.name, productRating, businessRating, comment, imageUri,
                            onSuccess = { message = "Review submitted"; screen = Screen.Detail }, onError = { message = it })
                    }, { message = it })
                }
                Screen.Verify -> {
                    val user = currentUser ?: return@Box
                    VerificationScreen(user, { screen = if (user.role == "shopkeeper") Screen.Shop else Screen.Home },
                        onVerified = { phone, cid, cidUri ->
                            TshongLaAuth.updateVerification(user, phone, true, cid, cidUri,
                                onSuccess = { currentUser = it; message = "Verification details saved"; screen = if (it.role == "shopkeeper") Screen.Shop else Screen.Home },
                                onError = { message = it })
                        }, onError = { message = it })
                }
            }
        }
    }
}

private fun saveProductWithOptionalImage(product: Product, imageUri: Uri?, success: () -> Unit, error: (String) -> Unit) {
    if (imageUri == null) TshongLaFirestore.saveProduct(product, onSuccess = success, onError = error)
    else TshongLaStorage.uploadProductImage(product.id, imageUri,
        onSuccess = { url -> TshongLaFirestore.saveProduct(product.copy(imageUrl = url), onSuccess = success, onError = error) }, onError = error)
}

@Composable
private fun RoleSelectionScreen(customer: () -> Unit, shopkeeper: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFFF3E4), Cream, Color.White))).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(42.dp))
        androidx.compose.foundation.Image(painter = androidx.compose.ui.res.painterResource(R.drawable.ic_launcher), contentDescription = "TshongLa logo", modifier = Modifier.size(112.dp))
        Spacer(Modifier.height(18.dp)); Text("TshongLa", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold); Text("Handmade Marketplace", color = Wine, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(34.dp)); Text("How would you like to continue?", fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp)); RoleCard("I'm a Customer", "Browse handmade products, reserve, collect and review.", Icons.Outlined.ShoppingBag, customer)
        Spacer(Modifier.height(16.dp)); RoleCard("I'm a Shopkeeper", "Verify your identity, publish products and manage reservations.", Icons.Outlined.Storefront, shopkeeper)
    }
}

@Composable
private fun RoleCard(title: String, text: String, icon: ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(18.dp), color = SoftRose, modifier = Modifier.size(62.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Wine, modifier = Modifier.size(30.dp)) } }
            Column(Modifier.padding(horizontal = 14.dp).weight(1f)) { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(text, color = Color.Gray, fontSize = 13.sp) }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

@Composable
private fun LoginScreen(title: String, subtitle: String, icon: ImageVector, back: () -> Unit, login: (String, String) -> Unit, createAccount: () -> Unit) {
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var error by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Back") }; Spacer(Modifier.height(12.dp)); Icon(icon, null, tint = Wine, modifier = Modifier.size(54.dp)); Text(title, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold); Text(subtitle, color = Color.Gray) }
        item { OutlinedTextField(email, { email = it; error = false }, Modifier.fillMaxWidth(), label = { Text("Email") }, leadingIcon = { Icon(Icons.Outlined.Email, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true) }
        item { OutlinedTextField(password, { password = it; error = false }, Modifier.fillMaxWidth(), label = { Text("Password") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, visualTransformation = PasswordVisualTransformation(), singleLine = true) }
        item { Button({ if (email.isNotBlank() && password.isNotBlank()) login(email, password) else error = true }, Modifier.fillMaxWidth().height(54.dp)) { Text("Login", fontWeight = FontWeight.Bold) }; if (error) Text("Enter your email and password.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp); OutlinedButton(createAccount, Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("Create an account") } }
    }
}

@Composable
private fun RegisterScreen(role: String, back: () -> Unit, register: (RegistrationPayload) -> Unit) {
    val shopkeeper = role == "shopkeeper"
    var name by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }; var code by remember { mutableStateOf("") }; var codeSent by remember { mutableStateOf(false) }; var phoneVerified by remember { mutableStateOf(false) }
    var shopName by remember { mutableStateOf("") }; var dzongkhag by remember { mutableStateOf("Thimphu") }; var address by remember { mutableStateOf("") }
    var cid by remember { mutableStateOf("") }; var cidImage by remember { mutableStateOf<Uri?>(null) }; var expanded by remember { mutableStateOf(false) }; var error by remember { mutableStateOf("") }
    val cidPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { cidImage = it }

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(back) { Icon(Icons.Default.ArrowBack, null) }; Column { Text(if (shopkeeper) "Create shopkeeper account" else "Create customer account", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold); Text("Firebase account + phone verification", color = Color.Gray, fontSize = 12.sp) } } }
        item { FormField("Full name", name, { name = it }, "Your name") }
        if (shopkeeper) item { FormField("Shop name", shopName, { shopName = it }, "e.g. Pema Crafts") }
        item { FormField("Email", email, { email = it }, "name@example.com", KeyboardType.Email) }
        item { OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, supportingText = { Text("At least 6 characters") }, visualTransformation = PasswordVisualTransformation(), singleLine = true) }
        item { OutlinedTextField(confirm, { confirm = it }, Modifier.fillMaxWidth(), label = { Text("Confirm password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true) }
        item { FormField("Phone number", phone, { phone = it.filter { ch -> ch.isDigit() || ch == '+' }; phoneVerified = false }, "+975...​", KeyboardType.Phone) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, Modifier.weight(1f), label = { Text("Verification code") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                Button({ codeSent = true }, enabled = phone.isNotBlank()) { Text("Send") }
            }
            if (codeSent) Text("Prototype SMS code: 123456", color = Wine, fontSize = 12.sp)
            OutlinedButton({ phoneVerified = code == "123456" }, enabled = codeSent && code.length == 6, modifier = Modifier.fillMaxWidth()) { Text(if (phoneVerified) "✓ Phone verified" else "Verify phone") }
        }
        if (shopkeeper) {
            item { ExposedDropdownMenuBox(expanded, { expanded = it }) { OutlinedTextField(dzongkhag, {}, readOnly = true, label = { Text("Dzongkhag") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()); ExposedDropdownMenu(expanded, { expanded = false }) { dzongkhags.forEach { d -> DropdownMenuItem({ Text(d) }, { dzongkhag = d; expanded = false }) } } } }
            item { FormField("Shop / pickup address", address, { address = it }, "Town, street or landmark", minLines = 2) }
            item { FormField("CID number", cid, { cid = it.filter(Char::isDigit) }, "CID number", KeyboardType.Number) }
            item { OutlinedButton({ cidPicker.launch("image/*") }, Modifier.fillMaxWidth().height(52.dp)) { Icon(Icons.Outlined.Badge, null); Text(if (cidImage == null) "  Select CID copy" else "  CID copy selected ✓") } }
        }
        item {
            Button({
                error = when {
                    name.isBlank() || email.isBlank() || password.isBlank() -> "Complete all required fields."
                    password.length < 6 -> "Password must contain at least 6 characters."
                    password != confirm -> "Passwords do not match."
                    !phoneVerified -> "Verify your phone number first."
                    shopkeeper && (shopName.isBlank() || address.isBlank() || cid.isBlank() || cidImage == null) -> "Shopkeepers must provide shop details and a CID copy."
                    else -> ""
                }
                if (error.isBlank()) {
                    if (shopkeeper) TshongLaStorage.saveCidImage(cidImage!!,
                        onSuccess = { uri -> register(RegistrationPayload(name, email, password, phone, true, shopName, dzongkhag, address, cid, uri)) },
                        onError = { error = it })
                    else register(RegistrationPayload(name, email, password, phone, true))
                }
            }, Modifier.fillMaxWidth().height(54.dp)) { Text("Create account", fontWeight = FontWeight.Bold) }
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
    }
}

@Composable
private fun VerificationScreen(user: TshongLaUser, back: () -> Unit, onVerified: (String, String, String) -> Unit, onError: (String) -> Unit) {
    var phone by remember { mutableStateOf(user.phone) }; var code by remember { mutableStateOf("") }; var sent by remember { mutableStateOf(false) }; var verified by remember { mutableStateOf(user.phoneVerified) }
    var cid by remember { mutableStateOf(user.cidNumber) }; var cidUri by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { cidUri = it }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(back) { Icon(Icons.Default.ArrowBack, null) }; Column { Text("Account verification", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold); Text("Required before a shopkeeper can publish products.", color = Color.Gray, fontSize = 12.sp) } } }
        item { FormField("Phone number", phone, { phone = it; verified = false }, "+975...​", KeyboardType.Phone) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, Modifier.weight(1f), label = { Text("Code") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)); Button({ sent = true }) { Text("Send") } }; if (sent) Text("Prototype SMS code: 123456", color = Wine, fontSize = 12.sp); OutlinedButton({ verified = code == "123456" }, enabled = sent, modifier = Modifier.fillMaxWidth()) { Text(if (verified) "✓ Phone verified" else "Verify phone") } }
        if (user.role == "shopkeeper") {
            item { FormField("CID number", cid, { cid = it.filter(Char::isDigit) }, "CID number", KeyboardType.Number) }
            item { OutlinedButton({ picker.launch("image/*") }, Modifier.fillMaxWidth()) { Text(if (cidUri == null && user.cidImageUri.isBlank()) "Select CID copy" else "CID copy selected ✓") } }
        }
        item { Button({
            if (!verified) { onError("Verify your phone first."); return@Button }
            if (user.role == "shopkeeper" && (cid.isBlank() || (cidUri == null && user.cidImageUri.isBlank()))) { onError("CID number and CID copy are required."); return@Button }
            if (user.role == "shopkeeper" && cidUri != null) TshongLaStorage.saveCidImage(cidUri!!, { onVerified(phone, cid, it) }, onError)
            else onVerified(phone, cid, user.cidImageUri)
        }, Modifier.fillMaxWidth().height(54.dp)) { Text("Save verification") } }
    }
}

@Composable
private fun CustomerBottomBar(current: Screen, count: Int, navigate: (Screen) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        listOf(Triple(Screen.Home, Icons.Outlined.Home, "Home"), Triple(Screen.Explore, Icons.Outlined.Search, "Explore"), Triple(Screen.Bookings, Icons.Outlined.ConfirmationNumber, "Bookings")).forEach { (s, i, l) ->
            NavigationBarItem(current == s, { navigate(s) }, { BadgedBox(badge = { if (s == Screen.Bookings && count > 0) Badge { Text(count.toString()) } }) { Icon(i, l) } }, label = { Text(l) })
        }
    }
}

@Composable
private fun SellerBottomBar(current: Screen, navigate: (Screen) -> Unit, verify: () -> Unit, signOut: () -> Unit) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(current == Screen.Shop, { navigate(Screen.Shop) }, { Icon(Icons.Outlined.Storefront, null) }, label = { Text("My Shop") })
        NavigationBarItem(current == Screen.SellerReservations, { navigate(Screen.SellerReservations) }, { Icon(Icons.Outlined.ReceiptLong, null) }, label = { Text("Reservations") })
        NavigationBarItem(false, verify, { Icon(Icons.Outlined.VerifiedUser, null) }, label = { Text("Verify") })
        NavigationBarItem(false, signOut, { Icon(Icons.Outlined.Logout, null) }, label = { Text("Sign out") })
    }
}

@Composable
private fun HomeScreen(products: List<Product>, loading: Boolean, open: (Product) -> Unit, explore: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
        item { Column(Modifier.background(Brush.verticalGradient(listOf(Color(0xFFFFF4E2), Cream))).padding(20.dp)) { Text("Kuzu zangpo!", color = Wine, fontWeight = FontWeight.SemiBold); Text("Find something\nmade with meaning.", fontSize = 29.sp, lineHeight = 33.sp, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.height(16.dp)); OutlinedButton(explore, Modifier.fillMaxWidth()) { Icon(Icons.Default.Search, null); Text("  Explore products") } } }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        items(products.take(6).chunked(2)) { row -> Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { row.forEach { p -> Box(Modifier.weight(1f)) { ProductCard(p) { open(p) } } }; if (row.size == 1) Spacer(Modifier.weight(1f)) } }
    }
}

@Composable
private fun ExploreScreen(products: List<Product>, loading: Boolean, open: (Product) -> Unit) {
    var query by remember { mutableStateOf("") }; var place by remember { mutableStateOf("All Bhutan") }; var expanded by remember { mutableStateOf(false) }
    val filtered = products.filter { it.name.contains(query, true) && (place == "All Bhutan" || it.dzongkhag == place) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Explore products", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold) }
        item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Search") }, leadingIcon = { Icon(Icons.Default.Search, null) }) }
        item { ExposedDropdownMenuBox(expanded, { expanded = it }) { OutlinedTextField(place, {}, readOnly = true, label = { Text("Dzongkhag") }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }); ExposedDropdownMenu(expanded, { expanded = false }) { (listOf("All Bhutan") + dzongkhags).forEach { d -> DropdownMenuItem({ Text(d) }, { place = d; expanded = false }) } } } }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        items(filtered.chunked(2)) { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { row.forEach { p -> Box(Modifier.weight(1f)) { ProductCard(p) { open(p) } } }; if (row.size == 1) Spacer(Modifier.weight(1f)) } }
    }
}

@Composable
private fun ProductArtwork(product: Product, modifier: Modifier, symbolSize: Int) {
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(product.colors)), contentAlignment = Alignment.Center) {
        if (product.imageUrl.isNotBlank()) AsyncImage(product.imageUrl, product.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else Text(product.symbol, fontSize = symbolSize.sp)
    }
}

@Composable
private fun ProductCard(product: Product, click: () -> Unit) {
    Card(onClick = click, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column { ProductArtwork(product, Modifier.fillMaxWidth().height(130.dp), 46); Column(Modifier.padding(12.dp)) { if (product.discountPercent > 0) Text("${product.discountPercent}% OFF", color = Wine, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text(product.name, fontWeight = FontWeight.Bold, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis); Text("★ ${String.format("%.1f", product.rating)}", color = Gold, fontSize = 12.sp); Text("Nu. ${product.discountedPrice}", color = Wine, fontWeight = FontWeight.ExtraBold); Text("📍 ${product.dzongkhag}", color = Color.Gray, fontSize = 11.sp); Text(if (product.stock > 0) "${product.stock} available" else "Out of stock", color = if (product.stock > 0) Sage else MaterialTheme.colorScheme.error, fontSize = 11.sp) } }
    }
}

@Composable
private fun ProductDetailScreen(product: Product, reviews: List<Review>, back: () -> Unit, book: (Int) -> Unit) {
    var quantity by remember(product.id) { mutableIntStateOf(1) }
    val businessAverage = if (reviews.isEmpty()) 0.0 else reviews.map { it.businessRating }.average()
    LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
        item { Box(Modifier.fillMaxWidth().height(290.dp)) { ProductArtwork(product, Modifier.fillMaxSize().clip(RoundedCornerShape(0.dp)), 90); Surface(Modifier.padding(16.dp), shape = CircleShape, color = Color.White) { IconButton(back) { Icon(Icons.Default.ArrowBack, null) } } } }
        item { Column(Modifier.padding(20.dp)) { Text(product.category.uppercase(), color = Wine, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text(product.name, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold); Text("★ ${String.format("%.1f", product.rating)} product rating", color = Gold); if (reviews.isNotEmpty()) Text("★ ${String.format("%.1f", businessAverage)} business rating", color = Gold); Text("Nu. ${product.discountedPrice}", color = Wine, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold); Text(product.seller, fontWeight = FontWeight.Bold); Text("📍 ${product.address}, ${product.dzongkhag}", color = Color.Gray); Spacer(Modifier.height(12.dp)); Text(product.description); Spacer(Modifier.height(12.dp)); Text("Available: ${product.stock}", color = if (product.stock > 0) Sage else MaterialTheme.colorScheme.error); Row(verticalAlignment = Alignment.CenterVertically) { OutlinedIconButton({ if (quantity > 1) quantity-- }) { Icon(Icons.Default.Remove, null) }; Text(quantity.toString(), Modifier.padding(18.dp), fontWeight = FontWeight.Bold); OutlinedIconButton({ if (quantity < product.stock) quantity++ }) { Icon(Icons.Default.Add, null) } }; Button({ book(quantity) }, enabled = product.stock > 0, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Book for pickup • Nu. ${product.discountedPrice * quantity}") } } }
        item { Text("Verified purchase reviews", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp)) }
        if (reviews.isEmpty()) item { Text("No reviews yet.", color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp)) }
        items(reviews, key = { it.id }) { r -> ReviewCard(r) }
    }
}

@Composable
private fun ReviewCard(review: Review) {
    Card(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(14.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Verified, null, tint = Sage); Text(" ${review.customerName} • Verified purchase", fontWeight = FontWeight.Bold, fontSize = 13.sp) }; Text("Product ${"★".repeat(review.productRating)}  Business ${"★".repeat(review.businessRating)}", color = Gold); if (review.comment.isNotBlank()) Text(review.comment, modifier = Modifier.padding(top = 6.dp)); if (review.imageUri.isNotBlank()) AsyncImage(review.imageUri, "Review photo", Modifier.fillMaxWidth().height(160.dp).padding(top = 8.dp), contentScale = ContentScale.Crop) }
    }
}

@Composable
private fun BookingSuccessScreen(booking: Booking?, view: () -> Unit, shop: () -> Unit) {
    if (booking == null) return
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Default.CheckCircle, null, tint = Sage, modifier = Modifier.size(72.dp)); Text("Item reserved!", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold); Text(booking.product.name, fontWeight = FontWeight.Bold); Text("Pickup code: ${booking.pickupCode}", color = Wine, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold); Text("${booking.product.address}, ${booking.product.dzongkhag}", color = Color.Gray, textAlign = TextAlign.Center); Spacer(Modifier.height(18.dp)); Button(view, Modifier.fillMaxWidth()) { Text("View my bookings") }; OutlinedButton(shop, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Continue shopping") } }
}

@Composable
private fun BookingsScreen(bookings: List<Booking>, explore: () -> Unit, review: (Booking) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("My bookings", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold) }
        if (bookings.isEmpty()) item { Button(explore) { Text("No bookings yet — explore products") } }
        items(bookings, key = { it.pickupCode }) { b -> Card(colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(14.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { ProductArtwork(b.product, Modifier.size(72.dp), 28); Column(Modifier.padding(start = 12.dp).weight(1f)) { Text(b.product.name, fontWeight = FontWeight.Bold); Text("Qty ${b.quantity} • ${b.status}", color = Wine); Text("Pickup: ${b.pickupCode}", fontSize = 12.sp) } }; if (b.status == "Collected") Button({ review(b) }, Modifier.fillMaxWidth().padding(top = 8.dp)) { Icon(Icons.Outlined.RateReview, null); Text("  Review purchase") } } } }
    }
}

@Composable
private fun ReviewScreen(booking: Booking, customerName: String, back: () -> Unit, submit: (Int, Int, String, String) -> Unit, onError: (String) -> Unit) {
    var productRating by remember { mutableIntStateOf(0) }; var businessRating by remember { mutableIntStateOf(0) }; var comment by remember { mutableStateOf("") }; var image by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { image = it }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(back) { Icon(Icons.Default.ArrowBack, null) }; Text("Review your purchase", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold) } }
        item { Text(booking.product.name, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("Verified pickup: ${booking.pickupCode}", color = Sage, fontSize = 12.sp) }
        item { Text("Product rating", fontWeight = FontWeight.Bold); StarSelector(productRating) { productRating = it } }
        item { Text("Business rating", fontWeight = FontWeight.Bold); StarSelector(businessRating) { businessRating = it } }
        item { FormField("Review", comment, { comment = it }, "Share your experience", minLines = 4) }
        item { OutlinedButton({ picker.launch("image/*") }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.AddPhotoAlternate, null); Text(if (image == null) "  Add optional review photo" else "  Review photo selected ✓") } }
        item { Button({ if (productRating == 0 || businessRating == 0) { onError("Choose both star ratings."); return@Button }; if (image != null) TshongLaStorage.saveReviewImage(booking.customerId, booking.product.id, image!!, { submit(productRating, businessRating, comment, it) }, onError) else submit(productRating, businessRating, comment, "") }, Modifier.fillMaxWidth().height(54.dp)) { Text("Submit verified review") } }
    }
}

@Composable
private fun StarSelector(value: Int, change: (Int) -> Unit) { Row { (1..5).forEach { i -> IconButton({ change(i) }) { Icon(if (i <= value) Icons.Filled.Star else Icons.Outlined.StarBorder, null, tint = Gold) } } } }

@Composable
private fun ShopScreen(products: List<Product>, bookings: List<Booking>, shopName: String, verified: Boolean, openReservations: () -> Unit, verify: () -> Unit, addProduct: () -> Unit, editProduct: (Product) -> Unit, deleteProduct: (Product) -> Unit) {
    var deleteTarget by remember { mutableStateOf<Product?>(null) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("My Shop", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold); Text(shopName, color = Wine) }; Button(addProduct) { Icon(Icons.Default.Add, null); Text(" Product") } } }
        item { Card(onClick = verify, colors = CardDefaults.cardColors(containerColor = if (verified) Color(0xFFEAF5EA) else SoftRose)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (verified) Icons.Default.Verified else Icons.Outlined.VerifiedUser, null, tint = if (verified) Sage else Wine); Column(Modifier.padding(start = 12.dp)) { Text(if (verified) "Verification submitted ✓" else "Verification required", fontWeight = FontWeight.Bold); Text(if (verified) "Phone and CID details are on file." else "Verify phone and CID before publishing products.", fontSize = 12.sp, color = Color.Gray) } } } }
        item { Card(onClick = openReservations, colors = CardDefaults.cardColors(containerColor = SoftRose)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.ReceiptLong, null, tint = Wine); Text("  ${bookings.count { it.status == "Reserved" || it.status == "Ready for pickup" }} active reservations", modifier = Modifier.weight(1f)); Text("View", color = Wine) } } }
        item { Text("Your products", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        items(products, key = { it.id }) { p -> Card(colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(12.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { ProductArtwork(p, Modifier.size(82.dp), 30); Column(Modifier.padding(start = 12.dp).weight(1f)) { Text(p.name, fontWeight = FontWeight.Bold); Text("Nu. ${p.discountedPrice}", color = Wine); Text("Stock ${p.stock} • ${p.discountPercent}% discount", fontSize = 12.sp) } }; Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ editProduct(p) }, Modifier.weight(1f)) { Text("Edit") }; OutlinedButton({ deleteTarget = p }, Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") } } } } }
    }
    deleteTarget?.let { p -> AlertDialog({ deleteTarget = null }, title = { Text("Delete product?") }, text = { Text(p.name) }, confirmButton = { TextButton({ deleteProduct(p); deleteTarget = null }) { Text("Delete") } }, dismissButton = { TextButton({ deleteTarget = null }) { Text("Cancel") } }) }
}

@Composable
private fun SellerReservationsScreen(bookings: List<Booking>, back: () -> Unit, update: (Booking, String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(back) { Icon(Icons.Default.ArrowBack, null) }; Text("Reservations", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold) } }
        items(bookings, key = { it.pickupCode }) { b -> Card(colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(14.dp)) { Text(b.product.name, fontWeight = FontWeight.Bold); Text("Qty ${b.quantity} • ${b.pickupCode}", color = Wine); Text(b.status, fontWeight = FontWeight.SemiBold); when (b.status) { "Reserved" -> { Button({ update(b, "Ready for pickup") }, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Mark ready for pickup") }; TextButton({ update(b, "Cancelled") }, Modifier.fillMaxWidth()) { Text("Cancel") } }; "Ready for pickup" -> Button({ update(b, "Collected") }, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Mark as collected") } } } } }
    }
}

@Composable
private fun ProductFormScreen(title: String, initial: Product?, nextId: Int, sellerId: String, sellerName: String, defaultDzongkhag: String, defaultAddress: String, save: (Product, Uri?) -> Unit, back: () -> Unit) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }; var price by remember(initial) { mutableStateOf(initial?.price?.toString() ?: "") }; var stock by remember(initial) { mutableStateOf(initial?.stock?.toString() ?: "") }; var discount by remember(initial) { mutableStateOf(initial?.discountPercent?.toString() ?: "0") }
    var description by remember(initial) { mutableStateOf(initial?.description ?: "") }; var category by remember(initial) { mutableStateOf(initial?.category ?: "Crafts") }; var dzongkhag by remember(initial) { mutableStateOf(initial?.dzongkhag ?: defaultDzongkhag) }; var address by remember(initial) { mutableStateOf(initial?.address ?: defaultAddress) }; var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }; var locationExpanded by remember { mutableStateOf(false) }; var error by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedImage = it }
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(back) { Icon(Icons.Default.ArrowBack, null) }; Text(title, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold) } }
        item { Card(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth().height(190.dp), colors = CardDefaults.cardColors(containerColor = SoftRose)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { val preview: Any? = selectedImage ?: initial?.imageUrl?.takeIf { it.isNotBlank() }; if (preview != null) AsyncImage(preview, "Product image", Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Outlined.AddPhotoAlternate, null, tint = Wine, modifier = Modifier.size(42.dp)); Text("Add product photo", fontWeight = FontWeight.Bold); Text("Stored locally for this free-tier demo", color = Color.Gray, fontSize = 11.sp) } } } }
        item { FormField("Product name", name, { name = it }, "e.g. Handmade basket") }
        item { ExposedDropdownMenuBox(categoryExpanded, { categoryExpanded = it }) { OutlinedTextField(category, {}, readOnly = true, label = { Text("Category") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()); ExposedDropdownMenu(categoryExpanded, { categoryExpanded = false }) { listOf("Textiles", "Crafts", "Home", "Food", "Wellness").forEach { c -> DropdownMenuItem({ Text(c) }, { category = c; categoryExpanded = false }) } } } }
        item { FormField("Regular price (Nu.)", price, { price = it.filter(Char::isDigit) }, "0", KeyboardType.Number) }
        item { FormField("Available stock", stock, { stock = it.filter(Char::isDigit) }, "10", KeyboardType.Number) }
        item { FormField("Discount (%)", discount, { discount = it.filter(Char::isDigit).take(2) }, "0", KeyboardType.Number) }
        item { ExposedDropdownMenuBox(locationExpanded, { locationExpanded = it }) { OutlinedTextField(dzongkhag, {}, readOnly = true, label = { Text("Dzongkhag") }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(locationExpanded) }); ExposedDropdownMenu(locationExpanded, { locationExpanded = false }) { dzongkhags.forEach { d -> DropdownMenuItem({ Text(d) }, { dzongkhag = d; locationExpanded = false }) } } } }
        item { FormField("Pickup address", address, { address = it }, "Town, street or landmark", minLines = 2) }
        item { FormField("Description", description, { description = it }, "Describe the product", minLines = 4) }
        item { Button({ val p = price.toIntOrNull(); val s = stock.toIntOrNull(); val d = discount.toIntOrNull(); error = when { name.isBlank() || p == null || s == null || address.isBlank() -> "Complete all required fields."; d == null || d !in 0..90 -> "Discount must be between 0 and 90%."; else -> "" }; if (error.isBlank()) save(Product(initial?.id ?: nextId, name.trim(), category, p!!, initial?.rating ?: 0.0, sellerName, sellerId, dzongkhag, address.trim(), s!!, d!!, initial?.symbol ?: "🎁", initial?.colors ?: listOf(Color(0xFFB95C4B), Color(0xFFE9A64A)), description.trim(), initial?.imageUrl ?: ""), selectedImage) }, Modifier.fillMaxWidth().height(54.dp)) { Text(if (initial == null) "Register product" else "Save changes", fontWeight = FontWeight.Bold) }; if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
    }
}

@Composable
private fun FormField(label: String, value: String, change: (String) -> Unit, placeholder: String, type: KeyboardType = KeyboardType.Text, minLines: Int = 1) {
    OutlinedTextField(value, change, Modifier.fillMaxWidth(), label = { Text(label) }, placeholder = { Text(placeholder) }, keyboardOptions = KeyboardOptions(keyboardType = type), minLines = minLines)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RolePreview() { TshongLaTheme { RoleSelectionScreen({}, {}) } }
