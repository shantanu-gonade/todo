package com.eulerity.todo.core.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserData(val themeMode: ThemeMode = ThemeMode.SYSTEM)
