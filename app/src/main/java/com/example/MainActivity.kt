package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.generator.AppBuilderGenerator
import com.example.data.model.Project
import com.example.data.repository.AiProviderManager
import com.example.data.repository.AiStudioRepository
import com.example.data.repository.AuthRepository
import com.example.ui.components.StudioBottomBar
import com.example.ui.components.StudioScreen
import com.example.ui.screens.AiModelsScreen
import com.example.ui.screens.AuthGateScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CreateAppScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.theme.AiCreatorStudioTheme
import com.example.ui.theme.DarkBackground
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {

    private lateinit var authRepository: AuthRepository
    private lateinit var aiProviderManager: AiProviderManager
    private lateinit var studioRepository: AiStudioRepository
    private lateinit var appBuilderGenerator: AppBuilderGenerator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch intent before UI/Firebase
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        authRepository = AuthRepository(this, lifecycleScope)
        aiProviderManager = AiProviderManager(this)
        studioRepository = AiStudioRepository(this)
        appBuilderGenerator = AppBuilderGenerator(this, aiProviderManager)

        // Attempt silent auto-sign-in on startup
        authRepository.attemptSilentAutoSignIn()

        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }

            AiCreatorStudioTheme(darkTheme = isDarkTheme) {
                val currentUser by authRepository.currentUser.collectAsState()

                if (currentUser == null) {
                    AuthGateScreen(authRepository = authRepository)
                } else {
                    MainAppLayout(
                        authRepository = authRepository,
                        aiProviderManager = aiProviderManager,
                        studioRepository = studioRepository,
                        appBuilderGenerator = appBuilderGenerator,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = it }
                    )
                }
            }
        }
    }
}

@Composable
fun MainAppLayout(
    authRepository: AuthRepository,
    aiProviderManager: AiProviderManager,
    studioRepository: AiStudioRepository,
    appBuilderGenerator: AppBuilderGenerator,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit
) {
    var currentScreen by remember { mutableStateOf(StudioScreen.HOME) }
    var createPrompt by remember { mutableStateOf("") }
    var createPlatform by remember { mutableStateOf("android") }
    var activeInspectProject by remember { mutableStateOf<Project?>(null) }

    // Custom back handler to return to Home or clear inspected project
    BackHandler(enabled = currentScreen != StudioScreen.HOME || activeInspectProject != null) {
        if (activeInspectProject != null) {
            activeInspectProject = null
        } else {
            currentScreen = StudioScreen.HOME
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            StudioBottomBar(
                currentScreen = currentScreen,
                onNavigate = { screen ->
                    if (screen != currentScreen) {
                        activeInspectProject = null
                        currentScreen = screen
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            when (currentScreen) {
                StudioScreen.HOME -> {
                    HomeScreen(
                        authRepository = authRepository,
                        aiProviderManager = aiProviderManager,
                        studioRepository = studioRepository,
                        onNavigate = { currentScreen = it },
                        onOpenCreateWithPrompt = { prompt, platform ->
                            createPrompt = prompt
                            createPlatform = platform
                            currentScreen = StudioScreen.CREATE
                        },
                        onOpenProject = { project ->
                            activeInspectProject = project
                            currentScreen = StudioScreen.PROJECTS
                        }
                    )
                }
                StudioScreen.MODELS -> {
                    AiModelsScreen(
                        aiProviderManager = aiProviderManager
                    )
                }
                StudioScreen.CREATE -> {
                    CreateAppScreen(
                        appBuilderGenerator = appBuilderGenerator,
                        studioRepository = studioRepository,
                        aiProviderManager = aiProviderManager,
                        initialPrompt = createPrompt,
                        initialPlatform = createPlatform,
                        onProjectSaved = { saved ->
                            activeInspectProject = saved
                            currentScreen = StudioScreen.PROJECTS
                        }
                    )
                }
                StudioScreen.PROJECTS -> {
                    ProjectsScreen(
                        studioRepository = studioRepository,
                        appBuilderGenerator = appBuilderGenerator,
                        activeProject = activeInspectProject,
                        onCloseProject = { activeInspectProject = null }
                    )
                }
                StudioScreen.PROFILE -> {
                    ProfileScreen(
                        authRepository = authRepository,
                        aiProviderManager = aiProviderManager,
                        studioRepository = studioRepository,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = onToggleTheme
                    )
                }
            }
        }
    }
}
