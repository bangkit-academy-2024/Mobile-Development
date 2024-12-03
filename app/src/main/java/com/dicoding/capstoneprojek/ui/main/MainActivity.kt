package com.dicoding.capstoneprojek.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.dicoding.capstoneprojek.helper.ImageClassifierHelper
import com.dicoding.capstoneprojek.R
import com.dicoding.capstoneprojek.databinding.ActivityMainBinding
import com.dicoding.capstoneprojek.ui.ViewModel.FactoryViewModel
import com.dicoding.capstoneprojek.ui.login.LoginActivity
import com.dicoding.capstoneprojek.ui.result.ResultActivity
import com.yalantis.ucrop.UCrop
import java.io.File

class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<ViewModelMain> {
        FactoryViewModel.getInstance(this)
    }
    private lateinit var binding: ActivityMainBinding

    private var currentImageUri: Uri? = null

    private var exitDialog: AlertDialog? = null
    private var logoutDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            title = "EpiAlert"
        }

        setupView()
        setupObservers()
        setupAction()

        onBackPressedDispatcher.addCallback(this) {
            showExitConfirmationDialog()
        }

        viewModel.getSession().observe(this) { user ->
            if (!user.isLoggedIn) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }

        viewModel.imageUri.observe(this) { uri ->
            uri?.let {
                binding.previewImageView.setImageURI(it)
                binding.analyzeButton.visibility = View.VISIBLE
            }
        }

        binding.apply {
            analyzeButton.setOnClickListener {
                viewModel.imageUri.value?.let {
                    analyzeImage(it)
                } ?: showToast(getString(R.string.empty_image))
            }
            btnGallery.setOnClickListener {
                openGallery()
            }
            btnCamera.setOnClickListener {
                currentImageUri = getImageUri(this@MainActivity)
                cameraLaunch.launch(currentImageUri!!)
            }
        }
    }

    private fun openGallery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private val launcherGallery =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            uri?.let {
                viewModel.setImageUri(it)
                startCrop(it)
            } ?: Log.d("Photo Picker", "No media selected")
        }

    private fun startCrop(imageUri: Uri) {
        val uniqueFileName = "cropped_image_${System.currentTimeMillis()}.jpg"
        val destinationUri = Uri.fromFile(File(cacheDir, uniqueFileName))

        val uCropIntent = UCrop.of(imageUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(224, 224)
            .getIntent(this)

        cropImageResultLauncher.launch(uCropIntent)
    }

    private val cropImageResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val resultUri = UCrop.getOutput(result.data!!)
                resultUri?.let {
                    viewModel.setImageUri(it)
                    showImage() // Tampilkan gambar dengan background dihapus
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError: Throwable? = UCrop.getError(result.data!!)
                cropError?.let {
                    showToast(getString(R.string.load_failed))
                }
            }
        }


    private fun showImage() {
        viewModel.imageUri.value?.let {
            binding.previewImageView.setImageURI(it)
            binding.previewImageView.background = null // Hapus background
            binding.analyzeButton.visibility = View.VISIBLE
        }
    }


//    private fun analyzeImage(imageUri: Uri) {
//        Log.d("Analyze Image", "URI: $imageUri")
//        val imageClassifierHelper = ImageClassifierHelper(
//            context = this,
//            classifierListener = object : ImageClassifierHelper.ClassifierListener {
//                override fun onError(error: String) {
//                    Log.e("Classifier Error", error)
//                    showToast(error)
//                }
//
//                override fun onResults(results: List<ImageClassifierHelper.Result>) {
//                    Log.d("Classifier Results", results.toString())
//                    val resultString = results?.joinToString("\n") {
//                        val label = it.categories[0].label
//                        val score = (it.categories[0].score * 100).toInt()
//                        "$label: $score%"
//                    } ?: getString(R.string.classification_failed)
//
//                    moveToResult(imageUri, resultString)
//                }
//            }
//        )
//        imageClassifierHelper.classifyStaticImage(imageUri)
//    }

    private fun analyzeImage(imageUri: Uri) {
        Log.d("Analyze Image", "URI: $imageUri")
        val imageClassifierHelper = ImageClassifierHelper(
            context = this,
            classifierListener = object : ImageClassifierHelper.ClassifierListener {
                override fun onError(error: String) {
                    Log.e("Classifier Error", error)
                    showToast(error)
                }

                override fun onResults(results: List<ImageClassifierHelper.Result>) {
                    Log.d("Classifier Results", results.toString())
                    val resultString = results.joinToString("\n") {
                        val label = it.label
                        val score = (it.probability * 100).toInt()
                        "$label: $score%"
                    }

                    moveToResult(imageUri, resultString)
                }
            }
        )
        imageClassifierHelper.classifyStaticImage(imageUri)
    }



    private fun moveToResult(imageUri: Uri, result: String) {
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra(ResultActivity.EXTRA_IMAGE_URI, imageUri.toString())
        intent.putExtra(ResultActivity.EXTRA_RESULT, result)
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private val cameraLaunch =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            if (isSuccess) {
                imageShow()
            } else {
                currentImageUri = null
            }
        }

    private fun imageShow() {
        currentImageUri?.let { uri ->
            binding.previewImageView.setImageURI(uri)
            binding.previewImageView.background = null
            startCrop(uri) // Tambahkan langkah ini
        }
    }


    private fun setupView() {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            supportActionBar?.show()
        }
    }

    private fun setupObservers() {
        viewModel.getSession().observe(this) { user ->
            if (!user.isLoggedIn) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
    }

    private fun setupAction() {
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        if (logoutDialog?.isShowing == true) {
            logoutDialog?.dismiss()
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.logout)
        builder.setMessage(R.string.confirmation_logout)
        builder.setPositiveButton(R.string.done) { _, _ ->
            viewModel.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
        builder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
        logoutDialog = builder.show()
    }

    private fun showExitConfirmationDialog() {
        if (exitDialog?.isShowing == true) {
            exitDialog?.dismiss()
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.exit_apl)
        builder.setMessage(R.string.confirmation_logout)
        builder.setPositiveButton(R.string.done) { _, _ ->
            finish()
        }
        builder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
        exitDialog = builder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        exitDialog?.dismiss()
        logoutDialog?.dismiss()
    }
}
