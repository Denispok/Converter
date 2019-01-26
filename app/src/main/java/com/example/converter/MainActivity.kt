package com.example.converter

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.net.URLConnection

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
    }

    override fun onResume() {
        super.onResume()

        CoroutineScope(Dispatchers.Main).launch {
            val url = URL("https://free.currencyconverterapi.com/api/v6/currencies/")
            val connection = url.openConnection()

            val jsonString = parseJSON(connection)

            val json = JSONObject(jsonString).getJSONObject("results")

            val adapter = ArrayAdapter(
                this@MainActivity,
                R.layout.support_simple_spinner_dropdown_item,
                json.keys().asSequence().sorted().toMutableList()
            )
            spinner1.adapter = adapter
            spinner2.adapter = adapter
        }
    }

    fun convert(view: View) {
        CoroutineScope(Dispatchers.Main).launch {
            val currencies = "${spinner1.selectedItem}_${spinner2.selectedItem}"
            val url = URL("https://free.currencyconverterapi.com/api/v6/convert?q=$currencies")
            val connection = url.openConnection()

            val jsonString = parseJSON(connection)

            val курс = JSONObject(jsonString).getJSONObject("results").getJSONObject(currencies).getDouble("val")
            text2.setText((text1.text.toString().toDouble() * курс).toString())
        }
    }

    private suspend fun parseJSON(connection: URLConnection) = withContext(Dispatchers.IO) {
        val reader = BufferedReader(InputStreamReader(connection.getInputStream()))
        val stringBuilder = StringBuilder()
        var line = reader.readLine()
        while (line != null) {
            stringBuilder.append(line)
            line = reader.readLine()
        }
        stringBuilder.toString()
    }

    private fun log(text: String) {
        Log.d("MYLOG", text)
    }

}
