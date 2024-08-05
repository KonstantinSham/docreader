
package com.example.documentreader

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.documentreader.databinding.FragmentXlsBinding

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Workbook
import java.io.InputStream

class XlsFragment : Fragment() {

    private var _binding: FragmentXlsBinding? = null
    private val binding get() = _binding!!

    private lateinit var xlsAdapter: XlsRowAdapter
    private lateinit var workbook: Workbook

    private var currentPageIndex = 0
    private var pageCount = 0

    private var currentTextSize = 16f
    private lateinit var recyclerView: RecyclerView
    private val anim by lazy {
        AnimationUtils.loadAnimation(
            requireContext(),
            android.R.anim.fade_in
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentXlsBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.backBtn.setOnClickListener {
            findNavController().navigate(R.id.action_xlsFragment_to_mainFragment)
            it.startAnimation(anim)
        }

        binding.zoomInBtn.setOnClickListener {
            adjustTextSize(increase = true)
            it.startAnimation(anim)
        }

        binding.zoomOutBtn.setOnClickListener {
            adjustTextSize(increase = false)
            it.startAnimation(anim)
        }

        arguments?.getString("file_uri")?.let { uriString ->
            Log.d("XlsFragment", "Received URI: $uriString")
            val uri = Uri.parse(uriString)
            try {
                workbook = readXlsFile(uri)
                val fileName = getFileName(uri)
                binding.fileName.text = fileName

                pageCount = workbook.numberOfSheets
                binding.pageSlider.max = pageCount - 1

                displayPage(currentPageIndex,fileName)

                binding.pageSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (fromUser) {
                            displayPage(progress, fileName)
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })

            } catch (e: Exception) {
                Log.e("XlsFragment", "Error reading XLS file", e)
                binding.fileName.text = "Error reading XLS file"
            }
        }

        return view
    }

    private fun adjustTextSize(increase: Boolean) {
        val changeFactor = if (increase) 1.2f else 0.8f
        currentTextSize *= changeFactor
        // Ограничиваем размер текста до разумного диапазона
        currentTextSize = currentTextSize.coerceIn(8f, 30f)
        xlsAdapter.updateTextSize(currentTextSize)
    }

    private fun readXlsFile(uri: Uri): Workbook {
        val inputStream: InputStream? = context?.contentResolver?.openInputStream(uri)
        return if (inputStream != null) {
            try {
                HSSFWorkbook(inputStream)
            } catch (e: Exception) {
                Log.e("XlsFragment", "Error reading XLS file", e)
                throw e
            } finally {
                inputStream.close()
            }
        } else {
            Log.e("XlsFragment", "Input stream is null")
            throw Exception("Unable to open XLS file")
        }
    }

    private fun displayPage(pageIndex: Int, fileName: String) {
        val sheet = workbook.getSheetAt(pageIndex)
        xlsAdapter = XlsRowAdapter(sheet,currentTextSize)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = xlsAdapter
        }
        currentPageIndex = pageIndex
        binding.pageIndicator.text = "${currentPageIndex + 1}/$pageCount $fileName"
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context?.contentResolver?.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "Unknown"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}