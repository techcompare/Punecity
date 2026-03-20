# Development Guide - Pune City Guide

## Quick Start

### Prerequisites
- Android Studio 2024.1 or later
- Java 11 or higher
- Gradle 8.7 or higher
- Android SDK 24 (API 24) minimum

### Setup
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Run on emulator or physical device

## Project Structure

```
app/
├── src/main/
│   ├── AndroidManifest.xml         # App manifest with permissions
│   ├── java/com/pranav/punecityguide/
│   │   ├── data/
│   │   │   ├── database/           # Room database setup
│   │   │   ├── model/              # Data classes
│   │   │   ├── repository/         # Data access layer
│   │   │   └── service/            # Business logic
│   │   ├── ui/
│   │   │   ├── components/         # Reusable UI components
│   │   │   ├── navigation/         # Navigation routes
│   │   │   ├── screens/            # Screen implementations
│   │   │   ├── theme/              # UI theming
│   │   │   ├── utils/              # UI utilities
│   │   │   └── viewmodel/          # MVVM ViewModels
│   │   ├── MainActivity.kt         # App entry point
│   │   └── AppConfig.kt            # Configuration constants
│   └── res/
│       ├── drawable/               # Drawables and vectors
│       ├── mipmap-*/               # App icons
│       ├── values/                 # Resources (strings, colors, themes)
│       └── xml/                    # XML resources
├── build.gradle.kts                # Module build config
└── proguard-rules.pro             # ProGuard rules for release builds
```

## Adding a New Attraction

1. Create an Attraction instance in `SampleDataService.kt`:
```kotlin
Attraction(
    name = "Your Attraction",
    description = "Description",
    imageUrl = "https://...",
    category = "Category",
    rating = 4.5f,
    reviewCount = 100,
    nativeLanguageName = "नाव मराठी मध्ये",
    bestTimeToVisit = "October to March",
    entryFee = "₹100",
    openingHours = "9:00 AM - 6:00 PM",
    latitude = 18.5204,
    longitude = 73.8567
)
```

2. Repository will handle database operations automatically

## Adding a New Screen

1. Create a new `YourScreen.kt` in `ui/screens/`
2. Add the screen route to `Navigation.kt`
3. Add navigation parameter if needed
4. Create corresponding ViewModel if state management needed
5. Update MainActivity's NavHost to include the new route

## State Management

ViewModels follow MVVM pattern:
```kotlin
// Define UI State
data class YourUiState(
    val isLoading: Boolean = false,
    val data: List<T> = emptyList(),
    val error: String? = null
)

// Create ViewModel
class YourViewModel(private val repository: YourRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(YourUiState())
    val uiState: StateFlow<YourUiState> = _uiState.asStateFlow()
}
```

## Database Operations

All database operations are handled through Repository:
```kotlin
// Read
repository.getTopAttractions(10).collect { attractions ->
    // Handle data
}

// Write
viewModelScope.launch {
    repository.addAttraction(attraction)
}
```

## Theming

App uses Material Design 3 with Pune-inspired colors. Colors are defined in:
- `Color.kt` - Compose color definitions
- `values/colors.xml` - XML color resources
- `Theme.kt` - Material theme configuration

## Animations

Common animations are in `AnimationUtils.kt`:
- Fade in/out
- Slide transitions
- Expand/collapse

## Testing

### Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

## Building for Release

### Generate Signed APK
1. Go to Build → Generate Signed APK
2. Select or create keystore
3. Choose release build type
4. APK will be generated in `app/release/`

### Command Line Build
```bash
./gradlew bundleRelease
```

## Performance Tips

1. Use `LazyColumn` for long lists
2. Avoid expensive operations in Composables
3. Use `.collectAsState()` for Flow collection
4. Implement proper caching strategies
5. Monitor composition recompositions

## Common Customizations

### Changing Colors
Update in `Color.kt` and `Theme.kt`:
```kotlin
val PrimaryColor = Color(0xFFYourColor)
```

### Adding New Categories
Update `SampleDataService.kt` and `CategoryScreen.kt`

### Changing App Name
Update in `strings.xml`:
```xml
<string name="app_name">Your App Name</string>
```

## Debugging

### Enable Verbose Logging
Add to `build.gradle.kts`:
```kotlin
buildTypes {
    debug {
        debuggable = true
    }
}
```

### Inspect Compose Layout
Use Compose Layout Inspector in Android Studio

## Dependencies

Core dependencies:
- `androidx.compose.*` - UI framework
- `androidx.room:*` - Database
- `androidx.navigation:navigation-compose` - Navigation
- `io.coil-kt.coil3:coil-compose` - Image loading
- `org.jetbrains.kotlinx:kotlinx-serialization-json` - Serialization

## Code Style & Standards

- Use Kotlin idioms and conventions
- Organize imports alphabetically
- Use meaningful variable names
- Add documentation for public APIs
- Keep functions small and focused
- Use sealed classes for type-safe routing

## Contributing

1. Create feature branch from main
2. Make changes
3. Test thoroughly
4. Submit pull request with description

## Resources

- [Jetpack Compose Documentation](https://developer.android.com/develop/ui/compose)
- [Room Database Guide](https://developer.android.com/training/data-storage/room)
- [Navigation Component](https://developer.android.com/guide/navigation)
- [Material Design 3](https://m3.material.io/)

## Support

For issues or questions, contact the development team.
