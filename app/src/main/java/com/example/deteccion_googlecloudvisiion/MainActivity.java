package com.example.deteccion_googlecloudvisiion;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.FaceAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.TextAnnotation;

import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    ImageView imgvCamara;
    Button btnCamara, btnFaceDetection, btnLabelDetection;
    TextView txtRespuesta;
    Vision vision;
    Uri imageUri;
    String result;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PICK_IMAGE = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgvCamara = (ImageView) findViewById(R.id.imgvCamara);
        btnCamara = (Button) findViewById(R.id.btnCamara);
        btnFaceDetection = (Button) findViewById(R.id.btnFaceDetection);
        btnLabelDetection = (Button) findViewById(R.id.btnLabelDetection);
        txtRespuesta = (TextView) findViewById(R.id.txtRespuesta);

        Vision.Builder visionBuilder = new Vision.Builder(new NetHttpTransport(),
                new AndroidJsonFactory(), null);
        visionBuilder.setVisionRequestInitializer(new VisionRequestInitializer(""));
        vision = visionBuilder.build();

    }

    public void reconocerEtiquetas(View view) {

        txtRespuesta.setText("");

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {


                BitmapDrawable drawable = (BitmapDrawable) imgvCamara.getDrawable();
                Bitmap bitmap = drawable.getBitmap();

                bitmap = scaleBitmapDown(bitmap, 1200);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                byte[] imageInByte = stream.toByteArray();

                //1.- Paso
                Image inputImage = new Image();
                inputImage.encodeContent(imageInByte);

                //2.- Feature
                Feature desiredFeature = new Feature();
                desiredFeature.setType("LABEL_DETECTION");

                //3.- Arma la Solicitud(es)
                AnnotateImageRequest request = new AnnotateImageRequest();
                request.setImage(inputImage);
                request.setFeatures(Arrays.asList(desiredFeature));

                BatchAnnotateImagesRequest batchRequest = new
                        BatchAnnotateImagesRequest();
                batchRequest.setRequests(Arrays.asList(request));

                //4.- Asignamos al control VisionBuilder la solicitud
                Vision.Images.Annotate annotateRequest = null;
                try {
                    annotateRequest = vision.images().annotate(batchRequest);


                    //5.- Enviamos la solicitud
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse batchResponse = annotateRequest.execute();

                    final StringBuilder message = new StringBuilder("Se encontraron estas cosas:\n\n");
                    List<EntityAnnotation> labels = batchResponse.getResponses().get(0).getLabelAnnotations();
                    if (labels != null) {
                        for (EntityAnnotation label : labels) {
                            message.append(String.format(Locale.US, "%.3f : %s",
                                    Double.parseDouble(label.getScore().toString()) * 100 , label.getDescription()));
                            message.append("\n");
                        }
                    } else {
                        message.append("nothing");
                    }


                    //7.- Asignar respuesta a la UI
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtRespuesta.setText(message.toString());
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void reconocerCara(View view) {

        txtRespuesta.setText("");

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {


                BitmapDrawable drawable = (BitmapDrawable) imgvCamara.getDrawable();
                Bitmap bitmap = drawable.getBitmap();

                bitmap = scaleBitmapDown(bitmap, 1200);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                byte[] imageInByte = stream.toByteArray();

                //1.- Paso
                Image inputImage = new Image();
                inputImage.encodeContent(imageInByte);

                //2.- Feature
                Feature desiredFeature = new Feature();
                desiredFeature.setType("FACE_DETECTION");

                //3.- Arma la Solicitud(es)
                AnnotateImageRequest request = new AnnotateImageRequest();
                request.setImage(inputImage);
                request.setFeatures(Arrays.asList(desiredFeature));

                BatchAnnotateImagesRequest batchRequest = new
                        BatchAnnotateImagesRequest();
                batchRequest.setRequests(Arrays.asList(request));

                //4.- Asignamos al control VisionBuilder la solicitud
                Vision.Images.Annotate annotateRequest = null;
                try {
                    annotateRequest = vision.images().annotate(batchRequest);


                    //5.- Enviamos la solicitud
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse batchResponse = annotateRequest.execute();

                    //6.- Obtener la respuesta
                    List<FaceAnnotation> faces = batchResponse.getResponses()
                            .get(0).getFaceAnnotations();
                    int numberOfFaces = faces.size();
                    int aux = 0;
                    String likelihoods = "";
                    for (int i = 0; i < numberOfFaces; i++) {
                        aux++;
                        likelihoods += "\n - Es " + traducirProbabilidadCara(faces.get(i).getJoyLikelihood()) +
                                " que la cara " + aux + " sea feliz";
                    }
                    final String message = "Esta foto tiene " + numberOfFaces + " cara/s:" + likelihoods;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtRespuesta.setText(message);
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void reconocerTexto(View view) {

        txtRespuesta.setText("");

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {


                BitmapDrawable drawable = (BitmapDrawable) imgvCamara.getDrawable();
                Bitmap bitmap = drawable.getBitmap();

                bitmap = scaleBitmapDown(bitmap, 1200);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                byte[] imageInByte = stream.toByteArray();

                //1.- Paso
                Image inputImage = new Image();
                inputImage.encodeContent(imageInByte);

                //2.- Feature
                Feature desiredFeature = new Feature();
                desiredFeature.setType("TEXT_DETECTION");

                //3.- Arma la Solicitud(es)
                AnnotateImageRequest request = new AnnotateImageRequest();
                request.setImage(inputImage);
                request.setFeatures(Arrays.asList(desiredFeature));

                BatchAnnotateImagesRequest batchRequest = new
                        BatchAnnotateImagesRequest();
                batchRequest.setRequests(Arrays.asList(request));

                //4.- Asignamos al control VisionBuilder la solicitud
                Vision.Images.Annotate annotateRequest = null;
                try {
                    annotateRequest = vision.images().annotate(batchRequest);


                    //5.- Enviamos la solicitud
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse batchResponse = annotateRequest.execute();

                    //6.- Obtener la respuesta
                    TextAnnotation text = batchResponse.getResponses().get(0).getFullTextAnnotation();

                    result = text.getText();

                    //7.- Asignar respuesta a la UI
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtRespuesta.setText(result);
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private String traducirProbabilidadCara(String joyLikelihood) {
        String respuesta = joyLikelihood;

        if (joyLikelihood.equals("VERY_LIKELY"))
            respuesta = "muy probable";
        else if (joyLikelihood.equals("VERY_UNLIKELY"))
            respuesta = "muy poco probable";
        else if (joyLikelihood.equals("LIKELY"))
            respuesta = "probable";
        else if (joyLikelihood.equals("UNLIKELY"))
            respuesta = "poco probable";

        return respuesta;
    }

    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    public void abrirCamara(View v) {
        txtRespuesta.setText("");

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    public void abrirGaleria(View v)
    {
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            imgvCamara.setImageBitmap(imageBitmap);
        }
        if(resultCode == RESULT_OK && requestCode == PICK_IMAGE){
            imageUri = data.getData();
            imgvCamara.setImageURI(imageUri);
        }
    }
}
