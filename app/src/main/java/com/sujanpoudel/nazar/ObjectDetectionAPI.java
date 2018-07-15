package com.sujanpoudel.nazar;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Vector;

public class ObjectDetectionAPI {

    private  String modelFile = "file:///android_asset/frozen_inference_graph.pb";
    public   String labelFile = "file:///android_asset/mscoco_label_map.pbtxt";
    public static int inputSize=300; //size of input image (square)
    private  int MAX_RESULTS = 100; //maximum number of output detections

    private Vector<String> labels = new Vector<String>();

    private int[] intValues; // buffer to store Color of each pixels in image
    private byte[] byteValues; //buffer to store the pixels in byte array
    private boolean logStats=false;

    private  String inputName ; //input node for tf graph
    private  String[] outputNames ; //input node for tf graph
    private float[] outputLocations;
    private float[] outputScores;
    private float[] outputClasses;
    private float[] outputNumDetections;

    final Graph g;
    final Operation inputOp;
    final Operation outputOp1;
    final Operation outputOp2;
    final Operation outputOp3;
    TensorFlowInferenceInterface inferenceInterface;

    ObjectDetectionAPI(AssetManager assetManager) {
        inputName = "image_tensor" ;
        outputNames = new String[] {"detection_boxes", "detection_scores",
                "detection_classes", "num_detections"};
        inferenceInterface = new TensorFlowInferenceInterface(assetManager, modelFile);
        g = inferenceInterface.graph();
        if(g!=null)
            Log.d("none","model loaded");

        inputOp = g.operation(inputName);
        if (inputOp == null)
            throw new RuntimeException("Failed to find node '" + inputName + "'");

        outputOp1 = g.operation("detection_scores");
        if (outputOp1 == null)
            throw new RuntimeException("Failed to find output Node 'detection_scores'");

        outputOp2 = g.operation("detection_boxes");
        if (outputOp2 == null)
            throw new RuntimeException("Failed to find output Node 'detection_boxes'");

        outputOp3 = g.operation("detection_classes");
        if (outputOp3 == null)
            throw new RuntimeException("Failed to find output Node 'detection_classes'");

        outputScores = new float[MAX_RESULTS];

        //pre allocating buffers
        intValues = new int[inputSize * inputSize];
        byteValues = new byte[inputSize * inputSize * 3];
        outputNumDetections = new float[1];
        outputLocations = new float[MAX_RESULTS * 4];
        outputClasses = new float[MAX_RESULTS];
    }
    public List<Recognition> detect( final Bitmap bitmap){
        //copy the bitmap to int array buffer
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        //normalize values for  0 to 255 to 8 bit float value
        for (int i = 0; i < intValues.length; ++i) {
            byteValues[i * 3 + 2] = (byte) (intValues[i] & 0xFF);
            byteValues[i * 3 + 1] = (byte) ((intValues[i] >> 8) & 0xFF);
            byteValues[i * 3 + 0] = (byte) ((intValues[i] >> 16) & 0xFF);
        }

        //copy the normalized byte array to TensorFlow
        inferenceInterface.feed(inputName, byteValues, 1, inputSize, inputSize, 3);
        //run the inference
        inferenceInterface.run(outputNames, logStats);
        Log.d("Nazar_Debug","finished infernece");
        //copy the result from
        inferenceInterface.fetch(outputNames[0], outputLocations);
        inferenceInterface.fetch(outputNames[1], outputScores);
        inferenceInterface.fetch(outputNames[2], outputClasses);
        inferenceInterface.fetch(outputNames[3], outputNumDetections);
        PriorityQueue<Recognition> pq  = new PriorityQueue<>(1, new Comparator<Recognition>() {
            @Override
            public int compare(Recognition o1, Recognition o2) {
                return  Float.compare(o2.getConfidence(),o1.getConfidence());
            }
        });
        for (int i = 0; i < outputScores.length; ++i) {
            final RectF detectionRect =
                    new RectF(
                            outputLocations[4 * i + 1] * inputSize,
                            outputLocations[4 * i] * inputSize,
                            outputLocations[4 * i + 3] * inputSize,
                            outputLocations[4 * i + 2] * inputSize);
            pq.add(
                    new Recognition((int) outputClasses[i],detectionRect,outputScores[i],"aa"));
        }
        final ArrayList<Recognition> recognitions = new ArrayList<>();
        for (int i = 0; i < Math.min(pq.size(), MAX_RESULTS); ++i) {
            Recognition a= pq.poll();
            recognitions.add(a);
            Log.d("none","class:"+a.getClassId()+", score:"+a.getConfidence() );
        }
        return recognitions;
    }

}
