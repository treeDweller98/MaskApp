package com.example.maskapp;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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

    Interpreter tflite;
    ImageProcessor imageProcessor;
    TensorImage tensorImage;
    TensorBuffer probabilityBuffer;
    Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonLoadImage = findViewById(R.id.button);
        Button detectButton = (Button) findViewById(R.id.detect);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        // Prepare the model and stuff for use
        {
            // Load tflite object from model file
            try {
                tflite = new Interpreter(loadModelFile());
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            // For input
            imageProcessor = new ImageProcessor.Builder()
                    .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                    .build();
            tensorImage = new TensorImage(DataType.UINT8);

            // For holding output
            probabilityBuffer = TensorBuffer.createFixedSize(new int[]{1, 2}, DataType.UINT8);
        }

        // Loading images
        buttonLoadImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                TextView textView = findViewById(R.id.result_text);
                textView.setText("");
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });

        // Running model
        detectButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                //Getting the image from the image view
                ImageView imageView = (ImageView) findViewById(R.id.image);
                bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

                // Run model
                doInference();
            }

        });
    }

    public void doInference() {
        // Process image before feeding into model
        tensorImage.load( bitmap );
        tensorImage = imageProcessor.process( tensorImage );

        // Run model
        tflite.run( tensorImage.getBuffer() , probabilityBuffer.getBuffer() );

        // Output True if wearing mask
        int[] resArr = probabilityBuffer.getIntArray();
        boolean myResult = ( resArr[0] <= resArr[1] );                   // 0 is no-mask

        // Print result
        TextView textView = findViewById(R.id.result_text);
        if ( myResult ) {
            textView.setText( "Yay masks" );
        } else {
            textView.setText( "subject is probably a cunt" );
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //This functions return the selected image from gallery
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            ImageView imageView = (ImageView) findViewById(R.id.image);
            imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));

            //Setting the URI so we can read the Bitmap from the image
            imageView.setImageURI(null);
            imageView.setImageURI(selectedImage);


        }
    }

    /** Memory-map the model file in Assets */
    private MappedByteBuffer loadModelFile() throws IOException {
        // Open model using input stream and memory map it to load
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd( "mask.tflite");
        FileInputStream inputStream = new FileInputStream( fileDescriptor.getFileDescriptor() );
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map( FileChannel.MapMode.READ_ONLY, startOffset, declaredLength );
    }
}
