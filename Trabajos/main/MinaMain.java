package main;


import kareltherobot.*;
import java.awt.Color;

public class MinaMain implements Directions {
    public static final Extraccion transporte = new Extraccion(); // Punto entre mineros y trenes
    public static final Extraccion extraccion = new Extraccion(); // Punto entre trenes y extractores

    // Bodegas donde los beepers terminan el recorrido
    public static final Extraccion[] bodegas = { new Extraccion(), new Extraccion(), new Extraccion(), new Extraccion() };

    public static void main(String[] args) {
        // Define las cantidades de robots, en 2 por defecto
        int cantidadM = 2;
        int cantidadT = 2;
        int cantidadE = 2;

        // Extrae las cantidades de robots de los argumentos del comando
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-m")) cantidadM = Integer.parseInt(args[i + 1]);
            else if (args[i].equals("-t")) cantidadT = Integer.parseInt(args[i + 1]);
            else if (args[i].equals("-e")) cantidadE = Integer.parseInt(args[i + 1]);
        }

        // Define los parametros del mundo
        World.readWorld("mina.kwld");
        World.setDelay(10);
        World.setVisible(true);

        // Crea los robots
        for (int i = 0; i < cantidadM; i++) new Minero(11, i + 2, Color.BLACK);
        for (int i = 0; i < cantidadT; i++) new Tren(13, i + 2, Color.BLUE);
        for (int i = 0; i < cantidadE; i++) new Extractor(16, i + 7, Color.RED);
    }
}
