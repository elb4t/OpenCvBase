package es.elb4t.opencvbase;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.*;

/**
 * Created by eloy on 9/4/17.
 */

public class Procesador {

    private Mat gris;
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


    public enum Salida {
        ENTRADA, INTENSIDAD, OPERADOR_LOCAL, BINARIZACION,
        SEGMENTACION, RECONOCIMIENTO
    }
    public enum TipoIntensidad {
        SIN_PROCESO, LUMINANCIA, AUMENTO_LINEAL_CONSTANTE,
        EQUALIZ_HISTOGRAMA, ZONAS_ROJAS
    }
    public enum TipoOperadorLocal {SIN_PROCESO, PASO_BAJO, PASO_ALTO, GRADIENTES}
    public enum TipoBinarizacion {SIN_PROCESO, ADAPTATIVA, MAXIMO}
    public enum TipoSegmentacion {SIN_PROCESO}
    public enum TipoReconocimiento {SIN_PROCESO}

    private Salida mostrarSalida;
    private TipoIntensidad tipoIntensidad;
    private TipoOperadorLocal tipoOperadorLocal;
    private TipoBinarizacion tipoBinarizacion;
    private TipoSegmentacion tipoSegmentacion;
    private TipoReconocimiento tipoReconocimiento;

    public Procesador() {
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
        gris = new Mat();

        // Aumento lineal de contraste
        canales = new MatOfInt(0);
        numero_bins = new MatOfInt(256);
        intervalo = new MatOfFloat(0,256);
        hist = new Mat();
        imagenes = new ArrayList<Mat>();
        historiograma = new float[256];

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
                Imgproc.cvtColor(entrada, salidaintensidad,
                        COLOR_RGBA2GRAY);
                break;
            case AUMENTO_LINEAL_CONSTANTE:
                Imgproc.cvtColor(entrada, gris, COLOR_RGBA2GRAY);
                Log.e("PROCESA-----","Aumento lineal contraste");
                salidaintensidad = aumentoLinealConstante(gris); //resultado en salidaintensidad
                break;
            case EQUALIZ_HISTOGRAMA:
                Imgproc.cvtColor(entrada, gris, COLOR_RGBA2GRAY);
//Eq. Hist necesita gris
                Imgproc.equalizeHist(gris, salidaintensidad);
                break;
            case ZONAS_ROJAS:
                zonaRoja(entrada); //resultado en salidaintensidad
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
                pasoBajo(salidaintensidad); //resultado en salidatrlocal
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

    void zonaRoja(Mat entrada){
        salidaintensidad = entrada;
    }
    Mat aumentoLinealConstante(Mat entrada) {
        Mat salida = new Mat();
        imagenes.clear();
        imagenes.add(entrada);
        Imgproc.calcHist(imagenes,canales,new Mat(),hist,numero_bins,intervalo);
        hist.get(0, 0, historiograma);
        int total_pixeles = entrada.cols()*entrada.rows();
        float porcentaje_saturacion = (float) 0.05;
        int pixeles_saturados = (int) (porcentaje_saturacion * total_pixeles);
        int xmin = 0;
        int xmax = 255;
        float acumulado = 0f;
        for (int n=0; n < 256; n++){
            acumulado = acumulado + historiograma[n];
            if (acumulado > pixeles_saturados){
                xmin = n;
                break;
            }
        }
        acumulado = 0;
        for (int n=255; n >= 0; n--){
            acumulado = acumulado + historiograma[n];
            if (acumulado > pixeles_saturados){
                xmax = n;
                break;
            }
        }
        Core.subtract(entrada, new Scalar(xmin), salida);
        float pendiente = ((float) 255.0) / ((float) (xmax-xmin));
        Core.multiply(salida, new Scalar(pendiente), salida);

        return salida;
    }
    void pasoBajo(Mat entrada) {
        salidatrlocal = entrada;
    }

    void mitadMitad(Mat entrada, Mat salida){
        Log.e("MITAD CHANNELS",""+salida.channels());
        if (salida.channels() == 1) {
            Imgproc.cvtColor(entrada, entrada, COLOR_RGBA2GRAY);
        }

        Rect mitad_izquierda = new Rect();
        mitad_izquierda.x = 0;
        mitad_izquierda.y = 0;
        mitad_izquierda.height = entrada.height();
        mitad_izquierda.width = entrada.width()/2;
        Mat salida_mitad_izquierda = salida.submat(mitad_izquierda);
        Mat entrada_mitad_izquierda = entrada.submat(mitad_izquierda);
        entrada_mitad_izquierda.copyTo(salida_mitad_izquierda);
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