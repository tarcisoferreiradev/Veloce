package com.veloce.presentation.auth

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.veloce.domain.model.Gender
import com.veloce.domain.model.MeasurementUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AuthScreen(
    state: AuthUiState,
    onLogin: (String, String) -> Unit,
    onGoToRegister: () -> Unit,
    onGoToLogin: () -> Unit,
    onGoToWelcome: () -> Unit,
    onNextStep: (String, String, String, String) -> Unit,
    onCompleteRegistration: (String, Gender, Double, Int, MeasurementUnit) -> Unit,
    onCompleteGoogleRegistration: (String, Gender, Double, Int, MeasurementUnit) -> Unit,
    onGoogleAuth: () -> Unit,
    onClearError: () -> Unit,
    onAuthenticated: () -> Unit
) {
    LaunchedEffect(state.screen) {
        if (state.screen is AuthScreenState.Authenticated) {
            onAuthenticated()
        }
    }

    Scaffold(
        snackbarHost = {
            if (state.error != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = onClearError) { Text("OK") }
                    }
                ) {
                    Text(state.error)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.screen is AuthScreenState.Welcome) {
                WelcomeContent(onGoToRegister, onGoToLogin)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (val screen = state.screen) {
                        is AuthScreenState.CheckingToken -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                        is AuthScreenState.Login -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AuroraBackground()
                                LoginContent(onLogin, onGoToRegister, onGoogleAuth, onGoToWelcome, state.isLoading)
                            }
                        }
                        is AuthScreenState.RegisterStep1 -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AuroraBackground()
                                RegisterStep1Content(screen, onNextStep, onGoToLogin, onGoogleAuth, onGoToWelcome, state.isLoading)
                            }
                        }
                        is AuthScreenState.RegisterStep2 -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AuroraBackground()
                                Box(modifier = Modifier.padding(24.dp).fillMaxSize(), contentAlignment = Alignment.Center) {
                                    RegisterStep2Content(screen, onCompleteRegistration, onGoToWelcome, state.isLoading)
                                }
                            }
                        }
                        is AuthScreenState.GoogleRegisterStep2 -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AuroraBackground()
                                Box(modifier = Modifier.padding(24.dp).fillMaxSize(), contentAlignment = Alignment.Center) {
                                    GoogleRegisterStep2Content(screen, onCompleteGoogleRegistration, onGoToWelcome, state.isLoading)
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun AuroraBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101611))
    ) {
        // Glowing green aurora blob top-left
        Box(
            modifier = Modifier
                .size(340.dp)
                .offset(x = (-90).dp, y = (-60).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFB8FB07).copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Glowing green aurora blob bottom-right
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 120.dp, y = 120.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFB8FB07).copy(alpha = 0.11f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
fun FullscreenBackgroundVideo(
    videoResId: Int,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            var storedWidth = 0
            var storedHeight = 0

            object : VideoView(context) {
                override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                    val width = MeasureSpec.getSize(widthMeasureSpec)
                    val height = MeasureSpec.getSize(heightMeasureSpec)

                    val vWidth = if (storedWidth > 0) storedWidth else width
                    val vHeight = if (storedHeight > 0) storedHeight else height

                    val viewRatio = width.toFloat() / height.toFloat()
                    val videoRatio = vWidth.toFloat() / vHeight.toFloat()

                    var newWidth = width
                    var newHeight = height

                    if (videoRatio > viewRatio) {
                        newWidth = (height.toFloat() * videoRatio).toInt()
                    } else {
                        newHeight = (width.toFloat() / videoRatio).toInt()
                    }

                    setMeasuredDimension(newWidth, newHeight)
                }
            }.apply {
                val uri = Uri.parse("android.resource://${context.packageName}/$videoResId")
                setVideoURI(uri)
                setOnPreparedListener { mediaPlayer ->
                    storedWidth = mediaPlayer.videoWidth
                    storedHeight = mediaPlayer.videoHeight
                    mediaPlayer.isLooping = true
                    mediaPlayer.setVolume(0f, 0f)
                    start()
                }
            }
        },
        update = { videoView ->
            if (!videoView.isPlaying) {
                videoView.start()
            }
        }
    )
}

