package es.elb4t.opencvbase;

import android.os.CpuUsageInfo;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.*;

/**
 * Created by eloy on 9/4/17.
 */

public class Procesador {

    //private boolean mostrarEntradaGris;
    private Mat salidaTemp;
    private Mat salidaintensidad;
    private Mat salidatrlocal;
    private Mat salidabinarizacion;
    private Mat salidasegmentacion;
    private Mat salidaocr;

    // Aumento lineal de contraste
    MatOfInt canales;
    MatOfInt numero_bins;
    MatOfFloat intervalo;
    Mat hist;
    List<Mat> imagenes;
    float[] historiograma;

    //Zonas rojas
    Mat red;
    Mat green;
    Mat blue;
    Mat maxGB;

    // Filtro paso-alto
    Mat paso_bajo;


    public enum Salida {
        ENTRADA, INTENSIDAD, OPERADOR_LOCAL, BINARIZACION,
        SEGMENTACION, RECONOCIMIENTO
    }

    public enum TipoIntensidad {
        SIN_PROCESO, LUMINANCIA, AUMENTO_LINEAL_CONSTANTE,
        EQUALIZ_HISTOGRAMA, ZONAS_ROJAS
    }

    public enum TipoOperadorLocal {SIN_PROCESO, PASO_BAJO, PASO_ALTO, GRADIENTES}

    public enum TipoBinarizacion {SIN_PROCESO,BINARIO_SOBRE_ROJA, ADAPTATIVA, MAXIMO}

    public enum TipoSegmentacion {SIN_PROCESO}

    public enum TipoReconocimiento {SIN_PROCESO}

    private Salida mostrarSalida;
    private TipoIntensidad tipoIntensidad;
    private TipoOperadorLocal tipoOperadorLocal;
    private TipoBinarizacion tipoBinarizacion;
    private TipoSegmentacion tipoSegmentacion;
    private TipoReconocimiento tipoReconocimiento;

    public Procesador() {
       // mostrarEntradaGris = false;
        mostrarSalida = Salida.INTENSIDAD;
        tipoIntensidad = TipoIntensidad.LUMINANCIA;
        tipoOperadorLocal = TipoOperadorLocal.SIN_PROCESO;
        tipoBinarizacion = TipoBinarizacion.SIN_PROCESO;
        tipoSegmentacion = TipoSegmentacion.SIN_PROCESO;
        tipoReconocimiento = TipoReconocimiento.SIN_PROCESO;
        salidaintensidad = new Mat();
        salidatrlocal = new Mat();
        salidabinarizacion = new Mat();
        salidasegmentacion = new Mat();
        salidaocr = new Mat();
        salidaTemp = new Mat();

        // Aumento lineal de contraste
        canales = new MatOfInt(0);
        numero_bins = new MatOfInt(256);
        intervalo = new MatOfFloat(0, 256);
        hist = new Mat();
        imagenes = new ArrayList<Mat>();
        historiograma = new float[256];

        // Zonas rojas
        red = new Mat();
        green = new Mat();
        blue = new Mat();
        maxGB = new Mat();

        // Filtro paso-alto
        paso_bajo = new Mat();
    }

    public Mat procesa(Mat entrada) {
        if (mostrarSalida == Salida.ENTRADA) {
            return entrada;
        }
// Transformación intensidad
        switch (tipoIntensidad) {
            case SIN_PROCESO:
                salidaintensidad = entrada;
                break;
            case LUMINANCIA:
                salidaintensidad = convertirAGris(entrada);
                break;
            case AUMENTO_LINEAL_CONSTANTE:
                Log.e("PROCESA-----", "Aumento lineal contraste");
                salidaintensidad = aumentoLinealConstante(convertirAGris(entrada)); //resultado en salidaintensidad
                break;
            case EQUALIZ_HISTOGRAMA:
//Eq. Hist necesita gris
                Imgproc.equalizeHist(convertirAGris(entrada), salidaintensidad);
                break;
            case ZONAS_ROJAS:
                salidaintensidad = zonaRoja(entrada); //resultado en salidaintensidad
                break;
            default:
                salidaintensidad = entrada;
        }
        if (mostrarSalida == Salida.INTENSIDAD) {
            return salidaintensidad;
        }
// Operador local
        switch (tipoOperadorLocal) {
            case SIN_PROCESO:
                salidatrlocal = salidaintensidad;
                break;
            case PASO_BAJO:
                salidatrlocal = pasoBajo(convertirAGris(salidaintensidad)); //resultado en salidatrlocal
                break;
        }
        if (mostrarSalida == Salida.OPERADOR_LOCAL) {
            return salidatrlocal;
        }
// Binarización
        switch (tipoBinarizacion) {
            case SIN_PROCESO:
                salidabinarizacion = salidatrlocal;
                break;
            case BINARIO_SOBRE_ROJA:
                salidabinarizacion = binarioSobreRoja(salidatrlocal);
                break;
            default:
                salidabinarizacion = salidatrlocal;
                break;
        }
        if (mostrarSalida == Salida.BINARIZACION) {
            return salidabinarizacion;
        }
// Segmentación
        switch (tipoSegmentacion) {
            case SIN_PROCESO:
                salidasegmentacion = salidabinarizacion;
                break;
        }
        if (mostrarSalida == Salida.SEGMENTACION) {
            return salidasegmentacion;
        }
// Reconocimiento OCR
        switch (tipoReconocimiento) {
            case SIN_PROCESO:
                salidaocr = salidasegmentacion;
                break;
        }
        return salidaocr;
    }

