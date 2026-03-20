# Pune City Guide - Android App

A modern, feature-rich Android application that showcases the best attractions, historical sites, temples, and cultural landmarks of Pune, India.

## 🏆 Why This App? (Unique Features)

Unlike generic map applications, Pune City Guide is **hand-crafted for Punekars and visitors**, focusing on experiences rather than just locations.

### 🌟 Exclusive Local Features
- **🎓 Student Hangouts**: A dedicated section for budget-friendly spots, cafes, and "katta" locations perfect for students.
- **🤫 Secret Spots**: Discover underrated views and hidden gems (like Vetal Tekdi at sunrise) that aren't swarming with tourists.
- **💬 Community Buzz**: A live "Pune Buzz" feed where locals share real-time updates—monsoon tips, new food joints, and traffic hacks.
- **🤖 AI Scan to Plan**: Point your camera at a ticket or landmark to instantly recognize it and add it to your itinerary.

### ⚡ Core Features
- **Beautiful UI with Material Design 3**: Modern, clean interface with vibrant Pune-inspired color scheme
- **Offline First**: Works without internet in remote areas (forts/hills) using local Room database.
- **Curated Categories**: 
  - **Historical**: Aga Khan Palace, Shaniwar Wada
  - **Spiritual**: Osho Meditation Resort, Dagdusheth
  - **Nature**: Parvati Hill, Pune Okayama Friendship Garden
  - **Student Specials**: Goodluck Cafe, FC Road
- **Detailed Information**: 
  - Local language names (in Marathi)
  - "Best time to visit" recommendations
  - Entry fees and opening hours

## 🛠️ Architecture

### Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Database**: Room (SQLite)
- **Navigation**: Compose Navigation
- **Image Loading**: Coil 3
- **Serialization**: Kotlinx Serialization
- **Architecture Pattern**: MVVM with Repository Pattern

### Project Structure
```
src/main/java/com/pranav/punecityguide/
├── data/
│   ├── database/       # Room Database entities and DAOs
│   ├── model/          # Data models
│   ├── repository/     # Repository pattern implementation
│   └── service/        # Sample data service
├── ui/
│   ├── components/     # Reusable UI components
│   ├── navigation/     # Navigation routes and handling
│   ├── screens/        # Screen composables (Home, Detail, Category, Search)
│   ├── theme/          # Theme, colors, and typography
│   ├── utils/          # UI utilities and animation helpers
│   └── viewmodel/      # ViewModels for state management
└── MainActivity.kt     # Main activity with navigation setup
```

## 📱 Screens

### Home Screen
- Welcome header banner
- Top attractions grid
- Category chips for quick navigation
- Search functionality access

### Details Screen
- Hero image with gradient overlay
- Attraction title and rating
- Detailed description
- Key information (hours, fees, best time to visit)
- Location details
- Local language name

### Category Screen
- Filtered attractions by category
- Scrollable list of related attractions
- Quick access to attraction details

### Search Screen
- Search bar for finding attractions
- Real-time search results

## 🎨 Design Features

- **Color Scheme**: Pune-inspired warm colors (Maroon primary, Gold secondary)
- **Typography**: Clear hierarchy with Material Design 3
- **Animations**: Smooth transitions and fade effects
- **Cards**: Elevated cards with rounded corners for visual appeal
- **Dark Mode**: Full support for dark theme
- **Accessibility**: Proper contrast ratios and touch targets

## 🏗️ Build & Deployment

### Build Configuration
- Minimum API Level: 24 (Android 7.0)
- Target API Level: 36 (Android 15)
- ProGuard enabled for release builds
- Resource shrinking enabled

### Release Build
```bash
./gradlew assembleRelease
```

### Debug Build
```bash
./gradlew assembleDebug
```

## 📦 Dependencies

- Androidx Core, Activity, Compose, Navigation
- Room Database
- Coil Image Loading
- Kotlinx Serialization
- Material Design 3

## 🚀 Performance Optimizations

- Lazy loading for lists
- Image caching with Coil
- Database indexing for queries
- Minification and resource shrinking in release builds
- Efficient recomposition in Compose

## 📋 Content

The app includes 10+ curated attractions across multiple categories:
- **Historical**: Aga Khan Palace, Shaniwar Wada, Vetal Hill, Lokhandwala Caves
- **Spiritual**: Osho Meditation Resort
- **Nature**: Parvati Hill, Pune Okayama Friendship Garden
- **Museums**: National War Museum, Raja Dinkar Kelkar Museum, Darshan Museum

## 🔒 Features for Production

- Proper error handling and user feedback
- Loading states for better UX
- Network-friendly architecture
- Secure local data storage
- Comprehensive ProGuard rules
- Proper logger configuration

## 📝 Future Enhancements

- Real-time location integration
- User ratings and reviews
- Favorite attractions functionality
- Google Maps integration
- In-app navigation to attractions
- Social sharing features
- Multi-language support

## 👤 Author

Developed by Pranav

## 📄 License

All rights reserved. © 2024 Pune City Guide
