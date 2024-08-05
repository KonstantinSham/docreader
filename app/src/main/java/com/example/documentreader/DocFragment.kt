package com.example.documentreader

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

import com.example.documentreader.databinding.FragmentDocBinding
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor


class DocFragment : Fragment() {

    private var _binding: FragmentDocBinding? = null
    private val binding get() = _binding!!

    private lateinit var pages: List<String>
    private var currentPageIndex = 0

    private val anim by lazy { AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in) }

    private var currentTextSize = 16f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDocBinding.inflate(inflater, container, false)
        val view = binding.root

        arguments?.getString("doc_uri")?.let { uriString ->
            val uri = Uri.parse(uriString)
            val content = readDocFromUri(uri)
            pages = paginateText(content, 1000)
            binding.pageSlider.max = pages.size - 1

            var fileName = getFileName(uri)
            binding.fileName.text = fileName

            binding.backBtn.setOnClickListener {
                findNavController().navigate(R.id.action_docFragment_to_mainFragment)
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

            displayPage(currentPageIndex,fileName)

            binding.pageSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        currentPageIndex = progress
                        displayPage(currentPageIndex, fileName)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        return view
    }

    private fun adjustTextSize(increase: Boolean) {
        val changeFactor = if (increase) 1.2f else 0.8f
        currentTextSize *= changeFactor
        binding.textView.textSize = currentTextSize
    }

    private fun readDocFromUri(uri: Uri): String {
        val inputStream = context?.contentResolver?.openInputStream(uri)
        val document = HWPFDocument(inputStream)
        val extractor = WordExtractor(document)
        return extractor.text
    }

    private fun paginateText(text: String, pageSize: Int): List<String> {
        val pages = mutableListOf<String>()
        var startIndex = 0
        while (startIndex < text.length) {
            val endIndex = (startIndex + pageSize).coerceAtMost(text.length)
            pages.add(text.substring(startIndex, endIndex))
            startIndex = endIndex
        }
        return pages
    }

    private fun displayPage(pageIndex: Int, fileName: String) {
        binding.textView.text = pages[pageIndex]
        binding.pageIndicator.text = "${pageIndex + 1}/${pages.size} ${fileName}"
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