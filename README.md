# Погода России — Android + Rust

Проект Android-приложения с интерфейсом на Kotlin и сетевой логикой на Rust.
Ищет города России через Open-Meteo Geocoding API и показывает текущую погоду и прогноз на 7 дней.

## Что работает
- поиск российских городов (по мере ввода);
- список найденных городов с регионом/координатами;
- текущая температура, ощущаемая температура, ветер и влажность;
- прогноз на 7 дней;
- ручное обновление погоды.

Для работы нужен интернет. API-ключ не требуется. Это не офлайн-каталог: поиск обращается к геокодеру Open-Meteo, поэтому можно найти города и населённые пункты, которые есть в его базе.

## Сборка APK на Windows 10/11

1. Установи Android Studio и открой папку `android` как проект.
2. Установи Android SDK Platform 35 и Build Tools через SDK Manager.
3. Установи Rust через rustup: https://rustup.rs/
4. В PowerShell установи Android target и cargo-ndk:

```powershell
rustup target add aarch64-linux-android
cargo install cargo-ndk
```

5. Установи Android NDK через Android Studio (SDK Manager → SDK Tools → NDK).
6. В PowerShell из папки проекта выполни:

```powershell
cd rust
cargo ndk -t arm64-v8a -o ..\android\app\src\main\jniLibs build --release
```

7. Открой проект `android` в Android Studio, дождись Gradle Sync и выбери Build → Build Bundle(s) / APK(s) → Build APK(s).

Готовый APK появится здесь:
`android/app/build/outputs/apk/debug/app-debug.apk`

## Важно
- Включена сборка для ARM64 (подходит большинству современных Android-телефонов).
- Для старых 32-битных устройств нужно отдельно добавить target `armv7-linux-androideabi` и собрать ABI `armeabi-v7a`.
- Rust-код находится в `rust/src/lib.rs`.
- Интерфейс Android находится в `android/app/src/main/java/com/example/russiweather/MainActivity.kt`.


## Самая простая сборка через GitHub Actions

В проект уже добавлен workflow `.github/workflows/build-apk.yml`.

1. Создай пустой репозиторий на GitHub.
2. Распакуй ZIP и загрузи ВСЕ файлы проекта в репозиторий (в корень, сохранив папки).
3. Открой вкладку **Actions** и выбери **Build Android APK**.
4. Нажми **Run workflow**. Дождись зелёной галочки.
5. Открой завершённый запуск и скачай артефакт **PogodaRossii-debug-apk**.
6. Внутри будет `app-debug.apk`, который можно установить на Android.

Сборка выполняется на GitHub Actions; для неё не нужно устанавливать Rust или Android SDK на свой ПК.