@Composable
fun WelcomeContent(
    onGoToRegister: () -> Unit,
    onGoToLogin: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Video
        FullscreenBackgroundVideo(
            videoResId = com.veloce.R.raw.background_intro,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient Dark Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.65f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Logo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 50.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = com.veloce.R.drawable.velocelogo),
                contentDescription = "Logo Veloce",
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(160.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }

        // Bottom Actions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val registerInteractionSource = remember { MutableInteractionSource() }
            val registerPressState by registerInteractionSource.collectIsPressedAsState()
            val registerScale by animateFloatAsState(
                targetValue = if (registerPressState) 0.95f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "register_scale"
            )

            Button(
                onClick = onGoToRegister,
                interactionSource = registerInteractionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer(scaleX = registerScale, scaleY = registerScale)
            ) {
                Text(
                    text = "Começar gratuitamente",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Click area restricted strictly to the text "Fazer login"
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onGoToLogin
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Fazer login",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun AuthHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.width(8.dp))
        Image(
            painter = painterResource(id = com.veloce.R.drawable.velocelogo),
            contentDescription = "Veloce",
            modifier = Modifier.height(28.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    }
}

@Composable
fun TermsAndPrivacyText(modifier: Modifier = Modifier) {
    val annotatedString = buildAnnotatedString {
        append("Ao continuar, você concorda com nossos ")
        pushStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold))
        append("Termos de Serviço")
        pop()
        append(" e a ")
        pushStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold))
        append("Política de Privacidade")
        pop()
        append(".")
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodySmall,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp)
    )
}

