# Changelog

## Version 8.0 - Pune City Utility & Expense Tracker (Complete Pivot)

### 🚀 Major Transformation
- **Pivot to Utility**: Transformed the "Pune City Guide" into a dedicated "Pune City Utility & Expense Tracker".
- **Fiscal Mastery**: Added a comprehensive **Daily Expense Tracker** with Room persistence and category-aware budgeting.
- **Smart AI Pune**: Rebuilt the chatbot to act as a **Pune Fiscal & Utility Expert**, providing advice on taxes, rickshaw fares, and local budgeting tips.
- **Official Rickshaw Guide**: Implemented an official Pune Rickshaw fare calculator based on **RTO Rates (Effective Feb 1, 2025)**.

### 🧹 Infrastructure Cleanup
- **Purged Legacy Code**: Removed over 5,000 lines of attraction-related code, repositories, and UI screens.
- **Database Schema v13**: Migration to a streamlined schema focusing on Expenses, AI, and System Audit logs.
- **Repository Optimization**: Deleted defunct Attraction, Itinerary, and RecentlyViewed repositories.
- **Health Diagnostics**: Rebuilt `BackendHealthService` to focus on mission-critical Auth and AI connectivity.

### 🎨 Design & Experience
- **Premium Glassmorphism**: Overhauled the Navigation and AI Chat Overlay with advanced glassmorphic aesthetics.
- **Smart Onboarding**: Rebuilt the onboarding flow to reflect the app's new utility identity.
- **Micro-Animations**: Added smooth spring animations to the Bottom Navigation and Expense cards.

### 🛠️ Technical Improvements
- **Supabase Integration**: Robust Auth and Profile management with local persistence.
- **Ktor Migration**: Standardized networking across the app using Ktor.
- **Kotlin 2.0 readiness**: Upgraded dependency patterns and ViewModels.

## Version 1.0 - Initial Pune City Guide (Legacy)
- *Note: Version 1.0 features (Attractions, Discovery, Search) have been deprecated and removed in favor of the v2.0 Utility Rewrite.*
- Initial implementation of Pune-inspired Material 3 theme.
- Basic attraction listing and navigation.
