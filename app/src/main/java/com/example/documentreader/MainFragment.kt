package com.example.documentreader

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.documentreader.databinding.FragmentMainBinding

private const val PICK_DOC_FILE = 3
private const val PICK_PDF_FILE = 2

class MainFragment : Fragment() {

    private val PICK_PDF_FILE = 2
    private val PICK_DOC_FILE = 3
    private val PICK_XLS_FILE = 4
    private val PICK_TXT_FILE = 5
    private val PICK_ALL_FILE = 6

    private val anim by lazy { AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in) }

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val launcher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                Toast.makeText(requireContext(), "Permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Permissions not granted", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        val view = binding.root

        // Запрос разрешений при запуске приложения
        checkPermissions()

        binding.pdfBtn.setOnClickListener {
            checkPermissionsAndOpenFilePicker("application/pdf", PICK_PDF_FILE)
            it.startAnimation(anim)
        }

        binding.docBtn.setOnClickListener {
            checkPermissionsAndOpenFilePicker("application/msword", PICK_DOC_FILE)
            it.startAnimation(anim)
        }

        binding.xlsBtn.setOnClickListener {
            checkPermissionsAndOpenFilePicker("application/vnd.ms-excel", PICK_XLS_FILE)
            it.startAnimation(anim)
        }

        binding.txtBtn.setOnClickListener {
            checkPermissionsAndOpenFilePicker("text/plain", PICK_TXT_FILE)
            it.startAnimation(anim)
        }

        binding.allBtn.setOnClickListener {
            checkPermissionsAndOpenFilePicker("*/*", PICK_ALL_FILE)
            it.startAnimation(anim)
        }

        return view
    }

    private fun checkPermissions() {
        if (!hasStoragePermissions()) {
            launcher.launch(REQUEST_PERMISSIONS)
        }
    }

    private fun checkPermissionsAndOpenFilePicker(mimeType: String, requestCode: Int) {
        if (hasStoragePermissions()) {
            openFilePicker(mimeType, requestCode)
        } else {
            Toast.makeText(requireContext(), "Storage permission is not granted", Toast.LENGTH_SHORT).show()
            launcher.launch(REQUEST_PERMISSIONS)
        }
    }

    private fun hasStoragePermissions(): Boolean {
        return REQUEST_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private val REQUEST_PERMISSIONS: Array<String> = buildList {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    private fun openFilePicker(mimeType: String, requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
        }
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            val uri: Uri? = data.data
            when (requestCode) {
                PICK_PDF_FILE, PICK_ALL_FILE -> {
                    uri?.let {
                        val mimeType = context?.contentResolver?.getType(it)
                        if (mimeType != null) {
                            when {
                                mimeType.startsWith("application/pdf") -> {
                                    Log.d("MainFragment", "PDF URI: $uri")
                                    openPdfFragment(uri)
                                }

                                mimeType.startsWith("application/msword") -> {
                                    Log.d("MainFragment", "DOC URI: $uri")
                                    openDocFragment(uri)
                                }

                                mimeType.startsWith("application/vnd.ms-excel") -> {
                                    Log.d("MainFragment", "XLS URI: $uri")
                                    openXlsFragment(uri)
                                }

                                mimeType.startsWith("text/plain") -> {
                                    Log.d("MainFragment", "TXT URI: $uri")
                                    openTxtFragment(uri)
                                }

                                else -> {
                                    Log.e("MainFragment", "Unsupported MIME type: $mimeType")
                                }
                            }
                        }
                    }
                }

                PICK_DOC_FILE -> {
                    uri?.let {
                        Log.d("MainFragment", "DOC URI: $uri")
                        openDocFragment(uri)
                    }
                }

                PICK_XLS_FILE -> {
                    uri?.let {
                        Log.d("MainFragment", "XLS URI: $uri")
                        openXlsFragment(uri)
                    }
                }

                PICK_TXT_FILE -> {
                    uri?.let {
                        Log.d("MainFragment", "TXT URI: $uri")
                        openTxtFragment(uri)
                    }
                }
            }
        } else {
            Log.e("MainFragment", "Error: Result code $resultCode, Data: $data")
        }
    }

    private fun openXlsFragment(uri: Uri) {
        val bundle = Bundle().apply {
            putString("file_uri", uri.toString())
            putString("file_type", "xls")
        }
        findNavController().navigate(R.id.action_mainFragment_to_xlsFragment, bundle)
    }

    private fun openPdfFragment(uri: Uri) {
        val bundle = Bundle().apply {
            putString("pdf_uri", uri.toString())
        }
        findNavController().navigate(R.id.action_mainFragment_to_pdfFragment, bundle)
    }

    private fun openDocFragment(uri: Uri) {
        val bundle = Bundle().apply {
            putString("doc_uri", uri.toString())
        }
        findNavController().navigate(R.id.action_mainFragment_to_docFragment, bundle)
    }

    private fun openTxtFragment(uri: Uri) {
        val bundle = Bundle().apply {
            putString("txt_uri", uri.toString())
        }
        findNavController().navigate(R.id.action_mainFragment_to_txtFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
