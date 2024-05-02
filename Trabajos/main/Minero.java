package main;
import java.awt.Color;
import java.util.concurrent.Semaphore;

public class Minero extends MejorRobot {
    private static final Semaphore minandoMutex = new Semaphore(1); // Solo un minero mina a la vez
    private static final Semaphore terminoMutex = new Semaphore(1); // Solo un minero cambia "terminó" a la vez
    public static boolean termino = false; // Estado de finalización de la mina

    public static int max = 5; // Cantidad maxima de beepers que pueden cargar

    public Minero(int street, int avenue, Color badge) {
        super(street, avenue, badge);
    }

    public void extract() {
        // Mientras le quede espacio y esté sobre un beeper lo recoge
        while (beepers < max && nextToABeeper()) {
            pickBeeper();
            this.beepers++;
        }
    }

    public boolean checkEnd() {
        try {
            // Revisa se se terminó la mina para salir del loop
            terminoMutex.acquire();
            if (termino) return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            terminoMutex.release();
        }
        return false;
    }

    @Override
    public void run() {
        // Al iniciar el mundo entra a la mina
        moveToEntrance();
        moveToMine();
        // Se mueve a la posicion de espera por defecto
        turn(East);
        moveTo(13);
        moveToWait(false);

        while (!checkEnd()) {
            // Revisa se se terminó la mina para salir del loop

            // Espera mirando hacia arriba y sube cuando hayan liberado la mina
            turn(North);
            try {
                minandoMutex.acquire();
                // Revisa se se terminó la mina para salir del loop
                if (checkEnd()) break;
                moveTo(11);
                
                // Se mueve hacia la derecha mientras no encuentre beepers y hasta que llegue a una pared
                while (!nextToABeeper()) {
                    turn(East);
                    if (!frontIsClear()) {
                        terminoMutex.acquire();
                        termino = true;
                        terminoMutex.release();
                        break;
                    }
                    moveTo(avenue + 1);
                }
                
                // Revisa se se terminó la mina para salir del loop
                if (checkEnd()) break;
            
                // Extrae todos los beepers y se mueve al punto de transporte
                extract();
                turn(West);
                moveTo(14);
                MinaMain.transporte.puntoMutex.acquire();
                // Cuando entre al punto de transporte libera la mina para el siguiente minero
                moveTo(13);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                minandoMutex.release();
            }

            // Pone todos sus beepers se mueve al punto de espera y libera el punto de transporte
            while (beepers > 0) MinaMain.transporte.putBeeper(this);
            turn(South);
            moveTo(10);
            MinaMain.transporte.puntoMutex.release();
            moveToWait(true);
        }

        try {
            // Si no se ha dado la orden de salir, espera a que la den y se sale
            if (!exit) synchronized (exitOrder) {
                exitOrder.wait();
            }
            exitFromMine(West, 18, 9);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // Al final, así no haya logrado salir se muere
            turnOff();
        }
    }
}