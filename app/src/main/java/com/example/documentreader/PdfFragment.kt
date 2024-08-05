package com.example.documentreader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
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
import com.example.documentreader.databinding.FragmentPdfBinding

class PdfFragment : Fragment() {

    private var _binding: FragmentPdfBinding? = null
    private val binding get() = _binding!!

    private lateinit var pdfRenderer: PdfRenderer
    private lateinit var currentPage: PdfRenderer.Page
    private var currentPageIndex = 0
    private var currentScaleFactor = 1.0f


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
        _binding = FragmentPdfBinding.inflate(inflater, container, false)
        val view = binding.root

        arguments?.getString("pdf_uri")?.let { uriString ->
            val uri = Uri.parse(uriString)
            val fileDescriptor = context?.contentResolver?.openFileDescriptor(uri, "r")
            fileDescriptor?.let {
                pdfRenderer = PdfRenderer(it)
                val pageCount = pdfRenderer.pageCount
                binding.pageSlider.max = pageCount - 1

                val fileName = getFileName(uri)
                binding.fileName.text = fileName

                displayPage(currentPageIndex, fileName, pageCount)

                binding.pageSlider.setOnSeekBarChangeListener(object :
                    SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        if (fromUser) {
                            currentPageIndex = progress
                            displayPage(currentPageIndex, fileName, pageCount)
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }
        }

        binding.zoomInBtn.setOnClickListener {
//            adjustImageViewSize(increase = true)
            binding.pdfImageView.layoutParams.height += 100
            binding.pdfImageView.layoutParams.width += 100
            binding.pdfImageView.requestLayout()
            it.startAnimation(anim)
        }

        binding.zoomOutBtn.setOnClickListener {
//            adjustImageViewSize(increase = false)
            binding.pdfImageView.layoutParams.height -= 100
            binding.pdfImageView.layoutParams.width -= 100
            binding.pdfImageView.requestLayout()
            it.startAnimation(anim)
        }

        binding.backBtn.setOnClickListener {
            findNavController().navigate(R.id.action_pdfFragment_to_mainFragment)
            it.startAnimation(anim)
        }

        return view
    }

    private fun displayPage(pageIndex: Int, fileName: String, pageCount: Int) {
        if (::currentPage.isInitialized) {
            currentPage.close()
        }
        currentPage = pdfRenderer.openPage(pageIndex)

        val width = currentPage.width
        val height = currentPage.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        binding.pdfImageView.setImageBitmap(bitmap)

        binding.pageIndicator.text = "${pageIndex + 1}/$pageCount $fileName"
    }

    private fun adjustImageViewSize(increase: Boolean) {
        val changeFactor = if (increase) 1.2f else 0.8f
        currentScaleFactor *= changeFactor

        val layoutParams = binding.pdfImageView.layoutParams
        layoutParams.width =
            (binding.pdfImageView.drawable.intrinsicWidth * currentScaleFactor).toInt()
        layoutParams.height =
            (binding.pdfImageView.drawable.intrinsicHeight * currentScaleFactor).toInt()

        binding.pdfImageView.layoutParams = layoutParams
        binding.pdfImageView.requestLayout()
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
        if (::currentPage.isInitialized) {
            currentPage.close()
        }
        pdfRenderer.close()
        _binding = null
    }
}