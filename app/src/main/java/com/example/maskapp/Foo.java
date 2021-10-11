/*package com.example.maskapp;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class Foo {

    public static void main ( String[] args ) {
        //System.loadLibrary();
        Interpreter tflite;
        ImageProcessor imageProcessor;
        TensorImage tensorImage;
        TensorBuffer probabilityBuffer;
        Bitmap bitmap;

        // Load tflite object from model file
        try {
            tflite = null;
            File f = new File( "/home/f_ahmed/AndroidStudioProjects/MaskApp/app/src/androidTest/assets" );
            tflite = new Interpreter( f );
        } catch ( Exception ex ) {
            ex.printStackTrace(); return;
        }

        // For input
        imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR ) )
                .build();
        tensorImage = new TensorImage( DataType.UINT8 );

        // For holding output
        probabilityBuffer = TensorBuffer.createFixedSize( new int[]{1, 2}, DataType.UINT8 );


        // Process image before feeding into model
        bitmap = BitmapFactory.decodeFile("/home/f_ahmed/AndroidStudioProjects/MaskApp/app/testImage/test.jpg" );
        tensorImage.load( bitmap );
        tensorImage = imageProcessor.process( tensorImage );

        // Run model
        tflite.run( tensorImage.getBuffer() , probabilityBuffer.getBuffer() );

        // Output True if wearing mask
        int[] resArr = probabilityBuffer.getIntArray();
        boolean result = ( resArr[0] <= resArr[1] );                   // 0 is no-mask

        System.out.println(result);
    }



    /** Memory-map the model file in Assets
    private MappedByteBuffer loadModelFile() throws IOException {
        // Open model using input stream and memory map it to load
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd( "mask.tflite" );
        FileInputStream inputStream = new FileInputStream( fileDescriptor.getFileDescriptor() );
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map( FileChannel.MapMode.READ_ONLY, startOffset, declaredLength );
    }
}*/