@Composable
fun LoginContent(
    onLogin: (String, String) -> Unit,
    onGoToRegister: () -> Unit,
    onGoogleAuth: () -> Unit,
    onGoToWelcome: () -> Unit,
    isLoading: Boolean
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back arrow at the very top left
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopStart
        ) {
            IconButton(
                onClick = onGoToWelcome,
                enabled = !isLoading,
                modifier = Modifier.offset(x = (-12).dp, y = (-8).dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Fazer o login com o Veloce",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        var passwordVisible by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha") },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    val description = if (passwordVisible) "Ocultar senha" else "Mostrar senha"
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = image,
                            contentDescription = description,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(onClick = { /* TODO: Forgot password */ }, enabled = !isLoading) {
                    Text(
                        text = "Esqueceu sua senha?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Button(
            onClick = { onLogin(email, password) },
            enabled = !isLoading,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Entrar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        OrDivider()

        GoogleSignInButton(onClick = onGoogleAuth)

        Spacer(modifier = Modifier.height(4.dp))

        TermsAndPrivacyText()
    }
}

@Composable
fun RegisterStep1Content(
    screen: AuthScreenState.RegisterStep1,
    onNextStep: (String, String, String, String) -> Unit,
    onGoToLogin: () -> Unit,
    onGoogleAuth: () -> Unit,
    onGoToWelcome: () -> Unit,
    isLoading: Boolean
) {
    var name by remember { mutableStateOf(screen.name) }
    var email by remember { mutableStateOf(screen.email) }
    var password by remember { mutableStateOf(screen.password) }
    var confirm by remember { mutableStateOf(screen.confirm) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back arrow at the very top left
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopStart
        ) {
            IconButton(
                onClick = onGoToWelcome,
                enabled = !isLoading,
                modifier = Modifier.offset(x = (-12).dp, y = (-18).dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        AuthHeader(title = "Crie sua conta no")

        Spacer(modifier = Modifier.height(12.dp))

        GoogleSignInButton(onClick = onGoogleAuth)

        Spacer(modifier = Modifier.height(4.dp))

        TermsAndPrivacyText()

        OrDivider()

        OutlinedTextField(value = name, onValueChange = { name = it }, enabled = !isLoading, label = { Text("Nome Completo") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, enabled = !isLoading, label = { Text("E-mail") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = password, onValueChange = { password = it }, enabled = !isLoading, label = { Text("Senha") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = confirm, onValueChange = { confirm = it }, enabled = !isLoading, label = { Text("Confirmar Senha") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())

        Button(
            onClick = { onNextStep(name, email, password, confirm) },
            enabled = !isLoading,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Próxima Etapa")
        }

        TextButton(onClick = onGoToLogin, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
            Text("Já tem uma conta? Entrar")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterStep2Content(
    screen: AuthScreenState.RegisterStep2,
    onComplete: (String, Gender, Double, Int, MeasurementUnit) -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean
) {
    var birthDate by remember { mutableStateOf(screen.birthDate) }
    var gender by remember { mutableStateOf(screen.gender) }
    var weightStr by remember { mutableStateOf(if (screen.weight > 0) screen.weight.toString() else "") }
    var heightStr by remember { mutableStateOf(if (screen.height > 0) screen.height.toString() else "") }
    var unit by remember { mutableStateOf(screen.unit) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        birthDate = sdf.format(Date(millis))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text("Criar Conta", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Etapa 2 de 2: Métricas Biométricas")
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = birthDate,
            onValueChange = {},
            enabled = !isLoading,
            label = { Text("Data de Nascimento (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                TextButton(onClick = { showDatePicker = true }, enabled = !isLoading) {
                    Text("Selecionar")
                }
            }
        )

        Text("Gênero Biológico", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Gender.entries.forEach { g ->
                FilterChip(
                    selected = gender == g,
                    onClick = { gender = g },
                    label = { Text(g.name) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = weightStr,
                onValueChange = { weightStr = it },
                enabled = !isLoading,
                label = { Text("Peso (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = heightStr,
                onValueChange = { heightStr = it },
                enabled = !isLoading,
                label = { Text("Altura (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Text("Unidade de Medida", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MeasurementUnit.entries.forEach { u ->
                FilterChip(
                    selected = unit == u,
                    onClick = { unit = u },
                    label = { Text(u.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val weight = weightStr.toDoubleOrNull() ?: 0.0
                val height = heightStr.toIntOrNull() ?: 0
                onComplete(birthDate, gender, weight, height, unit)
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Concluir Cadastro")
            }
        }

        TextButton(onClick = onBack, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
            Text("Voltar")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleRegisterStep2Content(
    screen: AuthScreenState.GoogleRegisterStep2,
    onComplete: (String, Gender, Double, Int, MeasurementUnit) -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean
) {
    var birthDate by remember { mutableStateOf(screen.birthDate) }
    var gender by remember { mutableStateOf(screen.gender) }
    var weightStr by remember { mutableStateOf(if (screen.weight > 0) screen.weight.toString() else "") }
    var heightStr by remember { mutableStateOf(if (screen.height > 0) screen.height.toString() else "") }
    var unitState by remember { mutableStateOf(screen.unit) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        birthDate = sdf.format(Date(millis))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text("Concluir com Google", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Olá, ${screen.name}! Precisamos de algumas métricas para completar seu perfil.")
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = birthDate,
            onValueChange = {},
            enabled = !isLoading,
            label = { Text("Data de Nascimento (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                TextButton(onClick = { showDatePicker = true }, enabled = !isLoading) {
                    Text("Selecionar")
                }
            }
        )

        Text("Gênero Biológico", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Gender.entries.forEach { g ->
                ItemFilterChip(
                    selected = gender == g,
                    onClick = { gender = g },
                    label = g.name
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = weightStr,
                onValueChange = { weightStr = it },
                enabled = !isLoading,
                label = { Text("Peso (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = heightStr,
                onValueChange = { heightStr = it },
                enabled = !isLoading,
                label = { Text("Altura (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Text("Unidade de Medida", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MeasurementUnit.entries.forEach { u ->
                ItemFilterChip(
                    selected = unitState == u,
                    onClick = { unitState = u },
                    label = u.name
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val weight = weightStr.toDoubleOrNull() ?: 0.0
                val height = heightStr.toIntOrNull() ?: 0
                onComplete(birthDate, gender, weight, height, unitState)
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Concluir Cadastro")
            }
        }

        TextButton(onClick = onBack, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
            Text("Voltar")
        }
    }
}

@Composable
fun ItemFilterChip(selected: Boolean, onClick: () -> Unit, label: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White,
            containerColor = Color.Transparent
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = com.veloce.R.drawable.google),
                contentDescription = "Google Logo",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Continuar com o Google",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
fun OrDivider(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "ou",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }
    }
}
