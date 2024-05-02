package main;
import java.awt.Color;
import java.util.concurrent.Semaphore;

public class Extractor extends MejorRobot {
    private static final Semaphore extraerMutex = new Semaphore(1); // Solo un extractor está en la mina a la vez
    private static final Semaphore bodegasMutex = new Semaphore(1); // Solo un extractor está en las bodegas a la vez
    
    public static int MAX_ALMACENADOS = 120; // Cantidad de beepers maxima a almacenar
    public static int almacenados = 0; // Cantidad de beepers llevada a las bodegas
    public static int max = 5; // Cantidad maxima de beepers que pueden cargar

    public Extractor(int street, int avenue, Color badge) {
        super(street, avenue, badge);
    }

    @Override
    public void run() {
        while (true) {
            // Espera su turno en la calle 9 avenida 2
            turn(West);
            moveTo(2);
            turn(South);
            moveTo(9);

            // Cuando no haya otro robot en la extraccion entra a la mina
            try {
                extraerMutex.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            moveToEntrance();
            turn(East);
            moveTo(2);

            // Mientras no se haya completado la extraccion
            while (almacenados < MAX_ALMACENADOS) {
                try {
                    // Espera si hay otro robot en el punto de extraccion
                    MinaMain.extraccion.puntoMutex.acquire();
                    // Si aún no hay suficientes beepers suelta el semaforo y repite el ciclo
                    if (MinaMain.extraccion.getBeepers() < max) MinaMain.extraccion.puntoMutex.release();
                    else break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Se sale del ciclo si ya se almacenó el maximo
            if (almacenados == MAX_ALMACENADOS) break;
    
            // Entra al punto de extraccion, ya agarró el semaforo en el ciclo
            turn(East);
            moveTo(3);
            // Recoge todos los beepers mientras hayan
            while (beepers < max && MinaMain.extraccion.getBeepers() > 0) MinaMain.extraccion.pickBeeper(this);
            // Se quita del punto y suelta el semaforo del punto
            turn(West);
            moveTo(2);
            MinaMain.extraccion.puntoMutex.release();
            // Sale de la mina y suelta el semaforo de extraccion para que entre el extractor esperando
            moveToBodegas();
            extraerMutex.release();

            // Entra a las bodegas y adquiere el semaforo de las bodegas
            turn(South);
            move();
            turn(East);
            try {
                bodegasMutex.acquire();
                short bodega = 0; // Indice de la bodega en la que está
                while (beepers > 0) {
                    if (MinaMain.bodegas[bodega].getBeepers() < (int) (MAX_ALMACENADOS / 4)) {
                        // Mientras la bodega no esté al limite pone el beeper
                        MinaMain.bodegas[bodega].putBeeper(this);
                        almacenados++;
                    } else {
                        // Si ya está al limite se mueve a la siguiente
                        bodega++;
                        move();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                // Suelta el semaforo al salir de las bodegas
                turn(North);
                moveTo(9);
                bodegasMutex.release();
            }
            
            // Se sale del ciclo si ya se almacenó el maximo
            if (almacenados == MAX_ALMACENADOS) break;
        }

        // Si aún no se ha notificado la orden se salida, hace la notificación
        if (!exit) synchronized (exitOrder) {
            exitOrder.notifyAll();
            exit = true;
        }

        // Se sale de la mina y va al descanso eterno
        exitFromExtract(19, 9);
        turnOff();
    }
}
