# Development Guide - Pune City Utility (Expense Tracker & AI)

## Quick Start

### Prerequisites
- Android Studio 2024.1 or later
- Java 17 or higher
- Gradle 8.7 or higher
- Android SDK 24 (API 24) minimum

### Setup
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Copy `secrets.defaults.properties` to `secrets.properties` and add your keys:
   - `OPENROUTER_API_KEY` or `CLAUDE_API_KEY`
   - `SUPABASE_URL` and `SUPABASE_ANON_KEY`
5. Run on emulator or physical device

## Project Structure

```
app/
├── src/main/
│   ├── java/com/pranav/punecityguide/
│   │   ├── data/
│   │   │   ├── database/           # Room (ExpenseDao, AiChatDao, SyncAuditDao)
│   │   │   ├── model/              # Data classes (Expense, AiMessage, SyncAuditLog)
│   │   │   ├── repository/         # Data access (AiChatRepository, SyncAuditRepository)
│   │   │   └── service/            # Business (SupabaseClient, AiTokenQuotaService)
│   │   ├── ui/
│   │   │   ├── components/         # Premium Glassmorphic UI & Overlays
│   │   │   ├── navigation/         # Navigation routes (Screen.kt)
│   │   │   ├── screens/            # Screens (HomeScreen, ExpenseCalculatorScreen, CityGuideScreen)
│   │   │   ├── theme/              # Pune-inspired Material 3 theme
│   │   │   └── viewmodel/          # ViewModels (ExpenseViewModel, ChatbotViewModel)
│   │   ├── MainActivity.kt         # App entry point & NavHost
│   │   └── AppConfig.kt            # Configuration constants
│   └── res/
│       ├── values/                 # Strings, Colors, Themes
```

## Core Modules

### 1. Expense Management
- Uses **Room Database** for local offline storage.
- UI: `ExpenseCalculatorScreen.kt` for detailed tracking.
- Logic: `ExpenseViewModel.kt` handles calculations and state.

### 2. AI Pune Assistant
- Powered by **OpenRouter/Claude** via `RealtimeChatService.kt`.
- Features context-aware responses about Pune city utilities and taxes.
- Database: local persistence of chat history in `ai_conversations`.

### 3. Rickshaw Fare Guide
- Utility for calculating Rickshaw fares based on **Official Pune RTO Rates (Effective Feb 1, 2025)**.
- Logic: `GuideViewModel.kt`.

## Adding a New Feature

1.  **Define Route**: Add to `Screen.kt`.
2.  **Create Screen**: Use Premium UI components from `ui/components/`.
3.  **ViewModel**: Follow the MVVM pattern. Use `MutableStateFlow` for UI state.
4.  **Database**: If new tables are needed, update `PuneCityDatabase.kt` and increment the version.
5.  **Audit**: Log critical events using `SyncAuditRepository.log()`.

## Building for Release

### Command Line Build
```bash
./gradlew bundleRelease
```

## Performance Tips

1.  **Lazy Lists**: Use `LazyColumn` for expense history.
2.  **Composition**: Keep Composables pure and avoid heavy logic inside `@Composable` functions.
3.  **Database**: Use `Flow` to observe database changes automatically.

## Dependencies

- `androidx.room:*`: Local persistent storage.
- `io.ktor:ktor-client-*`: Networking for AI and Supabase.
- `org.jetbrains.kotlinx:kotlinx-serialization-json`: Type-safe JSON.
- `androidx.navigation:navigation-compose`: Jetpack Navigation.

## Resources

- [Jetpack Compose Documentation](https://developer.android.com/develop/ui/compose)
- [Room Database Guide](https://developer.android.com/training/data-storage/room)
- [Material Design 3](https://m3.material.io/)
- [Supabase Documentation](https://supabase.com/docs)
