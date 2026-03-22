# Project Plan

Unscroll: An Android app to stop scrolling addiction on social media platforms like YouTube, Instagram, and TikTok. The app should help users reduce their time spent on addictive short-form video content.

## Project Brief

# Project Brief: Unscroll

Unscroll is a premium, high-energy Android application designed to help users break free from the addictive loop of short-form video content. It focuses on a modern, motivating experience that encourages digital mindfulness through sophisticated UI and real-time interventions.

## Features
- **Dynamic Usage Dashboard**: A vibrant Material 3 dashboard featuring glassmorphism-styled cards and fluid animations to track time spent on YouTube Shorts, Instagram Reels, and TikTok.
- **Immersive Intervention Overlays**: High-fidelity, full-screen overlays with modern typography and refined blur effects that trigger when a user exceeds their scrolling limit.
- **Intelligent App Monitoring**: Real-time detection and monitoring of specific addictive app segments using Android’s Accessibility and Usage Stats APIs.
- **Gamified Milestones**: A reward-based system featuring sophisticated Lottie animations to celebrate "focus streaks" and motivate users to stay off addictive feeds.

## High-Level Technical Stack
- **Kotlin**: The primary language for a type-safe and modern codebase.
- **Jetpack Compose**: For building a premium, high-performance UI with advanced layout and animation capabilities.
- **Material 3**: Utilizing the latest design components and dynamic color systems for a top-tier aesthetic.
- **Kotlin Coroutines**: For smooth, non-blocking background operations and app monitoring.
- **KSP (Kotlin Symbol Processing)**: Optimized code generation for faster build times and better performance.
- **Lottie for Compose**: For integrating high-quality, vector-based animations to enhance user engagement.
- **Android Accessibility Services**: The core system hook required to identify and restrict access to addictive UI components within third-party apps.

## Implementation Steps

### Task_1_Foundation: Setup project foundation including permissions, DataStore for settings, and Material 3 theme.
- **Status:** COMPLETED
- **Updates:** Successfully implemented project foundation for Unscroll.
- **Acceptance Criteria:**
  - Usage Stats and Overlay permissions are handled
  - DataStore is initialized for storing app limits
  - Vibrant Material 3 theme with Dark/Light mode is implemented
  - App builds successfully

### Task_2_Monitoring: Implement the core usage tracking logic using UsageStatsManager and a background service.
- **Status:** COMPLETED
- **Updates:** Successfully implemented Task_2_Monitoring:
- **Acceptance Criteria:**
  - Background service correctly tracks time spent in YouTube, Instagram, and TikTok
  - Usage data is persisted and updated in real-time
  - Service handles device reboots and app restarts

### Task_3_Dashboard: Create the Progress Dashboard and Settings UI using Jetpack Compose.
- **Status:** COMPLETED
- **Updates:** Successfully implemented Task_3_Dashboard:
- **Acceptance Criteria:**
  - Main screen shows daily/weekly usage trends with progress bars
  - Settings screen allows users to select apps and set time quotas
  - Edge-to-edge display is implemented

### Task_4_Blocking: Implement the Focus Overlay and interruption mechanism.
- **Status:** COMPLETED
- **Updates:** Successfully implemented Task_4_Blocking:
- **Acceptance Criteria:**
  - Overlay appears immediately when a monitored app's limit is exceeded
  - Accessibility Service or Overlay effectively blocks/interrupts access
  - Users can dismiss or snooze interruptions based on settings

### Task_5_Polish_Verify: Refine UI/UX, generate adaptive icon, and perform final verification.
- **Status:** COMPLETED
- **Updates:** Successfully implemented Task_5_Polish_Verify:
- **Acceptance Criteria:**
  - Adaptive app icon matches the Unscroll theme
  - Final UI matches Material 3 guidelines and project brief
  - App does not crash during usage
  - Run and Verify: Confirm application stability and requirement alignment

### Task_6_Premium_UI_Gamification: Enhance the UI with glassmorphism, fluid animations, and Lottie-based gamified milestones.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - Dashboard features glassmorphism-styled cards
  - Fluid animations are integrated into UI transitions
  - Lottie animations celebrate focus streaks (e.g., Lottie for Compose)
  - App builds and runs without crashes
- **StartTime:** 2026-03-22 14:24:58 IST

### Task_7_Advanced_Monitoring_Overlays: Implement Accessibility Service for segment-level monitoring and immersive blurred overlays.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Accessibility Service detects specific addictive segments (e.g. YouTube Shorts, Reels)
  - Overlays use modern typography and refined blur effects
  - Final Run and Verify: critic_agent verifies stability (no crashes) and UI fidelity
  - App does not crash

