
package com.dicoding.capstoneprojek.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.dicoding.capstoneprojek.R
import com.dicoding.capstoneprojek.ml.Modelkasaran
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.nio.ByteBuffer
import java.nio.ByteOrder


// Kelas ini membantu mengelola klasifikasi gambar menggunakan model machine learning.
class ImageClassifierHelper(
    private var threshold: Float = 0.1f, // Ambang batas untuk klasifikasi (default 0.1)
    private var maxResults: Int = 3, // Jumlah hasil maksimal yang akan ditampilkan
    private var modelName: String = "modelkasaran.tflite", // Nama file model ML yang digunakan
    var context: Context, // Konteks aplikasi
    val classifierListener: ClassifierListener? // Listener untuk mengelola hasil klasifikasi atau error
) {

    // Variabel untuk objek ImageClassifier
    private var imageClassifier: ImageClassifier? = null

    init {
        // Inisialisasi ImageClassifier saat objek dibuat
        setupImageClassifier()
    }

    // Menyiapkan ImageClassifier dengan opsi yang telah dikonfigurasi
    private fun setupImageClassifier() {
        val optionsBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(threshold) // Set ambang batas skor klasifikasi
            .setMaxResults(maxResults) // Set jumlah maksimal hasil klasifikasi
        val baseOptionsBuilder = BaseOptions.builder()
            .setNumThreads(4) // Set jumlah thread yang digunakan untuk klasifikasi
        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        try {
            // Mencoba untuk membuat classifier dari file model
            imageClassifier = ImageClassifier.createFromFileAndOptions(
                context,
                modelName,
                optionsBuilder.build()
            )
        } catch (e: IllegalStateException) {
            // Memanggil listener jika terjadi error saat inisialisasi classifier
            classifierListener?.onError(context.getString(R.string.image_classifier_failed))
            Log.e(TAG, e.message.toString())
        }
    }

    // Mengklasifikasikan gambar statis dari URI yang diberikan
    fun classifyStaticImage(imageUri: Uri) {
        if (imageClassifier == null) {
            setupImageClassifier() // Mengatur ulang classifier jika belum diinisialisasi
        }

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR)) // Ubah ukuran gambar ke 224x224
            .add(CastOp(DataType.UINT8)) // Mengubah tipe data menjadi UINT8
            .build()

        // Mendekode URI menjadi bitmap dan memprosesnya melalui model klasifikasi
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, imageUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        }.copy(Bitmap.Config.ARGB_8888, true)?.let { bitmap ->
            val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap)) // Mengonversi bitmap ke TensorImage
            val results = imageClassifier?.classify(tensorImage) // Mengklasifikasikan gambar
            classifierListener?.onResults(results) // Mengirim hasil ke listener
        }
    }

    // Interface untuk mendefinisikan listener hasil klasifikasi atau error
    interface ClassifierListener {
        fun onError(error: String) // Fungsi untuk menangani error
        fun onResults(results: List<Classifications>?) // Fungsi untuk menangani hasil klasifikasi
    }

    companion object {
        private const val TAG = "ImageClassifierHelper" // Tag log untuk debugging
    }
}
