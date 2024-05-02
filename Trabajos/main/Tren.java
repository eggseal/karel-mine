package main;
import java.awt.Color;

public class Tren extends MejorRobot {
    public static int max = 12;

    public Tren(int street, int avenue, Color badge) {
        super(street, avenue, badge);
    }

    @Override
    public void run() {
        // Entra a la mina y va al punto de transporte
        moveToEntrance();
        moveToMine();

        // Entra al ciclo hasta que se transporten todos los beepers
        while (!Minero.termino || MinaMain.transporte.getBeepers() > 0) {

            while (!Minero.termino) {
                try {
                    // Espera a que hayan mas del maximo de beepers para entrar a la posicion
                    MinaMain.transporte.puntoMutex.acquire();
                    if (MinaMain.transporte.getBeepers() < max) MinaMain.transporte.puntoMutex.release();
                    else break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            // Si salió de la espera porque terminaron de minar se sale del ciclo
            if (Minero.termino) {
                MinaMain.transporte.puntoMutex.release();
                break;
            }
    
            // Entra al punto de transporte y recoge los beepers que pueda
            moveTo(13);
            while (beepers < max) MinaMain.transporte.pickBeeper(this);
            // Se sale del punto y suelta el semaforo
            turn(South);
            moveTo(10);
            MinaMain.transporte.puntoMutex.release();

            // Se mueve al punto de extraccion
            moveToExtraction();
            try {
                // Suelta todos sus beepers
                MinaMain.extraccion.puntoMutex.acquire();
                turn(South);
                moveTo(1);
                while (beepers > 0) {
                    MinaMain.extraccion.putBeeper(this);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                // Se sale del punto de extraccion y regresa al de transporte
                turn(East);
                moveTo(4);
                MinaMain.extraccion.puntoMutex.release();
                moveToMine();
            }
        }

        try {
            // Si no se ha dado la orden de salir, espera a que la den y se sale
            if (!exit) synchronized (exitOrder) {
                exitOrder.wait();
            }
            exitFromMine(East, 17, 9);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // Al final, así no haya logrado salir se muere
            turnOff();
        }
    }
}
