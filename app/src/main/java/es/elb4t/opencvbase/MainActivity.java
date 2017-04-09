package es.elb4t.opencvbase;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.design.widget.Snackbar;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import android.hardware.Camera;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2,
        LoaderCallbackInterface {

    Procesador procesador;

    private static final int SOLICITUD_PERMISO_CAMARA = 0;
    private static final int SOLICITUD_PERMISO_EXTERNAL_STORAGE = 0;
    private View vista;
    private static final String TAG = "EjemploOCV(MainActivity";
    private CameraBridgeViewBase cameraView;

    private int tipoEntrada = 0; // 0 -> cámara 1 -> fichero1 2 -> fichero2
    Mat imagenRecurso_;
    boolean recargarRecurso = false;
    private int indiceCamara; // 0-> camara trasera; 1-> camara frontal
    private int cam_anchura = 320;// resolucion deseada de la imagen
    private int cam_altura = 240;
    private static final String STATE_CAMERA_INDEX = "cameraIndex";
    private boolean guardarSiguienteImagen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        vista = findViewById(R.id.layoutApp);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            solicitarPermisoCamara();
            solicitarPermisoArchivos();
        }

        if (savedInstanceState != null) {
            indiceCamara = savedInstanceState.getInt(STATE_CAMERA_INDEX, 0);
        } else {
            indiceCamara = CameraBridgeViewBase.CAMERA_ID_BACK;
        }
        cameraView = (CameraBridgeViewBase) findViewById(R.id.vista_camara);
        cameraView.setCameraIndex(indiceCamara);
        cameraView.setCvCameraViewListener(this);


    }

    void solicitarPermisoCamara() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Snackbar.make(vista, "Falta el permiso de la camera", Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, SOLICITUD_PERMISO_CAMARA);
                }
            }).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, SOLICITUD_PERMISO_CAMARA);

        }
    }

    void solicitarPermisoArchivos() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Snackbar.make(vista, "Falta el permiso de archivo", Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, SOLICITUD_PERMISO_EXTERNAL_STORAGE);
                }
            }).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, SOLICITUD_PERMISO_EXTERNAL_STORAGE);

        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        openOptionsMenu();
        return true;
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
// Save the current camera index.
        savedInstanceState.putInt(STATE_CAMERA_INDEX, indiceCamara);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cambiarCamara:
                indiceCamara++;
                if (indiceCamara == CameraBridgeViewBase.CAMERA_ID_BACK) {
                    indiceCamara = CameraBridgeViewBase.CAMERA_ID_BACK;
                } else
                    indiceCamara = CameraBridgeViewBase.CAMERA_ID_FRONT;
                recreate();
                break;
            case R.id.resolucion_800x600:
                cam_anchura = 800;
                cam_altura = 600;
                reiniciarResolucion();
                break;
            case R.id.resolucion_640x480:
                cam_anchura = 640;
                cam_altura = 480;
                reiniciarResolucion();
                break;
            case R.id.resolucion_320x240:
                cam_anchura = 320;
                cam_altura = 240;
                reiniciarResolucion();
                break;
            case R.id.entrada_camara:
                tipoEntrada = 0;
                break;
            case R.id.entrada_fichero1:
                tipoEntrada = 1;
                recargarRecurso = true;
                break;
            case R.id.entrada_fichero2:
                tipoEntrada = 2;
                recargarRecurso = true;
                break;
            case R.id.guardar_imagenes:
                guardarSiguienteImagen = true;
                break;
        }
        String msg = "W=" + Integer.toString(cam_anchura) + " H= " +
                Integer.toString(cam_altura) + " Cam= " +
                Integer.toBinaryString(indiceCamara);
        Toast.makeText(MainActivity.this, msg,
                Toast.LENGTH_SHORT).show();
        return true;
    }

    public void reiniciarResolucion() {
        cameraView.disableView();
        cameraView.setMaxFrameSize(cam_anchura, cam_altura);
        cameraView.enableView();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraView != null)
            cameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this,
                this);

        int rotacion = getWindowManager().getDefaultDisplay().getRotation();
        //Orientacion vertical
        if (rotacion==Surface.ROTATION_0 || rotacion == Surface.ROTATION_180) {
            cam_anchura = 240;
            cam_altura = 320;
            reiniciarResolucion();
        }// orientacion horizontal
        else {
            reiniciarResolucion();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraView != null)
            cameraView.disableView();
    }

    @Override
    public void onManagerConnected(int status) {
        switch (status) {
            case LoaderCallbackInterface.SUCCESS:
                Log.i(TAG, "OpenCV se cargo correctamente");
                cameraView.setMaxFrameSize(cam_anchura, cam_altura);
                cameraView.enableView();
                break;
            default:
                Log.e(TAG, "OpenCV no se cargo");
                Toast.makeText(MainActivity.this, "OpenCV no se cargo",
                        Toast.LENGTH_SHORT).show();
                finish();
                break;
        }

    }

    @Override
    public void onPackageInstall(int operation, InstallCallbackInterface callback) {

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        procesador = new Procesador();
        cam_altura = height; //Estas son las que se usan de verdad
        cam_anchura = width;
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat entrada;
        if (tipoEntrada == 0) {
            //entrada = inputFrame.rgba();
            entrada = inputFrame.gray();
        } else {
            if(recargarRecurso == true) {
                imagenRecurso_ = new Mat();
//Poner aqui el nombre de los archivos copiados
                int RECURSOS_FICHEROS[] = {0, R.raw.img1, R.raw.img2};
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
                        RECURSOS_FICHEROS[tipoEntrada]);
//Convierte el recurso a una Mat de OpenCV
                Utils.bitmapToMat(bitmap, imagenRecurso_);
                Imgproc.resize(imagenRecurso_, imagenRecurso_, new Size(cam_anchura,
                        cam_altura));
                recargarRecurso = false;
            }
            entrada = imagenRecurso_;
        }
        Mat esquina = entrada.submat(0,10,0,10); //Arriba-izquierda
        esquina.setTo(new Scalar(255,255,255));
        Mat salida = procesador.procesa(entrada);
        if (guardarSiguienteImagen) {//Para foto salida debe ser rgba
            takePhoto(entrada, salida);
            guardarSiguienteImagen = false;
        }
        if(tipoEntrada > 0) {
//Es necesario que el tamaño de la salida coincida con el real de captura
            Imgproc.resize(salida, salida, new Size(cam_anchura , cam_altura));
        }
        return salida;
    }
    private void takePhoto(final Mat input, final Mat output) {
// Determina la ruta para crear los archivos
        final long currentTimeMillis = System.currentTimeMillis();
        final String appName = getString(R.string.app_name);
        final String galleryPath = Environment
                .getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).toString();
        final String albumPath = galleryPath + "/" + appName;
        final String photoPathIn = albumPath + "/In_" + currentTimeMillis
                + ".png";
        final String photoPathOut = albumPath + "/Out_" + currentTimeMillis
                + ".png";
// Asegurarse que el directorio existe
        File album = new File(albumPath);
        if (!album.isDirectory() && !album.mkdirs()) {
            Log.e(TAG, "Error al crear el directorio " + albumPath);
            return;
        }
// Intenta crear los archivos
        Mat mBgr = new Mat();
        if (output.channels() == 1)
            Imgproc.cvtColor(output, mBgr, Imgproc.COLOR_GRAY2BGR, 3);
        else
            Imgproc.cvtColor(output, mBgr, Imgproc.COLOR_RGBA2BGR, 3);
        if (!Imgcodecs.imwrite(photoPathOut, mBgr)) {
            Log.e(TAG, "Fallo al guardar " + photoPathOut);
        }
        if (input.channels() == 1)
            Imgproc.cvtColor(input, mBgr, Imgproc.COLOR_GRAY2BGR, 3);
        else
            Imgproc.cvtColor(input, mBgr, Imgproc.COLOR_RGBA2BGR, 3);
        if (!Imgcodecs.imwrite(photoPathIn, mBgr))
            Log.e(TAG, "Fallo al guardar " + photoPathIn);
        mBgr.release();
        return;
    }
}
