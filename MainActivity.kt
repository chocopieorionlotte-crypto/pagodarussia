package com.example.russiweather

import android.app.Activity
import android.os.Bundle
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

class MainActivity : Activity() {
    companion object {
        init { System.loadLibrary("weather_core") }
    }
    private external fun nativeSearch(query: String): String
    private external fun nativeForecast(lat: Double, lon: Double): String

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var root: LinearLayout
    private lateinit var search: EditText
    private lateinit var status: TextView
    private lateinit var results: LinearLayout
    private lateinit var weather: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 24, 22, 28)
            setBackgroundColor(0xFFF4F7FC.toInt())
        }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(text("Погода России", 28, true, 0xFF17243A.toInt()))
        root.addView(text("Найди город и посмотри прогноз на неделю", 14, false, 0xFF65738A.toInt()))
        search = EditText(this).apply {
            hint = "Например, Казань или Владивосток"
            singleLine = true
            textSize = 16f
            setPadding(16, 8, 16, 8)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        val searchButton = Button(this).apply {
            text = "Найти город"
            setOnClickListener { doSearch(search.text.toString().trim()) }
        }
        root.addView(search, lp(top = 18))
        root.addView(searchButton, lp(top = 8))
        status = text("Введи название города России.", 14, false, 0xFF65738A.toInt())
        root.addView(status, lp(top = 8))
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(results, lp(top = 8))
        weather = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(weather, lp(top = 14))
        root.addView(text("Источник данных: Open-Meteo", 12, false, 0xFF8A96A8.toInt()), lp(top = 20))
    }

    private fun doSearch(query: String) {
        if (query.length < 2) {
            status.text = "Введи хотя бы две буквы."
            return
        }
        status.text = "Ищу город…"
        results.removeAllViews()
        weather.removeAllViews()
        executor.execute {
            try {
                val raw = nativeSearch(query)
                val arr = JSONArray(raw)
                runOnUiThread {
                    if (arr.length() == 0) status.text = "Ничего не найдено. Попробуй другое название."
                    else {
                        status.text = "Найдено: ${arr.length()}. Выбери город:"
                        for (i in 0 until arr.length()) {
                            val city = arr.getJSONObject(i)
                            val title = city.optString("name")
                            val region = city.optString("region")
                            val button = Button(this).apply {
                                text = if (region.isBlank()) title else "$title — $region"
                                isAllCaps = false
                                setOnClickListener {
                                    loadWeather(title, city.getDouble("latitude"), city.getDouble("longitude"))
                                }
                            }
                            results.addView(button, lp(top = 5))
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Ошибка поиска: ${e.message ?: "проверь интернет"}" }
            }
        }
    }

    private fun loadWeather(city: String, lat: Double, lon: Double) {
        status.text = "Загружаю погоду: $city…"
        weather.removeAllViews()
        executor.execute {
            try {
                val data = JSONObject(nativeForecast(lat, lon))
                if (data.has("error")) throw Exception(data.getString("error"))
                runOnUiThread {
                    status.text = "Погода в городе: $city"
                    weather.addView(text("${data.getDouble("temperature").toInt()}°C", 42, true, 0xFF17243A.toInt()))
                    weather.addView(text(data.optString("description"), 20, true, 0xFF3978D4.toInt()))
                    weather.addView(text("Ощущается как ${data.getDouble("feels_like").toInt()}°C", 15, false, 0xFF53627A.toInt()), lp(top = 5))
                    weather.addView(text("Влажность: ${data.getDouble("humidity").toInt()}%   •   Ветер: ${data.getDouble("wind").toInt()} км/ч", 14, false, 0xFF53627A.toInt()), lp(top = 4))
                    weather.addView(text("Прогноз на 7 дней", 20, true, 0xFF17243A.toInt()), lp(top = 18))
                    val days = data.getJSONArray("days")
                    for (i in 0 until days.length()) {
                        val day = days.getJSONObject(i)
                        val rain = if (day.isNull("rain")) "—" else "${day.getInt("rain")}%"
                        val line = "${day.getString("date")}   ${day.getString("description")}   ${day.getInt("min")}° / ${day.getInt("max")}°   дождь: $rain"
                        weather.addView(text(line, 14, false, 0xFF34445C.toInt()), lp(top = 8))
                        val divider = View(this).apply { setBackgroundColor(0xFFDCE4EF.toInt()) }
                        weather.addView(divider, LinearLayout.LayoutParams(-1, 1).apply { topMargin = 7 })
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Не удалось загрузить погоду: ${e.message ?: "проверь интернет"}" }
            }
        }
    }

    private fun text(value: String, size: Int, bold: Boolean, color: Int) =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun lp(top: Int = 0) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = top }
}