    Mat zonaRoja(Mat entrada) {
        if (entrada.channels() == 1)
            Imgproc.cvtColor(entrada, entrada, COLOR_GRAY2BGRA);
        Core.extractChannel(entrada, red, 0);
        Core.extractChannel(entrada, green, 1);
        Core.extractChannel(entrada, blue, 2);
        Core.max(green, blue, maxGB);
        Core.subtract(red, maxGB, salidaTemp);
        return salidaTemp;
    }

    Mat aumentoLinealConstante(Mat entrada) {
        imagenes.clear();
        imagenes.add(entrada);
        Imgproc.calcHist(imagenes, canales, new Mat(), hist, numero_bins, intervalo);
        hist.get(0, 0, historiograma);
        int total_pixeles = entrada.cols() * entrada.rows();
        float porcentaje_saturacion = (float) 0.05;
        int pixeles_saturados = (int) (porcentaje_saturacion * total_pixeles);
        int xmin = 0;
        int xmax = 255;
        float acumulado = 0f;
        for (int n = 0; n < 256; n++) {
            acumulado = acumulado + historiograma[n];
            if (acumulado > pixeles_saturados) {
                xmin = n;
                break;
            }
        }
        acumulado = 0;
        for (int n = 255; n >= 0; n--) {
            acumulado = acumulado + historiograma[n];
            if (acumulado > pixeles_saturados) {
                xmax = n;
                break;
            }
        }
        Core.subtract(entrada, new Scalar(xmin), salidaTemp);
        float pendiente = ((float) 255.0) / ((float) (xmax - xmin));
        Core.multiply(salidaTemp, new Scalar(pendiente), salidaTemp);

        return salidaTemp;
    }

    Mat pasoBajo(Mat entrada) {
        int filter_size = 17;
        Size s = new Size(filter_size, filter_size);
        Imgproc.blur(entrada, paso_bajo, s);
        Core.subtract(paso_bajo, entrada, salidaTemp);
        Scalar ganancia = new Scalar(2);
        Core.multiply(salidaTemp, ganancia, salidaTemp);
        return salidaTemp;
    }

    Mat binarioSobreRoja(Mat entrada){
        Core.MinMaxLocResult minMax = Core.minMaxLoc(entrada);
        int maximum = (int) minMax.maxVal;
        int thresh = maximum / 4;
        Imgproc.threshold(entrada, entrada, thresh, 255, THRESH_BINARY);
        return entrada;
    }

    void mitadMitad(Mat entrada, Mat salida) {
        Log.e("MITAD CHANNELS", "" + salida.channels());
        if (salida.channels() != 1) {
            Imgproc.cvtColor(entrada, entrada, COLOR_RGBA2GRAY);
        }

        Rect mitad_izquierda = new Rect();
        mitad_izquierda.x = 0;
        mitad_izquierda.y = 0;
        mitad_izquierda.height = entrada.height();
        mitad_izquierda.width = entrada.width() / 2;
        Mat salida_mitad_izquierda = salida.submat(mitad_izquierda);
        Mat entrada_mitad_izquierda = entrada.submat(mitad_izquierda);
        entrada_mitad_izquierda.copyTo(salida_mitad_izquierda);
        salida_mitad_izquierda.release();
        entrada_mitad_izquierda.release();
    }

    Mat convertirAGris(Mat temp){
        Log.e("CONVERTIR GRIS", "" + temp.channels());
        if (temp.channels() != 1)
            Imgproc.cvtColor(temp, temp, COLOR_RGBA2GRAY);
        return temp;
    }


    public void setMostrarSalida(Salida mostrarSalida) {
        this.mostrarSalida = mostrarSalida;
    }

    public void setTipoIntensidad(TipoIntensidad tipoIntensidad) {
        this.tipoIntensidad = tipoIntensidad;
    }

    public void setTipoOperadorLocal(TipoOperadorLocal tipoOperadorLocal) {
        this.tipoOperadorLocal = tipoOperadorLocal;
    }

    public void setTipoBinarizacion(TipoBinarizacion tipoBinarizacion) {
        this.tipoBinarizacion = tipoBinarizacion;
    }

    public void setTipoSegmentacion(TipoSegmentacion tipoSegmentacion) {
        this.tipoSegmentacion = tipoSegmentacion;
    }

    public void setTipoReconocimiento(TipoReconocimiento tipoReconocimiento) {
        this.tipoReconocimiento = tipoReconocimiento;
    }
}