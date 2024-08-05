package com.example.documentreader

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Workbook

class XlsRowAdapter(
    private val sheet: Sheet,
    private var textSize: Float // Добавьте параметр для размера шрифта
) : RecyclerView.Adapter<XlsRowAdapter.RowViewHolder>() {

    class RowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rowTextView: TextView = view.findViewById(R.id.rowTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_item, parent, false)
        return RowViewHolder(view)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        val row = sheet.getRow(position)
        val cellIterator = row.cellIterator()
        val rowData = StringBuilder()
        while (cellIterator.hasNext()) {
            val cell = cellIterator.next()
            rowData.append(cell.toString()).append(" ")
        }
        holder.rowTextView.text = rowData.toString()
        holder.rowTextView.textSize = textSize // Установите размер шрифта
    }

    override fun getItemCount(): Int = sheet.lastRowNum + 1

    fun updateTextSize(newSize: Float) {
        textSize = newSize
        notifyDataSetChanged() // Обновите данные в адаптере
    }
}