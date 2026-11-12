use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;
use serde::Deserialize;

#[derive(Deserialize)]
struct GeoResponse {
    results: Option<Vec<GeoCity>>,
}
#[derive(Deserialize)]
struct GeoCity {
    name: String,
    latitude: f64,
    longitude: f64,
    country: Option<String>,
    admin1: Option<String>,
    feature_code: Option<String>,
}
#[derive(Deserialize)]
struct ForecastResponse {
    current: Current,
    daily: Daily,
}
#[derive(Deserialize)]
struct Current {
    temperature_2m: f64,
    relative_humidity_2m: f64,
    apparent_temperature: f64,
    wind_speed_10m: f64,
    weather_code: i32,
}
#[derive(Deserialize)]
struct Daily {
    time: Vec<String>,
    temperature_2m_max: Vec<f64>,
    temperature_2m_min: Vec<f64>,
    precipitation_probability_max: Vec<Option<i32>>,
    weather_code: Vec<i32>,
}

fn weather_name(code: i32) -> &'static str {
    match code {
        0 => "Ясно",
        1 => "Преимущественно ясно",
        2 => "Переменная облачность",
        3 => "Пасмурно",
        45 | 48 => "Туман",
        51 | 53 | 55 => "Морось",
        56 | 57 => "Ледяная морось",
        61 | 63 | 65 => "Дождь",
        66 | 67 => "Ледяной дождь",
        71 | 73 | 75 | 77 => "Снег",
        80 | 81 | 82 => "Ливень",
        85 | 86 => "Снегопад",
        95 | 96 | 99 => "Гроза",
        _ => "Нет данных",
    }
}

fn search(query: &str) -> Result<String, String> {
    let q = urlencoding::encode(query);
    let url = format!(
        "https://geocoding-api.open-meteo.com/v1/search?name={}&count=20&language=ru&format=json&countryCode=RU",
        q
    );
    let response: GeoResponse = ureq::get(&url)
        .call().map_err(|e| e.to_string())?
        .into_json().map_err(|e| e.to_string())?;
    let cities = response.results.unwrap_or_default();
    let out: Vec<serde_json::Value> = cities.into_iter().map(|c| {
        serde_json::json!({
            "name": c.name,
            "region": c.admin1.unwrap_or_default(),
            "country": c.country.unwrap_or_else(|| "Россия".into()),
            "latitude": c.latitude,
            "longitude": c.longitude,
            "feature": c.feature_code.unwrap_or_default()
        })
    }).collect();
    serde_json::to_string(&out).map_err(|e| e.to_string())
}

fn forecast(lat: f64, lon: f64) -> Result<String, String> {
    let url = format!(
        "https://api.open-meteo.com/v1/forecast?latitude={}&longitude={}&current=temperature_2m,relative_humidity_2m,apparent_temperature,wind_speed_10m,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&forecast_days=7&timezone=auto",
        lat, lon
    );
    let data: ForecastResponse = ureq::get(&url)
        .call().map_err(|e| e.to_string())?
        .into_json().map_err(|e| e.to_string())?;

    let days: Vec<serde_json::Value> = (0..data.daily.time.len()).map(|i| {
        serde_json::json!({
            "date": data.daily.time[i],
            "min": data.daily.temperature_2m_min[i],
            "max": data.daily.temperature_2m_max[i],
            "rain": data.daily.precipitation_probability_max.get(i).copied().flatten(),
            "description": weather_name(data.daily.weather_code[i])
        })
    }).collect();

    serde_json::to_string(&serde_json::json!({
        "temperature": data.current.temperature_2m,
        "feels_like": data.current.apparent_temperature,
        "humidity": data.current.relative_humidity_2m,
        "wind": data.current.wind_speed_10m,
        "description": weather_name(data.current.weather_code),
        "days": days
    })).map_err(|e| e.to_string())
}

fn jstring_to_string(env: &mut JNIEnv, value: JString) -> String {
    env.get_string(&value).map(|s| s.into()).unwrap_or_default()
}
fn to_jstring(env: &mut JNIEnv, value: String) -> jstring {
    env.new_string(value).map(|s| s.into_raw()).unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_com_example_russiweather_MainActivity_nativeSearch(
    mut env: JNIEnv, _class: JClass, query: JString
) -> jstring {
    let q = jstring_to_string(&mut env, query);
    let result = search(&q).unwrap_or_else(|e| serde_json::json!({"error": e}).to_string());
    to_jstring(&mut env, result)
}

#[no_mangle]
pub extern "system" fn Java_com_example_russiweather_MainActivity_nativeForecast(
    mut env: JNIEnv, _class: JClass, lat: jni::sys::jdouble, lon: jni::sys::jdouble
) -> jstring {
    let result = forecast(lat as f64, lon as f64)
        .unwrap_or_else(|e| serde_json::json!({"error": e}).to_string());
    to_jstring(&mut env, result)
}
