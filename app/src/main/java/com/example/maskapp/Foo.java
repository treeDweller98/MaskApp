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
}





package com.example.maskapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


public class MainActivity extends AppCompatActivity {
    private static final int RESULT_LOAD_IMAGE = 1;

    TextView resultText;
    Button inferBtn;
    ImageView imageView;

    Interpreter tflite;
    ImageProcessor imageProcessor;
    TensorImage tensorImage;
    TensorBuffer probabilityBuffer;
    Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]  {android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        Button buttonLoadImage = (Button) findViewById(R.id.button);
        buttonLoadImage.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                resultText = findViewById( R.id.resultTxt );
                resultText.setText("");
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }

        } );

        inferBtn = (Button) findViewById( R.id.inferBtn );

        // Load tflite object from model file
        try {
            tflite = new Interpreter( loadModelFile() );
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }

        // For input
        imageProcessor = new ImageProcessor.Builder()
                            .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR ) )
                            .build();
        tensorImage = new TensorImage( DataType.UINT8 );

        // For holding output
        probabilityBuffer = TensorBuffer.createFixedSize( new int[]{1, 2}, DataType.UINT8 );
    }

    public void inferBtnOnClick( View v ) {
        if ( bitmap == null ) {
            return;
        } else if ( doInference() ) {
            resultText.setText( "Yay masks" );
        } else {
            resultText.setText( "You're probably a cunt" );
        }
    }

    public boolean doInference() {

        // Process image before feeding into model
        tensorImage.load( bitmap );
        tensorImage = imageProcessor.process( tensorImage );

        // Run model
        tflite.run( tensorImage.getBuffer() , probabilityBuffer.getBuffer() );

        // Output True if wearing mask
        int[] resArr = probabilityBuffer.getIntArray();
        return ( resArr[0] <= resArr[1] );                   // 0 is no-mask
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //This functions return the selected image from gallery
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query( selectedImage,
                    filePathColumn, null, null, null );
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            try {
                bitmap = BitmapFactory.decodeFile(picturePath);
                imageView = (ImageView) findViewById(R.id.image);
                imageView.setImageBitmap( bitmap );

                //Setting the URI so we can read the Bitmap from the image
                imageView.setImageURI(null);
                imageView.setImageURI(selectedImage);
            } catch ( Exception e ) {
                resultText.setText( "heheheh" );
            }

        }
    }

     Memory-map the model file in Assets
private MappedByteBuffer loadModelFile() throws IOException {
        // Open model using input stream and memory map it to load
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd( "mask.tflite");
        FileInputStream inputStream = new FileInputStream( fileDescriptor.getFileDescriptor() );
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map( FileChannel.MapMode.READ_ONLY, startOffset, declaredLength );
        }

// Storage Permissions
private static final int REQUEST_EXTERNAL_STORAGE = 1;
private static String[] PERMISSIONS_STORAGE = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
        };


 * Checks if the app has permission to write to device storage
 *
 * If the app does not has permission then the user will be prompted to grant permissions
 *
 * @param activity

public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
        // We don't have permission so prompt the user
        ActivityCompat.requestPermissions(
        activity,
        PERMISSIONS_STORAGE,
        REQUEST_EXTERNAL_STORAGE
        );
        }
        }

        }


<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <TextView
        android:id="@+id/resultTxt"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintVertical_bias="0.651" />

    <Button
        android:id="@+id/inferBtn"
        android:layout_width="213dp"
        android:layout_height="38dp"
        android:onClick="inferBtnOnClick"
        android:text="Infer"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintHorizontal_bias="0.498"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintVertical_bias="0.765" />

    <ImageView
        android:id="@+id/imageView"
        app:layout_constraintTop_toTopOf="parent"
        android:layout_width="match_parent"
        android:layout_height="400dp"
        android:layout_marginBottom="20dp"
        android:scaleType="fitCenter" />

    <Button
        android:id="@+id/button"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="LOAD IMAGE"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@+id/inferBtn" />

</androidx.constraintlayout.widget.ConstraintLayout>



*/








