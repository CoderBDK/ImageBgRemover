package com.coderbdk.imagebgremover

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.createBitmap
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

object OpenCVUtils  {

    fun init(context: Context) {
        // if (!OpenCVLoader.initLocal())
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Initialization Failed!")
        } else {
            Log.d("OpenCV", "Initialization Succeeded!")
        }
        // loadModel(context)
    }

    fun removeImageBackground(inputBitmap: Bitmap): Bitmap {
        // Convert Bitmap to Mat
        val mat = Mat()
        Utils.bitmapToMat(inputBitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB)

        val mask = Mat()
        val bgModel = Mat()
        val fgModel = Mat()
        val rect = Rect(10, 10, mat.cols() - 20, mat.rows() - 20)

        Imgproc.grabCut(mat, mask, rect, bgModel, fgModel, 2, Imgproc.GC_INIT_WITH_RECT)

        val resultMask = Mat()
        Core.compare(mask, Scalar(Imgproc.GC_PR_FGD.toDouble()), resultMask, Core.CMP_EQ)

        val foreground = Mat(mat.size(), CvType.CV_8UC3, Scalar(255.0, 255.0, 255.0))
        mat.copyTo(foreground, resultMask)

        val resultBitmap = createBitmap(foreground.cols(), foreground.rows())

        Utils.matToBitmap(foreground, resultBitmap)
        return resultBitmap
    }
}