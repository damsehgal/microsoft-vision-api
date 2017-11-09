package com.example.root.myapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.AnalysisResult;
import com.microsoft.projectoxford.vision.contract.Caption;
import com.microsoft.projectoxford.vision.rest.VisionServiceException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    Button takePhoto;
    TextView tags;
    ImageView capturedImage;
    public static final int TAKE_IMAGE_REQUEST = 123;
    public static final String subscriptionKey = "696e5e7513424dc8b24d8b4783b8f8e9";
    public static final String uriBase = "https://westcentralus.api.cognitive.microsoft.com/vision/v1.0/";
    String path;
    private VisionServiceClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        takePhoto = findViewById(R.id.button_take_photo);
        tags = findViewById(R.id.text_view_tags);
        capturedImage = findViewById(R.id.image_view_captured_image);
        client = new VisionServiceRestClient(subscriptionKey, uriBase);
        takePhoto.setOnClickListener(new TakePhotoOnClickListener());


    }

    public class TakePhotoOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            Intent takeImageIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(takeImageIntent, TAKE_IMAGE_REQUEST);
        }
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        Log.e(TAG, "getImageUri: " + inImage.getByteCount());
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public String getRealPathFromURI(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        return cursor.getString(idx);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TAKE_IMAGE_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Bitmap bmp = (Bitmap) data.getExtras().get("data");
                Uri tempUri = getImageUri(this, bmp);
                path = getRealPathFromURI(tempUri);
                File file = new File(path);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                capturedImage.setImageBitmap(bitmap);
                getImageCaption(new ByteArrayInputStream(stream.toByteArray()));
            }
        }
    }

    void getImageCaption(InputStream inputStream) {
        final AsyncTask<InputStream, String, String> visionTask = new AsyncTask<InputStream, String, String>() {
            @Override
            protected String doInBackground(InputStream... params) {
                String[] features = {"Description"};
                String[] details = {};

                AnalysisResult result = null;
                try {
                    result = client.analyzeImage(params[0], features, details);

                    Log.d(TAG, "doInBackground: " + result);
                } catch (VisionServiceException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return new Gson().toJson(result);
            }

            protected void onPostExecute(String s) {


                AnalysisResult result = new Gson().fromJson(s, AnalysisResult.class);
                StringBuilder stringBuilder = new StringBuilder();
                for (Caption caption : result.description.captions) {
                    stringBuilder.append(caption.text);
                }
                tags.setText(stringBuilder);

            }
        };
        visionTask.execute(inputStream);

    }

}
