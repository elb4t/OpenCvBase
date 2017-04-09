package es.elb4t.opencvbase;

import org.opencv.core.Mat;

/**
 * Created by eloy on 9/4/17.
 */

public class Procesador {

    public Procesador() { //Constructor
    }
    public Mat procesa(Mat entrada) {
        Mat salida = entrada.clone();
        return salida;
    }

}
