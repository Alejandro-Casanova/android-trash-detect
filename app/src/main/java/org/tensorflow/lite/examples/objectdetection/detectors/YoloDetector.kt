package org.tensorflow.lite.examples.objectdetection.detectors

import android.content.Context
import android.graphics.RectF
import com.ultralytics.yolo.ImageProcessing
import com.ultralytics.yolo.models.LocalYoloModel
import com.ultralytics.yolo.predict.detect.DetectedObject
import com.ultralytics.yolo.predict.detect.TfliteDetector
import org.tensorflow.lite.examples.objectdetection.ObjectDetectorHelper.Companion.MODEL_YOLO_TRASH_M
import org.tensorflow.lite.examples.objectdetection.ObjectDetectorHelper.Companion.MODEL_YOLO_TRASH_N
import org.tensorflow.lite.examples.objectdetection.ObjectDetectorHelper.Companion.MODEL_YOLO_TRASH_S
import org.tensorflow.lite.support.image.TensorImage


class YoloDetector(
    var confidenceThreshold: Float = 0.5f,
    var iouThreshold: Float = 0.3f,
    var numThreads: Int = 2,
    var maxResults: Int = 3,
    var currentDelegate: Int = 0,
    var currentModel: Int = 0,
    val context: Context
): ObjectDetector {

    private var yolo: TfliteDetector
    private var ip: ImageProcessing

    init {

        yolo = TfliteDetector(context)
        yolo.setIouThreshold(iouThreshold)
        yolo.setConfidenceThreshold(confidenceThreshold)

        // val modelPath = "YOLO11n-catsdogs_float32.tflite"
        // val metadataPath = "metadata-catsdogs.yaml"
        val modelPath =
            when (currentModel) {
                MODEL_YOLO_TRASH_M -> "best_10_imgsz480_int8.tflite"
                MODEL_YOLO_TRASH_S -> "best_10_imgsz320_int8.tflite"
                MODEL_YOLO_TRASH_N -> "best_10_imgsz160_int8.tflite"
                else -> "best_10_imgsz160_int8.tflite"
            }

        val metadataPath =
            when (currentModel) {
                MODEL_YOLO_TRASH_M -> "metadata_480.yaml"
                MODEL_YOLO_TRASH_S -> "metadata_320.yaml"
                MODEL_YOLO_TRASH_N -> "metadata_160.yaml"
                else -> "metadata_160.yaml"
        }

        val config = LocalYoloModel(
            "detect",
            "tflite",
            modelPath,
            metadataPath,
        )

        val useGPU = currentDelegate == 0
        yolo.loadModel(
            config,
            useGPU
        )

        ip = ImageProcessing()

    }

    override fun detect(image: TensorImage, imageRotation: Int): DetectionResult  {

        val bitmap = image.bitmap

        val ppImage = yolo.preprocess(bitmap)
        val results = yolo.predict(ppImage)

        val detections = ArrayList<ObjectDetection>()

        // ASPECT_RATIO = 4:3
        // => imgW = imgH * 3/4
        var imgH: Int
        var imgW: Int
        if (imageRotation == 90 || imageRotation == 270) {
            imgH = ppImage.height
            imgW = imgH * 3 / 4
        }
        else {
            imgW = ppImage.width
            imgH = imgW * 3 / 4

        }


        for (result: DetectedObject in results) {
            val category = Category(
                result.label,
                result.confidence,
            )
            val yoloBox = result.boundingBox

            val left = yoloBox.left * imgW
            val top = yoloBox.top * imgH
            val right = yoloBox.right * imgW
            val bottom = yoloBox.bottom * imgH

            val bbox = RectF(
                left,
                top,
                right,
                bottom
            )
            val detection = ObjectDetection(
                bbox,
                category
            )
            detections.add(detection)
        }

        val ret = DetectionResult(ppImage, detections)
        ret.info = yolo.stats
        return ret

    }


}