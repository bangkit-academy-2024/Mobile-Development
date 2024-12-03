package com.dicoding.capstoneprojek.ui.result

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dicoding.capstoneprojek.R
import com.dicoding.capstoneprojek.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {

    // Menggunakan View Binding untuk mengakses komponen layout
    private lateinit var binding: ActivityResultBinding

    // ViewModel untuk mengelola data history

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Menginisialisasi binding dan menghubungkan layout dengan activity
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)



        // Mendapatkan URI gambar dan hasil analisis dari Intent
        val imageUri = Uri.parse(intent.getStringExtra(EXTRA_IMAGE_URI))
        val result = intent.getStringExtra(EXTRA_RESULT)

        // Menampilkan gambar hasil analisis jika URI tidak null
        imageUri?.let {
            Log.d("Image URI", "showImage: $it") // Log URI gambar
            binding.resultImage.setImageURI(it) // Menampilkan gambar pada ImageView
        }

        // Menampilkan hasil analisis pada TextView jika hasil tidak null
        result?.let {
            Log.d("Result", "showResult: $it") // Log hasil analisis
            binding.resultText.text = it // Menampilkan hasil analisis
        }


    }



    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri" // Kunci untuk URI gambar dalam Intent
        const val EXTRA_RESULT = "extra_result" // Kunci untuk hasil analisis dalam Intent
    }
}