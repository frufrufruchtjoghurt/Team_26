package com.tugraz.chronos

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import com.tugraz.chronos.model.service.ChronosService
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class CreateTaskActivity : AppCompatActivity(), View.OnClickListener {

    lateinit var btn_create: Button
    lateinit var et_title: EditText
    lateinit var et_description: EditText
    lateinit var et_date: EditText
    lateinit var sp_group: Spinner
    lateinit var coordinator: CoordinatorLayout
    lateinit var chronosService: ChronosService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_task)

        chronosService = ChronosService(this)

        btn_create = findViewById(R.id.btn_ct_save)
        et_title = findViewById(R.id.et_ct_title)
        et_description = findViewById(R.id.et_ct_description)
        et_date = findViewById(R.id.et_ct_date)
        sp_group = findViewById(R.id.sp_ct_group)
        coordinator = findViewById(R.id.cl_ct)

        btn_create.setOnClickListener(this)
        et_date.setOnClickListener {
            pickDateTime()
        }

        val groups = mutableListOf<String>(getString(R.string.group))
        for (group in chronosService.getAllGroups()) {
            groups.add(group.taskGroup.title)
        }

        var adapter= ArrayAdapter(this, android.R.layout.simple_list_item_1, groups)
        sp_group.adapter = adapter
    }

    private fun pickDateTime() {
        val currentDateTime = Calendar.getInstance()
        val startYear = currentDateTime.get(Calendar.YEAR)
        val startMonth = currentDateTime.get(Calendar.MONTH)
        val startDay = currentDateTime.get(Calendar.DAY_OF_MONTH)
        val startHour = currentDateTime.get(Calendar.HOUR_OF_DAY)
        val startMinute = currentDateTime.get(Calendar.MINUTE)

        DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, year, month, day ->
            TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                val pickedDateTime = Calendar.getInstance()
                pickedDateTime.set(year, month, day, hour, minute)
                setDateTime(pickedDateTime)
            }, startHour, startMinute, false).show()
        }, startYear, startMonth, startDay).show()
    }

    private fun setDateTime(dt: Calendar) {
        val df: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
        et_date.setText(df.format(dt.time))
    }

    override fun onClick(v: View?) {
        if (et_title.text.toString().isEmpty())
        {
            Snackbar.make(
                coordinator,
                R.string.err_title_empty,
                Snackbar.LENGTH_SHORT
            ).show()

            return
        }
        else if (et_date.text.toString().isEmpty())
        {
            Snackbar.make(
                coordinator,
                R.string.err_date_empty,
                Snackbar.LENGTH_SHORT
            ).show()

            return
        }

        val db_success = chronosService.addTask(0L, et_title.text.toString(),
                et_description.text.toString(),
                LocalDateTime.parse(et_date.text.toString(), DateTimeFormatter.ISO_DATE_TIME))

        if (db_success.taskId == 0L)
        {
            Snackbar.make(
                coordinator,
                R.string.err_db_insert,
                Snackbar.LENGTH_SHORT
            ).show()

            return
        }
        else
        {
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }
    }
}