package com.example.converter

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.net.URLConnection
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val RECONNECT_INTERVAL = 3000L
    }

    private var rate1 = 1.0
    private var rate2 = 1.0
    private var hasConnection = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        updateCurrencies()

        text1.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (text1.hasFocus() && text1.isEnabled) calcText2()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        text2.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (text2.hasFocus() && text2.isEnabled) calcText1()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateRate()
            }
        }
        spinner1.onItemSelectedListener = onItemSelectedListener
        spinner2.onItemSelectedListener = onItemSelectedListener
    }

    fun calcText1() {
        if (!text2.text.isEmpty()) {
            val num = text2.text.toString().toDouble() * rate2
            text1.setText(DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(num))
        } else
            text1.setText("")
    }

    fun calcText2() {
        if (!text1.text.isEmpty()) {
            val num = text1.text.toString().toDouble() * rate1
            text2.setText(DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(num))
        } else
            text2.setText("")
    }

    fun updateCurrencies() {
        CoroutineScope(Dispatchers.Main).launch {
            setUIEnabled(false)
            val url = URL("https://free.currencyconverterapi.com/api/v6/currencies/")
            val connection = url.openConnection()

            val jsonString = try {
                parseJSON(connection)
            } catch (e: IOException) {
                networkError()
                delay(RECONNECT_INTERVAL)
                updateCurrencies()
                return@launch
            }
            val json = JSONObject(jsonString).getJSONObject("results")

            val currencies = json.keys().asSequence().sorted().toMutableList()
            val adapter = ArrayAdapter(
                this@MainActivity,
                R.layout.support_simple_spinner_dropdown_item,
                currencies
            )

            spinner1.adapter = adapter
            spinner2.adapter = adapter
            setUIEnabled(true)
        }
    }


    fun updateRate() {
        CoroutineScope(Dispatchers.Main).launch {
            setUIEnabled(false)
            val currency1 = "${spinner1.selectedItem}_${spinner2.selectedItem}"
            val currency2 = "${spinner2.selectedItem}_${spinner1.selectedItem}"
            val url = URL("https://free.currencyconverterapi.com/api/v6/convert?q=$currency1,$currency2")
            val connection = url.openConnection()

            val jsonString = try {
                parseJSON(connection)
            } catch (e: IOException) {
                networkError()
                delay(RECONNECT_INTERVAL)
                updateRate()
                return@launch
            }
            val jsonObject = JSONObject(jsonString).getJSONObject("results")

            rate1 = jsonObject.getJSONObject(currency1).getDouble("val")
            rate2 = jsonObject.getJSONObject(currency2).getDouble("val")
            calcText2()
            setUIEnabled(true)
        }
    }

    private fun networkError() {
        if (hasConnection) {
            Toast.makeText(this, getString(R.string.network_error_message), Toast.LENGTH_SHORT).show()
            hasConnection = false
        }
    }

    private fun setUIEnabled(isEnabled: Boolean) {
        text1.isEnabled = isEnabled
        text2.isEnabled = isEnabled
        spinner1.isEnabled = isEnabled
        spinner2.isEnabled = isEnabled
    }

    private suspend fun parseJSON(connection: URLConnection) = withContext(Dispatchers.IO) {
        val reader = BufferedReader(InputStreamReader(connection.getInputStream()))
        val stringBuilder = StringBuilder()
        var line = reader.readLine()
        while (line != null) {
            stringBuilder.append(line)
            line = reader.readLine()
        }
        hasConnection = true
        stringBuilder.toString()
    }
}
