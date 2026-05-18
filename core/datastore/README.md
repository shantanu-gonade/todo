# :core:datastore

Preferences DataStore module. Persists user settings that survive process death and
device reboots. Currently stores the selected theme mode (`SYSTEM` / `LIGHT` / `DARK`).

## Why DataStore over SharedPreferences

- Type-safe via proto or typed keys — no string-keyed `getString` calls at callsites
- Suspend-based writes (no ANR risk on main thread)
- `Flow`-based reads — automatically notifies all collectors when a value changes
- Atomic, transactional updates via `edit { }` lambda

## Key components

### `UserPreferencesDataSource`

The single public API for reading and writing user preferences.

```kotlin
class UserPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val userData: Flow<UserData> = dataStore.data.map { prefs ->
        val name = prefs[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.name
        val mode = runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.SYSTEM)
        UserData(themeMode = mode)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE_KEY] = mode.name }
    }

    private companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }
}
```

`runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.SYSTEM)` ensures
the app degrades gracefully if a future enum value is read on an older build.

### DataStore file

The file is named `user_preferences.preferences_pb` and lives in the app's private
data directory. It is provided as a `@Singleton` `DataStore<Preferences>` via the
`DataStoreModule` Hilt module.

## Dependencies

```
:core:datastore
 └── :core:model   (UserData, ThemeMode)
```

Plus `androidx.datastore:datastore-preferences`.

## Convention plugin

```kotlin
apply plugin: 'todoapp.android.library'
apply plugin: 'todoapp.android.hilt'
```
